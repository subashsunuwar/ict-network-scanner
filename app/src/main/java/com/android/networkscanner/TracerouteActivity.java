package com.android.networkscanner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class TracerouteActivity extends AppCompatActivity {
    TextView tv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traceroute);
        tv = (TextView) findViewById(R.id.tracerouteResult);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String portData = extras.getString("data");
            tv.setText(portData);
        }
    }
}
