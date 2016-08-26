$(call inherit-product, device/mediatek/mt6753_common/device_mt6753.mk)
$(call inherit-product, vendor/meizu/m2note/m2note-vendor.mk)

DEVICE_PACKAGE_OVERLAYS += device/meizu/m2note/overlay

# Device uses high-density artwork where available
PRODUCT_AAPT_CONFIG := normal xhdpi xxhdpi
PRODUCT_AAPT_PREF_CONFIG := xxhdpi

# Recovery allowed devices
TARGET_OTA_ASSERT_DEVICE := m2note,m571,m2n,meizu6753_65c_l1

# init.rc's
PRODUCT_COPY_FILES += \
	device/meizu/m2note/rootdir/init.mt6735.rc:root/init.mt6735.rc \
	device/meizu/m2note/rootdir/init.ssd.rc:root/init.ssd.rc \
	device/meizu/m2note/rootdir/init.xlog.rc:root/init.xlog.rc \
	device/meizu/m2note/rootdir/init.rc:root/init.rc \
	device/meizu/m2note/rootdir/init.mt6735.usb.rc:root/init.mt6735.usb.rc \
	device/meizu/m2note/rootdir/init.recovery.mt6735.rc:root/init.recovery.mt6735.rc \
	device/meizu/m2note/rootdir/init.project.rc:root/init.project.rc \
	device/meizu/m2note/rootdir/init.modem.rc:root/init.modem.rc \
	device/meizu/m2note/recovery/root/fstab.mt6753:root/fstab.mt6735  \
	device/meizu/m2note/rootdir/ueventd.mt6735.rc:root/ueventd.mt6735.rc \
	device/meizu/m2note/rootdir/factory_init.rc:root/factory_init.rc \
	device/meizu/m2note/rootdir/factory_init.project.rc:root/factory_init.project.rc \
	device/meizu/m2note/rootdir/meta_init.project.rc:root/meta_init.project.rc \
	device/meizu/m2note/rootdir/meta_init.modem.rc:root/meta_init.modem.rc \
	device/meizu/m2note/rootdir/meta_init.rc:root/meta_init.rc 

# TWRP
PRODUCT_COPY_FILES += device/meizu/m2note/recovery/twrp.fstab:recovery/root/etc/twrp.fstab

PRODUCT_COPY_FILES += \
    	device/meizu/m2note/rootdir/etc/hostapd_default.conf:system/etc/hostapd/hostapd_default.conf \

# limit dex2oat threads to improve thermals
PRODUCT_PROPERTY_OVERRIDES += \
    	dalvik.vm.boot-dex2oat-threads=4 \
    	dalvik.vm.dex2oat-threads=2 \
    	dalvik.vm.image-dex2oat-threads=4

$(call inherit-product, build/target/product/full.mk)
