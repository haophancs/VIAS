package com.haophan.vias.tools;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by HaoPhan on 11/24/2017.
 */

public class GetSharePrefs {
    Context context;

    public GetSharePrefs(Context ctx){
        this.context = ctx;
    }

    public String getCurrentLang(){
        String appPreference = "app_pref";

        SharedPreferences sharedPreferences = context.getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    public String getNodeMcuIP(){
        String appPreference = "app_pref";

        SharedPreferences sharedPreferences = context.getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("IPKey", "No IP address defined");
    }

    public String getWifiName(){
        String appPreference = "app_pref";

        SharedPreferences sharedPreferences = context.getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("wifiNameKey", "No wifi defined");
    }
}
