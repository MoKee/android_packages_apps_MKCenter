/*
 * Copyright (C) 2017 The MoKee Open Source Project
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

package com.mokee.center.service;

import android.app.IntentService;
import android.content.Intent;
import android.provider.Settings;

import com.mokee.os.Build;
import com.mokee.utils.CommonUtils;

public class DeviceRegistrationService extends IntentService {

    public DeviceRegistrationService() {
        super(DeviceRegistrationService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!CommonUtils.hasTelephony(getApplicationContext())) {
            Settings.Secure.putString(getContentResolver(), Settings.Secure.UNIQUE_REGISTRATION_IDS, Build.getUniqueID(getApplicationContext()));
        } else {
            Settings.Secure.putString(getContentResolver(), Settings.Secure.UNIQUE_REGISTRATION_IDS, Build.getUniqueIDS(getApplicationContext()));
        }
        stopSelf();
    }
}
