package com.Project.App.Multipong;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final int    MESSAGE_READ          = 1;
    private static final int    REQUEST_PERMISSIONS   = 1;
    private static final String TAG                   = "MultiPong";

    // ── Shared state (read by GameView / GameActivity) ────────────────────────
    public static SendReceive  sendReceive;
    public static boolean      isHost    = false;
    public static MainActivity instance; // set in onCreate; used by GameActivity to call returnToMainMenu

    // Screen dimensions captured from the main-menu layout (fallback for lobby Sa_Dim).
    // GameView.onSizeChanged() sends a corrected Sa_Dim once the canvas is sized.
    public static int displayWidth;
    public static int displayHeight;

    // Peer Sa_Dim buffered during the lobby, before GameView has started
    public static int   peerPos     = -1;
    public static float peerWidth;
    public static float peerHeight;
    public static float peerDensity;

    // ── UI ─────────────────────────────────────────────────────────────────────
    private Button   buttonHost, buttonJoin;
    private ListView deviceListView;

    // ── Bluetooth ────────────────────────────────────────────────────────────
    private BluetoothAdapter      bluetoothAdapter;
    private BtServerClass         btServer;
    private BtClientClass         btClient;
    private BtDiscoveryReceiver   btDiscoveryReceiver;
    private boolean               btReceiverRegistered = false;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private ArrayAdapter<String>  deviceListAdapter;

    // ── NFC ──────────────────────────────────────────────────────────────────
    private NfcPairingHandler nfcHandler;

    // ── Activity-result launchers ─────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> btEnableLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) continueSetup();
                else Toast.makeText(this, "Bluetooth is required to play", Toast.LENGTH_LONG).show();
            });

    private final ActivityResultLauncher<Intent> discoverableLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> startBtServer());

    // ── Current setup mode ────────────────────────────────────────────────────
    private enum Mode { NONE, HOST, JOIN }
    private Mode mode = Mode.NONE;

    // ─────────────────────────────────────────────────────────────────────────
    // Message handler — receives messages from the SendReceive background thread
    // ─────────────────────────────────────────────────────────────────────────

    final Handler handler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MESSAGE_READ) {
            byte[] buf = (byte[]) msg.obj;
            handleMessage(new String(buf, 0, msg.arg1));
        }
        return true;
    });

    // ─────────────────────────────────────────────────────────────────────────
    // Activity lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        enableImmersiveFullscreen();

        instance    = this;
        sendReceive = newSendReceive();
        nfcHandler  = new NfcPairingHandler(this, this::onNfcPaired);

        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = (bm != null) ? bm.getAdapter() : null;

        buttonHost = findViewById(R.id.server);
        buttonJoin = findViewById(R.id.join);

        buttonHost.setOnClickListener(v -> {
            captureDisplayDimensions();
            isHost = true;
            mode   = Mode.HOST;
            ensureBluetooth();
        });

        buttonJoin.setOnClickListener(v -> {
            captureDisplayDimensions();
            isHost = false;
            mode   = Mode.JOIN;
            ensureBluetooth();
        });

        findViewById(R.id.main_btn_exit).setOnClickListener(v -> finish());

        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcHandler.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcHandler.disable();
        unregisterBtReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Covers back-button dismissal from host/join screens — prevents thread leaks
        if (btServer != null) { btServer.cancel(); btServer = null; }
        if (btClient != null) { btClient.cancel(); btClient = null; }
        sendReceive.disconnect();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        nfcHandler.handleIntent(intent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Connection flow
    // ─────────────────────────────────────────────────────────────────────────

    private void onNfcPaired(String remoteMac) {
        Log.i(TAG, "NFC paired: " + remoteMac);
        if (mode == Mode.JOIN) connectToDevice(remoteMac);
    }

    private void ensureBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled())
            btEnableLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        else
            continueSetup();
    }

    private void continueSetup() {
        if (mode == Mode.HOST) showHostScreen();
        else if (mode == Mode.JOIN) showJoinScreen();
    }

    private void showHostScreen() {
        setContentView(R.layout.host);
        findViewById(R.id.host_btn_cancel).setOnClickListener(v -> returnToMainMenu());

        TextView info = findViewById(R.id.host_device_info);
        if (info != null) {
            @SuppressWarnings("MissingPermission") String name = bluetoothAdapter.getName();
            @SuppressWarnings("MissingPermission") String addr = bluetoothAdapter.getAddress();
            info.setText("Name:  " + (name != null ? name : "Unknown") + "\nAddr:  " + addr);
        }

        Intent disc = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        disc.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        discoverableLauncher.launch(disc);

        @SuppressWarnings("MissingPermission") String localMac = bluetoothAdapter.getAddress();
        nfcHandler.enable(localMac);
    }

    @SuppressWarnings("MissingPermission")
    private void startBtServer() {
        if (mode != Mode.HOST) return; // guard against stale discoverableLauncher callbacks
        btServer = new BtServerClass(bluetoothAdapter, sendReceive,
                new BtServerClass.ConnectionCallback() {
                    @Override public void onConnected()               { onConnectionEstablished(); }
                    @Override public void onConnectionFailed(String e) {
                        Toast.makeText(MainActivity.this, "Connection failed: " + e,
                                Toast.LENGTH_SHORT).show();
                    }
                });
        btServer.start();
    }

    private void showJoinScreen() {
        setContentView(R.layout.client);
        deviceListView = findViewById(R.id.peerListViewC);
        findViewById(R.id.client_btn_cancel).setOnClickListener(v -> returnToMainMenu());

        discoveredDevices.clear();
        deviceListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_2, android.R.id.text1) {
            @Override public View getView(int pos, View convertView, ViewGroup parent) {
                View v = super.getView(pos, convertView, parent);
                ((TextView) v.findViewById(android.R.id.text1)).setTextColor(Color.WHITE);
                ((TextView) v.findViewById(android.R.id.text2)).setTextColor(Color.GRAY);
                return v;
            }
        };
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setOnItemClickListener((parent, view, pos, id) ->
                connectToDevice(discoveredDevices.get(pos).getAddress()));

        @SuppressWarnings("MissingPermission") String localMac = bluetoothAdapter.getAddress();
        nfcHandler.enable(localMac);

        startBtScan();
    }

    @SuppressWarnings("MissingPermission")
    private void startBtScan() {
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();

        btDiscoveryReceiver = new BtDiscoveryReceiver(new BtDiscoveryReceiver.DiscoveryCallback() {
            @Override public void onDeviceFound(BluetoothDevice device) {
                if (!discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    @SuppressWarnings("MissingPermission") String name = device.getName();
                    deviceListAdapter.add((name != null ? name : "Unknown") + "\n" + device.getAddress());
                    deviceListAdapter.notifyDataSetChanged();
                }
            }
            @Override public void onDiscoveryFinished() {
                if (discoveredDevices.isEmpty())
                    Toast.makeText(MainActivity.this,
                            "No devices found. Try tapping phones via NFC.", Toast.LENGTH_LONG).show();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(btDiscoveryReceiver, filter);
        btReceiverRegistered = true;
        bluetoothAdapter.startDiscovery();
    }

    @SuppressWarnings("MissingPermission")
    private void connectToDevice(String mac) {
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
        unregisterBtReceiver();
        nfcHandler.disable();
        Toast.makeText(this, "Connecting…", Toast.LENGTH_SHORT).show();

        btClient = new BtClientClass(bluetoothAdapter, mac, sendReceive,
                new BtClientClass.ConnectionCallback() {
                    @Override public void onConnected()               { onConnectionEstablished(); }
                    @Override public void onConnectionFailed(String e) {
                        Toast.makeText(MainActivity.this, "Connection failed: " + e,
                                Toast.LENGTH_SHORT).show();
                    }
                });
        btClient.start();
    }

    private void onConnectionEstablished() {
        Log.i(TAG, "onConnectionEstablished: isHost=" + isHost);
        nfcHandler.disable();
        unregisterBtReceiver();
        View pb = findViewById(R.id.host_progress);
        if (pb != null) pb.setVisibility(View.GONE);

        if (isHost) {
            setContentView(R.layout.client_lobby);
            bindSeekBar(R.id.Lobby_SeekBar_StartVelocity, R.id.Lobby_TV_SpeedVal);
            bindSeekBar(R.id.Lobby_SeekBar_VelocityGain,  R.id.Lobby_TV_GainVal);
            bindSeekBar(R.id.Lobby_SeekBar_EndPoints,      R.id.Lobby_TV_PointsVal);
            ((Button) findViewById(R.id.Lobby_Switch_Ready)).setOnClickListener(v -> {
                int startVel  = ((SeekBar) findViewById(R.id.Lobby_SeekBar_StartVelocity)).getProgress();
                int velGain   = ((SeekBar) findViewById(R.id.Lobby_SeekBar_VelocityGain)).getProgress();
                int endPoints = ((SeekBar) findViewById(R.id.Lobby_SeekBar_EndPoints)).getProgress();
                String settMsg = "SettMsg" + startVel + ">" + velGain + ">" + endPoints;
                sendMessage(settMsg);
                GameView.applySettings(startVel, velGain, endPoints);
                sendMessage("All_start");
                startActivity(new Intent(this, GameActivity.class));
            });
            ((Button) findViewById(R.id.Lobby_BTN_Disonnect)).setOnClickListener(v ->
                    returnToMainMenu());
        } else {
            setContentView(R.layout.lobby_client);
            ((Button) findViewById(R.id.lobby_client_btn_leave)).setOnClickListener(v ->
                    returnToMainMenu());
        }

        // Send our screen dimensions so the peer can compute the shared play field.
        // GameView.onSizeChanged() will send a corrected message once it has the true canvas size.
        sendMessage("Sa_Dim" + displayWidth + ">" + displayHeight + "#"
                + getResources().getDisplayMetrics().density + "<" + (isHost ? 1 : 2));
    }

    void returnToMainMenu() {
        sendReceive.disconnect();
        if (btServer != null) { btServer.cancel(); btServer = null; }
        if (btClient != null) { btClient.cancel(); btClient = null; }
        unregisterBtReceiver();
        nfcHandler.disable();
        isHost      = false;
        mode        = Mode.NONE;
        peerPos     = -1;
        sendReceive = newSendReceive();
        recreate();
    }

    private void onPeerDisconnected() {
        Log.i(TAG, "onPeerDisconnected");
        GameActivity.finishIfActive();
        Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show();
        returnToMainMenu();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Message protocol
    // ─────────────────────────────────────────────────────────────────────────

    void handleMessage(String msg) {
        Log.i(TAG, "handleMessage: " + msg);

        if (msg.startsWith("SettMsg")) {
            String[] parts = msg.substring(7).split(">");
            if (parts.length == 3) {
                try {
                    GameView.applySettings(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignored) {}
            }
            return;
        }

        if (msg.equals("All_start")) {
            startActivity(new Intent(this, GameActivity.class));
            return;
        }
        if (msg.equals("QuitMsg")) {
            GameActivity.finishIfActive();
            Toast.makeText(this, "Opponent left the game", Toast.LENGTH_SHORT).show();
            returnToMainMenu();
            return;
        }

        if (msg.startsWith("GameEnd") && msg.length() == 8) {
            boolean leftWon = msg.charAt(7) == 'L';
            Log.i(TAG, "GameEnd received: leftWon=" + leftWon
                    + " activeInstance=" + GameView.activeInstance);
            if (GameView.activeInstance != null) {
                GameView.activeInstance.applyGameEnd(leftWon);
            }
            return;
        }

        if (msg.equals("NewGame")) {
            if (GameView.activeInstance != null) GameView.activeInstance.resetGame();
            return;
        }
        if (msg.length() < 6) return;

        String cmd = msg.substring(0, 6);

        if (cmd.equals("GtwMsg")) {
            int target = Integer.parseInt(msg.substring(6, msg.lastIndexOf("*")));
            if (target == GameView.thisDevice.deviceIndex) {
                GameView.circle.ownerIndex = GameView.thisDevice.deviceIndex;
                GameView.circle.ypos = GameView.thisDevice.offset
                        + Float.parseFloat(msg.substring(msg.lastIndexOf(">") + 1, msg.lastIndexOf("<")))
                        * GameView.thisDevice.adjustedHeight;
                GameView.circle.velX   = Float.parseFloat(msg.substring(msg.lastIndexOf("<") + 1, msg.lastIndexOf("#")));
                GameView.circle.velY   = Float.parseFloat(msg.substring(msg.lastIndexOf("#") + 1, msg.lastIndexOf("~")));
                GameView.circle.maxVelY = Float.parseFloat(msg.substring(msg.lastIndexOf("~") + 1));
                GameView.circle.xpos = (GameView.circle.velX < 0) ? GameView.thisDevice.width : 0;
            } else {
                sendMessage(msg); // relay to next device (for future 3+ player support)
            }
        }

        if (cmd.equals("ScoreM")) {
            String payload = msg.substring(6);
            int sep = payload.indexOf('>');
            if (sep >= 0) {
                GameView.scoreLeft  = Integer.parseInt(payload.substring(0, sep));
                GameView.scoreRight = Integer.parseInt(payload.substring(sep + 1));
            }
        }

        if (cmd.equals("Sa_Dim")) {
            float w   = Float.parseFloat(msg.substring(6,             msg.lastIndexOf(">")));
            float h   = Float.parseFloat(msg.substring(msg.lastIndexOf(">") + 1, msg.lastIndexOf("#")));
            float den = Float.parseFloat(msg.substring(msg.lastIndexOf("#") + 1, msg.lastIndexOf("<")));
            int   pos = Integer.parseInt( msg.substring(msg.lastIndexOf("<") + 1));

            if (GameView.screens != null && pos > 0 && pos <= GameView.screens.length) {
                GameView.screens[pos - 1].width       = w;
                GameView.screens[pos - 1].height      = h;
                GameView.screens[pos - 1].density     = den;
                GameView.screens[pos - 1].deviceIndex = pos;
                GameView.recomputePlayField();
            } else {
                peerPos     = pos;
                peerWidth   = w;
                peerHeight  = h;
                peerDensity = den;
            }
            Log.i(TAG, "Sa_Dim: pos=" + pos + " w=" + w + " h=" + h + " density=" + den);
        }
    }

    static void sendMessage(String text) {
        Log.i(TAG, "sendMessage: " + text);
        sendReceive.write(text); // write(String) appends '\n' delimiter for stream framing
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private SendReceive newSendReceive() {
        SendReceive sr = new SendReceive(MESSAGE_READ, handler);
        sr.onDisconnect = this::onPeerDisconnected;
        return sr;
    }

    private void bindSeekBar(int seekBarId, int valueLabelId) {
        SeekBar  bar   = findViewById(seekBarId);
        TextView label = findViewById(valueLabelId);
        if (bar == null || label == null) return;
        label.setText(String.valueOf(bar.getProgress()));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) { label.setText(String.valueOf(p)); }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    void captureDisplayDimensions() {
        View v = findViewById(R.id.messureBild);
        if (v != null) {
            displayWidth  = v.getWidth();
            displayHeight = v.getHeight();
            Log.i(TAG, "captureDisplayDimensions: " + displayWidth + "x" + displayHeight);
        }
    }

    private void enableImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat ctrl =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (ctrl != null) {
            ctrl.hide(WindowInsetsCompat.Type.systemBars());
            ctrl.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private void unregisterBtReceiver() {
        if (btReceiverRegistered && btDiscoveryReceiver != null) {
            try { unregisterReceiver(btDiscoveryReceiver); } catch (IllegalArgumentException ignored) {}
            btReceiverRegistered = false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permissions
    // ─────────────────────────────────────────────────────────────────────────

    private void checkAndRequestPermissions() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        List<String> missing = new ArrayList<>();
        for (String p : perms)
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                missing.add(p);
        if (!missing.isEmpty())
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQUEST_PERMISSIONS) {
            for (int i = 0; i < results.length; i++) {
                if (results[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission '" + perms[i] + "' denied, exiting",
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
        }
    }
}
