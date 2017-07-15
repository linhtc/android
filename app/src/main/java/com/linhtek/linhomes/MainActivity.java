package com.linhtek.linhomes;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

//import static android.text.Html.FROM_HTML_OPTION_USE_CSS_COLORS;

public class MainActivity extends AppCompatActivity {

    //Defining Variables
    private Toolbar toolbar;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    public int PERMISSION_REQUIREMENT = 0;
    private boolean logging = false;
    public boolean create_openned = false;
    public boolean NOT_ALLOW_PERMISSION = false;
    public String ACTIVE_PHONE_USER = "";
    private static final int REQUEST_SIGNUP = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing Toolbar and setting it as the actionbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initializing NavigationView
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                //Checking if the item is in checked state or not, if not make it in checked state
                if(menuItem.isChecked()) menuItem.setChecked(false);
                else menuItem.setChecked(true);

                //Closing drawer on item click
                drawerLayout.closeDrawers();

                //Check to see which item was being clicked and perform appropriate action
                switch (menuItem.getItemId()){

                    case R.id.nav_control: {
                        SwitchFragment fragment = new SwitchFragment();
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                        return true;
                    }
                    case R.id.nav_health: {
                        HomeFragment fragment = new HomeFragment();
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.commit();
                        return true;
                    }
                    case R.id.nav_main: {
                        HomeFragment fragment = new HomeFragment();
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.commit();
                        return true;
                    }
                    case R.id.nav_media: {
                        HomeFragment fragment = new HomeFragment();
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.commit();
                        return true;
                    }
                    case R.id.nav_security: {
                        HomeFragment fragment = new HomeFragment();
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.commit();
                        return true;
                    }
                    case R.id.nav_setting: {
                        HomeFragment fragment = new HomeFragment();
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.commit();
                        return true;
                    }
                    case R.id.nav_tree: {
                        HomeFragment fragment = new HomeFragment();
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.commit();
                        return true;
                    }
                    case R.id.nav_weather: {
                        HomeFragment fragment = new HomeFragment();
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frame, fragment);
                        fragmentTransaction.commit();
                        return true;
                    }
                    default: {
                        Toast.makeText(getApplicationContext(), "Somethings Wrong", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
            }
        });
        navigationView.setItemBackground(ContextCompat.getDrawable(MainActivity.this, android.R.color.transparent));

        // Initializing Drawer Layout and ActionBarToggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.openDrawer, R.string.closeDrawer){

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank

                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
//        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessay or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();

//        getSupportActionBar().setTitle("LINHOMES");
        setActionBarTitle(getResources().getString(R.string.app_name_upper));

        actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
        actionBarDrawerToggle.setHomeAsUpIndicator(R.drawable.man32);
        actionBarDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        DBHelper db = new DBHelper(getBaseContext());
        Cursor activeUser = db.getActiveUser();
        if(activeUser.getCount() < 1){
            logging = true;
            Intent intent = new Intent(this, LoginActivity.class);
//            startActivity(intent);
            startActivityForResult(intent, REQUEST_SIGNUP);
        } else{
            HomeFragment fragment = new HomeFragment();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.frame, fragment);
            fragmentTransaction.commit();
            setUserInfo();
        }
        activeUser.close();
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        setUserInfo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("MainActivity", "============> requestCode: "+requestCode);
        Log.e("MainActivity", "============> resultCode: "+resultCode);
        Log.e("MainActivity", "============> RESULT_OK: "+RESULT_OK);
        if (requestCode == REQUEST_SIGNUP) {
            Log.e("MainActivity", "============> REQUEST_SIGNUP");
            if (resultCode == RESULT_OK) {
                Log.e("MainActivity", "============> logging true -> RESULT_OK");
                setUserInfo();
                HomeFragment fragment = new HomeFragment();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.frame, fragment);
                fragmentTransaction.commit();
            }
        }
    }

    public void setActionBarTitle(String title) {
        try{
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(getLayoutInflater().inflate(R.layout.custom_actionbar, null),
                    new ActionBar.LayoutParams(
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            ActionBar.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER
                    )
            );
            getSupportActionBar().setTitle(title);
            TextView tv = (TextView)findViewById(R.id.actionbar_textview);
            tv.setText(title);
        } catch (Exception exx){
            Log.e("MainActivity", "============> Exception: "+exx.getMessage());
        }
    }

    public void setUserInfo() {
        DBHelper db = new DBHelper(getBaseContext());
        Cursor activeUser = db.getActiveUser();
        if(activeUser.getCount() > 0){
            Log.e("MainActivity", "============> setUserInfo");
            if(activeUser.moveToFirst()){
                String phone = activeUser.getString(activeUser.getColumnIndex("phone"));
                String name = activeUser.getString(activeUser.getColumnIndex("full_name"));
                Log.e("MainActivity", "============> setUserInfo name: "+name);
                NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
                View headerView = navigationView.getHeaderView(0);
                TextView tvName = (TextView) headerView.findViewById(R.id.username);
                tvName.setText(name);
                TextView tvEmail = (TextView) headerView.findViewById(R.id.email);
                tvEmail.setText(phone);
                ACTIVE_PHONE_USER = phone;
            }
        }
        activeUser.close();
        db.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.btn_add_new) {
            if(!create_openned){
                create_openned = true;
                ScanFragment fragment = new ScanFragment();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.frame, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
//                checkAllRequirePermission();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    public void checkAllRequirePermission(){
//        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.INTERNET}, PERMISSION_REQUIREMENT);
//        } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CHANGE_WIFI_STATE}, PERMISSION_REQUIREMENT);
//        } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUIREMENT);
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.e("MainActivity", "============> requestCode: "+requestCode);
        if(requestCode == PERMISSION_REQUIREMENT && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.e("MainActivity", "============> PERMISSION_REQUIREMENT");
            NOT_ALLOW_PERMISSION = false;
        } else{
            NOT_ALLOW_PERMISSION = true;
        }
    }

}
