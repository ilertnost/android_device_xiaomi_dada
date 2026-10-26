/*
 * Copyright (C) 2025 The LineageOS Project
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

package org.lineageos.settings.charge;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class ChargeUtils {

    private static final String TAG = "ChargeUtils";

    public static final String BYPASS_CHARGE_NODE =
            "/sys/class/xm_power/charger/charge_interface/charge_enable";
    private static final String BYPASS_CLIENT = "parts_bypass";
    private static final String BUCK_CHARGER = "buck";
    private static final String BYPASS_ENABLED_VALUE =
            BYPASS_CLIENT + " " + BUCK_CHARGER + " 0";
    private static final String BYPASS_DISABLED_VALUE =
            BYPASS_CLIENT + " " + BUCK_CHARGER + " 1";
    private static final String BYPASS_ENABLED_STATUS =
            BUCK_CHARGER + " eff_client:" + BYPASS_CLIENT + " 0";
    private static final String BATTERY_TEMP_NODE = "/sys/class/power_supply/battery/temp";
    private static final String BATTERY_CAPACITY_NODE = "/sys/class/power_supply/battery/capacity";

    private static final int MAX_BATTERY_TEMP = 450;
    private static final int MIN_BATTERY_CAPACITY = 20;

    private static final String PREF_BYPASS_CHARGE = "bypass_charge";

    private final Context context;
    private final SharedPreferences sharedPrefs;

    public ChargeUtils(Context context) {
        this.context = context;
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isBypassChargeEnabled() {
        String value = FileUtils.readOneLine(BYPASS_CHARGE_NODE);
        return value != null && BYPASS_ENABLED_STATUS.equals(value.trim());
    }

    /**
     * Enables or disables bypass charging.
     *
     * @return the outcome of the request; the stored preference is only updated
     *         when the hardware actually accepted it
     */
    public ApplyResult enableBypassCharge(boolean enable) {
        if (enable) {
            SafetyCheckResult safetyCheck = performSafetyChecks();
            if (!safetyCheck.isSafe()) {
                Log.w(TAG, "Safety check failed: " + safetyCheck.getReason());
                return new ApplyResult(false, safetyCheck.getReason());
            }
        }

        String value = enable ? BYPASS_ENABLED_VALUE : BYPASS_DISABLED_VALUE;
        if (!FileUtils.writeLine(BYPASS_CHARGE_NODE, value)) {
            Log.e(TAG, "Failed to write bypass charge status");
            return new ApplyResult(false,
                    context.getString(R.string.charge_bypass_write_failed));
        }

        sharedPrefs.edit().putBoolean(PREF_BYPASS_CHARGE, enable).apply();
        return new ApplyResult(true, null);
    }

    private boolean isNodeAccessible(String node) {
        return FileUtils.isFileReadable(node) && FileUtils.isFileWritable(node);
    }

    public boolean isBypassChargeSupported() {
        return isNodeAccessible(BYPASS_CHARGE_NODE);
    }

    public SafetyCheckResult performSafetyChecks() {
        if (!isBypassChargeSupported()) {
            return new SafetyCheckResult(false,
                context.getString(R.string.charge_safety_not_supported));
        }

        if (!isChargerConnected()) {
            return new SafetyCheckResult(false,
                context.getString(R.string.charge_safety_no_charger));
        }

        int batteryTemp = getBatteryTemperature();
        if (batteryTemp >= MAX_BATTERY_TEMP) {
            return new SafetyCheckResult(false,
                context.getString(R.string.charge_safety_temp_high, batteryTemp / 10.0f));
        }

        int batteryLevel = getBatteryCapacity();
        if (batteryLevel < MIN_BATTERY_CAPACITY) {
            return new SafetyCheckResult(false,
                context.getString(R.string.charge_safety_low_level, batteryLevel));
        }

        return new SafetyCheckResult(true,
            context.getString(R.string.charge_safety_ok));
    }

    private boolean isChargerConnected() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        if (batteryStatus == null) {
            return false;
        }

        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean isCharging = chargePlug != 0;

        Log.d(TAG, "Charger status - plugged: " + chargePlug
                + ", connected: " + isCharging);
        return isCharging;
    }

    private int getBatteryTemperature() {
        String tempStr = FileUtils.readOneLine(BATTERY_TEMP_NODE);
        if (tempStr != null) {
            try {
                return Integer.parseInt(tempStr.trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse battery temperature from sysfs", e);
            }
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        if (batteryStatus != null) {
            int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            Log.d(TAG, "Battery temperature from BatteryManager: " + temp);
            return temp;
        }

        Log.w(TAG, "Unable to read battery temperature");
        return 0;
    }

    private int getBatteryCapacity() {
        String capacityStr = FileUtils.readOneLine(BATTERY_CAPACITY_NODE);
        if (capacityStr != null) {
            try {
                return Integer.parseInt(capacityStr.trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse battery capacity from sysfs", e);
            }
        }

        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager != null) {
            int level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            Log.d(TAG, "Battery capacity from BatteryManager: " + level);
            return level;
        }

        Log.w(TAG, "Unable to read battery capacity");
        return 0;
    }

    public static class ApplyResult {
        private final boolean success;
        private final String reason;

        public ApplyResult(boolean success, String reason) {
            this.success = success;
            this.reason = reason;
        }

        public boolean isSuccess() {
            return success;
        }

        /**
         * @return why the request was rejected, null when it succeeded
         */
        public String getReason() {
            return reason;
        }
    }

    public static class SafetyCheckResult {
        private final boolean safe;
        private final String reason;

        public SafetyCheckResult(boolean safe, String reason) {
            this.safe = safe;
            this.reason = reason;
        }

        public boolean isSafe() {
            return safe;
        }

        public String getReason() {
            return reason;
        }
    }
}
