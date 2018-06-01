package com.haophan.vias;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.haophan.vias.homecontroller.DatabaseManager;
import com.haophan.vias.homecontroller.controller.Controller;
import com.haophan.vias.homecontroller.device.Device;
import com.haophan.vias.homecontroller.requester.RequestSender;
import com.haophan.vias.homecontroller.requester.ResponseEvent;
import com.haophan.vias.news.NewsReaderActivity;
import com.haophan.vias.objectrecognizer.Classifier;
import com.haophan.vias.objectrecognizer.ClassifierActivity;
import com.haophan.vias.objectrecognizer.TensorFlowImageClassifier;
import com.haophan.vias.service.Mp3Service;
import com.haophan.vias.service.NavigationService;
import com.haophan.vias.tools.Calculator;
import com.haophan.vias.tools.CurrentDateTime;
import com.haophan.vias.tools.HttpDataLoader;
import com.haophan.vias.tools.RingPlayer;
import com.haophan.vias.weather.Model.OpenWeatherMap;
import com.haophan.vias.weather.RequestURL;
import com.rey.material.widget.ProgressView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BaseActivity extends AppCompatActivity {

    private static final int HAND_INPUT_SIZE = 299;
    private static final int HAND_IMAGE_MEAN = 128;
    private static final float HAND_IMAGE_STD = 128f;
    private static final String HAND_INPUT_NAME = "Mul";
    private static final String HAND_OUTPUT_NAME = "final_result";
    private static final float HAND_THRESHOLD = 0.65f;
    private static final String HAND_MODEL_FILE = "file:///android_asset/hand_gestures_graph.pb";
    private static final String HAND_LABEL_FILE =
            "file:///android_asset/hand_gestures_labels.txt";

    private Classifier hand_classifier;

    private Executor executorTensorflow = Executors.newSingleThreadExecutor();
    private Executor executorBluetooth = Executors.newSingleThreadExecutor();

    private Button btnDetectHand;
    private CameraView cameraViewFront;
    private ProgressView loadingPanel;

    private TextToSpeech tts;
    private boolean isRequesting = false;
    
    private int headsetBtnTap = 0;
    private int backBtnTap = 0;

    private Vibrator vibrator;
    private RingPlayer ring;

    private final int VOICE_RECOGNITION_COMMAND_CODE = 1001;
    private final int VOICE_RECOGNITION_GET_CONTACT_CODE = 1002;
    private final int VOICE_RECOGNITION_GET_SMS_TARGET = 1009;
    private final int VOICE_RECOGNITION_HOME_CONTROL = 10010;
    private final int VOICE_RECOGNITION_GET_DESTINATION = 1003;
    private final int VOICE_RECOGNITION_GET_NEWS_TYPE = 1004;
    private final int VOICE_RECOGNITION_GET_SONG = 1005;
    private final int VOICE_RECOGNITION_CHECK_CALL_VEHICLE_DETECTOR = 1006;
    private final int VOICE_RECOGNITION_SEND_MSG = 1007;
    private final int VOICE_RECOGNITION_CALCULATE = 1008;
    private final int VOICE_RECOGNITION_WIKI = 10011;

    private CurrentDateTime dateTimeNow;

    private SpeechRecognizer recognizer;
    private RecognitionListener commandListener, contactListener, destinationListener,
            newsListener, songListener, checkCallVehicleListener, mathListener, msgContentListener,
            msgTargetListener, homeControlListener, wikiObjectListener;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private Mp3Service mp3Service;
    private boolean mp3ServiceBinded = false;
    private ServiceConnection mp3ServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Mp3Service.Mp3Binder binder = (Mp3Service.Mp3Binder) service;
            mp3Service = binder.getService();
            //Toast.makeText(getApplicationContext(), "binded", Toast.LENGTH_SHORT).show();
            mp3ServiceBinded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mp3ServiceBinded = false;
        }
    };

    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    boolean deviceConnected = false;
    byte buffer[];
    boolean stopThread;
    boolean isBluetoothEnabledPreviously = false;
    IntentFilter disconnectedFilter;
    BroadcastReceiver disconnectedReceiver;

    private String currentLanguage;
    private String appPreference = "app_pref";
    private String intentMsg = "";

    private String msgNumber = "";
    private String msgContent = "";
    private String SMS_DELIVERED = "vias.sms.msg.delivered";
    private String SMS_SENT = "vias.sms.msg.sent";
    IncomingSMS incomingSMS;
    IntentFilter intentFilter;

    private Location currentLocation;
    private OpenWeatherMap openWeatherMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        intentMsg = "";
        intentMsg = intent.getStringExtra("message");

        currentLanguage = getCurrentLang();

        loadingPanel = (ProgressView) findViewById(R.id.loadingPanel);
        btnDetectHand = (Button) findViewById(R.id.btnDetectHand);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        ring = new RingPlayer(getApplicationContext());

        cameraViewFront = (CameraView) findViewById(R.id.cameraViewFront);
        cameraViewFront.setFacing(CameraKit.Constants.FACING_FRONT);
        cameraViewFront.setFlash(CameraKit.Constants.FLASH_AUTO);

        dateTimeNow = new CurrentDateTime(this);

        cameraViewFront.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                //super.onPictureTaken(picture);
                Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                bitmap = Bitmap.createScaledBitmap(bitmap, HAND_INPUT_SIZE, HAND_INPUT_SIZE, false);

                new HandPictureHandler().execute(bitmap);
            }
        });

        btnDetectHand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //vibrator.vibrate(100);
                //Speak("Xin hãy chờ", "Please wait");
                //recognizeHandCommand();
            }
        });

        btnDetectHand.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                vibrator.vibrate(300);
                Speak("Bạn có yêu cầu gì?", "Can I help you?");

                btnDetectHand.setEnabled(false);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        voiceRecognizing(VOICE_RECOGNITION_COMMAND_CODE);
                    }
                }, 1000);
                return true;
            }
        });

        checkVoiceRecognitionAndTTS();
