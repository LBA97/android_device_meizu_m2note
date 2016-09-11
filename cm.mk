# Release name
PRODUCT_RELEASE_NAME := m2note

# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Inherit device configuration
$(call inherit-product, device/meizu/m2note/device_m2note.mk)

# Include generic Mediatek MT6753 part
$(call inherit-product, device/mediatek/mt6753_common/BoardConfigMT6753_common.mk)

# Include TWRP part
$(call inherit-product, device/meizu/m2note/twrp.mk)

# Configure dalvik heap
$(call inherit-product, frameworks/native/build/phone-xxhdpi-2048-dalvik-heap.mk)

# Configure hwui memory
$(call inherit-product, frameworks/native/build/phone-xxhdpi-2048-hwui-memory.mk)

TARGET_SCREEN_HEIGHT := 1920
TARGET_SCREEN_WIDTH := 1080

## Device identifier. This must come after all inclusions
PRODUCT_DEVICE := m2note
PRODUCT_NAME := cm_m2note
PRODUCT_BRAND := MEIZU
PRODUCT_MODEL := M2 Note
PRODUCT_MANUFACTURER := MEIZU
