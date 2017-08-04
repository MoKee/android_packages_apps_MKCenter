/*
 * Copyright (C) 2014-2015 The MoKee Open Source Project
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

package com.mokee.center.requests;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.mokee.os.Build;

import com.mokee.center.MKCenterApplication;
import com.mokee.center.fragments.MoKeeUpdaterFragment;
import com.mokee.center.misc.Constants;
import com.mokee.center.utils.Utils;

public class UpdatesRequest extends StringRequest {
    private String mUserAgent;

    public UpdatesRequest(int method, String url, String userAgent,
            Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        mUserAgent = userAgent;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<String, String>();
        if (mUserAgent != null) {
            headers.put("User-Agent", mUserAgent);
        }
        headers.put("Cache-Control", "no-cache");

        Locale mLocale = MKCenterApplication.getContext().getResources().getConfiguration().locale;
        String language = mLocale.getLanguage();
        String country = mLocale.getCountry();
        headers.put("Accept-Language", (language + "-" + country).toLowerCase(Locale.ENGLISH));

        return headers;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = new HashMap<String, String>();
        // Get the type of update we should check for
        SharedPreferences prefs = MKCenterApplication.getContext().getSharedPreferences(Constants.DOWNLOADER_PREF, 0);
        String releaseVersionType = Utils.getReleaseVersionType();
        boolean isExperimental = TextUtils.equals(releaseVersionType, "experimental");
        boolean isUnofficial = TextUtils.equals(releaseVersionType, "unofficial");
        boolean isHistory = TextUtils.equals(releaseVersionType, "history");
        boolean experimentalShow = prefs.getBoolean(MoKeeUpdaterFragment.EXPERIMENTAL_SHOW, isExperimental);
        int updateType = prefs.getInt(Constants.UPDATE_TYPE_PREF, Utils.getUpdateType(releaseVersionType));// 版本类型参数
        if (updateType == 2 && !experimentalShow) {
            prefs.edit().putBoolean(MoKeeUpdaterFragment.EXPERIMENTAL_SHOW, false).putInt(Constants.UPDATE_TYPE_PREF, 0).apply();
            updateType = 0;
        }
        if (updateType == 3 && !isUnofficial) {
            prefs.edit().putInt(Constants.UPDATE_TYPE_PREF, 0).apply();
            updateType = 0;
        }
        // disable ota option at old version or never donation
        boolean isOTA = prefs.getBoolean(Constants.OTA_CHECK_PREF, false);
        if (isOTA && !prefs.getBoolean(Constants.OTA_CHECK_MANUAL_PREF, false)) {
            String nowDate = Utils.subBuildDate(Build.VERSION, false);
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
            try {
                long nowVersionDate = Long.valueOf(sdf.parse(nowDate).getTime());
                long nowSystemDate = System.currentTimeMillis();
                if (!isExperimental || !isHistory || !isUnofficial) {
                    if (nowVersionDate + Utils.getVersionLifeTime(releaseVersionType) < nowSystemDate || !Utils.checkMinLicensed(MKCenterApplication.getContext())) {
                        prefs.edit().putBoolean(Constants.OTA_CHECK_PREF, false).apply();
                        isOTA = false;
                    }
                }
            } catch (ParseException exception) {
            }
        }
        prefs.edit().putBoolean(Constants.OTA_CHECK_MANUAL_PREF, false).apply();

        // disable verify option when never donation
        boolean isVerifyRom = prefs.getBoolean(Constants.VERIFY_ROM_PREF, false);
        if (isVerifyRom) {
            if (Utils.getPaidTotal(MKCenterApplication.getContext()) < Constants.DONATION_TOTAL) {
                prefs.edit().putBoolean(Constants.VERIFY_ROM_PREF, false).apply();
                isVerifyRom = false;
            }
        }
        if (isVerifyRom) {
            params.put("is_verified", "1");
        }

        params.put("device_name", Build.PRODUCT);
        params.put("device_version", Build.VERSION);
        params.put("build_user", android.os.Build.USER);
        if (!isOTA) {
            params.put("device_officail", String.valueOf(updateType));
            params.put("rom_all", "0");
        }
        if (Utils.checkMinLicensed(MKCenterApplication.getContext())) {
            String unique_id = Build.getUniqueID(MKCenterApplication.getContext());
            params.put("user_id", unique_id);
            String unique_id_external = Build.getUniqueID(MKCenterApplication.getContext(), 0);
            if (!TextUtils.equals(unique_id, unique_id_external)) {
                params.put("user_id_external", unique_id_external);
            }
        }
        return params;
    }
}
