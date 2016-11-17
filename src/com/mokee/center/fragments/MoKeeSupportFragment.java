/*
 * Copyright (C) 2014 The MoKee OpenSource Project
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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.mokee.utils.MoKeeUtils;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mokee.center.R;
import com.mokee.center.activities.MoKeeCenter;

public class MoKeeSupportFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    private static final int MENU_DONATE = 0;

    private static final String MKPUSH_PREF = "mokee_push";
    private static final String PREF_NEWS = "pref_news";

    private static final String KEY_MOKEE_WEBSITE = "mokee_website";
    private static final String KEY_MOKEE_FORUM = "mokee_forum";
    private static final String KEY_MOKEE_ISSUES = "mokee_issues";
    private static final String KEY_MOKEE_CHANGELOG = "mokee_changelog";
    private static final String KEY_MOKEE_BUILD_STATUS = "mokee_build_status";
    private static final String KEY_MOKEE_TRANSLATE = "mokee_translate";
    private static final String KEY_MOKEE_GITHUB = "mokee_github";
    private static final String KEY_MOKEE_WIKI = "mokee_wiki";
    private static final String KEY_MOKEE_NEWS = "mokee_news";

    private static final String URL_MOKEE_WEBSITE = "http://www.mokeedev.com";
    private static final String URL_MOKEE_FORUM = "http://bbs.mfunz.com";
    private static final String URL_MOKEE_ISSUES = "http://issues.mokeedev.com";
    private static final String URL_MOKEE_QUESTION = "http://bbs.mfunz.com/forum.php?mod=forumdisplay&fid=280&filter=typeid&typeid=1358";
    private static final String URL_MOKEE_CHANGELOG = "http://changelog.mokeedev.com";
    private static final String URL_MOKEE_BUILD_STATUS = "http://build.mokeedev.com";
    private static final String URL_MOKEE_TRANSLATE = "http://translate.mokeedev.com";
    private static final String URL_MOKEE_GITHUB = "https://github.com/MoKee";
    private static final String URL_MOKEE_WIKI = "http://wiki.mokeedev.com";
    public static final String URL_MOKEE_DONATE = "http://www.mokeedev.com/donate/";

    private Activity mContext;
    private SharedPreferences prefs;
    private SwitchPreference mMoKeeNewsPreferences;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getActivity();
        addPreferencesFromResource(R.xml.mokee_support);
        setHasOptionsMenu(true);
        prefs = getActivity().getSharedPreferences(MKPUSH_PREF, 0);
        mMoKeeNewsPreferences = (SwitchPreference) findPreference(KEY_MOKEE_NEWS);
        mMoKeeNewsPreferences.setOnPreferenceChangeListener(this);
        mMoKeeNewsPreferences.setChecked(prefs.getBoolean(PREF_NEWS, true));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mMoKeeNewsPreferences) {
            boolean value = (Boolean) newValue;
            prefs.edit().putBoolean(PREF_NEWS, value).apply();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (key.equals(KEY_MOKEE_WEBSITE)) {
            goToURL(mContext, URL_MOKEE_WEBSITE);
        } else if (key.equals(KEY_MOKEE_FORUM)) {
            goToURL(mContext, URL_MOKEE_FORUM);
        } else if (key.equals(KEY_MOKEE_ISSUES)) {
            goToURL(mContext, MoKeeUtils.isSupportLanguage(false) ? URL_MOKEE_QUESTION : URL_MOKEE_ISSUES);
        } else if (key.equals(KEY_MOKEE_CHANGELOG)) {
            goToURL(mContext, URL_MOKEE_CHANGELOG);
        } else if (key.equals(KEY_MOKEE_BUILD_STATUS)) {
            goToURL(mContext, URL_MOKEE_BUILD_STATUS);
        } else if (key.equals(KEY_MOKEE_TRANSLATE)) {
            goToURL(mContext, URL_MOKEE_TRANSLATE);
        } else if (key.equals(KEY_MOKEE_GITHUB)) {
            goToURL(mContext, URL_MOKEE_GITHUB);
        } else if (key.equals(KEY_MOKEE_WIKI)) {
            goToURL(mContext, URL_MOKEE_WIKI);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public static void goToURL(Activity mContext, String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        mContext.startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_DONATE, 0, R.string.menu_donate).setShowAsActionFlags(
                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DONATE:
                MoKeeCenter.donateOrRemoveAdsButton(mContext, true);
                return true;
        }
        return true;
    }

}
