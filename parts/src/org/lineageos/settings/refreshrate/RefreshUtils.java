/*
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.settings.refreshrate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.OrientationEventListener;

import androidx.preference.PreferenceManager;

public final class RefreshUtils {

    private static final String REFRESH_CONTROL = "refresh_control";
    private static final String REFRESH_OVERRIDE_ACTIVE = "refresh_override_active";
    private static final String REFRESH_DEFAULT_MAX = "refresh_default_max";
    private static final String REFRESH_DEFAULT_MIN = "refresh_default_min";

    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_STANDARD = 1;
    protected static final int STATE_EXTREME = 2;
    protected static final int STATE_LAND = 3;

    private static final float REFRESH_STATE_DEFAULT = 120f;
    private static final float REFRESH_STATE_STANDARD = 60f;
    private static final float REFRESH_STATE_EXTREME = 120f;
    private static final float REFRESH_STATE_LAND = 60f;

    private static final String REFRESH_STANDARD = "refresh.standard=";
    private static final String REFRESH_EXTREME = "refresh.extreme=";
    private static final String REFRESH_LAND = "refresh.land=";

    private final Context mContext;
    private final SharedPreferences mSharedPrefs;

    private float mDefaultMaxRate;
    private float mDefaultMinRate;
    private boolean mOverrideActive;
    private OrientationEventListener mOrientationListener;
    private boolean mIsLandscape;

    protected RefreshUtils(Context context) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mDefaultMaxRate = mSharedPrefs.getFloat(
                REFRESH_DEFAULT_MAX, REFRESH_STATE_DEFAULT);
        mDefaultMinRate = mSharedPrefs.getFloat(
                REFRESH_DEFAULT_MIN, REFRESH_STATE_DEFAULT);
        mOverrideActive = mSharedPrefs.getBoolean(REFRESH_OVERRIDE_ACTIVE, false);
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, RefreshService.class),
                UserHandle.CURRENT);
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(REFRESH_CONTROL, profiles).apply();
    }

    private void captureDefaultRates() {
        mDefaultMaxRate = Settings.System.getFloat(
                mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
        mDefaultMinRate = Settings.System.getFloat(
                mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_DEFAULT);
        if (mDefaultMinRate > mDefaultMaxRate) {
            mDefaultMaxRate = mDefaultMinRate;
        }
        mOverrideActive = true;
        mSharedPrefs.edit()
                .putFloat(REFRESH_DEFAULT_MAX, mDefaultMaxRate)
                .putFloat(REFRESH_DEFAULT_MIN, mDefaultMinRate)
                .putBoolean(REFRESH_OVERRIDE_ACTIVE, true)
                .apply();
    }

    private void restoreDefaultRates() {
        setRefreshRate(mDefaultMinRate, mDefaultMaxRate);
        mOverrideActive = false;
        mSharedPrefs.edit().putBoolean(REFRESH_OVERRIDE_ACTIVE, false).apply();
    }

    private void initializeOrientationListener() {
        disableOrientationListener();
        mIsLandscape = mContext.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        mOrientationListener = new OrientationEventListener(mContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return;
                }

                int currentOrientation = mContext.getResources().getConfiguration().orientation;
                boolean newIsLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);
                if (newIsLandscape != mIsLandscape) {
                    mIsLandscape = newIsLandscape;
                    adjustRefreshRateForOrientation();
                }
            }
        };

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        } else {
            mOrientationListener.disable();
        }
    }

    private void disableOrientationListener() {
        if (mOrientationListener != null) {
            mOrientationListener.disable();
            mOrientationListener = null;
        }
    }

    private void adjustRefreshRateForOrientation() {
        float maxRate = mIsLandscape ? REFRESH_STATE_LAND : REFRESH_STATE_EXTREME;
        setRefreshRate(mDefaultMinRate, maxRate);
    }

    protected void shutdown() {
        disableOrientationListener();
    }

    private void setRefreshRate(float minRate, float maxRate) {
        if (minRate > maxRate) {
            minRate = maxRate;
        }

        float currentMaxRate = Settings.System.getFloat(
                mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
        if (maxRate > currentMaxRate) {
            Settings.System.putFloat(
                    mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, maxRate);
            Settings.System.putFloat(
                    mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, minRate);
        } else {
            Settings.System.putFloat(
                    mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, minRate);
            Settings.System.putFloat(
                    mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, maxRate);
        }
    }


    private String getValue() {
        String value = mSharedPrefs.getString(REFRESH_CONTROL, null);

        if (value == null || value.isEmpty()) {
            value = REFRESH_STANDARD + ":" + REFRESH_EXTREME + ":" + REFRESH_LAND;
            writeValue(value);
        }

        String[] modes = value.split(":");
        if (modes.length < 3) {
            modes = new String[] {
                modes.length > 0 ? modes[0] : REFRESH_STANDARD,
                modes.length > 1 ? modes[1] : REFRESH_EXTREME,
                modes.length > 2 ? modes[2] : REFRESH_LAND
            };
            value = String.join(":", modes);
            writeValue(value);
        }
        return value;
    }

    protected void writePackage(String packageName, int mode) {
        String value = getValue();
        value = value.replace(packageName + ",", "");
        String[] modes = value.split(":");
        String finalString;

        switch (mode) {
            case STATE_STANDARD:
                modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_EXTREME:
                modes[1] = modes[1] + packageName + ",";
                break;
            case STATE_LAND:
                modes[2] = modes[2] + packageName + ",";
                break;

        }

        finalString = modes[0] + ":" + modes[1] + ":" + modes[2];

        writeValue(finalString);
    }

    protected int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        int state = STATE_DEFAULT;
        if (modes[0].contains(packageName + ",")) {
            state = STATE_STANDARD;
        } else if (modes[1].contains(packageName + ",")) {
            state = STATE_EXTREME;
        } else if (modes[2].contains(packageName + ",")) {
            state = STATE_LAND;
        }
        return state;
    }

    protected void setRefreshRate(String packageName) {
        int state = getStateForPackage(packageName);

        if (state == STATE_DEFAULT) {
            disableOrientationListener();
            if (mOverrideActive) {
                restoreDefaultRates();
            }
            return;
        }

        if (!mOverrideActive) {
            captureDefaultRates();
        }

        switch (state) {
            case STATE_STANDARD:
                disableOrientationListener();
                setRefreshRate(mDefaultMinRate, REFRESH_STATE_STANDARD);
                break;
            case STATE_EXTREME:
                disableOrientationListener();
                setRefreshRate(mDefaultMinRate, REFRESH_STATE_EXTREME);
                break;
            case STATE_LAND:
                initializeOrientationListener();
                adjustRefreshRateForOrientation();
                break;
            default:
                break;
        }
    }
}
