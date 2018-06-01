/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haophan.vias.objectrecognizer;

import android.Manifest;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Image.Plane;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.haophan.vias.R;
import com.haophan.vias.objectrecognizer.env.Logger;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;

public abstract class CameraActivity extends AppCompatActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;

    private boolean debug = false;

    private Handler handler;
    private HandlerThread handlerThread;

    protected TextToSpeech tts;
    protected String currentLang;

    int backBtnTap = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        currentLang = getCurrentLang();
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    if (currentLang.equals("vi")){
                        tts.setLanguage(new Locale("vi"));
                        tts.setSpeechRate(1.38f);

                    } else {
                        tts.setLanguage(Locale.US);
                    }
                    showToastMessage("Chế độ nhận dạng vật thể đã sẵn sàng.",
                            "Object recognizer has been ready.");
                    Speak("chế độ nhận dạng vật thể đã sẵn sàng.",
                            "Object recognizer has been ready.");
                }
            }
        });

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }
    }

    private String getCurrentLang(){
        String appPreference = "app_pref";

        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        if (!isFinishing()) {
            LOGGER.d("Requesting finish");
            finish();
        }

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setFragment();
                } else {
                    requestPermission();
                }
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(CameraActivity.this, "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    protected void setFragment() {
        final Fragment fragment =
                CameraConnectionFragment.newInstance(
                        new CameraConnectionFragment.ConnectionCallback() {
                            @Override
                            public void onPreviewSizeChosen(final Size size, final int rotation) {
                                CameraActivity.this.onPreviewSizeChosen(size, rotation);
                            }
                        },
                        this,
                        getLayoutId(),
                        getDesiredPreviewFrameSize());

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void requestRender() {
        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.postInvalidate();
        }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {
        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.addCallback(callback);
        }
    }

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
    protected abstract int getLayoutId();
    protected abstract Size getDesiredPreviewFrameSize();

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if ((keyCode == KeyEvent.KEYCODE_BACK)){
            tts.stop();
            backBtnTap++;

            if (backBtnTap >= 2) {
                Speak("thoát nhận dạng vật thể, trở lại màn hình chính", "exit object recognizer and return to main screen");
                showToastMessage("Thoát nhận dạng vật thể, trở lại màn hình chính", "Exit object recognizer and return to main screen");
                finishAndRemoveTask();
            } else {
                showToastMessage("Nhấn back lần nữa để thoát nhận dạng vật thể", "Tap again to exit object recognizer ");
                Speak("Nhấn back lần nữa để thoát nhận dạng vật thể", "Tap again to exit object recognizer");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        backBtnTap = 0;
                    }
                }, 3500);
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
        myHashAudio.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));

        switch (currentLang){
            case "vi":
                tts.speak(viText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
                break;
            default:
                tts.speak(engText, TextToSpeech.QUEUE_FLUSH, myHashAudio);
        }
    }
}
