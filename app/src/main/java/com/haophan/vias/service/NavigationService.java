package com.haophan.vias.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.haophan.vias.BaseActivity;
import com.haophan.vias.R;

import java.util.HashMap;
import java.util.Locale;

public class NavigationService extends Service implements SensorEventListener {

    double North = 0;                   double South =  180;
    double Northeast = 45;              double Southwest = 225;
    double East = 90;                   double West = 270;
    double Southeast = 135;             double Northwest = 315;

    double offset = 2.0;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    boolean compassOn = false;

    private TextToSpeech tts;
    private Vibrator vibrator;

    String pre_direction = "";
    String direction = "";
    String en_direction = "";

    private WindowManager mWindowManager;
    private View mFloatingButtonView;

    private String currentLang;

    public NavigationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Inflate the chat head layout we created
        mFloatingButtonView = LayoutInflater.from(this).inflate(R.layout.floating_button, null);

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the chat head position
        params.gravity = Gravity.CENTER;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingButtonView, params);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);

        currentLang = getCurrentLang();

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (currentLang.equals("vi")) {
                    tts.setLanguage(new Locale("vi"));
                    tts.setSpeechRate(1.30f);
                }
                else {
                    tts.setLanguage(Locale.US);
                }
            }
        });
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //Drag and move chat head using user's touch action.
        final ImageView chatHeadImage = (ImageView) mFloatingButtonView.findViewById(R.id.floating_image_button);

        chatHeadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compassOn = !compassOn;
            }
        });

        chatHeadImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(NavigationService.this, BaseActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("message", "return from navigator");
                startActivity(intent);
                stopSelf();
                compassOn = false;
                return false;
            }
        });
    }

    private void directionSpeak(){
        HashMap<String, String> myHashAudio = new HashMap<String, String>();
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
        if (!tts.isSpeaking()) {
            if (compassOn) {
                if (!direction.equals(pre_direction)) {
                    if (!direction.equals("")) {
                        vibrator.vibrate(400);
                        if (currentLang.equals("vi")) {
                            tts.speak("đây là hướng " + direction, TextToSpeech.QUEUE_FLUSH, myHashAudio);
                        } else {
                            tts.speak("this is " + en_direction, TextToSpeech.QUEUE_FLUSH, myHashAudio);
                        }
                    }
                }
            }
        }
    }
    private String getCurrentLang(){
        String appPreference = "app_pref";

        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;

            if ((azimuthInDegress >= North + 360 - offset && azimuthInDegress <= North + 360)
                    || (azimuthInDegress >= North && azimuthInDegress <= North + offset)){
                pre_direction = direction;
                direction = "bắc";
                en_direction = "the north";
            }
            else if (azimuthInDegress >= South - offset && azimuthInDegress <= South + offset){
                pre_direction = direction;
                direction = "nam";
                en_direction = "the south";
            }
            else if (azimuthInDegress >= East - offset && azimuthInDegress <= East + offset){
                pre_direction = direction;
                direction = "đông";
                en_direction = "the east";
            }
            else if (azimuthInDegress >= West - offset && azimuthInDegress <= West + offset){
                pre_direction = direction;
                direction = "tây";
                en_direction = "the west";
            }
            else if (azimuthInDegress >= Northeast - offset && azimuthInDegress <= Northeast + offset){
                pre_direction = direction;
                direction = "đông bắc";
                en_direction = "the northeast";
            }
            else if (azimuthInDegress >= Southeast - offset && azimuthInDegress <= Southeast + offset){
                pre_direction = direction;
                direction = "đông nam";
                en_direction = "the southeast";
            }
            else if (azimuthInDegress >= Northwest - offset && azimuthInDegress <= Northwest + offset){
                pre_direction = direction;
                direction = "tây bắc";
                en_direction = "the northwest";
            }
            else if (azimuthInDegress >= Southwest - offset && azimuthInDegress <= Southwest + offset){
                pre_direction = direction;
                direction = " tây nam";
                en_direction = "the southwest";
            }
            directionSpeak();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);

        if (mFloatingButtonView != null) mWindowManager.removeView(mFloatingButtonView);
    }
}
