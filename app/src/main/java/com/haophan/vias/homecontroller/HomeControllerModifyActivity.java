package com.haophan.vias.homecontroller;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.haophan.vias.R;
import com.haophan.vias.homecontroller.controller.Controller;
import com.haophan.vias.homecontroller.controller.ControllerAdapter;
import com.haophan.vias.homecontroller.device.Device;
import com.haophan.vias.homecontroller.device.DeviceAdapter;
import com.haophan.vias.homecontroller.requester.RequestSender;
import com.haophan.vias.homecontroller.requester.ResponseEvent;
import com.haophan.vias.tools.GetSharePrefs;

import java.util.ArrayList;

public class HomeControllerModifyActivity extends AppCompatActivity {

    public final String databaseName = "HomeControllerDB.sqlite";
    public DatabaseManager databaseManager;

    private ArrayList<Controller> controllers;
    private Controller selectedController;
    private int selectedId = 0;

    public Spinner controllerSpinner;
    public ListView deviceListView;
    public Button newDeviceBtn;

    private String currentLang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_controller_modify);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_back));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAndRemoveTask();
            }
        });

        currentLang = new GetSharePrefs(this).getCurrentLang();

        controllerSpinner = (Spinner) findViewById(R.id.controller_spinner);
        deviceListView = (ListView) findViewById(R.id.deviceListView);
        newDeviceBtn = (Button) findViewById(R.id.button_new_device);
        TextView tv = (TextView) findViewById(R.id.tvSelectCtrl);
        tv.setText(currentLang.equals("vi")?"Chọn bộ điều khiển":"Select Controller");

        databaseManager = new DatabaseManager(this, databaseName);
        updateDataFromDB();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(HomeControllerModifyActivity.this, R.style.MyAlertDialogStyle);
                builder.setTitle(currentLang.equals("vi")?"Bạn có chắc không?":"Are you sure?");
                builder.setMessage(currentLang.equals("vi")?"Dữ liệu về các bộ điều khiển và thiết bị điện sẽ bị xóa":"All data of controllers and devices will be wiped out");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseManager.deleteAllDevice();
                        databaseManager.deleteAllController();
                        updateDataFromDB();
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                ArrayList<Controller> controllers = databaseManager.getAllControllers();
                for (Controller ctrl : controllers){
                    Toast.makeText(HomeControllerModifyActivity.this, ctrl.toString(), Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });

        newDeviceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedController != null) {
                    showDeviceDialog(true, 0);

                } else {
                    showControllerDialog(true);
                }
            }
        });
    }

    public void updateListView() {

        if (selectedController != null) {
            deviceListView.setVisibility(View.VISIBLE);
            try {
                if (selectedController.getDevices().size() > 0)
                    newDeviceBtn.setVisibility(View.GONE);
                else {
                    newDeviceBtn.setVisibility(View.VISIBLE);
                    newDeviceBtn.setText(currentLang.equals("vi")?"Thêm thiết bị điện":"Add new device");
                }
            } catch (Exception e){
            }

            DeviceAdapter adapter = new DeviceAdapter(getApplicationContext(), selectedController.getDevices());
            deviceListView.setAdapter(adapter);

            deviceListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                    Device device = databaseManager.findDeviceByName(selectedController.getDevices().get(position).getName());
                    String req;
                    if (device.getStatus() == null || !device.getStatus().equals("ON")) {
                        req = "bật " + device.getName();
                    } else {
                        req = "tắt " + device.getName();
                    }
                    new RequestSender(HomeControllerModifyActivity.this).sendRawRequest(req, new ResponseEvent() {
                        @Override
                        public void onDone(ArrayList<Device> selectedDevices) {

                            updateDataFromDB();
                        }

                        @Override
                        public void onError() {

                            Toast.makeText(HomeControllerModifyActivity.this, "Error!", Toast.LENGTH_SHORT).show();
                        }
                    });

                    return false;
                }
            });

            deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                    final Device device = selectedController.getDevices().get(position);

                    PopupMenu popup = new PopupMenu(HomeControllerModifyActivity.this, view);
                    popup.getMenuInflater().inflate(currentLang.equals("vi")?R.menu.vi_menu_device_action:R.menu.menu_device_action, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {

                            if (item.getItemId() == R.id.modify_device) {
                                try {
                                    showDeviceDialog(false, position);
                                } catch (Exception e){
                                }
                            } else if (item.getItemId() == R.id.delete_device) {
                                showDeleteDeviceDialog(device);

                            } else if (item.getItemId() == R.id.add_new_device) {

                                showDeviceDialog(true, position);
                            }
                            return true;
                        }
                    });
                    popup.show();

                }
            });

        } else {
            deviceListView.setVisibility(View.INVISIBLE);
            newDeviceBtn.setVisibility(View.VISIBLE);
            newDeviceBtn.setText(currentLang.equals("vi")?"Thêm bộ điều khiển":"Add new controller");
        }
    }

    public void addItemToSpinner(){

        final ControllerAdapter spinAdapter = new ControllerAdapter(getApplicationContext(), controllers);
        controllerSpinner.setAdapter(spinAdapter);
        try {
            controllerSpinner.setSelection(selectedController.getController_id());
        } catch (Exception e){
            controllerSpinner.setSelection(0);
        }
        controllerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                selectedController = controllers.get(position);
                selectedId = position;
                updateListView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        controllerSpinner.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                final PopupMenu popup = new PopupMenu(HomeControllerModifyActivity.this, v);
                if (selectedController != null) {
                    popup.getMenuInflater().inflate(currentLang.equals("vi")?R.menu.vi_menu_controller_action:R.menu.menu_controller_action, popup.getMenu());
                } else {
                    popup.getMenuInflater().inflate(currentLang.equals("vi")?R.menu.vi_menu_add_controller:R.menu.menu_add_controller, popup.getMenu());
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        if (selectedController != null) {
                            if (item.getItemId() == R.id.modify_controller) {

                                try {
                                    showControllerDialog(false);
                                    updateDataFromDB();
                                } catch (Exception e){
                                }

                            } else if (item.getItemId() == R.id.delete_controller) {
                                showDeleteControllerDialog();

                            } else if (item.getItemId() == R.id.add_new_controller) {
                                showControllerDialog(true);
                            }
                        } else {
                            showControllerDialog(true);
                        }
                        return true;
                    }
                });
                popup.show();
                return true;
            }
        });
    }

    public void updateDataFromDB(){

        controllers = null;
        controllers = databaseManager.getAllControllers();

        if (controllers != null && controllers.size() > 0) {
            if (selectedId >= 0) {
                selectedController = controllers.get(selectedId);
            } else {
                selectedController = null;
            }
        } else {
            selectedController = null;
        }

        addItemToSpinner();
        updateListView();
    }


    private void showControllerDialog(boolean createNew){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        LayoutInflater inflater = this.getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.dialog_add_new_controller, null);
        builder.setView(dialogView);

        final TextView nameView = (TextView) dialogView.findViewById(R.id.add_controller_name_view);
        nameView.setText("Controller name: ");
        final EditText editName = (EditText) dialogView.findViewById(R.id.add_controller_name_edit);

        final TextView ipView = (TextView) dialogView.findViewById(R.id.add_controller_ip_view);
        ipView.setText("Controller IP Address: ");
        final EditText editIP = (EditText) dialogView.findViewById(R.id.add_controller_ip_edit);

        if (createNew){
            builder.setTitle("Add new controller: ");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    String newName = editName.getText().toString();
                    String newIP = editIP.getText().toString();
                    Controller newController = new Controller(controllers.size(), newName, newIP);
                    try {
                        if (!databaseManager.foundedController(newController)) {
                            if (newIP.matches(("^\\d+(\\.\\d+)*$"))) {
                                databaseManager.addNewController(newController);
                                //controllerSpinner.setSelection(controllers.size() - 1);
                                updateDataFromDB();
                                controllerSpinner.setSelection(controllers.size() - 1);
                            } else {
                                showControllerDialog(false);
                                Toast.makeText(HomeControllerModifyActivity.this, "IP address is invalid", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            showControllerDialog(true);
                            Toast.makeText(HomeControllerModifyActivity.this, "Name or IP address has been available, please try again", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        if (newIP.matches(("^\\d+(\\.\\d+)*$"))) {

                            databaseManager.addNewController(newController);
                            //controllerSpinner.setSelection(controllers.size() - 1);
                            updateDataFromDB();
                            controllerSpinner.setSelection(controllers.size() - 1);
                        }
                    }
                }
            });
        } else {

            final Controller modifiedController = selectedController;

            editName.setText(modifiedController.getName());
            editName.selectAll();

            editIP.setText(modifiedController.getIpAddr());
            editIP.selectAll();

            builder.setTitle("Modify controller: ");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //Toast.makeText(HomeControllerModifyActivity.this, modifiedController.getName() Toast.LENGTH_SHORT).show();

                    String newName = editName.getText().toString();
                    String newIP = editIP.getText().toString();
                    modifiedController.setName(newName);
                    modifiedController.setIpAddr(newIP);

                    if (newName.equals(selectedController.getName())
                            || newIP.equals(selectedController.getIpAddr())
                            || !databaseManager.foundedController(modifiedController)) {
                        if (newIP.matches(("^\\d+(\\.\\d+)*$"))) {
                            databaseManager.updateController(modifiedController);
                            updateDataFromDB();
                        } else {
                            showControllerDialog(false);
                            Toast.makeText(HomeControllerModifyActivity.this, "IP address is invalid", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        showControllerDialog(false);
                        Toast.makeText(HomeControllerModifyActivity.this, "Name or IP address has been available, please try again", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeviceDialog(boolean createNew, final int modifyingID){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        LayoutInflater inflater = this.getLayoutInflater();

        final Controller[] selectedCtrl = new Controller[1];

        final View dialogView = inflater.inflate(R.layout.dialog_add_new_device, null);
        builder.setView(dialogView);

        final TextView nameView = (TextView) dialogView.findViewById(R.id.add_device_name_view);
        nameView.setText("Device name: ");
        final EditText editName = (EditText) dialogView.findViewById(R.id.add_device_name_edit);

        final TextView controllerView = (TextView) dialogView.findViewById(R.id.add_device_controller_view);
        controllerView.setText("Select controller: ");

        final Spinner selectCtrlSpinner = (Spinner) dialogView.findViewById(R.id.select_controller_spinner);
        final ControllerAdapter spinAdapter = new ControllerAdapter(getApplicationContext(), controllers);
        selectCtrlSpinner.setAdapter(spinAdapter);
        selectCtrlSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCtrl[0] = controllers.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        selectCtrlSpinner.setSelection(selectedController.getController_id());

        if (createNew){

            builder.setTitle("Add new device: ");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    String newName = editName.getText().toString();
                    final Device newDevice = new Device(selectedController.getDevices().size(), newName, selectedCtrl[0].getName());

                    if (!databaseManager.foundedDevice(newDevice)){
                        databaseManager.addNewDevice(newDevice);
                        updateDataFromDB();
                        deviceListView.setSelection(deviceListView.getCount() - 1);
                    } else {
                        showDeviceDialog(true, modifyingID);
                        Toast.makeText(HomeControllerModifyActivity.this, "This name has been available, please try another name", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {

            final Device modifiedDevice = selectedController.getDevices().get(modifyingID);

            editName.setText(modifiedDevice.getName());
            editName.selectAll();
            //editName.addTextChangedListener();

            builder.setTitle("Modify device: ");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    String newName = editName.getText().toString();
                    modifiedDevice.setName(newName);
                    modifiedDevice.setController(selectedCtrl[0].getName());

                    if (!databaseManager.foundedDevice(modifiedDevice)) {
                        databaseManager.updateDevice(modifiedDevice);
                        updateDataFromDB();
                    } else {
                        showDeviceDialog(false, modifyingID);
                        Toast.makeText(HomeControllerModifyActivity.this, "This name has been available, please try another name", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteControllerDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(HomeControllerModifyActivity.this, R.style.MyAlertDialogStyle);
        builder.setTitle("Are you sure?");
        builder.setMessage("Delete controller: \"" + selectedController.getName() + "\"");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                databaseManager.deleteController(selectedController);
                selectedId--;
                updateDataFromDB();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteDeviceDialog(final Device device){


        AlertDialog.Builder builder = new AlertDialog.Builder(HomeControllerModifyActivity.this, R.style.MyAlertDialogStyle);
        builder.setTitle("Are you sure?");
        builder.setMessage("Delete device: \"" + device.getName() + "\"");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                databaseManager.deleteDevice(device);
                updateDataFromDB();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
