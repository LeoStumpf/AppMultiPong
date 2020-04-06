package com.Project.App.Multipong;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class p2pConnection extends base {

    WifiManager wifimanager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChanel;
    WifiDirectBroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    List<WifiP2pDevice> peers= new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    static  final int MESSAGE_READ =1;
    ServerClass serverClass;
    ClientClass clientClass;
    Context context;
    public SendReceive sendReceive;
    final HashMap<String, String> buddies = new HashMap<String, String>();

    String currentMacConnect;
    String currentNameConnect;
    private static final String TAG ="DEBUGINGER";
    public static boolean IsHost;
    public static boolean IsReady=false;
    int randomNumber;
    ArrayAdapter<String> adapter;
    Boolean ReadyToReceve = false;

    static MainActivity MainO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        p2pO = this;

        // P2p variables Init
        wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChanel = mManager.initialize(this,getMainLooper(),null);
        mReceiver = new WifiDirectBroadcastReceiver(mManager,mChanel,this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        sendReceive = new SendReceive(MESSAGE_READ,handler);

        //Switch to main Activity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }

    public void initServer(){
        WifiP2pServiceInfo wifiP2pServiceInfo;
        mManager.discoverPeers(mChanel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //connectionsStatus.setText("Discovery Started");
                Log.i("Empfanginger", "MainActivity: Discover Peers");
            }


            @Override
            public void onFailure(int i) {
                //connectionsStatus.setText("Discovery start fail");
            }
        });

    }

    @Override
    protected void onResume() {
        Log.i("Empfanginger", "MainActivity:On Resume" );
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);

    }

    @Override
    protected void onDestroy() {
        Log.i("Empfanginger", "MainActivity:On Destroy" );
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }


    public void initClient(){
        mManager.discoverPeers(mChanel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //connectionsStatus.setText("Discovery Started");
            }

            @Override
            public void onFailure(int i) {
                //connectionsStatus.setText("Discovery start fail");
            }
        });

        ReadyToReceve = true;

    }


    public void sendToSendRecive(String input){
        Log.i(TAG, "SendRecive: Sende Nachricht: "+input);
        sendReceive.write(input.getBytes());
        System.gc();
    }


    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.i("Empfanginger", "MainActivity: WifiP2p Manager.ConnectionInfoListener_ " );
            final InetAddress groupOwnwerAndres =wifiP2pInfo.groupOwnerAddress;

            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                Log.i("Empfanginger", "MainActivity: WifiP2p Manager.ConnectionInfoListener_Is group owner " );
                IsHost=true;
                //connectionsStatus.setText("host");
                //sendReceive = new SendReceive(MESSAGE_READ,handler);
                serverClass=new ServerClass(sendReceive);

                serverClass.start();
                Toast.makeText(getApplicationContext(),"host",Toast.LENGTH_SHORT).show();
                // serverClass.run();


            }else if (wifiP2pInfo.groupFormed){
                Log.i("Empfanginger", "MainActivity: WifiP2p Manager.ConnectionInfoListener_ Client" );
                IsHost=false;
                // sendReceive = new SendReceive(MESSAGE_READ,handler);

                //connectionsStatus.setText("client");

                clientClass = new ClientClass(groupOwnwerAndres,sendReceive);
                clientClass.start();
                Toast.makeText(getApplicationContext(),"client",Toast.LENGTH_SHORT).show();


                //Wechsele in Lobby für client

            }

            Handler handler = new Handler();


            handler.postDelayed(new Runnable() {
                public void run() {
                    try{
                        Log.i(TAG, "Temporare nachricht... glaube diser handler hatte alles zerstoert");

                        String snd = "hallole";
                        sendToSendRecive(snd);
                    }catch (Exception e){
                        Log.i(TAG, "Temporare nachricht... glaube diser handler hatte alles zerstoert_FEHLER");
                        Toast.makeText(getApplicationContext(),"invio fallito",Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }, 5000);


        }
    };


    WifiP2pManager.PeerListListener peerListListener= new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            Log.i("Empfanginger", "MainActivity: WifiP2p Manager.peerlistlisterner_ PeerList" );
            if(!peerList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray= new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;

                Log.i("Empfanginger", "MainActivity: WifiP2p Managerpeerlistlisterner__ GetDevice List" );
                for(WifiP2pDevice device : peerList.getDeviceList()){

                    deviceNameArray[index]= device.deviceAddress+"  "+device.deviceName;
                    deviceArray[index]=device;
                    Log.i("Empfanginger", "MainActivity: WifiP2p.peerlistlisterner_ DeviceName Arry"+deviceNameArray[index].toString() +"  "+deviceArray[index].toString());


                    index++;
                }
                adapter = new ArrayAdapter<String> (getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        Log.i("Empfanginger", "MainActivity: WifiP2p Manager.peerlistlisterner__ ArryAdapter" );
                        View view = super.getView(position, convertView, parent);
                        TextView text = view.findViewById(android.R.id.text1);
                        text.setTextColor(Color.BLACK);
                        return view;
                    }
                };

                //listView.setAdapter(adapter);
                //if(HasClicktJoin)joinList.setAdapter(adapter);
                if(ReadyToReceve)selectPeerO.joinList.setAdapter(adapter);


            }
            if (peers.size()==0){
                Toast.makeText(context.getApplicationContext(),"no device found", Toast.LENGTH_SHORT).show();
            }




        }
    };

    private void startRegistration() {
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("listenport", String.valueOf(8888));
        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChanel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
            }

            @Override
            public void onFailure(int arg0) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });
    }






    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {


                case MESSAGE_READ:
                    //Nachrichten Empfangen
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    Log.i(TAG, "Handler_Message_Read: Nachricht Empfangen: " + tempMsg);
                    filterservice(tempMsg);
            }
            return true;
        }
    });

    public void filterservice(String nachricht) {

        Log.i(TAG, "Filterservice: Neue Nachricht Empfangen: " + nachricht);

        //Filterservice für globale übergabe parameter:

        //Filter für All Start
        if (nachricht.equals("All_start")) {
            Log.i(TAG, "Filterservice_Parameter: All_start wurde empfangen");

        }
        //Filter für errore
        else if (nachricht.equals("errore")) {
            Log.i(TAG, "Filterservice_Parameter: errore wurde empfangen");

        }
        //Filter für Letsegooo
        else if (nachricht.equals("Letsegooo")) {
            Log.i(TAG, "Filterservice_Parameter: Letsegooo wurde empfangen");
            IsReady = true;

        }

        //Wenn client presses is ready
        else if (nachricht.equals("ClientIsReady")) {
            Log.i(TAG, "Filterservice_Parameter: Client Ready empfangen");


        }

        //Filter ob was auch immer
        else if(nachricht.equals("hallole")){
            Log.i(TAG, "Filterservice_Parameter: hallole aus dem komischen filter kahm an");
            WifiP2pConfig config = new WifiP2pConfig();

        }
        //Filter für Comand Parameter
        else{

            //Bilden eines Teilstrings der den Command enthält
            //Alle Commands müssen 6 zeichen enthalten
            String command;
            command=nachricht.substring(0,6);
            Log.i(TAG, "Filterservice: Command wurde gebildet: "+command);

            //Filterservice für alle commands

            //Filterservice für GtwMsg
            if(command.equals("GtwMsg")) {
                Log.i(TAG, "Filterservice_Command: GateWayMassage empfangen");

                //Ermitteln der Zielhandyposition
                int ziel_handy_pos=Integer.parseInt(nachricht.substring(6,nachricht.lastIndexOf("*")));

                if(ziel_handy_pos<1){
                    GameView.circle.Point_Scored('r');
                }
                if(ziel_handy_pos>GameView.amountPlayers){
                    GameView.circle.Point_Scored('l');
                }
                if(ziel_handy_pos==GameView.thisScreen.HandyPosition){
                    GameView.circle.CurrentHandy = GameView.thisScreen.HandyPosition;
                    //GameView.circle.xpos=Float.parseFloat(nachricht.substring(nachricht.lastIndexOf("*")+1,nachricht.lastIndexOf(">")));
                    GameView.circle.ypos=Float.parseFloat(nachricht.substring(nachricht.lastIndexOf(">")+1,nachricht.lastIndexOf("<"))) * GameView.thisScreen.adjustedHeight;
                    GameView.circle.standardxspeed=Float.parseFloat(nachricht.substring(nachricht.lastIndexOf("<")+1,nachricht.lastIndexOf("#")));
                    GameView.circle.standardyspeed=Float.parseFloat(nachricht.substring(nachricht.lastIndexOf("#")+1,nachricht.lastIndexOf("~")));
                    GameView.circle.standardmaxyspeed=Float.parseFloat(nachricht.substring(nachricht.lastIndexOf("~")+1));
                    if(GameView.circle.standardxspeed < 0) GameView.circle.xpos = GameView.thisScreen.width;
                    else GameView.circle.xpos = 0;

                }else{
                    sendToSendRecive(nachricht);
                }

                //Filterservice für NewBallMessage
            }if(command.equals("NBAMsg")){
                Log.i(TAG, "Filterservice_Command: New Ball Massage empfangen");
                int Positionee =Integer.parseInt(nachricht.substring(6));
                if(GameView.thisScreen.HandyPosition == Positionee){
                    GameView.circle.CurrentHandy = GameView.thisScreen.HandyPosition;
                    GameView.circle.xpos = 450;
                    GameView.circle.ypos = 900;
                    GameView.circle.standardyspeed = 3;
                    GameView.circle.standardradius = 10;
                    if(GameView.thisScreen.HandyPosition == 1) GameView.circle.standardxspeed = 6;
                    if(GameView.thisScreen.HandyPosition == GameView.amountPlayers) GameView.circle.standardxspeed = -6;
                }
            }

            //Filterservice SoftwareAndroid_Dimension
            if(command.equals("Sa_Dim")){
                Log.i(TAG, "Filterservice_Command: SoftwareAndroid_Dimension empfangen");

                String Sa_width=nachricht.substring(6,nachricht.lastIndexOf(">"));
                String Sa_height=nachricht.substring(nachricht.lastIndexOf(">")+1,nachricht.lastIndexOf("#"));
                String Sa_density=nachricht.substring(nachricht.lastIndexOf("#")+1,nachricht.lastIndexOf("<"));
                String Sa_position=nachricht.substring(nachricht.lastIndexOf("<")+1);
                Log.i(TAG, "Filterservice_Command: SoftwareAndroid_Dimension: Sa_width "+Sa_width);
                Log.i(TAG, "Filterservice_Command: SoftwareAndroid_Dimension: Sa_height "+Sa_height);
                Log.i(TAG, "Filterservice_Command: SoftwareAndroid_Dimension: Sa_density "+Sa_density);
                Log.i(TAG, "Filterservice_Command: SoftwareAndroid_Dimension: Sa_position " +Sa_position);
                GameView.screen[Integer.parseInt(Sa_position) - 1].width = Float.parseFloat(Sa_width);
                GameView.screen[Integer.parseInt(Sa_position) - 1].height = Float.parseFloat(Sa_height);
                GameView.screen[Integer.parseInt(Sa_position) - 1].density = Integer.parseInt(Sa_density);
                GameView.screen[Integer.parseInt(Sa_position) - 1].HandyPosition = Integer.parseInt(Sa_position);
            }
        }

    }

}
