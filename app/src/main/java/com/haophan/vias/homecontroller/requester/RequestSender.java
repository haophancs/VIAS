package com.haophan.vias.homecontroller.requester;

import android.app.Activity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.haophan.vias.homecontroller.DatabaseManager;
import com.haophan.vias.homecontroller.controller.Controller;
import com.haophan.vias.homecontroller.device.Device;

import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * Created by HaoPhan on 1/5/2018.
 */

public class RequestSender {

    private final String databaseName = "HomeControllerDB.sqlite";
    private DatabaseManager databaseManager;
    private RequestQueue queue;
    private Activity activity;
    private ArrayList<Device> commandDevice = null;

    public RequestSender(Activity activity){

        databaseManager = new DatabaseManager(activity, databaseName);
        queue = Volley.newRequestQueue(activity);
        this.activity = activity;
    }

    public boolean sendRawRequest(String request, ResponseEvent responseEvent){

        UrlGenerator urlGenerator = new UrlGenerator(activity);
        ArrayList<String> urls = urlGenerator.getUrls(request);
        ArrayList<Controller> selectedControllers = urlGenerator.getSelectedController(request);

        if (!(selectedControllers.size() > 0)) {

            return false;
        }

        commandDevice = new ArrayList<>();

        for (int i = 0; i < selectedControllers.size(); i++){

            for (Device device: selectedControllers.get(i).getDevices()){
                commandDevice.add(device);
            }
            String ctrlName = selectedControllers.get(i).getName();
            selectedControllers.set(i, databaseManager.findControllerByName(ctrlName));
        }

        for (int i = 0; i < urls.size(); i++){
            sendRequest(urls.get(i), selectedControllers.get(i), responseEvent);
        }
        return true;
    }

    public void sendRequest(String url, final Controller selectedController, final ResponseEvent responseEvent){

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {

                        StringTokenizer tokenizer = new StringTokenizer(s, ";");
                        while (tokenizer.hasMoreTokens()) {
                            String status = tokenizer.nextToken();
                            if (status != null && status.length() > 5) {
                                int i = Integer.parseInt(status.replaceAll("[^\\d]", ""));
                                if (i >= 0 && i < selectedController.getDevices().size()) {
                                    Device device = selectedController.getDevices().get(i);
                                    if (status.contains("ON")) {
                                        device.setStatus("ON");
                                    } else if (status.contains("OFF")) {
                                        device.setStatus("OFF");
                                    }
                                    if (commandDevice != null) {
                                        for (Device dv : commandDevice) {
                                            if (dv.getName().equals(device.getName())) {
                                                dv.setStatus(device.getStatus());
                                            }
                                        }
                                    }
                                    databaseManager.updateDevice(device);
                                }
                            }
                        }
                        if (commandDevice != null) {
                            responseEvent.onDone(commandDevice);
                        } else {
                            responseEvent.onDone(selectedController.getDevices());
                        }
                        commandDevice = null;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                commandDevice = null;
                responseEvent.onError();
            }
        });
        /*stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                500,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));*/
        queue.add(stringRequest);
    }
}
