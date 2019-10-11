package com.Project.App.Multipong;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private  WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;
    public WifiP2pDevice myDevice;

    public WifiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, MainActivity mActivity){
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mActivity = mActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Empfanginger", "WifiDirectBroadcastreciver: On Recive" );
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.i("Empfanginger", "WifiDirectBroadcastreciver: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION" );
             myDevice =intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        }

        if( WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            Log.i("Empfanginger", "WifiDirectBroadcastreciver: WIFI_P2P_STATE_CHANGED_ACTION" );
            // do something
            int state= intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                Toast.makeText(context,"wifi is on",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context,"wifi is off",Toast.LENGTH_SHORT).show();

            }
        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            Log.i("Empfanginger", "WifiDirectBroadcastreciver: WIFI_P2P_PEERS_CHANGED_ACTION" );
            //do somthing else
            if(mManager != null){

                mManager.requestPeers(mChannel,mActivity.peerListListener);
            }



            //dooo
            // WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            Log.i("Empfanginger", "WifiDirectBroadcastreciver: WIFI_P2P_CONNECTION_CHANGED_ACTION" );
            if(mManager == null) {
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()){

                mManager.requestConnectionInfo(mChannel,mActivity.connectionInfoListener);

            }else{
                //mActivity.connectionsStatus.setText("it's a disconect");
            }

        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            Log.i("Empfanginger", "WifiDirectBroadcastreciver: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION" );
            //do something
        }

        if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.i("Empfanginger", "WifiDirectBroadcastreciver: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION" );
             myDevice =(WifiP2pDevice)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        }

    }














}
