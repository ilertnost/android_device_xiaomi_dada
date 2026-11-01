#
# Copyright (C) 2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Use the allocator built with the same private ABI as the stock camera stack.
TARGET_USES_PREBUILT_DISPLAY_ALLOCATOR := false

# Inherit from sm8750-common
$(call inherit-product, device/xiaomi/sm8750-common/common.mk)

# Get non-open-source specific aspects
$(call inherit-product, vendor/xiaomi/dada/dada-vendor.mk)

# Stock camera stack: the MiuiCamera APK and camera blobs come from his vendor.
# Only the privileged permissions and hidden-API allowlist stay in the device tree.
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/permissions/privapp-permissions-miuicamera.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/privapp-permissions-miuicamera.xml \
    $(LOCAL_PATH)/configs/sysconfig/miuicamera-hiddenapi.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/sysconfig/miuicamera-hiddenapi.xml

# Device-specific settings
PRODUCT_PACKAGES += \
    XiaomiParts

# Android Virtualization Framework
# Stock pvmfw cannot verify the AVF remote-attestation Service VM shipped by
# the platform. Keep AVF enabled, but do not attempt unsupported AVF RKP.
PRODUCT_SYSTEM_PROPERTIES += \
    avf.remote_attestation.enabled=false

# The stock camera HAL loads the frozen sensors AIDL V2 library, which his
# vendor already installs as a prebuilt.

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

# Screen attention (adaptive sleep)
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/default-permissions/dada-default-permissions.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/default-permissions/dada-default-permissions.xml

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)
