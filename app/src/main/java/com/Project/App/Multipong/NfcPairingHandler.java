package com.Project.App.Multipong;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;

/**
 * Handles NFC receive-side pairing: uses foreground dispatch to intercept
 * an NDEF message carrying the host's Bluetooth MAC.
 *
 * Android Beam (setNdefPushMessage) was removed in API 34 and no longer
 * compiles. Primary pairing is Bluetooth scan; NFC is an optional shortcut
 * for devices that write their MAC to a physical NFC tag or use HCE.
 */
public class NfcPairingHandler {

    interface PairingCallback {
        void onPaired(String remoteMac);
    }

    private static final String NFC_MIME = "application/com.Project.App.Multipong";
    private static final String TAG      = "MultiPong";

    private final NfcAdapter nfcAdapter;
    private final Activity   activity;
    private final PairingCallback callback;

    private String localMac;
    private boolean active = false;

    public NfcPairingHandler(Activity activity, PairingCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
    }

    public boolean isAvailable() { return nfcAdapter != null; }
    public boolean isEnabled()   { return nfcAdapter != null && nfcAdapter.isEnabled(); }

    /** Call when entering pairing mode. Also call from onResume() to re-register after interruptions. */
    public void enable(String btMac) {
        localMac = btMac;
        active   = true;
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) return;

        // Register foreground dispatch to receive an NDEF with the remote MAC
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? PendingIntent.FLAG_MUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(
                activity, 0,
                new Intent(activity, activity.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                flags);
        nfcAdapter.enableForegroundDispatch(activity, pi,
                new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)}, null);

        Log.i(TAG, "NfcPairingHandler: foreground dispatch enabled");
    }

    /** Re-enable after onResume — no-op if not previously activated. */
    public void onResume() {
        if (active && localMac != null) enable(localMac);
    }

    public void disable() {
        active = false;
        if (nfcAdapter == null) return;
        try {
            nfcAdapter.disableForegroundDispatch(activity);
        } catch (IllegalStateException ignored) {
            // Activity may not be in foreground; safe to ignore
        }
        Log.i(TAG, "NfcPairingHandler: disabled");
    }

    /** Call from Activity.onNewIntent() to process an incoming NFC NDEF message. */
    public void handleIntent(Intent intent) {
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) return;

        Parcelable[] raw = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (raw == null || raw.length == 0) return;

        NdefMessage msg = (NdefMessage) raw[0];
        if (msg.getRecords().length == 0) return;

        String remoteMac = new String(msg.getRecords()[0].getPayload()).trim();
        Log.i(TAG, "NfcPairingHandler: received remote MAC: " + remoteMac);
        callback.onPaired(remoteMac);
    }
}
