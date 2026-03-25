package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class ConfigurationFile {

    private static final String TAG = "TEAMCHATBUDDY_ConfigurationFile";
    private static final int FILE_VERSION = 51; // upgrade this whenever you want to overwrite the file

    public static CustomProperties props = new CustomProperties();
    public static InputStream is = null;

    public static String createConfigurationFile(File directory, String app_version) {
        if (directory.exists() && directory.isDirectory()) {
            Log.i(TAG, "Le dossier 'TeamChatBuddy' existe déjà");
        }
        else {
            Log.i(TAG, "Création du dossier 'TeamChatBuddy' ...");
            directory.mkdir();
        }
        File configFile = new File(directory.getPath(), "TeamChatBuddy.properties");
        return WriteProperties(configFile, app_version);
    }


    public static CustomProperties loadproperties(File directory, String fileName, CustomProperties properties) {

        // First try loading from the current directory
        try {
            File f = new File(directory, fileName);
            is = new FileInputStream(f);

            if (is == null) {
                // Try loading from classpath
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                is = loader.getResourceAsStream(fileName);
            }
            // Try loading properties from the file (if found)
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                properties.load(reader);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading config file : "+e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error : "+e);
            }

        }
        return properties;
    }

    public static String WriteProperties(File configFile, String app_version) {
        String initOrMajOrNone = "NONE";
        try {

            if (!configFile.exists()) {
                Log.i(TAG, "Création du fichier de configuration 'TeamChatBuddy.properties' ...");
                props = new CustomProperties();
                initOrMajOrNone = "INIT";
            } else {
                Log.i(TAG, "Le fichier de configuration 'TeamChatBuddy.properties' existe déjà >> charger les propriétés existantes");
                File directory = new File("/storage/emulated/0/", "TeamChatBuddy");
                String fileName = "TeamChatBuddy.properties";
                props = loadproperties(directory, fileName, new CustomProperties());
                int existingFileVersion = props.containsKey("fileVersion") ? Integer.parseInt(props.getProperty("fileVersion")) : 0;
                if (existingFileVersion == FILE_VERSION) {
                    Log.i(TAG, "Pas besoin d'écraser. La version du fichier existant est compatible.");
                    initOrMajOrNone = "NONE";
                    return initOrMajOrNone;
                }
                else{
                    Log.i(TAG, "La version du fichier existant n'est pas à jour --> Ecraser le fichier.");
                    props = new CustomProperties();
                    initOrMajOrNone = "MAJ";
                }
            }

            // ---------------------------- VERSION DU FICHIER DE CONFIG -----------------
            //todo : upgrade FILE_VERSION whenever you want to overwrite the file !
            props.setProperty("fileVersion", String.valueOf(FILE_VERSION));


            // ---------------------------- PARAMETERS -----------------------------------

            props.addPropertyComment("Speech_To_Text_List","");
            props.addPropertyComment("Speech_To_Text_List","Speech to Text : SpeechRecognizer/ApiGoogle/Whisper/Cerence");
            setProperty("Speech_To_Text_List","SpeechRecognizer/ApiGoogle/Whisper/Cerence");
            setProperty("Speech_To_Text","SpeechRecognizer");
            setProperty("ApiGoogle_Key","");

            props.addPropertyComment("Change_STT","");
            props.addPropertyComment("Change_STT","Possibility of changing the STT (Yes/No)");
            setProperty("Change_STT","Yes");

            props.addPropertyComment("Android_Speech_minimum_length","");
            props.addPropertyComment("Android_Speech_minimum_length","Android Speech To Text config in seconds");
            setProperty("Android_Speech_minimum_length","15");
            setProperty("Android_Speech_silence_length","1");

            props.addPropertyComment("Language_Specification_STT", "");
            props.addPropertyComment("Language_Specification_STT", "Language_Specification_STT=Yes to specify the language parameter");
            setProperty("Language_Specification_STT","Yes");

            props.addPropertyComment("Seuil_dBFS", "");
            props.addPropertyComment("Seuil_dBFS","Decibel threshold to control the sensitivity of the STT Whisper");
            setProperty("Seuil_dBFS","-30");
            setProperty("Whisper_model","whisper-1");
            setProperty("Whisper_prompt","Buddy");

            props.addPropertyComment("Text_To_Speech_List", "");
            props.addPropertyComment("Text_To_Speech_List", "Text to Speech : ReadSpeaker/Android/ApiGoogle/OpenAI");
            setProperty("Text_To_Speech_List","ReadSpeaker/Android/ApiGoogle/OpenAI");
            props.addPropertyComment("ReadSpeaker_pitch_fr","Pitch and speed for TTS");
            props.addPropertyComment("ReadSpeaker_pitch_fr","ReadSpeaker Range : pitch [50-200] / speed [50-400]");
            props.addPropertyComment("ReadSpeaker_pitch_fr","Android/ApiGoogle Range : pitch & speed [50-150]");
            setProperty("ReadSpeaker_pitch_fr","160");
            setProperty("ReadSpeaker_pitch_en","160");
            setProperty("ReadSpeaker_speed_fr","100");
            setProperty("ReadSpeaker_speed_en","100");
            setProperty("TTS_Android_pitch_fr","115");
            setProperty("TTS_Android_pitch_en","87");
            setProperty("TTS_Android_speed_fr","100");
            setProperty("TTS_Android_speed_en","70");
            setProperty("TTS_ApiGoogle_pitch_fr","120");
            setProperty("TTS_ApiGoogle_pitch_en","130");
            setProperty("TTS_ApiGoogle_speed_fr","70");
            setProperty("TTS_ApiGoogle_speed_en","70");
            props.addPropertyComment("TTS_ApiGoogle_Voice_Type", "Voice type : Standard/Wavenet");
            props.addPropertyComment("TTS_ApiGoogle_Voice_Type", "If no voice is specified for a language in TTS_ApiGoogle_Language_Voice, the system uses the voice type defined in TTS_ApiGoogle_Voice_Type and appends '-A' as the default voice (e.g., Standard-A) ");
            setProperty("TTS_ApiGoogle_Voice_Type","Standard");
            setProperty("TTS_ApiGoogle_Language_Voice","[fr:Wavenet-C],[en:Standard-C]");
            props.addPropertyComment("TTS_OpenAI_ApiEndpoint", "TTS OpenAI parameters");
            setProperty("TTS_OpenAI_ApiEndpoint","/v1/audio/speech");
            setProperty("TTS_OpenAI_Model","tts-1");
            setProperty("TTS_OpenAI_Voice","sage");
            setProperty("TTS_OpenAI_Speed","1");
            setProperty("TTS_OpenAI_Instructions","Parle avec un ton heureux et enfantin.");


            props.addPropertyComment("ChatBot","");
            props.addPropertyComment("ChatBot","Default ChatBot : ChatGPT/CustomGPT");
            setProperty("ChatBot","ChatGPT");

            props.addPropertyComment("openAI_API_Key","");
            props.addPropertyComment("openAI_API_Key", "OpenAI key for conversation, emotion detection and whisper");
            setProperty("openAI_API_Key","");

            props.addPropertyComment("CustomGPT_API_Key","");
            props.addPropertyComment("CustomGPT_API_Key", "CustomGPT key and project number");
            setProperty("CustomGPT_API_Key","");
            setProperty("CustomGPT_Project_ID","");

            props.addPropertyComment("Change_chatBot","");
            props.addPropertyComment("Change_chatBot","Possibility of changing the chatbot (Yes/No)");
            setProperty("Change_chatBot","Yes");

            props.addPropertyComment("hotword_fr", "");
            props.addPropertyComment("hotword_fr", "List of hot words in French, English, Spanish and German");
            setProperty("hotword_fr", "ok buddy/hello/bonjour/salut/bonsoir/écoute/écoute-moi");
            setProperty("hotword_en","okay buddy/hello/good morning/good evening/listen/listen to me");

            props.addPropertyComment("Number_clicks_options", "");
            props.addPropertyComment("Number_clicks_options","Number of clicks to open options (0 invisible)");
            setProperty("Number_clicks_options","1");

            props.addPropertyComment("Start_Message_fr","");
            props.addPropertyComment("Start_Message_fr","Message spoken at the app's startup");
            setProperty("Start_Message_fr","Bonjour je suis le robot Buddy de Blue Frog Robotics.");
            setProperty("Start_Message_en","Hello, I am Buddy, the robot from Blue Frog Robotics.");
            props.addPropertyComment("Listening_time","");
            props.addPropertyComment("Listening_time","Maximum listening time (seconds) and number of successive listens");
            setProperty("Listening_time","10");
            setProperty("Number_listens","2");

            props.addPropertyComment("Displaying_QRCode_period","");
            props.addPropertyComment("Displaying_QRCode_period","Period for displaying the QRCode in seconds");
            setProperty("Displaying_QRCode_period","0");
            props.addPropertyComment("Displaying_QRCode_Duration","Duration for displaying the QRCode in seconds");
            setProperty("Displaying_QRCode_Duration","30");
            setProperty("QRcodeFileName","qrCode.png");
            setProperty("QR_Text","SCAN<br>FOR DRINKS<br>AND SNACKS");


            props.addPropertyComment("Lecture_QR_Code","");
            props.addPropertyComment("Lecture_QR_Code","Possibility of scanning Codes to communicate (Yes/No)");
            setProperty("Lecture_QR_Code","No");
            props.addPropertyComment("QRCode_System","Possible codes : QRCode / DataMatrix / AprilTag");
            setProperty("QRCode_System","QRCode/DataMatrix/AprilTag");

            props.addPropertyComment("Language","");
            props.addPropertyComment("Language","Languages available Français /Anglais /Espagnol /Allemand /Italien /Japonais /Arabe /Chinois /Danois /Néerlandais /Norvégien");
            setProperty("Language","Français");
            setProperty("Languages_available","Français /Anglais /Espagnol /Allemand /Italien /Japonais /Arabe /Chinois /Danois /Néerlandais /Norvégien");
            setProperty("Language_Code_Used_In_STT_Android","fr-FR/en-US/es-ES/de-DE/it-IT/ja-JP/ar-DZ/cmn-Hans-CN/da-DK/nl-NL/nb-NO");
            setProperty("Language_Code_Used_In_TTS_Android","fr-FR/en-US/es-ES/de-DE/it-IT/ja-JP/ar-DZ/zh-CN/da-DK/nl-NL/nb-NO");
            setProperty("Language_Code_Used_In_GoogleCloud_STT","fr-FR/en-US/es-ES/de-DE/it-IT/ja-JP/ar-DZ/zh/da-DK/nl-NL/no-NO");
            setProperty("Language_Code_Used_In_GoogleCloud_TTS","fr-FR/en-US/es-ES/de-DE/it-IT/ja-JP/ar-XA/cmn-CN/da-DK/nl-NL/nb-NO");
            setProperty("Language_Code_Used_In_Whisper","fr/en/es/de/it/ja/ar/zh/da/nl/no");
            setProperty("Language_Code_Used_In_Mlkit","fr/en/es/de/it/ja/ar/zh/da/nl/no");
            setProperty("Readspeaker_voices","roxane/kate/manuel/max/elisa/show/amir/ / /guus/");
            setProperty("BuddyOS_1.3_voices","fr:roxane/en:kate");
            setProperty("BuddyOS_1.4_voices","fr:roxane/en:kate,mark,alice/ar:amir,yasmin/de:lena,max/es:lola,manuel/it:elisa/ja:show/nl:alex,guus");


            props.addPropertyComment("Speech_volume","");
            props.addPropertyComment("Speech_volume","Speech volume (between 0 and 100)");
            setProperty("Speech_volume","100");

            props.addPropertyComment("Response_Timeout_in_seconds", "");
            props.addPropertyComment("Response_Timeout_in_seconds", "Waiting time for chatbot response and messages when exceeded");
            setProperty("Response_Timeout_in_seconds","10");
            setProperty("Message_Timeout_NotRespected_fr","Ça prend un peu de temps, la connexion est un peu lente./Aahh! Internet n'est pas très rapide aujourd'hui/une petite seconde je connecte mes circuits");
            setProperty("Message_Timeout_NotRespected_en","It takes a little time, the connection is a bit slow./ohh! The internet is not very fast today/Just a moment, I'm connecting my circuits.");

            props.addPropertyComment("Display_of_speech","");
            props.addPropertyComment("Display_of_speech","Speech display, Emotion activation, Language detection, Streaming mode, Activation stimulis, Commands and tracking (Yes/No/YesHid/NoHid)");
            setProperty("Display_of_speech", "Yes");
            setProperty("Activation_of_emotions", "Yes");
            setProperty("Language_detection", "Yes");
            setProperty("Stream_mode", "Yes");
            setProperty("Stimulis", "No");
            setProperty("Commands", "No");
            props.addPropertyComment("Tracking", "Activates user tracking.");
            setProperty("Tracking", "No");
            props.addPropertyComment("TRACKING_timeout_Switch","Enables or disables the automatic closing of the application when no person is detected");
            setProperty("TRACKING_timeout_Switch","No");

            props.addPropertyComment("Number_of_words","");
            props.addPropertyComment("Number_of_words","Minimum number of words in the response for activating language detection");
            setProperty("Number_of_words","5");
            setProperty("Detection_confidence_rate","90");


            props.addPropertyComment("Pattern_End_Phrase","");
            props.addPropertyComment("Pattern_End_Phrase","Set characters marking the end of a sentence using a regular expression for streaming TTS, fill with '</speak>' if you want to handle SSML blocks with Google Cloud TTS");
            setProperty("Pattern_End_Phrase","[.;!?][ ]{1,3}|:\\s*|\\n");

            props.addPropertyComment("Silence_time","");
            props.addPropertyComment("Silence_time","Manage end of speech detection");
            props.addPropertyComment("Silence_time","Silence time in seconds for STT ApiGoogle and Whisper");
            setProperty("Silence_time","2");

            props.addPropertyComment("Processing_the_audio_sequence","Activate processing of the audio sequence for click on the mouth and volume reduction");
            setProperty("Processing_the_audio_sequence","Yes");

            props.addPropertyComment("Volume_reduction","");
            props.addPropertyComment("Volume_reduction","Percentage of volume reduction");
            setProperty("Volume_reduction","3");

            props.addPropertyComment("Duration_sound_level_checked","");
            props.addPropertyComment("Duration_sound_level_checked", "Duration in seconds, at which the sound level should be checked");
            setProperty("Duration_sound_level_checked","3");

            props.addPropertyComment("chatBotServerNoResponce_fr", "");
            props.addPropertyComment("chatBotServerNoResponce_fr", "Responses in case of API error");
            setProperty("chatBotServerNoResponce_fr","Je n’ai pas de réponse ");
            setProperty("chatBotServerNoResponce_en","I have no response ");

            props.addPropertyComment("Chat_TextSize", "");
            props.addPropertyComment("Chat_TextSize", "Conversation window font size (between 20 and 50 px)");
            setProperty("Chat_TextSize","25");

            props.addPropertyComment("Maximum_Dialogs_in_Chat_Window","");
            props.addPropertyComment("Maximum_Dialogs_in_Chat_Window","maximum number of dialogs displayed in the Chat window");
            setProperty("Maximum_Dialogs_in_Chat_Window","100");

            props.addPropertyComment("Mail_Sender", "");
            props.addPropertyComment("Mail_Sender", "Conversation sending email");
            setProperty("Mail_Sender","TeamChat@teamnet.fr");
            setProperty("Mail_Destination","");
            setProperty("Mail_Subject_fr","Dialogues TeamChatBuddy");
            setProperty("Mail_Subject_en","TeamChatBuddy Dialogs");
            setProperty("Message_mail_send_fr","Le mail a bien été envoyé !");
            setProperty("Message_mail_send_en","The email was sent successfully!");

            props.addPropertyComment("username", "");
            props.addPropertyComment("username", "Configuring the SMTP server");
            setProperty("username","5c6edd30875fd6cea1fdccabbd267328");
            setProperty("username_Password","1c15d6edab43d80dc53a30319c29364e");
            setProperty("mail.smtp.host", "in-v3.mailjet.com");
            setProperty("mail.smtp.port", "587");

            props.addPropertyComment("ovh_identifier", "");
            props.addPropertyComment("ovh_identifier", "Unique identifier for the OVH Mail-to-SMS service");
            setProperty("ovh_identifier", "sms-fb3019-2");
            props.addPropertyComment("ovh_login", "OVH SMS account identifier (login)");
            setProperty("ovh_login", "aeva");
            setProperty("ovh_customer_password", "Aeva2025");
            setProperty("ovh_mail", "email2sms@ovh.net");
            props.addPropertyComment("ovh_num_sender", "Sender's number (international format).");
            setProperty("ovh_num_sender", "");

            props.addPropertyComment("BlueMic_Disponibility", "");
            props.addPropertyComment("BlueMic_Disponibility", "BlueMic Availability (Yes/No)");
            setProperty("BlueMic_Disponibility","No");

            props.addPropertyComment("emotion_Temperature", "");
            props.addPropertyComment("emotion_Temperature", "openAI settings for emotion detection");
            setProperty("emotion_Temperature","0");
            setProperty("emotion_Max_tokens","50");
            setProperty("emotion_Model","gpt-4o");
            setProperty("prompt_fr"," Réponds toujours et seulement le nom de l'émotion ou du sentiment parmi 'joyeux, pensif, malade, amoureux, fatigué, attentif, surpris, grincheux, effrayé, en colère, triste’ et si tu ne le trouves pas, répond neutre. ");
            setProperty("prompt_en","Answer always and only the name of the emotion or feeling among 'happy, thoughtful, sick, in love, tired, attentive, surprised, grumpy, scared, angry, sad' and if you can't find it, answer neutral.");
            setProperty("BuddyFace_Happy","joyeux/happy");
            setProperty("BuddyFace_Thinking","pensif/thoughtful");
            setProperty("BuddyFace_Sick","malade/sick");
            setProperty("BuddyFace_Love","amoureux/in love");
            setProperty("BuddyFace_Tired","fatigué/tired");
            setProperty("BuddyFace_Listening","attentif/attentive");
            setProperty("BuddyFace_Surprised","surpris/suprised");
            setProperty("BuddyFace_Grumpy","grincheux/grumpy");
            setProperty("BuddyFace_Scared","effrayé/scared");
            setProperty("BuddyFace_Angry","en colère/angry");
            setProperty("BuddyFace_Sad","triste/sad");

            props.addPropertyComment("touchLeftEye_Behavior","");
            props.addPropertyComment("touchLeftEye_Behavior","List of behaviors that will be used for each eye touch or caress");
            setProperty("touchLeftEye_Behavior","Growling");
            setProperty("touchRightEye_Behavior","Angry");
            setProperty("touchFace_Behavior","Happy");
            setProperty("touchLeftHead_Behavior","LeftHead");
            setProperty("touchRightHead_Behavior","RightHead");
            setProperty("touchCenterHead_Behavior","CenterHead");
            setProperty("touchLeftShoulder_Behavior","LeftShoulder");
            setProperty("touchRightShoulder_Behavior","RightShoulder");
            setProperty("touchHeart_Behavior","CenterHeart");

            props.addPropertyComment("use_companion_when_stimulis_disabled","");
            props.addPropertyComment("use_companion_when_stimulis_disabled","Parameter to configure whether to keep the Companion enabled when Stimulis are disabled (Yes/No).");
            setProperty("use_companion_when_stimulis_disabled","No");

            props.addPropertyComment("Listen_color","");
            props.addPropertyComment("Listen_color","Colors");
            setProperty("Listen_color","green");
            setProperty("Think_color","yellow");
            setProperty("Speak_color","blue");

            props.addPropertyComment("Mouth_messages","");
            setProperty("Mouth_messages","Yes");

            String[] Mouth_listen_fr = {"Comment puis-je vous aider ?/Que puis-je faire pour vous ?/Comment puis-je vous assister ?/Y a-t-il quelque chose avec laquelle je peux vous aider ?/Y a-t-il quelque chose que je peux faire pour vous ?/Comment puis-je vous soutenir ? Y a-t-il quelque chose avec laquelle je peux vous être utile ?"};
            String[] Mouth_speak_fr = {"D'accord, j'arrête d'écouter/Très bien, je me tais/Pas de problème, je fais une pause/Compris, je n'écoute plus/Bien sûr, je suis en attente/Écoute désactivée"};
            setProperty("Mouth_listen_fr",Arrays.toString(Mouth_listen_fr));
            setProperty("Mouth_speak_fr",Arrays.toString(Mouth_speak_fr));

            String[] Mouth_listen_en = {"How can I help you ?/What can I do for you ?/How may I assist you?/Is there anything you need help with?/Is there something I can do for you?/How can I support you? Is there anything I can help you with?"};
            String[] Mouth_speak_en = {"Alright, I'll stop listening/Okay, I’ll go silent/No problem, I'm pausing/Got it, I'm not listening anymore/Sure thing, I'm on hold/Listening disabled"};
            setProperty("Mouth_listen_en",Arrays.toString(Mouth_listen_en));
            setProperty("Mouth_speak_en",Arrays.toString(Mouth_speak_en));

            props.addPropertyComment("show_openAI_prices","");
            props.addPropertyComment("show_openAI_prices","Consumption Parameters");
            props.addPropertyComment("show_openAI_prices","Ability to display/manage openAI prices (Yes/No)");
            setProperty("show_openAI_prices","No");
            setProperty("Models_price","gpt-4o_0.005_0.015/gpt-4_0.03_0.06/gpt-4o-mini_0.00015_0.0006/whisper-1_0.006_0");
            props.addPropertyComment("ChatGPT_url","gpt-3.5-turbo_0.0005_0.0015");

            props.addPropertyComment("ChatGPT_url", "");
            props.addPropertyComment("ChatGPT_url", "ChatGPT parameters");
            setProperty("ChatGPT_url", "https://api.openai.com");
            setProperty("ChatGPT_ApiEndpoint","/v1/chat/completions");
            props.addPropertyComment("Model", "Model=gpt-3.5-turbo");
            props.addPropertyComment("Model", "Model=gpt-4");
            setProperty("Model","gpt-4o-mini");
            setProperty("Temperature",String.valueOf(0.5));

            props.addPropertyComment("Max_Tokens_req", "");
            props.addPropertyComment("Max_Tokens_req", "Max tokens : do not exceed the maximum number of tokens of the model");
            props.addPropertyComment("Max_Tokens_req", "Maximum number of tokens for the request with history");
            setProperty("Max_Tokens_req",String.valueOf(2000));
            props.addPropertyComment("Max_Tokens_resp", "Maximum number of tokens in the response");
            setProperty("Max_Tokens_resp",String.valueOf(2000));
            props.addPropertyComment("header", "gpt-3.5-turbo : maximum_tokens = 4096");
            props.addPropertyComment("header", "gpt-4 : maximum_tokens = 8192");

            props.addPropertyComment("Chatgpt_Header_en", "");
            props.addPropertyComment("Chatgpt_Header_en","Customization of ChatGPT dialogues");
            setProperty("Chatgpt_Header_en","You are a humanoid robot called BUDDY, you are an emotional robot made by the company Blue Frog Robotics in Paris, answer with 20 words maximum." );
            setProperty("Chatgpt_Header_fr","Tu es un robot humanoïde appelé BUDDY, tu es un robot émotionnel fabriqué par la société Blue Frog Robotics à paris, réponds avec 20 mots maximum.");

            props.addPropertyComment("CustomGPT_url","");
            props.addPropertyComment("CustomGPT_url","CustomGPT parameters");
            setProperty("CustomGPT_url", "https://app.customgpt.ai");
            setProperty("CustomGPT_ApiEndpoint","/api/v1/projects/{project_id}/conversations/{session_id}/messages");
            setProperty("CustomGPT_ApiEndpoint_SessionID","/api/v1/projects/{project_id}/conversations");
            setProperty("CustomGPT_model","gpt-4-turbo");

            props.addPropertyComment("CustomGPT_header_en", "");
            props.addPropertyComment("CustomGPT_header_en","Customization of CustomGPT dialogues");
            setProperty("CustomGPT_header_en","" );
            setProperty("CustomGPT_header_fr","");

            props.addPropertyComment("Response_format_fr","");
            props.addPropertyComment("Response_format_fr","Response_format_fr and Response_format_en specify response formats in French and English.");
            setProperty("Response_format_fr","<répond sans mise en forme>");
            setProperty("Response_format_en","<respond without formatting>");

            props.addPropertyComment("Response_filter","");
            props.addPropertyComment("Response_filter","This key allows filtering ChatGPT responses by replacing each first string within [] with the second string after the / in the response.");
            setProperty("Response_filter","[**/ ] [#/ ]");

            props.addPropertyComment("MLkit_timeout_in_seconds", "");
            props.addPropertyComment("MLkit_timeout_in_seconds", "Maximum waiting time (in seconds) for MLkit download before timing out");
            setProperty("MLkit_timeout_in_seconds","60");



            //-------------------------- Alert & Tracking ---------------------------
            props.addPropertyComment("ALERT_ACTIVITY","");
            props.addPropertyComment("ALERT_ACTIVITY","Alert parameters");
            props.addPropertyComment("ALERT_ACTIVITY", "Enable or disable inactivity alerts (Yes/No)");
            setProperty("ALERT_ACTIVITY", "Yes");

            props.addPropertyComment("ALERT_DURATION", "Alert duration threshold in minutes before triggering inactivity alert");
            setProperty("ALERT_DURATION", "120");

            props.addPropertyComment("ALERT_DAYS", "Days of the week when alerts are active (Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday)");
            setProperty("ALERT_DAYS", "Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday");

            props.addPropertyComment("ALERT_HOURS", "Time range for activating alerts (must be continuous within the same day, between 00:00 and 23:59, e.g. 08:00-20:00)");
            setProperty("ALERT_HOURS", "08:00-20:00");

            props.addPropertyComment("ALERT_REPETITIONS", "Number of detections required to consider a presence/activity detection as valid.");
            setProperty("ALERT_REPETITIONS", "1");
            props.addPropertyComment("ALERT_MAIL", "Email address of the user receiving the alert");
            setProperty("ALERT_MAIL", "fatimazahra.aeva@gmail.com");
            props.addPropertyComment("ALERT_SMS", "Phone number of the user receiving the SMS alert (international format)");
            setProperty("ALERT_SMS", "0727127338");

            props.addPropertyComment("ALERT_TOOL", "Alert sending method: SMS, MAIL, or both (SMS/MAIL)");
            setProperty("ALERT_TOOL", "SMS/MAIL");

            props.addPropertyComment("ALERT_MSG", "Message template for inactivity alerts.");
            setProperty("ALERT_MSG", "Hello from TeamChatBuddy, you have been inactive since [1] for [2] minutes");
            props.addPropertyComment("TRACKING_MIN_DETECTION_DURATION", "Minimum duration (in seconds) to consider a presence/activity detection as valid");
            setProperty("TRACKING_MIN_DETECTION_DURATION", "3");


            //-------------------------- COMMAND PARAMETERS ---------------------------

            props.addPropertyComment("COMMAND_Model","");
            props.addPropertyComment("COMMAND_Model","Commands parameters");
            setProperty( "COMMAND_Model", "gpt-3.5-turbo" );
            setProperty( "COMMAND_Temperature", "0" );
            setProperty("COMMAND_histo","NO");
            setProperty("COMMAND_maxdialog","30");


            setProperty( "COMMAND_Prompt_fr", "Tu es un assistant de commandes vocales. À partir de la phrase utilisateur, réponds uniquement par les commandes correspondantes entre <> si elles existent, séparées par des espaces. Si aucune commande ne correspond, ne réponds rien. Ne fais aucun ajout ou explication. ");

            setProperty("CMD_fr_1","Qu'est-ce que tu sais faire ? <CMD_NONE>");
            setProperty("CMD_fr_2","Quel est ton niveau de batterie ? <CMD_BATTERIE>");
            setProperty("CMD_fr_3","Change le volume du son à 50 (entre 0 et 100) <CMD_SOUND 50> ");
            setProperty("CMD_fr_4","Quel jour sommes-nous ? <CMD_DATE %DATE_FORMAT%>");
            setProperty("CMD_fr_5","Quelle heure est-il ? <CMD_HOUR %HOUR_FORMAT%>");
            setProperty("CMD_fr_6","Arrête d’écouter <CMD_STOP>");
            setProperty("CMD_fr_7","Quitte l’application <CMD_QUIT>");
            setProperty("CMD_fr_8","Lance-moi l’application « BuddyLab » (Buddylab/Spark) <CMD_RUN Buddylab>");
            setProperty("CMD_fr_9","Exécute une danse <CMD_DANCE>");
            setProperty("CMD_fr_10","Qu'est ce que tu vois <CMD_PHOTO %DESCRIBE_PHOTO%>");
            setProperty("CMD_fr_11","Lance le comportement énervé <CMD_BI angry>");
            setProperty("CMD_fr_12","Quelle est la météo à ville <CMD_METEO ville>");
            setProperty("CMD_fr_13","Mets-moi la radio « RTL »  <CMD_RADIO RTL>");
            setProperty("CMD_fr_14","Montre-moi une image de chien rouge <CMD_IMAGE chien rouge>");
            setProperty("CMD_fr_15","Ferme l'image <CMD_CLOSE_IMAGE>");
            setProperty("CMD_fr_16","Génère-moi une musique de « jazz avec une trompette » <CMD_MUSIC jazz avec une trompette>");
            setProperty("CMD_fr_45","Envoie à « Pierre paul » le mail « voici mon message » <CMD_MAIL [Pierre-paul] [voici mon message]>");
            setProperty("CMD_fr_47","Envoie à « Pierre paul » le sms « bonjour quoi de neuf  » <CMD_SMS [Pierre-paul] [bonjour quoi de neuf ]>");

            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_17=Mets la température à zéro <CMD_TEMP 0>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_18=Avance sur 3 mètres <CMD_MOVE 3>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_19=Tourne à droite de 30 degrés (entre -360 et 360) <CMD_TURN 30>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_20=Baisse la tête au maximum (entre -45 et 35) <CMD_HEAD -45>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_21=Regarde le plus à droite possible <CMD_LOOK 10>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_22=Résout le problème que je te montre <CMD_PHOTO %RESOLVE_PHOTO%>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_23=Traduit en français ce que je te montre <CMD_PHOTO %TRANSLATE_PHOTO%>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_24=Connectes-toi sur Dorian <HEALYSA_CONNECT Dorian>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_25=Donne 3 portions de nourriture au chat <HEALYSA_FEEDCAT 3>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_26=Prend mon rythme cardiaque <HEALYSA_HRV>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_27=Prend ma tension <HEALYSA_BLOODP>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_28=Récupère mon taux d'oxygène <HEALYSA_SPO2>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_29=Fais mon check up <HEALYSA_CHECKUP>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_30=Appelle Cyril <HEALYSA_CALL Cyril>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_31=Où se trouve Nabila <HEALYSA_LOC Nabila>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_32=Allume la lumière <SWITCHBOT_LIGHT On>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_33=Change-moi la langue en « Anglais »  (Français/Anglais/Espagnol/Allemand/Italien) <CMD_LANGUE Anglais>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_34=J’ai un vrai problème peux-tu m’aider <CMD_PROMPT %POURQUOI%>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_35=Parle-moi de Blue Frog <CMD_PROMPT %BFR%>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_36=Raconte moi une blague — optionnelle : (global/dev/limit/beauf/blondes) <CMD_JOKE global|dev|limit|beauf|blondes>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_37=Change l'entete  <CMD_HEADER %HEADER%>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_38=Arrête la radio <CMD_STOP_RADIO>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_39=Arrête la musique <CMD_STOP_MUSIC>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_40=arrête le comportement <CMD_STOP_BI>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_41=Lance le scénario aventure <CMD_SCEN aventure>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_42=Sauve l’image comme « oiseau bleu » <CMD_SAVE_IMAGE oiseau_bleu>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_43=Affiche l’image « oiseau bleu » <CMD_SHOW_IMAGE oiseau_bleu>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_44=Supprime l’image « oiseau bleu » <CMD_DEL_IMAGE oiseau_bleu>");
            props.addPropertyComment("COMMAND_Prompt_en","CMD_fr_46= Donne-moi les dernières informations (depuis 3 heures) <CMD_NEWS 3>");

            setProperty( "COMMAND_Prompt_en", "You are a voice command assistant. Based on the user's sentence, respond only with the corresponding commands enclosed in <> if they exist, separated by spaces. If no command matches, do not respond. Do not add any explanations or additional text. ");

            setProperty("CMD_en_1", "What can you do? <CMD_NONE>");
            setProperty("CMD_en_2", "What is your battery level? <CMD_BATTERIE>");
            setProperty("CMD_en_3", "Change the sound volume to 50 (between 0 and 100) <CMD_SOUND 50>");
            setProperty("CMD_en_4", "What day is it? <CMD_DATE %DATE_FORMAT%>");
            setProperty("CMD_en_5", "What time is it? <CMD_HOUR %HOUR_FORMAT%>");
            setProperty("CMD_en_6", "Stop listening <CMD_STOP>");
            setProperty("CMD_en_7", "Exit the application <CMD_QUIT>");
            setProperty("CMD_en_8", "Launch the application « BuddyLab » (Buddylab/Spark)  <CMD_RUN Buddylab>");
            setProperty("CMD_en_9", "Execute a dance <CMD_DANCE>");
            setProperty("CMD_en_10", "What do you see <CMD_PHOTO %DESCRIBE_PHOTO%>");
            setProperty("CMD_en_11", "Launch the behavior angry <CMD_BI angry>");
            setProperty("CMD_en_12", "What is the weather in city <CMD_METEO city>");
            setProperty("CMD_en_13", "Start the 'RTL' radio <CMD_RADIO RTL>");
            setProperty("CMD_en_14", "Show me an image of a red dog <CMD_IMAGE red dog>");
            setProperty("CMD_en_15", "Close the image <CMD_CLOSE_IMAGE>");
            setProperty("CMD_en_16", "Generate music « jazz with a trumpet » <CMD_MUSIC jazz with a trumpet>");
            setProperty("CMD_en_45", "Send to « Pierre paul » the email « here is my message »  <CMD_MAIL [Pierre-Paul] [here is my message]>");
            setProperty("CMD_en_47", "Send to « Pierre paul » the sms « hello how are you »  <CMD_SMS [Pierre-Paul] [hello how are you]>");

            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_17=Set the temperature to zero <CMD_TEMP 0>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_18=Move forward 3 meters <CMD_MOVE 3>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_19=Turn right 30 degrees (between -360 and 360) <CMD_TURN 30>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_20=Lower your head as far as possible (between -45 and 35) <CMD_HEAD -45>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_21=Look as far to the right as possible <CMD_LOOK 10>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_22=Solve the problem I show you <CMD_PHOTO %RESOLVE_PHOTO%>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_23=Translate into French what I show you <CMD_PHOTO %TRANSLATE_PHOTO%>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_24=Connect to Dorian <HEALYSA_CONNECT Dorian>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_25=Give the cat 3 portions of food <HEALYSA_FEEDCAT 3>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_26=Take my heart rate <HEALYSA_HRV>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_27=Take my blood pressure <HEALYSA_BLOODP>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_28=Get my oxygen level <HEALYSA_SPO2>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_29=Do my check-up <HEALYSA_CHECKUP>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_30=Call Cyril <HEALYSA_CALL Cyril>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_31=Where is Nabila <HEALYSA_LOC Nabila>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_32=Turn on the light <SWITCHBOT_LIGHT On>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_33=Change the language to « French » (Français/Anglais/Espagnol/Allemand/Italien) <CMD_LANGUE Français>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_34=I have a real problem can you help me <CMD_PROMPT %POURQUOI%>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_35=Tell me about Blue Frog <CMD_PROMPT %BFR%>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_36=Tell me a joke (dark/Any/Misc/Programming/Pun/Spooky/Christmas) <CMD_JOKE Any>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_37=Change the header <CMD_HEADER %HEADER%>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_38=Stop the radio <CMD_STOP_RADIO>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_39=Stop the music <CMD_STOP_MUSIC>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_40=stop the behaviour <CMD_STOP_BI>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_41=Starts the adventure scenario <CMD_SCEN aventure>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_42=Save the image as « blue bird » <CMD_SAVE_IMAGE blue_bird>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_43=Displays the « blue bird » image <CMD_SHOW_IMAGE blue_bird>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_44=Delete the « blue bird » image < CMD_DEL_IMAGE blue_bird>");
            props.addPropertyComment("CMD_MUSIC_fr", "CMD_en_46=Give me the latest news (from the past 3 hours) <CMD_NEWS 3>");


            setProperty( "CMD_MUSIC_fr", "Ok, je vais lire une musique correspondant à votre demande // Voila // Je n’arrive pas à générer la musique[1]" );
            setProperty( "CMD_MUSIC_en", "Ok, I will play music corresponding to your request // Done // I can't generate the music[1]" );

            setProperty( "CMD_STOP_MUSIC_fr", "Ok j'arrête la musique// " );
            setProperty( "CMD_STOP_MUSIC_en", "Ok I'll stop the music// " );

            setProperty( "CMD_DATE_fr", "// Nous sommes aujourd'hui le [1]" );
            setProperty( "CMD_DATE_en", "// Today is [1]" );

            setProperty( "CMD_HOUR_fr", "// Il est [1]" );
            setProperty( "CMD_HOUR_en", "// It's [1]" );

            setProperty( "CMD_LANGUE_fr", "Je change la langue // La langue a été changé  // Je n’arrive pas à changer la langue" );
            setProperty( "CMD_LANGUE_en", "I'm changing the language // The language has been changed  // I can't change the language" );

            setProperty( "CMD_TEMP_fr", "Je change la température // La température a été changé" );
            setProperty( "CMD_TEMP_en", "I'm changing the temperature // The temperature has been changed" );

            setProperty( "CMD_BATTERIE_fr", "Je vérifie le niveau de batterie // Mon niveau de batterie est de [1] pourcents" );
            setProperty( "CMD_BATTERIE_en", "I am checking the battery level // My battery level is [1] percent" );

            setProperty( "CMD_SOUND_fr", "Je modifie le volume // Le volume a été modifié" );
            setProperty( "CMD_SOUND_en", "I am modifying the volume // The volume has been modified" );

            setProperty( "CMD_MOVE_fr", "Ok, je me déplace // Voila je me suis déplacé" );
            setProperty( "CMD_MOVE_en", "Ok, I'm moving // Here I've moved" );

            setProperty( "CMD_TURN_fr", "Ok, je tourne // Voila j'ai tourné" );
            setProperty( "CMD_TURN_en", "Ok, I'm turning // Here I've turned" );

            setProperty( "CMD_HEAD_fr", "Ok, je bouge ma tête // Voila j'ai bougé ma tête" );
            setProperty( "CMD_HEAD_en", "Ok, I'm moving my head // There I moved my head" );

            setProperty( "CMD_STOP_fr", "Ok j'arrête // " );
            setProperty( "CMD_STOP_en", "Ok I'll stop // " );

            setProperty( "CMD_QUIT_fr", "D'accord, je vais fermer l'application // " );
            setProperty( "CMD_QUIT_en", "Ok I'll close the app // " );

            setProperty( "CMD_RUN_fr", "D'accord, je vais lancer l'application [1] // L'application [1] n'existe pas " );
            setProperty( "CMD_RUN_en", "Ok I'll run the app [1] // Application [1] does not exist" );

            setProperty( "CMD_SCEN_fr", "D'accord, je vais lancer le scénario [1] // le scénario est introuvable" );
            setProperty( "CMD_SCEN_en", "Ok I'll run the scenario [1] // the scenario is not found" );

            setProperty( "CMD_DANCE_fr", "Ok je vais danser  // T'as vu comment je danse bien // Problème lors de l'exécution du comportement [1] " );
            setProperty( "CMD_DANCE_en", "Ok I'm going to dance // Have you seen how well I dance // Issue during behavior execution [1]" );

            setProperty( "CMD_METEO_fr", "Je vérifie la météo à [1] // Le temps à [1] est [2], il fait [3]°C // Je n’arrive pas à récupérer la météo" );
            setProperty( "CMD_METEO_en", "I'm checking the weather at [1] // The weather at [1] is [2], it's [3]°C // I can't retrieve the weather" );

            setProperty( "CMD_RADIO_fr", "Je lance la radio [1] // Je n’arrive pas à lancer la radio[1]" );
            setProperty( "CMD_RADIO_en", "I'm starting the radio [1] // I can’t start the radio[1]" );

            setProperty( "CMD_STOP_RADIO_fr", "Ok j'arrête la radio// " );
            setProperty( "CMD_STOP_RADIO_en", "Ok I'll stop the radio// " );

            setProperty( "SWITCHBOT_LIGHT_fr", "Je mets la lumière [1] // C'est fait // Je n’arrive à allumer la lumière" );
            setProperty( "SWITCHBOT_LIGHT_en", "I'm turning [1] the light // Done // I can't turn on the light" );

            setProperty( "HEALYSA_CONNECT_fr", "Je te connecte à la plateforme Healysa // Tu es connecté // Erreur lors de la connexion[1]" );
            setProperty( "HEALYSA_CONNECT_en", "I am connecting you to the Healysa platform // You are connected // Error during connection[1]" );

            setProperty( "HEALYSA_HRV_fr", "Je prends ton rythme cardiaque // Ta fréquence cardiaque est de [1] bpm à [2] // Aucune mesure de rythme cardiaque n'a été effectuée aujourd'hui." );
            setProperty( "HEALYSA_HRV_en", "I'm taking your heart rate // Your heart rate is [1] bpm at [2] // No heart rate measurement was taken today." );

            setProperty( "HEALYSA_BLOODP_fr", "Je prends ta tension // Ta tension systolique est de [1] et diastolique de [2] à [3] // Aucune mesure de tension n'a été effectuée aujourd'hui.");
            setProperty( "HEALYSA_BLOODP_en", "I'm taking your blood pressure // Your systolic blood pressure is [1] and diastolic is [2] at [3] // No blood pressure measurement was taken today.");

            setProperty( "HEALYSA_SPO2_fr", "Je prends ton taux d'oxygène // Ton taux d'oxygène est de [1] à [2] // Aucune valeur de taux d'oxygène n'a été mesurée aujourd'hui");
            setProperty( "HEALYSA_SPO2_en", "I'm taking your oxygen level // Your oxygen level is [1] at [2] // No oxygen level value was measured today.");

            setProperty( "HEALYSA_CHECKUP_fr", "Je fais ton check up // Ta fréquence cardiaque est de [1], ta tension systolique est de [2] et diastolique de [3], ton taux d'oxygène est de [4]");
            setProperty( "HEALYSA_CHECKUP_en", "I'm doing your check up // Your heart rate is [1], your systolic blood pressure is [2] and diastolic blood pressure is [3], your oxygen level is [4]");


            setProperty( "HEALYSA_CALL_fr", "Je lance l'appel // Voila // Erreur lors de l'appel[1]" );
            setProperty( "HEALYSA_CALL_en", "I'll start the call // Done // Error during call[1]" );

            setProperty( "HEALYSA_LOC_fr", "Un instant je cherche // Il est à [1] // Je n’arrive pas à localiser [1] [2]" );
            setProperty( "HEALYSA_LOC_en", "Wait I'm searching // He's at [1] // I can’t locate [1] [2]" );

            setProperty( "HEALYSA_FEEDCAT_fr", "Ok je nourris le chat // Il est servi // Je n’arrive pas à nourrir le chat[1]" );
            setProperty( "HEALYSA_FEEDCAT_en", "Ok I'll feed the cat // He is served // I can't feed the cat [1]" );

            setProperty( "CMD_IMAGE_fr", "Je vais vous montrer une image correspondant à votre demande // Voici l'image // Problème lors du traitement de l'image[1]" );
            setProperty( "CMD_IMAGE_en", "I will show you an image corresponding to your request // Here is the image // Issue during image processing[1]" );

            setProperty( "CMD_CLOSE_IMAGE_fr", "Ok je ferme l'image" );
            setProperty( "CMD_CLOSE_IMAGE_en", "Ok I'll close the image" );

            setProperty( "CMD_SAVE_IMAGE_fr", "Ok, je vais sauvegarder l'image // l'image est sauvegardée // aucune image existe pour la sauvegarde");
            setProperty( "CMD_SAVE_IMAGE_en", "Ok, I will save the image // the image is saved // no image exists for saving");

            setProperty( "CMD_SHOW_IMAGE_fr", "Ok, j'affiche l'image // aucune image correspondante n’existe");
            setProperty( "CMD_SHOW_IMAGE_en", "Ok, I will display the image // No matching image exists");

            setProperty( "CMD_DEL_IMAGE_fr", "Ok, je vais supprimer l'image // aucune image correspondante n’existe");
            setProperty( "CMD_DEL_IMAGE_en", "Ok, I will delete the image // No matching image exists");

            setProperty("CMD_PHOTO_fr","Je vais prendre une photo // Problème lors de la prise de photo[1]");
            setProperty("CMD_PHOTO_en","I will take a picture // Problem while taking the photo[1]");

            setProperty( "CMD_BI_fr", "Je lance le comportement [1] // C'est bon // Problème lors de l'exécution du comportement [1]" );
            setProperty( "CMD_BI_en", "I'm starting the behaviour [1] // Done // Issue during behavior execution [1]" );

            setProperty( "CMD_JOKE_fr", "Ok, Voici une nouvelle blague // Voila j’espère que ma blague t’a fait rire // Aucune blague correspondante trouvée" );
            setProperty( "CMD_JOKE_en", "Ok, here’s a new joke // There you go, I hope my joke made you laugh // No matching joke found" );

            setProperty( "CMD_HEADER_fr", "// L'en-tête est modifié" );
            setProperty( "CMD_HEADER_en", "// The header is changed" );

            setProperty( "CMD_MAIL_fr", "Ok, je vais envoyer le mail // le mail est envoyé // Erreur lors d'envoi du mail[1]");
            setProperty( "CMD_MAIL_en", "Ok, I will send the mail // The email has been sent // Error while sending the email[1]" );

            setProperty("CMD_STOP_BI_fr","Ok j’arrête le comportement// " );
            setProperty("CMD_STOP_BI_en","Ok I'll stop the behaviour // ");

            setProperty("CMD_NEWS_fr","// Problème lors de la récupération des actualités " );
            setProperty("CMD_NEWS_en","// Problem retrieving news");


            setProperty("CMD_SMS_fr", "Ok, je vais envoyer le SMS [1] à [2]// Le SMS [1] est envoyé à [2] // Erreur lors de l’envoi du SMS [1] à [2] : [3]");
            setProperty("CMD_SMS_en", "Ok, I will send the sms [1] to [2]// The sms [1] has been sent to [2] //Error while sending the SMS [1] to [2] : [3]");

            setProperty("BFR","Raconte l’histoire de la société Blue Frog Robotics depuis sa création en 2014 jusqu’à maintenant et ce que le robot Buddy qu’elle fabrique est capable de faire");
            setProperty("POURQUOI","Aidez-moi à explorer mon problème en utilisant la technique des « n Pourquoi ». Suivez ces étapes, mais ne les énumérez pas : 1. Demandez-moi de décrire le problème auquel je suis confronté. 2. Reconnaissez mon problème et demandez pourquoi je pense que cela se produit. 3. En vous basant sur ma réponse, demandez pourquoi cela se produit. 4. Continuez à demander pourquoi de la manière la plus optimale en fonction de chacune de mes réponses jusqu'à ce que vous ayez demandé « pourquoi » n (généralement 3 à 10) fois jusqu'à ce que nous atteignions la cause profonde. 5. Résumez en détail la cause profonde potentielle que vous avez identifiée sur la base de mes réponses. 6. Demandez-moi de réfléchir aux actions ou aux changements que je peux entreprendre pour remédier à cette cause profonde. 7. Proposez de recommencer le processus pour approfondir le problème. Assurez-vous d'ajuster les questions de manière dynamique en fonction de mes réponses. Ne soyez pas trivial et posez des questions de bas niveau, attaquez-vous vraiment au problème. Travaillez étape par étape.");
            setProperty("HEADER","Raconte l’histoire de la société Blue Frog Robotics depuis sa création en 2014 jusqu’à maintenant et ce que le robot Buddy qu’elle fabrique est capable de faire");

            props.addPropertyComment("DATE_format", "");
            setProperty("DATE_format","");
            setProperty("HOUR_format","");
            props.addPropertyComment("DATE_format", "Si le champ DATE_format est vide, il prend par défaut la valeur EEEE d MMMM yyyy");
            props.addPropertyComment("HOUR_format", "Si le champ HOUR_format est vide, il prend par défaut la valeur h:mm");

            props.addPropertyComment("Image_URL","");
            setProperty( "Image_URL", "https://api.openai.com/" );
            setProperty( "Image_endpoint", "v1/images/generations" );
            setProperty( "Image_model", "dall-e-2" );
            setProperty( "Image_size", "512x512" );

            props.addPropertyComment("Music_URL","");
            setProperty( "Music_URL", "http://34.34.175.122:5000" );
            setProperty( "Music_endpoint", "/generate_music" );
            setProperty( "Music_model", "small" );
            setProperty( "Music_duration", "20" );

            props.addPropertyComment("Picture_Description_URL","");
            setProperty("Picture_Description_URL","https://api.openai.com/");
            setProperty("Picture_Description_model","gpt-4o");
            setProperty("Picture_Description_max_tokens","300");
            setProperty("Picture_Description_resolution","Low/Medium/High");
            setProperty("DESCRIBE_PHOTO","Décris moi cette image de façon détaillée sans * ni markdown");
            setProperty("RESOLVE_PHOTO","Résout le problème sur la photo de manière claire et détaillé");
            setProperty("TRANSLATE_PHOTO","traduit en français ce que tu lis sur la photo");

            props.addPropertyComment("Meteo_URL","");
            setProperty("Meteo_URL","https://api.openweathermap.org");
            setProperty("Meteo_API_Key","060ec1f9ecb0a63e0045980f1cf480b9");

            props.addPropertyComment("Radio_URL","");
            setProperty("Radio_URL","https:/bluefrogrobotics-bhqtj3.api.radioline.fr");
            setProperty("Radio_XCSRF_Token","MGdbCYLTMspN84n4QMZOIAhrLcyTBTAcPEc9feb7xe05oBnQoDwOyLMmsHAppZVI");

            props.addPropertyComment("BI_danse","");
            setProperty( "BI_danse", "Dance01/Victory02" );

            props.addPropertyComment("Healysa_URL_PROD","");
            props.addPropertyComment("Healysa_URL_PROD","Healysa");
            setProperty( "Healysa_URL_PROD", "https://care.healysa.io/" );
            setProperty( "Healysa_mail", "" );
            setProperty( "Healysa_password", "" );
           

            props.addPropertyComment("SwitchBot","");
            setProperty("Switchbot_token","" );
            setProperty("Switchbot_id","" );

            props.addPropertyComment("JOKE_URL", "");
            setProperty("JOKE_URL_fr", "https://blague-api.vercel.app/");
            setProperty("JOKE_URL","https://v2.jokeapi.dev/joke/");
            setProperty("pause_JOKE","3");
            setProperty("JOKE_fr","Yes");

            props.addPropertyComment("NEWS_ARTICLE_BBC_id", " HTML selector of the news web page https://www.bbc.com/");
            setProperty("NEWS_ARTICLE_BBC_id","div.sc-666b6d83-0");

            props.addPropertyComment("mail_pierre-paul", "");
            setProperty("mail_pierre-paul","fatimazahra.aeva@gmail.com");
            setProperty("CMD_MAIL_subject_en","Email from TeamChatBuddy");
            setProperty("CMD_MAIL_subject_fr","Mail de TeamChatBuddy");

            props.addPropertyComment("sms_pierre-paul", "");
            props.addPropertyComment("sms_pierre-paul", "Phone number to receive sms (international format)");
            setProperty("sms_pierre-paul","0727127338");


            //-------------------------- Tracking ---------------------------
            props.addPropertyComment("TRACKING_Camera","");
            props.addPropertyComment("TRACKING_Camera", "Tracking parameters");
            props.addPropertyComment("TRACKING_Camera", "Enabling tracking with/without the camera.");
            setProperty("TRACKING_Camera","No");
            props.addPropertyComment("TRACKING_Head", "Enabling tracking with/without head.");
            setProperty("TRACKING_Head","No");
            props.addPropertyComment("TRACKING_Body", "Enabling tracking with/without body.");
            setProperty("TRACKING_Body","No");
            props.addPropertyComment("WELCOME_hotword", "Enables/disables the invitation feature (via hotword)");
            setProperty("WELCOME_hotword","No");
            props.addPropertyComment("WELCOME_tracking", "Enables/disables the invitation feature (via tracking)");
            setProperty("WELCOME_tracking","No");

            props.addPropertyComment("TRACKING_watch","");
            props.addPropertyComment("TRACKING_watch", "Tracking is performed only if the robot detects that the target is looking at it");
            setProperty("TRACKING_watch","Yes");
            props.addPropertyComment("TRACKING_delay_nowatching", "The time delay in seconds for re-tracking and re-centering the gaze and head when the person is no longer looking at it depends on the specific operation of the tracking system being used.");
            setProperty("TRACKING_delay_nowatching","10");
            props.addPropertyComment("TRACKING_listening", "Start listening when someone watchs");
            setProperty("TRACKING_listening","Yes");
            props.addPropertyComment("TRACKING_delay_startlisten", "Delay in seconds for listening when someone watchs");
            setProperty("TRACKING_delay_startlisten","2");
            props.addPropertyComment("TRACKING_delay_stoplisten", "Delay in seconds to stop listening when no one watchs");
            setProperty("TRACKING_delay_stoplisten","3");
            props.addPropertyComment("TRACKING_regard_center", "Time to refocus the pupils of the eyes if the person being monitored no longer looks towards the robot");
            setProperty("TRACKING_regard_center","30");
            props.addPropertyComment("TRACKING_timeout", "Time in seconds to close TeamChatBuddy if no person is tracked within this duration ");
            setProperty("TRACKING_timeout","0");
            props.addPropertyComment("WATCHING_timeout","Time in seconds to close TeamChatBuddy if no person is watching Buddy within this duration ");
            setProperty("WATCHING_timeout","0");
            props.addPropertyComment("WELCOME_delay", "Delay in minutes to welcome after watching no one");
            setProperty("WELCOME_delay","10");
            props.addPropertyComment("WELCOME_duration_tracking", "the person looks at Buddy for 2 seconds to issue an invitation");
            setProperty("WELCOME_duration_tracking","2");
            props.addPropertyComment("WELCOME_messages_FR", "Welcoming messages");
            String[] TRACKING_INVITATIONS_FR = {"Bonjour, comment puis-je vous aider?/Bonjour ! En quoi puis-je rendre votre journée plus agréable?/Bonjour, que puis-je faire pour vous aujourd'hui?"};
            setProperty("WELCOME_messages_FR", Arrays.toString(TRACKING_INVITATIONS_FR));
            String[] TRACKING_INVITATIONS_EN = {"Hello, how can I help you?/Hello! How can I make your day more enjoyable?/Hello, what can I do for you today?"};
            setProperty("WELCOME_messages_EN", Arrays.toString(TRACKING_INVITATIONS_EN));
            props.addPropertyComment("WELCOME_CHATGPT", "Welcome message with ChatGPT");
            setProperty("WELCOME_CHATGPT","Yes");
            props.addPropertyComment("WELCOME_model", "Welcome ChatGPT model");
            setProperty("WELCOME_model","gpt-4o-mini");
            props.addPropertyComment("WELCOME_temperature", "Welcome Temperature");
            setProperty("WELCOME_temperature","0.5");
            props.addPropertyComment("WELCOME_prompt_FR", "Welcome Prompt for ChatGPT");
            setProperty("WELCOME_prompt_FR","dis une invitation au dialogue");
            setProperty("WELCOME_prompt_EN","say an invitation to dialogue");
            props.addPropertyComment("WELCOME_maxtoken", "Welcome max token");
            setProperty("WELCOME_maxtoken","100");

            FileOutputStream fileOut = new FileOutputStream(configFile);
            props.store(fileOut, "TeamChatBuddy configuration file - Compatible with "+app_version);
            fileOut.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return initOrMajOrNone;
    }

    public static void setProperty(String key, String value) {
        // Always set the property, even if it already exists
        props.setProperty(key, value);
    }

}
