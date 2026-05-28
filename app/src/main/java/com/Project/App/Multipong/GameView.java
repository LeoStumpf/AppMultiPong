package com.Project.App.Multipong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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

    // Retro colour palette (ARGB)
    private static final int COLOR_BACKGROUND   = Color.BLACK;
    private static final int COLOR_SAFE_ZONE    = 0xFFE8E8E8;   // pixel white
    private static final int COLOR_BALL         = 0xFFE8E8E8;
    private static final int COLOR_CENTER_LINE  = 0x44E8E8E8;   // semi-transparent
    private static final int COLOR_PADDLE_LEFT  = 0xFF00F5FF;   // neon cyan  (player 1)
    private static final int COLOR_PADDLE_RIGHT = 0xFFFF00AA;   // neon magenta (player 2)
    private static final int COLOR_SCORE        = 0xFFE8E8E8;
    private static final int COLOR_HINT         = 0x88E8E8E8;
    private static final int COLOR_SCANLINE     = 0x18000000;   // very subtle dark lines

    boolean firstTime  = true;
    boolean secondTime = false;
    int dummy   = 0;
    boolean ballStop = true;

    // Paddle hit flash: counts down from FLASH_FRAMES to 0, paddle is white while > 0
    private static final int FLASH_FRAMES = 5;
    int paddleFlashFrames = 0;

    public GameView(Context context) {
        super(context);

        paint = new Paint();
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
        circle.xpos             = 450;
        circle.ypos             = 400;
        circle.standardxspeed  = 0;
        circle.standardyspeed  = 0;
        circle.standardmaxyspeed = 20;
        circle.standardradius  = 10;
        circle.direction       = 1;
        circle.radius  = circle.standardradius  * thisScreen.density;
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

    // ─────────────────────────────────────────────────────────────────────────
    // Inner classes
    // ─────────────────────────────────────────────────────────────────────────

    class Circle {
        float xpos, ypos;
        float standardxspeed, standardyspeed;
        float xspeed, yspeed;
        float maxyspeed, standardmaxyspeed;
        float standardradius, radius;
        int CurrentHandy;
        int direction;

        public void move() {
            xpos += xspeed;
            ypos += yspeed;
        }

        public void getSpecificValues() {
            xspeed    = standardxspeed   * thisScreen.density;
            yspeed    = standardyspeed   * thisScreen.density;
            radius    = standardradius   * thisScreen.density;
            maxyspeed = standardmaxyspeed * thisScreen.density;
        }

        public void checkHitbox() {
            // Bounce off top/bottom walls
            if (ypos > thisScreen.height - radius - thisScreen.offset && standardyspeed > 0)
                standardyspeed *= -1;
            if (ypos < radius + thisScreen.offset && standardyspeed < 0)
                standardyspeed *= -1;

            // Ball exits right — point for left player
            if (xpos >= thisScreen.width + radius && standardxspeed > 0
                    && CurrentHandy == amountPlayers) {
                scoreLeft++;
                resetBall(6);
            }
            // Ball exits left — point for right player
            if (xpos < -radius && standardxspeed < 0 && CurrentHandy == 1) {
                scoreRight++;
                resetBall(6);
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
                paddleFlashFrames = FLASH_FRAMES;
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
                paddleFlashFrames = FLASH_FRAMES;
            }
        }

        private void resetBall(float xspeed) {
            xpos = 450; ypos = 900;
            standardxspeed = xspeed;
            standardyspeed = 3;
        }

        public void sendPos() {}

        public void getPosX(float value) { xpos = value; }
        public void getPosY(float value) { ypos = value; }

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
        float width, height, realWidth, realHeight;
        int density;
        float zwidensity, adjustedHeight, offset;
        int HandyPosition;
        char HandyTask;

        public void getHandyPosition()     {}
        public void getHandyDimensions()   {}
        public void sendHandyPosition()    {}
        public void sendHandyDimensions()  {}

        public void getOwnHandyPosition() {
            if (HandyTask == 'h') HandyPosition = 1;
            if (HandyTask == 'j') HandyPosition = 2;
        }

        public void getOwnHandyDimensions() {
            width  = MainActivity.x_display;
            height = MainActivity.y_display;
            zwidensity = getResources().getDisplayMetrics().density + 0.5f;
            density = (int) zwidensity;
            Log.i(TAG, "Screen: density=" + density);
        }

        public void getOwnHandyTask() {
            HandyTask = MainActivity.IsHost ? 'h' : 'j';
        }

        public void getAmountPlayers() { amountPlayers = 2; }
    }

    class Paddle {
        float xdistance, xpos, ypos, length, width, adjust;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Drawing
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // ── First draw: register host screen dimensions ───────────────────────
        if (thisScreen.HandyTask == 'h' && firstTime) {
            firstTime = false;
            Log.i(TAG, "draw: host screen init");
            int p = thisScreen.HandyPosition - 1;
            screen[p].width         = thisScreen.width;
            screen[p].height        = thisScreen.height;
            screen[p].density       = thisScreen.density;
            screen[p].HandyPosition = thisScreen.HandyPosition;
            screen[p].HandyTask     = 'h';
            intermediateFloat = 0;
        }

        // ── Second pass: compute adjusted heights once all screens are known ──
        if (intermediateFloat == 0 && !secondTime && thisScreen.HandyTask == 'h') {
            intermediateFloat = 1;
            for (int i = 0; i < amountPlayers; i++) intermediateFloat *= screen[i].width;
            if (intermediateFloat != 0) secondTime = true;
        }
        if (secondTime && thisScreen.HandyTask == 'h') {
            secondTime = false;
            intermediateFloat = 9999;
            for (int i = 0; i < amountPlayers; i++) {
                if (screen[i].height / screen[i].density < intermediateFloat)
                    intermediateFloat = screen[i].height / screen[i].density;
            }
            for (int i = 0; i < amountPlayers; i++) {
                screen[i].adjustedHeight = intermediateFloat * screen[i].density;
                screen[i].offset         = (screen[i].height - screen[i].adjustedHeight) / 2f;
            }
            thisScreen.offset = screen[thisScreen.HandyPosition - 1].offset;
        }

        // ── Game is ready to draw ─────────────────────────────────────────────
        if (MainActivity.IsReady) {
            float w = thisScreen.width;
            float h = thisScreen.height;
            float off = thisScreen.offset;

            // Background
            canvas.drawColor(COLOR_BACKGROUND);

            // Safe-zone bars (letterbox bands at top/bottom)
            paint.setColor(COLOR_SAFE_ZONE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, w, off, paint);
            canvas.drawRect(0, h - off, w, h, paint);

            // ── Center dashed dividing line ──────────────────────────────────
            paint.setColor(COLOR_CENTER_LINE);
            float cx   = w / 2f;
            float dashH = 20 * thisScreen.density;
            float gapH  = 12 * thisScreen.density;
            float lineW = 3 * thisScreen.density;
            for (float y = off; y < h - off; y += dashH + gapH) {
                canvas.drawRect(cx - lineW, y, cx + lineW,
                        Math.min(y + dashH, h - off), paint);
            }

            // ── Score display ────────────────────────────────────────────────
            paint.setColor(COLOR_SCORE);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            float scoreTextSize = Math.max(28 * thisScreen.density, 1);
            paint.setTextSize(scoreTextSize);
            paint.setTextAlign(Paint.Align.CENTER);
            // Position scores just inside the play field, below the top safe bar
            float scoreY = off + scoreTextSize * 1.2f;
            canvas.drawText(String.valueOf(scoreLeft),  w * 0.25f, scoreY, paint);
            canvas.drawText(String.valueOf(scoreRight), w * 0.75f, scoreY, paint);
            paint.setTextAlign(Paint.Align.LEFT);  // restore default

            // ── Ball ─────────────────────────────────────────────────────────
            if (thisScreen.HandyPosition == circle.CurrentHandy) {
                paint.setColor(COLOR_BALL);
                canvas.drawCircle(circle.xpos, circle.ypos, circle.radius, paint);
            }

            // ── Paddle ───────────────────────────────────────────────────────
            boolean drawPaddle = (thisScreen.HandyPosition == 1
                    || thisScreen.HandyPosition == amountPlayers);
            if (drawPaddle) {
                int paddleColor;
                if (paddleFlashFrames > 0) {
                    paddleColor = Color.WHITE;
                    paddleFlashFrames--;
                } else if (thisScreen.HandyPosition == 1) {
                    paddleColor = COLOR_PADDLE_LEFT;
                } else {
                    paddleColor = COLOR_PADDLE_RIGHT;
                }
                paint.setColor(paddleColor);
                canvas.drawRect(
                        paddle.xpos - paddle.width / 2,
                        paddle.ypos - paddle.length / 2,
                        paddle.xpos + paddle.width / 2,
                        paddle.ypos + paddle.length / 2,
                        paint);
            }

            // ── "TAP TO START" overlay ───────────────────────────────────────
            if (ballStop && thisScreen.HandyPosition == circle.CurrentHandy) {
                paint.setColor(COLOR_HINT);
                paint.setTypeface(Typeface.MONOSPACE);
                paint.setTextSize(14 * thisScreen.density);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("TAP TO START", w / 2f, h / 2f, paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }

            // ── Physics update (only on the device that owns the ball) ───────
            if (thisScreen.HandyPosition == circle.CurrentHandy) {
                circle.getSpecificValues();
                // Redraw ball after physics so it's at the updated position
                paint.setColor(COLOR_BALL);
                canvas.drawCircle(circle.xpos, circle.ypos, circle.radius, paint);
                circle.checkHitbox();
                circle.move();

                // Ball leaving left side → pass to device on the left
                if (circle.xpos < 30 && circle.standardxspeed < 0
                        && thisScreen.HandyPosition > 1) {
                    sendGateway(thisScreen.HandyPosition - 1);
                    circle.CurrentHandy--;
                } else if (circle.xpos < 30 && circle.standardxspeed < 0
                        && thisScreen.HandyPosition == 1) {
                    resetBallLocal(1);
                }

                // Ball leaving right side → pass to device on the right
                if (circle.xpos > w - 30 && circle.standardxspeed > 0
                        && thisScreen.HandyPosition < amountPlayers) {
                    sendGateway(thisScreen.HandyPosition + 1);
                    circle.CurrentHandy++;
                } else if (circle.xpos > w - 30 && circle.standardxspeed > 0
                        && thisScreen.HandyPosition == amountPlayers) {
                    resetBallLocal(-1);
                }
            }

            // ── Scanline effect (drawn last, over everything) ─────────────────
            paint.setColor(COLOR_SCANLINE);
            for (float y = 0; y < h; y += 4) {
                canvas.drawRect(0, y, w, y + 2, paint);
            }
        }

        invalidate();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void sendGateway(int targetPos) {
        MainActivity.sendToSendRecive(
                "GtwMsg" + targetPos
                        + "*" + circle.xpos
                        + ">" + (circle.ypos / thisScreen.adjustedHeight)
                        + "<" + circle.standardxspeed
                        + "#" + circle.standardyspeed
                        + "~" + circle.standardmaxyspeed);
    }

    private void resetBallLocal(int directionSign) {
        circle.xpos          = directionSign > 0 ? 450 : thisScreen.width - 450;
        circle.ypos          = 400;
        circle.standardxspeed = 0;
        circle.standardyspeed = 0;
        circle.direction      = directionSign;
        ballStop = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Tap launches the ball when it is waiting
        if (thisScreen.HandyPosition == circle.CurrentHandy && ballStop) {
            ballStop = false;
            circle.standardxspeed = circle.direction > 0 ? 6 : -6;
            circle.standardyspeed = 3;
        }

        // Track paddle position with finger
        if ((thisScreen.HandyPosition == 1 || thisScreen.HandyPosition == amountPlayers)
                && event.getY() < paddle.ypos + paddle.length / 2 + paddle.adjust
                && event.getY() > paddle.ypos - paddle.length / 2 - paddle.adjust) {
            paddle.ypos = event.getY();
            float minY = thisScreen.offset + paddle.length / 2;
            float maxY = thisScreen.height - thisScreen.offset - paddle.length / 2;
            if (paddle.ypos < minY) paddle.ypos = minY;
            if (paddle.ypos > maxY) paddle.ypos = maxY;
        }
        invalidate();
        return true;
    }
}
