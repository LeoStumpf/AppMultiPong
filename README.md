# Multi Pong — SpaceHats

A two-player local arcade game where each player holds their own Android phone. Place the phones side by side — the asteroid flies across both screens as if they share one continuous play field.

```
┌─────────────┐┌─────────────┐
│  HOST       ││  CLIENT     │
│  [paddle]   ││     [paddle]│
│         ☄───┼┼────►        │
│             ││             │
└─────────────┘└─────────────┘
```

No internet. No accounts. No ads. Just you, a friend, and two phones.

---

## Quick start

```bash
./build.sh debug          # debug APK → app/build/outputs/apk/debug/
./build.sh release        # signed release APK + AAB → app/build/outputs/
./build.sh clean          # wipe build outputs
```

For the first release build, set up signing first (see [Signing](#signing) below).

---

## Dependencies

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 17 or 21 | Set `JAVA_HOME` or let the Gradle wrapper find it |
| Android SDK | API 35 | Install via Android Studio or `sdkmanager` |
| Android build-tools | 35.x | Installed alongside the SDK |
| ImageMagick | 7.x | Only needed to regenerate image assets (`store_listing/graphics/`) |

### Install Android SDK without Android Studio

```bash
# macOS (Homebrew)
brew install --cask android-commandlinetools
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# Linux (Debian/Ubuntu)
sudo apt-get install android-sdk
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Then create `local.properties` in the project root:

```
sdk.dir=/path/to/your/Android/Sdk
```

(`local.properties` is git-ignored — each developer sets their own.)

---

## Build commands

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK (requires keystore.properties)
./gradlew bundleRelease          # release AAB for Play Store upload
./gradlew installDebug           # build + install to connected device via adb
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device required)
./gradlew clean                  # wipe outputs
```

---

## Signing

Google Play requires a signed release build. The signing key must be kept secret and **never committed to git**.

### 1. Generate a keystore (one-time)

```bash
keytool -genkey -v \
        -keystore multipong-release.jks \
        -alias multipong \
        -keyalg RSA -keysize 2048 -validity 10000
```

Store `multipong-release.jks` somewhere safe **outside** the repository (or in the parent directory).

### 2. Create `keystore.properties`

```bash
cp keystore.properties.example keystore.properties
# then edit keystore.properties with your actual paths and passwords
```

`keystore.properties` is in `.gitignore` — it will never be committed.

### 3. Build the signed release

```bash
./build.sh release
# or directly:
./gradlew assembleRelease bundleRelease
```

Outputs:
- `app/build/outputs/apk/release/app-release.apk` — sideload / manual install
- `app/build/outputs/bundle/release/app-release.aab` — upload to Play Console

---

## Google Play deployment checklist

Complete these steps in [Google Play Console](https://play.google.com/console) before submitting:

- [ ] **App info** — set category to *Games › Arcade*
- [ ] **Store listing** — copy text from `store_listing/en-US/`; upload graphics from `store_listing/graphics/`
- [ ] **Screenshots** — take at least 2 phone screenshots (main menu + game in progress) from real devices
- [ ] **Privacy policy** — host `store_listing/privacy_policy.md` publicly and paste the URL into *App content › Privacy policy* (required — the app uses Bluetooth)
- [ ] **Content rating** — complete the questionnaire in *App content › Content rating* (select Games, answer that it has no violence/adult content)
- [ ] **Target audience** — set to "Everyone"
- [ ] **Data safety** — declare that no user data is collected or shared
- [ ] **Release** — upload `app-release.aab` to the *Production* track (or *Internal testing* first)

---

## How it works

### Connecting

Two methods — no internet required:

1. **Bluetooth scan** — one player taps **HOST GAME**, the other taps **JOIN GAME** and selects the host from the discovered-device list.
2. **NFC tap** — tap the two phones back-to-back; the client receives the host's Bluetooth address automatically.

### Play field scaling

Each phone measures its own screen and shares dimensions with the peer. Both phones compute:

```
playFieldHeight = min(heightA / densityA, heightB / densityB) × ownDensity
```

The play field is the same **physical size** on both screens regardless of resolution or density. The phone with the larger screen shows dark letterbox bands above and below the play field. A dashed centre line helps align the phones vertically.

### Speed consistency

The game loop runs at the native screen refresh rate. Ball speed is normalised to 60 fps using `dtScale = elapsed × 60` so the asteroid moves at identical real-world speed on a 60 Hz Pixel and a 144 Hz Xiaomi.

---

## Architecture

### Connection layer

| File | Role |
|------|------|
| `MainActivity.java` | Bluetooth setup, device discovery, lobby UI, message routing |
| `ServerClass.java` | Host: opens `ServerSocket` on port 8888 and waits for client |
| `ClientClass.java` | Client: connects to host IP via socket |
| `SendReceive.java` | Background thread for reading/writing raw bytes over the socket |
| `WifiDirectBroadcastReceiver.java` | WiFi P2P state/peer/connection events |

### Game layer

| File | Role |
|------|------|
| `GameActivity.java` | Full-screen container; hosts `GameView`; LEAVE button overlay |
| `GameView.java` | Game loop, physics, bitmap rendering (animated asteroid, satellite paddle, starfield background), play-field scaling |

### Message protocol

All messages are UTF-8 strings.

| Prefix | Payload | Meaning |
|--------|---------|---------|
| `Sa_Dim` | `width>height#density<deviceIndex` | Screen dimensions exchange |
| `GtwMsg` | `targetIdx*x>normY<velX#velY~maxVelY` | Ball handoff between phones |
| `All_start` | — | Host signals game start |
| `ScoreM` | `left>right` | Score update |
| `QuitMsg` | — | Player left |

---

## App info

| Field | Value |
|-------|-------|
| Package ID | `com.spacehats.multipong` |
| Min Android | 7.0 (API 24) |
| Target Android | 15 (API 35) |
| Version | 1.0.0 (code 1) |
| Bluetooth | Required |
| NFC | Optional (tap-to-pair) |
| Internet | Not used |

---

## License

Beerware (Revision 42) — see `LICENSE`.
