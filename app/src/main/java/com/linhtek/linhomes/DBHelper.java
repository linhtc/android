package com.linhtek.linhomes;

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

public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "LinHomes.db";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "create table devices(" +
            "device_name text primary key, custom_name text, ws text, wp text, wi text, fcm text, sta INTEGER, sty int" +
        ")";
        db.execSQL(query);
        query = "create table users(" +
            "phone text primary key, full_name text, pw text, sta INTEGER, syn INTEGER" +
          ")";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS devices");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    public Cursor getActiveUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from users where sta = 1 limit 1", null );
        return res;
    }

    public boolean insertUser (String name, String phone, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("phone", phone);
        contentValues.put("full_name", name);
        contentValues.put("pw", password);
        contentValues.put("sta", 1);
        contentValues.put("syn", 0);
        db.insert("users", null, contentValues);
        return true;
    }

    public boolean insertUser (String name, String phone, String password, Integer status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("phone", phone);
        contentValues.put("full_name", name);
        contentValues.put("pw", password);
        contentValues.put("sta", status);
        contentValues.put("syn", 0);
        db.insert("users", null, contentValues);
        return true;
    }

    public Cursor getUser(String phone, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery( "select * from users where phone = ? and pw = ? limit 1", new String[] { phone, password } );
    }

    public boolean updateUser (String phone, Integer status, Integer sync) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("sta", status);
        contentValues.put("syn", sync);
        db.update("users", contentValues, "phone = ? ", new String[] { phone } );
        return true;
    }

    public boolean checkExistUser(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from users where phone = ? limit 1", new String[] { phone } );
        if(res.getCount() > 0){
            res.close();
            return true;
        }
        res.close();
        return false;
    }

    public boolean insertDevice (String firebase, String ap_ssid, String ap_pw, String ap_ip, String device_name, Integer style) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("fcm", firebase);
        contentValues.put("ws", ap_ssid);
        contentValues.put("wp", ap_pw);
        contentValues.put("wi", ap_ip);
        contentValues.put("device_name", device_name);
        contentValues.put("custom_name", device_name);
        contentValues.put("sta", 0);
        contentValues.put("sty", style);
        db.insert("devices", null, contentValues);
        return true;
    }

    public boolean insertDevice (String ap_ssid, String ap_ip, String device_name,  String custom_name, Integer status, Integer style) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("ws", ap_ssid);
        contentValues.put("wi", ap_ip);
        contentValues.put("device_name", device_name);
        contentValues.put("custom_name", custom_name);
        contentValues.put("sta", status);
        contentValues.put("sty", style);
        db.insert("devices", null, contentValues);
        return true;
    }

    public boolean checkExistDevice(String deviceName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from devices where device_name=?", new String[] { deviceName } );
        if(res.getCount() > 0){
            return true;
        }
        return false;
    }

    public boolean checkExistIdentifyDevice(String deviceID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select * from devices where fcm=?", new String[] { deviceID } );
        if(res.getCount() > 0){
            return true;
        }
        return false;
    }

    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery( "select * from devices where id="+id+"", null );
    }

    public Cursor getDeviceToSetup() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery( "select * from devices where sta = 0 limit 1", null );
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, "devices");
    }

    public boolean updateDevice (String device_name, String custom_name, String ssid, String pw) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("custom_name", custom_name);
        contentValues.put("ws", ssid);
        contentValues.put("wp", pw);
        db.update("devices", contentValues, "device_name = ? ", new String[] { device_name } );
        return true;
    }

    public boolean updateDevice (String device_name, String ip) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("wi", ip);
        db.update("devices", contentValues, "device_name = ? ", new String[] { device_name } );
        return true;
    }

    public boolean updateDevice (String device_name, Integer status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("sta", status);
        db.update("devices", contentValues, "device_name = ? ", new String[] { device_name } );
        return true;
    }

    public boolean updateDevice (String id, String custom_name, String ssid, String ip, Integer style, Integer status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("custom_name", custom_name);
        contentValues.put("ws", ssid);
        contentValues.put("wi", ip);
        contentValues.put("sty", style);
        contentValues.put("sta", status);
        db.update("devices", contentValues, "device_name = ? ", new String[] { id } );
        return true;
    }

    public Integer deleteDevice (Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("devices", "id = ? ", new String[] { Integer.toString(id) });
    }

    public Cursor getDevice(Integer style, String custom_name) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery( "select * from devices where sty = ? and custom_name = ? limit 1",
                new String[] { Integer.toString(style), custom_name } );
    }
    public boolean checkDevice(String device_name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "select device_name from devices where device_name = ? limit 1", new String[] { device_name } );
        if(res.getCount() > 0){
            return true;
        }
        return false;
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

    public ArrayList<String> getAllDevices(Integer style) {
        ArrayList<String> array_list = new ArrayList<String>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select custom_name from devices where sty = ?", new String[] { Integer.toString(style) } );
        if(res.moveToFirst()){
            do{
                array_list.add(res.getString(res.getColumnIndex("custom_name")));
            }while(res.moveToNext());
        }
        Log.e("Websocket", "============> json response err: "+array_list.toString());
        res.close();
        return array_list;
    }

    public int deleteAllDevice() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("devices", "sty > ? ", new String[]{Integer.toString(0)});
    }

}
