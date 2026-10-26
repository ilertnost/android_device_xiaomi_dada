/*
 * Copyright (C) 2025 kenway214
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

package org.lineageos.settings.gamebar;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import androidx.preference.PreferenceManager;
import java.util.HashSet;
import java.util.Set;

public class GameBarMonitorService extends Service {

    private static final String PREF_PREFIX = "game_bar";
    private static final long MONITOR_INTERVAL = 2000; // 2 seconds

    private Handler mHandler;
    private Runnable mMonitorRunnable;
    private SharedPreferences mPrefs;

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key != null && key.startsWith(PREF_PREFIX)) {
                        GameBar.getInstance(GameBarMonitorService.this).applyPreferences();
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        mMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                monitorForegroundApp();
                mHandler.postDelayed(this, MONITOR_INTERVAL);
            }
        };
        mHandler.post(mMonitorRunnable);
    }

    private void monitorForegroundApp() {
        boolean show;
        if (mPrefs.getBoolean("game_bar_enable", false)) {
            show = true;
        } else if (mPrefs.getBoolean("game_bar_auto_enable", false)) {
            String foreground = ForegroundAppDetector.getForegroundPackageName(this);
            Set<String> autoApps = mPrefs.getStringSet(
                    GameBarAppSelectorFragment.PREF_AUTO_APPS, new HashSet<>());
            show = autoApps.contains(foreground);
        } else {
            show = false;
        }

        GameBar gameBar = GameBar.getInstance(this);
        if (!show) {
            gameBar.hide();
        } else if (!gameBar.isShowing()) {
            // GameBar.show() applies the current preferences itself, so only do
            // the work when the overlay is actually being shown. Re-applying
            // them on every tick would rebuild the stats views and restart the
            // update timer continuously.
            gameBar.show();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mMonitorRunnable);
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }
}
