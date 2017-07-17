package com.linhtek.linhomes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import static android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

/**
 * Created by Admin on 04-06-2015.
 */
public class SetupFragment extends Fragment implements View.OnClickListener {

    ProgressDialog dialogLoading;
    private static final String TAG = "SetupFragment";
    WebSocketClient mWebSocketClient;
    ArrayList<String> scannedWifi;
    ArrayAdapter<String> dataAdapter;
    WifiManager wifiManager;
    WifiScanReceiver wifiReciever;
    String deviceName;
    String optionName;
    String deviceIP;
    String deviceSSID;
    String devicePw;
    EditText customName;
    EditText customSSID;
    EditText customPw;
    String reactiveWifi = "";
    private Spinner inputSSID;
    int times = 0;
    int state = 0;
    int style = 0; // loai thiet bi, 1 la switch
    boolean startConnect = false;
    boolean flag_apply = false;
    DBHelper db;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.setup_fragment,container,false);

        v.setFocusableInTouchMode(true);
        v.requestFocus();

        dialogLoading = new ProgressDialog(getActivity(), R.style.AppTheme_Dark_Dialog);
        dialogLoading.setMessage(getResources().getString(R.string.connecting));
        dialogLoading.show();
//        dialogLoading = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.connecting), true);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            style = bundle.getInt("style");
            optionName = bundle.getString("custom_name");
            reactiveWifi = bundle.getString("reactive_wifi");
            scannedWifi = bundle.getStringArrayList("scanned_list");
        }

        db = new DBHelper(getActivity());
        Cursor device = db.getDevice(style, optionName);
        if(device.moveToFirst()){
            state = device.getInt(device.getColumnIndex("sta"));
            style = device.getInt(device.getColumnIndex("sty"));
            deviceIP = device.getString(device.getColumnIndex("wi"));
            checkActiveWifi();

            deviceName = device.getString(device.getColumnIndex("device_name"));
//            String optionName = device.getString(device.getColumnIndex("custom_name"));
            deviceSSID = device.getString(device.getColumnIndex("ws"));
            devicePw = device.getString(device.getColumnIndex("wp"));
            customName = (EditText)v.findViewById(R.id.custom_name);
//            customName.setHint(getResources().getString(R.string.device_name)+ ": "+(optionName.isEmpty() ? deviceName : optionName));
//            customSSID = (EditText)v.findViewById(R.id.custom_ssid);
//            inputSSID = (Spinner) v.findViewById(R.id.input_ssid);
//            customSSID.setHint(getResources().getString(R.string.device_ssid)+ ": "+deviceSSID);
            customPw = (EditText)v.findViewById(R.id.custom_pw);
//            customPw.setHint(getResources().getString(R.string.device_pw)+ ": "+devicePw);
        }

        ((MainActivity) getActivity()).setActionBarTitle("SETUP DEVICES");
        Button btnSetupApply = (Button) v.findViewById(R.id.btnSetupApply);
        btnSetupApply.setOnClickListener(this);
        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if(scannedWifi == null){
            scannedWifi = new ArrayList<String>();
        }
        inputSSID = (Spinner) v.findViewById(R.id.input_ssid);
        if(scannedWifi.size() > 0){
            Log.e(TAG, "scannedWifi ------------------->"+scannedWifi.toString());
//            dataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, scannedWifi);
//            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.custom_spinner, scannedWifi);
            dataAdapter.setDropDownViewResource(R.layout.custom_spinner);
            inputSSID.setAdapter(dataAdapter);
        } else{
            Log.e(TAG, "WifiScanReceiver ------------------->");
            wifiReciever = new WifiScanReceiver();
            getActivity().registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            wifiManager.startScan();
        }

        return v;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnSetupApply:
                try{
                    if(!customName.getText().toString().isEmpty()){
                        optionName = customName.getText().toString();
                    }
//                    if(!customSSID.getText().toString().isEmpty()){
//                        deviceSSID = customSSID.getText().toString();
//                    }
                    if(!inputSSID.getSelectedItem().toString().isEmpty()){
                        deviceSSID = inputSSID.getSelectedItem().toString();
                    }
                    if(!customPw.getText().toString().isEmpty()){
                        devicePw = customPw.getText().toString();
                    }
                    db.updateDevice(deviceName, optionName, deviceSSID, devicePw);
                    if(!inputSSID.getSelectedItem().toString().isEmpty() && !customPw.getText().toString().isEmpty()){
                        flag_apply = true;
//                        dialogLoading = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.processing), true);
                        dialogLoading.setMessage(getResources().getString(R.string.processing));
                        if(!dialogLoading.isShowing()){
                            dialogLoading.show();
                        }
                        state = 1;
                        JSONObject req = new JSONObject();
                        req.put("cmd", 2);
                        req.put("ssid", inputSSID.getSelectedItem().toString());
                        req.put("pw", customPw.getText().toString());
                        Log.e("Websocket", "==> req: "+req.toString());
                        mWebSocketClient.send(req.toString());
                    } else{
                        flag_apply = false;
                        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.updated), Toast.LENGTH_LONG).show();
                        SwitchFragment fragment = new SwitchFragment();
                        FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
