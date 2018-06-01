package com.haophan.vias.service;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.haophan.vias.R;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class Mp3Service extends Service {

    private String currentLang = "";
    private TextToSpeech tts;
    private ContentResolver musicResolver;
    private Uri musicUri;
    private Cursor musicCursor;
    private class Song {
        private long id;
        private String title;
        private String artist;

        public Song(long songID, String songTitle, String songArtist) {
            id = songID;
            title = songTitle;
            artist = songArtist;
        }

        public long getID() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }
    }
    public Song selectedSong;
    private ArrayList<Song> songList = new ArrayList<Song>();
    private int currrentPos = 0;
    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private boolean isLoop = true;
    private boolean isRandom = false;

    private String currentStatus = "none";
    private long maxID = 0;

    public Mp3Service(){

    }

    private String getCurrentLang(){
        String appPreference = "app_pref";

        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    private final IBinder binder = new Mp3Binder();
    public class Mp3Binder extends Binder{

        public Mp3Service getService() {
            return Mp3Service.this;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        currentLang = getCurrentLang();
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (currentLang.equals("vi")){
                    tts.setLanguage(new Locale("vi"));
                    tts.setSpeechRate(1.31f);
                } else {
                    tts.setLanguage(Locale.US);
                }
            }
        });
        querySongList();
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return this.binder;
    }
    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }
    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void restartSong(){
        //this.isLoop = loop;
        //this.isRandom = !loop;

        if (!isLoop) {
            isLoop = true;
            replaySong();
            //Speak("Phát lại nhạc", "Restart song");
            isLoop = false;
        } else {
            replaySong();
            //Speak("Phát lại nhạc", "Restart song");
        }
        showToastMessage("Phát lại nhạc", "Restart song");
    }
    public void setRandom(boolean random){
        this.isRandom = random;
        this.isLoop = !random;
        if (isRandom) {
            replaySong();
            //Speak("Phát nhạc ngẫu nhiên", "Enable playing song randomly");
            showToastMessage("Phát nhạc ngẫu nhiên", "Enable playing song randomly");
        } else {
            //Speak("Tắt chế độ phát nhạc ngẫu nhiên", "Disable playing song randomly");
            showToastMessage("Tắt chế độ phát nhạc ngẫu nhiên", "Disable playing song randomly");
        }
    }
    public String getCurrentStatus(){
        return currentStatus;
    }
    public void setCurrentStatus(String s){
        currentStatus = s;
    }

    public void playSong(boolean isReplay){

        //stopSong();

            Uri contentUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, selectedSong.getID());

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    replaySong();
                }
            });

            try {
                mMediaPlayer.setDataSource(this, contentUri);
                mMediaPlayer.prepare();
                mMediaPlayer.start();

                String songName = selectedSong.getTitle();
                String artistName = selectedSong.getArtist();
                if (!isReplay) {
                    Speak("Bắt đầu phát bài \"" + songName + "\" - Nghệ sĩ: " + artistName,
                            "Start playing \"" + songName + "\" - Artist: " + artistName);
                    showToastMessage("Bắt đầu phát bài \"" + songName + "\" - Nghệ sĩ: " + artistName,
                            "Start playing \"" + songName + "\" - Artist: " + artistName);
                } else {
                    Speak("Phát lại bài \"" + songName + "\"",
                            "Replay \"" + songName + "\"");
                    showToastMessage("Phát lại bài \"" + songName + "\"",
                            "Replay \"" + songName + "\"");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        currentStatus = "playing";
    }

    private void replaySong(){
            if (isLoop) {
                playSong(true);
            } else if (isRandom) {
                Random random = new Random();
                int id = random.nextInt(songList.size());
                if (selectedSong != null) {
                    if (id == selectedSong.getID()) {
                        id++;
                        if (id >= songList.size() - 1) {
                            id = 0;
                        }
                    }
                }
                selectedSong = songList.get(id);
                playSong(false);
            }
    }

    private void searchSongWithID(long id){
        for (Song song : songList) {

            if (song.getID() == id){
                selectedSong = song;
                break;
            }
        }
            playSong(false);
    }

    public void searchAndPlaySong(String songRequest) {

        Song firstSelectedSong = null;
        Song finalSelectedSong = null;

        String request = songRequest.toLowerCase();
        if (request.equals("trước")){
            if (selectedSong != null) {
                try {
                    searchSongWithID(selectedSong.getID() - 1);
                } catch (Exception e){
                    searchSongWithID(maxID);
                }
            }

        } else if (request.equals("sau")) {

            if (selectedSong != null) {
                try {
                    searchSongWithID(selectedSong.getID() + 1);
                } catch (Exception e){
                    searchSongWithID(0);
                }
            }

        } else {
            request = convertedString(request);

            for (Song song : songList) {

                String title = song.getTitle().toLowerCase();
                String artist = song.getArtist().toLowerCase();
                    title = convertedString(title);
                    if (request.contains(title)) {
                        firstSelectedSong = song;
                        if (request.contains(artist)) {
                            finalSelectedSong = song;
                        }
                    }
                    if (finalSelectedSong != null) {
                        selectedSong = finalSelectedSong;
                    } else {
                        selectedSong = firstSelectedSong;
                    }
            }if (selectedSong != null) {
                playSong(false);
            } else {
                Speak("Không có bài hát \"" + songRequest + "\"",
                        "No song called \"" + songRequest + "\"");
                showToastMessage("Không có bài hát \"" + songRequest + "\"",
                        "No song called \"" + songRequest + "\"");
            }
        }
    }

    public void standBy(){
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (!audioManager.isWiredHeadsetOn()) {

            if (!currentStatus.equals("paused") && !currentStatus.equals("stopped") && !currentStatus.equals("none")) {
                pauseSong();
                currentStatus = "stand by";
            }
        }
    }

    public void pauseSong(){
        if (selectedSong != null) {
            if (currentStatus.equals("playing") || currentStatus.equals("stand by")) {
                Speak("Tạm dừng nhạc", "Pause song");
                showToastMessage("Tạm dừng nhạc", "Pause song");

                currentStatus = "paused";
                mMediaPlayer.pause();
                currrentPos = mMediaPlayer.getCurrentPosition();
            }
        }
    }

    public void stopSong(){
        if (selectedSong != null) {
            if (currentStatus.equals("playing") || currentStatus.equals("stand by")) {
                Speak("Dừng phát nhạc", "Stop song");
                showToastMessage("Dừng phát nhạc", "Stop song");

                currentStatus = "stopped";
                mMediaPlayer.stop();
                currrentPos = 0;
                //selectedSong = null;
            }
        }
    }

    public void resumeSong(){

            if (selectedSong != null) {
                if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.seekTo(currrentPos);

                    mMediaPlayer.start();
                    currentStatus = "playing";

                    Speak("Tiếp tục phát nhạc", "Resume song");
                    //showToastMessage("Tiếp tục phát nhạc", "Resume song");
                }
            }
    }

    private void querySongList() {
        musicResolver = getContentResolver();
        musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                if (!thisTitle.toLowerCase().contains("hangouts")) {
                    songList.add(new Song(thisId, thisTitle, thisArtist));
                    if (thisId > maxID){
                        maxID = thisId;
                    }
                }
            }
            while (musicCursor.moveToNext());
        }
    }
    private String convertedString(String str) {
        try {
            String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
            Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
            return pattern.matcher(temp).replaceAll("").toLowerCase().replaceAll("đ", "d");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    private void Speak(String viText, String enText) {
        tts.stop();

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
}
