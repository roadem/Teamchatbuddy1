package com.robotique.aevaweb.teamchatbuddy.chatbotresponse;

import android.app.Activity;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.knuddels.jtokkit.api.Encoding;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;

public class ChatGptStreamMode {

    private static final String TAG_STREAM = "MODE_STREAM";
    private static final String TAG_STREAM_USAGE = "MODE_STREAM_USAGE";
    private final TeamChatBuddyApplication app;
    private final Activity activity;
    private final JSONArray existingHistoryArray;
    private final Queue<String> phrasesQueue = new LinkedList<>();
    private final Handler wordsHandler = new Handler();
    private final Handler phrasesHandler = new Handler();
    private Runnable wordsRunnable;
    private Runnable phrasesRunnable;
    private String word = "";
    private String phrase = "";
    private String text = "";
    private String phraseToPronounceWhenResumed;
    private String errorMsg = "";
    private String currentDisplayedText = "";
    public boolean isReadyToSpeak = true;
    private boolean isReset = false;
    public boolean isError = false;
    private boolean isPaused = false;
    private boolean isFullResponseReceived = false;
    private boolean isDisplayFinished = true;
    private int requestTotalTokens = 0;
    public static CountDownTimer responseTimeout;


    public ChatGptStreamMode(Activity activity,JSONArray existingHistoryArray){
        this.activity = activity;
        this.app = (TeamChatBuddyApplication) activity.getApplicationContext();
        this.existingHistoryArray = existingHistoryArray;
    }

