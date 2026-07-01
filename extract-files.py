#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

import extract_utils.tools
from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixup_remove,
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)
from extract_utils.utils import run_cmd

CAMERA_STOCK_RUNPATH = '/vendor/lib64/camera-stock'

CAMERA_STOCK_LIBRARIES = (
    'vendor/lib64/camera-stock/android.hardware.graphics.allocator-V1-ndk.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.allocator-V2-ndk.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.allocator@2.0.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.allocator@3.0.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.allocator@4.0.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.common@1.0.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.common@1.1.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.common@1.2.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.mapper@2.0.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.mapper@2.1.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.mapper@3.0.so',
    'vendor/lib64/camera-stock/android.hardware.graphics.mapper@4.0.so',
    'vendor/lib64/camera-stock/libcamera_metadata.so',
    'vendor/lib64/camera-stock/libc++.so',
    'vendor/lib64/camera-stock/libcutils.so',
    'vendor/lib64/camera-stock/libdisplaydebug.so',
    'vendor/lib64/camera-stock/libdmabufheap.so',
    'vendor/lib64/camera-stock/libgralloc.qti.so',
    'vendor/lib64/camera-stock/libgralloccore.so',
    'vendor/lib64/camera-stock/libgralloctypes.so',
    'vendor/lib64/camera-stock/libgrallocutils.so',
    'vendor/lib64/camera-stock/libhardware.so',
    'vendor/lib64/camera-stock/libhidlbase.so',
    'vendor/lib64/camera-stock/libhidltransport.so',
    'vendor/lib64/camera-stock/libmapperutils.so',
    'vendor/lib64/camera-stock/libui.so',
    'vendor/lib64/camera-stock/libutils.so',
)

lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    # The stock camera blobs load both allocator AIDL V1 and V2 at runtime.
    # Soong cannot link two versions of the same AIDL interface into one
    # prebuilt dependency graph, so keep V1 as a runtime-only copy rule.
    (
        'android.hardware.graphics.allocator-V1-ndk',
        'android.hardware.sensors-V2-ndk',
    ): lib_fixup_remove,
}


def set_camera_stock_runpath(ctx, file, file_path, *args, **kwargs):
    run_cmd(
        [
            extract_utils.tools.patchelf_version_path_map[
                extract_utils.tools.DEFAULT_PATCHELF_VERSION
            ],
            '--set-rpath',
            CAMERA_STOCK_RUNPATH,
            file_path,
        ]
    )

namespace_imports = [
    'device/xiaomi/dada',
    'device/xiaomi/sm8750-common',
    'hardware/qcom-caf/sm8750',
    'hardware/xiaomi',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/xiaomi/sm8750-common',
]

blob_fixups: blob_fixups_user_type = {
    # Keep the camera provider on the platform vendor namespace. Pointing the
    # provider at camera-stock only affects its direct dependencies; libraries
    # loaded later by CHI/CamX still resolve from /vendor/lib64. That mixes two
    # libutils/libui/graphics ABI sets in one process and corrupts CHI request
    # state. The stock provider has no RUNPATH, so only add the tagging shim.
    'odm/bin/hw/vendor.qti.camera.provider-service_64': blob_fixup()
        .add_needed('libcamera_heap_tagging_shim.so'),
    'odm/lib64/camera/plugins/com.xiaomi.plugin.depurple.so': blob_fixup()
        .sig_replace(
            'E0 03 13 AA 65 04 00 94 60 82 01 91 5D 04 00 94',
            'E0 03 13 AA 1F 20 03 D5 60 82 01 91 5D 04 00 94',
        ),
    CAMERA_STOCK_LIBRARIES: blob_fixup()
        .call(
            set_camera_stock_runpath,
            need_tmp_dir=False,
        ),
    'vendor/bin/hw/vendor.qti.hardware.display.allocator-service': blob_fixup()
        .call(
            set_camera_stock_runpath,
            need_tmp_dir=False,
        ),
    (
        'odm/etc/camera/motiontuning.xml',
        'odm/etc/camera/snsc_bokeh_motiontuning.xml',
        'odm/etc/camera/snsc_enhance_motiontuning.xml',
        'odm/etc/camera/snsc_noface_motiontuning.xml',
        'odm/etc/camera/enhance_motiontuning.xml',
        'odm/etc/camera/snsc_motiontuning.xml'
    ): blob_fixup()
        .regex_replace('xml=version', 'xml version'),
    (
    'vendor/lib64/libcameraopt.so',
    ): blob_fixup().add_needed('libprocessgroup_shim.so'),
    (
        'odm/lib64/libMiEmojiEffect.so',
        'odm/lib64/libMiVideoFilter.so',
        'odm/lib64/libAncHumanPreviewBokeh.so',
        'odm/lib64/libTrueSight.so',
        'odm/lib64/libwa_widelens_undistort.so',
        'odm/lib64/libMiPhotoFilter.so'
    ): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_lockPlanes')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_isSupported'),
    (
       'vendor/lib64/vendor.xiaomi.hardware.camera.injection-V1-ndk.so',
       'vendor/lib64/vendor.xiaomi.hardware.camera.injection-client.so',
       'vendor/lib64/vendor.xiaomi.hardware.camera.injection-service.so',
    ): blob_fixup()
        .replace_needed(
            'android.hardware.camera.device-V1-ndk.so',
            'android.hardware.camera.device-V2-ndk.so'
        ),
    (
        'vendor/lib64/libjpegdecoder_dada.so',
        'vendor/lib64/libjpegencoder_dada.so',
    ): blob_fixup()
        .fix_soname(),
    'vendor/lib64/libultrahdr_dada.so': blob_fixup()
        .replace_needed(
            'libjpegencoder.so',
            'libjpegencoder_dada.so'
        )
        .replace_needed(
            'libjpegdecoder.so',
            'libjpegdecoder_dada.so'
        )
        .fix_soname(),
    ('odm/lib64/camera/plugins/com.xiaomi.plugin.jpegrAggr.so', 'odm/lib64/camera/plugins/com.xiaomi.plugin.gainmap.so'): blob_fixup()
        .replace_needed(
            'libultrahdr.so',
            'libultrahdr_dada.so'
        ),
    (
        'vendor/lib64/libcamera2ndk_vendor.so',
    ): blob_fixup()
        .replace_needed('android.frameworks.cameraservice.device-V2-ndk.so', 'android.frameworks.cameraservice.device-V3-ndk.so')
        .replace_needed('android.frameworks.cameraservice.service-V2-ndk.so', 'android.frameworks.cameraservice.service-V3-ndk.so')
}

module = ExtractUtilsModule(
    'dada',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    check_elf=True,
)

if __name__ == '__main__':
    utils = ExtractUtils.device_with_common(
        module, 'sm8750-common', module.vendor
    )
    utils.run()
