/*
 * Copyright (C) 2014-2017 The MoKee Open Source Project
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

package com.mokee.center;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.mokee.center.activities.MoKeeCenter;
import com.mokee.center.misc.Constants;
import com.mokee.center.service.DeviceRegistrationService;
import com.mokee.center.utils.Utils;
import com.mokee.utils.CommonUtils;

public class MKCenterApplication extends Application implements
        Application.ActivityLifecycleCallbacks {

    private static Context context;
    private boolean mMainActivityActive;
    private RequestQueue mRequestQueue;
    private SharedPreferences mSharedPreferences;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!ActivityThread.currentProcessName().equals(getPackageName())) return;
        mMainActivityActive = false;
        registerActivityLifecycleCallbacks(this);
        mRequestQueue = Volley.newRequestQueue(this);
        context = getApplicationContext();

        mSharedPreferences = Utils.getDefaultSharedPreferences();
        if (mSharedPreferences.getStringSet(Constants.UNIQUE_REGISTRATION_IDS, null) != null
                && CommonUtils.hasTelephony(context)) {
            Intent intent = new Intent();
            intent.setClass(context, DeviceRegistrationService.class);
            startService(intent);
        }

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (activity instanceof MoKeeCenter) {
            mMainActivityActive = true;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (activity instanceof MoKeeCenter) {
            mMainActivityActive = false;
        }
    }

    public boolean isMainActivityActive() {
        return mMainActivityActive;
    }

    public RequestQueue getQueue() {
        return mRequestQueue;
    }

}
