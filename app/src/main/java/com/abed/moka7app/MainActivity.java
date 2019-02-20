package com.abed.moka7app;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.abed.moka7app.moka7.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    static final String IP_ADDRESS = "IPADDRESS";
    static String IpAddr; //String IpAddr = "192.168.0.100";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(notifyTextReceiver,
                new IntentFilter("notifyText"));

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        String defaultIpAddr = "192.168.0.100";
        IpAddr = settings.getString(IP_ADDRESS, defaultIpAddr);
        ((TextView)findViewById(R.id.textViewIPaddress)).setText(IpAddr);

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        String defaultIpAddr = "192.168.0.100";
        IpAddr = settings.getString(IP_ADDRESS, defaultIpAddr);
        ((TextView)findViewById(R.id.textViewIPaddress)).setText(IpAddr);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(IP_ADDRESS, IpAddr);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            Toast.makeText(this, "Settings", Toast.LENGTH_LONG).show();
        } else {
            return super.onContextItemSelected(item);
        }
        return true;
    }

    private BroadcastReceiver notifyTextReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast t = Toast.makeText(getApplicationContext(), intent.getStringExtra("Message"), Toast.LENGTH_LONG);
            t.show();
        }
    };

    public void setIP(View v)
    {
        IpAddr = ((TextView)findViewById(R.id.textViewIPaddress)).getText().toString();
    }

    public void readDB(View v)
    {
        PlcConnection p = new PlcConnection();
        new Thread(p).start();
    }

    private class PlcConnection implements Runnable {
        private final S7Client Client;

        public PlcConnection() {
            Client = new S7Client();
        }

        @Override
        public void run() {
            String IpAddr = "192.168.0.100";
            int res;
            int selectedArea = S7.S7AreaDB;
            int dBNumber = 100;
            int offset = 0;
            int length =4;
            int rack = 0;
            int slot = 0;

            Client.SetConnectionType(S7.OP);
            res=Client.ConnectTo(IpAddr, rack, slot);

            if (res==0){
                byte[] data = new byte[4];
                res = Client.ReadArea(selectedArea, dBNumber, offset, length, data);
                if (res==0) {
                    int val = S7.GetDIntAt(data,0);
                    Intent i = new Intent("notifyText");
                    i.putExtra("Message",String.format("Value DB %d",val));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                }else {
                    Intent i = new Intent("notifyText");
                    i.putExtra("Message", String.format("Read error: %d - %s", res, S7Client.ErrorText(res)));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                }
            }
        } // end run
    }

    public void scheduleJob(View v) {
        ComponentName componentName = new ComponentName(this, PlcJobService.class);
        JobInfo info = new JobInfo.Builder(123, componentName)
                .setRequiresCharging(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled");
        } else {
            Log.d(TAG, "Job scheduling failed");
        }
    }

    public void cancelJob(View v) {
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.cancel(123);
        Log.d(TAG, "Job cancelled");
    }


}