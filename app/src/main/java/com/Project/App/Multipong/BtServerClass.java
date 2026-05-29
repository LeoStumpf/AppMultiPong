package com.Project.App.Multipong;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;

public class BtServerClass extends Thread {

    interface ConnectionCallback {
        void onConnected();
        void onConnectionFailed(String error);
    }

    private BluetoothServerSocket serverSocket;
    private final SendReceive     sendReceive;
    private final ConnectionCallback callback;
    private volatile boolean      cancelled = false;
    private static final String   TAG = "MultiPong";

    @SuppressWarnings("MissingPermission")
    public BtServerClass(BluetoothAdapter adapter, SendReceive sendReceive,
                         ConnectionCallback callback) {
        this.sendReceive = sendReceive;
        this.callback    = callback;
        try {
            serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(
                    "MultiPong", BtConstants.MULTIPONG_UUID);
            Log.i(TAG, "BtServerClass: server socket opened");
        } catch (IOException e) {
            Log.e(TAG, "BtServerClass: failed to open server socket", e);
        }
    }

    @Override
    public void run() {
        final Handler main = new Handler(Looper.getMainLooper());
        if (serverSocket == null) {
            main.post(() -> callback.onConnectionFailed("Server socket unavailable"));
            return;
        }
        try {
            Log.i(TAG, "BtServerClass: waiting for client");
            BluetoothSocket socket = serverSocket.accept();
            Log.i(TAG, "BtServerClass: client connected");
            sendReceive.setSocket(socket);
            if (sendReceive.getState() == Thread.State.NEW) sendReceive.start();
            main.post(callback::onConnected);
        } catch (IOException e) {
            if (!cancelled) {
                Log.e(TAG, "BtServerClass: accept failed", e);
                main.post(() -> callback.onConnectionFailed(e.getMessage()));
            } else {
                Log.i(TAG, "BtServerClass: socket closed by cancel, ignoring");
            }
        } finally {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
    }

    public void cancel() {
        cancelled = true;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "BtServerClass: cancel failed", e);
        }
    }
}
