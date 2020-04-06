package com.Project.App.Multipong;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


public class SelectPeer extends base {
    ListView joinList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        selectPeerO = this;
        setContentView(R.layout.client);

        p2pO.initClient();

        joinList = findViewById(R.id.peerListViewC);
        Log.i("Empfanginger", "MainActivity: button Join");

        joinList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                final WifiP2pDevice device = p2pO.deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.groupOwnerIntent = 0;  //Less probability to become the GO


                config.deviceAddress = device.deviceAddress;


                p2pO.mManager.connect(p2pO.mChanel, config, new WifiP2pManager.ActionListener() {


                    @Override
                    public void onSuccess() {
                        Log.i("Empfanginger", "MainActivity: Connected");
                        Toast.makeText(getApplicationContext(), "connected to" + device.deviceName + "  mac" + device.deviceAddress, Toast.LENGTH_SHORT).show();
                        p2pO.currentMacConnect = device.deviceAddress;
                        p2pO.currentNameConnect = device.deviceName;
                    }

                    @Override
                    public void onFailure(int i) {
                        Log.i("Empfanginger", "MainActivity: Not Connected");
                        Toast.makeText(getApplicationContext(), "not connected", Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });



    }



    public void init(p2pConnection p2pObject){
        Log.i("Empfanginger", "MainActivity: button Join");
        //joinList.setAdapter(p2pObject.adapter);
    }


}
