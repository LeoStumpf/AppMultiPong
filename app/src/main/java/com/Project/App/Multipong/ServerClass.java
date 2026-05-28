package com.Project.App.Multipong;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerClass extends Thread {

    Socket socket;
    ServerSocket serverSocket;
    SendReceive sendReceive;

    private static final String TAG = "MultiPong";

    public ServerClass(SendReceive sendReceive) {
        this.sendReceive = sendReceive;
    }

    @Override
    public void run() {
        try {
            Log.i(TAG, "ServerClass: listening on port 8888");
            serverSocket = new ServerSocket(8888);
            socket = serverSocket.accept();
            sendReceive.setSocket(socket);
            if (sendReceive.getState() == Thread.State.NEW) {
                sendReceive.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
