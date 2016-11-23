/*
 * Copyright (C) 2015-2016 The MoKee Open Source Project
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
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.mokee.center.R;

import cn.waps.AppConnect;

public class AppofferPreference extends Preference {

    private static View appofferCustomView;

    private static LinearLayout adView;

    public AppofferPreference(Context context) {
        super(context);
    }

    public AppofferPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppofferPreference(Context context, AttributeSet ui, int style) {
        super(context, ui, style);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        if (appofferCustomView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            appofferCustomView = inflater.inflate(R.layout.preference_appoffer, null);
            adView = (LinearLayout) appofferCustomView.findViewById(R.id.AdView);
            AppConnect.getInstance(parent.getContext()).showBannerAd(parent.getContext(), adView);
        }
        return appofferCustomView;
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
