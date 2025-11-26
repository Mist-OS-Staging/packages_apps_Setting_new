package org.lineageos.settings.mistify;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class MiuiControlCenterSettings extends SettingsPreferenceFragment {

    private static final String KEY_MIUI_CONTROL_CENTER = "miui_control_center_enabled";
    private SwitchPreference mMiuiControlCenterPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.miui_control_center_settings);

        mMiuiControlCenterPref = findPreference(KEY_MIUI_CONTROL_CENTER);
        if (mMiuiControlCenterPref != null) {
            mMiuiControlCenterPref.setChecked(isMiuiControlCenterEnabled());
            mMiuiControlCenterPref.setOnPreferenceChangeListener(this::onPreferenceChange);
        }
    }

    private boolean isMiuiControlCenterEnabled() {
        return Settings.Secure.getInt(getContentResolver(), KEY_MIUI_CONTROL_CENTER, 0) == 1;
    }

    private boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_MIUI_CONTROL_CENTER.equals(preference.getKey())) {
            boolean enabled = (Boolean) newValue;
            Settings.Secure.putInt(getContentResolver(), KEY_MIUI_CONTROL_CENTER, enabled ? 1 : 0);
            
            // Restart SystemUI to apply changes
            restartSystemUI();
            return true;
        }
        return false;
    }

    private void restartSystemUI() {
        try {
            Runtime.getRuntime().exec("pkill -f com.android.systemui");
        } catch (Exception e) {
            // Fallback method
            try {
                Runtime.getRuntime().exec("killall com.android.systemui");
            } catch (Exception ex) {
                // Silent fail
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return -1; // Custom category
    }
}
