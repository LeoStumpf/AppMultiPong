package com.Project.App.Multipong;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientClass extends Thread {

    Socket socket;
    String hostAddress;
    SendReceive sendReceive;

    private static final String TAG = "MultiPong";

    public ClientClass(InetAddress hostAddr, SendReceive sendReceive) {
        this.sendReceive = sendReceive;
        this.hostAddress = hostAddr.getHostAddress();
        socket = new Socket();
    }

    @Override
    public void run() {
        try {
            Log.i(TAG, "ClientClass: connecting to " + hostAddress + ":8888");
            socket.connect(new InetSocketAddress(hostAddress, 8888), 500);
            sendReceive.setSocket(socket);
            if (sendReceive.getState() == Thread.State.NEW) {
                sendReceive.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
