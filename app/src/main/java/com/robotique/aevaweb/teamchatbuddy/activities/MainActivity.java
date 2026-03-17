package com.robotique.aevaweb.teamchatbuddy.activities;

import static com.bfr.buddysdk.BuddySDK.USB;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddy.vision.shared.IVisionRsp;
import com.bfr.buddysdk.BuddyCompatActivity;
import com.bfr.buddysdk.BuddySDK;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.ChatGptStreamMode;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.CustomGPTStreamMode;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.ResponseFromChatbot;
import com.robotique.aevaweb.teamchatbuddy.fragments.BlueMicFragment;
import com.robotique.aevaweb.teamchatbuddy.fragments.ChatWindowFragment;
import com.robotique.aevaweb.teamchatbuddy.fragments.MainFragment;
import com.robotique.aevaweb.teamchatbuddy.fragments.OpenAiFragment;
import com.robotique.aevaweb.teamchatbuddy.fragments.SettingsFragment;
import com.robotique.aevaweb.teamchatbuddy.observers.IDBObserver;
import com.robotique.aevaweb.teamchatbuddy.utilis.AlertManager;
import com.robotique.aevaweb.teamchatbuddy.utilis.WifiBroadcastReceiver;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.io.IOException;


public class MainActivity extends BuddyCompatActivity implements IDBObserver {

    private static final String TAG = "MainActivity";

