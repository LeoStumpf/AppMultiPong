package com.Project.App.Multipong;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendReceive extends Thread {

    private InputStream  inputStream;
    private OutputStream outputStream;
    private volatile boolean running = false;

    // Incomplete message fragment carried over between read() calls
    private final StringBuilder readBuffer = new StringBuilder();

    static int MESSAGE_READ = 1;
    public Handler handler;
    /** Called on the main thread when the connection drops unexpectedly. */
    public Runnable onDisconnect;

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
        onDisconnect = null;   // prevent re-entrant callbacks after intentional close
        running = false;
        try {
            if (inputStream  != null) inputStream.close();
            if (outputStream != null) outputStream.close();
        } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        Log.i(TAG, "SendReceive: run started");
        byte[] buffer = new byte[4096];
        int bytes;

        while (running) {
            try {
                bytes = inputStream.read(buffer);
                if (bytes <= 0) continue;

                // Append the chunk to the carry-over buffer and dispatch complete lines.
                // Each message is delimited by '\n' (added by write()).
                // A single read() may contain 0, 1, or many complete messages.
                readBuffer.append(new String(buffer, 0, bytes));
                int newline;
                while ((newline = readBuffer.indexOf("\n")) >= 0) {
                    String msg = readBuffer.substring(0, newline);
                    readBuffer.delete(0, newline + 1);
                    if (!msg.isEmpty()) {
                        byte[] msgBytes = msg.getBytes();
                        handler.obtainMessage(MESSAGE_READ, msgBytes.length, -1, msgBytes)
                               .sendToTarget();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "SendReceive: read error, stopping", e);
                running = false;
                Runnable cb = onDisconnect;
                if (cb != null) {
                    new Handler(Looper.getMainLooper()).post(cb);
                }
            }
        }
    }

    public void write(final String message) {
        write((message + "\n").getBytes());
    }

    public void write(final byte[] bytes) {
        final OutputStream out = outputStream;
        if (out == null) { Log.w(TAG, "SendReceive: write skipped — no socket"); return; }
        Log.i(TAG, "SendReceive: write " + bytes.length + " bytes");
        new Thread(() -> {
            try {
                out.write(bytes);
            } catch (Exception e) {
                Log.w(TAG, "SendReceive: write error", e);
            }
        }).start();
    }
}
