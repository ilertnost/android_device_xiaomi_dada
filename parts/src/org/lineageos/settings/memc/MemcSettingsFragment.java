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

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class MemcSettingsFragment extends SettingsBasePreferenceFragment implements
        OnPreferenceChangeListener {

    private ListPreference mModePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.memc_settings, rootKey);

        mModePreference = findPreference(MemcUtils.KEY_MODE);
        mModePreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reflect the stored mode: the toggles init derives from it are not a
        // reliable source once a media process has cached them, and the
        // preference is non-persistent, so this only updates the display.
        mModePreference.setValue(MemcUtils.getMode());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!MemcUtils.KEY_MODE.equals(preference.getKey())) {
            return true;
        }
        final String mode = (String) newValue;
        // Store first; setMode rejects invalid values and verifies the stored
        // value by reading it back. init turns it into the toggles the media
        // stack reads.
        if (!MemcUtils.setMode(mode)) {
            // Roll the selection back to the stored mode.
            mModePreference.setValue(MemcUtils.getMode());
            return false;
        }
        return true;
    }
}
