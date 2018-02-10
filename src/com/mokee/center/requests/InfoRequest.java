/*
 * Copyright (C) 2016-2018 The MoKee Open Source Project
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

import android.provider.Settings;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.mokee.os.Build;

import com.mokee.center.MKCenterApplication;

public class InfoRequest extends StringRequest {

    private String mUserAgent;

    public InfoRequest(int method, String url, String userAgent,
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

        Locale mLocale = Locale.getDefault();
        String language = mLocale.getLanguage();
        String country = mLocale.getCountry();
        headers.put("Accept-Language", (language + "-" + country).toLowerCase(Locale.ENGLISH));

        return headers;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = new HashMap<String, String>();
        String unique_ids = Settings.Secure.getString(MKCenterApplication.getContext().getContentResolver(), Settings.Secure.UNIQUE_REGISTRATION_IDS);
        params.put("user_ids", unique_ids);
        return params;
    }

}
