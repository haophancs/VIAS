package com.haophan.vias.objectrecognizer;

import android.app.Activity;
import android.widget.Toast;

import com.haophan.vias.homecontroller.DatabaseManager;
import com.haophan.vias.homecontroller.device.Device;
import com.haophan.vias.homecontroller.requester.RequestSender;
import com.haophan.vias.homecontroller.requester.ResponseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by HaoPhan on 1/14/2018.
 */

public class ControlDeviceFromOBR {

    public final String databaseName = "HomeControllerDB.sqlite";
    public DatabaseManager databaseManager;
    private Activity activity;

    String pre_title = "";

    public ControlDeviceFromOBR(Activity activity) {

        this.activity = activity;
        databaseManager = new DatabaseManager(activity, databaseName);
    }

    public void setResult(List<Classifier.Recognition> results){

        String title = "";
        ArrayList<Device> devices = databaseManager.getAllDevices();
        Device selectedDevice = new Device();
        for (Classifier.Recognition result: results){

            if (result.getTitle().toLowerCase() != null){
                title = result.getTitle().toLowerCase();
            }
            for (Device device: devices){

                String dvName = device.getName().toLowerCase();
                if (title.contains(dvName) || dvName.contains(title)){

                    selectedDevice = device;
                }
            }
        }

        if (!pre_title.equals(title)) {

            if (selectedDevice.getName() != null && !selectedDevice.getName().equals("")) {
                String req;
                if (selectedDevice.getStatus().toLowerCase().equals("on")) {

                    req = "tắt " + selectedDevice.getName();
                } else {

                    req = "bật " + selectedDevice.getName();
                }
                new RequestSender(activity).sendRawRequest(req, new ResponseEvent() {
                    @Override
                    public void onDone(ArrayList<Device> selectedDevices) {

                        Toast.makeText(activity, "Success", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError() {

                        //Toast.makeText(activity, "Error", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            pre_title = title;
        }
    }
}
