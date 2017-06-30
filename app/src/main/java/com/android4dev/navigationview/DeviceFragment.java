package com.android4dev.navigationview;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
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

import static android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

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
    boolean flagControl = false;
    boolean flagShowDialog = false;
    boolean flagReconnect = false;
    boolean flagPing = false;
    boolean flagConnected = false;
    boolean flagWSconnected = false;
    String styString = "switch";
    DBHelper db;
    FirebaseDatabase database;
    DatabaseReference myRef;
    Handler handler;
    Handler handler2;
    Runnable r;
    Runnable r2;
    WifiManager wifiManager;
    ConnectivityManager conMgr;
    NetworkInfo activeNetwork;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.device_fragment,container,false);

        dialogLoading = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.connecting), true);

        v.setFocusableInTouchMode(true);
        v.requestFocus();

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
            state = device.getInt(device.getColumnIndex("sta"));
            style = device.getInt(device.getColumnIndex("sty"));
            deviceIP = device.getString(device.getColumnIndex("wi"));
            deviceID = device.getString(device.getColumnIndex("fcm"));
            deviceName = device.getString(device.getColumnIndex("device_name"));
            deviceSSID = device.getString(device.getColumnIndex("ws"));
            Log.e("Websocket", "============> deviceName: " + deviceName);
            Log.e("Websocket", "============> deviceIP: " + deviceIP);
            Log.e("Websocket", "============> deviceID: " + deviceID);
            Log.e("Websocket", "============> deviceSSID: " + deviceSSID);
        }
        device.close();

        ((MainActivity) getActivity()).setActionBarTitle("DEVICE INFO");
        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected() && !wifiManager.getConnectionInfo().getSSID().isEmpty()) {
            flagConnected = true;
            if(deviceIP.isEmpty()){
                dialogLoading.setMessage(getResources().getString(R.string.device_setting));
            } else{
                connectWebSocket();
            }
        } else{
            flagConnected = false;
            if (wifiManager.isWifiEnabled() == false){
                Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.turn_on_wifi), Toast.LENGTH_LONG).show();
                wifiManager.setWifiEnabled(true);
                dialogLoading = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.connecting), true);
            }
        }

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference(styString+deviceID);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try{
                    dialogLoading.dismiss();
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    Log.e(TAG, "=============> FCM response: " + map.toString());
                    if(!map.get("wi").toString().isEmpty() && !map.get("wi").toString().equalsIgnoreCase(deviceIP)){
                        Log.e(TAG, "=============> update :"+ deviceName+": "+map.get("wi").toString());
                        deviceIP = map.get("wi").toString();
                        db.updateDevice(deviceName, deviceIP);
                        connectWebSocket();
                    }
                } catch (Exception e){
                    Log.e("FCM err", e.getMessage());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FCM err", "=============> Failed to read value.", error.toException());
            }
        });

        ImageButton btn = (ImageButton)v.findViewById(R.id.btnConfig);
        btn.setOnClickListener(this);
        SwitchButton btnDeviceSwitch = (SwitchButton)v.findViewById(R.id.btnDeviceSwitch);
        btnDeviceSwitch.setChecked(state == 1 ? true : false);
        btnDeviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.e(TAG, "=============> mode: " + buttonView.isInTouchMode());
                if(buttonView.isInTouchMode()){
                    conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                    activeNetwork = conMgr.getActiveNetworkInfo();
                    if (activeNetwork != null && activeNetwork.isConnected() && !wifiManager.getConnectionInfo().getSSID().isEmpty()) {
                        flagControl = true;
                        Log.e(TAG, "=============> switch to: " + isChecked);
                        if(wifiManager.getConnectionInfo().getSSID().equalsIgnoreCase("\""+deviceSSID+"\"")){
                            try{
                                JSONObject req = new JSONObject();
                                req.put("cmd", 3);
                                req.put("ps", 18);
                                req.put("req", isChecked ? 1 : 0);
                                Log.e("Websocket", "==> touched req: "+req.toString());
                                mWebSocketClient.send(req.toString());
                            } catch (Exception e){
                                Log.e("mWebSocketClient", "============> err: " + e.getMessage());
                            }
                        }
                        Log.e(TAG, "=============> update :"+ deviceName+": "+isChecked);
                        db.updateDevice(deviceName, isChecked ? 1 : 0);
                        Map<String, Object> childUpdates = new HashMap<>();
                        childUpdates.put("/ps/18/req", isChecked ? 1 : 0);
                        myRef.updateChildren(childUpdates);
                    } else{
                        flagConnected = false;
                        if (wifiManager.isWifiEnabled() == false){
                            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.turn_on_wifi), Toast.LENGTH_LONG).show();
                            wifiManager.setWifiEnabled(true);
                        } else{
                            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.device_never_connected), Toast.LENGTH_LONG).show();
                        }
                        if(dialogLoading.isShowing()){
                            dialogLoading.dismiss();
                        }
                    }
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
                FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.frame, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            default:break;
        }
    }

    @Override
    public void onResume() {
        if(flagReconnect){
            Log.e("DEBUG", "onResume of DeviceFragment flagReconnect");
            try{
                connectWebSocket();
            } catch (Exception e){
                Log.e(TAG, "============> reconnect err: "+e.getMessage());
            }
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        Log.e("DEBUG", "closing socket ===> ...");
        try{
            mWebSocketClient.close();
        } catch (Exception e){
            Log.e("DEBUG", "closing socket err: "+e.getMessage());
        }
        super.onStop();
    }

    private void connectWebSocket() {
        if(deviceIP.isEmpty()){
            Log.e("Websocket", "deviceIP is empty");
            return;
        }
        URI uri;
        try {
            uri = new URI("ws://"+deviceIP+":9998");
        } catch (Exception e) {
            Log.e("Websocket", "============> URI err: " + e.getMessage());
            return;
        }
        Log.e("Websocket", "come on come on ===> ...");

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.e("Websocket", "Opened");
                try{
                    JSONObject req = new JSONObject();
                    req.put("cmd", 0); // ack to response wake up from master
                    Log.e("Websocket", "req open: "+req.toString());
                    mWebSocketClient.send(req.toString());
                } catch (Exception e){
                    Log.e("Websocket", "============> json send err: " + e.getMessage());
                }
            }

            @Override
            public void onMessage(String message) {
                Log.e("Websocket", "============> response: " + message);
                try{
                    if(message != null){
                        dialogLoading.dismiss();
                        flagReconnect = true;
                        JSONObject res = new JSONObject(message);
                        Log.e("Websocket", "============> json response: " + res);
                        if(res.has("ack")){
                            flagWSconnected = true;
                            JSONObject req = new JSONObject();
                            req.put("cmd", 0); // ack to response wake up from master
                            Log.e("Websocket", "req ack: "+req.toString());
                            mWebSocketClient.send(req.toString());
                        }
                    }
                } catch (Exception e){
                    Log.e("Websocket", "============> json response err: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.e("Websocket", "Closed ==> " + s);
                flagWSconnected = false;
            }

            @Override
            public void onError(Exception e) {
                Log.e("Websocket", "Error " + e.getMessage());
//                Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.device_connected_yet), Toast.LENGTH_LONG).show();
                flagShowDialog = true;
                flagWSconnected = false;
            }
        };
        mWebSocketClient.connect();
    }

}
