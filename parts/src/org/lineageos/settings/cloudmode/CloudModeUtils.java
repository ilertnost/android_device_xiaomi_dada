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

package org.lineageos.settings.cloudmode;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.TouchFeatureUtils;

public final class CloudModeUtils {

    private static final String TAG = "CloudModeUtils";

    private static final String KEY_WATER_PROOF = "cloud_water_proof_enabled";
    private static final String KEY_GLOVE = "cloud_glove_enabled";

    private static final boolean DEFAULT_WATER_PROOF_ENABLED = true;
    private static final boolean DEFAULT_GLOVE_ENABLED = true;
    private static final int WATER_PROOF_INIT_VALUE = 100;
    private static final int GLOVE_DEFAULT_LEVEL = 2;

    private static final int MODE_WATER_PROOF = 100;
    private static final int MODE_GLOVE = 101;

    private static final int RETRY_ATTEMPTS = 15;
    private static final long RETRY_DELAY_MS = 1000L;

    private static final Object sApplyLock = new Object();

    private CloudModeUtils() {}

    public static boolean isWaterProofEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_WATER_PROOF, DEFAULT_WATER_PROOF_ENABLED);
    }

    public static boolean isGloveEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_GLOVE, DEFAULT_GLOVE_ENABLED);
    }

    public static void setWaterProofEnabled(Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(KEY_WATER_PROOF, enabled).apply();
    }

    public static void setGloveEnabled(Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(KEY_GLOVE, enabled).apply();
    }

    /**
     * Apply the current settings synchronously on the calling thread.
     *
     * @return true if the touch feature service accepted the values
     */
    public static boolean applyAll(Context context) {
        boolean applied = true;
        applied &= applyWaterProof(context);
        applied &= applyGlove(context);
        return applied;
    }

    private static boolean applyWaterProof(Context context) {
        int value = isWaterProofEnabled(context) ? 1 : 0;
        return TouchFeatureUtils.setTouchMode(MODE_WATER_PROOF, value);
    }

    private static boolean applyGlove(Context context) {
        int value = isGloveEnabled(context) ? GLOVE_DEFAULT_LEVEL : 0;
        return TouchFeatureUtils.setTouchMode(MODE_GLOVE, value);
    }

    /**
     * Apply the current settings on a background thread, retrying while the
     * touch feature service is not yet available (e.g. right after boot).
     */
    public static void applyLater(final Context context) {
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            synchronized (sApplyLock) {
                for (int attempt = 0; attempt < RETRY_ATTEMPTS; attempt++) {
                    // Stock TouchService initializes waterproof processing with
                    // value 100 before applying the configured 0/1 state.
                    if (TouchFeatureUtils.setTouchMode(
                            MODE_WATER_PROOF, WATER_PROOF_INIT_VALUE)
                            && applyAll(appContext)) {
                        return;
                    }
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                Log.w(TAG, "Failed to apply cloud modes after " + RETRY_ATTEMPTS + " attempts");
            }
        }, "CloudModeApply").start();
    }
}
