package com.haophan.vias.tools;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by USER on 9/8/2017.
 */

public class CurrentDateTime {

    private String appPreference = "app_pref";
    String currentLang = "";
    Context context;

    public CurrentDateTime(Context context){
        this.context = context;
        currentLang = getCurrentLang();
    }

    public String currentTime(){
        Calendar rightNow = Calendar.getInstance();
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
        int currentMinute = rightNow.get(Calendar.MINUTE);
        switch (currentLang){
            case "vi":
                return "Bây giờ là " + currentHour + " giờ : "
                        + currentMinute + " phút";

            default:
                return "It is " + currentHour + ": "
                        + currentMinute;
        }
    }

    private String getCurrentLang(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    public String currentDate() {
        Calendar today = Calendar.getInstance();

        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        String DayOfWeek = "";
        if (dayOfWeek == 1) {
            DayOfWeek = "chủ nhật";
        } else {
            DayOfWeek = "thứ " + dayOfWeek;
        }
        String engDayOfWeek = "";
        if (dayOfWeek == 2){
            engDayOfWeek = "Monday";
        } else if (dayOfWeek == 3){
            engDayOfWeek = "Tuesday";
        } else if (dayOfWeek == 4){
            engDayOfWeek = "Wednesday";
        } else if (dayOfWeek == 5){
            engDayOfWeek = "Thursday";
        } else if (dayOfWeek == 6){
            engDayOfWeek = "Friday";
        } else if (dayOfWeek == 7){
            engDayOfWeek = "Saturday";
        } else if (dayOfWeek == 1){
            engDayOfWeek = "Sunday";
        }

        int dayOfMonth = today.get(Calendar.DAY_OF_MONTH);

        int currentMonth = today.get(Calendar.MONTH) + 1;
        String engCurrentMonth = "";
        if (currentMonth == 1) {
            engCurrentMonth = "January";
        } else if (currentMonth == 2) {
            engCurrentMonth = "February";
        } else if (currentMonth == 3) {
            engCurrentMonth = "March";
        } else if (currentMonth == 4) {
            engCurrentMonth = "April";
        } else if (currentMonth == 5) {
            engCurrentMonth = "May";
        } else if (currentMonth == 6) {
            engCurrentMonth = "June";
        } else if (currentMonth == 7) {
            engCurrentMonth = "July";
        } else if (currentMonth == 8) {
            engCurrentMonth = "August";
        } else if (currentMonth == 9) {
            engCurrentMonth = "September";
        } else if (currentMonth == 10) {
            engCurrentMonth = "October";
        } else if (currentMonth == 11) {
            engCurrentMonth = "November";
        } else if (currentMonth == 12) {
            engCurrentMonth = "December";
        }
        int currentYear = today.get(Calendar.YEAR);

        switch (currentLang) {
            case "vi":
                return "Hôm nay là " + DayOfWeek +
                        ", ngày " + dayOfMonth +
                        ", tháng " + currentMonth +
                        ", năm " + currentYear;

            default:
                return "Today is " + engDayOfWeek +
                        ". " + engCurrentMonth + " "+  dayOfMonth + "th" +
                        ". " + currentYear;
        }
    }
}
