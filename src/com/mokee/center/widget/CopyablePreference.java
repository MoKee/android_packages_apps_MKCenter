/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2017-2018 The MoKee Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mokee.center.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.Toast;

@SuppressWarnings({"WeakerAccess", "unused"})
public class CopyablePreference extends Preference {

    private final ClipboardManager cm;

    public CopyablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public CopyablePreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setLongClickable(true);
        holder.itemView.setOnLongClickListener(v -> {
            cm.setPrimaryClip(ClipData.newPlainText(null, getSummary()));
            Toast.makeText(getContext(), com.android.internal.R.string.text_copied, Toast.LENGTH_SHORT).show();
            return true;
        });
    }

}
