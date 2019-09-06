package com.android.networkscanner;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends AppCompatActivity implements ConnectionService.ConnectionServiceCallback {
    Activity mActivity;
    private Context mContext;
    Button btnClientlist, btnPing, btnTraceroute, btnSpeedTest;
    private String m_Text = "";
    ProgressDialog ringProgressDialog;
    private static final int RESULT_SETTINGS = 1;
    public String ip = "";

    ReceiveMessages myReceiver = null;
    Boolean myReceiverIsRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showUserSettings();
        mActivity = MainActivity.this;
        mContext = this;
        Intent intent = new Intent(this, ConnectionService.class);
        // Interval in seconds
        intent.putExtra(ConnectionService.TAG_INTERVAL, 5);
        // URL to ping
        intent.putExtra(ConnectionService.TAG_URL_PING, "http://www.google.com");
        // Name of the class that is calling this service
        intent.putExtra(ConnectionService.TAG_ACTIVITY_NAME, this.getClass().getName());
        // Starts the service
        startService(intent);


        myReceiver = new ReceiveMessages();
        if (!myReceiverIsRegistered) {
            registerReceiver(myReceiver, new IntentFilter("com.android.networkscanner.netstatus"));
            myReceiverIsRegistered = true;
        }

        btnClientlist = findViewById(R.id.btnList);
        btnPing = findViewById(R.id.btnPing);
        btnTraceroute = findViewById(R.id.btnTracer);


        btnClientlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ClientListActivity.class);
                startActivity(i);
                //finish();
            }
        });

        btnPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Ping");

                // Set up the input
                final EditText input = new EditText(v.getContext());
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(getString(R.string.default_ping));
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        m_Text = input.getText().toString();
                        new ping().execute(m_Text);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                //Intent i = new Intent(MainActivity.this, PingActivity.class);
                //startActivity(i);
                //finish();
            }
        });

        btnTraceroute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Traceroute");

                // Set up the input
                final EditText input = new EditText(v.getContext());
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(getString(R.string.default_ping));
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        m_Text = input.getText().toString();
                        new getData().execute(m_Text);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        btnSpeedTest = findViewById(R.id.btnSpeedTest);
        btnSpeedTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SpeedTestActivity.class));
            }
        });


        String ip = Network.getIPAddress(true);
        String mac = Network.getMACAddress("wlan0");
        ((TextView) findViewById(R.id.tvMyIP)).setText("IP ADDRESS : " + ip);
        ((TextView) findViewById(R.id.tvMyMAC)).setText("MAC ADDRESS : " + mac);

        String deviceName = Settings.System.getString(getContentResolver(), "device_name");
        if (deviceName != null) {
            ((TextView) findViewById(R.id.tvThisDevice)).setText("THIS DEVICE : " + deviceName);
        }

        String connType = getConnectionName();
        if (connType != null) {
            ((TextView) findViewById(R.id.tvConnType)).setText("Connection Type : " + connType);
        }

        checkWifiPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String name = getWifiName();

        if (name != null) {
            ((TextView) findViewById(R.id.tvConnName)).setText("SSID : " + name);
        }
    }

    public String getWifiName() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID();
                }
            }
        }
        return null;
    }

    void checkWifiPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_WIFI_STATE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_WIFI_STATE},
                        100);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }
    }

    String getConnectionName() {
        final ConnectivityManager connMgr = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi.isConnectedOrConnecting()) {
            return "WIFI";
        } else if (mobile.isConnectedOrConnecting()) {
            return "DATA";
        } else {
            return "No Connection";
        }
    }

    @Override
    protected void onDestroy() {
        if (myReceiverIsRegistered) {
            unregisterReceiver(myReceiver);
            myReceiverIsRegistered = false;
        }
        super.onDestroy();
    }

    @Override
    public void hasInternetConnection() {
        // has internet

        /*mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mActivity,"Internet Active!!",Toast.LENGTH_SHORT).show();
            }
        });*/

    }

    @Override
    public void hasNoInternetConnection() {
        // no internet :(
        /*mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mActivity,"No Internet!!",Toast.LENGTH_SHORT).show();
            }
        });*/

    }

    /*@Override
    public void stateChanged(final Boolean state){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,"No Internet!!",Toast.LENGTH_SHORT).show();
            }
        });
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d("Internet Status", state.toString());
                Toast.makeText(mContext,"No Internet!!",Toast.LENGTH_SHORT).show();
                //createNotification(state);
            }
        });
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(MainActivity.this,"No Internet!!",Toast.LENGTH_SHORT).show();
            }
        });
    }*/

    void createNotification(Boolean status) {
        Notification noti = new Notification.Builder(mActivity)
                .setContentTitle("Internet Status!!")
                .setContentText(status.toString()).setSmallIcon(R.drawable.logo).build();
        NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(NOTIFICATION_SERVICE);
        // hide the notification after its selected
        //noti.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(0, noti);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_settings:
                Intent i = new Intent(this, UserSettingActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;

        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                ip = showUserSettings();
                Toast.makeText(this, showUserSettings(), Toast.LENGTH_SHORT).show();
                break;

        }

    }

    public String showUserSettings() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        return sharedPrefs.getString("prefUsername", "null");

        /*StringBuilder builder = new StringBuilder();
        builder.append("\n Username: "
                + sharedPrefs.getString("prefUsername", "NULL"));
        builder.append("\n Send report:"
                + sharedPrefs.getBoolean("prefSendReport", false));
        builder.append("\n Sync Frequency: "
                + sharedPrefs.getString("prefSyncFrequency", "NULL"));
        TextView settingsTextView = (TextView) findViewById(R.id.textUserSettings);
        settingsTextView.setText(builder.toString());*/
    }

    private class getData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            try {
                URL url = new URL("http://" + showUserSettings() + ":8089/traceroute?ip=" + urls[0]);
                URLConnection tc = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        tc.getInputStream()));
                String line;
                String data = "";
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                    data += line + "\n";
                }
                return data;
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return "NULL";
        }

        @Override
        protected void onPreExecute() {
            ringProgressDialog = ProgressDialog.show(MainActivity.this, "Please wait ...", "Getting Data from Server...", true);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            ringProgressDialog.dismiss();
            Intent intent = new Intent(getBaseContext(), TracerouteActivity.class);
            intent.putExtra("data", result);
            startActivity(intent);
            //finish();
        }
    }

    private class ping extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String str = "";
            try {
                Process process = Runtime.getRuntime().exec(
                        "/system/bin/ping -c 8 " + urls[0]);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        process.getInputStream()));
                int i;
                char[] buffer = new char[4096];
                StringBuffer output = new StringBuffer();
                while ((i = reader.read(buffer)) > 0)
                    output.append(buffer, 0, i);
                reader.close();

                // body.append(output.toString()+"\n");
                str = output.toString();
                // Log.d(TAG, str);
            } catch (IOException e) {
                // body.append("Error\n");
                e.printStackTrace();
            }
            ringProgressDialog.dismiss();
            return str;
        }

        @Override
        protected void onPreExecute() {
            ringProgressDialog = ProgressDialog.show(MainActivity.this, "Please wait ...", "Ping in progress...", true);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            ringProgressDialog.dismiss();
            Intent intent = new Intent(getBaseContext(), PingActivity.class);
            intent.putExtra("data", result);
            startActivity(intent);
            //finish();
        }
    }


    public class ReceiveMessages extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean state = intent.getBooleanExtra("NET_STAT", false);
            String type = intent.getStringExtra("NET_TYPE");
            //Toast.makeText(mContext,"INTERNET : "+state.toString(),Toast.LENGTH_SHORT).show();
            createNotification(state);

            updateInfo(state, type);
        }
    }

    private void updateInfo(Boolean state, String type) {
        ((TextView) findViewById(R.id.tvConnStatus)).setText("Connection Status : " + (state ? "Connected" : "Disconnected"));
    }
}