//                                    fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                } catch (Exception e){
                    Log.e("Websocket", "req: "+e.getMessage());
                    dialogLoading.dismiss();
                    if(mWebSocketClient.getConnection() != null){
                        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.error_processing), Toast.LENGTH_LONG).show();
                    } else{
                        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.device_never_connected), Toast.LENGTH_LONG).show();
                        mWebSocketClient.connect();
                    }
                }
                break;
        }
    }

    @Override
    public void onStop() {
        Log.e("DEBUG", "closing socket...");
        try{
            mWebSocketClient.close();
            db.close();
        } catch (Exception e){
            Log.e("DEBUG", "closing socket err: "+e.getMessage());
        }
        super.onStop();
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            Log.e(TAG, "===================> Wifi scan receive: ");
            List<ScanResult> wifiScanList = wifiManager.getScanResults();
            Log.e(TAG, "===================> "+ wifiScanList.size());
            scannedWifi  = new ArrayList<String>();
            for(int i = 0; i < wifiScanList.size(); i++) {
                try{
                    if(!wifiScanList.get(i).SSID.equals(null)){
                        String ssid = wifiScanList.get(i).SSID;
                        if(!ssid.isEmpty()){
                            scannedWifi.add(ssid);
                        }
                    }
                    if(scannedWifi.size() > 0){
//                        dataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, scannedWifi);
//                        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.custom_spinner, scannedWifi);
                        dataAdapter.setDropDownViewResource(R.layout.custom_spinner);
                        inputSSID.setAdapter(dataAdapter);
//                        getActivity().unregisterReceiver(wifiReciever);
                    }
                } catch (Exception exx){
                    Log.e(TAG, "===================> "+ exx.getMessage());
                }
            }
        }
    }

    public boolean checkActiveWifi(){
        times++;
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                final ConnectivityManager conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
                Log.e(TAG, "===================> active network: "+activeNetwork);
                Log.e(TAG, "===================> active ssid: "+wifiManager.getConnectionInfo().getSSID());
                if (activeNetwork != null && activeNetwork.isConnected() && (
                        wifiManager.getConnectionInfo().getSSID().equalsIgnoreCase("\""+deviceName+"\"") ||
                        wifiManager.getConnectionInfo().getSSID().equalsIgnoreCase("\""+deviceSSID+"\"")
                    )
                ) {
                    Log.e(TAG, "===================> connected to device network: "+times);
                    if(!startConnect){
                        startConnect = true;
                        connectWebSocket();
                    }
                } else {
                    Log.e(TAG, "===================> Waiting network: "+times);
                    if(times < 10){
                        checkActiveWifi();
                    } else{
                        if(dialogLoading.isShowing()){
                            dialogLoading.dismiss();
                        }
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(getResources().getString(R.string.device_connected_yet))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                });
                        builder.create().show();
                    }
                }
            }
        }, 3000);
        return true;
    }

    private void connectWebSocket() {
        URI uri;
        try {
            String urip;
            if(deviceIP.isEmpty()){
                urip = "ws://192.168.4.1:9998";
            } else{
                urip = "ws://"+deviceIP+":9998";
            }
            Log.e(TAG, "====================>"+urip);
            uri = new URI(urip);
        } catch (Exception e) {
            Log.e("Websocket", "============> URI err: " + e.getMessage());
            return;
        }
        Log.e("Websocket", "come on come on...");

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.e("Websocket", "Opened");
//                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);

                try{
                    JSONObject req = new JSONObject();
                    req.put("cmd", 0);
                    Log.e("Websocket", "req: "+req.toString());
                    mWebSocketClient.send(req.toString());
                    dialogLoading.dismiss();
                } catch (Exception e){
                    Log.e("Websocket", "============> json response err: " + e.getMessage());
                }
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                Log.e("Websocket", "============> response: " + message);
//                dialogLoading.dismiss();
                try{
                    JSONObject res = new JSONObject(message);
                    Log.e("Websocket", "============> json response: " + res);
                    if(res.has("ack")){
                        JSONObject req = new JSONObject();
                        req.put("cmd", 0); // ack to response wake up from master
                        Log.e("Websocket", "req ack: "+req.toString());
                        mWebSocketClient.send(req.toString());
                    }
                    if(state == 1){
                        dialogLoading.dismiss();
                        if(res.getInt("status") == 1){
                            if(flag_apply){
                                switch (style){
                                    case 1:{ // switch
                                        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                                        if(reactiveWifi.isEmpty()){
                                            wifiManager.disconnect();
                                            wifiManager.reconnect();
                                        } else{
                                            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                                            for( WifiConfiguration i : list ) {
                                                if(i.SSID != null && i.SSID.equals("\"" + reactiveWifi + "\"")) {
                                                    wifiManager.disconnect();
                                                    wifiManager.enableNetwork(i.networkId, true);
                                                    wifiManager.reconnect();
                                                    Log.e("reconnecting", i.SSID);
                                                    break;
                                                }
                                            }
                                        }
                                        SwitchFragment fragment = new SwitchFragment();
                                        FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
                                        fragmentTransaction.replace(R.id.frame, fragment);
//                                    fragmentTransaction.addToBackStack(null);
                                        fragmentTransaction.commit();
                                        break;
                                    }
                                    default:break;
                                }
                            }
                        } else{
                            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Không nhận được phản hồi từ thiết bị")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                        }
                                    });
                            builder.create().show();
                        }
                    }
                } catch (Exception e){
                    Log.e("Websocket", "============> json response err: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.e("Websocket", "Closed " + s);
                Log.e("Websocket", "back to SwitchFragment " + s);
                if(flag_apply){
                    switch (style){
                        case 1:{ // switch
                            wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            if(reactiveWifi.isEmpty()){
                                wifiManager.disconnect();
                                wifiManager.reconnect();
                            } else{
                                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                                for( WifiConfiguration i2 : list ) {
                                    if(i2.SSID != null && i2.SSID.equals("\"" + reactiveWifi + "\"")) {
                                        wifiManager.disconnect();
                                        wifiManager.enableNetwork(i2.networkId, true);
                                        wifiManager.reconnect();
                                        Log.e("reconnecting", i2.SSID);
                                        break;
                                    }
                                }
                            }
                            SwitchFragment fragment = new SwitchFragment();
                            FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
                            fragmentTransaction.replace(R.id.frame, fragment);
//                                    fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        }
                        default:break;
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("Websocket", "Error " + e.getMessage());
                if(deviceIP.isEmpty()){
//                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.device_never_connected), Toast.LENGTH_LONG).show();
                } else{
//                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.error_processing), Toast.LENGTH_LONG).show();
                }
            }
        };
        mWebSocketClient.connect();
    }

}
