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

package com.mokee.center;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpHeaders;
import com.mokee.center.activities.MoKeeCenter;
import com.mokee.center.misc.Constants;
import com.mokee.center.receiver.DownloadReceiver;
import com.mokee.center.receiver.UpdateCheckReceiver;

public class MKCenterApplication extends Application implements
        Application.ActivityLifecycleCallbacks {

    private static Context context;
    private boolean mMainActivityActive;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!ActivityThread.currentProcessName().equals(getPackageName())) return;
        mMainActivityActive = false;
        registerActivityLifecycleCallbacks(this);
        context = getApplicationContext();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put("User-Agent", Constants.USER_AGENT);
        OkGo.getInstance().init(this).addCommonHeaders(httpHeaders);

        final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

        final IntentFilter updateCheckFilter = new IntentFilter();
        updateCheckFilter.addAction(UpdateCheckReceiver.ACTION_UPDATE_CHECK);
        lbm.registerReceiver(new UpdateCheckReceiver(), updateCheckFilter);

        final IntentFilter downloadFilter = new IntentFilter();
        downloadFilter.addAction(DownloadReceiver.ACTION_DOWNLOAD_COMPLETE);
        downloadFilter.addAction(DownloadReceiver.ACTION_DOWNLOAD_START);
        lbm.registerReceiver(new DownloadReceiver(), downloadFilter);
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

}
