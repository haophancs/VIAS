package com.haophan.vias.homecontroller.controller;

import android.support.annotation.NonNull;

import com.haophan.vias.homecontroller.device.Device;

import java.util.ArrayList;

/**
 * Created by HaoPhan on 12/22/2017.
 */

public class Controller {

    private int _id;
    private int controller_id;
    private String name;
    private String ipAddr;

    private ArrayList<Device> devices;

    public Controller(int _id, int controller_id, @NonNull String name, @NonNull String ipAddr) {
        this._id = _id;
        this.controller_id = controller_id;
        this.name = name;
        this.ipAddr = ipAddr;
        devices = new ArrayList<>();
    }

    public Controller(int controller_id, @NonNull String name, @NonNull String ipAddr) {
        this.controller_id = controller_id;
        this.name = name;
        this.ipAddr = ipAddr;
        devices = new ArrayList<>();
    }

    public void setName(String name){
        this.name = name;
    }

    public void setIpAddr(String ipAddr){
        this.ipAddr = ipAddr;
    }

    public String getName() {
        return name;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setDevices(ArrayList<Device> devices){
        this.devices = devices;
    }

    public ArrayList<Device> getDevices() {
        return devices;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(_id); sb.append(" - ");
        sb.append(name); sb.append(" - ");
        sb.append(ipAddr); sb.append(" - ");
        sb.append("Size = "); sb.append(devices.size());
        sb.append("\n");

        for (int i = 0; i < devices.size(); i++){
            Device device = devices.get(i);

            sb.append(device.toString());
            if (i < devices.size() - 1) {
                sb.append("\n");
            }
            //sb.append(device.getController());
        }
        return sb.toString();
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public int getController_id() {
        return controller_id;
    }

    public void setController_id(int controller_id) {
        this.controller_id = controller_id;
    }
}
