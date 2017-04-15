/*
 * Copyright (C) 2015-2017 The MoKee Open Source Project
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

package com.mokee.center.widget;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.mokee.center.R;

public class AdmobPreference extends Preference {

    private AdView mAdView;
    private AdRequest mAdRequest;

    @SuppressWarnings("unused")
    public AdmobPreference(Context context) {
        this(context, null);
    }

    @SuppressWarnings("WeakerAccess")
    public AdmobPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                android.support.v7.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    @SuppressWarnings("WeakerAccess")
    public AdmobPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("WeakerAccess")
    public AdmobPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_admob);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        if (mAdView == null) {
            mAdView = (AdView) holder.itemView;
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int errorCode) {
                    super.onAdFailedToLoad(errorCode);
                    if (errorCode == AdRequest.ERROR_CODE_NO_FILL) {
                        mAdView.loadAd(mAdRequest);
                    }
                }
            });
            mAdView.loadAd(mAdRequest);
        }
    }

    public void setAdRequest(AdRequest adRequest) {
        mAdRequest = adRequest;
    }

    public void onAdPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
    }

    public void onAdResume() {
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    public void onAdDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

}
