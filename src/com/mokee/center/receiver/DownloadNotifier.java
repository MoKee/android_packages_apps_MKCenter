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

package com.mokee.center.receiver;

import java.io.File;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.mokee.center.R;
import com.mokee.center.utils.Utils;

public class DownloadNotifier {

    private DownloadNotifier() {
        // Don't instantiate me bro
    }

    public static void notifyDownloadComplete(Context context,
            Intent updateIntent, File updateFile) {
        String updateUiName = updateFile.getName();

        // Set Notification Info
        int mContentTitleID, mTickerID, mActionTitleID, mTextID;
        mContentTitleID = R.string.not_download_success;
        mTickerID = R.string.not_download_success;
        mTextID = updateUiName.startsWith("OTA") ? R.string.not_download_install_ota_notice : R.string.not_download_install_notice;

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
                .setBigContentTitle(context.getString(mContentTitleID))
                .bigText(context.getString(mTextID, updateUiName));

        NotificationCompat.Builder builder = createBaseContentBuilder(context, updateIntent)
                .setColor(context.getResources().getColor(com.android.internal.R.color.system_notification_accent_color))
                .setSmallIcon(R.drawable.ic_mokee_updater)
                .setContentTitle(context.getString(mContentTitleID))
                .setContentText(updateUiName)
                .setTicker(context.getString(mTickerID))
                .setStyle(style);
//        if (Utils.checkLicensed(context)) {
//            builder.addAction(R.drawable.ic_tab_install,
//                    context.getString(mActionTitleID),
//                    createInstallPendingIntent(context, updateFile));
//        }

        // Wearable install action
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();
        NotificationCompat.Action wearInstallAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_mokee_updater,
                context.getString(R.string.not_action_install_update),
                createInstallPendingIntent(context, updateFile))
                .build();
        extender.addAction(wearInstallAction);
        builder.extend(extender);

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(mContentTitleID, builder.build());
    }

    private static NotificationCompat.Builder createBaseContentBuilder(Context context,
            Intent updateIntent) {
        PendingIntent contentIntent = PendingIntent.getBroadcast(context, 1,
                updateIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
    }

    public static void notifyDownloadError(Context context,
            Intent updateIntent, int failureMessageResId) {
        NotificationCompat.Builder builder = createBaseContentBuilder(context, updateIntent)
                .setColor(context.getResources().getColor(com.android.internal.R.color.system_notification_accent_color))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(context.getString(R.string.not_download_failure))
                .setContentText(context.getString(failureMessageResId))
                .setTicker(context.getString(R.string.not_download_failure));

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(R.string.not_download_success, builder.build());
    }

    private static PendingIntent createInstallPendingIntent(Context context, File updateFile) {
        Intent installIntent = new Intent(context, DownloadReceiver.class);
        installIntent.setAction(DownloadReceiver.ACTION_INSTALL_UPDATE);
        installIntent.putExtra(DownloadReceiver.EXTRA_FILENAME, updateFile.getName());

        return PendingIntent.getBroadcast(context, 0,
                installIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
