package com.haophan.vias;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.haophan.vias.homecontroller.HomeControllerModifyActivity;

import java.util.Locale;

public class OptionsActivity extends AppCompatActivity {

    String currentLang = "";
    String appPreference = "app_pref";
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;

    ListView optionsListView;

    int currentLangSelection;
    String currentWiFiChoice = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_options);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_back));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToastMessage("Thoát tùy chọn, trở lại màn hình chính", "Close options, return to main screen");
                finishAndRemoveTask();
            }
        });

        sharedpreferences = getSharedPreferences(appPreference, Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();

        currentLang = getCurrentLang();
        currentLangSelection = currentLang.equals("vi")?1:0;

        currentWiFiChoice = getWifiName();

        final String optionItems[] = currentLang.equals("vi")?getResources().getStringArray(R.array.vie_options_array):getResources().getStringArray(R.array.eng_options_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.custom_listview_item, optionItems);

        optionsListView = (ListView) findViewById(R.id.option_list);
        optionsListView.setVisibility(View.VISIBLE);
        optionsListView.setAdapter(adapter);
        optionsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        optionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {

                if (id == 0) {
                    setLanguageOption();
                } else if (id == 1){
                    setWifiNameOption();
                } else if (id == 2){
                    startActivity(new Intent(OptionsActivity.this, HomeControllerModifyActivity.class));
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.apply_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.apply_option){
            editor.apply();
            try {
                Thread.sleep(500);
            }catch (Exception e){
            }

            showToastMessage("Áp dụng thay đổi tùy chọn, khởi động lại ứng dụng",
                    "Apply options and reset the application");

            Intent mStartActivity = new Intent(getApplicationContext(), BaseActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        }
        return super.onOptionsItemSelected(item);
    }

    private String getWifiName(){
        String appPreference = "app_pref";

        SharedPreferences sharedPreferences = getSharedPreferences(appPreference, MODE_PRIVATE);
        return sharedPreferences.getString("wifiNameKey", "...");
    }

    private String getCurrentLang(){
        return sharedpreferences.getString("languageKey", Locale.getDefault().getLanguage());
    }

    private void setLanguageOption(){
        String s_v[] = {"Tiếng Anh", "Tiếng Việt"};
        String s_e[] = {"English", "Vietnamese"};
        String s[] = currentLang.equals("vi")? s_v: s_e;

        final int[] selectedItem = new int[1];

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(currentLang.equals("vi")? "Chọn ngôn ngữ:": "Choose the language:")
                .setCancelable(true)
                .setSingleChoiceItems(s, currentLangSelection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        selectedItem[0] = which;
                        //showToastMessage(getCurrentLang(), getCurrentLang());
                    }
                })
                .setCancelable(true)
                .setNegativeButton(currentLang.equals("vi") ? "Hủy" : "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (selectedItem[0] == 0) {
                            editor.putString("languageKey", "en");
                            currentLangSelection = 0;
                        } else if (selectedItem[0] == 1) {
                            editor.putString("languageKey", "vi");
                            currentLangSelection = 1;
                        }
                        dialog.dismiss();
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void setWifiNameOption(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.wifi_name_option, null);
        builder.setView(dialogView);

        final EditText edt = (EditText) dialogView.findViewById(R.id.editWifi);
        edt.setText(currentWiFiChoice);
        edt.selectAll();

        builder.setTitle(currentLang.equals("vi")?"Nhập tên mạng WiFi:":"Enter WiFi SSID:");
        builder.setMessage(currentLang.equals("vi")?"(Tên Wifi mà bộ điều khiển thiết bị điện VIAS kết nối)": "Wifi SSID which VIAS Home-Controller connected");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String wifiName = edt.getText().toString();
                currentWiFiChoice = wifiName;
                editor.putString("wifiNameKey", wifiName);

                //showToastMessage(wifiName, wifiName);
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(currentLang.equals("vi") ? "WiFi đang kết nối" : "Currently connected WiFI", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String SSID = wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length() - 1);
                currentWiFiChoice = SSID;
                editor.putString("wifiNameKey", SSID);
            }
        });
        builder.setNegativeButton(currentLang.equals("vi")? "Hủy":"Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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
}
