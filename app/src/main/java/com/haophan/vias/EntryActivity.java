package com.haophan.vias;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.haophan.vias.tools.RingPlayer;

import java.util.HashMap;
import java.util.Locale;

public class EntryActivity extends AppCompatActivity {

    final int SPLASH_DURATION = 2000;
    private static final int PERMISSIONS_REQUEST = 1;
    private String currentLang;
    private String appPreference = "app_pref";

    final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    final String PERMISSION_CALL = Manifest.permission.CALL_PHONE;
    final String PERMISSION_READ_SMS = Manifest.permission.READ_SMS;
    final String PERMISSION_RECEIVE_SMS = Manifest.permission.RECEIVE_SMS;
    final String PERMISSION_SEND_SMS = Manifest.permission.SEND_SMS;
    final String PERMISSION_CONTACTS = Manifest.permission.READ_CONTACTS;
    final String PERMISSION_AC_FINE = Manifest.permission.ACCESS_FINE_LOCATION;
    final String PERMISSION_AC_COARSE = Manifest.permission.ACCESS_COARSE_LOCATION;
    final String PERMISSION_RECORD = Manifest.permission.RECORD_AUDIO;
    final String PERMISSION_RES = Manifest.permission.READ_EXTERNAL_STORAGE;

    Animation splashAnim;
    RelativeLayout entryLayout;

    TextToSpeech tts;
    RingPlayer ring;
    private final int TTS_CHECK_CODE = 1001;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                boolean granted = true;
                for (int i = 0; i < grantResults.length; i++){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                        granted = false;
                }
                if (granted) {
                    if (isNetworkAvailable()) {
                        startApplication();
                    } else {
                        showToastMessage("Ứng dụng cần kết nối internet để hoạt động. Hãy thử kết nối và khởi động lại ứng dụng",
                                " This application needs internet connection to work. Try connecting and restart application.");
                        Speak("Ứng dụng cần kết nối internet để hoạt động. Hãy thử kết nối và khởi động lại ứng dụng",
                                " This application needs internet connection to work. Try connecting and restart application.");
                        finish();
                    }
                } else {
                    requestPermission();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        entryLayout = (RelativeLayout) findViewById(R.id.entry_layout);
        splashAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.splash_transition);
        entryLayout.setAnimation(splashAnim);


        ring = new RingPlayer(getApplicationContext());

        currentLang = getCurrentLang();


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent checkIntent = new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkIntent, TTS_CHECK_CODE);
            }
        }, SPLASH_DURATION);
    }

    private String getCurrentLang(){
        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TTS_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            if (currentLang.equals("vi")) {
                                tts.setLanguage(new Locale("vi"));
                                tts.setSpeechRate(1.35f);
                            } else {
                                tts.setLanguage(Locale.US);
                            }
                            if (!hasPermission()){
                                requestPermission();
                            } else {
                                if (isNetworkAvailable()) {
                                    startApplication();
                                } else {
                                    showToastMessage("Ứng dụng cần kết nối internet để hoạt động. Hãy thử kết nối và khởi động lại ứng dụng",
                                            " This application needs internet connection to work. Try connecting and restart application.");
                                    Speak("Ứng dụng cần kết nối internet để hoạt động. Hãy thử kết nối và khởi động lại ứng dụng",
                                            " This application needs internet connection to work. Try connecting and restart application.");
                                    finish();
                                }
                            }
                        } else {
                            showToastMessage("Khởi tạo giọng nói gặp lỗi", "Error initializing Text-To-Speech");
                        }
                    }
                });
            } else {
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void startApplication(){
        ring.play();

        Intent intent = new Intent(EntryActivity.this, BaseActivity.class);
        startActivity(intent);
        finish();
    }

    private void Speak(String viText, String enText) {
        if (tts.isSpeaking()) tts.stop();
        HashMap<String, String> myHashAudio = new HashMap<String, String>();
        int streamType = getResources().getString(R.string.audio_stream).equals("alarm")?AudioManager.STREAM_ALARM:AudioManager.STREAM_MUSIC;
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(streamType));
        switch (currentLang) {
            case "vi":
                if (!tts.isSpeaking()) {
                    tts.speak(viText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
                }
                break;

            default:
                if (!tts.isSpeaking()) {
                    tts.speak(enText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
                }
                break;
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

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_CALL) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_CONTACTS) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_AC_FINE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_AC_COARSE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_RECORD) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_RES) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_READ_SMS) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_SEND_SMS) == PackageManager.PERMISSION_GRANTED;

        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //showToastMessage("request", null);
            /*if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)
                    || shouldShowRequestPermissionRationale(PERMISSION_CALL)
                    || shouldShowRequestPermissionRationale(PERMISSION_CONTACTS)
                    || shouldShowRequestPermissionRationale(PERMISSION_AC_FINE)
                    || shouldShowRequestPermissionRationale(PERMISSION_AC_COARSE)
                    || shouldShowRequestPermissionRationale(PERMISSION_RECORD)
                    || shouldShowRequestPermissionRationale(PERMISSION_RES)
                    || shouldShowRequestPermissionRationale(PERMISSION_READ_SMS)
                    || shouldShowRequestPermissionRationale(PERMISSION_RECEIVE_SMS)
                    || shouldShowRequestPermissionRationale(PERMISSION_SEND_SMS)) {*/
                showToastMessage("Xin hãy cấp quyền cho ứng dụng", "Please accept permissions for this application");
                Speak("Xin hãy cấp quyền cho ứng dụng", "Please accept permissions for this application");

                requestPermissions(new String[]{
                        PERMISSION_CAMERA,
                        PERMISSION_CALL,
                        PERMISSION_CONTACTS,
                        PERMISSION_AC_FINE,
                        PERMISSION_AC_COARSE,
                        PERMISSION_RECORD,
                        PERMISSION_RES,
                        PERMISSION_READ_SMS,
                        PERMISSION_RECEIVE_SMS,
                        PERMISSION_SEND_SMS
                }, PERMISSIONS_REQUEST);
            }
        //}
    }
}
