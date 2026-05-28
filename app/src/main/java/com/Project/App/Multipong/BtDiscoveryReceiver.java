package com.Project.App.Multipong;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BtDiscoveryReceiver extends BroadcastReceiver {

    interface DiscoveryCallback {
        void onDeviceFound(BluetoothDevice device);
        void onDiscoveryFinished();
    }

    private final DiscoveryCallback callback;
    private static final String TAG = "MultiPong";

    public BtDiscoveryReceiver(DiscoveryCallback callback) {
        this.callback = callback;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
            } else {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            }
            if (device != null) {
                Log.i(TAG, "BtDiscoveryReceiver: found device " + device.getAddress());
                callback.onDeviceFound(device);
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            Log.i(TAG, "BtDiscoveryReceiver: discovery finished");
            callback.onDiscoveryFinished();
        }
    }
}
