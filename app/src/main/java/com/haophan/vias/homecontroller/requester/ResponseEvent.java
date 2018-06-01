package com.haophan.vias.homecontroller.requester;

import com.haophan.vias.homecontroller.device.Device;

import java.util.ArrayList;

/**
 * Created by HaoPhan on 1/5/2018.
 */

public interface ResponseEvent {

    void onDone(ArrayList<Device> selectedDevices);
    void onError();
}
