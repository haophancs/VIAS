package com.haophan.vias.homecontroller;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.haophan.vias.homecontroller.controller.Controller;
import com.haophan.vias.homecontroller.device.Device;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by HaoPhan on 12/22/2017.
 */

public class DatabaseManager {

    //private String databaseName;
    public SQLiteDatabase hcDB;
    private Activity activity;
    private String databaseName;

    public DatabaseManager(Activity activity, String databaseName){
        this.activity = activity;
        this.databaseName = databaseName;
        hcDB = initDatabase(activity, databaseName);
    }

    public ArrayList<Controller> getAllControllers(){

        hcDB = initDatabase(activity, databaseName);

        ArrayList<Controller> controllerList = new ArrayList<>();
        Cursor cursor = hcDB.rawQuery("SELECT * FROM TB_CONTROLLER", null);
        if (cursor != null){
            int i = 0;
            cursor.moveToFirst();
            while (!cursor.isAfterLast()){

                Controller controller = new Controller(cursor.getInt(0), i, cursor.getString(2), cursor.getString(3));
                ArrayList<Device> devices = getDevicesByController(controller.getName());
                controller.setDevices(devices);
                controllerList.add(controller);

                //Toast.makeText(activity, controller.toString(), Toast.LENGTH_LONG).show();
                i++;
                cursor.moveToNext();
            }
            cursor.close();
            return controllerList;
        }
        return null;
    }

    public ArrayList<Device> getDevicesByController(String controllerName){
        ArrayList<Device> deviceList = new ArrayList<>();

        Cursor cursor = hcDB.rawQuery("SELECT * FROM TB_DEVICE WHERE CONTROLLER = '" + controllerName + "'", null);
        if (cursor != null) {
            int i = 0;
            cursor.moveToFirst();
            while (!cursor.isAfterLast()){
                Device device = new Device(cursor.getInt(0), i, cursor.getString(2), cursor.getString(3), cursor.getString(4));
                deviceList.add(device);
                i++;
                cursor.moveToNext();
            }
            cursor.close();
        } else {
            //Toast.makeText(activity.getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
            return null;
        }
        return deviceList;
    }

    public ArrayList<Device> getAllDevices(){
        ArrayList<Device> deviceList = new ArrayList<>();

        ArrayList<Controller> controllers = getAllControllers();

        for (Controller controller: controllers){

            for (Device device: controller.getDevices()){

                deviceList.add(device);
            }
        }
        return deviceList;
    }

    public void updateController(Controller controller){
        ContentValues cv = new ContentValues();
        cv.put("CONTROLLER_ID", controller.getController_id());
        cv.put("NAME", controller.getName());
        cv.put("IP_ADDR", controller.getIpAddr());

        for (Device device: controller.getDevices()){
            device.setController(controller.getName());
            updateDevice(device);
        }
        //Toast.makeText(activity, cv.toString(), Toast.LENGTH_SHORT).show();
        hcDB.update("TB_CONTROLLER", cv, "_ID="+controller.get_id(), null);
    }
    public boolean deleteController(Controller controller){
        for (Device device: controller.getDevices()){
            deleteDevice(device);
        }
        return hcDB.delete("TB_CONTROLLER", "_ID='" + controller.get_id() + "'", null) > 0;
    }
    public void addNewController(Controller controller){
        ContentValues cv = new ContentValues();
        cv.put("CONTROLLER_ID", controller.getController_id());
        cv.put("NAME", controller.getName());
        cv.put("IP_ADDR", controller.getIpAddr());

        hcDB.insert("TB_CONTROLLER", null, cv);
    }

    public void updateDevice(Device device){

        ContentValues cv = new ContentValues();
        cv.put("DEVICE_ID", device.getDeviceID());
        cv.put("NAME", device.getName());
        cv.put("CONTROLLER", device.getController());
        cv.put("STATUS", device.getStatus());

        hcDB.update("TB_DEVICE", cv, "_ID="+device.get_id(), null);
    }
    public boolean deleteDevice(Device device){
        return hcDB.delete("TB_DEVICE", "_ID=" + device.get_id(), null) > 0;
    }
    public void addNewDevice(Device device){

        ContentValues cv = new ContentValues();
        cv.put("DEVICE_ID", device.getDeviceID());
        cv.put("NAME", device.getName());
        cv.put("CONTROLLER", device.getController());
        //cv.put("STATUS", device.getStatus());

        hcDB.insert("TB_DEVICE", null, cv);
    }

    public boolean foundedDevice(Device device){
        try {
            ArrayList<Controller> controllers = getAllControllers();
            for (Controller ctrl : controllers) {
                for (Device dv : ctrl.getDevices()) {
                    if (dv.getName().equals(device.getName()))
                        return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    public Device findDeviceByName(String deviceName){
        ArrayList<Device> devices = getAllDevices();
        for (Device dv: devices){
            if (dv.getName().equals(deviceName))
                return dv;
        }
        return null;
    }

    public boolean foundedController(Controller controller){
        try {
            ArrayList<Controller> controllers = getAllControllers();
            for (Controller ctrl : controllers) {
                if (ctrl.getName().equals(controller.getName()) && ctrl.getIpAddr().equals(controller.getIpAddr()))
                    return true;
            }
        } catch (Exception e){}
        return false;
    }

    public Controller findControllerByName(String controllerName){
        ArrayList<Controller> controllers = getAllControllers();
        for (Controller ctrl: controllers){
            if (ctrl.getName().equals(controllerName))
                return ctrl;
        }
        return null;
    }

    public void deleteAllDevice(){
        hcDB.execSQL("DELETE FROM " + "TB_CONTROLLER");
        hcDB.execSQL("DELETE FROM sqlite_sequence WHERE NAME = 'TB_CONTROLLER'");
    }

    public void deleteAllController(){
        hcDB.execSQL("DELETE FROM TB_DEVICE");
        hcDB.execSQL("DELETE FROM sqlite_sequence WHERE NAME = 'TB_DEVICE'");
    }

    public static SQLiteDatabase initDatabase(Activity activity, String databaseName){
        try {
            String outFileName = activity.getApplicationInfo().dataDir + "/databases/" + databaseName;
            File f = new File(outFileName);
            if(!f.exists()) {
                InputStream e = activity.getAssets().open(databaseName);
                File folder = new File(activity.getApplicationInfo().dataDir + "/databases/");
                if (!folder.exists()) {
                    folder.mkdir();
                }
                FileOutputStream myOutput = new FileOutputStream(outFileName);
                byte[] buffer = new byte[1024];

                int length;
                while ((length = e.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }

                myOutput.flush();
                myOutput.close();
                e.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return activity.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null);
    }
}
