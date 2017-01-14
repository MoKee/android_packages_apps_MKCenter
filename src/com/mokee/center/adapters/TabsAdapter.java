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

package com.mokee.center.adapters;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import com.mokee.center.R;
import com.mokee.center.fragments.MoKeeSupportFragment;
import com.mokee.center.fragments.MoKeeUpdaterFragment;

public class TabsAdapter extends android.support.v4.app.FragmentPagerAdapter {

    private final Context mContext;

    public TabsAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.mContext = context;
    }

    @Override
    public android.support.v4.app.Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new MoKeeUpdaterFragment();
            case 1:
                return new MoKeeSupportFragment();
            default:
                return null;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getString(R.string.mokee_updater_title);
            case 1:
                return mContext.getString(R.string.mokee_support_title);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

}
