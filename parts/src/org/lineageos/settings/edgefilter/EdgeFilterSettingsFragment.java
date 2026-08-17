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

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;
import org.lineageos.settings.widget.SeekBarPreference;

public class EdgeFilterSettingsFragment extends SettingsBasePreferenceFragment implements
        OnPreferenceChangeListener {

    public static final String KEY_ENABLED = "edge_filter_enabled";
    public static final String KEY_LEVEL = "edge_filter_level";

    private SwitchPreferenceCompat mEnabledPreference;
    private SeekBarPreference mLevelPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.edge_filter_settings, rootKey);

        mEnabledPreference = findPreference(KEY_ENABLED);
        mEnabledPreference.setOnPreferenceChangeListener(this);

        mLevelPreference = findPreference(KEY_LEVEL);
        if (mLevelPreference != null) {
            mLevelPreference.setOnPreferenceChangeListener(this);
        }

        updateLevelDependency();
        updateLevelSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_ENABLED.equals(key)) {
            boolean enabled = (boolean) newValue;
            EdgeFilterUtils.setEnabled(getContext(), enabled);
            EdgeFilterUtils.apply(getContext());
            updateLevelDependency(enabled);
            return true;
        } else if (KEY_LEVEL.equals(key)) {
            final int level = (Integer) newValue;
            EdgeFilterUtils.setLevel(getContext(), level);
            EdgeFilterUtils.apply(getContext());
            updateLevelSummary();
            return true;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLevelDependency();
        updateLevelSummary();
    }

    private void updateLevelDependency() {
        if (mLevelPreference != null) {
            updateLevelDependency(mEnabledPreference.isChecked());
        }
    }

    private void updateLevelDependency(boolean enabled) {
        if (mLevelPreference != null) {
            mLevelPreference.setEnabled(enabled);
        }
    }

    private void updateLevelSummary() {
        if (mLevelPreference != null) {
            mLevelPreference.setSummary(EdgeFilterUtils.getLevelSummary(getContext()));
        }
    }
}