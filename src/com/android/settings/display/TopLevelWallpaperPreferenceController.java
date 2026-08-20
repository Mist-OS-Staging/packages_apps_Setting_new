/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.display;

import static android.os.UserManager.DISALLOW_SET_WALLPAPER;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.RestrictedTopLevelPreference;

/** This controller manages the wallpaper preference of the top level page. */
public class TopLevelWallpaperPreferenceController extends BasePreferenceController {
    private static final String TAG = "TopLevelWallpaperPreferenceController";

    public TopLevelWallpaperPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setTitle(getTitle());
        }
    }

    public String getTitle() {
        return mContext.getString(R.string.wallpaper_settings_title);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        disablePreferenceIfManaged((RestrictedTopLevelPreference) preference);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            new SubSettingLauncher(mContext)
                    .setDestination("org.mist.settings.fragments.themes.Wallpaper")
                    .setSourceMetricsCategory(getMetricsCategory())
                    .setTitleRes(R.string.wallpaper_settings_title)
                    .launch();
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    private void disablePreferenceIfManaged(RestrictedTopLevelPreference pref) {
        if (pref != null) {
            pref.checkRestrictionAndSetDisabled(DISALLOW_SET_WALLPAPER);
        }
    }
}
