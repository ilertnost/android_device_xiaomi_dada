/*
 * Copyright (C) 2024 The LineageOS Project
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

package org.lineageos.settings.saturation;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;

/**
 * Applies the display saturation boost through the SurfaceFlinger transaction
 * the stock settings app uses. The factor only lives in SurfaceFlinger memory,
 * so the saved value has to be re-applied after every SurfaceFlinger restart.
 */
public final class SaturationUtils {

    private static final String TAG = "SaturationUtils";

    private static final String SURFACE_FLINGER_SERVICE = "SurfaceFlinger";
    private static final String SURFACE_COMPOSER_INTERFACE = "android.ui.ISurfaceComposer";
    private static final int TRANSACTION_SET_SATURATION = 1022;

    private static final int DEFAULT_VALUE = 100;

    private SaturationUtils() {}

    /**
     * @return the saturation (in percent) the user selected, 100 being the default
     */
    private static int getSavedValue(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Constants.KEY_SATURATION, DEFAULT_VALUE);
    }

    /**
     * Applies the persisted saturation on the calling thread.
     *
     * @return true if SurfaceFlinger accepted the value
     */
    public static boolean restoreSaved(Context context) {
        return setSaturation(getSavedValue(context));
    }

    /**
     * @param value saturation in percent, 100 being the default
     * @return true if SurfaceFlinger accepted the value
     */
    public static boolean setSaturation(int value) {
        IBinder surfaceFlinger = ServiceManager.getService(SURFACE_FLINGER_SERVICE);
        if (surfaceFlinger == null) {
            Log.w(TAG, "SurfaceFlinger is not available");
            return false;
        }

        // SurfaceFlinger clamps the factor to [0.0, 2.0] and treats 1.0 as "no
        // boost", so the stock implementation nudges the neutral value up.
        float saturation = value == DEFAULT_VALUE ? 1.001f : value / 100.0f;

        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE);
            data.writeFloat(saturation);
            surfaceFlinger.transact(TRANSACTION_SET_SATURATION, data, null, 0);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to apply saturation", e);
            return false;
        } finally {
            data.recycle();
        }
    }
}
