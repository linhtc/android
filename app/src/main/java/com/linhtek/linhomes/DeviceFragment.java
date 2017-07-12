package com.linhtek.linhomes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
public class DeviceFragment extends Fragment {

    ProgressDialog dialogLoading;
    private static final String TAG = "MainActivity";
    WebSocketClient mWebSocketClient;
    String deviceName;
    String customName;
    String deviceIP;
    String deviceSSID;
    String deviceID;
    TextView tvCustomName;
    TextView tvState;
    ImageButton btnDeviceSwitch;
    int state = 0;
    int style = 0; // loai thiet bi, 1 la switch
    boolean flagControl = false;
    boolean flagShowDialog = false;
    boolean flagReconnect = false;
    boolean flagConnected = false;
    boolean flagWSconnected = false;
    String styString = "switch";
    DBHelper db;
    FirebaseDatabase database;
    DatabaseReference myRef;
    ValueEventListener mListener;
    WifiManager wifiManager;
    ConnectivityManager conMgr;
    NetworkInfo activeNetwork;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.device_fragment,container,false);

//        dialogLoading = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.connecting), true);
        dialogLoading = new ProgressDialog(getActivity(), R.style.AppTheme_Dark_Dialog);
        dialogLoading.setMessage(getResources().getString(R.string.connecting));
        dialogLoading.show();

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
//        tvCustomName = (TextView)v.findViewById(R.id.custom_name);
        tvState = (TextView)v.findViewById(R.id.tv_status);
//        tvCustomName.setText(customName);

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

//        ((MainActivity) getActivity()).setActionBarTitle("DEVICE INFO");
        ((MainActivity) getActivity()).setActionBarTitle(customName);
        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected() && !wifiManager.getConnectionInfo().getSSID().isEmpty()) {
            flagConnected = true;
            if(deviceIP.isEmpty()){
                dialogLoading.setMessage(getResources().getString(R.string.device_setting));
                if(!dialogLoading.isShowing()){
                    dialogLoading.show();
                }
            } else{
                connectWebSocket();
            }
        } else{
            flagConnected = false;
            if (!wifiManager.isWifiEnabled()){
                Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.turn_on_wifi), Toast.LENGTH_LONG).show();
                wifiManager.setWifiEnabled(true);
//                dialogLoading = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.connecting), true);
                dialogLoading.setMessage(getResources().getString(R.string.processing));
                if(!dialogLoading.isShowing()){
                    dialogLoading.show();
                }
            }
        }

        database = FirebaseDatabase.getInstance();
        if(myRef == null){
            myRef = database.getReference(styString+deviceID);
            mListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try{
                        if(dialogLoading.isShowing()){
                            dialogLoading.dismiss();
                        }
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
            };
            myRef.addValueEventListener(mListener);
        }

        tvState.setText(state == 1 ? getResources().getString(R.string.on) : getResources().getString(R.string.off));
        addListenerOnButton(v);
        return v;
    }

    public void addListenerOnButton(View v) {
        ImageButton btnSwitch = (ImageButton)v.findViewById(R.id.btnDeviceSwitch);
        btnSwitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        ImageButton view = (ImageButton ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        ImageButton view = (ImageButton) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();

                        String strState = tvState.getText().toString();
                        boolean isChecked = false;
                        if(strState.equalsIgnoreCase(getResources().getString(R.string.off))){
                            isChecked = true;
                        }
                        tvState.setText(!isChecked ? getResources().getString(R.string.off) : getResources().getString(R.string.on));

                        conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                        activeNetwork = conMgr.getActiveNetworkInfo();
                        if (activeNetwork != null && activeNetwork.isConnected() && !wifiManager.getConnectionInfo().getSSID().isEmpty()) {
                            flagControl = true;
                            Log.e(TAG, "=============> switch to: " + isChecked);
                            if(wifiManager.getConnectionInfo().getSSID().equalsIgnoreCase("\""+deviceSSID+"\"") && flagWSconnected){
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

                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        ImageButton view = (ImageButton) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });

        ImageButton btnConfig = (ImageButton)v.findViewById(R.id.btnConfig);
        btnConfig.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        ImageButton view = (ImageButton ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        ImageButton view = (ImageButton) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();

                        Bundle arguments = new Bundle();
                        arguments.putInt("style", style);
                        arguments.putString("custom_name", customName);
//                arguments.putString("reactive_wifi", reactiveWifi);
//                arguments.putStringArrayList("scanned_list", scannedWifi);
                        SetupFragment fragment = new SetupFragment();
                        fragment.setArguments(arguments);
                        FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();

                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        ImageButton view = (ImageButton) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });

    }

    @Override
    public void onResume() {
        if(flagReconnect){
            Log.e("DEBUG", "onResume of DeviceFragment flagReconnect");
            try{
                connectWebSocket();
                db = new DBHelper(getActivity());
            } catch (Exception e){
                Log.e(TAG, "============> reconnect err: "+e.getMessage());
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.e("DEBUG", "onPause");
        try{
            mWebSocketClient.close();
            db.close();
            if (mListener != null && myRef!=null) {
                myRef.removeEventListener(mListener);
            }
        } catch (Exception e){
            Log.e("DEBUG", "onPause err: "+e.getMessage());
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e("DEBUG", "onStop");
        try{
            mWebSocketClient.close();
            db.close();
            if (mListener != null && myRef!=null) {
                myRef.removeEventListener(mListener);
            }
        } catch (Exception e){
            Log.e("DEBUG", "onStop err: "+e.getMessage());
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
                            if(res.has("pin18")){
                                state = res.getInt("pin18");
                                tvState.setText(state == 1 ? getResources().getString(R.string.on) : getResources().getString(R.string.off));
                            }
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
                if(flagReconnect){
//                    Log.e("Websocket", "reconnect to ws");
//                    mWebSocketClient.connect();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("Websocket", "Error " + e.getMessage());
                flagShowDialog = true;
                flagWSconnected = false;
            }
        };
        mWebSocketClient.connect();
    }

}
