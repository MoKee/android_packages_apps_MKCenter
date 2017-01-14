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

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.mokee.center.R;

import cn.waps.AppConnect;

public class AppofferPreference extends Preference {

    private Activity mContext;
    private LinearLayout adView;

    private boolean hasInited = false;

    @SuppressWarnings("unused")
    public AppofferPreference(Context context) {
        this(context, null);
    }

    @SuppressWarnings("WeakerAccess")
    public AppofferPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                android.support.v7.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    @SuppressWarnings("WeakerAccess")
    public AppofferPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("WeakerAccess")
    public AppofferPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_appoffer);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        adView = (LinearLayout) holder.findViewById(R.id.adView);
        if (!hasInited && mContext != null) {
            hasInited = true;
            AppConnect.getInstance(mContext).showBannerAd(mContext, adView);
        }
    }

    public void onAdCreate(Activity context) {
        this.mContext = context;
    }

    public void onAdResume(Activity mContext) {
        if (adView != null) {
            if (adView.getChildCount() != 0) {
                adView.removeAllViews();
            }
            AppConnect.getInstance(mContext).showBannerAd(mContext, adView);
        }
    }

}
