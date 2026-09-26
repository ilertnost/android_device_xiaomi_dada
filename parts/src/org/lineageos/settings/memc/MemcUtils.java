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

package org.lineageos.settings.memc;

import android.os.SystemProperties;
import android.util.Log;

/**
 * Video enhancement is a single mutually exclusive mode (off, memc, aie, ais)
 * held in the persist.vendor.media.video.mode property.
 *
 * The media stack only reads the volatile debug.media.video.* toggles, which a
 * reboot clears. init derives them from this property through the
 * on property: blocks in init.target.rc, so they are in place before any media
 * process can read them and the choice itself survives on its own.
 */
public final class MemcUtils {

    private static final String TAG = "MemcUtils";

    /** The preference key of the mode list. */
    public static final String KEY_MODE = "memc_mode";

    public static final String MODE_OFF = "off";
    public static final String MODE_MEMC = "memc";
    public static final String MODE_AIE = "aie";
    public static final String MODE_AIS = "ais";

    /** Persisted mode; init expands it into the debug.media.video.* toggles. */
    private static final String PROP_MODE = "persist.vendor.media.video.mode";

    private MemcUtils() {}

    private static boolean isValidMode(String mode) {
        return MODE_OFF.equals(mode) || MODE_MEMC.equals(mode)
                || MODE_AIE.equals(mode) || MODE_AIS.equals(mode);
    }

    /** The stored mode is the user's choice; anything else reports off. */
    public static String getMode() {
        final String mode = SystemProperties.get(PROP_MODE, MODE_OFF);
        return isValidMode(mode) ? mode : MODE_OFF;
    }

    /**
     * Store the mode, verified by reading it back. Only the property is
     * written: init turns it into the toggles, now and on every later boot.
     * Returns false for an invalid value or a failed write, in which case
     * callers must not show the mode as applied.
     */
    public static boolean setMode(String mode) {
        if (!isValidMode(mode)) {
            return false;
        }
        try {
            SystemProperties.set(PROP_MODE, mode);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to store video enhancement mode " + mode, e);
            return false;
        }
        final String applied = getMode();
        if (!mode.equals(applied)) {
            Log.e(TAG, "Readback mismatch: requested " + mode + ", applied "
                    + applied);
            return false;
        }
        return true;
    }
}
