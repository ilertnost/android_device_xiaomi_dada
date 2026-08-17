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

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class CloudModeSettingsFragment extends SettingsBasePreferenceFragment implements
        OnPreferenceChangeListener {

    public static final String KEY_WATER_PROOF = "cloud_water_proof_enabled";
    public static final String KEY_GLOVE = "cloud_glove_enabled";

    private SwitchPreferenceCompat mWaterProofPreference;
    private SwitchPreferenceCompat mGlovePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.cloud_mode_settings, rootKey);

        mWaterProofPreference = findPreference(KEY_WATER_PROOF);
        mWaterProofPreference.setOnPreferenceChangeListener(this);

        mGlovePreference = findPreference(KEY_GLOVE);
        mGlovePreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_WATER_PROOF.equals(key)) {
            boolean enabled = (boolean) newValue;
            CloudModeUtils.setWaterProofEnabled(getContext(), enabled);
            CloudModeUtils.applyAll(getContext());
        } else if (KEY_GLOVE.equals(key)) {
            boolean enabled = (boolean) newValue;
            CloudModeUtils.setGloveEnabled(getContext(), enabled);
            CloudModeUtils.applyAll(getContext());
        }
        return true;
    }
}