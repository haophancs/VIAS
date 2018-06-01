package com.haophan.vias;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class OCRActivity extends AppCompatActivity {

    SurfaceView cameraView;
    TextView textView;
    Button btnOcr;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;

    TextToSpeech tts;
    Vibrator vibrator;

    String currentLang;

    boolean ocrEnabled = true;
    int recognitionTimes = 0;

    int backBtnTap = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_ocr);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_back));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Speak("thoát nhận dạng chữ, trở lại màn hình chính", "exit text recognizer and return to main screen");
                //showToastMessage("Thoát nhận dạng chữ, trở lại màn hình chính", "exit text recognizer and return to main screen");
                finishAndRemoveTask();
            }
        });

        cameraView = (SurfaceView) findViewById(R.id.surface_view_ocr);
        textView = (TextView) findViewById(R.id.ocr_text_view);
        btnOcr = (Button) findViewById(R.id.btnOCR);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        currentLang = getCurrentLang();
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    if (currentLang.equals("vi")){
                        tts.setLanguage(new Locale("vi"));
                        tts.setSpeechRate(1.32f);
                        btnOcr.setText("Nhận dạng chữ");

                    } else {
                        tts.setLanguage(Locale.US);
                        btnOcr.setText("Recognize Text");
                    }
                    Speak("chế độ nhận dạng chữ đã sẵn sàng.",
                            "Text recognizer has been ready.");
                    showToastMessage("Chế độ nhận dạng chữ đã sẵn sàng.",
                            "Text recognizer has been ready.");
                }
            }
        });

        btnOcr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tts.isSpeaking())
                    tts.stop();
                ocrEnabled = !ocrEnabled;
                if (ocrEnabled){
                    //Speak("Nhận dạng chữ", "Nhận dạng chữ");
                }
            }
        });

        btnOcr.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Speak("Thoát chế độ nhận dạng chữ, trở về màn hình chính", "Close text recognizer, return to main screen");
                showToastMessage("Thoát chế độ nhận dạng chữ, trở về màn hình chính", "Close text recognizer, return to main screen");

                Intent intent = new Intent(OCRActivity.this, BaseActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("message", "return");
                startActivity(intent);

                finishAndRemoveTask();
                return false;
            }
        });

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
        } else {

            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {

                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(OCRActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    if (ocrEnabled) {
                        final SparseArray<TextBlock> items = detections.getDetectedItems();
                        if (items.size() != 0) {
                            if (!tts.isSpeaking()) {
                                ocrEnabled = false;
                                textView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        StringBuilder stringBuilder = new StringBuilder();
                                        for (int i = 0; i < items.size(); ++i) {
                                            TextBlock item = items.valueAt(i);
                                            stringBuilder.append(item.getValue());
                                            stringBuilder.append(" ");
                                        }
                                        String recognizedText = stringBuilder.toString();
                                        textView.setText(recognizedText);
                                        Speak("Văn bản đã nhận dạng được: "+recognizedText, "Recognized words:"+recognizedText);
                                        showToastMessage(recognizedText, recognizedText);
                                        recognitionTimes++;
                                    }
                                });
                            }
                        }
                    }
                    if (!ocrEnabled) {
                        if (recognitionTimes == 1) {
                            if (!tts.isSpeaking()) {
                                recognitionTimes++;
                                Speak("Đã nhận dạng xong, để tiếp tục nhận dạng, hãy chạm vào màn hình, để thoát, nhấn giữ màn hình",
                                        "Recognizing Done, to continue recognizing, tap on the screen, to close ocr recognizer, hold on the screen");
                            }
                        }
                    }
                }
            });
        }
    }

    private String getCurrentLang(){
        String appPreference = "app_pref";

        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    @Override
    protected void onPause() {
        tts.stop();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if ((keyCode == KeyEvent.KEYCODE_BACK)){
            tts.stop();
            backBtnTap++;

            if (backBtnTap >= 2) {
                Speak("thoát nhận dạng chữ, trở lại màn hình chính", "exit text recognizer and return to main screen");
                showToastMessage("thoát nhận dạng chữ, trở lại màn hình chính", "exit text recognizer and return to main screen");
                finishAndRemoveTask();
            } else {
                Speak("Nhấn back lần nữa để thoát nhận dạng chữ", "Tap again to exit text recognizer");
                Toast.makeText(getApplicationContext(), "Nhấn back lần nữa để thoát nhận dạng chữ", Toast.LENGTH_SHORT).show();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        backBtnTap = 0;
                    }
                }, 3000);
            }
        }
        return false;
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

    private void Speak(String viText, String engText){
        if (tts.isSpeaking()) tts.stop();
        HashMap<String, String> myHashAudio = new HashMap<String, String>();
        int streamType = getResources().getString(R.string.audio_stream).equals("alarm")?AudioManager.STREAM_ALARM:AudioManager.STREAM_MUSIC;
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(streamType));
        switch (currentLang){
            case "vi":
                tts.speak(viText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
                break;
            default:
                tts.speak(engText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
        }
        vibrator.vibrate(80);
    }
}