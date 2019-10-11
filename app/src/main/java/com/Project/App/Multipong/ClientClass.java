package com.Project.App.Multipong;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;


    public class ClientClass extends Thread{
        Socket socket;
        String hostAdd;
        SendReceive sendReceive;

        @Override
        public void run() {
            try{
                socket.connect(new InetSocketAddress(hostAdd,8888),500);
                Log.i("Empfanginger", "Clientclass: run" );
                sendReceive.setSocket(socket);
                if (sendReceive.getState() == Thread.State.NEW){
                    sendReceive.start();
                };

            }catch (IOException e){
                e.printStackTrace();
            }
        }
        public ClientClass(InetAddress hostAddress,SendReceive sendReceive){
            this.sendReceive = sendReceive;
            hostAdd = hostAddress.getHostAddress();
            socket= new Socket();
        }
    }

