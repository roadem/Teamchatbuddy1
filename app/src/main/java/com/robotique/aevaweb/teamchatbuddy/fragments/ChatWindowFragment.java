package com.robotique.aevaweb.teamchatbuddy.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.activities.MainActivity;
import com.robotique.aevaweb.teamchatbuddy.adapters.ReplicaListAdapter;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.ChatGptStreamMode;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.CustomGPTStreamMode;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.ResponseFromChatbot;
import com.robotique.aevaweb.teamchatbuddy.models.Langue;
import com.robotique.aevaweb.teamchatbuddy.models.Replica;
import com.robotique.aevaweb.teamchatbuddy.models.Session;
import com.robotique.aevaweb.teamchatbuddy.models.Setting;
import com.robotique.aevaweb.teamchatbuddy.observers.IDBObserver;
import com.robotique.aevaweb.teamchatbuddy.utilis.AlertManager;
import com.robotique.aevaweb.teamchatbuddy.utilis.ITTSCallbacks;
import com.robotique.aevaweb.teamchatbuddy.utilis.MailSender;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatWindowFragment extends Fragment implements IDBObserver {

    private static final String TAG = "ChatWindowFragment";

    private TeamChatBuddyApplication teamChatBuddyApplication;

    private MainActivity _parentActivity;

    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private boolean isListeningFreeSpeech = false;
    private boolean isWaitingForResponse = false;


    private Setting settingClass;
    private ArrayList<Replica> listRep=new ArrayList();
    private ArrayList<Replica> listRepGlobale=new ArrayList();
    private ReplicaListAdapter adapter;

    //timers
    private CountDownTimer timerEcoute;

    //views
    private RelativeLayout popupAddMail;
    private LinearLayout popupAddMailContent;
    private RelativeLayout parent_chat;
    private RelativeLayout lyt_close_menu_chat;
    private ImageView micro_btn;
    private ImageView send_btn;
    private ImageView clear_btn;
    private ImageView send_btn2;
    private RecyclerView recyclerView;
    private ScrollView scrollView;
    private TextView textEmail;
    private EditText editTextEmail;
    int click=1;
    boolean startlisten=true;
    MailSender smtpService;
    private ResponseFromChatbot responseFromChatbot;
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String langueEs = "Espagnol";
    private String langueDe = "Allemand";
    private Handler handlerTTSError = new Handler();
    private Runnable runnableTTSError;
    private String configFile ="TeamChatBuddy.properties";
    private String header ="header";
    private String entete ="entete";
    private String openAIKey = "openAI_API_Key";
    private String addDestinationMail="";
    private String addDestinationMailEditText="";
    private Handler handlerPauseTime = new Handler();
    private Runnable runnablePauseTime;
    private Handler handler= new Handler();
    private Runnable runnable;

    public ChatWindowFragment(MainActivity _parentActivity) {
        this._parentActivity = _parentActivity;
    }

    public ChatWindowFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat_window, container, false);
        Log.d(TAG," --- onCreate() chatWindow---");

        teamChatBuddyApplication = (TeamChatBuddyApplication) requireActivity().getApplicationContext();
        teamChatBuddyApplication.setInitSharedpreferences(false);
        teamChatBuddyApplication.hideSystemUI(requireActivity());

        responseFromChatbot=new ResponseFromChatbot(teamChatBuddyApplication,getActivity());
        //init views
        popupAddMail = view.findViewById(R.id.popup_add_mail);
        parent_chat = view.findViewById( R.id.parent_chat );
        micro_btn = view.findViewById( R.id.micro_btn );
        scrollView= view.findViewById(R.id.scrollview);
        recyclerView= view.findViewById(R.id.chatRecyclerView);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        textEmail = view.findViewById(R.id.popup_add_mail_textView);
        popupAddMailContent = view.findViewById(R.id.popup_add_mail_linearLayout);
        lyt_close_menu_chat = view.findViewById(R.id.lyt_close_menu_chat);
        send_btn = view.findViewById(R.id.send_btn);
        clear_btn = view.findViewById(R.id.clear_btn);
        send_btn2 = view.findViewById(R.id.send_btn2);

        teamChatBuddyApplication.isOnApp = true;

        lyt_close_menu_chat.setOnClickListener(view1 -> btnCloseChat(view1));
        micro_btn.setOnClickListener(view2 -> onClickMicro(view2));
        send_btn.setOnClickListener(view3 -> onClickSend(view3));
        clear_btn.setOnClickListener(view4 -> onClickClearConversation(view4));
        send_btn2.setOnClickListener(view5 -> onClickSendFromPopup(view5));

        teamChatBuddyApplication.setparam("listening_mode","");
        teamChatBuddyApplication.setparam("remaining_attempts","");

        setAddMailDestinationText();
        init();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG," --- onResume() ChatWindowFragment---");
        teamChatBuddyApplication.registerObserver(this);
        teamChatBuddyApplication.hideSystemUI(requireActivity());

        Log.i(TAG, "MainActivity.isFirstLaunch : "+MainActivity.isFirstLaunch);
        if(!MainActivity.isFirstLaunch){
            String listeningMode = teamChatBuddyApplication.getparam("listening_mode");
            Log.i(TAG, "listening_mode : "+listeningMode);
            if (listeningMode.equals("hotword")) teamChatBuddyApplication.startListeningHotwor(_parentActivity);
            else if (listeningMode.equals("listening")) startNextCycle();
            String remaining_attempts = teamChatBuddyApplication.getparam("remaining_attempts");
            if(remaining_attempts!=null && !remaining_attempts.isEmpty()){
                teamChatBuddyApplication.setRemainingAttempts(Integer.parseInt(remaining_attempts));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        teamChatBuddyApplication.isOnApp = false;
        Log.i(TAG," onPause ChatWindowFragment ");
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
        onSdkReadyIsAlreadyCalledOnce = false;
        isWaitingForResponse = false;
        startlisten=true;
        listRep=new ArrayList();
        listRepGlobale=new ArrayList();
        teamChatBuddyApplication.stopTTS();
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        }
        catch (Exception e){
            Log.e(TAG,"BuddySDK Exception  "+e);
        }
        stopListeningFreeSpeech();
        if(teamChatBuddyApplication.getAppIsListeningToTheQuestion()){
            Log.e(TAG,"getAppIsListeningToTheQuestion()  "+teamChatBuddyApplication.getAppIsListeningToTheQuestion());
            teamChatBuddyApplication.setparam("listening_mode","listening");
        } else if(teamChatBuddyApplication.getSpeaking()){
            Log.e(TAG,"teamChatBuddyApplication.getSpeaking()  "+teamChatBuddyApplication.getSpeaking());
            teamChatBuddyApplication.setparam("listening_mode","speaking");
        } else {
            Log.e(TAG,"teamChatBuddyApplication.isListeningHotw  "+teamChatBuddyApplication.isListeningHotw);
            teamChatBuddyApplication.setparam("listening_mode","hotword");
        }

        teamChatBuddyApplication.setparam("remaining_attempts",String.valueOf(teamChatBuddyApplication.getRemainingAttempts()));
        teamChatBuddyApplication.removeObserver(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!teamChatBuddyApplication.getInitSharedpreferences()){
            teamChatBuddyApplication.setparam("firstLaunch","true");
            teamChatBuddyApplication.notifyObservers("ChatDestroy");
        }
        Log.d(TAG," --- onDestroy() chatWindow---");
        if(!teamChatBuddyApplication.isOnApp && teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")){
            AlertManager.getInstance(_parentActivity).stop();
        }
    }
    /**
     * ------------------ Register to the SDK callbacks deleted onSDKReady & onEvent ---------------------
     */

    /**
     * Initialisations
     */
    private void init(){

        click=1;
        settingClass = new Setting();
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
        settingClass.setLangue(teamChatBuddyApplication.getLangue().getNom());
        settingClass.setVolume(teamChatBuddyApplication.getparam("speak_volume"));
        if(teamChatBuddyApplication.getparam("switch_visibility").contains("yes")){
            settingClass.setSwitchVisibility("true");
        }
        else{
            settingClass.setSwitchVisibility("false");
        }
        refreshSTTLangue();

        popupAddMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Vérifier si le popup_add_mail est visible et si le clic est en dehors de celui-ci
                if (popupAddMail.getVisibility() == View.VISIBLE) {
                    MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                    if (!isViewInsideBounds(popupAddMailContent, (int) event.getRawX(), (int) event.getRawY())) {
                        // Si le clic est en dehors, rendre le popup invisible
                        popupAddMail.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        popupAddMailContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ne rien faire pour empêcher la propagation du clic aux éléments enfants du popup
            }
        });

        adapter = new ReplicaListAdapter(teamChatBuddyApplication,initDataset());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        editTextEmail.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);


        editTextEmail.setText(teamChatBuddyApplication.getparam("Mail_Destination"));


        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {


                teamChatBuddyApplication.setparam("Mail_Destination",charSequence.toString());
            }
            @Override
            public void afterTextChanged(Editable editable) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
        });

        editTextEmail.setOnFocusChangeListener((v,hasFocus) -> {
            if (hasFocus) {
                View decorView = requireActivity().getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
                // color black + opacity 50%
            } else {
                teamChatBuddyApplication.hideSystemUI(requireActivity());
            }
        });

        scroll();

    }
    // Vérifie si les coordonnées de l'événement sont à l'intérieur de la vue spécifiée
    private boolean isViewInsideBounds(View view, int x, int y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        return !(x < viewX || x > viewX + view.getWidth() || y < viewY || y > viewY + view.getHeight());
    }
    private void refreshSTTLangue() {
        List<String> STTAndroidLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_STT_Android");
        String codeLanguageSTTAndroid = STTAndroidLangueCode.get(new Gson().fromJson(teamChatBuddyApplication.getparam(settingClass.getLangue()), Langue.class).getId()-1);
        teamChatBuddyApplication.refresh(codeLanguageSTTAndroid,getActivity());
    }
    /**
     * Récupération des questions/réponses
     */
    private Replica[] initDataset() {

        int size=0;
        ArrayList<Session> ss = teamChatBuddyApplication.getListSession();

        Log.i("SessionManager", "listSession restaurée avec " + ss.size() + " sessions");
        for (int i = 0; i < ss.size(); i++) {

            ArrayList<Replica> s = ss.get(i).getSession();
            size=size+s.size();

        }
        for (int i = 0; i < ss.size(); i++) {

            ArrayList<Replica> s = ss.get(i).getSession();

            for (int j = 0; j < s.size(); j++){

                listRepGlobale.add(s.get(j));
            }
        }
        if (teamChatBuddyApplication.getParamFromFile("Maximum_Dialogs_in_Chat_Window","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("Maximum_Dialogs_in_Chat_Window","TeamChatBuddy.properties").isEmpty()
                && Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Maximum_Dialogs_in_Chat_Window","TeamChatBuddy.properties"))!=0){
            while (listRepGlobale.size() > Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Maximum_Dialogs_in_Chat_Window","TeamChatBuddy.properties"))*2) {
                listRepGlobale.remove(0); // Supprime le premier élément
            }
        }

        Replica[] mDataset = new Replica[listRepGlobale.size()];
        for(int i=0; i<listRepGlobale.size(); i++){
            mDataset[i] = listRepGlobale.get(i);
        }

        return mDataset;
    }

    /**
     * Mettre à jour la liste des messages
     */
    private void updateChat(){
        if (teamChatBuddyApplication.getParamFromFile("Maximum_Dialogs_in_Chat_Window","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("Maximum_Dialogs_in_Chat_Window","TeamChatBuddy.properties").isEmpty()
                && Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Maximum_Dialogs_in_Chat_Window","TeamChatBuddy.properties"))!=0){
            while (listRepGlobale.size() > Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Maximum_Dialogs_in_Chat_Window","TeamChatBuddy.properties"))*2) {
                listRepGlobale.remove(0); // Supprime le premier élément
            }
        }

        Replica[] mDataset = new Replica[listRepGlobale.size()];
        for(int i=0; i<listRepGlobale.size(); i++){
            mDataset[i] = listRepGlobale.get(i);
        }
        adapter.setData(mDataset);
        scroll();
    }

    /**
     * Gestion du scroll automatique
     */
    private void scroll(){
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.smoothScrollToPosition(adapter.getItemCount()+5);
            }
        });
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    /**
     * Gestion du clic sur l'icone Micro
     */
    public void onClickMicro(View view) {
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
        if (click==1){
            click=2;
            if(!isListeningFreeSpeech && !isWaitingForResponse){
                teamChatBuddyApplication.stopTTS();
                startlisten=true;
                startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
            }

        }else if (click==2){
            Log.e("MEHDII","teamChatBuddyApplication.getAppIsListeningToTheQuestion()----------------------"+teamChatBuddyApplication.getAppIsListeningToTheQuestion());
            if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence") || !teamChatBuddyApplication.getAppIsListeningToTheQuestion() || teamChatBuddyApplication.getParamFromFile("Processing_the_audio_sequence","TeamChatBuddy.properties").trim().equalsIgnoreCase("No")) {
                click = 1;
                isListeningFreeSpeech = false;
                isWaitingForResponse = false;
                startlisten = false;
                teamChatBuddyApplication.stopTTS();
                teamChatBuddyApplication.setStoredResponse("");
                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                } catch (Exception e) {
                    Log.e(TAG, "BuddySDK Exception  " + e);
                }
                stopListeningFreeSpeech();
            }
            else {
                micro_btn.setImageResource(R.drawable.micro_off);
                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                Log.e("MEHDII","teamChatBuddyApplication.traitementAudio---------------------------------------");
                teamChatBuddyApplication.traitementAudio(false);
            }

        }
    }
    private void setAddMailDestinationText(){
        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
            textEmail.setText(teamChatBuddyApplication.getString(R.string.destination_mail_texte));
            editTextEmail.setHint(teamChatBuddyApplication.getString(R.string.destination_mail_Edittexte));
        }
        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)){
            textEmail.setText(teamChatBuddyApplication.getString(R.string.destination_mail_texte_fr));
            editTextEmail.setHint(teamChatBuddyApplication.getString(R.string.destination_mail_Edittexte_fr));
        }else {
            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.destination_mail_texte)).addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String translatedText) {

                    textEmail.setText(translatedText);
                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG,"translatedText exception  "+e);
                    textEmail.setText(teamChatBuddyApplication.getString(R.string.destination_mail_texte));
                }
            });

            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.destination_mail_Edittexte)).addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String translatedText) {
                    editTextEmail.setHint(translatedText);

                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG,"translatedText exception  "+e);
                    editTextEmail.setHint(teamChatBuddyApplication.getString(R.string.destination_mail_Edittexte));
                }
            });
        }
    }
    /**
     * Gestion du clic sur l'icone ClearCoonversation
     */
    public void onClickClearConversation(View view){
        teamChatBuddyApplication.listSessionClear();
        teamChatBuddyApplication.setparam("listSession", new Gson().toJson(teamChatBuddyApplication.getListSession()));
        listRep.clear();
        listRepGlobale.clear();
        adapter = new ReplicaListAdapter(teamChatBuddyApplication,initDataset());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);



    }
    /**
     * Gestion du clic sur l'icone Send
     */
    public void onClickSend(View view){
        popupAddMail.setVisibility(View.VISIBLE);

    }
    /**
     * Gestion du clic sur l'icone Send depuis le popUP
     */
    public void onClickSendFromPopup(View view){
        if (!teamChatBuddyApplication.getparam("Mail_Destination").trim().isEmpty()){
            if(teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                smtpService = new MailSender(requireActivity(),writeMail(), teamChatBuddyApplication.getparam("Mail_Destination"), teamChatBuddyApplication.getParamFromFile("Mail_Subject_en",configFile));
                smtpService.execute();
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals(langueFr)){
                smtpService = new MailSender(requireActivity(),writeMail(), teamChatBuddyApplication.getparam("Mail_Destination"), teamChatBuddyApplication.getParamFromFile("Mail_Subject_fr",configFile));
                smtpService.execute();
            }
            else{
                final Activity activity = requireActivity();
                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Mail_Subject_en",configFile)).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        smtpService = new MailSender(activity,writeMail(), teamChatBuddyApplication.getparam("Mail_Destination"), translatedText);
                        smtpService.execute();
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG,"translatedText exception  "+e);
                    }
                });
            }
            popupAddMail.setVisibility(View.INVISIBLE);

        }else {
            if(teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                Toast.makeText(teamChatBuddyApplication,teamChatBuddyApplication.getString(R.string.add_mail_toast_en),Toast.LENGTH_LONG).show();
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals(langueFr)){
                Toast.makeText(teamChatBuddyApplication,teamChatBuddyApplication.getString(R.string.add_mail_toast_fr),Toast.LENGTH_LONG).show();
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals(langueEs)){
                Toast.makeText(teamChatBuddyApplication,teamChatBuddyApplication.getString(R.string.add_mail_toast_es),Toast.LENGTH_LONG).show();
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals(langueDe)){
                Toast.makeText(teamChatBuddyApplication,teamChatBuddyApplication.getString(R.string.add_mail_toast_de),Toast.LENGTH_LONG).show();
            }
            else{
                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.add_mail_toast_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        Toast.makeText(teamChatBuddyApplication, translatedText, Toast.LENGTH_LONG).show();
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG,"translatedText exception  "+e);
                    }
                });
            }
        }

    }
    public String writeMail(){
        Replica[] mDataset;
        String question="";
        int x = 1;
        int size=0;
        ArrayList<Session> ss = teamChatBuddyApplication.getListSession();
        int y=0;
        question = "Chatbot : ChatGPT<br>";
        for (int i = 0; i < ss.size(); i++) {

            ArrayList<Replica> s = ss.get(i).getSession();
            size=size+s.size();

        }
        mDataset = new Replica[size];
        for (int i = 0; i < ss.size(); i++) {

            ArrayList<Replica> s = ss.get(i).getSession();

            for (int j = 0; j < s.size(); j++){

                mDataset[y] =  s.get(j);
                //listRepGlobale.add(s.get(j));
                y++;
            }
            x++;
        }
        for (int z=0; z < mDataset.length;z++){
            if(mDataset[z].getType().equals("reponse")){
                question=question+"<br>"+mDataset[z].getType()+" : "+mDataset[z].getValue()+" ["+mDataset[z].getDuration()+"]";
            }
            else{
                question=question+"<br>"+mDataset[z].getType()+" : "+mDataset[z].getValue();
            }
        }

        return question;
    }
    /**
     * Fermeture de la fenetre de discussion
     */
    public void btnCloseChat(View view) {
        if (runnable!=null){
            handler.removeCallbacks(runnable);
            runnable = null;
        }
        runnable= new Runnable() {
            @Override
            public void run() {
                teamChatBuddyApplication.stopTTS();
                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                }
                catch (Exception e){
                    Log.e(TAG,"BuddySDK Exception  "+e);
                }
                //stopListeningFreeSpeech();
                ((MainActivity) requireActivity()).navigateTo(new MainFragment(_parentActivity), true);
            }
        };
        handler.postDelayed(runnable,300);

    }

    /**
     * ------------------------------------------ Gestion de notifications --------------------------
     */

    @Override
    public void update(String message) throws IOException {
        if(message != null){

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

            if(message.contains("MODE_STREAM_SPEAK;SPLIT;")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.split(";SPLIT;").length > 1){
                            String phraseToPronounce = message.split(";SPLIT;")[1];
                            speak(phraseToPronounce, "nothealysa");
                        }
                    }
                });
            }

            if(message.contains("STTQuestion_success")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                        isWaitingForResponse = true;

                        stopListeningFreeSpeech();

                        String detectedSTTMessage = message.split(";")[1].replaceAll("' ","'");


                        teamChatBuddyApplication.setQuestionNumber(teamChatBuddyApplication.getCurrentQuestionNubmer()+1);
                        teamChatBuddyApplication.setQuestionTime(System.currentTimeMillis());
                        String time =new SimpleDateFormat("HH:mm:ss").format(new Date());
                        Replica question=new Replica();
                        question.setType("question");
                        question.setTime(time);
                        question.setValue(detectedSTTMessage);
                        listRep.add(question);
                        listRepGlobale.add(question);
                        updateChat();
                        teamChatBuddyApplication.setLed("thinking");
                        teamChatBuddyApplication.setActivityClosed(false);
                        if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
                            if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                if (teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties").trim())){
                                    responseFromChatbot.getResponseFromChatGPT(detectedSTTMessage+" "+teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties"),teamChatBuddyApplication.getQuestionNumber());
                                }
                                else {
                                    responseFromChatbot.getResponseFromChatGPT(detectedSTTMessage, teamChatBuddyApplication.getQuestionNumber());
                                }
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                if (teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties").trim())){
                                    responseFromChatbot.getResponseFromChatGPT(detectedSTTMessage+" "+teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties"),teamChatBuddyApplication.getQuestionNumber());
                                }
                                else {
                                    responseFromChatbot.getResponseFromChatGPT(detectedSTTMessage, teamChatBuddyApplication.getQuestionNumber());
                                }
                            }
                            else {
                                if (teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties").trim())){
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")).addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {

                                            responseFromChatbot.getResponseFromChatGPT(detectedSTTMessage+" "+translatedText,teamChatBuddyApplication.getQuestionNumber());
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG,"translatedText exception  "+e);
                                        }
                                    });

                                }
                                else {
                                    responseFromChatbot.getResponseFromChatGPT(detectedSTTMessage, teamChatBuddyApplication.getQuestionNumber());
                                }
                            }
                        }
                        else {
                            responseFromChatbot.getSessionId(detectedSTTMessage);
                        }
                        if(teamChatBuddyApplication.isAlertActivated.trim().equalsIgnoreCase("Yes")) {
                            AlertManager.getInstance(_parentActivity).incremente("hotword", _parentActivity);
                        }
                    }
                });
            }


            else if (message.contains("TTS_success")) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isWaitingForResponse = false;
                        if (startlisten==true) {
                            //startListeningFreeSpeech(teamChatBuddyApplication.getListeningAttempt() * 5);
                            teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt()-1);
                            startCycle();
                        }
                        //creationFile.updateFile(teamChatBuddyApplication.getFileupdate(),settingClass, teamChatBuddyApplication);
                    }
                });
            }

            else if (message.contains("TTS_error") || message.contains("TTS_exception")) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        String text = message.split(";")[1];

                        Log.w(TAG,"TTS_ERROR:"+text);

                        teamChatBuddyApplication.tryNextTTSFromList(text, new ITTSCallbacks() {
                            @Override
                            public void onSuccess(String s) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
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
                                            isWaitingForResponse = false;
                                            if (startlisten==true) {
                                                //startListeningFreeSpeech(teamChatBuddyApplication.getListeningAttempt() * 5);
                                                teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt()-1);
                                                startCycle();
                                            }
                                            //creationFile.updateFile(teamChatBuddyApplication.getFileupdate(),settingClass, teamChatBuddyApplication);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError(String s) {
                                int textLength = text.length();// Calculate the length of the pronounced text
                                int delayTime = (textLength / 20) * 1000; // 1 second for every 20 characters
                                String langCode = teamChatBuddyApplication.getCurrentLanguage(); // fr, en, de, etc.
                                String defaultVoice = teamChatBuddyApplication.getReadSpeakerVoiceFromLangCode(langCode);
                                String validatedVoice = "";
                                if(defaultVoice!=null && defaultVoice.isEmpty()){
                                    validatedVoice = teamChatBuddyApplication.checkReadSpeakerVoices(defaultVoice);
                                }
                                if(teamChatBuddyApplication.getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && (validatedVoice!=null || !validatedVoice.isEmpty()) && teamChatBuddyApplication.getUsingReadSpeaker() ){
                                    delayTime = 0;
                                }
                                handlerTTSError.postDelayed(runnableTTSError = new Runnable() {
                                    @Override
                                    public void run() {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
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
                                                    isWaitingForResponse = false;
                                                    if (startlisten==true) {
                                                        //startListeningFreeSpeech(teamChatBuddyApplication.getListeningAttempt() * 5);
                                                        teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt()-1);
                                                        startCycle();
                                                    }
                                                    //creationFile.updateFile(teamChatBuddyApplication.getFileupdate(),settingClass, teamChatBuddyApplication);
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

            else if(message.contains("CHATBOTS_RETURN")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String action = message.split(";SPLIT;")[1];
                        String value =  message.split(";SPLIT;")[2];
                        if(action.equals("translation_question") && !value.equals("OPERATION_FAILED")){
                            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                            Replica question = new Replica();
                            question.setType("question traduction");
                            question.setTime(time);
                            question.setValue(value);
                            listRep.add(question);
                            listRepGlobale.add(question);
                            updateChat();
                        }
                        else if(action.equals("speak")){
                            if (message.split(";SPLIT;").length>3) {
                                int numberOfQuestion = Integer.parseInt(message.split(";SPLIT;")[3]);

                                if (numberOfQuestion == teamChatBuddyApplication.getQuestionNumber()) {
                                    if (!teamChatBuddyApplication.isTimeoutExpired()) {
                                        speak(value, "nothealysa");
                                    } else {
                                        teamChatBuddyApplication.setStoredResponse(value);
                                    }
                                }
                            }
                        }
                    }
                });
            }


            else if (message.contains("conversationFinished google assistant responce")){

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isWaitingForResponse = false;
                        //startListeningFreeSpeech(teamChatBuddyApplication.getListeningAttempt()*5);
                        teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt()-1);
                        startCycle();
                        //creationFile.updateFile(teamChatBuddyApplication.getFileupdate(),settingClass, teamChatBuddyApplication);
                    }
                });
            }
            else if(message.contains("main destroy")){
                teamChatBuddyApplication.setFileCreate(false);
                teamChatBuddyApplication.setparam("firstLaunch","false");
            }
            else if (message.contains("getResponseF;SPLIT;chatbot;SPLIT;response google complete")){
                teamChatBuddyApplication.notifyObservers("play google response");
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
            else if (message.contains("mailSend")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                            if (!teamChatBuddyApplication.getParamFromFile("Message_mail_send_en",configFile).trim().equals("")){
                                Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getParamFromFile("Message_mail_send_en",configFile),Toast.LENGTH_LONG).show();
                            }

                        }
                        else if(teamChatBuddyApplication.getLangue().getNom().equals(langueFr)){
                            if (!teamChatBuddyApplication.getParamFromFile("Message_mail_send_fr",configFile).trim().equals("")) {
                                Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getParamFromFile("Message_mail_send_fr", configFile), Toast.LENGTH_LONG).show();
                            }
                        }

                        else{
                            if (!teamChatBuddyApplication.getParamFromFile("Message_mail_send_en",configFile).trim().equals("")) {
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Message_mail_send_en", configFile)).addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        Toast.makeText(teamChatBuddyApplication, translatedText, Toast.LENGTH_LONG).show();
                                    }

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "translatedText exception  " + e);
                                    }
                                });
                            }
                        }
                    }
                });
            }
            else if (message.contains("ErrorSending")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                            Toast.makeText(teamChatBuddyApplication,teamChatBuddyApplication.getString(R.string.error_mail_toast_en),Toast.LENGTH_LONG).show();
                        }
                        else if(teamChatBuddyApplication.getLangue().getNom().equals(langueFr)){
                            Toast.makeText(teamChatBuddyApplication,teamChatBuddyApplication.getString(R.string.error_mail_toast_fr),Toast.LENGTH_LONG).show();
                        }
                        else if(teamChatBuddyApplication.getLangue().getNom().equals(langueEs)){
                            Toast.makeText(teamChatBuddyApplication,teamChatBuddyApplication.getString(R.string.error_mail_toast_es),Toast.LENGTH_LONG).show();
                        }
                        else if(teamChatBuddyApplication.getLangue().getNom().equals(langueDe)){
                            Toast.makeText(teamChatBuddyApplication,teamChatBuddyApplication.getString(R.string.error_mail_toast_de),Toast.LENGTH_LONG).show();
                        }
                        else{
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.error_mail_toast_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    Toast.makeText(teamChatBuddyApplication, translatedText, Toast.LENGTH_LONG).show();
                                }

                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG,"translatedText exception  "+e);
                                }
                            });
                        }
                    }
                });

            }
            else if (message.contains("changeDetected")){
                int speakVolume = teamChatBuddyApplication.getVolume();
                int max = teamChatBuddyApplication.getMaxVolume();
                int defaultVolume = teamChatBuddyApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e("FCH","volumeMedia  "+String.valueOf(defaultVolume));
                teamChatBuddyApplication.setparam("speak_volume", String.valueOf(defaultVolume));
            }
            else if (message.contains("restartListeningHotword")){
                click=1;
                isListeningFreeSpeech=false;
                isWaitingForResponse=false;
                startlisten=false;
                teamChatBuddyApplication.stopTTS();
                teamChatBuddyApplication.setStoredResponse("");
                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                }
                catch (Exception e){
                    Log.e(TAG,"BuddySDK Exception  "+e);
                }
                stopListeningFreeSpeech();
            }
            else if (message.contains("end of cycle")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        micro_btn.setImageResource(R.drawable.micro_off);
                    }
                });

            }
            else if (message.contains("restartNewCycle")){
                runnablePauseTime =new Runnable() {
                    @Override
                    public void run() {
                        // startCycle(settingClass, listRep, nameActivity, adapter, cancelTheTimer);
                        startNextCycle();
                        Log.e("ARR","startNextCycle  after handler ");
                    }
                };
                handlerPauseTime.postDelayed(runnablePauseTime,1000);

            }
            else if (message.contains("Obtain audio transcription after the listening time has elapsed")){
                String shouldRestartNewCycle = message.split(";SPLIT;")[1];
                Log.e("ARR","Obtain audio transcription after the listening time has elapsed "+shouldRestartNewCycle);
                micro_btn.setImageResource(R.drawable.micro_off);
                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                if (shouldRestartNewCycle.equals("true")) {
                    teamChatBuddyApplication.traitementAudio(true);
                }else {
                    teamChatBuddyApplication.traitementAudio(false);
                }

            }
            else if (message.equals("SpeechRecognizerTimeout")){
                stopListeningFreeSpeech();
                click = 1;
            }
            else if (message.equals("SpeechRecognizerAttemptTimeout")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        micro_btn.setImageResource(R.drawable.micro_off);
                    }
                });
                runnablePauseTime = new Runnable() {
                    @Override
                    public void run() {
                        // startCycle(settingClass, listRep, nameActivity, adapter, cancelTheTimer);
                        startNextCycle();
                        Log.e("ARR", "startNextCycle  after handler ");
                    }
                };
                handlerPauseTime.postDelayed(runnablePauseTime, 1000);
            }
        }
    }
    /**
     * ------------------------------------------ STT  -------------------------------------------
     */

    private void startListeningFreeSpeech(int duration) {
        Boolean notUsingSpeechRecognizer=true;
        isListeningFreeSpeech = true;
        teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
        teamChatBuddyApplication.setAppIsListeningToTheQuestion(true);

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
            }else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Whisper")) {
                //startWhisperSTT(settingClass, listRep, nameActivity, adapter);
                teamChatBuddyApplication.startWhisperRecording(getActivity());
            }else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")){
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
            if (timerEcoute != null) timerEcoute.cancel();
            timerEcoute = new CountDownTimer(duration * 1000L, 1000) {
                @Override
                public void onTick(long l) {
                    Log.d(TAG, "timerEcoute onTick");
                }

                @Override
                public void onFinish() {

                    if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")) {
                        Log.i(TAG, "timerEcoute onFinish");
                        stopListeningFreeSpeech();
                        click = 1;
                    } else {
                        teamChatBuddyApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;false");
                    }
                }
            };
            timerEcoute.start();
        }

        micro_btn.setImageResource(R.drawable.micro_on);

    }
    private void startCycle(){
        Boolean notUsingSpeechRecognizer=true;
        isListeningFreeSpeech = true;
        teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
        teamChatBuddyApplication.setAppIsListeningToTheQuestion(true);


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
            }else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Whisper")) {
                //startWhisperSTT(settingClass, listRep, nameActivity, adapter);
                teamChatBuddyApplication.startWhisperRecording(getActivity());
            }else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")){
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
        if (notUsingSpeechRecognizer) {
            if (timerEcoute != null) timerEcoute.cancel();
            timerEcoute = new CountDownTimer(teamChatBuddyApplication.getListeningDuration() * 1000L, 1000) {
                @Override
                public void onTick(long l) {
                    Log.d(TAG, "timerEcoute onTick");
                }

                @Override
                public void onFinish() {
                    Log.i(TAG, "timerEcoute onFinish");
                    if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")) {
                        teamChatBuddyApplication.notifyObservers("end of cycle");
                        runnablePauseTime = new Runnable() {
                            @Override
                            public void run() {
                                // startCycle(settingClass, listRep, nameActivity, adapter, cancelTheTimer);
                                startNextCycle();
                                Log.e("ARR", "startNextCycle  after handler ");
                            }
                        };
                        handlerPauseTime.postDelayed(runnablePauseTime, 1000);
                    } else {
                        teamChatBuddyApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;true");
                    }
                }
            };
            timerEcoute.start();
        }

        micro_btn.setImageResource(R.drawable.micro_on);
    }
    public void startNextCycle() {
        // Si nous avons encore des tentatives restantes
        Log.e("ARR","startNextCycle  remainingattempts= "+teamChatBuddyApplication.getRemainingAttempts());
        if (teamChatBuddyApplication.getRemainingAttempts() > 0) {
            teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getRemainingAttempts()-1);
            startCycle();

            Log.e("ARR","startNextCycle  after handler ");
        } else {
            stopListeningFreeSpeech();
            click=1;
            // Si toutes les tentatives ont été épuisées, vous pouvez faire quelque chose ici si nécessaire
        }
    }
    private void stopListeningFreeSpeech() {

        isListeningFreeSpeech = false;

        Log.d(TAG," --- stopListeningFreeSpeech() ---");

        if (timerEcoute!=null) timerEcoute.cancel();
        teamChatBuddyApplication.stopListening(requireActivity());
        micro_btn.setImageResource(R.drawable.micro_off);
    }

    public void onUserTouch(MotionEvent event) {
        Log.d(TAG, "AlertManager: Touch détecté dans ChatFragment");
    }

    /**
     * ------------------------------------------ TTS  -------------------------------------------
     */

    private void speak(final String texte,String type) {
        Log.d(TAG," --- speak("+texte+") ---");
        if(ResponseFromChatbot.responseTimeout !=null){
            ResponseFromChatbot.responseTimeout.cancel();
        }
        if(ChatGptStreamMode.responseTimeout !=null){
            ChatGptStreamMode.responseTimeout.cancel();
        }
        if(CustomGPTStreamMode.responseTimeout !=null){
            CustomGPTStreamMode.responseTimeout.cancel();
        }

        if (type.equals("nothealysa") || type.equals("storedResponse")) {

            teamChatBuddyApplication.setAlreadyGetAnswer(true);
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            if (teamChatBuddyApplication.getparam("Mode_Stream").contains("no") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")){
                Replica reponse = new Replica();
                reponse.setValue(texte);
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
                ArrayList<Replica> ll = new ArrayList<>();
                for (int t = 0; t < listRep.size(); t++) {
                    ll.add(listRep.get(t));
                }
                Session session = new Session(ll);
                teamChatBuddyApplication.getListSession().add(session);
                teamChatBuddyApplication.setparam("listSession", new Gson().toJson(teamChatBuddyApplication.getListSession()));
                listRep.clear();
                listRepGlobale.add(reponse);
                updateChat();
            }
            else{
                if (!listRep.isEmpty()) {
                    //--> this function is called right after a question : we should create a new Replica for the response
                    Replica reponse = new Replica();
                    reponse.setValue(texte);
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
                    listRepGlobale.add(reponse);
                    updateChat();
                }
                else{
                    //---> this function is called after finishing pronouncing a phrase from the response : we should add the new phrase to the already existing Replica
                    Replica lastReplica = listRepGlobale.get(listRepGlobale.size() - 1);
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
                    updateChat();
                }
            }

            teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, type);
        }
        else if (type.equals("timeOutExpired")){
            teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
        }
    }

}
