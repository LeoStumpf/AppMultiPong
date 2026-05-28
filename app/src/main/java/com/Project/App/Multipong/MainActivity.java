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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button btnOnOff, btnGameStart, btnClientReady;
    ImageButton buttonserver, buttonjoin;
    ListView listView, joinList, listConnected;
    TextView connectionsStatus;
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    WifiDirectBroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    List<WifiP2pDevice> peers = new ArrayList<>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    static final int MESSAGE_READ = 1;
    ServerClass serverClass;
    ClientClass clientClass;
    public static SendReceive sendReceive;
    final HashMap<String, String> buddies = new HashMap<>();

    String currentMacConnect;
    String currentNameConnect;
    private static final String TAG = "MultiPong";
    public static boolean IsHost;
    public static boolean IsReady = false;
    boolean hasClickedJoin = false;
    public static int x_display;
    public static int y_display;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate: app started");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        // Enable immersive fullscreen (replaces deprecated setSystemUiVisibility)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        sendReceive = new SendReceive(MESSAGE_READ, handler);
        initialWork();
        checkPermissions();
    }

    void getDisplayDimensions() {
        View v = findViewById(R.id.messureBild);
        x_display = v.getWidth();
        y_display = v.getHeight();
        Log.i(TAG, "getDisplayDimensions: x=" + x_display + " y=" + y_display);
    }

    // Receives messages from the SendReceive background thread
    Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MESSAGE_READ) {
                byte[] readBuf = (byte[]) msg.obj;
                String message = new String(readBuf, 0, msg.arg1);
                Log.i(TAG, "handler: received: " + message);
                filterservice(message);
            }
            return true;
        }
    });

    public void filterservice(String message) {
        Log.i(TAG, "filterservice: " + message);

        if (message.equals("All_start")) {
            Log.i(TAG, "filterservice: All_start → launching game");
            startActivity(new Intent(MainActivity.this, GameActivity.class));

        } else if (message.equals("errore")) {
            Log.i(TAG, "filterservice: error signal received");

        } else if (message.equals("Letsegooo")) {
            Log.i(TAG, "filterservice: Letsegooo → marking ready");
            IsReady = true;

        } else if (message.equals("ClientIsReady")) {
            Log.i(TAG, "filterservice: client ready");

        } else if (message.equals("hallole")) {
            Log.i(TAG, "filterservice: initial handshake received");

        } else {
            // All protocol commands are exactly 6 characters
            String command = message.substring(0, 6);
            Log.i(TAG, "filterservice: command=" + command);

            if (command.equals("GtwMsg")) {
                Log.i(TAG, "filterservice: GatewayMessage");
                int targetPos = Integer.parseInt(message.substring(6, message.lastIndexOf("*")));

                if (targetPos < 1) {
                    GameView.circle.Point_Scored('r');
                }
                if (targetPos > GameView.amountPlayers) {
                    GameView.circle.Point_Scored('l');
                }
                if (targetPos == GameView.thisScreen.HandyPosition) {
                    GameView.circle.CurrentHandy = GameView.thisScreen.HandyPosition;
                    GameView.circle.ypos = Float.parseFloat(
                            message.substring(message.lastIndexOf(">") + 1, message.lastIndexOf("<")))
                            * GameView.thisScreen.adjustedHeight;
                    GameView.circle.standardxspeed = Float.parseFloat(
                            message.substring(message.lastIndexOf("<") + 1, message.lastIndexOf("#")));
                    GameView.circle.standardyspeed = Float.parseFloat(
                            message.substring(message.lastIndexOf("#") + 1, message.lastIndexOf("~")));
                    GameView.circle.standardmaxyspeed = Float.parseFloat(
                            message.substring(message.lastIndexOf("~") + 1));
                    if (GameView.circle.standardxspeed < 0)
                        GameView.circle.xpos = GameView.thisScreen.width;
                    else
                        GameView.circle.xpos = 0;
                } else {
                    sendToSendRecive(message);
                }
            }

            if (command.equals("NBAMsg")) {
                Log.i(TAG, "filterservice: NewBallMessage");
                int targetPos = Integer.parseInt(message.substring(6));
                if (GameView.thisScreen.HandyPosition == targetPos) {
                    GameView.circle.CurrentHandy = GameView.thisScreen.HandyPosition;
                    GameView.circle.xpos = 450;
                    GameView.circle.ypos = 900;
                    GameView.circle.standardyspeed = 3;
                    GameView.circle.standardradius = 10;
                    if (GameView.thisScreen.HandyPosition == 1)
                        GameView.circle.standardxspeed = 6;
                    if (GameView.thisScreen.HandyPosition == GameView.amountPlayers)
                        GameView.circle.standardxspeed = -6;
                }
            }

            if (command.equals("Sa_Dim")) {
                Log.i(TAG, "filterservice: ScreenDimensions");
                String saWidth   = message.substring(6, message.lastIndexOf(">"));
                String saHeight  = message.substring(message.lastIndexOf(">") + 1, message.lastIndexOf("#"));
                String saDensity = message.substring(message.lastIndexOf("#") + 1, message.lastIndexOf("<"));
                String saPos     = message.substring(message.lastIndexOf("<") + 1);
                int pos = Integer.parseInt(saPos);
                GameView.screen[pos - 1].width       = Float.parseFloat(saWidth);
                GameView.screen[pos - 1].height      = Float.parseFloat(saHeight);
                GameView.screen[pos - 1].density     = Integer.parseInt(saDensity);
                GameView.screen[pos - 1].HandyPosition = pos;
            }
        }
    }

    private void startRegistration() {
        Map<String, String> record = new HashMap<>();
        record.put("listenport", String.valueOf(8888));
        record.put("buddyname", "Player" + (int) (Math.random() * 1000));
        record.put("available", "visible");

        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {}

            @Override
            public void onFailure(int arg0) {}
        });
    }

    public static void sendToSendRecive(String input) {
        Log.i(TAG, "sendToSendRecive: " + input);
        sendReceive.write(input.getBytes());
    }

    private void exqListener() {

        buttonjoin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getDisplayDimensions();

                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onFailure(int i) {}
                });

                setContentView(R.layout.client);
                hasClickedJoin = true;
                joinList = findViewById(R.id.peerListViewC);
                Log.i(TAG, "exqListener: join pressed");

                joinList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        final WifiP2pDevice device = deviceArray[i];
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.groupOwnerIntent = 0;  // Low priority = client role
                        config.deviceAddress = device.deviceAddress;

                        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.i(TAG, "exqListener: connected to " + device.deviceName);
                                Toast.makeText(getApplicationContext(),
                                        "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                                currentMacConnect = device.deviceAddress;
                                currentNameConnect = device.deviceName;
                            }

                            @Override
                            public void onFailure(int i) {
                                Log.i(TAG, "exqListener: connection failed");
                                Toast.makeText(getApplicationContext(),
                                        "Connection failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        buttonserver.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "exqListener: host pressed");
                getDisplayDimensions();

                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "exqListener: peer discovery started");
                    }

                    @Override
                    public void onFailure(int i) {}
                });

                setContentView(R.layout.host);
                btnGameStart = findViewById(R.id.game_Start);
                listConnected = findViewById(R.id.Player_connected);

                btnGameStart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sendToSendRecive("All_start");
                        startActivity(new Intent(MainActivity.this, GameActivity.class));
                    }
                });
            }
        });
    }

    private void initialWork() {
        btnOnOff    = findViewById(R.id.onOff);
        buttonserver = findViewById(R.id.server);
        buttonjoin   = findViewById(R.id.join);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            Log.i(TAG, "peerListListener: peers available: " + peerList.getDeviceList().size());
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray = new String[peers.size()];
                deviceArray = new WifiP2pDevice[peers.size()];
                int index = 0;
                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceAddress + "  " + device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        ((TextView) view.findViewById(android.R.id.text1)).setTextColor(Color.BLACK);
                        return view;
                    }
                };

                if (hasClickedJoin) joinList.setAdapter(adapter);
            }
            if (peers.isEmpty()) {
                Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT).show();
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.i(TAG, "connectionInfoListener: groupFormed=" + wifiP2pInfo.groupFormed
                    + " isGroupOwner=" + wifiP2pInfo.isGroupOwner);
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                Log.i(TAG, "connectionInfoListener: acting as host");
                IsHost = true;
                serverClass = new ServerClass(sendReceive);
                serverClass.start();
                Toast.makeText(getApplicationContext(), "Host", Toast.LENGTH_SHORT).show();
                setContentView(R.layout.client_lobby);

            } else if (wifiP2pInfo.groupFormed) {
                Log.i(TAG, "connectionInfoListener: acting as client");
                IsHost = false;
                clientClass = new ClientClass(groupOwnerAddress, sendReceive);
                clientClass.start();
                Toast.makeText(getApplicationContext(), "Client", Toast.LENGTH_SHORT).show();
                setContentView(R.layout.client_lobby);

                btnClientReady = findViewById(R.id.Lobby_Switch_Ready);
                btnClientReady.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sendToSendRecive("ClientIsReady");
                    }
                });
            }

            // Send initial handshake shortly after connection is established
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.i(TAG, "connectionInfoListener: sending initial handshake");
                        sendToSendRecive("hallole");
                    } catch (Exception e) {
                        Log.e(TAG, "connectionInfoListener: handshake failed", e);
                        Toast.makeText(getApplicationContext(), "Send failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, 5000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        unregisterReceiver(mReceiver);
    }

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    private static String[] getRequiredPermissions() {
        List<String> perms = new ArrayList<>(Arrays.asList(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        ));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: NEARBY_WIFI_DEVICES replaces ACCESS_COARSE_LOCATION for WiFi P2P
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        } else {
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        return perms.toArray(new String[0]);
    }

    protected void checkPermissions() {
        final String[] required = getRequiredPermissions();
        final List<String> missing = new ArrayList<>();
        for (String permission : required) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]),
                    REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[required.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, required, grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            for (int i = permissions.length - 1; i >= 0; --i) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission '" + permissions[i] + "' not granted, exiting",
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            initialWork();
            exqListener();
        }
    }
}
