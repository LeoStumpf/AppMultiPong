package com.Project.App.Multipong;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;

public class BtClientClass extends Thread {

    interface ConnectionCallback {
        void onConnected();
        void onConnectionFailed(String error);
    }

    private final BluetoothSocket socket;
    private final SendReceive sendReceive;
    private final ConnectionCallback callback;
    private static final String TAG = "MultiPong";

    @SuppressWarnings("MissingPermission")
    public BtClientClass(BluetoothAdapter adapter, String remoteMac, SendReceive sendReceive,
                         ConnectionCallback callback) {
        this.sendReceive = sendReceive;
        this.callback    = callback;

        BluetoothSocket tmp = null;
        try {
            BluetoothDevice device = adapter.getRemoteDevice(remoteMac);
            tmp = device.createInsecureRfcommSocketToServiceRecord(BtConstants.MULTIPONG_UUID);
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "BtClientClass: failed to create socket for " + remoteMac, e);
        }
        socket = tmp;
    }

    @Override
    public void run() {
        final Handler main = new Handler(Looper.getMainLooper());
        if (socket == null) {
            main.post(() -> callback.onConnectionFailed("Socket creation failed"));
            return;
        }
        try {
            Log.i(TAG, "BtClientClass: connecting...");
            socket.connect();
            Log.i(TAG, "BtClientClass: connected");
            sendReceive.setSocket(socket);
            if (sendReceive.getState() == Thread.State.NEW) sendReceive.start();
            main.post(callback::onConnected);
        } catch (IOException e) {
            Log.e(TAG, "BtClientClass: connection failed", e);
            try { socket.close(); } catch (IOException ignored) {}
            main.post(() -> callback.onConnectionFailed(e.getMessage()));
        }
    }

    public void cancel() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.e(TAG, "BtClientClass: cancel failed", e);
        }
    }
}
