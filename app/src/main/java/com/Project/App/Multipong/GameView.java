package com.Project.App.Multipong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.net.wifi.p2p.WifiP2pInfo;



import java.util.logging.Handler;


public class GameView extends View  {

    public static int amountPlayers;
    public static int scoreLeft = 0;
    public static int scoreRight = 0;
    float zwischenfloat;
    public static Circle circle;
    public static Screen thisScreen;
    public static Screen[] screen;
    Screen saveScreen;
    Paddle paddle;
    Paint paint;
    private static final String TAG ="DEBUGINGER";
    boolean firstime = true;
    boolean secondtime = false;
    int dummy = 0;
    boolean ballstop = true;



    public GameView(Context context) {
        super(context);


        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        thisScreen = new Screen();
        thisScreen.getOwnHandyDimensions();
        thisScreen.getOwnHandyTask();
        thisScreen.getAmountPlayers();
        thisScreen.getOwnHandyPosition();
        thisScreen.adjustedHeight = thisScreen.height;



        String msgcript;

        /*
        msgcript = ("Bliblablublubblub"); //Verschlüsselt ie nachricht
        //thisScreen.HandyPosition--;
        MainActivity.sendToSendRecive(msgcript); //senden
*/


        if(thisScreen.HandyTask == 'h'){
            screen = new Screen[amountPlayers];
            for(int i = 0; i < amountPlayers; i++){
                screen[i] = new Screen();
                screen[i].width = 0;
            }
        }

        if(thisScreen.HandyTask == 'j'){
            //thisScreen.HandyPosition++;MainActivity.cript.encript
            msgcript = ("Sa_Dim" + String.valueOf(thisScreen.width) + ">" + String.valueOf(thisScreen.height) + "#" + String.valueOf(thisScreen.density) + "<" + String.valueOf(thisScreen.HandyPosition)); //Verschlüsselt ie nachricht
            Log.i(TAG, "GameView: Sa_Dim wird gesendet: " + msgcript);
            //thisScreen.HandyPosition--;
            //MainActivity.sendToSendRecive(msgcript); //senden
        }

        Log.i(TAG, "GameView: Objekt Circle initialisiert");



        for(int i = 0; i < 9000000; i++) dummy++;



        circle = new Circle();
        circle.xpos = 450;
        circle.ypos = 400;
        circle.standardxspeed = 0;
        circle.standardyspeed = 0;
        circle.standardmaxyspeed = 20;
        circle.standardradius = 10;
        circle.direction = 1;
        circle.radius = circle.standardradius * thisScreen.density;
        circle.xspeed = circle.standardxspeed * thisScreen.density;
        circle.yspeed = circle.standardyspeed * thisScreen.density;

        paddle = new Paddle();
        if(thisScreen.HandyPosition == 1){
            paddle.xdistance = 80 * thisScreen.density;
            paddle.length = 100 * thisScreen.density;
            paddle.width = 10 * thisScreen.density;
            paddle.ypos = thisScreen.height/2;
            paddle.adjust = 50 * thisScreen.density;
            paddle.xpos = paddle.xdistance;
        }

        if(thisScreen.HandyPosition == amountPlayers){
            paddle.xdistance = 80 * thisScreen.density;
            paddle.length = 100 * thisScreen.density;
            paddle.width = 10 * thisScreen.density;
            paddle.ypos = thisScreen.height/2;
            paddle.adjust = 50 * thisScreen.density;
            paddle.xpos = thisScreen.width - paddle.xdistance;
        }


        circle.CurrentHandy = 1;

        //Verschlüsselt ie nachricht
        //MainActivity.sendToSendRecive("Letsegooo"); //senden

        for(int i = 0; i < 9000000; i++) dummy--;

    }


    class Circle {
        float xpos;
        float ypos;
        float standardxspeed;
        float standardyspeed;
        float xspeed;
        float yspeed;
        float maxyspeed;
        float standardmaxyspeed;
        float standardradius;
        float radius;
        int CurrentHandy;
        int direction;

        public void move(){
            xpos += xspeed;
            ypos += yspeed;
        }

        public void getSpecificValues(){
            xspeed = standardxspeed * thisScreen.density;
            yspeed = standardyspeed * thisScreen.density;
            radius = standardradius * thisScreen.density;
            maxyspeed = standardmaxyspeed * thisScreen.density;
        }

