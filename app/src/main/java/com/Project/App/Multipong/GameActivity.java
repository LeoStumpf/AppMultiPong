package com.Project.App.Multipong;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class GameActivity extends AppCompatActivity {

    public static float x;
    public static float y;
    private static final String TAG ="DEBUGINGER";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);



/*
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int width = size.x;
        int height = size.y;

        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        x = (float) Math.pow(width/dm.xdpi, 2);
        y = (float) Math.pow(height/dm.ydpi, 2);
        double screenInches = Math.sqrt(x+y);
        */

        Log.i(TAG,"GameActivity");





        setContentView(new GameView(this));



    }




}

