/*
 * Copyright (C) 2014-2017 The MoKee OpenSource Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mokee.center.fragments;

import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import mokee.providers.MKSettings;

import com.mokee.center.R;

public class MoKeeSupportFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_MOKEE_NEWS = "mokee_news";

    private SwitchPreference mPushNewsPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.mokee_support);
        setHasOptionsMenu(true);
        mPushNewsPreferences = (SwitchPreference) findPreference(KEY_MOKEE_NEWS);
        mPushNewsPreferences.setOnPreferenceChangeListener(this);
        mPushNewsPreferences.setChecked(MKSettings.System.getInt(getContext().getContentResolver(), MKSettings.System.RECEIVE_PUSH_NOTIFICATIONS, 1) == 1);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPushNewsPreferences) {
            MKSettings.System.putInt(getContext().getContentResolver(), MKSettings.System.RECEIVE_PUSH_NOTIFICATIONS, (Boolean) newValue ? 1 : 0);
            return true;
        }
        return false;
    }

}
