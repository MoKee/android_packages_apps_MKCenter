/*
 * Copyright (C) 2014-2016 The MoKee Open Source Project
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
import android.util.AttributeSet;
import android.webkit.WebView;

public class NotifyingWebView extends WebView {
    public interface OnInitialContentReadyListener {
        void onInitialContentReady(WebView view);
    }

    private OnInitialContentReadyListener mListener;
    private boolean mContentReady = false;

    public NotifyingWebView(Context context) {
        super(context);
    }

    public NotifyingWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyingWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnInitialContentReadyListener(OnInitialContentReadyListener listener) {
        mListener = listener;
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (getContentHeight() > 0 && !mContentReady) {
            if (mListener != null) {
                mListener.onInitialContentReady(this);
            }
            mContentReady = true;
        }
    }
}
