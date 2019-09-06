package com.android.networkscanner;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class ClientListActivity extends AppCompatActivity {
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    ListView moreListView;
    ProgressDialog ringProgressDialog;
    Button refresh;
    ListView lv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_list);
        //setListAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, this.fetchTwitterPublicTimeline()));
        //System.out.println(fetchClientList());

        lv = (ListView) findViewById(android.R.id.list);
        new HttpAsyncTask().execute("http://"+showUserSettings()+":8089/devices");
        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        //setListAdapter(adapter);
        lv.setAdapter(adapter);
        //Toast.makeText(this,showUserSettings(),Toast.LENGTH_SHORT).show();
        moreListView = (ListView)findViewById(android.R.id.list);
        moreListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                // TODO Auto-generated method stub
                Log.d("############","Items " +  listItems.get(arg2) );
                ProgressDialog dialog = ProgressDialog.show(ClientListActivity.this, "",
                        "Loading. Please wait...", true);
                new getData().execute("http://"+showUserSettings()+":8089/portscan?ip="+listItems.get(arg2));

            }

        });

        refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new HttpAsyncTask().execute("http://"+showUserSettings()+":8089/refresh");
            }
        });
    }

    public String showUserSettings() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        return sharedPrefs.getString("prefUsername","null");
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            try {
                URL twitter = new URL(urls[0]);
                URLConnection tc = twitter.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        tc.getInputStream()));
                listItems.clear();
                String line;
                while ((line = in.readLine()) != null) {
                    JSONArray ja = new JSONArray(line);

                    for (int i = 0; i < ja.length(); i++) {
                        JSONObject jo = (JSONObject) ja.get(i);
                        listItems.add(jo.getString("ip"));
                    }
                }
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println(listItems);
            return "A";
        }
        @Override
        protected void onPreExecute() {
            ringProgressDialog = ProgressDialog.show(ClientListActivity.this, "Please wait ...", "Getting Data from Server...", true);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            ringProgressDialog.dismiss();
            adapter.notifyDataSetChanged();
            Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();
        }
    }

    private class getData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            try {
                URL url = new URL(urls[0]);
                URLConnection tc = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        tc.getInputStream()));
                String line;
                String data="";
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                    data+=line+"\n";
                }
                return data.toString();
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
            ringProgressDialog = ProgressDialog.show(ClientListActivity.this, "Please wait ...", "Getting Data from Server...", true);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            ringProgressDialog.dismiss();
            Intent intent = new Intent(getBaseContext(), PortActivity.class);
            intent.putExtra("PORT_LIST", result);
            startActivity(intent);
            finish();
        }
    }
}
