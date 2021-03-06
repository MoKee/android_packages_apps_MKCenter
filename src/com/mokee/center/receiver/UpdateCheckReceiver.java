/*
 * Copyright (C) 2014-2018 The MoKee OpenSource Project
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
import android.util.Log;

import com.mokee.center.misc.Constants;
import com.mokee.center.service.UpdateCheckService;
import com.mokee.center.utils.RequestUtils;
import com.mokee.center.utils.Utils;

import static com.mokee.center.misc.Constants.KEY_DONATION_AMOUNT;
import static com.mokee.center.misc.Constants.KEY_DONATION_CHECK_COMPLETED;
import static com.mokee.center.misc.Constants.KEY_DONATION_PERCENT;
import static com.mokee.center.misc.Constants.KEY_DONATION_RANK;

public class UpdateCheckReceiver extends BroadcastReceiver {

    public static final String ACTION_UPDATE_CHECK = "com.mokee.center.action.UPDATE_CHECK";

    private static final String TAG = "UpdateCheckReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        // Load the required settings from preferences
        SharedPreferences prefs = context.getSharedPreferences(Constants.DOWNLOADER_PREF, 0);
        SharedPreferences donationPrefs = context.getSharedPreferences(Constants.DONATION_PREF, Context.MODE_PRIVATE);
        int updateFrequency = prefs.getInt(Constants.UPDATE_INTERVAL_PREF, Constants.UPDATE_FREQ_DAILY);

        // reset for no license user
        if (!Utils.checkMinLicensed(context)) {
            updateFrequency = Constants.UPDATE_FREQ_DAILY;
            prefs.edit().putInt(Constants.UPDATE_INTERVAL_PREF, Constants.UPDATE_FREQ_DAILY).apply();
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // We just booted. Store the boot check state
            prefs.edit().putBoolean(Constants.BOOT_CHECK_COMPLETED, false).apply();
            if (Utils.getPaidTotal(context) > 0 || !donationPrefs.getBoolean(KEY_DONATION_CHECK_COMPLETED, false)) {
                RequestUtils.fetchDonationRanking(context);
            } else {
                // Reset donation info
                donationPrefs.edit().putInt(KEY_DONATION_PERCENT, 0)
                        .putInt(KEY_DONATION_RANK, 0)
                        .putFloat(KEY_DONATION_AMOUNT, 0).apply();
            }
        }

        // Check if we are set to manual updates and don't do anything
        if (updateFrequency == Constants.UPDATE_FREQ_NONE) {
            return;
        }

        // Not set to manual updates, parse the received action
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            // Connectivity has changed
            boolean hasConnection = !intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Log.i(TAG, "Got connectivity change, has connection: " + hasConnection);
            if (!hasConnection) {
                return;
            }
        }

        // Handle the actual update check based on the defined frequency
        if (updateFrequency == Constants.UPDATE_FREQ_AT_BOOT) {
            boolean bootCheckCompleted = prefs.getBoolean(Constants.BOOT_CHECK_COMPLETED, false);
            if (!bootCheckCompleted) {
                Log.i(TAG, "Start an on-boot check");
                Intent i = new Intent(context, UpdateCheckService.class);
                i.setAction(UpdateCheckService.ACTION_CHECK);
                context.startService(i);
            } else {
                // Nothing to do
                Log.i(TAG, "On-boot update check was already completed.");
            }
        } else if (updateFrequency > 0) {
            Log.i(TAG, "Scheduling future, repeating update checks.");
            Utils.scheduleUpdateService(context, updateFrequency * 1000);
        } else if (ACTION_UPDATE_CHECK.equals(action)) {
            Intent i = new Intent(context, UpdateCheckService.class);
            i.setAction(UpdateCheckService.ACTION_CHECK);
            context.startService(i);
        }
    }

}
