package com.linhtek.linhomes;

/**
 * Created by leon on 6/14/17.
 */

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketClient extends AsyncTask<Object, Object, String> {

    String dstAddress;
    int dstPort;
    String response = "";
    TextView textResponse;
    private static final String TAG = "MainActivity";

    SocketClient(String addr, int port, TextView textResponse) {
        dstAddress = addr;
        dstPort = port;
        this.textResponse = textResponse;
    }

    @Override
    protected String doInBackground(Object... arg0) {
        Socket socket = null;
        try {
            socket = new Socket(dstAddress, dstPort);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            String outMsg =  "{\"ps\":18,\"req\":1}";
            out.write(outMsg);
            out.flush();
            out.close();

            int bytesRead;
            InputStream inputStream = socket.getInputStream();

			/*
             * notice: inputStream.read() will block if no data return
			 */
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                response += byteArrayOutputStream.toString("UTF-8");
            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return response;
    }

    @Override
    protected void onPostExecute(String result) {
        if(response != null){
//            textResponse.setText(response);
            Log.e(TAG, "===================> Server socket response: "+response);
        }
        super.onPostExecute(result);
    }

}
