package com.Project.App.Multipong;

import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.support.v7.widget.DialogTitle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import static android.graphics.Color.GRAY;
import static android.graphics.Color.GREEN;

public class Lobby extends AppCompatActivity {
    boolean IsReady=false;
    boolean IsHost;

    //Gui Elements
    TextView StartVelocityText, EndPointsText, VelocityGainText, LobbyTitle;
    SeekBar StartVelocityValue, EndPointsValue, VelocityGainValue;
    Button BtnDisonnect, BtnReady;
    ImageView ClientPosInfo;
    ScrollView ListPlayers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lobby);
        IsHost = MainActivity.IsHost;

        //Init Gui Elements
        StartVelocityText =     findViewById(R.id.Lobby_TextView_StartVelocity);
        EndPointsText =         findViewById(R.id.Lobby_TextView_EndPoints);
        VelocityGainText =      findViewById(R.id.Lobby_TextView_VelocityGain);
        LobbyTitle =            findViewById(R.id.Lobby_Textview_Title);

        StartVelocityValue =    findViewById(R.id.Lobby_SeekBar_StartVelocity);
        EndPointsValue =        findViewById(R.id.Lobby_SeekBar_EndPoints);
        VelocityGainValue =     findViewById(R.id.Lobby_SeekBar_VelocityGain);

        BtnDisonnect =          findViewById(R.id.Lobby_BTN_Disonnect);
        BtnReady =              findViewById(R.id.Lobby_BTN_Ready);
        ClientPosInfo =         findViewById(R.id.Lobby_ImageView_ClientInfo);
        ListPlayers =           findViewById(R.id.Lobby_Scrollview_Players);


        setDynamicElements();

        //Make server and Client Lobby
        if(IsHost){
            server();
        }else{
            client();
        }

    }

    public void client(){
        LobbyTitle.setText("Client");

        //Deactivate Lobby elements
        StartVelocityText.setEnabled(false);
        StartVelocityValue.setEnabled(false);

        EndPointsText.setEnabled(false);
        EndPointsValue.setEnabled(false);

        VelocityGainText.setEnabled(false);
        VelocityGainValue.setEnabled(false);

        ClientPosInfo.setVisibility(View.VISIBLE);
        ListPlayers.setVisibility(View.INVISIBLE);

    }

    public void server(){
        LobbyTitle.setText("Server");

        //Activate Lobby elements
        StartVelocityText.setEnabled(true);
        StartVelocityValue.setEnabled(true);

        EndPointsText.setEnabled(true);
        EndPointsValue.setEnabled(true);

        VelocityGainText.setEnabled(true);
        VelocityGainValue.setEnabled(true);

        ClientPosInfo.setVisibility(View.INVISIBLE);
        ListPlayers.setVisibility(View.VISIBLE);

        // Do wifi stuff (Magic)
        //MainActivity.p2pObject.initServer();


    }

    public void setDynamicElements(){
        EndPointsValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                EndPointsText.setText("End Points = " +String.valueOf(new Integer(i)));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        VelocityGainValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                VelocityGainText.setText("Velovity Gain = " +String.valueOf(new Integer(i)));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        StartVelocityValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                StartVelocityText.setText("Start Velocity = " +String.valueOf(new Integer(i)));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        BtnDisonnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        BtnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IsReady = !IsReady;
                if(IsReady){
                    if(IsHost){
                        //Start Game
                    }else{
                        BtnReady.setBackgroundColor(GREEN);
                    }

                }else{
                    BtnReady.setBackgroundColor(GRAY);

                }

            }
        });


    }

}

