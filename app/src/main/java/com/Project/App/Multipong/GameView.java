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

    public static Circle circle;
    public static Screen thisScreen;
    public static Screen[] screen;
    Paddle paddle;
    Paint paint;

    private static final String TAG = "MultiPong";

    // Retro colour palette (ARGB)
    private static final int COLOR_BACKGROUND   = Color.BLACK;
    private static final int COLOR_SAFE_ZONE    = 0xFF2A2A2A;
    private static final int COLOR_BALL         = 0xFFE8E8E8;
    private static final int COLOR_CENTER_LINE  = 0x44E8E8E8;
    private static final int COLOR_PADDLE_LEFT  = 0xFF00F5FF;
    private static final int COLOR_PADDLE_RIGHT = 0xFFFF00AA;
    private static final int COLOR_SCORE        = 0xFFE8E8E8;
    private static final int COLOR_HINT         = 0x88E8E8E8;
    private static final int COLOR_SCANLINE     = 0x18000000;

    boolean ballStop = true;
    boolean waitingForFingerUp = false;

    // Paddle hit flash — duration in seconds so it looks the same at any refresh rate
    private static final float FLASH_DURATION = 0.083f; // ~5 frames at 60 fps
    float paddleFlashRemaining = 0f;

    // Time-delta: normalize movement to REFERENCE_FPS so speed is identical
    // regardless of screen refresh rate (60 Hz vs 144 Hz, etc.)
    private long lastFrameNanos = 0;
    private static final float REFERENCE_FPS = 60f;

    public GameView(Context context) {
        super(context);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        thisScreen = new Screen();
        thisScreen.getOwnHandyTask();
        thisScreen.getAmountPlayers();
        thisScreen.getOwnHandyPosition();
        // density is needed early (paddle/ball sizing); width/height come from onSizeChanged
        thisScreen.density        = getResources().getDisplayMetrics().density;
        thisScreen.adjustedHeight = 1; // placeholder; set in onSizeChanged

        if (thisScreen.HandyTask == 'h') {
            screen = new Screen[amountPlayers];
            for (int i = 0; i < amountPlayers; i++) {
                screen[i] = new Screen();
                screen[i].width = 0;
            }
            // Apply any Sa_Dim that arrived during the lobby before GameView started
            int p = MainActivity.pendingSaDimPos;
            if (p > 0 && p <= amountPlayers) {
                screen[p - 1].width         = MainActivity.pendingSaDimWidth;
                screen[p - 1].height        = MainActivity.pendingSaDimHeight;
                screen[p - 1].density       = MainActivity.pendingSaDimDensity;
                screen[p - 1].HandyPosition = p;
                MainActivity.pendingSaDimPos = -1;
                Log.i(TAG, "GameView: buffered Sa_Dim applied pos=" + p);
            }
        }

        circle = new Circle();
        circle.standardxspeed    = 0;
        circle.standardyspeed    = 0;
        circle.standardmaxyspeed = 20;
        circle.standardradius    = 10;
        circle.direction         = 1;
        circle.CurrentHandy      = 1;

        paddle = new Paddle();

        scoreLeft          = 0;
        scoreRight         = 0;
        waitingForFingerUp = false;
    }

    // Called by the Android framework with the exact canvas dimensions after the window
    // is fully settled (immersive fullscreen system-bar hide completed).
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i(TAG, "onSizeChanged: w=" + w + " h=" + h);

        thisScreen.width   = w;
        thisScreen.height  = h;
        thisScreen.density = getResources().getDisplayMetrics().density;

        if (thisScreen.HandyTask == 'h' && screen != null) {
            int p = thisScreen.HandyPosition - 1;
            screen[p].width         = w;
            screen[p].height        = h;
            screen[p].density       = thisScreen.density;
            screen[p].HandyPosition = thisScreen.HandyPosition;
            recomputeAdjustedHeight(); // fires immediately if client Sa_Dim already buffered
        }

        if (thisScreen.HandyTask == 'j') {
            if (MainActivity.pendingSaDimHeight > 0 && MainActivity.pendingSaDimDensity > 0) {
                float remoteRefH = MainActivity.pendingSaDimHeight / MainActivity.pendingSaDimDensity;
                float ownRefH    = h / thisScreen.density;
                float minRefH    = Math.min(remoteRefH, ownRefH);
                thisScreen.adjustedHeight = minRefH * thisScreen.density;
                thisScreen.offset         = (h - thisScreen.adjustedHeight) / 2f;
                MainActivity.pendingSaDimPos = -1;
                Log.i(TAG, "onSizeChanged client: adjustedHeight=" + thisScreen.adjustedHeight
                        + " offset=" + thisScreen.offset);
            } else {
                thisScreen.adjustedHeight = h;
                thisScreen.offset         = 0;
            }
            // Send corrected Sa_Dim to the host so it can recalculate too
            String msg = "Sa_Dim" + w + ">" + h + "#"
                    + thisScreen.density + "<" + thisScreen.HandyPosition;
            Log.i(TAG, "onSizeChanged: sending Sa_Dim: " + msg);
            MainActivity.sendToSendRecive(msg);
        }

        repositionBallAndPaddle();
    }

    // Recalculate the play-field dimensions for all connected screens.
    // Finds the smallest physical height (pixels / density = dp) across all devices,
    // then converts it back to each device's pixel space. The device with more physical
    // screen real estate gets letterbox bands; both play fields are the same physical size.
    static void recomputeAdjustedHeight() {
        if (screen == null || thisScreen == null) return;
        float minRef = Float.MAX_VALUE;
        for (Screen s : screen) {
            if (s.width <= 0 || s.density <= 0) return; // not all peers known yet
            float ref = s.height / s.density;
            if (ref < minRef) minRef = ref;
        }
        for (Screen s : screen) {
            s.adjustedHeight = minRef * s.density;
            s.offset         = (s.height - s.adjustedHeight) / 2f;
        }
        int idx = thisScreen.HandyPosition - 1;
        thisScreen.adjustedHeight = screen[idx].adjustedHeight;
        thisScreen.offset         = screen[idx].offset;
        Log.i(TAG, "recomputeAdjustedHeight: adjustedHeight=" + thisScreen.adjustedHeight
                + " offset=" + thisScreen.offset);
    }

    private void repositionBallAndPaddle() {
        float centreY = thisScreen.offset + thisScreen.adjustedHeight / 2f;

        circle.xpos   = thisScreen.width / 2f;
        circle.ypos   = centreY;
        circle.radius = circle.standardradius * thisScreen.density;
        circle.xspeed = 0;
        circle.yspeed = 0;

        if (thisScreen.HandyPosition == 1) {
            paddle.xdistance = 80  * thisScreen.density;
            paddle.length    = 100 * thisScreen.density;
            paddle.width     = 10  * thisScreen.density;
            paddle.ypos      = centreY;
            paddle.adjust    = 50  * thisScreen.density;
            paddle.xpos      = paddle.xdistance;
        } else if (thisScreen.HandyPosition == amountPlayers) {
            paddle.xdistance = 80  * thisScreen.density;
            paddle.length    = 100 * thisScreen.density;
            paddle.width     = 10  * thisScreen.density;
            paddle.ypos      = centreY;
            paddle.adjust    = 50  * thisScreen.density;
            paddle.xpos      = thisScreen.width - paddle.xdistance;
        }

        lastFrameNanos = 0;
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

        public void move(float dtScale) {
            xpos += xspeed * dtScale;
            ypos += yspeed * dtScale;
        }

        public void getSpecificValues() {
            xspeed    = standardxspeed    * thisScreen.density;
            yspeed    = standardyspeed    * thisScreen.density;
            radius    = standardradius    * thisScreen.density;
            maxyspeed = standardmaxyspeed * thisScreen.density;
        }

        public void checkHitbox() {
            // Bounce off top/bottom play-field walls
            if (ypos > thisScreen.height - radius - thisScreen.offset && standardyspeed > 0)
                standardyspeed *= -1;
            if (ypos < radius + thisScreen.offset && standardyspeed < 0)
                standardyspeed *= -1;

            // Left paddle collision (device 1)
            if (thisScreen.HandyPosition == 1
                    && xpos - radius <= paddle.xpos + paddle.width
                    && xpos - radius >= paddle.xpos - paddle.width
                    && ypos >= paddle.ypos - paddle.length / 2
                    && ypos <= paddle.ypos + paddle.length / 2
                    && standardxspeed < 0) {
                standardxspeed *= -1;
                // standardmaxyspeed is density-independent → standardyspeed stays density-independent
                standardyspeed = (float) Math.sin(Math.PI * (ypos - paddle.ypos) / paddle.length)
                        * standardmaxyspeed * 0.3f;
                paddleFlashRemaining = FLASH_DURATION;
            }

            // Right paddle collision (last device)
            if (thisScreen.HandyPosition == amountPlayers
                    && xpos + radius >= paddle.xpos - paddle.width
                    && xpos + radius <= paddle.xpos + paddle.width
                    && ypos >= paddle.ypos - paddle.length
                    && ypos <= paddle.ypos + paddle.length
                    && standardxspeed > 0) {
                standardxspeed *= -1;
                standardyspeed = (float) Math.sin(Math.PI * (ypos - paddle.ypos) / paddle.length)
                        * standardmaxyspeed * 0.3f;
                paddleFlashRemaining = FLASH_DURATION;
            }
        }

        public void getPosX(float value) { xpos = value; }
        public void getPosY(float value) { ypos = value; }
    }

    class Screen {
        float width, height;
        float density;
        float adjustedHeight, offset;
        int HandyPosition;
        char HandyTask;

        public void getOwnHandyPosition() {
            if (HandyTask == 'h') HandyPosition = 1;
            if (HandyTask == 'j') HandyPosition = 2;
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

        // Guard: skip until onSizeChanged has given us real dimensions
        if (thisScreen.width == 0 || thisScreen.height == 0) {
            invalidate();
            return;
        }

        // Time-delta — computed once per frame, used for both physics and flash countdown
        long now = System.nanoTime();
        float dt = (lastFrameNanos == 0) ? (1f / REFERENCE_FPS)
                   : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        float dtScale = Math.min(dt * REFERENCE_FPS, 3f); // cap prevents tunnelling on lag spikes

        float w   = thisScreen.width;
        float h   = thisScreen.height;
        float off = thisScreen.offset;

        // Background
        canvas.drawColor(COLOR_BACKGROUND);

        // Letterbox bars — only drawn when this screen is larger than the play field
        if (off > 0) {
            paint.setColor(COLOR_SAFE_ZONE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, w, off, paint);
            canvas.drawRect(0, h - off, w, h, paint);
        }

        // ── Center dashed dividing line ──────────────────────────────────
        paint.setColor(COLOR_CENTER_LINE);
        float cx    = w / 2f;
        float dashH = 20 * thisScreen.density;
        float gapH  = 12 * thisScreen.density;
        float lineW = 3  * thisScreen.density;
        for (float y = off; y < h - off; y += dashH + gapH) {
            canvas.drawRect(cx - lineW, y, cx + lineW, Math.min(y + dashH, h - off), paint);
        }

        // ── Score display ────────────────────────────────────────────────
        paint.setColor(COLOR_SCORE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        float scoreTextSize = Math.max(28 * thisScreen.density, 1);
        paint.setTextSize(scoreTextSize);
        paint.setTextAlign(Paint.Align.CENTER);
        float scoreY = off + scoreTextSize * 1.2f;
        canvas.drawText(String.valueOf(scoreLeft),  w * 0.25f, scoreY, paint);
        canvas.drawText(String.valueOf(scoreRight), w * 0.75f, scoreY, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // ── Ball ─────────────────────────────────────────────────────────
        if (thisScreen.HandyPosition == circle.CurrentHandy) {
            paint.setColor(COLOR_BALL);
            canvas.drawCircle(circle.xpos, circle.ypos, circle.radius, paint);
        }

        // ── Paddle ───────────────────────────────────────────────────────
        if (thisScreen.HandyPosition == 1 || thisScreen.HandyPosition == amountPlayers) {
            int paddleColor;
            if (paddleFlashRemaining > 0) {
                paddleFlashRemaining -= dt;
                paddleColor = Color.WHITE;
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

        // ── Physics update (only the device that currently owns the ball) ─
        if (thisScreen.HandyPosition == circle.CurrentHandy) {
            circle.getSpecificValues();
            paint.setColor(COLOR_BALL);
            canvas.drawCircle(circle.xpos, circle.ypos, circle.radius, paint);
            circle.checkHitbox();
            circle.move(dtScale);

            // Ball leaving left side
            if (circle.xpos < 30 && circle.standardxspeed < 0
                    && thisScreen.HandyPosition > 1) {
                sendGateway(thisScreen.HandyPosition - 1);
                circle.CurrentHandy--;
            } else if (circle.xpos < 30 && circle.standardxspeed < 0
                    && thisScreen.HandyPosition == 1) {
                scoreRight++;
                sendScoreUpdate();
                resetBallLocal(1);
            }

            // Ball leaving right side
            if (circle.xpos > w - 30 && circle.standardxspeed > 0
                    && thisScreen.HandyPosition < amountPlayers) {
                sendGateway(thisScreen.HandyPosition + 1);
                circle.CurrentHandy++;
            } else if (circle.xpos > w - 30 && circle.standardxspeed > 0
                    && thisScreen.HandyPosition == amountPlayers) {
                scoreLeft++;
                sendScoreUpdate();
                resetBallLocal(-1);
            }
        }

        // ── Scanline effect ───────────────────────────────────────────────
        paint.setColor(COLOR_SCANLINE);
        for (float y = 0; y < h; y += 4) {
            canvas.drawRect(0, y, w, y + 2, paint);
        }

        invalidate();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void sendGateway(int targetPos) {
        MainActivity.sendToSendRecive(
                "GtwMsg" + targetPos
                        + "*" + circle.xpos
                        + ">" + ((circle.ypos - thisScreen.offset) / thisScreen.adjustedHeight)
                        + "<" + circle.standardxspeed
                        + "#" + circle.standardyspeed
                        + "~" + circle.standardmaxyspeed);
    }

    private void resetBallLocal(int directionSign) {
        circle.xpos           = directionSign > 0 ? thisScreen.width * 0.25f : thisScreen.width * 0.75f;
        circle.ypos           = thisScreen.offset + thisScreen.adjustedHeight / 2f;
        circle.standardxspeed = 0;
        circle.standardyspeed = 0;
        circle.direction      = directionSign;
        ballStop              = true;
        waitingForFingerUp    = true;
        lastFrameNanos        = 0; // reset so the first move after tap isn't a giant leap
    }

    private void sendScoreUpdate() {
        MainActivity.sendToSendRecive("ScoreM" + scoreLeft + ">" + scoreRight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            waitingForFingerUp = false;
        }

        if (action == MotionEvent.ACTION_DOWN
                && thisScreen.HandyPosition == circle.CurrentHandy
                && ballStop && !waitingForFingerUp) {
            ballStop       = false;
            lastFrameNanos = 0; // fresh timer for a clean first move
            circle.standardxspeed = circle.direction > 0 ? 6 : -6;
            circle.standardyspeed = 3;
        }

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
