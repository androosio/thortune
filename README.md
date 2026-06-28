# ⚡ThorTune

[![License: GPL v2](https://img.shields.io/badge/License-GPL_v2-blue.svg)](LICENSE)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-support-FFDD00?logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/androosio)

A minimal Android app for the AYN **Thor**, It bundles two essential system tweaks behind one UI:

- **Display saturation** — adjust the panel's colour intensity through SurfaceFlinger.
- **JamesDSP** — the [JamesDSP](https://github.com/Audio4Linux/JDSP4Linux) system-wide audio
  DSP, wired into the Android audio effect chain.

It bundles Tim Schneeberger's "rootfull" **JamesDSP Manager** fork (package `james.dsp`)
plus the native engine and the audio-effects configuration. It combines two earlier
single-purpose apps: the JamesDSP extraction from
[o2ptweaks.app](https://github.com/FeralAI/o2ptweaks.app) (based on
[jdsp4rp5.app](https://github.com/kokoko3k/jdsp4rp5.app) by kokoko3k) and the saturation
control from ThorSaturation (derived from [OdinTools](https://github.com/langerhans/OdinTools)).

## How it works

**Display saturation** persists via the `persist.sys.sf.color_saturation` system property
(read by SurfaceFlinger at boot) and is also applied immediately via a runtime SurfaceFlinger
transaction. `BootReceiver` re-issues the runtime call as a belt-and-braces fallback.

**JamesDSP** activates one of two ways, chosen automatically:

- **Temporary root** (no permanent root installed): each boot, `BootReceiver` runs
  `jdsp.enable.sh` through the PServer binder, which bind-mounts the JamesDSP
  `audio_effects.conf` over the system config and restarts the audio servers. Toggle this
  with the in-app switch.
- **Permanent root** (Magisk detected): the app installs a JamesDSP Magisk module
  (`jdsp_v6.4-trimmed.zip`) that applies the same change persistently. Requires a reboot.



## Usage

1. Install and launch the app; allow the notification permission.
2. **Audio** tab → **Install JamesDSP Manager**, complete the system installer, then open it
   once (optionally importing the preset backup written to your Downloads folder). Enable the
   engine with the **JamesDSP** switch (temporary root) or **Install JamesDSP module** + reboot
   (permanent root).
3. **Display** tab → drag the slider to set saturation (1.00× is neutral).
4. **Settings** tab → toggle the second-screen companion panel and check device support.

## Supported devices

Any device whose manufacturer provides the "Run script as root" / PServer service. JamesDSP
additionally requires a Snapdragon `kalama` (8 Gen 2) audio config — confirmed on the AYN
Odin2 Portal and AYN Thor. Other SKUs may need a different `audio_effects` config under
`app/src/main/assets/app/support/conf_files/`. The companion panel appears only on devices
that expose a secondary presentation display (e.g. the Thor's lower screen).

## Building

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK
```

Requires an Android SDK (set `sdk.dir` in `local.properties`). min SDK 33, target/compile 35,
JVM target 17.

## License

GNU General Public License v2.0 — see [LICENSE](LICENSE). Inherited from the upstream
projects above.
