package com.Project.App.Multipong;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SendReceive extends Thread{
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    static int MESSAGE_READ =1;
    public Handler handler;

    private static final String TAG ="DEBUGINGER";

    public SendReceive(int MESSAGE_READ,Handler handler){
        Log.i("Empfanginger", "SenRecive: New Handler" );
        this.handler = handler;

        this.MESSAGE_READ = MESSAGE_READ;
    }

    public void setSocket(Socket skt){
        socket = skt;
        try{
            Log.i("Empfanginger", "Serversucket: set Socket" );
            inputStream= socket.getInputStream();
            outputStream = socket.getOutputStream();
        }catch (IOException e){
            e.printStackTrace();
        }


    }

    @Override
    public void run(){
        Log.i("Empfanginger", "Serversucket: run" );
        byte[] buffer = new byte[1024];
        int bytes ;

        while(socket != null){
            try {
                bytes = inputStream.read(buffer);
                if(bytes > 0){
                    handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();

            }
        }

    }

    public void write(final byte[] bytes) {
        Log.i("Empfanginger", "Serversucket: Write" );

        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    outputStream.write(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
