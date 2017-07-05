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
public class SwitchFragment extends Fragment implements View.OnClickListener {

    ProgressDialog dialogLoading;
    WifiManager wifi;
    List<String> wifis;
    ArrayList<String> scannedWifi;
    boolean flagScan = false;
    WifiScanReceiver wifiReciever;
    WifiManager wifiManager;
    ConnectivityManager conMgr;
    NetworkInfo activeNetwork;
    LocationManager locationManager;
    private static final String TAG = "SwitchFragment";
    int times = 0;
    String finalSSID = "";
    String reactiveWifi = "";
    DBHelper db;
    boolean openGPS = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.switch_fragment,container,false);

        v.setFocusableInTouchMode(true);
        v.requestFocus();

        DBHelper mydb;
        mydb = new DBHelper(getActivity());
        ArrayList devices = mydb.getAllDevices(1);
        Log.e("Websocket", "============> devices: "+devices.toString());

        if(devices.size() < 1){
            devices.add(getResources().getString(R.string.not_found_device));
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(getActivity(), R. layout.activity_listview, devices);
        ListView listView = (ListView) v.findViewById(R.id.device_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
//                Toast.makeText(getActivity().getApplicationContext(), "" + position, Toast.LENGTH_SHORT).show();
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

        ImageButton btn = (ImageButton)v.findViewById(R.id.btnAddNew);
        btn.setOnClickListener(this);

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
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnAddNew:
                locationManager = (LocationManager) getActivity().getSystemService( Context.LOCATION_SERVICE );
                if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                    openGPS = true;
                    buildAlertMessageNoGps();
                } else{
                    try{
                        dialogLoading = ProgressDialog.show(getActivity(), "", "Đang tìm thiết bị...", true);
                        wifi = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if (wifi.isWifiEnabled() == false){
                            Toast.makeText(getActivity().getApplicationContext(), "Đã bật WiFi", Toast.LENGTH_LONG).show();
                            wifi.setWifiEnabled(true);
                        }
                        flagScan = true;
                        wifiReciever = new WifiScanReceiver();
                        getActivity().registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                        wifi.startScan();
                    } catch (Exception e){
                        Log.e(TAG, "===================> add new e: "+e.getMessage());
                    }
                }
                break;
        }
    }

    @Override
    public void onResume() {
        if(openGPS){
            Log.e("DEBUG", "onResume of SwitchFragment openGPS");
            locationManager = (LocationManager) getActivity().getSystemService( Context.LOCATION_SERVICE );
            if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                openGPS = true;
                buildAlertMessageNoGps();
            } else{
                try{
                    dialogLoading = ProgressDialog.show(getActivity(), "", "Đang tìm thiết bị...", true);
                    wifi = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wifi.isWifiEnabled() == false){
                        Toast.makeText(getActivity().getApplicationContext(), "Đã bật WiFi", Toast.LENGTH_LONG).show();
                        wifi.setWifiEnabled(true);
                    }
                    flagScan = true;
                    wifiReciever = new WifiScanReceiver();
                    getActivity().registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    wifi.startScan();
                } catch (Exception e){
                    Log.e(TAG, "===================> add new e: "+e.getMessage());
                }
            }
        }
        super.onResume();
    }

    public void showScanningDevices(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if(wifis.size() > 0){
            CharSequence[] items = wifis.toArray(new CharSequence[wifis.size()]);
            builder.setTitle("Danh sách thiết bị").setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // The 'which' argument contains the index position
                    // of the selected item
                    dialog.dismiss();
                    Log.e(TAG, "===================> Wifi choose: "+which);
                    Log.e(TAG, "===================> Wifi choose: "+wifis.get(which).toString());
                    String ssid = wifis.get(which).toString();
                    dialogLoading = ProgressDialog.show(getActivity(), "", "Kết nối tới "+ssid, true);

                    boolean connected = ConnectToNetworkWPA(ssid, "11330232");
                    if(connected){
                        try{
//                            new getDeviceInfo().execute();
                            Log.e(TAG, "===================> Webview start");
                            finalSSID = ssid;
                            times = 0;
                            checkActiveWifi();
                        } catch (Exception exx2){
                            Log.e(TAG, "===================> Wifi exx2: "+exx2.getMessage());
                        }
                    } else{
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
                        builder2.setMessage("Lỗi").setCancelable(true).create().show();
                    }
                    Log.e(TAG, "===================> Wifi choose: "+connected);
//                    dialogLoading.dismiss();
                }
            });
        } else{
            builder.setMessage("Không tìm thấy. Hãy thử khởi động lại thiết bị")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        dialog.dismiss();
                    }
                });
        }
        dialogLoading.hide();
        builder.create();
        builder.show();
        flagScan = false;
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            Log.e(TAG, "===================> Wifi scan receive: ");
            List<ScanResult> wifiScanList = wifi.getScanResults();
            Log.e(TAG, "===================> "+ wifiScanList.size());
            wifis  = new ArrayList<String>();
            scannedWifi = new ArrayList<String>();
            for(int i = 0; i < wifiScanList.size(); i++) {
                try{
                    if(!wifiScanList.get(i).SSID.equals(null)){
                        String ssid = wifiScanList.get(i).SSID;
                        if(ssid.contains("Switch-") == true){
                            if(!db.checkDevice(ssid)){
                                wifis.add(ssid);
                            }
                        }
                        if(!ssid.isEmpty()){
                            scannedWifi.add(ssid);
                        }
                    }
                } catch (Exception exx){
                    Log.e(TAG, "===================> "+ exx.getMessage());
                }
            }

            if(flagScan){
                showScanningDevices();
            }
        }
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

            Log.e("connecting", conf.SSID + " " + conf.preSharedKey);

            wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.addNetwork(conf);

            Log.e("after connecting", conf.SSID + " " + conf.preSharedKey);

            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    Log.e("reconnecting", i.SSID + " " + conf.preSharedKey);

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

    public boolean checkActiveWifi(){
        times++;
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                activeNetwork = conMgr.getActiveNetworkInfo();
                Log.e(TAG, "===================> active network: "+activeNetwork);
                Log.e(TAG, "===================> active ssid: "+wifiManager.getConnectionInfo().getSSID());
                Log.e(TAG, "===================> active finalSSID: "+finalSSID);
                if (activeNetwork != null && activeNetwork.isConnected() && wifiManager.getConnectionInfo().getSSID().equalsIgnoreCase("\""+finalSSID+"\"")) {
//                    new getDeviceInfo().execute();
                    Log.e(TAG, "===================> connected to device network: "+times);
//                    connectWebSocket();
                    dialogLoading.dismiss();
                    DBHelper mydb;
                    mydb = new DBHelper(getActivity());
                    boolean ins = mydb.insertDevice(finalSSID.substring(finalSSID.indexOf("-") + 1), "", "", "", finalSSID, 1);
                    if(ins){
                        Bundle arguments = new Bundle();
                        arguments.putInt("style", 1);
                        arguments.putString("custom_name", finalSSID);
                        arguments.putString("reactive_wifi", reactiveWifi);
                        arguments.putStringArrayList("scanned_list", scannedWifi);
                        SetupFragment fragment = new SetupFragment();
                        fragment.setArguments(arguments);
                        FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                } else {
                    Log.e(TAG, "===================> Waiting network: "+times);
                    if(times < 10){
//                        ConnectToNetworkWPA(finalSSID, "11330232");
                        checkActiveWifi();
                    } else{
                        dialogLoading.dismiss();
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage("Không kết nối được.Hãy khởi động máy, thiết bị và thử lại")
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

    private void buildAlertMessageNoGps() {
        openGPS = false;
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Hãy bật GPS và thử lại")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

}
