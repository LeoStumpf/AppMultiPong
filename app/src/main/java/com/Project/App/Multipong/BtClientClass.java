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
    private final SendReceive     sendReceive;
    private final ConnectionCallback callback;
    private volatile boolean      cancelled = false;
    private static final String   TAG = "MultiPong";

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
            try { socket.close(); } catch (IOException ignored) {}
            if (!cancelled) {
                Log.e(TAG, "BtClientClass: connection failed", e);
                main.post(() -> callback.onConnectionFailed(e.getMessage()));
            } else {
                Log.i(TAG, "BtClientClass: socket closed by cancel, ignoring");
            }
        }
    }

    public void cancel() {
        cancelled = true;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.e(TAG, "BtClientClass: cancel failed", e);
        }
    }
}
