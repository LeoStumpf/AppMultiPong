package com.Project.App.Multipong;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;
    private final MainActivity mActivity;
    public WifiP2pDevice myDevice;

    private static final String TAG = "MultiPong";

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity) {
        this.mManager  = manager;
        this.mChannel  = channel;
        this.mActivity = activity;
    }

    @SuppressWarnings("deprecation")  // NetworkInfo deprecated in API 29 but required for WiFi P2P
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "WifiDirectBroadcastReceiver: " + action);

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(context, "WiFi Direct enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "WiFi Direct disabled", Toast.LENGTH_SHORT).show();
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (mManager != null) {
                mManager.requestPeers(mChannel, mActivity.peerListListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mManager == null) return;

            NetworkInfo networkInfo = getNetworkInfoCompat(intent);
            if (networkInfo != null && networkInfo.isConnected()) {
                mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            myDevice = getWifiP2pDeviceCompat(intent);
        }
    }

    @SuppressWarnings("deprecation")
    private NetworkInfo getNetworkInfoCompat(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo.class);
        } else {
            return intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        }
    }

    @SuppressWarnings("deprecation")
    private WifiP2pDevice getWifiP2pDeviceCompat(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice.class);
        } else {
            return intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        }
    }
}
