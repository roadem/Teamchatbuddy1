package com.robotique.aevaweb.teamchatbuddy.application;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bfr.buddy.speech.shared.ISTTCallback;
import com.bfr.buddy.speech.shared.ITTSCallback;
import com.bfr.buddy.speech.shared.STTResult;
import com.bfr.buddy.speech.shared.STTResultsData;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddyApplication;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.speech.STTTask;
import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.ibm.icu.text.BreakIterator;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.konovalov.vad.Vad;
import com.konovalov.vad.VadConfig;
import com.konovalov.vad.VadListener;
import com.robotique.aevaweb.bluemicapp.callbacks.IBlueMicAudioDataListener;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.activities.MainActivity;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.ChatGptStreamMode;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.CustomGPTStreamMode;
import com.robotique.aevaweb.teamchatbuddy.models.ApriltagDetection;
import com.robotique.aevaweb.teamchatbuddy.fragments.BlueMicFragment;
import com.robotique.aevaweb.teamchatbuddy.fragments.ChatWindowFragment;
import com.robotique.aevaweb.teamchatbuddy.fragments.MainFragment;
import com.robotique.aevaweb.teamchatbuddy.fragments.OpenAiFragment;
import com.robotique.aevaweb.teamchatbuddy.fragments.SettingsFragment;
import com.robotique.aevaweb.teamchatbuddy.models.History;
import com.robotique.aevaweb.teamchatbuddy.models.Langue;
import com.robotique.aevaweb.teamchatbuddy.models.LocaleEntry;
import com.robotique.aevaweb.teamchatbuddy.models.OpenAiInfo;
import com.robotique.aevaweb.teamchatbuddy.models.Replica;
import com.robotique.aevaweb.teamchatbuddy.models.Session;
import com.robotique.aevaweb.teamchatbuddy.models.Setting;
import com.robotique.aevaweb.teamchatbuddy.observers.IDBObserver;
import com.robotique.aevaweb.teamchatbuddy.utilis.BlueMic;
import com.robotique.aevaweb.teamchatbuddy.utilis.ConfigurationFile;
import com.robotique.aevaweb.teamchatbuddy.utilis.CustomProperties;
import com.robotique.aevaweb.teamchatbuddy.utilis.DetectionCallback;
import com.robotique.aevaweb.teamchatbuddy.utilis.GoogleSTT;
import com.robotique.aevaweb.teamchatbuddy.utilis.GoogleSTTCallbacks;
import com.robotique.aevaweb.teamchatbuddy.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.teamchatbuddy.utilis.ITTSCallbacks;
import com.robotique.aevaweb.teamchatbuddy.utilis.PcmToWavConverter;
import com.robotique.aevaweb.teamchatbuddy.utilis.SettingsContentObserver;
import com.robotique.aevaweb.teamchatbuddy.utilis.TtsFactory;
import com.robotique.aevaweb.teamchatbuddy.utilis.TtsGoogleApiListener;
import com.robotique.aevaweb.teamchatbuddy.utilis.TtsGoogleC;
import com.robotique.aevaweb.teamchatbuddy.utilis.TtsOpenAI;
import com.robotique.aevaweb.teamchatbuddy.utilis.TtsOpenAIListener;
import com.robotique.aevaweb.teamchatbuddy.utilis.VoicesList;
import com.robotique.aevaweb.teamchatbuddy.utilis.apriltag.ApriltagNative;
import com.robotique.aevaweb.teamchatbuddy.utilis.apriltag.BarcodeScannerProcessor;
import com.robotique.aevaweb.teamchatbuddy.utilis.apriltag.BitmapUtils;
import com.robotique.aevaweb.teamchatbuddy.utilis.apriltag.FrameProcessing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class TeamChatBuddyApplication extends BuddyApplication {
    private static final String TAG = "TEAM_CHAT_BUDDY_Application";
    private static final String TAG_BLUEMIC_STREAMING = "TeamChatBuddy_BlueMic_Streaming";
    private static final String TAG_STREAM = "MODE_STREAM";
    private static final String TAG_STREAM_USAGE = "MODE_STREAM_USAGE";
    private int listeningDuration;
    private int listeningAttempt;
    private int speakSpeed;
    private int speakVolume;
    public int max;
    private Replica question;
    private Replica reponse;
    private Setting setting;
    private Boolean fileCreate=true;
    private Session session;
    private ArrayList<Session> listSession=new ArrayList<>();
    private History history;
    private String switchdetectLanguage;
    private String switchModeStream;
    private String switchCommande;
    private String switchVisibility;
    private String switchBIDisplay;
    private String switchEmotion;
    private Uri fileup;
    private Boolean isSpeaking = false;
    private List<IDBObserver> observers = new ArrayList<>();
    private Boolean notYet=true;
    private int textSizeBullesPX;
    private boolean activityClosed =false;
    private Boolean startRecording=false;
    private Boolean usingEmotions=false;
    private ConnectivityManager cm;
    private int questionNumber = 0;
    private int currentQuestionNubmer = 0;
    private boolean alreadyGetAnswer = false;
    private boolean openaialreadySwitchEmotion = false ;
    private boolean timeoutExpired = false ;
    private long questionTime = 0;
    private String storedResponse = "";
    private Boolean buddyFaceisTired = false;
    private int bestTextSize = 0;
    private TextToSpeech tts_android;
    private Boolean shouldPlayEmotion = false;
    private String currentEmotion = "";
    private Boolean messageError = false;
    private Langue langue;

    private List<Replica> listRepGlobale = new ArrayList<>();
    private String french = "Français";
    private String english = "Anglais";
    private String spanish = "Espagnol";
    private String deutsch = "Allemand";
    private String listeningDurationPseudo = "listening_duration";
    private String listeningAttemptPseudo = "listening_attempt";
    private String speakVolumePseudo ="speak_volume";
    private String visibilityString = "switch_visibility";
    private String emotionString = "switch_emotion";
    private String detectionLanguageString = "Detection_de_langue";
    private String modeStreamString = "Mode_Stream";
    private String commandeString = "Commands";
    private String configurationFilePseudo ="TeamChatBuddy.properties";
    private File fileupdate;
    private String langueFr = "Français";
    private String langueEn = "Anglais";

    private  int currentIndexText = 0;

    private  boolean allTextPronoucedSuccess = true;
    private String[] texteToSpeakSplitted;
    public boolean Stop_TTS_ReadSpeaker = false;

    Runnable runnableListeningHotword;
    private Handler handlerListeningHotword =new Handler();
    private Handler handler2 = new Handler();
    private final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    private final Intent speechRecognizerIntent2 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    android.speech.SpeechRecognizer speechRecognizer;
    private STTTask freeSpeechSttTask;
    private GoogleSTT googleSTT;
    private GoogleSTTCallbacks googleSTTCallbacks;
    private Boolean SttGoogleCallbackCalledOnce =true;
    private String header ="Chatgpt_Header_en";
    private String entete ="Chatgpt_Header_fr";
    private String openAIKey = "openAI_API_Key";
    private String langueInconfigurationFilePseudo = "Language";
    private Boolean initSharedpreferences = true;
    private Translator englishLanguageSelectedTranslator;
    private Translator frenchLanguageSelectedTranslator;
    private Translator languageSelectedEnglishTranslator;
    private TranslatorOptions options;
    private TranslatorOptions options1;
    private String translatedList="";
    private String languageDetected ="";
    private Boolean usingReadSpeaker;
    private boolean alreadyCalled =false;
    private Vad vad;
    public boolean isListeningHotw = false;

    private static final String TAG_STREAMING = "AudioCapture";
    private static final int SAMPLE_RATE = 8000; // Exemple : 8 kHz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private String currentState = "";

    private Boolean endRecordingWhisperAudio = false;
    private Boolean stopProcessus = false;
    private Boolean alReadyHadSpoke=false;
    private Activity activityTemp;

    private long getResponseTime = 0;
    private Boolean answerHasExceededTimeOut =false;
    private BlueMic blueMic;
    private TranscribeTask transcribeTask;
    private Thread thread;
    private Thread thread1;
    private Float previousVolume=Float.valueOf(0);
    private ChatGptStreamMode chatGptStreamMode;
    private CustomGPTStreamMode customGPTStreamMode;
    private EncodingRegistry registry;
    private Boolean appIsListeningToTheQuestion = false;
    private String toast_stt_android_indispo;
    private String toast_tts_android_indispo;
    private String toast_tts_googleApi_indispo;
    private TtsGoogleC googleCloudTTS;
    private VoicesList voiceList;
    private TtsOpenAI ttsOpenAI;
    private String chosenTTS = "";
    private int remainingAttempts;
    private Boolean appIsCurrentlyDealingWithTheQuestion = false;
    private Boolean BIExecution = false;
    private boolean alreadyChatting = false; // pour savoir si BUDDY doit prononcer l'invitation au dialogue ou non
    private String imeiDevice;
    private String tokenHealysa;
    private boolean shouldLaunchListeningAfterGetingHotWord = true;
    private boolean modeContinuousListeningON= false;
    private boolean multiCommandsDetected= false;
    private boolean timeToExecuteNextCommande = false;
    private List<String> listOfCommandmustToHavePlayed;
    public ArrayList<String> listOfQuestionInContinuousListeningMode = new ArrayList<>();
    public ArrayList<String> listOfResponseInContinuousListeningMode = new ArrayList<>();
    public ArrayList<String> listOfDetectedLanguagesOfResponseInContinuousListeningMode = new ArrayList<>();
    public ArrayList<String> listOfEmotionsForQuestionInContinuousListeningMode = new ArrayList<>();
    private boolean shouldDisplayQRCode =false;

    public boolean isShouldDisplayQRCode() {
        return shouldDisplayQRCode;
    }

    public void setShouldDisplayQRCode(boolean shouldDisplayQRCode) {
        this.shouldDisplayQRCode = shouldDisplayQRCode;
    }
    public String isAlertActivated = "No";

    public boolean isOnApp = true;
    private int counterTouch = 0;
    private int counterHotword = 0;
    private int counterTracking = 0;

    private static final String KEY_COUNT_TOUCH = "alert_count_touch";
    private static final String KEY_COUNT_HOTWORD = "alert_count_hotword";
    private static final String KEY_COUNT_TRACKING = "alert_count_tracking";
    private static final String KEY_REMAINING_TIME = "alert_remaining_time";
    private static final String KEY_WAS_PAUSED = "alert_was_paused";

    public List<String> listQRTypes = new ArrayList<>();

    public String listeningState;

    public boolean isStartMsg = false;

    public int getCounterTouch() {
        return counterTouch;
    }

    public void setCounterTouch(int counterTouch) {
        this.counterTouch = counterTouch;
    }

    public int getCounterHotword() {
        return counterHotword;
    }

    public void setCounterHotword(int counterHotword) {
        this.counterHotword = counterHotword;
    }

    public int getCounterTracking() {
        return counterTracking;
    }

    public void setCounterTracking(int counterTracking) {
        this.counterTracking = counterTracking;
    }

    public boolean isPlayingMusic() {
        return isPlayingMusic;
    }

    public void setPlayingMusic(boolean playingMusic) {
        isPlayingMusic = playingMusic;
    }

    private boolean isPlayingMusic = false;

    public boolean isPlayingRadio() {
        return isPlayingRadio;
    }

    public void setPlayingRadio(boolean playingRadio) {
        isPlayingRadio = playingRadio;
    }

    private boolean isPlayingRadio = false;


    public boolean isAlreadyChatting() {
        return alreadyChatting;
    }

    public void setAlreadyChatting(boolean alreadyChatting) {
        this.alreadyChatting = alreadyChatting;
    }

    public boolean isShouldLaunchListeningAfterGetingHotWord() {
        return shouldLaunchListeningAfterGetingHotWord;
    }

    public void setShouldLaunchListeningAfterGetingHotWord(boolean shouldLaunchListeningAfterGetingHotWord) {
        this.shouldLaunchListeningAfterGetingHotWord = shouldLaunchListeningAfterGetingHotWord;
    }

    public Boolean getBIExecution() {
        return BIExecution;
    }

    public void setBIExecution(Boolean BIExecution) {
        this.BIExecution = BIExecution;
    }

    public Boolean getAppIsCurrentlyDealingWithTheQuestion() {
        return appIsCurrentlyDealingWithTheQuestion;
    }

    public void setAppIsCurrentlyDealingWithTheQuestion(Boolean appIsCurrentlyDealingWithTheQuestion) {
        this.appIsCurrentlyDealingWithTheQuestion = appIsCurrentlyDealingWithTheQuestion;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(int remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }
    public String getChosenTTS() {
        return chosenTTS;
    }

    public void setChosenTTS(String chosenTTS) {
        this.chosenTTS = chosenTTS;
    }

    public EncodingRegistry getRegistry() {
        return registry;
    }

    public ChatGptStreamMode getChatGptStreamMode() {
        return chatGptStreamMode;
    }

    public void setChatGptStreamMode(ChatGptStreamMode chatGptStreamMode) {
        this.chatGptStreamMode = chatGptStreamMode;
    }
    public CustomGPTStreamMode getCustomGPTStreamMode() {
        return customGPTStreamMode;
    }

    public void setCustomGPTStreamMode(CustomGPTStreamMode customGPTStreamMode) {
        this.customGPTStreamMode = customGPTStreamMode;
    }
    public Boolean getAppIsListeningToTheQuestion() {
        return appIsListeningToTheQuestion;
    }

    public void setAppIsListeningToTheQuestion(Boolean appIsListeningToTheQuestion) {
        this.appIsListeningToTheQuestion = appIsListeningToTheQuestion;
    }
    public BlueMic getBlueMic() {
        return blueMic;
    }

    public void setBlueMic(BlueMic blueMic) {
        this.blueMic = blueMic;
    }

    public Translator getEnglishLanguageSelectedTranslator() {
        return englishLanguageSelectedTranslator;
    }

    public void setEnglishLanguageSelectedTranslator(Translator englishLanguageSelectedTranslator) {
        this.englishLanguageSelectedTranslator = englishLanguageSelectedTranslator;
    }

    public Translator getLanguageSelectedEnglishTranslator() {
        return languageSelectedEnglishTranslator;
    }

    public void setLanguageSelectedEnglishTranslator(Translator languageSelectedEnglishTranslator) {
        this.languageSelectedEnglishTranslator = languageSelectedEnglishTranslator;
    }

    public Boolean getInitSharedpreferences() {
        return initSharedpreferences;
    }

    public void setInitSharedpreferences(Boolean initSharedpreferences) {
        this.initSharedpreferences = initSharedpreferences;
    }

    public Boolean getUsingReadSpeaker() {
        return usingReadSpeaker;
    }

    public void setUsingReadSpeaker(Boolean usingReadSpeaker) {
        this.usingReadSpeaker = usingReadSpeaker;
    }

    public String getLanguageDetected() {
        return languageDetected;
    }

    public void setLanguageDetected(String languageDetected) {
        this.languageDetected = languageDetected;
    }
    public Translator getFrenchLanguageSelectedTranslator() {
        return frenchLanguageSelectedTranslator;
    }

    public void setFrenchLanguageSelectedTranslator(Translator frenchLanguageSelectedTranslator) {
        this.frenchLanguageSelectedTranslator = frenchLanguageSelectedTranslator;
    }
    public int getListeningDuration() {
        return listeningDuration;
    }

    public void setListeningDuration(int listeningDuration) {
        this.listeningDuration = listeningDuration;
    }

    public int getListeningAttempt() {
        return listeningAttempt;
    }

    public void setListeningAttempt(int listeningAttempt) {
        this.listeningAttempt = listeningAttempt;
    }

    public int getSpeakSpeed() {
        return speakSpeed;
    }

    public void setSpeakSpeed(int speakSpeed) {
        this.speakSpeed = speakSpeed;
    }

    public File getFileupdate() {
        return fileupdate;
    }

    public void setFileupdate(File fileupdate) {
        this.fileupdate = fileupdate;
    }

    public int getSpeakVolume() {
        return speakVolume;
    }

    public void setSpeakVolume(int speakVolume) {
        this.speakVolume = speakVolume;
    }


    public Replica getQuestion() {
        return question;
    }

    public void setQuestion(Replica question) {
        this.question = question;
    }

    public Replica getReponse() {
        return reponse;
    }

    public void setReponse(Replica reponse) {
        this.reponse = reponse;
    }

    public Setting getSetting() {
        return setting;
    }

    public void setSetting(Setting setting) {
        this.setting = setting;
    }

    public Boolean getFileCreate() {
        return fileCreate;
    }

    public void setFileCreate(Boolean fileCreate) {
        this.fileCreate = fileCreate;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public ArrayList<Session> getListSession() {
        return listSession;
    }

    public void setListSession(ArrayList<Session> listSession) {
        this.listSession = listSession;
    }

    public void listSessionClear(){
        listSession.clear();
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(History history) {
        this.history = history;
    }

    public String getSwitchVisibility() {
        return switchVisibility;
    }

    public void setSwitchVisibility(String switchVisibility) {
        this.switchVisibility = switchVisibility;
    }

    public String getSwitchBIDisplay() {
        return switchBIDisplay;
    }

    public void setSwitchBIDisplay(String switchBIDisplay) {
        this.switchBIDisplay = switchBIDisplay;
    }

    public int getCurrentIndexText() {
        return currentIndexText;
    }

    public void setCurrentIndexText(int currentIndexText) {
        this.currentIndexText = currentIndexText;
    }

    public boolean isAllTextPronoucedSuccess() {
        return allTextPronoucedSuccess;
    }

    public void setAllTextPronoucedSuccess(boolean allTextPronoucedSuccess) {
        this.allTextPronoucedSuccess = allTextPronoucedSuccess;
    }

    public boolean isStop_TTS_ReadSpeaker() {
        return Stop_TTS_ReadSpeaker;
    }

    public void setStop_TTS_ReadSpeaker(boolean stop_TTS_ReadSpeaker) {
        Stop_TTS_ReadSpeaker = stop_TTS_ReadSpeaker;
    }

    public boolean isAlreadyCalled() {
        return alreadyCalled;
    }

    public void setAlreadyCalled(boolean alreadyCalled) {
        this.alreadyCalled = alreadyCalled;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public Boolean getStopProcessus() {
        return stopProcessus;
    }

    public void setStopProcessus(Boolean stopProcessus) {
        this.stopProcessus = stopProcessus;
    }

    public Boolean getAlReadyHadSpoke() {
        return alReadyHadSpoke;
    }

    public void setAlReadyHadSpoke(Boolean alReadyHadSpoke) {
        this.alReadyHadSpoke = alReadyHadSpoke;
    }

    public Float getPreviousVolume() {
        return previousVolume;
    }

    public void setPreviousVolume(Float previousVolume) {
        this.previousVolume = previousVolume;
    }

    public String getSwitchEmotion() {
        return switchEmotion;
    }

    public String getSwitchdetectLanguage() {
        return switchdetectLanguage;
    }

    public void setSwitchdetectLanguage(String switchdetectLanguage) {
        this.switchdetectLanguage = switchdetectLanguage;
    }
    public void setSwitchEmotion(String switchEmotion) {
        this.switchEmotion = switchEmotion;
    }

    public String getSwitchModeStream() {
        return switchModeStream;
    }

    public void setSwitchModeStream(String switchModeStream) {
        this.switchModeStream = switchModeStream;
    }
    public String getSwitchCommande() {
        return switchCommande;
    }

    public void setSwitchCommande(String switchCommande) {
        this.switchCommande = switchCommande;
    }

    public Uri getFileup() {
        return fileup;
    }

    public void setFileup(Uri fileup) {
        this.fileup = fileup;
    }

    public Boolean getSpeaking() {
        return isSpeaking;
    }

    public void setSpeaking(Boolean speaking) {
        isSpeaking = speaking;
    }

    public List<IDBObserver> getObservers() {
        return observers;
    }

    public void setObservers(List<IDBObserver> observers) {
        this.observers = observers;
    }

    public Boolean getNotYet() {
        return notYet;
    }

    public void setNotYet(Boolean notYet) {
        this.notYet = notYet;
    }

    public int getTextSizeBullesPX() {
        return textSizeBullesPX;
    }

    public void setTextSizeBullesPX(int textSizeBullesPX) {
        this.textSizeBullesPX = textSizeBullesPX;
    }

    public boolean isActivityClosed() {
        return activityClosed;
    }

    public void setActivityClosed(boolean activityClosed) {
        this.activityClosed = activityClosed;
    }

    public Boolean getStartRecording() {
        return startRecording;
    }

    public void setStartRecording(Boolean startRecording) {
        this.startRecording = startRecording;
    }

    public Boolean getUsingEmotions() {
        return usingEmotions;
    }

    public void setUsingEmotions(Boolean usingEmotions) {
        this.usingEmotions = usingEmotions;
    }

    public ConnectivityManager getCm() {
        return cm;
    }

    public void setCm(ConnectivityManager cm) {
        this.cm = cm;
    }


    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public int getCurrentQuestionNubmer() {
        return currentQuestionNubmer;
    }

    public void setCurrentQuestionNubmer(int currentQuestionNubmer) {
        this.currentQuestionNubmer = currentQuestionNubmer;
    }

    public boolean isAlreadyGetAnswer() {
        return alreadyGetAnswer;
    }

    public void setAlreadyGetAnswer(boolean alreadyGetAnswer) {
        this.alreadyGetAnswer = alreadyGetAnswer;
    }

    public boolean isOpenaialreadySwitchEmotion() {
        return openaialreadySwitchEmotion;
    }

    public void setOpenaialreadySwitchEmotion(boolean openaialreadySwitchEmotion) {
        this.openaialreadySwitchEmotion = openaialreadySwitchEmotion;
    }

    public boolean isTimeoutExpired() {
        return timeoutExpired;
    }

    public void setTimeoutExpired(boolean timeoutExpired) {
        this.timeoutExpired = timeoutExpired;
    }

    public long getQuestionTime() {
        return questionTime;
    }

    public void setQuestionTime(long questionTime) {
        this.questionTime = questionTime;
    }

    public long getGetResponseTime() {
        return getResponseTime;
    }

    public void setGetResponseTime(long getResponseTime) {
        this.getResponseTime = getResponseTime;
    }

    public Boolean getAnswerHasExceededTimeOut() {
        return answerHasExceededTimeOut;
    }

    public void setAnswerHasExceededTimeOut(Boolean answerHasExceededTimeOut) {
        this.answerHasExceededTimeOut = answerHasExceededTimeOut;
    }

    public String getStoredResponse() {
        return storedResponse;
    }

    public void setStoredResponse(String storedResponse) {
        this.storedResponse = storedResponse;
    }

    public Boolean getBuddyFaceisTired() {
        return buddyFaceisTired;
    }

    public void setBuddyFaceisTired(Boolean buddyFaceisTired) {
        this.buddyFaceisTired = buddyFaceisTired;
    }

    public int getBestTextSize() {
        return bestTextSize;
    }

    public void setBestTextSize(int bestTextSize) {
        this.bestTextSize = bestTextSize;
    }


    public Boolean getShouldPlayEmotion() {
        return shouldPlayEmotion;
    }

    public void setShouldPlayEmotion(Boolean shouldPlayEmotion) {
        this.shouldPlayEmotion = shouldPlayEmotion;
    }

    public String getCurrentEmotion() {
        return currentEmotion;
    }

    public void setCurrentEmotion(String currentEmotion) {
        this.currentEmotion = currentEmotion;
    }

    public Boolean getMessageError() {
        return messageError;
    }

    public void setMessageError(Boolean messageError) {
        this.messageError = messageError;
    }

    public Langue getLangue() {
        return langue;
    }

    public void setLangue(Langue langue) {
        this.langue = langue;
    }

    public List<Replica> getListRepGlobale() {
        return listRepGlobale;
    }

    public void setListRepGlobale(List<Replica> listRepGlobale) {
        this.listRepGlobale = listRepGlobale;
    }

    public TextToSpeech getTts_android() {
        return tts_android;
    }

    public void setTts_android(TextToSpeech tts_android) {
        this.tts_android = tts_android;
    }
    public TtsGoogleC getGoogleCloudTTS() {
        return googleCloudTTS;
    }

    public void setGoogleCloudTTS(TtsGoogleC googleCloudTTS) {
        this.googleCloudTTS = googleCloudTTS;
    }

    public VoicesList getVoiceList() {
        return voiceList;
    }

    public void setVoiceList(VoicesList voiceList) {
        this.voiceList = voiceList;
    }

    public String getImeiDevice() {
        return imeiDevice;
    }

    public String getTokenHealysa() {
        return tokenHealysa;
    }

    public void setTokenHealysa(String tokenHealysa) {
        this.tokenHealysa = tokenHealysa;
    }

    public void setImeiDevice(String imeiDevice) {
        this.imeiDevice = imeiDevice;
    }

    public boolean isModeContinuousListeningON() {
        return modeContinuousListeningON;
    }

    public void setModeContinuousListeningON(boolean modeContinuousListeningON) {
        this.modeContinuousListeningON = modeContinuousListeningON;
    }

    public boolean isMultiCommandsDetected() {
        return multiCommandsDetected;
    }

    public void setMultiCommandsDetected(boolean multiCommandsDetected) {
        this.multiCommandsDetected = multiCommandsDetected;
    }

    public boolean isTimeToExecuteNextCommande() {
        return timeToExecuteNextCommande;
    }

    public void setTimeToExecuteNextCommande(boolean timeToExecuteNextCommande) {
        this.timeToExecuteNextCommande = timeToExecuteNextCommande;
    }

    public List<String> getListOfCommandmustToHavePlayed() {
        return listOfCommandmustToHavePlayed;
    }

    public void setListOfCommandmustToHavePlayed(List<String> listOfCommandmustToHavePlayed) {
        this.listOfCommandmustToHavePlayed = listOfCommandmustToHavePlayed;
    }

    public static final HashMap<String, Locale> supportedLocales = new HashMap<>();

    /**
     * initialisations
     */
    @Override
    public void onCreate() {
        super.onCreate();
        AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver( new Handler() ,getApplicationContext() );
        getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                mSettingsContentObserver );


        speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(getApplicationContext());

        //create a new EncodingRegistry to use JTokkit
        try{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    registry = Encodings.newDefaultEncodingRegistry();
                }
            }).start();
        }
        catch (Exception e) {
            Log.e("MODE_STREAM_USAGE", "Exception when creating a new EncodingRegistry to use JTokkit : "+e);
            e.printStackTrace();
        }
    }

    public void init() {
        Log.e("MRAA","init");

        String number_attempt = getParamFromFile("Number_listens",configurationFilePseudo);
        if(number_attempt.equals("")||Integer.parseInt(number_attempt)<=0){
            remainingAttempts= Integer.parseInt("1");
        }
        else{
            remainingAttempts= Integer.parseInt(number_attempt)+1;
        }

        if (getparam("firstLaunch").equals("")){
            setparam("firstLaunch","true");
        }
        //initialisation du mail du destinataire
        if (getparam("Mail_Destination").equals("")){
            setparam("Mail_Destination",getParamFromFile("Mail_Destination",configurationFilePseudo));
        }
        initLanguageSetting();
        initListeningSettings();
        initProjectID();
        initChatGPTSettings();
        initSpeakVolumeSetting();
        initVisibilitySetting();
        initSTTSetting();
        initTTSSetting();
        initOpenAiSettings();
        initEmotionSetting();
        initLanguageDetectionSetting();
        initChatTextSize();
        initModeStreamSetting();
        initCommandeSetting();
        initBIDisplay();
        initTracking();
        listQRTypes = getDisponibleScaningSystem();
        Log.e("MRAA","init google api");

        Log.e("MRAA","init google api out if");
        if (getparam("STT_chosen").equalsIgnoreCase("Google")){
            Log.e("MRAA","init google api inside if");
            alreadyCalled = true;
            releaseGoogleAPI();
            initGoogleAPI();
        }

        notifyObservers("properties file done");

    }


    public void initAlert(){
        isAlertActivated = getParamFromFile("ALERT_ACTIVITY", "TeamChatBuddy.properties");
        if (isAlertActivated == null)isAlertActivated="";
        String tool = getParamFromFile("ALERT_TOOL", "TeamChatBuddy.properties").trim();
        if(isAlertActivated.trim().equalsIgnoreCase("Yes") && !tool.isEmpty()){
            String phoneNumber = getParamFromFile("ALERT_SMS", "TeamChatBuddy.properties");
            String mailTo = getParamFromFile("ALERT_MAIL", "TeamChatBuddy.properties").trim();

            if ((tool.equalsIgnoreCase("SMS") && phoneNumber.isEmpty()) && (tool.equalsIgnoreCase("MAIL") && mailTo.isEmpty())) {
                Log.i("AlertManager_App", "ALERT_SMS ou ALERT_MAIL est vide. Aucune alerte ne sera envoyée.");
                getEnglishLanguageSelectedTranslator().translate("Inactivity alerts via email/sms will not be sent.").addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        isAlertActivated = "No";
                        notifyObservers("STOP_ALERT");
                        Toast.makeText(getApplicationContext(), translatedText, Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("AlertManager_App", "translatedText exception  " + e);
                    }
                });
            }
            else if (tool.equalsIgnoreCase("SMS") && phoneNumber.isEmpty()) {
                Log.i("AlertManager_App", "ALERT_SMS est vide. Aucune alerte ne sera envoyée.");
                getEnglishLanguageSelectedTranslator().translate("Inactivity alerts via sms will not be sent.").addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        isAlertActivated = "No";
                        notifyObservers("STOP_ALERT");
                        Toast.makeText(getApplicationContext(), translatedText, Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("AlertManager_App", "translatedText exception  " + e);
                    }
                });

            }
            else if (tool.equalsIgnoreCase("MAIL") && mailTo.isEmpty()) {
                Log.i("AlertManager_App", "ALERT_MAIL est vide. Aucune alerte ne sera envoyée.");
                getEnglishLanguageSelectedTranslator().translate("Inactivity alerts via email will not be sent.").addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        isAlertActivated = "No";
                        notifyObservers("STOP_ALERT");
                        Toast.makeText(getApplicationContext(), translatedText, Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("AlertManager_App", "translatedText exception  " + e);
                    }
                });
            }
        }
    }

    private void initOpenAiSettings() {
        double totalConsumption = 0;
        String totalConsumptionSaved = getparam("Total_cons");
        if(totalConsumptionSaved != null && !totalConsumptionSaved.isEmpty()){
            totalConsumption = Double.parseDouble(totalConsumptionSaved);
        }
        setparam("Total_cons",totalConsumption+"");
    }


    private void initListeningSettings() {
        String listening_duration =getParamFromFile("Listening_time",configurationFilePseudo);
        String listening_attempt = getParamFromFile("Number_listens",configurationFilePseudo);
        if(listening_duration.equals("")||Integer.parseInt(listening_duration)<=0){
            listening_duration = "10";
        }
        if(listening_attempt.equals("")||Integer.parseInt(listening_attempt)<=0){
            listening_attempt = "1";
        }
        listeningDuration = Integer.parseInt(listening_duration);
        listeningAttempt = Integer.parseInt(listening_attempt) + 1;
        remainingAttempts= listeningAttempt;
    }

    private void initProjectID(){
        if (getparam("CustomGPT_Project_ID").equals("")) {
            setparam("CustomGPT_Project_ID", getParamFromFile("CustomGPT_Project_ID",configurationFilePseudo));
        }
    }
    private void initChatGPTSettings() {
        // ChatGPT settings


        if (getparam("firstLaunch").equals("true")) {
            setparam("messages", "[]");
            setparam("commandes", "[]");
            if (getparam(openAIKey).equals("")) {
                setparam(openAIKey, getParamFromFile(openAIKey, configurationFilePseudo));
            }
            if (getparam("CustomGPT_API_Key").equals("")){
                setparam("CustomGPT_API_Key", getParamFromFile("CustomGPT_API_Key", configurationFilePseudo));
            }
            if (getparam("header").equals("")) {
                setparam("header", getParamFromFile(header, configurationFilePseudo));
            }
            Log.e("TEST","init HeaDER = "+getparam(header));
            if (getparam("entete").equals("")) {
                setparam("entete", getParamFromFile(entete, configurationFilePseudo));
            }
            if (getparam("CustomGPT_header_en").equals("")) {
                setparam("CustomGPT_header_en", getParamFromFile("CustomGPT_header_en", configurationFilePseudo));
            }
            if (getparam("CustomGPT_header_fr").equals("")) {
                setparam("CustomGPT_header_fr", getParamFromFile("CustomGPT_header_fr", configurationFilePseudo));
            }
            setparam("firstLaunch","false");
        }
        setparam("model", getParamFromFile("Model", configurationFilePseudo));
        setparam("Temperature_chatgpt", getParamFromFile("Temperature", configurationFilePseudo));
        setparam("Max_Tokens_req", getParamFromFile("Max_Tokens_req", configurationFilePseudo));
        setparam("Max_Tokens_resp", getParamFromFile("Max_Tokens_resp", configurationFilePseudo));

        if (getparam("nb_max_phrases").equals("")) {
            setparam("nb_max_phrases", "0");
        }

        // ChatGPT
        setparam("ChatGPT_url", getParamFromFile("ChatGPT_url", configurationFilePseudo));
        setparam("ChatGPT_api", getParamFromFile("ChatGPT_ApiEndpoint", configurationFilePseudo));

        String can_change_chatBot = getParamFromFile("Change_chatBot", "TeamChatBuddy.properties");
        if (getparam("chatbot_chosen").equals("") || (can_change_chatBot == null || can_change_chatBot.trim().equalsIgnoreCase("No")) ){
            setparam("chatbot_chosen",getParamFromFile("ChatBot",configurationFilePseudo));
        }
    }

    private void initChatTextSize () {
        int textSize = Integer.parseInt(getParamFromFile("Chat_TextSize",configurationFilePseudo));
        if (textSize<20 || textSize>50){
            setTextSizeBullesPX(25);
        }
        else setTextSizeBullesPX(textSize);


    }

    private void initSpeakVolumeSetting() {
        max = getMaxVolume();
        if (getparam(speakVolumePseudo).equals("")) {
            int defaultVolume=Integer.parseInt(getParamFromFile("Speech_volume",configurationFilePseudo));
            if (defaultVolume<0 || defaultVolume >100){
                speakVolume = getVolume();
                defaultVolume = getClosestInt((double) (speakVolume * 100) / max);
            }

            setparam(speakVolumePseudo, String.valueOf(defaultVolume));
        }
        speakVolume = Integer.parseInt(getparam(speakVolumePseudo));
    }

    private void initVisibilitySetting() {
        if (getparam(visibilityString).equals("")) {
            setparam(visibilityString, getParamFromFile("Display_of_speech",configurationFilePseudo).toLowerCase());
        }
        switchVisibility = getparam(visibilityString);
    }

    private void initBIDisplay(){
        if (getparam("Stimulis").equals("")) {
            setparam("Stimulis", getParamFromFile("Stimulis",configurationFilePseudo).toLowerCase());
        }
        switchBIDisplay = getparam("Stimulis");
    }

    private void initSTTSetting(){
        String can_change_stt = getParamFromFile("Change_STT", "TeamChatBuddy.properties");
        if (getparam("STT_chosen").equals("") || (can_change_stt == null || can_change_stt.trim().equalsIgnoreCase("No")) ){
            Log.e("MRAA","initSTTSetting inside if");
            if(getParamFromFile("Speech_To_Text","TeamChatBuddy.properties").trim().equalsIgnoreCase("SpeechRecognizer")){
                setparam("STT_chosen","Android");
            }
            else  if(getParamFromFile("Speech_To_Text","TeamChatBuddy.properties").trim().equalsIgnoreCase("ApiGoogle")){
                setparam("STT_chosen","Google");
            }
            else if (getParamFromFile("Speech_To_Text","TeamChatBuddy.properties").trim().equalsIgnoreCase("Whisper")){
                setparam("STT_chosen","Whisper");
            }
            else if (getParamFromFile("Speech_To_Text","TeamChatBuddy.properties").trim().equalsIgnoreCase("Cerence")){
                setparam("STT_chosen","Cerence");
            }
            else {

                String[] listSTT= getParamFromFile("Speech_To_Text_List","TeamChatBuddy.properties").split("/");
                if (listSTT.length>0){
                    if (listSTT[0].trim().equalsIgnoreCase("SpeechRecognizer")){
                        setparam("STT_chosen","Android");
                    }
                    else if (listSTT[0].trim().equalsIgnoreCase("ApiGoogle")){
                        setparam("STT_chosen","Google");
                    }
                    else if (listSTT[0].trim().equalsIgnoreCase("Whisper")){
                        setparam("STT_chosen","Whisper");
                    }
                    else if (listSTT[0].trim().equalsIgnoreCase("Cerence")){
                        setparam("STT_chosen","Cerence");
                    }
                    else setparam("STT_chosen","Android");
                }
                else setparam("STT_chosen","Android");


                if (getLangue().getNom().equals("Anglais")){
                    showToast("Chosen STT is not found. Buddy will use "+getparam("STT_chosen"));
                }
                else if (getLangue().getNom().equals("Français")) {
                    showToast("Le STT choisi est introuvable. Buddy utilisera "+getparam("STT_chosen"));
                }
                else if (getLangue().getNom().equals("Espagnol")) {
                    showToast("No se encuentra el STT elegido. Buddy usará "+getparam("STT_chosen"));
                }
                else if (getLangue().getNom().equals("Allemand")){
                    showToast("Ausgewählte STT wurde nicht gefunden. Buddy wird "+getparam("STT_chosen")+" verwenden");
                }
                else{
                    showToast("Chosen STT is not found. Buddy will use "+getparam("STT_chosen"));
                }

            }


        }
    }

    private void initTTSSetting(){
        Log.e("MRAA","initTTSSetting");
        String[] listTTS= getParamFromFile("Text_To_Speech_List",configurationFilePseudo).split("/");
        if (listTTS.length>0) {
            if (listTTS[0].trim().equalsIgnoreCase("ReadSpeaker")) {
                chosenTTS = "ReadSpeaker";
            }
            else if (listTTS[0].trim().equalsIgnoreCase("Android")) {
                chosenTTS = "Android";
            }
            else if (listTTS[0].trim().equalsIgnoreCase("ApiGoogle")) {
                chosenTTS = "ApiGoogle";
            }
            else if (listTTS[0].trim().equalsIgnoreCase("OpenAI")) {
                chosenTTS = "OpenAI";
            }
            else {
                chosenTTS = "ReadSpeaker";

                if (getLangue().getNom().equals("Anglais")){
                    showToast("Chosen TTS is not found. Buddy will use "+chosenTTS);
                }
                else if (getLangue().getNom().equals("Français")) {
                    showToast("Le TTS choisi est introuvable. Buddy utilisera "+chosenTTS);
                }
                else if (getLangue().getNom().equals("Espagnol")) {
                    showToast("No se encuentra el TTS elegido. Buddy usará "+chosenTTS);
                }
                else if (getLangue().getNom().equals("Allemand")){
                    showToast("Ausgewählte TTS wurde nicht gefunden. Buddy wird "+chosenTTS+" verwenden");
                }
                else{
                    showToast("Chosen TTS is not found. Buddy will use "+chosenTTS);
                }

            }
        }
        else {
            chosenTTS = "ReadSpeaker";
            if (getLangue().getNom().equals("Anglais")){
                showToast("Chosen TTS is not found. Buddy will use "+chosenTTS);
            }
            else if (getLangue().getNom().equals("Français")) {
                showToast("Le TTS choisi est introuvable. Buddy utilisera "+chosenTTS);
            }
            else if (getLangue().getNom().equals("Espagnol")) {
                showToast("No se encuentra el TTS elegido. Buddy usará "+chosenTTS);
            }
            else if (getLangue().getNom().equals("Allemand")){
                showToast("Ausgewählte TTS wurde nicht gefunden. Buddy wird "+chosenTTS+" verwenden");
            }
            else{
                showToast("Chosen TTS is not found. Buddy will use "+chosenTTS);
            }
        }
    }

    private void initLanguageSetting() {

        List<String> langueDisponible = getDisponibleLangue();
        List<Langue> langues = new ArrayList<>();
        for (int i=0;i<langueDisponible.size();i++){

            if (getparam(langueDisponible.get(i)).equals("")) {

                Boolean isChosen;
                if(getParamFromFile(langueInconfigurationFilePseudo,configurationFilePseudo).trim().equalsIgnoreCase(langueDisponible.get(i).trim())){
                    isChosen = true;
                }
                else{
                    isChosen = false;
                }
                Langue langue_utili = new Langue(i+1, langueDisponible.get(i), isChosen);
                Gson json_langue_utili = new Gson();
                String jsonString_langue_utili = json_langue_utili.toJson(langue_utili);
                setparam(langueDisponible.get(i), jsonString_langue_utili);
            }
            else {
                Langue langueTemp =new Gson().fromJson(getparam(langueDisponible.get(i)), Langue.class);
                langueTemp.setId(i+1);
                setparam(langueDisponible.get(i),new Gson().toJson(langueTemp));
            }
            langues.add(new Gson().fromJson(getparam(langueDisponible.get(i)), Langue.class));

        }
        if (langues.isEmpty()){
            Langue langue_francais = new Langue(1, "Français", true);
            Gson json_langue_francais = new Gson();
            String jsonString_langue_francais = json_langue_francais.toJson(langue_francais);
            setparam(french, jsonString_langue_francais);
            langues.add(new Gson().fromJson(getparam(french), Langue.class));
        }
        int iterationCount = 0;
        for (Langue language : langues) {
            iterationCount++;
            if (language.isChosen()) {
                this.langue = language;
                setLangue(language);
                break;
            }
            if (iterationCount==langueDisponible.size()){
                this.langue = language;
                setLangue(language);
            }
        }
    }
    public List<String> getDisponibleLangue(){
        StringTokenizer st = new StringTokenizer(getParamFromFile("Languages_available",configurationFilePseudo), "/", false);
        List<String> list = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String result = st.nextToken();
            list.add(result.trim());
        }
        return list;

    }
    public List<String> getLanguageCodeForDisponibleLangue(String ServiceLangugeCode){
        StringTokenizer st = new StringTokenizer(getParamFromFile(ServiceLangugeCode,configurationFilePseudo), "/", false);
        List<String> list = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String result = st.nextToken();
            list.add(result.trim());
        }
        return list;

    }
    public void downloadModel(IMLKitDownloadCallback imlKitDownloadCallback, String langue){

        options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(langue) // Remplacez par la langue choisie par l'utilisateur
                .build();
        englishLanguageSelectedTranslator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();
        englishLanguageSelectedTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        setEnglishLanguageSelectedTranslator(englishLanguageSelectedTranslator);
                        imlKitDownloadCallback.onDownloadEnd(true,"english");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imlKitDownloadCallback.onDownloadEnd(false,"english");
                    }
                });
        options1 = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.FRENCH)
                .setTargetLanguage(langue) // Remplacez par la langue choisie par l'utilisateur
                .build();
        frenchLanguageSelectedTranslator = Translation.getClient(options1);
        DownloadConditions conditions1 = new DownloadConditions.Builder()
                .build();
        frenchLanguageSelectedTranslator.downloadModelIfNeeded(conditions1)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        setFrenchLanguageSelectedTranslator(frenchLanguageSelectedTranslator);
                        imlKitDownloadCallback.onDownloadEnd(true,"french");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imlKitDownloadCallback.onDownloadEnd(false,"french");
                    }
                });
        options = new TranslatorOptions.Builder()
                .setSourceLanguage(langue)
                .setTargetLanguage(TranslateLanguage.ENGLISH) // Remplacez par la langue choisie par l'utilisateur
                .build();
        languageSelectedEnglishTranslator = Translation.getClient(options);
        DownloadConditions conditions2 = new DownloadConditions.Builder()
                .build();
        languageSelectedEnglishTranslator.downloadModelIfNeeded(conditions2)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        setLanguageSelectedEnglishTranslator(languageSelectedEnglishTranslator);
                        imlKitDownloadCallback.onDownloadEnd(true,"languageToEnglish");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imlKitDownloadCallback.onDownloadEnd(false,"languageToEnglish");
                    }
                });

    }



    private void initEmotionSetting() {
        if (getparam(emotionString).equals("")) {
            setparam(emotionString, getParamFromFile("Activation_of_emotions",configurationFilePseudo).toLowerCase());
        }
        switchEmotion = getparam(emotionString);
    }

    private void initLanguageDetectionSetting() {
        if (getparam(detectionLanguageString).equals("")) {
            setparam(detectionLanguageString, getParamFromFile("Language_detection",configurationFilePseudo).toLowerCase());
        }
        switchdetectLanguage = getparam(detectionLanguageString);
    }

    private void initModeStreamSetting() {
        if (getparam(modeStreamString).equals("")) {
            setparam(modeStreamString, getParamFromFile("Stream_mode",configurationFilePseudo).toLowerCase());
        }
        switchModeStream = getparam(modeStreamString);
    }

    private void initCommandeSetting() {
        if (getparam(commandeString).equals("")) {
            setparam(commandeString, getParamFromFile(commandeString,configurationFilePseudo).toLowerCase());
        }
        setparam("COMMAND_Model", getParamFromFile("COMMAND_Model", configurationFilePseudo));
        setparam("COMMAND_Temperature", getParamFromFile("COMMAND_Temperature", configurationFilePseudo));
        switchCommande = getparam(commandeString);
    }

    private void initTracking(){

        //Tracking activation
        if (getparam("Tracking_Activation").equals("")) {
            setparam("Tracking_Activation", getParamFromFile("Tracking",configurationFilePseudo).toLowerCase());
        }


        //Tracking Timeout
        if (getparam("trackingTimeout").equals("")) {
            setparam("trackingTimeout", getParamFromFile("TRACKING_timeout",configurationFilePseudo).toLowerCase());
        }

        //Tracking camera display
        if (getparam("Tracking_Camera_Display").equals("")) {
            if (getParamFromFile("TRACKING_Camera",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                setparam("Tracking_Camera_Display", "false");
            }
            else {
                setparam("Tracking_Camera_Display", "true");
            }
        }

        //Tracking head
        if (getparam("Tracking_Head").equals("")) {
            if (getParamFromFile("TRACKING_Head",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                setparam("Tracking_Head", "false");
            }
            else {
                setparam("Tracking_Head", "true");
            }
        }

        //Tracking body
        if (getparam("Tracking_Body").equals("")) {
            if (getParamFromFile("TRACKING_Body",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                setparam("Tracking_Body", "false");
            }
            else {
                setparam("Tracking_Body", "true");
            }
        }

        //Tracking auto listen
        if (getparam("Tracking_Auto_Listen").equals("")) {
            if (getParamFromFile("TRACKING_listening",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                setparam("Tracking_Auto_Listen", "false");
            }
            else {
                setparam("Tracking_Auto_Listen", "true");
            }
        }

        //Tracking invitation
        if (getparam("Tracking_Invitation").equals("")) {
            if (getParamFromFile("WELCOME_tracking",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                setparam("Tracking_Invitation", "false");
            }
            else {
                setparam("Tracking_Invitation", "true");
            }
        }

        //Tracking invitation chatGpt
        if (getparam("Tracking_Invitation_ChatGpt").equals("")) {
            if (getParamFromFile("WELCOME_CHATGPT",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                setparam("Tracking_Invitation_ChatGpt", "false");
            }
            else {
                setparam("Tracking_Invitation_ChatGpt", "true");
            }
        }
        //Tracking Timeout
        if (!getparam("TRACKING_timeout_Switch").equals("")) {
            if (getParamFromFile("TRACKING_timeout_Switch",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                setparam("TRACKING_timeout_Switch", "false");
            }
            else {
                setparam("TRACKING_timeout_Switch", "true");
            }
        }
        else setparam("TRACKING_timeout_Switch", "false");
    }

    public List<String> separator(String hotword) {
        StringTokenizer st = new StringTokenizer(hotword, "/", false);
        List<String> list = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String result = st.nextToken();
            list.add(result.trim());
        }
        return list;
    }

    public boolean isStringEmptyOrNoWords(String input) {
        if (input == null || input.trim().isEmpty()) {
            // Si la chaîne est nulle ou uniquement composée d'espaces
            return true;
        }

        // Supprimer les espaces autour de la chaîne
        String trimmedInput = input.trim();

        // Vérifier si la chaîne contient un mot entre < >
        boolean hasWordInBrackets = trimmedInput.matches(".*<\\s*\\S+\\s*>.*");

        // Vérifier si la chaîne contient des mots en dehors des balises < >
        boolean hasTextOutsideBrackets = trimmedInput.matches(".*\\S.*") && !trimmedInput.matches("<\\s*>");

        // Retourner true si aucun mot entre < > et aucun texte en dehors
        return !(hasWordInBrackets || hasTextOutsideBrackets);
    }
    public String applyFilters(String responseFilter, String inputPhrase) {
        // Expression régulière pour extraire tous les filtres sous la forme [avant / après]
        String outputPhrase = "";

        if (responseFilter.contains("]")){
            String[] filters = responseFilter.trim().replaceAll("\\] +\\[", "][").split("]");
            for (int i=0;i<filters.length;i++){
                Log.e(TAG,"filter = "+filters[i].replace("[", ""));
                if (filters[i].contains("/")) {
                    if (outputPhrase.equalsIgnoreCase("")) {
                        String[] parts = filters[i].split("/");
                        String before = parts[0].replace("[", "");
                        String after = parts.length > 1 ? parts[1] : "";
                        outputPhrase = inputPhrase.replace(before, after);
                    } else {
                        String[] parts = filters[i].split("/");
                        String before = parts[0].replace("[", "");
                        String after = parts.length > 1 ? parts[1] : "";
                        outputPhrase = outputPhrase.replace(before, after);
                    }
                }
                Log.e(TAG,"outPutphrase = "+outputPhrase);
            }
            if(outputPhrase.equalsIgnoreCase("")){
                inputPhrase=inputPhrase.replace("\\","");
                Log.e("testTTS","AFter .inputPhrase = "+inputPhrase);
                return inputPhrase;
            }else {
                outputPhrase=outputPhrase.replace("\\","");
                Log.e("testTTS","AFter .outPutphrase = "+outputPhrase);
                return outputPhrase;
            }

        }
        else {
            inputPhrase=inputPhrase.replace("\\","");
            Log.e("testTTS","AFter .inputPhrase = "+inputPhrase);
            return inputPhrase;
        }

    }
    //#region ******************************************************* STT **********************************************************************

    //#region ******************************************************* blue mic *******************************************************


    public void startListeningBlueMic(boolean isHotword, Activity activity){
        List<String> langueCode = getLanguageCodeForDisponibleLangue("Language_Code_Used_In_STT_Android");
        String language_stt = langueCode.get(getLangue().getId()-1).replace("-","_");
        if(isHotword){
            Log.i(TAG_BLUEMIC_STREAMING,"startStreamingBlueMic(hotword)");
            try {
                getBlueMic().getmBlueMicService().startStreaming(
                        getBlueMic().selectedBlueMic,
                        true,
                        language_stt,
                        getParamFromFile("ApiGoogle_Key",configurationFilePseudo),
                        new IBlueMicAudioDataListener.Stub() {
                            @Override
                            public void onNewAudioData(String[] audioSample) throws RemoteException {}

                            @Override
                            public void onNewStateVAD(String stateVAD) throws RemoteException {
                                Log.w(TAG_BLUEMIC_STREAMING,"onNewStateVAD() : "+stateVAD);
                            }

                            @Override
                            public void onSTTResponse(String text) throws RemoteException {
                                Log.i(TAG_BLUEMIC_STREAMING,"onSTTResponse() : "+text);
                                if(text != null && !text.isEmpty()){
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            checkTheHotword(text,"blueMic");
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onSTTError(int type) throws RemoteException {
                                Log.e(TAG_BLUEMIC_STREAMING,"onSTTError() : "+type);
                            }
                        }
                );
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        else{
            Log.i(TAG_BLUEMIC_STREAMING,"startStreamingBlueMic(speech)");
            try {
                if (!isModeContinuousListeningON()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listeningAnimation();
                        }
                    });
                    alreadyGetAnswer = false;
                    questionNumber++;
                    currentEmotion = "";
                    shouldPlayEmotion = false;
                }
                stopListening(activity);

                getBlueMic().getmBlueMicService().startStreaming(
                        getBlueMic().selectedBlueMic,
                        true,
                        language_stt,
                        getParamFromFile("ApiGoogle_Key",configurationFilePseudo),
                        new IBlueMicAudioDataListener.Stub() {
                            @Override
                            public void onNewAudioData(String[] audioSample) throws RemoteException {}

                            @Override
                            public void onNewStateVAD(String stateVAD) throws RemoteException {
                                Log.w(TAG_BLUEMIC_STREAMING,"onNewStateVAD() : "+stateVAD);
                            }

                            @Override
                            public void onSTTResponse(String text) throws RemoteException {
                                Log.i(TAG_BLUEMIC_STREAMING,"onSTTResponse() : "+text);
                                if(text != null && !text.isEmpty()){
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifyObservers("STTQuestion_success;"+text);
                                            try {
                                                BuddySDK.UI.stopListenAnimation();
                                            }
                                            catch (Exception e){
                                                Log.e(TAG,"BuddySDK Exception  "+e);
                                            }
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onSTTError(int type) throws RemoteException {
                                Log.e(TAG_BLUEMIC_STREAMING,"onSTTError() : "+type);
                            }
                        }
                );
                notifyObservers("listen");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopListeningBlueMic(){
        if(getBlueMic() != null && getBlueMic().getmBlueMicService() != null){
            Log.i(TAG_BLUEMIC_STREAMING,"stopStreamingBlueMic()");
            try {
                getBlueMic().getmBlueMicService().stopStreaming();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    //#endregion ******************************************************* blue mic *******************************************************

    //#region ******************************************************* STT Cerence Local fcf **********************************************************************



    /**
     * Cette fonction permet de lancer l'écoute STT Cerence local .fcf OU BIEN l'écoute de OK BUDDY
     * @return : l'objet STTTask ou null si erreur
     */
    public void startListeningHotwor(Activity activity) {
        Log.e(TAG,"startListeningHotword invoked");
        Log.i("MYA_fragment", "startListeningHotword invoked");
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            Log.e("MYA_YAKINE","listeningState = "+listeningState+"\nstartListeningHotwor ------ Delay");
//                }, 2000);
        notifyObservers("showCameraQr");
        listeningState = "hotword";
        isListeningHotw = true;
        Log.i("OpenAITTS", "------------------------ startListeningHotword isListeningHotw = true ");
        hotwordAlreadyHandled = false;
        Log.e("MYA_YAKINE","listeningState = "+listeningState+"\nstartListeningHotwor");
        Log.e("MYA_QR_H_","startListeningHotwor fct");
        getTranslateHotwordList();
        neutralAnimation();
        isSpeaking = false;
        currentEmotion="";
        shouldPlayEmotion=false;

        stopListening(activity);
        speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(activity);

        setAlreadyChatting(false);

        if (getCurrentLanguage().equals("en")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_en);
        }
        else if (getCurrentLanguage().equals("fr")){
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_fr);
        }
        else if (getCurrentLanguage().equals("de")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_de);
        }
        else if (getCurrentLanguage().equals("es")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_es);
        }
        else{
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_stt_android_indispo_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            toast_stt_android_indispo = translatedText;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_en);
                        }
                    });
        }
        activity.runOnUiThread(() -> {

            //USE BLUE MIC
            if(getBlueMic() != null
                    && getBlueMic().getmBlueMicService() != null
                    && getBlueMic().selectedBlueMic != null
                    && getBlueMic().selectedBlueMic.getState().equals("Connected")
            ){
                startListeningBlueMic(true, activity);
            }

            //DO NOT USE BLUE MIC
            else{

                try {
                    setLed("listening");
                    speechRecognizer.startListening(speechRecognizerIntent2);
                    if(!isAppInstalled(getApplicationContext(),"com.google.android.googlequicksearchbox")) {
                        showToast(toast_stt_android_indispo);
                    }
                    speechRecognizer.setRecognitionListener(new RecognitionListener() {
                        @Override
                        public void onReadyForSpeech(Bundle bundle) {
                            Log.e(TAG, "onReadyForSpeech");
                        }

                        @Override
                        public void onBeginningOfSpeech() {
                            stopTTS(false);
                            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                            Log.e(TAG, "onBeginningOfSpeech");
                            Log.i("MYA_fragment", "startListeningHotword onBeginningOfSpeech");
                        }

                        @Override
                        public void onRmsChanged(float v) {
                            //Log.e(TAG, "onRmsChanged");

                        }

                        @Override
                        public void onBufferReceived(byte[] bytes) {
                            Log.e(TAG, "Hotword onBufferReceived listening  : ");
                        }

                        @Override
                        public void onEndOfSpeech() {
                            Log.e(TAG, "Hotword onEndOfSpeech listening  : ");
                        }

                        @Override
                        public void onError(int i) {
                            switch (i) {
                                case SpeechRecognizer.ERROR_AUDIO:
                                    Log.d(TAG, "Audio recording error");
                                    logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_AUDIO","Audio recording error");
                                    break;
                                case SpeechRecognizer.ERROR_CLIENT:
                                    Log.d(TAG, "Client side error");
                                    logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_CLIENT","Client side error");
                                    break;
                                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                    Log.d(TAG, "Insufficient permissions");
                                    logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS","Insufficient permissions");
                                    break;
                                case SpeechRecognizer.ERROR_NETWORK:
                                    Log.d(TAG, "Network error");
                                    logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_NETWORK","Network error");
                                    break;
                                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                                    Log.d(TAG, "Network timeout");
                                    logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_NETWORK_TIMEOUT","Network timeout");
                                    break;
                                case SpeechRecognizer.ERROR_NO_MATCH:
                                    Log.d(TAG, "No match");
                                    break;
                                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                                    Log.d(TAG, "RecognitionService busy");
                                    logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_RECOGNIZER_BUSY","RecognitionService busy");
                                    break;
                                case SpeechRecognizer.ERROR_SERVER:
                                    Log.d(TAG, "Server error");
                                    logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_SERVER","Server error");
                                    break;
                                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                                    Log.d(TAG, "No speech input");
                                    logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_SPEECH_TIMEOUT","No speech input");
                                    break;
                                case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                                    Log.e(TAG, "ERROR_LANGUAGE_NOT_SUPPORTED");
                                    break;
                                default:
                                    Log.d(TAG, "Unknown error");
                                    logErrorSTTAndroid(i,"Unknown error","Unknown error");
                                    break;
                            }
                            setLed("listening");
                            speechRecognizer.startListening(speechRecognizerIntent2);
                        }

                        @Override
                        public void onResults(Bundle bundle) {
                            ArrayList<String> data = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                            if (data!=null && data.size()>0) {
                                Log.e(TAG, "Hotword result  : " + data.get(0));
                                Log.i("MYA_fragment", "startListeningHotword onResults");
                                checkTheHotword(data.get(0),"listening");
                            }
                            else {
                                Log.e(TAG, "Hotword result  size = 0 : " );
                                setLed("listening");
                                speechRecognizer.startListening(speechRecognizerIntent2);
                            }
                        }

                        @Override
                        public void onPartialResults(Bundle bundle) {
                            Log.e(TAG, "Hotword onPartialResults listening  : ");
                            ArrayList<String> data = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                            if (data!=null && data.size()>0) {
                                Log.e(TAG, "Hotword result onPartialResults  : " + data.get(0));
                                checkTheHotword(data.get(0),"listening");
                            }
                            else {
                                Log.e(TAG, "Hotword result onPartialResults size = 0 : " );
                            }
                        }

                        @Override
                        public void onEvent(int i, Bundle bundle) {
                            Log.e(TAG, "Hotword onEvent listening  : ");
                        }

                    });
                } catch (Exception e) {
                    Log.e(TAG, "Runnable : Erreur pendant la vérification [isReadyToListen] - Hotword : " + e);
                }


            }

        });

    }


    //#region ******************************************************* STT Free Speech **********************************************************************



    /**
     * Cette fonction permet de lancer l'écoute STT Free Speech
     * @return : l'objet STTTask ou null si erreur
     */

    private void initializeSTT(STTTask sttTask) {
        try{
            sttTask.initialize();
        }
        catch (Exception e) {
            Log.e("MRRR","Runnable : Erreur pendant l'initialisation du STT Task : "+e);
        }
    }
    public STTTask startListeningCerence(Activity activity){
        notifyObservers("showCameraQr");
        Log.e("MRRR","startListeningFreeSpeechStt fonction start");
        if (!isModeContinuousListeningON()) {
            alreadyGetAnswer = false;
            questionNumber++;
            currentEmotion = "";
            shouldPlayEmotion = false;
        }
        stopListening(activity);

        try {
            if (getParamFromFile("Language_Specification_STT",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                freeSpeechSttTask=BuddySDK.Speech.createCerenceFreeSpeechTask();
            }
            else {
                if(getCurrentLanguage().equals("en")){
                    Log.e("MRRR","init ENfreeSpeechSttTask");
                    freeSpeechSttTask=BuddySDK.Speech.createCerenceFreeSpeechTask(Locale.ENGLISH);
                }
                else {
                    freeSpeechSttTask=BuddySDK.Speech.createCerenceFreeSpeechTask(Locale.FRENCH);
                }
            }

        }
        catch (Exception e){
            Log.e(TAG,"Exception lors de la création de cerence "+e);
        }


        if(freeSpeechSttTask == null) return null;

        initializeSTT(freeSpeechSttTask);

        Log.w("MRRR", "startListeningfreeSpeechStt : cerence");

        try{Log.w("MRRR", "startListeningfreeSpeechStt :try ");
            freeSpeechSttTask.start(true, new ISTTCallback.Stub() {
                @Override
                public void onSuccess(STTResultsData sttResultsData) throws RemoteException {
                    Log.i("MRA", "Succès d'écoute cerence Free Speech");

                    if (!sttResultsData.getResults().isEmpty()) {

                        STTResult result = sttResultsData.getResults().get(0);

                        Log.e("MRRR","Listening cerence Free Speech : " +
                                "\nScore : " + result.getConfidence() + //the recognition score
                                "\nUtterance: " + result.getUtterance() +  //actual phrase pronounced by the user and recognised by free speech (google/cerence)
                                "\nRule: " + result.getRule()); //the respective tag of the Uterrance, as described in the grammar
                        notifyObservers("STTQuestion_success;"+result.getUtterance());

                    }
                }

                @Override
                public void onError(String s) throws RemoteException {
                    Log.e("MRRR","onError cerence "+s);

                }
            });
        }
        catch (Exception e) {
            Log.e("MRRR","onError cerence "+e);

        }

        setLed("listening");


        return freeSpeechSttTask;

    }

    public void refresh(String langue,Activity activity) {
        Log.e("TEST", "Langue Stt refresh  : " + langue);
        speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(activity);
        if (getParamFromFile("Language_Specification_STT",configurationFilePseudo).trim().equalsIgnoreCase("Yes")) {
            if (getparam("STT_chosen").equalsIgnoreCase("Android")) {
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,getListeningDuration()*1000);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,Integer.parseInt(getParamFromFile("Android_Speech_silence_length",configurationFilePseudo))*1000);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langue);
            }
            if (getparam("STT_chosen").equalsIgnoreCase("Cerence")) {
                if (!langue.toLowerCase().contains("en") && !langue.toLowerCase().contains("fr")) {
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,getListeningDuration()*1000);
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,Integer.parseInt(getParamFromFile("Android_Speech_silence_length",configurationFilePseudo))*1000);
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langue);
                }
            }
            if (!getLangue().getNom().equals(langueFr) && !getLangue().getNom().equals(langueEn)) {
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,Integer.parseInt(getParamFromFile("Android_Speech_minimum_length",configurationFilePseudo))*1000);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,Integer.parseInt(getParamFromFile("Android_Speech_silence_length",configurationFilePseudo))*1000);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

            } else {
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,Integer.parseInt(getParamFromFile("Android_Speech_minimum_length",configurationFilePseudo))*1000);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,Integer.parseInt(getParamFromFile("Android_Speech_silence_length",configurationFilePseudo))*1000);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langue);
            }
        }
        else {
            speechRecognizerIntent.removeExtra(RecognizerIntent.EXTRA_LANGUAGE);
            speechRecognizerIntent2.removeExtra(RecognizerIntent.EXTRA_LANGUAGE);
        }



    }
    public void startListeningQuestion(Activity activity,String type) {
        notifyObservers("showCameraQr");
        isListeningHotw = false;
        Log.e(TAG,"startListeningFreeSpeechStt fonction start");
        if (!isModeContinuousListeningON()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listeningAnimation();
                }
            });
            alreadyGetAnswer = false;
            questionNumber++;
            currentEmotion = "";
            shouldPlayEmotion = false;
        }
        stopListening(activity);
        speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(activity);

        if (getCurrentLanguage().equals("en")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_en);
        }
        else if (getCurrentLanguage().equals("fr")){
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_fr);
        }
        else if (getCurrentLanguage().equals("de")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_de);
        }
        else if (getCurrentLanguage().equals("es")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_es);
        }
        else{
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_stt_android_indispo_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            toast_stt_android_indispo = translatedText;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_en);
                        }
                    });
        }
        activity.runOnUiThread(() -> {
            try {
                setLed("listening");
                speechRecognizer.startListening(speechRecognizerIntent);
                if(!isAppInstalled(getApplicationContext(),"com.google.android.googlequicksearchbox")) {
                    showToast(toast_stt_android_indispo);
                }
                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle bundle) {
                        Log.e(TAG, " listen start");

                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        Log.i(TAG, "start listen");


                    }

                    @Override
                    public void onRmsChanged(float v) {
                        //Log.i(TAG, "onRmsChanged listen");
                    }

                    @Override
                    public void onBufferReceived(byte[] bytes) {
                        Log.i(TAG, "onBufferReceived listen");
                    }

                    @Override
                    public void onEndOfSpeech() {
                        Log.i(TAG, "onEndOfSpeech listen");
                    }

                    @Override
                    public void onError(int i) {
                        switch (i) {
                            case SpeechRecognizer.ERROR_AUDIO:
                                Log.d(TAG, "Audio recording error");
                                logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_AUDIO","Audio recording error");
                                break;
                            case SpeechRecognizer.ERROR_CLIENT:
                                Log.d(TAG, "Client side error");
                                logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_CLIENT","Client side error");
                                break;
                            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                Log.d(TAG, "Insufficient permissions");
                                logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS","Insufficient permissions");
                                break;
                            case SpeechRecognizer.ERROR_NETWORK:
                                Log.d(TAG, "Network error");
                                logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_NETWORK","Network error");
                                break;
                            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                                Log.d(TAG, "Network timeout");
                                logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_NETWORK_TIMEOUT","Network timeout");
                                break;
                            case SpeechRecognizer.ERROR_NO_MATCH:
                                Log.d(TAG, "No match");
                                if (!isModeContinuousListeningON()) {
                                    if (type.equalsIgnoreCase("startCycle")) {
                                        notifyObservers("SpeechRecognizerAttemptTimeout");
                                    } else {
                                        notifyObservers("SpeechRecognizerTimeout");
                                    }
                                }
                                else{
                                    setLed("listening");
                                    speechRecognizer.startListening(speechRecognizerIntent);
                                }
                                break;
                            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                                Log.d(TAG, "RecognitionService busy");
                                logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_RECOGNIZER_BUSY","RecognitionService busy");
                                break;
                            case SpeechRecognizer.ERROR_SERVER:
                                Log.d(TAG, "Server error");
                                logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_SERVER","Server error");
                                break;
                            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                                Log.d(TAG, "No speech input");
                                logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_SPEECH_TIMEOUT","No speech input");
                                break;
                            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                                Log.e(TAG, "ERROR_LANGUAGE_NOT_SUPPORTED");
                                logErrorSTTAndroid(i,"SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED","Language not supported");
                                break;
                            default:
                                Log.d(TAG, "Unknown error "+i);
                                logErrorSTTAndroid(i,"Unknown error","Unknown error");
                                break;
                        }

                    }

                    @Override
                    public void onResults(Bundle bundle) {
                        ArrayList<String> data = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                        if (data!=null && data.size()>0) {
                            if(!data.get(0).trim().equals("")) {
                                Log.e(TAG, "question result onResults  : " + data.get(0));
                                notifyObservers("STTQuestion_success;" + data.get(0));
                                BuddySDK.UI.stopListenAnimation();
                            }else{
                                Log.e(TAG, "question result onResults  : vide " + data.get(0));
                            }
                        }
                        else {
                            Log.e(TAG, "question result onResults size = 0 : " );
                            //setLed("listening");
                            //speechRecognizer.startListening(speechRecognizerIntent);
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle bundle) {
                        Log.i(TAG, "onPartialResults listen");
                        ArrayList<String> data = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                        if (data!=null && data.size()>0) {
                            Log.e(TAG, "data langth  : " + data.size());
                            if(!data.get(0).trim().equals("")) {
                                Log.e(TAG, "question result onPartialResults  : " + data.get(0));
                                notifyObservers("STTQuestion_success;" + data.get(0));
                                BuddySDK.UI.stopListenAnimation();
                            }else{
                                Log.e(TAG, "question result onPartialResults  : vide " + data.get(0));
                            }
                        }
                        else {
                            Log.e(TAG, "question result onPartialResults size = 0 : " );
                        }
                    }

                    @Override
                    public void onEvent(int i, Bundle bundle) {
                        Log.i(TAG, "onEvent listen");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Runnable : Erreur pendant la vérification [isReadyToListen] : " + e);
            }



        });





    }
    public void logErrorSTTAndroid(int code,String type,String message){
        String errorTXT= new Date().toString()+", STTAndroidERROR,ERROR CODE= "+String.valueOf(code)+", ERROR Body{ type= "+type+", message= "+message+"}"+System.getProperty("line.separator");
        File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");


        try {

            FileWriter fileWriter = new FileWriter(file2,true);
            fileWriter.write(errorTXT);
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void startListeningQuestionWithGoogleApi(Activity activity){
        notifyObservers("showCameraQr");
        SttGoogleCallbackCalledOnce = false;
        activityTemp =activity;
        if (!isModeContinuousListeningON()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listeningAnimation();
                }
            });
            alreadyGetAnswer = false;
            questionNumber++;
            currentEmotion = "";
            shouldPlayEmotion = false;
        }
        stopListening(activity);
        if (isRecording) {
            Log.d(TAG_STREAMING, "Already recording");
            return;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE);

        // Adjust the path and file name as needed
        String outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.pcm";

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            setLed("listening");
            audioRecord.startRecording();
            isRecording = true;
            currentState="";
            startVAD();
            processAudio(outputFile);
        } else {
            Log.e(TAG_STREAMING, "Failed to initialize AudioRecord");
        }
    }
    public void startWhisperRecording(Activity activity){
        notifyObservers("showCameraQr");
        Log.e("MRA","startWhisperRecording");
        alReadyHadSpoke=false;
        activityTemp =activity;
        if (!isModeContinuousListeningON()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listeningAnimation();
                }
            });
            alreadyGetAnswer = false;
            questionNumber++;
            currentEmotion = "";
            shouldPlayEmotion = false;
            stopListening(activity);
        }
        if (isRecording) {
            Log.d(TAG_STREAMING, "Already recording");
            return;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE);

        // Adjust the path and file name as needed
        String outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.pcm";

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            setLed("listening");
            audioRecord.startRecording();
            isRecording = true;
            currentState="";
            startVAD();
            processAudio(outputFile);
        } else {
            Log.e(TAG_STREAMING, "Failed to initialize AudioRecord");
        }
    }
    public void stopWhisperSTT(Boolean shouldRestartListening,Boolean shouldRestartNewCycle) {
        try {
            byte[] audioDataF = readAudioFile(); // Read the recorded audio data
            // Annuler la tâche précédente si elle existe
            if (transcribeTask != null && transcribeTask.getStatus() == AsyncTask.Status.RUNNING) {
                transcribeTask.cancel(true);
            }
            Log.e("MIDO","start dbfs calcul");
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            thread =new Thread(() -> {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(activityTemp));
                }
                Python py = Python.getInstance();
                PyObject pyobj = py.getModule("calculDBFS");
                try {
                    PyObject reponse;
                    JSONObject parameters = new JSONObject();
                    parameters.put("fichier_audio", Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav"); // Chemin de votre fichier audio

                    // Appel de la fonction main avec le chemin du fichier audio
                    reponse = pyobj.callAttr("main", parameters.getString("fichier_audio"));

                    //Mettre  le dernier fichier json envoyé à l’API



                    Log.e("MIDO","result dBFS python "+reponse.toString());
                    if (!reponse.toString().trim().equals("-inf")) {
                        if (Float.parseFloat(reponse.toString()) >= Float.parseFloat(getParamFromFile("Seuil_dBFS", configurationFilePseudo))) {
                            Log.d("MIDO", "volume est bien : " + Float.parseFloat(reponse.toString()));
                            transcribeTask = new TranscribeTask();
                            transcribeTask.execute(audioDataF); // Transcribe the audio
                        } else {
                            Log.d("MIDO", "volume est trop bas : " + Float.parseFloat(reponse.toString()));
                            if (!isModeContinuousListeningON()) {
                                if (shouldRestartListening) {
                                    startWhisperRecording(activityTemp);
                                } else {
                                    Log.e("ARR", "stopWhisper restartNewCycle  shouldRestartNewCycle" + shouldRestartListening);
                                    if (shouldRestartNewCycle) {
                                        activityTemp.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                notifyObservers("restartNewCycle");
                                            }
                                        });
                                    } else {
                                        activityTemp.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                notifyObservers("restartListeningHotword");
                                            }
                                        });
                                    }
                                }
                            }
                            else {
                                startWhisperRecording(activityTemp);
                            }
                        }
                    }
                    else {
                        if (shouldRestartListening) {
                            startWhisperRecording(activityTemp);
                        } else {
                            if (shouldRestartNewCycle){
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyObservers("restartNewCycle");
                                    }
                                });
                            }
                            else {
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyObservers("restartListeningHotword");
                                    }
                                });
                            }
                        }
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        return; // Terminer le thread s'il a été interrompu
                    }


                } catch (PyException | JSONException p) {
                    Log.e(TAG, "Exception "+p);
                }

            });
            thread.start();
        } catch (Exception e) {
            Log.e("MRA", "Exception " + e);
        }

    }
    private int getAudioDuration() {
        try {

            // Use MediaPlayer to get the duration
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav");
            mediaPlayer.prepare();
            int duration = mediaPlayer.getDuration();

            // Release the MediaPlayer resources
            mediaPlayer.release();

            return duration;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }
    private byte[] readAudioFile() throws IOException {
        // Convert PCM data to WAV format
        String outputFileWav = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav";
        PcmToWavConverter.convert(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.pcm", outputFileWav);

        Log.d("FilePath", "File path: " + outputFileWav);
        File audioFileWav = new File(outputFileWav);
        if (audioFileWav.exists()) {
            return Files.readAllBytes(audioFileWav.toPath());
        } else {
            // Handle the case where the file does not exist
            Log.e("FileError", "The file does not exist at the specified path.");
            return null;
        }
    }

    //    private void writeAudioDataToFile() {
//        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TeamChatBuddy/audioF.raw";
//        File file = new File(filePath);
//
//        try {
//            FileOutputStream os = new FileOutputStream(file);
//            byte[] buffer = new byte[BUFFER_SIZE];
//
//            while (isRecording) {
//                int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
//                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
//                    os.write(buffer, 0, read);
//                }
//            }
//
//            os.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        // Une fois l'enregistrement terminé, convertir le fichier audio brut en MP3
//        convertToMp3(filePath);
//    }
//
//    private void convertToMp3(String inputPath) {
//        Log.e("MMMM","start convertToMp3");
//        String outputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TeamChatBuddy/audioF.mp3";
//
//        String[] cmd = new String[]{"-b", "128", inputPath, outputPath};
//
//        try {
//            Process process = Runtime.getRuntime().exec(cmd);
//            process.waitFor();
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Supprimer le fichier audio brut (facultatif)
//        File rawFile = new File(inputPath);
//        if (rawFile.exists()) {
//            rawFile.delete();
//        }
//        Log.e("MMMM","end convertToMp3");
//        byte[] audioDataF;
//        try {
//            File audioFileF = new File("/storage/emulated/0/TeamChatBuddy/audioF.mp3");
//            audioDataF = Files.readAllBytes(audioFileF.toPath());
//
//            new TranscribeTask().execute(audioDataF);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    public String transcribe(byte[] audioData) throws IOException {
        Log.e("MMMM","start TRanscribe");
        List<String> langueCode = getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Whisper");
        String language = langueCode.get(getLangue().getId()-1);

        if (getParamFromFile("Language_Specification_STT",configurationFilePseudo).trim().equalsIgnoreCase("No")){
            language="";
        }
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", getParamFromFile("Whisper_model",configurationFilePseudo).trim())
                .addFormDataPart("file", "GPTAudio.mp3",
                        RequestBody.create(MediaType.parse("audio/mp3"), audioData))
                .addFormDataPart("language",language)
                .addFormDataPart("prompt",getParamFromFile("Whisper_prompt",configurationFilePseudo).trim())
                .build();

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + getparam("openAI_API_Key"))
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Log.i("MRA", "Calling Whisper API is successful");

                double durationInMinutes = (double) getAudioDuration() / (60 * 1000);
                Log.i("MYA", "Calling Whisper API is successful1------"+durationInMinutes);
                calcul_consommation(getParamFromFile("Whisper_model","TeamChatBuddy.properties"), (double) durationInMinutes,0);

                Gson gson = new Gson();
                String responseBody = response.body().string();
                TranscriptionResult result = gson.fromJson(responseBody, TranscriptionResult.class);
                return result.text;

            } else {
                int checkErrorCode = response.code();
                // Calcul de la consommation openai de le cas d'echec
                if (checkErrorCode == 500 || checkErrorCode == 503 || checkErrorCode == 504) {
                    double durationInMinutes = (double) getAudioDuration() / (60 * 1000);
                    calcul_consommation(getParamFromFile("Whisper_model","TeamChatBuddy.properties"), (double) durationInMinutes,0);
                }
                Log.i("MRA", "Calling Whisper API is failed");
                throw new IOException("Unexpected response code: " + response.code());
            }
        }
        catch (Exception e){
            throw new IOException("Unexpected response code: " + e.toString());
        }
    }

    private static class TranscriptionResult {
        public String text;
    }

    private class TranscribeTask extends AsyncTask<byte[], Void, String> {
        String question = "";
        @Override
        protected String doInBackground(byte[]... audioData) {
            try {
                //duration = System.currentTimeMillis();
                Log.e("MRA","doInBackground stopProcessus---------- "+stopProcessus);
                if (!stopProcessus) {
                    Log.e("MRA","doInBackground stopProcessus if---------- "+stopProcessus);
                    question =transcribe(audioData[0]);
                    Log.e("MRA","doInBackground stopProcessus question---------- "+question);
                    if (!question.trim().contains("Thank you") && !question.equals("")) {
                        if (!stopProcessus) {
                            if (activityTemp!=null){
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("MRA","envoie traitement de la question");
                                        notifyObservers("STTQuestion_success;"+question);
                                        BuddySDK.UI.stopListenAnimation();
                                    }
                                });
                            }

                        }

                    } else {
                        if (!endRecordingWhisperAudio) {
                            if (activityTemp!=null){
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        startWhisperRecording(activityTemp);
                                    }
                                });
                            }



                        }
                    }
                }
                return question;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String transcription) {
            if (transcription != null) {
                // endTime = System.currentTimeMillis(); // Record the end time
                Log.i("MRA", "------it took: ms");
            } else {
                // Gestion des erreurs

            }
        }
    }

    /*
     * VAD library only accepts 16-bit mono PCM audio stream and can work with the next Sample Rates and Frame Sizes :
     *
     *  Valid Sample Rate     Valid Frame Size
     *      8000Hz              80, 160, 240
     *      16000Hz             160, 320, 480
     *      32000Hz             320, 640, 960
     *      48000Hz             480, 960, 1440
     *
     * the number of bytes received by the BlueMic is by default 40 (AUDIO_PACKAGE_SIZE=40).
     * in order to be able to pass the audio stream to the VAD function with a SampleRate of 8000Hz
     * we have to find a way to modify the number of processed bytes to 80 bytes (AUDIO_PACKAGE_SIZE=80)
     *
     * we are going to build a new shorts[80] which is the combination of two shorts[40] received from the BlueMic.
     *
     * Algo:
     * I store each new short[40] in a circularBuffer and wait for the next short[40] to be received.
     * Once received, I combine the two in a short[80] and send it in the callback : onNewAudioData
     */
    private final VadListener vadListener = new VadListener() {
        @Override
        public void onSpeechDetected() {
            Log.d(TAG_STREAMING, "Speech detected!");
            // Votre code lorsque la parole est détectée
            if (!currentState.equals("SPEECH")) {
                currentState = "SPEECH";
                alReadyHadSpoke=true;
                if (!getParamFromFile("Volume_reduction",configurationFilePseudo).trim().equals("")
                        && !getParamFromFile("Volume_reduction",configurationFilePseudo).trim().equals("0")
                        && !getParamFromFile("Duration_sound_level_checked",configurationFilePseudo).trim().equals("")
                        && !getParamFromFile("Duration_sound_level_checked",configurationFilePseudo).trim().equals("0")
                ){
                    handler2.postDelayed(periodicTask,Integer.valueOf(getParamFromFile("Duration_sound_level_checked",configurationFilePseudo))*1000 );
                }
            }
        }

        @Override
        public void onNoiseDetected() {
            Log.d(TAG_STREAMING, "Noise detected!");
            // Votre code lorsque du bruit est détecté
            if (!currentState.equals("NOISE")) {
                currentState = "NOISE";
                if(alReadyHadSpoke){
                    alReadyHadSpoke=false;
                    stopProcessus =false;

                    stopRecording();
                    if (getparam("STT_chosen").trim().equalsIgnoreCase("Whisper")){
                        stopWhisperSTT(true,false);
                    }
                    else{
                        stopGoogleApiSTT(true,false);
                    }
                }

            }
        }

    };
    Runnable periodicTask = new Runnable() {
        @Override
        public void run() {
            try {
                readAudioFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e("MRAE","start dbfs calcul 3---------------");
            if (thread1 != null && thread1.isAlive()) {
                thread1.interrupt();
            }
            thread1 =new Thread(() -> {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(activityTemp));
                }
                Python py = Python.getInstance();
                PyObject pyobj = py.getModule("calculDBFS");
                try {
                    PyObject reponse;
                    JSONObject parameters = new JSONObject();
                    parameters.put("fichier_audio", Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav"); // Chemin de votre fichier audio

                    // Appel de la fonction main avec le chemin du fichier audio
                    reponse = pyobj.callAttr("main", parameters.getString("fichier_audio"));

                    //Mettre  le dernier fichier json envoyé à l’API
                    Log.e("MRAE", "test comparaison flot--------------- " + reponse.toString());
                    Log.e("MRAE", "result dBFS python--------------- " + reponse.toString());
                    Log.e("MRAE", "previousVolume--------------- " + previousVolume);
                    Log.e("MRAE", "previousVolume after traitement--------------- " + (previousVolume - (Math.abs(previousVolume) * Float.parseFloat(getParamFromFile("Volume_reduction", configurationFilePseudo)) / 100)));
                    if (!reponse.toString().trim().equals("-inf")){
                        if (previousVolume == 0) {
                            Log.e("MRAE", "result dBFS if--------------- ");
                            previousVolume = Float.parseFloat(reponse.toString());
                        } else {
                            if (Float.parseFloat(reponse.toString()) <= (previousVolume - (Math.abs(previousVolume) * Float.parseFloat(getParamFromFile("Volume_reduction", configurationFilePseudo)) / 100))) {
                                traitementAudio(false);
                                previousVolume = Float.valueOf(0);
                                Log.e("MRAE", "result dBFS else if--------------- ");

                            } else {
                                Log.e("MRAE", "result dBFS else else--------------- ");
                                previousVolume = Float.parseFloat(reponse.toString());
                            }
                        }
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        return; // Terminer le thread s'il a été interrompu
                    }


                } catch (PyException | JSONException p) {
                    Log.e("MRAE","exception dBFS python "+p);
                }

            });
            thread1.start();
            handler2.postDelayed(this, Integer.valueOf(getParamFromFile("Duration_sound_level_checked",configurationFilePseudo))*1000);
        }
    };
    public void stopRecording() {
        if (handler2 != null && periodicTask != null) {
            handler2.removeCallbacks(periodicTask);
        }
        if (!isRecording) {
            Log.d(TAG_STREAMING, "Not recording");
            return;
        }
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (vad != null) {
            Log.e("MRA","+++++++++++++++++++++++++++++++++vad stop");
            vad.stop();
        }
    }

    private void startVAD() {
        int silenceTime;
        if (!getParamFromFile("Silence_time",configurationFilePseudo).trim().equals("")){
            try {
                silenceTime= Integer.parseInt(getParamFromFile("Silence_time",configurationFilePseudo).trim()) *1000;
            }
            catch (Exception e){
                silenceTime = 500;
            }
        }
        else{
            silenceTime = 500;
        }
        // Configure and start VAD
        vad = new Vad(VadConfig.newBuilder()
                .setSampleRate(VadConfig.SampleRate.SAMPLE_RATE_8K)
                .setFrameSize(VadConfig.FrameSize.FRAME_SIZE_80)
                .setMode(VadConfig.Mode.VERY_AGGRESSIVE)
                .setSilenceDurationMillis(silenceTime)
                .setVoiceDurationMillis(500)
                .build());
        vad.start();
    }

    private void processAudio(String outputFile) {
        new Thread(() -> {
            short[] buffer = new short[BUFFER_SIZE / 2]; // Divided by 2 because each short is 2 bytes
            try {
                FileOutputStream fos = new FileOutputStream(outputFile);
                while (isRecording) {
                    int numRead = audioRecord.read(buffer, 0, buffer.length);
                    if (numRead > 0) {
                        vad.addContinuousSpeechListener(buffer, vadListener);
                        fos.write(shortArrayToByteArray(buffer), 0, numRead * 2); // * 2 because each short is 2 bytes
                    }
                }
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Log.e(TAG,"processAudioFinally");
//                stopWhisperSTT(); // Stop recording and process the remaining audio
            }
        }).start();
    }

    // Convertir un tableau de shorts en un tableau de bytes (pour le buffer combiné)
    private byte[] shortArrayToByteArray(short[] shortArray) {
        int length = shortArray.length;
        byte[] byteArray = new byte[length * 2]; // Each short is 2 bytes
        for (int i = 0; i < length; i++) {
            byteArray[i * 2] = (byte) (shortArray[i] & 0xFF);
            byteArray[i * 2 + 1] = (byte) ((shortArray[i] >> 8) & 0xFF);
        }
        return byteArray;
    }
    public void traitementAudio(boolean shouldRestartNewCycle){
        currentState = "NOISE";

        alReadyHadSpoke = false;
        stopProcessus =false;
        stopRecording();
        Log.e("MIDO","appel stopWhisper onNoiseDetected");
        if (getparam("STT_chosen").trim().equalsIgnoreCase("Whisper")){
            stopWhisperSTT(false,shouldRestartNewCycle);
        }
        else{
            stopGoogleApiSTT(false,shouldRestartNewCycle);
        }

    }


    public Locale getGoogleSTTLang(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            Log.e("MYA GoogleSTT", "getLocale() : languageCode est nul ou vide");
            return Locale.ENGLISH;
        }
        String normalizedCode = languageCode;

        Log.e("MYA GoogleSTT", "getLocale() : languageCode pas nul ni vide");
        // Recherche exacte dans les locales supportées
        for (Map.Entry<String, Locale> entry : supportedLocales.entrySet()) {
            String key = entry.getKey();
            if (key.equals(normalizedCode)) {
                Log.w("MYA GoogleSTT", "getLocale(" + normalizedCode + ") result : " + entry.getValue());
                return entry.getValue();
            }
        }

        // Si non trouvée
        Log.e("MYA GoogleSTT", "getLocale(" + normalizedCode + ") result : null");
        return Locale.ENGLISH;
    }

    public void initGoogleAPI() {
        Log.w("GoogleSTT","initGoogleAPI");
        try {
            FetchLangGoogleCloudSTT();
            Locale localeLanguage;
            List<String> langueCode = getLanguageCodeForDisponibleLangue("Language_Code_Used_In_GoogleCloud_STT");
            String language = langueCode.get(getLangue().getId()-1);
            Log.w("GoogleSTT","initGoogleAPI language= "+language);
            if (getParamFromFile("Language_Specification_STT",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                localeLanguage= null;
            }
            else {
                localeLanguage=getGoogleSTTLang(language);
            }
            Log.e("MYA GoogleSTT","localeLanguage=  "+localeLanguage);
            googleSTT = new GoogleSTT(getParamFromFile("ApiGoogle_Key",configurationFilePseudo), localeLanguage);

            googleSTTCallbacks = new GoogleSTTCallbacks() {
                @Override
                public void onRequestSent() {
                    Log.w("GoogleSTT","onRequestSent");
                }

                @Override
                public void onResponse(String text) {
                    Log.i("MYA GoogleSTT","onResponse mya: "+text);
                    if (text != null && !text.isEmpty()) {
                        notifyObservers("STTQuestion_success;"+text);
                        BuddySDK.UI.stopListenAnimation();
                    }
                    else {
                        startListeningQuestionWithGoogleApi(activityTemp);
                    }
                }

                @Override
                public void onResponseError(String error) {
                    Log.e("MYA GoogleSTT","onResponseError : "+error);
                    if(!error.equals("NO_ERROR")){
                        startListeningQuestionWithGoogleApi(activityTemp);
                    }
                }
            };
        } catch (Exception e) {
            Log.e("GoogleSTT","EXCEPTION : "+e);
            e.printStackTrace();
        }
    }

    public void releaseGoogleAPI() {
        Log.w("GoogleSTT","releaseGoogleAPI");
        googleSTT = null;
        googleSTTCallbacks = null;
    }

    public void stopGoogleApiSTT(Boolean shouldRestartListening,Boolean shouldRestartNewCycle) {
        try {
            byte[] audioDataF = readAudioFile(); // Read the recorded audio data
            // Annuler la tâche précédente si elle existe
            Log.e("MIDO","start dbfs calcul");
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            thread =new Thread(() -> {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(activityTemp));
                }
                Python py = Python.getInstance();
                PyObject pyobj = py.getModule("calculDBFS");
                try {
                    PyObject reponse;
                    JSONObject parameters = new JSONObject();
                    parameters.put("fichier_audio", Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav"); // Chemin de votre fichier audio

                    // Appel de la fonction main avec le chemin du fichier audio
                    reponse = pyobj.callAttr("main", parameters.getString("fichier_audio"));

                    //Mettre  le dernier fichier json envoyé à l’API



                    Log.e("MIDO","result dBFS python "+reponse.toString());
                    if (!reponse.toString().trim().equals("-inf")) {
                        if (Float.parseFloat(reponse.toString()) >= Float.parseFloat(getParamFromFile("Seuil_dBFS", configurationFilePseudo))) {
                            Log.d("MIDO", "volume est bien : " + Float.parseFloat(reponse.toString()));

                            if(googleSTT != null) {
                                googleSTT.sendRequest(audioDataF, 8000, googleSTTCallbacks);
                            }
                            else{
                                Log.e("GoogleSTT","GoogleSTT not initialized ! Can't send request");
                            }
                        } else {
                            Log.d("MIDO", "volume est trop bas : " + Float.parseFloat(reponse.toString()));
                            if (!isModeContinuousListeningON()) {
                                if (shouldRestartListening) {
                                    startListeningQuestionWithGoogleApi(activityTemp);
                                } else {
                                    if (shouldRestartNewCycle) {
                                        activityTemp.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                notifyObservers("restartNewCycle");
                                            }
                                        });
                                    } else {
                                        activityTemp.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                notifyObservers("restartListeningHotword");
                                            }
                                        });
                                    }

                                }
                            }
                            else {
                                startListeningQuestionWithGoogleApi(activityTemp);
                            }
                        }
                    }
                    else {
                        Log.d("MIDO", "volume est trop bas");
                        if (shouldRestartListening) {
                            startListeningQuestionWithGoogleApi(activityTemp);
                        } else {
                            if (shouldRestartNewCycle){
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyObservers("restartNewCycle");
                                    }
                                });
                            }else {
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyObservers("restartListeningHotword");
                                    }
                                });
                            }

                        }
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        return; // Terminer le thread s'il a été interrompu
                    }


                } catch (PyException | JSONException p) {
                    Log.e("MIDO","exception dBFS python "+p);

                }

            });
            thread.start();


        } catch (Exception e) {
            Log.e("MRA", "Exception " + e);
        }

    }

    public List<String> getHotwordList(){
        if (getLangue().getNom().equals(langueFr)){
            return separator(getParamFromFile("hotword_fr", configurationFilePseudo));
        }else if (getLangue().getNom().equals(langueEn)) {
            return separator(getParamFromFile("hotword_en", configurationFilePseudo));
        }
        else {
            Log.e(TAG,"getHotwordList"+translatedList);

            return separator(translatedList);
        }

    }
    public void getTranslateHotwordList(){
        if (!getLangue().getNom().equals(langueFr) && !getLangue().getNom().equals(langueEn)){
            translatedList=getParamFromFile("hotword_en", configurationFilePseudo);
//            getEnglishLanguageSelectedTranslator().translate(getParamFromFile("hotword_en", configurationFilePseudo)).addOnSuccessListener(new OnSuccessListener<String>() {
//                @Override
//                public void onSuccess(String s) {
//                    translatedList = s.trim();
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    Log.e(TAG,"liste traduite onFailure "+e);
//                }
//            });
        }
    }

    private boolean hotwordAlreadyHandled = false;
    public void checkTheHotword(String word, String type) {
        List<String> hotword = getHotwordList();
        boolean rightHotwordDetected = false;

        // Si on a déjà traité un hotword → on sort immédiatement
        if (hotwordAlreadyHandled) {
            return;
        }

        for (String hw : hotword) {

            if (word.trim().equalsIgnoreCase(hw.trim())) {

                rightHotwordDetected = true;

                hotwordAlreadyHandled = true;

                listeningState = "qst";
                new Handler(Looper.getMainLooper()).postDelayed(() -> {

                    // Vérification : toujours en mode hotword ?
                    if (listeningState.equals("hotword")) {

                        //listeningState = "qst";
                        isAprilTagProcessing.set(false);
                        isListeningHotw = false;
                        Log.i("OpenAITTS", "------------------------ checkHotw isListeningHotw = false ");

                        if(type.equalsIgnoreCase("AprilTag")||type.equalsIgnoreCase("DataMatrix")||type.equalsIgnoreCase("QRCode")) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String scanTXT = "[" + sdf.format(new Date()) + "] - " + type + " - " + word + System.getProperty("line.separator");
                            File file = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Scan_History.txt");

                            try {
                                FileWriter fileWriter = new FileWriter(file, true);
                                fileWriter.write(scanTXT);
                                fileWriter.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                }, 1500);

                // On notifie UNE SEULE FOIS
                notifyObservers("STTHotword_success");

                return;
            }
        }

        // Si rien détecté et que le hotword n'a pas encore été traité
        if (!rightHotwordDetected && !hotwordAlreadyHandled) {
            if (speechRecognizer != null && speechRecognizerIntent2 != null) {
                setLed("listening");
                speechRecognizer.startListening(speechRecognizerIntent2);
            }
        }
    }



    /**
     * Cette fonction permet d'arrêter l'écoute STT Free Speech
     */
    public void stopListening(Activity activity) {



        activity.runOnUiThread(() -> {

            stopProcessus =true;

            stopListeningBlueMic();

            if (handlerListeningHotword != null && runnableListeningHotword != null) {

                handlerListeningHotword.removeCallbacksAndMessages(null);
                handlerListeningHotword.removeCallbacks(runnableListeningHotword);

            }
            try{
                if (speechRecognizer != null ) {
                    speechRecognizer.stopListening();
                    speechRecognizer.stopListening();
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }
            }
            catch (Exception e){}
                if(freeSpeechSttTask != null) {
                    Log.w(TAG, "stopListeningFreeSpeechStt");
                    try {
                        freeSpeechSttTask.stop();
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur pendant l'arrêt d'écoute STT Free Speech : "+e);
                    }
                }


            stopRecording();


        });



    }





    private void listeningAnimation() {
        Log.i(TAG, "startVoiceRecorder");
        BuddySDK.UI.setFacialExpression(FacialExpression.LISTENING,1);
        BuddySDK.UI.startListenAnimation();
        //setLed("listening");

    }

    private void neutralAnimation() {

        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
        BuddySDK.UI.stopListenAnimation();

    }




    //#endregion ******************************************************* STT Free Speech **********************************************************************

    //#endregion ******************************************************* STT **********************************************************************


    /**
     * Cette fonction permet de prononcer le texte passé en argument de manière récursive, afin de gérer le problème de décalage entre la bouche et le discours pour les réponses plus longues.
     * @param texteToSpeak : message à dire par Buddy.
     * @param expression : jouer un mouvement spécial de la bouche [SPEAK_ANGRY / NO_FACE / SPEAK_HAPPY / SPEAK_NEUTRAL]
     * @param texteToSpeakSplitted : Liste de phrases courtes à dire par Buddy.
     *
     */
    public void startSpeakingSplittedText(final String texteToSpeak , LabialExpression expression,String type, String[] texteToSpeakSplitted ){

        Log.i("FCH_DEBUG", "startSpeakingSplittedText "+ Arrays.toString(texteToSpeakSplitted) + " , " + type);


        setSpeaking(true);
        Handler handler_all = new Handler(Looper.getMainLooper());
        Runnable delayedTask = new Runnable() {
            @Override
            public void run() {
                Log.i("FCH_DEBUG", "handler_all start ");

                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                }
                catch (Exception e){
                    Log.e(TAG,"BuddySDK Exception  "+e);
                }

                if(currentIndexText < texteToSpeakSplitted.length ) {

                    Log.e("FCH_DEBUG", "call startSpeaking");
                    //setLed("speaking");
                    if(texteToSpeak.contains(";splitNews;")){
                        //          texteToSpeak = texteToSpeak.replaceAll("news;","");
                        BuddySDK.Speech.startSpeaking(
                                texteToSpeakSplitted[currentIndexText],
                                expression,
                                new ITTSCallback.Stub() {
                                    @Override
                                    public void onSuccess(String iText) throws RemoteException {
                                        Log.i(TAG, "Succès de prononciation : "+iText);
                                        Log.w("FCH_DEBUG", "onSuccess");
                                        currentIndexText++;
                                        if (!Stop_TTS_ReadSpeaker) {
                                            Log.w("FCH_DEBUG", "onSuccess 1 ");
                                            Handler handler = new Handler(Looper.getMainLooper());
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.w("FCH_DEBUG", "onSuccess 2");
                                                    startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                                                }
                                            }, 3000);
                                        }
                                    }
                                    @Override
                                    public void onError(String iError) throws RemoteException {
                                        Log.e(TAG, "Erreur pendant la prononciation 1 : "+iError);
                                        Log.w("FCH_DEBUG", "onError");
                                        allTextPronoucedSuccess = false;
                                        currentIndexText++;
                                        if (!Stop_TTS_ReadSpeaker) {
                                            Log.w("FCH_DEBUG", "onError 1 ");
                                            Handler handler = new Handler(Looper.getMainLooper());
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.w("FCH_DEBUG", "onError 2");
                                                    startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                                                }
                                            }, 150);
                                        }
                                    }
                                    @Override
                                    public void onPause() throws RemoteException {}
                                    @Override
                                    public void onResume() throws RemoteException {}
                                }
                        );
                    }
                    else{
                        BuddySDK.Speech.startSpeaking(
                                texteToSpeakSplitted[currentIndexText],
                                expression,
                                new ITTSCallback.Stub() {
                                    @Override
                                    public void onSuccess(String iText) throws RemoteException {
                                        Log.i(TAG, "Succès de prononciation : " + iText);

                                        Log.w("FCH_DEBUG", "onSuccess");

                                        currentIndexText++;

                                        if (!Stop_TTS_ReadSpeaker) {
                                            Log.w("FCH_DEBUG", "onSuccess 1 ");
                                            Handler handler = new Handler(Looper.getMainLooper());
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.w("FCH_DEBUG", "onSuccess 2");
                                                    startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                                                }
                                            }, 150);
                                        }


                                    }

                                    @Override
                                    public void onError(String iError) throws RemoteException {
                                        //Log.e(TAG, "Erreur pendant la prononciation 2 : " + iError);
                                        Log.e("test_welcome", "Erreur pendant la prononciation 2 : " + iError);

                                        Log.w("FCH_DEBUG", "onError");

                                        allTextPronoucedSuccess = false;


                                        currentIndexText++;
                                        Log.e("test_welcome", "Erreur pendant la prononciation Stop_TTS_ReadSpeaker : " + Stop_TTS_ReadSpeaker);

                                        if (!Stop_TTS_ReadSpeaker) {
                                            Log.w("FCH_DEBUG", "onError 1 ");
                                            Handler handler = new Handler(Looper.getMainLooper());
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.w("FCH_DEBUG", "onError 2");
                                                    startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                                                }
                                            }, 150);
                                        }

                                    }

                                    @Override
                                    public void onPause() throws RemoteException {
                                    }

                                    @Override
                                    public void onResume() throws RemoteException {
                                    }
                                }
                        );
                    }
                }
                else{
                    Log.e("FCH_DEBUG", "END OF SPEAK : " + allTextPronoucedSuccess);

                    if(allTextPronoucedSuccess){
                        //success
                        allTextPronouced(texteToSpeak,  type);
                    }

                    else{
                        setLanguageDetected("");
                        //error
                        if (type.equals("storedResponse")){
                            questionNumber++;
                            notifyObservers("TTS_error;"+texteToSpeak);
                            storedResponse="";
                        }
                        else {
                            questionNumber++;
                            notifyObservers("TTS_error;"+texteToSpeak);
                        }
                    }
                }
            }
        };

        handler_all.postDelayed(delayedTask, 0);
    }
    /**
     * Cette fonction s'exécute lorsque le TTS prononce la réponse du ChatBot.
     */
    public  void allTextPronouced(final String texteToSpeak, String type){

        if (type.equals("timeOutExpired")){
            timeoutExpired = false;

            if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null){
                getChatGptStreamMode().resumeStreaming();
            }
            else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null){
                getCustomGPTStreamMode().resumeStreaming();
            }
            else{
                notifyObservers("playStoredResponse");
            }

        }
        else if (type.equals("storedResponse")){
            questionNumber++;
            notifyObservers("TTS_success;" + texteToSpeak);
            storedResponse="";
            setLanguageDetected("");
        }
        else {
            questionNumber++;
            setLanguageDetected("");
            if (!type.equals("commande") && getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null && !type.equals("INVITATION")){
                getChatGptStreamMode().onTTSEnd();
            }
            else if (!type.equals("commande") && getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null && !type.equals("INVITATION")){
                getCustomGPTStreamMode().onTTSEnd();
            }
            else{
                notifyObservers("TTS_success;" + texteToSpeak);
            }
        }
    }

    public interface TTSCallback {
        void onSpeechCompleted(int nextIndex);
    }

    // Function to speak an article and trigger the callback on completion
    private void speakArticle(String article, int index, TTSCallback callback, Boolean isGoogle, Boolean isOpenAI) {
        Log.i("OpenAITTS", "Speaking article " + (index) + ": " + article);
        if (isGoogle) {
            new AsyncTask<Void, Void, Void>() {
                @SuppressLint("StaticFieldLeak")
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        SystemClock.sleep(2000);
                        getGoogleCloudTTS().start(getParamFromFile("TTS_ApiGoogle_URL", configurationFilePseudo)+ "key=" +getParamFromFile("ApiGoogle_Key", configurationFilePseudo), article);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute();
        }
        else if (isOpenAI) {
            ttsOpenAI = new TtsOpenAI(this, this);
            ttsOpenAI.setVoice(getParamFromFile("TTS_OpenAI_Voice", configurationFilePseudo));
            ttsOpenAI.setModel(getParamFromFile("TTS_OpenAI_Model", configurationFilePseudo));
            ttsOpenAI.setResponseFormat("wav");
            ttsOpenAI.setSpeed(Float.parseFloat(getParamFromFile("TTS_OpenAI_Speed", configurationFilePseudo)));
            ttsOpenAI.setInstructions(getParamFromFile("TTS_OpenAI_Instructions", configurationFilePseudo));
            ttsOpenAI.setStreamFormat("audio");
            new AsyncTask<Void, Void, Void>() {
                @SuppressLint("StaticFieldLeak")
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        SystemClock.sleep(2000);
                        ttsOpenAI.start(getparam(openAIKey), article, new TtsOpenAIListener() {
                            @Override
                            public void onStart() {
                                Log.d("OpenAITTS", "Lecture démarrée isListeningHotw : " + isListeningHotw);
                                Log.d("OpenAITTS", "Lecture démarrée isStartMsg : " + isStartMsg);
                                BuddySDK.UI.stopListenAnimation();
                                try {
                                    if(!isListeningHotw || isStartMsg) {
                                        Log.d("OpenAITTS", "TTS OpenAI: onStart ------- LabialExpression.SPEAK_NEUTRAL");
                                        BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                            }

                            @Override
                            public void onDone() {
                                ttsOpenAI.close();
                                try {
                                    Log.e("OpenAITTS", "TTS OpenAI: onDone ------- LabialExpression.NO_EXPRESSION");
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }

                                Log.d("OpenAITTS", "Lecture terminée");
                            }

                            @Override
                            public void onError() {
                                Log.e("OpenAITTS", "Erreur TTS OpenAI");
                                ttsOpenAI.close();
                                handleTTSError(article);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute();
        }
        else {
            tts_android.speak(article, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
        }
    }

    /**
     * Cette fonction permet de prononcer le texte passé en argument.
     * @param texteToSpeak : message à dire par Buddy.
     * @param expression : jouer un mouvement spécial de la bouche [SPEAK_ANGRY / NO_FACE / SPEAK_HAPPY / SPEAK_NEUTRAL]
     */
    public void speakTTS(final String texteToSpeak, LabialExpression expression, String type) {
        Log.w("TEST_voix","text To Speak : "+texteToSpeak);
        Log.i("TEST_voix","Selected Language in app : "+getCurrentLanguage());
        setLed("speaking");
        setAlreadyChatting(true);
        Log.e("MEHDI", "texteToSpeak " + texteToSpeak);
        currentIndexText = 0;
        Stop_TTS_ReadSpeaker = false;
        allTextPronoucedSuccess = true;
        Log.w(TAG, "speakTTS : " + texteToSpeak);

        currentIndexText = 0;
        Stop_TTS_ReadSpeaker = false;

        if (getCurrentLanguage().equals("en")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_en);
        } else if (getCurrentLanguage().equals("fr")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_fr);
        } else if (getCurrentLanguage().equals("de")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_de);
        } else if (getCurrentLanguage().equals("es")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_es);
        } else {
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_tts_android_indispo_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            toast_tts_android_indispo = translatedText;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_en);
                        }
                    });
        }

        try {
            setTTSAfterDetectingLanguage();
            Log.i("TEST_voix","ConfigFile --> Text_To_Speech_List : "+getParamFromFile("Text_To_Speech_List",configurationFilePseudo));

            if (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && usingReadSpeaker) {
                Log.i("TEST_voix","SPEAK using TTS ReadSpeaker ");
                if (getCurrentLanguage().equals("en")){
                    BuddySDK.Speech.setSpeakerSpeed(Integer.parseInt(getParamFromFile("ReadSpeaker_speed_en",configurationFilePseudo)));
                    BuddySDK.Speech.setSpeakerPitch(Integer.parseInt(getParamFromFile("ReadSpeaker_pitch_en",configurationFilePseudo)));
                }
                else{
                    BuddySDK.Speech.setSpeakerSpeed(Integer.parseInt(getParamFromFile("ReadSpeaker_speed_fr",configurationFilePseudo)));
                    BuddySDK.Speech.setSpeakerPitch(Integer.parseInt(getParamFromFile("ReadSpeaker_pitch_fr",configurationFilePseudo)));
                }
                BuddySDK.Speech.setSpeakerVolume(getSpeakVolume());
                if (BuddySDK.Speech.isReadyToSpeak()) {
                    String texteToSpeak_modified = texteToSpeak;
                    if (texteToSpeak.toLowerCase().contains("content")) {
                        texteToSpeak_modified = texteToSpeak.replaceAll("\\bcontent\\b", "contents");
                    }
                    if (!type.equals("timeOutExpired")) {
                        if (texteToSpeak_modified.contains(";splitNews;")) {
                            texteToSpeakSplitted = texteToSpeak_modified.split(";splitNews;");
                            startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                        } else {
                            // Split the text based on periods and commas
                            texteToSpeakSplitted = texteToSpeak_modified.split("[.,]");
                            Log.e("texteToSpeakSplitted", texteToSpeakSplitted.toString());
                            startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                        }
                    } else {
                        BuddySDK.Speech.startSpeaking(
                                texteToSpeak_modified,
                                expression,
                                new ITTSCallback.Stub() {
                                    @Override
                                    public void onSuccess(String iText) throws RemoteException {
                                        Log.i(TAG, "Succès de prononciation : " + iText);
                                        allTextPronouced(texteToSpeak, type);
                                    }

                                    @Override
                                    public void onError(String iError) throws RemoteException {
                                        Log.e(TAG, "Erreur pendant la prononciation 3 :  " + iError);
                                        if (type.equals("timeOutExpired")) {
                                            timeoutExpired = false;

                                            if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                                                getChatGptStreamMode().resumeStreaming();
                                            } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                                                getCustomGPTStreamMode().resumeStreaming();
                                            } else {
                                                notifyObservers("playStoredResponse");
                                            }

                                        }


                                    }

                                    @Override
                                    public void onPause() throws RemoteException {
                                    }

                                    @Override
                                    public void onResume() throws RemoteException {
                                    }
                                }
                        );
                    }
                }
            }
            else if (getChosenTTS().trim().equalsIgnoreCase("Android") || (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("Android")) && !usingReadSpeaker){
                Log.i("TEST_voix","SPEAK using TTS Android");
                setSpeaking(true);
                if(!isAppInstalled(getApplicationContext(),"com.google.android.tts")) {
                    showToast(toast_tts_android_indispo);
                }
                if (texteToSpeak.contains(";splitNews;")) {
                    String[] articlesArray = texteToSpeak.split(";splitNews;");
                    List<String> articlesList = new ArrayList<>();
                    for (String article : articlesArray) {
                        if (!article.trim().isEmpty()) { // Ignore empty articles
                            articlesList.add(article);
                        }
                    }
                    AtomicInteger currentArticleIndex = new AtomicInteger(0);

                    // Define the callback implementation
                    TTSCallback callback = new TTSCallback() {
                        @Override
                        public void onSpeechCompleted(int nextIndex) {
                            Log.e("MEHDI", "onSpeechCompleted " + nextIndex);
                            if (nextIndex < articlesList.size()) {
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    speakArticle(articlesList.get(nextIndex), nextIndex, this, false, false);
                                }, 2000); // Pause before the next article
                            }
                        }
                    };

                    if (!articlesList.isEmpty()) {
                        speakArticle(articlesList.get(0), 0, callback, false, false);
                    } else {
                        Log.e(TAG, "No articles to speak after splitting");
                        setSpeaking(false);
                    }
                    tts_android.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            try {
                                BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                            } catch (Exception e) {
                                Log.e(TAG, "BuddySDK Exception " + e);
                            }
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            Log.d("Commande", "onDone news : " + currentArticleIndex);
                            int nextIndex = currentArticleIndex.incrementAndGet();
                            if (nextIndex == articlesList.size()) {
                                Log.e(TAG, "onDone android speak   ");
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }

                                if (type.equals("timeOutExpired")) {
                                    timeoutExpired = false;

                                    if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                                        getChatGptStreamMode().resumeStreaming();
                                    } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                                        getCustomGPTStreamMode().resumeStreaming();
                                    } else {
                                        notifyObservers("playStoredResponse");
                                    }

                                } else if (type.equals("storedResponse")) {
                                    questionNumber++;
                                    notifyObservers("TTS_success;" + texteToSpeak);
                                    storedResponse = "";
                                    setLanguageDetected("");
                                } else {
                                    questionNumber++;
                                    setLanguageDetected("");
                                    if (!type.equals("commande") && getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null && !type.equals("INVITATION")) {
                                        Log.e("MODE_STREAM", "call onTTSEnd  speakTTS");
                                        getChatGptStreamMode().onTTSEnd();
                                    } else if (!type.equals("commande") && getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null && !type.equals("INVITATION")) {
                                        getCustomGPTStreamMode().onTTSEnd();
                                    } else {
                                        notifyObservers("TTS_success;" + texteToSpeak);
                                    }
                                }
                            } else {
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                                callback.onSpeechCompleted(nextIndex);// Trigger the callback
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.e(TAG, "Erreur pendant la prononciation " + utteranceId);
                            if (type.equals("timeOutExpired")) {
                                timeoutExpired = false;
                                if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                                    getChatGptStreamMode().resumeStreaming();
                                } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                                    getCustomGPTStreamMode().resumeStreaming();
                                } else {
                                    notifyObservers("playStoredResponse");
                                }
                            } else if (type.equals("storedResponse")) {
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                                questionNumber++;
                                notifyObservers("TTS_error;" + texteToSpeak);
                                storedResponse = "";
                                setLanguageDetected("");
                            } else {
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                                questionNumber++;
                                notifyObservers("TTS_error;" + texteToSpeak);
                                setLanguageDetected("");
                            }
                        }
                    });
                } else {
                    // Lecture normale
                    int result = tts_android.speak(texteToSpeak, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
                    if (result == -1) {
                        notifyObservers("TTS_error;" + texteToSpeak);
                    } else {
                        tts_android.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                            }

                            @Override
                            public void onDone(String utteranceId) {
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }

                                if (type.equals("timeOutExpired")) {
                                    timeoutExpired = false;

                                    if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                                        getChatGptStreamMode().resumeStreaming();
                                    } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                                        getCustomGPTStreamMode().resumeStreaming();
                                    } else {
                                        notifyObservers("playStoredResponse");
                                    }

                                } else if (type.equals("storedResponse")) {
                                    questionNumber++;
                                    notifyObservers("TTS_success;" + texteToSpeak);
                                    storedResponse = "";
                                    setLanguageDetected("");
                                } else {
                                    questionNumber++;
                                    setLanguageDetected("");
                                    if (!type.equals("commande") && getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null && !type.equals("INVITATION")) {
                                        getChatGptStreamMode().onTTSEnd();
                                    } else if (!type.equals("commande") && getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null && !type.equals("INVITATION")) {
                                        getCustomGPTStreamMode().onTTSEnd();
                                    } else {
                                        notifyObservers("TTS_success;" + texteToSpeak);
                                    }
                                }


                            }

                            @Override
                            public void onError(String utteranceId) {
                                Log.e(TAG, "Erreur pendant la prononciation 1 " + utteranceId);
                                if (type.equals("timeOutExpired")) {
                                    timeoutExpired = false;

                                    if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                                        getChatGptStreamMode().resumeStreaming();
                                    } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                                        getCustomGPTStreamMode().resumeStreaming();
                                    } else {
                                        notifyObservers("playStoredResponse");
                                    }

                                } else if (type.equals("storedResponse")) {
                                    try {
                                        BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                    } catch (Exception e) {
                                        Log.e(TAG, "BuddySDK Exception  " + e);
                                    }
                                    questionNumber++;
                                    notifyObservers("TTS_error;" + texteToSpeak);
                                    storedResponse = "";
                                    setLanguageDetected("");

                                } else {
                                    try {
                                        BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                    } catch (Exception e) {
                                        Log.e(TAG, "BuddySDK Exception  " + e);
                                    }
                                    questionNumber++;
                                    notifyObservers("TTS_error;" + texteToSpeak);
                                    setLanguageDetected("");

                                }
                            }
                        });
                    }
                }
            }
            else if (getChosenTTS().trim().equalsIgnoreCase("ApiGoogle") || (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle") && !usingReadSpeaker)) {
                Log.i("TEST_voix","SPEAK using TTS ApiGoogle");
                String languageToUseInApiGoogle = "";
                if (!getLanguageDetected().equals("")) {
                    if(isLangSupportedByMLKit(getLanguageDetected())){
                        languageToUseInApiGoogle = getLanguageDetected();
                        Log.i("TEST_voix", "getLanguageDetected() google  " + getLanguageDetected() );
                    }
                    else{
                        languageToUseInApiGoogle = getCurrentLanguage();
                        Log.i("TEST_voix", "getCurrentLanguage() google " + getCurrentLanguage() );
                    }
                } else {
                    languageToUseInApiGoogle = getCurrentLanguage();
                    Log.i("TEST_voix", "getCurrentLanguage() google 1 " + getCurrentLanguage() );
                }

                List<String> googleTTSLangs = getLanguageCodeForDisponibleLangue("Language_Code_Used_In_GoogleCloud_TTS");
                String fullLangCode = null;
                List<String> mlkitLangs = getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
                int index = mlkitLangs.indexOf(languageToUseInApiGoogle);
                Log.i("TEST_voix", "languageToUseInApiGoogle : " + languageToUseInApiGoogle
                        + "\ngoogleTTSLangs : " + googleTTSLangs+ "\nmlkitLangs : " + mlkitLangs+ "\nindex : " + index);

                if (index == -1){
                    List<String> langsAlternative = Arrays.asList("fr", "en", "es", "de", "it", "ja", "ar", "cmn", "da", "nl", "nb");
                    index = langsAlternative.indexOf(languageToUseInApiGoogle);
                }
                if (index >= 0 && index < googleTTSLangs.size()) {
                    fullLangCode = googleTTSLangs.get(index);
                    Log.i("TEST_voix", "fullLangCode google : " + fullLangCode + "\nindex : " + index);
                }

                usingReadSpeaker = false;
                Log.i("TEST_voix", "fullLangCode google : " + fullLangCode );
                speakGoogleCloudTTS(fullLangCode, texteToSpeak, type);
            }
            else if (getChosenTTS().trim().equalsIgnoreCase("OpenAI") || (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("OpenAI") && !usingReadSpeaker)) {
                Log.i("OpenAITTS","SPEAK using TTS OpenAI");

                usingReadSpeaker = false;
                speakOpenAITTS(texteToSpeak, type);

            }

            } catch (Exception e) {
            Log.e(TAG, "Exception pendant la prononciation : " + e);
            notifyObservers("TTS_exception;" + texteToSpeak);
        }

    }

    public volatile boolean isMouthMessagePlaying = false;

    /**
     * Cette fonction permet d'arrêter la prononciation.
     * @param explicit true = arrêt explicite (toujours appliqué),
     *                 false = arrêt automatique (ignoré si un message Mouth est en cours)
     */
    public void stopTTS(boolean explicit) {
        if (!explicit && isMouthMessagePlaying) {
            Log.w(TAG, "stopTTS(false) ignoré : message Mouth en cours");
            return;
        }
        rawStopTTS();
    }

    /**
     * Cette fonction permet d'arrêter la prononciation
     */
    public void stopTTS() {
        if (isMouthMessagePlaying) {
            Log.w(TAG, "stopTTS() ignoré : message Mouth en cours");
            return;
        }
        rawStopTTS();
    }

    private void rawStopTTS() {
        Stop_TTS_ReadSpeaker = true;
        Log.w(TAG, "stopTTS");
        Log.w(TAG, "stopTTS_STACK", new Throwable("stopTTS caller"));
        Stop_TTS_ReadSpeaker = true;
        try {
            if (BuddySDK.Speech.isSpeaking()) {
                BuddySDK.Speech.stopSpeaking();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant l'arrêt de la prononciation TTS : "+e);
        }
        if (tts_android!= null){
            tts_android.stop();
        }
        if (googleCloudTTS != null ){
            googleCloudTTS.stop();
        }
        if (ttsOpenAI != null ){
            ttsOpenAI.stop();
        }
        setLanguageDetected("");
    }

    /**
     * Cette méthode permet d'inialiser le TTS selon la langue du robot
     */
    public void setTTSAfterDetectingLanguage(){
        if (!getLanguageDetected().equals("")){
            if(isLangSupportedByMLKit(getLanguageDetected())){
                setTTSLanguage(getLanguageDetected());
                Log.i("TEST_voix", "getLanguageDetected() " + getLanguageDetected() );
            }
            else{
                setTTSLanguage(getCurrentLanguage());
                Log.i("TEST_voix", "getCurrentLanguage() 1 " + getCurrentLanguage() );
            }
        }
        else{
            setTTSLanguage(getCurrentLanguage());
            Log.i("TEST_voix", "getCurrentLanguage() 2 " + getCurrentLanguage() );
        }
    }

    private boolean isLangSupportedByMLKit(String langCode) {
        String supportedLangs = getParamFromFile("Language_Code_Used_In_Mlkit", configurationFilePseudo);
        if (supportedLangs == null || supportedLangs.trim().isEmpty()) {
            Log.w("MLKit", "Aucune langue configurée dans Language_Code_Used_In_Mlkit");
            return false;
        }

        // Ex: "fr/en/es" → ["fr", "en", "es"]
        String[] supportedLangArray = supportedLangs.trim().split("/");
        for (String supported : supportedLangArray) {
            if (supported.trim().equalsIgnoreCase(langCode.trim())) {
                Log.d("MLKit", "Langue MLKit supportée : " + langCode);
                return true;
            }
        }

        Log.d("MLKit", "Langue MLKit non supportée : " + langCode);
        return false;
    }

    private String[] getAppVersion(String packageName) {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
            String versionName = packageInfo.versionName;
            String versionCode = String.valueOf(packageInfo.getLongVersionCode());
            return new String[]{versionName, versionCode};
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getBuddyOSVersionMajorMinor(String fullVersion) {
        if (fullVersion == null || fullVersion.isEmpty()) return "";

        String[] parts = fullVersion.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        } else {
            return fullVersion; // retourne tel quel si pas de deux parties
        }
    }

    public String getReadSpeakerVoiceFromLangCode(String shortLangCode) {
        // Lire les paramètres
        String mlkitCodes = getParamFromFile("Language_Code_Used_In_Mlkit", configurationFilePseudo);
        String voices = getParamFromFile("Readspeaker_voices", configurationFilePseudo);
        Log.i("TEST_voix", "getReadSpeakerVoiceFromLangCode");

        if (mlkitCodes == null || voices == null) return null;

        // Nettoyage des chaînes : suppression des espaces superflus
        mlkitCodes = mlkitCodes.trim().replaceAll("\\s*/\\s*", "/");
        voices = voices.trim().replaceAll("\\s*/\\s*", "/");

        String[] langCodes = mlkitCodes.split("/");
        String[] voiceList = voices.split("/", -1);

        Log.i("TEST_voix", "getReadSpeakerVoiceFromLangCode 1 " + new Gson().toJson(langCodes));
        Log.i("TEST_voix", "getReadSpeakerVoiceFromLangCode 2 " + new Gson().toJson(voiceList));

        for (int i = 0; i < langCodes.length; i++) {
            String langCode = langCodes[i].trim();
            if (langCode.equalsIgnoreCase(shortLangCode)) {
                Log.i("TEST_voix", "langcode " + langCode +" shortLangCode "+shortLangCode);
                Log.i("TEST_voix", "getReadSpeakerVoiceFromLangCode 3: " + i +" voiceList.length "+voiceList.length);
                if (i < voiceList.length) {
                    String voice = voiceList[i].trim();
                    Log.i("TEST_voix", "getReadSpeakerVoiceFromLangCode 4: " + voice+" voiceList.length "+voiceList.length);
                    return voice.isEmpty() ? null : voice;
                }
            }
        }

        return null; // Code langue non trouvé ou voix absente
    }

    public String checkReadSpeakerVoices(String voice) {
        String packageName = "com.bfr.buddy.core";
        String[] version = getAppVersion(packageName);
        String versionBuddyOS;

        if (version != null) {
            versionBuddyOS = version[0];
            String versionKey = getBuddyOSVersionMajorMinor(versionBuddyOS); // ex: "1.5"
            String voiceKey = "BuddyOS_" + versionKey + "_voices";
            Log.d("TEST_voix", "App found version BuddyOS: " + versionBuddyOS + " -> try using key: " + voiceKey);

            String voicesParam = getParamFromFile(voiceKey, configurationFilePseudo);

            // decide fallback target based on version comparator (major.minor)
            boolean isAtLeast14 = false;
            try {
                String[] parts = versionKey.split("\\.");
                int major = Integer.parseInt(parts[0]);
                int minor = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
                isAtLeast14 = (major > 1) || (major == 1 && minor >= 4);
            } catch (Exception ignored) {
                // if parsing fails, keep isAtLeast14=false by default (treat as < 1.4)
            }

            if (voicesParam == null || voicesParam.trim().isEmpty()) {
                String fallbackKey = isAtLeast14 ? "BuddyOS_1.4_voices" : "BuddyOS_1.3_voices";
                String fallbackVoices = getParamFromFile(fallbackKey, configurationFilePseudo);

                if (fallbackVoices != null && !fallbackVoices.trim().isEmpty()) {
                    Log.i("TEST_voix", "No voices for key: " + voiceKey +
                            " -> falling back to: " + fallbackKey);
                    voiceKey = fallbackKey;
                    voicesParam = fallbackVoices;
                } else {
                    Log.w("TEST_voix", "No voices found for key: " + voiceKey +
                            " and no fallback available (" + fallbackKey + ").");
                    return null;
                }
            } else {
                Log.d("TEST_voix", "Using voices for exact key: " + voiceKey +
                        " (BuddyOS=" + versionBuddyOS + ")");
            }

            voicesParam = voicesParam
                    .trim()
                    .replaceAll("\\s*/\\s*", "/")
                    .replaceAll("\\s*:\\s*", ":")
                    .replaceAll("\\s*,\\s*", ",");

            // Ex: "fr:roxane/en:kate,mark,alice/ar:amir,yasmin"
            String[] languageEntries = voicesParam.split("/");

            for (String entry : languageEntries) {
                if (entry.contains(":")) {
                    String[] parts = entry.split(":", 2); // Only split on first colon
                    String lang = parts[0].trim();
                    String[] voices = parts[1].split(",");

                    for (String v : voices) {
                        if (v.trim().equalsIgnoreCase(voice.trim())) {
                            Log.d("TEST_voix", "Voix trouvée : " + voice + " pour langue : " + lang);
                            return voice;
                        }
                    }
                }
            }

            Log.w("TEST_voix", "Voix non trouvée : " + voice);
            return null;

        } else {
            Log.e("TEST_voix", "BuddyOS app not found or version retrieval failed.");
            return null;
        }
    }

    public void setTTSLanguage(String language){

        Log.i("TEST_voix","Detected Language : "+language);

        if (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker")){
            Log.i("TEST_voix","ReadSpeaker chosen  ");
            usingReadSpeaker = true;
            String voice = getReadSpeakerVoiceFromLangCode(language);
            Log.i("TEST_voix","ReadSpeaker chosen 1 "+voice);
            if(voice!=null && !voice.isEmpty()){
                Log.i("TEST_voix","ReadSpeaker chosen 1 ");
                String selectedVoice = checkReadSpeakerVoices(voice);
                if(selectedVoice!=null && !selectedVoice.isEmpty()){
                    Log.i("TEST_voix","ReadSpeaker chosen 2 "+selectedVoice);
                    BuddySDK.Speech.setSpeakerVoice(selectedVoice);
                }
                else{
                    Log.i("TEST_voix","ReadSpeaker chosen 3 ");
                    //todo
                    usingReadSpeaker = false;
                    initFallbackTTS(language);
                }
            }
            else{
                Log.i("TEST_voix","ReadSpeaker  chosen empty voice  ");
                //todo
                usingReadSpeaker = false;
                initFallbackTTS(language);
            }
        }
        else {
            Log.i("TEST_voix","ReadSpeaker not chosen 1 ");
            usingReadSpeaker = false;
            initFallbackTTS(language);
        }
    }

    private void initFallbackTTS(String language){

        try {
            List<String> mlkitLangs = getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
            List<String> ttsLangs = getLanguageCodeForDisponibleLangue("Language_Code_Used_In_TTS_Android");


            String fullLangCode = null;
            int index = mlkitLangs.indexOf(language);
                if (index != -1 && index < ttsLangs.size()) {
                    fullLangCode = ttsLangs.get(index);
                }

            String[] parts = fullLangCode.split("-");
            Locale locale = new Locale(parts[0].trim(), parts[1].trim());

            usingReadSpeaker = false;

            String pitchKey;
            String speedKey;
            if ("fr".equalsIgnoreCase(language)) {
                pitchKey = "TTS_Android_pitch_fr";
                speedKey = "TTS_Android_speed_fr";
            } else {
                pitchKey = "TTS_Android_pitch_en";
                speedKey = "TTS_Android_speed_en";
            }

            float pitch = getConvertedPitchAndSpeedValue(
                    Integer.parseInt(getParamFromFile(pitchKey, configurationFilePseudo))
            );
            float speed = getConvertedPitchAndSpeedValue(
                    Integer.parseInt(getParamFromFile(speedKey, configurationFilePseudo))
            );

            tts_android.setPitch(pitch);
            tts_android.setSpeechRate(speed);
            tts_android.setLanguage(locale);

            Log.i("TEST_voix", "TTS initialisé avec : " + locale.toString());

        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant l'initialisation de la langue TTS : " + e.getMessage(), e);
        }
    }

    public String getLanguageVoice(String languageCode) {
        // Parse voice configuration
        String voice = "";
        Log.i("TTSG", "TTS_ApiGoogle_Language_Voice :: " + getParamFromFile("TTS_ApiGoogle_Language_Voice", configurationFilePseudo));
        String voiceConfig = getParamFromFile("TTS_ApiGoogle_Language_Voice", configurationFilePseudo);
        if(voiceConfig!=null){
            voiceConfig = voiceConfig.trim().replaceAll(" ","");
        }
        Log.i("TTSG", "TTS_ApiGoogle_Language_Voice " + voiceConfig);

        Log.i("TTSG", "TTS_ApiGoogle_Voice_Type :" + getParamFromFile("TTS_ApiGoogle_Voice_Type", configurationFilePseudo));
        String defaultVoiceType = getParamFromFile("TTS_ApiGoogle_Voice_Type", configurationFilePseudo);
        if(defaultVoiceType!=null){
            defaultVoiceType = defaultVoiceType.trim();
        }

        Map<String, String> voiceMap = new HashMap<>();
        if (voiceConfig!=null && !voiceConfig.isEmpty()) {
            String[] entries = voiceConfig.replace("[", "").split("],");
            for (String entry : entries) {
                String[] parts = entry.replace("]", "").split(":");
                if (parts.length == 2) {
                    // Normalize by trimming spaces and converting to lowercase for the key
                    String key = parts[0].replace(" ", "").toLowerCase();
                    // Format voice name
                    String value = formatVoiceName(parts[1].replace(" ", ""));
                    voiceMap.put(key, value);
                }
            }
        }

        // Normalize language code for lookup
        String languageKey = languageCode.split("-")[0].toLowerCase();

        // Check if a specific voice is configured for the language
        if (voiceMap.containsKey(languageKey)) {
            voice = languageCode + "-" + voiceMap.get(languageKey);
        } else if (defaultVoiceType!=null && !defaultVoiceType.isEmpty()) {
            voice = languageCode + "-" + formatVoiceName(defaultVoiceType.replace(" ", ""))+ "-A";
        } else {
            return "";
        }
        return voice;
    }

    /**
     * Formats the voice name to have the first letter of each part capitalized and removes spaces.
     * For example, "wavenet-c" or "WAVENET-C" becomes "Wavenet-C".
     */
    private String formatVoiceName(String voiceName) {
        String[] parts = voiceName.split("-");
        StringBuilder formattedName = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (!part.isEmpty()) {
                formattedName.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            if (i < parts.length - 1) {
                formattedName.append("-");
            }
        }
        return formattedName.toString();
    }

    public void speakGoogleCloudTTS(String languageCode, String texteToSpeak, String type) {
        Log.e("MRA", "speakGoogleCloudTTS  LanguageCode  " + languageCode);
        String voice = "";
        String languageVoice = getLanguageVoice(languageCode);
        Log.i("TTSG", "  LanguageCode  " + languageCode + " voice name " + languageVoice);
        if (!languageVoice.isEmpty()) {
            if (getVoiceList() != null) {
                for (String voiceName : getVoiceList().getVoiceNames(languageCode)) {
                    if (voiceName.equals(languageVoice)) {
                        Log.i("TTSG", "  voiceName exist --- " + languageVoice);
                        voice = languageVoice;
                        break;
                    }
                    voice = voiceName;
                }
                Log.i("TTSG", "  voice   --- " + voice);
            }
            if (voice.isEmpty()) {
                voice = languageVoice;
            }
        } else {
            Log.i("TEST_voix", "  code language null  ");
            languageCode = "en-US";
            voice = getLanguageVoice(languageCode);
            if (voice.isEmpty()) {
                voice = "en-US-Standard-C";
            }
        }
        Log.i("TEST_voix", "  final voice name " + voice);
        setSpeaking(true);
        String finalLanguageCode = languageCode;
        String finalVoice = voice;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {

                    float pitch;
                    float speed;
                    if(finalLanguageCode.contains("fr-")){
                        pitch = getConvertedPitchValueGoogle(Integer.parseInt(getParamFromFile("TTS_ApiGoogle_pitch_fr",configurationFilePseudo)));
                        speed = getConvertedSpeedValueGoogle(Integer.parseInt(getParamFromFile("TTS_ApiGoogle_speed_fr",configurationFilePseudo)));
                    }
                    else{
                        pitch = getConvertedPitchValueGoogle(Integer.parseInt(getParamFromFile("TTS_ApiGoogle_pitch_en",configurationFilePseudo)));
                        speed = getConvertedSpeedValueGoogle(Integer.parseInt(getParamFromFile("TTS_ApiGoogle_speed_en",configurationFilePseudo)));
                    }

                    Log.d("DEBUG_TTS_Google","LanguageCode : "+finalLanguageCode + " , Voice : " + finalVoice);
                    Log.d("MYA_TTS_Google","LanguageCode : "+finalLanguageCode + " , Voice : " + finalVoice);
                    TtsGoogleC.VoiceSelectionRaw voice = new TtsGoogleC.VoiceSelectionRaw();
                    voice.languageCode = finalLanguageCode;
                    voice.name = finalVoice;
                    voice.ssmlGender = "NEUTRAL";   // équivalent par défaut
                    TtsGoogleC.AudioConfigRaw audio = new TtsGoogleC.AudioConfigRaw();
                    audio.audioEncoding = "MP3";    // équivalent AudioEncoding.MP3
                    audio.speakingRate = speed;
                    audio.pitch = pitch;

                    getGoogleCloudTTS().setVoiceSelectionParams(voice).setAudioConfig(audio);

                    if (texteToSpeak.contains(";splitNews;")) {
                        String[] articlesArray = texteToSpeak.split(";splitNews;");
                        List<String> articlesList = new ArrayList<>();
                        for (String article : articlesArray) {
                            if (!article.trim().isEmpty()) { // Ignore empty articles
                                articlesList.add(article);
                            }
                        }
                        AtomicInteger currentArticleIndex = new AtomicInteger(0);

                        // Define the callback implementation
                        TTSCallback callback1 = new TTSCallback() {
                            @Override
                            public void onSpeechCompleted(int nextIndex) {
                                Log.d("googleNews", " onSpeechCompleted ");
                                if (nextIndex < articlesList.size()) {
                                    try {
                                        speakArticle(articlesList.get(nextIndex), nextIndex, this, true, false);
                                    } catch (Exception e) {
                                        Log.e("googleNews", "Error while speaking article: " + e.getMessage());
                                    }
                                }
                            }
                        };
                        speakArticle(articlesList.get(0), 0, callback1, true, false);

                        getGoogleCloudTTS().setTtsListener(new TtsGoogleApiListener() {
                            @Override
                            public void onStart() {
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                            }

                            @Override
                            public void onDone() {
                                Log.d("googleNews", "onDone ");

                                int nextIndex = currentArticleIndex.incrementAndGet();
                                if (nextIndex == articlesList.size()) {
                                    getGoogleCloudTTS().close();
                                    try {
                                        BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                    } catch (Exception e) {
                                        Log.e(TAG, "BuddySDK Exception  " + e);
                                    }

                                    if (type.equals("timeOutExpired")) {
                                        timeoutExpired = false;

                                        if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                                            getChatGptStreamMode().resumeStreaming();
                                        } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                                            getCustomGPTStreamMode().resumeStreaming();
                                        } else {
                                            notifyObservers("playStoredResponse");
                                        }

                                    } else if (type.equals("storedResponse")) {
                                        questionNumber++;
                                        notifyObservers("TTS_success;" + texteToSpeak);
                                        storedResponse = "";
                                        setLanguageDetected("");
                                    } else {
                                        questionNumber++;
                                        setLanguageDetected("");
                                        if (!type.equals("commande") && getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null && !type.equals("INVITATION")) {
                                            getChatGptStreamMode().onTTSEnd();
                                        } else if (!type.equals("commande") && getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null && !type.equals("INVITATION")) {
                                            getCustomGPTStreamMode().onTTSEnd();
                                        } else {
                                            notifyObservers("TTS_success;" + texteToSpeak);
                                        }
                                    }
                                } else {
                                    try {
                                        BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                    } catch (Exception e) {
                                        Log.e(TAG, "BuddySDK Exception  " + e);
                                    }
                                    callback1.onSpeechCompleted(nextIndex);// Trigger the callback
                                }

                            }

                            @Override
                            public void onError() {
                                Log.e("googleNews", "speakGoogleCloudTTS  onError-----------  ");
                            }
                        });

                    } else {
                        getGoogleCloudTTS().setTtsListener(new TtsGoogleApiListener() {
                            @Override
                            public void onStart() {
                                try {
                                    if(!isListeningHotw)BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                            }

                            @Override
                            public void onDone() {
                                getGoogleCloudTTS().close();
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }

                                if (type.equals("timeOutExpired")) {
                                    timeoutExpired = false;

                                    if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                                        getChatGptStreamMode().resumeStreaming();
                                    } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                                        getCustomGPTStreamMode().resumeStreaming();
                                    } else {
                                        notifyObservers("playStoredResponse");
                                    }

                                } else if (type.equals("storedResponse")) {
                                    questionNumber++;
                                    notifyObservers("TTS_success;" + texteToSpeak);
                                    storedResponse = "";
                                    setLanguageDetected("");
                                } else {
                                    questionNumber++;
                                    setLanguageDetected("");
                                    if (!type.equals("commande") && getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null && !type.equals("INVITATION")) {
                                        getChatGptStreamMode().onTTSEnd();
                                    } else if (!type.equals("commande") && getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null && !type.equals("INVITATION")) {
                                        getCustomGPTStreamMode().onTTSEnd();
                                    } else {
                                        notifyObservers("TTS_success;" + texteToSpeak);
                                    }
                                }
                            }

                            @Override
                            public void onError() {
                                Log.e("MRA", "speakGoogleCloudTTS  onError-----------  ");
                            }
                        });
                        getGoogleCloudTTS().start(getParamFromFile("TTS_ApiGoogle_URL", configurationFilePseudo)+ "key=" +getParamFromFile("ApiGoogle_Key", configurationFilePseudo), texteToSpeak);

                    }
                }
                catch (Exception e) {
                    Log.e("MRA", "speakGoogleCloudTTS  Exception-----------  " + e);
                    Log.e(TAG, "Exception " + e);
                    try {
                        int startIndex = e.getMessage().indexOf('{');
                        // Trouver la fin de la réponse JSON
                        int endIndex = e.getMessage().lastIndexOf('}') + 1;
                        // Extraire la réponse JSON
                        String jsonContent = e.getMessage().substring(startIndex, endIndex);
                        JsonObject errorLOG = JsonParser.parseString(jsonContent).getAsJsonObject();

                        //Mettre   le fichier le plus récent reçu
                        String fileName = "ERROR-LOG";
                        File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");
                        if (file1.exists() && file1.isFile()) {
                            file1.delete();
                        }
                        FileWriter fileWriter = new FileWriter(file1);
                        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                        String jsonStringF = gson.toJson(errorLOG);
                        fileWriter.write(jsonStringF);
                        fileWriter.close();
                        String errorTXT = new Date().toString() + ", GoogleCloudTTSERROR,ERROR CODE= " + errorLOG.getAsJsonObject("error").get("code") + ", ERROR Body{ message= " + errorLOG.getAsJsonObject("error").get("message") + ", status= " + errorLOG.getAsJsonObject("error").get("status") + "}" + System.getProperty("line.separator");
                        File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");
                        FileWriter fileWriter2 = new FileWriter(file2, true);
                        fileWriter2.write(errorTXT);
                        fileWriter2.close();

                    } catch (IOException ej) {
                        e.printStackTrace();
                    }
                    getGoogleCloudTTS().close();
                    if (type.equals("timeOutExpired")) {
                        timeoutExpired = false;

                        if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                            getChatGptStreamMode().resumeStreaming();
                        } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                            getCustomGPTStreamMode().resumeStreaming();
                        } else {
                            notifyObservers("playStoredResponse");
                        }

                    } else if (type.equals("storedResponse")) {
                        try {
                            if(!isListeningHotw) BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                        } catch (Exception ej) {
                            Log.e(TAG, "BuddySDK Exception  " + ej);
                        }
                        questionNumber++;
                        notifyObservers("TTS_error;" + texteToSpeak);
                        storedResponse = "";
                        setLanguageDetected("");

                    } else {
                        try {
                            if(!isListeningHotw) BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                        } catch (Exception ej) {
                            Log.e(TAG, "BuddySDK Exception  " + ej);
                        }
                        questionNumber++;
                        notifyObservers("TTS_error;" + texteToSpeak);
                        setLanguageDetected("");

                    }

                }
                return null;
            }
        }.execute();
    }

    public void speakOpenAITTS(String texteToSpeak, String type) {
        ttsOpenAI = new TtsOpenAI(this, this);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {

                    // Configuration voix
                    ttsOpenAI.setVoice(getParamFromFile("TTS_OpenAI_Voice", configurationFilePseudo));
                    ttsOpenAI.setModel(getParamFromFile("TTS_OpenAI_Model", configurationFilePseudo));
                    ttsOpenAI.setResponseFormat("wav");
                    ttsOpenAI.setSpeed(Float.parseFloat(getParamFromFile("TTS_OpenAI_Speed", configurationFilePseudo)));
                    ttsOpenAI.setStreamFormat("audio");
                    ttsOpenAI.setInstructions(getParamFromFile("TTS_OpenAI_Instructions", configurationFilePseudo));

                    // Gestion splitNews
                    if (texteToSpeak.contains(";splitNews;")) {
                        Log.d("OpenAITTS", "----------texte contains splitNews----------");
                        Log.d("OpenAITTS", "texteToSpeak :"+texteToSpeak);
                        String[] articlesArray = texteToSpeak.split(";splitNews;");
                        List<String> articlesList = new ArrayList<>();
                        for (String article : articlesArray) {
                            if (!article.trim().isEmpty()) articlesList.add(article);
                        }

                        Log.d("OpenAITTS", "articlesArray :"+articlesList);

                        AtomicInteger currentArticleIndex = new AtomicInteger(0);

                        TTSCallback callback2 = new TTSCallback() {
                            @Override
                            public void onSpeechCompleted(int nextIndex) {
                                Log.d("googleNews", " onSpeechCompleted ");
                                if (nextIndex < articlesList.size()) {
                                    try {
                                        speakArticle(articlesList.get(nextIndex), nextIndex, this, false, true);
                                    } catch (Exception e) {
                                        Log.e("googleNews", "Error while speaking article: " + e.getMessage());
                                    }
                                }
                            }
                        };
                        speakArticle(articlesList.get(0), 0, callback2, false, true);

                        //ttsOpenAI.setTtsListener();
                        ttsOpenAI.start(getparam(openAIKey), articlesList.get(0),new TtsOpenAIListener() {
                            @Override
                            public void onStart() {
                                // Ici tu peux mettre une expression labiale
                                Log.d("OpenAITTS", "Lecture démarrée");
                                BuddySDK.UI.stopListenAnimation();
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                            }

                            @Override
                            public void onDone() {
                                Log.d("googleNews", "onDone ");

                                int nextIndex = currentArticleIndex.incrementAndGet();
                                if (nextIndex == articlesList.size()) {
                                    ttsOpenAI.close();
                                    try {
                                        Log.e("OpenAITTS", "TTS OpenAI: onDone ------- LabialExpression.NO_EXPRESSION");
                                        BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                    } catch (Exception e) {
                                        Log.e(TAG, "BuddySDK Exception  " + e);
                                    }

                                    if (type.equals("timeOutExpired")) {
                                        timeoutExpired = false;

                                        if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                                            getChatGptStreamMode().resumeStreaming();
                                        } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                                            getCustomGPTStreamMode().resumeStreaming();
                                        } else {
                                            notifyObservers("playStoredResponse");
                                        }

                                    } else if (type.equals("storedResponse")) {
                                        questionNumber++;
                                        notifyObservers("TTS_success;" + texteToSpeak);
                                        storedResponse = "";
                                        setLanguageDetected("");
                                    } else {
                                        questionNumber++;
                                        setLanguageDetected("");
                                        if (!type.equals("commande") && getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null && !type.equals("INVITATION")) {
                                            getChatGptStreamMode().onTTSEnd();
                                        } else if (!type.equals("commande") && getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null && !type.equals("INVITATION")) {
                                            getCustomGPTStreamMode().onTTSEnd();
                                        } else {
                                            notifyObservers("TTS_success;" + texteToSpeak);
                                        }
                                    }
                                } else {
                                    try {
                                        BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                    } catch (Exception e) {
                                        Log.e(TAG, "BuddySDK Exception  " + e);
                                    }
                                    callback2.onSpeechCompleted(nextIndex);// Trigger the callback
                                }

                            }

                            @Override
                            public void onError() {
                                Log.e("OpenAITTS", "Erreur TTS OpenAI");
                                ttsOpenAI.close();
                                handleTTSError(texteToSpeak);//todo : handleTTSError
                            }
                        });

                    } else {

                        Log.d("OpenAITTS", "---------- Texte simple ----------");
                        Log.d("OpenAITTS", "texteToSpeak :"+texteToSpeak);

                        ttsOpenAI.start(getparam(openAIKey), texteToSpeak, new TtsOpenAIListener() {
                            @Override
                            public void onStart() {
                                Log.d("OpenAITTS", "Lecture démarrée isListeningHotw 1: " + isListeningHotw);
                                Log.d("OpenAITTS", "Lecture démarrée isStartMsg 1 : " + isStartMsg);
                                BuddySDK.UI.stopListenAnimation();
                                try {
                                    if(!isListeningHotw || isStartMsg) {
                                        Log.d("OpenAITTS", "TTS OpenAI: onStart ---1---- LabialExpression.SPEAK_NEUTRAL");
                                        BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                            }

                            @Override
                            public void onDone() {
                                ttsOpenAI.close();
                                try {
                                    Log.e("OpenAITTS", "TTS OpenAI: onDone ------- LabialExpression.NO_EXPRESSION");
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }

                                if (type.equals("timeOutExpired")) {
                                    timeoutExpired = false;

                                    if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                                        getChatGptStreamMode().resumeStreaming();
                                    } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                                        getCustomGPTStreamMode().resumeStreaming();
                                    } else {
                                        notifyObservers("playStoredResponse");
                                    }

                                } else if (type.equals("storedResponse")) {
                                    questionNumber++;
                                    notifyObservers("TTS_success;" + texteToSpeak);
                                    storedResponse = "";
                                    setLanguageDetected("");
                                } else {
                                    questionNumber++;
                                    setLanguageDetected("");
                                    if (!type.equals("commande") && getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null && !type.equals("INVITATION")) {
                                        getChatGptStreamMode().onTTSEnd();
                                    } else if (!type.equals("commande") && getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null && !type.equals("INVITATION")) {
                                        getCustomGPTStreamMode().onTTSEnd();
                                    } else {
                                        notifyObservers("TTS_success;" + texteToSpeak);
                                    }
                                }
                                Log.d("OpenAITTS", "Lecture terminée");
                            }

                            @Override
                            public void onError() {
                                Log.e("OpenAITTS", "Erreur TTS OpenAI");
                                ttsOpenAI.close();
                                handleTTSError(texteToSpeak);
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e("OpenAITTS", "Exception TTS OpenAI : " + e.getMessage(), e);
                    Log.e(TAG, "Exception " + e);
                    try {
                        int startIndex = e.getMessage().indexOf('{');
                        // Trouver la fin de la réponse JSON
                        int endIndex = e.getMessage().lastIndexOf('}') + 1;
                        // Extraire la réponse JSON
                        String jsonContent = e.getMessage().substring(startIndex, endIndex);
                        JsonObject errorLOG = JsonParser.parseString(jsonContent).getAsJsonObject();

                        //Mettre   le fichier le plus récent reçu
                        String fileName = "ERROR-LOG";
                        File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");
                        if (file1.exists() && file1.isFile()) {
                            file1.delete();
                        }
                        FileWriter fileWriter = new FileWriter(file1);
                        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                        String jsonStringF = gson.toJson(errorLOG);
                        fileWriter.write(jsonStringF);
                        fileWriter.close();
                        String errorTXT = new Date().toString() + ", GoogleCloudTTSERROR,ERROR CODE= " + errorLOG.getAsJsonObject("error").get("code") + ", ERROR Body{ message= " + errorLOG.getAsJsonObject("error").get("message") + ", status= " + errorLOG.getAsJsonObject("error").get("status") + "}" + System.getProperty("line.separator");
                        File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");
                        FileWriter fileWriter2 = new FileWriter(file2, true);
                        fileWriter2.write(errorTXT);
                        fileWriter2.close();

                    } catch (IOException ej) {
                        e.printStackTrace();
                    }
                    getGoogleCloudTTS().close();
                    if (type.equals("timeOutExpired")) {
                        timeoutExpired = false;

                        if (getparam("Mode_Stream").contains("yes") && getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && getChatGptStreamMode() != null) {
                            getChatGptStreamMode().resumeStreaming();
                        } else if (getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && getCustomGPTStreamMode() != null) {
                            getCustomGPTStreamMode().resumeStreaming();
                        } else {
                            notifyObservers("playStoredResponse");
                        }

                    } else if (type.equals("storedResponse")) {
                        try {
                            if(!isListeningHotw) BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                        } catch (Exception ej) {
                            Log.e(TAG, "BuddySDK Exception  " + ej);
                        }
                        questionNumber++;
                        notifyObservers("TTS_error;" + texteToSpeak);
                        storedResponse = "";
                        setLanguageDetected("");

                    } else {
                        try {
                            if(!isListeningHotw) BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                        } catch (Exception ej) {
                            Log.e(TAG, "BuddySDK Exception  " + ej);
                        }
                        questionNumber++;
                        notifyObservers("TTS_error;" + texteToSpeak);
                        setLanguageDetected("");

                    }
                }
                return null;
            }
        }.execute();
    }


    /**
     * Exemple de gestion d'erreur TTS
     */
    private void handleTTSError(String texte) {
        notifyObservers("TTS_error;" + texte);
    }

    public String getSecondTTSfromTTSList(){
        String[] listTTS= getParamFromFile("Text_To_Speech_List",configurationFilePseudo).split("/");
        if (listTTS.length>1){
            if (listTTS[1].trim().equalsIgnoreCase("Android") || listTTS[1].trim().equalsIgnoreCase("ApiGoogle")){
                return listTTS[1].trim();
            }
            else return "Android";
        }
        else return "Android";
    }
    private float getConvertedPitchAndSpeedValue(int nombre) {
        return getConvertedPitchAndSpeedValue(nombre, 0.5f, 2.0f);
    }

    private float getConvertedPitchValueGoogle(int nombre) {
        return getConvertedPitchAndSpeedValue(nombre, -20.0f, 20.0f);
    }

    private float getConvertedSpeedValueGoogle(int nombre) {
        return getConvertedPitchAndSpeedValue(nombre, 0.25f, 4.0f);
    }

    private float getConvertedPitchAndSpeedValue(int nombre, float valeurMinSortie, float valeurMaxSortie) {
        int valeurMinEntree = 50;
        int valeurMaxEntree = 150;
        // Vérification si le nombre se trouve dans l'intervalle d'entrée
        if (nombre < valeurMinEntree || nombre > valeurMaxEntree) {
            nombre = (valeurMinEntree + valeurMinEntree) / 2;
        }

        // Conversion linéaire : (nombre - minE) / (maxE - minE) → [0,1]
        float ratio = (float) (nombre - valeurMinEntree) / (valeurMaxEntree - valeurMinEntree);

        // Conversion vers l'intervalle de sortie
        float valeurFloat = valeurMinSortie + ratio * (valeurMaxSortie - valeurMinSortie);

        Log.e("TEST_voix", "converted value :nombre= " + nombre + "  converted =" + valeurFloat);
        return valeurFloat;
    }

    private String getFirstFullLanguageCode(String shortLanguageCode) {
        Locale[] locales = Locale.getAvailableLocales();
        Boolean hasThesame =false;
        boolean firstLanguageCode = true;
        String FullLanguageCode="";
        for (Locale locale : locales) {
            if (shortLanguageCode.equalsIgnoreCase(locale.getLanguage()) && !locale.getCountry().isEmpty()) {
                if (firstLanguageCode){
                    firstLanguageCode=false;
                    FullLanguageCode =locale.getLanguage() + "-" + locale.getCountry();
                }
                if (shortLanguageCode.equalsIgnoreCase(locale.getCountry())){
                    hasThesame =true;
                    break;
                }
                Log.e("MMMM","getFirstFullLanguageCode if "+locale.getLanguage() + "-" + locale.getCountry());

            }
        }
        if (hasThesame){
            FullLanguageCode = shortLanguageCode.toLowerCase()+"-"+shortLanguageCode.toUpperCase();
        }
        return FullLanguageCode;
    }
    /**
     * Cette méthode permet d'inialiser le TTS d'android
     */
    public void initTTSAndroid(){
        tts_android = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // TTS is initialized successfully
                    Log.e("TTS_Android","TTS is initialized successfully");
                    notifyObservers("TTSAndroidIsInitialized");
                }else {
                    Log.e("TTS_Android", "TTS Initilization Failed!" + status);
                    notifyObservers("TTSAndroidIsInitialized");

                }

            }
        },"com.google.android.tts");
    }
    public void initTTSGoogleCoud(){
        googleCloudTTS = TtsFactory.create(this, getParamFromFile("ApiGoogle_Key",configurationFilePseudo));
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    voiceList = googleCloudTTS.load(getParamFromFile("ApiGoogle_Key",configurationFilePseudo));
                } catch (Exception e) {
                    Log.e("MRA","load  Exception-----------  "+e);
                    Log.e(TAG,"Exception "+e);
                    try {
                        if (e.getMessage()!=null && e.getMessage().contains("{") && e.getMessage().contains("}")) {
                            int startIndex = e.getMessage().indexOf('{');
                            // Trouver la fin de la réponse JSON
                            int endIndex = e.getMessage().lastIndexOf('}') + 1;
                            // Extraire la réponse JSON
                            String jsonContent = e.getMessage().substring(startIndex, endIndex);
                            JsonObject errorLOG = JsonParser.parseString(jsonContent).getAsJsonObject();

                            //Mettre   le fichier le plus récent reçu
                            String fileName = "ERROR-LOG";
                            File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");
                            if (file1.exists() && file1.isFile()) {
                                file1.delete();
                            }
                            FileWriter fileWriter = new FileWriter(file1);
                            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                            String jsonStringF = gson.toJson(errorLOG);
                            fileWriter.write(jsonStringF);
                            fileWriter.close();
                            String errorTXT = new Date().toString() + ", GoogleCloudTTSERROR,ERROR CODE= " + errorLOG.getAsJsonObject("error").get("code") + ", ERROR Body{ message= " + errorLOG.getAsJsonObject("error").get("message") + ", status= " + errorLOG.getAsJsonObject("error").get("status") + "}" + System.getProperty("line.separator");
                            File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");
                            FileWriter fileWriter2 = new FileWriter(file2, true);
                            fileWriter2.write(errorTXT);
                            fileWriter2.close();
                        }

                    } catch (IOException ej) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.execute();

    }



    public void playUsingReadSpeakerCaseError(String text, ITTSCallbacks ittsCallbacks){
        if(usingReadSpeaker){
            ittsCallbacks.onError("error is in readspeaker not tts_android");
            return;
        }
        Log.i("TEST_voix", "playUsingReadSpeakerCaseError ");

        String langCode = getCurrentLanguage();
        String validatedVoice = "";
        String defaultVoice = getReadSpeakerVoiceFromLangCode(langCode);

        if (defaultVoice != null && !defaultVoice.isEmpty()) {
            validatedVoice = checkReadSpeakerVoices(defaultVoice);
        }

        if (langCode.equals("en")) {
            toast_tts_googleApi_indispo =getString(R.string.toast_tts_googleApi_indispo_en);
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_en);
            if (getChosenTTS().trim().equalsIgnoreCase("ApiGoogle") || (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle"))){
                showToast(toast_tts_googleApi_indispo);
            }
            else{
                showToast(toast_tts_android_indispo);
            }
        }
        else if (langCode.equals("fr")){
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_fr);
            toast_tts_googleApi_indispo =getString(R.string.toast_tts_googleApi_indispo_fr);
            if (getChosenTTS().trim().equalsIgnoreCase("ApiGoogle") || (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle"))){
                showToast(toast_tts_googleApi_indispo);
            }
            else{
                showToast(toast_tts_android_indispo);
            }
        }
        else if (langCode.equals("de")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_de);
            toast_tts_googleApi_indispo =getString(R.string.toast_tts_googleApi_indispo_de);
            if (getChosenTTS().trim().equalsIgnoreCase("ApiGoogle") || (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle"))){
                showToast(toast_tts_googleApi_indispo);
            }
            else{
                showToast(toast_tts_android_indispo);
            }
        }
        else if (langCode.equals("es")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_es);
            toast_tts_googleApi_indispo =getString(R.string.toast_tts_googleApi_indispo_es);
            if (getChosenTTS().trim().equalsIgnoreCase("ApiGoogle") || (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle"))){
                showToast(toast_tts_googleApi_indispo);
            }
            else{
                showToast(toast_tts_android_indispo);
            }
        }
        else{
            if (getChosenTTS().trim().equalsIgnoreCase("ApiGoogle") || (getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle"))){

                getEnglishLanguageSelectedTranslator()
                        .translate(getString(R.string.toast_tts_googleApi_indispo_en))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                toast_tts_googleApi_indispo = translatedText;
                                showToast(toast_tts_googleApi_indispo);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                toast_tts_googleApi_indispo = getString(R.string.toast_tts_googleApi_indispo_en);
                                showToast(toast_tts_googleApi_indispo);
                            }
                        });
            }
            else{
                getEnglishLanguageSelectedTranslator()
                        .translate(getString(R.string.toast_tts_android_indispo_en))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                toast_tts_android_indispo = translatedText;
                                showToast(toast_tts_android_indispo);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_en);
                                showToast(toast_tts_android_indispo);
                            }
                        });

            }

        }

        //Vérifier la voix finale
        if (validatedVoice == null || validatedVoice.isEmpty()) {
            ittsCallbacks.onError("No valid ReadSpeaker voice found");
            return;
        }

        BuddySDK.Speech.setSpeakerVoice(validatedVoice);

        if(BuddySDK.Speech.isReadyToSpeak()) {
            Log.e("FCH_TEST","start play from TTS error");
            BuddySDK.Speech.startSpeaking(
                    text,
                    LabialExpression.SPEAK_NEUTRAL,
                    new ITTSCallback.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            ittsCallbacks.onSuccess(s);
                            Log.e("FCH_TEST","start play from TTS error  onSuccess");
                        }
                        @Override
                        public void onPause() throws RemoteException {}
                        @Override
                        public void onResume() throws RemoteException {}
                        @Override
                        public void onError(String s) throws RemoteException {
                            ittsCallbacks.onError(s);
                            Log.e("FCH_TEST","start play from TTS error  onERRor");
                        }
                    });
        }
        else{
            Log.e("FCH_TEST","else---------- start play from TTS error");
            ittsCallbacks.onError("ReadSpeaker indisponible");
        }

    }

    //#endregion ******************************************************* TTS **********************************************************************


    //#region ******************************************************* LEDs **********************************************************************

    public final IUsbCommadRsp iUsbLedCommandRsp = new IUsbCommadRsp.Stub(){
        @Override
        public void onSuccess(String success) throws RemoteException {
            Log.i(TAG, "Led success : " + success);
        }
        @Override
        public void onFailed(String error) throws RemoteException {
            Log.e(TAG, "Led error : " + error);
        }
    };

    /**
     * La fonction setLed() permet de changer la couleur des LEDs.
     * @param state : "listening" : pour la couleur GREEN #53B300
     *                "neutral"   : pour la couleur BLUE  #00D4D0
     *                "off"       : pour la couleur BLACK #000000
     */
    public void setLed(String state)  {
        Log.i(TAG, "setLed --- ["+state+"]");
        SystemClock.sleep(200);
        try {
            switch (state) {
                case "listening":
                    String listen_color = getParamFromFile("Listen_color","TeamChatBuddy.properties");
                    if(!listen_color.equals("")){
                        String listenColorHex = getHexColorFromName(listen_color);
                        if(listenColorHex!=null){
                            BuddySDK.USB.updateAllLed(listenColorHex, iUsbLedCommandRsp);
                        }
                        else {
                            BuddySDK.USB.updateAllLed("#008000", iUsbLedCommandRsp);
                        }
                    }
                    break;
                case "off":
                    BuddySDK.USB.updateAllLed("#000000", iUsbLedCommandRsp);
                    break;
                case "speaking":
                    String speak_color = getParamFromFile("Speak_color","TeamChatBuddy.properties");
                    if(!speak_color.equals("")){
                        String speakColorHex = getHexColorFromName(speak_color);
                        if(speakColorHex!=null){
                            BuddySDK.USB.updateAllLed(speakColorHex, iUsbLedCommandRsp);
                        }
                        else {
                            BuddySDK.USB.updateAllLed("#00D4D0", iUsbLedCommandRsp);
                        }
                    }
                    break;
                case "thinking":
                    String think_color = getParamFromFile("Think_color","TeamChatBuddy.properties");
                    if(!think_color.equals("")){
                        String thinkColorHex = getHexColorFromName(think_color);
                        if(thinkColorHex!=null){
                            BuddySDK.USB.updateAllLed(thinkColorHex, iUsbLedCommandRsp);
                        }
                        else {
                            BuddySDK.USB.updateAllLed("#FFFF00", iUsbLedCommandRsp);
                        }
                    }
                    break;
                case "displayQRCode":
                    BuddySDK.USB.updateAllLed("#00D4D0", iUsbLedCommandRsp);
                    break;
            }
            Log.i(TAG, "Changement de couleurs des LEDs ["+state+"]");
        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant le changement de couleurs des LEDs ["+state+"]: "+e);
        }
    }

    private String getHexColorFromName(String colorName) {
        switch (colorName.toLowerCase()) {
            case "blue":
                return "#00D4D0";
            case "green":
                return "#008000";
            case "yellow":
                return "#FFFF00";
            case "orange":
                return "#FFA500";
            case "red":
                return "#FF0000";
            case "purple":
                return "#800080";
            case "white":
                return "#FFFFFF";
            default:
                return null; // Couleur inconnue
        }
    }

    //#endregion ******************************************************* LEDs **********************************************************************

    //#region ******************************************************* Fonctions utiles *********************************************************

    private Toast mToast;
    public void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                mToast.show();
            }
        });
    }

    public boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public boolean nombreDeMotsCheck(String chaine) {
        // Utilisation d'une expression régulière pour vérifier si la chaîne contient au moins 3 mots
        // Crée un itérateur de mots pour la langue par défaut (la détection automatique de la langue)
        BreakIterator wordIterator = BreakIterator.getWordInstance();

        // Définit la chaîne de texte sur laquelle l'itérateur de mots va travailler
        wordIterator.setText(chaine);

        int wordCount = 0;
        int lastIndex = 0;

        // Boucle pour compter les mots en utilisant l'itérateur de mots
        while (wordIterator.next() != BreakIterator.DONE) {
            int currentIndex = wordIterator.current();

            // Vérifie si l'index actuel n'est pas un espace
            if (Character.isLetterOrDigit(chaine.charAt(currentIndex - 1))) {
                wordCount++;
            }
            lastIndex = currentIndex;
        }
        Log.e("MEHDI","nombre de mots  ------------ "+wordCount);
        return wordCount >= Integer.parseInt(getParamFromFile("Number_of_words","TeamChatBuddy.properties"));
    }

    /**
     * Cette méthode permet de checker si le device est connecté à l'internet
     */
    public boolean isConnectedToInternet() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final Network n = cm.getActiveNetwork();
        if (n != null) {
            final NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            if (nc != null) {
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    haveConnectedWifi = true;

                } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    haveConnectedMobile = true;
                }
            }

            return haveConnectedWifi || haveConnectedMobile;
        }
        return false;
    }


    /**
     * cette méthode permet de récupérer la langue à utiliser
     */
    public String getCurrentLanguage(){
        if(this.langue.getNom() .equals("Français")){
            return "fr";
        }
        else if (this.langue.getNom() .equals("Anglais")){
            return "en";
        }
        else if (this.langue.getNom() .equals("Espagnol")){
            return "es";
        }
        else if (this.langue.getNom() .equals("Allemand")){
            return "de";
        }
        else{
            List<String> TTSAndroidLangueCode = getLanguageCodeForDisponibleLangue("Language_Code_Used_In_TTS_Android");
            String codeLanguageTTSAndroid = TTSAndroidLangueCode.get(this.langue.getId()-1);
            return codeLanguageTTSAndroid.split("-")[0].trim();
        }
    }
    /**
     * cette fonction permet de stocker un paramètre dans la mémoire
     */
    public void setparam(String a,String b){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(a, b);
        editor.apply();
    }

    /**
     * cette fonction permet de récupérer la valeur du paramètre stocké
     */
    public String getparam(String a){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getString(a, "");
    }

    /**
     * la fonction notifyObservers() permet d'envoyer un message "notification" aux classes qui implémentent IDBObserver.
     * @param message : le message à envoyer
     */
    public void notifyObservers(String message) {
        Log.i("mehdi", "notifyObservers: " + message);
        for (int i = 0; i < observers.size(); i++) {
            try {
                IDBObserver ob = observers.get(i);
                ob.update(message);
            } catch (IOException e) {
                Log.e(TAG, "Erreur lors de l'envoi de la notification aux observateurs [ "+message+" ] :" + e);
            }
        }
    }

    /**
     * la fonction registerObserver() permet de s'enregistrer au pattern Observer afin de recevoir les notifications
     */
    public void registerObserver(IDBObserver observer) {
        observers.add(observer);
    }

    /**
     * la fonction removeObserver() permet de se désinscrire du pattern Observer pour ne plus recevoir les notifications
     */
    public void removeObserver(IDBObserver observer) {
        observers.remove(observer);
    }

    /**
     * Cette fonction permet de cacher les barres du système
     */
    public int hideSystemUI(Activity myActivityReference) {
        View decorView = myActivityReference.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
        return (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Cette fonction permet de récupérer un paramètre depuis le fichier de configuration
     */
    public String getParamFromFile(String param, String fileName) {
        File directory = new File(getString(R.string.path), "TeamChatBuddy");
        CustomProperties props = ConfigurationFile.props;
        CustomProperties newProps = ConfigurationFile.loadproperties(directory, fileName, props);
        return newProps.getProperty(param);
    }
    public String getPromptFromFile(String type,String fileName) {
        File directory = new File(getString(R.string.path), "TeamChatBuddy");
        File fileconfig = new File(directory, fileName);

        StringBuilder prompt = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileconfig))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Vérifier si la ligne correspond au type requis
                if (line.startsWith(type) && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length > 1) {
                        String command = parts[1].trim();
                        if (prompt.length() > 0) {
                            prompt.append(" / "); // Ajouter un séparateur entre les commandes
                        }
                        prompt.append(command.replace("\"", "")); // Retirer les guillemets si nécessaire
                    }
                }
            }
        } catch (IOException e) {
            Log.e("MRA_prompt", "Erreur de lecture du fichier : " + e.getMessage());
        }
        Log.e("MRA_prompt","Prompt final : " + prompt.toString());
        return prompt.toString();  // Return the prompt as a single string
    }

    /**
     * Cette fonction permet de créer le fichier de configuration
     */
    public String createPropertiesFile() {
        File directory = new File(getString(R.string.path), "TeamChatBuddy");
        String initOrMajOrNone = ConfigurationFile.createConfigurationFile(directory, getString(R.string.version_app));
        init();
        notYet=false;
        return initOrMajOrNone;
    }

    /**
     * Cette fonction permet de changer le volume du device
     */
    public void setVolume(int percentage,int type) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);


        int volume = getClosestInt((double) (percentage * max) / 100);


        if (audioManager.isBluetoothScoOn()) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            audioManager.setStreamVolume(6, volume, type);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume,AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE );
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume,type);
        }
    }

    /**
     * cette fonction permet de récupérer le volume du device
     */
    public int getVolume(){
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * Cette fonction permet de récupérer le volume max du device
     */
    public int getMaxVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isBluetoothScoOn())
            return audioManager.getStreamMaxVolume(6);
        else
            return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * Cette fonction permet de récupérer l'entier le plus proche au double passé en argument
     */
    public int getClosestInt(double x){
        return (int)Math.rint(x);
    }

    public void calcul_consommation(String modelName, double inputTokens, double outputTokens) {
        Log.i("USAGE","----------- calcul_consommation : modelName=" + modelName + " , inputTokens=" + inputTokens + " , outputTokens=" + outputTokens);

        String show_openAI_prices = getParamFromFile("show_openAI_prices", "TeamChatBuddy.properties");
        if (show_openAI_prices != null && show_openAI_prices.trim().equalsIgnoreCase("yes")) {

            List<OpenAiInfo> openAiInfoList = parseAndLoadPrices();
            double totalConsumptionSaved = Double.parseDouble(getparam("Total_cons"));
            Log.i("USAGE", "totalConsumption (avant calcul) : "+totalConsumptionSaved);

            double prixCalcul = 0;
            boolean modelFound = false;
            for (OpenAiInfo info : openAiInfoList) {
                if (info.getModelName().equalsIgnoreCase(modelName)) {
                    modelFound = true;
                    if (modelName.toLowerCase().contains("whisper")) {
                        Log.i("USAGE", "(is calculating) inputPrice="+info.getInputPrice() + " ,  duration=" + inputTokens + " --> "+info.getInputPrice() + " * " + inputTokens);
                        prixCalcul += info.getInputPrice() * inputTokens;
                        setparam(info.getModelName() + "_outputConsumption", String.valueOf(0));

                        String _outputTokens = getparam(modelName + "_outputTokens");
                        if (_outputTokens.isEmpty()) {
                            setparam(modelName+"_outputTokens", String.valueOf(outputTokens));
                        }
                        else{
                            setparam(modelName+"_outputTokens", String.valueOf(Double.parseDouble(_outputTokens)+outputTokens));
                        }

                        String _inputTokens = getparam(modelName + "_inputTokens");
                        if (_inputTokens.isEmpty()) {
                            setparam(modelName+"_inputTokens", String.valueOf(inputTokens));
                        }
                        else{
                            setparam(modelName+"_inputTokens", String.valueOf(Double.parseDouble(_inputTokens)+inputTokens));
                        }

                        String _entryConsumption = getparam(modelName + "_entryConsumption");
                        if (_entryConsumption.isEmpty()) {
                            setparam(modelName+"_entryConsumption", String.valueOf(inputTokens*info.getInputPrice()));
                        }
                        else{
                            setparam(modelName+"_entryConsumption", String.valueOf(Double.parseDouble(_entryConsumption)+inputTokens*info.getInputPrice()));
                        }
                    } else {
                        prixCalcul += (info.getInputPrice() * inputTokens + info.getOutputPrice() * outputTokens) / 1000;
                        Log.i("USAGE", "(is calculating) inputPrice="+info.getInputPrice() + " ,  inputTokens=" + inputTokens + " ,  outputPrice=" + info.getOutputPrice() + " ,  outputTokens=" + outputTokens
                                + " --> ("+info.getInputPrice() + " * " + inputTokens+ " + " + info.getOutputPrice()+ " * " + outputTokens+ ")/1000");
                        String _outputTokens = getparam(modelName + "_outputTokens");
                        if (_outputTokens.isEmpty()) {
                            setparam(modelName+"_outputTokens", String.valueOf(outputTokens));
                        }
                        else{
                            setparam(modelName+"_outputTokens", String.valueOf(Double.parseDouble(_outputTokens)+outputTokens));
                        }

                        String _inputTokens = getparam(modelName + "_inputTokens");
                        if (_inputTokens.isEmpty()) {
                            setparam(modelName+"_inputTokens", String.valueOf(inputTokens));
                        }
                        else{
                            setparam(modelName+"_inputTokens", String.valueOf(Double.parseDouble(_inputTokens)+inputTokens));
                        }

                        String _entryConsumption = getparam(modelName + "_entryConsumption");
                        if (_entryConsumption.isEmpty()) {
                            setparam(modelName+"_entryConsumption", String.valueOf(inputTokens*info.getInputPrice()/1000));
                        }
                        else{
                            setparam(modelName+"_entryConsumption", String.valueOf(Double.parseDouble(_entryConsumption)+inputTokens*info.getInputPrice()/1000));
                        }

                        String _outputConsumption = getparam(modelName + "_outputConsumption");
                        if (_outputConsumption.isEmpty()) {
                            setparam(modelName+"_outputConsumption", String.valueOf(outputTokens*info.getInputPrice()/1000));
                        }
                        else{
                            setparam(modelName+"_outputConsumption", String.valueOf(Double.parseDouble(_outputConsumption)+outputTokens*info.getInputPrice()/1000));
                        }
                    }
                    break;
                }
            }

            if (!modelFound) {
                Log.e("USAGE","Model ("+modelName+") not found !");
                String _outputTokens = getparam(modelName + "_outputTokens");
                if (_outputTokens.isEmpty()) {
                    setparam(modelName+"_outputTokens", String.valueOf(outputTokens));
                }
                else{
                    setparam(modelName+"_outputTokens", String.valueOf(Double.parseDouble(_outputTokens)+outputTokens));
                }

                String _inputTokens = getparam(modelName + "_inputTokens");
                if (_inputTokens.isEmpty()) {
                    setparam(modelName+"_inputTokens", String.valueOf(inputTokens));
                }
                else{
                    setparam(modelName+"_inputTokens", String.valueOf(Double.parseDouble(_inputTokens)+inputTokens));
                }
            } else {
                totalConsumptionSaved += prixCalcul;
                setparam("Total_cons", String.valueOf(totalConsumptionSaved));
                Log.i("USAGE", "totalConsumption (après calcul) : "+totalConsumptionSaved);
            }
        }
        else{
            Log.e("USAGE","show_openAI_prices is not set to 'YES' ");
        }
    }


    public List<OpenAiInfo> parseAndLoadPrices() {

        String modelsPrice = getParamFromFile("Models_price", "TeamChatBuddy.properties");
        if(modelsPrice == null || modelsPrice.isEmpty()){
            modelsPrice = "gpt-3.5-turbo_0.0005_0.0015/gpt-4o_0.005_0.015/gpt-4_0.03_0.06/gpt-4o-mini_0.00015_0.0006/whisper-1_0.006_0";
        }

        List<OpenAiInfo> openAiInfoList = new ArrayList<>();
        String[] models = modelsPrice.split("/");

        for (String model : models) {
            String[] parts = model.split("_");
            String modelName = parts[0].toLowerCase();
            double defaultInputPrice = Double.parseDouble(parts[1]);
            double defaultOutputPrice = Double.parseDouble(parts[2]);

            // Check if SharedPreferences already contain the price
            String savedInputPrice = getparam(modelName + "_inputPrice");
            String savedOutputPrice = getparam(modelName + "_outputPrice");

            double inputPrice = (savedInputPrice != null && !savedInputPrice.isEmpty()) ? Double.parseDouble(savedInputPrice) : defaultInputPrice;
            double outputPrice = (savedOutputPrice != null && !savedOutputPrice.isEmpty()) ? Double.parseDouble(savedOutputPrice) : defaultOutputPrice;

            if(modelName.contains("whisper")){
                openAiInfoList.add(new OpenAiInfo(modelName, inputPrice, outputPrice, "$/minutes"));
            }
            else{
                openAiInfoList.add(new OpenAiInfo(modelName, inputPrice, outputPrice, "$/K Tokens"));
            }
        }

        return openAiInfoList;
    }

    public void priceNotAvailable(){
        if (getLangue().getNom().equals("Anglais")){
            showToast(getString(R.string.toast_pricing_indispo_en));
        }
        else if (getLangue().getNom().equals("Français")) {
            showToast(getString(R.string.toast_pricing_indispo_fr));
        }
        else if (getLangue().getNom().equals("Espagnol")) {
            showToast(getString(R.string.toast_pricing_indispo_de));
        }
        else if (getLangue().getNom().equals("Allemand")){
            showToast(getString(R.string.toast_pricing_indispo_es));
        }
        else{
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_pricing_indispo_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            showToast(translatedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showToast(getString(R.string.toast_pricing_indispo_en));
                        }
                    });
        }
    }

    public int getRequestTotalTokens(RequestBody requestBody){
        try{
            int requestTotalTokens = 0;
            int totalTokensContent = 0;
            int totalTokensRole = 0;
            Optional<Encoding> encoding = getRegistry().getEncodingForModel(getparam("model"));
            if (encoding.isPresent()) {
                Encoding actualEncoding = encoding.get();
                Log.i(TAG_STREAM_USAGE, "Encoding is available for the model "+getparam("model") + " --> " + actualEncoding.getName());
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                String requestBodyString = buffer.readUtf8();
                Log.d(TAG_STREAM, "requestBody: " + requestBodyString);
                JSONObject json = new JSONObject(requestBodyString);
                if (json.has("messages")) {
                    JSONArray messagesArray = json.getJSONArray("messages");
                    String[] roleArray = new String[messagesArray.length()];
                    String[] contentArray = new String[messagesArray.length()];
                    for (int i = 0; i < messagesArray.length(); i++) {
                        JSONObject message = messagesArray.getJSONObject(i);
                        if (message.has("role") && message.has("content")) {
                            String role = message.getString("role");
                            String content = message.getString("content");
                            roleArray[i] = role;
                            contentArray[i] = content;
                            totalTokensContent += actualEncoding.countTokens(content);
                            totalTokensRole += actualEncoding.countTokens(role);
                        }
                    }
                    //Log.v(TAG_STREAM_USAGE, "contentArray: " + Arrays.toString(contentArray));
                    //Log.v(TAG_STREAM_USAGE, "Total Tokens for Content: " + totalTokensContent);
                    //Log.v(TAG_STREAM_USAGE, "roleArray: " + Arrays.toString(roleArray));
                    //Log.v(TAG_STREAM_USAGE, "Total Tokens for Role: " + totalTokensRole);
                    requestTotalTokens = totalTokensContent + totalTokensRole;
                    Log.d(TAG_STREAM_USAGE, "Request Total Tokens : " + requestTotalTokens);
                    return requestTotalTokens;
                }
                else {
                    Log.e(TAG_STREAM_USAGE, "No 'messages' array found in the JSON.");
                    return 0;
                }
            }
            else {
                Log.e(TAG_STREAM_USAGE, "Encoding is not available for the model "+getparam("model"));
                return 0;
            }
        }
        catch(Exception e){
            Log.e(TAG_STREAM_USAGE, "getRequestTotalTokens() ERROR : " + e);
            e.printStackTrace();
            return 0;
        }
    }

    public void FetchLangGoogleCloudSTT() {

        try {
            // Open file from assets
            InputStream is = this.getAssets().open("lang_stt_google.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            // Parse JSON
            Type listType = new TypeToken<List<LocaleEntry>>() {}.getType();
            List<LocaleEntry> entries = new Gson().fromJson(jsonBuilder.toString(), listType);

            // Store locales
            supportedLocales.clear();
            for (LocaleEntry entry : entries) {
                //Log.i("GoogleSTT", "entry.code: " + entry.code+"\nentry.language: "+entry.language+"\nentry.country: "+entry.country);
                supportedLocales.put(entry.code, new Locale(entry.language, entry.country));
            }

            Log.i("GoogleSTT", "Locales loaded: " + supportedLocales.size());

        } catch (Exception e) {
            Log.e("GoogleSTT", "Error loading locales", e);
        }

    }

    public static String stripSSML(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder textContent = new StringBuilder();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new java.io.StringReader(input));

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.TEXT) {
                    textContent.append(parser.getText());
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            // Fallback simple en cas d’erreur
            return input.replaceAll("<[^>]+>", "").trim();
        }
        return textContent.toString().trim();
    }


    // region ------------------------------------------------------------ QR code scanner ------------------------------------------------------------

    private FrameProcessing zoomFrameProcessor;
    private DetectionCallback detectionZoomCallback;
    private final AtomicBoolean isAprilTagProcessing = new AtomicBoolean(false);
    private final AtomicBoolean isQRProcessing = new AtomicBoolean(false);

    public void setupZoomQRCode(DetectionCallback detectionCallback) {
        if (zoomFrameProcessor == null) {
            zoomFrameProcessor = new FrameProcessing();
        }
        detectionZoomCallback = detectionCallback;
    }


    public List<String> getDisponibleScaningSystem(){
        StringTokenizer st = new StringTokenizer(getParamFromFile("QRCode_System",configurationFilePseudo), "/", false);
        List<String> list = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String result = st.nextToken();
            list.add(result.trim());
        }
        return list;
    }

    ExecutorService visionExecutor = Executors.newSingleThreadExecutor();

    public void processImage(Context context, Bitmap bitmap,Image image, List<String> types) {
        boolean shouldProcessQR = types.contains("QRCode");
        boolean shouldProcessDataMatrix = types.contains("DataMatrix");
        boolean shouldProcessAprilTag = types.contains("AprilTag");

        if ((shouldProcessQR || shouldProcessDataMatrix) && isQRProcessing.get()) {
            image.close();
            Log.i(TAG, "processImage: image.close(1) ");

            return;
        }

        if (shouldProcessAprilTag && isAprilTagProcessing.get()) {
            image.close();
            Log.i(TAG, "processImage: image.close(2) ");
            return;
        }

        // background processing
        Future<?> future = visionExecutor.submit(() ->{

            try {
                final int width = image.getWidth();
                final int height = image.getHeight();
                final int rowStride = image.getPlanes()[0].getRowStride();
                final ByteBuffer yBuffer = image.getPlanes()[0].getBuffer().duplicate();

                if ((shouldProcessQR || shouldProcessDataMatrix) && isQRProcessing.compareAndSet(false, true)) {
                    isQRProcessing.set(true);
                    zoomFrameProcessor.setNextFrame(bitmap);
                }
                if (shouldProcessAprilTag && !isAprilTagProcessing.get()) {
                    isAprilTagProcessing.set(true);
                    ArrayList<ApriltagDetection> detections = ApriltagNative.apriltag_detect_yuv_zoom(
                            yBuffer, width, height, rowStride
                    );

//                    ((MainActivity) context).runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            ((MainActivity) context).runOnUiThread(() -> {

                    for (ApriltagDetection aprilTag : detections) {

                        Log.d("AprilTagProcess", "Detected tag ID = " + aprilTag.id
                                + ", listeningState = " + listeningState
                                + ", currentLanguage = " + getCurrentLanguage());

                        if (listeningState.equals("hotword")) {

                            String tagText = getTxtfromTag(String.valueOf(aprilTag.id));
                            Log.d("AprilTagProcess", "Extracted text for tag " + aprilTag.id + " = " + tagText);

                            if (getCurrentLanguage().equals("fr")) {

                                Log.d("AprilTagProcess", "No translation needed (language = fr). Passing to checkTheHotword.");
                                checkTheHotword(tagText, "AprilTag");

                            } else {

                                Log.d("AprilTagProcess", "Translating text to French: " + tagText);

                                getFrenchLanguageSelectedTranslator()
                                        .translate(tagText)
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                Log.i("MYA_trans", "Translation success. Result = " + translatedText);
                                                checkTheHotword(translatedText, "AprilTag");
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e("AprilTagProcess", "Translation FAILED for text: " + tagText
                                                        + " | Exception = " + e.getMessage(), e);
                                            }
                                        });
                            }

                        } else if (listeningState.equals("qst")) {

                            Log.d("AprilTagProcess", "Notifying observers with AprilTagScan event. Tag ID = " + aprilTag.id);
                            notifyObservers("AprilTagScan;SPLIT;" + aprilTag.id);

                        } else {

                            Log.w("AprilTagProcess", "Unknown listeningState: " + listeningState
                                    + " — ignoring tag " + aprilTag.id);
                        }
                    }

                    isAprilTagProcessing.set(false);
//                            });
//                        }
//                    });
                }
            }catch (Exception e){
                Log.e(TAG, "processImage: "+e);
            }
            finally {
                image.close();

                // ALWAYS RESET FLAGS
                if (shouldProcessAprilTag) isAprilTagProcessing.set(false);
                if (shouldProcessQR || shouldProcessDataMatrix) isQRProcessing.set(false);
            }
        });
        try {
            future.get(); // blocks until task is 100% done
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


    public void startBarcodeScanner(Context context, List<String> types) {
        if (zoomFrameProcessor == null) {


            zoomFrameProcessor = new FrameProcessing();
            Log.i("MYA_QR", "zoomFrameProcessor initialized.");
        }
        BarcodeScannerOptions.Builder optionsBuilder = new BarcodeScannerOptions.Builder();

        if (types.contains("QRCode") && types.contains("DataMatrix")) {
            optionsBuilder.setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX);
        } else if (types.contains("DataMatrix")) {
            optionsBuilder.setBarcodeFormats(Barcode.FORMAT_DATA_MATRIX);
        } else if (types.contains("QRCode")) {
            optionsBuilder.setBarcodeFormats(Barcode.FORMAT_QR_CODE);
        } else {
            // Aucun code supporté
            if (zoomFrameProcessor != null) {
                zoomFrameProcessor.setMachineLearningFrameProcessor(null);
            }
            return;
        }

        BarcodeScannerOptions options = optionsBuilder.build();

        BarcodeScannerProcessor processor = new BarcodeScannerProcessor(context, new DetectionCallback() {
            @Override
            public void onDetection(String text) {
                detectionZoomCallback.onDetection(text);

                Log.i("MYA_QR_H", "run: QR/MATRIX " + text);
                if(listeningState == "hotword"){
                    Log.e("MYA_YAKINE","listeningState = hotword\ncheckTheHotword(text.split(\";\")[0]): "+ text.split(";")[0]);
                    checkTheHotword(text.split(";")[0],text.split(";")[1]);
                }
                else if (listeningState == "qst"){
                    Log.e("MYA_YAKINE","listeningState = qst\n" +text.split(";")[1] +" = "+ text.split(";")[0]);
                    notifyObservers("QRCodeScan;SPLIT;" + text.split(";")[1] + ";SPLIT;" + text.split(";")[0]);
                }
            }

            @Override
            public void onNoDetection() {
                detectionZoomCallback.onNoDetection();
            }
        }, options);

        zoomFrameProcessor.setMachineLearningFrameProcessor(processor);
    }

    public void stopQRCode() {
        if (zoomFrameProcessor != null) {
            zoomFrameProcessor.stop();
        }
//        if (cameraUtils != null) {
//            cameraUtils.stopCamera(); // ceci va aussi cacher la vue
//        }
    }


    public void createAndDeployDefaultAprilTagJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("1", "Bonjour, comment tu t'appelles ?");
            json.put("2", "Quel temps fait-il aujourd'hui ?");
            json.put("3", "Raconte-moi une blague !");
            json.put("4", "Exécute une danse.");
            json.put("5", "C’est quoi ton jeu préféré ?");
            json.put("6", "Répète après moi : Bonjour les amis !");
            json.put("7", "Montre-moi une image de chien rouge.");
            json.put("8", "Quelle est ta couleur préférée ?");
            json.put("9", "Dis-moi une devinette.");
            json.put("10", "écoute-moi");
            json.put("11", "bonsoir");
            json.put("12", "bonjour");


            File dir = new File("/storage/emulated/0/", "TeamChatBuddy");
            if (!dir.exists()) dir.mkdirs();

            File jsonFile = new File(dir, "association_apriltag.json");

            if (jsonFile.exists()) return;

            FileOutputStream fos = new FileOutputStream(jsonFile);
            fos.write(json.toString(4).getBytes());
            fos.flush();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getTxtfromTag(String targetKey) {
        try {
            File directory = new File(getString(R.string.path), "TeamChatBuddy");
            File jsonFile = new File(directory, "association_apriltag.json");
            if (!jsonFile.exists()) {
                Log.e("MYA_QR", "Fichier JSON introuvable : " + jsonFile.getAbsolutePath());
                return "";
            }


            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(jsonFile));
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();

            // Parsing JSON
            JSONObject jsonObject = new JSONObject(builder.toString());

            final String[] result = new String[1];
            // Recherche par key
            if (jsonObject.has(targetKey)) {
                return jsonObject.getString(targetKey);
            }

        } catch (IOException e) {
            Log.e("MYA_QR", "Erreur de lecture du fichier : " + e.getMessage());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return ""; // return empty if no valid value found
    }

    //#endregion ------------------------------------------------------------ QR code scanner ------------------------------------------------------------

    //#endregion ******************************************************* Fonctions utiles *********************************************************

}