    private static final String[] REQUESTED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_NUMBERS

    };
    private static final int PERMISSION_REQ_ID = 22;
    private String currentFragment = "MainFragment"; // fragment par défaut

    private AudioManager amanager;
    private WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();

    private TeamChatBuddyApplication teamChatBuddyApplication;

    private Boolean initHasBeenLaunchedFromOnResume = true;
    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    public static boolean isFirstLaunch = true;
    //views
    private View decorView;
    private RelativeLayout view_face;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "OnCreate MainActivity");
        initHasBeenLaunchedFromOnResume = false;
        teamChatBuddyApplication = (TeamChatBuddyApplication) getApplicationContext();
        teamChatBuddyApplication.setInitSharedpreferences(true);
        teamChatBuddyApplication.hideSystemUI(this);
        decorView=getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if(visibility==0){
                decorView.setSystemUiVisibility(teamChatBuddyApplication.hideSystemUI(MainActivity.this));
            }
        });

        view_face = findViewById(R.id.view_face);

        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        isFirstLaunch = true;

        Log.i("MYA_fragment", "onCreate currentFragment: "+currentFragment);
    }

    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        currentFragment = fragment.getClass().getSimpleName();
        Log.i("MYA_fragment", "onCreate navigateTo: "+currentFragment);
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG," --- onResume() ---");
        teamChatBuddyApplication.hideSystemUI(this);
        teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        if (initHasBeenLaunchedFromOnResume){
            if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[3], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[4], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[5], PERMISSION_REQ_ID)

            ){
                runOnUiThread(() -> {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                    if (current instanceof SettingsFragment) {
                        Log.i(TAG, "replace SettingsFragment");
                        return;
                    }
                    if (current instanceof ChatWindowFragment) {
                        Log.i(TAG, "replace ChatWindowFragment");
                        return;
                    }
                    if (current instanceof OpenAiFragment) {
                        Log.i(TAG, "replace OpenAiFragment");
                        return;
                    }
                    if (current instanceof BlueMicFragment) {
                        Log.i(TAG, "replace BlueMicFragment");
                        return;
                    }
                    if (current instanceof MainFragment) {
                        Log.i(TAG, "replace MainFragment");
                        return;
                    }
                    if (!isFinishing() && !getSupportFragmentManager().isStateSaved()) {
                        Log.i(TAG, "launch MainFragment");
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new MainFragment(this))
                                .commit();
                    } else {
                        Log.i(TAG, "launch MainFragment");
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new MainFragment(this))
                                .commitAllowingStateLoss();
                    }
                });
            }
        }
        try {
            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("wasAlterActivated"))&&teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")){
                Log.w("MARIA", "actionType as touch : touch [source: fermeture BuddyCore]");
                AlertManager.getInstance(MainActivity.this).resume();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du resume de AlertManager: " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        teamChatBuddyApplication.isOnApp = false;
        Log.d("MYA_Fragment"," --- onPause() ---");
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
        if(teamChatBuddyApplication.getChatGptStreamMode() != null){
            teamChatBuddyApplication.getChatGptStreamMode().reset();
        }
        if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
            teamChatBuddyApplication.getCustomGPTStreamMode().reset();
        }
        onSdkReadyIsAlreadyCalledOnce = false;
        teamChatBuddyApplication.removeObserver(this);
        teamChatBuddyApplication.stopTTS();
        teamChatBuddyApplication.setActivityClosed(true);
        teamChatBuddyApplication.setStartRecording(false);
        teamChatBuddyApplication.setSpeaking(false);
        teamChatBuddyApplication.setShouldLaunchListeningAfterGetingHotWord(true);
        teamChatBuddyApplication.setModeContinuousListeningON(false);
        isFirstLaunch= false;
        if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")){
            try {
                Log.w("MARIA", "actionType as touch : touch [source: ouverture BuddyCore]");
                AlertManager.getInstance(MainActivity.this).incremente("touch", MainActivity.this);
                AlertManager.getInstance(MainActivity.this).pause();
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du pause de AlertManager: " + e.getMessage());
            }
        }
        else{
            teamChatBuddyApplication.setparam("wasAlterActivated", String.valueOf(false));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG," --- onDestroy() mainActivity---");
        try {
            unregisterReceiver(wifiBroadCastReceiver);
        }catch(IllegalArgumentException e) {
            Log.i(TAG,"---unregisterReceiver wifiBroadcast:: IllegalArgumentException---"+e.getMessage());
        }
        teamChatBuddyApplication.setparam("firstLaunch","true");
        teamChatBuddyApplication.setFileCreate(true);
        teamChatBuddyApplication.notifyObservers("main destroy");
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
            try {
                BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);
                BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
            }
            catch (Exception e){
                Log.e(TAG,"BuddySDK Exception  "+e);
            }
        }
        teamChatBuddyApplication.setparam("remainingTime", "0");
        teamChatBuddyApplication.setparam("touch", "0");
        teamChatBuddyApplication.setparam("hotword", "0");
        teamChatBuddyApplication.setparam("tracking", "0");
    }

    /**
     * ------------------ Register to the SDK callbacks ---------------------
     */

    public void onSDKReady() {
        Log.w(TAG, "onSDKReady");
        initHasBeenLaunchedFromOnResume=false;
        if(!onSdkReadyIsAlreadyCalledOnce){
            try {
                //initialisation du visage de Buddy
                BuddySDK.UI.setFaceEnergy(1.0f);
                BuddySDK.UI.setFacePositivity(1.0f);
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                BuddySDK.UI.lookAt(GazePosition.CENTER, true);
                BuddySDK.UI.stopListenAnimation();
                BuddySDK.UI.setViewAsFace(view_face);
                BuddySDK.UI.setMenuWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
                BuddySDK.UI.setCloseWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
            }
            catch (Exception e){
                Log.e(TAG,"BuddySDK Exception  "+e);
            }

            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))){
                try {
                    USB.enableYesMove( true, iUsbCommadRspTracking);
                    USB.enableNoMove(true, iUsbCommadRspTracking);
                }
                catch (Exception e){
                    Log.e(TAG,"BuddySDK Exception  "+e);
                }
            }

            try {
                //Désactiver le trigger Companion de la bouche et de OK BUDDY
                BuddySDK.Companion.raiseEvent("disableOkBuddy");
                BuddySDK.Companion.raiseEvent("disableOnMouth");
                BuddySDK.Vision.stopCamera(0, new IVisionRsp.Stub() {
                    @Override
                    public void onSuccess(String s) throws RemoteException {
                        Log.i(TAG, "stopCamera(0) onSuccess : " + s);
                    }
                    @Override
                    public void onFailed(String s) throws RemoteException {
                        Log.e(TAG, "stopCamera(0) onFailed : " + s);
                    }
                });
            }
            catch (Exception e){
                Log.e(TAG,"BuddySDK Exception  "+e);
            }

            if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[3], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[4], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[5], PERMISSION_REQ_ID)

            ){
                runOnUiThread(() -> {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                    if (current instanceof SettingsFragment) {
                        Log.i(TAG, "update: properties file done but SettingsFragment is active -> skip replace");
                        return;
                    }
                    if (current instanceof ChatWindowFragment) {
                        Log.i(TAG, "update: properties file done but ChatFragment is active -> skip replace");
                        return;
                    }
                    if (current instanceof OpenAiFragment) {
                        Log.i(TAG, "update: properties file done but ChatFragment is active -> skip replace");
                        return;
                    }
                    if (current instanceof BlueMicFragment) {
                        Log.i(TAG, "update: properties file done but ChatFragment is active -> skip replace");
                        return;
                    }
                    if (current instanceof MainFragment) {
                        Log.i(TAG, "update: properties file done but MainFragment is active -> skip replace");
                        return;
                    }
                    if (!isFinishing() && !getSupportFragmentManager().isStateSaved()) {
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new MainFragment(this))
                                .commit();
                    } else {
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new MainFragment(this))
                                .commitAllowingStateLoss();
                    }
                });
            }
        }
        onSdkReadyIsAlreadyCalledOnce = true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            // --- ton code existant ---
            if (teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")) {
                Log.w("MARIA", "actionType as touch : touch [source: écran]");
                AlertManager.getInstance(this).incremente("touch", this);
            }


            // --- Vérification par fragment ---
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null && currentFragment.isVisible()) {
                if (currentFragment instanceof ChatWindowFragment) {
                    Log.d("AlertManager", "Touch détecté dans ChatWindowFragment");
                    // ton code pour ProfileFragment ici
                }
                else if (currentFragment instanceof SettingsFragment) {
                    Log.d("AlertManager", "Touch détecté dans SettingsFragment");
                    // ton code pour SettingsFragment ici
                }
            }
        }

        return super.dispatchTouchEvent(event);
    }



    private final IUsbCommadRsp iUsbCommadRspTracking = new IUsbCommadRsp.Stub(){
        @Override
        public void onSuccess(String s) throws RemoteException {}
        @Override
        public void onFailed(String s) throws RemoteException {}
    };


    /**
     *   -------------------------------  Gestion des permissions  ---------------------------------------------------------------
     */

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private boolean checkPermission(@NonNull int[] grantResults){
        return grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                grantResults[2] != PackageManager.PERMISSION_GRANTED ||
                grantResults[3] != PackageManager.PERMISSION_GRANTED;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermission(grantResults)) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Need permissions " + Manifest.permission.READ_EXTERNAL_STORAGE +
                                    "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE +
                                    "/" + Manifest.permission.RECORD_AUDIO +
                                    "/" + Manifest.permission.CAMERA
                            , Toast.LENGTH_LONG).show();
                }
            });
            /*
             * Terminer l'activité si l'utilisateur n'a pas activé une autorisation
             */
            finish();
            return;
        }
        isFirstLaunch = true;
    }

    /**
     *   -------------------------------  Gestion d'affichage des barres du systemUI  ----------------------------------------------
     */

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            teamChatBuddyApplication.hideSystemUI(this);
        }
    }

    @Override
    public void update(String message) throws IOException {
        if (message != null && message.contains("properties file done")) {
            Log.i(TAG, "update: properties file done mainActivity");

//            runOnUiThread(() -> getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container, new MainFragment())
//                    .commitAllowingStateLoss());
        }

    }
}