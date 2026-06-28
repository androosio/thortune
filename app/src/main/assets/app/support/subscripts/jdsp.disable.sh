#!/system/bin/sh

SDIR="$1"
TMPFS="$SDIR/support/o2ptweaks_tmpfs"
TMPFS64="$SDIR/support/o2ptweaks_tmpfs64"
SOUNDFX_DIR=/vendor/lib/soundfx
SOUNDFX_DIR64=/vendor/lib64/soundfx

# Stop the Manager's process. We deliberately do NOT `pm disable` it — that hides
# its launcher icon and makes it awkward to find/uninstall. The DSP effect is removed
# by unmounting audio_effects.conf and restarting audioserver below, so force-stopping
# is enough to fully deactivate it while keeping the app visible.
am force-stop james.dsp

# Cleanup
for m in $(mount |grep tmpfs | grep $(basename $TMPFS)| awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
	umount -l "$m"
done

for m in $(mount |grep tmpfs | grep $(basename $TMPFS64| awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
	umount -l "$m"
done

for m in $(mount |grep tmpfs | grep "$SOUNDFX_DIR"| awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
	umount -l "$m"
done

for m in $(mount | grep tmpfs | grep "$SOUNDFX_DIR64" | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

umount /system/etc/audio_effects.conf
umount /system/vendor/etc/audio/sku_kalama/audio_effects.conf
umount /system/vendor/etc/audio/sku_kalama/audio_effects.xml

# Restart audio system
killall -q audioserver
killall -q mediaserver
