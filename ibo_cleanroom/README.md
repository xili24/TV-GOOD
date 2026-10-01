# Xili TV Player — IBO-style clean-room build

This is a separate Android TV IPTV player implementation on branch `ibo-cleanroom-v1`.

The public repository `Silkiercomet/IBO-player` is a React/Vite marketing website and does not contain an IPTV playback engine. This module therefore implements the player functionality independently instead of copying proprietary IBO Player code or assets.

## v1 features
- Android TV / Google TV launcher
- Portal URL + Xtream username/password login
- Login persistence
- Subscription status validation
- Stable per-device locally administered MAC
- TV-focused home: Live TV / Movies / Series / Settings
- Live categories and channels
- VOD categories and movies
- Series, seasons and episode loading
- Media3/ExoPlayer playback
- Live TS -> HLS fallback
- HTTP portal support for legacy IPTV servers
- Separate GitHub Actions APK build

Use only with IPTV services and streams you are authorized to access.
