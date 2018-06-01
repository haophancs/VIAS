package com.haophan.vias.news;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.haophan.vias.R;
import com.haophan.vias.tools.RingPlayer;
import com.rey.material.widget.ProgressView;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class NewsReaderActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private ArrayList<String> titleArray = new ArrayList<String>();
    private ArrayList<String> linkArray = new ArrayList<String>();
    private ArrayList<String> sentences = new ArrayList<String>();

    Button btnReadNews;
    ProgressView loadingPanel;
    WebView textViewNews;

    SpeechRecognizer recognizer;
    RecognitionListener listener;
    boolean recognized = false;
    boolean isRequesting = false;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    int titleIndex = 0;
    int sentenceIndex = -1;
    int VOICE_RECOGNITION_NEWS_CODE = 1002;

    boolean toReadTitle = false;
    boolean toReadNews = false;

    private String RSSType = "";
    private String UTF8Type = "";
    private String selectedTitle = "";
    private String selectedLink = "";

    TextToSpeech tts;
    Vibrator vibrator;
    RingPlayer ring;

    String currentLang;
    String previousDoing;

    private int backBtnTap = 0;
    private int headsetBtnTap = 0;

    private boolean paused = false;
    private boolean isOver = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_reader);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_news);
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

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        ring = new RingPlayer(getApplicationContext());

        btnReadNews = (Button) findViewById(R.id.buttonRead);
        textViewNews = (WebView) findViewById(R.id.textViewNews);
        loadingPanel = (ProgressView) findViewById(R.id.loadingPanelNews);

        loadingPanel.setVisibility(View.VISIBLE);
        btnReadNews.setEnabled(false);

        textViewNews.setVerticalScrollBarEnabled(true);
        textViewNews.setBackgroundColor(Color.parseColor("#ffffff"));

        Intent intent = getIntent();
        RSSType = intent.getStringExtra("RSSType");
        UTF8Type = intent.getStringExtra("UTF8Type");

        checkVoiceRecognition();
        currentLang = getCurrentLang();
        if (RSSType.equals("") || UTF8Type.equals("")) {
            RSSType = "thoi-su";
            UTF8Type = "thời sự";
        }

        showToastMessage("Đang mở tin tức " + UTF8Type, "");
        tts = new TextToSpeech(getApplicationContext(), this);

        btnReadNews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(100);
                paused = !paused;
                if (paused) {
                    showToastMessage("Tạm dừng", "Paused");
                    stopReading();
                    Speak("Tạm dừng", "paused");
                } else {
                    if (sentenceIndex >= 0) {
                        sentenceIndex--;
                    }
                    if (titleIndex >= 0) {
                        titleIndex--;
                    }
                    showToastMessage("Xin được tiếp tục","");
                    continueReading();
                }
            }
        });

        btnReadNews.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                vibrator.vibrate(300);

                btnReadNews.setEnabled(false);

                stopReading();

                Speak("bạn có yêu cầu gì", "");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        voiceRecognize(VOICE_RECOGNITION_NEWS_CODE);
                    }
                }, 1500);
                return false;
            }
        });
        builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        recognizer = SpeechRecognizer.createSpeechRecognizer(this.getApplicationContext());
        listener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {

                dialog.dismiss();
                isRequesting = false;

                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (textMatchList != null && !textMatchList.isEmpty()) {

                    recognized = true;

                    String s = textMatchList.get(0);
                    showToastMessage("Yêu cầu nhận dạng được: " + s, "Recognized request: " + s);

                    s = s.toLowerCase();
                    if (s.contains("thoát") || s.contains("tắt") || s.contains("đóng")
                            || s.contains("exit") || s.contains("close")) {
                        toReadTitle = true;
                        toReadNews = false;
                        vibrator.vibrate(400);
                        Speak("Thoát đọc báo, trở lại màn hình chính", "");
                        finishAndRemoveTask();
                    } else if (s.contains("đọc lại") || s.contains("điểm lại")
                            || (s.contains("repeat") && s.contains("title"))) {
                        //btnReadTitle.setEnabled(false);
                        setTextViewNews("");
                        titleIndex = -1;
                        selectedTitle = "";
                        selectedLink = "";

                        toReadNews = false;
                        toReadTitle = true;
                        Speak("đọc lại điểm tin", "read title");
                    } else if (s.contains("số")) {
                        getNews(s);
                    } else if (s.contains("tạm dừng") || s.contains("pause")) {
                        stopReading();
                    } else if (s.contains("đọc tiếp")) {
                        paused = false;
                        continueReading();
                    } else if (s.contains("tin")) {
                        Speak("Đang mở lại bản tin", "Re-opening news reader");
                        if (s.contains("thời sự") || s.contains("news")) {
                            resetNewsRequest("thoi-su", "thời sự");
                        } else if (s.contains("thế giới") || s.contains("global") || s.contains("world")) {
                            resetNewsRequest("the-gioi", "thế giới");
                        } else if (s.contains("kinh doanh") || s.contains("business")) {
                            resetNewsRequest("kinh-doanh", "kinh doanh");
                        } else if (s.contains("giải trí") || s.contains("entertainment")) {
                            resetNewsRequest("giai-tri", "giải trí");
                        } else if (s.contains("thể thao") || s.contains("sport")) {
                            resetNewsRequest("the-thao", "thể thao");
                        } else if (s.contains("pháp luật") || s.contains("law")) {
                            resetNewsRequest("phap-luat", "pháp luật");
                        } else if (s.contains("giáo dục") || s.contains("education")) {
                            resetNewsRequest("giao-duc", "giáo dục");
                        }
                    } else {
                        Speak("Không rõ yêu cầu", "");
                        paused = false;
                        continueReading();
                    }
                } else {
                    paused = false;
                    continueReading();
                }
            }

            @Override
            public void onReadyForSpeech(Bundle params) {
                ring.play();
            }

            @Override
            public void onError(int error) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onEndOfSpeech() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // TODO Auto-generated method stub

            }
        };
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {

            if (!currentLang.equals("vi")) {
                tts.setLanguage(Locale.US);
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                }
                Speak("Vias - News Reader currently only supports Vietnamese, sorry for the inconvenience","");
                showToastMessage("","Vias - News Reader currently only supports Vietnamese, sorry for the inconvenience");
                finishAndRemoveTask();
            } else {
                new ReadData().execute("https://vnexpress.net/rss/" + RSSType + ".rss");
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {

                    }

                    @Override
                    public void onDone(String utteranceId) {
                        switch (utteranceId) {
                            case "read title":
                                if (toReadTitle)
                                    readTitle();
                                break;

                            case "read news":
                                if (toReadNews)
                                    readNewsContent();
                                break;

                            default:
                                break;
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {

                    }
                });
                if (currentLang.equals("vi")) {
                    tts.setSpeechRate(1.42f);
                    tts.setLanguage(new Locale("vi"));
                    //tts.speak("đã sẵn sàng", TextToSpeech.QUEUE_FLUSH, null);
                } else {
                    tts.setLanguage(Locale.US);
                }
            }
        } else {
            showToastMessage("Đã xảy ra lỗi", "");
        }
    }

    private void Speak(String text, String message) {
        if (tts.isSpeaking()) tts.stop();

        HashMap<String, String> myHashAudio = new HashMap<>();

        int streamType = getResources().getString(R.string.audio_stream).equals("alarm")?AudioManager.STREAM_ALARM:AudioManager.STREAM_MUSIC;
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(streamType));
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, message);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, myHashAudio);
    }

    private void stopReading(){
        paused = true;

        if (toReadNews) {
            previousDoing = "read news";
        } else if (toReadTitle) {
            previousDoing = "read title";
        }

        toReadNews = false;
        toReadTitle = false;
        tts.stop();
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

    private String handledWords(String words){
        words = words.toLowerCase();
        words = words.replace("thuộc bộ khoa học công nghệ.", "");
        words = words.replace("tp", "thành phố");
        words = words.replace("hcm", " Hồ Chí Minh");
        words = words.replace("hn", "hà nội");
        words = words.replace("vn", "Việt Nam");
        words = words.replace("ubnd", "ủy ban nhân dân");
        words = words.replace("km/h", "km trên giờ");
        words = words.replace("0123.888.0123 (hn)", "");
        words = words.replace("0129.233.3555 (tp hcm)", "");
        words = words.replace("0123.888.0123 (hà nội)", "");
        words = words.replace(selectedTitle.toLowerCase(), "");
        words = words.replace("vnexpress", "vn express");
        words = words.replace("vnexpress.net", "");
        words = words.replace("nông nghiệp sạch", "");
        words = words.replace("giao thông", "");
        words = words.replace(".000.000.000", " tỷ");
        words = words.replace(".000.000", " triệu");
        words = words.replace(".000", " nghìn");
        words = words.replace("/", " / ");
        words = words.replace("...", ".");
        words = words.replace("gmt+7", "gmt+7,");
        words = words.replace("km", "ki lô mét");
        words = words.replace("giữ bản quyền nội dung trên website này.", "");

        for (int i = 0; i <= 9; i++){
            for (int j = 0; j <= 9; j++){
                words = words.replace(i + ":" + j, i + " giờ " + j);
                words = words.replace(i + "." + j, i + "" + j);
            }
        }
        return words;
    }

    private void readNewsContent(){

        if (!paused) {
            toReadNews = true;
            toReadTitle = false;

            sentenceIndex++;
            String s = sentences.get(sentenceIndex);

            if (sentenceIndex < sentences.size() && !s.contains("ý kiến bạn đọc")) {
                if (s.length() > 1) Speak(s, "read news");
            } else {
                Speak("Vừa rồi là bản tin " + selectedTitle + ". Cảm ơn bạn đã nghe", "");
                sentenceIndex = -1;
                isOver = true;
            }
        }
    }

    private void setTextViewNews(String words){

        String text = "";
        String htmlwords;
        htmlwords = words.replaceAll("\n", "<br>");
        htmlwords = htmlwords.replace(selectedTitle, "");
        text+= "<html>";
        text+= "<body>";

        if (!words.equals("")) {
            text += "<h3>" + selectedTitle + "</h3>";
        }

        text+="<p align=\"justify\">";
        text+= htmlwords;
        text+= "</p>";

        text+="</body>";
        text+="</html>";
        textViewNews.loadData(text, "text/html", "utf-8");
    }

    private void readTitle(){
        toReadTitle = true;
        toReadNews = false;

        titleIndex++;
        if (titleIndex >= titleArray.size()) {
            titleIndex = 0;
            showToastMessage("Vừa rồi là điểm tin " + UTF8Type, "");
            Speak(", vừa rồi là điểm tin " + UTF8Type + ", bạn muốn nghe tin gì?", "");
            voiceRecognize(VOICE_RECOGNITION_NEWS_CODE);
        } else {
            final int i = titleIndex + 1;
            showToastMessage("Tin số " + i + ": " + titleArray.get(titleIndex), "read title");
            Speak("Tin số " + i + ", " + titleArray.get(titleIndex), "read title");
        }
    }

    private void continueReading(){
        if (!isOver) {
            if (!tts.isSpeaking()) {
                Speak("xin được tiếp tục", "");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switch (previousDoing) {
                            case "read news":
                                readNewsContent();
                                break;
                            case "read title":
                                readTitle();
                                break;
                        }
                    }
                }, 1500);
            }
        }
    }

    private void getNews(String num) {
        toReadNews = true;
        toReadTitle = false;

        previousDoing = "read news";
        paused = false;

        num = num.replaceAll("[^\\d.]", "");
        if (!num.equals("") && num.matches("[0-9]+") && num.length() < 3) {
            int titleId = Integer.parseInt(num) - 1;
            if (titleId <= linkArray.size() && titleId >= 0) {
                selectedTitle = titleArray.get(titleId);
                selectedLink = linkArray.get(titleId);

                showToastMessage(selectedLink, selectedLink);
                new htmlTextPuller().execute();
            }
        }
    }

    private class htmlTextPuller extends AsyncTask<Void, Void, Void> {

        String words;

        @Override
        protected Void doInBackground(Void... params) {
            words = "";
            try {
                org.jsoup.nodes.Document doc = Jsoup.connect(selectedLink).get();
                words = doc.text();
            } catch (IOException e) {
                Speak("Đã xảy ra lỗi", "Error happened");
                showToastMessage("Đã xảy ra lỗi", "Error happened");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            isOver = false;

            int i = 0;
            if (words != null) {
                /*String str = words;
                while (!words.substring(i, i + 2 + 1).equals("Thứ") && (i < words.length() - 3)) {
                    str = words.substring(i);
                    i++;
                }
                if (str.length() < 10){

                    str = words;
                    while (!words.substring(i, i + 7 + 1).equals("Chủ nhật") && (i < words.length() - 8)) {
                        str = words.substring(i);
                        i++;
                    }
                }
                words = str.trim();*/
                if (words.contains("Thứ")) {
                    words = words.substring(words.indexOf("Thứ"));
                } else {
                    words = words.substring(words.indexOf("Chủ nhật"));
                }
                setTextViewNews(words);
                words = handledWords(words);
                String[] result = words.split("\\.");

                sentences.clear();
                for (String aResult : result) {
                    sentences.add(aResult + ",");
                }

                if (!selectedTitle.equals("")) {
                    sentenceIndex = -1;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    //ring.play();
                    Speak("Sau đây là bản tin " + selectedTitle + ". Dẫn nguồn tin từ báo vnexpress.vn", "read news");
                }
            } else {
                Speak("Đã xảy ra lỗi", "Error happened");
                showToastMessage("Đã xảy ra lỗi", "Error happened");
            }
        }
    }

    private void resetNewsRequest(String RSS, String UTF8){
        titleIndex = 0;
        sentenceIndex = -1;

        setTextViewNews("");
        titleArray.clear();
        linkArray.clear();
        sentences.clear();


        paused = false;
        toReadTitle = false;
        toReadNews = false;

        RSSType = RSS;
        UTF8Type = UTF8;
        selectedTitle = "";
        selectedLink = "";
        previousDoing = "";

        tts.stop();
        new ReadData().execute("https://vnexpress.net/rss/"+RSSType+".rss");
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_NEWS_CODE) {
            paused = true;
            btnReadNews.setEnabled(true);
            // Truong hop co gia tri tra ve
            if (resultCode == RESULT_OK) {
                ArrayList<String> textMatchList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (!textMatchList.isEmpty()) {
                    String s = textMatchList.get(0);
                    s = s.toLowerCase();
                    if (s.contains("thoát") || s.contains("tắt") || s.contains("đóng")
                            || s.contains("exit") || s.contains("close")) {
                        toReadTitle = true;
                        toReadNews = false;
                        vibrator.vibrate(400);
                        Speak("Thoát đọc báo, trở lại màn hình chính", "");
                        finishAndRemoveTask();
                    } else if (s.contains("đọc lại") || s.contains("điểm lại")
                            || (s.contains("repeat") && s.contains("title"))) {
                        //btnReadTitle.setEnabled(false);
                        setTextViewNews("");
                        titleIndex = -1;
                        selectedTitle = "";
                        selectedLink = "";

                        toReadNews = false;
                        toReadTitle = true;
                        Speak("đọc lại điểm tin", "read title");
                    } else if (s.contains("đọc") && s.contains("số")){
                        getNews(s);
                    } else if (s.contains("tạm dừng") || s.contains("pause")) {
                        stopReading();
                    } else if (s.contains("đọc tiếp")){
                        paused = false;
                        continueReading();
                    } else if (s.contains("đọc tin") || s.contains("đọc báo")){
                        Speak("Đang mở lại bản tin", "Re-opening news reader");
                        if (s.contains("thời sự") || s.contains("news")) {
                            resetNewsRequest("thoi-su", "thời sự");
                        }
                        else if (s.contains("thế giới") || s.contains("global") || s.contains("world")) {
                            resetNewsRequest("the-gioi", "thế giới");
                        }
                        else if (s.contains("kinh doanh") || s.contains("business")) {
                            resetNewsRequest("kinh-doanh", "kinh doanh");
                        }
                        else if (s.contains("giải trí") || s.contains("entertainment")) {
                            resetNewsRequest("giai-tri", "giải trí");
                        }
                        else if (s.contains("thể thao") || s.contains("sport")) {
                            resetNewsRequest("the-thao", "thể thao");
                        }
                        else if (s.contains("pháp luật") || s.contains("law")) {
                            resetNewsRequest("phap-luat", "pháp luật");
                        }
                        else if (s.contains("giáo dục") || s.contains("education")) {
                            resetNewsRequest("giao-duc", "giáo dục");
                        }
                    } else {
                        paused = false;
                        continueReading();
                    }
                } else {
                    paused = false;
                    continueReading();
                }
            } else {
                paused = false;
                continueReading();
            }
        }
    }*/

    private void stopMySelf(){
        paused = true;
        stopReading();
        showToastMessage("Thoát đọc báo, trở về màn hình chính", "");
        Speak("Thoát đọc báo, trở về màn hình chính", "");
        tts.shutdown();
        finishAndRemoveTask();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        /*if ((keyCode == KeyEvent.KEYCODE_BACK)){
            toReadTitle = false;
            toReadNews = false;
            Speak("thoát đọc báo", "");

            finishAndRemoveTask();
        }
        return super.onKeyDown(keyCode, event);*/
        if ((keyCode == KeyEvent.KEYCODE_BACK)){
            paused = true;
            stopReading();
            backBtnTap++;
            if (backBtnTap >= 2){
                backBtnTap = 0;
                showToastMessage("Thoát đọc báo, trở về màn hình chính", "");
                Speak("thoát đọc báo, trở về màn hình chính", "");
                finishAndRemoveTask();
                vibrator.vibrate(300);
            } else {
                showToastMessage("Chạm lần nữa để thoát đọc báo", "");
                Speak("Chạm lần nữa để thoát đọc báo", "Tap again to exit news reader");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (backBtnTap > 0) {
                            backBtnTap = 0;
                            paused = false;
                            continueReading();
                        }
                    }
                }, 3000);
            }
            return false;

        } else if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            if (isRequesting) {
                isRequesting = false;
                recognizer.stopListening();
                btnReadNews.setEnabled(true);

                dialog.dismiss();
                //ring.playBtnOffSound();
                vibrator.vibrate(30);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!recognized){
                            paused = false;
                            continueReading();
                        }
                    }
                }, 1000);
            } else {
                headsetBtnTap++;
                if (headsetBtnTap >= 2) {
                    vibrator.vibrate(300);
                    headsetBtnTap = 0;

                    btnReadNews.setEnabled(false);

                    stopReading();

                    Speak("bạn có yêu cầu gì", "");
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            voiceRecognize(VOICE_RECOGNITION_NEWS_CODE);
                        }
                    }, 1500);
                } else {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (headsetBtnTap > 0) {
                                headsetBtnTap = 0;
                            }
                        }
                    }, 1000);
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private class ReadData extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            return ReadDataFromURL(params[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            XMLDOMParser parser = new XMLDOMParser();
            Document document = parser.getDocument(s);
            NodeList nodeList = document.getElementsByTagName("item");

            String title;
            String link;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                title = parser.getValue(element, "title");
                link = parser.getValue(element, "link");
                if (!link.contains("video") && !link.contains("infographic")) {
                    titleArray.add(title);
                    linkArray.add(link);
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnReadNews.setVisibility(View.VISIBLE);
                    btnReadNews.setEnabled(true);
                    loadingPanel.setVisibility(View.GONE);
                }
            });
            titleIndex = -1;
            toReadTitle = true;
            toReadNews = false;
            showToastMessage("Sau đây là điểm tin " + UTF8Type + " từ báo vnexpress.vn", "Sau đây là điểm tin " + UTF8Type + " từ báo  VnExpress.vn");
            Speak("Sau đây là điểm tin " + UTF8Type + " từ báo vn-express chấm vn.", "read title");
            super.onPostExecute(s);
        }
    }

    private static String ReadDataFromURL(String theUrl) {
        StringBuilder content = new StringBuilder();
        try {
            // create a url object
            URL url = new URL(theUrl);
            // create a urlconnection object
            URLConnection urlConnection = url.openConnection();
            // wrap the urlconnection in a bufferedreader
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            // read from the urlconnection via the bufferedreader
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line + "\n");
            }
            bufferedReader.close();
        } catch (Exception e) {
        }
        return content.toString();
    }

    public void checkVoiceRecognition() {
        Log.v("", "checkVoiceRecognition checkVoiceRecognition");
        // Kiem tra thiet bi cho phep nhan dang giong noi hay ko
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            showToastMessage("Nhận dạng giọng nói không có sẵn", "Voice recognizer not present");
        }
    }

    private String getCurrentLang(){
        String appPreference = "app_pref";

        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    public void voiceRecognize(int code) {
        /*Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // xac nhan ung dung muon gui yeu cau
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        // goi y nhung dieu nguoi dung muon noi
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Bạn có yêu cầu gì");
        // goi y nhan dang nhung gi nguoi dung se noi
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        // Xac dinh ban muon bao nhieu ket qua gan dung duoc tra ve
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        // Gui yeu cau di
        startActivityForResult(intent, code);*/
        isRequesting = true;
        recognized = false;
        if (code == VOICE_RECOGNITION_NEWS_CODE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String textHint = "";

                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                    intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, textHint);

                    DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {

                            isRequesting = false;
                            recognizer.stopListening();
                            btnReadNews.setEnabled(true);

                            dialog.dismiss();
                            //ring.playBtnOffSound();
                            vibrator.vibrate(30);

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!recognized){
                                        paused = false;
                                        continueReading();
                                    }
                                }
                            }, 1500);

                        }
                    };
                    DialogInterface.OnClickListener onNegativeBtnListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            //recognizer.stopListening();
                            isRequesting = false;
                            recognizer.cancel();
                            btnReadNews.setEnabled(true);

                            dialog.dismiss();
                            //ring.playBtnOffSound();
                            vibrator.vibrate(30);

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!recognized){
                                        paused = false;
                                        continueReading();
                                    }
                                }
                            }, 1500);
                        }
                    };
                    if (currentLang.equals("vi")) {
                        textHint = "Bạn có yêu cầu gì";
                        builder.setMessage("Đang ghi âm...");
                        builder.setNegativeButton("Hủy", onNegativeBtnListener);
                    } else {
                        textHint = "Can I help you?";
                        builder.setMessage("Recording...");
                        builder.setNegativeButton("Cancel", onNegativeBtnListener);
                    }

                    recognizer.setRecognitionListener(listener);
                    recognizer.startListening(intent);

                    builder.setTitle(textHint);
                    builder.setCancelable(true);
                    builder.setOnCancelListener(onCancelListener);

                    builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if ((keyCode == KeyEvent.KEYCODE_BACK)){
                                paused = true;
                                stopReading();
                                backBtnTap++;
                                if (backBtnTap >= 2){
                                    backBtnTap = 0;
                                    showToastMessage("Thoát đọc báo, trở về màn hình chính", "");
                                    Speak("thoát đọc báo, trở về màn hình chính", "");
                                    finishAndRemoveTask();
                                    vibrator.vibrate(300);
                                } else {
                                    showToastMessage("Chạm lần nữa để thoát đọc báo", "");
                                    Speak("Chạm lần nữa để thoát đọc báo", "Tap again to exit news reader");
                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (backBtnTap > 0) {
                                                backBtnTap = 0;
                                                paused = false;
                                                continueReading();
                                            }
                                        }
                                    }, 3000);
                                }
                                return false;

                            } else if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
                                if (isRequesting) {
                                    isRequesting = false;
                                    recognizer.stopListening();
                                    btnReadNews.setEnabled(true);

                                    dialog.dismiss();
                                    //ring.playBtnOffSound();
                                    vibrator.vibrate(30);

                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!recognized){
                                                paused = false;
                                                continueReading();
                                            }
                                        }
                                    }, 1000);
                                } else {
                                    headsetBtnTap++;
                                    if (headsetBtnTap >= 2) {
                                        vibrator.vibrate(300);
                                        headsetBtnTap = 0;

                                        btnReadNews.setEnabled(false);

                                        stopReading();

                                        Speak("bạn có yêu cầu gì", "");
                                        final Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                voiceRecognize(VOICE_RECOGNITION_NEWS_CODE);
                                            }
                                        }, 1500);
                                    } else {
                                        final Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (headsetBtnTap > 0) {
                                                    headsetBtnTap = 0;
                                                }
                                            }
                                        }, 1000);
                                    }
                                }
                            }
                            return false;
                        }
                    });

                    dialog = builder.create();
                    dialog.show();
                }
            });
        }
    }
}
