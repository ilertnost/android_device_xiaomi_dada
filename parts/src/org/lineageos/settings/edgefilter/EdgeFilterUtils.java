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

package org.lineageos.settings.edgefilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;
import org.lineageos.settings.thermal.Constants;
import org.lineageos.settings.utils.TouchFeatureUtils;

public final class EdgeFilterUtils {

    private static final String TAG = "EdgeFilterUtils";

    private static final String KEY_ENABLED = "edge_filter_enabled";
    private static final String KEY_LEVEL = "edge_filter_level";

    private static final boolean DEFAULT_ENABLED = true;
    private static final int DEFAULT_LEVEL = 2;
    private static final int MAX_LEVEL = 3;

    private static final int RETRY_ATTEMPTS = 15;
    private static final long RETRY_DELAY_MS = 1000L;

    private static final Object sApplyLock = new Object();

    private EdgeFilterUtils() {}

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }

    public static int getLevel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Math.max(0, Math.min(MAX_LEVEL, prefs.getInt(KEY_LEVEL, DEFAULT_LEVEL)));
    }

    public static void setEnabled(Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static void setLevel(Context context, int level) {
        level = Math.max(0, Math.min(MAX_LEVEL, level));
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(KEY_LEVEL, level).apply();
    }

    public static String getLevelSummary(Context context) {
        switch (getLevel(context)) {
            case 0:
                return context.getString(R.string.edge_filter_level_off);
            case 1:
                return context.getString(R.string.edge_filter_level_low);
            case 3:
                return context.getString(R.string.edge_filter_level_high);
            default:
                return context.getString(R.string.edge_filter_level_medium);
        }
    }

    /**
     * Apply the current setting synchronously on the calling thread.
     *
     * @return true if the touch feature service accepted the value
     */
    public static boolean apply(Context context) {
        int value = isEnabled(context) ? getLevel(context) : 0;
        return TouchFeatureUtils.setModeValue(Constants.MODE_TOUCH_EDGE_FILTER, value);
    }

    /**
     * Apply the current setting on a background thread, retrying while the
     * touch feature service is not yet available (e.g. right after boot).
     */
    public static void applyLater(final Context context) {
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            synchronized (sApplyLock) {
                for (int attempt = 0; attempt < RETRY_ATTEMPTS; attempt++) {
                    if (apply(appContext)) {
                        return;
                    }
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                Log.w(TAG, "Failed to apply edge filter after " + RETRY_ATTEMPTS + " attempts");
            }
        }, "EdgeFilterApply").start();
    }
}