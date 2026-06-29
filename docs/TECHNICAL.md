# ThorTune - technical notes

How ThorTune works under the hood, plus build and device-support detail. For the user-facing
overview see the [README](../README.md).

## What's bundled

ThorTune ships Tim Schneeberger's "rootfull" **JamesDSP Manager** fork (package `james.dsp`)
plus the native engine and the audio-effects configuration. It combines two earlier
single-purpose apps: the JamesDSP extraction from
[o2ptweaks.app](https://github.com/FeralAI/o2ptweaks.app) (based on
[jdsp4rp5.app](https://github.com/kokoko3k/jdsp4rp5.app) by kokoko3k) and the saturation
control from ThorSaturation (derived from [OdinTools](https://github.com/langerhans/OdinTools)).

## Root mechanism

There is no `su` and no Magisk module - nobody roots the Thor. Privileged work goes through the
manufacturer's **PServer binder** (`ServiceManager.getService("PServerBinder")`): ThorTune
marshals commands via `Parcel.transact()` and runs them as root that way. This is the only
privileged mechanism the app uses.

## Display saturation

Saturation persists via the `persist.sys.sf.color_saturation` system property (read by
SurfaceFlinger at boot) and is also applied immediately via a runtime SurfaceFlinger
transaction. `BootReceiver` re-issues the runtime call as a belt-and-braces fallback.

The SurfaceFlinger transaction code used for the immediate apply is AYN-firmware specific.

## JamesDSP

The first time the engine comes up each boot, `jdsp.enable.sh` bind-mounts the JamesDSP
`audio_effects.conf` over the system config and restarts the audio server once so the effect
becomes loadable; `BootReceiver` re-applies this on boot when the engine was left on.

After that, toggling the engine on or off goes through JamesDSP's own `SET_POWER_STATE`
broadcast - the same path its in-app switch and Quick Settings tile use. So no audioserver
restart (and no audio glitch) happens per toggle, and ThorTune's switch stays in lockstep with
JamesDSP's own controls. ThorTune also reads JamesDSP's persisted `powered_on` state so changes
made through JamesDSP's own switch or QS tile are reflected back in ThorTune.

## Dual screen

On the Thor's lower screen, ThorTune shows a live quick-controls panel as an Android
`Presentation` on the secondary display. It hosts the same Compose UI state as the main screen,
so a change on either screen updates the other instantly. This relies on the lower screen being
exposed as a `DISPLAY_CATEGORY_PRESENTATION` display - best-effort and unverified on every Thor
SKU.

## Supported devices

Any device whose manufacturer provides the "Run script as root" / PServer service. JamesDSP
additionally requires a Snapdragon `kalama` (8 Gen 2) audio config - confirmed on the AYN
Odin2 Portal and AYN Thor. Other SKUs may need a different `audio_effects` config under
`app/src/main/assets/app/support/conf_files/` and matching mount paths in the scripts. The
quick-controls panel appears only on devices that expose a secondary presentation display.

## Building

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (minified; APK named ThorTune_<ver>_<type>.apk)
./gradlew test               # unit tests
./gradlew lint
```

Requires an Android SDK (set `sdk.dir` in `local.properties`) and a JDK 17. min SDK 33,
target/compile 35, JVM target 17. Package `com.androosio.thortune`.
</content>
</invoke>
