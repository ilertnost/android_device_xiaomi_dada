#
# Copyright (C) 2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Use the allocator built with the same private ABI as the stock camera stack.
TARGET_USES_PREBUILT_DISPLAY_ALLOCATOR := true

# Inherit from sm8750-common
$(call inherit-product, device/xiaomi/sm8750-common/common.mk)

# Get non-open-source specific aspects
$(call inherit-product, vendor/xiaomi/dada/dada-vendor.mk)

# MIUI Camera
$(call inherit-product, device/xiaomi/dada-miuicamera/device.mk)

# Device-specific settings
PRODUCT_PACKAGES += \
    XiaomiParts

# Android Virtualization Framework
# Stock pvmfw cannot verify the AVF remote-attestation Service VM shipped by
# the platform. Keep AVF enabled, but do not attempt unsupported AVF RKP.
PRODUCT_SYSTEM_PROPERTIES += \
    avf.remote_attestation.enabled=false

# The stock camera HAL directly loads the frozen sensors AIDL V2 while the
# platform sensorservice uses V3.
PRODUCT_PACKAGES += \
    android.hardware.sensors-V2-ndk

# Overlays
PRODUCT_PACKAGES += \
    FrameworksResDada \
    FrameworksResDadaEsim \
    SettingsOverlayDada \
    SystemUIResDada

# eSIM
PRODUCT_PACKAGES += \
    XiaomiEsimSwitcher

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.telephony.euicc.mep.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/android.hardware.telephony.euicc.mep.xml \
    $(LOCAL_PATH)/configs/permissions/privapp-permissions-EuiccGoogle.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/privapp-permissions-EuiccGoogle.xml

# Android 17 privileged permission additions for the stock camera package
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/permissions/privapp-permissions-MiuiCamera-A17.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/privapp-permissions-MiuiCamera-A17.xml

# Screen attention (adaptive sleep)
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/default-permissions/dada-default-permissions.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/default-permissions/dada-default-permissions.xml

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)
