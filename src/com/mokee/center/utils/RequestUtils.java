/*
 * Copyright (C) 2014-2016 The MoKee Open Source Project
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
import android.mokee.utils.MoKeeUtils;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.misc.Constants;
import com.mokee.center.requests.InfoRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class RequestUtils {

    public static void getRanking(Context mContext) {
        if (MoKeeUtils.isOnline(mContext)) {
            SharedPreferences mPrefs = mContext.getSharedPreferences(Constants.DOWNLOADER_PREF, 0);
            InfoRequest infoRequest = new InfoRequest(Request.Method.POST,
                    URI.create(mContext.getString(R.string.conf_get_ranking_server_url_def)).toASCIIString(), Utils.getUserAgentString(mContext), new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.has("amount")) {
                            int percent = Integer.valueOf(jsonObject.get("percent").toString());
                            int rank = Integer.valueOf(jsonObject.get("rank").toString());
                            float amount = Float.valueOf(jsonObject.get("amount").toString());
                            long flashTime = Long.valueOf(jsonObject.get("flash_time").toString());
                            mPrefs.edit().putInt(Constants.KEY_DONATE_PERCENT, percent)
                                    .putInt(Constants.KEY_DONATE_RANK, rank)
                                    .putFloat(Constants.KEY_DONATE_AMOUNT, amount)
                                    .putLong(Constants.KEY_FLASH_TIME, flashTime).apply();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (mPrefs.getBoolean(Constants.DONATION_FIRST_CHECK, true)) {
                        mPrefs.edit().putBoolean(Constants.DONATION_FIRST_CHECK, false).apply();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.e("Error: ", error.getMessage());
                    VolleyLog.e("Error type: " + error.toString());
                }
            });
            infoRequest.setTag("Rank");
            ((MKCenterApplication) mContext.getApplicationContext()).getQueue().add(infoRequest);
        }
    }

}
