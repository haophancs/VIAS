package com.haophan.vias.homecontroller.controller;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.haophan.vias.R;

import java.util.ArrayList;

/**
 * Created by HaoPhan on 12/22/2017.
 */

public class ControllerAdapter extends ArrayAdapter<Controller> {

    private Context context;
    private ArrayList<Controller> controllerList;
    public Resources res;
    private LayoutInflater inflater;

    public ControllerAdapter(Context context, ArrayList<Controller> objects) {
        super(context, R.layout.controller_spinner_row, objects);

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

        View row = inflater.inflate(R.layout.controller_spinner_row, parent, false);
        TextView controllerNameView = (TextView) row.findViewById(R.id.controllerNameView);
        TextView controllerIPView = (TextView) row.findViewById(R.id.controllerIPView);

        controllerNameView.setText(controllerList.get(position).getName());

        String ip = "IP: " + controllerList.get(position).getIpAddr();
        controllerIPView.setText(ip);

        return row;
    }
}
