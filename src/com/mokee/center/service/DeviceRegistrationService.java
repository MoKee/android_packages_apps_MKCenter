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
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.mokee.utils.CommonUtils;

import mokee.providers.MKSettings;

public class DeviceRegistrationService extends IntentService {

    public DeviceRegistrationService() {
        super(DeviceRegistrationService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final TelephonyManager telephonyManager =
                (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        StringBuffer stringBuffer = new StringBuffer();
        for (int slotId = 0; slotId < telephonyManager.getSimCount(); slotId ++) {
            final Phone phone = PhoneFactory.getPhone(slotId);
            if (phone != null) {
                if (phone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
                    if (CommonUtils.isValid(phone.getMeid())) {
                        stringBuffer.append(CommonUtils.digest(phone.getMeid() + android.os.Build.SERIAL)).append(",");
                    }
                    if (phone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                        if (CommonUtils.isValid(phone.getImei())) {
                            stringBuffer.append(CommonUtils.digest(phone.getImei() + android.os.Build.SERIAL)).append(",");
                        }
                    }
                } else {
                    if (CommonUtils.isValid(phone.getImei())) {
                        stringBuffer.append(CommonUtils.digest(phone.getImei() + android.os.Build.SERIAL)).append(",");
                    }
                }
            }
        }
        MKSettings.Secure.putString(getContentResolver(), MKSettings.Secure.UNIQUE_REGISTRATION_IDS, stringBuffer.toString());
        stopSelf();
    }
}
