/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package com.slim.device.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.slim.device.KernelControl;
import com.slim.device.R;
import com.slim.device.util.FileUtils;

public class AdvanceButtonsSettings extends PreferenceActivity
        implements OnPreferenceChangeListener {
	
    private SharedPreferences sharedpreferences;

    private final String APP_PREFERENCES = "com.slim.device_preferences" ;
    private final String BUTTON_BACKLIGHT_DEFAULT_KEY = "hw_backlight_state";

    private SwitchPreference mButtonsSwap;
    private SwitchPreference mDisableBacklight;
    private SwitchPreference mNavbarToggle;
    private ListPreference mSliderTop;
    private ListPreference mSliderMiddle;
    private ListPreference mSliderBottom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.adv_buttons);

        sharedpreferences = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        mButtonsSwap = (SwitchPreference) findPreference("button_swap");
        mButtonsSwap.setOnPreferenceChangeListener(this);

        mDisableBacklight = (SwitchPreference) findPreference("disable_hw_button_backlight");
        mDisableBacklight.setOnPreferenceChangeListener(this);

        mNavbarToggle = (SwitchPreference) findPreference("enable_navigation_bar");
	boolean enabled = Settings.Secure.getIntForUser(getContentResolver(), Settings.Secure.NAVIGATION_BAR_ENABLED,
                getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar) ? 1 : 0,
                UserHandle.USER_CURRENT) == 1;
	mNavbarToggle.setChecked(enabled);
	updateButtonSwapState(!enabled);
	updateButtonBacklightState(enabled, 0);
        mNavbarToggle.setOnPreferenceChangeListener(this);

        mSliderTop = (ListPreference) findPreference("keycode_top_position");
        mSliderTop.setOnPreferenceChangeListener(this);

        mSliderMiddle = (ListPreference) findPreference("keycode_middle_position");
        mSliderMiddle.setOnPreferenceChangeListener(this);

        mSliderBottom = (ListPreference) findPreference("keycode_bottom_position");
        mSliderBottom.setOnPreferenceChangeListener(this);
    }

    private void setSummary(ListPreference preference, String file) {
        String keyCode;
        if ((keyCode = FileUtils.readOneLine(file)) != null) {
            preference.setValue(keyCode);
            preference.setSummary(preference.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String file;
        if (preference == mSliderTop) {
            file = KernelControl.KEYCODE_SLIDER_TOP;
        } else if (preference == mSliderMiddle) {
            file = KernelControl.KEYCODE_SLIDER_MIDDLE;
        } else if (preference == mSliderBottom) {
            file = KernelControl.KEYCODE_SLIDER_BOTTOM;
        } else if (preference == mButtonsSwap) {
            Boolean value = (Boolean) newValue;
            FileUtils.writeLine(KernelControl.BUTTON_SWAP_NODE, value ? "1" : "0");
            return true;
        } else if (preference == mDisableBacklight) {
            Boolean value = (Boolean) newValue;
	    updateButtonBacklightState(value, 1);
            return true;
        } else if (preference == mNavbarToggle) {
            boolean value = (Boolean) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.NAVIGATION_BAR_ENABLED, value ? 1 : 0,
                    UserHandle.USER_CURRENT);
            mNavbarToggle.setChecked(value);
	    updateButtonSwapState(!value);
	    updateButtonBacklightState(value, 0);
            FileUtils.writeLine(KernelControl.BUTTON_VIRTUAL_KEY_NODE, value ? "1" : "0");
            return true;
	} else {
            return false;
        }

        FileUtils.writeLine(file, (String) newValue);
        setSummary((ListPreference) preference, file);

        return true;
    }

    private void updateButtonSwapState(boolean enable) {
	mButtonsSwap.setEnabled(enable);
    }

    private void updateButtonBacklightState(boolean enable, int src) {
	switch (src) {
	    case 0:
		boolean originalState = sharedpreferences.getBoolean(BUTTON_BACKLIGHT_DEFAULT_KEY, false);
		if (enable) {
		    mDisableBacklight.setChecked(enable);
		    disableBacklight(enable);
		}
		else {
		    mDisableBacklight.setChecked(originalState);
		    disableBacklight(originalState);
		}
		break;
	    case 1:
		Editor editor = sharedpreferences.edit();
		editor.putBoolean(BUTTON_BACKLIGHT_DEFAULT_KEY, enable);
		editor.apply();
		disableBacklight(enable);
		break;
	}
    }

    private void disableBacklight(boolean disable) {
	FileUtils.writeLine(KernelControl.BUTTON_BACKLIGHT_NODE, disable ? "0" : "5");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Remove padding around the listview
        getListView().setPadding(0, 0, 0, 0);

        setSummary(mSliderTop, KernelControl.KEYCODE_SLIDER_TOP);
        setSummary(mSliderMiddle, KernelControl.KEYCODE_SLIDER_MIDDLE);
        setSummary(mSliderBottom, KernelControl.KEYCODE_SLIDER_BOTTOM);
    }
}
