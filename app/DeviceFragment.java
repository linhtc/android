package com.android4dev.navigationview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kyleduo.switchbutton.SwitchButton;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by Admin on 04-06-2015.
 */
public class DeviceFragment extends Fragment implements View.OnClickListener {

    ProgressDialog dialogLoading;
    private static final String TAG = "MainActivity";
    WebSocketClient mWebSocketClient;
    String deviceName;
    String customName;
    String deviceIP;
    String deviceSSID;
    String deviceID;
    TextView tvCustomName;
    int state = 0;
    int style = 0; // loai thiet bi, 1 la switch
    String styString = "switch";
    DBHelper db;
    FirebaseDatabase database;
    DatabaseReference myRef;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.device_fragment,container,false);

        dialogLoading = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.processing), true);

        v.setFocusableInTouchMode(true);
        v.requestFocus();
        v.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return true;
            }
        });

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            style = bundle.getInt("style");
            customName = bundle.getString("custom_name");
        }
        if(style == 1){
            styString = "switchs/";
        }
        Log.e("Websocket", "============> customName: " + customName);
        tvCustomName = (TextView)v.findViewById(R.id.custom_name);
        tvCustomName.setText(customName);

        db = new DBHelper(getActivity());
        Cursor device = db.getDevice(style, customName);

        if(device.moveToFirst()){
            int sta = device.getInt(device.getColumnIndex("sta"));
            style = device.getInt(device.getColumnIndex("sty"));
            deviceIP = device.getString(device.getColumnIndex("wi"));
            deviceID = device.getString(device.getColumnIndex("fcm"));
            deviceName = device.getString(device.getColumnIndex("device_name"));
            deviceSSID = device.getString(device.getColumnIndex("ws"));
            Log.e("Websocket", "============> deviceName: " + deviceName);
        }

        ((MainActivity) getActivity()).setActionBarTitle("DEVICE INFO");

        if(deviceIP.isEmpty()){
            dialogLoading.setMessage(getResources().getString(R.string.device_setting));
        }

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference(styString+deviceID);

        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final ConnectivityManager conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        Log.e(TAG, "===================> active network: "+activeNetwork);
        Log.e(TAG, "===================> active ssid: "+wifiManager.getConnectionInfo().getSSID());
        if (activeNetwork != null && activeNetwork.isConnected() && !wifiManager.getConnectionInfo().getSSID().isEmpty()) {
            Log.e("Websocket", "============> deviceID: " + deviceID);
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try{
                        dialogLoading.dismiss();
                        Log.e(TAG, "=============> Value is: " + dataSnapshot.toString());
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                        deviceIP = map.get("wi").toString();
                        Log.e(TAG, "=============> Fcm response wi:"+ deviceIP);
                        db.updateDevice(deviceName, deviceIP);
                        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if(!deviceIP.isEmpty() && wifiManager.getConnectionInfo().getSSID().equalsIgnoreCase("\""+deviceSSID+"\"")){
                            connectWebSocket();
                            Log.e(TAG, "=============> opening ws...");
                        } else{
                            Log.e(TAG, "=============> code here");
                        }
                    } catch (Exception e){
                        Log.e("Websocket", "============> Exception: " + e.getMessage());
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.e(TAG, "=============> Failed to read value.", error.toException());
                }
            });
        } else{
            dialogLoading.setMessage(getResources().getString(R.string.app_connecting));
//            dialogLoading.dismiss();
//            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//            builder.setTitle("Hay ket noi wifi de su dung")
//                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.dismiss();
//                    }
//                });
//            builder.create().show();
        }

        ImageButton btn = (ImageButton)v.findViewById(R.id.btnConfig);
        btn.setOnClickListener(this);
        SwitchButton btnDeviceSwitch = (SwitchButton)v.findViewById(R.id.btnDeviceSwitch);
        btnDeviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final ConnectivityManager conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
                Log.e(TAG, "=============> switch to: " + isChecked);
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if(wifiManager.getConnectionInfo().getSSID().equalsIgnoreCase("\""+deviceSSID+"\"")){
                        try{
                            JSONObject req = new JSONObject();
                            req.put("cmd", 3);
                            req.put("ps", 18);
                            req.put("req", isChecked ? 1 : 0);
                            Log.e("Websocket", "==> req: "+req.toString());
                            if(WebSocket.READYSTATE.OPEN.compareTo(mWebSocketClient.getReadyState()) == 0){
                                mWebSocketClient.send(req.toString());
                            } else {
                                mWebSocketClient.connect();
                                mWebSocketClient.send(req.toString());
                            }
                        } catch (Exception e){
                            Log.e("mWebSocketClient", "============> err: " + e.getMessage());
                        }
                    } else{
                        Map<String, Object> childUpdates = new HashMap<>();
                        childUpdates.put("/ps/18/req", isChecked ? 1 : 0);
                        myRef.updateChildren(childUpdates);
                    }
                } else{
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Hay ket noi wifi de su dung")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                    builder.create().show();
                }
            }
        });

        return v;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnConfig:
                Bundle arguments = new Bundle();
                arguments.putInt("style", style);
                arguments.putString("custom_name", customName);
                SetupFragment fragment = new SetupFragment();
                fragment.setArguments(arguments);
                FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.frame, fragment);
                fragmentTransaction.commit();
                break;
            default:break;
        }
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
            }
        };
        mWebSocketClient.connect();
    }

}
