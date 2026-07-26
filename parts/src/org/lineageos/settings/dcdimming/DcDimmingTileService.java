/*
* Copyright (C) 2018 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.lineageos.settings.dcdimming;

import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.preference.PreferenceManager;

public class DcDimmingTileService extends TileService {

    private static final String DC_DIMMING_KEY = "dc_dimming";

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            (prefs, key) -> {
                if (DC_DIMMING_KEY.equals(key)) {
                    updateUI(prefs.getBoolean(DC_DIMMING_KEY, false));
                }
            };

    private void updateUI(boolean enabled) {
        final Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        boolean enabled = DcDimmingUtils.isEnabled();
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(DC_DIMMING_KEY, enabled).apply();
        updateUI(enabled);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();

        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);

        final boolean enabled = !DcDimmingUtils.isEnabled();
        if (DcDimmingUtils.setEnabled(enabled)) {
            sharedPrefs.edit().putBoolean(DC_DIMMING_KEY, enabled).apply();
        } else {
            updateUI(!enabled);
        }
    }
}