        public void checkHitbox() {
           /*
            if (xpos > screen.width - radius || xpos < radius)
                xspeed *= -1;*/
            //unwichtig
            //if (ypos > thisScreen.height - radius - thisScreen.offset || ypos < radius + thisScreen.offset) standardyspeed *= -1;

            //abprallen unten
            if(ypos > thisScreen.height - radius - thisScreen.offset && standardyspeed > 0) standardyspeed *= -1;
            //abprallen oben
            if(ypos < radius + thisScreen.offset && standardyspeed < 0 ) standardyspeed *= -1;

            /*
            if(xpos >= thisScreen.width && standardxspeed > 0 && CurrentHandy != amountPlayers) {
                CurrentHandy++;
                xpos = 0;
            }*/

            //Ball rechts raus
            if(xpos >= thisScreen.width + radius && standardxspeed > 0 && CurrentHandy == amountPlayers){
                scoreLeft++;
                xpos = 450;
                ypos = 900;
                standardxspeed = 6;
                standardyspeed = 3;
            }

            /*
            if(xpos < 0 && standardxspeed < 0 && CurrentHandy != 1){
                CurrentHandy--;
                xpos = thisScreen.width;
            }*/

            //Ball links raus
            if(xpos < - radius && standardxspeed < 0 && CurrentHandy == 1){
                scoreRight++;
                xpos = 450;
                ypos = 900;
                standardxspeed = 6;
                standardyspeed = 3;
            }

            //Paddle links
            if (thisScreen.HandyPosition == 1 && xpos - radius <= paddle.xpos + paddle.width && xpos - radius >= paddle.xpos - paddle.width && ypos >= paddle.ypos - paddle.length/2 && ypos <= paddle.ypos + paddle.length/2 && standardxspeed < 0) {
                standardxspeed *= -1;
                standardyspeed = (float) Math.sin(Math.PI*(ypos - paddle.ypos)/paddle.length) * Math.abs(maxyspeed) * (float) 0.1;
                //standardxspeed++;
                //standardyspeed++;
                //standardmaxyspeed++;
            }

            //Paddle Rechts
            if (thisScreen.HandyPosition == amountPlayers && xpos + radius >= paddle.xpos - paddle.width && xpos + radius <= paddle.xpos + paddle.width && ypos >= paddle.ypos - paddle.length && ypos <= paddle.ypos + paddle.length && standardxspeed > 0) {
                standardxspeed *= -1;
                standardyspeed = (float) Math.sin(Math.PI*(ypos - paddle.ypos)/paddle.length) * Math.abs(maxyspeed) * (float) 0.1;
                //standardxspeed++;
                //standardyspeed++;
                //standardmaxyspeed++;
            }
        }

        public void sendPos(){
        }

        public void getPosX(float wert){
            xpos = wert;
            Log.i(TAG, "Circle: getPosX hat Empfangen: "+String.valueOf(wert));
        }

        public void getPosY(float wert){
            ypos = wert;
            Log.i(TAG, "Circle: getPosY hat Empfangen: "+String.valueOf(wert));
        }

        public void Point_Scored(char input){
            if(input == 'l'){
                Log.i(TAG, "Punkt links");
                scoreLeft++;
                //MainActivity.sendToSendRecive("EoPMsg" + String.valueOf(circle.CurrentHandy));
            }
            if(input == 'r'){
                Log.i(TAG, "Punkt links");
                scoreRight++;
                //MainActivity.sendToSendRecive("EoPMsg" + String.valueOf(circle.CurrentHandy));
            }
        }

    }


    class Screen {
        float width;
        float height;
        float realWidth;
        float realHeight;
        int density;
        float zwidensity;
        float adjustedHeight;
        float offset;
        int HandyPosition;
        char HandyTask;

        public void getHandyPosition(){

        }

        public void getHandyDimensions(){
            //_----------------------------------
            /*width = Resources.getSystem().getDisplayMetrics().widthPixels;
            height = Resources.getSystem().getDisplayMetrics().heightPixels;
            //height = 900;
            density =  getResources().getDisplayMetrics().density;*/
            //---------------------------
        }

        public void sendHandyPosition(){

        }

        public void sendHandyDimensions(){

        }

        public void getOwnHandyPosition(){
            if(HandyTask == 'h') HandyPosition = 1;
            if(HandyTask == 'j') HandyPosition = 2;
            //if(HandyTask == 'j' && height < 1000) HandyPosition = 2;
            //if(HandyTask == 'j' && height > 1000) HandyPosition = 3;

        }

