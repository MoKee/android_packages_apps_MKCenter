/*
 * Copyright (C) 2014-2018 The MoKee Open Source Project
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

package com.mokee.center.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.adapter.Call;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.convert.StringConvert;
import com.lzy.okgo.model.HttpParams;
import com.lzy.okgo.model.Response;
import com.mokee.center.R;
import com.mokee.center.fragments.MoKeeUpdaterFragment;
import com.mokee.center.misc.Constants;
import com.mokee.os.Build;
import com.mokee.security.RSAUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static com.mokee.center.misc.Constants.KEY_DONATION_AMOUNT;
import static com.mokee.center.misc.Constants.KEY_DONATION_CHECK_COMPLETED;
import static com.mokee.center.misc.Constants.KEY_DONATION_FLASH_TIME;
import static com.mokee.center.misc.Constants.KEY_DONATION_PERCENT;
import static com.mokee.center.misc.Constants.KEY_DONATION_RANK;

public class RequestUtils {

    public static void fetchDonationRanking(Context context) {
        OkGo.<String>post(context.getString(R.string.conf_get_ranking_server_url_def))
                .tag(Constants.DONATION_RANKING_TAG)
                .params("user_ids", Build.getUniqueIDS(context))
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        SharedPreferences mPrefs = Utils.getDonationPrefs(context);
                        try {
                            JSONObject jsonObject = new JSONObject(response.body());
                            if (jsonObject.has(KEY_DONATION_AMOUNT)) {
                                mPrefs.edit().putInt(KEY_DONATION_PERCENT, Integer.valueOf(jsonObject.get(KEY_DONATION_PERCENT).toString()))
                                        .putInt(KEY_DONATION_RANK, Integer.valueOf(jsonObject.get(KEY_DONATION_RANK).toString()))
                                        .putFloat(KEY_DONATION_AMOUNT, Float.valueOf(jsonObject.get(KEY_DONATION_AMOUNT).toString()))
                                        .putLong(KEY_DONATION_FLASH_TIME, Long.valueOf(jsonObject.get(KEY_DONATION_FLASH_TIME).toString())).apply();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (!mPrefs.getBoolean(KEY_DONATION_CHECK_COMPLETED, false)) {
                            mPrefs.edit().putBoolean(KEY_DONATION_CHECK_COMPLETED, true).apply();
                        }
                    }
                });
    }

    public static Call<String> fetchChangeLog(String url) {
        return OkGo.<String>get(url).tag(Constants.CHANGELOG_TAG).converter(new StringConvert()).adapt();
    }

    public static void fetchAvailableUpdates(Context context, String url, StringCallback callback) {
        HttpParams params = new HttpParams();
        // Get the type of update we should check for
        SharedPreferences mPrefs = Utils.getMainPrefs(context);
        String releaseVersionType = Utils.getReleaseVersionType();
        int suggestUpdateType = Utils.getUpdateType(releaseVersionType);
        int configUpdateType = mPrefs.getInt(Constants.UPDATE_TYPE_PREF, suggestUpdateType);

        if (configUpdateType == 2 && suggestUpdateType != 2) { // 若当前不是测试版，则取消显示测试版选项并重置当前更新类型设置。
            mPrefs.edit().putBoolean(MoKeeUpdaterFragment.EXPERIMENTAL_SHOW, false).putInt(Constants.UPDATE_TYPE_PREF, suggestUpdateType).apply();
            configUpdateType = suggestUpdateType;
        } else if (configUpdateType == 3 && suggestUpdateType != 3) { // 若当前不是适配版，则重置当前更新类型设置。
            mPrefs.edit().putInt(Constants.UPDATE_TYPE_PREF, suggestUpdateType).apply();
            configUpdateType = suggestUpdateType;
        }
        // disable ota option if never donation
        boolean isOTACheck = mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false);
        if (isOTACheck) {
            if (!Utils.checkMinLicensed(context)) {
                mPrefs.edit().putBoolean(Constants.OTA_CHECK_PREF, false).apply();
            }
        } else {
            params.put("device_officail", String.valueOf(configUpdateType));
            params.put("rom_all", "0");
        }
        // disable verify option when never donation
        boolean isVerifyRom = mPrefs.getBoolean(Constants.VERIFY_ROM_PREF, false);
        if (isVerifyRom) {
            if (Utils.getPaidTotal(context) < Constants.DONATION_TOTAL) {
                mPrefs.edit().putBoolean(Constants.VERIFY_ROM_PREF, false).apply();
            } else {
                params.put("is_verified", 1);
            }
        }

        try {
            params.put("device_name", RSAUtils.rsaEncryptByPublicKey(Build.PRODUCT));
            params.put("device_version", RSAUtils.rsaEncryptByPublicKey(Build.VERSION));
        } catch (Exception e) {
        }
        params.put("build_user", android.os.Build.USER);
        params.put("is_encrypted", "1");

        if (Utils.checkMinLicensed(context)) {
            String unique_id = Build.getUniqueID(context);
            params.put("user_id", unique_id);
        }
        OkGo.<String>post(url).tag(Constants.AVAILABLE_UPDATES_TAG).params(params).execute(callback);
    }

}
