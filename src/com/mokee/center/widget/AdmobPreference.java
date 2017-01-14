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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.mokee.center.R;

public class AdmobPreference extends Preference {

    private static AdView adView;

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
        adView = (AdView) holder.itemView;
        if (adView.getChildCount() == 0) {
            adView.loadAd(new AdRequest.Builder().build());
        }
    }

    public void onAdCreate() {
        if (adView != null && !adView.isLoading()) {
            adView.loadAd(new AdRequest.Builder().build());
        }
    }

    public void onAdPause() {
        if (adView != null) {
            adView.pause();
        }
    }

    public void onAdResume() {
        if (adView != null) {
            adView.resume();
        }
    }

    public void onAdDestroy() {
        if (adView != null) {
            adView.destroy();
        }
    }

}
