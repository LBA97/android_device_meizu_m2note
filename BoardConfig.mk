# include proprietary libraries and binaries
-include vendor/meizu/m2note/BoardConfigVendor.mk
 
# Bootloader
TARGET_BOOTLOADER_BOARD_NAME := mt6753
 
# needed for mass storage mode
TARGET_USE_CUSTOM_LUN_FILE_PATH := /sys/class/android_usb/android0/f_mass_storage/lun/file
 
#extracted from /proc/partinfo
BOARD_BOOTIMAGE_PARTITION_SIZE := 16777216
BOARD_RECOVERYIMAGE_PARTITION_SIZE := 20971520
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 1610612736
BOARD_CACHEIMAGE_PARTITION_SIZE := 419430400
BOARD_USERDATAIMAGE_PARTITION_SIZE := 12831948800
BOARD_FLASH_BLOCK_SIZE := 131072
 
# build kernel from source
TARGET_KERNEL_SOURCE := kernel/meizu/m2note
TARGET_KERNEL_CONFIG := cm_m2note_defconfig
BOARD_KERNEL_CMDLINE := bootopt=64S3,32N2,64N2 androidboot.selinux=permissive
BOARD_KERNEL_BASE := 0x40078000
BOARD_RAMDISK_OFFSET := 0x03f88000
BOARD_KERNEL_OFFSET := 0x00008000
BOARD_TAGS_OFFSET := 0x0df88000
BOARD_KERNEL_PAGESIZE := 2048
BOARD_MKBOOTIMG_ARGS := --kernel_offset $(BOARD_KERNEL_OFFSET) --ramdisk_offset $(BOARD_RAMDISK_OFFSET) --tags_offset $(BOARD_TAGS_OFFSET)

# system.prop
TARGET_SYSTEM_PROP := device/meizu/m2note/system.prop

# CyanogenMod Hardware Hooks
BOARD_HARDWARE_CLASS := device/meizu/m2note/cmhw/

# Bluetooth
BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := device/meizu/m2note/bluetooth


BOARD_EGL_CFG := device/meizu/m2note/configs/egl.cfg

# recovery
#TARGET_RECOVERY_INITRC := device/meizu/m2note/recovery/init.mt6753.rc
TARGET_RECOVERY_FSTAB := device/meizu/m2note/recovery/root/fstab.mt6753
TARGET_RECOVERY_LCD_BACKLIGHT_PATH := \"/sys/devices/platform/leds-mt65xx/leds/lcd-backlight/brightness\"

# Hack for build
$(shell mkdir -p $(OUT)/obj/KERNEL_OBJ/usr)
