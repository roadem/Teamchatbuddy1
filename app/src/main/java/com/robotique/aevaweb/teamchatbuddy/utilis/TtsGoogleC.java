package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Base64;
import android.util.Log;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.robotique.aevaweb.teamchatbuddy.models.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TtsGoogleC implements AutoCloseable {

    private final Context context;
    private MediaPlayer mMediaPlayer;
    private TtsGoogleApiListener mTtsListener;

    private VoiceSelectionRaw voiceSelection;
    private AudioConfigRaw audioConfig;

    private int mVoiceLength = -1;

    public TtsGoogleC(Context context) {
        this.context = context;
    }

    public static class VoiceSelectionRaw {
        public String languageCode;
        public String name;
        public String ssmlGender;
    }

    public static class AudioConfigRaw {
        public String audioEncoding;
        public float speakingRate;
        public float pitch;
    }

    public TtsGoogleC setVoiceSelectionParams(VoiceSelectionRaw voice) {
        this.voiceSelection = voice;
        return this;
    }

    public TtsGoogleC setAudioConfig(AudioConfigRaw cfg) {
        this.audioConfig = cfg;
        return this;
    }
    public void setTtsListener(TtsGoogleApiListener ttsListener) {
        mTtsListener = ttsListener;
    }

    public VoicesList load(String apiKey) {
        try {
            String url = "https://texttospeech.googleapis.com/v1/voices?key=" + apiKey;

            Map<String, String> headers = new HashMap<>();
            HttpResponse resp = HttpClientUtils.sendGet(url, headers, 30000);

            if (resp.responseCode != 200 || resp.body == null) {
                Log.e("MYA_TTS", "Impossible de charger les voix : " + resp.responseCode);
                return new VoicesList();
            }

            JsonObject json = new Gson().fromJson(resp.body, JsonObject.class);
            JsonArray voices = json.getAsJsonArray("voices");

            VoicesList voicesList = new VoicesList();

            for (int i = 0; i < voices.size(); i++) {

                JsonObject v = voices.get(i).getAsJsonObject();

                VoiceSelectionRaw vs = new VoiceSelectionRaw();

                // Récupère le premier languageCode (comme le SDK)
                vs.languageCode = v.getAsJsonArray("languageCodes").get(0).getAsString();
                vs.name = v.get("name").getAsString();
                vs.ssmlGender = v.get("ssmlGender").getAsString();

                voicesList.add(vs.languageCode, vs);
            }

            return voicesList;

        } catch (Exception e) {
            Log.e("MYA_TTS", "Erreur load()", e);
            return new VoicesList();
        }
    }

    public void start(String url, String text) {
        if (voiceSelection == null)
            throw new IllegalStateException("VoiceSelectionParams manquant");
        if (audioConfig == null)
            throw new IllegalStateException("AudioConfig manquant");

        stop();

        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(() -> {
            try {
                Log.i("MYA_API_Google", "=== Lancement synthèse vocale API REST ===");

                Gson gson = new Gson();

                // --- INPUT ---
                JsonObject input = new JsonObject();
                if (text.contains("<speak>"))
                    input.addProperty("ssml", text);
                else
                    input.addProperty("text", text);

                // --- VOICE ---
                JsonObject voice = new JsonObject();
                voice.addProperty("languageCode", voiceSelection.languageCode);
                voice.addProperty("name", voiceSelection.name);
                voice.addProperty("ssmlGender", voiceSelection.ssmlGender);

                Log.i("MYA_API_Google", "Voix : " + voiceSelection.name);

                // --- AUDIO CONFIG ---
                JsonObject cfg = new JsonObject();
                cfg.addProperty("audioEncoding", audioConfig.audioEncoding);
                cfg.addProperty("speakingRate", audioConfig.speakingRate);
                cfg.addProperty("pitch", audioConfig.pitch);

                Log.i("MYA_API_Google", "Encoding audio : " + audioConfig.audioEncoding);

                // --- BODY ---
                JsonObject body = new JsonObject();
                body.add("input", input);
                body.add("voice", voice);
                body.add("audioConfig", cfg);

                // --- URL ---
                String TTS_URL = "https://texttospeech.googleapis.com/v1/text:synthesize?key=" ;
                Log.i("MYA_API_Google", "URL API TTS : " + TTS_URL);
                Log.d("MYA_API_Google", "start: "+url);

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");

                HttpResponse httpResp = HttpClientUtils.sendPost(
                        url,
                        gson.toJson(body),
                        headers,
                        60000
                );

                if (httpResp.responseCode < 200 || httpResp.responseCode >= 300) {
                    Log.e("MYA_API_Google", "Erreur API : " + httpResp.responseCode);
                    if (mTtsListener != null) mTtsListener.onError();
                    return;
                }

                JsonObject respJson = gson.fromJson(httpResp.body, JsonObject.class);
                if (!respJson.has("audioContent")) {
                    Log.e("MYA_API_Google", "Champ audioContent absent");
                    if (mTtsListener != null) mTtsListener.onError();
                    return;
                }

                // --- DÉCODAGE ---
                String base64Audio = respJson.get("audioContent").getAsString();
                byte[] audioData = Base64.decode(base64Audio, Base64.DEFAULT);

                File outputDir = context.getCacheDir();
                File rawFile = File.createTempFile("tts_raw_", ".wav", outputDir);

                try (FileOutputStream fos = new FileOutputStream(rawFile)) {
                    fos.write(audioData);
                }

                // --- TRIMMING FFmpeg ---
                File trimmedFile = File.createTempFile("tts_trim_", ".wav", outputDir);

                String cmd =
                        "-y -i \"" + rawFile.getAbsolutePath() + "\" " +
                                "-af \"areverse,silenceremove=start_periods=1:start_duration=0.01:start_threshold=-40dB:detection=peak,areverse\" " +
                                "-c:a pcm_s16le \"" + trimmedFile.getAbsolutePath() + "\"";

                long t = System.currentTimeMillis();

                FFmpeg.executeAsync(cmd, (id, rc) -> {
                    Log.i("MYA_API_Google",
                            rc == 0 ?
                                    "Trimming OK en " + (System.currentTimeMillis() - t) + " ms" :
                                    "FFmpeg KO (" + rc + ")");

                    playAudioFile(rc == 0 ? trimmedFile : rawFile);
                });

            } catch (Exception e) {
                Log.e("MYA_API_Google", "Exception pendant l'appel TTS : " + e.getMessage(), e);
                if (mTtsListener != null) mTtsListener.onError();
            }
        });
    }

    private void playAudioFile(File audioFile) {
        try {
            mMediaPlayer = new MediaPlayer();
            FileInputStream fis = new FileInputStream(audioFile);
            mMediaPlayer.setDataSource(fis.getFD());
            fis.close();

            mMediaPlayer.setOnPreparedListener(mp -> {
                if (mTtsListener != null) {
                    mTtsListener.onStart();
                }
                mp.start();
            });

            mMediaPlayer.setOnCompletionListener(mp -> {
                if (mTtsListener != null) {
                    Log.d("MYA_TTS", "retard onDone");
                    mTtsListener.onDone();
                }
                mp.release();
            });

            mMediaPlayer.prepareAsync();

        } catch (Exception e) {
            if (mTtsListener != null) mTtsListener.onError();
        }
    }

    public void stop() {
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.stop();
            } catch (IllegalStateException ignored) {
                // Already stopped or not initialized
            }
            try {
                mMediaPlayer.reset();
            } catch (IllegalStateException ignored) {}
            mMediaPlayer.release();
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
        if (mMediaPlayer !=null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

    }

}
