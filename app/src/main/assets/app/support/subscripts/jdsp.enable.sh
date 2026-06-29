#!/system/bin/sh

SDIR="$1"
TMPFS="$SDIR/support/o2ptweaks_tmpfs"
TMPFS64="$SDIR/support/o2ptweaks_tmpfs64"
SOUNDFX_DIR=/vendor/lib/soundfx
SOUNDFX_DIR64=/vendor/lib64/soundfx

ENGINE="james.dsp/me.timschneeberger.rootlessjamesdsp.activity.EngineLauncherActivity"
POWER_RX="james.dsp/me.timschneeberger.rootlessjamesdsp.receiver.PowerStateReceiver"
POWER_ACT="me.timschneeberger.rootlessjamesdsp.SET_POWER_STATE"

# Bring the engine up and powered on, in sync with JamesDSP's own controls.
#
# Two steps that mirror exactly what the Manager itself does on its switch / QS tile:
#   1. Ensure the foreground service is running so the AudioEffect is attached on the global
#      (session 0) mix. If the process is already alive we skip this — re-launching the engine
#      activity would needlessly steal focus from whatever's in the foreground (e.g. a game on
#      the top screen when toggling from the companion panel). Starting it via root `am` is also
#      what lets this work while ThorTune is backgrounded (legal activity + foreground-service
#      start).
#   2. Power on via the SET_POWER_STATE broadcast. This is what guarantees "on" even if the user
#      had previously switched JamesDSP off (which persists powered_on=false); a bare service
#      start would leave it off. Harmless when already on.
activate_engine() {
  if ! pidof james.dsp >/dev/null 2>&1; then
    am start $ENGINE
    sleep 1
  fi
  am broadcast -a $POWER_ACT --ez rootlessjamesdsp.enabled true -n $POWER_RX
}

# --- Fast path: effect already loaded -------------------------------------------------
#
# JamesDSP only *processes* audio while the Manager app holds an AudioEffect on the global
# (session 0) output mix; the audio_effects.conf we bind-mount merely makes that effect
# *loadable*. Once the config + libjamesdsp.so are mounted and audioserver has re-read them,
# turning the DSP on/off is purely a matter of the app attaching/releasing that AudioEffect
# — no audioserver restart needed. So if our effect library is already mounted into the
# system soundfx dir (only our bind mount puts libjamesdsp.so there), we skip the heavy,
# audioserver-restarting load and just (re)activate the engine. This is what makes repeated
# toggles glitch-free for whatever audio is currently playing.
if [ -f "$SOUNDFX_DIR64/libjamesdsp.so" ] && [ -f "$SOUNDFX_DIR/libjamesdsp.so" ]; then
  echo "JamesDSP effect already loaded; activating engine only"
  activate_engine
  exit 0
fi

# --- Heavy path: load the effect (one-time per boot) ----------------------------------
#
# Reached on first enable after a (re)boot — tmpfs bind mounts don't survive a reboot — or
# after the config changed. This restarts audioserver so it re-reads audio_effects.conf,
# which briefly disrupts any active audio; at boot nothing is playing, so it's invisible.

echo "Start cleanup"

