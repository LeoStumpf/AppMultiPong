package com.Project.App.Multipong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    public static int amountPlayers;
    public static int scoreLeft  = 0;
    public static int scoreRight = 0;
    float intermediateFloat;
    public static Circle circle;
    public static Screen thisScreen;
    public static Screen[] screen;
    Screen saveScreen;
    Paddle paddle;
    Paint paint;
    private static final String TAG = "MultiPong";
    boolean firstTime  = true;
    boolean secondTime = false;
    int dummy = 0;
    boolean ballStop = true;

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

        if (thisScreen.HandyTask == 'h') {
            screen = new Screen[amountPlayers];
            for (int i = 0; i < amountPlayers; i++) {
                screen[i] = new Screen();
                screen[i].width = 0;
            }
        }

        if (thisScreen.HandyTask == 'j') {
            String msg = "Sa_Dim"
                    + thisScreen.width + ">"
                    + thisScreen.height + "#"
                    + thisScreen.density + "<"
                    + thisScreen.HandyPosition;
            Log.i(TAG, "GameView: sending Sa_Dim: " + msg);
            MainActivity.sendToSendRecive(msg);
        }

        Log.i(TAG, "GameView: initializing ball");

        // Busy-wait to allow screen dimensions to settle before game starts
        for (int i = 0; i < 9000000; i++) dummy++;

        circle = new Circle();
        circle.xpos            = 450;
        circle.ypos            = 400;
        circle.standardxspeed  = 0;
        circle.standardyspeed  = 0;
        circle.standardmaxyspeed = 20;
        circle.standardradius  = 10;
        circle.direction       = 1;
        circle.radius  = circle.standardradius * thisScreen.density;
        circle.xspeed  = circle.standardxspeed  * thisScreen.density;
        circle.yspeed  = circle.standardyspeed  * thisScreen.density;

        paddle = new Paddle();
        if (thisScreen.HandyPosition == 1) {
            paddle.xdistance = 80  * thisScreen.density;
            paddle.length    = 100 * thisScreen.density;
            paddle.width     = 10  * thisScreen.density;
            paddle.ypos      = thisScreen.height / 2;
            paddle.adjust    = 50  * thisScreen.density;
            paddle.xpos      = paddle.xdistance;
        }
        if (thisScreen.HandyPosition == amountPlayers) {
            paddle.xdistance = 80  * thisScreen.density;
            paddle.length    = 100 * thisScreen.density;
            paddle.width     = 10  * thisScreen.density;
            paddle.ypos      = thisScreen.height / 2;
            paddle.adjust    = 50  * thisScreen.density;
            paddle.xpos      = thisScreen.width - paddle.xdistance;
        }

        circle.CurrentHandy = 1;

        MainActivity.sendToSendRecive("Letsegooo");

        for (int i = 0; i < 9000000; i++) dummy--;
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

        public void move() {
            xpos += xspeed;
            ypos += yspeed;
        }

        public void getSpecificValues() {
            xspeed    = standardxspeed  * thisScreen.density;
            yspeed    = standardyspeed  * thisScreen.density;
            radius    = standardradius  * thisScreen.density;
            maxyspeed = standardmaxyspeed * thisScreen.density;
        }

        public void checkHitbox() {
            // Bounce off bottom wall
            if (ypos > thisScreen.height - radius - thisScreen.offset && standardyspeed > 0)
                standardyspeed *= -1;
            // Bounce off top wall
            if (ypos < radius + thisScreen.offset && standardyspeed < 0)
                standardyspeed *= -1;

            // Ball exits right — point for left player
            if (xpos >= thisScreen.width + radius && standardxspeed > 0
                    && CurrentHandy == amountPlayers) {
                scoreLeft++;
                xpos = 450;
                ypos = 900;
                standardxspeed = 6;
                standardyspeed = 3;
            }

            // Ball exits left — point for right player
            if (xpos < -radius && standardxspeed < 0 && CurrentHandy == 1) {
                scoreRight++;
                xpos = 450;
                ypos = 900;
                standardxspeed = 6;
                standardyspeed = 3;
            }

            // Left paddle collision
            if (thisScreen.HandyPosition == 1
                    && xpos - radius <= paddle.xpos + paddle.width
                    && xpos - radius >= paddle.xpos - paddle.width
                    && ypos >= paddle.ypos - paddle.length / 2
                    && ypos <= paddle.ypos + paddle.length / 2
                    && standardxspeed < 0) {
                standardxspeed *= -1;
                standardyspeed = (float) Math.sin(Math.PI * (ypos - paddle.ypos) / paddle.length)
                        * Math.abs(maxyspeed) * 0.1f;
            }

            // Right paddle collision
            if (thisScreen.HandyPosition == amountPlayers
                    && xpos + radius >= paddle.xpos - paddle.width
                    && xpos + radius <= paddle.xpos + paddle.width
                    && ypos >= paddle.ypos - paddle.length
                    && ypos <= paddle.ypos + paddle.length
                    && standardxspeed > 0) {
                standardxspeed *= -1;
                standardyspeed = (float) Math.sin(Math.PI * (ypos - paddle.ypos) / paddle.length)
                        * Math.abs(maxyspeed) * 0.1f;
            }
        }

        public void sendPos() {}

        public void getPosX(float value) {
            xpos = value;
            Log.i(TAG, "Circle.getPosX: " + value);
        }

        public void getPosY(float value) {
            ypos = value;
            Log.i(TAG, "Circle.getPosY: " + value);
        }

        public void Point_Scored(char side) {
            if (side == 'l') {
                Log.i(TAG, "Point_Scored: left");
                scoreLeft++;
                MainActivity.sendToSendRecive("EoPMsg" + circle.CurrentHandy);
            }
            if (side == 'r') {
                Log.i(TAG, "Point_Scored: right");
                scoreRight++;
                MainActivity.sendToSendRecive("EoPMsg" + circle.CurrentHandy);
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

        public void getHandyPosition() {}

        public void getHandyDimensions() {}

        public void sendHandyPosition() {}

        public void sendHandyDimensions() {}

        public void getOwnHandyPosition() {
            if (HandyTask == 'h') HandyPosition = 1;
            if (HandyTask == 'j') HandyPosition = 2;
        }

        public void getOwnHandyDimensions() {
            width  = MainActivity.x_display;
            height = MainActivity.y_display;
            zwidensity = getResources().getDisplayMetrics().density + 0.5f;
            density = (int) zwidensity;
            Log.i(TAG, "Screen.getOwnHandyDimensions: density=" + density);
        }

        public void getOwnHandyTask() {
            if (MainActivity.IsHost) HandyTask = 'h';
            else HandyTask = 'j';
        }

        public void getAmountPlayers() {
            amountPlayers = 2;
        }
    }

    class Paddle {
        float xdistance;
        float xpos;
        float ypos;
        float length;
        float width;
        float adjust;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // First draw: register host screen dimensions into the shared screen array
        if (thisScreen.HandyTask == 'h' && firstTime) {
            firstTime = false;
            Log.i(TAG, "draw: host initializing screen array");
            screen[thisScreen.HandyPosition - 1].width         = thisScreen.width;
            screen[thisScreen.HandyPosition - 1].height        = thisScreen.height;
            screen[thisScreen.HandyPosition - 1].density       = thisScreen.density;
            screen[thisScreen.HandyPosition - 1].HandyPosition = thisScreen.HandyPosition;
            screen[thisScreen.HandyPosition - 1].HandyTask     = 'h';
            intermediateFloat = 0;
        }

        // Second pass: once all screens have reported dimensions, compute adjusted heights
        if (intermediateFloat == 0 && !secondTime && thisScreen.HandyTask == 'h') {
            intermediateFloat = 1;
            for (int i = 0; i < amountPlayers; i++) intermediateFloat *= screen[i].width;
            if (intermediateFloat != 0) secondTime = true;
        }

        if (secondTime && thisScreen.HandyTask == 'h') {
            secondTime = false;

            // Find the smallest logical height across all devices so the play field fits on all
            intermediateFloat = 9999;
            for (int i = 0; i < amountPlayers; i++) {
                Log.i(TAG, "draw: screen " + i + " h=" + screen[i].height + " d=" + screen[i].density);
                if (screen[i].height / screen[i].density < intermediateFloat) {
                    intermediateFloat = screen[i].height / screen[i].density;
                }
            }
            for (int i = 0; i < amountPlayers; i++) {
                screen[i].adjustedHeight = intermediateFloat * screen[i].density;
                screen[i].offset = (screen[i].height - screen[i].adjustedHeight) / 2;
                Log.i(TAG, "draw: screen " + i + " adjustedHeight=" + screen[i].adjustedHeight);
            }
            thisScreen.offset = screen[thisScreen.HandyPosition - 1].offset;
        }

        if (MainActivity.IsReady) {
            canvas.drawColor(Color.BLACK);

            // Draw top/bottom safe-zone bars (ensures play field is same aspect ratio on all devices)
            canvas.drawRect(0, 0, thisScreen.width, thisScreen.offset, paint);
            canvas.drawRect(0, thisScreen.height - thisScreen.offset,
                    thisScreen.width, thisScreen.height, paint);

            // Draw ball on the device that currently owns it
            if (thisScreen.HandyPosition == circle.CurrentHandy)
                canvas.drawCircle(circle.xpos, circle.ypos, circle.radius, paint);

            // Draw paddle
            if ((thisScreen.HandyPosition == 1 || thisScreen.HandyPosition == amountPlayers)
                    && thisScreen.HandyTask == 'j')
                canvas.drawRect(paddle.xpos - paddle.width / 2, paddle.ypos - paddle.length / 2,
                        paddle.xpos + paddle.width / 2, paddle.ypos + paddle.length / 2, paint);
            if (thisScreen.HandyPosition == 1 && thisScreen.HandyTask == 'h')
                canvas.drawRect(paddle.xpos - paddle.width / 2, paddle.ypos - paddle.length / 2,
                        paddle.xpos + paddle.width / 2, paddle.ypos + paddle.length / 2, paint);
            if (thisScreen.HandyPosition == amountPlayers && thisScreen.HandyTask == 'h')
                canvas.drawRect(paddle.xpos - paddle.width / 2, paddle.ypos - paddle.length / 2,
                        paddle.xpos + paddle.width / 2, paddle.ypos + paddle.length / 2, paint);

            // Update ball physics on the device that owns the ball
            if (thisScreen.HandyPosition == circle.CurrentHandy) {
                circle.getSpecificValues();
                canvas.drawCircle(circle.xpos, circle.ypos, circle.radius, paint);
                circle.checkHitbox();
                circle.move();
                Log.i(TAG, "draw: ball pos x=" + circle.xpos + " y=" + circle.ypos);

                // Ball leaving left side → send to the device on the left
                if (circle.xpos < 30 && circle.standardxspeed < 0
                        && thisScreen.HandyPosition > 1) {
                    MainActivity.sendToSendRecive(
                            "GtwMsg" + (thisScreen.HandyPosition - 1)
                                    + "*" + circle.xpos
                                    + ">" + (circle.ypos / thisScreen.adjustedHeight)
                                    + "<" + circle.standardxspeed
                                    + "#" + circle.standardyspeed
                                    + "~" + circle.standardmaxyspeed);
                    circle.CurrentHandy--;

                } else if (circle.xpos < 30 && circle.standardxspeed < 0
                        && thisScreen.HandyPosition == 1) {
                    // Ball exits left edge on leftmost device — reset
                    circle.xpos = 450;
                    circle.ypos = 400;
                    circle.standardxspeed = 0;
                    circle.standardyspeed = 0;
                    circle.direction = 1;
                    ballStop = true;
                }

                // Ball leaving right side → send to the device on the right
                if (circle.xpos > thisScreen.width - 30 && circle.standardxspeed > 0
                        && thisScreen.HandyPosition < amountPlayers) {
                    MainActivity.sendToSendRecive(
                            "GtwMsg" + (thisScreen.HandyPosition + 1)
                                    + "*" + circle.xpos
                                    + ">" + (circle.ypos / thisScreen.adjustedHeight)
                                    + "<" + circle.standardxspeed
                                    + "#" + circle.standardyspeed
                                    + "~" + circle.standardmaxyspeed);
                    circle.CurrentHandy++;

                } else if (circle.xpos > thisScreen.width - 30 && circle.standardxspeed > 0
                        && thisScreen.HandyPosition == amountPlayers) {
                    // Ball exits right edge on rightmost device — reset
                    circle.xpos = thisScreen.width - 450;
                    circle.ypos = 400;
                    circle.standardxspeed = 0;
                    circle.standardyspeed = 0;
                    circle.direction = -1;
                    ballStop = true;
                }
            }
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Tap to launch the ball when it is stopped
        if (thisScreen.HandyPosition == circle.CurrentHandy && ballStop && circle.direction > 0) {
            ballStop = false;
            circle.standardxspeed = 6;
            circle.standardyspeed = 3;
        }
        if (thisScreen.HandyPosition == circle.CurrentHandy && ballStop && circle.direction < 0) {
            ballStop = false;
            circle.standardxspeed = -6;
            circle.standardyspeed = 3;
        }

        // Move paddle by following the touch Y position
        if ((thisScreen.HandyPosition == 1 || thisScreen.HandyPosition == amountPlayers)
                && event.getY() < paddle.ypos + paddle.length / 2 + paddle.adjust
                && event.getY() > paddle.ypos - paddle.length / 2 - paddle.adjust) {
            paddle.ypos = event.getY();
            if (paddle.ypos < thisScreen.offset + paddle.length / 2)
                paddle.ypos = thisScreen.offset + paddle.length / 2;
            if (paddle.ypos > thisScreen.height - thisScreen.offset - paddle.length / 2)
                paddle.ypos = thisScreen.height - thisScreen.offset - paddle.length / 2;
        }
        invalidate();
        return true;
    }
}
