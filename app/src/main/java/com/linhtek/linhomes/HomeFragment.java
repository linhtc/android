package com.linhtek.linhomes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by Admin on 04-06-2015.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    FirebaseDatabase database;
    DatabaseReference myRef;
    ValueEventListener mListener;
    DBHelper db;
    boolean flagRm = false;
    String phone;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.home_fragment,container,false);

        v.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK){
                    try{
                        FragmentManager fm = getFragmentManager();
                        if (fm.getBackStackEntryCount() > 0) {
                            Log.e("MainActivity", "==============> popping backstack");
                            fm.popBackStack();
                        }
                    } catch (Exception e){
                        Log.e("Websocket", "============> Exception: " + e.getMessage());
                    }
                }
                return true;
            }
        });

        addListenerOnButton(v);
        ((MainActivity) getActivity()).setActionBarTitle(getResources().getString(R.string.app_name_upper));

        db = new DBHelper(getActivity());
        Cursor activeUser = db.getActiveUser();
        if(activeUser.getCount() > 0){
            if(activeUser.moveToFirst()){
//                Integer sync = activeUser.getInt(activeUser.getColumnIndex("syn"));
//                if(sync < 1){
                    phone = activeUser.getString(activeUser.getColumnIndex("phone"));
                    ((MainActivity) getActivity()).ACTIVE_PHONE_USER = phone;
                    database = FirebaseDatabase.getInstance();
                    myRef = database.getReference("users/"+phone+"/devices");
                    mListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try{
                                if(dataSnapshot.hasChildren()){
                                    for(DataSnapshot device : dataSnapshot.getChildren() ){
                                        Log.e("FCM", "response device ==========> "+device.toString());
                                        if(!db.checkExistIdentifyDevice(device.getKey())){
                                            flagRm = true;
                                            Log.e("FCM", "response device.getKey() ==========> "+device.getKey());
                                            String cn = device.child("cn").getValue().toString();
                                            String wi = device.child("wi").getValue().toString();
                                            String ws = device.child("ws").getValue().toString();
                                            Long sta = (Long) device.child("sta").getValue();
                                            Long sty = (Long) device.child("sty").getValue();
                                            if(!db.checkDevice(device.getKey())){
                                                Log.e("FCM", "response insertDevice ==========> "+device.getKey());
                                                db.insertDevice(ws, wi, device.getKey(), cn, sta.intValue(), sty.intValue());
                                            } else{
                                                Log.e("FCM", "response updateDevice ==========> "+device.getKey());
                                                db.updateDevice(device.getKey(), cn, ws, wi, sty.intValue(), sta.intValue());
                                            }
                                        }
                                    }
                                    myRef.removeEventListener(mListener);
                                    if(flagRm){
                                        db.updateUser(phone, 1, 1);
                                    }
                                }
                                Log.e("FCM", "response dataSnapshot ==========> "+dataSnapshot.getValue().toString());
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
//                }
            }
        }
        activeUser.close();
//        db.close();

        return v;
    }

    public void addListenerOnButton(View v) {
        LinearLayout btnControl = (LinearLayout) v.findViewById(R.id.container_btn_control);
        btnControl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        LinearLayout view = (LinearLayout ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();

                        SwitchFragment fragment = new SwitchFragment();
                        FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();

                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });

        LinearLayout btnHealth = (LinearLayout) v.findViewById(R.id.container_btn_health);
        btnHealth.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        LinearLayout view = (LinearLayout ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });

        LinearLayout btnMedia = (LinearLayout) v.findViewById(R.id.container_btn_media);
        btnMedia.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        LinearLayout view = (LinearLayout ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });

        LinearLayout btnMore = (LinearLayout) v.findViewById(R.id.container_btn_more);
        btnMore.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        LinearLayout view = (LinearLayout ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });

        LinearLayout btnSecurity = (LinearLayout) v.findViewById(R.id.container_btn_security);
        btnSecurity.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        LinearLayout view = (LinearLayout ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });

        LinearLayout btnSetting = (LinearLayout) v.findViewById(R.id.container_btn_setting);
        btnSetting.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        LinearLayout view = (LinearLayout ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });

        LinearLayout btnTemperature = (LinearLayout) v.findViewById(R.id.container_btn_temperature);
        btnTemperature.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        LinearLayout view = (LinearLayout ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });

        LinearLayout btnTree = (LinearLayout) v.findViewById(R.id.container_btn_tree);
        btnTree.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        LinearLayout view = (LinearLayout ) v;
                        view.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        LinearLayout view = (LinearLayout) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        LinearLayout view = (LinearLayout) v;
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
    public void onStop() {
        Log.e("DEBUG", "onStop");
        try{
            myRef.removeEventListener(mListener);
        } catch (Exception e){
            Log.e("DEBUG", "onStop err: "+e.getMessage());
        }
        super.onStop();
    }

}
