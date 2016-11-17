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

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.mokee.center.R;

public class AdmobPreference extends Preference {

    private static AdView adView;
    private static View admobCustomView;

    public AdmobPreference(Context context) {
        super(context);
    }

    public AdmobPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdmobPreference(Context context, AttributeSet ui, int style) {
        super(context, ui, style);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        if (admobCustomView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            admobCustomView = inflater.inflate(R.layout.preference_admob, null);
            adView = (AdView) admobCustomView.findViewById(R.id.adView);
            adView.loadAd(new AdRequest.Builder().build());
        }
        return admobCustomView;
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

    public void onAdDestory() {
        if (adView != null) {
            adView.destroy();
        }
    }
}
