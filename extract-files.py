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
    'android.hardware.graphics.allocator-V2-ndk.so',
    'android.hardware.graphics.allocator@2.0.so',
    'android.hardware.graphics.allocator@3.0.so',
    'android.hardware.graphics.allocator@4.0.so',
    'android.hardware.graphics.common@1.0.so',
    'android.hardware.graphics.common@1.1.so',
    'android.hardware.graphics.common@1.2.so',
    'android.hardware.graphics.mapper@2.0.so',
    'android.hardware.graphics.mapper@2.1.so',
    'android.hardware.graphics.mapper@3.0.so',
    'android.hardware.graphics.mapper@4.0.so',
    'libcamera_metadata.so',
    'libcutils.so',
    'libdisplaydebug.so',
    'libdmabufheap.so',
    'libgralloc.qti.so',
    'libgralloccore.so',
    'libgralloctypes.so',
    'libgrallocutils.so',
    'libhardware.so',
    'libhidlbase.so',
    'libhidltransport.so',
    'libmapperutils.so',
    'libui.so',
    'libutils.so',
)

# OS3.0.6.0 camera blobs load allocator AIDL V1 at runtime alongside V2.
# Keep the stock V1 library as a copy rule, but omit it from Soong's link
# graph because Soong rejects two versions of one AIDL interface in a module.
lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
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


def camera_stock_executable_fixup():
    fixup = blob_fixup().call(
        set_camera_stock_runpath,
        need_tmp_dir=False,
    )

    for library in CAMERA_STOCK_LIBRARIES:
        fixup.add_needed(library)

    return fixup

namespace_imports = [
    'device/xiaomi/sm8750-common',
    'hardware/qcom-caf/sm8750',
    'hardware/xiaomi',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/xiaomi/sm8750-common',
]

blob_fixups: blob_fixups_user_type = {
    'odm/bin/hw/vendor.qti.camera.provider-service_64': camera_stock_executable_fixup(),
    # Xiaomi's super-night formatter routes its bounded destination through a
    # compiler-generated sprintf helper. The generated version string exceeds
    # that 128-byte destination and Android's FORTIFY aborts the entire camera
    # provider. Convert the helper to the equivalent snprintf ABI, preserving
    # the full super-night pipeline while safely truncating diagnostic text.
    'odm/lib64/libmi_supernight_raw.so': blob_fixup()
        .sig_replace(
            'E8 03 02 AA E2 03 01 AA A4 43 01 D1 E1 03 1F 2A '
            'A9 83 1F F8 A9 83 00 91 AD B3 3E A9 E3 03 08 AA '
            'A9 AB 3D A9 60 05 40 AD A0 87 3D AD 38 79 08 94',
            'E8 03 02 AA E3 03 01 AA A5 43 01 D1 E2 03 1F 2A '
            'A9 83 1F F8 A9 83 00 91 AD B3 3E A9 E4 03 08 AA '
            'A9 AB 3D A9 60 05 40 AD A0 87 3D AD 60 79 08 94',
        )
        .sig_replace(
            'E8 03 02 AA E2 03 01 AA A4 43 01 D1 E1 03 1F 2A '
            'A9 83 1F F8 A9 83 00 91 AD B3 3E A9 E3 03 08 AA '
            'A9 AB 3D A9 60 05 40 AD A0 87 3D AD A4 76 08 94',
            'E8 03 02 AA E3 03 01 AA A5 43 01 D1 E2 03 1F 2A '
            'A9 83 1F F8 A9 83 00 91 AD B3 3E A9 E4 03 08 AA '
            'A9 AB 3D A9 60 05 40 AD A0 87 3D AD CC 76 08 94',
        ),
    # alCFR_Run invalidates its instance when processing fails. Closing that
    # stale handle aborts in libmorpho_Depurple, while merely skipping cleanup
    # leaves Depurple stuck in its initialized state and degrades later frames.
    # Preserve the tuning allocation, mark the instance uninitialized, and let
    # the existing reference-count path recreate it on the next request.
    'odm/lib64/camera/plugins/com.xiaomi.plugin.depurple.so': blob_fixup()
        .sig_replace(
            'E0 03 13 AA 65 04 00 94 60 82 01 91 5D 04 00 94',
            'E0 03 13 AA 7F A2 00 B9 60 82 01 91 5D 04 00 94',
        ),
    'vendor/bin/hw/vendor.qti.hardware.display.allocator-service': camera_stock_executable_fixup(),
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
