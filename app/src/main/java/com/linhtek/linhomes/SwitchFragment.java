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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
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
public class SwitchFragment extends Fragment {

    WifiManager wifiManager;
    ConnectivityManager conMgr;
    NetworkInfo activeNetwork;
    private static final String TAG = "SwitchFragment";
    String reactiveWifi = "";
    DBHelper db;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.switch_fragment,container,false);

        v.setFocusableInTouchMode(true);
        v.requestFocus();

        DBHelper mydb;
        mydb = new DBHelper(getActivity().getBaseContext());
        ArrayList devices = mydb.getAllDevices(1);
        Log.e("Websocket", "============> devices: "+devices.toString());

        if(devices.size() < 1){
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
            devices.add(getResources().getString(R.string.not_found_device));
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(getActivity(), R. layout.activity_listview, devices);
        ListView listView = (ListView) v.findViewById(R.id.device_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                String item = (String)parent.getItemAtPosition(position);
                Log.e("Websocket", "============> item clicked: " + item);
                if(!item.equalsIgnoreCase(getResources().getString(R.string.not_found_device))){
                    Bundle arguments = new Bundle();
                    arguments.putInt("style", 1);
                    arguments.putString("custom_name", item);
                    DeviceFragment fragment = new DeviceFragment();
                    fragment.setArguments(arguments);
                    FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.frame, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            }
        });

        ((MainActivity) getActivity()).setActionBarTitle("SWITCH DEVICES");

        db = new DBHelper(getActivity());
        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected() && !wifiManager.getConnectionInfo().getSSID().isEmpty()) {
            reactiveWifi = wifiManager.getConnectionInfo().getSSID();
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
