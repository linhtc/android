package com.android4dev.navigationview;

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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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

/**
 * Created by Admin on 04-06-2015.
 */
public class SetupFragment extends Fragment implements View.OnClickListener {

    ProgressDialog dialogLoading;
    private static final String TAG = "MainActivity";
    WebSocketClient mWebSocketClient;
    String deviceName;
    String optionName;
    String deviceIP;
    EditText customName;
    EditText customSSID;
    EditText customPw;
    int times = 0;
    int state = 0;
    int style = 0; // loai thiet bi, 1 la switch
    DBHelper db;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.setup_fragment,container,false);

        v.setFocusableInTouchMode(true);
        v.requestFocus();
        v.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    db.close();
                    Log.e(TAG, "===========> CookerFragment key back!");
                    HomeFragment fragment = new HomeFragment();
                    FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.frame, fragment);
                    fragmentTransaction.commit();
                    return true;
                } else {
                    return false;
                }
            }
        });

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            style = bundle.getInt("style");
            optionName = bundle.getString("custom_name");
        }

        db = new DBHelper(getActivity());
//        Cursor device = db.getDeviceToSetup();
        Cursor device = db.getDevice(style, optionName);
        if(device.moveToFirst()){
            int sta = device.getInt(device.getColumnIndex("sta"));
            style = device.getInt(device.getColumnIndex("sty"));
            deviceIP = device.getString(device.getColumnIndex("wi"));
//            if(sta == 0){
//                Log.e("Websocket", "connect to ws");
//                connectWebSocket();
//            }
            checkActiveWifi();

            deviceName = device.getString(device.getColumnIndex("device_name"));
            String optionName = device.getString(device.getColumnIndex("custom_name"));
            String deviceSSID = device.getString(device.getColumnIndex("ws"));
            String devicePw = device.getString(device.getColumnIndex("wp"));
            customName = (EditText)v.findViewById(R.id.custom_name);
            customName.setHint(getResources().getString(R.string.device_name)+ ": "+(optionName.isEmpty() ? deviceName : optionName));
            customSSID = (EditText)v.findViewById(R.id.custom_ssid);
            customSSID.setHint(getResources().getString(R.string.device_ssid)+ ": "+deviceSSID);
            customPw = (EditText)v.findViewById(R.id.custom_pw);
            customPw.setHint(getResources().getString(R.string.device_pw)+ ": "+devicePw);
        }

        ((MainActivity) getActivity()).setActionBarTitle("SETUP DEVICES");
        Button btnSetupApply = (Button) v.findViewById(R.id.btnSetupApply);
        btnSetupApply.setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnSetupApply:
                try{
                    if(customSSID.getText().toString().isEmpty()){
                        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.error_input), Toast.LENGTH_LONG).show();
                    } else{
                        dialogLoading = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.processing), true);
                        state = 1;
                        JSONObject req = new JSONObject();
                        req.put("cmd", 2);
                        req.put("ssid", customSSID.getText());
                        req.put("pw", customPw.getText());
                        Log.e("Websocket", "==> req: "+req.toString());
                        mWebSocketClient.send(req.toString());
                        if(customName.getText().toString().isEmpty()){
                            customName.setText(deviceName);
                        }
                        db.updateDevice(deviceName, customName.getText().toString(), customSSID.getText().toString(), customPw.getText().toString());
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

    public boolean checkActiveWifi(){
        times++;
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                final ConnectivityManager conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
                Log.e(TAG, "===================> active network: "+activeNetwork);
                Log.e(TAG, "===================> active ssid: "+wifiManager.getConnectionInfo().getSSID());
                if (activeNetwork != null && activeNetwork.isConnected() && wifiManager.getConnectionInfo().getSSID().equalsIgnoreCase("\""+deviceName+"\"")) {
//                    new getDeviceInfo().execute();
                    Log.e(TAG, "===================> connected to device network: "+times);
                    connectWebSocket();
                } else {
                    Log.e(TAG, "===================> Waiting network: "+times);
                    if(times < 10){
                        checkActiveWifi();
                    } else{
                        dialogLoading.dismiss();
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Không kết nối được")
                                .setMessage("Hãy khởi động máy, thiết bị và thử lại")
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

    public boolean ConnectToNetworkWPA( String networkSSID, String password ) {
        try {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + networkSSID + "\"";   // Please note the quotes. String should contain SSID in quotes

            conf.preSharedKey = "\"" + password + "\"";

            conf.status = WifiConfiguration.Status.ENABLED;
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

            Log.d("connecting", conf.SSID + " " + conf.preSharedKey);

            WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.addNetwork(conf);

            Log.d("after connecting", conf.SSID + " " + conf.preSharedKey);

            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    Log.d("re connecting", i.SSID + " " + conf.preSharedKey);

                    break;
                }
            }
            Log.e(TAG, "===================> Wifi dsaexx2: ");

            //WiFi Connection success, return true
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "===================> ConnectToNetworkWPA: "+ex.getMessage());
            return false;
        }
    }

    private void connectWebSocket() {
        URI uri;
        try {
            String urip;
            if(deviceIP.isEmpty()){
                urip = "ws://192.168.4.1:9998";
            } else{
                urip = "ws://"+deviceIP;
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
                    req.put("cmd", 1);
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
                    if(state == 1){
                        dialogLoading.dismiss();
                        if(res.getInt("status") == 1){
                            switch (style){
                                case 1:{ // switch
                                    db.close();

//                                    ConnectToNetworkWPA(customSSID.getText().toString(), customPw.getText().toString());
                                    SwitchFragment fragment = new SwitchFragment();
                                    FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                                    fragmentTransaction.replace(R.id.frame, fragment);
                                    fragmentTransaction.commit();
                                    break;
                                }
                                default:break;
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
            }

            @Override
            public void onError(Exception e) {
                Log.e("Websocket", "Error " + e.getMessage());
                if(deviceIP.isEmpty()){
                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.device_never_connected), Toast.LENGTH_LONG).show();
                } else{
                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.error_processing), Toast.LENGTH_LONG).show();
                }
            }
        };
        mWebSocketClient.connect();
    }

}
