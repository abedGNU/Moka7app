package com.abed.moka7app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.abed.moka7app.moka7.*;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(notifyTextReceiver,
                new IntentFilter("notifyText"));

    }

    private BroadcastReceiver notifyTextReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast t = Toast.makeText(getApplicationContext(), intent.getStringExtra("Message"), Toast.LENGTH_LONG);
            t.show();
        }
    };

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
        }
    }

}