        public void getOwnHandyDimensions(){
            //width = Resources.getSystem().getDisplayMetrics().widthPixels;
            //height = Resources.getSystem().getDisplayMetrics().heightPixels;

            width = MainActivity.x_display;
            height = MainActivity.y_display;
            zwidensity =  getResources().getDisplayMetrics().density + (float) 0.5;
            density =  (int) zwidensity;

            Log.i(TAG,"GameActivity: density: " + String.valueOf(density));

            /*
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            height = displayMetrics.heightPixels;
            width = displayMetrics.widthPixels;
            */

            //width = 1070;
            //height = 2100;
        }

        public void getOwnHandyTask(){
            if(true) HandyTask = 'h';
            else HandyTask = 'j';
        }

        public void getAmountPlayers(){
            amountPlayers = 2;
        }

    }

    class Paddle{
        float xdistance;
        float xpos;
        float ypos;
        float length;
        float width;
        float adjust;
    }


    @Override
    public void draw(Canvas canvas){
        super.draw(canvas);

        if(thisScreen.HandyTask == 'h' && firstime == true){
            firstime = false;
            Log.i(TAG, "Draw: hat den Initialisirungslauf erreicht ");
            screen[thisScreen.HandyPosition - 1].width = thisScreen.width;
            screen[thisScreen.HandyPosition - 1].height = thisScreen.height;
            screen[thisScreen.HandyPosition - 1].density = thisScreen.density;
            screen[thisScreen.HandyPosition - 1].HandyPosition = thisScreen.HandyPosition;
            screen[thisScreen.HandyPosition - 1].HandyTask = 'h';

            zwischenfloat = 0;


        }


        if(zwischenfloat == 0 && secondtime == false && thisScreen.HandyTask == 'h'){
            zwischenfloat = 1;
            for(int i = 0; i < amountPlayers; i++)zwischenfloat *= screen[i].width;
            if(zwischenfloat != 0) secondtime = true;

        }

        if(secondtime == true && thisScreen.HandyTask == 'h'){
            secondtime = false;

            zwischenfloat = 9999;
            for(int i = 0; i < amountPlayers; i++){
                Log.i(TAG, "GameView: Kleinste Screen Berechnung: Handy: "+ String.valueOf(i) + "   Height:" + String.valueOf(screen[i].height));
                Log.i(TAG, "GameView: Kleinste Screen Berechnung: Handy: "+ String.valueOf(i) + "   Density:" + String.valueOf(screen[i].density));
                if(screen[i].height / screen[i].density < zwischenfloat){
                    zwischenfloat = screen[i].height / screen[i].density;
                    //zwischenspeicher = i;
                }
            }
            Log.i(TAG, "GameView: Kleinste Screen Berechnung: Handy: Zwischenfloat: " + String.valueOf(zwischenfloat));
            for(int i = 0; i < amountPlayers; i++){
                screen[i].adjustedHeight = zwischenfloat * screen[i].density;
                screen[i].offset = (screen[i].height - screen[i].adjustedHeight)/2;
                Log.i(TAG, "GameView: Kleinste Screen Berechnung: Handy: " + String.valueOf(i) + "    Adjusted Height: " + String.valueOf(screen[i].adjustedHeight));
                //_-------------------------------------

                //SENDEN VON OFFSET UND HEIGHT

                //_-------------------------------------
            }
            thisScreen.offset = screen[thisScreen.HandyPosition - 1].offset;

        }


        if(true){

            canvas.drawColor(Color.BLACK);

            canvas.drawRect(0, 0, thisScreen.width, thisScreen.offset, paint);
            canvas.drawRect(0, thisScreen.height - thisScreen.offset, thisScreen.width, thisScreen.height, paint);



            //muss wieder eingefuegt werden:!!!!
            if(thisScreen.HandyPosition == circle.CurrentHandy)canvas.drawCircle(circle.xpos, circle.ypos, circle.radius, paint);
            //----------------------------------------
            //canvas.drawCircle(circle.xpos, circle.ypos, circle.radius, paint);
            //----------------------------------------
            if((thisScreen.HandyPosition == 1 || thisScreen.HandyPosition == amountPlayers) && thisScreen.HandyTask == 'j') canvas.drawRect(paddle.xpos - paddle.width/2, paddle.ypos - paddle.length/2,paddle.xpos + paddle.width/2, paddle.ypos + paddle.length/2, paint);
            if(thisScreen.HandyPosition == 1 && thisScreen.HandyTask == 'h') canvas.drawRect(paddle.xpos - paddle.width/2, paddle.ypos - paddle.length/2,paddle.xpos + paddle.width/2, paddle.ypos + paddle.length/2, paint);
            if(thisScreen.HandyPosition == amountPlayers && thisScreen.HandyTask == 'h') canvas.drawRect(paddle.xpos - paddle.width/2, paddle.ypos - paddle.length/2,paddle.xpos + paddle.width/2, paddle.ypos + paddle.length/2, paint);



            if(thisScreen.HandyPosition == circle.CurrentHandy){
                circle.getSpecificValues();
                canvas.drawCircle(circle.xpos, circle.ypos, circle.radius, paint);
                circle.checkHitbox();
                circle.move();
                Log.i("Dauerrutine: ", "Draw: Auf Handy ist Ball (zeichnen move usw.)" + circle.xpos + " " + circle.ypos);
                if(circle.xpos < 0 + 30 && circle.standardxspeed < 0 && thisScreen.HandyPosition > 1){
                    //Verschlüsselt ie nachricht
                    Log.i(TAG, "Draw: Gateway Message nach links: "+ String.valueOf(thisScreen.HandyPosition - 1) + "*" + String.valueOf(circle.xpos) + ">" + String.valueOf(circle.ypos/thisScreen.adjustedHeight) + "<" + String.valueOf(circle.standardxspeed) + "#" + String.valueOf(circle.standardyspeed));
                    //MainActivity.sendToSendRecive("GtwMsg" + String.valueOf(thisScreen.HandyPosition - 1) + "*" + String.valueOf(circle.xpos) + ">" + String.valueOf(circle.ypos/thisScreen.adjustedHeight) + "<" + String.valueOf(circle.standardxspeed) + "#" + String.valueOf(circle.standardyspeed) + "~" + String.valueOf(circle.standardmaxyspeed));
                    circle.CurrentHandy--;
                }
                else if(circle.xpos < 0 + 30 && circle.standardxspeed < 0 && thisScreen.HandyPosition == 1){
                    circle.xpos = 450;
                    circle.ypos = 400;
                    circle.standardxspeed = 0;
                    circle.standardyspeed = 0;
                    circle.direction = 1;
                    ballstop = true;
                }
                if(circle.xpos > thisScreen.width - 30 && circle.standardxspeed > 0 && thisScreen.HandyPosition < amountPlayers){
                    //Verschlüsselt ie nachricht
                    Log.i(TAG, "Draw: Gateway Message nach rechts: "+ String.valueOf(thisScreen.HandyPosition + 1) + "*" + String.valueOf(circle.xpos) + ">" + String.valueOf(circle.ypos/thisScreen.adjustedHeight) + "<" + String.valueOf(circle.standardxspeed) + "#" + String.valueOf(circle.standardyspeed));
                    //MainActivity.sendToSendRecive("GtwMsg" + String.valueOf(thisScreen.HandyPosition + 1) + "*" + String.valueOf(circle.xpos) + ">" + String.valueOf(circle.ypos/thisScreen.adjustedHeight) + "<" + String.valueOf(circle.standardxspeed) + "#" + String.valueOf(circle.standardyspeed) + "~" + String.valueOf(circle.standardmaxyspeed));
                    circle.CurrentHandy++;
                }
                else if(circle.xpos > thisScreen.width - 30 && circle.standardxspeed > 0 && thisScreen.HandyPosition == amountPlayers){
                    circle.xpos = thisScreen.width - 450;
                    circle.ypos = 400;
                    circle.standardxspeed = 0;
                    circle.standardyspeed = 0;
                    circle.direction = -1;
                    ballstop = true;
                }
            }
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(thisScreen.HandyPosition == circle.CurrentHandy && ballstop == true && circle.direction > 0){
            ballstop = false;
            circle.standardxspeed = 6;
            circle.standardyspeed = 3;
        }
        if(thisScreen.HandyPosition == circle.CurrentHandy && ballstop == true && circle.direction < 0){
            ballstop = false;
            circle.standardxspeed = -6;
            circle.standardyspeed = 3;
        }
        if((thisScreen.HandyPosition == 1 || thisScreen.HandyPosition == amountPlayers) && event.getY() < paddle.ypos + paddle.length/2 + paddle.adjust && event.getY() > paddle.ypos - paddle.length/2 - paddle.adjust) {
            paddle.ypos = event.getY();
            if(paddle.ypos < thisScreen.offset + paddle.length/2) paddle.ypos = thisScreen.offset + paddle.length/2;
            if(paddle.ypos > thisScreen.height - thisScreen.offset - paddle.length/2) paddle.ypos = thisScreen.height - thisScreen.offset - paddle.length/2;
        }
        invalidate();
        return true;
    }




}

