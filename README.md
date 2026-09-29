# TV GOOD — Android TV IPTV client

Original clean-room Android TV IPTV client. No Smart IPTV source code, assets, signing keys, or proprietary resources are included.

## Current TV GOOD v1 design
- Login with Portal URL + Username + Password
- Direct connection to the IPTV portal: no gateway/server required
- Xtream-style authentication through `player_api.php`
- Live categories and channels
- Search
- Short EPG
- Favorites (long-press OK)
- Media3/ExoPlayer fullscreen playback
- Uses `container_extension` and `direct_source` when provided by the portal
- Automatic TS ↔ HLS fallback and up to 3 reconnect attempts
- Android TV / Google TV / Fire TV launcher support

## Build
Toolchain used by the included GitHub Actions workflow:
- JDK 17
- Android Gradle Plugin 9.4.0
- Gradle 9.6.0
- compileSdk 36
- Android Build Tools 36.0.0
- Media3 1.11.1

The workflow builds `app-debug.apk`, which can be sideloaded on Android TV for testing.

## Build baseline (2026-09-30)
- Android Gradle Plugin 9.4.0
- Gradle 9.6.0 (GitHub Actions)
- compileSdk / targetSdk 36 (stable Android 16)
- Media3 1.11.1
- Direct Xtream portal login: Portal URL + Username + Password