    public void sendRequest(JSONObject jsonParams){
        try{
            //Calculer les tokens de la requête entière (entête + historique de discussion) :
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            requestTotalTokens = getRequestTotalTokens(requestBody);

            if(responseTimeout!=null){
                responseTimeout.cancel();
                responseTimeout = null;
            }
            if (
                    ( Integer.parseInt(app.getParamFromFile("Response_Timeout_in_seconds","TeamChatBuddy.properties"))!=0 )
                            && ((app.getCurrentLanguage().equals("en")
                            && !app.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").trim().isEmpty())
                            || (app.getCurrentLanguage().equals("fr")
                            && !app.getParamFromFile("Message_Timeout_NotRespected_fr","TeamChatBuddy.properties").trim().isEmpty())
                            ||(!app.getCurrentLanguage().equals("en")
                            && !app.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").trim().isEmpty())
                    )
            ) {
                app.setAnswerHasExceededTimeOut(false);
                responseTimeout = new CountDownTimer(Integer.parseInt(app.getParamFromFile("Response_Timeout_in_seconds", "TeamChatBuddy.properties")) * 1000, 1000) {
                    @Override
                    public void onTick(long l) {
                        Log.d("responseTimeout", "on Tick");
                    }
                    @Override
                    public void onFinish() {
                        if (app.isAlreadyGetAnswer()) {
                        } else {
                            app.setAnswerHasExceededTimeOut(true);
                            app.setTimeoutExpired(true);
                            if (!app.isOpenaialreadySwitchEmotion()) {
                                try {
                                    BuddySDK.UI.setFacialExpression(FacialExpression.TIRED,1);
                                }
                                catch (Exception e){
                                    Log.e("TAG","BuddySDK Exception  "+e);
                                }

                            }
                            if (app.getCurrentLanguage().equals("en")) {
                                String[] message_Timeout_NotRespected_en = app.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").split("/");
                                int randomNumber_message_Timeout_NotRespected_en = new Random().nextInt(message_Timeout_NotRespected_en.length);
                                app.speakTTS(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en], LabialExpression.SPEAK_NEUTRAL,"timeOutExpired");
                            }
                            else if (app.getCurrentLanguage().equals("fr")){
                                String[] message_Timeout_NotRespected_fr = app.getParamFromFile("Message_Timeout_NotRespected_fr","TeamChatBuddy.properties").split("/");
                                int randomNumber_message_Timeout_NotRespected_fr = new Random().nextInt(message_Timeout_NotRespected_fr.length);
                                app.speakTTS(message_Timeout_NotRespected_fr[randomNumber_message_Timeout_NotRespected_fr], LabialExpression.SPEAK_NEUTRAL,"timeOutExpired");
                            }
                            else {
                                String[] message_Timeout_NotRespected_en = app.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").split("/");
                                int randomNumber_message_Timeout_NotRespected_en = new Random().nextInt(message_Timeout_NotRespected_en.length);
                                app.getEnglishLanguageSelectedTranslator().translate(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en]).addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        app.speakTTS(translatedText, LabialExpression.SPEAK_NEUTRAL,"timeOutExpired");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e("responseTimeout","translatedText exception  "+e);
                                    }
                                });
                            }
                        }
                    }
                };
                Log.i("responseTimeout", " start responseTimeout StreamMode ON");
                responseTimeout.start();
            }
            Log.i("responseTimeout", " call ChatGPT API (StreamMode ON) ");
            //Envoyer la requête
            new Thread(() -> {
                try {

                    String apiUrl = app.getParamFromFile("ChatGPT_url","TeamChatBuddy.properties")+"/v1/chat/completions";

                    String payload = jsonParams.toString();
                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(50000);
                    conn.setReadTimeout(50000);
                    conn.setDoOutput(true);

                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + app.getparam("openAI_API_Key"));
                    headers.put("Content-Type", "application/json");
                    if (headers != null) {
                        for (Map.Entry<String, String> entry : headers.entrySet()) {
                            conn.setRequestProperty(entry.getKey(), entry.getValue());
                        }
                    }

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(payload.getBytes(StandardCharsets.UTF_8));
                    }

                    int responseCode = conn.getResponseCode();
                    if(responseTimeout!=null){
                        responseTimeout.cancel();
                        responseTimeout = null;
                        Log.i( "responseTimeout", "réponse ChatGPT stream mode --> cancel timeout  "+responseTimeout);
                    }
                    app.setGetResponseTime(System.currentTimeMillis());
                    Log.i(TAG_STREAM,"onResponse code         : "+responseCode);
                    if (responseCode >= 200 && responseCode < 300 ) {
                        if(!isReset){
                            try {
                                new Thread(() -> {
                                    try {
                                        Log.i("MYA_API_Google", "------------------ choose streaming mode -------------------");

                                        if(app.getParamFromFile("Pattern_End_Phrase", "TeamChatBuddy.properties").trim().contains("</speak>")){
                                            Log.i("MYA_API_Google", "------------------ handleStreamingResponseSsml streaming mode chosen -------------------");

                                            handleStreamingResponseSsml(conn.getInputStream());
                                        }
                                        else handleStreamingResponse(conn.getInputStream());
                                    } catch (IOException e) {
                                        Log.i("MYA_API_Google", "------------------ choose streaming mode error -------------------"+e);

                                        throw new RuntimeException(e);
                                    }
                                }).start();
                            } catch (Exception e) {
                                e.printStackTrace();
                                onErrorStreaming("EXCEPTION", null, null);
                            }
                        }
                        else{
                            Log.w(TAG_STREAM,"Ignore API response because reset() was called");
                        }
                    } else {
                        // Lire le corps de la réponse d'erreur (error stream)
                        String errorBody = "";
                        try (InputStream errorStream = conn.getErrorStream()) {
                            if (errorStream != null) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
                                StringBuilder sb = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    sb.append(line);
                                }
                                errorBody = sb.toString();
                            }
                        } catch (Exception e) {
                            Log.e(TAG_STREAM, "Erreur lors de la lecture du errorStream", e);
                        }
                        onErrorStreaming("RESPONSE_NOT_SUCCESSFUL", errorBody, responseCode);
                    }
                } catch (Exception e) {
                    app.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    app.setGetResponseTime(System.currentTimeMillis());
                    e.printStackTrace();
                    onErrorStreaming("EXCEPTION", null, null);
                }
            }).start();
        }
        catch (Exception e){
            app.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
            app.setGetResponseTime(System.currentTimeMillis());
            e.printStackTrace();
            onErrorStreaming("EXCEPTION",null,null);
        }
    }

    private void handleStreamingResponse(InputStream inputStream) {
        try {
            onStartStreaming();

            //get Pattern_Fin_Phrase from config file
            String pattern_fin_phrase = app.getParamFromFile("Pattern_End_Phrase","TeamChatBuddy.properties");
            if(pattern_fin_phrase == null || pattern_fin_phrase.isEmpty()) {
                pattern_fin_phrase = "[.]";
            }
            Log.w(TAG_STREAM, "pattern_fin_phrase: " + pattern_fin_phrase);
            //pattern_fin_phrase = "</speak>";
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String fileName = "ChatGPT-recv-stream";
            StringBuilder formattedContent = new StringBuilder();
            String line;
            int responseTotalTokens = 0;// responseTotalTokens = number of non empty delta content
            while ((line = reader.readLine()) != null && !isReset && !isError) {
                //Initialiser formattedContent qui sera sauvegardé sur : ChatGPT-recv-stream.txt
                if (line.trim().isEmpty()) {}
                else if (line.contains("[DONE]")) formattedContent.append(line);
                else{
                    JSONObject jsonObject = new JSONObject(line.replace("data:", "").trim());
                    String formattedObject = jsonObject.toString(4);
                    formattedContent.append("data: ").append(formattedObject);
                    formattedContent.append("\n\n");
                }
                //Récupération de : word/phrase/text :
                if (line.trim().startsWith("data:")) {
                    String jsonData = line.substring("data:".length()).trim();
                    if(jsonData.contains("[DONE]")){
                        Log.i(TAG_STREAM, "------------------ FULL RESPONSE RECEIVED -------------------");
                        Log.i(TAG_STREAM, "Text  : " + text);
                        Log.i(TAG_STREAM, "OpenAITTS  : " + text);
                        isFullResponseReceived = true;
                    }
                    else if (!jsonData.isEmpty()) {
                        JSONObject json = new JSONObject(jsonData);
                        if (!json.isNull("choices")) {
                            JSONArray choicesArray = json.getJSONArray("choices");
                            for (int j = 0; j < choicesArray.length(); j++) {
                                JSONObject choiceObject = choicesArray.getJSONObject(j);
                                if (choiceObject.has("delta")) {
                                    JSONObject deltaObject = choiceObject.getJSONObject("delta");
                                    if (deltaObject.has("content") && !deltaObject.getString("content").isEmpty()) {
                                        responseTotalTokens++;
                                    }
                                }
                                if (!choiceObject.isNull("finish_reason") && choiceObject.getString("finish_reason").equals("stop")) {
                                    Log.e("MODE_STREAM","IF:");
                                    phrase += word;
                                    text += word;
                                    onNewPhrase();
                                }
                                else {
                                    Log.e("Test_pattern","ELSE:");
                                    String resp = choiceObject.getJSONObject("delta").getString("content");
                                    onNewWord(resp);
                                    if (resp.length() > 0) {
                                        if (!phrase.isEmpty()){
                                            Matcher matcher = Pattern.compile(pattern_fin_phrase).matcher(String.valueOf(phrase.charAt(phrase.length() - 1)));
                                            if (matcher.find() ){
                                                Log.e("Test_pattern1","ELSE: phrase "+phrase);
                                                onNewPhrase();
                                                phrase = "";
                                            }
                                            else if (phrase.length() >= 2) {
                                                // Extraire les caractères spécifiés dans le pattern
                                                // Split the string by "[" and then by "]"
                                                String[] parts = pattern_fin_phrase.split("\\[");

                                                // Check if the split results contain the needed part
                                                String firstBracketContent = "";
                                                if (parts.length > 1) {
                                                    String[] subParts = parts[1].split("\\]");
                                                    if (subParts.length > 0) {
                                                        firstBracketContent = "["+subParts[0]+"]";
                                                    }
                                                }
                                                Log.e("Test_pattern1","firstBracketContent************** ="+firstBracketContent);
                                                Pattern characterPattern = Pattern.compile(firstBracketContent);
                                                Matcher matcher3 = characterPattern.matcher(pattern_fin_phrase);
                                                StringBuilder regexBuilder = new StringBuilder();
                                                // Construire une expression régulière dynamique pour les caractères de fin de phrase
                                                while (matcher3.find()) {
                                                    regexBuilder.append("\\").append(matcher3.group());
                                                }

                                                Pattern searchPattern = Pattern.compile("[" + regexBuilder.toString() + "]");
                                                Log.e("Test_pattern1","ContentAfterMatch************** ="+"[" + regexBuilder.toString() + "]");
                                                // Utiliser le pattern de recherche pour détecter la fin de la phrase
                                                Matcher searchMatcher = searchPattern.matcher(phrase);
                                                if (searchMatcher.find()){
                                                    String caracFound =searchMatcher.group();
                                                    Log.e("Test_pattern1", "Caractère de fin de phrase trouvé: " + caracFound);
                                                    int index = phrase.indexOf(caracFound);
                                                    if (index != -1 && index < phrase.length() - 1){
                                                        String subString = phrase.substring(index, index + 2); // récupérer deux caractères à partir de l'index trouvé
                                                        Log.e("Test_pattern1","ELSE: phrase2 les deux derniers caractères  "+subString);
                                                        Matcher matcher2 = Pattern.compile(pattern_fin_phrase).matcher(subString);
                                                        if (matcher2.find() ){
                                                            String beforeCharacter = phrase.substring(0, index + 1);
                                                            String afterCharacter = phrase.substring(index + 1);
                                                            phrase =beforeCharacter;
                                                            Log.e("Test_pattern1","ELSE: phrase2 "+phrase);
                                                            onNewPhrase();
                                                            phrase = afterCharacter;
                                                        }
                                                    }else {
                                                        Log.e("Test_pattern1","ELSE: phrase2 le point n'est pas suivi d'un caractére  ");
                                                    }
                                                }
                                            }
                                        }
                                        phrase += word;
                                        text += word;
                                        word = resp;
                                    }
                                    else {
                                        Log.e("Test_pattern","ELSE: else");
                                        word += resp;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stocker la nouvelle version de l'historique
            JSONObject history = new JSONObject();
            history.put("role", "assistant");
            history.put("content", text);
            existingHistoryArray.put(history);

            //vérifier limit tokens
            Log.d(TAG_STREAM_USAGE, "Response Total Tokens : " + responseTotalTokens);
            int totalTokens = requestTotalTokens + responseTotalTokens;
            Log.i(TAG_STREAM_USAGE, "-------> TOTAL Tokens : " + totalTokens);
            verifyLimitTokens(totalTokens);

            //-------Calculer la consommation OpenAI
            app.calcul_consommation(app.getparam("model"),requestTotalTokens,responseTotalTokens);

            // Sauvegarder formattedContent dans ChatGPT-recv-stream.txt
            storeStreamResponse(fileName, formattedContent.toString(), text);

        }
        catch (Exception e) {
            e.printStackTrace();
            onErrorStreaming("EXCEPTION",null, null);
        }
    }

    private void handleStreamingResponseSsml(InputStream inputStream) {
        try {
            onStartStreaming();

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String fileName = "ChatGPT-recv-stream";
            StringBuilder formattedContent = new StringBuilder();
            String line;
            int responseTotalTokens = 0;

            StringBuilder ssmlBuffer = new StringBuilder(); // accumule texte SSML en streaming

            while ((line = reader.readLine()) != null && !isReset && !isError) {

                // ----------- STOCKAGE DU BRUT ----------- //
                if (!line.trim().isEmpty()) {
                    if (line.contains("[DONE]")) {
                        formattedContent.append("[DONE]\n");
                    } else {
                        JSONObject json = new JSONObject(line.replace("data:", "").trim());
                        formattedContent.append("data: ").append(json.toString(4)).append("\n\n");
                    }
                }

                // ----------- TRAITEMENT STREAM ----------- //
                if (line.trim().startsWith("data:")) {

                    String jsonData = line.substring("data:".length()).trim();

                    if (jsonData.contains("[DONE]")) {
                        isFullResponseReceived = true;
                        continue;
                    }

                    if (!jsonData.isEmpty()) {
                        JSONObject json = new JSONObject(jsonData);

                        JSONArray choices = json.getJSONArray("choices");
                        for (int j = 0; j < choices.length(); j++) {

                            JSONObject delta = choices.getJSONObject(j).optJSONObject("delta");
                            if (delta == null) continue;

                            String content = delta.optString("content", "");
                            if (content.isEmpty()) continue;

                            responseTotalTokens++;

                            // Ajouter le nouveau texte SSML en streaming
                            ssmlBuffer.append(content);

                            // ---------- ICI : DETECTION D'UN BLOC COMPLET SSML ---------- //
                            String bufferStr = ssmlBuffer.toString();

                            int endIndex;
                            while ((endIndex = bufferStr.indexOf("</speak>")) != -1) {

                                // Extraire le bloc complet
                                String ssmlBlock = bufferStr.substring(0, endIndex + "</speak>".length());

                                // Notifier l'arrivée d'un bloc complet
                                onNewWord(ssmlBlock);
                                phrase = ssmlBlock;
                                text += ssmlBlock;

                                onNewPhrase(); // <------ BLOC TRAITÉ ICI

                                // Remove processed block from buffer
                                bufferStr = bufferStr.substring(endIndex + "</speak>".length());
                                ssmlBuffer = new StringBuilder(bufferStr);
                            }
                        }
                    }
                }
            }

            // HISTORIQUE
            JSONObject history = new JSONObject();
            history.put("role", "assistant");
            history.put("content", text);
            existingHistoryArray.put(history);

            storeStreamResponse(fileName, formattedContent.toString(), text);

        } catch (Exception e) {
            e.printStackTrace();
            onErrorStreaming("EXCEPTION", null, null);
        }
    }

    private int getRequestTotalTokens(RequestBody requestBody){
        try{
            int requestTotalTokens = 0;
            int totalTokensContent = 0;
            int totalTokensRole = 0;
            Optional<Encoding> encoding = app.getRegistry().getEncodingForModel(app.getparam("model"));
            if (encoding.isPresent()) {
                Encoding actualEncoding = encoding.get();
                Log.i(TAG_STREAM_USAGE, "Encoding is available for the model "+app.getparam("model") + " --> " + actualEncoding.getName());
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
                Log.e(TAG_STREAM_USAGE, "Encoding is not available for the model "+app.getparam("model"));
                return 0;
            }
        }
        catch(Exception e){
            Log.e(TAG_STREAM_USAGE, "getRequestTotalTokens() ERROR : " + e);
            e.printStackTrace();
            return 0;
        }
    }

    private void verifyLimitTokens(int totalTokens){
        int MaxLimit = Integer.parseInt(app.getparam("Max_Tokens_req"));
        Log.w(TAG_STREAM_USAGE, "verifyLimitTokens() --> MaxLimit=" + MaxLimit + " / totalTokens= "+totalTokens);
        if(totalTokens > MaxLimit){
            Log.e(TAG_STREAM_USAGE, "verifyLimitTokens() : exceeding --> remove the 1st request and its response");
            existingHistoryArray.remove(1);
            existingHistoryArray.remove(1);
        }else {
            Log.i(TAG_STREAM_USAGE, "verifyLimitTokens() : not exceeding");
        }
        app.setparam("messages", existingHistoryArray.toString());
    }

    private void storeStreamResponse(String fileName, String formattedContent, String fullResponse) {
        Log.w(TAG_STREAM, "storeStreamResponse()");
        try{
            File file = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".txt");
            if (file.exists() && file.isFile()) {
                boolean result = file.delete();
                Log.i(TAG_STREAM, "storeStreamResponse() : file deleted : "+result);
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(formattedContent);
            fileWriter.write("\n");

            // Ajouter la réponse complète à la fin
            if (fullResponse != null && !fullResponse.isEmpty()) {
                fileWriter.write("\n--- Full Response ---\n");
                fileWriter.write(fullResponse);
            }

            fileWriter.close();
            Log.i(TAG_STREAM, "storeStreamResponse() : new file added");
        }
        catch(Exception e){
            Log.e(TAG_STREAM, "storeStreamResponse() : "+e);
            e.printStackTrace();
        }
    }

    private void showPhrase(String phrase) {

        String cleanPhrase = phrase.replaceAll("<[^>]+>", "").trim();

        if (cleanPhrase.isEmpty()) return;

        isDisplayFinished = false;

        if (isError) currentDisplayedText = "";

        final int totalLength = currentDisplayedText.length() + cleanPhrase.length();

        for (int i = 1; i <= cleanPhrase.length(); i++) {

            final String phraseToShow = currentDisplayedText + cleanPhrase.substring(0, i);

            wordsHandler.postDelayed(wordsRunnable = new Runnable() {
                @Override
                public void run() {
                    app.notifyObservers("MODE_STREAM_TEXT;SPLIT;" + phraseToShow);
                    if (phraseToShow.length() == totalLength) {
                        isDisplayFinished = true;
                    }
                }
            }, i * 50L);
        }

        currentDisplayedText += cleanPhrase + " ";
    }


    private void processPhrasesWithDelay() {
        if (!phrasesQueue.isEmpty() && isDisplayFinished) {
            if(isReadyToSpeak) {
                isReadyToSpeak = false;
                String phraseToPronounce = phrasesQueue.poll();
                if(phraseToPronounce != null){
                    if (app.getparam("Detection_de_langue").contains("yes") && app.nombreDeMotsCheck(phraseToPronounce)) {
                        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
                        languageIdentifier.identifyPossibleLanguages(phraseToPronounce)
                                .addOnSuccessListener(
                        new OnSuccessListener<List<IdentifiedLanguage>>() {
                            @Override
                            public void onSuccess(List<IdentifiedLanguage> identifiedLanguages) {
                                if (identifiedLanguages.isEmpty()) {
                                    Log.e("MRA_idetifyLanguage", "languageIdentifier : Can't identify language of : " + phraseToPronounce);
                                    pronouncePhrase(phraseToPronounce);
                                } else {
                                    // Utiliser la première langue identifiée
                                    IdentifiedLanguage language = identifiedLanguages.get(0);
                                    String languageCode = language.getLanguageTag();
                                    float confidence = language.getConfidence();

                                    Log.i("MRA_idetifyLanguage", "Language of : [ " + phraseToPronounce + " ] is : " + languageCode + ", Confidence: " + confidence);
                                    if (app.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties")!=null && !app.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("")&& !app.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("0")) {
                                        if (Integer.parseInt(app.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties")) <= (confidence * 100)) {
                                            app.setLanguageDetected(languageCode.trim());
                                            pronouncePhrase(phraseToPronounce);
                                        } else {
                                            pronouncePhrase(phraseToPronounce);
                                        }
                                    }
                                    else {
                                        app.setLanguageDetected(languageCode.trim());
                                        pronouncePhrase(phraseToPronounce);
                                    }
                                }
                            }
                        })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        pronouncePhrase(phraseToPronounce);
                                    }
                                });
                    }
                    else{
                        pronouncePhrase(phraseToPronounce);
                    }
                }
            }
        }
        else{
            if( isDisplayFinished && ((isFullResponseReceived && isReadyToSpeak) || (isError && isReadyToSpeak) )){
                onFinishStreaming();
                app.notifyObservers("TTS_success");
                reset();
                return;
            }
        }
        phrasesHandler.postDelayed(phrasesRunnable = new Runnable() {
            @Override
            public void run() {
                processPhrasesWithDelay();
            }
        }, 50);
    }

    private void pronouncePhrase(String phraseToPronounce){
        if(!isReset){
            if (!app.isTimeoutExpired()) {
                Log.i(TAG_STREAM, "TTS : [ " + phraseToPronounce + " ]");
                if (app.getparam("switch_visibility").contains("yes")) {
                    showPhrase(phraseToPronounce);
                }
                else{
                    isDisplayFinished = true;
                }
                app.notifyObservers("MODE_STREAM_SPEAK;SPLIT;"+phraseToPronounce);
            }
            else {
                Log.w(TAG_STREAM, "Pause streaming until TTS is ready again [ " + phraseToPronounce + " ]");
                pauseStreaming(phraseToPronounce);
            }
        }
    }

    public void onTTSEnd(){
        Log.i(TAG_STREAM,"TTS END");
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        }
        catch (Exception e){
            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
        }
        isReadyToSpeak = true;
    }

    public void resumeStreaming(){
        if(isPaused) {
            isPaused = false;
            pronouncePhrase(phraseToPronounceWhenResumed);
        }
    }

    private void pauseStreaming(String phraseToPronounceWhenResumed){
        isPaused = true;
        this.phraseToPronounceWhenResumed = phraseToPronounceWhenResumed;
    }

    private void onNewPhrase() {
        Log.w("MYA", "onNewPhrase--phrase: " + phrase);
        Log.w("MARIA_TEST", "Phrase: " + phrase);
        if (app.getParamFromFile("Response_filter","TeamChatBuddy.properties")!=null && !app.getParamFromFile("Response_filter","TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
            phrase = app.applyFilters(app.getParamFromFile("Response_filter","TeamChatBuddy.properties"),phrase);
        }
        Log.w(TAG_STREAM, "Phrase after response filter: " + phrase);
        phrasesQueue.add(phrase);
    }

    private void onNewWord(String resp) {
        Log.d(TAG_STREAM, "Word  : " + resp);
    }

    private void onStartStreaming(){
        if(!isReset){
            Log.i(TAG_STREAM, "------------------START-------------------");
            processPhrasesWithDelay();
        }
    }

    private void onFinishStreaming(){
        Log.i(TAG_STREAM, "------------------END-------------------");
    }

    private void onErrorStreaming(String error,String response, Integer  responseCode){
        Log.e(TAG_STREAM, "------------------ERROR-------------------");

        if(!isReset){

            if(phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
            phrasesHandler.removeCallbacksAndMessages(null);
            phrasesQueue.clear();

            if(wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
            wordsHandler.removeCallbacksAndMessages(null);

            app.stopTTS();
            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            }
            catch (Exception e){
                Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
            }

            SystemClock.sleep(1000);

            isReadyToSpeak = false;
            isError = true;

            if(error.equals("RESPONSE_NOT_SUCCESSFUL")){

                try {
                    if (response != null){
                        JsonObject errorLOG = new JsonObject();
                        JsonObject errorCode = new JsonObject();
                        errorCode.addProperty("ERROR CODE", responseCode);
                        JSONObject jsonErrorContent = new JSONObject(response);
                        JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                        String message = errorObject.getString("message");
                        String type = errorObject.getString("type");
                        String param = errorObject.getString("param");
                        String code = errorObject.getString("code");
                        JsonObject reformErrorJson = new JsonObject();
                        reformErrorJson.addProperty("message",message);
                        reformErrorJson.addProperty("type",type);
                        reformErrorJson.addProperty("param",param);
                        reformErrorJson.addProperty("code",code);
                        errorCode.add("ERROR Body",reformErrorJson);
                        errorLOG.add("OpenAIERROR",errorCode);
                        String fileName = "ERROR-LOG";
                        File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");
                        try {
                            if (file1.exists() && file1.isFile()) {
                                file1.delete();
                            }
                            FileWriter fileWriter = new FileWriter(file1);
                            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                            String jsonStringF=gson.toJson(errorLOG);
                            fileWriter.write(jsonStringF);
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String errorTXT= new Date().toString()+", OpenAIERROR,ERROR CODE= "+responseCode+", ERROR Body{ message= "+message+", type= "+type+", param= "+param+", code= "+code+"}"+System.getProperty("line.separator");
                        File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");
                        try {
                            FileWriter fileWriter = new FileWriter(file2,true);
                            fileWriter.write(errorTXT);
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        if(code.equals("context_length_exceeded")){
                            //vérifier limit tokens
                            verifyLimitTokens(requestTotalTokens);
                        }

                        int checkErrorCode = responseCode;
                        // Calcul de la consommation openai de le cas d'echec
                        if (checkErrorCode==500 ||checkErrorCode==503 || checkErrorCode==504){
                            app.calcul_consommation(app.getparam("model"),requestTotalTokens,0);
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }

                if (app.getLangue().getNom().equals("Anglais")){
                    errorMsg = app.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties");
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (app.getLangue().getNom().equals("Français")) {
                    errorMsg =  app.getParamFromFile("chatBotServerNoResponce_fr","TeamChatBuddy.properties");
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else{
                    app.getEnglishLanguageSelectedTranslator().translate(app.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties"))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                    app.setMessageError(true);
                                    if (!app.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = app.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties");
                                    app.setMessageError(true);
                                    if (!app.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception ex){
                                            Log.e(TAG_STREAM,"BuddySDK Exception  "+ex);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            });
                }
            }
            else if(error.equals("FAILURE")){
                if (app.getLangue().getNom().equals("Anglais")){
                    errorMsg = app.getString(R.string.chatBotNoFound_en);
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (app.getLangue().getNom().equals("Français")) {
                    errorMsg =  app.getString(R.string.chatBotNoFound_fr);
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (app.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = app.getString(R.string.chatBotNoFound_es);
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (app.getLangue().getNom().equals("Allemand")){
                    errorMsg =  app.getString(R.string.chatBotNoFound_de);
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else{
                    app.getEnglishLanguageSelectedTranslator().translate(app.getString(R.string.chatBotNoFound_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                    app.setMessageError(true);
                                    if (!app.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = app.getString(R.string.chatBotNoFound_en);
                                    app.setMessageError(true);
                                    if (!app.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception ex){
                                            Log.e(TAG_STREAM,"BuddySDK Exception  "+ex);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            });
                }
            }
            else{
                if (app.getLangue().getNom().equals("Anglais")){
                    errorMsg = app.getString(R.string.chatBot_ERROR_en);
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (app.getLangue().getNom().equals("Français")) {
                    errorMsg =  app.getString(R.string.chatBot_ERROR_fr);
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (app.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = app.getString(R.string.chatBot_ERROR_es);
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (app.getLangue().getNom().equals("Allemand")){
                    errorMsg =  app.getString(R.string.chatBot_ERROR_de);
                    app.setMessageError(true);
                    if (!app.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else{
                    app.getEnglishLanguageSelectedTranslator().translate(app.getString(R.string.chatBot_ERROR_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                    app.setMessageError(true);
                                    if (!app.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = app.getString(R.string.chatBot_ERROR_en);
                                    app.setMessageError(true);
                                    if (!app.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception ex){
                                            Log.e(TAG_STREAM,"BuddySDK Exception  "+ex);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            });
                }
            }


        }
    }

    public void reset(){
        Log.i(TAG_STREAM, "------------------reset-------------------");
        isReset = true;
        //reset phrasesQueue:
        if(phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
        phrasesHandler.removeCallbacksAndMessages(null);
        phrasesQueue.clear();
        isReadyToSpeak = true;
        //reset wordsQueue:
        if(wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
        wordsHandler.removeCallbacksAndMessages(null);
        app.setChatGptStreamMode(null);
    }

}
