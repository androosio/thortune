# JamesDSP Tweaks

A minimal Android app that enables the [JamesDSP](https://github.com/Audio4Linux/JDSP4Linux)
system-wide audio DSP on Android handhelds that expose a manufacturer **"run script as
root"** service (e.g. AYN Odin2 / Thor, Retroid Pocket devices).

It bundles Tim Schneeberger's "rootfull" **JamesDSP Manager** fork (package `james.dsp`)
plus the native engine and the audio-effects configuration, and wires up the root plumbing
to register JamesDSP into the Android audio effect chain.

This is a single-purpose extraction of the JamesDSP feature from
[o2ptweaks.app](https://github.com/FeralAI/o2ptweaks.app), which itself was based on
[jdsp4rp5.app](https://github.com/kokoko3k/jdsp4rp5.app) by kokoko3k.

## How it works

There are two ways JamesDSP gets activated, chosen automatically:

- **Temporary root** (no permanent root installed): each boot, `BootReceiver` runs
  `jdsp.enable.sh` through the manufacturer "PServer" binder, which bind-mounts the
  JamesDSP `audio_effects.conf` over the system config and restarts the audio servers.
  Toggle this with the in-app switch.
- **Permanent root** (Magisk detected): the app installs a JamesDSP Magisk module
  (`jdsp_v6.4-trimmed.zip`) that applies the same change persistently. Requires a reboot.

## Usage

1. Install and launch the app; allow the notification permission.
2. Tap **Install JamesDSP Manager** and complete the system installer.
3. Open JamesDSP Manager once (and optionally import the preset backup written to your
   Downloads folder).
4. Enable the engine: flip the **JamesDSP** switch (temporary root) or tap
   **Install JamesDSP Module** and reboot (permanent root).

## Supported devices

Any device whose manufacturer provides the "Run script as root" / PServer service and
uses a Snapdragon `kalama` (8 Gen 2) audio config — confirmed on the AYN Odin2 Portal and
AYN Thor. Other SKUs may need a different `audio_effects` config under
`app/src/main/assets/app/support/conf_files/`.

## Building

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK
```

Requires an Android SDK (set `sdk.dir` in `local.properties`). min SDK 29, target/compile 35.

## License

GNU General Public License v2.0 — see [LICENSE](LICENSE). Inherited from the upstream
projects above.
