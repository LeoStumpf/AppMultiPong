package com.Project.App.Multipong;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "MultiPong";
    static GameActivity activeInstance;

    static void finishIfActive() {
        if (activeInstance != null) activeInstance.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = this;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        Log.i(TAG, "GameActivity: onCreate");

        GameView gameView = new GameView(this);

        // Small leave button pinned to top-right corner
        Button leaveBtn = new Button(this);
        leaveBtn.setText("✕ LEAVE");
        leaveBtn.setTextSize(11f);
        leaveBtn.setAlpha(0.7f);
        leaveBtn.setOnClickListener(v -> leaveGame());

        FrameLayout frame = new FrameLayout(this);
        frame.addView(gameView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.setMargins(0, 48, 16, 0);
        frame.addView(leaveBtn, lp);

        setContentView(frame);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeInstance == this) activeInstance = null;
    }

    private void leaveGame() {
        if (MainActivity.sendReceive != null) {
            // Null out our own disconnect callback first — we're quitting intentionally,
            // so we must not show "Connection lost" when the peer closes their end.
            MainActivity.sendReceive.onDisconnect = null;
            // Write synchronously so the message is in the OS buffer before we close the socket.
            MainActivity.sendReceive.writeSync("QuitMsg");
        }
        finish();
        if (MainActivity.instance != null) {
            MainActivity.instance.returnToMainMenu();
        }
    }
}
