package com.android4dev.navigationview;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Admin on 04-06-2015.
 */
public class DeviceFragment extends Fragment implements View.OnClickListener {

    ProgressDialog dialogLoading;
    private static final String TAG = "MainActivity";
    WebSocketClient mWebSocketClient;
    String deviceName;
    TextView customName;
    EditText customSSID;
    EditText customPw;
    int state = 0;
    int style = 0; // loai thiet bi, 1 la switch
    DBHelper db;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.device_fragment,container,false);

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
            deviceName = bundle.getString("device_name");
        }
        Log.e("Websocket", "============> deviceName: " + deviceName);
        customName = (TextView)v.findViewById(R.id.custom_name);
        customName.setText(deviceName);


        ((MainActivity) getActivity()).setActionBarTitle("SETUP DEVICES");

        return v;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnSetupApply:
                dialogLoading = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.processing), true);
                try{
                    state = 1;
                    JSONObject req = new JSONObject();
                    req.put("cmd", 2);
                    req.put("ssid", customSSID.getText());
                    req.put("pw", customPw.getText());
                    Log.e("Websocket", "req: "+req.toString());
                    mWebSocketClient.send(req.toString());
                    if(customName.getText().toString().isEmpty()){
                        customName.setText(deviceName);
                    }
                    db.updateDevice(deviceName, customName.getText().toString(), customSSID.getText().toString(), customPw.getText().toString());
                } catch (Exception e){
                    Log.e("Websocket", "req: "+e.getMessage());
                    Toast.makeText(getActivity().getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                    dialogLoading.dismiss();
                }
                break;
        }
    }

    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://192.168.4.1:9998");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

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
