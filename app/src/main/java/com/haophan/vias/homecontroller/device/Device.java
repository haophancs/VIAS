package com.haophan.vias.homecontroller.device;

/**
 * Created by HaoPhan on 12/22/2017.
 */

public class Device {

    private int _id;
    private int deviceID;
    private String name;
    private String controller;
    private String status;

    public Device(){}

    public Device(int id, int device_id, String name, String controller, String status) {
        _id = id;
        this.deviceID = device_id;
        this.name = name;
        this.controller = controller;
        this.status = status;
    }

    public Device(int device_id, String name, String controller) {
        this.deviceID = device_id;
        this.name = name;
        this.controller = controller;
    }

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(get_id());
        sb.append(" - ");
        sb.append(getDeviceID());
        sb.append(" - ");
        sb.append(getName());
        sb.append(" - ");
        sb.append(getController());
        sb.append(" - ");
        sb.append(getStatus());
        return sb.toString();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }
}
