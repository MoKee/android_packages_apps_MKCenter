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

package com.mokee.center.misc;

import android.os.Environment;

import com.mokee.center.MKCenterApplication;
import com.mokee.center.utils.Utils;

public class Constants {

    // Actions
    public static final String ACTION_PAYMENT_REQUEST = "com.mokee.pay.action.PAYMENT_REQUEST";
    public static final String ACTION_RESTORE_REQUEST = "com.mokee.pay.action.RESTORE_REQUEST";
    public static final String ACTION_POINT_REQUEST = "com.mokee.pay.action.POINT_REQUEST";
    public static final String ACTION_VERIFY_REQUEST = "com.mokee.center.action.VERIFY_REQUEST";

    public static final String CHECK_LOG_FILE = "/cache/recovery/check_log";

    // Download related
    public static final String UPDATES_FOLDER = "mkupdater";

    // Preferences
    public static final String DOWNLOADER_PREF = "downloader";
    public static final String ROOT_PREF = "pref_root";
    public static final String ADMOB_PREF = "pref_admob";
    public static final String UPDATE_INTERVAL_PREF = "pref_update_interval";
    public static final String UPDATE_TYPE_PREF = "pref_update_types";
    public static final String OTA_CHECK_PREF = "pref_ota_check";
    public static final String VERIFY_ROM_PREF = "pref_verify_rom";
    public static final String LAST_UPDATE_CHECK_PREF = "pref_last_update_check";

    // Update Check items
    public static final String BOOT_CHECK_COMPLETED = "boot_check_completed";
    public static final int UPDATE_FREQ_AT_BOOT = -1;
    public static final int UPDATE_FREQ_NONE = -2;
    public static final int UPDATE_FREQ_TWICE_DAILY = 43200;
    public static final int UPDATE_FREQ_DAILY = 86400;
    public static final int UPDATE_FREQ_TWICE_WEEKLY = 302400;

    // Update types
    public static final int UPDATE_TYPE_RELEASE = 0;
    public static final int UPDATE_TYPE_NIGHTLY = 1;
    public static final int UPDATE_TYPE_EXPERIMENTAL = 2;
    public static final int UPDATE_TYPE_UNOFFICIAL = 3;
    public static final int UPDATE_TYPE_ALL = 4;

    public static final String OTA_CHECK_MANUAL_PREF = "pref_ota_check_manual";

    // Intent Flag
    public static final int INTENT_FLAG_GET_UPDATE = 1024;

    // About License
    public static final String LICENSE_FILE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mokee.license";

    // Donation amount
    public static final String DONATION_FIRST_CHECK = "donation_first_check";
    public static final int DONATION_TOTAL = 68;
    public static final int DONATION_REQUEST = 30;
    public static final int DONATION_REQUEST_MIN = Utils.getPaidTotal(MKCenterApplication.getContext()) > 0 ? 10 : DONATION_REQUEST;
    public static final int DONATION_MAX = 1000;
    public static final String PAYMENT_TYPE_DONATION = "donation";

    public static final String KEY_FLASH_TIME = "flash_time";
    public static final String KEY_DONATE_PERCENT = "donate_percent";
    public static final String KEY_DONATE_RANK = "donate_rank";
    public static final String KEY_DONATE_AMOUNT = "donate_amount";

    public static final String ROOT_ACCESS_PROPERTY = "persist.sys.root_access";

    // Public key
    public static final String PUB_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCwN8FdvNOu5A8yP2Bfb7rk1o6N" +
                    "dXik/DO+Kw6+q7nIZjTh4qpPL3Gyoa7A3MI01gTRKaM+MU2+zkiZND8qoB8EGlF6" +
                    "BfDfi9BLyFyx+nOTgz3KDEYutLJhopS18DfrdZTohNXsM7+MEsk5y+GHFjYHePXN" +
                    "oE4fjtfCg3xbtwU29wIDAQAB";
}
