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

package com.mokee.center.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.util.Log;

import com.mokee.center.misc.Constants;
import com.mokee.center.service.UpdateCheckService;
import com.mokee.center.utils.Utils;

public class UpdateCheckReceiver extends BroadcastReceiver {

    private static final String TAG = "UpdateCheckReceiver";
    public static final String ACTION_UPDATE_CHECK = "com.mokee.center.action.UPDATE_CHECK";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Load the required settings from preferences
        SharedPreferences prefs = context.getSharedPreferences(Constants.DOWNLOADER_PREF, 0);
        int updateFrequency = prefs.getInt(Constants.UPDATE_INTERVAL_PREF, Constants.UPDATE_FREQ_DAILY);

        // Check if we are set to manual updates and don't do anything
        if (updateFrequency == Constants.UPDATE_FREQ_NONE) {
            return;
        }

        // Not set to manual updates, parse the received action
        final String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            // Connectivity has changed
            boolean hasConnection = !intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Log.i(TAG, "Got connectivity change, has connection: " + hasConnection);
            if (!hasConnection) {
                return;
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // We just booted. Store the boot check state
            prefs.edit().putBoolean(Constants.BOOT_CHECK_COMPLETED, false).apply();
        }

        // Handle the actual update check based on the defined frequency
        if (updateFrequency == Constants.UPDATE_FREQ_AT_BOOT) {
            boolean bootCheckCompleted = prefs.getBoolean(Constants.BOOT_CHECK_COMPLETED, false);
            if (!bootCheckCompleted) {
                Log.i(TAG, "Start an on-boot check");
                Intent i = new Intent(context, UpdateCheckService.class);
                i.setAction(UpdateCheckService.ACTION_CHECK);
                context.startServiceAsUser(i, UserHandle.CURRENT);
            } else {
                // Nothing to do
                Log.i(TAG, "On-boot update check was already completed.");
                return;
            }
        } else if (updateFrequency > 0) {
            Log.i(TAG, "Scheduling future, repeating update checks.");
            Utils.scheduleUpdateService(context, updateFrequency * 1000);
        } else if (ACTION_UPDATE_CHECK.equals(action)) {
            Intent i = new Intent(context, UpdateCheckService.class);
            i.setAction(UpdateCheckService.ACTION_CHECK);
            context.startServiceAsUser(i, UserHandle.CURRENT);
        }
    }
}
