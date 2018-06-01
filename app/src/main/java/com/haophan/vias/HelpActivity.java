package com.haophan.vias;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class HelpActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private final String vieHelpFile = "vie_help.txt";
    private final String engHelpFile = "eng_help.txt";

    WebView textViewHelp;
    Button btnHelp;

    private ArrayList<String> helpSentences;
    private String currentLang;
    private String appPreference = "app_pref";

    int sentenceIndex = -1;
    int backBtnTap = 0;
    int headSetBtnTap = 0;
    Boolean paused = false;

    TextToSpeech tts;
    Vibrator vibrator;

    boolean stop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_help);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_back));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMySelf();
            }
        });

        textViewHelp = (WebView) findViewById(R.id.textViewHelp);
        textViewHelp.setVerticalScrollBarEnabled(true);
        textViewHelp.setBackgroundColor(Color.parseColor("#ffffff"));

        btnHelp = (Button) findViewById(R.id.buttonHelp);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        currentLang = getCurrentLang();

        helpSentences = new ArrayList<String>();
        helpSentences.clear();

        tts = new TextToSpeech(getApplicationContext(), this);

        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(100);
                paused = !paused;
                tts.stop();
                if (paused) {
                    Speak("Tạm dừng", "pause", "");
                    showToastMessage("Tạm dừng", "Paused");
                } else {
                    if (sentenceIndex >= 0) {
                            sentenceIndex--;
                        Speak("xin được tiếp tục, ", "resume reading, ", "help");
                        showToastMessage("Xin được tiếp tục", "Resume reading");
                    } else {
                        sentenceIndex = -1;
                        readHelp();
                    }
                }
            }
        });

        btnHelp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                vibrator.vibrate(300);
                btnHelp.setEnabled(false);
                paused = true;
                tts.stop();
                Speak("thoát hướng dẫn sử dụng, trở lại màn hình chính", "close user manual", "");
                showToastMessage("Thoát hướng dẫn sử dụng, trở lại màn hình chính", "close user manual");
                finishAndRemoveTask();
                return false;
            }
        });
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            String[] helpContent;
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {

                }

                @Override
                public void onDone(String utteranceId) {
                    if (!paused){
                        if (utteranceId.equals("help")){
                            readHelp();
                        }
                    }
                }

                @Override
                public void onError(String utteranceId) {

                }
            });
            if (currentLang.equals("vi")) {
                btnHelp.setText("HƯỚNG DẪN SỬ DỤNG");
                tts.setLanguage(new Locale("vi"));
                tts.setSpeechRate(1.42f);
                helpContent = getHelpContent(vieHelpFile).replace("VIAS", "Vias").split("\n");
            } else {
                btnHelp.setText("USER MANUAL");
                tts.setLanguage(Locale.US);
                helpContent = getHelpContent(engHelpFile).replace("VIAS", "Vias").split("\n");
            }
            for (String aHelpContent : helpContent) {
                helpSentences.add(aHelpContent + ", ");
            }
            sentenceIndex = -1;
            paused = false;
            Speak("sau đây là hướng dẫn sử dụng", "this is user manual", "help");
        }
    }

    private void stopMySelf(){
        paused = true;
        tts.stop();
        backBtnTap++;
        Speak("thoát hướng dẫn sử dụng, trở lại màn hình chính", "exit user manual and return to main screen", "");
        showToastMessage("Thoát hướng dẫn sử dụng, trở lại màn hình chính", "Exit user manual and return to main screen");
        finishAndRemoveTask();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if ((keyCode == KeyEvent.KEYCODE_BACK)){
            paused = true;
            tts.stop();
            backBtnTap++;

            if (backBtnTap >= 2) {
                Speak("Thoát hướng dẫn sử dụng, trở lại màn hình chính", "exit user manual and return to main screen", "");
                showToastMessage("Thoát hướng dẫn sử dụng, trở lại màn hình chính", "Exit user manual and return to main screen");
                stop = true;
                finishAndRemoveTask();
            } else {
                showToastMessage("Nhấn back lần nữa để thoát hướng dẫn sử dụng", "Tap again to return to main screen");
                Speak("Nhấn back lần nữa để thoát hướng dẫn sử dụng", "Tap again to exit user manual", "");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!stop) {
                            if (backBtnTap > 0) {
                                backBtnTap = 0;
                                paused = false;
                                if (sentenceIndex >= 0) {
                                    sentenceIndex--;
                                    Speak("xin được tiếp tục, ", "resume reading, ", "help");
                                    showToastMessage("Xin được tiếp tục", "Resume reading");
                                } else {
                                    sentenceIndex = -1;
                                    readHelp();
                                }
                                ;
                            }
                        }
                    }
                }, 3000);
            }
        } else if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            vibrator.vibrate(100);
            headSetBtnTap++;

            if (headSetBtnTap < 2) {
                paused = !paused;
                tts.stop();
                if (paused) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (headSetBtnTap < 2) {
                                Speak("Tạm dừng", "pause", "");
                                showToastMessage("Tạm dừng", "Paused");
                            }
                        }
                    }, 200);
                } else {
                    if (sentenceIndex >= 0) {
                        sentenceIndex--;
                        Speak("xin được tiếp tục, ", "resume reading, ", "help");
                        showToastMessage("Xin được tiếp tục", "Resume reading");
                    } else {
                        sentenceIndex = -1;
                        readHelp();
                    }
                }
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        headSetBtnTap = 0;
                    }
                }, 500);
            } else {
                stopMySelf();
            }
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            vibrator.vibrate(300);
            btnHelp.setEnabled(false);
            paused = true;
            tts.stop();
            Speak("thoát hướng dẫn sử dụng, trở lại màn hình chính", "close user manual", "");
            showToastMessage("Thoát hướng dẫn sử dụng, trở lại màn hình chính", "close user manual");
            finishAndRemoveTask();
        }
        return false;
    }

    public void readHelp(){
        sentenceIndex++;
        if (sentenceIndex >= 0 && sentenceIndex < helpSentences.size()) {
            String s = helpSentences.get(sentenceIndex);
            Speak(s, s, "help");
        } else {
            sentenceIndex = -1;
            paused = true;
            tts.stop();
            Speak("hướng dẫn sử dụng đã xong, cảm ơn bạn đã nghe", "I've just read user manual, thanks for your listening", "");
            showToastMessage("Hướng dẫn sử dụng đã xong, cảm ơn bạn đã nghe", "I've just read user manual, thanks for your listening");
        }
    }

    private void showToastMessage(final String viText, final String enText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast;
                switch (currentLang) {
                    case "vi":
                        toast = Toast.makeText(getApplicationContext(), viText, Toast.LENGTH_SHORT);
                        break;

                    default:
                        toast = Toast.makeText(getApplicationContext(), enText, Toast.LENGTH_SHORT);
                        break;
                }
                ViewGroup group = (ViewGroup) toast.getView();
                TextView msgTv = (TextView) group.getChildAt(0);
                msgTv.setTextSize(23);
                toast.show();
            }
        });
    }

    private void Speak(String viText, String enText, String message){
        if (tts.isSpeaking()) tts.stop();

        HashMap<String, String> myHashAudio = new HashMap<String, String>();
        int streamType = getResources().getString(R.string.audio_stream).equals("alarm")?AudioManager.STREAM_ALARM:AudioManager.STREAM_MUSIC;
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(streamType));
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, message);

        switch (currentLang){
            case "vi":
                tts.speak(viText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
                break;

            default:
                tts.speak(enText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
                break;
        }
    }

    private String getHelpContent(String textFile){
        String content = "";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open(textFile)));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                //process line
                content += mLine + "\n";
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
        if (!content.equals("")){
            String text;
            String htmlwords = content.replaceAll("\n", "<br>");
            text = "<html><body><p align=\"justify\">";
            text+= "<font color=\"#424242\""+htmlwords+"</font>";
            text+= "</p></body></html>";
            textViewHelp.loadData(text, "text/html", "utf-8");
            return content;
        } else {
            if (currentLang.equals("vi"))
                return "đã xảy ra lỗi, không tìm thấy nội dung";
            else
                return "content not found";
        }
    }
    private String getCurrentLang(){
        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }
}
