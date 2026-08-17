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

package org.lineageos.settings.utils;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public final class TouchFeatureUtils {

    private static final String TAG = "TouchFeatureUtils";
    private static final String DESCRIPTOR = "vendor.xiaomi.hw.touchfeature.ITouchFeature";
    private static final String SERVICE = DESCRIPTOR + "/default";

    private static final int TRANSACTION_MODE_RESET = IBinder.FIRST_CALL_TRANSACTION + 6;
    private static final int TRANSACTION_SET_MODE_VALUE = IBinder.FIRST_CALL_TRANSACTION + 8;
    // Xiaomi's stock TouchService calls its wrapper setTouchMode(), but the
    // stable V1 AIDL service implements it through setModeValue (transaction 9).
    private static final int TRANSACTION_SET_TOUCH_MODE = TRANSACTION_SET_MODE_VALUE;
    private static final int TOUCH_ID_PRIMARY = 0;

    private TouchFeatureUtils() {}

    public static boolean resetMode(int mode) {
        return transact(TRANSACTION_MODE_RESET, mode, 0, false);
    }

    public static boolean setModeValue(int mode, int value) {
        return transact(TRANSACTION_SET_MODE_VALUE, mode, value, true);
    }

    public static boolean setTouchMode(int mode, int value) {
        return transact(TRANSACTION_SET_TOUCH_MODE, mode, value, true);
    }

    private static boolean transact(int transaction, int mode, int value, boolean hasValue) {
        IBinder service = ServiceManager.checkService(SERVICE);
        if (service == null) {
            Log.w(TAG, "TouchFeature service is unavailable");
            return false;
        }

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(TOUCH_ID_PRIMARY);
            data.writeInt(mode);
            if (hasValue) {
                data.writeInt(value);
            }
            if (!service.transact(transaction, data, reply, 0)) {
                Log.w(TAG, "TouchFeature transaction " + transaction + " failed");
                return false;
            }
            reply.readException();
            return reply.readInt() == 0;
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "TouchFeature transaction failed", e);
            return false;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }
}
