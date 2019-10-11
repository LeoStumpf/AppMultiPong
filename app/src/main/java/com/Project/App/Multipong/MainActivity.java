package com.Project.App.Multipong;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class MainActivity extends AppCompatActivity {
    int backView = R.layout.activity_main;

    Button btnOnOff, btnGameStart, btnClientReady;
    ImageButton  buttonserver, buttonjoin;
    ListView  listView, joinList, listConnected;
    TextView connectionsStatus;
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
    public static SendReceive sendReceive;
    final HashMap<String, String> buddies = new HashMap<String, String>();

    String currentMacConnect;
    String currentNameConnect;
    private static final String TAG ="DEBUGINGER";
    public static boolean IsHost;
    public static boolean IsReady=false;
    boolean HasClicktJoin=false;
    public static int x_display;
    public static int y_display;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "OnCreate: Neue Instanz der App Geöffnet");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        backView = R.layout.activity_main;
        setContentView(R.layout.activity_main);

        sendReceive = new SendReceive(MESSAGE_READ,handler);
        initialWork();
        checkPermissions();



    }




    void get_display_dim(){
        int zahl=0;
        View v =  findViewById(R.id.messureBild);
        x_display = v.getWidth();
        y_display = v.getHeight();
        Log.i(TAG, "GameActivity: neue groese x " + x_display + "     y: " + y_display);
    }


    //-------------------------Handler der die Nachrichten Empfängt----------------------------------------
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
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
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

//neu eingefügt zus service discovery

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










    public static void sendToSendRecive(String input){
        Log.i(TAG, "SendRecive: Sende Nachricht: "+input);
        String temp =new String();
        sendReceive.write(input.getBytes());
        temp=null;
        System.gc();


    }

    private void exqListener() {

        buttonjoin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                get_display_dim();

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

                //neu
/*
                WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
                mManager.addServiceRequest(mChanel,
                        serviceRequest,
                        new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                // Success!
                            }

                            @Override
                            public void onFailure(int code) {
                                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                            }
                        });

                mManager.discoverServices(mChanel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        // Success!
                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                            Log.d(TAG, "P2P isn't supported on this device.");

                    }}});
*/


                setContentView(R.layout.client);
                HasClicktJoin = true;
                joinList = findViewById(R.id.peerListViewC);
                Log.i("Empfanginger", "MainActivity: button Join");

                joinList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        final WifiP2pDevice device = deviceArray[i];
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.groupOwnerIntent = 0;  //Less probability to become the GO


                        config.deviceAddress = device.deviceAddress;


                        mManager.connect(mChanel, config, new WifiP2pManager.ActionListener() {


                            @Override
                            public void onSuccess() {
                                Log.i("Empfanginger", "MainActivity: Connected");
                                Toast.makeText(getApplicationContext(), "connected to" + device.deviceName + "  mac" + device.deviceAddress, Toast.LENGTH_SHORT).show();
                                currentMacConnect = device.deviceAddress;
                                currentNameConnect = device.deviceName;
                            }

                            @Override
                            public void onFailure(int i) {
                                Log.i("Empfanginger", "MainActivity: Not Connected");
                                Toast.makeText(getApplicationContext(), "not connected", Toast.LENGTH_SHORT).show();
                            }
                        });


                    }
                });


            }
        });


        buttonserver.setOnClickListener(new OnClickListener() {


            @Override
            public void onClick(View view) {
                Log.i("Empfanginger", "MainActivity: Button server");

                WifiP2pServiceInfo wifiP2pServiceInfo;


                get_display_dim();

/*

                mManager.createGroup(mChanel, new WifiP2pManager.ActionListener(){
                    @Override
                    public void onSuccess() {
                        connectionsStatus.setText("Host Started");

                    }

                    @Override
                    public void onFailure(int i) {
                        connectionsStatus.setText("fail");
                    }
                });*/
//alternative für die Uni deaktiviert aber wichtig

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

                setContentView(R.layout.host);

                btnGameStart = findViewById(R.id.game_Start);
                listConnected = findViewById(R.id.Player_connected);
                WifiP2pConfig config = new WifiP2pConfig();
                config.groupOwnerIntent = 15;

                btnGameStart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String msg = "All_start";
                        // String msgcript = cript.encript(msg); //Verschlüsselt ie nachricht
                        String msgcript = msg;
                        sendToSendRecive(msgcript); //senden

                        Intent intent = new Intent(MainActivity.this, GameActivity.class);
                        startActivity(intent);

                    }
                });

            }
        });
    }




        //-----------------------------START CONNECTION----------------------------------------------------



    private void initialWork() {

        btnOnOff = findViewById(R.id.onOff);

        buttonserver = findViewById(R.id.server);
        buttonjoin = findViewById(R.id.join);



        wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChanel = mManager.initialize(this,getMainLooper(),null);
        mReceiver = new WifiDirectBroadcastReceiver(mManager,mChanel,this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }




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





                ArrayAdapter<String> adapter = new ArrayAdapter<String> (getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray){
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
                if(HasClicktJoin)joinList.setAdapter(adapter);
            }
            if (peers.size()==0){
                Toast.makeText(getApplicationContext(),"no device found",Toast.LENGTH_SHORT).show();
            }




        }
    };


    //---------------------------------------LISTENER DELLA CONNESSIONE----------------------------------------------------------
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

                //setContentView(R.layout.host);
                setContentView(R.layout.client_lobby);



            }else if (wifiP2pInfo.groupFormed){
                Log.i("Empfanginger", "MainActivity: WifiP2p Manager.ConnectionInfoListener_ Client" );
                IsHost=false;
                // sendReceive = new SendReceive(MESSAGE_READ,handler);

                //connectionsStatus.setText("client");

                clientClass = new ClientClass(groupOwnwerAndres,sendReceive);
                clientClass.start();
                Toast.makeText(getApplicationContext(),"client",Toast.LENGTH_SHORT).show();


                //Wechsele in Lobby für client
                setContentView(R.layout.client_lobby);

                btnClientReady=findViewById(R.id.Lobby_Switch_Ready);

                btnClientReady.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sendToSendRecive("ClientIsReady");
                    }
                });
            }

            Handler handler = new Handler();

//Dieser kauz hatt alles zerstört glaube ich
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



    @Override
    protected void onResume() {
        Log.i("Empfanginger", "MainActivity:On Resume" );
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);

    }
    @Override
    protected void onPause() {
        Log.i("Empfanginger", "MainActivity:On Pause" );
        super.onPause();
        unregisterReceiver(mReceiver);
    }




    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE};


    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);

            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "android.permission.ACCESS_COARSE_LOCATION" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted

                initialWork();
                exqListener();
                break;
        }
    }
}

