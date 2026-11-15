# The aarch64 NDK's make is built for Termux and hardcodes its shell path;
# override it with the device's system shell.
SHELL := /system/bin/sh
include $(call all-subdir-makefiles)
