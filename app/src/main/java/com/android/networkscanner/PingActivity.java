package com.android.networkscanner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class PingActivity extends AppCompatActivity {

    TextView result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);
        result = (TextView) findViewById(R.id.pingResult);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String portData = extras.getString("data");
            result.setText(portData);
        }

    }
}