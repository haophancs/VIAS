package com.haophan.vias.tools;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * Created by USER on 10/7/2017.
 */

public class RingPlayer {

    Context context;

    public RingPlayer(Context context){
        this.context = context;
    }

    public void play(){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, notification);

            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
