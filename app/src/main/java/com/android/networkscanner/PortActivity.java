package com.android.networkscanner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class PortActivity extends AppCompatActivity {
    TextView pl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_port);
        pl = (TextView) findViewById(R.id.portList);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String portData = extras.getString("PORT_LIST");
            pl.setText(portData);
            //The key argument here must match that used in the other activity
        }
    }
}
