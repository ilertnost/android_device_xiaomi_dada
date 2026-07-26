/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.dcdimming;

import android.os.SystemProperties;
import android.util.Log;

public final class DcDimmingUtils {

    private static final String TAG = "DcDimmingUtils";
    private static final String DC_DIMMING_PROPERTY = "persist.vendor.dc_backlight.enable";

    private DcDimmingUtils() {}

    public static boolean isEnabled() {
        return SystemProperties.getBoolean(DC_DIMMING_PROPERTY, false);
    }

    public static boolean setEnabled(boolean enabled) {
        try {
            SystemProperties.set(DC_DIMMING_PROPERTY, Boolean.toString(enabled));
            return isEnabled() == enabled;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to set DC dimming", e);
            return false;
        }
    }
}
