package com.robotique.aevaweb.teamchatbuddy.chatbotresponse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import com.google.gson.JsonParser;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.HttpResponse;
import com.robotique.aevaweb.teamchatbuddy.models.Langue;
import com.robotique.aevaweb.teamchatbuddy.models.Missions;
import com.robotique.aevaweb.teamchatbuddy.models.Setting;
import com.robotique.aevaweb.teamchatbuddy.utilis.AlertManager;
import com.robotique.aevaweb.teamchatbuddy.utilis.BIPlayer;
import com.robotique.aevaweb.teamchatbuddy.utilis.HttpClientUtils;
import com.robotique.aevaweb.teamchatbuddy.utilis.IBehaviourCallBack;
import com.robotique.aevaweb.teamchatbuddy.utilis.ImageGenerator;
import com.robotique.aevaweb.teamchatbuddy.utilis.MailSender;
import com.google.gson.GsonBuilder;
import com.robotique.aevaweb.teamchatbuddy.utilis.SmsSender;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Commande {
    private String TAG="Commande";
    Activity activity;
    TeamChatBuddyApplication teamChatBuddyApplication;
    private Setting settingClass;
    ResponseFromChatbot responseClass;
    private String configFile ="TeamChatBuddy.properties";
    private String heart_rate;
    private String tensionS;
    private String tensionD;
    private String spo2;
    private String imeiLocation;
    private String imeiFeeder;
    public static MediaPlayer radioPlayer;
    public static MediaPlayer musicPlayer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String historicMessages = "messages";
    private String content= "content";
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String langueEs = "Espagnol";
    private String langueDe = "Allemand";
    private JSONArray existingHistoryArray=new JSONArray();
    String date="";
    String heure="";

    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm");

    String subject="";


    public Commande(Activity activity){
        this.activity = activity;
        teamChatBuddyApplication = (TeamChatBuddyApplication) activity.getApplicationContext();
        settingClass = new Setting();
    }


    public List<String> regex(String text) {
        Log.e("Commande","regex================================================================ "+text);
        String commande = extracCommandtBetweenPercent(text);
        if (commande!=null){
            if (teamChatBuddyApplication.getParamFromFile(commande,"TeamChatBuddy.properties")!=null) {
                text = teamChatBuddyApplication.getParamFromFile(commande, "TeamChatBuddy.properties");
                Log.e("Commande", "text= " + text);
            }
        }
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile("<(CMD_.*?)>|<(HEALYSA_.*?)>|<(SWITCHBOT_.*?)>");
        Matcher matcher = pattern.matcher(text);

        Log.i("text", text);
        while (matcher.find()) {
            String match = matcher.group(1) != null ? matcher.group(1) :
                    matcher.group(2) != null ? matcher.group(2) :
                            matcher.group(3);

            if (match != null) {
                Log.e("Commande", "match = " + match);
                matches.add(match);
            }
        }

        if (matches.isEmpty()) {
            Log.e("Commande", "Aucune commande trouvée");
            matches.add("INCONNUE");
        }

        return matches;
    }
    public String extracCommandtBetweenPercent(String text) {
        // Définir le motif pour matcher la chaîne sous la forme <%.*?%>
        Pattern pattern = Pattern.compile("<%(.*?)%>");
        Matcher matcher = pattern.matcher(text);

        // Vérifier si la chaîne correspond au motif
        if (matcher.matches()) {
            // Retourner le texte entre les %
            return matcher.group(1);
        }

        // Retourner null si la chaîne ne correspond pas au motif
        return null;
    }
    public String getDescription(String text, Boolean isHealysa){
        int index;
        if(isHealysa) index = text.indexOf("HEALYSA_");
        else index = text.indexOf("CMD_");
        if(index != -1) {
            int cmdLength = 4; // Length of "CMD_"
            int endIndex = text.indexOf(" ", index + cmdLength); // Find the index of the next space after CMD_
            if (endIndex == -1) {
                Log.e(TAG, "Extract Description from : "+text + " --> INCONNUE");
                return "INCONNUE"; // No space found after CMD_
            }
            Log.i(TAG, "Extract Description from : "+text + " --> " + text.substring(endIndex + 1));
            if (text.substring(endIndex + 1).contains("%")){
                if (teamChatBuddyApplication.getParamFromFile(text.substring(endIndex + 1).split("%")[1],"TeamChatBuddy.properties")!=null){
                    return teamChatBuddyApplication.getParamFromFile(text.substring(endIndex + 1).split("%")[1],"TeamChatBuddy.properties");
                }
                else return "Prompt_INCONNUE";
            }
            else {
                return text.substring(endIndex + 1);
            }
            // Return substring after the space
        }
        Log.e(TAG, "Extract Description from : "+text + " --> INCONNUE");
        return "INCONNUE"; // No CMD_ found
    }

    public String getDescription(String text){
        return getDescription(text, false);
    }

    public String verifyCmdMessages(String message) {
        if (message.isEmpty()) {
            return "EMPTY";
        }
        else if (!message.matches(".*\\s*/\\s*(?:/\\s*)?.*")) {
            return "DO_NOT_CONTAIN_SPLIT_CHARACTER";
        }

        String[] parts = message.split("\\s*/\\s*(?:/\\s*)?", -1);

        String before = parts[0].trim();
        String after = parts.length > 1 ? parts[1].trim() : "";

        boolean hasBefore = !before.isEmpty();
        boolean hasAfter = !after.isEmpty();

        if (hasBefore && hasAfter) {
            return "CONTAIN_BOTH_PARTS";
        }
        else if (hasBefore) {
            return "CONTAIN_ONLY_FIRST_PART";
        }
        else if (hasAfter) {
            return "CONTAIN_ONLY_SECOND_PART";
        }
        else {
            return "CONTAIN_ONLY_SPLIT_CHARACTER";
        }
    }

    public void translate(String message, ITranslationCallback iTranslationCallback) {
        String lang = teamChatBuddyApplication.getLangue().getNom();
        String message_en = teamChatBuddyApplication.getParamFromFile(message + "_en", "TeamChatBuddy.properties");
        String message_fr = teamChatBuddyApplication.getParamFromFile(message + "_fr", "TeamChatBuddy.properties");
        Log.i(TAG, "-- > message fr "+message_fr);
        Log.i(TAG, "-- > message en "+message_en);
        if ("Français".equals(lang)) {
            if (message_fr != null && !message_fr.isEmpty()) {
                iTranslationCallback.onTranslated(message_fr);
            } else if (message_en != null && !message_en.isEmpty()) {
                Log.i(TAG, "Aucun message défini pour fr --> passe au message en");

                // Translate English message to French
                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator()
                        .translate(message_en)
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                iTranslationCallback.onTranslated(translatedText);
                                Log.i(TAG, "Aucun message défini pour fr --> passe au message en --< translated message "+translatedText);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                iTranslationCallback.onTranslated(message_en); // Fallback to English if translation fails
                            }
                        });
            } else {
                Log.i(TAG, "Aucun message défini pour " + message + "");
                // Neither French nor English message exists, use ChatGPT
                iTranslationCallback.onTranslated("No_message_defined");
            }
        } else if ("Anglais".equals(lang)) {
            if (message_en != null && !message_en.isEmpty()) {
                iTranslationCallback.onTranslated(message_en);
            } else {
                // No English message, fallback to ChatGPT
                iTranslationCallback.onTranslated("No_message_defined");
                Log.i(TAG, "Aucun message défini pour " + message + " 2");
            }
        } else {
            // Default to English and translate to target language
            if (message_en != null && !message_en.isEmpty()) {
                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator()
                        .translate(message_en)
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                iTranslationCallback.onTranslated(translatedText);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                iTranslationCallback.onTranslated(message_en); // Fallback to English if translation fails
                            }
                        });
            } else {
                // No message available, use ChatGPT
                iTranslationCallback.onTranslated("No_message_defined");
                Log.i(TAG, "Aucun message défini pour " + message + " 3.");
            }
        }
    }

    public void translatePrompt(String message, ITranslationCallback iTranslationCallback){
        if (teamChatBuddyApplication.getLangue().getNom().equals("Français") ) {
            iTranslationCallback.onTranslated(message);
        }
        else {
            teamChatBuddyApplication.getFrenchLanguageSelectedTranslator().translate(message)
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            iTranslationCallback.onTranslated(translatedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            iTranslationCallback.onTranslated(message);
                        }
                    });
        }
    }

    public void translateNews(String message, ITranslationCallback iTranslationCallback){
        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(message).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String translatedText) {
                iTranslationCallback.onTranslated(translatedText);
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                iTranslationCallback.onTranslated(message);
            }
        });
    }

    public void translateSpecificErrors(String message, ITranslationCallback iTranslationCallback){
        teamChatBuddyApplication.getFrenchLanguageSelectedTranslator().translate(message).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String translatedText) {
                iTranslationCallback.onTranslated(translatedText);
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                iTranslationCallback.onTranslated(message);
            }
        });
    }

    private Locale getCurrentLocale(){
        List<String> langueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_GoogleCloud_STT");
        String language = langueCode.get(teamChatBuddyApplication.getLangue().getId()-1).replace("-","_");
        Locale[] locales = Locale.getAvailableLocales();
        for (Locale locale : locales) {
            if (locale.toString().equals(language)) {
                return locale;
            }
        }
        return Locale.ENGLISH;
    }

    boolean containsLanguageWithName(List<Langue> langues, String languageName) {
        for (Langue langue : langues) {
            if (langue.getNom().equalsIgnoreCase(languageName)) {
                return true;
            }
        }
        return false;
    }

    public interface ITranslationCallback {
        void onTranslated(String translatedText);
    }

    public boolean start_action(@NonNull String action, int numberOfQuestion, String texte){
        boolean is_command;
        Log.d("joke_check", "action: "+action);
        switch (action.split( " " )[0]){
            case "CMD_MUSIC":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_MUSIC", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_MUSIC(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_MUSIC(getDescription(action));
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_MUSIC(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_MUSIC(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_DATE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_DATE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        Log.i(TAG," verifyMusicMessage "+verifyMusicMessage+" translatedText "+translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_DATE(getDescription(action));
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_DATE(getDescription(action));
                                    }
                                }, 2000);
                            }
                            else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_DATE(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_HOUR":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_HOUR", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_HEURE(getDescription(action));
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_HEURE(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_HEURE(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_LANGUE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_LANGUE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_LANGUE(action.split(" ")[1]);
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_LANGUE(action.split(" ")[1]);
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_LANGUE(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_LANGUE(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_TEMP":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_TEMP", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TEMP(action.split(" ")[1]);
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_TEMP(action.split(" ")[1]);
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_TEMP(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_TEMP(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_BATTERIE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_BATTERIE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_BATTERIE();
                                    }
                                }, 3000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_BATTERIE();
                                    }
                                }, 3000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_BATTERIE();
                                    }
                                }, 3000);
                            }
                        }
                    }
                });
                break;

            case "CMD_SOUND":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_SOUND", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_SOUND(action.split(" ")[1]);
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SOUND(action.split(" ")[1]);
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SOUND(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SOUND(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_MOVE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_MOVE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_MOVE(action.split(" ")[1]);
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_MOVE(action.split(" ")[1]);
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_MOVE(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_MOVE(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_TURN":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_TURN", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TURN(action.split(" ")[1]);
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_TURN(action.split(" ")[1]);
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_TURN(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_TURN(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_HEAD":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_HEAD", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_HEAD(action.split(" ")[1]);
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_HEAD(action.split(" ")[1]);
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_HEAD(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_HEAD(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_STOP":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_STOP", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.setStartRecording(false);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_STOP();
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.setStartRecording(false);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP();
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.setStartRecording(false);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP();
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP();
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_QUIT":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_QUIT", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_QUIT();
                                }
                            }, 3000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_QUIT();
                                    }
                                }, 3000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_QUIT();
                                    }
                                }, 3000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_QUIT();
                                    }
                                }, 3000);
                            }
                        }
                    }
                });
                break;
            case "CMD_RUN":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_RUN", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_RUN(getDescription(action));
                                }
                            }, 5000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]", getDescription(action)));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_RUN(getDescription(action));
                                    }
                                }, 5000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_RUN(getDescription(action));
                                    }
                                }, 5000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_RUN(getDescription(action));
                                    }
                                }, 5000);
                            }
                        }
                    }
                });
                break;
            case "CMD_DANCE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_DANCE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_DANCE();
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_DANCE();
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_DANCE();
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_DANCE();
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_METEO":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_METEO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                Log.i(TAG," notify 1 first message ");
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]", getDescription(action)));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_METEO(getDescription(action));
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.replace("[1]", getDescription(action)));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_METEO(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_METEO(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_RADIO":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_RADIO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_RADIO(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]", getDescription(action)));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_RADIO(getDescription(action));
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.replace("[1]", getDescription(action)));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_RADIO(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_RADIO(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_BI":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_BI", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyBIMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyBIMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_BI(getDescription(action));
                                }
                            }, 3000);
                        } else {
                            if (verifyBIMessage.equals("CONTAIN_BOTH_PARTS") || verifyBIMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]", getDescription(action)));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_BI(getDescription(action));
                                    }
                                }, 3000);

                            } else if (verifyBIMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.replace("[1]", getDescription(action)));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_BI(getDescription(action));
                                    }
                                }, 3000);
                            }
                            else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_BI(getDescription(action));
                                    }
                                }, 3000);
                            }
                        }
                    }
                });
                break;
            case "SWITCHBOT_LIGHT":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("SWITCHBOT_LIGHT", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    SWITCHBOT_LIGHT(action.split(" ")[1]);
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]", action.split(" ")[1]));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        SWITCHBOT_LIGHT(action.split(" ")[1]);
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        SWITCHBOT_LIGHT(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        SWITCHBOT_LIGHT(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "HEALYSA_CONNECT":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_CONNECT", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CONNECT(action.split(" ")[1]);
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_CONNECT(action.split(" ")[1]);
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_CONNECT(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_CONNECT(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "HEALYSA_HRV":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_HRV", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_HRV();
                                    }
                                },2000);
                            }
                            else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_HRV();
                                    }
                                },2000);
                            }
                            else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_HRV();
                                    }
                                },2000);
                            }
                        }
                    }
                });
                break;

            case "HEALYSA_BLOODP":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_BLOODP", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_BLOODP();
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_BLOODP();
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_BLOODP();
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "HEALYSA_SPO2":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_SPO2", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_SPO2();
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_SPO2();
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_SPO2();
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "HEALYSA_CHECKUP":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_CHECKUP", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_CHECKUP();
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_CHECKUP();
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_CHECKUP();
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "HEALYSA_CALL":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_CALL", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CALL(getDescription(action, true));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_CALL(getDescription(action, true));
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_CALL(getDescription(action, true));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_CALL(getDescription(action, true));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "HEALYSA_LOC":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_LOC", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_LOC(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_LOC(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_LOC(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "HEALYSA_FEEDCAT":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_FEEDCAT", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_FEEDCAT(action.split(" ")[1]);
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_FEEDCAT(action.split(" ")[1]);
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_FEEDCAT(action.split(" ")[1]);
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        HEALYSA_FEEDCAT(action.split(" ")[1]);
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_IMAGE":
                is_command = true;
                Log.i(TAG, action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_IMAGE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_IMAGE(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_IMAGE(getDescription(action));
                                    }
                                }, 2000);

                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;

            case "CMD_CLOSE_IMAGE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_CLOSE_IMAGE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_CLOSE_IMAGE();
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_CLOSE_IMAGE();
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_CLOSE_IMAGE();
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_CLOSE_IMAGE();
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_PHOTO":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_PHOTO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TAKE_PHOTO(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("//")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_TAKE_PHOTO(getDescription(action));
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_TAKE_PHOTO(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_TAKE_PHOTO(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_PROMPT":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                CMD_PROMPT(getDescription(action),numberOfQuestion);
                break;
            case "CMD_NONE":
                is_command = false;
                Log.i(TAG,"CMD_NONE : "+ action);
                break;
            case "CMD_JOKE":
                is_command = true;
                Log.i("joke_check",action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_JOKE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_JOKE(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("//")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_JOKE(getDescription(action));
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_JOKE(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_JOKE(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_HEADER":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_HEADER", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_HEADER(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("//")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_HEADER(getDescription(action));
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_HEADER(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_HEADER(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_STOP_RADIO":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_STOP_RADIO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_STOP_RADIO();
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP_RADIO();
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP_RADIO();
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP_RADIO();
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_STOP_MUSIC":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_STOP_MUSIC", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_STOP_MUSIC();
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP_MUSIC();
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP_MUSIC();
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP_MUSIC();
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_STOP_BI":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_STOP_BI", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_STOP_BI();
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP_BI();
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP_BI();
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_STOP_BI();
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_SCEN":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_SCEN", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_SCEN(getDescription(action));
                                }
                            }, 5000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]", getDescription(action)));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SCEN(getDescription(action));
                                    }
                                }, 5000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SCEN(getDescription(action));
                                    }
                                }, 5000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SCEN(getDescription(action));
                                    }
                                }, 5000);
                            }
                        }
                    }
                });
                break;
            case "CMD_SAVE_IMAGE":
                is_command = true;
                Log.i(TAG, action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_SAVE_IMAGE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_SAVE_IMAGE(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SAVE_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SAVE_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SAVE_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_SHOW_IMAGE":
                is_command = true;
                Log.i(TAG, action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_SHOW_IMAGE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_SHOW_IMAGE(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SHOW_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SHOW_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SHOW_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_DEL_IMAGE":
                is_command = true;
                Log.i(TAG, action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_DEL_IMAGE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_DEL_IMAGE(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_DEL_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_DEL_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_DEL_IMAGE(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_MAIL":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_MAIL", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_MAIL(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_MAIL(getDescription(action));
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_MAIL(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_MAIL(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_SMS":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_SMS", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if (translatedText.contains("No_message_defined") || verifyMusicMessage.equals("CONTAIN_ONLY_SPLIT_CHARACTER")) {
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_SMS(getDescription(action));
                                }
                            }, 2000);
                        } else {
                            if (verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART")) {
                                Log.i("CMD_SMS_TEST", "verifyMusicMessage CONTAIN_BOTH_PARTS: "+getDescription(action));
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]", action.split("\\[")[2].split("]")[0]).replace("[2]", action.split("\\[")[1].split("]")[0]));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SMS(getDescription(action));
                                    }
                                }, 2000);
                            } else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")) {
                                Log.i("CMD_SMS_TEST", "verifyMusicMessage else if : "+action);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SMS(getDescription(action));
                                    }
                                }, 2000);
                            } else {
                                Log.i("CMD_SMS_TEST", "verifyMusicMessage else: "+action);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        CMD_SMS(getDescription(action));
                                    }
                                }, 2000);
                            }
                        }
                    }
                });
                break;
            case "CMD_NEWS":
                is_command = true;
                Log.i(TAG, action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                CMD_NEWS(getDescription(action));
                break;
            default:
                is_command = false;
                Log.i(TAG,"DEFAULT : "+ action);
        }
        return is_command;
    }

    public void CMD_SMS(String description) {
        Log.i("MYA","CMD_SMS  "+description);
        String regex = "\\[([^\\]]+)\\]"; // Capture tout ce qui est entre [ ]
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(description);

        String recipient = null;
        String message = null;

        // Extraire les deux valeurs entre crochets
        if (matcher.find()) {
            recipient = matcher.group(1);// Premier groupe capturé : nom du destinataire
            if(recipient!=null){
                recipient = recipient.toLowerCase();
            }
        }
        if (matcher.find()) {
            message = matcher.group(1); // Deuxième groupe capturé : message
        }

        String finalRecipient = recipient;
        String finalMessage = message;
        // Afficher les résultats
        if (recipient != null && message != null) {
            Log.d("CMD", "CMD_SMS: recipient != null && message != null ");

            Log.i("CMD_MYA","CMD_SMS destinataire "+recipient);
            Log.i("CMD_MYA","CMD_SMS message  "+message);
            String keyBase = "sms_";

            // Chercher dans le fichier de configuration en gérant les inversions
            String[] recipientParts = recipient.split(" |-"); // Découper le nom et prénom
            List<String> possibleKeys = generatePossibleKeys(keyBase, recipientParts);
            Log.i("CMD_MYA", "CMD_SMS: "+possibleKeys);
            String sms_num = null;
            for (String key : possibleKeys) {
                Log.i("CMD_MYA", "CMD_SMS key  " + key);

                sms_num = teamChatBuddyApplication.getParamFromFile(key, configFile);
                Log.i("CMD_MYA", "CMD_SMS message  " + message);

                Log.d("CMD_MYA", "CMD_SMS: sms_num != null ?"+(sms_num != null));
                Log.d("CMD_MYA", "CMD_SMS sms: "+sms_num);
                if(sms_num!=null){
                    break;
                }
            }
            Log.d("CMD_MYA", " ----------CMD_SMS Final sms---------- "+sms_num);

            Log.e("CMD_MYA", "CMD_SMS message: "+message);
            if (sms_num != null && !sms_num.trim().isEmpty()) {
                Log.d("CMD_MYA", "isAirplaneModeOn: ");

                Log.d("CMD_MYA", "not plane mode and sms_num not empty");
                if(sms_num.matches("^\\+?[0-9]{8,15}$")){
                    try {

                        Log.d("CMD_MYA", "sms_num.matches(\"^\\\\+?[0-9]{8,15}$\")");
                        SmsSender smsSender = new SmsSender(activity);

                        smsSender.sendSmsOrEmailFallback(sms_num, message, new SmsSender.MailCallback() {
                            @Override
                            public void onSuccess() {
                                Log.i("CMD_MYA", "SMS envoyée avec succès (SMS ou fallback OVH).");
                                translate("CMD_SMS", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        if (translatedText.contains("No_message_defined")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                        } else {
                                            String verifyMessage = verifyCmdMessages(translatedText);
                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                try {
                                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                    JSONObject history1 = new JSONObject();
                                                    history1.put("role", "assistant");
                                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[2]", finalRecipient).replace("[1]", finalMessage));
                                                    existingHistoryArray.put(history1);
                                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);

                                                Log.d("CMD_MYA", "mail sent here is the response");
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[2]", finalRecipient).replace("[1]", finalMessage));
                                            }
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                translate("CMD_SMS", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            translateSpecificErrors(e.getMessage(), new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedMessage) {
                                                    Log.i(TAG, "Error Translated: " + translatedMessage);
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                            translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[3]", translatedMessage).replace("[2]", finalRecipient).replace("[1]", finalMessage));
                                                }
                                            });
                                        }
                                    }
                                });
                                Log.e("CMD_SMS", "Erreur lors de l’envoi du sms : " + e.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Log.e("CMD_SMS", "Erreur fatale dans sendSmsIfAvailable: " + e.getMessage(), e);
                    }
                }
                else{

                    Log.d("CMD_MYA", "numéro non valide");
                    // Gestion d’un numéro invalide
                    translate("CMD_SMS", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors("le numéro du destinataire est invalide", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage);
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[3]", translatedMessage).replace("[2]", finalRecipient).replace("[1]", finalMessage));
                                    }
                                });
                            }
                        }
                    });
                }

            }
            else if (sms_num == null || sms_num.trim().isEmpty()) {
                Log.d("CMD_MYA", "CMD_SMS: sms_num == null ");
                Log.d("SMS", "CMD_SMS: le numéro du destinataire est introuvable");
                translate("CMD_SMS", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" le numéro du destinataire est introuvable", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage);
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[3]", translatedMessage).replace("[2]", finalRecipient).replace("[1]", finalMessage));//this one
                                    }
                                });
                            }
                        }
                    }
                });
            }

        } else {
            Log.d("CMD", "CMD_SMS: recipient == null || message == null ");
            Log.i("MYA","CMD_SMS Format invalide. Veuillez utiliser : CMD_SMS [destinataire] [message]");
            if(recipient == null){
                translate("CMD_SMS", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" le nom du destinataire est unconnu", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage); // Check description translation
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[3]", translatedMessage).replace("[2]", " ").replace("[1]", finalMessage));
                                    }
                                });
                            }
                        }
                    }
                });
            }
            else if(message == null){
                translate("CMD_SMS", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" le sms est vide", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage); // Check description translation
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[3]", translatedMessage).replace("[2]", finalRecipient).replace("[1]", ""));
                                    }
                                });
                            }
                        }
                    }
                });

            }
        }
    }

    public void CMD_SMS_(String description) {
        Log.i("MYA","CMD_SMS  "+description);
        String regex = "\\[([^\\]]+)\\]"; // Capture tout ce qui est entre [ ]
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(description);

        String recipient = null;
        String message = null;

        // Extraire les deux valeurs entre crochets
        if (matcher.find()) {
            recipient = matcher.group(1);// Premier groupe capturé : nom du destinataire
            if(recipient!=null){
                recipient = recipient.toLowerCase();
            }
        }
        if (matcher.find()) {
            message = matcher.group(1); // Deuxième groupe capturé : message
        }
        // Afficher les résultats
        if (recipient != null && message != null) {
            Log.d("CMD", "CMD_SMS: recipient != null && message != null ");

            Log.i("CMD_SMS","CMD_SMS destinataire "+recipient);
            Log.i("CMD_SMS","CMD_SMS message  "+message);
            String keyBase = "sms_";

            // Chercher dans le fichier de configuration en gérant les inversions
            String[] recipientParts = recipient.split(" |-"); // Découper le nom et prénom
            List<String> possibleKeys = generatePossibleKeys(keyBase, recipientParts);
            Log.i("MYA", "CMD_SMS: "+possibleKeys);

            for (String key : possibleKeys) {
                Log.i("CMD_MYA", "CMD_SMS key  " + key);

                String sms_num = teamChatBuddyApplication.getParamFromFile(key, configFile);
                Log.i("CMD_MYA", "CMD_SMS message  " + message);

                Log.d("CMD_MYA", "CMD_SMS: sms_num != null ?"+(sms_num != null));
                Log.d("CMD_MYA", "CMD_SMS sms: "+sms_num);
                Log.e("CMD_MYA", "CMD_SMS message: "+message);
                if (sms_num != null && !sms_num.trim().isEmpty()) {
                    boolean isAirplaneModeOn = Settings.Global.getInt(
                            teamChatBuddyApplication.getContentResolver(),
                            Settings.Global.AIRPLANE_MODE_ON, 0
                    ) != 0;
                    Log.d("CMD_MYA", "isAirplaneModeOn: "+isAirplaneModeOn);
                    if (isAirplaneModeOn) {

                        Log.d("CMD_MYA", "CMD_SMS: mode avion");
                        // Gestion d’un numéro invalide
                        translate("CMD_SMS", new ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedText) {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    translateSpecificErrors(" le robot est en mode avion", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedMessage) {
                                            Log.i(TAG, "Error Translated: " + translatedMessage);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                    translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                        }
                                    });
                                }
                            }
                        });
                        break;
                    }
                    else{
                        if (message != null) {

                            Log.d("CMD_MYA", "not plane mode and sms_num not empty");
                            if(sms_num.matches("^\\+?[0-9]{8,15}$")){
                                try {

                                    SmsSender smsSender = new SmsSender(activity);

                                    smsSender.sendSmsOrEmailFallback(sms_num, message, new SmsSender.MailCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.i("CMD_SMS", "SMS envoyée avec succès (SMS ou fallback OVH).");
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Log.e("CMD_SMS", "Erreur lors de l’envoi du sms : " + e.getMessage());
                                        }
                                    });

                                } catch (Exception e) {
                                    Log.e("CMD_SMS", "Erreur fatale dans sendSmsIfAvailable: " + e.getMessage(), e);
                                }
                                translate("CMD_SMS", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        if (translatedText.contains("No_message_defined")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                        } else {
                                            String verifyMessage = verifyCmdMessages(translatedText);
                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                try {
                                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                    JSONObject history1 = new JSONObject();
                                                    history1.put("role", "assistant");
                                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                    existingHistoryArray.put(history1);
                                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                            }
                                        }
                                    }
                                });
                                break;
                            }
                            else{

                                Log.d("CMD_MYA", "numéro non valide");
                                // Gestion d’un numéro invalide
                                translate("CMD_SMS", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            translateSpecificErrors("le numéro du destinataire est invalide", new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedMessage) {
                                                    Log.i(TAG, "Error Translated: " + translatedMessage);
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                            translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                                }
                                            });
                                        }
                                    }
                                });
                                break;
                            }

                        } else {

                            Log.d("CMD_MYA", "not plane mode and sms_num not empty but not msg");
                            // Gestion d’un numéro invalide
                            translate("CMD_SMS", new ITranslationCallback() {
                                @Override
                                public void onTranslated(String translatedText) {
                                    String verifyMessage = verifyCmdMessages(translatedText);
                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                        translateSpecificErrors(" le message du sms est vide", new ITranslationCallback() {
                                            @Override
                                            public void onTranslated(String translatedMessage) {
                                                Log.i(TAG, "Error Translated: " + translatedMessage);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                        translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                            }
                                        });
                                    }
                                }
                            });
                            break;
                        }
                    }

                } else {


                    Log.d("CMD_MYA", "CMD_SMS: sms_num == null ");
                    Log.d("SMS", "CMD_SMS: le numéro du destinataire est introuvable");
                    translate("CMD_SMS", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    translateSpecificErrors(" le numéro du destinataire est vide", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedMessage) {
                                            Log.i(TAG, "Error Translated: " + translatedMessage);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                    translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));//this one
                                        }
                                    });
                                }
                            }
                        }
                    });
                    break;
                }
            }
            translate("CMD_SMS", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    String verifyMessage = verifyCmdMessages(translatedText);
                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        translateSpecificErrors("Le numéro de votre destinataire est introuvable.", new ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedMessage) {
                                Log.i(TAG, "Error Translated: " + translatedMessage);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                        translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                            }
                        });
                    }
                }
            });

        } else {
            Log.d("CMD", "CMD_SMS: recipient == null || message == null ");
            Log.i("MYA","CMD_SMS Format invalide. Veuillez utiliser : CMD_SMS [destinataire] [message]");
            translate("CMD_SMS", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            translateSpecificErrors(" le nom du destinataire est invalide", new ITranslationCallback() {
                                @Override
                                public void onTranslated(String translatedMessage) {
                                    Log.i(TAG, "Error Translated: " + translatedMessage); // Check description translation
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": "+translatedMessage));
                                }
                            });
                        }
                    }
                }
            });
        }
    }


    public String extractCmdNewsValue(Activity activity) {
        // Définition du fichier de configuration
        File directory = new File(activity.getString(R.string.path), "TeamChatBuddy");
        File configFile = new File(directory, "TeamChatBuddy.properties");

        try {
            // Lire tout le fichier en une seule chaîne (compatible Java 8)
            String content = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);

            // Expression régulière pour capturer le nombre après <CMD_NEWS
            Pattern pattern = Pattern.compile("<CMD_NEWS\\s+(\\d+)>");
            Matcher matcher = pattern.matcher(content);

            // Extraction du nombre
            if (matcher.find()) {
                String extractedNumber = matcher.group(1);
                Log.i("Commande", "Le nombre extrait est : " + extractedNumber);
                return extractedNumber; // Retourne le nombre extrait
            } else {
                Log.i("Commande", "Aucune correspondance trouvée.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; // Retourne null si aucune correspondance n'est trouvée
    }

    private void CMD_NEWS(String description) {

        Log.i(TAG,"CMD_NEWS  "+description);

        int durée = 3; // Valeur par défaut

        if (description.isEmpty()) {
            String extractedNumber = extractCmdNewsValue(activity);
            if (extractedNumber != null) {
                try {
                    durée = Integer.parseInt(extractedNumber);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la conversion de extractedNumber en entier", e);
                }
            }
        } else {
            try {
                durée = Integer.parseInt(description);
            } catch (Exception e) {
                String extractedNumber = extractCmdNewsValue(activity);
                if (extractedNumber != null) {
                    try {
                        durée = Integer.parseInt(extractedNumber);
                    } catch (Exception exception) {
                        Log.e(TAG, "Erreur lors de la conversion de extractedNumber en entier", exception);
                    }
                }
            }
        }
        Log.i(TAG, "Durée finale: " + durée);

        final int descriptionFinal = durée;
        new Thread(() -> {
            try {
                HttpResponse httpResponse = HttpClientUtils.sendGet("https://www.bbc.com/", null, 50000);
                if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300 && httpResponse.body != null) {
                    Log.i(TAG, "---------------------------- BBC news ---------------------------- \n" + httpResponse.body);
                    extractAndLogNews(httpResponse.body, descriptionFinal);
                } else {
                    // Cas d'erreur HTTP → on remplace la partie else de onResponse
                    translate("CMD_NEWS", translatedText -> {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            }
                        }
                    });
                }

            } catch (IOException e) {
                // Cas exception → on remplace la partie onFailure
                translate("CMD_NEWS", translatedText -> {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                existingHistoryArray.put(history1);
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                        }
                    }
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void extractAndLogNews(String html, int thresholdHours) {
        Document doc = Jsoup.parse(html);
        //    Elements articles = doc.select("div[data-testid=london-article]");
        String cssQuery = teamChatBuddyApplication.getParamFromFile("NEWS_ARTICLE_BBC_id","TeamChatBuddy.properties");
        Elements articles = doc.select(cssQuery);

        Set<String> uniqueTitles = new HashSet<>(); // Pour stocker les titres uniques
        StringBuilder responseBuilder = new StringBuilder();
        boolean foundNews = false;
        int newsCount = 0;
        Log.i(TAG, "articles: " + articles.size());

        for (Element article : articles) {
            String title = getText(article, "h2[data-testid=card-headline]");
            String description = getText(article, "p[data-testid=card-description]");
            String timeStr = getText(article, "span[data-testid=card-metadata-lastupdated]");
            int minutesAgo = convertTimeToMinutes(timeStr);

            if (title != null && description != null && minutesAgo <= thresholdHours * 60  && uniqueTitles.add(title)) {
                foundNews = true;
                newsCount++;
                Log.i(TAG, "Titre: " + title);
                Log.i(TAG, "Description: " + description);
                Log.i(TAG, "Publié il y a: " + timeStr);
                Log.i(TAG, "----------------------------");
                responseBuilder.append("\n\n").append("Title "+newsCount+": ").append(title).append("\n").append(description);
            }
        }
        String finalResponse = "The number of titles returned is : " + newsCount + "\n\n" +responseBuilder.toString().trim();

        if(!foundNews){

            translate("CMD_NEWS", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                // get the historic commandes :
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                existingHistoryArray.put(history1);
                                // Stocker la nouvelle version de l'historique
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                        }
                    }
                }
            });
            return;
        }

        if(teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
            Log.i(TAG, "---------------------------- Translated Text -----en \n " + finalResponse.replaceAll("\n\n",";splitNews;"));
            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + finalResponse.replaceAll("\n\n",";splitNews;"));
        }
        else{
            // Split articles by \n\n
            String[] articlesArray = finalResponse.split("\n\n");

            String firstSegment = articlesArray[0];
            Log.i(TAG, "---------------------------- firstSegment ----- \n " + firstSegment);
            boolean hasHeader = firstSegment.startsWith("The number of titles returned is :");

            StringBuilder translatedResponse = new StringBuilder();
            AtomicInteger pendingTranslations = new AtomicInteger(hasHeader ? articlesArray.length : articlesArray.length - 1);

            if (hasHeader) {
                translateNews(firstSegment, new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedHeader) {
                        synchronized (translatedResponse) {
                            translatedResponse.append(translatedHeader);
                            Log.i(TAG, "-------------------------translatedResponse --- firstSegment ----- \n " + translatedResponse);
                        }

                        // Start translating articles after header translation is done
                        translateArticles(articlesArray, hasHeader, translatedResponse, pendingTranslations);
                    }
                });
            } else {
                // If there's no header, directly process articles
                translateArticles(articlesArray, false, translatedResponse, pendingTranslations);
            }
        }
    }

    // Function to translate articles
    private void translateArticles(String[] articlesArray, boolean hasHeader, StringBuilder translatedResponse, AtomicInteger pendingTranslations) {
        int numberOfArticles = articlesArray.length - (hasHeader ? 1 : 0); // Adjust for header if present
        pendingTranslations.set(numberOfArticles); // Initialize with the number of articles to be translated

        for (int i = (hasHeader ? 1 : 0); i < articlesArray.length; i++) {

            String article = articlesArray[i];
            String[] lines = article.split("\n", 2); // Split into title and description
            if (lines.length < 2) continue; // Skip malformed entries

            String title = lines[0];
            String description = lines[1];

            translateNews(title, new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedTitle) {
                    Log.i(TAG, "Title Translated: " + translatedTitle); // Check title translation
                    translateNews(description, new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedDescription) {
                            Log.i(TAG, "Description Translated: " + translatedDescription); // Check description translation
                            synchronized (translatedResponse) {
                                translatedResponse.append(";splitNews;").append(translatedTitle).append("\n").append(translatedDescription);
                            }

                            if (pendingTranslations.decrementAndGet() == 0) {
                                // Notify once all translations are done
                                String finalTranslatedText = translatedResponse.toString().trim();
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + finalTranslatedText);
                                Log.i(TAG, "---------------------------- Translated Text ----- fr \n " + finalTranslatedText);
                            }
                        }
                    });
                }
            });
        }
    }

    private String getText(Element parent, String selector) {
        Element element = parent.selectFirst(selector);
        return (element != null) ? element.text().trim() : null;
    }

    private int convertTimeToMinutes(String timeStr) {
        if (timeStr == null) return Integer.MAX_VALUE;
        timeStr = timeStr.toLowerCase();
        if (timeStr.contains("min")) return Integer.parseInt(timeStr.replaceAll("\\D+", ""));
        if (timeStr.contains("hr")) return Integer.parseInt(timeStr.replaceAll("\\D+", "")) * 60;
        if (timeStr.contains("day")) return Integer.parseInt(timeStr.replaceAll("\\D+", "")) * 1440;
        return Integer.MAX_VALUE;
    }

    public void CMD_MAIL(String description) {
        Log.i("HHO","CMD_MAIL  "+description);
        String regex = "\\[([^\\]]+)\\]"; // Capture tout ce qui est entre [ ]
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(description);

        String recipient = null;
        String message = null;

        // Extraire les deux valeurs entre crochets
        if (matcher.find()) {
            recipient = matcher.group(1);// Premier groupe capturé : nom du destinataire
            if(recipient!=null){
                recipient = recipient.toLowerCase();
            }
        }
        if (matcher.find()) {
            message = matcher.group(1); // Deuxième groupe capturé : message
        }
        // Afficher les résultats
        if (recipient != null && message != null) {
            Log.i("HHO","CMD_MAIL destinataire "+recipient);
            Log.i("HHO","CMD_MAIL message  "+message);
            String keyBase = "mail_";

            // Chercher dans le fichier de configuration en gérant les inversions
            String[] recipientParts = recipient.split(" |-"); // Découper le nom et prénom
            List<String> possibleKeys = generatePossibleKeys(keyBase, recipientParts);

            for (String key : possibleKeys) {

                String email = teamChatBuddyApplication.getParamFromFile(key, configFile);
                Log.i("HHO","CMD_MAIL email  "+email);
                if (email != null) {
                    if(teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                        subject = teamChatBuddyApplication.getParamFromFile("CMD_MAIL_subject_en",configFile);
                    }
                    else if(teamChatBuddyApplication.getLangue().getNom().equals(langueFr)){
                        subject = teamChatBuddyApplication.getParamFromFile("CMD_MAIL_subject_fr",configFile);
                    }
                    else{
                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("CMD_MAIL_subject_en",configFile)).addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                subject = translatedText;
                            }

                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG,"translatedText exception  "+e);
                            }
                        });
                    }
                    // return email; // Retourner l'email si une clé correspondante est trouvée
                    MailSender smtpService;
                    smtpService = new MailSender(activity,message, email, subject);
                    smtpService.execute();
                    translate("CMD_MAIL", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        }
                    });
                    break;
                } else {
                    translate("CMD_MAIL", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    translateSpecificErrors(" l'email du destinataire est introuvable", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedMessage) {
                                            Log.i(TAG, "Error Translated: " + translatedMessage); // Check description translation
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": "+translatedMessage));
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }
        } else {
            Log.i("HHO","CMD_MAIL Format invalide. Veuillez utiliser : CMD_MAIL [destinataire] [message]");
            translate("CMD_MAIL", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            translateSpecificErrors(" le nom du destinataire est invalide", new ITranslationCallback() {
                                @Override
                                public void onTranslated(String translatedMessage) {
                                    Log.i(TAG, "Error Translated: " + translatedMessage); // Check description translation
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": "+translatedMessage));
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private static List<String> generatePossibleKeys(String keyBase, String[] parts) {
        Log.i("HHO","generatePossibleKeys "+new Gson().toJson(parts));
        List<String> keys = new ArrayList<>();
        if (parts.length == 0) {
            return keys; // Aucun élément, pas de clé à générer
        }

        // Générer toutes les permutations possibles pour n'importe quel ordre des parties du nom
        generatePermutations(parts, 0, keys, keyBase);

        Log.i("HHO", "Possible keys: " + new Gson().toJson(keys));
        return keys;
    }

    /**
     * Génère toutes les permutations d'un tableau `parts` et les ajoute à la liste `keys`.
     */
    private static void generatePermutations(String[] parts, int index, List<String> keys, String keyBase) {
        if (index == parts.length - 1) {
            // Ajouter une clé formée avec la permutation actuelle
            keys.add(keyBase + String.join("-", parts).toLowerCase());
            return;
        }

        for (int i = index; i < parts.length; i++) {
            swap(parts, i, index);
            generatePermutations(parts, index + 1, keys, keyBase);
            swap(parts, i, index); // Revenir à l'état original
        }
    }

    private static void swap(String[] array, int i, int j) {
        String temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    public void CMD_DEL_IMAGE(String description) {
        String directoryPath = "/storage/emulated/0/TeamChatBuddy/images/sent"; // Update with your directory path
        description = description.replaceAll(" ","_");
        boolean isDeleted = searchAndDeleteImage(directoryPath, description+".png");
        if(!isDeleted){
            isDeleted = searchAndDeleteImage("/storage/emulated/0/TeamChatBuddy/images/recv", description+".png");
            if(!isDeleted){
                translate("CMD_DEL_IMAGE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            }
                        }
                    }
                });
            }
        }
    }


    public boolean searchAndDeleteImage(String directoryPath, String fileName) {
        File directory = new File(directoryPath);
        // Check if the directory exists and is a directory
        if (!directory.exists() || !directory.isDirectory()) {
            Log.e(TAG, "Invalid directory: " + directoryPath);
            return false;
        }
        // Get a list of files in the directory
        File[] files = directory.listFiles();

        if (files == null || files.length == 0) {
            Log.i(TAG, "No files found in the directory: " + directoryPath);
            return false;
        }

        // Search for the file by name
        for (File file : files) {
            if (file.isFile() && file.getName().equals(fileName)) {
                // Attempt to delete the file
                if (file.delete()) {
                    Log.i(TAG, "File deleted successfully: " + file.getAbsolutePath());
                    return true;
                } else {
                    Log.e(TAG, "Failed to delete file: " + file.getAbsolutePath());
                    return false;
                }
            }
        }
        Log.i(TAG, "File not found: " + fileName);
        return false;
    }

    public void CMD_SHOW_IMAGE(String description) {
        Log.e("HHO", "CMD_SHOW: description "+description);
        description = description.replaceAll(" ","_");
        // Define the two directories to search
        String sentDirectoryPath = Environment.getExternalStorageDirectory() + "/TeamChatBuddy/images/sent";
        String recvDirectoryPath = Environment.getExternalStorageDirectory() + "/TeamChatBuddy/images/recv";
        boolean found = false;
        // Check in "sent" directory
        File sentDirectory = new File(sentDirectoryPath);
        File foundImage = findImageInDirectory(sentDirectory, description+".png");
        if (foundImage != null) {
            found = true;
            Log.e("HHO", "CMD_SHOW: foundImage "+foundImage);
            teamChatBuddyApplication.notifyObservers("showImage;SPLIT;TeamChatBuddy/images/sent/"+foundImage.getName());
        }

        // Check in "recv" directory
        if(!found){
            File recvDirectory = new File(recvDirectoryPath);
            foundImage = findImageInDirectory(recvDirectory, description+".png");
            if (foundImage != null) {
                Log.e("HHO", "CMD_SHOW: foundImage 1 "+foundImage);
                found = true;
                teamChatBuddyApplication.notifyObservers("showImage;SPLIT;TeamChatBuddy/images/recv/"+foundImage.getName());
            }
        }

        if(!found){
            translate("CMD_SHOW_IMAGE", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                existingHistoryArray.put(history1);
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                        }
                    }
                }
            });
        }

    }

    public File findImageInDirectory(File directory, String imageName) {
        if (directory.exists() && directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isFile() && file.getName().equals(imageName)) {
                    return file;
                }
            }
        }
        return null;
    }


    public void CMD_SAVE_IMAGE(String description) {
        Log.e("HHO", "CMD_SAVE: description "+description);
        description = description.replaceAll(" ","_");
        // Source file: the captured image
        File sourceFile = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/capturedImage.png");

        // Ensure the source file exists
        if (!sourceFile.exists()) {
            Log.e("HHO", "CMD_SAVE: No captured image found to save.");
            translate("CMD_SAVE_IMAGE", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                        }
                    }
                }
            });
            return;
        }

        // Target directory: TeamChatBuddy/images/sent/
        File targetDir = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/images/sent");
        if (!targetDir.exists()) {
            boolean dirCreated = targetDir.mkdirs();
            if (!dirCreated) {
                Log.e("HHO", "CMD_SAVE: Failed to create target directory.");
                return;
            }
        }

        // Target file: Target directory + given file name
        File targetFile = new File(targetDir, description + ".png");

        // Perform the file copy
        try (FileInputStream inputStream = new FileInputStream(sourceFile);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            Log.i("HHO", "CMD_SAVE: Successfully saved the captured image as " + targetFile.getAbsolutePath());
            translate("CMD_SAVE_IMAGE", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                existingHistoryArray.put(history1);
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                        }
                    }
                }
            });

        } catch (IOException e) {
            Log.e("HHO", "CMD_SAVE: Failed to save the image.", e);
        }
    }

    public void CMD_STOP_RADIO(){
        if (radioPlayer != null) {
            Log.i( TAG, "stop radioPlayer");
            radioPlayer.stop();
            radioPlayer.release();
            radioPlayer = null;
            teamChatBuddyApplication.setPlayingRadio(false);
        }
    }

    public void CMD_STOP_MUSIC(){
        if (musicPlayer != null) {
            Log.i( TAG, "stop musicPlayer");
            musicPlayer.stop();
            musicPlayer.release();
            musicPlayer = null;
            teamChatBuddyApplication.setPlayingMusic(false);
        }
    }
    public void CMD_STOP_BI(){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BIPlayer.getInstance().stopBehaviour();
            }
        });
    }


    public void CMD_JOKE(String description){
        String fullUrl;
        Log.i("joke_check", "CMD JOKE '"+ description +"' début." );
        String pauseJokeStr = teamChatBuddyApplication.getParamFromFile("pause_JOKE", configFile);
        int pause_joke = 0; // valeur par défaut
        if (pauseJokeStr != null && !pauseJokeStr.isEmpty()) {
            try {
                pause_joke = Integer.parseInt(pauseJokeStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid pause_JOKE format in config, defaulting to 0", e);
            }
        }
        int finalPause_joke = pause_joke;
        String jokeFrParam = teamChatBuddyApplication.getParamFromFile("JOKE_fr", configFile);
        boolean isJokeFr = (jokeFrParam == null || jokeFrParam.trim().isEmpty()) || jokeFrParam.trim().equalsIgnoreCase("yes");

        if(teamChatBuddyApplication.getLangue().getNom().equals("Français") && isJokeFr){
            new Thread(() -> {
                try {
                    Log.i("joke_check", "C'est en français ");
                    String joke_url = teamChatBuddyApplication.getParamFromFile("JOKE_URL_fr", configFile);
                    if (joke_url == null || joke_url.isEmpty()) joke_url = "https://blague-api.vercel.app/";

                    String finalUrl = joke_url + "api?mode=" + URLEncoder.encode(description, "UTF-8");
                    HttpResponse httpResponse = HttpClientUtils.sendGet(finalUrl, null, 50000);
                    Log.i("joke_check", "Envoi Http "+finalUrl);
                    Log.i("joke_check", "httpResponse "+httpResponse);
                    Log.i("joke_check", "httpResponse.responseCode "+httpResponse.responseCode);


                    if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300 && httpResponse.body != null) {
                        try {
                            //int x_points = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("JOKE_X_points", configFile));

                            Log.i("joke_check", "pause_joke = "+ finalPause_joke);
                            // Parsing du JSON reçu
                            JsonObject jsonObject = JsonParser.parseString(httpResponse.body).getAsJsonObject();
                            String joke = jsonObject.get("blague").getAsString();
                            String jokeResponse = jsonObject.get("reponse").getAsString();
                            //String points = new String(new char[x_points]).replace("\0", ".");

                            Log.i("joke_check","joke "+ joke+" \n response "+jokeResponse);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;"+joke);

                            // Retarder la traduction
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;"+jokeResponse);
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            translate("CMD_JOKE", translatedText -> {
                                                if (translatedText.contains("No_message_defined")) {
                                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                                } else {
                                                    String verifyMessage = verifyCmdMessages(translatedText);
                                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                        try {
                                                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                            JSONObject history1 = new JSONObject();
                                                            history1.put("role", "assistant");
                                                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                            existingHistoryArray.put(history1);
                                                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                        Log.i("joke_check blague","translatedText: " + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                    }
                                                }
                                            });
                                        };

                                    },2000);}

                            },(1000 * finalPause_joke)+4000);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        // Cas erreur HTTP → correspond au else de onResponse
                        translate("CMD_JOKE", translatedText -> {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                }
                            }
                        });
                    }

                } catch (Exception e) {
                    // Correspond au onFailure
                    e.printStackTrace();
                    translate("CMD_JOKE", translatedText -> {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException ex) {
                                    ex.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                            }
                        }
                    });
                }
            }).start();
        }
        else{
            Log.i("joke_check", "AUTRE LANGUE");
            Log.i("joke_check", "teamChatBuddyApplication.getParamFromFile(\"JOKE_fr\",configFile).trim().equalsIgnoreCase(\"yes\") "+ teamChatBuddyApplication.getParamFromFile("JOKE_fr",configFile).trim().equalsIgnoreCase("yes"));

            String lang = "en";
            String blacklistFlags = "nsfw,sexist,explicit";
            if(teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                lang = "en";
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                lang="fr";
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals("Allemand")){
                lang="de";
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals("Espagnol")){
                lang="es";
            }

            String joke_url = teamChatBuddyApplication.getParamFromFile("JOKE_URL",configFile);
            if(joke_url == null || joke_url.isEmpty()) joke_url = "https://v2.jokeapi.dev/joke/";

            fullUrl = joke_url + description + "?lang=" + lang + "&blacklistFlags=" + blacklistFlags + "&type=twopart";

            if(teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                fullUrl = joke_url + "Any?lang=" + lang + "&blacklistFlags=" + blacklistFlags + "&type=twopart";
            }
            if(teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                fullUrl = joke_url + description +  "?blacklistFlags=" + blacklistFlags + "&type=twopart";
            }

            Log.i("joke_check", "fullUrl : "+fullUrl);
            String finalFullUrl = fullUrl;
            new Thread(() -> {
                try {
                    HttpResponse result = HttpClientUtils.sendGet(finalFullUrl, null, 50000);

                    int httpCode = result.responseCode;
                    String body = result.body;
                    Log.i("joke_check", "Was sent http");
                    Log.i("joke_check", "body: "+body);
                    if (httpCode >= 200 && httpCode < 300 && body != null && !body.isEmpty()) {
                        try {
                            JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

                            if (jsonObject.has("joke")) {
                                String joke = jsonObject.get("joke").getAsString();
                                Log.i("joke_check","One Part : joke "+ joke);
                            } else if (jsonObject.has("setup") && jsonObject.has("delivery")) {
                                String setup = jsonObject.get("setup").getAsString();
                                String delivery = jsonObject.get("delivery").getAsString();
                                Log.i("joke_check","Two Parts : joke "+ setup + "\n" + delivery);

                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;"+setup);

                                // Retarder la traduction
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;"+delivery);
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                translate("CMD_JOKE", new ITranslationCallback() {
                                                    @Override
                                                    public void onTranslated(String translatedText) {
                                                        String verifyMessage = verifyCmdMessages(translatedText);
                                                        if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                                            try {
                                                                // get the historic commandes :

                                                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");


                                                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);

                                                                JSONObject history1 = new JSONObject();
                                                                history1.put("role", "assistant");
                                                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);

                                                                existingHistoryArray.put(history1);
                                                                // Stocker la nouvelle version de l'historique
                                                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            }
                                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                        }
                                                    }
                                                });
                                            };

                                        },2000);}

                                },(1000 * finalPause_joke)+4000);

                            } else {
                                translate("CMD_JOKE", translatedText -> {
                                    if (translatedText.contains("No_message_defined")) {
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                    } else {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            try {
                                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                JSONObject history1 = new JSONObject();
                                                history1.put("role", "assistant");
                                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                existingHistoryArray.put(history1);
                                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                        }
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Cas équivalent à Retrofit onFailure ou errorBody
                        translate("CMD_JOKE", translatedText -> {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                }
                            }
                        });
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    // onFailure équivalent
                    translate("CMD_JOKE", translatedText -> {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException ex) {
                                    ex.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                            }
                        }
                    });
                }
            }).start();
        }
    }

    public void CMD_HEADER(String header){
        if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
            translatePrompt(header, new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    teamChatBuddyApplication.setparam("header",translatedText);
                    translate("CMD_HEADER", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        }
                    });
                }
            });
        }
        else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
            teamChatBuddyApplication.setparam("entete",header);
            translate("CMD_HEADER", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                existingHistoryArray.put(history1);
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                        }
                    }
                }
            });
        }
        else{
            translatePrompt(header, new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    teamChatBuddyApplication.setparam(teamChatBuddyApplication.getLangue().getNom()+"entete",translatedText);
                    translate("CMD_HEADER", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        }
                    });
                }
            });
        }


    }

    public void CMD_MUSIC(String description) {
        Log.i(TAG, "CMD MUSIC '" + description + "' début.");

        new Thread(() -> {
            //init parameters
            String music_URL = teamChatBuddyApplication.getParamFromFile("Music_URL", configFile);
            String music_endpoint = teamChatBuddyApplication.getParamFromFile("Music_endpoint", configFile);
            String music_model = teamChatBuddyApplication.getParamFromFile("Music_model", configFile);
            int music_duration = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Music_duration", configFile));
            if (music_URL == null || music_URL.isEmpty()) music_URL = "http://34.34.175.122:5000";
            if (music_endpoint == null || music_endpoint.isEmpty())
                music_endpoint = "/generate_music";
            if (music_model == null || music_model.isEmpty()) music_model = "small";
            if (music_duration == 0) music_duration = 20;

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("text", description);
                jsonObject.put("model", music_model);
                jsonObject.put("duration", music_duration);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                String payload = jsonObject.toString();
                URL url = new URL(music_URL + music_endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(50000);
                conn.setReadTimeout(50000);
                conn.setDoOutput(true);

                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                Log.e(TAG, "generateMusic  result :  " + code);
                if (code >= 200 && code < 300) {
                    try {
                        Log.e(TAG, "generateMusic response successful --> save audio file : generated_music.mp3");
                        File directory = new File(teamChatBuddyApplication.getString(R.string.path), "TeamChatBuddy");
                        File outputFile = new File(directory, "generated_music.mp3");
                        try (InputStream in = new BufferedInputStream(conn.getInputStream());
                             FileOutputStream out = new FileOutputStream(outputFile)) {
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = in.read(buf)) != -1) {
                                out.write(buf, 0, n);
                            }
                            out.flush();
                        } catch (Exception e) {
                            translate("CMD_MUSIC", translatedText -> {
                                if (translatedText.contains("No_message_defined")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                } else {
                                    String verifyMessage = verifyCmdMessages(translatedText);
                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                        try {
                                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                            JSONObject history1 = new JSONObject();
                                            history1.put("role", "assistant");
                                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                            existingHistoryArray.put(history1);
                                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                        } catch (JSONException ex) {
                                            ex.printStackTrace();
                                        }
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                        translateSpecificErrors(" problème lors d'enregistrement du fichier", translatedMessage -> {
                                            Log.i(TAG, "Error Translated: " + translatedMessage);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                    translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                        });
                                    }
                                }
                            });
                            e.printStackTrace();
                        }
                        new Handler(Looper.getMainLooper()).post(() -> playMusic(outputFile));
                    } catch (Exception e) {
                        Log.e(TAG, "generateMusic ERROR " + e);
                        e.printStackTrace();
                    }

                } else {
                    String err = "";
                    try (InputStream es = conn.getErrorStream()) {
                        if (es != null) {
                            StringBuilder sb = new StringBuilder();
                            BufferedReader br = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8));
                            String line;
                            while ((line = br.readLine()) != null) sb.append(line);
                            err = sb.toString();
                        }
                    }
                    Log.e(TAG, "generateMusic: HTTP " + code + " error=" + err);
                    if (err != null) {
                        Log.e(TAG, "generateMusic response [not successful] ");
                        try {
                            JSONObject jsonErrorContent = new JSONObject(err);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_MUSIC, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("CMD_MUSIC", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "generateMusic response [not successful]1 catch" + e);
                        }
                    }

                    translate("CMD_MUSIC", translatedText -> {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" problème dans l'API", translatedMessage -> {
                                    Log.i(TAG, "Error Translated: " + translatedMessage);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                            translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                });
                            }
                        }
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "generateMusic onFailure : " + e.getMessage());
                logErrorAPIHealysa("CMD_MUSIC", e.getMessage(), "onFailure");
                e.printStackTrace();
                translate("CMD_MUSIC", translatedText -> {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                existingHistoryArray.put(history1);
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            translateSpecificErrors(" problème dans l'API", translatedMessage -> {
                                Log.i(TAG, "Error Translated: " + translatedMessage);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                        translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                            });
                        }
                    }
                });
            }
        }).start();

    }

    public void CMD_PROMPT(String prompt,int numberOfQuestion){
        Log.e(TAG,prompt+numberOfQuestion);
        if (teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
            prompt = teamChatBuddyApplication.applyFilters(teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties"),prompt);
        }
        translatePrompt(prompt, new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {

                Log.e("MRRM", "translated prompt " + translatedText);
                teamChatBuddyApplication.notifyObservers("ExecuteCMDPROMPT;SPLIT;" + translatedText + ";SPLIT;" + numberOfQuestion);
            }
        });

    }
    public void CMD_DATE(String descritpion){
        Log.i(TAG," cmd_date "+descritpion);
        String format = "EEEE d MMMM yyyy";
        if(!descritpion.equals("")){
            format = descritpion;
        }

        try{
            date = new SimpleDateFormat(format, getCurrentLocale()).format(new Date());
            Log.i(TAG, date);
        }catch(Exception e){
            Log.i(TAG, "Format de date invalide "+e);
            date = new SimpleDateFormat("EEEE d MMMM yyyy", getCurrentLocale()).format(new Date());
        }

        translate("CMD_DATE", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){

                    try {
                        // get the historic commandes :

                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");

                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);

                        JSONObject history1 = new JSONObject();
                        history1.put("role", "assistant");
                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",date));

                        existingHistoryArray.put(history1);
                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",date));
                }
            }
        });
    }
    public void CMD_HEURE(String descritpion){
        Log.i("HHO"," cmd_heure "+descritpion);
        String format = "h:mm";
        if(!descritpion.equals("")){
            format = descritpion;
        }
        try{
            heure = new SimpleDateFormat(format, getCurrentLocale()).format(new Date());
            Log.i(TAG, heure);
        }catch(Exception e){
            Log.i(TAG, "Format d'heure invalide "+e);
            heure = new SimpleDateFormat("h:mm", getCurrentLocale()).format(new Date());
        }
        Log.i(TAG, heure);
        translate("CMD_HOUR", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){

                    try {
                        // get the historic commandes :

                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");


                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);

                        JSONObject history1 = new JSONObject();
                        history1.put("role", "assistant");
                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",heure));

                        existingHistoryArray.put(history1);
                        // Stocker la nouvelle version de l'historique
                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",heure));
                }
            }
        });
    }
    public void CMD_LANGUE(String languageName){
        List<Langue> langues = new ArrayList<>();
        List<String> langueDisponible = teamChatBuddyApplication.getDisponibleLangue();
        for (int i=0;i<langueDisponible.size();i++){
            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam(langueDisponible.get(i)), Langue.class));
        }
        if (langues.isEmpty()){
            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam("Français"), Langue.class));
        }

        boolean containsLanguage = containsLanguageWithName(langues, languageName);
        if (containsLanguage) {
            for(Langue langue : langues ) {
                langue.setChosen(false);
                if (langue.getNom().equalsIgnoreCase(languageName)) {
                    langue.setChosen(true);
                    teamChatBuddyApplication.setLangue(langue);
                }
                if(langue.getNom().equals("Français")){
                    teamChatBuddyApplication.setparam("Français",new Gson().toJson(langue));
                }
                else if(langue.getNom().equals("Anglais")){
                    teamChatBuddyApplication.setparam("Anglais",new Gson().toJson(langue));
                }
                else if (langue.getNom().equals("Espagnol")){
                    teamChatBuddyApplication.setparam("Espagnol",new Gson().toJson(langue));
                }
                else if (langue.getNom().equals("Allemand")){
                    teamChatBuddyApplication.setparam("Allemand",new Gson().toJson(langue));
                }else{
                    teamChatBuddyApplication.setparam(langue.getNom(),new Gson().toJson(langue));
                }
                if (langue.getNom().equalsIgnoreCase(languageName)) {
                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CHANGE_LANGUE");
                }
            }
        }
        else {
            //langue non disponible
        }
    }
    public void CMD_TEMP(String temp){
        teamChatBuddyApplication.setparam( "Temperature_chatgpt", temp );
        translate("CMD_TEMP", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                if (translatedText.contains("No_message_defined")) {
                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                } else {
                    String verifyMessage = verifyCmdMessages(translatedText);
                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                        try {
                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                            JSONObject history1 = new JSONObject();
                            history1.put("role", "assistant");
                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            existingHistoryArray.put(history1);
                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                    }
                }
            }
        });
    }
    public void CMD_BATTERIE(){
        Log.i( TAG, "CMD BATTERIE début." );
        int batLevel = BuddySDK.Sensors.Battery().getBatteryLevel();
        Log.i(TAG, batLevel+"%");
        translate("CMD_BATTERIE", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                    try {
                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                        JSONObject history1 = new JSONObject();
                        history1.put("role", "assistant");
                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",batLevel+""));
                        existingHistoryArray.put(history1);
                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",batLevel+""));
                }
            }
        });
    }
    public void CMD_SOUND(String sound){
        teamChatBuddyApplication.setVolume( Integer.parseInt( sound ), AudioManager.FLAG_SHOW_UI );
        teamChatBuddyApplication.setparam("speak_volume", sound);
        teamChatBuddyApplication.setSpeakVolume( Integer.parseInt( sound ) );
        settingClass.setVolume(sound);
        translate("CMD_SOUND", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                if (translatedText.contains("No_message_defined")) {
                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                } else {
                    String verifyMessage = verifyCmdMessages(translatedText);
                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                        try {
                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                            JSONObject history1 = new JSONObject();
                            history1.put("role", "assistant");
                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            existingHistoryArray.put(history1);
                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                    }
                }
            }
        });
    }

    public void CMD_MOVE(String move){

        teamChatBuddyApplication.notifyObservers("STOP_TRACKING");

        int no_speed = 30;
        int dist = Integer.parseInt( move );

        if(BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
            BuddySDK.USB.enableWheels( true,new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    Log.i( "Motor Wheel", "Wheel motor Enabled" );
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Log.i( "Motor Wheel", "Wheel motor Enabled Failed" );
                }
            } );
        }

        BuddySDK.USB.moveBuddy( no_speed, dist, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String success) throws RemoteException {
                Log.i("No movement", success);
                if(success.equals("WHEEL_MOVE_FINISHED")){
                    translate("CMD_MOVE", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        }
                    });
                    teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
                }
            }

            @Override
            public void onFailed(String error) throws RemoteException {
                Log.e("No movement", error);
                teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
            }
        } );
    }

    public void CMD_TURN(String turn){

        teamChatBuddyApplication.notifyObservers("STOP_TRACKING");

        int no_speed = 100;
        int angle = Integer.parseInt(turn);

        if(BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
            BuddySDK.USB.enableWheels( true,new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    Log.i( "Motor Wheel", "Wheel motor Enabled" );
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Log.i( "Motor Wheel", "Wheel motor Enabled Failed" );
                }
            } );
        }

        BuddySDK.USB.rotateBuddy( no_speed, angle, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String success) throws RemoteException {
                Log.i( "No movement", success );
                if(success.equals("WHEEL_MOVE_FINISHED")){
                    translate("CMD_TURN", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        }
                    });
                    teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
                }
            }

            @Override
            public void onFailed(String error) throws RemoteException {
                Log.e( "No movement", error );
                teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
            }
        } );
    }

    public void CMD_HEAD(String head){

        teamChatBuddyApplication.notifyObservers("STOP_TRACKING");

        int no_speed = 30;
        int angle = Integer.parseInt( head );
        if(BuddySDK.Actuators.getYesStatus().toUpperCase().contains("DISABLE")) {
            BuddySDK.USB.enableYesMove( true, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    Log.i( "Motor Yes", "Yes motor Enabled" );
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Log.i( "Motor Yes", "Yes motor Enabled Failed" );
                }
            } );
        }

        BuddySDK.USB.buddySayYes( no_speed, angle, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String success) throws RemoteException {
                Log.i("No movement", success);
                if(success.equals("YES_MOVE_FINISHED")){
                    translate("CMD_HEAD", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        }
                    });
                    teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
                }
            }

            @Override
            public void onFailed(String error) throws RemoteException {
                Log.e("No movement", error);
                teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
            }
        } );
    }

    public void CMD_STOP(){
        BIPlayer.getInstance().stopBehaviour();
        teamChatBuddyApplication.setQuestionNumber(teamChatBuddyApplication.getQuestionNumber() + 1);
        teamChatBuddyApplication.setActivityClosed(true);
        teamChatBuddyApplication.setSpeaking(false);
        teamChatBuddyApplication.setStartRecording(false);
        teamChatBuddyApplication.notifyObservers("end of timer");
        teamChatBuddyApplication.setShouldLaunchListeningAfterGetingHotWord(false);
        translate("CMD_STOP", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                if (translatedText.contains("No_message_defined")) {
                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                } else {
                    String verifyMessage = verifyCmdMessages(translatedText);
                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                        try {
                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                            JSONObject history1 = new JSONObject();
                            history1.put("role", "assistant");
                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            existingHistoryArray.put(history1);
                            // Stocker la nouvelle version de l'historique
                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                    }
                }
            }
        });
    }

    public void CMD_QUIT(){
        activity.finishAffinity();
        AlertManager.getInstance(activity).stop();
        System.exit(0);
    }
    public void CMD_RUN(String application){
        Missions mission = new Missions();
        Log.e(TAG," trigger  ="+"TeamChatLaunch"+application);
        String packageApp = "";
        String result[]=mission.getTaskForTrigger("TeamChatLaunch"+application);
        if (result!=null) {
            packageApp = result[0];

            Log.e(TAG, " triggerGetFromFile  =" + result[1]);
            if (packageApp != null) {
                if (teamChatBuddyApplication.isAppInstalled(activity, packageApp)) {
                    Log.e(TAG, " package  =" + packageApp);
                    BuddySDK.Companion.raiseEvent(result[1]);
                } else {
                    Log.e(TAG, " application n'existe pas ");
                    translate("CMD_RUN", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", result[1].substring("TeamChatLaunch".length())));
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", result[1].substring("TeamChatLaunch".length())));
                                }
                            }
                        }
                    });
                }
            } else {
                Log.e(TAG, " trigger n'existe pas ");
                translate("CMD_RUN", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", application));
                            }
                        }
                    }
                });
            }
        }
        else {
            Log.e(TAG, " trigger n'existe pas ");
            translate("CMD_RUN", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", application));
                        }
                    }
                }
            });
        }

    }
    public void CMD_SCEN(String scenario){
        if (scenario!=null) {
            Map<String, String> extra = new HashMap<>();
            extra.put("fileName", scenario + ".json"); //(VALUE DOIT ETRE NON NULL)
            BuddySDK.Companion.raiseEvent("LaunchPlayer", extra);
            Log.w(TAG, "raiseEvent(LaunchPlayer," + extra.toString() + ") is called");

        }
        else {
            Log.e(TAG, " scenario null ");
            translate("CMD_SCEN", new ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                existingHistoryArray.put(history1);
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                        }
                    }
                }
            });
        }
    }

    public void CMD_DANCE(){

        //Get biToPlay from configFile
        String biToPlay;
        StringTokenizer st = new StringTokenizer(teamChatBuddyApplication.getParamFromFile("BI_danse", configFile), "/", false);
        List<String> listBehaviour = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String result = st.nextToken();
            listBehaviour.add(result.toLowerCase());
        }
        if (listBehaviour.size() > 0) {
            biToPlay = listBehaviour.get(new Random().nextInt(listBehaviour.size())).trim();
        } else {
            biToPlay = "";
        }
        Log.w(TAG,"Get random from List 'BI_danse' --> "+biToPlay);
        if (!biToPlay.contains(".xml") && !biToPlay.equals("")){
            List<String> biNames = new ArrayList<>();
            File[] biFiles = new File("/storage/emulated/0/BI/Behaviour/").listFiles();
            if (biFiles != null) {
                for (File biFile : biFiles) {
                    if (biFile.isFile() && biFile.getName().toLowerCase().endsWith(".xml")) {
                        if (biFile.getName().toLowerCase().contains(biToPlay.toLowerCase())) {
                            biNames.add(biFile.getName());
                        }
                    }
                }
            }
            if (!biNames.isEmpty()) {
                biToPlay = biNames.get(new Random().nextInt(biNames.size()));
                Log.i(TAG,"BI choisi : " + biToPlay);
            }
        }

        if(biToPlay.isEmpty()){
            Log.i(TAG,"BI introuvable, nous allons utiliser : Dance01.xml");
            biToPlay = "Dance01.xml";
        }

        teamChatBuddyApplication.setBIExecution(true);
        BIPlayer.getInstance().playBI(activity, biToPlay, new IBehaviourCallBack() {
            @Override
            public void onEnd(boolean hasAborted, String reason) {
                teamChatBuddyApplication.setBIExecution(false);
                if(reason.equals("SUCCESS")) {
                    translate("CMD_DANCE", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");

                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        }
                    });
                } else{
                    translate("CMD_DANCE", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ""));
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onRun(String s) {

            }
        });
    }

    public void CMD_METEO(String city){
        Log.e(TAG, "METEO : LA VILLE EST " + city);
        new Thread(() -> {
            try {
                String baseUrl = teamChatBuddyApplication.getParamFromFile("Meteo_URL", "TeamChatBuddy.properties");
                String apiKey = teamChatBuddyApplication.getParamFromFile("Meteo_API_Key", "TeamChatBuddy.properties");
                String urlStr = baseUrl + "/data/2.5/weather?q=" + URLEncoder.encode(city, "UTF-8")
                        + "&appid=" + URLEncoder.encode(apiKey, "UTF-8")
                        + "&lang=fr&units=metric";

                HttpResponse httpResponse = HttpClientUtils.sendGet(urlStr, null, 30000);

                if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300) {
                    Log.i(TAG, "Réponse Météo [successful] :" + httpResponse.body);
                    try {
                        JSONObject jsonObj = new JSONObject(httpResponse.body);
                        JSONArray weatherArray = jsonObj.getJSONArray("weather");
                        JSONObject weatherObject = weatherArray.getJSONObject(0);
                        String description = weatherObject.getString("description");
                        JSONObject tempObject = jsonObj.getJSONObject("main");
                        double temp = tempObject.getDouble("temp");
                        int temperature = (int) Math.ceil(temp);
                        Log.i(TAG, "Réponse Météo : " + "Il fait " + description + " et " + temperature + " °C");

                        if (!teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                            teamChatBuddyApplication.getFrenchLanguageSelectedTranslator().translate(description)
                                    .addOnSuccessListener(translatedText -> {
                                        String descriptionTraduite = translatedText;
                                        Log.e(TAG, "Réponse Météo : " + "Il fait " + descriptionTraduite + " et " + temperature + " °C");
                                        translate("CMD_METEO", translated -> {
                                            String verifyMessage = verifyCmdMessages(translated);
                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                try {
                                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                    JSONObject history1 = new JSONObject();
                                                    history1.put("role", "assistant");
                                                    history1.put("content", translated.split("\\s*/\\s*(?:/\\s*)?")[1]
                                                            .replace("[1]", city)
                                                            .replace("[2]", descriptionTraduite)
                                                            .replace("[3]", Integer.toString(temperature)));
                                                    existingHistoryArray.put(history1);
                                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                Log.i(TAG," notify 2 réponse  ");
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                        translated.split("\\s*/\\s*(?:/\\s*)?")[1]
                                                                .replace("[1]", city)
                                                                .replace("[2]", descriptionTraduite)
                                                                .replace("[3]", Integer.toString(temperature)));
                                            }
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Réponse Météo : " + "Il fait " + "onfailure description traduction" + " et " + temperature + " °C");
                                        translate("CMD_METEO", translated -> {
                                            String verifyMessage = verifyCmdMessages(translated);
                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                        translated.split("\\s*/\\s*(?:/\\s*)?")[1]
                                                                .replace("[1]", city)
                                                                .replace("[2]", description)
                                                                .replace("[3]", Integer.toString(temperature)));
                                            }
                                        });
                                    });
                        } else {
                            translate("CMD_METEO", translated -> {
                                String verifyMessage = verifyCmdMessages(translated);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translated.split("\\s*/\\s*(?:/\\s*)?")[1]
                                                .replace("[1]", city)
                                                .replace("[2]", description)
                                                .replace("[3]", Integer.toString(temperature)));
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                            translated.split("\\s*/\\s*(?:/\\s*)?")[1]
                                                    .replace("[1]", city)
                                                    .replace("[2]", description)
                                                    .replace("[3]", Integer.toString(temperature)));
                                }
                            });
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (httpResponse.body != null) {
                        Log.e(TAG, "Réponse Météo [not successful]");
                        try {
                            JSONObject jsonErrorContent = new JSONObject(httpResponse.body);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_METEO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("CMD_METEO", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse Météo [not successful]1 catch" + e);
                        }
                    }
                    translate("CMD_METEO", translated -> {
                        String verifyMessage = verifyCmdMessages(translated);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translated.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                existingHistoryArray.put(history1);
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                    translated.split("\\s*/\\s*(?:/\\s*)?")[2]);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception pendant la récupération de la réponse Météo : " + e);
                translate("CMD_METEO", translated -> {
                    String verifyMessage = verifyCmdMessages(translated);
                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                        try {
                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                            JSONObject history1 = new JSONObject();
                            history1.put("role", "assistant");
                            history1.put("content", translated.split("\\s*/\\s*(?:/\\s*)?")[2]);
                            existingHistoryArray.put(history1);
                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                        } catch (JSONException ex) {
                            ex.printStackTrace();
                        }
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                translated.split("\\s*/\\s*(?:/\\s*)?")[2]);
                    }
                });
            }
        }).start();
    }

    public void CMD_RADIO(String radio){
        final String[] accessToken = new String[1];
        Log.e(TAG, "RADIO : LA RADIO EST " + radio);
        new Thread(() -> {
            try {
                // 1. Récupérer le token d'accès (POST)
                String radioUrl = teamChatBuddyApplication.getParamFromFile("Radio_URL", "TeamChatBuddy.properties");
                String csrfToken = teamChatBuddyApplication.getParamFromFile("Radio_XCSRF_Token", "TeamChatBuddy.properties");
                String tokenUrl = radioUrl + "/Pillow/session/v2/session/create";
                JSONObject jsonParams = new JSONObject();
                jsonParams.put("client_id", "rjnefu93CdsUX4xXPH7P7RmcCbYMbFRHWiCTCtbg");
                jsonParams.put("client_key", "cuEkLJWVWp9Ee4oFvL9nfWdqWvwXddbaYWAbpWJd34FAEvy4fAUjUzaLCeUbJooE9dwf4bePg4qFsyps9HtwruNnaqhWv9KjKHnqvENALTmqMqJzrefNWyeVhRkqkvxK");
                jsonParams.put("device_serial", "device_unique_serial_number");

                Map<String, String> headersToken = new HashMap<>();
                headersToken.put("X-CSRFTOKEN", csrfToken);
                headersToken.put("Content-Type", "application/json; charset=utf-8");

                HttpResponse tokenResponse = HttpClientUtils.sendPost(tokenUrl, jsonParams.toString(), headersToken, 50000);

                if (tokenResponse.responseCode >= 200 && tokenResponse.responseCode < 300 && tokenResponse.body != null) {
                    JSONObject jsonObj = new JSONObject(tokenResponse.body);
                    JSONObject dataObject = jsonObj.getJSONObject("data");
                    JSONObject accessTokenObject = dataObject.getJSONObject("access_token");
                    accessToken[0] = accessTokenObject.getString("key");
                    Log.i(TAG, "Réponse Radio Token : Token : " + accessToken[0]);
                    Log.i(TAG, "Radio : " + radio);

                    // 2. Recherche du nom de la radio (GET)
                    String radioNameUrl = radioUrl + "/Pillow/search?page=1&pageSize=30&query=" + URLEncoder.encode(radio.trim(), "UTF-8");
                    Map<String, String> headersRadioName = new HashMap<>();
                    headersRadioName.put("Authorization", "Bearer " + accessToken[0]);

                    HttpResponse radioNameResponse = HttpClientUtils.sendGet(radioNameUrl, headersRadioName, 50000);

                    if (radioNameResponse.responseCode >= 200 && radioNameResponse.responseCode < 300 && radioNameResponse.body != null) {
                        JSONObject jsonObject = new JSONObject(radioNameResponse.body);
                        JSONObject bodyObject = jsonObject.getJSONObject("body");
                        JSONArray contentObject = bodyObject.getJSONArray("content");
                        JSONObject contentArray = contentObject.getJSONObject(0);
                        String permalink = contentArray.getString("permalink");
                        Log.i(TAG, "Radio Name : " + permalink);

                        // 3. Récupérer le flux radio (GET)
                        String radioPlayUrl = radioUrl + "/Pillow/radios/" + permalink.split("/")[1] + "/play";
                        Map<String, String> headersRadio = new HashMap<>();
                        headersRadio.put("Authorization", "Bearer " + accessToken[0]);

                        HttpResponse radioResponse = HttpClientUtils.sendGet(radioPlayUrl, headersRadio, 50000);

                        if (radioResponse.responseCode >= 200 && radioResponse.responseCode < 300 && radioResponse.body != null) {
                            JSONObject radioJson = new JSONObject(radioResponse.body);
                            JSONObject bodyObj = radioJson.getJSONObject("body");
                            JSONObject contentObj = bodyObj.getJSONObject("content");
                            JSONArray streamsArray = contentObj.getJSONArray("streams");
                            JSONObject streamsObject = streamsArray.getJSONObject(0);
                            String url = streamsObject.getString("url");
                            Log.i(TAG, "Réponse Radio : URL : " + url);
                            String finalUrl = url;
                            mainHandler.post(() -> playRadio(finalUrl));
                        } else {
                            // Gestion erreur HTTP radioResponse
                            if (radioResponse.body != null) {
                                try {
                                    JSONObject jsonErrorContent = new JSONObject(radioResponse.body);
                                    String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_RADIO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                    logErrorAPIHealysa("CMD_RADIO", errorTXT, "notOnFailure");
                                    translate("CMD_RADIO", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            if (translatedText.contains("No_message_defined")) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                            } else {
                                                String verifyMessage = verifyCmdMessages(translatedText);
                                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                    try {
                                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                        JSONObject history1 = new JSONObject();
                                                        history1.put("role", "assistant");
                                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                        existingHistoryArray.put(history1);
                                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                    translateSpecificErrors(" erreur lors de la récupération du flux de la radio ", new ITranslationCallback() {
                                                        @Override
                                                        public void onTranslated(String translatedMessage) {
                                                            Log.i(TAG, "Error Translated: " + translatedMessage);
                                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", ": " + translatedMessage));
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e(TAG, "Réponse Radio [not successful]1 catch" + e);
                                }
                            }
                        }
                    } else {
                        // Gestion erreur HTTP radioNameResponse
                        if (radioNameResponse.body != null) {
                            try {
                                JSONObject jsonErrorContent = new JSONObject(radioNameResponse.body);
                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_RADIO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("CMD_RADIO", errorTXT, "notOnFailure");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        translate("CMD_RADIO", new ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedText) {
                                if (translatedText.contains("No_message_defined")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                } else {
                                    String verifyMessage = verifyCmdMessages(translatedText);
                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                        try {
                                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                            JSONObject history1 = new JSONObject();
                                            history1.put("role", "assistant");
                                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                            existingHistoryArray.put(history1);
                                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                        translateSpecificErrors(" erreur lors de la récupération du nom de la radio  ", new ITranslationCallback() {
                                            @Override
                                            public void onTranslated(String translatedMessage) {
                                                Log.i(TAG, "Error Translated: " + translatedMessage);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", ": " + translatedMessage));
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    }
                } else {
                    // Gestion erreur HTTP tokenResponse
                    if (tokenResponse.body != null) {
                        try {
                            JSONObject jsonErrorContent = new JSONObject(tokenResponse.body);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_RADIO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("CMD_RADIO", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    translate("CMD_RADIO", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    translateSpecificErrors(" erreur lors de la génération du token ", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedMessage) {
                                            Log.i(TAG, "Error Translated: " + translatedMessage);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", ": " + translatedMessage));
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception pendant la récupération de la réponse Radio : " + e);
                logErrorAPIHealysa("CMD_RADIO", e.getMessage(), "onFailure");
                translate("CMD_RADIO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException ex) {
                                    ex.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" erreur lors de la génération du token ", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage);
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", ": " + translatedMessage));
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }).start();
    }

    public void CMD_BI(String bi){
        Log.e("TeamChat_BIPlayer", "BI : Le BI EST " + bi);
        final String[] biName = {""};
        teamChatBuddyApplication.setBIExecution(true);
        getTheRightBINameFromConfigFile(bi, new BINameCallback() {
            @Override
            public void onResult(String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result.equals("ERROR")){
                            biName[0] = bi;
                        }
                        else {
                            biName[0] = result;
                        }
                        Log.e("TeamChat_BIPlayer", "BI : Le BI envoyé à la fonction playBI EST " + biName[0]);
                        BIPlayer.getInstance().playBI(activity, biName[0], new IBehaviourCallBack() {
                            @Override
                            public void onEnd(boolean hasAborted, String reason) {
                                teamChatBuddyApplication.setBIExecution(false);
                                if(reason.equals("SUCCESS")) {
                                    translate("CMD_BI", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            if (translatedText.contains("No_message_defined")) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                            } else {
                                                String verifyMessage = verifyCmdMessages(translatedText);
                                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                    try {
                                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                        JSONObject history1 = new JSONObject();
                                                        history1.put("role", "assistant");
                                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                        existingHistoryArray.put(history1);
                                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                }
                                            }
                                        }
                                    });
                                }
                                else if (reason.equals("ERROR_TASK")){
                                    translate("CMD_BI", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            if (translatedText.contains("No_message_defined")) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            } else {
                                                String verifyMessage = verifyCmdMessages(translatedText);
                                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ""));
                                                }
                                            }
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onRun(String s) {

                            }
                        });
                    }
                });
            }
        });


    }
    public interface BINameCallback {
        void onResult(String result);
    }
    private void getTheRightBINameFromConfigFile(String biName,BINameCallback biNameCallback){
        final String[] nomBIAfterTraduction = {""};
        // Chemin du dossier dans le stockage externe
        String dossierExterne = activity.getString(R.string.path) + "/BI/Behaviour";
        // Chemin complet du fichier
        String cheminFichier = dossierExterne + "/" + biName;

        // Création d'un objet File avec le chemin du fichier
        File fichier = new File(cheminFichier);
        if (!biName.contains(".xml") && !biName.equals("")){

            // Récupération des noms de fichiers XML correspondant à la catégorie recherchée
            List<String> nomsFichiers = getFilenamesForCategory(dossierExterne, biName.trim());

            // Choix aléatoire d'un nom de fichier parmi ceux trouvés
            if (!nomsFichiers.isEmpty()) {
                biName = nomsFichiers.get(new Random().nextInt(nomsFichiers.size()));
                Log.e("TeamChat_BIPlayer","Nom du fichier choisi : " + biName);
                biNameCallback.onResult(biName);
            } else {
                Log.e("TeamChat_BIPlayer","Aucun fichier trouvé pour la catégorie : " + biName);

                teamChatBuddyApplication.getLanguageSelectedEnglishTranslator().translate(biName)
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                Log.e("TeamChat_BIPlayer","Nom du fichier traduit: " + translatedText);
                                List<String> nomsFichiers1 = getFilenamesForCategory(dossierExterne, translatedText.trim());
                                if (!nomsFichiers1.isEmpty()) {
                                    translatedText = nomsFichiers1.get(new Random().nextInt(nomsFichiers1.size()));
                                    nomBIAfterTraduction[0] =translatedText;
                                    Log.e("TeamChat_BIPlayer","Nom du fichier choisi après traduction: " + translatedText);
                                    biNameCallback.onResult(translatedText);
                                }
                                else {
                                    Log.e("TeamChat_BIPlayer","Aucun fichier trouvé après traduction pour la catégorie : " + translatedText);
                                    biNameCallback.onResult(translatedText);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                biNameCallback.onResult("ERROR");
                            }
                        });
            }
        }
        else  {
            biNameCallback.onResult(biName);
        }
    }

    private static List<String> getFilenamesForCategory(String dossier, String categorieRecherchee) {
        List<String> nomsFichiers = new ArrayList<>();
        File dossierBehaviours = new File(dossier);
        Log.e("TeamChat_BIPlayer","Check file NAME : categorieRecherchee.replaceAll(\"\\\\s+\", \"\").toLowerCase()) ="+categorieRecherchee.replaceAll("\\s+", "").toLowerCase() );
        // Liste des fichiers dans le dossier
        File[] fichiers = dossierBehaviours.listFiles();

        if (fichiers != null) {
            // Parcours des fichiers
            for (File fichier : fichiers) {
                if (fichier.isFile() && fichier.getName().toLowerCase().endsWith(".xml")) {
                    // Ajout du nom du fichier si la catégorie recherchée est présente dans le nom du fichier
                    if (fichier.getName().toLowerCase().contains(categorieRecherchee.replaceAll("\\s+", "").toLowerCase())) {
                        nomsFichiers.add(fichier.getName());
                    }
                }
            }
        }
        return nomsFichiers;
    }

    public void HEALYSA_CONNECT(String utilisateur) {
        final String TAG = "Commande";
        new Thread(() -> {
            try {
                // 1. Authentification (POST)
                String baseUrl = teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD", configFile);
                JSONObject jsonParams = new JSONObject();
                jsonParams.put("email", teamChatBuddyApplication.getParamFromFile("Healysa_mail", configFile));
                jsonParams.put("password", teamChatBuddyApplication.getParamFromFile("Healysa_password", configFile));

                Map<String, String> headersCmd = new HashMap<>();
                headersCmd.put("Content-Type", "application/json; charset=utf-8");

                HttpResponse authResponse = HttpClientUtils.sendPost(
                        baseUrl + "publicApi/auth/login",
                        jsonParams.toString(),
                        headersCmd,
                        30000
                );

                if (authResponse.responseCode >= 200 && authResponse.responseCode < 300 && authResponse.body != null) {
                    Log.i(TAG, "Réponse Auth Healysa [successful] :" + authResponse.body);
                    JSONObject jsonObj = new JSONObject(authResponse.body);
                    Log.i(TAG, "Réponse Auth Healysa [successful] token :" + jsonObj.getString("token"));
                    teamChatBuddyApplication.setTokenHealysa(jsonObj.getString("token"));

                    // 2. Récupération IMEI (GET)
                    Map<String, String> headersGetIMEI = new HashMap<>();
                    headersGetIMEI.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                    headersGetIMEI.put("Content-Type", "application/json; charset=utf-8");

                    HttpResponse imeiResponse = HttpClientUtils.sendGet(
                            baseUrl + "api/users/account",
                            headersGetIMEI,
                            30000
                    );

                    if (imeiResponse.responseCode >= 200 && imeiResponse.responseCode < 300 && imeiResponse.body != null) {
                        try {
                            JSONObject jsonObjIMEI = new JSONObject(imeiResponse.body);
                            Log.i(TAG, " devices : " + imeiResponse.body);
                            JSONArray devices = jsonObjIMEI.getJSONArray("devices");
                            Boolean findConsumer = false;
                            for (int i = 0; i < devices.length(); i++) {
                                Log.i(TAG, "Réponse Consumer Firstname Healysa [successful] :" + devices.getJSONObject(i).getJSONObject("consumer").getString("firstname"));
                                if (devices.getJSONObject(i).getJSONObject("consumer").getString("firstname").toLowerCase().equals(utilisateur.toLowerCase())) {
                                    findConsumer = true;
                                    teamChatBuddyApplication.setImeiDevice(devices.getJSONObject(i).getString("imei"));
                                    Log.i(TAG, "Réponse Imei Healysa [successful] :" + teamChatBuddyApplication.getImeiDevice());
                                    translate("HEALYSA_CONNECT", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            if (translatedText.contains("No_message_defined")) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            } else {
                                                String verifyMessage = verifyCmdMessages(translatedText);
                                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                    try {
                                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                        JSONObject history1 = new JSONObject();
                                                        history1.put("role", "assistant");
                                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                        existingHistoryArray.put(history1);
                                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                            if (!findConsumer) {
                                logErrorAPIHealysa("HEALYSA_CONNECT", "Consumer not found", "onFailure");
                                translate("HEALYSA_CONNECT", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        if (translatedText.contains("No_message_defined")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                        } else {
                                            String verifyMessage = verifyCmdMessages(translatedText);
                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ""));
                                            }
                                        }
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (imeiResponse.body != null) {
                            Log.e(TAG, "Réponse Imei Healysa [not successful] ");
                            try {
                                JSONObject jsonErrorContent = new JSONObject(imeiResponse.body);
                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_CONNECT, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("HEALYSA_CONNECT", errorTXT, "notOnFailure");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse Imei Healysa [not successful]1 catch" + e);
                            }
                            translate("HEALYSA_CONNECT", new ITranslationCallback() {
                                @Override
                                public void onTranslated(String translatedText) {
                                    if (translatedText.contains("No_message_defined")) {
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    } else {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            translateSpecificErrors(" problème dans l'api de récuperation de l'IMEI", new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedMessage) {
                                                    Log.i(TAG, "Error Translated: " + translatedMessage);
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        }
                        Log.e(TAG, "Réponse Imei Healysa [not successful]");
                    }
                } else {
                    Log.e(TAG, "Réponse Auth Healysa [not successful]");
                    if (authResponse.body != null) {
                        Log.e(TAG, "Réponse Auth Healysa  [not successful] ");
                        try {
                            JSONObject jsonErrorContent = new JSONObject(authResponse.body);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_CONNECT, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_CONNECT", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse Auth Healysa  [not successful]1 catch" + e);
                        }
                        translate("HEALYSA_CONNECT", new ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedText) {
                                if (translatedText.contains("No_message_defined")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                } else {
                                    String verifyMessage = verifyCmdMessages(translatedText);
                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ""));
                                    }
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception pendant la récupération de la réponse Healysa : " + e);
                logErrorAPIHealysa("HEALYSA_CONNECT", e.getMessage(), "onFailure");
                translate("HEALYSA_CONNECT", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ""));
                            }
                        }
                    }
                });
            }
        }).start();
    }

    public void HEALYSA_HRV() {
        final String TAG = "Commande";
        new Thread(() -> {
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());//
                Map<String, String> headersCmd = new HashMap<>();
                headersCmd.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                headersCmd.put("Content-Type", "application/json; charset=utf-8");

                String baseUrl = teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD", configFile);
                String imei = teamChatBuddyApplication.getImeiDevice();
                String product = "silver";
                String cmd = "IWBPXL," + imei + ",080835#";

                String urlStr = baseUrl + "api/deviceMessageProcessor/test/cmd"
                        + "?product=" + URLEncoder.encode(product, "UTF-8")
                        + "&imei=" + URLEncoder.encode(imei, "UTF-8")
                        + "&cmd=" + URLEncoder.encode(cmd, "UTF-8");
                HttpResponse cmdResponse = HttpClientUtils.sendPost(
                        urlStr,
                        "", // corps vide
                        headersCmd,
                        30000
                );

                if (cmdResponse.responseCode >= 200 && cmdResponse.responseCode < 300 && cmdResponse.body != null) {
                    Log.i(TAG, "Réponse Fréquence Cardiaque Healysa [successful] :" + cmdResponse.body);
                    SystemClock.sleep(35000);
                    Log.i(TAG, "Réponse Fréquence Cardiaque Healysa [successful] : 30 secondes plus tard");

                    // 2. Récupérer la donnée HRV (GET)
                    Map<String, String> headersGet = new HashMap<>();
                    headersGet.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());

                    String start = date + "T00:00:00.000Z";
                    String end = date + "T23:59:59.000Z";

                    String urlGet = baseUrl + "api/devicesData/chart?"
                            + "deviceImei=" + URLEncoder.encode(imei, "UTF-8")
                            + "&dateDebut=" + URLEncoder.encode(start, "UTF-8")
                            + "&dateFin=" + URLEncoder.encode(end, "UTF-8")
                            + "&dataType=" + URLEncoder.encode("HEART_RATE", "UTF-8")
                            + "&jsmFiltre=" + URLEncoder.encode("day", "UTF-8");

                    HttpResponse getHRResponse = HttpClientUtils.sendGet(urlGet, headersGet, 30000);

                    if (getHRResponse.responseCode >= 200 && getHRResponse.responseCode < 300 && getHRResponse.body != null) {
                        Log.i(TAG, "Réponse GET Fréquence Cardiaque Healysa [successful] :" + getHRResponse.body);
                        try {
                            JSONObject reponse = new JSONObject(getHRResponse.body);
                            if (reponse.has("HEART_RATE")) {
                                JSONArray array = new JSONArray(reponse.getString("HEART_RATE"));
                                JSONObject data = array.getJSONObject(array.length() - 1);
                                heart_rate = data.getString("dataValue");
                                heart_rate = heart_rate.replace(".", ",");
                                String dateStr = data.getString("date");
                                String hour = outputFormat.format(inputFormat.parse(dateStr));
                                Log.i(TAG, "Réponse GET Fréquence Cardiaque Healysa [successful] : " + heart_rate);
                                translate("HEALYSA_HRV", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            try {
                                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                JSONObject history1 = new JSONObject();
                                                history1.put("role", "assistant");
                                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", heart_rate));
                                                existingHistoryArray.put(history1);
                                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", heart_rate).replace("[2]", hour));
                                        }
                                    }
                                });
                            } else {
                                translate("HEALYSA_HRV", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                        }
                                    }
                                });
                            }
                        } catch (JSONException | ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (getHRResponse.body != null) {
                            Log.e(TAG, "Réponse Imei Healysa [not successful] response code " + getHRResponse.responseCode + " response.body " + getHRResponse.body);
                            try {
                                JSONObject jsonErrorContent = new JSONObject(getHRResponse.body);
                                Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 jsonErrorContent " + jsonErrorContent);
                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_HRV, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("HEALYSA_HRV", errorTXT, "notOnFailure");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 catch" + e);
                            }
                        }
                        Log.e(TAG, "Réponse GET Fréquence Cardiaque Healysa [not successful]");
                    }
                } else {
                    String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                    Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]");
                    if (cmdResponse.body != null) {
                        Log.e(TAG, "Réponse Imei Healysa [not successful] response code " + cmdResponse.responseCode + " response.body " + cmdResponse.body);
                        try {
                            JSONObject jsonErrorContent = new JSONObject(cmdResponse.body);
                            Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 jsonErrorContent " + jsonErrorContent);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_HRV, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_HRV", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 catch" + e);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful] :" + e);
                logErrorAPIHealysa("HEALYSA_HRV", e.getMessage(), "onFailure");
            }
        }).start();
    }

    public void HEALYSA_BLOODP() {
        final String TAG = "Commande";
        new Thread(() -> {
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

                Log.i(TAG, "Tension Healysa imei:" + teamChatBuddyApplication.getImeiDevice());
                Log.i(TAG, "Tension Healysa token :" + teamChatBuddyApplication.getTokenHealysa());

                JSONObject cmdBody = new JSONObject();
                cmdBody.put("cmd", "IWBPXY," + teamChatBuddyApplication.getImeiDevice() + ",080835#");

                Map<String, String> headersCmd = new HashMap<>();
                headersCmd.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                headersCmd.put("Content-Type", "application/json; charset=utf-8");

                String baseUrl = teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD", configFile);
                String imei = teamChatBuddyApplication.getImeiDevice();
                String product = "silver";
                String cmd = "IWBPXL," + imei + ",080835#";

                String urlStr = baseUrl + "api/deviceMessageProcessor/test/cmd"
                        + "?product=" + URLEncoder.encode(product, "UTF-8")
                        + "&imei=" + URLEncoder.encode(imei, "UTF-8")
                        + "&cmd=" + URLEncoder.encode(cmd, "UTF-8");

                HttpResponse cmdResponse = HttpClientUtils.sendPost(
                        urlStr,
                        "", // corps vide
                        headersCmd,
                        30000
                );

                if (cmdResponse.responseCode >= 200 && cmdResponse.responseCode < 300 && cmdResponse.body != null) {
                    Log.i(TAG, "Réponse Tension Healysa [successful] :" + cmdResponse.body);
                    SystemClock.sleep(35000);
                    Log.i(TAG, "Réponse Tension Healysa [successful] : 30 secondes plus tard");


                    Map<String, String> headersGet = new HashMap<>();
                    headersGet.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());

                    String start = date + "T00:00:00.000Z";
                    String end = date + "T23:59:59.000Z";

                    String urlTensionS = baseUrl + "api/devicesData/chart?"
                            + "deviceImei=" + URLEncoder.encode(imei, "UTF-8")
                            + "&dateDebut=" + URLEncoder.encode(start, "UTF-8")
                            + "&dateFin=" + URLEncoder.encode(end, "UTF-8")
                            + "&dataType=" + URLEncoder.encode("BLOOD_PRESSURE_SYSTOLIC", "UTF-8")
                            + "&jsmFiltre=" + URLEncoder.encode("day", "UTF-8");

                    HttpResponse getTensionSResponse = HttpClientUtils.sendGet(urlTensionS, headersGet, 30000);

                    if (getTensionSResponse.responseCode >= 200 && getTensionSResponse.responseCode < 300 && getTensionSResponse.body != null) {
                        Log.i(TAG, "Réponse GET Tension Healysa [successful] :" + getTensionSResponse.body);
                        try {
                            JSONObject reponse = new JSONObject(getTensionSResponse.body);
                            if (reponse.has("BLOOD_PRESSURE_SYSTOLIC")) {
                                JSONArray array = new JSONArray(reponse.getString("BLOOD_PRESSURE_SYSTOLIC"));
                                JSONObject data = array.getJSONObject(array.length() - 1);
                                tensionS = data.getString("dataValue");
                                tensionS = tensionS.replace(".", ",");
                                String dateTension = data.getString("date");
                                String hour = outputFormat.format(inputFormat.parse(dateTension));
                                Log.i(TAG, "Réponse GET Tension Healysa [successful] : " + tensionS);

                                String urlTensionD = baseUrl + "api/devicesData/chart?"
                                        + "deviceImei=" + URLEncoder.encode(imei, "UTF-8")
                                        + "&dateDebut=" + URLEncoder.encode(start, "UTF-8")
                                        + "&dateFin=" + URLEncoder.encode(end, "UTF-8")
                                        + "&dataType=" + URLEncoder.encode("BLOOD_PRESSURE_DIASTOLIC", "UTF-8")
                                        + "&jsmFiltre=" + URLEncoder.encode("day", "UTF-8");

                                HttpResponse getTensionDResponse = HttpClientUtils.sendGet(urlTensionD, headersGet, 30000);

                                if (getTensionDResponse.responseCode >= 200 && getTensionDResponse.responseCode < 300 && getTensionDResponse.body != null) {
                                    Log.i(TAG, "Réponse GET Tension Healysa [successful] :" + getTensionDResponse.body);
                                    try {
                                        JSONObject reponseD = new JSONObject(getTensionDResponse.body);
                                        if (reponseD.has("BLOOD_PRESSURE_DIASTOLIC")) {
                                            JSONArray arrayD = new JSONArray(reponseD.getString("BLOOD_PRESSURE_DIASTOLIC"));
                                            JSONObject dataD = arrayD.getJSONObject(arrayD.length() - 1);
                                            tensionD = dataD.getString("dataValue");
                                            tensionD = tensionD.replace(".", ",");
                                            Log.i(TAG, "Réponse GET Tension Healysa [successful] : " + tensionD);

                                            Log.i(TAG, "Réponse GET Tension Healysa [successful] : " + tensionS + tensionD);
                                            translate("HEALYSA_BLOODP", new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedText) {
                                                    String verifyMessage = verifyCmdMessages(translatedText);
                                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                        try {
                                                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                            JSONObject history1 = new JSONObject();
                                                            history1.put("role", "assistant");
                                                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", tensionS).replace("[2]", tensionD));
                                                            existingHistoryArray.put(history1);
                                                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", tensionS).replace("[2]", tensionD).replace("[3]", hour));
                                                    }
                                                }
                                            });
                                        } else {
                                            translate("HEALYSA_BLOODP", new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedText) {
                                                    String verifyMessage = verifyCmdMessages(translatedText);
                                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                    }
                                                }
                                            });
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Log.i(TAG, "Réponse GET Tension Healysa [not successful] : " + (getTensionDResponse.body != null ? getTensionDResponse.body : getTensionDResponse.responseCode));
                                }
                            } else {
                                translate("HEALYSA_BLOODP", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                        }
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        if (getTensionSResponse.body != null) {
                            Log.e(TAG, "Réponse Tension Healysa [not successful]");
                            try {
                                JSONObject jsonErrorContent = new JSONObject(getTensionSResponse.body);
                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_BLOODP, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("HEALYSA_BLOODP", errorTXT, "notOnFailure");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse Tension Healysa[not successful]1 catch" + e);
                            }
                        }
                        Log.e(TAG, "Réponse Tension Healysa [not successful]");
                    }
                } else {
                    String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                    if (cmdResponse.body != null) {
                        Log.e(TAG, "Réponse Tension Healysa [not successful]");
                        try {
                            JSONObject jsonErrorContent = new JSONObject(cmdResponse.body);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_BLOODP, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_BLOODP", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse Tension Healysa[not successful]1 catch" + e);
                        }
                    }
                    Log.e(TAG, "Réponse Tension Healysa [not successful]");
                }
            } catch (Exception e) {
                Log.e(TAG, "Réponse Tension Healysa [not successful] :" + e);
                logErrorAPIHealysa("HEALYSA_BLOODP", e.getMessage(), "onFailure");
            }
        }).start();
    }

    public void HEALYSA_SPO2() {
        final String TAG = "Commande";
        new Thread(() -> {
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                JSONObject cmdBody = new JSONObject();
                cmdBody.put("cmd", "IWBPXY," + teamChatBuddyApplication.getImeiDevice() + ",080835#");

                Map<String, String> headersCmd = new HashMap<>();
                headersCmd.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                headersCmd.put("Content-Type", "application/json; charset=utf-8");

                String baseUrl = teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD", configFile);
                String imei = teamChatBuddyApplication.getImeiDevice();
                String product = "silver";
                String cmd = "IWBPXL," + imei + ",080835#";

                String urlStr = baseUrl + "api/deviceMessageProcessor/test/cmd"
                        + "?product=" + URLEncoder.encode(product, "UTF-8")
                        + "&imei=" + URLEncoder.encode(imei, "UTF-8")
                        + "&cmd=" + URLEncoder.encode(cmd, "UTF-8");

                HttpResponse cmdResponse = HttpClientUtils.sendPost(
                        urlStr,
                        "", // corps vide
                        headersCmd,
                        30000
                );

                if (cmdResponse.responseCode >= 200 && cmdResponse.responseCode < 300 && cmdResponse.body != null) {
                    Log.i(TAG, "Réponse SPO2 Healysa [successful] :" + cmdResponse.body);
                    SystemClock.sleep(35000);
                    Log.i(TAG, "Réponse SPO2 Healysa [successful] : 30 secondes plus tard");

                    Map<String, String> headersGet = new HashMap<>();
                    headersGet.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());

                    String start = date + "T00:00:00.000Z";
                    String end = date + "T23:59:59.000Z";

                    String urlGet = baseUrl + "api/devicesData/chart?"
                            + "deviceImei=" + URLEncoder.encode(imei, "UTF-8")
                            + "&dateDebut=" + URLEncoder.encode(start, "UTF-8")
                            + "&dateFin=" + URLEncoder.encode(end, "UTF-8")
                            + "&dataType=" + URLEncoder.encode("SPO2", "UTF-8")
                            + "&jsmFiltre=" + URLEncoder.encode("day", "UTF-8");

                    HttpResponse getHRResponse = HttpClientUtils.sendGet(urlGet, headersGet, 30000);

                    if (getHRResponse.responseCode >= 200 && getHRResponse.responseCode < 300 && getHRResponse.body != null) {
                        Log.i(TAG, "Réponse GET SPO2 Healysa [successful] :" + getHRResponse.body);
                        try {
                            JSONObject reponse = new JSONObject(getHRResponse.body);
                            if (reponse.has("SPO2")) {
                                JSONArray array = new JSONArray(reponse.getString("SPO2"));
                                JSONObject data = array.getJSONObject(array.length() - 1);
                                spo2 = data.getString("dataValue");
                                spo2 = spo2.replace(".", ",");
                                String dateStr = data.getString("date");
                                String hour = outputFormat.format(inputFormat.parse(dateStr));
                                Log.i(TAG, "Réponse GET SPO2 Healysa [successful] : " + spo2);
                                translate("HEALYSA_SPO2", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            try {
                                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                JSONObject history1 = new JSONObject();
                                                history1.put("role", "assistant");
                                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", spo2));
                                                existingHistoryArray.put(history1);
                                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", spo2).replace("[2]", hour));
                                        }
                                    }
                                });
                            } else {
                                translate("HEALYSA_SPO2", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                        }
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        if (getHRResponse.body != null) {
                            Log.e(TAG, "Réponse SPO2 Healysa [not successful]");
                            try {
                                JSONObject jsonErrorContent = new JSONObject(getHRResponse.body);
                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_SPO2, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("HEALYSA_SPO2", errorTXT, "notOnFailure");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse SPO2 Healysa [not successful]1 catch" + e);
                            }
                        }
                        Log.e(TAG, "Réponse SPO2 Healysa [not successful]");
                    }
                } else {
                    String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                    if (cmdResponse.body != null) {
                        Log.e(TAG, "Réponse SPO2 Healysa [not successful]");
                        try {
                            JSONObject jsonErrorContent = new JSONObject(cmdResponse.body);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_SPO2, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_SPO2", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse SPO2 Healysa [not successful]1 catch" + e);
                        }
                    }
                    Log.e(TAG, "Réponse SPO2 Healysa [not successful]");
                }
            } catch (Exception e) {
                Log.e(TAG, "Réponse SPO2 Healysa [not successful] :" + e);
                logErrorAPIHealysa("HEALYSA_SPO2", e.getMessage(), "onFailure");
            }
        }).start();
    }

    public void HEALYSA_CHECKUP() {
        final String TAG = "Commande";
        new Thread(() -> {
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

                Map<String, String> headersCmd = new HashMap<>();
                headersCmd.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                headersCmd.put("Content-Type", "application/json; charset=utf-8");

                String baseUrl = teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD", configFile);
                String imei = teamChatBuddyApplication.getImeiDevice();
                String product = "silver";
                String cmd = "IWBPXL," + imei + ",080835#";

                String urlStr = baseUrl + "api/deviceMessageProcessor/test/cmd"
                        + "?product=" + URLEncoder.encode(product, "UTF-8")
                        + "&imei=" + URLEncoder.encode(imei, "UTF-8")
                        + "&cmd=" + URLEncoder.encode(cmd, "UTF-8");
                HttpResponse cmdResponse = HttpClientUtils.sendPost(
                        urlStr,
                        "", // corps vide
                        headersCmd,
                        30000
                );

                if (cmdResponse.responseCode >= 200 && cmdResponse.responseCode < 300 && cmdResponse.body != null) {
                    Log.i(TAG, "Réponse SANTE Healysa [successful] :" + cmdResponse.body);
                    SystemClock.sleep(35000);
                    Log.i(TAG, "Réponse SANTE Healysa [successful] : 30 secondes plus tard");

                    // 2. Récupérer la donnée HEART_RATE (GET)
                    Map<String, String> headersGet = new HashMap<>();
                    headersGet.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());

                    String start = date + "T00:00:00.000Z";
                    String end = date + "T23:59:59.000Z";

                    String urlHR = baseUrl + "api/devicesData/chart?"
                            + "deviceImei=" + URLEncoder.encode(imei, "UTF-8")
                            + "&dateDebut=" + URLEncoder.encode(start, "UTF-8")
                            + "&dateFin=" + URLEncoder.encode(end, "UTF-8")
                            + "&dataType=" + URLEncoder.encode("HEART_RATE", "UTF-8")
                            + "&jsmFiltre=" + URLEncoder.encode("day", "UTF-8");

                    HttpResponse getHRResponse = HttpClientUtils.sendGet(urlHR, headersGet, 30000);

                    if (getHRResponse.responseCode >= 200 && getHRResponse.responseCode < 300 && getHRResponse.body != null) {
                        Log.i(TAG, "Réponse GET Fréquence Cardiaque Healysa [successful] :" + getHRResponse.body);
                        try {
                            JSONObject reponse = new JSONObject(getHRResponse.body);
                            if (reponse.has("HEART_RATE")) {
                                JSONArray array = new JSONArray(reponse.getString("HEART_RATE"));
                                JSONObject data = array.getJSONObject(0);
                                heart_rate = data.getString("dataValue");
                                heart_rate = heart_rate.replace(".", ",");

                                String urlTension1 = baseUrl + "api/devicesData/chart?"
                                        + "deviceImei=" + URLEncoder.encode(imei, "UTF-8")
                                        + "&dateDebut=" + URLEncoder.encode(start, "UTF-8")
                                        + "&dateFin=" + URLEncoder.encode(end, "UTF-8")
                                        + "&dataType=" + URLEncoder.encode("BLOOD_PRESSURE_SYSTOLIC", "UTF-8")
                                        + "&jsmFiltre=" + URLEncoder.encode("day", "UTF-8");

                                HttpResponse getTension1Response = HttpClientUtils.sendGet(urlTension1, headersGet, 30000);

                                if (getTension1Response.responseCode >= 200 && getTension1Response.responseCode < 300 && getTension1Response.body != null) {
                                    Log.i(TAG, "Réponse GET Tension Healysa [successful] :" + getTension1Response.body);
                                    try {
                                        JSONObject reponseTension1 = new JSONObject(getTension1Response.body);
                                        if (reponseTension1.has("BLOOD_PRESSURE_SYSTOLIC")) {
                                            JSONArray arrayTension1 = new JSONArray(reponseTension1.getString("BLOOD_PRESSURE_SYSTOLIC"));
                                            JSONObject dataTension1 = arrayTension1.getJSONObject(0);
                                            tensionS = dataTension1.getString("dataValue");
                                            tensionS = tensionS.replace(".", ",");

                                            String urlTension2 = baseUrl + "api/devicesData/chart?"
                                                    + "deviceImei=" + URLEncoder.encode(imei, "UTF-8")
                                                    + "&dateDebut=" + URLEncoder.encode(start, "UTF-8")
                                                    + "&dateFin=" + URLEncoder.encode(end, "UTF-8")
                                                    + "&dataType=" + URLEncoder.encode("BLOOD_PRESSURE_DIASTOLIC", "UTF-8")
                                                    + "&jsmFiltre=" + URLEncoder.encode("day", "UTF-8");

                                            HttpResponse getTension2Response = HttpClientUtils.sendGet(urlTension2, headersGet, 30000);

                                            if (getTension2Response.responseCode >= 200 && getTension2Response.responseCode < 300 && getTension2Response.body != null) {
                                                Log.i(TAG, "Réponse GET Tension Healysa [successful] :" + getTension2Response.body);
                                                try {
                                                    JSONObject reponseTension2 = new JSONObject(getTension2Response.body);
                                                    if (reponseTension2.has("BLOOD_PRESSURE_DIASTOLIC")) {
                                                        JSONArray arrayTension2 = new JSONArray(reponseTension2.getString("BLOOD_PRESSURE_DIASTOLIC"));
                                                        JSONObject dataTension2 = arrayTension2.getJSONObject(0);
                                                        tensionD = dataTension2.getString("dataValue");
                                                        tensionD = tensionD.replace(".", ",");

                                                        // 5. Récupérer la donnée SPO2 (GET)
                                                        String urlSPO2 = baseUrl + "api/devicesData/chart?"
                                                                + "deviceImei=" + URLEncoder.encode(imei, "UTF-8")
                                                                + "&dateDebut=" + URLEncoder.encode(start, "UTF-8")
                                                                + "&dateFin=" + URLEncoder.encode(end, "UTF-8")
                                                                + "&dataType=" + URLEncoder.encode("SPO2", "UTF-8")
                                                                + "&jsmFiltre=" + URLEncoder.encode("day", "UTF-8");

                                                        HttpResponse getSPO2Response = HttpClientUtils.sendGet(urlSPO2, headersGet, 30000);

                                                        if (getSPO2Response.responseCode >= 200 && getSPO2Response.responseCode < 300 && getSPO2Response.body != null) {
                                                            Log.i(TAG, "Réponse GET SPO2 Healysa [successful] :" + getSPO2Response.body);
                                                            try {
                                                                JSONObject reponseSPO2 = new JSONObject(getSPO2Response.body);
                                                                if (reponseSPO2.has("SPO2")) {
                                                                    JSONArray arraySPO2 = new JSONArray(reponseSPO2.getString("SPO2"));
                                                                    JSONObject dataSPO2 = arraySPO2.getJSONObject(0);
                                                                    spo2 = dataSPO2.getString("dataValue");
                                                                    spo2 = spo2.replace(".", ",");
                                                                    Log.i(TAG, "Réponse GET SPO2 Healysa [successful] : " + spo2);
                                                                    translate("HEALYSA_CHECKUP", new ITranslationCallback() {
                                                                        @Override
                                                                        public void onTranslated(String translatedText) {
                                                                            String verifyMessage = verifyCmdMessages(translatedText);
                                                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                                                try {
                                                                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                                                    JSONObject history1 = new JSONObject();
                                                                                    history1.put("role", "assistant");
                                                                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]
                                                                                            .replace("[1]", heart_rate)
                                                                                            .replace("[2]", tensionS)
                                                                                            .replace("[3]", tensionD)
                                                                                            .replace("[4]", spo2));
                                                                                    existingHistoryArray.put(history1);
                                                                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                                                } catch (JSONException e) {
                                                                                    e.printStackTrace();
                                                                                }
                                                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]
                                                                                        .replace("[1]", heart_rate)
                                                                                        .replace("[2]", tensionS)
                                                                                        .replace("[3]", tensionD)
                                                                                        .replace("[4]", spo2));
                                                                            }
                                                                        }
                                                                    });
                                                                } else {
                                                                    Log.i(TAG, "Réponse GET SPO2 Healysa [successful] : SPO2 null");
                                                                    translate("HEALYSA_SPO2", new ITranslationCallback() {
                                                                        @Override
                                                                        public void onTranslated(String translatedText) {
                                                                            String verifyMessage = verifyCmdMessages(translatedText);
                                                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            }
                                                        } else {
                                                            translate("HEALYSA_SPO2", new ITranslationCallback() {
                                                                @Override
                                                                public void onTranslated(String translatedText) {
                                                                    String verifyMessage = verifyCmdMessages(translatedText);
                                                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    } else {
                                                        translate("HEALYSA_BLOODP", new ITranslationCallback() {
                                                            @Override
                                                            public void onTranslated(String translatedText) {
                                                                String verifyMessage = verifyCmdMessages(translatedText);
                                                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                                }
                                                            }
                                                        });
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                translate("HEALYSA_BLOODP", new ITranslationCallback() {
                                                    @Override
                                                    public void onTranslated(String translatedText) {
                                                        String verifyMessage = verifyCmdMessages(translatedText);
                                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                        }
                                                    }
                                                });
                                            }
                                        } else {
                                            translate("HEALYSA_BLOODP", new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedText) {
                                                    String verifyMessage = verifyCmdMessages(translatedText);
                                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                    }
                                                }
                                            });
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    translate("HEALYSA_BLOODP", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            String verifyMessage = verifyCmdMessages(translatedText);
                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                            }
                                        }
                                    });
                                }
                            } else {
                                translate("HEALYSA_HRV", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedText) {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                        }
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e(TAG, "Réponse GET Fréquence Cardiaque Healysa [not successful]");
                        if (getHRResponse.body != null) {
                            try {
                                JSONObject jsonErrorContent = new JSONObject(getHRResponse.body);
                                Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 jsonErrorContent " + jsonErrorContent);
                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_CHECKUP, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("HEALYSA_CHECKUP", errorTXT, "notOnFailure");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 catch" + e);
                            }
                        }
                    }
                } else {
                    String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                    Log.e(TAG, "Réponse Santé Healysa [not successful]");
                    if (cmdResponse.body != null) {
                        Log.e(TAG, "Réponse Santé Healysa [not successful]");
                        try {
                            JSONObject jsonErrorContent = new JSONObject(cmdResponse.body);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_CHECKUP, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_CHECKUP", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse Santé Healysa [not successful]1 catch" + e);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Réponse SANTE Healysa [not successful] :" + e);
                logErrorAPIHealysa("HEALYSA_CHECKUP", e.getMessage(), "onFailure");
            }
        }).start();
    }

    public void HEALYSA_CALL(String destinataire) {
        final String TAG = "Commande";
        new Thread(() -> {
            try {
                String baseUrl = teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD", configFile);

                // 1. Récupérer la liste des numéros (GET)
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                headers.put("Content-Type", "application/json; charset=utf-8");


                String url = baseUrl + "api/deviceParams/device/" + teamChatBuddyApplication.getImeiDevice() +
                        "?paramsTypes=" + URLEncoder.encode("AGENDA", "UTF-8");
                HttpResponse httpResponse = HttpClientUtils.sendGet(url, headers, 30000);

                if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300 && httpResponse.body != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(httpResponse.body);
                        Log.i(TAG, "get PhoneNumber [successful]: " + jsonArray.toString());

                        JSONObject agendaObject = jsonArray.getJSONObject(0);
                        JSONArray valueArray = agendaObject.getJSONArray("value");

                        String phone_number = null;
                        for (int i = 0; i < valueArray.length(); i++) {
                            JSONObject contact = valueArray.getJSONObject(i);
                            String name = contact.getString("name");
                            if (name.toLowerCase().contains(destinataire.toLowerCase())) {
                                phone_number = contact.getString("phoneNumber");
                            }
                        }
                        if (phone_number == null) {
                            Log.e(TAG, "Aucun numéro de téléphone est attribué à " + destinataire);
                            logErrorAPIHealysa("HEALYSA_CALL", "No telephone number is assigned to " + destinataire, "onFailure");
                            translate("HEALYSA_CALL", new ITranslationCallback() {
                                @Override
                                public void onTranslated(String translatedText) {
                                    if (translatedText.contains("No_message_defined")) {
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    } else {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            translateSpecificErrors(" aucun numéro de téléphone  ", new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedMessage) {
                                                    Log.i(TAG, "Error Translated: " + translatedMessage);
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                            return;
                        }
                        Log.i(TAG, "Réponse numéro de téléphone :" + phone_number + "  imei " + teamChatBuddyApplication.getImeiDevice());

                        JSONObject cmdBody = new JSONObject();
                        cmdBody.put("cmd", "IWBP32," + teamChatBuddyApplication.getImeiDevice() + ",080835," + phone_number + "#");

                        String imei = teamChatBuddyApplication.getImeiDevice();
                        String product = "silver";
                        String cmd = "IWBP32," + teamChatBuddyApplication.getImeiDevice() + ",080835," + phone_number + "#";

                        String urlStr = baseUrl + "api/deviceMessageProcessor/test/cmd"
                                + "?product=" + URLEncoder.encode(product, "UTF-8")
                                + "&imei=" + URLEncoder.encode(imei, "UTF-8")
                                + "&cmd=" + URLEncoder.encode(cmd, "UTF-8");

                        HttpResponse callResponse = HttpClientUtils.sendPost(
                                urlStr,
                                "", // corps vide
                                headers,
                                30000
                        );

                        if (callResponse.responseCode >= 200 && callResponse.responseCode < 300 && callResponse.body != null) {
                            Log.i(TAG, "Réponse de l'appel [successful] :" + callResponse.body);
                            translate("HEALYSA_CALL", new ITranslationCallback() {
                                @Override
                                public void onTranslated(String translatedText) {
                                    if (translatedText.contains("No_message_defined")) {
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    } else {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            try {
                                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                JSONObject history1 = new JSONObject();
                                                history1.put("role", "assistant");
                                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                existingHistoryArray.put(history1);
                                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                        }
                                    }
                                }
                            });
                        } else {
                            String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                            if (callResponse.body != null) {
                                Log.e(TAG, "Réponse de l'appel [not successful]  " + callResponse.body);
                                try {
                                    JSONObject jsonErrorContent = new JSONObject(callResponse.body);
                                    String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_CALL, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                    logErrorAPIHealysa("HEALYSA_CALL", errorTXT, "notOnFailure");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e(TAG, "Réponse de l'appel [not successful]1 catch" + e);
                                }
                            }
                            Log.e(TAG, "Réponse de l'appel [not successful]");
                            translate("HEALYSA_CALL", new ITranslationCallback() {
                                @Override
                                public void onTranslated(String translatedText) {
                                    if (translatedText.contains("No_message_defined")) {
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    } else {
                                        String verifyMessage = verifyCmdMessages(translatedText);
                                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ""));
                                        }
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.i(TAG, "get PhoneNumber [successful] :" + e.getMessage());
                        translate("HEALYSA_CALL", new ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedText) {
                                if (translatedText.contains("No_message_defined")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                } else {
                                    String verifyMessage = verifyCmdMessages(translatedText);
                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                        translateSpecificErrors(" problème lors de la récuperation du numéro ", new ITranslationCallback() {
                                            @Override
                                            public void onTranslated(String translatedMessage) {
                                                Log.i(TAG, "Error Translated: " + translatedMessage);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    }
                } else {
                    if (httpResponse.body != null) {
                        try {
                            Log.e(TAG, "get PhoneNumber [not successful]  " + httpResponse.body);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    translate("HEALYSA_CALL", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    translateSpecificErrors(" problème lors de la récuperation du numéro ", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedMessage) {
                                            Log.i(TAG, "Error Translated: " + translatedMessage);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "onFailure get PhoneNumber [not successful] :" + e);
                translate("HEALYSA_CALL", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" problème lors de la récuperation du numéro ", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage);
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }).start();
    }

    public void HEALYSA_LOC(String prénom) {
        final String TAG = "Commande";
        new Thread(() -> {
            try {

                String baseUrl = teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD", configFile);
                JSONObject jsonParams = new JSONObject();
                jsonParams.put("email", teamChatBuddyApplication.getParamFromFile("Healysa_mail", configFile));
                jsonParams.put("password", teamChatBuddyApplication.getParamFromFile("Healysa_password", configFile));

                Map<String, String> headersCmd = new HashMap<>();
                headersCmd.put("Content-Type", "application/json; charset=utf-8");

                HttpResponse authResponse = HttpClientUtils.sendPost(
                        baseUrl + "publicApi/auth/login",
                        jsonParams.toString(),
                        headersCmd,
                        30000
                );

                if (authResponse.responseCode >= 200 && authResponse.responseCode < 300 && authResponse.body != null) {
                    Log.i(TAG, "Réponse Auth Healysa [successful] :" + authResponse.body);
                    JSONObject jsonObj = new JSONObject(authResponse.body);
                    teamChatBuddyApplication.setTokenHealysa(jsonObj.getString("token"));

                    // 2. Récupération IMEI (GET)
                    Map<String, String> headersGetIMEI = new HashMap<>();
                    headersGetIMEI.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                    headersGetIMEI.put("Content-Type", "application/json; charset=utf-8");

                    HttpResponse imeiResponse = HttpClientUtils.sendGet(
                            baseUrl + "api/users/account",
                            headersGetIMEI,
                            30000
                    );

                    if (imeiResponse.responseCode >= 200 && imeiResponse.responseCode < 300 && imeiResponse.body != null) {
                        JSONObject jsonObjIMEI = new JSONObject(imeiResponse.body);
                        JSONArray devices = jsonObjIMEI.getJSONArray("devices");
                        Boolean findConsumer = false;
                        String imeiLocation = null;
                        for (int i = 0; i < devices.length(); i++) {
                            Log.i(TAG, "Réponse Consumer Firstname Healysa [successful] :" + devices.getJSONObject(i).getJSONObject("consumer").getString("firstname"));
                            if (devices.getJSONObject(i).getJSONObject("consumer").getString("firstname").toLowerCase().equals(prénom.toLowerCase())) {
                                findConsumer = true;
                                imeiLocation = devices.getJSONObject(i).getString("imei");
                                Log.i(TAG, "Réponse Imei Healysa [successful] :" + imeiLocation);
                                Log.i(TAG, "Réponse Location Healysa [successful] :" + prénom);

                                // 3. Récupération localisation (GET)
                                Map<String, String> headersLocation = new HashMap<>();
                                headersLocation.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());

                                HttpResponse locationResponse = HttpClientUtils.sendGet(
                                        baseUrl + "api/beacon/" + imeiLocation,
                                        headersLocation,
                                        30000
                                );

                                if (locationResponse.responseCode >= 200 && locationResponse.responseCode < 300 && locationResponse.body != null) {
                                    Log.i(TAG, "Réponse Location Healysa [successful] -- :" + locationResponse.body);
                                    try {
                                        JSONArray localisation = new JSONArray(locationResponse.body);
                                        if (localisation.length() > 0) {
                                            JSONObject firstObject = localisation.getJSONObject(0);
                                            String description = firstObject.getString("description");
                                            translate("HEALYSA_LOC", new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedText) {
                                                    String verifyMessage = verifyCmdMessages(translatedText);
                                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                        try {
                                                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                            JSONObject history1 = new JSONObject();
                                                            history1.put("role", "assistant");
                                                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", description));
                                                            existingHistoryArray.put(history1);
                                                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", description));
                                                    }
                                                }
                                            });
                                        } else {
                                            translate("HEALYSA_LOC", new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedText) {
                                                    String verifyMessage = verifyCmdMessages(translatedText);
                                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                        try {
                                                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                            JSONObject history1 = new JSONObject();
                                                            history1.put("role", "assistant");
                                                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                            existingHistoryArray.put(history1);
                                                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", prénom).replace("[2]", ""));
                                                    }
                                                }
                                            });
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    translate("HEALYSA_LOC", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            String verifyMessage = verifyCmdMessages(translatedText);
                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                try {
                                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                    JSONObject history1 = new JSONObject();
                                                    history1.put("role", "assistant");
                                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                                    existingHistoryArray.put(history1);
                                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                translateSpecificErrors(" Erreur lors de la récupération de la localisation", new ITranslationCallback() {
                                                    @Override
                                                    public void onTranslated(String translatedMessage) {
                                                        Log.i(TAG, "Error Translated: " + translatedMessage);
                                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", prénom).replace("[2]", ": " + translatedMessage));
                                                    }
                                                });
                                            }
                                        }
                                    });
                                    if (locationResponse.body != null) {
                                        try {
                                            JSONObject jsonErrorContent = new JSONObject(locationResponse.body);
                                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_LOC, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                            logErrorAPIHealysa("HEALYSA_LOC", errorTXT, "notOnFailure");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                break; // On a trouvé le bon consommateur, on sort de la boucle
                            }
                        }
                        if (!findConsumer) {
                            logErrorAPIHealysa("HEALYSA_LOC", "Consumer not found", "onFailure");
                        }
                    } else {
                        // Erreur lors de la récupération de l'IMEI
                        translate("HEALYSA_LOC", new ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedText) {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    translateSpecificErrors(" Erreur lors de la récupération de l'IMEI", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedMessage) {
                                            Log.i(TAG, "Error Translated: " + translatedMessage);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", prénom).replace("[2]", ": " + translatedMessage));
                                        }
                                    });
                                }
                            }
                        });
                        if (imeiResponse.body != null) {
                            try {
                                JSONObject jsonErrorContent = new JSONObject(imeiResponse.body);
                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_LOC, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("HEALYSA_LOC", errorTXT, "notOnFailure");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    // Erreur d'authentification
                    translate("HEALYSA_LOC", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" Erreur d’authentification", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage);
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", prénom).replace("[2]", ": " + translatedMessage));
                                    }
                                });
                            }
                        }
                    });
                    if (authResponse.body != null) {
                        try {
                            JSONObject jsonErrorContent = new JSONObject(authResponse.body);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_LOC, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_LOC", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception pendant la récupération de la réponse Healysa : " + e);
                logErrorAPIHealysa("HEALYSA_LOC", e.getMessage(), "onFailure");
                translate("HEALYSA_LOC", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                existingHistoryArray.put(history1);
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            translateSpecificErrors(" Erreur d’authentification", new ITranslationCallback() {
                                @Override
                                public void onTranslated(String translatedMessage) {
                                    Log.i(TAG, "Error Translated: " + translatedMessage);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", prénom).replace("[2]", ": " + translatedMessage));
                                }
                            });
                        }
                    }
                });
            }
        }).start();
    }

    public void HEALYSA_FEEDCAT(String portion) {
        new Thread(() -> {
            try {

                String baseUrl = teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD", configFile);
                JSONObject jsonParams = new JSONObject();
                jsonParams.put("email", teamChatBuddyApplication.getParamFromFile("Healysa_mail", configFile));
                jsonParams.put("password", teamChatBuddyApplication.getParamFromFile("Healysa_password", configFile));

                Map<String, String> headersCmd = new HashMap<>();
                headersCmd.put("Content-Type", "application/json; charset=utf-8");

                HttpResponse authResponse = HttpClientUtils.sendPost(
                        baseUrl + "publicApi/auth/login",
                        jsonParams.toString(),
                        headersCmd,
                        30000
                );


                if (authResponse.responseCode >= 200 && authResponse.responseCode < 300) {
                    Log.i(TAG, "Réponse Auth Healysa [successful] :" + authResponse.body);
                    JSONObject jsonObj = new JSONObject(authResponse.body);
                    teamChatBuddyApplication.setTokenHealysa(jsonObj.getString("token"));
                } else {
                    Log.e(TAG, "Réponse FeedCat [not successful]");
                    String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_FEEDCAT, ERROR Body= " + authResponse.body + System.getProperty("line.separator");
                    logErrorAPIHealysa("HEALYSA_FEEDCAT", errorTXT, "notOnFailure");
                    translate("HEALYSA_FEEDCAT", translatedText -> {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" problème lors d'authentification", translatedMessage -> {
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                            translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]
                                                    .replace("[1]", ": " + translatedMessage));
                                });
                            }
                        }
                    });
                    return;
                }

                Map<String, String> headersGetIMEI = new HashMap<>();
                headersGetIMEI.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                headersGetIMEI.put("Content-Type", "application/json; charset=utf-8");

                HttpResponse imeiResponse = HttpClientUtils.sendGet(
                        baseUrl + "api/users/account",
                        headersGetIMEI,
                        30000
                );

                String imeiFeeder = null;
                if (imeiResponse.responseCode >= 200 && imeiResponse.responseCode < 300) {
                    Log.i(TAG, "Réponse Feed Healysa imei [successful] "+imeiResponse.body);
                    JSONObject jsonObj = new JSONObject(imeiResponse.body);
                    JSONArray devices = jsonObj.getJSONArray("devices");
                    Log.i(TAG, "Réponse Feed Healysa imei [successful] "+new Gson().toJson(devices));
                    for (int i = 0; i < devices.length(); i++) {
                        if (devices.getJSONObject(i).getJSONObject("consumer").getString("firstname").equals("FEEDER USA")) {
                            imeiFeeder = devices.getJSONObject(i).getString("imei");
                        }
                    }
                } else {
                    logErrorAPIHealysa("HEALYSA_FEEDCAT", imeiResponse.body, "notOnFailure");
                    translate("HEALYSA_FEEDCAT", translatedText -> {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" problème lors de la récuperation de l'IMEI", translatedMessage -> {
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                            translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]
                                                    .replace("[1]", ": " + translatedMessage));
                                });
                            }
                        }
                    });
                    return;
                }
                Log.i(TAG, "Réponse Feed Healysa imei [successful] imei feeder "+imeiFeeder);

                // 3. ENVOI COMMANDE FEED
                if (imeiFeeder != null) {
                    JSONObject jsonA = new JSONObject();
                    JSONObject jsonBody = new JSONObject();
                    jsonA.put("command_type", "FEED_PET");
                    jsonA.put("command_value", portion);
                    jsonBody.put("command", jsonA);
                    jsonBody.put("user_id", JSONObject.NULL);

                    Map<String, String> headersFeed = new HashMap<>();
                    headersFeed.put("Authorization", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                    headersFeed.put("Content-Type", "application/json");

                    HttpResponse feedResponse = HttpClientUtils.sendPost(
                            baseUrl + "publicApi/Tuya/" + imeiFeeder,
                            jsonBody.toString(),
                            headersFeed,
                            30000
                    );

                    if (feedResponse.responseCode >= 200 && feedResponse.responseCode < 300) {
                        Log.i(TAG, "Réponse Feed Healysa [successful] :" + feedResponse.body);
                        translate("HEALYSA_FEEDCAT", translatedText -> {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    try {
                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                        existingHistoryArray.put(history1);
                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                            translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        });
                    } else {
                        logErrorAPIHealysa("HEALYSA_FEEDCAT", feedResponse.body, "notOnFailure");
                        translate("HEALYSA_FEEDCAT", translatedText -> {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    translateSpecificErrors(" problème dans l'api", translatedMessage -> {
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                                translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]
                                                        .replace("[1]", ": " + translatedMessage));
                                    });
                                }
                            }
                        });
                    }
                }
                else {
                    logErrorAPIHealysa("HEALYSA_FEEDCAT", imeiResponse.body, "notOnFailure");
                    translate("HEALYSA_FEEDCAT", translatedText -> {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" problème lors de la récuperation de l'IMEI", translatedMessage -> {
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +
                                            translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]
                                                    .replace("[1]", ": " + translatedMessage));
                                });
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception pendant la récupération de la réponse Healysa : " + e);
            }
        }).start();
    }

    public void logErrorAPIHealysa(String commande,String message,String type){
        String errorTXT="";
        if (type.equals("onFailure")){
            errorTXT= new Date().toString()+", COMMANDERRORAPI,Commande= "+commande+ " ERROR Body{  message= "+message+"}"+System.getProperty("line.separator");
        }
        else {
            errorTXT= message;
        }

        File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");


        try {

            FileWriter fileWriter = new FileWriter(file2,true);
            fileWriter.write(errorTXT);
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void SWITCHBOT_LIGHT(String state) {
        String switchbot_id = teamChatBuddyApplication.getParamFromFile("Switchbot_id", configFile);
        String switchbot_token = teamChatBuddyApplication.getParamFromFile("Switchbot_token", configFile);
        new Thread(() -> {
            try {
                // URL avec l'ID encodé si besoin
                String urlStr = "https://api.switch-bot.com/v1.0/devices/" + URLEncoder.encode(switchbot_id, "UTF-8") + "/commands";

                // Corps JSON
                JSONObject jsonParams = new JSONObject();
                jsonParams.put("command", "turn" + state);
                jsonParams.put("parameter", "default");
                jsonParams.put("commandType", "command");

                // Headers
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authorization", switchbot_token);

                // Appel POST via ta classe HttpClientUtils
                HttpResponse httpResponse = HttpClientUtils.sendPost(urlStr, jsonParams.toString(), headers, 30000);

                if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300) {
                    Log.i(TAG, "Réponse SwitchBot [successful] :" + httpResponse.body);
                    translate("SWITCHBOT_LIGHT", translatedText -> {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            }
                        }
                    });
                } else {
                    Log.e(TAG, "Réponse SwitchBot [not successful] :" + httpResponse.body);
                    translate("SWITCHBOT_LIGHT", translatedText -> {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                            }
                        }
                    });

                    try {
                        JSONObject jsonErrorContent = new JSONObject(httpResponse.body);
                        String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= SWITCHBOT_LIGHT, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                        logErrorAPIHealysa("SWITCHBOT_LIGHT", errorTXT, "notOnFailure");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Erreur lors du parsing de la réponse d'erreur: " + e);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception pendant la récupération de la réponse SwitchBot : " + e);
                translate("SWITCHBOT_LIGHT", translatedText -> {
                    if (translatedText.contains("No_message_defined")) {
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    } else {
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                            try {
                                String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                                existingHistoryArray.put(history1);
                                teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2]);
                        }
                    }
                });
            }
        }).start();
    }

    public void CMD_IMAGE(String description){
        Log.i( TAG, "CMD IMAGE "+ description +" début." );
        new Thread(() -> {
            try {
                //init parameters
                String image_URL = teamChatBuddyApplication.getParamFromFile("Image_URL",configFile);
                String image_endpoint = teamChatBuddyApplication.getParamFromFile("Image_endpoint",configFile);
                String image_model = teamChatBuddyApplication.getParamFromFile("Image_model",configFile);
                String image_size = teamChatBuddyApplication.getParamFromFile("Image_size",configFile);
                if(image_URL == null || image_URL.isEmpty()) image_URL = "https://api.openai.com/";
                if(image_endpoint == null || image_endpoint.isEmpty()) image_endpoint = "v1/images/generations";
                if(image_model == null || image_model.isEmpty()) image_model = "dall-e-2";
                if(image_size == null || image_size.isEmpty()) image_size = "512x512";

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("model", image_model);
                    jsonObject.put("prompt", description);
                    jsonObject.put("n", 1);
                    jsonObject.put("size", image_size);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                createJsonFile("ImageGeneration-sent", jsonObject);

                // Préparation des headers
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authorization", "Bearer " + teamChatBuddyApplication.getparam("openAI_API_Key"));

                // Appel HTTP via HttpClientUtils
                HttpResponse httpResponse = HttpClientUtils.sendPost(image_URL + image_endpoint, jsonObject.toString(), headers, 50000);

                if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300) {
                    // Succès
                    JSONObject responseObject = new JSONObject(httpResponse.body);
                    createJsonFile("ImageGeneration-recv", responseObject);

                    JSONArray dataArray = responseObject.getJSONArray("data");
                    if (dataArray.length() > 0) {
                        JSONObject imageObject = dataArray.getJSONObject(0);
                        String urlImage = imageObject.getString("url");

                        Log.i(TAG, "CMD IMAGE " + description + " URL : " + urlImage);
                        String filename = description.replaceAll(" ", "_") + ".png";

                        @SuppressLint("StaticFieldLeak")
                        ImageGenerator imageDownloader = new ImageGenerator(
                                teamChatBuddyApplication,
                                filename,
                                new ImageGenerator.ImageSaveCallback() {
                                    @Override
                                    public void onImageSaved(String savedFileName) {
                                        if (savedFileName != null) {
                                            Log.i("HHO", "onImageSaved " + savedFileName);
                                            teamChatBuddyApplication.notifyObservers("showImage;SPLIT;TeamChatBuddy/images/recv/" + savedFileName);
                                        }
                                    }
                                }
                        ) {
                            @Override
                            protected void onPostExecute(Bitmap bitmap) {
                                super.onPostExecute(bitmap);
                                Log.i(TAG, "CMD IMAGE " + description + " téléchargé!");
                                if (bitmap != null) {
                                    translate("CMD_IMAGE", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            if (translatedText.contains("No_message_defined")) {
                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                                            } else {
                                                String verifyMessage = verifyCmdMessages(translatedText);
                                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                    try {
                                                        String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                                        JSONObject history1 = new JSONObject();
                                                        history1.put("role", "assistant");
                                                        history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                        existingHistoryArray.put(history1);
                                                        teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        };
                        imageDownloader.execute(urlImage);
                    }
                } else {
                    // Erreur HTTP
                    Log.e(TAG, "generateImage response not successful");
                    if (httpResponse.body != null) {
                        try {
                            JSONObject jsonErrorContent = new JSONObject(httpResponse.body);
                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_IMAGE, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("CMD_IMAGE", errorTXT, "notOnFailure");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "generateImage error parsing JSON error body");
                        }
                    }
                    translate("CMD_IMAGE", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            if (translatedText.contains("No_message_defined")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                            } else {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                    translateSpecificErrors(" problème lors de la génération de l'image", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedMessage) {
                                            Log.i(TAG, "Error Translated: " + translatedMessage);
                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                        }
                                    });
                                }
                            }
                        }
                    });
                }

            } catch (IOException | JSONException e) {
                Log.e(TAG, "generateImageHttpUrlConnection ERROR", e);
                logErrorAPIHealysa("CMD_IMAGE", e.getMessage(), "onFailure");
                translate("CMD_IMAGE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" problème lors de la génération de l'image", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage);
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": " + translatedMessage));
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }).start();
    }

    public void CMD_CLOSE_IMAGE(){
        Log.i( TAG, "CMD CLOSE IMAGE début.");
        translate("CMD_CLOSE_IMAGE", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                teamChatBuddyApplication.notifyObservers( "closeImage");
                if (translatedText.contains("No_message_defined")) {
                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                } else {
                    String verifyMessage = verifyCmdMessages(translatedText);
                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                        try {
                            String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                            JSONObject history1 = new JSONObject();
                            history1.put("role", "assistant");
                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);

                            existingHistoryArray.put(history1);
                            // Stocker la nouvelle version de l'historique
                            teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                    }
                }
            }
        });
    }
    public void CMD_TAKE_PHOTO(String prompt){
        Log.e("MRRM","commande take photo fonction CMD_TAKE_PHOTO");

        teamChatBuddyApplication.notifyObservers("takePicture;SPLIT;"+prompt);

    }
    public void phototakedMessage(){
        translate("CMD_PHOTO", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
//                if (translatedText.contains("No_message_defined")) {
//                    teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
//                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
//                } else {
//                    String verifyMessage = verifyCmdMessages(translatedText);
//                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
//                        teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
//                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
//                    }
//                }
            }
        });
    }

    private void playRadio(String radioUrl){
        CMD_STOP_RADIO();
        radioPlayer = new MediaPlayer();
        teamChatBuddyApplication.setPlayingRadio(true);
        Log.i("RADIO_DEBUG", "playRadio() → isPlayingRadio=true, isOnApp=" + teamChatBuddyApplication.isOnApp + ", url=" + radioUrl);
        radioPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        try {
            radioPlayer.setDataSource(radioUrl);
            radioPlayer.prepareAsync();
            Log.i("RADIO_DEBUG", "prepareAsync() lancé");
            radioPlayer.setVolume(0.5f,0.5f);
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la configuration du lecteur audio", e);
        }

        radioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i("RADIO_DEBUG", "onPrepared() → isOnApp=" + teamChatBuddyApplication.isOnApp + ", isPlayingRadio=" + teamChatBuddyApplication.isPlayingRadio());
                if (teamChatBuddyApplication.isOnApp) {
                    Log.i("RADIO_DEBUG", "onPrepared() → start() appelé");
                    radioPlayer.start();
                } else {
                    Log.w("RADIO_DEBUG", "onPrepared() → app en pause, start() ignoré");
                }
            }
        });

        radioPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                // Gérer les erreurs de lecture ici
                Log.e(TAG, "Erreur de lecture audio. Code : " + what + ", Extra : " + extra);
                translate("CMD_RADIO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" Erreur de lecture audio ", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage); // Check description translation
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", ": "+translatedMessage));
                                    }
                                });
                            }
                        }
                    }
                });
                return false;
            }
        });
    }

    private void playMusic(File audioFile){

        Log.i( TAG, "CMD MUSIC début." );

        CMD_STOP_MUSIC();
        musicPlayer = new MediaPlayer();
        musicPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        try {
            Uri uri = Uri.parse(audioFile.getPath());
            musicPlayer.setDataSource(teamChatBuddyApplication, uri);
            musicPlayer.prepareAsync();
            musicPlayer.setVolume(0.5f,0.5f);
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la configuration du lecteur audio", e);
            e.printStackTrace();
        }

        musicPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                translate("CMD_MUSIC", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        teamChatBuddyApplication.setPlayingMusic(true);
                        if (teamChatBuddyApplication.isOnApp) {
                            musicPlayer.start();
                        }
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            }
                        }
                    }
                });
            }
        });

        musicPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "Erreur de lecture audio. Code : " + what + ", Extra : " + extra);
                translate("CMD_MUSIC", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        if (translatedText.contains("No_message_defined")) {
                            teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        } else {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                try {
                                    // get the historic commandes :
                                    String jsonArrayString = teamChatBuddyApplication.getparam("messages");
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                    existingHistoryArray.put(history1);
                                    // Stocker la nouvelle version de l'historique
                                    teamChatBuddyApplication.setparam("messages", existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.setTimeToExecuteNextCommande(true);
                                translateSpecificErrors(" erreur lors de la lecture du fichier audio", new ITranslationCallback() {
                                    @Override
                                    public void onTranslated(String translatedMessage) {
                                        Log.i(TAG, "Error Translated: " + translatedMessage); // Check description translation
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", ": "+translatedMessage));
                                    }
                                });
                            }
                        }
                    }
                });
                return false;
            }
        });

    }

    public void createJsonFile(String fileName, JSONObject jsonObject){
        File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");


        try {
            if (file1.exists() && file1.isFile()) {
                file1.delete();
            }
            FileWriter fileWriter = new FileWriter(file1);
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String jsonString=gson.toJson(jsonObject);
            fileWriter.write(jsonString);
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void translateSpecificError(String message, ITranslationCallback iTranslationCallback) {
        if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
            iTranslationCallback.onTranslated(message);
        } else {
            teamChatBuddyApplication.getFrenchLanguageSelectedTranslator().translate(message)
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            iTranslationCallback.onTranslated(translatedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            iTranslationCallback.onTranslated(message);
                        }
                    });
        }
    }

}

