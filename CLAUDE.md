# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./build.sh                       # Build signed release APK + AAB (requires keystore.properties)
./build.sh debug                 # Build debug APK
./gradlew assembleDebug          # Build debug APK only
./gradlew installDebug           # Install debug APK to connected device
./gradlew clean                  # Clean build artifacts
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires connected device)
```

## Architecture

Two-phase runtime: **Bluetooth setup** (MainActivity) → **Game** (GameActivity/GameView).

### Java source: `app/src/main/java/com/Project/App/Multipong/`

| File | Role |
|------|------|
| `MainActivity.java` | Bluetooth manager: peer discovery, Host/Client selection, connection lifecycle, incoming message routing |
| `BtServerClass.java` | Host-side: opens BluetoothServerSocket, hands off accepted socket to SendReceive |
| `BtClientClass.java` | Client-side: connects to host by Bluetooth MAC, hands off to SendReceive |
| `BtDiscoveryReceiver.java` | BroadcastReceiver for Bluetooth scan results |
| `BtConstants.java` | Shared RFCOMM service UUID |
| `NfcPairingHandler.java` | NFC foreground dispatch — delivers host MAC to client on tap |
| `SendReceive.java` | Async thread for reading/writing raw bytes over the Bluetooth socket |
| `GameActivity.java` | Thin container; sets fullscreen portrait, hosts GameView |
| `GameView.java` | Game loop (`invalidate()` → `draw(Canvas)`), ball physics, paddle touch input, collision detection, scoring, bitmap rendering; inner classes `Circle`, `Screen`, `Paddle` |

### Message protocol (string prefixes over Bluetooth socket)

| Prefix | Meaning |
|--------|---------|
| `GtwMsg` | Ball crossing device boundary (gateway handoff) |
| `Sa_Dim` | Share screen dimensions |
| `SettMsg` | Lobby settings (startVel, velGain, endPoints) sent by host on game start |
| `GameEnd` | Game over — payload indicates which side won |
| `All_start` | Start game signal |
| `ScoreM` | Score update |
| `QuitMsg` | Player left |

### Layouts: `app/src/main/res/layout/`
`activity_main.xml` (menu) → `host.xml` / `client.xml` → `client_lobby.xml` / `lobby_client.xml` → game (built in code by GameActivity)

### Source assets: `assets-source/`
Original artwork files (GIF, PNG) used to generate the drawable resources.

## Conventions

- All comments and UI strings are in English.
- `amountPlayers` / `PLAYER_COUNT` in GameView is wired for future expansion beyond 2 devices.
- Application ID: `com.spacehats.multipong` (Play Store). Java namespace remains `com.Project.App.Multipong`.
