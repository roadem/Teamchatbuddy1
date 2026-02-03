package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Implémentation TTS OpenAI sans Retrofit.
 * Flux binaire direct + preprocessing FFmpeg.
 */
public class TtsOpenAI implements AutoCloseable {

    private final Context context;
    private MediaPlayer mMediaPlayer;
    private TtsOpenAIListener mTtsListener;

    private final TeamChatBuddyApplication app;

    private String model = null;
    private String voice = null;
    private String responseFormat = "mp3";
    private String instructions = null;
    private double speed ;
    private String streamFormat = "audio";

    private int mVoiceLength = -1;

    public TtsOpenAI(Context context, TeamChatBuddyApplication app) {
        this.context = context;
        this.app = app;
    }

    public TtsOpenAI setModel(String model) { this.model = model; return this; }
    public TtsOpenAI setVoice(String voice) { this.voice = voice; return this; }
    public TtsOpenAI setResponseFormat(String format) { this.responseFormat = format; return this; }
    public TtsOpenAI setInstructions(String instructions) { this.instructions = instructions; return this; }
    public TtsOpenAI setSpeed(double speed) { this.speed = speed; return this; }
    public TtsOpenAI setStreamFormat(String format) { this.streamFormat = format; return this; }
    public void setTtsListener(TtsOpenAIListener listener) { this.mTtsListener = listener; }


    /**
     * Lancement TTS OpenAI
     */
    public void start(String apiKey, String text, TtsOpenAIListener mTtsListener) {

        generateSpeech(apiKey,text,mTtsListener);
    }

    public void generateSpeech(
            String apiKey,
            String text,
            TtsOpenAIListener mTtsListener
    ) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        Log.w("MYA", "generateSpeech: " + text);
        try{
            // Corps JSON de la requête
            String apiUrl =
                    app.getParamFromFile("ChatGPT_url", "TeamChatBuddy.properties")
                            + app.getParamFromFile("TTS_OpenAI_ApiEndpoint", "TeamChatBuddy.properties");

            model = app.getParamFromFile("TTS_OpenAI_Model", "TeamChatBuddy.properties").trim();
            voice = app.getParamFromFile("TTS_OpenAI_Voice", "TeamChatBuddy.properties").trim();
            speed = Double.parseDouble(app.getParamFromFile("TTS_OpenAI_Speed", "TeamChatBuddy.properties").trim());
            instructions = app.getParamFromFile("TTS_OpenAI_Instructions", "TeamChatBuddy.properties").trim();
            JSONObject json = new JSONObject();
            json.put("model", model);
            json.put("voice", voice);
            json.put("input", text);
            json.put("response_format", "mp3");
            json.put("speed", speed);

            // Ajouter "instructions" uniquement si non vide et si le modèle les supporte
            if (instructions != null && !instructions.trim().isEmpty()) {
                json.put("instructions", instructions);
            }

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Timeout détecté automatiquement par OkHttp
                    if (e instanceof SocketTimeoutException
                            || e.getMessage().contains("timeout")) {
                        Log.e("OpenAITTS", "TIMEOUT détecté : " + e.getMessage());
                        mTtsListener.onError();    // Action souhaitée
                        return;
                    }
                    // Autres erreurs réseau
                    Log.e("OpenAITTS", "Erreur réseau : " + e.getMessage());
                    mTtsListener.onError();
                }


                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    long startTotal = System.nanoTime(); // Début total
                    long startDownload = System.nanoTime(); // Pour mesurer juste le téléchargement

                    if (!response.isSuccessful()) {
                        mTtsListener.onError();
                        return;
                    }

                    // --- Téléchargement du MP3 ---
                    byte[] audioBytes = response.body().bytes();
                    long endDownload = System.nanoTime();
                    Log.w("OpenAITTS", "Temps téléchargement (ms): " + ((endDownload - startDownload) / 1_000_000));

                    // --- Écriture fichier ---
                    long startWrite = System.nanoTime();
                    File temp = File.createTempFile("tts_openai_", ".mp3");
                    try (FileOutputStream fos = new FileOutputStream(temp)) {
                        fos.write(audioBytes);
                    }
                    long endWrite = System.nanoTime();
                    Log.w("OpenAITTS", "Temps écriture fichier (ms): " + ((endWrite - startWrite) / 1_000_000));

