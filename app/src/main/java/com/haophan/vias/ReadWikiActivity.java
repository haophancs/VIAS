package com.haophan.vias;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class ReadWikiActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    WebView textViewWiki;
    Button btnWiki;

    private ArrayList<String> wikiSentences;
    private String rootContent = "";
    private String wikiLink = "";

    private String currentLang;
    private String appPreference = "app_pref";

    int sentenceIndex = -1;
    int backBtnTap = 0, headSetBtnTap = 0;
    Boolean paused = false;

    TextToSpeech tts;
    Vibrator vibrator;

    boolean stop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_wiki);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_wiki);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_back));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(300);
                btnWiki.setEnabled(false);
                paused = true;
                Speak("thoát đọc wikipedia, trở lại màn hình chính", "close reading wikipedia", "");
                showToastMessage("Thoát đọc wikipedia, trở lại màn hình chính", "Close Wikipedia reader");
                finishAndRemoveTask();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tts.shutdown();
                    }
                }, 1500);
            }
        });
        textViewWiki = (WebView) findViewById(R.id.textViewWiki);
        textViewWiki.setVerticalScrollBarEnabled(true);
        textViewWiki.setBackgroundColor(Color.parseColor("#ffffff"));

        btnWiki = (Button) findViewById(R.id.buttonWiki);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        currentLang = getCurrentLang();

        wikiSentences = new ArrayList<String>();
        wikiSentences.clear();

        Intent intent = getIntent();
        wikiLink = intent.getStringExtra("wiki_link");

        tts = new TextToSpeech(getApplicationContext(), this);

        btnWiki.setOnClickListener(new View.OnClickListener() {
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
                        Speak("xin được tiếp tục, ", "resume reading, ", "wiki");
                        showToastMessage("Xin được tiếp tục", "Resume reading");
                    } else {
                        sentenceIndex = -1;
                        readWiki();
                    }
                }
            }
        });

        btnWiki.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                vibrator.vibrate(300);
                btnWiki.setEnabled(false);
                paused = true;
                tts.stop();
                Speak("thoát đọc wikipedia, trở lại màn hình chính", "close reading wikipedia", "");
                showToastMessage("Thoát đọc wikipedia, trở lại màn hình chính", "close reading wikipedia");
                finishAndRemoveTask();
                return false;
            }
        });
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {

                }

                @Override
                public void onDone(String utteranceId) {
                    if (!paused) {
                        if (utteranceId.equals("wiki")) {
                            readWiki();
                        }
                    }
                }

                @Override
                public void onError(String utteranceId) {

                }
            });

            if (currentLang.equals("vi")) {
                btnWiki.setText("ĐỌC WIKIPEDIA");
                tts.setLanguage(new Locale("vi"));
                tts.setSpeechRate(1.42f);
            } else {
                btnWiki.setText("WIKIPEDIA READER");
                tts.setLanguage(Locale.US);
            }

            new htmlTextPuller().execute(wikiLink);
        }
    }

    private void stopMySelf(){
        paused = true;
        tts.stop();
        backBtnTap++;
        Speak("thoát đọc wikipedia, trở lại màn hình chính", "close reading wikipedia and return to main screen", "");
        showToastMessage("Thoát đọc wikipedia, trở lại màn hình chính", "Close reading wikipedia and return to main screen");
        finishAndRemoveTask();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if ((keyCode == KeyEvent.KEYCODE_BACK)){
            paused = true;
            tts.stop();
            backBtnTap++;

            if (backBtnTap >= 2) {
                Speak("Thoát đọc wikipedia, trở lại màn hình chính", "exit user manual and return to main screen", "");
                showToastMessage("Thoát đọc wikipedia, trở lại màn hình chính", "exit user manual and return to main screen");
                stop = true;
                finishAndRemoveTask();
            } else {
                showToastMessage("Chạm lần nữa để thoát đọc wikipedia", "Tap again to return to main screen");
                Speak("Chạm lần nữa để thoát đọc wikipedia", "Tap again to exit user manual", "");
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
                                    Speak("xin được tiếp tục, ", "resume reading, ", "wiki");
                                    showToastMessage("Xin được tiếp tục", "Resume reading");
                                } else {
                                    sentenceIndex = -1;
                                    readWiki();
                                }
                                ;
                            }
                        }
                    }
                }, 3000);
            }
        } else if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            vibrator.vibrate(100);

            headSetBtnTap++;

            if (headSetBtnTap < 2) {
                paused = !paused;
                tts.stop();
                if (paused) {
                    if (headSetBtnTap < 2) {
                        Speak("Tạm dừng", "pause", "");
                        showToastMessage("Tạm dừng", "Paused");
                    }

                } else {
                    if (sentenceIndex >= 0) {
                        sentenceIndex--;
                        Speak("xin được tiếp tục, ", "resume reading, ", "wiki");
                        showToastMessage("Xin được tiếp tục", "Resume reading");
                    } else {
                        sentenceIndex = -1;
                        readWiki();
                    }
                }
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        headSetBtnTap = 0;
                    }
                }, 200);

            } else {
                stopMySelf();
            }
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        vibrator.vibrate(300);
        btnWiki.setEnabled(false);
        paused = true;
        tts.stop();
        Speak("thoát đọc wikipedia, trở lại màn hình chính", "close reading wikipedia", "");
        showToastMessage("Thoát đọc wikipedia, trở lại màn hình chính", "close reading wikipedia");
        finishAndRemoveTask();
       // return false;
        return super.onKeyLongPress(keyCode, event);
    }

    public void readWiki(){
        sentenceIndex++;
        if (sentenceIndex >= 0 && sentenceIndex < wikiSentences.size()) {
            String s = wikiSentences.get(sentenceIndex);
            Speak(s, s, "wiki");
        } else {
            sentenceIndex = -1;
            paused = true;
            tts.stop();
            Speak("Đã đọc xong, cảm ơn bạn đã nghe", "Reading done, thanks for your listening", "");
            showToastMessage("Đã đọc xong, cảm ơn bạn đã nghe", "Reading done, thanks for your listening");
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

    private class htmlTextPuller extends AsyncTask<String, Void, Void> {

        String words;
        String title = "";

        @Override
        protected Void doInBackground(String... params) {
            words = "";
            try {
                Document doc = Jsoup.connect(params[0]).get();
                Elements paragraphs = doc.select(".mw-content-ltr p, .mw-content-ltr li");
                Element firstPara = paragraphs.first();
                Element lastPara = paragraphs.last();
                Element p;

                StringBuilder sb = new StringBuilder();

                p = firstPara;
                sb.append(p.text());

                int i = 1;
                while (p != lastPara){
                    p = paragraphs.get(i);
                    sb.append(p);
                    i++;
                }
                words = sb.toString();
                words = Html.fromHtml(words).toString().trim();
                //words = words.replaceAll( String.format("[a]"))
                title = params[0].replace("https://vi.wikipedia.org/wiki/", "");
                title = title.replace("https://en.wikipedia.org/wiki/", "");
                title = title.replace("_", " ");

            } catch (IOException e) {
                //e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (words != null) {

                if (!words.equals("")){
                    for (int i = 0; i <= 9; i++){
                        for (int j = 0; j <= 9; j++){
                            words = words.replace("[" + i + "]", "");
                            words = words.replace("[" + i + "" + j + "]", "");
                            words = words.replace(i + "." + j, i + "" + j);
                        }
                    }
                    String text = "";
                    String htmlwords = words.replaceAll("\n", "<br>");
                    text+= "<html>";
                    text+= "<body>";

                    if (!words.equals("")) {
                        text += "<h3>" + title + "</h3>";
                    }

                    text+="<p align=\"justify\">";
                    text+= htmlwords;
                    text+= "</p>";

                    text+="</body>";
                    text+="</html>";
                    textViewWiki.loadData(text, "text/html", "utf-8");

                    String wikiContent[] = words.split("\\.");
                    for (String aWikiContent : wikiContent) {
                        //aWikiContent = aWikiContent.replaceAll("[^a-zA-z0-9\\s+]", "");
                        wikiSentences.add(aWikiContent + ", ");
                    }
                    sentenceIndex = -1;
                    paused = false;
                    Speak("Bắt đầu đọc thông tin về " + title,
                            "Start reading about " + title + " tap the screen to pause or hold the screen to stop reading", "wiki");
                } else {
                    showToastMessage("Null word", "");
                }
            } else {
                Speak("Đã xảy ra lỗi", "Error happened", "");
                showToastMessage("Đã xảy ra lỗi", "Error happened");
            }
        }
    }

    private String getCurrentLang(){
        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

}
