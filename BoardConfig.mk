#
# Copyright (C) 2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

DEVICE_PATH := device/xiaomi/dada
KERNEL_PATH := $(DEVICE_PATH)-kernel

# Inherit from sm8750-common
include device/xiaomi/sm8750-common/BoardConfigCommon.mk

# Display
TARGET_SCREEN_DENSITY := 520

SOONG_CONFIG_NAMESPACES += xiaomi_display
SOONG_CONFIG_xiaomi_display += \
    cwb_notify_sensor \
    dual_als_fusion
SOONG_CONFIG_xiaomi_display_cwb_notify_sensor := true
SOONG_CONFIG_xiaomi_display_dual_als_fusion := true

SOONG_CONFIG_qtidisplay += prebuilt_allocator
SOONG_CONFIG_qtidisplay_prebuilt_allocator := false

# Dtb/o
BOARD_PREBUILT_DTBOIMAGE := $(KERNEL_PATH)/dtbo.img
BOARD_PREBUILT_DTBIMAGE_DIR := $(KERNEL_PATH)/dtb

TARGET_NO_KERNEL_OVERRIDE := true
TARGET_KERNEL_SOURCE := $(KERNEL_PATH)/kernel-headers
PRODUCT_COPY_FILES += \
    $(KERNEL_PATH)/kernel:kernel

# Kernel modules
BOARD_VENDOR_RAMDISK_KERNEL_MODULES_LOAD := $(strip $(shell cat $(KERNEL_PATH)/vendor_ramdisk/modules.load))
BOARD_VENDOR_RAMDISK_RECOVERY_KERNEL_MODULES_LOAD := $(strip $(shell cat $(KERNEL_PATH)/vendor_ramdisk/modules.load.recovery))
BOARD_VENDOR_KERNEL_MODULES_LOAD := $(strip $(shell cat $(KERNEL_PATH)/vendor_dlkm/modules.load))

PRODUCT_COPY_FILES += \
    $(call find-copy-subdir-files,*,$(KERNEL_PATH)/vendor_dlkm/,$(TARGET_COPY_OUT_VENDOR_DLKM)/lib/modules) \
    $(call find-copy-subdir-files,*,$(KERNEL_PATH)/vendor_ramdisk/,$(TARGET_COPY_OUT_VENDOR_RAMDISK)/lib/modules) \
    $(call find-copy-subdir-files,*,$(KERNEL_PATH)/system_dlkm_flatten/,$(TARGET_COPY_OUT_SYSTEM_DLKM)/flatten/lib/modules) \
    $(call find-copy-subdir-files,*,$(KERNEL_PATH)/system_dlkm/,$(TARGET_COPY_OUT_SYSTEM_DLKM)/lib/modules/6.6.144-4k-APEX-Foundry-v1.4-newbase+)

# Properties
TARGET_ODM_PROP += $(DEVICE_PATH)/configs/properties/odm.prop

# SELinux
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/private
SYSTEM_EXT_PUBLIC_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/public
BOARD_VENDOR_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/vendor

# Inherit from the proprietary version
include vendor/xiaomi/dada/BoardConfigVendor.mk

# Keep the stock OS4 firmware AVB descriptors (pvmfw, countrycode, mi_ext, mi_product)
# that ABL expects in the main vbmeta image.
BOARD_AVB_MAKE_VBMETA_IMAGE_ARGS += \
    --include_descriptors_from_image vendor/xiaomi/dada/avb/os4_stock_descriptors.img

# Firmware
-include vendor/xiaomi/dada-firmware/BoardConfigVendor.mk

# Firmware partitions shipped by the device-specific firmware repository
AB_OTA_PARTITIONS += \
    abl \
    aop \
    aop_config \
    bluetooth \
    countrycode \
    cpucp \
    cpucp_dtb \
    devcfg \
    dsp \
    featenabler \
    hyp \
    idmanager \
    imagefv \
    keymaster \
    modem \
    modemfirmware \
    multiimgqti \
    pdp \
    pdp_cdb \
    pvmfw \
    qupfw \
    shrm \
    soccp_dcd \
    soccp_debug \
    spuservice \
    tz \
    uefi \
    uefisecapp \
    vm-bootsys \
    xbl \
    xbl_config \
    xbl_ramdump
