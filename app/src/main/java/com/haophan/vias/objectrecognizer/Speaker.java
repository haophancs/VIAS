package com.haophan.vias.objectrecognizer;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;

import com.haophan.vias.R;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by USER on 10/8/2017.
 */

public class Speaker {

    private String currentLang;
    private TextToSpeech tts;

    private Vibrator vibrator;

    private String pre_result = "";

    private String sentence;

    private int recognitionTimes = 0;

    public boolean entrySoundDone = false;

    private Context context;

    public Speaker(Context context, String lang){
        currentLang = lang;
        this.context = context;
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    if (currentLang.equals("vi")){
                        tts.setLanguage(new Locale("vi"));
                        tts.setSpeechRate(1.38f);
                        sentence = "Tôi thấy ";
                    } else {
                        tts.setLanguage(Locale.US);
                        sentence = "I see ";
                    }
                }
            }
        });
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setResults(final List<Classifier.Recognition> results){

        String title = "";

        for (Classifier.Recognition recog : results) {
            if (recog.getTitle() != null) {
                title = recog.getTitle();
            }
        }
        if (!title.equals(pre_result)) {
            if (!tts.isSpeaking()) {

                String s = sentence + title;
                if (recognitionTimes != 0) {
                    if (!title.equals("")) {
                        speak(s, s);
                    }
                } else {
                    speak(s + ". Đã nhận dạng xong, để tiếp tục nhận dạng, hãy tap vào màn hình. Để thoát nhận dạng vật thể, hãy nhấn giữ màn hình",
                            "To continue recognizing, tap on the screen. To close object recognizer, hold on screen");
                    recognitionTimes++;
                    entrySoundDone = true;
                }
            }
            pre_result = title;
        }
    }

    public void speak(String viText, String enText){
        vibrator.vibrate(200);
        HashMap<String, String> myHashAudio = new HashMap<String, String>();

        int streamType = context.getResources().getString(R.string.audio_stream).equals("alarm")?AudioManager.STREAM_ALARM:AudioManager.STREAM_MUSIC;
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(streamType));
        if (currentLang.equals("vi")){
            tts.speak(viText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
        } else {
            tts.speak(enText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
        }
    }
}
