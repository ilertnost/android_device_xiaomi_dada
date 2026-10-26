/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2020 The LineageOS Project
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

package org.lineageos.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserManager;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.doze.PocketService;
import org.lineageos.settings.cloudmode.CloudModeUtils;
import org.lineageos.settings.dcdimming.DcDimmingSettingsActivity;
import org.lineageos.settings.dcdimming.DcDimmingTileService;
import org.lineageos.settings.dcdimming.DcDimmingUtils;
import org.lineageos.settings.edgefilter.EdgeFilterUtils;
import org.lineageos.settings.thermal.ThermalUtils;
import org.lineageos.settings.refreshrate.RefreshUtils;
import org.lineageos.settings.saturation.SaturationUtils;
import org.lineageos.settings.utils.ComponentUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String TAG = "XiaomiParts";
    private static final String DC_DIMMING_KEY = "dc_dimming";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (DEBUG)
            Log.d(TAG, "Received boot completed intent");

        // Per-user state: components and per-user settings keep their own copy
        // in every profile, so these stay outside the system user check.
        boolean dcDimmingSupported = DcDimmingUtils.isSupported();
        ComponentUtils.toggleComponent(context, DcDimmingSettingsActivity.class, dcDimmingSupported);
        ComponentUtils.toggleComponent(context, DcDimmingTileService.class, dcDimmingSupported);

        RefreshUtils.startService(context);

        // Pocket
        PocketService.startService(context);

        // Everything below restores device-global hardware state from the
        // preferences of the user that received the broadcast. Secondary users,
        // such as a clone profile, have their own (usually empty) preferences
        // and would otherwise reset the owner's settings to the defaults.
        UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null || !userManager.isSystemUser()) {
            if (DEBUG)
                Log.d(TAG, "Skipping device-global restoration for a secondary user");
            return;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        ThermalUtils.startService(context);

        // DC Dimming
        boolean dcDimmingEnabled = sharedPrefs.getBoolean(DC_DIMMING_KEY, false);
        DcDimmingUtils.setEnabled(dcDimmingEnabled);

        // Edge mis-touch protection
        EdgeFilterUtils.applyLater(context);

        // Wet touch and glove cloud modes
        CloudModeUtils.applyLater(context);

        // Display saturation
        SaturationUtils.restoreSaved(context);
    }
}
