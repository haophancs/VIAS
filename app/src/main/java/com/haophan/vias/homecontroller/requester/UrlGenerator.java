package com.haophan.vias.homecontroller.requester;

import android.app.Activity;

import com.haophan.vias.homecontroller.DatabaseManager;
import com.haophan.vias.homecontroller.controller.Controller;
import com.haophan.vias.homecontroller.device.Device;

import java.util.ArrayList;

/**
 * Created by HaoPhan on 1/5/2018.
 */

public class UrlGenerator {

    private final String databaseName = "HomeControllerDB.sqlite";
    private DatabaseManager databaseManager;
    private Activity activity;

    public UrlGenerator(Activity activity){

        databaseManager = new DatabaseManager(activity, databaseName);
        this.activity = activity;
    }

    private String getState(String s){

        s = s.toLowerCase();
        if (s.contains("mở") || s.contains("bật") || s.contains("turn on")){
            return "on";

        } else if (s.contains("đóng") || s.contains("tắt") || s.contains("turn off")) {
            return "off";
        }
        return "";
    }

    private int checkController(ArrayList<Controller> controllers, String controllerName){

        for (int i = 0; i < controllers.size(); i++){

            if (controllers.get(i).getName().equals(controllerName)){

                return i;
            }
        }
        return -1;
    }

    public ArrayList<Controller> getSelectedController(String request){

        ArrayList<Device> allDevices = databaseManager.getAllDevices();
        ArrayList<Controller> selectedControllers = new ArrayList<>();
        request = request.toLowerCase();

        if (request.contains("all") || request.contains("tất cả") || request.contains("hết")){

            selectedControllers = databaseManager.getAllControllers();
        } else {
            for (Device device : allDevices) {

                if (request.contains(device.getName().toLowerCase())) {
                    if (databaseManager.findControllerByName(device.getController()) != null) {

                        int i = checkController(selectedControllers, device.getController());
                        if (i > -1) {
                            ArrayList<Device> devices = selectedControllers.get(i).getDevices();
                            devices.add(device);
                            selectedControllers.get(i).setDevices(devices);
                        } else {
                            Controller newController = databaseManager.findControllerByName(device.getController());
                            ArrayList<Device> devices = new ArrayList<>();
                            devices.add(device);
                            newController.setDevices(devices);

                            selectedControllers.add(newController);
                        }
                    }
                }
            }
        }
        return selectedControllers;
    }

    /* Hàm tạo các url gửi lệnh bật/tắt thiết bị điện cho bộ điều khiển
    thiết bị điện VIAS dựa trên yêu cầu bằng tiếng nói của người dùng */
    // do lệnh
    public ArrayList<String> getUrls(String request){
        //khởi tạo danh sách url
        ArrayList<String> urls = new ArrayList<>();
        String state = getState(request); //phân tích yêu cầu người dùng(muốn bật hay tắt)
        //tìm tên các bộ điều khiển chứa các thiết bị cần bật/tắt theo yêu cầu
        ArrayList<Controller> selectedControllers = getSelectedController(request);
        for (int i = 0; i < selectedControllers.size(); i++){
        //phân tích các thiết bị điện cần bật tắt và tạo tên mã(ví dụ: d0, d1, d2,...)
            String dvReq = "";
            for (Device dv: selectedControllers.get(i).getDevices()){
                dvReq += "d" + dv.getDeviceID();
            }
            //tạo chuỗi url
            String url = "http://" + selectedControllers.get(i).getIpAddr() + "/commandArgs?device=" + dvReq + "&state=" + state + "&code=ph77894456";
            urls.add(url); //đưa chuỗi url vào danh sách
        }
        return urls; //trả về danh sách url
    }
}
