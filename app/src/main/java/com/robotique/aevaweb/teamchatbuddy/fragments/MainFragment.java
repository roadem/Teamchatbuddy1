package com.robotique.aevaweb.teamchatbuddy.fragments;


import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bfr.buddy.ui.shared.FaceTouchData;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.ui.shared.IUIFaceTouchCallback;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddysdk.BuddySDK;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.activities.MainActivity;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.ChatGptStreamMode;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.Commande;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.CustomGPTStreamMode;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.ResponseFromChatbot;
import com.robotique.aevaweb.teamchatbuddy.models.Langue;
import com.robotique.aevaweb.teamchatbuddy.models.Replica;
import com.robotique.aevaweb.teamchatbuddy.models.Session;
import com.robotique.aevaweb.teamchatbuddy.models.Setting;
import com.robotique.aevaweb.teamchatbuddy.observers.IDBObserver;
import com.robotique.aevaweb.teamchatbuddy.utilis.AlertManager;
import com.robotique.aevaweb.teamchatbuddy.utilis.BIPlayer;
import com.robotique.aevaweb.teamchatbuddy.utilis.CustomToast;
import com.robotique.aevaweb.teamchatbuddy.utilis.DetectionCallback;
import com.robotique.aevaweb.teamchatbuddy.utilis.IBehaviourCallBack;
import com.robotique.aevaweb.teamchatbuddy.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.teamchatbuddy.utilis.ITTSCallbacks;
import com.robotique.aevaweb.teamchatbuddy.utilis.NextSegmentCallback;
import com.robotique.aevaweb.teamchatbuddy.utilis.WifiBroadcastReceiver;
import com.robotique.aevaweb.teamchatbuddy.utilis.apriltag.CameraUtils;
import com.robotique.aevaweb.teamchatbuddy.utilis.tracking.MainViewModel;
import com.robotique.aevaweb.teamchatbuddy.utilis.tracking.OverlayView;
import com.robotique.aevaweb.teamchatbuddy.utilis.tracking.PoseLandmarkerHelper;
import com.robotique.aevaweb.teamchatbuddy.utilis.tracking.PoseTracking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainFragment extends Fragment implements IDBObserver{

    private static final String TAG = "MainFragment";
    private static final String TAG_TRACKING = "TEAMCHAT_BUDDY_TRACKING_INFO";
    private static final String TAG_TRACKING_DEBUG = "TEAMCHAT_BUDDY_TRACKING_DEBUG";

    private MainActivity _parentActivity;

    private TeamChatBuddyApplication teamChatBuddyApplication;

    //views
    private RelativeLayout buddy_texte_qst_lyt;
    private RelativeLayout buddy_texte_resp_lyt;
    private RelativeLayout lyt_open_menu_settings;
    private View lyt_settings_touch_zone;
    private boolean settingsButtonHiddenByUser = false;
    private boolean settingsBtnLongPressTriggered = false;
    private final Handler settingsRevealHandler = new Handler(Looper.getMainLooper());
    private final Runnable settingsRevealRunnable = () -> {
        settingsBtnLongPressTriggered = true;
        toggleSettingsBtn();
    };

    private void toggleSettingsBtn() {
        if (lyt_open_menu_settings == null) return;
        settingsButtonHiddenByUser = !settingsButtonHiddenByUser;
        applySettingsBtnVisibility();
        teamChatBuddyApplication.setparam("settings_button_hidden", String.valueOf(settingsButtonHiddenByUser));
        Log.d(TAG, "Settings button toggled - New state: " + (settingsButtonHiddenByUser ? "HIDDEN" : "VISIBLE"));
    }

    private void applySettingsBtnVisibility() {
        if (lyt_open_menu_settings != null) {
            lyt_open_menu_settings.setVisibility(settingsButtonHiddenByUser ? View.INVISIBLE : View.VISIBLE);
        }
    }
    private RelativeLayout lyt_open_menu_chat;
    private RelativeLayout view_face;
    private RelativeLayout launch_view;
    private RelativeLayout reGroup;
    private RelativeLayout preViewViewLyt;
    private RelativeLayout photo_timer_bg_rlyt;
    private RelativeLayout photo_timer_rlyt;
    private FrameLayout preview_container;
    private OverlayView overlay;
    private PreviewView previewView;
    private PreviewView previewViewphoto;
    private TextView photo_timer_txtView;
    private TextView buddy_texte_qst;
    private TextView buddy_texte_resp;
    private RelativeLayout previewView_qr;
    private TextView textViewQRMessage;
    private ImageView noNetwork;
    private ImageView imageArrow;
    private ImageView imageQRCode;
    private long qrTimeRemaining = 0;
    private boolean qr_displayed = false;
    private long qrTimerEndTimestamp = 0;
    private RelativeLayout layoutTexteQR;
    private RelativeLayout layoutQRCode;
    private ProgressBar downloadingBar;
    private ImageView photo_timer_imgview;
    private ImageView bi_imageView;
    private PlayerView bi_videoView;

    private double TRACKING_WELCOME_TEMPERATURE;

    private long lastVisibleTime = 0L;          // Track the time when person was last seen
    private long lastVisibleTime_saved = 0L;    // Track the time when person was last seen and do not reset it when re-track (useful for invitation check)
    private long firstVisibleTime = 0L;         // Track the time when person started being visible
    private long visibleDuration = 0L;          // How long a person remained visible
    private long lastLookingAtCameraTime = 0L;  // Track the time when the person last looked at the camera
    private long lastLookingAtCameraTimeToCloseApp = 0L;  // Track the time when the person last looked at the camera
    private long personDetectedTimeToCloseApp = 0L; // Track the time when the person last detected at the camera
    private long totalTimeLookingAtCamera = 0L; // Total time spent looking at the camera
    private long startLookingAtCameraTime = 0L; // Track the start time of the current interval when the person is looking directly at the camera

    private Float[] res = {(float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0,(float) 0, (float) 0, (float) 0};
    private float initLang=190F;
    private float degx,degy,x0,x2,x5,y0,y5,y2,lang,dLeft,eog,Eod, dRight;

    private int path= R.string.path;
    private int pathLog=R.string.pathConfig;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private int TRACKING_DELAY_NO_WATCH;
    //private int TRACKING_DELAY_NO_TRACK;
    private int TRACKING_DELAY_START_LISTEN;
    private int TRACKING_DELAY_STOP_LISTEN;
    private int TRACKING_REGARD_CENTER;
    private int TRACKING_DELAY_WELCOME;
    private int TRACKING_DURATION_WELCOME;
    private int TRACKING_WELCOME_MAX_TOKEN;
    private int TRACKING_TIMEOUT;
    private int WATCHING_timeout;

    private String TRACKING_WELCOME_FR;
    private String TRACKING_WELCOME_EN;
    private String TRACKING_WELCOME_PROMPT_FR;
    private String TRACKING_WELCOME_PROMPT_EN;
    private String TRACKING_WELCOME_MODEL;
    private String TRACKING_WATCH;
    private String directionRegardNez= "";
    private CameraUtils cameraUtils;
    List<String> types;
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String header ="header";
    private String entete ="entete";
    private String openAIKey = "openAI_API_Key";
    private String info_toast = "";
    private String gptResponse;
    private String qrqst;
    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private boolean isListeningFreeSpeech = false;
    private Boolean mlKitIsDownloading = false;
    private boolean english_is_downloaded = false;
    private boolean french_is_downloaded = false;
    private boolean languageToEnglish_is_downloaded = false;
    private boolean isCMDLangue = false;
    private boolean isSpeaking = false;
    private Boolean gptSend=false;
    private boolean isFirstLaunch = true;
    private boolean regarde_camera=false;
    private boolean direction=false;
    private boolean isRecenter=false;
    private boolean deFace=false;
    private boolean isPersonDetected = false;
    private boolean wasPersonDetected = false;
    private boolean personIsVisible = false;
    private boolean sendInvitationPending = false;
    private boolean isProcessingReTrack = false;
    private boolean isTrackingAlreadyInitialised = false;
    private boolean isReTrack = false;
    private boolean isFirstInvitaion = false;

    private CountDownTimer timerEcoute;
    private CountDownTimer timerDownloading;
    private CountDownTimer timerPhoto;
    private CountDownTimer timerToApplyBI;
    private CountDownTimer timerDisplayQRCode;
    private CountDownTimer timerPeriodToDisplayQRCode;
    private static final Random random = new Random();
    private WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();
    private ArrayList<Replica> listRep=new ArrayList();
    private AudioManager amanager;
    private Dialog dialog;
    private ResponseFromChatbot responseFromChatbot;
    private Setting settingClass;
    private Commande commande;
    private PoseTracking poseTracking;
    private ExecutorService backgroundExecutor;
    private CameraSelector cameraSelector;
    private MainViewModel viewModel;
    private PoseLandmarkerHelper poseLandmarkerHelper;
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private AnimationDrawable animationTimerPhoto;
    private ImageCapture imageCapture;
    private String initOrMajOrNone="";
    private Boolean useListeningNumberWithAutomaicListening=false;
    private Boolean applyBIAfterDelay=false;
    private Boolean currentlyBIExecuted = false;
    private Boolean initHasBeenLaunchedFromOnResume = true;

    boolean isConnected = true;
    private Toast currentToast;
    private IStartMessageCallback iStartMessageCallback;
    private IMouthMessageCallback iMouthMessageCallback;
    public interface IStartMessageCallback {
        void onEnd(String s);
    }
    public interface IMouthMessageCallback {
        void onEnd(String s);
    }
    private IMLKitDownloadCallback imlKitDownloadCallback = new IMLKitDownloadCallback() {
        @Override
        public void onDownloadEnd(boolean success,String english_or_french) {
            Log.i(TAG, "onDownloadEnd ");
            if(success){
                switch (english_or_french) {
                    case "english":
                        english_is_downloaded = true;
                        break;
                    case "french":
                        french_is_downloaded = true;
                        break;
                    case "languageToEnglish":
                        languageToEnglish_is_downloaded = true;
                        break;
                }
                if (english_is_downloaded && french_is_downloaded && languageToEnglish_is_downloaded) {

                    Log.i("MYA_fragment", " onDownloadEnd: ");
                    mlKitIsDownloading = false;
                    handlerProgressBar.removeCallbacksAndMessages(null);
                    handlerProgressBar.removeCallbacks(runnableProgressBar);
                    launch_view.setVisibility(View.INVISIBLE);
                    if(timerDownloading!=null){
                        timerDownloading.cancel();
                    }
                    if(currentToast!=null){
                        currentToast.cancel();
                    }
                    switch (initOrMajOrNone) {
                        case "INIT":
                            if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                info_toast = getString(R.string.toast_config_file_init_en);
                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                info_toast = getString(R.string.toast_config_file_init_fr);
                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("de")) {
                                info_toast = getString(R.string.toast_config_file_init_de);
                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("es")) {
                                info_toast = getString(R.string.toast_config_file_init_es);
                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                            }
                            else{
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator()
                                        .translate(getString(R.string.toast_config_file_init_en))
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                info_toast = translatedText;
                                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                info_toast = getString(R.string.toast_config_file_init_en);
                                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                                            }
                                        });
                            }

                            break;
                        case "MAJ":
                            //traduire l'info du configFile :
                            if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                info_toast = getString(R.string.toast_config_file_maj_en);
                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                info_toast = getString(R.string.toast_config_file_maj_fr);
                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("de")) {
                                info_toast = getString(R.string.toast_config_file_maj_de);
                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("es")) {
                                info_toast = getString(R.string.toast_config_file_maj_es);
                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                            }
                            else{
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator()
                                        .translate(getString(R.string.toast_config_file_maj_en))
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                info_toast = translatedText;
                                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                info_toast = getString(R.string.toast_config_file_maj_en);
                                                CustomToast.getInstance().showInfo(getActivity(), info_toast,2000);
                                            }
                                        });
                            }

                            break;
                        case "NONE":
                            break;
                    }
                    if (isCMDLangue){
                        isCMDLangue = false;
                        commande.translate("CMD_LANGUE", new Commande.ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedText) {
                                if (translatedText.contains("No_message_defined")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                } else {
                                    String verifyMessage = commande.verifyCmdMessages(translatedText);
                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                        if (teamChatBuddyApplication.getParamFromFile("COMMAND_histo", "TeamChatBuddy.properties") != null && teamChatBuddyApplication.getParamFromFile("COMMAND_histo", "TeamChatBuddy.properties").trim().equalsIgnoreCase("yes")) {
                                            try {
                                                // get the historic commandes :
                                                Log.e("DLA", "get the historic commandes ");
                                                String jsonArrayString = teamChatBuddyApplication.getparam("commandes");
                                                Log.e("DLA", "get the historic commandes  " + jsonArrayString);
                                                Log.e("DLA", "get the historic commandes 1");
                                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                Log.e("DLA", "get the historic commandes 2");
                                                JSONObject history1 = new JSONObject();
                                                history1.put("role", "assistant");
                                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);

                                                existingHistoryArray.put(history1);
                                                // Stocker la nouvelle version de l'historique
                                                if (existingHistoryArray.length() > Integer.parseInt(teamChatBuddyApplication.getParamFromFile("COMMAND_maxdialog", "TeamChatBuddy.properties"))) {

                                                    existingHistoryArray.remove(1);
                                                    existingHistoryArray.remove(1);

                                                    teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());

                                                } else {
                                                    teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                    }
                                }
                            }
                        });
                    }
                    else if (MainActivity.isFirstLaunch && teamChatBuddyApplication.getParamFromFile("Start_Message_fr","TeamChatBuddy.properties") !=null && (!teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties").isEmpty() || !teamChatBuddyApplication.getParamFromFile("Start_Message_fr","TeamChatBuddy.properties").isEmpty())){
                        mlKitIsDownloading=true;

                        //mlKitIsDownloading=false;

                        Log.i("MYA_fragment", "MainFragment : isFirstLaunch");
                        teamChatBuddyApplication.isStartMsg = true;
                        teamChatBuddyApplication.notifyObservers("hideCameraQr");
                        //Log.d("MainFragment", "------------ hideCameraQr mlkit -------------");
                        playStartMessage(new IStartMessageCallback() {
                            @Override
                            public void onEnd(String s) {
                                Log.e(TAG,"end of playing Start Message");
                                mlKitIsDownloading=false;
                                teamChatBuddyApplication.setSpeaking(false);
                                teamChatBuddyApplication.isStartMsg = false;
                                //teamChatBuddyApplication.listeningState = "hotword";
                                Log.i("MYA_fragment", "MainFragment : playStartMessage --- onEnd before conditions");
                                Log.i("MYA_fragment", "teamChatBuddyApplication.getparam(\"Tracking_Activation\") "+teamChatBuddyApplication.getparam("Tracking_Activation"));
                                Log.i("MYA_fragment", "!teamChatBuddyApplication.getSpeaking() "+!teamChatBuddyApplication.getSpeaking());
                                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                                    if (!teamChatBuddyApplication.getSpeaking()){
                                        Log.i("MYA_fragment", "MainFragment : playStartMessage --- startListeningHotwor 1 ");
                                        teamChatBuddyApplication.startListeningHotwor(getActivity());
                                    }
                                    updateCameraPreviewVisibility();
                                    if (teamChatBuddyApplication.isShouldDisplayQRCode()) {
                                        Log.e(TAG,"playStartMessage fonction Display---------------");


                                        if(!teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim().equalsIgnoreCase("0")){

                                            teamChatBuddyApplication.notifyObservers("hideCameraQr");
                                            //Log.d("MainFragment", "------------ hideCameraQr mlkit play no tracking -------------");

                                        }
                                        displayQRCode(new IDisplayQrCodeCallback() {
                                            @Override
                                            public void onEnd() {
                                            }
                                        });
                                    }
                                    else {
                                        if (timerPeriodToDisplayQRCode!=null) timerPeriodToDisplayQRCode.start();
                                    }
                                }
                                else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                                    if (!teamChatBuddyApplication.getSpeaking()){
                                        Log.i("MYA_fragment", "MainFragment : playStartMessage --- startListeningHotwor 2 ");
                                        teamChatBuddyApplication.startListeningHotwor(getActivity());
                                    }
                                    if (teamChatBuddyApplication.isShouldDisplayQRCode()) {
                                        Log.e(TAG,"playStartMessage 2 fonction Display---------------");
                                        if(!teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim().equalsIgnoreCase("0")){

                                            teamChatBuddyApplication.notifyObservers("hideCameraQr");
                                            //Log.d("MainFragment", "------------ hideCameraQr mlkit yes tracking -------------");

                                        }
                                        displayQRCode(new IDisplayQrCodeCallback() {
                                            @Override
                                            public void onEnd() {
                                            }
                                        });

                                        isReTrack = false;
                                        initTracking();
                                    }
                                    else{
                                        if (timerPeriodToDisplayQRCode!=null) timerPeriodToDisplayQRCode.start();
                                        isReTrack = false;
                                        initTracking();
                                    }
                                }
                            }
                        });
                    }
                    else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                        if (!teamChatBuddyApplication.getSpeaking()){
                            String listeningMode = teamChatBuddyApplication.getparam("listening_mode");
                            if(listeningMode==null || listeningMode.isEmpty()){
                                Log.e(TAG,"startListeningHotword 3");
                                teamChatBuddyApplication.startListeningHotwor(getActivity());
                                Log.i("MYA_fragment", "MainFragment : should startListeningHotwor ");
                            }
                        }
                        updateCameraPreviewVisibility();
                        if (teamChatBuddyApplication.isShouldDisplayQRCode() && !qr_displayed) {


                            if(!teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim().equalsIgnoreCase("0")){

                                teamChatBuddyApplication.notifyObservers("hideCameraQr");
                                //Log.d("MainFragment", "------------ hideCameraQr !qr_displayed -------------");

                            }
                            Log.e("TEST_QR","loading model fonction Display---------------");
                            displayQRCode(new IDisplayQrCodeCallback() {
                                @Override
                                public void onEnd() {
                                    if (!teamChatBuddyApplication.getSpeaking()){
                                    }

                                }
                            });
                        }
                        else {
                            if (timerPeriodToDisplayQRCode!=null) timerPeriodToDisplayQRCode.start();
                        }
                    }
                    else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                        if (!teamChatBuddyApplication.getSpeaking()){
                            String listeningMode = teamChatBuddyApplication.getparam("listening_mode");
                            if(listeningMode==null || listeningMode.isEmpty()){
                                Log.e(TAG,"startListeningHotword 4");
                                Log.i("MYA_fragment", "MainFragment : playStartMessage --- startListeningHotwor 3 ");
                                teamChatBuddyApplication.startListeningHotwor(getActivity());
                            }
                        }
                        if (teamChatBuddyApplication.isShouldDisplayQRCode() && !qr_displayed) {
                            if(!teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim().equalsIgnoreCase("0")){
                                teamChatBuddyApplication.notifyObservers("hideCameraQr");
                                //Log.d("MainFragment", "------------ hideCameraQr mlkit Tracking_Activation -------------");
                            }
                            Log.e(TAG,"loading model 2 fonction Display---------------");
                            displayQRCode(new IDisplayQrCodeCallback() {
                                @Override
                                public void onEnd() {
                                }
                            });
                            isReTrack = false;
                            initTracking();
                        }
                        else{
                            if (timerPeriodToDisplayQRCode!=null) timerPeriodToDisplayQRCode.start();
                            isReTrack = false;
                            initTracking();
                        }
                    }
                    if(MainActivity.isFirstLaunch)teamChatBuddyApplication.initAlert();
                    Boolean wasAlterActivated = Boolean.parseBoolean(teamChatBuddyApplication.getparam("wasAlterActivated"));
                    if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")&&!wasAlterActivated){
                        AlertManager.getInstance(_parentActivity).start();
                    }
                    teamChatBuddyApplication.setparam("previousLanguage",new Gson().toJson(teamChatBuddyApplication.getLangue()));


                    Log.i("MYA_QR", "teamChatBuddyApplication.getparam(\"Lecture_QR_Code\") " + teamChatBuddyApplication.getParamFromFile("Lecture_QR_Code","TeamChatBuddy.properties"));

                    if(teamChatBuddyApplication.getParamFromFile("Lecture_QR_Code","TeamChatBuddy.properties").trim().equalsIgnoreCase("yes")){
                        teamChatBuddyApplication.createAndDeployDefaultAprilTagJson();
                        Log.i("MYA_QR", "init Lecture_QR_Code ");
                        types = teamChatBuddyApplication.listQRTypes;
                        Log.i("MYA_QR", "QRCode_System : "+types);
                        initScaning(types);
                    }
                    else {
                        teamChatBuddyApplication.stopQRCode();
                        if (cameraUtils != null) {
                            cameraUtils.stopCamera();
                        }
                        Log.i("MYA_QR", "init NO Lecture_QR_Code ");
                    }



                }
            }
            else{
                commande.translate("CMD_LANGUE", new Commande.ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                        }
                    }
                });
                mlKitIsDownloading = true;
                french_is_downloaded = false;
                english_is_downloaded = false;
                languageToEnglish_is_downloaded =false;
                List<String> mlkitLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
                String codeLanguageMlkit = mlkitLangueCode.get(new Gson().fromJson(teamChatBuddyApplication.getparam(settingClass.getLangue()), Langue.class).getId()-1);
                teamChatBuddyApplication.downloadModel(imlKitDownloadCallback,codeLanguageMlkit.trim());
                handlerProgressBar.postDelayed(runnableProgressBar,500);
                timerDownloading.start();
            }
        }
    };
    private Handler handlerProgressBar = new Handler(Looper.getMainLooper());
    private Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {
                            teamChatBuddyApplication.stopTTS(true); // Arrêt explicite demandé par l'utilisateur
            stopListeningFreeSpeech();
            try {
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                BuddySDK.UI.stopListenAnimation();
            } catch (Exception e) {
                Log.e(TAG, "BuddySDK Exception  " + e);
            }
            launch_view.setVisibility(View.VISIBLE);
        }
    };
    private Handler handlerForSensor;
    private Runnable runnableForSensor;
    private Handler handlerTTSError = new Handler();
    private Runnable runnableTTSError;
    private Handler handlerPauseTime = new Handler();
    private Runnable runnablePauseTime;
    private Handler handler = new Handler();
    private Runnable runnable;
    private Handler handlerForClickBouton = new Handler();
    private Runnable runnableforClickBouton;
    private Handler mouthDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable mouthDebounceRunnable;
    private int mouthMessageGeneration = 0;
    private String mouthPendingAction = null;
    private boolean mouthPendingNotifyEndOfTimer = false;
    private int clickCount = 0;
    private static final int CLICK_TIMEOUT = 2000;
    private Handler clickHandler = new Handler();
    private Runnable resetClickCountRunnable = new Runnable() {
        @Override
        public void run() {
            clickCount = 0;
        }
    };
    private Boolean isFirstResponse = true;
    private Boolean isQuestionAlreadyDetected = false;
    private ObjectAnimator animator;
    boolean isScan = false;
    boolean isTag = false;
    boolean isSpeakingScan = false;

    View view;

    public MainFragment() {
        Log.i(TAG, "MainFragment constructeur");
    }

    public MainFragment(MainActivity _parentActivity) {
        this._parentActivity = _parentActivity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.main_fragment_layout, container, false);
        Log.d(TAG," --- onCreateView() MainFragment ---");

        initHasBeenLaunchedFromOnResume = false;
        teamChatBuddyApplication = (TeamChatBuddyApplication) requireActivity().getApplicationContext();
        teamChatBuddyApplication.setInitSharedpreferences(true);


        //init views
        buddy_texte_qst = view.findViewById( R.id.buddy_texte_qst );
        buddy_texte_qst_lyt = view.findViewById( R.id.buddy_texte_qst_lyt );
        buddy_texte_resp = view.findViewById( R.id.buddy_texte_resp );
        buddy_texte_resp_lyt = view.findViewById( R.id.buddy_texte_resp_lyt );
        lyt_open_menu_settings = view.findViewById( R.id.lyt_open_menu_settings );
        lyt_settings_touch_zone = view.findViewById( R.id.lyt_settings_touch_zone );
        lyt_open_menu_chat = view.findViewById( R.id.lyt_open_menu_chat );
        view_face = view.findViewById(R.id.view_face);
        launch_view = view.findViewById(R.id.launch_view);
        noNetwork = view.findViewById(R.id.noNetwork);
        bi_videoView = view.findViewById(R.id.bi_videoView);
        bi_imageView = view.findViewById(R.id.bi_imageView);
        downloadingBar = view.findViewById(R.id.progressBar_MLKitDownload);
        preview_container = view.findViewById(R.id.preview_container);
        reGroup = view.findViewById(R.id.reGroup);
        overlay = view.findViewById(R.id.overlay);
        previewView = view.findViewById(R.id.view_finder);
        photo_timer_bg_rlyt= view.findViewById(R.id.photo_timer_bg_rlyt);
        photo_timer_rlyt = view.findViewById(R.id.photo_timer_rlyt);
        photo_timer_imgview = view.findViewById(R.id.photo_timer_imgview);
        photo_timer_txtView = view.findViewById(R.id.photo_timer_txtView);
        imageArrow = view.findViewById(R.id.imageArrow);
        imageQRCode = view.findViewById(R.id.imageQRCode);
        layoutTexteQR = view.findViewById(R.id.layout_Texte);
        layoutQRCode = view.findViewById(R.id.layout_QRCode);
        textViewQRMessage = view.findViewById(R.id.textViewQRMessage);
        previewView_qr = view.findViewById(R.id.previewView_qr);

        // Initialize settings button as VISIBLE by default (first time ever)
        String firstInitDone = teamChatBuddyApplication.getparam("settings_button_first_init");
        if (firstInitDone == null) {
            // First initialization - set button to VISIBLE
            teamChatBuddyApplication.setparam("settings_button_hidden", "false");
            teamChatBuddyApplication.setparam("settings_button_first_init", "true");
            settingsButtonHiddenByUser = false;
            applySettingsBtnVisibility();
            Log.d(TAG, "Settings button first init: set to VISIBLE");
        } else {
            // Load the saved toggle state for subsequent launches
            String hiddenState = teamChatBuddyApplication.getparam("settings_button_hidden");
            settingsButtonHiddenByUser = (hiddenState != null && hiddenState.equals("true"));
            applySettingsBtnVisibility();
            Log.d(TAG, "Settings button loaded state: " + (settingsButtonHiddenByUser ? "HIDDEN" : "VISIBLE"));
        }

        lyt_open_menu_chat.setOnClickListener(v -> btnOpenChat(v));

        // Settings button: tap court = ouvrir settings, appui long 5s = toggle visibilite
        lyt_open_menu_settings.setOnClickListener(null);
        lyt_open_menu_settings.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    settingsBtnLongPressTriggered = false;
                    settingsRevealHandler.removeCallbacks(settingsRevealRunnable);
                    settingsRevealHandler.postDelayed(settingsRevealRunnable, 5000);
                    Log.d(TAG, "Long press started on settings button");
                    return true;
                case MotionEvent.ACTION_UP:
                    settingsRevealHandler.removeCallbacks(settingsRevealRunnable);
                    if (!settingsBtnLongPressTriggered) {
                        btnOpenSettings(v);
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    settingsRevealHandler.removeCallbacks(settingsRevealRunnable);
                    return true;
            }
            return false;
        });

        // Zone tactile cachee: appui long 5s pour faire reapparaitre le bouton quand il est invisible
        lyt_settings_touch_zone.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    settingsBtnLongPressTriggered = false;
                    settingsRevealHandler.removeCallbacks(settingsRevealRunnable);
                    settingsRevealHandler.postDelayed(settingsRevealRunnable, 5000);
                    Log.d(TAG, "Long press started on settings zone");
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    settingsRevealHandler.removeCallbacks(settingsRevealRunnable);
                    return true;
            }
            return false;
        });

        Intent myIntent = getActivity().getIntent();
        isFirstLaunch = true;
        if (myIntent != null) {
            if (myIntent.hasExtra("fromSettings")) {
                teamChatBuddyApplication.setShouldDisplayQRCode(false);
                String fromString = myIntent.getStringExtra("fromSettings");
                if (fromString != null && fromString.equals("true")) {
                    isFirstLaunch= false;
                    Log.i(TAG_TRACKING,"is back from Settings");
                }
            }
            else if(myIntent.hasExtra("fromChatWindow")){
                teamChatBuddyApplication.setShouldDisplayQRCode(false);
                String fromChatWindow = myIntent.getStringExtra("fromChatWindow");
                if (fromChatWindow != null && fromChatWindow.equals("true")) {
                    isFirstLaunch= false;
                    Log.i(TAG_TRACKING,"is back from ChatWindow");
                }
            }
            else {
                teamChatBuddyApplication.setShouldDisplayQRCode(true);
                teamChatBuddyApplication.setSpeaking(false);
                teamChatBuddyApplication.setNotYet(true);
                teamChatBuddyApplication.setActivityClosed(false);
                teamChatBuddyApplication.setStartRecording(false);
                teamChatBuddyApplication.setUsingEmotions(false);
                teamChatBuddyApplication.setQuestionNumber(0);
                teamChatBuddyApplication.setCurrentQuestionNubmer(0);
                teamChatBuddyApplication.setAlreadyGetAnswer(false);
                teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
                teamChatBuddyApplication.setTimeoutExpired(false);
                teamChatBuddyApplication.setQuestionTime(0);
                teamChatBuddyApplication.setStoredResponse("");
                teamChatBuddyApplication.setBuddyFaceisTired(false);
                teamChatBuddyApplication.setShouldPlayEmotion(false);
                teamChatBuddyApplication.setCurrentEmotion("");
                teamChatBuddyApplication.setMessageError(false);
                teamChatBuddyApplication.setCurrentIndexText(0);
                teamChatBuddyApplication.setAllTextPronoucedSuccess(true);
                teamChatBuddyApplication.setStop_TTS_ReadSpeaker(false);
                teamChatBuddyApplication.setInitSharedpreferences(true);
                teamChatBuddyApplication.setLanguageDetected("");
                teamChatBuddyApplication.setAlreadyCalled(false);
                teamChatBuddyApplication.setRecording(false);
                teamChatBuddyApplication.setCurrentState("");


                teamChatBuddyApplication.setStopProcessus(false);
                teamChatBuddyApplication.setAlReadyHadSpoke(false);

                teamChatBuddyApplication.setGetResponseTime(0);
                teamChatBuddyApplication.setAnswerHasExceededTimeOut(false);
                teamChatBuddyApplication.setPreviousVolume(Float.valueOf(0));
                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                teamChatBuddyApplication.setChosenTTS("");
                teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                teamChatBuddyApplication.setBIExecution(false);
                teamChatBuddyApplication.setAlreadyChatting(false);
                teamChatBuddyApplication.setShouldLaunchListeningAfterGetingHotWord(true);
                Log.i(TAG,"First launch of application myIntent!=null 2");
            }
        }
        else {
            teamChatBuddyApplication.setShouldDisplayQRCode(true);
            teamChatBuddyApplication.setSpeaking(false);
            teamChatBuddyApplication.setNotYet(true);
            teamChatBuddyApplication.setActivityClosed(false);
            teamChatBuddyApplication.setStartRecording(false);
            teamChatBuddyApplication.setUsingEmotions(false);
            teamChatBuddyApplication.setQuestionNumber(0);
            teamChatBuddyApplication.setCurrentQuestionNubmer(0);
            teamChatBuddyApplication.setAlreadyGetAnswer(false);
            teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
            teamChatBuddyApplication.setTimeoutExpired(false);
            teamChatBuddyApplication.setQuestionTime(0);
            teamChatBuddyApplication.setStoredResponse("");
            teamChatBuddyApplication.setBuddyFaceisTired(false);
            teamChatBuddyApplication.setShouldPlayEmotion(false);
            teamChatBuddyApplication.setCurrentEmotion("");
            teamChatBuddyApplication.setMessageError(false);
            teamChatBuddyApplication.setCurrentIndexText(0);
            teamChatBuddyApplication.setAllTextPronoucedSuccess(true);
            teamChatBuddyApplication.setStop_TTS_ReadSpeaker(false);
            teamChatBuddyApplication.setInitSharedpreferences(true);
            teamChatBuddyApplication.setLanguageDetected("");
            teamChatBuddyApplication.setAlreadyCalled(false);
            teamChatBuddyApplication.setRecording(false);
            teamChatBuddyApplication.setCurrentState("");

            teamChatBuddyApplication.setStopProcessus(false);
            teamChatBuddyApplication.setAlReadyHadSpoke(false);

            teamChatBuddyApplication.setGetResponseTime(0);
            teamChatBuddyApplication.setAnswerHasExceededTimeOut(false);
            teamChatBuddyApplication.setPreviousVolume(Float.valueOf(0));
            teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
            teamChatBuddyApplication.setChosenTTS("");
            teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
            teamChatBuddyApplication.setBIExecution(false);
            teamChatBuddyApplication.setAlreadyChatting(false);
            teamChatBuddyApplication.setShouldLaunchListeningAfterGetingHotWord(true);
            Log.i(TAG,"First launch of application");
        }

        /**
         * init animated drawables for timer
         */
        animationTimerPhoto = new AnimationDrawable();
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0001), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0002), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0003), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0004), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0005), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0006), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0007), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0008), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0009), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0010), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0011), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0012), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0013), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0014), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0015), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0016), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0017), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0018), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0019), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0020), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0021), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0022), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0023), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0024), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0025), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0026), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0027), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0028), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0029), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0030), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0031), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0032), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0033), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0034), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0035), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0036), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0037), 1000/37);

        if (_parentActivity != null) AlertManager.getInstance(_parentActivity);
        else Log.e("FZE", "onCreateView — _parentActivity null, AlertManager non initialisé");
        teamChatBuddyApplication.isOnApp = true;

        teamChatBuddyApplication.setparam("qr_time_remaining",0+"");
        teamChatBuddyApplication.setparam("qr_displayed", String.valueOf(false));
        teamChatBuddyApplication.setparam("listening_mode","");
        teamChatBuddyApplication.setparam("remaining_attempts","");

        if(commande!=null){
            commande.CMD_STOP_MUSIC();
            commande.CMD_STOP_RADIO();
        }

        teamChatBuddyApplication.setPlayingMusic(false);
        teamChatBuddyApplication.setPlayingRadio(false);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG," --- onCreate() MainFragment ---");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG," --- onResume() ---");
        teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(requireActivity()));
        }
        currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
        totalTimeLookingAtCamera = 0L;
        try {
            //AlertManager.getInstance(_parentActivity).resume();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du resume de AlertManager: " + e.getMessage());
        }
        
        // Restore settings button visibility state on resume (from settings or other fragments)
        if (lyt_open_menu_settings != null) {
            String hiddenState = teamChatBuddyApplication.getparam("settings_button_hidden");
            if (hiddenState != null && hiddenState.equals("true")) {
                settingsButtonHiddenByUser = true;
                applySettingsBtnVisibility();
            } else {
                settingsButtonHiddenByUser = false;
                applySettingsBtnVisibility();
            }
        }
        
        init();
        if(!MainActivity.isFirstLaunch){
            String qrDisplayedStr = teamChatBuddyApplication.getparam("qr_displayed");
            String qrTimeRemainingStr = teamChatBuddyApplication.getparam("qr_time_remaining");

            boolean qrDisplayed = qrDisplayedStr != null && qrDisplayedStr.equalsIgnoreCase("true");
            long timeRemaining = 0;

            if (qrTimeRemainingStr != null && !qrTimeRemainingStr.isEmpty()) {
                try {
                    timeRemaining = Long.parseLong(qrTimeRemainingStr);
                } catch (NumberFormatException e) {
                    Log.e("MainFragment", "Erreur conversion qrTimeRemaining : " + e.getMessage());
                }
            }

            // Si le QR code était encore affiché avant la pause
            if (qrDisplayed) {
                if (timeRemaining > 0) {
                    qr_displayed = qrDisplayed;
                    qrTimeRemaining = timeRemaining;
                    Log.i("MainFragment", "Reprise du timer QR (" + timeRemaining + " ms restants)");
                    if (timerDisplayQRCode!=null){
                        timerDisplayQRCode.cancel();
                    }
                    if (timerPeriodToDisplayQRCode!=null){
                        timerPeriodToDisplayQRCode.cancel();
                    }
                    Log.e(TAG,"call displayQRCode 3 ");
                    teamChatBuddyApplication.notifyObservers("hideCameraQr");
                    //Log.d("MainFragment", "------------ hideCameraQr onResume -------------");
                    displayQRCode(new IDisplayQrCodeCallback() {
                        @Override
                        public void onEnd() {
                        }
                    });
                } else {
                    Log.i("MainFragment", "Le QR était affiché mais timer écoulé → on le masque");
                    // éventuellement displayQRCode() pour le masquer ou le réinitialiser
                }
            }
            else {
                //Log.d("MainFragment", "------------ showCameraQr onResume -------------");
                if (!shouldHideCameraPreview()) {
                    teamChatBuddyApplication.notifyObservers("showCameraQr");
                }
            }

            String listeningMode = teamChatBuddyApplication.getparam("listening_mode");
            Log.i("MainFragment", "listening_mode : "+listeningMode);
            if (listeningMode.equals("hotword")) teamChatBuddyApplication.startListeningHotwor(_parentActivity);
            else if (listeningMode.equals("listening")) {
                startNextCycle();
            }
            //startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());

            String remaining_attempts = teamChatBuddyApplication.getparam("remaining_attempts");
            if(remaining_attempts!=null && !remaining_attempts.isEmpty()){
                teamChatBuddyApplication.setRemainingAttempts(Integer.parseInt(remaining_attempts));
            }

            if(teamChatBuddyApplication.isPlayingMusic()){
                Log.i("MUSIC_DEBUG", "onResume() → isPlayingMusic=true, musicPlayer=" + (commande.musicPlayer != null ? "non-null" : "NULL") + ", prepared=" + Commande.musicPlayerPrepared);
                if (commande.musicPlayer != null) {
                    if (Commande.musicPlayerPrepared) {
                        try {
                            if (!commande.musicPlayer.isPlaying()) {
                                Log.i("MUSIC_DEBUG", "onResume() → musicPlayer.start() appelé");
                                commande.musicPlayer.start();
                            } else {
                                Log.i("MUSIC_DEBUG", "onResume() → music already playing, skip");
                            }
                        } catch (IllegalStateException e) {
                            Log.w("MUSIC_DEBUG", "onResume() → IllegalStateException, onPrepared lancera start()");
                        }
                    } else {
                        Log.w("MUSIC_DEBUG", "onResume() → musicPlayer pas encore prêt, onPrepared lancera start()");
                    }
                } else {
                    Log.w("MUSIC_DEBUG", "onResume() → musicPlayer est NULL, start ignoré");
                }
            }

         if(teamChatBuddyApplication.isPlayingRadio()){
                Log.i("RADIO_DEBUG", "onResume() → isPlayingRadio=true, radioPlayer=" + (commande.radioPlayer != null ? "non-null" : "NULL") + ", prepared=" + Commande.radioPlayerPrepared);
                if (commande.radioPlayer != null) {
                    if (Commande.radioPlayerPrepared) {
                        try {
                            if (!commande.radioPlayer.isPlaying()) {
                                Log.i("RADIO_DEBUG", "onResume() → radioPlayer.start() appelé");
                                commande.radioPlayer.start();
                            } else {
                                Log.i("RADIO_DEBUG", "onResume() → radio already playing, skip");
                            }
                        } catch (IllegalStateException e) {
                            Log.w("RADIO_DEBUG", "onResume() → IllegalStateException, onPrepared lancera start()");
                        }
                    } else {
                        Log.w("RADIO_DEBUG", "onResume() → radioPlayer pas encore prêt, onPrepared lancera start()");
                    }
                } else {
                    Log.w("RADIO_DEBUG", "onResume() → radioPlayer est NULL, start ignoré");
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG," --- onPause() ---");
        Log.d("MYA_fragment"," --- onPause() MainFragment ---");
        teamChatBuddyApplication.listeningState = "traitement";
        teamChatBuddyApplication.isOnApp = false;
        initHasBeenLaunchedFromOnResume=true;
        if(ResponseFromChatbot.responseTimeout !=null){
            ResponseFromChatbot.responseTimeout.cancel();
        }
        if(ChatGptStreamMode.responseTimeout !=null){
            ChatGptStreamMode.responseTimeout.cancel();
        }
        if(CustomGPTStreamMode.responseTimeout !=null){
            CustomGPTStreamMode.responseTimeout.cancel();
        }
        if (timerToApplyBI!=null) timerToApplyBI.cancel();
        if(teamChatBuddyApplication.getChatGptStreamMode() != null){
            teamChatBuddyApplication.getChatGptStreamMode().reset();
        }
        if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
            teamChatBuddyApplication.getCustomGPTStreamMode().reset();
        }
        if(handlerTTSError!=null && runnableTTSError!=null){
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null);
        }
        if (handlerForSensor != null && runnableForSensor != null) {

            handlerForSensor.removeCallbacksAndMessages(null);
            handlerForSensor.removeCallbacks(runnableForSensor);

        }
        if(handler!=null && runnable!=null){
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
        }
        if(handlerForClickBouton!=null && runnableforClickBouton!=null){
            handlerForClickBouton.removeCallbacks(runnableforClickBouton);
            handlerForClickBouton.removeCallbacksAndMessages(null);
        }
        if (timerDisplayQRCode!=null){
            timerDisplayQRCode.cancel();
        }
        if (timerPeriodToDisplayQRCode!=null){
            timerPeriodToDisplayQRCode.cancel();
        }
        onSdkReadyIsAlreadyCalledOnce = false;
        isListeningFreeSpeech = false;
        listRep=new ArrayList();
        gptSend=false;
        teamChatBuddyApplication.removeObserver(this);
                            teamChatBuddyApplication.stopTTS(false); // Arrêt automatique ignoré
        teamChatBuddyApplication.setActivityClosed(true);
        teamChatBuddyApplication.setStartRecording(false);
        teamChatBuddyApplication.setSpeaking(false);
        teamChatBuddyApplication.setShouldLaunchListeningAfterGetingHotWord(true);
        teamChatBuddyApplication.setModeContinuousListeningON(false);
        stopListeningFreeSpeech();
        CustomToast.getInstance().hideToast();
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            BuddySDK.UI.removeFaceTouchListener(iuiFaceTouchCallback);
        }
        catch (Exception e){
            Log.e(TAG,"BuddySDK Exception  "+e);
        }
        if(cameraProvider != null) cameraProvider.unbindAll();
        MainActivity.isFirstLaunch= false;

        teamChatBuddyApplication.stopQRCode();
        if (cameraUtils != null) {
            cameraUtils.stopCamera();
        }
        isSpeakingScan = false;


        //todo: Sauvegarde des états de la mise en pause
        teamChatBuddyApplication.setparam("qr_time_remaining",qrTimeRemaining+"");
        teamChatBuddyApplication.setparam("qr_displayed", String.valueOf(qr_displayed));

        if(teamChatBuddyApplication.getAppIsListeningToTheQuestion()){
            Log.e("MYA_fragment","----------- getAppIsListeningToTheQuestion()  "+teamChatBuddyApplication.getAppIsListeningToTheQuestion());
            teamChatBuddyApplication.setparam("listening_mode","listening");
        } else if(teamChatBuddyApplication.getSpeaking()){
            Log.e("MYA_fragment","----------- teamChatBuddyApplication.getSpeaking()  "+teamChatBuddyApplication.getSpeaking());
            teamChatBuddyApplication.setparam("listening_mode","speaking");
        } else {
            Log.e("MYA_fragment","----------- teamChatBuddyApplication.isListeningHotw  "+teamChatBuddyApplication.isListeningHotw);
            teamChatBuddyApplication.setparam("listening_mode","hotword");
        }

        teamChatBuddyApplication.setparam("remaining_attempts",String.valueOf(teamChatBuddyApplication.getRemainingAttempts()));
        Log.i("MUSIC_DEBUG", "onPause() → isPlayingMusic=" + teamChatBuddyApplication.isPlayingMusic() + ", isOnApp=" + teamChatBuddyApplication.isOnApp);
        if(teamChatBuddyApplication.isPlayingMusic()){
            if (commande.musicPlayer != null) {
                try {
                    if (commande.musicPlayer.isPlaying()) {
                        Log.i("MUSIC_DEBUG", "onPause() → musicPlayer.pause() appelé");
                        commande.musicPlayer.pause();
                    } else {
                        Log.w("MUSIC_DEBUG", "onPause() → musicPlayer not playing, pause ignorée");
                    }
                } catch (IllegalStateException e) {
                    Log.w("MUSIC_DEBUG", "onPause() → IllegalStateException, pause ignorée");
                }
            } else {
                Log.w("MUSIC_DEBUG", "onPause() → musicPlayer est NULL, pause ignorée");
            }
        }
        Log.i("RADIO_DEBUG", "onPause() → isPlayingRadio=" + teamChatBuddyApplication.isPlayingRadio() + ", isOnApp=" + teamChatBuddyApplication.isOnApp);
        if(teamChatBuddyApplication.isPlayingRadio()){
            if (commande.radioPlayer != null) {
                Log.i("RADIO_DEBUG", "onPause() → radioPlayer.pause() appelé, isPlaying=" + commande.radioPlayer.isPlaying());
                if (commande.radioPlayer.isPlaying()) {
                    commande.radioPlayer.pause();
                } else {
                    Log.w("RADIO_DEBUG", "onPause() → radioPlayer not playing, pause ignorée");
                }
            } else {
                Log.w("RADIO_DEBUG", "onPause() → radioPlayer est NULL, pause ignorée");
            }
        }
        try {
            //AlertManager.getInstance(_parentActivity).pause();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du pause de AlertManager: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG," --- onDestroy() main fragment---");
        if(!teamChatBuddyApplication.isOnApp && teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")){
            //AlertManager.getInstance(_parentActivity).stop();
        }
        closeImage();
        try {
            requireActivity().unregisterReceiver(wifiBroadCastReceiver);
        }catch(IllegalArgumentException e) {
            Log.i(TAG,"---unregisterReceiver wifiBroadcast:: IllegalArgumentException---"+e.getMessage());
        }
        teamChatBuddyApplication.setparam("firstLaunch","true");
        teamChatBuddyApplication.setFileCreate(true);
        teamChatBuddyApplication.notifyObservers("main destroy");
        handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
        handlerCheckPersonDetection.removeCallbacksAndMessages(null);
        if(poseTracking != null) poseTracking.stopMovingAndCancelRunnables();
        if(backgroundExecutor != null) backgroundExecutor.shutdownNow();
        try{
            if(!BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || !BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
                BuddySDK.USB.enableWheels(false, iUsbCommadRspTracking);
            }
            if(!BuddySDK.Actuators.getYesStatus().toUpperCase().contains("DISABLE")) {
                BuddySDK.USB.enableYesMove(false, iUsbCommadRspTracking);
            }
            if(!BuddySDK.Actuators.getNoStatus().toUpperCase().contains("DISABLE")) {
                BuddySDK.USB.enableNoMove(false, iUsbCommadRspTracking);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
            BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);
            BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
        }
        downloadingBar.setVisibility(View.GONE);
        launch_view.setVisibility(View.GONE);
        if(currentToast!=null){
            currentToast.cancel();
        }

        if (timerDisplayQRCode!=null){
            timerDisplayQRCode.cancel();
        }
        if (timerPeriodToDisplayQRCode!=null){
            timerPeriodToDisplayQRCode.cancel();
        }

        teamChatBuddyApplication.stopQRCode();
        if (cameraUtils != null) {

            cameraUtils.stopCamera();

        }
        if (Commande.radioPlayer != null)commande.CMD_STOP_RADIO();
        if (Commande.musicPlayer != null)commande.CMD_STOP_MUSIC();
    }

    public void onEvent(EventItem iEvent){
        Log.w(TAG, "onEvent : "+iEvent.toString());
    }

    private final IUsbCommadRsp iUsbCommadRspBI = new IUsbCommadRsp.Stub(){
        @Override
        public void onSuccess(String s) throws RemoteException {
            Log.e("DEBUG_BI","detectBI onSuccess");
            runnableForSensor= new Runnable() {
                public void run() {
                    detectBI();
                    handlerForSensor.postDelayed(this, 100);
                }
            };
            handlerForSensor.post(runnableForSensor);
        }
        @Override
        public void onFailed(String s) throws RemoteException {
            Log.e("DEBUG_BI","detectBI onFailed");
        }
    };

    private final IUsbCommadRsp iUsbCommadRspTracking = new IUsbCommadRsp.Stub(){
        @Override
        public void onSuccess(String s) throws RemoteException {}
        @Override
        public void onFailed(String s) throws RemoteException {}
    };

    private final IUIFaceTouchCallback iuiFaceTouchCallback = new IUIFaceTouchCallback.Stub() {
        @Override
        public void onTouch(FaceTouchData faceTouchData) throws RemoteException {

            if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")) {
//                AlertManager.getInstance(_parentActivity).incremente("touch", _parentActivity);
            }
            Log.d("ALERTE", "FaceTouchData ");
            //Right_Eyebrow
            if(faceTouchData.getX() < 460 &&  faceTouchData.getY() < 250){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume"))>0){
                            int curr = Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume")) - 10;
                            if (curr<0){
                                curr=0;
                            }
                            teamChatBuddyApplication.setVolume(curr,AudioManager.FLAG_SHOW_UI);
                            teamChatBuddyApplication.setparam("speak_volume", String.valueOf(curr));
                            teamChatBuddyApplication.setSpeakVolume(curr);
                            settingClass.setVolume(String.valueOf(curr));
                            Log.d(TAG, "RIGHT_EYE update volume : " + curr);
                        }
                        else{
                            Log.d(TAG, "Volume MIN");
                        }
                    }
                });
            }

            //Left_Eyebrow
            else if(faceTouchData.getX() > 720 &&  faceTouchData.getY() < 250){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume"))<100){
                            int curr = Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume")) + 10;
                            if (curr>100){
                                curr=100;
                            }
                            teamChatBuddyApplication.setVolume(curr,AudioManager.FLAG_SHOW_UI);
                            teamChatBuddyApplication.setparam("speak_volume", String.valueOf(curr));
                            teamChatBuddyApplication.setSpeakVolume(curr);
                            settingClass.setVolume(String.valueOf(curr));
                            Log.d(TAG, "LEFT_EYE update volume : " + curr);
                        }
                        else{
                            Log.d(TAG, "Volume MAX");
                        }
                    }
                });
            }

            //eyes
            else if (faceTouchData.getY() > 250 && faceTouchData.getY() < 568){
                Log.e("FCHH","click1");
                if (teamChatBuddyApplication.getparam("Stimulis").contains("yes")) {
                    if (!teamChatBuddyApplication.getAppIsCurrentlyDealingWithTheQuestion() && !currentlyBIExecuted) {
                        if (faceTouchData.getX()>200 && faceTouchData.getX()<565){
                            if (teamChatBuddyApplication.getParamFromFile("touchLeftEye_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchLeftEye_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")) {
                                executeBI("touchLeftEye_Behavior");
                            }
                        }
                        else if (faceTouchData.getX()>773 && faceTouchData.getX()<1120){
                            if (teamChatBuddyApplication.getParamFromFile("touchRightEye_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchRightEye_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")) {
                                executeBI("touchRightEye_Behavior");
                            }
                        }
                        else {
                            if (teamChatBuddyApplication.getParamFromFile("touchFace_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchFace_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")) {
                                executeBI("touchFace_Behavior");
                            }
                        }
                    }
                }
            }

            //Mouth
            else if(faceTouchData.getX() > 400 && faceTouchData.getX() < 820 &&  faceTouchData.getY() > 568){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("MOUTH_DEBUG", "──── Mouth touched ────");
                        Log.i("MOUTH_DEBUG", "  getSpeaking=" + teamChatBuddyApplication.getSpeaking()
                                + "  isSpeaking=" + isSpeaking
                                + "  isSpeakingScan=" + isSpeakingScan
                                + "  isListeningFreeSpeech=" + isListeningFreeSpeech
                                + "  isMouthMessagePlaying=" + teamChatBuddyApplication.isMouthMessagePlaying
                                + "  mouthPendingAction=" + mouthPendingAction
                                + "  mlKitIsDownloading=" + mlKitIsDownloading);

                        // ── IMMÉDIAT : arrêter tout ce qui joue/enregistre ──────────────────

                        // 1. Annuler les handlers en cours
                        isSplitStreamingNews = false;
                        boolean wasSpeakingResponse = isSpeaking && !teamChatBuddyApplication.isMouthMessagePlaying;
                        isSpeaking = false;
                        if (iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                        if (handler != null && runnable != null) {
                            handler.removeCallbacks(runnable);
                            handler.removeCallbacksAndMessages(null);
                        }
                        if (ResponseFromChatbot.responseTimeout != null) ResponseFromChatbot.responseTimeout.cancel();
                        if (ChatGptStreamMode.responseTimeout != null) ChatGptStreamMode.responseTimeout.cancel();
                        if (CustomGPTStreamMode.responseTimeout != null) CustomGPTStreamMode.responseTimeout.cancel();
                        if (teamChatBuddyApplication.getChatGptStreamMode() != null) teamChatBuddyApplication.getChatGptStreamMode().reset();
                        if (teamChatBuddyApplication.getCustomGPTStreamMode() != null) teamChatBuddyApplication.getCustomGPTStreamMode().reset();
                        if (handlerTTSError != null && runnableTTSError != null) {
                            handlerTTSError.removeCallbacks(runnableTTSError);
                            handlerTTSError.removeCallbacksAndMessages(null);
                        }

                        // 2. BI en cours → stopper et ignorer
                        if (teamChatBuddyApplication.getBIExecution()) {
                            BIPlayer.getInstance().stopBehaviour();
                            mouthPendingAction = null;
                            return;
                        }

                        // 2.5. Buddy traite la question (chatbot en vol, pas encore parlé) → annuler et retour idle
                        if (teamChatBuddyApplication.getAppIsCurrentlyDealingWithTheQuestion()
                                && !wasSpeakingResponse
                                && !teamChatBuddyApplication.isMouthMessagePlaying) {
                            Log.i("MOUTH_DEBUG", "  → traitement interrompu : annulation + retour idle");
                            teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                            if (buddy_texte_resp_lyt != null) buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                            if (buddy_texte_qst_lyt != null) buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                            applySettingsBtnVisibility();
                            lyt_open_menu_chat.setVisibility(View.VISIBLE);
                            mouthPendingAction = null;
                            return;
                        }

                        // 3. Arrêter le mouth message en cours si actif
                        if (teamChatBuddyApplication.isMouthMessagePlaying) {
                            mouthMessageGeneration++;
                            iMouthMessageCallback = null;
                            teamChatBuddyApplication.isMouthMessagePlaying = false;
                            teamChatBuddyApplication.stopTTS(true);
                            // Effacer immédiatement le texte affiché
                            if (buddy_texte_resp_lyt != null) buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                            if (buddy_texte_qst_lyt != null) buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                            Log.i("MOUTH_DEBUG", "  → mouth message interrompu (gen=" + mouthMessageGeneration + ")");
                        }

                        // 3.5. Buddy parlait une réponse TTS → arrêter et afficher mouth message stop
                        if (wasSpeakingResponse) {
                            mouthMessageGeneration++;
                            teamChatBuddyApplication.stopTTS(true);
                            if (buddy_texte_resp_lyt != null) buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                            if (buddy_texte_qst_lyt != null) buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                            mouthPendingAction = "stop";
                            mouthPendingNotifyEndOfTimer = false;
                            Log.i("MOUTH_DEBUG", "  → réponse TTS interrompue (wasSpeakingResponse), action=stop gen=" + mouthMessageGeneration);
                        }

                        // 4. Déterminer l'action et arrêter le STT immédiatement si en écoute
                        // isListeningFreeSpeech inclus : getSpeaking peut être false pendant l'écoute libre
                        boolean wasListening = teamChatBuddyApplication.getSpeaking() || isSpeakingScan || isListeningFreeSpeech;
                        Log.i("MOUTH_DEBUG", "  wasListening=" + wasListening + " (getSpeaking=" + teamChatBuddyApplication.getSpeaking() + " isSpeakingScan=" + isSpeakingScan + " isListeningFreeSpeech=" + isListeningFreeSpeech + ")");
                        if (wasListening && !mlKitIsDownloading) {
                            Log.i("MOUTH_DEBUG", "  → action=STOP (arrêt écoute immédiat)");
                            mouthPendingAction = "stop";
                            teamChatBuddyApplication.listeningState = "traitement";
                            currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                            totalTimeLookingAtCamera = 0L;
                            teamChatBuddyApplication.isListeningHotw = true;
                            Log.i("OpenAITTS", "click mouth isListeningHotw = true");

                            mouthPendingNotifyEndOfTimer =
                                teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android")
                                || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")
                                || !teamChatBuddyApplication.getAppIsListeningToTheQuestion()
                                || teamChatBuddyApplication.getParamFromFile("Processing_the_audio_sequence", "TeamChatBuddy.properties").trim().equalsIgnoreCase("No");
                            Log.i("MOUTH_DEBUG", "  notifyEndOfTimer=" + mouthPendingNotifyEndOfTimer
                                    + "  STT=" + teamChatBuddyApplication.getparam("STT_chosen")
                                    + "  isListeningToQuestion=" + teamChatBuddyApplication.getAppIsListeningToTheQuestion());

                            if (mouthPendingNotifyEndOfTimer) {
                                Log.i("MOUTH_DEBUG", "  → branche notifyEndOfTimer=true : arrêt immédiat recording");
                                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                teamChatBuddyApplication.setStartRecording(false);
                                teamChatBuddyApplication.setSpeaking(false);
                                teamChatBuddyApplication.setActivityClosed(true);
                                isListeningFreeSpeech = false;
                                if (timerEcoute != null) timerEcoute.cancel();
                                teamChatBuddyApplication.setStoredResponse("");
                                if (buddy_texte_qst_lyt != null && buddy_texte_resp_lyt != null && buddy_texte_qst != null && buddy_texte_resp != null) {
                                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                    buddy_texte_qst.setMovementMethod(null);
                                    buddy_texte_resp.setMovementMethod(null);
                                }
                                applySettingsBtnVisibility();
                                lyt_open_menu_chat.setVisibility(View.VISIBLE);
                                try { BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION); } catch (Exception e) { Log.e(TAG, "BuddySDK Exception " + e); }
                                try { BuddySDK.UI.stopListenAnimation(); } catch (Exception e) { Log.e(TAG, "BuddySDK Exception " + e); }
                            } else {
                                Log.i("MOUTH_DEBUG", "  → branche notifyEndOfTimer=false : traitementAudio");
                                isListeningFreeSpeech = false;
                                try {
                                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                    BuddySDK.UI.stopListenAnimation();
                                } catch (Exception e) { Log.e(TAG, "BuddySDK Exception " + e); }
                                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                                teamChatBuddyApplication.traitementAudio(false);
                            }
                        } else if (mouthPendingAction == null && !mlKitIsDownloading) {
                            // Pas en écoute et pas de debounce en attente → commencer l'écoute
                            mouthPendingAction = "listen";
                            mouthPendingNotifyEndOfTimer = false;
                            Log.i("MOUTH_DEBUG", "  → action=LISTEN (pas en écoute)");
                        } else {
                            Log.i("MOUTH_DEBUG", "  → action conservée=" + mouthPendingAction + " (debounce déjà en attente)");
                        }

                        // ── DEBOUNCE 300ms : jouer le mouth message du dernier clic ──────────

                        if (mouthDebounceRunnable != null) {
                            mouthDebounceHandler.removeCallbacks(mouthDebounceRunnable);
                        }

                        final String actionToExecute = mouthPendingAction;
                        final boolean shouldNotify = mouthPendingNotifyEndOfTimer;
                        final int myGen = mouthMessageGeneration;

                        mouthDebounceRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (myGen != mouthMessageGeneration) {
                                    Log.i("MOUTH_DEBUG", "Debounce ignoré (gen obsolète myGen=" + myGen + " current=" + mouthMessageGeneration + ")");
                                    return;
                                }
                                mouthPendingAction = null;
                                Log.i("MOUTH_DEBUG", "──── Debounce fired action=" + actionToExecute + " shouldNotify=" + shouldNotify + " gen=" + myGen + " ────");

                                String mouth_messages = teamChatBuddyApplication.getParamFromFile("Mouth_messages", "TeamChatBuddy.properties");
                                Log.i("MOUTH_DEBUG", "  Mouth_messages config=" + mouth_messages);

                                if ("stop".equals(actionToExecute)) {
                                    if (mouth_messages != null && mouth_messages.equalsIgnoreCase("Yes")) {
                                        Log.i("MOUTH_DEBUG", "  → speakMouthMessages(stop)");
                                        speakMouthMessages("stop", new IMouthMessageCallback() {
                                            @Override
                                            public void onEnd(String s) {
                                                Log.i("MOUTH_DEBUG", "  speakMouthMessages(stop).onEnd s=" + s + " gen check: myGen=" + myGen + " current=" + mouthMessageGeneration);
                                                if (myGen != mouthMessageGeneration) return;
                                                teamChatBuddyApplication.isMouthMessagePlaying = false;
                                                if (shouldNotify) {
                                                    Log.i("MOUTH_DEBUG", "  → notifyObservers(end of timer)");
                                                    teamChatBuddyApplication.notifyObservers("end of timer");
                                                }
                                            }
                                        });
                                    } else if (shouldNotify) {
                                        Log.i("MOUTH_DEBUG", "  → pas de mouth message, notifyObservers direct");
                                        teamChatBuddyApplication.notifyObservers("end of timer");
                                    } else {
                                        Log.i("MOUTH_DEBUG", "  → pas de mouth message et pas de notify, rien à faire");
                                    }

                                } else if ("listen".equals(actionToExecute)) {
                                    Log.i("MOUTH_DEBUG", "  → commencer l'écoute, isListeningFreeSpeech=" + isListeningFreeSpeech);
                                    teamChatBuddyApplication.listeningState = "qst";
                                    teamChatBuddyApplication.isListeningHotw = false;
                                    List<String> types = teamChatBuddyApplication.listQRTypes;
                                    if (teamChatBuddyApplication.getParamFromFile("Lecture_QR_Code", "TeamChatBuddy.properties").trim().equalsIgnoreCase("yes")) {
                                        initScaning(types);
                                        cameraUtils.readyCamera(types);
                                    }
                                    teamChatBuddyApplication.setStartRecording(true);
                                    teamChatBuddyApplication.setSpeaking(true);
                                    teamChatBuddyApplication.setModeContinuousListeningON(false);
                                    if (!isListeningFreeSpeech) {
                                        if (mouth_messages != null && mouth_messages.equalsIgnoreCase("Yes")) {
                                            Log.i("MOUTH_DEBUG", "  → speakMouthMessages(listen)");
                                            speakMouthMessages("listen", new IMouthMessageCallback() {
                                                @Override
                                                public void onEnd(String s) {
                                                    Log.i("MOUTH_DEBUG", "  speakMouthMessages(listen).onEnd s=" + s + " gen check: myGen=" + myGen + " current=" + mouthMessageGeneration);
                                                    if (myGen != mouthMessageGeneration) return;
                                                    teamChatBuddyApplication.isMouthMessagePlaying = false;
                                                    isListeningFreeSpeech = true;
                                                    teamChatBuddyApplication.setActivityClosed(false);
                                                    startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                                                }
                                            });
                                        } else {
                                            isListeningFreeSpeech = true;
                                            teamChatBuddyApplication.setActivityClosed(false);
                                            startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                                        }
                                    } else {
                                        Log.i("MOUTH_DEBUG", "  → déjà isListeningFreeSpeech=true, écoute directe");
                                    }
                                } else {
                                    Log.w("MOUTH_DEBUG", "  → action inconnue ou null : " + actionToExecute);
                                }
                            }
                        };
                        mouthDebounceHandler.postDelayed(mouthDebounceRunnable, 300);
                    }
                });
            }
        }
        @Override
        public void onRelease(FaceTouchData faceTouchData) throws RemoteException {}
    };

    public void speakMouthMessages(String type, IMouthMessageCallback iMouthMessageCallback){
        Log.i(TAG, "speakMouthMessages : "+type);
        Log.i("MOUTH_DEBUG", "  speakMouthMessages(" + type + ") langue=" + teamChatBuddyApplication.getLangue().getNom() + " gen=" + mouthMessageGeneration);
        this.iMouthMessageCallback = iMouthMessageCallback;
        teamChatBuddyApplication.isMouthMessagePlaying = true;
        teamChatBuddyApplication.stopListening(getActivity());
        if(type.equals("listen")){
            String mouth_listen_fr = teamChatBuddyApplication.getParamFromFile("Mouth_listen_fr", "TeamChatBuddy.properties");
            String mouth_listen_en =  teamChatBuddyApplication.getParamFromFile("Mouth_listen_en", "TeamChatBuddy.properties");

            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                if(mouth_listen_en != null && !mouth_listen_en.isEmpty()){
                    String[] englishMessages = mouth_listen_en.substring(1, mouth_listen_en.length() - 1).split("/");
                    String randomMessageEN = englishMessages[random.nextInt(englishMessages.length)];
                    Log.d(TAG_TRACKING, "Random English Message: " + randomMessageEN);
                    Log.i("MOUTH_DEBUG", "  → speak phrase EN(listen): \"" + randomMessageEN + "\"");
                    teamChatBuddyApplication.setActivityClosed(false);
                    speak(randomMessageEN, "INVITATION");
                }
                else {
                    Log.w("MOUTH_DEBUG", "  speakMouthMessages(listen): no EN config, firing onEnd immediately");
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain English Invitation");
                }
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                if(mouth_listen_fr != null && !mouth_listen_fr.isEmpty()){
                    String[] frenchMessages = mouth_listen_fr.substring(1, mouth_listen_fr.length() - 1).split("/");
                    String randomMessagesFR = frenchMessages[random.nextInt(frenchMessages.length)];
                    Log.d(TAG_TRACKING, "Random French Invitation: " + randomMessagesFR);
                    Log.i("MOUTH_DEBUG", "  → speak phrase FR(listen): \"" + randomMessagesFR + "\"");
                    teamChatBuddyApplication.setActivityClosed(false);
                    speak(randomMessagesFR, "INVITATION");
                }
                else {
                    Log.w("MOUTH_DEBUG", "  speakMouthMessages(listen): no FR config, firing onEnd immediately");
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain French Invitation");
                }
            }
            else {
                if(mouth_listen_en != null && !mouth_listen_en.isEmpty()){
                    String[] englishMessages = mouth_listen_en.substring(1, mouth_listen_en.length() - 1).split("/");
                    String randomMessagesEN = englishMessages[random.nextInt(englishMessages.length)];
                    Log.i("MOUTH_DEBUG", "  → translating EN→other(listen): \"" + randomMessagesEN + "\"");
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(randomMessagesEN)
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    Log.d(TAG_TRACKING, "Translated Invitation: " + translatedText);
                                    Log.i("MOUTH_DEBUG", "  → speak phrase translated(listen): \"" + translatedText + "\"");
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    speak(translatedText, "INVITATION");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG_TRACKING, "Translation failed, using English message : " + randomMessagesEN);
                                    Log.i("MOUTH_DEBUG", "  → speak phrase EN fallback(listen): \"" + randomMessagesEN + "\"");
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    speak(randomMessagesEN, "INVITATION");
                                }
                            });
                }
                else {
                    Log.w("MOUTH_DEBUG", "  speakMouthMessages(listen): no config at all, firing onEnd immediately");
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain English Invitation");
                }
            }
        }
        else if(type.equals("stop")){
            String Mouth_messages_fr = teamChatBuddyApplication.getParamFromFile("Mouth_speak_fr", "TeamChatBuddy.properties");
            String Mouth_messages_en =  teamChatBuddyApplication.getParamFromFile("Mouth_speak_en", "TeamChatBuddy.properties");

            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                if(Mouth_messages_en != null && !Mouth_messages_en.isEmpty()){
                    String[] englishMessages = Mouth_messages_en.substring(1, Mouth_messages_en.length() - 1).split("/");
                    String randomMessagesEN = englishMessages[random.nextInt(englishMessages.length)];
                    Log.d(TAG_TRACKING, "Random English Invitation: " + randomMessagesEN);
                    Log.i("MOUTH_DEBUG", "  → speak phrase EN(stop): \"" + randomMessagesEN + "\"");
                    teamChatBuddyApplication.setActivityClosed(false);
                    speak(randomMessagesEN, "INVITATION");
                }
                else {
                    Log.w("MOUTH_DEBUG", "  speakMouthMessages(stop): no EN config, firing onEnd immediately");
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain English Invitation");
                }
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                if(Mouth_messages_fr != null && !Mouth_messages_fr.isEmpty()){
                    String[] frenchMessages = Mouth_messages_fr.substring(1, Mouth_messages_fr.length() - 1).split("/");
                    String randomMessagesFR = frenchMessages[random.nextInt(frenchMessages.length)];
                    Log.i("MOUTH_DEBUG", "  → speak phrase FR(stop): \"" + randomMessagesFR + "\"");
                    teamChatBuddyApplication.setActivityClosed(false);
                    speak(randomMessagesFR, "INVITATION");
                }
                else {
                    Log.w("MOUTH_DEBUG", "  speakMouthMessages(stop): no FR config, firing onEnd immediately");
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain French Invitation");
                }
            }
            else {
                if(Mouth_messages_en != null && !Mouth_messages_en.isEmpty()){
                    String[] englishMessages = Mouth_messages_en.substring(1, Mouth_messages_en.length() - 1).split("/");
                    String randomMessagesEN = englishMessages[random.nextInt(englishMessages.length)];
                    Log.i("MOUTH_DEBUG", "  → translating EN→other(stop): \"" + randomMessagesEN + "\"");
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(randomMessagesEN)
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    Log.d(TAG_TRACKING, "Translated Invitation: " + translatedText);
                                    Log.i("MOUTH_DEBUG", "  → speak phrase translated(stop): \"" + translatedText + "\"");
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    speak(translatedText, "INVITATION");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG_TRACKING, "Translation failed, using English Invitation: " + randomMessagesEN);
                                    Log.i("MOUTH_DEBUG", "  → speak phrase EN fallback(stop): \"" + randomMessagesEN + "\"");
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    speak(randomMessagesEN, "INVITATION");
                                }
                            });
                }
                else {
                    Log.w("MOUTH_DEBUG", "  speakMouthMessages(stop): no config at all, firing onEnd immediately");
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain English Invitation");
                }
            }
        }
    }
    /**
     * ----------------- Gestion de notifications ---------------------------
     */
    @Override
    public void update(String message) throws IOException {
        if (message != null) {
            Log.e(TAG,"message update main fragment --> "+message);


            if (message.contains("CANCEL_RESPONSE_TIMEOUT")) {
                if(ResponseFromChatbot.responseTimeout !=null){
                    ResponseFromChatbot.responseTimeout.cancel();
                }
                if(ChatGptStreamMode.responseTimeout !=null){
                    ChatGptStreamMode.responseTimeout.cancel();
                }
                if(CustomGPTStreamMode.responseTimeout !=null){
                    CustomGPTStreamMode.responseTimeout.cancel();
                }
            }

            else if (message.contains("MODE_STREAM_TEXT;SPLIT;")) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.split(";SPLIT;").length > 1) {
                            //BuddySDK.UI.stopListenAnimation();

                            String response = message.split(";SPLIT;")[1];
                            //translate Response Title and show the response :
                            if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                showStream("Response",response);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                showStream("Réponse",response);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("de")) {
                                showStream("Antwort",response);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("es")) {
                                showStream("Respuesta",response);
                            }
                            else{
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("Response")
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                showStream(translatedText,response);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                showStream("Response",response);
                                            }
                                        });
                            }
                        }
                    }
                });
            }

            else if (message.contains("MODE_STREAM_SPEAK;SPLIT;")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.split(";SPLIT;").length > 1){
                            String phraseToPronounce = message.split(";SPLIT;")[1];
                            speak(phraseToPronounce, "nothealysa");
                            if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") &&Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                                useListeningNumberWithAutomaicListening= true;
                            }
                        }
                    }
                });
            }

            else if (message.contains("STTHotword_success")) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")) {
                            AlertManager.getInstance(_parentActivity).incremente("hotword", _parentActivity);
                        }
                        Log.e("TEST_HOT","hotword Success");
                        if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                            teamChatBuddyApplication.setSpeaking(true);
                            isListeningFreeSpeech = true;
                            teamChatBuddyApplication.setActivityClosed(false);
                            Log.i("MYA_QR_H", "update STTHotword_success : "+message);
                            Log.i("MYA_QR", "setActivityClosed ---- 12 ---- ");
                            teamChatBuddyApplication.setStartRecording(true);
                            // Délai avant d'accepter un QR question : le user tient encore
                            // le QR hotword levé, on attend qu'il le baisse
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (teamChatBuddyApplication.listeningState.equals("hotword"))
                                    teamChatBuddyApplication.listeningState = "qst";
                            }, 3000);
                            startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                        }
                        else if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                            Log.e("TEST_HOT","hotword Success tracking activé ");
                            if(teamChatBuddyApplication.getParamFromFile("WELCOME_hotword","TeamChatBuddy.properties").equalsIgnoreCase("Yes")){
                                Log.e("TEST_HOT"," welcome hotword yes  "+iInvitationCallback);
                                invitation(new IInvitationCallback() {
                                    @Override
                                    public void onEnd(String s) {
                                        Log.e("TEST_HOT", "Invitation onEnd Callback welcome hotword  : "+s);
                                        iInvitationCallback = null;
                                        teamChatBuddyApplication.listeningState = "qst";
                                        startListeningQuestion();
                                    }
                                });
                            }
                            else{

                                teamChatBuddyApplication.setSpeaking(true);
                                isListeningFreeSpeech = true;
                                teamChatBuddyApplication.setActivityClosed(false);
                                teamChatBuddyApplication.setStartRecording(true);
                                // Délai avant d'accepter un QR question : le user tient encore
                                // le QR hotword levé, on attend qu'il le baisse
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    if (teamChatBuddyApplication.listeningState.equals("hotword"))
                                        teamChatBuddyApplication.listeningState = "qst";
                                }, 3000);
                                startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                            }
                        }
                        Log.d("MYA_QR", "STTHotword_success");
                        //isSpeakingScan = false;
                    }
                });
            }

            else if (message.contains("STTQuestion_success")) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("Test_Continuous","STTQuestion_success receive");
                        teamChatBuddyApplication.listeningState = "traitement";
                        Log.i("MYA", "listeningState : "+teamChatBuddyApplication.listeningState);
                        BuddySDK.UI.stopListenAnimation();
                        Log.e("MYA_YAKINE","listeningState = traitement");
                        Log.e("MYA_QR_H_","STTQuestion_success receive");

                        stopListeningFreeSpeech();
                        teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(true);
                        SystemClock.sleep(200);
                        teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                        Log.i("zomilito",""+message);
                        String detectedSTTMessage = message.split(";")[1].replaceAll("' ", "'");
                        Log.i("TEST_voix","Question : "+detectedSTTMessage);
                        if (message.split(";").length==3) {
                            isScan = message.split(";")[2].contains("scan");
                            isTag = message.split(";")[2].equals("scanTAG");
                            teamChatBuddyApplication.setActivityClosed(false);
                            isSpeakingScan = true;
                        }
                        else {
                            isScan = false;
                            isTag = false;
                        }
                        Log.i("MYA_QR","Question : "+detectedSTTMessage);

                        if (!teamChatBuddyApplication.isActivityClosed()) {
                            if (!teamChatBuddyApplication.isModeContinuousListeningON()) {
                                teamChatBuddyApplication.setQuestionNumber(teamChatBuddyApplication.getQuestionNumber() + 1);
                                teamChatBuddyApplication.setQuestionTime(System.currentTimeMillis());
                                BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                                teamChatBuddyApplication.setLed("thinking");
                                Log.i("MYA_QR","setLed : =================== thinking ================= ");
                                if (settingClass.getSwitchVisibility().equals("true")) {
                                    if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                        if (isScan){
                                            buddy_texte_qst.setText(String.format("I scanned :  %s ", detectedSTTMessage.split("<")[0]));
                                        }
                                        else{
                                            buddy_texte_qst.setText(String.format("I heard :  %s ", detectedSTTMessage));
                                        }
                                    } else if (settingClass.getLangue().equals(langueFr)) {
                                        if (isScan){
                                            Log.i("MYA_QR","J'ai scanné la commande : " + detectedSTTMessage);
                                            buddy_texte_qst.setText(String.format("J'ai scanné :  %s ", detectedSTTMessage.split("<")[0]));
                                        }
                                        else{
                                            buddy_texte_qst.setText(String.format("J'ai entendu :  %s ", detectedSTTMessage));
                                        }
                                    } else {
                                        if (isScan){
                                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("I scanned ").addOnSuccessListener(new OnSuccessListener<String>() {
                                                @Override
                                                public void onSuccess(String translatedText) {

                                                    buddy_texte_qst.setText(String.format(translatedText + " :  %s ", detectedSTTMessage.split("<")[0]));
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e(TAG, "translatedText exception  " + e);
                                                }
                                            });

                                        }
                                        else{
                                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("I heard ").addOnSuccessListener(new OnSuccessListener<String>() {
                                                @Override
                                                public void onSuccess(String translatedText) {

                                                    buddy_texte_qst.setText(String.format(translatedText + " :  %s ", detectedSTTMessage));
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e(TAG, "translatedText exception  " + e);
                                                }
                                            });
                                        }
                                    }
                                    buddy_texte_qst_lyt.setVisibility(View.VISIBLE);
                                    buddy_texte_qst.setMovementMethod(new ScrollingMovementMethod());
                                    buddy_texte_qst.scrollTo(0, 0);
                                    lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                                    lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                                }
                                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                                Replica question = new Replica();
                                question.setType("question");
                                question.setTime(time);
                                question.setValue(detectedSTTMessage);
                                listRep.add(question);
                            }
                            else {
                                teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.add(detectedSTTMessage);
                                Log.e("Test_Continuous","question detected "+teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0));
                            }
                            lastLookingAtCameraTimeToCloseApp= System.currentTimeMillis();
                            personDetectedTimeToCloseApp= System.currentTimeMillis();
                            if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
                                responseFromChatbot.getCommandsResponseFromChatGPT(detectedSTTMessage, teamChatBuddyApplication.getQuestionNumber());
                            }
                            else {
                                responseFromChatbot.getSessionId(detectedSTTMessage);
                            }

                            if (teamChatBuddyApplication.getParamFromFile("Permanent_listening","TeamChatBuddy.properties")!=null
                                    &&teamChatBuddyApplication.getParamFromFile("Permanent_listening","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")) {
                                teamChatBuddyApplication.setModeContinuousListeningON(true);
                                startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                            }
                        }
                        if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")){
                            AlertManager.getInstance(_parentActivity).incremente("hotword", _parentActivity);
                        }
                    }
                });
            }

            else if (message.contains("TTS_success")) {
                Log.e("MYA_QR_H_","TTS_success receive isStartMsg "+teamChatBuddyApplication.isStartMsg);
                if(!teamChatBuddyApplication.isStartMsg) {
                    teamChatBuddyApplication.listeningState = "qst";
                    Log.e("MYA_YAKINE","========================"+teamChatBuddyApplication.listeningState);
                }
                else {
                    teamChatBuddyApplication.isStartMsg = false;
                    if (!shouldHideCameraPreview()) {
                        teamChatBuddyApplication.notifyObservers("showCameraQr");
                    }

                    //Log.d("MainFragment", "------------ showCameraQr TTS_success -------------");
                }
                //Log.e("MYA_YAKINE","listeningState = "+teamChatBuddyApplication.listeningState+"\nTTS_success ------ Delay");
                //teamChatBuddyApplication.listeningState = "qst";
                if(isSplitStreamingNews) {
                    callback.onSegmentDone();
                    Log.e("SplitStreamingNews", "callback.onSegmentDone :: update ");
                }
                else{
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, " TTS_success");
                        lastLookingAtCameraTimeToCloseApp = System.currentTimeMillis();
                        personDetectedTimeToCloseApp = System.currentTimeMillis();
                        teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                        buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                        buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                        buddy_texte_qst.setMovementMethod(null);
                        buddy_texte_resp.setMovementMethod(null);
                        if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null ){
                            String Number_clicks_options = teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties");
                            if(Number_clicks_options.equals("")||Integer.parseInt(Number_clicks_options)<=0){
                                applySettingsBtnVisibility();
                            }
                            else{
                                applySettingsBtnVisibility();
                            }
                        }else {
                            applySettingsBtnVisibility();
                        }
                        lyt_open_menu_chat.setVisibility(View.VISIBLE);
                        isSpeaking = false;
                        if (iInvitationCallback != null)
                            iInvitationCallback.onEnd("INVITATION_END");
                        if (iStartMessageCallback != null)
                            iStartMessageCallback.onEnd("STARTMESSAGE_END");
                        if (iMouthMessageCallback != null) {
                            Log.w(TAG, "MOUTH_FLAG: isMouthMessagePlaying=false (TTS_success 1)");
                            teamChatBuddyApplication.isMouthMessagePlaying = false;
                            iMouthMessageCallback.onEnd("MOUTHMESSAGE_END");
                        }
                    });
                    Log.e(TAG, " TTS_success 1");

                    if (handler != null && runnable != null) {
                        handler.removeCallbacks(runnable);
                        handler.removeCallbacksAndMessages(null);
                    }
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, " TTS_success 2 "+teamChatBuddyApplication.getStoredResponse().equals(""));
                            try {
                                if (!teamChatBuddyApplication.getStoredResponse().equals("")) {
                                    speak(teamChatBuddyApplication.getStoredResponse(), "storedResponse");
                                } else {
                                    Log.e(TAG, " TTS_success 4");

                                    if (!teamChatBuddyApplication.isModeContinuousListeningON()) {
                                        if (!teamChatBuddyApplication.isMultiCommandsDetected()) {
                                            String listensStr = teamChatBuddyApplication.getParamFromFile("Number_listens", "TeamChatBuddy.properties");
                                            int numberListens = 0; // valeur par défaut
                                            if (listensStr != null && !listensStr.isEmpty()) {
                                                try {
                                                    numberListens = Integer.parseInt(listensStr);
                                                } catch (NumberFormatException e) {
                                                    Log.e(TAG, "Invalid number format in config, defaulting to 0", e);
                                                }
                                            }

                                            if (teamChatBuddyApplication.getStartRecording() && numberListens > 0) {
                                                Log.e(TAG, "startCycle TTS_success 2");
                                                teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt() - 1);
                                                startCycle();
                                            }
                                            else if (teamChatBuddyApplication.getStartRecording() && numberListens==0){
                                                teamChatBuddyApplication.startListeningHotwor(_parentActivity);
                                            }
                                        } else {

                                            if (teamChatBuddyApplication.isTimeToExecuteNextCommande()) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(false);
                                                Log.e("MRA_TEST", "executeCommand TTS_SUCCESS");
                                                responseFromChatbot.executeCommand();
                                            }

                                        }
                                    } else {
                                        Log.e(TAG, " TTS_success 7");

                                        continuePlayingResponses();
                                    }
                                }
                            }
                            catch(Exception e)
                            {
                                Log.e(TAG, " TTS_success 8 "+e.getMessage());
                                e.printStackTrace();

                            }
                        }
                    };
                    handler.postDelayed(runnable, 500);

                }
            }

            else if (message.contains("TTS_error") || message.contains("TTS_exception")) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Do NOT clear isMouthMessagePlaying here: the fallback chain (ReadSpeaker,
                        // ApiGoogle…) is still trying to play this mouth message. Keeping the flag
                        // true ensures that a click during fallback playback calls stopTTS() and
                        // increments the generation counter so stale callbacks are discarded.
                        String text = message.split(";")[1];
                        Log.w(TAG,"TTS_ERROR (fallback, isMouthMessagePlaying stays true): " + text);
                        // Capture the callback NOW so a subsequent speakMouthMessages() call that
                        // overwrites iMouthMessageCallback does not corrupt this chain's completion.
                        final IMouthMessageCallback capturedMouthCallback = iMouthMessageCallback;
                        teamChatBuddyApplication.tryNextTTSFromList(text, new ITTSCallbacks() {
                            @Override
                            public void onSuccess(String s) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                                        try {
                                            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                        if (teamChatBuddyApplication.getparam("Mode_Stream").contains("yes") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && teamChatBuddyApplication.getChatGptStreamMode() != null) {
                                            Log.w("MODE_STREAM","TTS ERROR");
                                            teamChatBuddyApplication.getChatGptStreamMode().isReadyToSpeak = true;
                                        }
                                        else if(teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                                            teamChatBuddyApplication.getCustomGPTStreamMode().isReadyToSpeak = true;
                                        }
                                        else{
                                            buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                            buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                            buddy_texte_qst.setMovementMethod(null);
                                            buddy_texte_resp.setMovementMethod(null);
                                            if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null ){
                                                String Number_clicks_options = teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties");
                                                if(Number_clicks_options.equals("")||Integer.parseInt(Number_clicks_options)<=0){
                                                    applySettingsBtnVisibility();
                                                }
                                                else{
                                                    applySettingsBtnVisibility();
                                                }
                                            }else {
                                                applySettingsBtnVisibility();
                                            }
                                            lyt_open_menu_chat.setVisibility(View.VISIBLE);
                                            isSpeaking =false;
                                            if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                                            if(iStartMessageCallback != null) iStartMessageCallback.onEnd("STARTMESSAGE_END");
                                            if(capturedMouthCallback != null) {
                                                Log.w(TAG, "MOUTH_FLAG: isMouthMessagePlaying=false (TTS_error onSuccess recovery)");
                                                teamChatBuddyApplication.isMouthMessagePlaying = false;
                                                capturedMouthCallback.onEnd("MOUTHMESSAGE_END");
                                            }
                                            if(handler!=null && runnable!=null){
                                                handler.removeCallbacks(runnable);
                                                handler.removeCallbacksAndMessages(null);
                                            }
                                            runnable =new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (!teamChatBuddyApplication.getStoredResponse().equals("")){
                                                        getActivity().runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                speak(teamChatBuddyApplication.getStoredResponse(),"storedResponse");
                                                            }
                                                        });
                                                    }
                                                    else {
                                                        if (!teamChatBuddyApplication.isMultiCommandsDetected()) {
                                                            String listensStr = teamChatBuddyApplication.getParamFromFile("Number_listens", "TeamChatBuddy.properties");
                                                            int numberListens = 0; // valeur par défaut
                                                            if (listensStr != null && !listensStr.isEmpty()) {
                                                                try {
                                                                    numberListens = Integer.parseInt(listensStr);
                                                                } catch (NumberFormatException e) {
                                                                    Log.e(TAG, "Invalid number format in config, defaulting to 0", e);
                                                                }
                                                            }

                                                            if (teamChatBuddyApplication.getStartRecording() && numberListens > 0) {
                                                                Log.e(TAG, "startCycle TTS_success 2");
                                                                teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt() - 1);
                                                                startCycle();
                                                            }
                                                        }
                                                        else {

                                                            if (teamChatBuddyApplication.isTimeToExecuteNextCommande()){
                                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(false);
                                                                Log.e("MRA_TEST","executeCommand TTS_SUCCESS");
                                                                responseFromChatbot.executeCommand();
                                                            }

                                                        }
                                                    }
                                                }
                                            };
                                            handler.postDelayed(runnable,500);
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onError(String s) {
                                int textLength = text.length();// Calculate the length of the pronounced text
                                int delayTime = (textLength / 20) * 1000; // 1 second for every 20 characters
                                if (delayTime==0){
                                    delayTime=1500;
                                }
                                String langCode = teamChatBuddyApplication.getCurrentLanguage(); // fr, en, de, etc.
                                String defaultVoice = teamChatBuddyApplication.getReadSpeakerVoiceFromLangCode(langCode);
                                String validatedVoice = "";
                                if(defaultVoice!=null && defaultVoice.isEmpty()){
                                    validatedVoice = teamChatBuddyApplication.checkReadSpeakerVoices(defaultVoice);
                                }
                                if(teamChatBuddyApplication.getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && (validatedVoice!=null || !validatedVoice.isEmpty()) && teamChatBuddyApplication.getUsingReadSpeaker() ){
                                    delayTime = 0;
                                    Log.e(TAG,"set 0 delay time  "+delayTime);
                                }
                                handlerTTSError.postDelayed(runnableTTSError = new Runnable() {
                                    @Override
                                    public void run() {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                                                try {
                                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                                }
                                                catch (Exception e){
                                                    Log.e(TAG,"BuddySDK Exception  "+e);
                                                }
                                                if (teamChatBuddyApplication.getparam("Mode_Stream").contains("yes") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && teamChatBuddyApplication.getChatGptStreamMode() != null) {
                                                    Log.w("MODE_STREAM","TTS ERROR");
                                                    teamChatBuddyApplication.getChatGptStreamMode().isReadyToSpeak = true;
                                                }
                                                else if(teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                                                    teamChatBuddyApplication.getCustomGPTStreamMode().isReadyToSpeak = true;
                                                }
                                                else{
                                                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                                    buddy_texte_qst.setMovementMethod(null);
                                                    buddy_texte_resp.setMovementMethod(null);
                                                    if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null ){
                                                        String Number_clicks_options = teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties");
                                                        if(Number_clicks_options.equals("")||Integer.parseInt(Number_clicks_options)<=0){
                                                            applySettingsBtnVisibility();
                                                        }
                                                        else{
                                                            applySettingsBtnVisibility();
                                                        }
                                                    }else {
                                                        applySettingsBtnVisibility();
                                                    }
                                                    lyt_open_menu_chat.setVisibility(View.VISIBLE);
                                                    isSpeaking =false;
                                                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                                                    if(iStartMessageCallback != null) iStartMessageCallback.onEnd("STARTMESSAGE_END");
                                                    if(capturedMouthCallback != null) {
                                                        Log.w(TAG, "MOUTH_FLAG: isMouthMessagePlaying=false (TTS_error onError recovery)");
                                                        teamChatBuddyApplication.isMouthMessagePlaying = false;
                                                        capturedMouthCallback.onEnd("MOUTHMESSAGE_END");
                                                    }
                                                    if(handler!=null && runnable!=null){
                                                        handler.removeCallbacks(runnable);
                                                        handler.removeCallbacksAndMessages(null);
                                                    }
                                                    runnable =new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (!teamChatBuddyApplication.getStoredResponse().equals("")){
                                                                getActivity().runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        speak(teamChatBuddyApplication.getStoredResponse(),"storedResponse");
                                                                    }
                                                                });
                                                            }
                                                            else {
                                                                if (!teamChatBuddyApplication.isMultiCommandsDetected()) {
                                                                    String listensStr = teamChatBuddyApplication.getParamFromFile("Number_listens", "TeamChatBuddy.properties");
                                                                    int numberListens = 0; // valeur par défaut
                                                                    if (listensStr != null && !listensStr.isEmpty()) {
                                                                        try {
                                                                            numberListens = Integer.parseInt(listensStr);
                                                                        } catch (NumberFormatException e) {
                                                                            Log.e(TAG, "Invalid number format in config, defaulting to 0", e);
                                                                        }
                                                                    }

                                                                    if (teamChatBuddyApplication.getStartRecording() && numberListens > 0){
                                                                        Log.e(TAG, "startCycle TTS_success 2");
                                                                        teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt() - 1);
                                                                        startCycle();
                                                                    }
                                                                }
                                                                else {

                                                                    if (teamChatBuddyApplication.isTimeToExecuteNextCommande()){
                                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(false);
                                                                        Log.e("MRA_TEST","executeCommand TTS_SUCCESS");
                                                                        responseFromChatbot.executeCommand();
                                                                    }

                                                                }
                                                            }
                                                        }
                                                    };
                                                    handler.postDelayed(runnable,500);
                                                }
                                            }
                                        });
                                    }
                                },delayTime);
                            }
                        });
                    }
                });
            }

            else if (message.contains("CHATBOTS_RETURN")) {
                Log.e("Test_Continuous","CHATBOTS_RETURN receive notif");
                getActivity().runOnUiThread(() ->{
                    String action = message.split(";SPLIT;")[1];
                    Log.i(TAG,"action : "+action);
                    String value = message.split(";SPLIT;")[2];
                    Log.i(TAG,"value : "+value);
                    if (action.equals("speak")) {
                        if (message.split(";SPLIT;").length>3) {
                            if (!teamChatBuddyApplication.isModeContinuousListeningON() || isFirstResponse) {
                                Log.e("Test_Continuous","CHATBOTS_RETURN first response to get speaked");
                                isFirstResponse=false;
                                int numberOfQuestion = Integer.parseInt(message.split(";SPLIT;")[3]);
                                if (numberOfQuestion == teamChatBuddyApplication.getQuestionNumber()) {
                                    if (message.split(";SPLIT;").length > 4) {
                                        if (message.split(";SPLIT;")[4].equals("onError")) {
                                            teamChatBuddyApplication.setMessageError(true);
                                            if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                                BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                            }
                                        }
                                    }

                                    if (!teamChatBuddyApplication.isTimeoutExpired()) {
                                        speak(value, "nothealysa");
                                    } else {
                                        teamChatBuddyApplication.setStoredResponse(value);
                                    }
                                    if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))) {
                                        useListeningNumberWithAutomaicListening = true;
                                    }
                                }
                            }
                            else {
                                if (message.split(";SPLIT;").length > 4) {
                                    if (message.split(";SPLIT;")[4].equals("onError")) {
                                        teamChatBuddyApplication.setMessageError(true);
                                    }
                                    else{
                                        teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.add(message.split(";SPLIT;")[4].trim());
                                    }
                                }

                                teamChatBuddyApplication.listOfResponseInContinuousListeningMode.add(value);
                                Log.e("Test_Continuous","response added "+teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0));
                                if (isQuestionAlreadyDetected){
                                    isQuestionAlreadyDetected = false;
                                    if (!teamChatBuddyApplication.isTimeoutExpired()) {
                                        speak(teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0), "nothealysa");
                                    } else {
                                        teamChatBuddyApplication.setStoredResponse(teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0));
                                    }
                                    teamChatBuddyApplication.listOfResponseInContinuousListeningMode.remove(0);
                                    if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))) {
                                        useListeningNumberWithAutomaicListening = true;
                                    }
                                    if (!teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.isEmpty()) {
                                        if (teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.get(0).trim().equalsIgnoreCase("NotDetected")) {
                                            teamChatBuddyApplication.setLanguageDetected("");
                                        } else {
                                            teamChatBuddyApplication.setLanguageDetected(teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.get(0));
                                        }
                                        teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.remove(0);
                                    }
                                    if (!teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.isEmpty()) {
                                        setAnimation(teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.get(0));
                                        teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.remove(0);
                                    }
                                }

                            }
                        }
                    }
                    else if (action.equals("INVITATION")) {
                        Log.e(TAG_TRACKING, "ChatGPT Invitation: " + value);
                        teamChatBuddyApplication.setActivityClosed(false);
                        speak(value, "INVITATION");
                        if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                            useListeningNumberWithAutomaicListening= true;
                        }
                    }
                });
            }

            else if (message.contains("properties file done")) {
                Log.e(TAG,"properties file done main fragment ");
                teamChatBuddyApplication.setNotYet(false);
                getData();
            }

            else if (message.contains("end of timer")) {
                Log.e("TEST_QR","end of timer fonction Display qrcode---------------");
                isSpeakingScan = false;
                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                stopListeningFreeSpeech();
                SystemClock.sleep(200);
                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                    teamChatBuddyApplication.startListeningHotwor(getActivity());

                }
                else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                    teamChatBuddyApplication.startListeningHotwor(getActivity());
                }
            }

            else if (message.contains("end of cycle")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                        BuddySDK.UI.stopListenAnimation();
                    }
                });
            }

            else if (message.contains("restartNewCycle")){
                runnablePauseTime =new Runnable() {
                    @Override
                    public void run() {
                        startNextCycle();
                        Log.e(TAG,"startNextCycle  after handler ");
                    }
                };
                handlerPauseTime.postDelayed(runnablePauseTime,1000);
            }

            else if (message.contains("Obtain audio transcription after the listening time has elapsed")){
                String shouldRestartNewCycle = message.split(";SPLIT;")[1];
                Log.e(TAG,"Obtain audio transcription after the listening time has elapsed "+shouldRestartNewCycle);
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                BuddySDK.UI.stopListenAnimation();
                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                if (shouldRestartNewCycle.equals("true")) {
                    teamChatBuddyApplication.traitementAudio(true);
                }
                else {
                    teamChatBuddyApplication.traitementAudio(false);
                }
            }

            else if (message.contains("restartListeningHotword")){
                if (teamChatBuddyApplication.isMouthMessagePlaying) return;
                Log.e("TEST_QR","restartListeningHotword notif ");
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                teamChatBuddyApplication.setStartRecording(false);
                teamChatBuddyApplication.setSpeaking(false);
                teamChatBuddyApplication.setActivityClosed(true);
                isListeningFreeSpeech = false;
                teamChatBuddyApplication.stopTTS();
                teamChatBuddyApplication.setStoredResponse("");
                if (buddy_texte_qst_lyt != null && buddy_texte_resp_lyt != null && buddy_texte_qst != null && buddy_texte_resp != null) {
                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_qst.setMovementMethod(null);
                    buddy_texte_resp.setMovementMethod(null);
                }
                if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null ){
                    String Number_clicks_options = teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties");
                    if(Number_clicks_options.equals("")||Integer.parseInt(Number_clicks_options)<=0){
                        applySettingsBtnVisibility();
                    }
                    else{
                        applySettingsBtnVisibility();
                    }
                }else {
                    applySettingsBtnVisibility();
                }
                lyt_open_menu_chat.setVisibility(View.VISIBLE);
                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                } catch (Exception e) {
                    Log.e(TAG, "BuddySDK Exception  " + e);
                }
                teamChatBuddyApplication.notifyObservers("end of timer");
            }

            else if (message.contains("getResponseF")) {
                if (message.split(";SPLIT;")[1].equalsIgnoreCase("gpt")) {
                    gptResponse = message.split(";SPLIT;")[2];
                    gptSend = true;
                }
                if (gptSend ) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Integer.parseInt(message.split(";SPLIT;")[3])== teamChatBuddyApplication.getQuestionNumber()) {
                                if (!teamChatBuddyApplication.isModeContinuousListeningON()) {
                                    if (!teamChatBuddyApplication.getAnswerHasExceededTimeOut()) {
                                        setAnimation(gptResponse);
                                    }
                                }
                                else {
                                    teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.add(gptResponse);
                                }
                                teamChatBuddyApplication.setOpenaialreadySwitchEmotion(true);
                            }
                            gptSend = false;
                        }
                    });
                }
            }

            else if (message.contains("QRCodeScan")&&teamChatBuddyApplication.listeningState.equalsIgnoreCase("qst")){
                Log.i("MYA", "QRCodeScan -----------" + qrqst + "-----------stopListenAnimation");
                BuddySDK.UI.stopListenAnimation();
                teamChatBuddyApplication.listeningState = "traitement";
                Log.i("MYA", "listeningState : "+teamChatBuddyApplication.listeningState);
                qrqst = message.split(";SPLIT;")[2];
                if (teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    qrqst = teamChatBuddyApplication.applyFilters(teamChatBuddyApplication.getParamFromFile("Response_filter", "TeamChatBuddy.properties"), qrqst);
                }
                teamChatBuddyApplication.notifyObservers("STTQuestion_success;"+qrqst+";scan");
                teamChatBuddyApplication.setActivityClosed(false);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String scanTXT = "[" + sdf.format(new Date()) +"] - " + message.split(";SPLIT;")[1] + " - " + qrqst+ System.getProperty("line.separator");
                File file = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Scan_History.txt");
                try {
                    FileWriter fileWriter = new FileWriter(file,true);
                    fileWriter.write(scanTXT);
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            else if (message.contains("AprilTagScan")&&teamChatBuddyApplication.listeningState.equalsIgnoreCase("qst")){
                BuddySDK.UI.stopListenAnimation();
                Log.i("MYA", "AprilTagScan -----------" + qrqst + "-----------stopListenAnimation");
                String tag = message.split(";SPLIT;")[1];
                qrqst = teamChatBuddyApplication.getTxtfromTag(tag);
                teamChatBuddyApplication.listeningState = "traitement";
                Log.i("MYA", "listeningState : "+teamChatBuddyApplication.listeningState);
                Log.i("MYA_QR", "update: AprilTagScan -----------" + qrqst);
                teamChatBuddyApplication.getFrenchLanguageSelectedTranslator().translate( qrqst).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        if (translatedText != "") {
                            teamChatBuddyApplication.notifyObservers("STTQuestion_success;" + translatedText + ";scanTAG");
                            teamChatBuddyApplication.setActivityClosed(false);
                            Log.i("MYA_QR", "setActivityClosed ---- 16 ---- ");
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String scanTXT = "[" + sdf.format(new Date()) + "] - AprilTag - " + translatedText.split("<")[0] + System.getProperty("line.separator");
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
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "translatedText exception  " + e);
                    }
                });
            }

            else if (message.equalsIgnoreCase("hideCameraQr")){
                //Log.d("MainFragment", "------------ hideCameraQr -------------");
                getActivity().runOnUiThread(() -> {
                    previewView_qr.setTranslationY(100000);
                });
            }

            else if (message.equalsIgnoreCase("showCameraQr")){
                //Log.d("MainFragment", "------------ showCameraQr -------------");
                getActivity().runOnUiThread(() -> {
                    if (!shouldHideCameraPreview()) {
                        Log.d("CAMERA_PREVIEW_DEBUG", "showCameraQr message - SHOWING previewView_qr (QR preview camera)");
                        previewView_qr.setTranslationY(0);
                    } else {
                        Log.d("CAMERA_PREVIEW_DEBUG", "showCameraQr message - NOT showing previewView_qr (Displaying_QRCode is active)");
                        previewView_qr.setTranslationY(100000);
                    }
                });
            }

            else if (message.contains("playStoredResponse")){
                if (!teamChatBuddyApplication.getStoredResponse().equals("")){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speak(teamChatBuddyApplication.getStoredResponse(),"storedResponse");
                        }
                    });
                }
            }

            else if (message.contains("makeBuddyFaceNeutral")){
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
            }

            else if (message.contains("playEmotion")){
                if (!teamChatBuddyApplication.getCurrentEmotion().equals("")){
                    if (!teamChatBuddyApplication.getAnswerHasExceededTimeOut()){
                        setAnimation(teamChatBuddyApplication.getCurrentEmotion());
                    }
                    teamChatBuddyApplication.setOpenaialreadySwitchEmotion(true);
                }
            }

            else if (message.contains("ChatDestroy")){
                teamChatBuddyApplication.setparam("firstLaunch","false");
            }

            else if (message.contains("isConnected")){
                isConnected = true;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadingBar.setVisibility(View.VISIBLE);
                        noNetwork.setVisibility(View.GONE);
                    }
                });
            }

            else if (message.contains("isNotConnected")){
                isConnected = false;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadingBar.setVisibility(View.GONE);
                        noNetwork.setVisibility(View.VISIBLE);
                        if(timerDownloading!=null){
                            timerDownloading.cancel();
                        }
                    }
                });
            }

            else if (message.contains("changeDetected")){
                int speakVolume = teamChatBuddyApplication.getVolume();
                int max = teamChatBuddyApplication.getMaxVolume();
                int defaultVolume = teamChatBuddyApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e(TAG,"volumeMedia  "+String.valueOf(defaultVolume));
                teamChatBuddyApplication.setparam("speak_volume", String.valueOf(defaultVolume));
            }

            else if (message.contains( "commandResponse" )){
                Log.i(TAG,"  message commandResponse arrivé : "+message);
                if(message.split( ";SPLIT;" )[1].equals("CANCEL")){
                    Log.i(TAG," message commandResponse arrivé 1 ");

                    if(!isSpeaking){
                        if(ResponseFromChatbot.responseTimeout !=null){
                            ResponseFromChatbot.responseTimeout.cancel();
                        }
                        if(ChatGptStreamMode.responseTimeout !=null){
                            ChatGptStreamMode.responseTimeout.cancel();
                        }
                        if(CustomGPTStreamMode.responseTimeout !=null){
                            CustomGPTStreamMode.responseTimeout.cancel();
                        }
                        if (!teamChatBuddyApplication.isMultiCommandsDetected()) {
                            if (!teamChatBuddyApplication.getUsingEmotions()) {
                                Log.d(TAG, "FacialExpression NEUTRAL");
                                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            }
                            teamChatBuddyApplication.notifyObservers("TTS_success");
                        }
                        else {
                            Log.e("MRA_TEST","executeCommand commandResponse");
                            responseFromChatbot.executeCommand();
                        }
                    }
                }
                else if(message.split( ";SPLIT;" )[1].equals("CHANGE_LANGUE")){
                    Log.i(TAG," message commandResponse arrivé 2 ");

                    isCMDLangue = true;
                    settingClass.setLangue(teamChatBuddyApplication.getLangue().getNom());
                    mlKitIsDownloading = true;
                    List<String> mlkitLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
                    String codeLanguageMlkit = mlkitLangueCode.get(teamChatBuddyApplication.getLangue().getId()-1);
                    teamChatBuddyApplication.downloadModel(imlKitDownloadCallback,codeLanguageMlkit.trim());
                    handlerProgressBar.postDelayed(runnableProgressBar,500);
                    timerDownloading.start();
                }
                else{
                    Log.i(TAG," message commandResponse arrivé 3 "+isSpeaking);

                    if(!isSpeaking)
                    {
                        stopListeningFreeSpeech();
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            BuddySDK.UI.stopListenAnimation();
                        } catch (Exception e) {
                            Log.e(TAG, "BuddySDK Exception  " + e);
                        }
                        Log.i(TAG," message avant speak ");
                        speak(message.split( ";SPLIT;" )[1], "commande");
                    }
                    else
                        teamChatBuddyApplication.setStoredResponse( message.split( ";SPLIT;" )[1] );
                }
            }

            else if (message.contains( "showImage" )){
                Log.i( TAG, "J'affiche une image" );
                getActivity().runOnUiThread(() -> {
                    afficherPopupAvecBitmap( "/storage/emulated/0/"+message.split( ";SPLIT;" )[1]);
                });
            }

            else if (message.equals( "closeImage" )){
                closeImage();
            }

            else if (message.contains("takePicture")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("MRRM","prompt CMD_TAKE_PHOTO"+message.split(";SPLIT;")[1]);
                        startCameraForCommand(message.split(";SPLIT;")[1]);
                    }
                });
            }

            else if(message.contains("STOP_TRACKING")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                            stopTracking();
                        }
                    }
                });
            }

            else if(message.contains("RESTART_TRACKING")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                            isReTrack = true;
                            initTracking();
                        }
                    }
                });
            }

            else if (message.contains("ExecuteCMDPROMPT")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                            if (teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties").trim())){
                                responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1]+" "+teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties"),Integer.parseInt(message.split(";SPLIT;")[2]));
                            }
                            else {
                                responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1],Integer.parseInt(message.split(";SPLIT;")[2]));
                            }
                        }
                        else if (settingClass.getLangue().equals(langueFr)) {
                            if (teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties").trim())){
                                responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1]+" "+teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties"),Integer.parseInt(message.split(";SPLIT;")[2]));
                            }
                            else {
                                responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1],Integer.parseInt(message.split(";SPLIT;")[2]));
                            }
                        }
                        else {
                            if (teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties").trim())){
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")).addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {

                                        responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1]+" "+translatedText,Integer.parseInt(message.split(";SPLIT;")[2]));
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG,"translatedText exception  "+e);
                                    }
                                });

                            }
                            else {
                                responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1],Integer.parseInt(message.split(";SPLIT;")[2]));
                            }
                        }
                    }
                });
            }

            else if (message.contains("TTSAndroidIsInitialized")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mlKitIsDownloading = true;
                        french_is_downloaded = false;
                        english_is_downloaded = false;
                        languageToEnglish_is_downloaded =false;
                        List<String> mlkitLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
                        String codeLanguageMlkit = mlkitLangueCode.get(new Gson().fromJson(teamChatBuddyApplication.getparam(settingClass.getLangue()), Langue.class).getId()-1);
                        teamChatBuddyApplication.downloadModel(imlKitDownloadCallback,codeLanguageMlkit.trim());
                        handlerProgressBar.postDelayed(runnableProgressBar,500);

                        timerDownloading.start();
                    }
                });
            }

            else if (message.equals("SpeechRecognizerTimeout")){
                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && regarde_camera){
                    Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                    teamChatBuddyApplication.startListeningQuestion(getActivity(),"FirstListening");
                }
                else{
                    Log.i(TAG, "timerEcoute onFinish");
                    teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                    stopListeningFreeSpeech();
                    SystemClock.sleep(200);
                    if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                        teamChatBuddyApplication.startListeningHotwor(getActivity());

                    }
                    else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                        teamChatBuddyApplication.startListeningHotwor(getActivity());
                    }
                }
            }

            else if (message.equals("SpeechRecognizerAttemptTimeout")){
                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && regarde_camera){
                    Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                    teamChatBuddyApplication.startListeningQuestion(getActivity(),"startCycle");
                }
                else{
                    Log.i(TAG, "timerEcoute onFinish");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                            BuddySDK.UI.stopListenAnimation();
                        }
                    });
                    runnablePauseTime =new Runnable() {
                        @Override
                        public void run() {
                            startNextCycle();
                            Log.e(TAG,"startNextCycle  after handler ");
                        }
                    };
                    handlerPauseTime.postDelayed(runnablePauseTime,1000);
                }
            }

            else if (message.equals("STOP_ALERT")){
                AlertManager.getInstance(_parentActivity).stop();
            }
        }
    }
    public void continuePlayingResponses(){
        if (!teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.isEmpty()) {
            if (!teamChatBuddyApplication.isActivityClosed()) {
                teamChatBuddyApplication.setQuestionNumber(teamChatBuddyApplication.getQuestionNumber() + 1);
                teamChatBuddyApplication.setQuestionTime(System.currentTimeMillis());
                BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                teamChatBuddyApplication.setLed("thinking");
                if (settingClass.getSwitchVisibility().equals("true")) {
                    if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                        buddy_texte_qst.setText(String.format("I heard :  %s ", teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0)));
                    } else if (settingClass.getLangue().equals(langueFr)) {
                        buddy_texte_qst.setText(String.format("J'ai entendu :  %s ",  teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0)));
                    } else {
                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("I heard ").addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {

                                buddy_texte_qst.setText(String.format(translatedText + " :  %s ",  teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0)));
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "translatedText exception  " + e);
                            }
                        });
                    }
                    buddy_texte_qst_lyt.setVisibility(View.VISIBLE);
                    buddy_texte_qst.setMovementMethod(new ScrollingMovementMethod());
                    buddy_texte_qst.scrollTo(0, 0);
                    lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                    lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                }
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                Replica question = new Replica();
                question.setType("question");
                question.setTime(time);
                question.setValue( teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0));
                listRep.add(question);
                teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.remove(0);
            }
            if (!teamChatBuddyApplication.listOfResponseInContinuousListeningMode.isEmpty()) {
                if (!teamChatBuddyApplication.isTimeoutExpired()) {
                    speak(teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0), "nothealysa");
                } else {
                    teamChatBuddyApplication.setStoredResponse(teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0));
                }
                teamChatBuddyApplication.listOfResponseInContinuousListeningMode.remove(0);
                if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))) {
                    useListeningNumberWithAutomaicListening = true;
                }
                if (!teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.isEmpty()) {
                    if (teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.get(0).trim().equalsIgnoreCase("NotDetected")) {
                        teamChatBuddyApplication.setLanguageDetected("");
                    } else {
                        teamChatBuddyApplication.setLanguageDetected(teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.get(0));
                    }
                    teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.remove(0);
                }
                if (!teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.isEmpty()) {
                    setAnimation(teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.get(0));
                    teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.remove(0);
                }
            }
            else {
                isQuestionAlreadyDetected= true;
            }

        }
        else{
            teamChatBuddyApplication.setModeContinuousListeningON(false);
            isFirstResponse=true;
            String listensStr = teamChatBuddyApplication.getParamFromFile("Number_listens", "TeamChatBuddy.properties");
            int numberListens = 0; // valeur par défaut
            if (listensStr != null && !listensStr.isEmpty()) {
                try {
                    numberListens = Integer.parseInt(listensStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid number format in config, defaulting to 0", e);
                }
            }

            if (teamChatBuddyApplication.getStartRecording() && numberListens > 0) {
                Log.e(TAG, "startCycle TTS_success 2");
                teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt() - 1);
                startCycle();
            }

        }
    }

    /**
     * ----------------- Utils ---------------------------
     */

    /**
     * Restore settings button visibility based on toggle state
     * Respects the user's long-press toggle preference
     */
    private void restoreSettingsButtonVisibility() {
        if (lyt_open_menu_settings != null) {
            applySettingsBtnVisibility();
        }
    }

    private void init() {
        Log.e(TAG,"init() ");
        try {
            BuddySDK.UI.addFaceTouchListener(iuiFaceTouchCallback);
        }
        catch (Exception e){
            Log.e(TAG,"Exception BuddySdk "+e);
        }
        if (teamChatBuddyApplication.getparam("Stimulis").contains("yes")){
            handlerForSensor = new Handler();
            BuddySDK.USB.enableSensorModule(true,iUsbCommadRspBI);
        }

        teamChatBuddyApplication.registerObserver(this);

        buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
        buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
        buddy_texte_qst.setMovementMethod(null);
        buddy_texte_resp.setMovementMethod(null);
        applySettingsBtnVisibility();
        lyt_open_menu_chat.setVisibility(View.VISIBLE);

        commande = new Commande(requireActivity());

        //init config file
        initOrMajOrNone = teamChatBuddyApplication.createPropertiesFile();

        checkParametersValues();
        migrateBooleanToString();
    }

    private void migrateBooleanToString() {
        if(teamChatBuddyApplication.getparam("switch_emotion").equals("true")){
            teamChatBuddyApplication.setparam("switch_emotion","yes");
        }else if(teamChatBuddyApplication.getparam("switch_emotion").equals("false")){
            teamChatBuddyApplication.setparam("switch_emotion","no");
        }

        if(teamChatBuddyApplication.getparam("switch_visibility").equals("true")){
            teamChatBuddyApplication.setparam("switch_visibility","yes");
        }else if(teamChatBuddyApplication.getparam("switch_visibility").equals("false")){
            teamChatBuddyApplication.setparam("switch_visibility","no");
        }


        if(teamChatBuddyApplication.getparam("Stimulis").equals("true")){
            teamChatBuddyApplication.setparam("Stimulis","yes");
        }else if(teamChatBuddyApplication.getparam("Stimulis").equals("false")){
            teamChatBuddyApplication.setparam("Stimulis","no");
        }


        if(teamChatBuddyApplication.getparam("Detection_de_langue").equals("true")){
            teamChatBuddyApplication.setparam("Detection_de_langue","yes");
        }else if(teamChatBuddyApplication.getparam("Detection_de_langue").equals("false")){
            teamChatBuddyApplication.setparam("Detection_de_langue","no");
        }

        if(teamChatBuddyApplication.getparam("Commands").equals("true")){
            teamChatBuddyApplication.setparam("Commands","yes");
        }else if(teamChatBuddyApplication.getparam("Commands").equals("false")){
            teamChatBuddyApplication.setparam("Commands","no");
        }

        if(teamChatBuddyApplication.getparam("Tracking_Activation").equals("true")){
            teamChatBuddyApplication.setparam("Tracking_Activation","yes");
        }else if(teamChatBuddyApplication.getparam("Tracking_Activation").equals("false")){
            teamChatBuddyApplication.setparam("Tracking_Activation","no");
        }

        if(teamChatBuddyApplication.getparam("Mode_Stream").equals("true")){
            teamChatBuddyApplication.setparam("Mode_Stream","yes");
        }else if(teamChatBuddyApplication.getparam("Mode_Stream").equals("false")){
            teamChatBuddyApplication.setparam("Mode_Stream","no");
        }

    }

    private void checkParametersValues() {
        Log.i("HHO"," check parameteres values ");

        // *** Activation_of_emotions
        if(teamChatBuddyApplication.getParamFromFile("Activation_of_emotions","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("switch_emotion",teamChatBuddyApplication.getParamFromFile("Activation_of_emotions","TeamChatBuddy.properties").toLowerCase());

        }
        else{
            if(teamChatBuddyApplication.getparam("switch_emotion").contains("hid")){
                teamChatBuddyApplication.setparam("switch_emotion",teamChatBuddyApplication.getParamFromFile("Activation_of_emotions","TeamChatBuddy.properties").toLowerCase());
            }
        }
        // *** Display of speech
        if(teamChatBuddyApplication.getParamFromFile("Display_of_speech","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("switch_visibility",teamChatBuddyApplication.getParamFromFile("Display_of_speech","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("switch_visibility").contains("hid")){
                teamChatBuddyApplication.setparam("switch_visibility",teamChatBuddyApplication.getParamFromFile("Display_of_speech","TeamChatBuddy.properties").toLowerCase());
            }
        }

        // *** Stimulis
        if(teamChatBuddyApplication.getParamFromFile("Stimulis","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Stimulis",teamChatBuddyApplication.getParamFromFile("Stimulis","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("Stimulis").contains("hid")){
                teamChatBuddyApplication.setparam("Stimulis",teamChatBuddyApplication.getParamFromFile("Stimulis","TeamChatBuddy.properties").toLowerCase());
            }
        }

        // *** Detection_de_langue
        if(teamChatBuddyApplication.getParamFromFile("Language_detection","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Detection_de_langue",teamChatBuddyApplication.getParamFromFile("Language_detection","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("Detection_de_langue").contains("hid")){
                teamChatBuddyApplication.setparam("Detection_de_langue",teamChatBuddyApplication.getParamFromFile("Language_detection","TeamChatBuddy.properties").toLowerCase());
            }
        }
        // *** Commands
        if(teamChatBuddyApplication.getParamFromFile("Commands","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Commands",teamChatBuddyApplication.getParamFromFile("Commands","TeamChatBuddy.properties").toLowerCase());
            Log.i("HHO"," Commands "+teamChatBuddyApplication.getparam("Commands"));
        }
        else{
            if(teamChatBuddyApplication.getparam("Commands").contains("hid")){
                teamChatBuddyApplication.setparam("Commands",teamChatBuddyApplication.getParamFromFile("Commands","TeamChatBuddy.properties").toLowerCase());
            }
        }
        // *** tracking
        if(teamChatBuddyApplication.getParamFromFile("Tracking","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Tracking_Activation",teamChatBuddyApplication.getParamFromFile("Tracking","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("hid")){
                teamChatBuddyApplication.setparam("Tracking_Activation",teamChatBuddyApplication.getParamFromFile("Tracking","TeamChatBuddy.properties").toLowerCase());
            }
        }

        // *** Mode stream
        if(teamChatBuddyApplication.getParamFromFile("Stream_mode","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Mode_Stream",teamChatBuddyApplication.getParamFromFile("Stream_mode","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("Mode_Stream").contains("hid")){
                teamChatBuddyApplication.setparam("Mode_Stream",teamChatBuddyApplication.getParamFromFile("Stream_mode","TeamChatBuddy.properties").toLowerCase());
            }
        }
    }

    private void getData(){
        Log.i(TAG," getData ");

        //Visibility of Settings button
        if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null ){
            String Number_clicks_options = teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties");
            if(Number_clicks_options.equals("")||Integer.parseInt(Number_clicks_options)<=0){
                applySettingsBtnVisibility();
            }
            else{
                applySettingsBtnVisibility();
            }
        }else {
            applySettingsBtnVisibility();
        }
        //init Settings
        settingClass=new Setting();
        String listeningDuration =teamChatBuddyApplication.getParamFromFile("Listening_time","TeamChatBuddy.properties");
        String listeningAttempt = teamChatBuddyApplication.getParamFromFile("Number_listens","TeamChatBuddy.properties");
        if(listeningDuration.equals("")||Integer.parseInt(listeningDuration)<=0){
            settingClass.setDuration("10");
        }
        else{
            settingClass.setDuration(listeningDuration);
        }
        if(listeningAttempt.equals("")||Integer.parseInt(listeningAttempt)<=0){
            settingClass.setAttempt("1");
        }
        else{
            settingClass.setAttempt(listeningAttempt);
        }
        settingClass.setChatbot("ChatGPT");
        settingClass.setLangue(teamChatBuddyApplication.getLangue().getNom());
        settingClass.setVolume(teamChatBuddyApplication.getparam("speak_volume"));
        if(teamChatBuddyApplication.getparam("switch_visibility").contains("yes")){
            settingClass.setSwitchVisibility("true");
        }else{
            settingClass.setSwitchVisibility("false");
        }

        if(teamChatBuddyApplication.getparam("switch_emotion").contains("yes")){
            settingClass.setSwitchEmotion("true");
        }else{
            settingClass.setSwitchEmotion("false");
        }

        Log.i(TAG, settingClass.toString());
        if (teamChatBuddyApplication.getparam("Stimulis").contains("yes")){
            Log.e("MRARA","disnable Raise event Stimilus");
            
                BuddySDK.Companion.raiseEvent("disableRightEye");
                BuddySDK.Companion.raiseEvent("disableLeftEye");
                BuddySDK.Companion.raiseEvent("disableHeadSensors");
                BuddySDK.Companion.raiseEvent("disableBodySensors");
            
            timerToApplyBI=new CountDownTimer(10*1000,1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    applyBIAfterDelay=true;
                }
            };
            timerToApplyBI.start();
        }else {
            if (teamChatBuddyApplication.getParamFromFile("use_companion_when_stimulis_disabled","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
                Log.e("MRARA","enable Raise event Yes");
        
                    BuddySDK.Companion.raiseEvent("enableRightEye");
                    BuddySDK.Companion.raiseEvent("enableLeftEye");
                    BuddySDK.Companion.raiseEvent("enableHeadSensors");
                    BuddySDK.Companion.raiseEvent("enableBodySensors");
                    BuddySDK.Companion.raiseEvent("disableOnMouth");
            
            }else {
                Log.e("MRARA","disable Raise event NO "+BuddySDK.Companion+"  --- ");
                    BuddySDK.Companion.raiseEvent("disableRightEye");
                    BuddySDK.Companion.raiseEvent("disableLeftEye");
                    BuddySDK.Companion.raiseEvent("disableHeadSensors");
                    BuddySDK.Companion.raiseEvent("disableBodySensors");
                
            }
        }


        refreshSTTLangue();

        //set volume
        teamChatBuddyApplication.setVolume(Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume")),AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

        Log.i("SessionManager"," getData "+teamChatBuddyApplication.getFileCreate());

        //create Log file
        //     if (teamChatBuddyApplication.getFileCreate()) {
//            teamChatBuddyApplication.listSessionClear();
//            teamChatBuddyApplication.setparam("listSession", new Gson().toJson(teamChatBuddyApplication.getListSession()));
//            listRep.clear();
//            teamChatBuddyApplication.setFileCreate(false);
        //     }

        //init chatbots
        responseFromChatbot = new ResponseFromChatbot(teamChatBuddyApplication,getActivity());


        Long mLkit_timeout_in_seconds = (long) Integer.parseInt(teamChatBuddyApplication.getParamFromFile("MLkit_timeout_in_seconds", "TeamChatBuddy.properties")) * 1000;

        timerDownloading = new CountDownTimer(mLkit_timeout_in_seconds, 1000) {
            @Override
            public void onTick(long l) {
                if(l < mLkit_timeout_in_seconds - 1000) {
                    if (mlKitIsDownloading) {
                        if (isConnected) {
                            showToastMessage();
                        }

                    }
                }
            }

            @Override
            public void onFinish() {
                Log.d("FZE", "timerDownloading onFinish — isAdded=" + isAdded() + " activity=" + getActivity());
                if (!isAdded() || getActivity() == null) return;
                if(teamChatBuddyApplication.getparam("previousLanguage")!=null && !teamChatBuddyApplication.getparam("previousLanguage").trim().equals("")) {
                    if (currentToast != null) currentToast.cancel();
                    launch_view.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), R.string.toast_message_error_downloading_en, Toast.LENGTH_LONG).show();
                    teamChatBuddyApplication.setLangue(new Gson().fromJson(teamChatBuddyApplication.getparam("previousLanguage"), Langue.class));
                    setPreviousLanguage();
                    if(commande!=null){
                        commande.translate("CMD_LANGUE", new Commande.ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedText) {
                                if(translatedText.contains("No_message_defined")){
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                }
                                else{
                                    String verifyMessage = commande.verifyCmdMessages(translatedText);
                                    if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                        if (teamChatBuddyApplication.getParamFromFile("COMMAND_histo","TeamChatBuddy.properties")!=null && teamChatBuddyApplication.getParamFromFile("COMMAND_histo","TeamChatBuddy.properties").trim().equalsIgnoreCase("yes")) {
                                            try {
                                                // get the historic commandes :
                                                String jsonArrayString = teamChatBuddyApplication.getparam("commandes");
                                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                JSONObject history1 = new JSONObject();
                                                history1.put("role", "assistant");
                                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                existingHistoryArray.put(history1);
                                                if (existingHistoryArray.length() > Integer.parseInt(teamChatBuddyApplication.getParamFromFile("COMMAND_maxdialog", "TeamChatBuddy.properties"))) {

                                                    existingHistoryArray.remove(1);
                                                    existingHistoryArray.remove(1);

                                                    teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());

                                                } else {
                                                    teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                    }
                                }
                            }
                        });
                    }
                }
                else{
                    timerDownloading.start();
                }

            }
        };
        if (teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties")!=null
                && Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim())!=0) {
            timerPeriodToDisplayQRCode = new CountDownTimer(Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period", "TeamChatBuddy.properties").trim())*1000, 100) {
                @Override
                public void onTick(long l) {
                    Log.d("TEST_QR"," onTick QR code ---------------");
                    if(!teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim().equalsIgnoreCase("0")){
                        //teamChatBuddyApplication.notifyObservers("hideCameraQr");
                        //Log.d("MainFragment", "------------ hideCameraQr getData -------------");
                    }
                }

                @Override
                public void onFinish() {
                    Log.e("TEST_QR"," onFinish QR code ---------------");
                    if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                        if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Camera_Display"))) {
                            Log.d("CAMERA_PREVIEW_DEBUG", "Line 3165 - HIDING preview (onFinish QR code countdown)");
                            setReGroupTranslationY(1000);
                            if (!shouldHideCameraPreview()) {
                                teamChatBuddyApplication.notifyObservers("showCameraQr");
                            }
                            //Log.d("MainFragment", "------------ showCameraQr finish countdown get Data -------------");
                        }
                    }
                    Log.e(TAG,"call displayQRCode 2 ");
                    if(!qr_displayed){
                        displayQRCode(new IDisplayQrCodeCallback() {
                            @Override
                            public void onEnd() {
                                teamChatBuddyApplication.setShouldDisplayQRCode(false);
                                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                                    if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Camera_Display"))) {
                                        updateCameraPreviewVisibility();
                                        if(!teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim().equalsIgnoreCase("0")){
                                            teamChatBuddyApplication.notifyObservers("hideCameraQr");
                                            //Log.d("MainFragment", "------------ hideCameraQr getData onFinish -------------");
                                        }
                                    } else {
                                        Log.d("CAMERA_PREVIEW_DEBUG", "Line 3184 - HIDING preview (Tracking_Camera_Display=false)");
                                        setReGroupTranslationY(1000);
                                        if (!shouldHideCameraPreview()) {
                                            teamChatBuddyApplication.notifyObservers("showCameraQr");
                                        }
                                        //Log.d("MainFragment", "------------ showCameraQr finish getData tracking -------------");
                                    }
                                }
                            }
                        });
                    }
                }
            };
        }

        teamChatBuddyApplication.setActivityClosed(false);

        teamChatBuddyApplication.initTTSAndroid();
        teamChatBuddyApplication.initTTSGoogleCoud();

    }

    public void setPreviousLanguage() {
        List<Langue> langues = new ArrayList<>();
        List<String> langueDisponible = teamChatBuddyApplication.getDisponibleLangue();
        for (int i = 0; i < langueDisponible.size(); i++) {
            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam(langueDisponible.get(i)), Langue.class));
        }
        if (langues.isEmpty()) {
            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam("Français"), Langue.class));
        }

        if (langues != null && !langues.isEmpty()) {
            for (Langue langue : langues) {
                langue.setChosen(false);
                if (langue.getNom().equalsIgnoreCase(new Gson().fromJson(teamChatBuddyApplication.getparam("previousLanguage"), Langue.class).getNom())) {
                    langue.setChosen(true);
                    teamChatBuddyApplication.setLangue(langue);
                }
                if (langue.getNom().equals("Français")) {
                    teamChatBuddyApplication.setparam("Français", new Gson().toJson(langue));
                } else if (langue.getNom().equals("Anglais")) {
                    teamChatBuddyApplication.setparam("Anglais", new Gson().toJson(langue));
                } else if (langue.getNom().equals("Espagnol")) {
                    teamChatBuddyApplication.setparam("Espagnol", new Gson().toJson(langue));
                } else if (langue.getNom().equals("Allemand")) {
                    teamChatBuddyApplication.setparam("Allemand", new Gson().toJson(langue));
                } else {
                    teamChatBuddyApplication.setparam(langue.getNom(), new Gson().toJson(langue));
                }
            }
        }
    }

    private void showToastMessage() {
        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
            currentToast = Toast.makeText(getActivity(), R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT);
        } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
            currentToast = Toast.makeText(getActivity(), R.string.mlkit_model_is_downloading_fr, Toast.LENGTH_SHORT);
        } else {
            currentToast = Toast.makeText(getActivity(), R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT);
        }
        currentToast.show();
    }
    public void onUserTouch(MotionEvent event) {
        Log.d(TAG, "AlertManager: Touch détecté dans ChatFragment");
    }

    private void showStream(String responseTitle,String response) {
        if (!teamChatBuddyApplication.isActivityClosed()) {
            buddy_texte_resp.setText(String.format(responseTitle + " :  %s ", response));
            buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
            buddy_texte_resp.setMovementMethod(new ScrollingMovementMethod());
            // Scroll to the end
            buddy_texte_resp.post(new Runnable() {
                @Override
                public void run() {
                    int scrollAmount = buddy_texte_resp.getLayout().getLineTop(buddy_texte_resp.getLineCount())
                            - buddy_texte_resp.getHeight() + buddy_texte_resp.getLineHeight();
                    if (scrollAmount > 0) {
                        buddy_texte_resp.scrollTo(0, scrollAmount);
                    } else {
                        buddy_texte_resp.scrollTo(0, 0);
                    }
                }
            });
        }
    }
    NextSegmentCallback callback = new NextSegmentCallback() {
        @Override
        public void onSegmentDone() {

        }

        @Override
        public void onError(Exception e) {

        }
    };

    boolean isSplitStreamingNews = false;
    private void streamSpeakSegments(String fullText, String title, String type) {
        Log.d("currentIndex", "fullText: "+fullText);
        Log.d("EVO", "streamSpeakSegments ------- fct ");
        String[] segments = fullText.split(";splitNews;");

        buddy_texte_resp.setText("");
        final int[] currentIndex = {0};

        // Callback unique pour gérer la suite
        callback = new NextSegmentCallback() {
            @Override
            public void onSegmentDone() {
                if  (currentIndex[0] < segments.length) {
                    Log.d("EVO", "onSegmentDone :: speakSegment ");
                    Log.e("EVO", "onSegmentDone :: speakSegment "+segments[currentIndex[0]]);
                    speakSegment(segments[currentIndex[0]]);
                    currentIndex[0]++;
                } else {
                    Log.i("EVO", "Tous les segments ont été lus ");
                }
                if (segments.length == currentIndex[0]){
                    isSplitStreamingNews = false;
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("EVO", "onError :: speakSegment "+e);
            }

            private void speakSegment(String segment) {
                getActivity().runOnUiThread(() -> {
                    String fill = String.valueOf(buddy_texte_resp.getText());

                    if(fill!="")buddy_texte_resp.setText(fill+"\n"+"\n" + segment.trim());
                    else buddy_texte_resp.setText(title+" : "+segment.trim());

                    lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                    lyt_open_menu_chat.setVisibility(View.INVISIBLE);


                    if (buddy_texte_qst_lyt.getVisibility() != View.VISIBLE) {
                        buddy_texte_resp_lyt.setTranslationY(-155);
                    } else {
                        buddy_texte_resp_lyt.setTranslationY(0);
                    }

                    buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
                    buddy_texte_resp.setMovementMethod(new ScrollingMovementMethod());
                    buddy_texte_resp.post(() -> {
                        int scrollY = buddy_texte_resp.getLayout()
                                .getLineTop(buddy_texte_resp.getLineCount())
                                - buddy_texte_resp.getHeight();

                        if (scrollY > 0) {
                            buddy_texte_resp.scrollTo(0, scrollY+40);   // pile sur la dernière ligne
                        } else {
                            buddy_texte_resp.scrollTo(0, 0);
                        }
                    });
                });


                Log.i("EVO", "speakSegment:: SpeakTTS "+segment);
                teamChatBuddyApplication.speakTTS(
                        segment.trim(),
                        LabialExpression.SPEAK_NEUTRAL,
                        type
                );
            }
        };


        Log.d("EVO", "segments.length > 0"+segments.length);
        if (segments.length > 0) {
            Log.d("EVO", "segments.length > 0");
            callback.onSegmentDone();
        }
    }

    private void afficherPopupAvecBitmap(String imagePath) {
        if(dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_layout);
        ImageView imageView = dialog.findViewById(R.id.imageView);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bitmap);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.horizontalMargin = 0;
        layoutParams.verticalMargin = 0;
        dialog.getWindow().setAttributes(layoutParams);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
    }

    private void closeImage(){
        if(dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    private void startCameraForCommand(String promptPhoto) {
        Log.e("CameraX", " starting camera1");
        preViewViewLyt = view.findViewById(R.id.previewView_lyt);
        previewViewphoto = view.findViewById(R.id.previewView);
        cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
        previewViewphoto.post(() -> {
            try {
                if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")) {
                    stopTracking();
                }
                cameraProvider = ProcessCameraProvider.getInstance(getActivity()).get();
                // Récupérer la vue PreviewView avec findViewById()

                preViewViewLyt.setVisibility(View.VISIBLE);

                previewViewphoto.setTranslationX(0);

                preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Assurez-vous d'avoir un mode de capture
                        .setTargetRotation(getActivity().getWindowManager().getDefaultDisplay().getRotation()) // Définir la rotation correcte
                        .build();

                cameraProvider.unbindAll();

                // Utiliser previewView pour définir la SurfaceProvider
                preview.setSurfaceProvider(previewViewphoto.getSurfaceProvider());

                // Liaison de la caméra aux use cases
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                capturePhoto(promptPhoto);
                Log.i(TAG, "Camera use cases bound successfully");
            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        });
    }

    public void capturePhoto(String promptPhoto) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                timerPhoto = new CountDownTimer(3*1000L, 1000) {
                    public void onTick(long millisUntilFinished) {
                        showTimer(millisUntilFinished);
                    }
                    public void onFinish() {
                        hideTimer();
                        imageCapture.takePicture(ContextCompat.getMainExecutor(getActivity()), new ImageCapture.OnImageCapturedCallback() {
                            @Override
                            public void onCaptureSuccess(@NonNull ImageProxy image) {
                                commande.phototakedMessage();
                                Bitmap bitmap = imageProxyToBitmap(image);
                                showImageInDialog(bitmap);
                                responseFromChatbot.getQuestionToDescribePicture(bitmap,promptPhoto);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        stopCamera();
                                    }
                                },1000);
                                if(timerPhoto != null) timerPhoto.cancel();
                                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                                    isReTrack = true;
                                    initTracking();
                                }
                            }
                            @Override
                            public void onError(@NonNull ImageCaptureException exception) {
                                Toast.makeText(getActivity(), "Erreur lors de la capture de l'image", Toast.LENGTH_SHORT).show();
                                Log.e("CameraX", "Image capture error", exception);
                                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                                    isReTrack = true;
                                    initTracking();
                                }
                                if(commande!=null){
                                    commande.translate("CMD_PHOTO", new Commande.ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            if (translatedText.contains("No_message_defined")) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                            } else {
                                                String verifyMessage = commande.verifyCmdMessages(translatedText);
                                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                    commande.translateSpecificErrors(" problème lors de la capture de la photo", new Commande.ITranslationCallback() {
                                                        @Override
                                                        public void onTranslated(String translatedMessage) {
                                                            Log.i(TAG, "Error Translated: " + translatedMessage); // Check description translation
                                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",": "+translatedMessage));
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    }
                };
                timerPhoto.start();
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    private void showImageInDialog(Bitmap imageBitmap) {
        if(dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new Dialog(getActivity());

        dialog.setContentView(R.layout.dialog_layout);

        ImageView imageView = dialog.findViewById(R.id.imageView);
        imageView.setImageBitmap(imageBitmap);

        // Ajoutez un écouteur de clic à l'image pour fermer la boîte de dialogue lorsqu'elle est cliquée
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // To remove padding/margin/color of dialog (full screen image)
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.horizontalMargin = 0;
        layoutParams.verticalMargin = 0;
        dialog.getWindow().setAttributes(layoutParams);

        // Affichez la boîte de dialogue
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
    }

    public void stopCamera() {
        if (cameraProvider != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cameraProvider.unbindAll();
                    if (previewViewphoto != null) {
                        previewViewphoto.setTranslationX(1000000000);

                    }
                }
            });
        }
    }

    private void showTimer(long millisUntilFinished){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photo_timer_rlyt.setVisibility(View.VISIBLE);
                photo_timer_bg_rlyt.setVisibility(View.VISIBLE);
                photo_timer_imgview.setImageDrawable(animationTimerPhoto);
                String millisUntilFinishedString = Long.toString(( millisUntilFinished / 1000 ) + 1);
                photo_timer_txtView.setText(millisUntilFinishedString);
                animationTimerPhoto.start();
            }
        });

    }

    private void hideTimer(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photo_timer_rlyt.setVisibility(View.GONE);
                photo_timer_bg_rlyt.setVisibility(View.GONE);
                photo_timer_txtView.setText("");
            }
        });

    }

    private void detectBI(){
        boolean head_top_touched = BuddySDK.Sensors.HeadTouchSensors().Top().isTouched();
        boolean head_left_touched = BuddySDK.Sensors.HeadTouchSensors().Left().isTouched();
        boolean head_right_touched = BuddySDK.Sensors.HeadTouchSensors().Right().isTouched();
        boolean body_torso_touched = BuddySDK.Sensors.BodyTouchSensors().Torso().isTouched();
        boolean body_left_touched = BuddySDK.Sensors.BodyTouchSensors().LeftShoulder().isTouched();
        boolean body_right_touched = BuddySDK.Sensors.BodyTouchSensors().RightShoulder().isTouched();
        if (!teamChatBuddyApplication.getAppIsCurrentlyDealingWithTheQuestion() && !currentlyBIExecuted){
            if (head_top_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchCenterHead_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchCenterHead_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchCenterHead_Behavior");
                }
            }
            else if (head_left_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchLeftHead_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchLeftHead_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchLeftHead_Behavior");
                }
            }
            else if (head_right_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchRightHead_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchRightHead_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchRightHead_Behavior");
                }
            }
            else if (body_torso_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchHeart_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchHeart_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchHeart_Behavior");
                }
            }
            else if (body_left_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchLeftShoulder_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchLeftShoulder_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchLeftShoulder_Behavior");
                }
            }
            else if (body_right_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchRightShoulder_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchRightShoulder_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchRightShoulder_Behavior");
                }
            }
        }
    }

    private String getTheRightBINameFromConfigFile(String propertyName){
        String xmlBehaviour;
        if (!propertyName.equals("") && teamChatBuddyApplication.getParamFromFile(propertyName, "TeamChatBuddy.properties")!=null) {
            StringTokenizer st = new StringTokenizer(teamChatBuddyApplication.getParamFromFile(propertyName, "TeamChatBuddy.properties"), "/", false);
            List<String> listBehaviour = new ArrayList<>();
            while (st.hasMoreTokens()) {
                String result = st.nextToken();
                listBehaviour.add(result.toLowerCase());
            }
            if (listBehaviour.size() > 0) {
                xmlBehaviour = listBehaviour.get(new Random().nextInt(listBehaviour.size())).trim();
            } else {
                xmlBehaviour = "";
            }
        }
        else xmlBehaviour = "";
        Log.i("DEBUG_BI","xmlBehaviour récupéré depuis le fichier de configuration  "+xmlBehaviour);
        String dossierExterne = getString(R.string.path) + "/BI/Behaviour";
        if (!xmlBehaviour.contains(".xml") && !xmlBehaviour.equals("")){
            List<String> nomsFichiers = getFilenamesForCategory(dossierExterne, xmlBehaviour);
            if (!nomsFichiers.isEmpty()) {
                xmlBehaviour = nomsFichiers.get(new Random().nextInt(nomsFichiers.size()));
                Log.i("DEBUG_BI","Nom du fichier choisi : " + xmlBehaviour);
            } else {
                Log.i("DEBUG_BI","Aucun fichier trouvé pour la catégorie : " + xmlBehaviour);
            }
            Log.i("DEBUG_BI","Nom du fichier selon la  catégorie choisi : " + xmlBehaviour);
            return xmlBehaviour;
        }
        else if (xmlBehaviour.equals("")) {
            Log.i("DEBUG_BI", "Le fichier n'existe pas.");
            return "";
        }
        else {
            Log.i("DEBUG_BI", "xmlBehaviour : "+xmlBehaviour);
            return xmlBehaviour;
        }
    }

    private static List<String> getFilenamesForCategory(String dossier, String categorieRecherchee) {
        List<String> nomsFichiers = new ArrayList<>();
        File dossierBehaviours = new File(dossier);
        File[] fichiers = dossierBehaviours.listFiles();
        if (fichiers != null) {
            for (File fichier : fichiers) {
                if (fichier.isFile() && fichier.getName().toLowerCase().endsWith(".xml")) {
                    if (fichier.getName().toLowerCase().contains(categorieRecherchee.toLowerCase())) {
                        nomsFichiers.add(fichier.getName());
                    }
                }
            }
        }
        return nomsFichiers;
    }

    private void executeBI(String behaviour){
        if (applyBIAfterDelay) {
            Log.i("DEBUG_BI", "executeBI : " + behaviour);
            teamChatBuddyApplication.setBIExecution(true);
            currentlyBIExecuted= true;
            BIPlayer.getInstance().playBI(getActivity(), getTheRightBINameFromConfigFile(behaviour), new IBehaviourCallBack() {
                @Override
                public void onEnd(boolean hasAborted, String reason) {
                    Log.e("DEBUG_BI", "on END BI execution");
                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                    BuddySDK.UI.lookAt(GazePosition.CENTER, true);
                    currentlyBIExecuted = false;
                    teamChatBuddyApplication.setBIExecution(false);
                    currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                    totalTimeLookingAtCamera = 0L;
                    teamChatBuddyApplication.setSpeaking(false);
                }

                @Override
                public void onRun(String s) {
                }
            });
        }
    }
    private void playStartMessage(IStartMessageCallback iStartMessageCallback){
        this.iStartMessageCallback = iStartMessageCallback;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Get invitation from config File
                if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                    if(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties") != null && !teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties") .isEmpty()){
                        teamChatBuddyApplication.setActivityClosed(false);
                        speak(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties"), "STARTMESSAGE");
                    }
                    else {
                        if(iStartMessageCallback != null) iStartMessageCallback.onEnd("ConfigFile do not contain English Start Message");
                    }
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                    if(teamChatBuddyApplication.getParamFromFile("Start_Message_fr","TeamChatBuddy.properties") != null && !teamChatBuddyApplication.getParamFromFile("Start_Message_fr","TeamChatBuddy.properties").isEmpty()){
                        teamChatBuddyApplication.setActivityClosed(false);
                        speak(teamChatBuddyApplication.getParamFromFile("Start_Message_fr","TeamChatBuddy.properties"), "STARTMESSAGE");
                    }
                    else {
                        if(iStartMessageCallback != null) iStartMessageCallback.onEnd("ConfigFile do not contain French Start Message");
                    }
                }
                else {
                    if(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties") != null && !teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties").isEmpty()){
                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties"))
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        Log.d(TAG_TRACKING, "Translated Invitation: " + translatedText);
                                        teamChatBuddyApplication.setActivityClosed(false);
                                        speak(translatedText, "STARTMESSAGE");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        teamChatBuddyApplication.setActivityClosed(false);
                                        speak(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties"), "INVITATION");
                                    }
                                });
                    }
                    else {
                        if(iStartMessageCallback != null) iStartMessageCallback.onEnd("ConfigFile do not contain English Start Message");
                    }
                }

            }
        });


    }

    private void refreshSTTLangue() {
        List<String> STTAndroidLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_STT_Android");
        String codeLanguageSTTAndroid = STTAndroidLangueCode.get(new Gson().fromJson(teamChatBuddyApplication.getparam(settingClass.getLangue()), Langue.class).getId()-1);
        Log.i(TAG," context "+_parentActivity);
        teamChatBuddyApplication.refresh(codeLanguageSTTAndroid,_parentActivity);
    }

    private void setAnimation(String emotion){
        teamChatBuddyApplication.setCurrentEmotion("");
        if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Happy", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Thinking", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.THINKING,1);
            teamChatBuddyApplication.setLed("thinking");
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Sick", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SICK,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Love", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.LOVE,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Tired", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Listening", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.LISTENING,1);
            //teamChatBuddyApplication.setLed("listening");
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Surprised", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SURPRISED,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Grumpy", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.GRUMPY,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Scared", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SCARED,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Angry", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.ANGRY,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Sad", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SAD,1);
        }
        else{
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
        }
    }

    public void btnOpenSettings(View view) {
        if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")) {
            //AlertManager.getInstance(_parentActivity).incremente("touch", _parentActivity);
        }
        // Réinitialiser le timeout précédent
        if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null) {
            clickHandler.removeCallbacks(resetClickCountRunnable);
            clickCount++;
            if (clickCount >= Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Number_clicks_options", "TeamChatBuddy.properties"))) {
                if (runnableforClickBouton!=null){
                    handlerForClickBouton.removeCallbacks(runnableforClickBouton);
                    runnableforClickBouton = null;
                }
                runnableforClickBouton= new Runnable() {
                    @Override
                    public void run() {
                        if (!mlKitIsDownloading) {
                            Bundle args = new Bundle();
                            args.putString("activity_name", "main");
                            SettingsFragment settingsFragment = new SettingsFragment(_parentActivity);
                            settingsFragment.setArguments(args);
                            ((MainActivity) requireActivity()).navigateTo(settingsFragment, true);
                        }
                        else if (teamChatBuddyApplication.getBIExecution()) {
                            BIPlayer.getInstance().stopBehaviour();
                            Bundle args = new Bundle();
                            args.putString("activity_name", "main");
                            SettingsFragment settingsFragment = new SettingsFragment(_parentActivity);
                            settingsFragment.setArguments(args);
                            ((MainActivity) requireActivity()).navigateTo(settingsFragment, true);
                        }
                    }
                };
                handlerForClickBouton.postDelayed(runnableforClickBouton,300);
            } else {
                // Redémarrer le délai avant de réinitialiser le compteur
                clickHandler.postDelayed(resetClickCountRunnable, CLICK_TIMEOUT);
            }
        }
        else {
            if (runnableforClickBouton!=null){
                handlerForClickBouton.removeCallbacks(runnableforClickBouton);
                runnableforClickBouton = null;
            }
            runnableforClickBouton= new Runnable() {
                @Override
                public void run() {
                    if (!mlKitIsDownloading) {
                        Bundle args = new Bundle();
                        args.putString("activity_name", "main");
                        SettingsFragment settingsFragment = new SettingsFragment(_parentActivity);
                        settingsFragment.setArguments(args);
                        ((MainActivity) requireActivity()).navigateTo(settingsFragment, true);
                    } else if (teamChatBuddyApplication.getBIExecution()) {
                        BIPlayer.getInstance().stopBehaviour();
                        Bundle args = new Bundle();
                        args.putString("activity_name", "main");
                        SettingsFragment settingsFragment = new SettingsFragment(_parentActivity);
                        settingsFragment.setArguments(args);
                        ((MainActivity) requireActivity()).navigateTo(settingsFragment, true);

                    }
                }
            };
            handlerForClickBouton.postDelayed(runnableforClickBouton,300);
        }
    }

    public void btnOpenChat(View view) {
        if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")) {
            //AlertManager.getInstance(_parentActivity).incremente("touch", _parentActivity);
        }
        if (runnableforClickBouton!=null){
            handlerForClickBouton.removeCallbacks(runnableforClickBouton);
            runnableforClickBouton = null;
        }
        runnableforClickBouton= new Runnable() {
            @Override
            public void run() {
                if ( !mlKitIsDownloading) {
                    ((MainActivity) requireActivity()).navigateTo(new ChatWindowFragment(_parentActivity), true);
                }
                else if (teamChatBuddyApplication.getBIExecution()){
                    BIPlayer.getInstance().stopBehaviour();
                    ((MainActivity) requireActivity()).navigateTo(new ChatWindowFragment(_parentActivity), true);
                }
            }
        };
        handlerForClickBouton.postDelayed(runnableforClickBouton,300);
    }


    /**
     * ------------------------------------------ STT  -------------------------------------------
     */

    private void startListeningFreeSpeech(int duration) {
        // Si on vient du hotword (état encore "hotword"), le passage à "qst" est géré
        // par le postDelayed dans le handler STTHotword_success — ne pas l'écraser ici.
        if (!teamChatBuddyApplication.listeningState.equals("hotword")) {
            teamChatBuddyApplication.listeningState = "qst";
        }
        // NE PAS réinitialiser le flag hotword! Il doit rester TRUE pendant toute l'écoute
        // pour que checkTheHotword ne soit appelée qu'UNE SEULE FOIS

        Log.d("MYA_fragment"," --- startListeningFreeSpeech("+duration+") ---");
        Boolean notUsingSpeechRecognizer = true;
        isListeningFreeSpeech = true;
        teamChatBuddyApplication.setMessageError(false);
        teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
        teamChatBuddyApplication.setAppIsListeningToTheQuestion(true);
        teamChatBuddyApplication.setAlreadyChatting(false);

        Log.d(TAG," --- startListeningFreeSpeech("+duration+") ---");

        if(teamChatBuddyApplication.getBlueMic() != null
                && teamChatBuddyApplication.getBlueMic().getmBlueMicService() != null
                && teamChatBuddyApplication.getBlueMic().selectedBlueMic != null
                && teamChatBuddyApplication.getBlueMic().selectedBlueMic.getState().equals("Connected")){
            teamChatBuddyApplication.startListeningBlueMic(false, getActivity());
        }
        else{
            if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android")){
                notUsingSpeechRecognizer=false;
                teamChatBuddyApplication.startListeningQuestion(getActivity(),"FirstListening");
            }
            else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Whisper")) {
                teamChatBuddyApplication.startWhisperRecording(getActivity());
            }
            else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")){
                if (teamChatBuddyApplication.getCurrentLanguage().equals("fr") || teamChatBuddyApplication.getCurrentLanguage().equals("en")){
                    teamChatBuddyApplication.startListeningCerence(getActivity());
                }
                else{
                    notUsingSpeechRecognizer=false;
                    teamChatBuddyApplication.startListeningQuestion(getActivity(),"FirstListening");
                }
            }
            else teamChatBuddyApplication.startListeningQuestionWithGoogleApi(getActivity());
        }
        if (notUsingSpeechRecognizer) {
            if (!teamChatBuddyApplication.isModeContinuousListeningON()) {
                if (timerEcoute != null) timerEcoute.cancel();
                timerEcoute = new CountDownTimer(duration * 1000L, 1000) {
                    @Override
                    public void onTick(long l) {
                        Log.d(TAG, "timerEcoute onTick");
                    }

                    @Override
                    public void onFinish() {
                        if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && regarde_camera) {
                            Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                            timerEcoute.start();
                        } else {
                            Log.i(TAG, "timerEcoute onFinish");
                            if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")) {
                                teamChatBuddyApplication.notifyObservers("end of timer");
                            } else {
                                teamChatBuddyApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;false");
                            }
                        }
                    }
                };
                timerEcoute.start();
            }
        }
        else {
            notUsingSpeechRecognizer = true;
        }

    }

    private void stopListeningFreeSpeech() {
        isListeningFreeSpeech = false;
        Log.d(TAG," --- stopListeningFreeSpeech() ---");
        if (timerEcoute!=null) timerEcoute.cancel();
        teamChatBuddyApplication.stopListening(getActivity());
    }

    private void startCycle() {
        Log.e(TAG,"startCycle  after handler ");
        // NE PAS réinitialiser le flag hotword! Il reste TRUE pendant toute l'écoute
        // Forcer listeningState à "qst" pour que les détections soient traitées comme des questions
        teamChatBuddyApplication.listeningState = "qst";
        Boolean notUsingSpeechRecognizer =true;
        isListeningFreeSpeech = true;
        teamChatBuddyApplication.setMessageError(false);
        teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
        teamChatBuddyApplication.setAppIsListeningToTheQuestion(true);
        teamChatBuddyApplication.setAlreadyChatting(false);

        if(teamChatBuddyApplication.getBlueMic() != null
                && teamChatBuddyApplication.getBlueMic().getmBlueMicService() != null
                && teamChatBuddyApplication.getBlueMic().selectedBlueMic != null
                && teamChatBuddyApplication.getBlueMic().selectedBlueMic.getState().equals("Connected")){
            teamChatBuddyApplication.startListeningBlueMic(false, getActivity());
        }
        else{
            if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android")){
                notUsingSpeechRecognizer=false;
                teamChatBuddyApplication.startListeningQuestion(getActivity(),"startCycle");
            }
            else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Whisper")) {
                teamChatBuddyApplication.startWhisperRecording(getActivity());
            }
            else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")){
                if (teamChatBuddyApplication.getCurrentLanguage().equals("fr") || teamChatBuddyApplication.getCurrentLanguage().equals("en")){
                    teamChatBuddyApplication.startListeningCerence(getActivity());
                }
                else{
                    notUsingSpeechRecognizer=false;
                    teamChatBuddyApplication.startListeningQuestion(getActivity(),"startCycle");
                }
            }
            else teamChatBuddyApplication.startListeningQuestionWithGoogleApi(getActivity());
        }
        if (notUsingSpeechRecognizer){
            if(timerEcoute != null) timerEcoute.cancel();
            timerEcoute = new CountDownTimer(teamChatBuddyApplication.getListeningDuration() * 1000L,1000) {
                @Override
                public void onTick(long l) {
                    Log.d(TAG, "timerEcoute onTick");
                }
                @Override
                public void onFinish() {
                    if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && regarde_camera){
                        Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                        timerEcoute.start();
                    }
                    else{
                        Log.i(TAG, "timerEcoute onFinish");
                        if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")){
                            teamChatBuddyApplication.notifyObservers("end of cycle");
                            runnablePauseTime =new Runnable() {
                                @Override
                                public void run() {
                                    startNextCycle();
                                    Log.e(TAG,"startNextCycle  after handler ");
                                }
                            };
                            handlerPauseTime.postDelayed(runnablePauseTime,1000);
                        }
                        else{
                            teamChatBuddyApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;true");
                        }
                    }
                }
            };
            timerEcoute.start();
        }

    }

    private void startNextCycle() {
        Log.e(TAG,"startNextCycle  remainingattempts= "+teamChatBuddyApplication.getRemainingAttempts());
        String listensStr = teamChatBuddyApplication.getParamFromFile("Number_listens", "TeamChatBuddy.properties");
        //todo vérification Number_listens
        int numberListens = 0; // valeur par défaut
        if (listensStr != null && !listensStr.isEmpty()) {
            try {
                numberListens = Integer.parseInt(listensStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid number format in config, defaulting to 0", e);
            }
        }
        if (teamChatBuddyApplication.getRemainingAttempts() > 0 && numberListens > 0) {
            teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getRemainingAttempts()-1);
            startCycle();
            Log.e(TAG,"startNextCycle  after handler ");
        }
        else {
            teamChatBuddyApplication.notifyObservers("end of timer");
        }
    }


    /**
     * ------------------------------------------ TTS  -------------------------------------------
     */

    private void speak(final String texte, String type) {
        teamChatBuddyApplication.listeningState = "speaking";
        Log.e("MYA_YAKINE","listeningState in speak");
        Log.e("MYA_QR_H_","Start speaking receive");
        Log.e("TEAMCHAT_BUDDY_TRACKING"," --- speak("+texte+") ---type="+type+" isActivityClosed="+teamChatBuddyApplication.isActivityClosed());
        isSpeaking = true;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buddy_texte_resp_lyt.setTranslationY(0);
                if(ResponseFromChatbot.responseTimeout !=null){
                    ResponseFromChatbot.responseTimeout.cancel();
                }
                if(ChatGptStreamMode.responseTimeout !=null){
                    ChatGptStreamMode.responseTimeout.cancel();
                }
                if(CustomGPTStreamMode.responseTimeout !=null){
                    CustomGPTStreamMode.responseTimeout.cancel();
                }
                if (!teamChatBuddyApplication.getUsingEmotions()){
                    Log.d(TAG,"FacialExpression NEUTRAL");
                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                }
                if (!teamChatBuddyApplication.isActivityClosed()) {
                    if (type.equals("nothealysa") || type.equals("storedResponse")) {
                        teamChatBuddyApplication.setAlreadyGetAnswer(true);
                        if ( (teamChatBuddyApplication.getparam("Mode_Stream").contains("no") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") )
                                || ( teamChatBuddyApplication.getparam("Mode_Stream").contains("yes") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && teamChatBuddyApplication.getChatGptStreamMode() == null )
                        ){
                            if (settingClass.getSwitchVisibility().equals("true")) {
                                String stripSSML = teamChatBuddyApplication.stripSSML(texte);
                                if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                    buddy_texte_resp.setText(String.format("Response :  %s ", stripSSML));
                                } else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                    buddy_texte_resp.setText(String.format("Réponse :  %s ", stripSSML));
                                }
                                else if (teamChatBuddyApplication.getCurrentLanguage().equals("de")) {
                                    buddy_texte_resp.setText(String.format("Antwort :  %s ", stripSSML));
                                } else if (teamChatBuddyApplication.getCurrentLanguage().equals("es")) {
                                    buddy_texte_resp.setText(String.format("Respuesta :  %s ", stripSSML));
                                }
                                else{
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("Response").addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            buddy_texte_resp.setText(String.format(translatedText+" :  %s ", stripSSML));
                                        }

                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG,"translatedText exception  "+e);
                                        }
                                    });
                                }
                                if(type.equals("storedResponse")){
                                    lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                                    lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                                    if(buddy_texte_qst_lyt.getVisibility() != View.VISIBLE) buddy_texte_resp_lyt.setTranslationY(-155);
                                    else buddy_texte_resp_lyt.setTranslationY(0);
                                }
                                buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
                                buddy_texte_resp.setMovementMethod(new ScrollingMovementMethod());
                                buddy_texte_resp.scrollTo(0, 0);
                            }
                        }

                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        if ( (teamChatBuddyApplication.getparam("Mode_Stream").contains("no") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") )
                                || ( teamChatBuddyApplication.getparam("Mode_Stream").contains("yes") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && teamChatBuddyApplication.getChatGptStreamMode() == null )
                        ){
                            Replica reponse = new Replica();
                            reponse.setValue(teamChatBuddyApplication.stripSSML(texte));
                            reponse.setTime(time);
                            long responseTime = teamChatBuddyApplication.getGetResponseTime()- teamChatBuddyApplication.getQuestionTime();
                            DecimalFormat df = new DecimalFormat("#,###");
                            String formattedTime= df.format(responseTime);
                            reponse.setType("reponse");
                            reponse.setDuration(formattedTime + " ms");
                            DecimalFormat decimalFormatter = new DecimalFormat("0.00");
                            String formattedValue = decimalFormatter.format(Double.parseDouble(teamChatBuddyApplication.getparam("Total_cons")));
                            reponse.setPrix(formattedValue+" $");
                            listRep.add(reponse);
                            ArrayList<Replica> ll = new ArrayList<>();
                            for (int t = 0; t < listRep.size(); t++) {
                                ll.add(listRep.get(t));
                            }
                            Session session = new Session(ll);
                            ArrayList<Session> sessionTemp= teamChatBuddyApplication.getListSession();
                            sessionTemp.add(session);
                            teamChatBuddyApplication.setListSession(sessionTemp);
                            teamChatBuddyApplication.setparam("listSession", new Gson().toJson(sessionTemp));
                            listRep.clear();
                        }
                        else{
                            if (!listRep.isEmpty()) {
                                //--> this function is called right after a question : we should create a new Replica for the response
                                Replica reponse = new Replica();
                                reponse.setValue(teamChatBuddyApplication.stripSSML(texte));
                                reponse.setTime(time);
                                long responseTime = teamChatBuddyApplication.getGetResponseTime() - teamChatBuddyApplication.getQuestionTime();
                                DecimalFormat df = new DecimalFormat("#,###");
                                String formattedTime= df.format(responseTime);
                                reponse.setType("reponse");
                                reponse.setDuration(formattedTime + " ms");
                                DecimalFormat decimalFormatter = new DecimalFormat("0.00");
                                String formattedValue = decimalFormatter.format(Double.parseDouble(teamChatBuddyApplication.getparam("Total_cons")));
                                reponse.setPrix(formattedValue+" $");
                                listRep.add(reponse);
                                Session session = new Session(new ArrayList<>(listRep));
                                teamChatBuddyApplication.getListSession().add(session);
                                teamChatBuddyApplication.setparam("listSession", new Gson().toJson(teamChatBuddyApplication.getListSession()));
                                listRep.clear();
                            }
                            else{
                                //---> this function is called after finishing pronouncing a phrase from the response : we should add the new phrase to the already existing Replica
                                ArrayList<Session> listSessions = teamChatBuddyApplication.getListSession();
                                ArrayList<Replica> lastSession = listSessions.get(listSessions.size() - 1).getSession();
                                Replica lastReplica = lastSession.get(lastSession.size() - 1);
                                if (lastReplica.getType().equals("reponse")) {
                                    if (teamChatBuddyApplication.getChatGptStreamMode()!=null){
                                        if(!teamChatBuddyApplication.getChatGptStreamMode().isError) lastReplica.setValue(lastReplica.getValue() + texte);
                                    }
                                    else if (teamChatBuddyApplication.getCustomGPTStreamMode()!=null) {
                                        if (!teamChatBuddyApplication.getCustomGPTStreamMode().isError)
                                            lastReplica.setValue(lastReplica.getValue() + texte);
                                    }
                                    else lastReplica.setValue(texte);
                                    DecimalFormat decimalFormatter = new DecimalFormat("0.00");
                                    String formattedValue = decimalFormatter.format(Double.parseDouble(teamChatBuddyApplication.getparam("Total_cons")));
                                    lastReplica.setPrix(formattedValue+" $");
                                }
                            }
                        }

                        teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
                    else if (type.equals("timeOutExpired")){
                        teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
                    else if (type.equals("commande")) {
                        Log.i(TAG," type commande ");
                        String[] respo = new String[1];
                        teamChatBuddyApplication.setAlreadyGetAnswer( true );
                        if (!teamChatBuddyApplication.getMessageError() && !teamChatBuddyApplication.getUsingEmotions()) {
                            BuddySDK.UI.setFacialExpression( FacialExpression.NEUTRAL, 1 );
                        }
                        String displayText = texte.contains(";splitNews;") ? texte.replaceAll(";splitNews;","\n\n") : texte;

                        if (settingClass.getSwitchVisibility().equals( "true" )) {
                            switch (teamChatBuddyApplication.getCurrentLanguage()) {
                                case "fr": respo[0] = "Réponse"; break;
                                case "de": respo[0] = "Antwort"; break;
                                case "es": respo[0] = "Respuesta"; break;
                                case "en": respo[0] = "Response"; break;
                                default: teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate( "Response" ).addOnSuccessListener( new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        respo[0] = translatedText;
                                    }

                                } ).addOnFailureListener( new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e( TAG, "translatedText exception  " + e );
                                    }
                                } ); break;
                            }
                            if (texte.contains(";splitNews;")) {
                                isSplitStreamingNews = true;
                                streamSpeakSegments(texte, respo[0], type);
                            }
                            lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                            lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                            if(buddy_texte_qst_lyt.getVisibility() != View.VISIBLE) buddy_texte_resp_lyt.setTranslationY(-155);
                            else buddy_texte_resp_lyt.setTranslationY(0);
                            buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
                            buddy_texte_resp.setMovementMethod( new ScrollingMovementMethod() );
                            buddy_texte_resp.scrollTo( 0, 0 );
                        }

                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        Replica reponse = new Replica();
                        reponse.setValue(teamChatBuddyApplication.stripSSML(displayText));
                        reponse.setTime(time);
                        long responseTime = teamChatBuddyApplication.getGetResponseTime()- teamChatBuddyApplication.getQuestionTime();
                        DecimalFormat df = new DecimalFormat("#,###");
                        String formattedTime= df.format(responseTime);
                        reponse.setType("reponse");
                        reponse.setDuration(formattedTime + " ms");
                        DecimalFormat decimalFormatter = new DecimalFormat("0.00");
                        String formattedValue = decimalFormatter.format(Double.parseDouble(teamChatBuddyApplication.getparam("Total_cons")));
                        reponse.setPrix(formattedValue+" $");
                        listRep.add(reponse);
                        ArrayList<Replica> ll = new ArrayList<>();
                        for (int t = 0; t < listRep.size(); t++) {
                            ll.add(listRep.get(t));
                        }
                        Session session = new Session(ll);
                        ArrayList<Session> sessionTemp= teamChatBuddyApplication.getListSession();
                        sessionTemp.add(session);
                        teamChatBuddyApplication.setListSession(sessionTemp);
                        teamChatBuddyApplication.setparam("listSession", new Gson().toJson(sessionTemp));
                        listRep.clear();

                        if(!texte.contains(";splitNews;")){
                            String stripSSML = teamChatBuddyApplication.stripSSML(texte);
                            buddy_texte_resp.setText(String.format("%s :  %s", respo[0], stripSSML));
                            teamChatBuddyApplication.speakTTS( texte, LabialExpression.SPEAK_NEUTRAL, type );
                        }
                    }
                    else if(type.equals("INVITATION")){
                        Log.e("TEAMCHAT_BUDDY_TRACKING"," --- speakTTS from speak Main");
                        if (settingClass.getSwitchVisibility().equals( "true" )) {
                            String stripSSML = teamChatBuddyApplication.stripSSML(texte);
                            if (teamChatBuddyApplication.getCurrentLanguage().equals( "en" )) {
                                buddy_texte_resp.setText( String.format( "Response :  %s ", stripSSML) );
                            } else if (teamChatBuddyApplication.getCurrentLanguage().equals( "fr" )) {
                                buddy_texte_resp.setText( String.format( "Réponse :  %s ", stripSSML) );
                            } else if (teamChatBuddyApplication.getCurrentLanguage().equals( "de" )) {
                                buddy_texte_resp.setText( String.format( "Antwort :  %s ", stripSSML) );
                            } else if (teamChatBuddyApplication.getCurrentLanguage().equals( "es" )) {
                                buddy_texte_resp.setText( String.format( "Respuesta :  %s ", stripSSML) );
                            } else {
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate( "Response" ).addOnSuccessListener( new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        buddy_texte_resp.setText( String.format( translatedText + " :  %s ", stripSSML) );
                                    }
                                } ).addOnFailureListener( new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e( TAG, "translatedText exception  " + e );
                                    }
                                } );
                            }
                            lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                            lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                            if(buddy_texte_qst_lyt.getVisibility() != View.VISIBLE) buddy_texte_resp_lyt.setTranslationY(-155);
                            else buddy_texte_resp_lyt.setTranslationY(0);
                            buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
                            buddy_texte_resp.setMovementMethod( new ScrollingMovementMethod() );
                            buddy_texte_resp.scrollTo( 0, 0 );
                        }
                        teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
                    else if (type.equals("STARTMESSAGE")){
                        teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
                }
            }
        });
    }


    /**
     * ----------------------------------------- Scaning Qr Code ---------------------------------------
     */
    private void initScaning(List<String> types){
        teamChatBuddyApplication.setupZoomQRCode(new DetectionCallback() {
            @Override
            public void onDetection(String text) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }

            @Override
            public void onNoDetection() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Log.i("MYA_QR", "setupZoomQRCode:onNoDetection ");
                    }
                });
            }
        });
        teamChatBuddyApplication.startBarcodeScanner(getActivity(), types);
        cameraUtils = new CameraUtils(getActivity(), teamChatBuddyApplication);
        cameraUtils.setCameraView(view.findViewById(R.id.previewViewQr));
        cameraUtils.readyCamera(types);
    }

    /**
     * Helper method to determine if camera preview should be hidden
     * Preview must be hidden if QR/AprilTag detection is enabled (Lecture_QR_Code=yes)
     * AND Displaying_QRCode is enabled (Displaying_QRCode_period != "0" AND Displaying_QRCode_Duration != "0")
     */
    private boolean shouldHideCameraPreview() {
        boolean isQRDetectionActive = teamChatBuddyApplication.getParamFromFile("Lecture_QR_Code", "TeamChatBuddy.properties").trim().equalsIgnoreCase("yes");
        boolean isDisplayingQRCodePeriodEnabled = !teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period", "TeamChatBuddy.properties").trim().equalsIgnoreCase("0");
        boolean isDisplayingQRCodeDurationEnabled = !teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_Duration", "TeamChatBuddy.properties").trim().equalsIgnoreCase("0");
        boolean shouldHide = isQRDetectionActive && isDisplayingQRCodePeriodEnabled && isDisplayingQRCodeDurationEnabled;
        Log.d("CAMERA_PREVIEW_DEBUG", "shouldHideCameraPreview - QRDetection:" + isQRDetectionActive + ", Period:" + isDisplayingQRCodePeriodEnabled + ", Duration:" + isDisplayingQRCodeDurationEnabled + ", Result:" + shouldHide);
        return shouldHide;
    }

    /**
     * CENTRALIZED METHOD: Set reGroup visibility with automatic QR detection check
     * If Displaying_QRCode is enabled, preview is ALWAYS hidden regardless of requested value
     */
    private void setReGroupTranslationY(float translationY) {
        if (reGroup == null) return;

        // FORCE hide preview if Displaying_QRCode is enabled
        if (shouldHideCameraPreview()) {
            Log.d("CAMERA_PREVIEW_DEBUG", "setReGroupTranslationY() - FORCING HIDE (Displaying_QRCode active)");
            reGroup.setTranslationY(1000);
        } else {
            Log.d("CAMERA_PREVIEW_DEBUG", "setReGroupTranslationY() - Setting to: " + translationY);
            reGroup.setTranslationY(translationY);
        }
    }

    /**
     * Update camera preview visibility based on QR/AprilTag detection settings
     */
    private void updateCameraPreviewVisibility() {
        if (reGroup == null) {
            Log.w("CAMERA_PREVIEW_DEBUG", "updateCameraPreviewVisibility - reGroup is NULL");
            return;
        }

        boolean shouldHide = shouldHideCameraPreview();
        if (shouldHide) {
            // Always hide preview if both QR detection and Displaying_QRCode are enabled
            Log.d("CAMERA_PREVIEW_DEBUG", "updateCameraPreviewVisibility - HIDING preview (QR detection + Displaying enabled)");
            setReGroupTranslationY(1000);
        } else if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Camera_Display"))) {
            if (layoutTexteQR.getVisibility() == View.VISIBLE && layoutQRCode.getVisibility() == View.VISIBLE) {
                Log.d("CAMERA_PREVIEW_DEBUG", "updateCameraPreviewVisibility - HIDING preview (QR layout visible)");
                setReGroupTranslationY(1000);
            } else {
                Log.d("CAMERA_PREVIEW_DEBUG", "updateCameraPreviewVisibility - SHOWING preview (Tracking enabled, QR layout not visible)");
                setReGroupTranslationY(0);
            }
        } else {
            Log.d("CAMERA_PREVIEW_DEBUG", "updateCameraPreviewVisibility - HIDING preview (Tracking disabled)");
            setReGroupTranslationY(1000);
        }
    }

    /**
     * ----------------------------------------- Tracking ---------------------------------------
     */

    private IInvitationCallback iInvitationCallback;
    public interface IInvitationCallback {
        void onEnd(String s);
    }

    private enum StateTrackingListening {
        NONE,
        PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT,
        PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT,
        PERSON_IS_NOT_VISIBLE_TIMEOUT
    }
    private StateTrackingListening currentTrackingListeningState = StateTrackingListening.NONE;

    private enum StateTrackingWelcome {
        NONE,
        PERSON_IS_NOT_VISIBLE_TIMEOUT,
        PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT
    }
    private StateTrackingWelcome currentTrackingWelcomeState = StateTrackingWelcome.NONE;

    private Handler handlerCheckPersonDetection = new Handler();
    private Runnable runnableCheckPersonDetection = new Runnable() {
        public void run() {
            Log.i(TAG, "run: runnable person detection ");
            long currentTime = System.currentTimeMillis();

            if(isPersonDetected){
                personDetectedTimeToCloseApp = currentTime;
                if(!personIsVisible){
                    firstVisibleTime = currentTime;  // Update the time when person started being visible
                }
                personIsVisible =true;
                lastVisibleTime = currentTime;  // Update the time when person was last seen
                lastVisibleTime_saved = currentTime;// Update the time when person was last seen
                // --- Déclenchement du Handler de validation tracking ---
                Handler trackingHandler = new Handler(Looper.getMainLooper());
                int minDurationSec = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("TRACKING_MIN_DETECTION_DURATION", "TeamChatBuddy.properties"));

                final long visibleStart = firstVisibleTime;

                trackingHandler.postDelayed(() -> {
                    // Vérifie que la personne est encore visible après la durée minimale
                    long now = System.currentTimeMillis();
                    long visibleDuration = (now - visibleStart) / 1000;

                    if (isPersonDetected && visibleDuration >= minDurationSec) {
                        Log.i("MYA_ALERTE", "Présence validée après " + visibleDuration + "s → incrémentation activité tracking");

                        try {
                            if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")) {
                                AlertManager.getInstance(_parentActivity).incremente("tracking", _parentActivity); // activité confirmée
                            }
                        } catch (Exception ex) {
                            Log.e("MYA_ALERTE", "Erreur AlertManager lors de l’incrémentation tracking : " + ex.getMessage());
                        }
                    } else {
                        Log.i("MYA_ALERTE", "Présence non confirmée (durée " + visibleDuration + "s < " + minDurationSec + "s)");
                    }
                }, 1000L * minDurationSec);
                if (regarde_camera) {
                    lastLookingAtCameraTime = currentTime;  // Update the time when the person last looked at the camera
                    lastLookingAtCameraTimeToCloseApp= currentTime;
                    // If starting to look at the camera, set the start time
                    if (startLookingAtCameraTime == 0L) {
                        startLookingAtCameraTime = currentTime;
                        totalTimeLookingAtCamera = 0L; // Reset total time when starting to look at the camera
                    }
                    else {
                        // Calculate the interval time looking at the camera
                        totalTimeLookingAtCamera += currentTime - startLookingAtCameraTime;
                        // Update the start time for the next interval calculation
                        startLookingAtCameraTime = currentTime;
                    }
                }
                else {
                    // Reset the start time if not looking at the camera
                    startLookingAtCameraTime = 0L;
                    totalTimeLookingAtCamera = 0L;
                }
            }
            else{
                if(personIsVisible){
                    visibleDuration = currentTime - firstVisibleTime;
                    Log.e(TAG_TRACKING, "Person was detected for " + visibleDuration + " milliseconds");
                }
                personIsVisible =false;
                totalTimeLookingAtCamera = 0L;
                startLookingAtCameraTime = 0L;
            }


            //#region arrêt et lancement d'écoute
            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && !isSpeaking){
                /**
                 *  Lorsque personne ne regarde la camera pendant (TRACKING_DELAY_STOP_LISTEN secondes) il arrête d’écouter y compris les hotwords.
                 *  Si une personne regarde la caméra pendant (TRACKING_DELAY_START_LISTEN secondes), il se met à l’écoute sans attendre un hotword.
                 */
                if (isPersonDetected) {
                    if (regarde_camera) {
                        Log.w(TAG_TRACKING_DEBUG, "A person is visible again and is looking directly at the CAMERA totalTimeLookingAtCamera= "+totalTimeLookingAtCamera+" currentTrackingListeningState= "+currentTrackingListeningState);
                        useListeningNumberWithAutomaicListening = false;
                        if (totalTimeLookingAtCamera  >= TRACKING_DELAY_START_LISTEN * 1000L) {
                            if (currentTrackingListeningState != StateTrackingListening.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT) {
                                currentTrackingListeningState = StateTrackingListening.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT;

                                Log.w(TAG_TRACKING_DEBUG, "A person has been looking directly at the camera for TRACKING_DELAY_START_LISTEN="+TRACKING_DELAY_START_LISTEN+" seconds (or more) --> start listening");
                                Log.e(TAG_TRACKING_DEBUG,"isFirstInvitaion = "+isFirstInvitaion+"Tracking_Invitation= "+Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation")));
                                if (!isFirstInvitaion && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
                                    startListeningQuestion();
                                    teamChatBuddyApplication.listeningState = "qst";
                                }
                                else if (!Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
                                    startListeningQuestion();
                                    teamChatBuddyApplication.listeningState = "qst";
                                }
                                Log.w(TAG_TRACKING, "isFirstInvitaion= "+isFirstInvitaion);
                            }
                        }
                    }
                    else {
                        Log.w(TAG_TRACKING_DEBUG, "A person is visible again BUT is not looking at the CAMERA");
                        if (currentTime - lastLookingAtCameraTime >= TRACKING_DELAY_STOP_LISTEN * 1000L) {
                            if (currentTrackingListeningState != StateTrackingListening.PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT) {
                                currentTrackingListeningState = StateTrackingListening.PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT;
                                Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_DELAY_STOP_LISTEN="+TRACKING_DELAY_STOP_LISTEN+" seconds (or more) --> stop listening");
                                if (!useListeningNumberWithAutomaicListening){
                                    stopListeningEverything();
                                }

                            }
                        }
                    }
                }
                else if (currentTime - lastVisibleTime >= TRACKING_DELAY_STOP_LISTEN * 1000L) {
                    Log.w(TAG_TRACKING_DEBUG, "No person is visible");
                    if (currentTrackingListeningState != StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT) {
                        currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                        Log.w(TAG_TRACKING_DEBUG, "No person has been visible for TRACKING_DELAY_STOP_LISTEN="+TRACKING_DELAY_STOP_LISTEN+" seconds (or more) --> stop listening");
                        if (!useListeningNumberWithAutomaicListening){
                            stopListeningEverything();
                            teamChatBuddyApplication.listeningState = "hotword";
                        }
                    }
                }
            }
            //#endregion arrêt et lancement d'écoute


            //#region Invitation
            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
                Log.d("ALERTE", "Tracking_Invitation ");
                /**
                 * S’il n’a pas vu de personnes depuis (TRACKING_DELAY_WELCOME minutes)
                 * et qu’il détecte qu'une personne le regarde pendant (TRACKING_DURATION_WELCOME secondes),
                 * alors il prononce une invitation
                 */
                if (!isPersonDetected && currentTime - lastVisibleTime_saved >= TRACKING_DELAY_WELCOME * 60L * 1000L) {
                    if (currentTrackingWelcomeState != StateTrackingWelcome.PERSON_IS_NOT_VISIBLE_TIMEOUT) {
                        currentTrackingWelcomeState = StateTrackingWelcome.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                        Log.w(TAG_TRACKING, "No person has been visible for TRACKING_DELAY_WELCOME="+TRACKING_DELAY_WELCOME+" minutes (or more)");
                        sendInvitationPending = true;
                    }
                }
                if (sendInvitationPending && isPersonDetected && regarde_camera) {
                    if (totalTimeLookingAtCamera >= TRACKING_DURATION_WELCOME * 1000L) {
                        if (currentTrackingWelcomeState != StateTrackingWelcome.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT) {
                            currentTrackingWelcomeState = StateTrackingWelcome.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT;
                            Log.e(TAG_TRACKING, "A person has been looking directly at the camera for TRACKING_DURATION_WELCOME="+TRACKING_DURATION_WELCOME+" seconds (or more) --> Invitation");
                            sendInvitationPending = false;
                            if(!teamChatBuddyApplication.isAlreadyChatting()){
                                stopListeningFreeSpeech();
                                teamChatBuddyApplication.setStartRecording(false);
                                teamChatBuddyApplication.setSpeaking(false);
                                try {
                                    BuddySDK.UI.stopListenAnimation();
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
//                                invitation(null);
                                if (MainActivity.isFirstLaunch && isFirstInvitaion) {
                                    isFirstInvitaion = false;
                                }
                                invitation(new IInvitationCallback() {
                                    @Override
                                    public void onEnd(String s) {
                                        Log.e(TAG_TRACKING, "Invitation onEnd Callback : "+s);
                                        iInvitationCallback = null;
                                        if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                                            startListeningQuestion();
                                        }
                                        else{

                                            if (teamChatBuddyApplication.isShouldDisplayQRCode() && !qr_displayed) {
                                                if(!teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim().equalsIgnoreCase("0")){
                                                    teamChatBuddyApplication.notifyObservers("hideCameraQr");
                                                    //Log.d("MainFragment", "------------ hideCameraQr runnable -------------");
                                                }
                                                Log.e(TAG,"call displayQRCode 1 ");
                                                displayQRCode(new IDisplayQrCodeCallback() {
                                                    @Override
                                                    public void onEnd() {
                                                        teamChatBuddyApplication.setAlreadyChatting(false);
                                                        Log.e(TAG,"startListeningHotword 9");
                                                        teamChatBuddyApplication.startListeningHotwor(getActivity());
                                                    }
                                                });
                                            }
                                            else{
                                                teamChatBuddyApplication.setAlreadyChatting(false);
                                                Log.e(TAG,"startListeningHotword 10");
                                                teamChatBuddyApplication.startListeningHotwor(getActivity());
                                            }
                                        }

                                    }
                                });

                            }
                            else{
                                Log.e(TAG_TRACKING, "Do not say invitation because the person is already chatting");
                            }
                        }
                    }
                }
            }
            //#endregion Invitation


            //#region re-tracking, re-centering gaze and head
            if(TRACKING_DELAY_NO_WATCH!=0&&isRecenter){
                Log.i(TAG_TRACKING," +++++++++++++++++++++++++re-tracking++++++++++++++++++++++++++");
                if (isPersonDetected && !regarde_camera && currentTime - lastLookingAtCameraTime >= TRACKING_DELAY_NO_WATCH * 1000L) {
                    Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_DELAY_NO_WATCH=" + TRACKING_DELAY_NO_WATCH + " seconds --> re-tracking + re-centering the gaze and head");
                    //re_track_and_center_head();

                    isRecenter=false;
                }
                if (!isPersonDetected && !regarde_camera && currentTime - lastLookingAtCameraTime >= TRACKING_REGARD_CENTER * 1000L) {
                    Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_REGARD_CENTER=" + TRACKING_REGARD_CENTER + " seconds --> refocus the pupils");
                    //re_track_and_center_head();
                    isRecenter=false;
                }
            }else{
                Log.i(TAG_TRACKING," +++++++++++++++++++++++pas de re-tracking++++++++++++++++++++++++++");
            }
            //#endregion re-tracking, re-centering gaze and head

            //#region Timer to exit the application
            Log.i("Finiche"," currentTime : "+currentTime+" /lastLookingAtCameraTimeToCloseApp "+lastLookingAtCameraTimeToCloseApp );
            Log.i("Finiche"," currentTime - lastLookingAtCameraTimeToCloseApp : "+(currentTime - lastLookingAtCameraTimeToCloseApp));
            if ( WATCHING_timeout!=0 && !regarde_camera && currentTime - lastLookingAtCameraTimeToCloseApp >= WATCHING_timeout * 1000L){
                Log.i("Finiche","closed app ");
                requireActivity().finishAffinity();
                if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")) {
                    AlertManager.getInstance(_parentActivity).stop();
                }
                System.exit(0);
            }
            //#region Timer to exit the application
            Log.i("Finiche"," currentTime : "+currentTime+" /personDetectedTimeToCloseApp "+personDetectedTimeToCloseApp );
            Log.i("Finiche"," currentTime - personDetectedTimeToCloseApp : "+(currentTime - personDetectedTimeToCloseApp));
            if (teamChatBuddyApplication.getparam("TRACKING_timeout_Switch").trim().equalsIgnoreCase("true") && TRACKING_TIMEOUT!=0 && !regarde_camera && currentTime - personDetectedTimeToCloseApp >= TRACKING_TIMEOUT * 1000L){
                Log.i("Finiche","closed app ");
                requireActivity().finishAffinity();
                if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")) {
                    AlertManager.getInstance(_parentActivity).stop();
                }
                System.exit(0);
            }

            //#endregion Timer to exit the application

        }
    };

    private void initTracking(){
        Log.d(TAG_TRACKING, "initTracking(isReTrack="+isReTrack+")");

        if(!MainActivity.isFirstLaunch && !isReTrack){
            try{
                if(BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
                    BuddySDK.USB.enableWheels(true, iUsbCommadRspTracking);
                }
                if(BuddySDK.Actuators.getYesStatus().toUpperCase().contains("DISABLE")) {
                    BuddySDK.USB.enableYesMove(true, iUsbCommadRspTracking);
                }
                if(BuddySDK.Actuators.getNoStatus().toUpperCase().contains("DISABLE")) {
                    BuddySDK.USB.enableNoMove(true, iUsbCommadRspTracking);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        if(!isReTrack){
            //récupération des paramètres TRACKING du fichier de config:
            TRACKING_WATCH = teamChatBuddyApplication.getParamFromFile("TRACKING_watch", "TeamChatBuddy.properties");
            TRACKING_DELAY_NO_WATCH = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("TRACKING_delay_nowatching", "TeamChatBuddy.properties"));
            TRACKING_DELAY_START_LISTEN = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("TRACKING_delay_startlisten", "TeamChatBuddy.properties"));
            TRACKING_DELAY_STOP_LISTEN = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("TRACKING_delay_stoplisten", "TeamChatBuddy.properties"));
            TRACKING_REGARD_CENTER = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("TRACKING_regard_center", "TeamChatBuddy.properties"));
            TRACKING_DELAY_WELCOME = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("WELCOME_delay", "TeamChatBuddy.properties"));
            TRACKING_DURATION_WELCOME = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("WELCOME_duration_tracking", "TeamChatBuddy.properties"));
            TRACKING_WELCOME_FR = teamChatBuddyApplication.getParamFromFile("WELCOME_messages_FR", "TeamChatBuddy.properties");
            TRACKING_WELCOME_EN =teamChatBuddyApplication.getParamFromFile("WELCOME_messages_EN", "TeamChatBuddy.properties");
            TRACKING_WELCOME_MODEL = teamChatBuddyApplication.getParamFromFile("WELCOME_model", "TeamChatBuddy.properties");
            TRACKING_WELCOME_TEMPERATURE = Double.parseDouble(teamChatBuddyApplication.getParamFromFile("WELCOME_temperature", "TeamChatBuddy.properties"));
            TRACKING_WELCOME_PROMPT_FR = teamChatBuddyApplication.getParamFromFile("WELCOME_prompt_FR", "TeamChatBuddy.properties");
            TRACKING_WELCOME_PROMPT_EN = teamChatBuddyApplication.getParamFromFile("WELCOME_prompt_EN", "TeamChatBuddy.properties");
            TRACKING_WELCOME_MAX_TOKEN = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("WELCOME_maxtoken", "TeamChatBuddy.properties"));
            try {
                TRACKING_TIMEOUT=Integer.parseInt(teamChatBuddyApplication.getparam("trackingTimeout"));
            }
            catch (Exception e){
                TRACKING_TIMEOUT=0;
            }
            try {
                WATCHING_timeout=Integer.parseInt(teamChatBuddyApplication.getParamFromFile("WATCHING_timeout","TeamChatBuddy.properties"));
            }
            catch (Exception e){
                WATCHING_timeout=0;
            }
        }

        if(MainActivity.isFirstLaunch && !isReTrack && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
            sendInvitationPending = true;
            isFirstInvitaion = true;
            startTracking();
        }
        else {
            startTracking();
        }
    }
    private void stopTrackingTest() {
        // Arrêter l'analyse d'image
        if (imageAnalyzer != null) {
            imageAnalyzer.clearAnalyzer();
            imageAnalyzer = null;
        }

        // Arrêter et libérer les ressources de la caméra
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        if (preview != null) {
            preview.setSurfaceProvider(null);
            preview = null;
        }

        // Stop and release resources from the PoseLandmarkerHelper
        if (poseLandmarkerHelper != null) {
            poseLandmarkerHelper.clearPoseLandmarker();
            poseLandmarkerHelper = null;
        }
        // Arrêter l'exécuteur de thread
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdownNow();
            backgroundExecutor = null;
        }
    }

    private void startTracking(){
        Log.d(TAG_TRACKING, "startTracking(isReTrack="+isReTrack+")");

        stopTrackingTest();
        if (poseTracking != null) {
            poseTracking.stopMovingAndCancelRunnables();
            poseTracking = null;
        }
        poseTracking= new PoseTracking();
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
            backgroundExecutor = null;
        }
        backgroundExecutor = Executors.newSingleThreadExecutor();
        cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
        viewModel = new ViewModelProvider(getActivity()).get(MainViewModel.class);
        viewModel.setMinPoseDetectionConfidence(PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE);
        viewModel.setMinPoseTrackingConfidence(PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE);
        viewModel.setMinPosePresenceConfidence(PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE);
        viewModel.setDelegate(PoseLandmarkerHelper.DELEGATE_GPU);
        viewModel.set_model(PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL);

        setUpCamera();
    }

    private void setUpCamera() {
        Log.e(TAG_TRACKING_DEBUG,"setUpCamera(isReTrack="+isReTrack+")");
        isTrackingAlreadyInitialised = false;
        previewView.post(() -> {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(getActivity()).get();
                if (preview != null) {
                    preview.setSurfaceProvider(null);
                    preview = null;
                }

                if (imageAnalyzer != null) {
                    imageAnalyzer.clearAnalyzer();
                    imageAnalyzer = null;
                }
                preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();
                imageAnalyzer = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                imageAnalyzer.setAnalyzer(backgroundExecutor, this::detectPose);
                cameraProvider.unbindAll();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
                Log.i(TAG, "Camera bound successfully");
            } catch (Exception e) {
                Log.e(TAG, "Camera binding failed", e);
            }
        });
        backgroundExecutor.execute(() -> {
            Context context = getActivity();
            poseLandmarkerHelper = new PoseLandmarkerHelper(
                    context,
                    RunningMode.LIVE_STREAM,
                    PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE,
                    PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE,
                    PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE,
                    PoseLandmarkerHelper.DELEGATE_CPU,
                    PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
                    new PoseLandmarkerHelper.LandmarkerListener() {
                        @Override
                        public void onError(String error, int errorCode) {}
                        @Override
                        public void onResults(PoseLandmarkerHelper.ResultBundle resultBundle) {

                            if(!isTrackingAlreadyInitialised){
                                isTrackingAlreadyInitialised = true;
                                //initialisations
                                lastVisibleTime = System.currentTimeMillis();
                                if(!isReTrack){
                                    lastVisibleTime_saved = System.currentTimeMillis(); //do not reset it when re-track (useful for invitation check)
                                    lastLookingAtCameraTimeToCloseApp = System.currentTimeMillis();
                                    personDetectedTimeToCloseApp = System.currentTimeMillis();
                                }
                                firstVisibleTime = System.currentTimeMillis();
                                lastLookingAtCameraTime = System.currentTimeMillis();
                                visibleDuration = 0;
                                isPersonDetected = false;
                                personIsVisible = false;
                                isProcessingReTrack = false;
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateCameraPreviewVisibility();
                                    }
                                });
                            }

                            if(!isProcessingReTrack){

                                if(dLeft < initLang){
                                    regarde_camera =  direction && deFace && directionRegardNez.equals("CAMERA");
                                }
                                else {
                                    regarde_camera =   deFace && directionRegardNez.equals("CAMERA");
                                }

                                PoseLandmarkerResult poseLandmarkerResult = resultBundle.results.get(0);
                                isPersonDetected = PoseLandmarkerHelper.extractLandmarks(poseLandmarkerResult);

                                //Log.i(TAG_TRACKING_DEBUG, "regarde_camera : "+regarde_camera);
                                //Log.i(TAG_TRACKING_DEBUG, "direction : "+direction);
                                //Log.i(TAG_TRACKING_DEBUG, "deFace : "+deFace);
                                //Log.i(TAG_TRACKING_DEBUG, "directionRegardNez : "+directionRegardNez);
                                //Log.i(TAG_TRACKING_DEBUG, "isPersonDetected : "+ isPersonDetected);

                                res = poseTracking.suivi(poseLandmarkerResult);
                                eog = res[0];
                                Eod = res[1];
                                degx = res[2];
                                degy = res[3];
                                x0 = res[4];
                                x2 = res[5];
                                x5 = res[6];
                                y0 = res[7];
                                y2 = res[8];
                                y5 = res[9];
                                lang = res[12];
                                dLeft = res[10];
                                dRight = res[11];

                                if (isPersonDetected != wasPersonDetected) {
                                    if (!isPersonDetected) {
                                        poseTracking.stopMovingAndCancelRunnables();
                                    }
                                    // Update the previous state
                                    wasPersonDetected = isPersonDetected;
                                }

                                if(isPersonDetected){
                                    poseTracking.look_at(degx, degy);

                                    if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body")) || Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
                                        if (TRACKING_WATCH.trim().equalsIgnoreCase("Yes")) {
                                            if (regarde_camera) {
                                                isRecenter=true;
                                                poseTracking.Rotation(degx, Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body")));
                                            }else{
                                                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
                                                    BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);
                                                }if(isRecenter) {
                                                    BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);
                                                }else {
                                                    int No_position = BuddySDK.Actuators.getNoPosition();
                                                    if(No_position!=0){
                                                        BuddySDK.USB.buddySayNo(100,0,iUsbCommadRspBI);

                                                    }else {
                                                        BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);
                                                    }
                                                }
                                                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body"))){
                                                    BuddySDK.USB.emergencyStopMotors(iUsbCommadRspTracking);
                                                }

                                            }
                                        }
                                        else {
                                            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
                                                if(isRecenter){
                                                    poseTracking.Rotation(degx, Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body")));
                                                }
                                                if (regarde_camera) {
                                                    isRecenter = true;
                                                } else {
                                                    if (!isRecenter) {
                                                        int No_position = BuddySDK.Actuators.getNoPosition();
                                                        if (No_position != 0) {
                                                            BuddySDK.USB.buddySayNo(100, 0, iUsbCommadRspBI);

                                                        } else {
                                                            BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);

                                                        }
                                                    }
                                                }
                                            }else{
                                                poseTracking.Rotation(degx, Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body")));
                                            }
                                        }
                                    }

                                    if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
                                        poseTracking.yesTracking(degy);