for m in $(mount | grep tmpfs | grep $(basename $TMPFS) | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

for m in $(mount | grep tmpfs | grep $(basename $TMPFS64) | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

for m in $(mount | grep tmpfs | grep "$SOUNDFX_DIR" | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

for m in $(mount | grep tmpfs | grep "$SOUNDFX_DIR64" | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

umount /system/etc/audio_effects.conf
umount /system/vendor/etc/audio/sku_kalama/audio_effects.conf
umount /system/vendor/etc/audio/sku_kalama/audio_effects.xml

echo "End cleanup"


# Registers JamesDSP library in the Android Audio effect chain
echo "Mounting audio_effects.conf"
mount -o bind $SDIR/support/conf_files/audio_effects-jdsp.conf /system/etc/audio_effects.conf
chown root:root /system/etc/audio_effects.conf
chmod 0644      /system/etc/audio_effects.conf
chcon u:object_r:vendor_configs_file:s0 /system/etc/audio_effects.conf

echo "Mounting sku_kalama/audio_effects.conf"
mount -o bind $SDIR/support/conf_files/sku_kalama/audio_effects-jdsp.conf /system/vendor/etc/audio/sku_kalama/audio_effects.conf
chown root:root /system/vendor/etc/audio/sku_kalama/audio_effects.conf
chmod 0644      /system/vendor/etc/audio/sku_kalama/audio_effects.conf
chcon u:object_r:vendor_configs_file:s0 /system/vendor/etc/audio/sku_kalama/audio_effects.conf

echo "Mounting sku_kalama/audio_effects.xml"
mount -o bind $SDIR/support/conf_files/sku_kalama/audio_effects-jdsp.xml /system/vendor/etc/audio/sku_kalama/audio_effects.xml
chown root:root /system/vendor/etc/audio/sku_kalama/audio_effects.xml
chmod 0644      /system/vendor/etc/audio/sku_kalama/audio_effects.xml
chcon u:object_r:vendor_configs_file:s0 /system/vendor/etc/audio/sku_kalama/audio_effects.xml


# Create TMPFS partitions for audio effects
if [ ! -d "$TMPFS" ]; then
	echo "Creating mountpoint $TMPFS"
	mkdir "$TMPFS"
fi
mount -t tmpfs tmpfs $TMPFS

if [ ! -d "$TMPFS64" ]; then
	echo "Creating mountpoint $TMPFS64"
	mkdir "$TMPFS64"
fi
mount -t tmpfs tmpfs $TMPFS64

# Copy effects libraries to TMPFS folders
echo "Copying sfx libs to TMPFS"
VDIR="$SDIR/support/libs"
cp $VDIR/libjamesdsp.so $TMPFS/
cp -av $SOUNDFX_DIR/* $TMPFS/
cp $VDIR/libjamesdsp.so $TMPFS64/
cp -av $SOUNDFX_DIR64/* $TMPFS64/

# bind mount the cooked TMPFS over the system soundfx dir
echo "Mount TMPFS"
mount -o bind $TMPFS $SOUNDFX_DIR
mount -o bind $TMPFS64 $SOUNDFX_DIR64

# Set permissions and SELinux context
echo "Set TMPFS permissions"
chown root:root $SOUNDFX_DIR/*
chmod 0644      $SOUNDFX_DIR/*
chcon u:object_r:vendor_configs_file:s0 $SOUNDFX_DIR/*
chown root:root $SOUNDFX_DIR64/*
chmod 0644      $SOUNDFX_DIR64/*
chcon u:object_r:vendor_configs_file:s0 $SOUNDFX_DIR64/*

#Enable hidden API
settings put global hidden_api_policy 1

# Restart audio system so audioserver re-reads the freshly-mounted audio_effects.conf and
# can load libjamesdsp.so. This is the only step that disrupts active audio, and it only
# runs on the heavy (first-load-per-boot) path. mediaserver is deliberately NOT killed: on
# this platform the effect-chain reload is driven entirely by audioserver, and killing
# mediaserver only adds pointless disruption.
echo "Restarting audioserver"
killall -q audioserver

# Enabled JamesDSP
pm enable james.dsp

# Note: we deliberately do NOT pm grant POST_NOTIFICATIONS here. The engine runs fine
# without it (the foreground-service notification is simply hidden), and re-granting it on
# every boot/engine-enable would override a user who has chosen to silence JamesDSP. The
# preset-import flow grants it once, at setup time, only where it actually prevents a crash.

# Wait for audioserver to respawn with the freshly-mounted effect chain before
# launching the engine. Without this, EngineLauncherActivity tries to attach to
# the JamesDSP effect before audioserver has reloaded audio_effects.conf and
# flashes an attach-error toast (JDSP load fail, session=0).
sleep 3

# Attach the effect on the global session and power it on.
activate_engine
