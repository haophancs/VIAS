package com.haophan.vias.homecontroller.device;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.haophan.vias.R;
import com.haophan.vias.tools.GetSharePrefs;

import java.util.ArrayList;

/**
 * Created by HaoPhan on 12/22/2017.
 */

public class DeviceAdapter extends ArrayAdapter<Device> {

    private Context context;
    private ArrayList<Device> controllerList;
    public Resources res;
    private LayoutInflater inflater;

    public DeviceAdapter(Context context, ArrayList<Device> objects){

        super(context, R.layout.device_listview_row, objects);

        this.context = context;
        controllerList = objects;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    // This funtion called for each row ( Called data.size() times )
    public View getCustomView(int position, View convertView, ViewGroup parent) {

        View row = inflater.inflate(R.layout.device_listview_row, parent, false);
        TextView deviceIDView = (TextView) row.findViewById(R.id.deviceIDView);
        TextView deviceNameView = (TextView) row.findViewById(R.id.deviceNameView);
        TextView deviceSttView = (TextView) row.findViewById(R.id.deviceSttView);

        deviceNameView.setText(controllerList.get(position).getName());

        String stt = controllerList.get(position).getStatus();
        if (stt != null) {
            deviceSttView.setText(stt);
            if (stt.equals("ON") || (stt.equals("OPEN"))) {
                deviceSttView.setTextColor(context.getResources().getColor(R.color.colorGreen));
            } else {
                deviceSttView.setTextColor(context.getResources().getColor(R.color.colorRed));
            }
        }

        String currentLang = new GetSharePrefs(context).getCurrentLang();
        int i = controllerList.get(position).getDeviceID() + 1;
        String id = currentLang.equals("vi")?"Thiết bị ":"Device ";
        id = id + i + ": ";
        deviceIDView.setText(id);

        return row;
    }
}
