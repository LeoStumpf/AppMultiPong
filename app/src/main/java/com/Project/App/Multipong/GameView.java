package com.Project.App.Multipong;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String TAG          = "MultiPong";
    private static final int    PLAYER_COUNT = 2;
    private static final float  REFERENCE_FPS = 60f;

    // Roles stored in Screen.role
    static final char ROLE_HOST   = 'h';
    static final char ROLE_CLIENT = 'c';

    // Retro colour palette (ARGB)
    private static final int COLOR_BACKGROUND  = Color.BLACK;
    private static final int COLOR_LETTERBOX   = 0xFF2A2A2A; // dark grey dead zone on larger screen
    private static final int COLOR_BALL        = 0xFFE8E8E8;
    private static final int COLOR_ALIGN_LINE  = 0x44E8E8E8; // semi-transparent alignment guide
    private static final int COLOR_PADDLE_HOST = 0xFF00F5FF; // neon cyan  — left/host device
    private static final int COLOR_PADDLE_JOIN = 0xFFFF00AA; // neon magenta — right/client device
    private static final int COLOR_SCORE       = 0xFFE8E8E8;
    private static final int COLOR_HINT        = 0x88E8E8E8;
    private static final int COLOR_SCANLINE    = 0x18000000;

    // ── Shared state accessed by MainActivity ──────────────────────────────────
    public static GameView activeInstance; // set in constructor; used by MainActivity to call applyGameEnd
    public static int    scoreLeft  = 0;
    public static int    scoreRight = 0;
    public static Circle circle;
    public static Screen  thisDevice;  // this phone's screen data
    public static Screen[]  screens;   // all connected phones (indexed 0..PLAYER_COUNT-1)

    // ── Lobby settings (applied via applySettings() before game starts) ────────
    // Defaults match the SeekBar initial values in client_lobby.xml (progress=3).
    private static int configStartVel  = 3; // 1–10: initial ball speed
    private static int configVelGain   = 3; // 0–10: speed gain after each hit
    private static int configEndPoints = 3; // 0=unlimited, else win score

    public static void applySettings(int startVel, int velGain, int endPoints) {
        configStartVel  = Math.max(1, startVel);
        configVelGain   = velGain;
        configEndPoints = endPoints;
    }

    // ── Per-instance fields ────────────────────────────────────────────────────
    private Paddle paddle;
    private Paint  paint;

    private boolean ballPaused           = true;
    private boolean requireLift          = false; // wait for finger-up before allowing next launch
    private float   paddleFlashRemaining = 0f;
    private long    lastFrameNanos       = 0;
    private boolean gameOver             = false;
    private boolean localPlayerWon       = false; // only valid when gameOver=true

    // Space-theme bitmap assets
    private Bitmap   bitmapBackground;    // night sky, scaled to screen in onSizeChanged
    private Bitmap[] bitmapFrames;        // asteroid animation frames
    private Bitmap   bitmapPaddleRotated; // satellite rotated 90° to fit vertical paddle slot

    // Asteroid animation state
    private int   frameIndex  = 0;
    private float frameTimer  = 0f;
    private float lastBallAngle = 135f;          // persists direction when ball is paused
    private static final float FRAME_DURATION = 0.14f; // 14/100 s per GIF frame
    private static final float BASE_TAIL_ANGLE = 45f;  // flame points lower-right in source image

    private static final float FLASH_DURATION = 0.083f; // ~5 frames at 60 fps

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor — sets up objects with placeholder dimensions.
    // Real dimensions arrive in onSizeChanged() once the canvas is laid out.
    // ─────────────────────────────────────────────────────────────────────────

    public GameView(Context context) {
        super(context);
        activeInstance = this;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        bitmapBackground = BitmapFactory.decodeResource(getResources(), R.drawable.background_game);

        // Load all 5 asteroid animation frames
        int[] frameResIds = {
            R.drawable.asteroid_f0, R.drawable.asteroid_f1, R.drawable.asteroid_f2,
            R.drawable.asteroid_f3, R.drawable.asteroid_f4
        };
        bitmapFrames = new Bitmap[frameResIds.length];
        for (int i = 0; i < frameResIds.length; i++) {
            bitmapFrames[i] = BitmapFactory.decodeResource(getResources(), frameResIds[i]);
        }

        Bitmap bitmapSatellite = BitmapFactory.decodeResource(getResources(), R.drawable.satellite);
        if (bitmapSatellite != null) {
            Matrix m = new Matrix();
            m.postRotate(90);
            bitmapPaddleRotated = Bitmap.createBitmap(
                    bitmapSatellite, 0, 0,
                    bitmapSatellite.getWidth(), bitmapSatellite.getHeight(), m, true);
            bitmapSatellite.recycle();
        }

        thisDevice = new Screen();
        thisDevice.role     = MainActivity.isHost ? ROLE_HOST : ROLE_CLIENT;
        thisDevice.deviceIndex = thisDevice.role == ROLE_HOST ? 1 : 2;
        thisDevice.density  = getResources().getDisplayMetrics().density;
        thisDevice.adjustedHeight = 1; // updated in onSizeChanged

        if (thisDevice.role == ROLE_HOST) {
            screens = new Screen[PLAYER_COUNT];
            for (int i = 0; i < PLAYER_COUNT; i++) {
                screens[i] = new Screen();
                screens[i].width = 0;
            }
            // Apply any peer Sa_Dim that arrived during the lobby before the game started
            int p = MainActivity.peerPos;
            if (p > 0 && p <= PLAYER_COUNT) {
                screens[p - 1].width       = MainActivity.peerWidth;
                screens[p - 1].height      = MainActivity.peerHeight;
                screens[p - 1].density     = MainActivity.peerDensity;
                screens[p - 1].deviceIndex = p;
                MainActivity.peerPos = -1;
                Log.i(TAG, "GameView: applied buffered peer Sa_Dim pos=" + p);
            }
        }

        circle = new Circle();
        circle.velX       = 0;
        circle.velY       = 0;
        circle.maxVelY    = 10 + configVelGain * 2f; // scales with lobby setting
        circle.baseRadius = 10;
        circle.direction  = 1;
        circle.ownerIndex = 1;

        paddle = new Paddle();

        scoreLeft  = 0;
        scoreRight = 0;
        requireLift = false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // onSizeChanged — called with accurate canvas dimensions after the immersive
    // fullscreen window has fully settled. This is the authoritative source for
    // thisDevice.width / thisDevice.height.
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i(TAG, "onSizeChanged: w=" + w + " h=" + h);

        thisDevice.width   = w;
        thisDevice.height  = h;
        thisDevice.density = getResources().getDisplayMetrics().density;

        if (thisDevice.role == ROLE_HOST && screens != null) {
            int idx = thisDevice.deviceIndex - 1;
            screens[idx].width       = w;
            screens[idx].height      = h;
            screens[idx].density     = thisDevice.density;
            screens[idx].deviceIndex = thisDevice.deviceIndex;
            recomputePlayField(); // fires immediately if peer Sa_Dim already buffered
        }

        if (thisDevice.role == ROLE_CLIENT) {
            if (MainActivity.peerHeight > 0 && MainActivity.peerDensity > 0) {
                float remoteRefH = MainActivity.peerHeight / MainActivity.peerDensity;
                float ownRefH    = h / thisDevice.density;
                float minRefH    = Math.min(remoteRefH, ownRefH);
                thisDevice.adjustedHeight = minRefH * thisDevice.density;
                thisDevice.offset         = (h - thisDevice.adjustedHeight) / 2f;
                MainActivity.peerPos = -1;
                Log.i(TAG, "onSizeChanged client: adjustedHeight=" + thisDevice.adjustedHeight
                        + " offset=" + thisDevice.offset);
            } else {
                thisDevice.adjustedHeight = h;
                thisDevice.offset         = 0;
            }
            // Send corrected Sa_Dim so the host can recalculate its play field too
            MainActivity.sendMessage("Sa_Dim" + w + ">" + h + "#"
                    + thisDevice.density + "<" + thisDevice.deviceIndex);
        }

        positionBallAndPaddle();

        // Scale background to fill the screen
        if (bitmapBackground != null) {
            bitmapBackground = Bitmap.createScaledBitmap(bitmapBackground, w, h, true);
        }
    }

    // Recalculates play-field height for all connected screens.
    // Finds the smallest physical height (pixels / density = dp) across all devices,
    // converts it to each device's pixel space, and centres it with offset bands.
    static void recomputePlayField() {
        if (screens == null || thisDevice == null) return;
        float minRefH = Float.MAX_VALUE;
        for (Screen s : screens) {
            if (s.width <= 0 || s.density <= 0) return; // still waiting for peer
            float ref = s.height / s.density;
            if (ref < minRefH) minRefH = ref;
        }
        for (Screen s : screens) {
            s.adjustedHeight = minRefH * s.density;
            s.offset         = (s.height - s.adjustedHeight) / 2f;
        }
        int idx = thisDevice.deviceIndex - 1;
        thisDevice.adjustedHeight = screens[idx].adjustedHeight;
        thisDevice.offset         = screens[idx].offset;
        Log.i(TAG, "recomputePlayField: adjustedHeight=" + thisDevice.adjustedHeight
                + " offset=" + thisDevice.offset);
    }

    private void positionBallAndPaddle() {
        float centreY = thisDevice.offset + thisDevice.adjustedHeight / 2f;

        circle.xpos     = thisDevice.width / 2f;
        circle.ypos     = centreY;
        circle.pxRadius = circle.baseRadius * thisDevice.density;
        circle.pxVelX   = 0;
        circle.pxVelY   = 0;

        if (thisDevice.deviceIndex == 1) {
            paddle.edgePadding = 80  * thisDevice.density;
            paddle.halfLength  = 50  * thisDevice.density;
            paddle.halfWidth   = 5   * thisDevice.density;
            paddle.ypos        = centreY;
            paddle.touchSlop   = 50  * thisDevice.density;
            paddle.xpos        = paddle.edgePadding;
        } else if (thisDevice.deviceIndex == PLAYER_COUNT) {
            paddle.edgePadding = 80  * thisDevice.density;
            paddle.halfLength  = 50  * thisDevice.density;
            paddle.halfWidth   = 5   * thisDevice.density;
            paddle.ypos        = centreY;
            paddle.touchSlop   = 50  * thisDevice.density;
            paddle.xpos        = thisDevice.width - paddle.edgePadding;
        }

        lastFrameNanos = 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner classes
    // ─────────────────────────────────────────────────────────────────────────

    static class Circle {
        float xpos, ypos;
        float velX, velY;         // density-independent velocities (dp/frame at 60fps)
        float pxVelX, pxVelY;    // pixel velocities, computed each frame by applyDensity()
        float maxVelY, pxMaxVelY;
        float baseRadius, pxRadius;
        int   ownerIndex;         // deviceIndex of the phone currently running the ball
        int   direction;          // launch direction: +1 = right, -1 = left

        void applyDensity() {
            pxVelX    = velX    * thisDevice.density;
            pxVelY    = velY    * thisDevice.density;
            pxRadius  = baseRadius * thisDevice.density;
            pxMaxVelY = maxVelY * thisDevice.density;
        }

        void move(float dtScale) {
            xpos += pxVelX * dtScale;
            ypos += pxVelY * dtScale;
        }
    }

    static class Screen {
        float width, height;
        float density;
        float adjustedHeight, offset;
        int   deviceIndex; // 1 = leftmost (host), 2 = rightmost (client)
        char  role;        // ROLE_HOST or ROLE_CLIENT
    }

    static class Paddle {
        float xpos, ypos;
        float edgePadding; // distance from screen edge to paddle centre
        float halfLength;  // half-height of paddle
        float halfWidth;   // half-width of paddle
        float touchSlop;   // extra touch area above/below paddle
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Collision detection
    // ─────────────────────────────────────────────────────────────────────────

    private void checkHitbox() {
        // Bounce off top/bottom play-field walls
        float topWall    = thisDevice.offset + circle.pxRadius;
        float bottomWall = thisDevice.height - thisDevice.offset - circle.pxRadius;
        if (circle.ypos > bottomWall && circle.velY > 0) circle.velY *= -1;
        if (circle.ypos < topWall    && circle.velY < 0) circle.velY *= -1;

        // Left paddle collision (host device)
        if (thisDevice.deviceIndex == 1
                && circle.xpos - circle.pxRadius <= paddle.xpos + paddle.halfWidth
                && circle.xpos - circle.pxRadius >= paddle.xpos - paddle.halfWidth
                && circle.ypos >= paddle.ypos - paddle.halfLength
                && circle.ypos <= paddle.ypos + paddle.halfLength
                && circle.velX < 0) {
            circle.velX = -(Math.abs(circle.velX) + configVelGain * 0.15f);
            circle.velY  = (float) Math.sin(Math.PI * (circle.ypos - paddle.ypos)
                    / (paddle.halfLength * 2)) * circle.maxVelY * 0.3f;
            paddleFlashRemaining = FLASH_DURATION;
        }

        // Right paddle collision (client device)
        if (thisDevice.deviceIndex == PLAYER_COUNT
                && circle.xpos + circle.pxRadius >= paddle.xpos - paddle.halfWidth
                && circle.xpos + circle.pxRadius <= paddle.xpos + paddle.halfWidth
                && circle.ypos >= paddle.ypos - paddle.halfLength
                && circle.ypos <= paddle.ypos + paddle.halfLength
                && circle.velX > 0) {
            circle.velX = Math.abs(circle.velX) + configVelGain * 0.15f;
            circle.velY  = (float) Math.sin(Math.PI * (circle.ypos - paddle.ypos)
                    / (paddle.halfLength * 2)) * circle.maxVelY * 0.3f;
            paddleFlashRemaining = FLASH_DURATION;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Drawing
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Skip until onSizeChanged has given us real dimensions
        if (thisDevice.width == 0 || thisDevice.height == 0) { invalidate(); return; }

        // Time-delta: normalize movement to REFERENCE_FPS so ball speed is identical
        // regardless of device refresh rate (60 Hz Pixel 4a vs 144 Hz Xiaomi 13T Pro)
        long now = System.nanoTime();
        float dt = (lastFrameNanos == 0) ? (1f / REFERENCE_FPS)
                   : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        float dtScale = Math.min(dt * REFERENCE_FPS, 3f); // cap prevents tunnelling on lag spikes

        float w   = thisDevice.width;
        float h   = thisDevice.height;
        float off = thisDevice.offset;

        // Background
        if (bitmapBackground != null) {
            canvas.drawBitmap(bitmapBackground, 0, 0, null);
        } else {
            canvas.drawColor(COLOR_BACKGROUND);
        }

        // Dark letterbox bands on the device with the larger screen
        if (off > 0) {
            paint.setColor(COLOR_LETTERBOX);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, w, off, paint);
            canvas.drawRect(0, h - off, w, h, paint);
        }

        // Horizontal dashed alignment line — helps players line up the two phones
        paint.setColor(COLOR_ALIGN_LINE);
        float cy    = h / 2f;
        float dashW = 20 * thisDevice.density;
        float gapW  = 12 * thisDevice.density;
        float lineH = 3  * thisDevice.density;
        for (float x = 0; x < w; x += dashW + gapW) {
            canvas.drawRect(x, cy - lineH, Math.min(x + dashW, w), cy + lineH, paint);
        }

        // Score labels ("YOU" / "OPPONENT")
        paint.setColor(COLOR_SCORE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.MONOSPACE);
        float labelSize = Math.max(11 * thisDevice.density, 1);
        paint.setTextSize(labelSize);
        paint.setTextAlign(Paint.Align.CENTER);
        float labelY = off + labelSize * 1.5f;
        String leftLabel  = (thisDevice.deviceIndex == 1) ? "YOU" : "OPPONENT";
        String rightLabel = (thisDevice.deviceIndex == 1) ? "OPPONENT" : "YOU";
        canvas.drawText(leftLabel,  w * 0.25f, labelY, paint);
        canvas.drawText(rightLabel, w * 0.75f, labelY, paint);

        // Score numbers
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        float scoreTextSize = Math.max(28 * thisDevice.density, 1);
        paint.setTextSize(scoreTextSize);
        float scoreY = labelY + scoreTextSize * 1.1f;
        canvas.drawText(String.valueOf(scoreLeft),  w * 0.25f, scoreY, paint);
        canvas.drawText(String.valueOf(scoreRight), w * 0.75f, scoreY, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Paddle
        if (thisDevice.deviceIndex == 1 || thisDevice.deviceIndex == PLAYER_COUNT) {
            RectF paddleRect = new RectF(
                    paddle.xpos - paddle.halfWidth,
                    paddle.ypos - paddle.halfLength,
                    paddle.xpos + paddle.halfWidth,
                    paddle.ypos + paddle.halfLength);
            if (bitmapPaddleRotated != null) {
                // Crop the top/bottom 25% of the rotated satellite so the body+wings fill the paddle
                int pH = bitmapPaddleRotated.getHeight();
                int pW = bitmapPaddleRotated.getWidth();
                Rect srcCrop = new Rect(0, pH / 4, pW, pH * 3 / 4);
                canvas.drawBitmap(bitmapPaddleRotated, srcCrop, paddleRect, paint);
            } else {
                int paddleColor = (thisDevice.deviceIndex == 1) ? COLOR_PADDLE_HOST : COLOR_PADDLE_JOIN;
                paint.setColor(paddleColor);
                canvas.drawRect(paddleRect, paint);
            }
            // Flash overlay on hit
            if (paddleFlashRemaining > 0) {
                paddleFlashRemaining -= dt;
                paint.setColor(0x88FFFFFF);
                canvas.drawRect(paddleRect, paint);
            }
        }

        // Game-over overlay (drawn over everything; tap returns to menu)
        if (gameOver) {
            paint.setColor(0xCC000000);
            canvas.drawRect(0, 0, w, h, paint);
            paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(32 * thisDevice.density);
            paint.setColor(localPlayerWon ? 0xFFFFD700 : 0xFFE8E8E8);
            canvas.drawText(localPlayerWon ? "YOU WIN" : "OPPONENT WINS", w / 2f, h / 2f - 20 * thisDevice.density, paint);
            paint.setTextSize(13 * thisDevice.density);
            paint.setColor(COLOR_HINT);
            canvas.drawText("tap to return", w / 2f, h / 2f + 20 * thisDevice.density, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            invalidate();
            return; // skip physics
        }

        // "TAP TO START" hint
        if (ballPaused && thisDevice.deviceIndex == circle.ownerIndex) {
            paint.setColor(0xCCE8E8E8);
            paint.setTypeface(Typeface.MONOSPACE);
            paint.setTextSize(14 * thisDevice.density);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("TAP TO START", w / 2f, h / 2f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        // Physics — only runs on the device that currently owns the ball
        if (thisDevice.deviceIndex == circle.ownerIndex) {
            circle.applyDensity();
            float r = circle.pxRadius;
            float drawR = r * 2f; // visual radius 2× hitbox — bigger image, same collision

            // Advance animation frame
            frameTimer += dt;
            if (frameTimer >= FRAME_DURATION) {
                frameTimer -= FRAME_DURATION;
                frameIndex = (frameIndex + 1) % bitmapFrames.length;
            }

            // Rotation: tail (lower-right in source) must oppose the travel direction
            if (circle.pxVelX != 0 || circle.pxVelY != 0) {
                lastBallAngle = (float) Math.toDegrees(
                        Math.atan2(circle.pxVelY, circle.pxVelX));
            }
            float rotation = lastBallAngle + 180f - BASE_TAIL_ANGLE;

            Bitmap frame = (bitmapFrames != null && bitmapFrames[frameIndex] != null)
                    ? bitmapFrames[frameIndex] : null;
            if (frame != null) {
                canvas.save();
                canvas.translate(circle.xpos, circle.ypos);
                canvas.rotate(rotation);
                canvas.drawBitmap(frame,
                        new Rect(0, 0, frame.getWidth(), frame.getHeight()),
                        new RectF(-drawR, -drawR, drawR, drawR),
                        paint);
                canvas.restore();
            } else {
                paint.setColor(COLOR_BALL);
                canvas.drawCircle(circle.xpos, circle.ypos, drawR, paint);
            }
            checkHitbox();
            circle.move(dtScale);

            // Ball exits left edge
            if (circle.xpos < 30 && circle.velX < 0) {
                if (thisDevice.deviceIndex > 1) {
                    sendGateway(thisDevice.deviceIndex - 1);
                    circle.ownerIndex--;
                } else {
                    // Left wall — right player scores
                    scoreRight++;
                    sendScoreUpdate();
                    if (checkWin()) return;
                    resetBall(1);
                }
            }

            // Ball exits right edge
            if (circle.xpos > w - 30 && circle.velX > 0) {
                if (thisDevice.deviceIndex < PLAYER_COUNT) {
                    sendGateway(thisDevice.deviceIndex + 1);
                    circle.ownerIndex++;
                } else {
                    // Right wall — left player scores
                    scoreLeft++;
                    sendScoreUpdate();
                    if (checkWin()) return;
                    resetBall(-1);
                }
            }
        }

        // Retro scanline overlay (drawn last)
        paint.setColor(COLOR_SCANLINE);
        for (float y = 0; y < h; y += 4) {
            canvas.drawRect(0, y, w, y + 2, paint);
        }

        invalidate();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void sendGateway(int targetDeviceIndex) {
        MainActivity.sendMessage(
                "GtwMsg" + targetDeviceIndex
                        + "*" + circle.xpos
                        + ">" + ((circle.ypos - thisDevice.offset) / thisDevice.adjustedHeight)
                        + "<" + circle.velX
                        + "#" + circle.velY
                        + "~" + circle.maxVelY);
    }

    private void resetBall(int launchDirection) {
        circle.xpos      = launchDirection > 0 ? thisDevice.width * 0.25f : thisDevice.width * 0.75f;
        circle.ypos      = thisDevice.offset + thisDevice.adjustedHeight / 2f;
        circle.velX      = 0;
        circle.velY      = 0;
        circle.direction = launchDirection;
        ballPaused       = true;
        requireLift      = true;
        lastFrameNanos   = 0;
    }

    private void sendScoreUpdate() {
        MainActivity.sendMessage("ScoreM" + scoreLeft + ">" + scoreRight);
    }

    // Returns true if the game just ended (caller should skip resetBall).
    private boolean checkWin() {
        if (configEndPoints == 0) return false;
        boolean leftWins  = scoreLeft  >= configEndPoints;
        boolean rightWins = scoreRight >= configEndPoints;
        if (!leftWins && !rightWins) return false;

        gameOver = true;
        ballPaused = true;
        // Host (deviceIndex=1) owns the left score; client owns the right.
        localPlayerWon = (thisDevice.deviceIndex == 1) ? leftWins : rightWins;

        // Tell the peer; payload encodes which side won so both devices draw correctly.
        MainActivity.sendMessage("GameEnd" + (leftWins ? "L" : "R"));
        return true;
    }

    /** Called from MainActivity when a GameEnd message arrives on the non-scoring device. */
    public void applyGameEnd(boolean leftWon) {
        gameOver = true;
        ballPaused = true;
        localPlayerWon = (thisDevice.deviceIndex == 1) ? leftWon : !leftWon;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            requireLift = false;
        }

        // Tap anywhere on game-over overlay to leave
        if (gameOver && action == MotionEvent.ACTION_DOWN) {
            MainActivity.sendMessage("QuitMsg");
            GameActivity.finishIfActive();
            return true;
        }

        // Launch ball on first finger-down after a stop (never re-launch with the same touch)
        if (action == MotionEvent.ACTION_DOWN
                && thisDevice.deviceIndex == circle.ownerIndex
                && ballPaused && !requireLift) {
            ballPaused     = false;
            lastFrameNanos = 0;
            float spd = 2 + configStartVel * 0.8f; // configStartVel 1–10 → ~2.8–10 dp/frame
            circle.velX = circle.direction > 0 ? spd : -spd;
            circle.velY = spd * 0.4f;
        }

        // Move paddle to follow finger (within a touch-slop zone around the paddle)
        if ((thisDevice.deviceIndex == 1 || thisDevice.deviceIndex == PLAYER_COUNT)
                && event.getY() < paddle.ypos + paddle.halfLength + paddle.touchSlop
                && event.getY() > paddle.ypos - paddle.halfLength - paddle.touchSlop) {
            paddle.ypos = event.getY();
            float minY = thisDevice.offset + paddle.halfLength;
            float maxY = thisDevice.height - thisDevice.offset - paddle.halfLength;
            if (paddle.ypos < minY) paddle.ypos = minY;
            if (paddle.ypos > maxY) paddle.ypos = maxY;
        }
        invalidate();
        return true;
    }
}