//                                        if (TRACKING_WATCH.trim().equalsIgnoreCase("Yes")) {
//                                            if (regarde_camera) {
//                                                isRecenter=true;
//                                                poseTracking.yesTracking(degy);
//                                            }else{
//                                                if(isRecenter) {
//                                                    BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
//                                                }else {
//                                                    int No_position = BuddySDK.Actuators.getYesPosition();
//                                                    if(No_position!=6){
//                                                        BuddySDK.USB.buddySayYes(100,6,iUsbCommadRspBI);
//
//                                                    }else {
//                                                        BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
//                                                    }
//                                                }
//                                            }
//                                        } else {
//                                            poseTracking.yesTracking(degy);
//                                            if (regarde_camera) {
//                                                isRecenter = true;
//                                            } else{
//                                                if(isRecenter) {
//                                                    BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
//                                                }else {
//                                                    int No_position = BuddySDK.Actuators.getYesPosition();
//                                                    if(No_position!=6){
//                                                        BuddySDK.USB.buddySayYes(100,6,iUsbCommadRspBI);
//
//                                                    }else {
//                                                        BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
//                                                    }
//                                                }
//                                            }
//                                        }
                                    }
                                }

                                directionRegardNez = poseTracking.directionVisage(x2, y2, x5, y5, x0, y0, lang);

                                if ((x2 - x5) > 0 ) {
                                    deFace = true;
                                }
                                else {
                                    deFace = false;
                                }
                                if (eog > Eod * 2) {
                                    direction = false;
                                }
                                else {
                                    if (eog * 1.5 < Eod) {
                                        direction = false;
                                    } else {
                                        direction = true;
                                    }
                                }

                                getActivity().runOnUiThread(() -> {
                                    if (overlay != null) {
                                        overlay.setResults(resultBundle.results.get(0), resultBundle.inputImageHeight, resultBundle.inputImageWidth, RunningMode.LIVE_STREAM);
                                        overlay.setNbrLandmarks(poseTracking.getLandmarksCamera(poseLandmarkerResult));
                                    }
                                });

                                handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
                                handlerCheckPersonDetection.removeCallbacksAndMessages(null);
                                handlerCheckPersonDetection.post(runnableCheckPersonDetection);
                            }
                            else{
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateCameraPreviewVisibility();
                                    }
                                });
                            }
                        }
                    }
            );
        });
    }
    private void detectPose(ImageProxy imageProxy) {
        poseLandmarkerHelper.detectLiveStream(imageProxy);
    }

    private void re_track_and_center_head(){
        //Log.d(TAG_TRACKING, "re_track_and_center_head");
        //isProcessingReTrack = true;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //stopTracking();
                Log.d(TAG_TRACKING, "re_track_and_center_eys");
                poseTracking.lookAtCenter();
                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))){
                    Log.d(TAG_TRACKING, "re_track_and_center_head");
                    poseTracking.centerHead();
                }

            }
        });
    }

    private void stopTracking(){
        Log.d("CAMERA_PREVIEW_DEBUG", "stopTracking() - HIDING preview");
        setReGroupTranslationY(1000);
        cameraProvider.unbindAll();
        handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
        handlerCheckPersonDetection.removeCallbacksAndMessages(null);
        poseTracking.stopMovingAndCancelRunnables();
    }

    private void startListeningQuestion(){
        Log.d(TAG_TRACKING, "startListeningQuestion()");
        teamChatBuddyApplication.listeningState = "qst";
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    isSpeaking =false;
                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                    if(handler!=null && runnable!=null){
                        handler.removeCallbacks(runnable);
                        handler.removeCallbacksAndMessages(null);
                    }
                    if(ResponseFromChatbot.responseTimeout !=null){
                        ResponseFromChatbot.responseTimeout.cancel();
                    }
                    if(ChatGptStreamMode.responseTimeout !=null){
                        ChatGptStreamMode.responseTimeout.cancel();
                    }
                    if(CustomGPTStreamMode.responseTimeout !=null){
                        CustomGPTStreamMode.responseTimeout.cancel();
                    }
                    if(teamChatBuddyApplication.getChatGptStreamMode() != null){
                        teamChatBuddyApplication.getChatGptStreamMode().reset();
                    }
                    if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                        teamChatBuddyApplication.getCustomGPTStreamMode().reset();
                    }
                    if(handlerTTSError!=null && runnableTTSError!=null){
                        handlerTTSError.removeCallbacks(runnableTTSError);
                        handlerTTSError.removeCallbacksAndMessages(null);
                    }
                    Log.d(TAG_TRACKING, "startListeningQuestion() if first");
                    if (!teamChatBuddyApplication.getSpeaking() && !mlKitIsDownloading){
                        teamChatBuddyApplication.setStartRecording(true);
                        teamChatBuddyApplication.setSpeaking(true);
                        if(!isListeningFreeSpeech ) {
                            Log.d(TAG_TRACKING, "startListeningQuestion() if second");
                            isListeningFreeSpeech=true;
                            teamChatBuddyApplication.setActivityClosed(false);
                            startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                        }
                        Log.d(TAG_TRACKING, "startListeningQuestion() isListeningFreeSpeech="+isListeningFreeSpeech);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void stopListeningEverything(){
        Log.e(TAG_TRACKING, "stopListeningEverything()");
        getActivity().runOnUiThread( new Runnable() {
            @Override
            public void run() {
                try{
                    isSpeaking =false;
                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                    if(handler!=null && runnable!=null){
                        handler.removeCallbacks(runnable);
                        handler.removeCallbacksAndMessages(null);
                    }
                    if(ResponseFromChatbot.responseTimeout !=null){
                        ResponseFromChatbot.responseTimeout.cancel();
                    }
                    if(ChatGptStreamMode.responseTimeout !=null){
                        ChatGptStreamMode.responseTimeout.cancel();
                    }
                    if(CustomGPTStreamMode.responseTimeout !=null){
                        CustomGPTStreamMode.responseTimeout.cancel();
                    }
                    if(teamChatBuddyApplication.getChatGptStreamMode() != null){
                        teamChatBuddyApplication.getChatGptStreamMode().reset();
                    }
                    if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                        teamChatBuddyApplication.getCustomGPTStreamMode().reset();
                    }
                    if(handlerTTSError!=null && runnableTTSError!=null){
                        handlerTTSError.removeCallbacks(runnableTTSError);
                        handlerTTSError.removeCallbacksAndMessages(null);
                    }
                    if (teamChatBuddyApplication.getSpeaking() && !mlKitIsDownloading) {
                        if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence") || !teamChatBuddyApplication.getAppIsListeningToTheQuestion() || teamChatBuddyApplication.getParamFromFile("Processing_the_audio_sequence","TeamChatBuddy.properties").trim().equalsIgnoreCase("No")) {
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            teamChatBuddyApplication.setStartRecording(false);
                            teamChatBuddyApplication.setSpeaking(false);
                            teamChatBuddyApplication.setActivityClosed(true);
                            isListeningFreeSpeech = false;
                            teamChatBuddyApplication.stopTTS();
                            teamChatBuddyApplication.setStoredResponse("");
                            if (buddy_texte_qst_lyt != null && buddy_texte_resp_lyt != null && buddy_texte_qst != null && buddy_texte_resp != null) {
                                buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                buddy_texte_qst.setMovementMethod(null);
                                buddy_texte_resp.setMovementMethod(null);
                            }
                            if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null ){
                                String Number_clicks_options = teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties");
                                if(Number_clicks_options.equals("")||Integer.parseInt(Number_clicks_options)<=0){
                                    applySettingsBtnVisibility();
                                }
                                else{
                                    applySettingsBtnVisibility();
                                }
                            }else {
                                applySettingsBtnVisibility();
                            }
                            lyt_open_menu_chat.setVisibility(View.VISIBLE);
                            try {
                                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                            } catch (Exception e) {
                                Log.e(TAG, "BuddySDK Exception  " + e);
                            }
                            teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                            stopListeningFreeSpeech();
                            try {
                                BuddySDK.UI.stopListenAnimation();
                            } catch (Exception e) {
                                Log.e(TAG, "BuddySDK Exception  " + e);
                            }
                            teamChatBuddyApplication.notifyObservers("end of timer");
                        }
                        else{
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            try {
                                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                BuddySDK.UI.stopListenAnimation();
                            } catch (Exception e) {
                                Log.e(TAG, "BuddySDK Exception  " + e);
                            }
                            BuddySDK.UI.stopListenAnimation();
                            teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                            teamChatBuddyApplication.traitementAudio(false);
                        }
                    }
                }
                catch (Exception e){
                    Log.e(TAG,"Exception  "+e);
                    e.printStackTrace();
                }
            }
        } );
    }

    private void invitation(IInvitationCallback iInvitationCallback){
        Log.e(TAG_TRACKING, "invitation()");
        this.iInvitationCallback = iInvitationCallback;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation_ChatGpt"))){
                    //Get invitation from ChatGPT
                    if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                        if(TRACKING_WELCOME_PROMPT_EN != null && !TRACKING_WELCOME_PROMPT_EN.isEmpty()){
                            responseFromChatbot.getInvitationFromChatGPT(TRACKING_WELCOME_MODEL,TRACKING_WELCOME_TEMPERATURE,TRACKING_WELCOME_MAX_TOKEN,TRACKING_WELCOME_PROMPT_EN);
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English ChatGPT Invitation prompt");
                        }
                    }
                    else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                        if(TRACKING_WELCOME_PROMPT_FR != null && !TRACKING_WELCOME_PROMPT_FR.isEmpty()){
                            responseFromChatbot.getInvitationFromChatGPT(TRACKING_WELCOME_MODEL,TRACKING_WELCOME_TEMPERATURE,TRACKING_WELCOME_MAX_TOKEN,TRACKING_WELCOME_PROMPT_FR);
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain French ChatGPT Invitation prompt");
                        }
                    }
                    else {
                        if(TRACKING_WELCOME_PROMPT_EN != null && !TRACKING_WELCOME_PROMPT_EN.isEmpty()){
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(TRACKING_WELCOME_PROMPT_EN)
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            responseFromChatbot.getInvitationFromChatGPT(TRACKING_WELCOME_MODEL,TRACKING_WELCOME_TEMPERATURE,TRACKING_WELCOME_MAX_TOKEN,translatedText);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            responseFromChatbot.getInvitationFromChatGPT(TRACKING_WELCOME_MODEL,TRACKING_WELCOME_TEMPERATURE,TRACKING_WELCOME_MAX_TOKEN,TRACKING_WELCOME_PROMPT_EN);
                                        }
                                    });
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English ChatGPT Invitation prompt");
                        }
                    }
                }
                else{
                    //Get invitation from config File
                    if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                        if(TRACKING_WELCOME_EN != null && !TRACKING_WELCOME_EN.isEmpty()){
                            String[] englishInvitations = TRACKING_WELCOME_EN.substring(1, TRACKING_WELCOME_EN.length() - 1).split("/");
                            String randomInvitationEN = englishInvitations[random.nextInt(englishInvitations.length)];
                            Log.d(TAG_TRACKING, "Random English Invitation: " + randomInvitationEN);
                            teamChatBuddyApplication.setActivityClosed(false);
                            speak(randomInvitationEN, "INVITATION");
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English Invitation");
                        }
                    }
                    else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                        if(TRACKING_WELCOME_FR != null && !TRACKING_WELCOME_FR.isEmpty()){
                            String[] frenchInvitations = TRACKING_WELCOME_FR.substring(1, TRACKING_WELCOME_FR.length() - 1).split("/");
                            String randomInvitationFR = frenchInvitations[random.nextInt(frenchInvitations.length)];
                            Log.d(TAG_TRACKING, "Random French Invitation: " + randomInvitationFR);
                            teamChatBuddyApplication.setActivityClosed(false);
                            speak(randomInvitationFR, "INVITATION");
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain French Invitation");
                        }
                    }
                    else {
                        if(TRACKING_WELCOME_EN != null && !TRACKING_WELCOME_EN.isEmpty()){
                            String[] englishInvitations = TRACKING_WELCOME_EN.substring(1, TRACKING_WELCOME_EN.length() - 1).split("/");
                            String randomInvitationEN = englishInvitations[random.nextInt(englishInvitations.length)];
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(randomInvitationEN)
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            Log.d(TAG_TRACKING, "Translated Invitation: " + translatedText);
                                            teamChatBuddyApplication.setActivityClosed(false);
                                            speak(translatedText, "INVITATION");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG_TRACKING, "Translation failed, using English Invitation: " + randomInvitationEN);
                                            teamChatBuddyApplication.setActivityClosed(false);
                                            speak(randomInvitationEN, "INVITATION");
                                        }
                                    });
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English Invitation");
                        }
                    }
                }
            }
        });
    }

    /**
     * ----------------------------------------- Display QRCode ---------------------------------------
     */
    private IDisplayQrCodeCallback iDisplayQrCodeCallback;
    public interface IDisplayQrCodeCallback {
        void onEnd();
    }

    private void displayQRCode(IDisplayQrCodeCallback iDisplayQrCodeCallback){
        Log.e("TEST_QR","displayQRCode");
        if (teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties")!=null
                && teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_Duration","TeamChatBuddy.properties")!=null
                && Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim())!=0
                && Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_Duration","TeamChatBuddy.properties").trim())!=0
                && qRCodeExist()){

            // Vérifier si une image correspondante (insensible à la casse) existe
            File fichierImage = findFileIgnoreCase();

            if (fichierImage != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(fichierImage.getAbsolutePath());
                imageQRCode.setImageBitmap(bitmap);
            } else {
                // Image par défaut si le fichier n'existe pas
                imageQRCode.setImageResource(R.drawable.qrcode_bg);
            }
            int largeurEcran = getResources().getDisplayMetrics().widthPixels;

            // Déplacer les layouts hors écran avant le début de l'animation
            layoutTexteQR.setTranslationX(-largeurEcran);
            layoutQRCode.setTranslationX(largeurEcran/4);



            // Rendre les layouts visibles juste avant le début de l'animation
            layoutTexteQR.setVisibility(View.VISIBLE);
            layoutQRCode.setVisibility(View.VISIBLE);

            String qrCodeText = teamChatBuddyApplication.getParamFromFile("QR_Text","TeamChatBuddy.properties");
            textViewQRMessage.setText(Html.fromHtml(qrCodeText, Html.FROM_HTML_MODE_LEGACY));


            // Animation pour les faire entrer au centre
            ObjectAnimator animTexte = ObjectAnimator.ofFloat(layoutTexteQR, "translationX", 0);
            ObjectAnimator animQRCode = ObjectAnimator.ofFloat(layoutQRCode, "translationX", 0);
            animTexte.setDuration(1000);
            animQRCode.setDuration(1000);

            // Exécuter les animations en même temps
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animTexte, animQRCode);
            animatorSet.start();


            new Handler().postDelayed(() -> startAnimateArrow(imageArrow), 1000);
            long duration;
            if(qr_displayed && qrTimeRemaining>0){
                duration = qrTimeRemaining;
            }
            else{
                duration = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_Duration","TeamChatBuddy.properties").trim())* 1000L;
            }
            Log.d(TAG, "timerDisplay QRCode duration "+duration);
            if(timerDisplayQRCode != null) timerDisplayQRCode.cancel();
            qr_displayed=true;
            timerDisplayQRCode = new CountDownTimer(duration,1000) {
                @Override
                public void onTick(long l) {
                    qrTimeRemaining=l;
                    if(!teamChatBuddyApplication.getParamFromFile("Displaying_QRCode_period","TeamChatBuddy.properties").trim().equalsIgnoreCase("0")){
                        teamChatBuddyApplication.notifyObservers("hideCameraQr");
                        //Log.d("MainFragment", "------------ hideCameraQr displayQRCode -------------");
                    }
                    Log.d(TAG, "timerDisplay QRCode onTick");
                }
                @Override
                public void onFinish() {
                    Log.d(TAG, "timerDisplay QRCode onFinish");
                    if (!shouldHideCameraPreview()) {
                        teamChatBuddyApplication.notifyObservers("showCameraQr");
                    }

                    qrTimeRemaining=0;
                    qr_displayed=false;
                    mlKitIsDownloading=false;
                    stopAnimateArrow();

                    // Animation pour les faire entrer au centre
                    ObjectAnimator animTexte = ObjectAnimator.ofFloat(layoutTexteQR, "translationX", -largeurEcran);
                    ObjectAnimator animQRCode = ObjectAnimator.ofFloat(layoutQRCode, "translationX", largeurEcran/4);
                    animTexte.setDuration(1000);
                    animQRCode.setDuration(1000);

                    // Exécuter les animations en même temps
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(animTexte, animQRCode);
                    animatorSet.start();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            layoutTexteQR.setVisibility(View.INVISIBLE);
                            layoutQRCode.setVisibility(View.INVISIBLE);

                            teamChatBuddyApplication.setShouldDisplayQRCode(false);
                            if (timerPeriodToDisplayQRCode!=null) timerPeriodToDisplayQRCode.start();
                            iDisplayQrCodeCallback.onEnd();
                        }
                    },1000);
                }
            };
            timerDisplayQRCode.start();

        }
        else {
            iDisplayQrCodeCallback.onEnd();
            teamChatBuddyApplication.setShouldDisplayQRCode(false);
            if (!shouldHideCameraPreview()) {
                teamChatBuddyApplication.notifyObservers("showCameraQr");
            }
            //Log.d("MainFragment", "------------ showCameraQr displayQRCode -------------");
        }
    }

    public boolean qRCodeExist() {
        File dossier = new File("/storage/emulated/0/TeamChatBuddy");
        if (!dossier.exists() || !dossier.isDirectory()) {
            return false; // Le répertoire n'existe pas
        }

        String qrCodeFileName = teamChatBuddyApplication.getParamFromFile("QRcodeFileName","TeamChatBuddy.properties");
        if(qrCodeFileName!=null && !qrCodeFileName.isEmpty()) {
            // Lister les fichiers du dossier
            String[] fichiers = dossier.list();
            if (fichiers != null) {
                for (String fichier : fichiers) {
                    if (fichier.equalsIgnoreCase(qrCodeFileName.trim())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Méthode pour trouver un fichier en ignorant la casse
    private File findFileIgnoreCase() {
        File dossier = new File("/storage/emulated/0/TeamChatBuddy");
        if (!dossier.exists() || !dossier.isDirectory()) {
            return null;
        }
        String qrCodeFileName = teamChatBuddyApplication.getParamFromFile("QRcodeFileName","TeamChatBuddy.properties");
        if(qrCodeFileName!=null && !qrCodeFileName.isEmpty()) {
            File[] fichiers = dossier.listFiles();
            if (fichiers != null) {
                for (File fichier : fichiers) {
                    if (fichier.getName().equalsIgnoreCase(qrCodeFileName.trim())) {
                        return fichier;
                    }
                }
            }
        }
        return null;
    }
    private void startAnimateArrow(ImageView imageArrow) {
        // Réinitialiser la position à zéro avant de relancer l'animation
        imageArrow.setTranslationX(0f);
        // Animation de translation X (déplacement de la flèche)
        animator = ObjectAnimator.ofFloat(imageArrow, "translationX", 0f, 70f);
        animator.setDuration(1000); // Durée de 1 seconde
        animator.setRepeatCount(ValueAnimator.INFINITE); // Répéter indéfiniment
        animator.setRepeatMode(ValueAnimator.REVERSE); // Revenir en arrière
        animator.start();
    }
    private void stopAnimateArrow(){
        if (animator != null && animator.isRunning()) {
            animator.cancel(); // Arrête immédiatement l'animation
        }
    }

}
