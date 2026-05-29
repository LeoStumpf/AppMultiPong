# MultiPong

A two-player Pong game for Android where each player holds their own phone. Place the phones side by side — the ball travels across both screens as if they share one continuous play field.

---

## How it works

Two phones connect over **Bluetooth RFCOMM**. Once connected, the ball crosses from one screen to the other at the shared edge. Each player controls a paddle on their own phone with touch input.

```
┌─────────────┐┌─────────────┐
│  HOST       ││  CLIENT     │
│  [paddle]   ││     [paddle]│
│         ●───┼┼────►        │
│             ││             │
└─────────────┘└─────────────┘
```

### Connecting

Two methods:

1. **Bluetooth scan** — one player taps **CREATE GAME**, the other taps **JOIN GAME** and selects the host from the discovered-device list.
2. **NFC tap** — tap the two phones together; the client receives the host's Bluetooth address automatically and connects without scanning.

### Play field scaling

Each phone measures its own screen dimensions and shares them with the peer. Both phones independently compute:

- `playFieldHeight = min(heightA / densityA, heightB / densityB) × ownDensity`

This ensures the play field is the **same physical size** on both screens regardless of resolution or screen size. The phone with the larger screen shows dark grey letterbox bands above and below the play field.

A faint horizontal dashed line runs across the centre of each screen — use it to align the phones at the same height before playing.

### Speed consistency

The game loop runs at the native screen refresh rate. Speed is normalised to 60 fps using a time-delta (`dtScale = elapsed × 60`) so the ball moves at the same real-world speed on both a 60 Hz and a 144 Hz device.

---

## Building

Requirements: JDK 21, Android SDK (API 34).

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install to connected device
./gradlew build                  # build debug + release
./gradlew clean                  # wipe build outputs
```

The SDK path is set in `local.properties` (not committed — create it yourself):

```
sdk.dir=/path/to/your/Android/Sdk
```

---

## Architecture

### Connection layer

| File | Role |
|------|------|
| `BtServerClass.java` | Host: opens a `BluetoothServerSocket` and waits for one client |
| `BtClientClass.java` | Client: connects to host by Bluetooth MAC address |
| `BtDiscoveryReceiver.java` | `BroadcastReceiver` for Bluetooth scan results |
| `BtConstants.java` | Shared RFCOMM service UUID |
| `NfcPairingHandler.java` | NFC foreground dispatch — delivers host MAC to client on tap |
| `SendReceive.java` | Background thread for reading/writing raw bytes over the socket |

### Game layer

| File | Role |
|------|------|
| `MainActivity.java` | Bluetooth setup, device discovery, lobby UI, message routing |
| `GameActivity.java` | Full-screen container; hosts `GameView`; LEAVE button overlay |
| `GameView.java` | Game loop, physics, rendering, play-field scaling |

### Message protocol

All messages are UTF-8 strings sent over the RFCOMM socket.

| Prefix | Direction | Payload |
|--------|-----------|---------|
| `Sa_Dim` | both → peer | `width>height#density<deviceIndex` — screen dimensions |
| `GtwMsg` | owner → peer | `targetIndex*xpos>normY<velX#velY~maxVelY` — ball handoff |
| `All_start` | host → client | start the game |
| `QuitMsg` | either → peer | player left |
| `ScoreM` | owner → peer | `left>right` — updated score |

`GtwMsg` normalises the y-position as `(ypos − offset) / adjustedHeight` (0–1 within the play field) so it maps correctly to the receiving device's coordinate space regardless of screen size.

---

## Requirements

- Android 7.0+ (API 24)
- Bluetooth (required)
- NFC (optional — Bluetooth scan works without it)
- Two physical Android devices (emulators cannot use Bluetooth)