                    // --- Extraction durée et décision trim ---
                    long startMeta = System.nanoTime();
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(temp.getAbsolutePath());
                    String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long durationMs = Long.parseLong(durationStr);
                    mmr.release();
                    long endMeta = System.nanoTime();
                    Log.w("OpenAITTS", "Temps extraction metadata (ms): " + ((endMeta - startMeta) / 1_000_000));

                    long startTrimPlay = System.nanoTime();
                    if (durationMs > 700) {
                        // --- Trim silence via FFmpeg ---
                        File trimmed = File.createTempFile("tts_openai_trim_", ".mp3", temp.getParentFile());
                        String[] cmd = {
                                "-y",
                                "-i", temp.getAbsolutePath(),
                                "-af", "areverse,silenceremove=start_periods=1:start_duration=0.01:start_threshold=-40dB:detection=peak,areverse",
                                "-c:a", "mp3",
                                trimmed.getAbsolutePath()
                        };
                        FFmpeg.executeAsync(cmd, (id, rc) -> {
                            File toPlay = (rc == 0 ? trimmed : temp);
                            new Handler(Looper.getMainLooper())
                                    .post(() -> {
                                        playAudio(toPlay, mTtsListener);
                                        long endTotal = System.nanoTime();
                                        Log.w("OpenAITTS", "Temps total jusqu'à lecture (ms): " + ((endTotal - startTotal) / 1_000_000));
                                    });
                        });
                    } else {
                        playAudio(temp, mTtsListener);
                        long endTotal = System.nanoTime();
                        Log.w("OpenAITTS", "Temps total jusqu'à lecture (ms): " + ((endTotal - startTotal) / 1_000_000));
                    }
                }

            });
        }catch (Exception e) {
            mTtsListener.onError();
        }
    }

    /**
     * Lecture MediaPlayer
     */


    private void playAudio(File audioFile, TtsOpenAIListener listener) {
        try {
            if (mMediaPlayer != null) {
                try { mMediaPlayer.stop(); } catch (Exception ignored) {}
                try { mMediaPlayer.release(); } catch (Exception ignored) {}
                mMediaPlayer = null;
            }

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
            );

            mMediaPlayer.setDataSource(audioFile.getAbsolutePath());

            final Handler handler = new Handler(Looper.getMainLooper());

            final Runnable safetyTimeout = () -> {
                MediaPlayer mp = mMediaPlayer;
                if (mp != null) {
                    try {
                        if (mp.isPlaying()) mp.stop();
                    } catch (Exception ignored) {}
                    try { mp.release(); } catch (Exception ignored) {}
                    mMediaPlayer = null;
                }
                listener.onDone();
            };

            mMediaPlayer.setOnPreparedListener(mp -> {
                listener.onStart();
                mp.start();

                int duration = mp.getDuration();
                handler.postDelayed(safetyTimeout, duration + 1000);
            });

            mMediaPlayer.setOnCompletionListener(mp -> {
                handler.removeCallbacks(safetyTimeout);
                try { mp.release(); } catch (Exception ignored) {}
                mMediaPlayer = null;
                listener.onDone();
            });

            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                handler.removeCallbacks(safetyTimeout);
                try { mp.release(); } catch (Exception ignored) {}
                mMediaPlayer = null;
                listener.onError();
                return true;
            });

            mMediaPlayer.prepareAsync();

        } catch (Exception e) {
            listener.onError();
        }
    }


    public void stop() {
        Log.d("MYA_", "stop:");
        if (mMediaPlayer != null) {
            Log.d("MYA_", "stop: mMediaPlayer != null");
            try {
                Log.d("MYA_", "stop: mMediaPlayer");
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
            } catch (Exception ignored) {
                Log.d("MYA_", "stop: mMediaPlayer -------- Exception");
            }

            try { mMediaPlayer.reset(); } catch (Exception ignored) {}

            try { mMediaPlayer.release(); } catch (Exception ignored) {}

            mMediaPlayer = null;
            mVoiceLength = -1;
        }
    }

    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mVoiceLength = mMediaPlayer.getCurrentPosition();
        }
    }

    public void resume() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying() && mVoiceLength != -1) {
            mMediaPlayer.seekTo(mVoiceLength);
            mMediaPlayer.start();
        }
    }

    @Override
    public void close() {
        stop();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}