////
        try {
            tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        if (currentLanguage.equals("vi")) {
                            btnDetectHand.setText("Màn hình chính");
                            tts.setLanguage(new Locale("vi"));
                            tts.setSpeechRate(1.36f);

                        } else {
                            btnDetectHand.setText("Main screen");
                            tts.setLanguage(Locale.US);
                        }
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {

                        }
                        initTensorFlowAndLoadModel();
                        if (intentMsg != null) {
                            if (intentMsg.contains("return")) {

                                if (intentMsg.contains("from navigator")) {
                                    Speak("Đã trở lại màn hình chính của Vias, bạn có muốn mở chế độ phát hiện phương tiện giao thông?",
                                            "Already returned to Vias main screen, do you want to open Vias Vehicles Detector?");
                                    showToastMessage("Đã trở lại màn hình chính của VIAS, bạn có muốn mở chế độ phát hiện phương tiện giao thông?",
                                            "Already returned to VIAS main screen, do you want to open VIAS Vehicles Detector?");

                                    intentMsg = "return";

                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            voiceRecognizing(VOICE_RECOGNITION_CHECK_CALL_VEHICLE_DETECTOR);
                                        }
                                    }, 6500);
                                } else {
                                    Speak("Đã trở lại màn hình chính của Vias", "Already returned to Vias main screen");
                                    showToastMessage("Đã trở lại màn hình chính của VIAS", "Already returned to VIAS main screen");
                                }
                            }
                        } else {
                            Speak("Xin chào, trợ lý ảo Vias đã sẵn sàng", "Vias has been ready");
                            showToastMessage("Xin chào, trợ lý ảo VIAS đã sẵn sàng.", "VIAS has been ready.");
                        }
                    } else {
                        showToastMessage("Giọng nói gặp lỗi", "Text-To-Speech error");
                    }
                }
            });
        } catch (final Exception e) {
        }
        builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        recognizer = SpeechRecognizer.createSpeechRecognizer(this.getApplicationContext());
        commandListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (textMatchList != null && !textMatchList.isEmpty()) {
                    String command = textMatchList.get(0);
                    showToastMessage("Yêu cầu nhận dạng được: " + command, "Recognized request: " + command);
                    command = command.toLowerCase();
                    executeVoiceCommand(command);
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
        contactListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (textMatchList != null && !textMatchList.isEmpty()) {

                    String contact = textMatchList.get(0);
                    if (!contact.matches(".*[a-z].*")) {
                        showToastMessage("Số điện thoại nhận dạng được: " + contact, "Recognized phone number: " + contact);
                    } else {
                        showToastMessage("Tên liên hệ nhận dạng được: " + contact, "Recognized contact name: " + contact);
                    }
                    makeCall(contact);
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
        msgTargetListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (textMatchList != null && !textMatchList.isEmpty()) {

                    String contact = textMatchList.get(0);
                    if (!contact.matches(".*[a-z].*")) {
                        showToastMessage("Số điện thoại nhận dạng được: " + contact, "Recognized phone number: " + contact);
                    } else {
                        showToastMessage("Tên liên hệ nhận dạng được: " + contact, "Recognized contact name: " + contact);
                    }
                    executeVoiceCommand("gửi tin nhắn đến " + contact);
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
        homeControlListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (textMatchList != null && !textMatchList.isEmpty()) {

                    String command = textMatchList.get(0);
                    command = command.toLowerCase();
                    if (isHomeControlCommand(command)){
                    }
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
        wikiObjectListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String wikiRequest = textMatchList != null ? textMatchList.get(0) : null;
                if (wikiRequest != null) {
                    wikiRequest = wikiRequest.toLowerCase();
                    executeVoiceCommand(wikiRequest + " là gì");
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

        destinationListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (textMatchList != null && !textMatchList.isEmpty()) {
                    String destination = textMatchList.get(0);
                    showToastMessage("Điểm đến nhận dạng được: " + destination, "Recognized destination: " + destination);
                    sendTurnByTurnRequest(destination);
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
        newsListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (textMatchList != null && !textMatchList.isEmpty()) {
                    String type = textMatchList.get(0);
                    showToastMessage("Thể loại tin nhận dạng được: " + type, "Recognized news type: " + type);
                    if (type.contains("thời sự") || type.contains("news")) {
                        sendNewsRequest("thoi-su", "thời sự");
                    } else if (type.contains("thế giới") || type.contains("global") || type.contains("world")) {
                        sendNewsRequest("the-gioi", "thế giới");
                    } else if (type.contains("kinh doanh") || type.contains("business")) {
                        sendNewsRequest("kinh-doanh", "kinh doanh");
                    } else if (type.contains("giải trí") || type.contains("entertainment")) {
                        sendNewsRequest("giai-tri", "giải trí");
                    } else if (type.contains("thể thao") || type.contains("sport")) {
                        sendNewsRequest("the-thao", "thể thao");
                    } else if (type.contains("pháp luật") || type.contains("law")) {
                        sendNewsRequest("phap-luat", "pháp luật");
                    } else if (type.contains("giáo dục") || type.contains("education")) {
                        sendNewsRequest("giao-duc", "giáo dục");
                    } else {
                        Speak("Không có thể loại này", "This news type is not available");
                        showToastMessage("Không có thể loại này", "This news type is not available");
                    }
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
        songListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String songRequest = textMatchList != null ? textMatchList.get(0) : null;
                showToastMessage("Tên bài hát nhận dạng được: " + songRequest, "Recognized song name: " + songRequest);
                if (mp3ServiceBinded) {
                    mp3Service.searchAndPlaySong(songRequest);
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
        mathListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String math = textMatchList != null ? textMatchList.get(0) : null;
                Calculator calculator = new Calculator(getApplicationContext());
                String s = calculator.getResultOf(math);
                if (!s.contains("không hợp lệ") && !s.contains("error")) {
                    if (currentLanguage.equals("vi")) {
                        showToastMessage("Kết quả: " + s, "Kết quả: " + s);
                        Speak("Kết quả: " + s, "Kết quả: " + s);
                    } else {
                        showToastMessage("Result: " + s, "Result: " + s);
                        Speak("Result: " + s, "Result: " + s);
                    }
                } else {
                    showToastMessage(s, s);
                    Speak(s, s);
                }
                //showToastMessage(s, s);
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
        checkCallVehicleListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                isRequesting = false;
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String request = textMatchList.get(0).toLowerCase();
                if (request.contains("yes") || request.contains("ok") || request.contains("có") || request.contains("ô kê")) {
                    callViasVehiclesDetector();
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

        registerReceiver(smsSentListener, new IntentFilter(SMS_SENT));  // SMS_SENT is a constant
        registerReceiver(smsDeliveredListener, new IntentFilter(SMS_DELIVERED));  // SMS_SENT is a constant
        msgContentListener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                dialog.dismiss();
                btnDetectHand.setEnabled(true);
                ArrayList<String> textMatchList = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                msgContent = textMatchList.get(0);

                if (!msgNumber.equals("")) {
                    SmsManager manager = SmsManager.getDefault();
                    PendingIntent piSend = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(SMS_SENT), 0);
                    PendingIntent piDelivered = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(SMS_DELIVERED), 0);

                    manager.sendTextMessage(msgNumber, null, msgContent, piSend, piDelivered);
                } else {
                    showToastMessage("Chưa nêu số điện thoại cần gửi", "You haven't tell the phone number yet");
                    Speak("Chưa nêu số điện thoại cần gửi", "You haven't tell the phone number yet");
                }
            }

            @Override
            public void onReadyForSpeech(Bundle params) {

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

        disconnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        disconnectedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                    if (device.getName().contains("VIAS Eyewear")) {
                        BTSend("f");
                        try {
                            BTStop();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        };

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        incomingSMS = new IncomingSMS();
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(incomingSMS, intentFilter);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    private String getCurrentLang(){
        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        if (currentLanguage.equals("vi")) {
            menuInflater.inflate(R.menu.toolbar_menu, menu);
        } else {
            menuInflater.inflate(R.menu.eng_toolbar_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.vie_option || id == R.id.eng_option) {
            Intent intent = new Intent(BaseActivity.this, OptionsActivity.class);
            startActivity(intent);
        } else if (id == R.id.info || id == R.id.eng_info) {
            showAuthorInfo();
        } else if (id == R.id.manual /*|| id == R.id.eng_manual*/) {
            sendHelpRequest();
        } else if (id == R.id.news || id == R.id.eng_news) {
            showNewsChoiceDialog();
        } else if (id == R.id.call || id == R.id.eng_call) {
            executeVoiceCommand("gọi điện thoại");
        } else if (id == R.id.weather || id == R.id.eng_weather){
            callWeatherForecast();
        } /* else if (id == R.id.vehicles || id == R.id.eng_vehicles) {
            executeVoiceCommand("phát hiện xe");
        } */else if (id == R.id.ocr || id == R.id.eng_ocr) {
            executeVoiceCommand("nhận dạng chữ");
        } else if (id == R.id.object_recog || id == R.id.eng_object_recog) {
            recognizeObject();
        } /*else if (id == R.id.bluetooth || id == R.id.eng_bluetooth) {
            if (!deviceConnected) {
                BTStart("f");
                //item.setTitle("Ngắt kết nối kính VIAS");
            } else {
                BTSend("f");
                try {
                    BTStop();
                    //item.setTitle("Kết nối kính VIAS");
                } catch (IOException e) {
                }
            }
        }*/else if (id == R.id.music || id == R.id.eng_music){
            Speak("Bạn muốn nghe bài gì?", "What song do you want me to play?");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    voiceRecognizing(VOICE_RECOGNITION_GET_SONG);
                }
            }, 1500);
        } else if (id == R.id.calculate_item || id == R.id.eng_calculate_item){
            Speak("Xin hãy đọc biểu thức toán!", "Please tell the math expression!");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    voiceRecognizing(VOICE_RECOGNITION_CALCULATE);
                }
            }, 1500);
        } else if (id == R.id.send_sms || id == R.id.eng_send_sms){
            Speak("Bạn muốn gửi tin nhắn đến ai?", "Who you want to send message to?");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    voiceRecognizing(VOICE_RECOGNITION_GET_SMS_TARGET);
                }
            }, 1500);
        } /* else if (id == R.id.home_controller || id == R.id.eng_home_controller){
            Speak("Hãy ra yêu cầu bật tăt thiết bị điện", "");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    voiceRecognizing(VOICE_RECOGNITION_HOME_CONTROL);
                }
            }, 2000);
        }*/ else if (id == R.id.wiki_item || id == R.id.eng_wiki_item){
            Speak("Bạn muốn tìm wikipedia về cái gì?", "What do you want to search wikipedia for?");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    voiceRecognizing(VOICE_RECOGNITION_WIKI);
                }
            }, 2000);
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAuthorInfo() {
        String title, content;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);

        if (currentLanguage.equals("vi")){
            content = getResources().getString(R.string.vie_description);
            title = "Thông tin ứng dụng: ";
            builder.setNegativeButton("Đóng", null);
        } else {
            content = getResources().getString(R.string.eng_description);
            title = "About application:";
            builder.setNegativeButton("Cancel", null);
        }

        builder.setTitle(title);
        builder.setMessage(content);
        builder.show();
        //String s = title + content;
        //Speak(s, s);
    }

    private void showNewsChoiceDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        DialogInterface.OnClickListener dialogChoiceCallback = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        sendNewsRequest("thoi-su","thời sự");
                        break;

                    case 1:
                        sendNewsRequest("the-gioi","thế giới");
                        break;

                    case 2:
                        sendNewsRequest("kinh-doanh","kinh doanh");
                        break;

                    case 3:
                        sendNewsRequest("giai-tri","giải trí");
                        break;

                    case 4:
                        sendNewsRequest("the-thao", "thể thao");
                        break;

                    case 5:
                        sendNewsRequest("phap-luat", "pháp luật");
                        break;

                    case 6:
                        sendNewsRequest("giao-duc", "giáo dục");
                        break;
                }
            }
        };
        if (currentLanguage.equals("vi")) {
            builder.setTitle("Hãy chọn thể loại tin:");
            builder.setNegativeButton("Đóng", null);
            builder.setItems(R.array.vie_news_choice, dialogChoiceCallback);

        } else {
            builder.setTitle("Please choice news type:");
            builder.setNegativeButton("Cancel", null);
            builder.setItems(R.array.eng_news_choice, dialogChoiceCallback);
        }
        builder.setCancelable(true);
        builder.show();
    }

    private void Speak(String viText, String enText) {
        if (tts.isSpeaking()) {
            tts.stop();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        HashMap<String, String> myHashAudio = new HashMap<String, String>();
        int streamType = getResources().getString(R.string.audio_stream).equals("alarm")?AudioManager.STREAM_ALARM:AudioManager.STREAM_MUSIC;
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(streamType));

        switch (currentLanguage) {
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
                switch (currentLanguage) {
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

    private void initTensorFlowAndLoadModel() {
        executorTensorflow.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    hand_classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            HAND_MODEL_FILE,
                            HAND_LABEL_FILE,
                            HAND_INPUT_SIZE,
                            HAND_IMAGE_MEAN,
                            HAND_IMAGE_STD,
                            HAND_INPUT_NAME,
                            HAND_OUTPUT_NAME,
                            HAND_THRESHOLD);
                    /*object_classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            OBJECT_MODEL_FILE,
                            OBJECT_LABEL_FILE,
                            OBJECT_INPUT_SIZE,
                            OBJECT_IMAGE_MEAN,
                            OBJECT_IMAGE_STD,
                            OBJECT_INPUT_NAME,
                            OBJECT_OUTPUT_NAME,
                            OBJECT_THRESHOLD);*/

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingPanel.setVisibility(View.GONE);
                            btnDetectHand.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private void executeHandCommand(String command) {
        switch (command) {
            case "phone":
                getContactandMakeCall();
                break;

            case "point":
                recognizeObject();
                break;

            case "ok":
                recognizeText();
                break;

            case "palm":
                if (!deviceConnected) {
                    BTStart("f");

                } else {
                    BTSend("f");
                    try {
                        BTStop();
                    } catch (IOException e) {
                        throw new RuntimeException("Error stopping bluetooth", e);
                    }
                }
                break;

            default:
                break;
        }
    }

    private boolean BTinitialized() {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Speak("Điện thoại của bạn không hỗ trợ bluetooth", "your device doesn't support bluetooth");
            showToastMessage("Điện thoại của bạn không hỗ trợ bluetooth", "your device doesn't support bluetooth");
            //Speak("Điện thoại của bạn không hỗ trợ bluetooth", "your device doesn't support bluetooth");
        }
        if (!bluetoothAdapter.isEnabled()) {
            Speak("Đang mở bluetooth", "Enabling bluetooth");
            bluetoothAdapter.enable();
        } else {
            isBluetoothEnabledPreviously = true;
        }
        try {
            Thread.sleep(2000);
        } catch (Exception e){
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.isEmpty()) {
            Speak("Trước tiên, điện thoại của bạn phải ghép nối với kính Vias", "Your phone must pair with Vias - Eyewear device first");
            showToastMessage("Trước tiên, điện thoại của bạn phải ghép nối với kính VIAS", "Your phone must pair with VIAS - Eyewear device first");
        } else {
            for (BluetoothDevice iterator : bondedDevices) {

                if (iterator.getName().contains("VIAS Eyewear")) {

                    device = iterator;
                    //showToastMessage(iterator.getAddress(), iterator.getAddress());
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    private boolean BTconnected() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            connected = false;
        }
        if (connected) {
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
            }
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
            }

        }

        return connected;
    }

    void beginListenForData() {
        stopThread = false;
        buffer = new byte[1024];
        Thread thread = new Thread(new Runnable() {
            public void run() {

                while (!Thread.currentThread().isInterrupted() && !stopThread) {

                    try {

                        int byteCount = inputStream.available();
                        if (byteCount > 0) {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string = new String(rawBytes, "UTF-8");
                            //showToastMessage(string, string);
                            if (string.equals("n")) {
                                if (!isRequesting) {
                                    if (currentLanguage.equals("vi"))
                                        tts.setSpeechRate(1.9f);
                                    if (!tts.isSpeaking()) {
                                        Speak("Phát hiện vật cản", "Obstacle detected");
                                        showToastMessage("Phát hiện vật cản", "Obstacle detected");
                                    }
                                    tts.setSpeechRate(1.35f);
                                }
                            }
                        }
                    } catch (IOException ex) {

                        stopThread = true;

                    }
                }
            }
        });
        thread.start();
    }

    private void BTSend(String data) {
        try {
            outputStream.write(data.getBytes());
        } catch (IOException e) {
        }
    }

    private void BTStart(final String startCommand) {

        this.registerReceiver(disconnectedReceiver, disconnectedFilter);

        executorBluetooth.execute(new Runnable() {
            @Override
            public void run() {
                if (BTinitialized()) {

                    showToastMessage("Đang kết nối kính VIAS....", "Start connecting VIAS Eyewear...");
                    Speak("Đang kết nối kính Vias....", "Start connecting Vias Eyewear...");

                    if (BTconnected()) {

                        deviceConnected = true;
                        Speak("Kính Vias đã kết nối", "Vias Eyewear, engaged");
                        showToastMessage("Kính VIAS đã kết nối", "VIAS Eyewear, engaged");
                        beginListenForData();
                        BTSend(startCommand);
                    } else {
                        Speak("Kết nối kính Vias thất bại", "Connecting failed");
                        showToastMessage("Kết nối kính VIAS thất bại", "Connecting failed");
                    }
                } else {
                    Speak("Không tìm thấy kính Vias", "Can't find Vias Eyewear");
                    showToastMessage("Không tìm thấy kính VIAS", "Can't find VIAS Eyewear");
                }
            }
        });
    }

    private void BTStop() throws IOException {
        stopThread = true;
        try {
            outputStream.close();
            inputStream.close();
            socket.close();
        } catch (IOException e){
            throw e;
        }
        deviceConnected = false;
        this.unregisterReceiver(disconnectedReceiver);
        Speak("Đã ngắt kết nối kính Vias", "Disconnect Vias Eyewear");
        showToastMessage("Ngắt kết nối kính VIAS", "Disconnect VIAS Eyewear");
    }

    public void recognizeHandCommand() {
        btnDetectHand.setEnabled(false);

        cameraViewFront.captureImage();
    }

    private class HandPictureHandler extends AsyncTask<Bitmap, Void, String>{

        @Override
        protected String doInBackground(Bitmap... params) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingPanel.setVisibility(View.VISIBLE);
                }
            });

            Bitmap bitmap = params[0];

            final List<Classifier.Recognition> results = hand_classifier.recognizeImage(bitmap);
            String s = results.toString();
            s = s.substring(1, s.length() - 1);

            return s;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (!s.equals("") && !s.equals("none")) {
                ring.play();
                executeHandCommand(s);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnDetectHand.setEnabled(true);
                    loadingPanel.setVisibility(View.GONE);
                }
            });
        }
    }

    public void recognizeObject(){
        Intent intent = new Intent(BaseActivity.this, ClassifierActivity.class);

        try {
            Speak("Đang mở chế độ nhận dạng vật thể...", "Opening Vias - Object Recognizer...");
            showToastMessage("Đang mở chế độ nhận dạng vật thể", "Opening VIAS - Object Recognizer...");
            startActivity(intent);
        } catch (Exception e) {
            Speak("chế độ nhận dạng vật thể chưa được cài đặt", "Vias - Object Recognizer has not been installed");
            showToastMessage("Chế độ nhận dạng vật thể chưa được cài đặt", "VIAS - Object Recognizer has not been installed");
        }
    }

    public void recognizeText() {
        Speak("Đang mở chế độ nhận dạng chữ...", "Opening text recognizer...");
        showToastMessage("Đang mở chế độ nhận dạng chữ...", "Opening text recognizer...");

        Intent intent = new Intent(BaseActivity.this, OCRActivity.class);
        startActivity(intent);
    }

    public void getContactandMakeCall() {
        Speak("xin hãy đọc số điện thoại hoặc tên liên hệ",
                "please tell number phone or contact name");
        showToastMessage("Xin hãy đọc số điện thoại hoặc tên liên hệ",
                "Please tell number phone or contact name");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                voiceRecognizing(VOICE_RECOGNITION_GET_CONTACT_CODE);
            }
        }, 1500);
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    public void makeCall(String contact) {
        if (!contact.matches(".*[a-z].*")) {
            Speak("Đang gọi tới số " + contact, "Start calling to " + contact);
            showToastMessage("Đang gọi tới số " + contact + "...", "Start calling to " + contact + "...");

            final Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + contact));

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(callIntent);
                }
            }, 3000);
        } else {
            String[] PROJECTION = new String[]{
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };

            ContentResolver cr = getContentResolver();
            // Tạo con trỏ truy vấn danh bạ điện thoại
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                    new String[]{contact}, // tiến hành tìm kiếm tên liên hệ với tham biến contact
                    null);
            String number = "";
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    final int indexNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    number = cursor.getString(indexNumber);
                    //number = number.replace("0", "");
                    cursor.close();
                    if (!number.equals("")) {
                        // Nếu tìm thấy
                        //number = number.trim();
                        number = number.replace(" ", "");

                        Speak("Đang gọi tới " + contact, "Calling to " + contact);
                        showToastMessage("Đang gọi tới " + contact + "...", "Calling to " + contact + "...");

                        final Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse("tel:" + number));

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(callIntent);
                            }
                        }, 1500);

                    } else {
                        Speak("Không tìm thấy liên hệ \"" + contact + "\"", "Not found \"" + contact + "\"");
                        showToastMessage("Không tìm thấy liên hệ \"" + contact + "\"", "Not found \"" + contact + "\"");
                    }
                } else {
                    Speak("Không tìm thấy liên hệ \"" + contact + "\"", "Not found \"" + contact + "\"");
                    showToastMessage("Không tìm thấy liên hệ \"" + contact + "\"", "Not found \"" + contact + "\"");
                }
            } else {
                Speak("Không tìm thấy liên hệ \"" + contact + "\"", "Not found \"" + contact + "\"");
                showToastMessage("Không tìm thấy liên hệ \"" + contact + "\"", "Not found \"" + contact + "\"");
            }
        }
    }

    public void checkVoiceRecognitionAndTTS() {
        // Kiem tra thiet bi cho phep nhan dang giong noi hay ko
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            Speak("Nhận dạng giọng nói chưa được cài đặt", "Voice recognizer is not present");
            showToastMessage("Nhận dạng giọng nói chưa được cài đặt", "Voice recognizer is not present");
        }
    }

    private void callWeatherForecast(){
        if (openWeatherMap == null) {
            tts.setSpeechRate(1.6f);
            Speak("Xin hãy chờ", "Please wait");
            tts.setSpeechRate(1.36f);
            showToastMessage("Xin hãy chờ", "Please wait");
            new GetWeather().execute(RequestURL.apiRequest(currentLocation.getLatitude(), currentLocation.getLongitude()));
        } else {
            String weatherForecast = currentLanguage.equals("vi")?"Tình hình thời tiết ở ":"Weather condition in ";
            weatherForecast += openWeatherMap.getName() + ", " + openWeatherMap.getSys().getCountry() + ": ";
            weatherForecast += currentLanguage.equals("vi")?
                    openWeatherMap.getWeather().get(0).getVieDescription():
                    openWeatherMap.getWeather().get(0).getDescription();
            weatherForecast += ", ";

            weatherForecast += currentLanguage.equals("vi")?"Nhiệt độ trung bình: ":"Average temperature: ";
            weatherForecast += openWeatherMap.getMain().getTemp();
            weatherForecast += currentLanguage.equals("vi")?" độ C, ": "C degrees, ";

            weatherForecast += currentLanguage.equals("vi")?"Độ ẩm: ":"Huminity: ";
            weatherForecast += openWeatherMap.getMain().getHumidity() + "%, ";

            Speak(weatherForecast, weatherForecast);
            showToastMessage(weatherForecast, weatherForecast);
        }
    }

    public void callViasVehiclesDetector() {
        Intent intent = new Intent("org.tensorflow.demo.SHARE");
        intent.putExtra("lang", currentLanguage);
        try {
            Speak("Đang mở chế độ phát hiện phương tiện giao thông...", "Opening Vias - Vehicles Detector");
            showToastMessage("Đang mở chức năng phát hiện phương tiện giao thông", "Opening VIAS - Vehicles Detector...");
            startActivity(intent);
        } catch (Exception e) {
            Speak("chế độ phương tiện giao thông chưa được cài đặt", "Vias - Vehicles Detector has not been installed");
            showToastMessage("chế độ phát hiện phương tiện giao thông chưa được cài đặt", "VIAS - Vehicles Detector has not been installed");
        }
        //finishAndRemoveTask();
    }

    public void voiceRecognizing(final int code) {

        recognizer.cancel();

        if (mp3ServiceBinded) {
            String s = mp3Service.getCurrentStatus();
            if (!s.equals("paused") && !s.equals("stopped") && !s.equals("none")) {
                mp3Service.pauseSong();
                mp3Service.setCurrentStatus("stand by");
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                String textHint = "";

                RecognitionListener listener = new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle params) {

                    }

                    @Override
                    public void onBeginningOfSpeech() {

                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {

                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {

                    }

                    @Override
                    public void onEndOfSpeech() {

                    }

                    @Override
                    public void onError(int error) {

                    }

                    @Override
                    public void onResults(Bundle results) {

                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {

                    }

                    @Override
                    public void onEvent(int eventType, Bundle params) {

                    }
                };
                switch (code) {
                    case VOICE_RECOGNITION_GET_CONTACT_CODE:
                        if (currentLanguage.equals("vi")) {
                            textHint = "Đọc số điện thoại hoặc tên liên hệ";
                        }
                        else {
                            textHint = "Tell number phone or contact name";
                        }
                        listener = contactListener;
                        break;

                    case VOICE_RECOGNITION_SEND_MSG:
                        if (currentLanguage.equals("vi")) {
                            textHint = "Xin hãy đọc nội dung tin nhắn!";
                        }
                        else {
                            textHint = "Please tell the message's content!";
                        }
                        listener = msgContentListener;
                        break;

                    case VOICE_RECOGNITION_COMMAND_CODE:
                        if (currentLanguage.equals("vi"))
                            textHint = "Bạn có yêu cầu gì?";
                        else
                            textHint = "Can I help you?";
                        listener = commandListener;
                        break;

                    case VOICE_RECOGNITION_GET_DESTINATION:
                        if (currentLanguage.equals("vi"))
                            textHint = "Xin hãy nêu điểm đến!";
                        else
                            textHint = "Please tell destination!";
                        listener = destinationListener;
                        break;

                    case VOICE_RECOGNITION_GET_NEWS_TYPE:
                        if (currentLanguage.equals("vi"))
                            textHint = "Xin hãy nêu thể loại tin!";
                        else
                            textHint = "Please tell news type";
                        listener = newsListener;
                        break;

                    case VOICE_RECOGNITION_GET_SONG:
                        if (currentLanguage.equals("vi"))
                            textHint = "Bạn muốn nghe bài gì?";
                        else
                            textHint = "What song do you want me to play?";
                        listener = songListener;
                        break;

                    case VOICE_RECOGNITION_CHECK_CALL_VEHICLE_DETECTOR:
                        if (currentLanguage.equals("vi")){
                            textHint = "Bạn có muốn mở chế độ phát hiện phương tiện giao thông?";
                        } else {
                            textHint = "Do you want to open VIAS Vehicles Detector?";
                        }
                        listener = checkCallVehicleListener;
                        break;

                    case VOICE_RECOGNITION_CALCULATE:
                        if (currentLanguage.equals("vi")){
                            textHint = "Xin hãy đọc biểu thức toán!";
                        } else {
                            textHint = "Please tell me the math expression";
                        }
                        listener = mathListener;
                        break;

                    case VOICE_RECOGNITION_GET_SMS_TARGET:
                        if (currentLanguage.equals("vi")){
                            textHint = "Bạn muốn gửi tin nhắn đến ai";
                        } else {
                            textHint = "Who you want to send message?";
                        }
                        listener = msgTargetListener;
                        break;

                    case VOICE_RECOGNITION_HOME_CONTROL:
                        if (currentLanguage.equals("vi")){
                            textHint = "Hãy ra yêu cầu bật/tắt thiết bị điện";
                        } else {
                            textHint = "Please tell me your request about controlling electric devices";
                        }
                        listener = homeControlListener;
                        break;

                    case VOICE_RECOGNITION_WIKI:
                        if (currentLanguage.equals("vi")){
                            textHint = "Bạn muốn tìm Wikipedia về điều gì?";
                        } else {
                            textHint = "What do you want to search Wikipedia for?";
                        }
                        listener = wikiObjectListener;
                        break;

                    default:
                        break;
                }

                // Khởi tạo intent nhận dạng giọng nói
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                // Khai báo bộ dữ liệu nhận dạng, ở đây chọn kiểu WEB SEARCH do có khả năng tối ưu kêt quả
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage);
                intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, currentLanguage);
                // Khai báo tên ứng dụng tham gia nhận dạng giọng nói
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
                // Khai báo số lượng kết quả trả về tối đa, ở đây là 1
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, textHint);

                DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                        dialog.dismiss();
                        //ring.playBtnOffSound();
                        vibrator.vibrate(60);
                        isRequesting = false;

                        recognizer.stopListening();
                        btnDetectHand.setEnabled(true);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mp3ServiceBinded) {
                                    String s = mp3Service.getCurrentStatus();
                                    if (!s.equals("paused") && !s.equals("stopped") && !s.equals("none")) {
                                        mp3Service.resumeSong();
                                    }
                                };
                            }
                        }, 1000);

                    }
                };
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                        //ring.playBtnOffSound();
                        vibrator.vibrate(60);
                        isRequesting = false;

                        recognizer.cancel();
                        btnDetectHand.setEnabled(true);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mp3ServiceBinded) {
                                    String s = mp3Service.getCurrentStatus();
                                    if (!s.equals("paused") && !s.equals("stopped") && !s.equals("none")) {
                                        mp3Service.resumeSong();
                                    }
                                };
                            }
                        }, 1000);
                    }
                };
                if (currentLanguage.equals("vi")) {
                    builder.setMessage("Đang ghi âm...");
                    builder.setNegativeButton("Hủy", onClickListener);
                } else {
                    builder.setMessage("Recording...");
                    builder.setNegativeButton("Cancel", onClickListener);
                }

                isRequesting = true;
                recognizer.setRecognitionListener(listener);
                recognizer.startListening(intent);

                builder.setTitle(textHint);
                builder.setCancelable(true);
                builder.setOnCancelListener(onCancelListener);

                builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {

                            vibrator.vibrate(200);
                            return false;
                        } else if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                            //Speak("abc", "abc");
                            if (isRequesting) {
                                dialog.dismiss();
                                //ring.playBtnOffSound();
                                vibrator.vibrate(60);
                                isRequesting = false;

                                recognizer.stopListening();
                                btnDetectHand.setEnabled(true);
                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mp3ServiceBinded) {
                                            String s = mp3Service.getCurrentStatus();
                                            if (!s.equals("paused") && !s.equals("stopped") && !s.equals("none")) {
                                                mp3Service.resumeSong();
                                            }
                                        }
                                        ;
                                    }
                                }, 1000);
                            } else {
                                headsetBtnTap++;
                                if (headsetBtnTap >= 2) {
                                    headsetBtnTap = 0;

                                    Speak("Bạn có yêu cầu gì?", "Can I help you?");
                                    vibrator.vibrate(300);
                                    //Speak("Bạn có yêu cầu gì?", "Can I help you?");
                                    btnDetectHand.setEnabled(false);
                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            voiceRecognizing(VOICE_RECOGNITION_COMMAND_CODE);
                                        }
                                    }, 1000);
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

    private void sendHelpRequest() {
        Intent intent = new Intent(BaseActivity.this, HelpActivity.class);
        Speak("Đang mở hướng dẫn sử dụng...", "Opening user manual...");
        showToastMessage("Đang mở hướng dẫn sử dụng...", "Opening user manual...");
        startActivity(intent);
    }

    private void sendNewsRequest(String RSSTypeName, String UTF8TypeName) {
        Intent intent = new Intent(BaseActivity.this, NewsReaderActivity.class);
        intent.putExtra("RSSType", RSSTypeName);
        intent.putExtra("UTF8Type", UTF8TypeName);
        startActivity(intent);
    }

    private void sendTurnByTurnRequest(String destination) {
        Speak("bắt đầu dẫn đường đến " + destination + " bằng Google Maps, " +
                        "Để biết hướng đi, hãy click vào bên trái phía trên màn hình. " +
                        "Sau khi biết hướng đi, hãy click vào giữa màn hình để mở la bàn. " +
                        "Để quay lại màn hình chính, nhấn giữ vào giữa màn hình",
                "Start navigating to " + destination + " by Google Maps. Make sure you are on a certain road." +
                        "To get direction, click on the top-left corner of the screen " +
                        "After having direction, click on the middle of the screen to open voice compass" +
                        " To return main screen, hold on the middle of the screen");
        showToastMessage("Bắt đầu dẫn đường đến " + destination, "Start navigating to " + destination);

        /*Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destination + "&mode=w");
        final Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");*/

        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destination +"&mode=w");
        final Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        recognizer.destroy();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(mapIntent);
                Intent intent = new Intent(BaseActivity.this, NavigationService.class);
                startService(intent);
                finishAndRemoveTask();
            }
        }, 6000);
    }

    private void executeVoiceCommand(String command) {

        //Speak("xin hãy chờ", "please wait");
        //showToastMessage("Xin hãy chờ...", "Please wait...");
        command = command.replace("= mấy", "bằng mấy");
        if (command.contains("mấy giờ") || command.contains("what time")) {
            String time = dateTimeNow.currentTime();
            Speak(time, time);
            showToastMessage(time + ".", time + ".");
        } else if (command.contains("ngày mấy") || command.contains("ngày gì")
                || (command.contains("what") && command.contains("the date"))) {
            String date = dateTimeNow.currentDate();
            Speak(date, date);
            showToastMessage(date + ".", date + ".");
        } else if (command.contains("gọi") || command.contains("dial")
                || command.contains("call") || command.contains("phone")) {
            String s = command.replace("gọi", "");
            s = s.replace("call", "")
                    .replace("to", "")
                    .trim();
            if (s.equals("") || command.contains("điện thoại")) {
                getContactandMakeCall();
            } else {
                makeCall(s);
            }
        } /*else if (isHomeControlCommand(command)) {

        } */else if (command.contains("dẫn đường") || command.contains("navigate")) {
            String destination;
            destination = command.replace("dẫn đường", "");
            destination = destination.replace("navigate", "");
            destination = destination.replace("đến", "");
            destination = destination.replace("to", "");
            if (!destination.equals("") && destination.length() >= 3) {
                sendTurnByTurnRequest(destination);
            } else {
                Speak("Xin hãy nêu điểm đến", "Please tell me the destination");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        voiceRecognizing(VOICE_RECOGNITION_GET_DESTINATION);
                    }
                }, 1500);            }

        } /*else if ((command.contains("kết nối") && (command.contains("kính")
                || command.contains("thiết bị")))
                || (command.contains("connect") && (command.contains("glasses")
                || command.contains("eyewear") || command.contains("device")))) {

            if (command.contains("ngắt") || command.contains("ngừng") || command.contains("dừng") || command.contains("hủy")
                    || command.contains("disconnect")){
                BTSend("f");
                try {
                    BTStop();
                } catch (IOException e) {
                }
            } else {
                if (!deviceConnected) {
                    BTStart("f");
                }
            }
        } */
        /*else if (command.contains("phát hiện xe") || command.contains("trợ giúp qua đường")
                || command.contains("phương tiện giao thông")
                || command.contains("detect vehicle") || command.contains("car")
                || (command.contains("cross") && command.contains("road"))) {

            callViasVehiclesDetector();

        }*/ else if (command.contains("tôi") && (command.contains("ở") || command.contains("nơi")) && command.contains("đâu")
                || (command.contains("where") && (command.contains("I")))){
            Geocoder geocoder;
            List<Address> addresses = null;
            geocoder = new Geocoder(this, Locale.getDefault());

            try {
                addresses = geocoder.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                String addrInfo = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL

                if (addrInfo.trim().substring(0, 1).matches("\\d+")) {
                    addrInfo = currentLanguage.equals("vi") ? "Bạn đang ở " + addrInfo : "You are currenly at " + addrInfo;
                } else {
                    addrInfo = currentLanguage.equals("vi") ? "Bạn đang ở " + addrInfo : "You are currenly in " + addrInfo;
                }
                Speak(addrInfo, addrInfo);
                showToastMessage(addrInfo, addrInfo);
            } catch (IOException e) {
                showToastMessage("Có lỗi", "Error");
            }
           
        } else if (command.contains("tin") && command.contains("nhắn")
                || command.contains("send") && (command.contains("message") || command.contains("sms"))){
            String contact = command.replace("tin", "");
            contact = contact.replace("nhắn", "")
                    .replace("gửi", "")
                    .replace("đến", "")
                    .replace("tới", "")
                    .replace("cho", "")
                    .replace("số", "")
                    .replace("điện thoại", "")
                    .replace("  ", " ")
                    .trim();
            contact = contact.replace("message", "")
                    .replace("sms", "")
                    .replace("send", "")
                    .replace("to", "")
                    .replace("for", "")
                    .replace("  ", " ")
                    .trim();
            if (!contact.equals("")) {
                if (!contact.matches(".*[a-z].*")) {
                    msgNumber = contact;
                    Speak("Xin hãy đọc nội dung cần gửi", "Please tell me the message's content");
                    showToastMessage("Xin hãy đọc nội dung cần gửi", "Please tell me the message's content");
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            voiceRecognizing(VOICE_RECOGNITION_SEND_MSG);
                        }
                    }, 1500);
                } else {
                    //showToastMessage(contact, contact);

                    String[] PROJECTION = new String[]{
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    };
                    ContentResolver cr = getContentResolver();

                    // Tạo con trỏ truy vấn danh bạ điện thoại
                    Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                            new String[]{contact}, // tiến hành tìm kiếm tên liên hệ với tham biến contact
                            null);
                    String number = "";
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            final int indexNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            number = cursor.getString(indexNumber);
                            cursor.close();
                            if (!number.equals("")) {
                                msgNumber = number;
                                Speak("Xin hãy đọc nội dung cần gửi", "Please tell the message's content");
                                showToastMessage("Xin hãy đọc nội dung cần gửi", "Please tell the message's content");
                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        voiceRecognizing(VOICE_RECOGNITION_SEND_MSG);
                                    }
                                }, 1500);
                            }
                        }
                    }
                }
            } else {
                Speak("Bạn muốn gửi tin nhắn đến ai?", "Who you want to send message to?");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        voiceRecognizing(VOICE_RECOGNITION_GET_SMS_TARGET);
                    }
                }, 1500);
            }
        } else if(command.contains("thời tiết") || command.contains("weather")) {

            callWeatherForecast();

        } else if ((command.contains("nhận dạng")&& command.contains("vật thể"))
                || ((command.contains("đây") || command.contains("này")) && command.contains("là") && command.contains("gì"))
                || (command.contains("what") && (command.contains("this") || command.contains("it")))
                || (command.contains("recognize")&& (command.contains("object") || command.contains("thing")))){
            recognizeObject();
        } else if ((command.contains("đọc") && (command.contains("tin") ||  command.contains("báo"))) ||
                (command.contains("read") && command.contains("news"))) {
            Speak("Đang mở đọc báo...", "Opening news reader...");
            if (command.contains("thời sự") || command.contains("news")) {
                sendNewsRequest("thoi-su", "thời sự");
            } else if (command.contains("thế giới") || command.contains("global") || command.contains("world")) {
                sendNewsRequest("the-gioi", "thế giới");
            } else if (command.contains("kinh doanh") || command.contains("business")) {
                sendNewsRequest("kinh-doanh", "kinh doanh");
            } else if (command.contains("giải trí") || command.contains("entertainment")) {
                sendNewsRequest("giai-tri", "giải trí");
            } else if (command.contains("thể thao") || command.contains("sport")) {
                sendNewsRequest("the-thao", "thể thao");
            } else if (command.contains("pháp luật") || command.contains("law")) {
                sendNewsRequest("phap-luat", "pháp luật");
            } else if (command.contains("giáo dục") || command.contains("education")) {
                sendNewsRequest("giao-duc", "giáo dục");
            } else {
                Speak("Xin hãy nêu thể loại tin tức", "Please tell the news type");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        voiceRecognizing(VOICE_RECOGNITION_GET_NEWS_TYPE);
                    }
                }, 1500);
            }
        } else if ((command.contains("hướng dẫn") || command.contains("trợ giúp")
                || command.contains("help") || command.contains("guide"))
                && ((command.contains("sử dụng") || command.contains("use")))
                || command.contains("manual")) {

            sendHelpRequest();

        } else if (command.contains("đọc văn bản") || command.contains("đọc chữ")
                || command.contains("nhận dạng chữ")
                || command.contains("read text") || command.contains("recognize text")
                || command.contains("recognize word")) {

            recognizeText();

        } else if (command.contains("nhạc") || command.contains("bài hát")
                || command.contains("song") || command.contains("music")) {
            if(mp3ServiceBinded) {

                executeMp3Command(command);
            }

        } else if (command.contains("tao")
                && (command.contains("buồn") || command.contains("vui") || command.contains("chán"))) {
            Speak("Kệ mày", "");
            showToastMessage("Kệ mày", "");

        } else if (((command.contains("là") || command.contains("gì")) && (command.indexOf("là") < command.indexOf("gì")))
                    || ((command.contains("là") || command.contains("ai")) && (command.indexOf("là") < command.indexOf("ai")))
                    || ((command.contains("what") || command.contains("who")) && (command.contains("is") || command.contains("are")))
                    || command.contains("thông tin về") || command.contains("tìm hiểu về")
                || command.contains("information about")){

            if (currentLanguage.equals("vi")) {
                if (command.contains("là")){
                    command = command.substring(0, command.indexOf("là"));
                }
                command = command.replace("thông tin về", "");
                command = command.replace("tìm hiểu về", "");
            } else {
                command = command.replace("what is", "")
                                 .replace("who is", "")
                                 .replace("what are", "")
                                 .replace("who are", "");
                if (command.contains("about")) {
                    command = command.substring(command.indexOf("about"));
                    command = command.replace("about", "");
                }
            }
            new getWikiSearchData().execute(command);

        }  else if (command.contains("tính") || command.contains("làm toán") || command.contains("bằng mấy") || command.contains("calculate")){
            boolean noExp = true;
            if (!command.contains("làm toán") && !command.contains("tính toán") || command.contains("calculate")) {
                if (command.contains("tính") || command.contains("calculate")) {
                    if (!command.replace("tính", "").replace("calculate", "").trim().equals("")) {
                        noExp = false;
                        command = command.replace("tính", "").trim();
                        String s = new Calculator(getApplicationContext()).getResultOf(command);

                        if (!s.contains("không hợp lệ") && !s.contains("error")) {
                            if (currentLanguage.equals("vi")) {
                                showToastMessage("Kết quả: " + s, "Kết quả: " + s);
                                Speak("Kết quả: " + s, "Kết quả: " + s);
                            } else {
                                showToastMessage("Result: " + s, "Result: " + s);
                                Speak("Result: " + s, "Result: " + s);
                            }
                        } else {
                            showToastMessage(s, s);
                            Speak(s, s);
                        }
                    }
                } else if (command.contains("bằng mấy") || command.contains("equal to") || command.contains("result of")){
                    if (!command.replace("bằng mấy", "").replace("equal to", "").replace("result of", "").trim().equals("")) {
                        noExp = false;
                        command = command.replace("bằng mấy", "").trim();
                        String s = new Calculator(getApplicationContext()).getResultOf(command);
                        if (!s.contains("không hợp lệ") && !s.contains("error")) {
                            if (currentLanguage.equals("vi")) {
                                showToastMessage("Bằng: " + s, "Bằng: " + s);
                                Speak("Bằng: " + s, "Bằng: " + s);
                            } else {
                                showToastMessage("Equal to " + s, "Equal to: " + s);
                                Speak("Equal to: " + s, "Equal to: " + s);
                            }
                        } else {
                            showToastMessage(s, s);
                            Speak(s, s);
                        }
                    }
                }
            }
            if (noExp) {
                Speak("Xin hãy đọc biểu thức toán", "Please tell the math expression");
                showToastMessage("Xin hãy đọc biểu thức toán", "Please tell the math expression");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        voiceRecognizing(VOICE_RECOGNITION_CALCULATE);
                    }
                }, 1500);
            }
        } else {
            Speak("không rõ yêu cầu", "I can't see your request");
            showToastMessage("Không rõ yêu cầu", "Can't see your request");
            if (mp3ServiceBinded) {
                String s = this.mp3Service.getCurrentStatus();
                if (!s.equals("paused") && !s.equals("stopped")) {
                    this.mp3Service.resumeSong();
                }
            }
        }
    }

    private ArrayList<String> getDeviceList(){
        ArrayList<String> arrayList = new ArrayList<String>();

        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        for (int i = 0; i < 4; i++){
            String s = sharedPreferences.getString("device"+i, currentLanguage.equals("vi")?"Thiết bị "+(i+1):"Device "+(i+1));
            arrayList.add(s);
        }

        return arrayList;
    }

    private ArrayList<Integer> checkDevice(ArrayList<String> deviceList, String command){

        ArrayList<Integer> list = new ArrayList<>();
        command = command.toLowerCase();
        for (int i = 0; i < deviceList.size(); i++){
            String dv = deviceList.get(i).toLowerCase();
            if (command.contains(dv)){
                list.add(i);
            }
        }

        return list;
    }

    private void executeMp3Command(String command){

        command = command.toLowerCase();

        if (command.contains("lặp lại") || command.contains("lập lại") || command.contains("phát lại") || command.contains("restart")){

            //if (mp3ServiceBinded) mp3Service.resumeSong();
            this.mp3Service.restartSong();

        } else if (command.contains("ngẫu nhiên") || command.contains("random")){
                if (command.contains("tắt") || command.contains("ngừng") || command.contains("dừng")
                        ||command.contains("stop") || command.contains("disable")) {

                    this.mp3Service.setRandom(false);
                } else {

                    this.mp3Service.setRandom(true);
                }
            //if (mp3ServiceBinded) mp3Service.resumeSong();

        } else if ((command.contains("mở") || command.contains("bật") || command.contains("chơi") || command.contains("nghe"))  || command.contains("bài hát")
                ||command.contains("open") || command.contains("play")) {

            if (command.contains("tiếp") || command.contains("sau") || command.contains("next")){

                this.mp3Service.searchAndPlaySong("sau");

            } else if (command.contains("trước") || command.contains("previous")){

                this.mp3Service.searchAndPlaySong("trước");

            } else {

                if (command.contains("bài hát")) {
                    command = command.substring(command.indexOf("bài hát"));
                    command = command.replace("bài hát", "");

                } else if (command.contains("song")){
                    command = command.replace("song", "");
                    command = command.replace("open", "");
                    command = command.trim();
                } else {
                    command = "";
                }

                if (!command.equals("")) {
                    this.mp3Service.searchAndPlaySong(command.trim());
                } else {
                    Speak("Bạn muốn nghe bài gì?", "What song do you want me to play?");
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            voiceRecognizing(VOICE_RECOGNITION_GET_SONG);
                        }
                    }, 1500);
                }
            }
        } else if (command.contains("tạm dừng") || command.contains("tạm ngưng") || command.contains("pause")) {

            //Speak("Tạm ngưng nhạc", "Pause song");
            this.mp3Service.pauseSong();

        } else if (command.contains("tiếp") || command.contains("continue") || command.contains("resume")) {

            this.mp3Service.resumeSong();

        } else if (command.contains("ngưng")|| command.contains("dừng") || command.contains("stop")){

            this.mp3Service.stopSong();

        } else {
            Speak("không rõ yêu cầu", "I can't see your request");
            showToastMessage("Không rõ yêu cầu", "Can't see your request");
            String s = this.mp3Service.getCurrentStatus();
            if (!s.equals("paused") && !s.equals("stopped")) {
                this.mp3Service.resumeSong();
            }
        }
    }

    private boolean isHomeControlCommand(final String command){

        String s = command.toLowerCase()
                .replace("mở", "")
                .replace("bật", "")
                .replace("turn on", "")
                .replace("tắt", "")
                .replace("đóng", "")
                .replace("turn off", "")
                .replace(" ", "");

        RequestSender requestSender = new RequestSender(BaseActivity.this);
        if ((command.contains("trạng thái")||command.contains("status")||command.contains("state")||command.contains("tình trạng"))
                && (command.contains("thiết bị")||command.contains("device"))){
            String databaseName = "HomeControllerDB.sqlite";
            DatabaseManager databaseManager = new DatabaseManager(BaseActivity.this, databaseName);
            final StringBuilder allDeviceStatus = new StringBuilder();

            ArrayList<Controller>allControllers = databaseManager.getAllControllers();

            final int triggerPoint = allControllers.get(allControllers.size() - 1).getController_id();
            for (int i = 0; i < allControllers.size(); i++) {
                final Controller controller = allControllers.get(i);

                final StringBuilder sb = new StringBuilder();

                requestSender.sendRequest("http://" + controller.getIpAddr(), controller, new ResponseEvent() {
                    @Override
                    public void onDone(ArrayList<Device> selectedDevices) {

                        if (currentLanguage.equals("vi")) {

                            sb.append("Trạng thái các thiết bị điện của bộ điều khiển ");
                            sb.append(controller.getName());
                            sb.append(": ");
                            ArrayList<Device> devices = controller.getDevices();
                            for (int i1 = 0; i1 < devices.size(); i1++) {
                                Device dv = devices.get(i1);

                                sb.append(dv.getName());
                                sb.append(dv.getStatus().toLowerCase().equals("on") ? " đã bật" : " đã tắt");
                                if (i1 != devices.size() - 1) sb.append(", "); else sb.append("\n");
                            }
                        } else {

                            sb.append("States of the ");
                            sb.append(controller.getName());
                            sb.append("'s devices: ");

                            ArrayList<Device> devices = controller.getDevices();
                            for (int i1 = 0; i1 < devices.size(); i1++) {
                                Device dv = devices.get(i1);

                                sb.append(dv.getName());
                                sb.append(dv.getStatus().toLowerCase().equals("on") ? " is on" : " is off");
                                if (i1 != devices.size() - 1) sb.append(", "); else sb.append(".");
                            }
                        }
                        allDeviceStatus.append(sb.toString());
                        allDeviceStatus.append("\n");

                        if (controller.getController_id() == triggerPoint) {
                            String stt = allDeviceStatus.toString();
                            Speak(stt, stt);
                            showToastMessage(stt, stt);
                        }
                    }

                    @Override
                    public void onError() {

                        if (currentLanguage.equals("vi")) {

                            sb.append("Trạng thái các thiết bị điện của bộ điều khiển ");
                            sb.append(controller.getName());
                            sb.append(" không rõ");
                        } else {

                            sb.append("States of all devices of the ");
                            sb.append(controller.getName()); sb.append(" controller");
                            sb.append(" are unknown.");
                        }
                        allDeviceStatus.append(sb.toString());
                        allDeviceStatus.append(".\n");

                        if (controller.getController_id() == triggerPoint) {
                            String stt = allDeviceStatus.toString();
                            Speak(stt, stt);
                            showToastMessage(stt, stt);
                        }
                    }
                });
            }
            return true;
        } else if (!s.equals("")) {

            if (requestSender.sendRawRequest(command, new ResponseEvent() {
                    @Override
                    public void onDone(ArrayList<Device> selectedDevices) {

                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < selectedDevices.size(); i++) {
                            Device device = selectedDevices.get(i);

                            sb.append(device.getName());
                            if (device.getStatus().toLowerCase().equals("on")) {
                                sb.append(" ");
                                sb.append(currentLanguage.equals("vi") ? "đã bật" : "on");
                            } else if (device.getStatus().toLowerCase().equals("off")) {
                                sb.append(" ");
                                sb.append(currentLanguage.equals("vi") ? "đã tắt" : "off");
                            }
                            if (i < selectedDevices.size() - 1) sb.append("\n");
                        }
                        String s = sb.toString();
                        showToastMessage(s, s);
                        Speak(s, s);
                    }

                    @Override
                    public void onError() {

                        String s = currentLanguage.equals("vi")?"Đã xảy ra lỗi!":"Error!";
                        showToastMessage(s, s);
                        Speak(s, s);
                    }
                })) {
                return true;
            }
        }
        return false;
    }

    private class GetWeather extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... params) {
            String stream = null;
            String urlString = params[0];

            HttpDataLoader http = new HttpDataLoader();
            stream = http.getHTTPData(urlString);
            return stream;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s.contains("Error: Not found city")){
                return;
            }
            Gson gson = new Gson();
            Type mType = new TypeToken<OpenWeatherMap>(){}.getType();
            openWeatherMap = gson.fromJson(s, mType);

            callWeatherForecast();
        }
    }

    private class getWikiSearchData extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            String stream = null;
            //ring.play();
            String urlString = currentLanguage.equals("vi")?
                    "https://vi.wikipedia.org/w/api.php?action=opensearch" +
                    "&search=" + params[0] +
                    "&limit=1&namespace=0"
                    : "https://en.wikipedia.org/w/api.php?action=opensearch" +
                    "&search=" + params[0] +
                    "&limit=1&namespace=0";
            HttpDataLoader http = new HttpDataLoader();
            stream = http.getHTTPData(urlString);
            return stream;
        }

        @Override
        protected void onPostExecute(String s) {
            String st[] = s.split(",");

            String wikiUrl = "";
            for (int i = 0; i < st.length; i++){
                if (st[i].contains("http")){
                    wikiUrl = st[i];
                }
            }
            if (!wikiUrl.equals("")) {
                wikiUrl = wikiUrl.substring(2, wikiUrl.length() - 3);

                String title = "";
                try {
                    wikiUrl = URLDecoder.decode(wikiUrl, "UTF-8");
                    title = wikiUrl.replace("https://vi.wikipedia.org/wiki/", "");
                    title = title.replace("https://en.wikipedia.org/wiki/", "");
                    title = title.replace("_", " ");

                    Intent intent = new Intent(BaseActivity.this, ReadWikiActivity.class);
                    intent.putExtra("wiki_link", wikiUrl);

                    Speak("Đang mở tài liệu wikipedia về " + title, "Opening wikipedia articles about " + title);
                    showToastMessage("Đang mở tài liệu wikipedia về " + title, "Opening wikipedia articles about " + title);
                    startActivity(intent);

                } catch (UnsupportedEncodingException e) {
                    showToastMessage("Đã xảy ra lỗi", "Error happened");
                    Speak("Đã xảy ra lỗi", "Error happened");
                }
            } else {
                showToastMessage("Đã xảy ra lỗi", "Error happened");
                Speak("Đã xảy ra lỗi", "Error happened");
            }
            super.onPostExecute(s);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //cameraViewFront.start();
        if (mp3ServiceBinded) {
            if (mp3Service.getCurrentStatus().equals("stand by")) this.mp3Service.resumeSong();
        }
        //stopService(new Intent(BaseActivity.this, NavigationService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent mp3Intent = new Intent(this, Mp3Service.class);
        this.bindService(mp3Intent, mp3ServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        //cameraViewFront.stop();
        if (mp3ServiceBinded) this.mp3Service.standBy();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mp3ServiceBinded) this.mp3Service.standBy();
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        if (BTconnected()){
            BTSend("f");
        }
        if (!isBluetoothEnabledPreviously){
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isEnabled()){
                bluetoothAdapter.disable();
            }
        }

        unregisterReceiver(incomingSMS);
        unregisterReceiver(smsSentListener);
        unregisterReceiver(smsDeliveredListener);

        this.unbindService(mp3ServiceConnection);

        executorTensorflow.execute(new Runnable() {
            @Override
            public void run() {
                hand_classifier.close();
            }
        });
        super.onDestroy();
    }

    public class IncomingSMS extends BroadcastReceiver {

        final SmsManager sms = SmsManager.getDefault();

        public IncomingSMS(){

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Retrieves a map of extended data from the intent.
            final Bundle bundle = intent.getExtras();

            try {
                if (bundle != null) {
                    final Object[] pdusObj = (Object[]) bundle.get("pdus");

                    for (int i = 0; i < pdusObj.length; i++) {

                        SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                        String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                        String contact = getContactName(phoneNumber, context);
                        String message = currentMessage.getDisplayMessageBody();

                        vibrator.vibrate(200);
                        ring.play();
                        if (contact != null && !contact.equals("")) {
                            Speak("Có tin nhắn từ " + contact + ": \n" + message.toLowerCase(), "New incoming message from " + contact + ": \n" + message.toLowerCase());
                            showToastMessage("Có tin nhắn từ " + contact + ": \n" + message, "New incoming message from " + contact + ": \n" + message);
                        } else {
                            Speak("Có tin nhắn từ số " + phoneNumber + ": \n" + message.toLowerCase(), "New incoming message from " + phoneNumber + ": \n" + message.toLowerCase());
                            showToastMessage("Có tin nhắn từ số " + phoneNumber + ": \n" + message, "New incoming message from " + phoneNumber + ": \n" + message);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public String getContactName(final String phoneNumber, Context context)
    {
        Uri uri=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(phoneNumber));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        String contactName = "";
        Cursor cursor = context.getContentResolver().query(uri,projection,null,null,null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                contactName=cursor.getString(0);
            }
            cursor.close();
        }

        return contactName;
    }

    private BroadcastReceiver smsSentListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = null;

            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    message = currentLanguage.equals("vi")?"Đã gửi tin nhắn cho ":"Message was sent to ";
                    String contact = getContactName(msgNumber, getApplicationContext());
                    message += contact.equals("")?msgNumber:contact;
                    message += currentLanguage.equals("vi")?" với nội dung: ": " with content: ";
                    message += msgContent;
                    msgContent = "";
                    ring.play();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    message = currentLanguage.equals("vi")?"Có lỗi: Tin nhắn chưa được gửi":"Error! Message was not sent!";
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    message = "Error: No service.";
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    message = "Error: Null PDU.";
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    message = "Error: Radio off.";
                    break;
            }

            Speak(message, message);
            showToastMessage(message, message);
        }
    };

    private BroadcastReceiver smsDeliveredListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = null;

            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    message = currentLanguage.equals("vi")?"Tin nhắn vừa gửi đã được nhận":"Sent message has been delivered!";
                    ring.play();
                    msgNumber = "";
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    message = currentLanguage.equals("vi")?"Có lỗi! Tin nhắn chưa được nhận":"Error! Sent message was not delivered!";
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    message = "Error: No service.";
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    message = "Error: Null PDU.";
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    message = "Error: Radio off.";
                    break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            Speak(message, message);
            showToastMessage(message, message);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backBtnTap++;
            if (backBtnTap >= 2) {
                Speak("Đã đóng trợ lý ảo Vias", "Vias closed");
                showToastMessage("Đã đóng trợ lý ảo VIAS", "VIAS closed");

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 700);
                //return super.onKeyDown(keyCode, event);
            } else {
                Speak("Nhấn back lần nữa để đóng trợ lý ảo Vias", "Tap again to close Vias");
                showToastMessage("Nhấn back lần nữa để đóng trợ lý ảo VIAS", "Tap again to close VIAS");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        backBtnTap = 0;
                    }
                }, 3500);
            }
        } else if (keyCode == KeyEvent.KEYCODE_POWER){
            Speak("Tắt màn hình", "Turn off screen");
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            vibrator.vibrate(200);
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            //Speak("abc", "abc");
            if (isRequesting) {
                dialog.dismiss();
                //ring.playBtnOffSound();
                vibrator.vibrate(60);
                isRequesting = false;

                recognizer.stopListening();
                btnDetectHand.setEnabled(true);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mp3ServiceBinded) {
                            String s = mp3Service.getCurrentStatus();
                            if (!s.equals("paused") && !s.equals("stopped") && !s.equals("none")) {
                                mp3Service.resumeSong();
                            }
                        };
                    }
                }, 1000);
            } else {
                headsetBtnTap++;
                if (headsetBtnTap >= 2) {
                    headsetBtnTap = 0;

                    Speak("Bạn có yêu cầu gì?", "Can I help you?");
                    vibrator.vibrate(300);
                    //Speak("Bạn có yêu cầu gì?", "Can I help you?");
                    btnDetectHand.setEnabled(false);
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            voiceRecognizing(VOICE_RECOGNITION_COMMAND_CODE);
                        }
                    }, 1000);
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
}
