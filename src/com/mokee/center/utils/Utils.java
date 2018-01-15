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

package com.mokee.center.utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.RecoverySystem;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.mokee.center.R;
import com.mokee.center.db.DownLoadDao;
import com.mokee.center.db.ThreadDownLoadDao;
import com.mokee.center.misc.Constants;
import com.mokee.center.misc.DownLoadInfo;
import com.mokee.center.service.UpdateCheckService;
import com.mokee.os.Build;
import com.mokee.security.License;
import com.mokee.security.LicenseInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import mokee.providers.MKSettings;

public class Utils {

    public static final String MOKEE_UPDATE_EVENTS_NOTIFICATION_CHANNEL =
            "mokee_update_events_notification_channel";

    public static final String MOKEE_UPDATE_PROGRESS_NOTIFICATION_CHANNEL =
            "mokee_update_progress_notification_channel";

    public static File makeUpdateFolder() {
        return new File(Environment.getExternalStorageDirectory(),
                Constants.UPDATES_FOLDER);
    }

    /**
     * 检测rom是否已下载
     */
    public static boolean isLocaUpdateFile(String fileName) {
        File file = new File(Environment.getExternalStorageDirectory() + "/"
                + Constants.UPDATES_FOLDER, fileName);
        return file.exists();
    }

