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
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ── Constants ──────────────────────────────────────────────────────────────
    static final int MESSAGE_READ = 1;
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final String TAG = "MultiPong";

    // ── Shared game state (accessed by GameView) ────────────────────────────
    public static SendReceive sendReceive;
    public static boolean IsHost = false;
    public static int x_display;
    public static int y_display;

    // Sa_Dim received before GameView.screen[] is allocated (lobby phase)
    public static int   pendingSaDimPos     = -1;
    public static float pendingSaDimWidth;
    public static float pendingSaDimHeight;
    public static float pendingSaDimDensity;

    // ── UI fields ───────────────────────────────────────────────────────────
    Button buttonServer, buttonJoin;
    ListView joinList;

    // ── Bluetooth ────────────────────────────────────────────────────────────
    private BluetoothAdapter bluetoothAdapter;
    private BtServerClass btServerClass;
    private BtClientClass btClientClass;
    private BtDiscoveryReceiver btDiscoveryReceiver;
    private boolean btReceiverRegistered = false;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private ArrayAdapter<String> deviceListAdapter;

    // ── NFC ──────────────────────────────────────────────────────────────────
    private NfcPairingHandler nfcPairingHandler;

    // ── Activity-result launchers (must be registered before onStart) ────────
    private final ActivityResultLauncher<Intent> btEnableLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.i(TAG, "Bluetooth enabled by user");
                    continueWithCurrentMode();
                } else {
                    Toast.makeText(this, "Bluetooth is required to play", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> discoverableLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Continue whether or not the user accepted discoverability.
                // NFC-paired connections work without it; BT-scan connections benefit from it.
                Log.i(TAG, "Discoverability result: " + result.getResultCode());
                startBtServer();
            });

    // ── Current mode (set when HOST/JOIN button is pressed) ─────────────────
    private enum Mode { NONE, HOST, JOIN }
    private Mode currentMode = Mode.NONE;

    // ─────────────────────────────────────────────────────────────────────────

    // Handler that delivers messages from the SendReceive background thread
    final Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MESSAGE_READ) {
                byte[] buf = (byte[]) msg.obj;
                String message = new String(buf, 0, msg.arg1);
                Log.i(TAG, "handler: " + message);
                filterservice(message);
            }
            return true;
        }
    });

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        enableImmersiveFullscreen();

        sendReceive       = createSendReceive();
        nfcPairingHandler = new NfcPairingHandler(this, this::onNfcPaired);

        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = (bm != null) ? bm.getAdapter() : null;

        buttonServer = findViewById(R.id.server);
        buttonJoin   = findViewById(R.id.join);

        buttonServer.setOnClickListener(v -> {
            getDisplayDimensions();
            IsHost      = true;
            currentMode = Mode.HOST;
            ensureBluetoothThenContinue();
        });

        buttonJoin.setOnClickListener(v -> {
            getDisplayDimensions();
            IsHost      = false;
            currentMode = Mode.JOIN;
            ensureBluetoothThenContinue();
        });

        findViewById(R.id.main_btn_exit).setOnClickListener(v -> finish());

        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcPairingHandler.onResume();  // re-enable NFC if pairing was active
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcPairingHandler.disable();
        unregisterBtReceiver();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        nfcPairingHandler.handleIntent(intent);
    }

    // ── NFC callback ──────────────────────────────────────────────────────────

    private void onNfcPaired(String remoteMac) {
        Log.i(TAG, "NFC paired: remoteMac=" + remoteMac);
        if (currentMode == Mode.JOIN) {
            // We are the client — connect to the host whose MAC arrived via NFC
            connectToDevice(remoteMac);
        }
        // If we are HOST, the remote device (client) will connect to us via BT
    }

    // ── Connection flow ────────────────────────────────────────────────────────

    private void ensureBluetoothThenContinue() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not available on this device", Toast.LENGTH_LONG).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            btEnableLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        } else {
            continueWithCurrentMode();
        }
    }

    private void continueWithCurrentMode() {
        if (currentMode == Mode.HOST) startHostMode();
        else if (currentMode == Mode.JOIN) startJoinMode();
    }

    private void startHostMode() {
        Log.i(TAG, "startHostMode");
        setContentView(R.layout.host);

        findViewById(R.id.host_btn_cancel).setOnClickListener(v -> returnToMainMenu());

        // Show own BT name + address so the client knows which device to tap
        TextView deviceInfo = findViewById(R.id.host_device_info);
        if (deviceInfo != null) {
            @SuppressWarnings("MissingPermission")
            String btName = bluetoothAdapter.getName();
            @SuppressWarnings("MissingPermission")
            String btAddr = bluetoothAdapter.getAddress();
            deviceInfo.setText(
                    "Name:  " + (btName != null ? btName : "Unknown") + "\n"
                    + "Addr:  " + btAddr);
        }

        // Request discoverability so clients can find us via BT scan
        Intent disc = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        disc.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        discoverableLauncher.launch(disc);

        // Enable NFC receive so a client tap can skip scanning
        @SuppressWarnings("MissingPermission")
        String localMac = bluetoothAdapter.getAddress();
        nfcPairingHandler.enable(localMac);
    }

    private void startBtServer() {
        btServerClass = new BtServerClass(bluetoothAdapter, sendReceive,
                new BtServerClass.ConnectionCallback() {
                    @Override public void onConnected()               { onConnectionEstablished(); }
                    @Override public void onConnectionFailed(String e) {
                        Toast.makeText(MainActivity.this, "Connection failed: " + e,
                                Toast.LENGTH_SHORT).show();
                    }
                });
        btServerClass.start();
        Log.i(TAG, "startBtServer: listening for client");
    }

    private void startJoinMode() {
        Log.i(TAG, "startJoinMode");
        setContentView(R.layout.client);
        joinList = findViewById(R.id.peerListViewC);
        findViewById(R.id.client_btn_cancel).setOnClickListener(v -> returnToMainMenu());

        discoveredDevices.clear();
        deviceListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_2, android.R.id.text1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view.findViewById(android.R.id.text1))
                        .setTextColor(Color.WHITE);
                ((TextView) view.findViewById(android.R.id.text2))
                        .setTextColor(Color.GRAY);
                return view;
            }
        };
        joinList.setAdapter(deviceListAdapter);
        joinList.setOnItemClickListener((parent, view, position, id) ->
                connectToDevice(discoveredDevices.get(position).getAddress()));

        // Enable NFC receive — if host taps us, we get their MAC immediately
        @SuppressWarnings("MissingPermission")
        String localMac = bluetoothAdapter.getAddress();
        nfcPairingHandler.enable(localMac);

        startBtScan();
    }

    @SuppressWarnings("MissingPermission")
    private void startBtScan() {
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();

        btDiscoveryReceiver = new BtDiscoveryReceiver(new BtDiscoveryReceiver.DiscoveryCallback() {
            @Override
            public void onDeviceFound(BluetoothDevice device) {
                if (!discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    @SuppressWarnings("MissingPermission")
                    String name = device.getName();
                    deviceListAdapter.add((name != null ? name : "Unknown") + "\n" + device.getAddress());
                    deviceListAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onDiscoveryFinished() {
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
        Log.i(TAG, "startBtScan: discovery started");
    }

    @SuppressWarnings("MissingPermission")
    private void connectToDevice(String remoteMac) {
        Log.i(TAG, "connectToDevice: " + remoteMac);
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
        unregisterBtReceiver();
        nfcPairingHandler.disable();

        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
        btClientClass = new BtClientClass(bluetoothAdapter, remoteMac, sendReceive,
                new BtClientClass.ConnectionCallback() {
                    @Override public void onConnected()               { onConnectionEstablished(); }
                    @Override public void onConnectionFailed(String e) {
                        Toast.makeText(MainActivity.this, "Connection failed: " + e,
                                Toast.LENGTH_SHORT).show();
                    }
                });
        btClientClass.start();
    }

    private void onConnectionEstablished() {
        Log.i(TAG, "onConnectionEstablished: IsHost=" + IsHost);
        nfcPairingHandler.disable();
        unregisterBtReceiver();

        if (IsHost) {
            setContentView(R.layout.client_lobby);
            setupLobbySeekBars();
            ((Button) findViewById(R.id.Lobby_Switch_Ready)).setOnClickListener(v -> {
                sendToSendRecive("All_start");
                startActivity(new Intent(this, GameActivity.class));
            });
            ((Button) findViewById(R.id.Lobby_BTN_Disonnect)).setOnClickListener(v ->
                    returnToMainMenu());
        } else {
            setContentView(R.layout.lobby_client);
            ((Button) findViewById(R.id.lobby_client_btn_leave)).setOnClickListener(v ->
                    returnToMainMenu());
        }

        // Send screen dimensions to the other device so both can compute the play field
        String saMsg = "Sa_Dim" + x_display + ">" + y_display + "#"
                + getResources().getDisplayMetrics().density + "<"
                + (IsHost ? 1 : 2);
        sendToSendRecive(saMsg);
        Log.i(TAG, "onConnectionEstablished: sent " + saMsg);
    }

    private void returnToMainMenu() {
        Log.i(TAG, "returnToMainMenu");
        sendReceive.disconnect();
        if (btServerClass != null) { btServerClass.cancel(); btServerClass = null; }
        if (btClientClass != null) { btClientClass.cancel(); btClientClass = null; }
        unregisterBtReceiver();
        nfcPairingHandler.disable();
        IsHost          = false;
        currentMode     = Mode.NONE;
        pendingSaDimPos = -1;
        sendReceive     = createSendReceive();
        recreate();
    }

    private void handleRemoteDisconnect() {
        Log.i(TAG, "handleRemoteDisconnect");
        GameActivity.finishIfActive();
        returnToMainMenu();
    }

    private SendReceive createSendReceive() {
        SendReceive sr = new SendReceive(MESSAGE_READ, handler);
        sr.onDisconnect = this::handleRemoteDisconnect;
        return sr;
    }

    private void setupLobbySeekBars() {
        bindSeekBar(R.id.Lobby_SeekBar_StartVelocity, R.id.Lobby_TV_SpeedVal);
        bindSeekBar(R.id.Lobby_SeekBar_VelocityGain,  R.id.Lobby_TV_GainVal);
        bindSeekBar(R.id.Lobby_SeekBar_EndPoints,      R.id.Lobby_TV_PointsVal);
    }

    private void bindSeekBar(int seekBarId, int valueTextId) {
        SeekBar seekBar   = findViewById(seekBarId);
        TextView valueTV  = findViewById(valueTextId);
        if (seekBar == null || valueTV == null) return;
        valueTV.setText(String.valueOf(seekBar.getProgress()));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                valueTV.setText(String.valueOf(p));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    // ── Message protocol ──────────────────────────────────────────────────────

    public void filterservice(String message) {
        Log.i(TAG, "filterservice: " + message);

        if (message.equals("All_start")) {
            startActivity(new Intent(this, GameActivity.class));

        } else if (message.equals("QuitMsg")) {
            GameActivity.finishIfActive();
            returnToMainMenu();

        } else if (message.equals("errore")) {
            Log.i(TAG, "filterservice: error signal");

        } else if (message.length() >= 6) {
            String command = message.substring(0, 6);

            if (command.equals("GtwMsg")) {
                int targetPos = Integer.parseInt(message.substring(6, message.lastIndexOf("*")));
                if (targetPos == GameView.thisScreen.HandyPosition) {
                    GameView.circle.CurrentHandy   = GameView.thisScreen.HandyPosition;
                    GameView.circle.ypos           = GameView.thisScreen.offset
                            + Float.parseFloat(message.substring(
                            message.lastIndexOf(">") + 1, message.lastIndexOf("<")))
                            * GameView.thisScreen.adjustedHeight;
                    GameView.circle.standardxspeed = Float.parseFloat(message.substring(
                            message.lastIndexOf("<") + 1, message.lastIndexOf("#")));
                    GameView.circle.standardyspeed = Float.parseFloat(message.substring(
                            message.lastIndexOf("#") + 1, message.lastIndexOf("~")));
                    GameView.circle.standardmaxyspeed = Float.parseFloat(message.substring(
                            message.lastIndexOf("~") + 1));
                    if (GameView.circle.standardxspeed < 0)
                        GameView.circle.xpos = GameView.thisScreen.width;
                    else
                        GameView.circle.xpos = 0;
                } else {
                    sendToSendRecive(message);
                }
            }

            if (command.equals("NBAMsg")) {
                int targetPos = Integer.parseInt(message.substring(6));
                if (GameView.thisScreen.HandyPosition == targetPos) {
                    GameView.circle.CurrentHandy     = GameView.thisScreen.HandyPosition;
                    GameView.circle.xpos             = 450;
                    GameView.circle.ypos             = 900;
                    GameView.circle.standardyspeed   = 3;
                    GameView.circle.standardradius   = 10;
                    if (GameView.thisScreen.HandyPosition == 1)
                        GameView.circle.standardxspeed = 6;
                    if (GameView.thisScreen.HandyPosition == GameView.amountPlayers)
                        GameView.circle.standardxspeed = -6;
                }
            }

            if (command.equals("ScoreM")) {
                String payload = message.substring(6);
                int sep = payload.indexOf('>');
                if (sep >= 0) {
                    GameView.scoreLeft  = Integer.parseInt(payload.substring(0, sep));
                    GameView.scoreRight = Integer.parseInt(payload.substring(sep + 1));
                }
            }

            if (command.equals("Sa_Dim")) {
                String saWidth   = message.substring(6, message.lastIndexOf(">"));
                String saHeight  = message.substring(message.lastIndexOf(">") + 1, message.lastIndexOf("#"));
                String saDensity = message.substring(message.lastIndexOf("#") + 1, message.lastIndexOf("<"));
                String saPos     = message.substring(message.lastIndexOf("<") + 1);
                int pos = Integer.parseInt(saPos);
                if (GameView.screen != null && pos > 0 && pos <= GameView.screen.length) {
                    GameView.screen[pos - 1].width         = Float.parseFloat(saWidth);
                    GameView.screen[pos - 1].height        = Float.parseFloat(saHeight);
                    GameView.screen[pos - 1].density       = Float.parseFloat(saDensity);
                    GameView.screen[pos - 1].HandyPosition = pos;
                    // Recalculate play-field bounds now that a peer's dimensions updated
                    GameView.recomputeAdjustedHeight();
                } else {
                    // GameView not started yet — buffer; GameView.onSizeChanged will apply it
                    pendingSaDimPos     = pos;
                    pendingSaDimWidth   = Float.parseFloat(saWidth);
                    pendingSaDimHeight  = Float.parseFloat(saHeight);
                    pendingSaDimDensity = Float.parseFloat(saDensity);
                }
                Log.i(TAG, "filterservice: Sa_Dim pos=" + pos + " w=" + saWidth + " h=" + saHeight);
            }
        }
    }

    public static void sendToSendRecive(String input) {
        Log.i(TAG, "sendToSendRecive: " + input);
        sendReceive.write(input.getBytes());
    }

    // ── Permissions ────────────────────────────────────────────────────────────

    private static String[] getRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            // API < 31: location permission required for Bluetooth discovery
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        return perms.toArray(new String[0]);
    }

    private void checkAndRequestPermissions() {
        String[] required = getRequiredPermissions();
        List<String> missing = new ArrayList<>();
        for (String perm : required) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                missing.add(perm);
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission '" + permissions[i] + "' denied, exiting",
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    void getDisplayDimensions() {
        View v = findViewById(R.id.messureBild);
        if (v != null) {
            x_display = v.getWidth();
            y_display = v.getHeight();
            Log.i(TAG, "getDisplayDimensions: x=" + x_display + " y=" + y_display);
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
}
