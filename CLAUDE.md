# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build                  # Build debug + release APKs
./gradlew assembleDebug          # Build debug APK only
./gradlew installDebug           # Install debug APK to connected device
./gradlew clean                  # Clean build artifacts
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires connected device)
```

## Architecture

Two-phase runtime: **WiFi P2P setup** (MainActivity) → **Game** (GameActivity/GameView).

### Java source: `app/src/main/java/com/Project/App/Multipong/`

| File | Role |
|------|------|
| `MainActivity.java` | WiFi Direct manager: peer discovery, Host/Client selection, connection lifecycle, incoming message routing |
| `WifiDirectBroadcastReceiver.java` | BroadcastReceiver for WiFi P2P state/peer/connection events; delegates back to MainActivity |
| `ServerClass.java` | Host-side: opens ServerSocket on port 8888, hands off accepted socket to SendReceive |
| `ClientClass.java` | Client-side: connects to host IP via socket, hands off to SendReceive |
| `SendReceive.java` | Async thread for reading/writing raw bytes over the socket |
| `GameActivity.java` | Thin container; sets fullscreen portrait, hosts GameView |
| `GameView.java` | Game loop (`invalidate()` → `draw(Canvas)`), ball physics, paddle touch input, collision detection, scoring; inner classes `Circle`, `Screen`, `Paddle` |

### Message protocol (string prefixes over socket)

| Prefix | Meaning |
|--------|---------|
| `GtwMsg` | Ball crossing device boundary (gateway handoff) |
| `NBAMsg` | Spawn new ball |
| `Sa_Dim` | Share screen dimensions |
| `All_start` | Start game signal |
| `Letsegooo` | Player ready |

### Layouts: `app/src/main/res/layout/`
`activity_main.xml` (menu) → `host.xml` / `client.xml` → `client_lobby.xml` → `activity_game.xml`

## Conventions

- Comments and some UI strings are in German.
- SpongyCastle (`com.madgag.spongycastle`) is included as a dependency but encryption is not yet implemented.
- `amountPlayers` in GameView is wired for future expansion beyond 2 devices.
