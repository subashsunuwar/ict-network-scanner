package com.android.networkscanner;

/**
 * Created by shishir on 10/23/16.
 */


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by shishir on 20/03/15.
 */
public class ConnectionService extends Service {

    // Constant
    public static String TAG_INTERVAL = "interval";
    public static String TAG_URL_PING = "url_ping";
    public static String TAG_ACTIVITY_NAME = "activity_name";

    private int interval;
    private String url_ping;
    private String activity_name;

    private Timer mTimer = null;

    private Boolean prevState= false;

    ConnectionServiceCallback mConnectionServiceCallback;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public interface ConnectionServiceCallback {
        void hasInternetConnection();
        void hasNoInternetConnection();
    }

    void stateChanged(Boolean x){
        Intent i = new Intent("com.android.networkscanner.netstatus");
        i.putExtra("NET_STAT",x);
        //i.putExtra("NET_TYPE", "test");
        sendBroadcast(i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Service Started");
        interval = intent.getIntExtra(TAG_INTERVAL, 10);
        url_ping = intent.getStringExtra(TAG_URL_PING);
        activity_name = intent.getStringExtra(TAG_ACTIVITY_NAME);

        try {
            mConnectionServiceCallback = (ConnectionServiceCallback) Class.forName(activity_name).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new CheckForConnection(), 0, interval * 1000);

        return super.onStartCommand(intent, flags, startId);
    }

    class CheckForConnection extends TimerTask{
        @Override
        public void run() {
            isNetworkAvailable();
            //System.out.println("Timer...");
            //System.out.println(isNetworkAvailable());
        }
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
        super.onDestroy();
    }

    private boolean isNetworkAvailable(){
        HttpGet httpGet = new HttpGet(url_ping);
        HttpParams httpParameters = new BasicHttpParams();

        int timeoutConnection = 2000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);

        int timeoutSocket = 3000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

        DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
        try{
            httpClient.execute(httpGet);
            mConnectionServiceCallback.hasInternetConnection();
            if(prevState!=true){
                stateChanged(true);
            }
            prevState=true;
            return true;
        }
        catch(ClientProtocolException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        mConnectionServiceCallback.hasNoInternetConnection();
        if(prevState!=false){
            stateChanged(false);
        }
        prevState=false;
        return false;
    }

    /*public boolean isNetworkAvailable(){

        boolean x1 = false;


        try {
            Socket s = new Socket("utcnist.colorado.edu", 37);

            InputStream i = s.getInputStream();

            Scanner scan = new Scanner(i);

            while(scan.hasNextLine()){

                System.out.println(scan.nextLine());
                x1 = true;
            }
        } catch (Exception e) {


            x1 = false;
        }
        if(x1){
            mConnectionServiceCallback.hasInternetConnection();
            return true;
        }else{
            mConnectionServiceCallback.hasNoInternetConnection();
            return false;
        }
    }*/
}