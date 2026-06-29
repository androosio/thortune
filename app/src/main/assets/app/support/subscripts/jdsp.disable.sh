#!/system/bin/sh

# Deactivate JamesDSP without disturbing the rest of the audio system.
#
# The DSP only processes audio while the Manager app holds an AudioEffect on the global
# (session 0) output mix. Force-stopping the app's process releases that effect and
# AudioFlinger tears it down, so processing stops immediately. We deliberately:
#
#   * do NOT `pm disable` it — that hides its launcher icon and makes it awkward to
#     find/uninstall;
#   * do NOT unmount audio_effects.conf or restart audioserver — an unattached, idle
#     effect costs nothing, and bouncing audioserver would glitch whatever is currently
#     playing (the very lag this avoids). The bind mounts are torn down naturally on
#     reboot (they're tmpfs), and a later enable reattaches instantly via the fast path
#     in jdsp.enable.sh with no audioserver restart.
am force-stop james.dsp
