package com.Project.App.Multipong;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendReceive extends Thread {

    private InputStream  inputStream;
    private OutputStream outputStream;
    private volatile boolean running = false;

    static int MESSAGE_READ = 1;
    public Handler handler;

    private static final String TAG = "MultiPong";

    public SendReceive(int messageRead, Handler handler) {
        Log.i(TAG, "SendReceive: created");
        this.handler      = handler;
        this.MESSAGE_READ = messageRead;
    }

    /** Assign a Bluetooth socket as the communication channel. */
    public void setSocket(BluetoothSocket btSocket) {
        try {
            Log.i(TAG, "SendReceive: BT socket assigned");
            inputStream  = btSocket.getInputStream();
            outputStream = btSocket.getOutputStream();
            running = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        running = false;
    }

    @Override
    public void run() {
        Log.i(TAG, "SendReceive: run started");
        byte[] buffer = new byte[1024];
        int bytes;

        while (running) {
            try {
                bytes = inputStream.read(buffer);
                if (bytes > 0) {
                    // Deliver a copy of the buffer — the same byte[] is reused each loop
                    byte[] copy = new byte[bytes];
                    System.arraycopy(buffer, 0, copy, 0, bytes);
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, copy).sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "SendReceive: read error, stopping", e);
                running = false;
            }
        }
    }

    public void write(final byte[] bytes) {
        Log.i(TAG, "SendReceive: write " + bytes.length + " bytes");
        new Thread(() -> {
            try {
                outputStream.write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
