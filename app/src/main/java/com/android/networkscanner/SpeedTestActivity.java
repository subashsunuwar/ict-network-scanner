package com.android.networkscanner;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class SpeedTestActivity extends AppCompatActivity {

    TextView download, upload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        download = findViewById(R.id.download);
        upload = findViewById(R.id.upload);
    }

    public void startTest(View view) {
        new SpeedTest(false).execute();
        view.setEnabled(false);
    }

    public static String formatFileSize(BigDecimal actual) {
        String hrSize = null;
        long size = actual.longValue();
        double b = size;
        double k = size / 1024.0;
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);

        DecimalFormat dec = new DecimalFormat("0.00");

        if (t > 1) {
            hrSize = dec.format(t).concat(" TB/s");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" GB/s");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" MB/s");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB/s");
        } else {
            hrSize = dec.format(b).concat(" Bit/s");
        }

        return hrSize;
    }

    private class SpeedTest extends AsyncTask<String, Void, String> {
        boolean isUpload;

        public SpeedTest(boolean isUpload) {
            this.isUpload = isUpload;
        }

        private void updateSpeed(final BigDecimal value, final boolean isComplete) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isUpload)
                        upload.setText(formatFileSize(value));
                    else
                        download.setText(formatFileSize(value));
                    if (isComplete)
                        findViewById(R.id.startTestBtn).setEnabled(true);
                }
            });
        }

        @Override
        protected String doInBackground(String... values) {
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();

            // add a listener to wait for speedtest completion and progress
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                @Override
                public void onCompletion(SpeedTestReport report) {
                    // called when download/upload is complete
                    System.out.println("[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
                    System.out.println("[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
                    updateSpeed(report.getTransferRateBit(), isUpload);
                    if (!isUpload) {
                        new SpeedTest(true).execute();
                    }
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // called when a download/upload error occur
                    if (!isUpload) {
                        new SpeedTest(true).execute();
                    } else {
                        updateSpeed(new BigDecimal(0), true);
                    }
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {
                    // called to notify download/upload progress
                    System.out.println("[PROGRESS] progress : " + percent + "%");
                    System.out.println("[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                    System.out.println("[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
                    updateSpeed(report.getTransferRateBit(), false);
                }
            });

            if (isUpload)
                speedTestSocket.startFixedUpload("http://ipv4.ikoula.testdebit.info/", 10000000, 10000);
            else
                speedTestSocket.startFixedDownload("http://ipv4.ikoula.testdebit.info/100M.iso", 10000);

            return null;
        }
    }

}