    public static void cancelNotification(Context context) {
        final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.string.not_new_updates_found_title);
        nm.cancel(R.string.not_download_success);
    }

    public static void createEventsNotificationChannel(Context context) {
        final NotificationChannel channel = new NotificationChannel(
                MOKEE_UPDATE_EVENTS_NOTIFICATION_CHANNEL,
                context.getString(R.string.mokee_center_title),
                NotificationManager.IMPORTANCE_HIGH);

        final NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
    }

    public static void createProgressNotificationChannel(Context context) {
        final NotificationChannel channel = new NotificationChannel(
                MOKEE_UPDATE_PROGRESS_NOTIFICATION_CHANNEL,
                context.getString(R.string.mokee_center_title),
                NotificationManager.IMPORTANCE_LOW);

        final NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
    }

    public static String getReleaseVersionTypeString(Context mContext, String MoKeeVersionType) {
        switch (MoKeeVersionType) {
            case "release":
                return mContext.getString(R.string.mokee_version_type_release);
            case "experimental":
                return mContext.getString(R.string.mokee_version_type_experimental);
            case "history":
                return mContext.getString(R.string.mokee_version_type_history);
            case "nightly":
                return mContext.getString(R.string.mokee_version_type_nightly);
            case "unofficial":
                return mContext.getString(R.string.mokee_version_type_unofficial);
            default:
                return mContext.getString(R.string.mokee_info_default);
        }
    }

    public static String getReleaseVersionType() {
        return Build.RELEASE_TYPE.toLowerCase(Locale.ENGLISH);
    }

    public static int getUpdateType(String MoKeeVersionType) {
        switch (MoKeeVersionType) {
            case "nightly":
                return 1;
            case "experimental":
                return 2;
            case "unofficial":
                return 3;
            default:
                return 0;
        }
    }

    public static long getVersionLifeTime(String versionType) {
        if (versionType.equals("release")) {
            return DateUtils.DAY_IN_MILLIS * 30;
        } else {
            return DateUtils.DAY_IN_MILLIS * 7;
        }
    }

    public static void scheduleUpdateService(Context context, int updateFrequency) {
        // Load the required settings from preferences
        SharedPreferences prefs = context.getSharedPreferences(Constants.DOWNLOADER_PREF, 0);
        long lastCheck = prefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0);

        // Get the intent ready
        Intent i = new Intent(context, UpdateCheckService.class);
        i.setAction(UpdateCheckService.ACTION_CHECK);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        // Clear any old alarms and schedule the new alarm
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);

        if (updateFrequency != Constants.UPDATE_FREQ_NONE) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, lastCheck + updateFrequency, updateFrequency, pi);
        }
    }

    public static HashMap<String, String> buildSystemCompatibleMessage(Context context) {
        HashMap<String, String> hashMap = new HashMap<>();
        File logFile = new File(Constants.CHECK_LOG_FILE);
        try {
            InputStream inputStream = new FileInputStream(logFile);
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean wrotePatchBlock = false;
            if (bufferedReader != null) {
                while ((line = bufferedReader.readLine()) != null) {
                    String msg = line.trim().split("=")[1];
                    if (line.startsWith("verify_trustzone")) {
                        stringBuilder.append(String.format(context.getString(R.string.verify_system_compatible_img),
                                "trustzone", msg) + "\n");
                    } else if (line.startsWith("verify_bootloader")) {
                        stringBuilder.append(String.format(context.getString(R.string.verify_system_compatible_img),
                                "bootloader", msg) + "\n");
                    } else if (line.startsWith("verify_baseband")) {
                        stringBuilder.append(String.format(context.getString(R.string.verify_system_compatible_img),
                                "baseband", msg) + "\n");
                    } else if (line.startsWith("verify_modem")) {
                        stringBuilder.append(String.format(context.getString(R.string.verify_system_compatible_img),
                                "modem", msg) + "\n");
                    } else if (line.startsWith("exit_status")) {
                        hashMap.put("status", msg);
                    } else if (line.startsWith("apply_patch")) {
                        if (!wrotePatchBlock) {
                            wrotePatchBlock = true;
                            stringBuilder.append(context.getString(R.string.verify_system_compatible_patch) + "\n");
                        }
                        stringBuilder.append(msg).append("\n");
                    }
                }
                hashMap.put("result", stringBuilder.toString());
            }
            inputStream.close();
            if (logFile.exists()) logFile.delete();
            return hashMap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hashMap;
    }

    public static void verifySystemCompatible(Activity context, String updateFileName) throws IOException {
        // Add the update folder/file name
        String primaryStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        // Create the path for the update package
        String updatePackagePath = primaryStorage + "/" + Constants.UPDATES_FOLDER + "/" + updateFileName;

        File checker = new File("/system/bin/mkchecker");
        if (updateFileName.toUpperCase().startsWith("MK") || !checker.exists()) {
            // Reboot into recovery and trigger the update
            RecoverySystem.installPackageLegacy(context, new File(updatePackagePath), false);
        } else {
            Intent intent = new Intent(Constants.ACTION_VERIFY_REQUEST);
            intent.putExtra("update_package_path", updatePackagePath);
            context.startActivityForResult(intent, 0);
        }
    }

    public static void triggerUpdate(Context context, String updateFileName)
            throws IOException {
        // Add the update folder/file name
        String primaryStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        // Create the path for the update package
        String updatePackagePath = primaryStorage + "/" + Constants.UPDATES_FOLDER + "/" + updateFileName;
        // Reboot into recovery and trigger the update
        RecoverySystem.installPackageLegacy(context, new File(updatePackagePath), false);
    }

    public static void triggerUpdateByPath(Context context, String updatePackagePath)
            throws IOException {
        // Reboot into recovery and trigger the update
        try {
            RecoverySystem.installPackageLegacy(context, new File(updatePackagePath), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUserAgentString(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.packageName + "/" + pi.versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            return null;
        }
    }

    /**
     * 截取日期
     */
    public static String subBuildDate(String name, boolean sameVersion) {
        String[] strs = name.split("-");
        String date;
        if (name.toLowerCase(Locale.ENGLISH).startsWith("ota")) {
            date = strs[4];
        } else {
            date = strs[2];
        }
        if (!isNum(date)) return "0";
        if (date.startsWith("20")) {
            date = date.substring(2, date.length());
        }
        if (!sameVersion) {
            if (date.length() > 6) {
                date = date.substring(0, 6);
            }
        }
        return date;
    }

    public static boolean isNum(String str) {
        return str.matches("^[-+]?(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)$");
    }

    /**
     * 截取日期长度
     */
    public static int getBuildDateLength(String name) {
        String[] strs = name.split("-");
        String date = strs[2];
        if (date.startsWith("20")) {
            date = date.substring(2, date.length());
        }
        return date.length();
    }

    /**
     * 截取版本
     */
    public static String subMoKeeVersion(String name) {
        String[] strs = name.split("-");
        String version = strs[0];
        if (name.toLowerCase(Locale.ENGLISH).startsWith("ota")) {
            version = strs[1];
        }
        version = version.substring(2, version.length());
        return version;
    }

    /**
     * 判断版本新旧
     */
    public static boolean isNewVersion(String itemName) {
        int nowDateLength = getBuildDateLength(Build.VERSION);
        int itemDateLength = getBuildDateLength(itemName);
        boolean sameVersion = (nowDateLength == itemDateLength);
        int nowDate = Integer.valueOf(subBuildDate(Build.VERSION, sameVersion));
        int itemDate = Integer.valueOf(subBuildDate(itemName, sameVersion));
        float nowVersion = Float.valueOf(subMoKeeVersion(Build.VERSION));
        float itemVersion = Float.valueOf(subMoKeeVersion(itemName));
        return (itemDate > nowDate && itemVersion >= nowVersion);
    }

    public static void setSummaryFromString(PreferenceFragmentCompat prefFragment, String preference,
                                            String value) {
        if (prefFragment == null) {
            return;
        }
        try {
            prefFragment.findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            prefFragment.findPreference(preference).setSummary(
                    prefFragment.getActivity().getString(R.string.mokee_info_default));
        }
    }

    /**
     * 文件目录清理
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        } else {
            DownLoadInfo dli = null;
            if (dir.getName().endsWith(".partial")) {
                dli = DownLoadDao.getInstance().getDownLoadInfoByName(dir.getName());
            } else {
                dli = DownLoadDao.getInstance().getDownLoadInfoByName(dir.getName() + ".partial");
            }
            if (dli != null) {
                ThreadDownLoadDao.getInstance().delete(dli.getUrl());
                DownLoadDao.getInstance().delete(dli.getUrl());
            }
        }
        // The directory is now empty so delete it
        File newFile = new File(dir.getAbsolutePath() + System.currentTimeMillis());
        dir.renameTo(newFile);
        return newFile.delete();
    }

    public static Float getPaidTotal(Context mContext) {
        if (new File(Constants.LICENSE_FILE).exists()) {
            try {
                LicenseInfo licenseInfo = License.readLicense(Constants.LICENSE_FILE, Constants.PUB_KEY);
                String unique_ids = MKSettings.Secure.getString(mContext.getContentResolver(), MKSettings.Secure.UNIQUE_REGISTRATION_IDS);
                if (Arrays.asList(unique_ids.split(",")).contains(licenseInfo.getUniqueID())
                        && licenseInfo.getPackageName().equals(mContext.getPackageName())) {
                    return licenseInfo.getPrice();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return 0f;
        }
        return 0f;
    }

    public static void restorePaymentRequest(Activity mContext) {
        Intent intent = new Intent(Constants.ACTION_RESTORE_REQUEST);
        mContext.startActivityForResult(intent, 0);
    }

    public static void pointPaymentRequest(Activity mContext) {
        Intent intent = new Intent(Constants.ACTION_POINT_REQUEST);
        mContext.startActivityForResult(intent, 0);
    }

    public static void sendPaymentRequest(Activity mContext, String channel, String name, String description, String price, String type) {
        try {
            Intent intent = new Intent(Constants.ACTION_PAYMENT_REQUEST);
            intent.putExtra("packagename", mContext.getPackageName());
            intent.putExtra("channel", channel);
            intent.putExtra("type", type);
            intent.putExtra("name", name);
            intent.putExtra("description", description);
            intent.putExtra("price", price);
            mContext.startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean checkLicensed(Context mContext) {
        return getPaidTotal(mContext) >= Constants.DONATION_TOTAL;
    }

    public static boolean checkMinLicensed(Context mContext) {
        return getPaidTotal(mContext) >= Constants.DONATION_REQUEST;
    }

    private static int getRandomDays() {
        int max = 90;
        int min = 60;
        Random random = new Random();
        return random.nextInt(max) % (max - min + 1) + min;
    }

    public static boolean Discounting(SharedPreferences mPrefs) {
        long flashTime = mPrefs.getLong(Constants.KEY_FLASH_TIME, 0);
        Float amount = mPrefs.getFloat(Constants.KEY_DONATE_AMOUNT, 0);
        if (flashTime != 0 && amount < Constants.DONATION_REQUEST) {
            if (flashTime * 1000 + DateUtils.DAY_IN_MILLIS * getRandomDays() < System.currentTimeMillis()) {
                long discountTime = mPrefs.getLong(Constants.KEY_DISCOUNT_TIME, 0);
                if (discountTime == 0 || discountTime + DateUtils.DAY_IN_MILLIS * 30 < System.currentTimeMillis()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

}
