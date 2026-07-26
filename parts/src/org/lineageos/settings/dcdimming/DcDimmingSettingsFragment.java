/*
 * Copyright (C) 2018 The LineageOS Project
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

import android.os.Bundle;
import android.content.SharedPreferences;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class DcDimmingSettingsFragment extends SettingsBasePreferenceFragment implements
        OnPreferenceChangeListener {

    private SwitchPreferenceCompat mDcDimmingPreference;
    private static final String DC_DIMMING_KEY = "dc_dimming";

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            (prefs, key) -> {
                if (DC_DIMMING_KEY.equals(key)) {
                    mDcDimmingPreference.setChecked(prefs.getBoolean(DC_DIMMING_KEY, false));
                }
            };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.dcdimming_settings, rootKey);
        mDcDimmingPreference = findPreference(DC_DIMMING_KEY);
        mDcDimmingPreference.setEnabled(true);
        mDcDimmingPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        boolean enabled = DcDimmingUtils.isEnabled();
        prefs.edit().putBoolean(DC_DIMMING_KEY, enabled).apply();
        mDcDimmingPreference.setChecked(enabled);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (DC_DIMMING_KEY.equals(preference.getKey())) {
            boolean enabled = (boolean) newValue;
            return DcDimmingUtils.setEnabled(enabled);
        }
        return true;
    }
}
