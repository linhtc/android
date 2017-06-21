package com.android4dev.navigationview;

/**
 * Created by linhtc on 12/05/2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "LinHomes.db";
    public static final String DEVICES_TABLE_NAME = "devices";
    public static final String CONTACTS_COLUMN_ID = "id";
    public static final String CONTACTS_COLUMN_IP = "ip";
    public static final String CONTACTS_COLUMN_CAT = "cat";
    public static final String DEVICE_COLUMN_NAME = "name";
    public static final String CONTACTS_COLUMN_PATH = "path";
    private HashMap hp;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "create table devices (" +
                "device_name text primary key, ws text, wp text, wi text, fcm text, sta int" +
        ");";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS devices");
        onCreate(db);
    }

    public boolean insertDevice (String firebase, String ap_ssid, String ap_pw, String ap_ip, String device_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("fcm", firebase);
        contentValues.put("ws", ap_ssid);
        contentValues.put("wp", ap_pw);
        contentValues.put("wi", ap_ip);
        contentValues.put("device_name", device_name);
        contentValues.put("sta", 0);
        db.insert("devices", null, contentValues);
        return true;
    }

    public boolean checkExistDevice(String ssid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from devices where ap_ssid="+ssid+"", null );
        if(res.getCount() > 0){
            return true;
        }
        return false;
    }

    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from devices where id="+id+"", null );
        return res;
    }

    public Cursor getDeviceToSetup() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from devices where sta = 0 limit 1", null );
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, DEVICES_TABLE_NAME);
        return numRows;
    }

    public boolean updateDevice (Integer id, String ip, String cat, String name, String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("ip", ip);
        contentValues.put("cat", cat);
        contentValues.put("name", name);
        contentValues.put("path", path);
        db.update("devices", contentValues, "id = ? ", new String[] { Integer.toString(id) } );
        return true;
    }

    public Integer deleteDevice (Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("devices", "id = ? ", new String[] { Integer.toString(id) });
    }

    public ArrayList<String> getAllDevices() {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select device_name from devices", null );
//        res.moveToFirst();

//        while(res.isAfterLast() == false){
//            array_list.add(res.getString(4).toString());
//            res.moveToNext();
//        }
        if(res.moveToFirst()){
            do{
                //assing values
                String column1 = res.getString(0);
                Log.e("Websocket", "============> json response err: "+column1);
                array_list.add(res.getString(0));
                //Do something Here with values

            }while(res.moveToNext());
        }
        Log.e("Websocket", "============> json response err: "+array_list.toString());
        res.close();
        return array_list;
    }
}