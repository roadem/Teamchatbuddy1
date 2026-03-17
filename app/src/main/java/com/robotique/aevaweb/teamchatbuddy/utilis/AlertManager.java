package com.robotique.aevaweb.teamchatbuddy.utilis;

import static com.robotique.aevaweb.teamchatbuddy.utilis.TimeParser.parseTo24h;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AlertManager {
    private static AlertManager instance;
    private final Handler handler;
    private final Runnable alertRunnable;
    private final TeamChatBuddyApplication app;
    private Activity activity;

    private long alertDelay; // ms
    private int requiredRepetitions;
    private LocalDateTime inactivityStartTime, pauseStartTime;
    String msg, subject ;
    private long remainingTime = 0;
    private long pauseTimestamp = 0;



    /** --- Singleton --- */
    public static synchronized AlertManager getInstance(Activity activity) {
        if (instance == null) {
            if (activity == null) {
                Log.e("AlertManager", "getInstance appelé avec activity=null — instance non créée");
                return null;
            }
            instance = new AlertManager(
                    (TeamChatBuddyApplication) activity.getApplicationContext(),
                    activity
            );
        }
        return instance;
    }

    private AlertManager(TeamChatBuddyApplication app, Activity activity) {
        this.app = app;
        this.activity = activity;
        this.handler = new Handler(Looper.getMainLooper());
        this.alertRunnable = this::executeAlert;
    }

    /** -------------------- DÉMARRAGE DE LA SURVEILLANCE -------------------- **/
    public void start() {
        Log.i("AlertManager", "-------------------- start --------------------" );
        inactivityStartTime = LocalDateTime.now();
        handler.removeCallbacks(alertRunnable);
        pauseStartTime = null;
        try{
            alertDelay = Long.parseLong(app.getParamFromFile("ALERT_DURATION", "TeamChatBuddy.properties")) * 60000;
        }catch (Exception e) {
            Log.i("AlertManager", "alertDelay Erreur:"+e);
            alertDelay = 1 * 60000;
        }

        handler.postDelayed(alertRunnable, alertDelay);
        Log.i("AlertManager", "Surveillance démarrée à " + inactivityStartTime);
        app.setparam("wasAlertActivated", "true");
    }

    /** -------------------- MÉTHODE INCREMENTE -------------------- **/
    public void incremente(String actionType, Activity activity) {
        String rep = app.getParamFromFile("ALERT_REPETITIONS", "TeamChatBuddy.properties");
        if( rep != null && !rep.isEmpty()){
            requiredRepetitions = Integer.parseInt(rep);
            Log.i("AlertManager", "requiredRepetitions not empty ^^ " + requiredRepetitions);
        }else{
            requiredRepetitions = 0;
            Log.i("AlertManager", "Oups requiredRepetitions is empty ^^ ");
        }

        Log.w("AlertManager", "actionType : "+actionType);

        if (actionType.toLowerCase(Locale.ROOT).contains("touch")) {
            Log.w("MARIA", "actionType as touch : "+actionType);
            app.setCounterTouch(app.getCounterTouch() + 1);
            Log.d("AlertManager", "Touch count = " + app.getCounterTouch());
            if (app.getCounterTouch() + app.getCounterTracking() +  app.getCounterHotword() >= requiredRepetitions+1){
                reinit();
            }
        }
        else if (actionType.toLowerCase(Locale.ROOT).contains("hotword")) {
            Log.w("MARIA", "actionType as hotword : "+actionType);
            app.setCounterHotword(app.getCounterHotword() + 1);
            Log.d("AlertManager", "Hotword count = " + app.getCounterHotword());
            if (app.getCounterTouch() + app.getCounterTracking() +  app.getCounterHotword() >= requiredRepetitions+1){
                reinit();
            }
        }
        else if (actionType.toLowerCase(Locale.ROOT).contains("tracking")) {
            Log.w("MARIA", "actionType as tracking : "+actionType);
            app.setCounterTracking(app.getCounterTracking() + 1);
            Log.d("AlertManager", "Tracking count = " + app.getCounterTracking());
            if (app.getCounterTouch() + app.getCounterTracking() + app.getCounterHotword() >= requiredRepetitions+1) {
                reinit();
            }
        }

        Log.i("MARIA", "------------------------------------------ incremente ------------------------------------------" + actionType);
        Log.d("MARIA", "Touch count = " + app.getCounterTouch());
        Log.d("MARIA", "Hotword count = " + app.getCounterHotword());
        Log.d("MARIA", "Tracking count = " + app.getCounterTracking());
        Log.d("MARIA", "requiredRepetitions = " + requiredRepetitions+ "\n");

        Log.w("AlertManager", "Compteur d'interaction : \n"+String.valueOf(app.getCounterTouch() + app.getCounterTracking() +  app.getCounterHotword()));


    }

    /** -------------------- RÉINITIALISATION APRÈS ACTIVITÉ -------------------- **/
    public void reinit() {
        Log.i("AlertManager", "-------------------- reinit --------------------" );
        handler.removeCallbacks(alertRunnable);
        inactivityStartTime = LocalDateTime.now();
        alertDelay = Long.parseLong(app.getParamFromFile("ALERT_DURATION", "TeamChatBuddy.properties")) * 60000;
        handler.postDelayed(alertRunnable, alertDelay);
        app.setCounterHotword(0);
        app.setCounterTouch(0);
        app.setCounterTracking(0);
    }

    /** -------------------- ARRÊT COMPLET -------------------- **/
    public void stop() {

        Log.i("AlertManager", "-------------------- stop --------------------" );
        handler.removeCallbacks(alertRunnable);
        app.setCounterHotword(0);
        app.setCounterTouch(0);
        app.setCounterTracking(0);
        Log.i("AlertManager", "Surveillance arrêtée.");
    }

    /** -------------------- MISE EN PAUSE -------------------- **/
    public void pause() {

        Log.i("AlertManager", "-------------------- pause --------------------" );

        // Annuler l'alerte en attente
        handler.removeCallbacks(alertRunnable);

        // Calcul du temps restant
        long elapsed;
        if(pauseStartTime != null) {
            elapsed = System.currentTimeMillis() - pauseStartTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            remainingTime = remainingTime - elapsed;
            if (remainingTime < 0) remainingTime = 0;
            pauseStartTime = null;
        }
        else if (inactivityStartTime != null) {
            elapsed = System.currentTimeMillis() - inactivityStartTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            remainingTime = alertDelay - elapsed;
            if (remainingTime < 0) remainingTime = 0;
        } else {
            remainingTime = alertDelay;
        }

        app.setparam("touch", app.getCounterTouch()+"");
        app.setparam("hotword", app.getCounterHotword()+"");
        app.setparam("tracking", app.getCounterTracking()+"");
        app.setparam("remainingTime", String.valueOf(remainingTime));
        app.setparam("wasAlterActivated", String.valueOf(true));
        Log.i("AlertManager", "--- remainingTime=" + remainingTime + " savedTouch=" + app.getCounterTouch() + " savedHotword=" + app.getCounterHotword() + " savedTracking=" + app.getCounterTracking());

        Log.i("AlertManager", "Pause activée. remainingTime = " + remainingTime);
        pauseTimestamp = System.currentTimeMillis();
    }

    /** -------------------- REPRISE -------------------- **/
    public void resume() {

        Log.i("AlertManager", "-------------------- resume --------------------" );
        // Récupération des valeurs sauvegardées avec gestion des valeurs vides
        String touchStr = app.getparam("touch");
        String hotwordStr = app.getparam("hotword");
        String trackingStr = app.getparam("tracking");
        String remainingTimeStr = app.getparam("remainingTime");

        int savedTouch = 0;
        int savedHotword = 0;
        int savedTracking = 0;
        long savedRemaining = 0;

        try { savedTouch = (touchStr != null && !touchStr.isEmpty()) ? Integer.parseInt(touchStr) + 1 : 0; } catch (Exception ignored) {}
        try { savedHotword = (hotwordStr != null && !hotwordStr.isEmpty()) ? Integer.parseInt(hotwordStr) : 0; } catch (Exception ignored) {}
        try { savedTracking = (trackingStr != null && !trackingStr.isEmpty()) ? Integer.parseInt(trackingStr) : 0; } catch (Exception ignored) {}
        try { savedRemaining = (remainingTimeStr != null && !remainingTimeStr.isEmpty()) ? Long.parseLong(remainingTimeStr) : 0; } catch (Exception ignored) {}

        app.setCounterTouch(savedTouch);
        app.setCounterHotword(savedHotword);
        app.setCounterTracking(savedTracking);

        remainingTime = savedRemaining;

        String rep = app.getParamFromFile("ALERT_REPETITIONS", "TeamChatBuddy.properties");
        requiredRepetitions = (rep != null && !rep.isEmpty()) ? Integer.parseInt(rep) : 0;

        pauseStartTime = LocalDateTime.now();

        if ((remainingTime <= 0)||(app.getCounterTouch() + app.getCounterTracking() +  app.getCounterHotword() >= requiredRepetitions+1)) {
            remainingTime = alertDelay;
            reinit();
            Log.w("MARIA", "actionType as touch : touch [source: fermeture BuddyCore → reinit]");
            Log.d("MARIA", "Touch count = " + app.getCounterTouch());
            Log.d("MARIA", "Hotword count = " + app.getCounterHotword());
            Log.d("MARIA", "Tracking count = " + app.getCounterTracking());
            Log.d("MARIA", "requiredRepetitions = " + requiredRepetitions);
            return;
        }

        inactivityStartTime = LocalDateTime.now();
        handler.removeCallbacks(alertRunnable);
        handler.postDelayed(alertRunnable, remainingTime);

        Log.i("AlertManager",
                "Resume exécuté. touch=" + savedTouch +
                        " hotword=" + savedHotword +
                        " tracking=" + savedTracking +
                        " remainingTime=" + remainingTime);
    }

    /** -------------------- DÉCLENCHEMENT DE L’ALERTE -------------------- **/
    private void executeAlert() {

        Log.i("AlertManager", "-------------------- executeAlert --------------------" );
        // Vérification du jour
        if (!isAlertDay()) {
            Log.i("AlertManager","Alerte ignorée : jour non actif");
            app.getEnglishLanguageSelectedTranslator().translate("Alert ignored: The alert is not activated today.").addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String translatedText) {
                    Toast.makeText(activity, translatedText, Toast.LENGTH_LONG).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("AlertManager_App", "translatedText exception  " + e);
                }
            });
            stop();
            start();
            return;
        }

        // Vérification du créneau horaire
        if (!isWithinAlertHours()) {
            Log.i("AlertManager","Alerte ignorée : hors créneau horaire autorisé");
            app.getEnglishLanguageSelectedTranslator().translate("Alert ignored: the alert is outside the authorized time.").addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String translatedText) {
                    Toast.makeText(activity, translatedText, Toast.LENGTH_LONG).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("AlertManager_App", "translatedText exception  " + e);
                }
            });
            stop();
            start();
            return;
        }

        // Construction du message && should add translation
        msg = buildMessage();
        subject = "TeamChatBuddy, Alert of inactivity!!";

        app.getEnglishLanguageSelectedTranslator().translate(subject + " # " + msg).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String translatedText) {
                Log.i("AlertManager", "onSuccess: "+translatedText.replace("\\[.*?\\]", "[TeamChatBuddy]"));

                subject =  translatedText.split("#")[0];
                msg = translatedText.split("#")[1];
                String tool = app.getParamFromFile("ALERT_TOOL", "TeamChatBuddy.properties").trim().toUpperCase(Locale.ROOT);
                Log.i("AlertManager", "Déclenchement alerte (" + tool + ") : " + subject + "\n" + msg);

                // Envoi selon le mode configuré
                switch (tool) {
                    case "MAIL":
                        sendMailIfAvailable(activity, subject, msg);
                        Log.i("AlertManager","Alerte tool : " + app.getParamFromFile("ALERT_TOOL", "TeamChatBuddy.properties"));
                        break;

                    case "SMS":
                        sendSmsIfAvailable(msg);
                        Log.i("AlertManager","Alerte tool : " + app.getParamFromFile("ALERT_TOOL", "TeamChatBuddy.properties"));
                        break;

                    case "SMS/MAIL":
                        sendMailIfAvailable(activity, subject, msg);
                        sendSmsIfAvailable(msg);
                        Log.i("AlertManager","Alerte tool : " + app.getParamFromFile("ALERT_TOOL", "TeamChatBuddy.properties"));
                        break;
                }

                Log.i("AlertManager","Alerte d’inactivité déclenchée : " + msg);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("AlertManager", "translatedText exception  " + e);
            }
        });

        stop();
        start();
    }

    /** -------------------- CONSTRUCTION DU MESSAGE -------------------- **/
    private String buildMessage() {
        String msgTemplate = app.getParamFromFile("ALERT_MSG", "TeamChatBuddy.properties");
        if (msgTemplate == null || msgTemplate.trim().isEmpty()) {
            msgTemplate = "Inactivity detected since [1] during [2] minutes.";
            Log.i("AlertManager","Here is the message : " + msg);
        }

        String startTimeStr = inactivityStartTime != null
                ? inactivityStartTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                : "inconnue";

        long durationMinutes = alertDelay / 60000;

        return msgTemplate
                .replace("[1]", startTimeStr)
                .replace("[2]", String.valueOf(durationMinutes));
    }

    /** -------------------- VÉRIFICATION DU JOUR ACTIF -------------------- **/
    private boolean isAlertDay() {
        String activeDaysStr = app.getParamFromFile("ALERT_DAYS", "TeamChatBuddy.properties").trim();
        if (activeDaysStr == null || activeDaysStr.isEmpty()) return false;

        List<String> activeDays = Arrays.asList(activeDaysStr.split(","));
        String today = LocalDateTime.now().getDayOfWeek().name(); // e.g. MONDAY

        for (String day : activeDays) {
            if (today.equalsIgnoreCase(day.trim())) return true;
        }
        return false;
    }

    /** -------------------- VÉRIFICATION DU CRÉNEAU HORAIRE -------------------- **/
    private boolean isWithinAlertHours() {
        try {
            String hoursRange = app.getParamFromFile("ALERT_HOURS", "TeamChatBuddy.properties");

            // Si vide, null ou mal formé → valeur par défaut
            if (hoursRange == null || hoursRange.trim().isEmpty() ||
                    (!hoursRange.contains("-") && !hoursRange.contains("/"))) {
                hoursRange = "08:00-20:00";
                Log.i("AlertManager_time", "ALERT_HOURS non défini — utilisation de la valeur par défaut : " + hoursRange);
            }

            String[] parts = hoursRange.split("[-/]");
            if (parts.length != 2) {
                Log.w("AlertManager_time", "ALERT_HOURS invalide — fallback sur la valeur par défaut 08:00-20:00");
                parts = new String[]{"08:00", "20:00"};
            }

            int[] start = parseTo24h(parts[0].trim());
            int[] end = parseTo24h(parts[1].trim());

            LocalTime startTime = LocalTime.of(start[0], start[1]);
            LocalTime endTime = LocalTime.of(end[0], end[1]);
            LocalTime now = LocalTime.now();

            Log.i("AlertManager_time", "Initial ALERT_HOURS start=" + startTime + " end=" + endTime);

            // Si la plage traverse minuit (ex. 14:00–07:00)
            if (!startTime.isBefore(endTime)) {
                Log.w("AlertManager_time", "ALERT_HOURS traverse minuit — ajustement automatique à " + startTime + "-23:59");
                endTime = LocalTime.of(23, 59);
            }

            // Si la fin dépasse 23:59, on la limite
            if (endTime.isAfter(LocalTime.of(23, 59))) {
                endTime = LocalTime.of(23, 59);
                Log.w("AlertManager_time", "ALERT_HOURS ajusté : fin limitée à 23:59");
            }

            Log.i("AlertManager_time", "Plage horaire effective : start=" + startTime + " end=" + endTime);
            Log.i("AlertManager_time", String.valueOf(!now.isBefore(startTime) && !now.isAfter(endTime)));

            // Vérifie si l’heure actuelle est dans la plage
            return !now.isBefore(startTime) && !now.isAfter(endTime);

        } catch (Exception e) {
            Log.e("AlertManager_time", "Erreur parsing ALERT_HOURS : " + e.getMessage());
            return true; // sécurité
        }
    }


    /** -------------------- MÉTHODE D’ENVOI MAIL -------------------- **/
    private void sendMailIfAvailable(Activity activity, String subject, String body) {
        String mailTo = app.getParamFromFile("ALERT_MAIL", "TeamChatBuddy.properties").trim();
        if (TextUtils.isEmpty(mailTo)) {
            Log.e("AlertManager", "ALERT_MAIL non défini — aucun e-mail ne sera envoyé.");
            Toast.makeText(activity, "ALERT_MAIL non défini — aucun e-mail ne sera envoyé.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            new MailSender(activity,String.join("\n", body.split("[,.]")), mailTo, subject).execute();
        } catch (Exception e) {
            Log.e("AlertManager", "Erreur lors de l'envoi du mail : " + e.getMessage());
        }
        Log.w("AlertManager", "Mail sent");
    }

    /** -------------------- MÉTHODE D’ENVOI SMS -------------------- **/
    private void sendSmsIfAvailable(String msg) {
        String phoneNumber = app.getParamFromFile("ALERT_SMS", "TeamChatBuddy.properties");
        if (TextUtils.isEmpty(phoneNumber)) {
            Log.i("AlertManager", "ALERT_SMS est vide. Aucune alerte SMS ne sera envoyée.");
            Toast.makeText(activity, "ALERT_SMS est vide. Aucune alerte SMS ne sera envoyée.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            SmsSender smsSender = new SmsSender(activity);

            smsSender.sendSmsOrEmailFallback(phoneNumber, msg, new SmsSender.MailCallback() {
                @Override
                public void onSuccess() {
                    Log.i("AlertManager", "Alerte envoyée avec succès (SMS ou fallback OVH).");
                    new Handler(Looper.getMainLooper()).post(() ->
                            Log.i("AlertManager", "Alerte SMS envoyée !"));
                }

                @Override
                public void onError(Exception e) {
                    Log.e("AlertManager", "Erreur lors de l’envoi de l’alerte : " + e.getMessage());
                    new Handler(Looper.getMainLooper()).post(() ->
                            Log.i("AlertManager","Erreur lors de l’envoi SMS : " + e.getMessage()));
                }
            });

        } catch (Exception e) {
            Log.e("AlertManager", "Erreur fatale dans sendSmsIfAvailable: " + e.getMessage(), e);
        }
    }


}

