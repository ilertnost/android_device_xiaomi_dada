/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include <android/log.h>
#include <malloc.h>

__attribute__((constructor)) static void disable_camera_heap_tagging(void) {
    const int result =
            mallopt(M_BIONIC_SET_HEAP_TAGGING_LEVEL, M_HEAP_TAGGING_LEVEL_NONE);

    if (result == 0) {
        __android_log_print(ANDROID_LOG_ERROR, "CameraHeapTaggingShim",
                            "Failed to disable heap pointer tagging");
    }
}
