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

package com.mokee.center.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.mokee.utils.MoKeeUtils;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import cn.waps.AppConnect;

import mokee.support.widget.snackbar.Snackbar;
import mokee.support.widget.snackbar.SnackbarManager;
import mokee.support.widget.snackbar.listeners.ActionClickListener;

import com.mokee.center.R;
import com.mokee.center.adapters.TabsAdapter;
import com.mokee.center.fragments.MoKeeSupportFragment;
import com.mokee.center.fragments.MoKeeUpdaterFragment;
import com.mokee.center.misc.Constants;
import com.mokee.center.service.UpdateCheckService;
import com.mokee.center.utils.RequestUtils;
import com.mokee.center.utils.Utils;

public class MoKeeCenter extends FragmentActivity {

    public static final String BR_ONNewIntent = "com.mokee.center.action.ON_NEW_INTENT";

    private ActionBar bar;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
        bar.setTitle(R.string.mokee_center_title);

        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.mokee_updater_title), MoKeeUpdaterFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.mokee_support_title), MoKeeSupportFragment.class, null);

        AppConnect.getInstance("179a03b58d0dc099e7770f1f5e1f8887", "default", this);
        if (!Utils.checkLicensed(this) && MoKeeUtils.isSupportLanguage(false)) {
            AppConnect.getInstance(this).initPopAd(this);
        }

        // Turn on the Options Menu
        invalidateOptionsMenu();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Intent send = new Intent(BR_ONNewIntent);
        send.putExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_ID, intent.getLongExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_ID, -1));
        send.putExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_PATH, intent.getStringExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_PATH));
        sendBroadcastAsUser(send, UserHandle.CURRENT);
    }

    public static void donateOrRemoveAdsDialog(Activity mContext, final boolean isDonate) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout donateView = (LinearLayout)inflater.inflate(R.layout.donate, null);
        final TextView mRequest = (TextView) donateView.findViewById(R.id.request);
        final SeekBar mSeekBar = (SeekBar) donateView.findViewById(R.id.price);
        mSeekBar.setMax(Constants.DONATION_MAX - Constants.DONATION_REQUEST_MIN);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBar.setProgress(progress / 10 * 10);
                mRequest.setText(String.format(mContext.getString(R.string.donate_money_currency), progress / 10 * 10 + Constants.DONATION_REQUEST_MIN));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }});
        ProgressBar mProgressBar = (ProgressBar) donateView.findViewById(R.id.progress);
        Float paid = Utils.getPaidTotal(mContext);
        Float unPaid = Constants.DONATION_TOTAL - paid;
        if (isDonate) {
            mSeekBar.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mRequest.setText(String.format(mContext.getString(R.string.donate_money_currency), Constants.DONATION_REQUEST_MIN));
        } else {
            mSeekBar.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setMax(Constants.DONATION_TOTAL);
            mProgressBar.setProgress(paid.intValue());
            mRequest.setText(String.format(mContext.getString(R.string.remove_ads_request_price), paid.intValue(), unPaid.intValue()));
        }

        DialogInterface.OnClickListener mDialogButton = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String price = isDonate ? String.valueOf(which == DialogInterface.BUTTON_POSITIVE ? Float.valueOf(mSeekBar.getProgress() + Constants.DONATION_REQUEST_MIN) / 6 : String.valueOf(mSeekBar.getProgress() + Constants.DONATION_REQUEST_MIN)) : String.valueOf(which == DialogInterface.BUTTON_POSITIVE ? unPaid / 6 : unPaid);
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Utils.sendPaymentRequest(mContext, "paypal", mContext.getString(isDonate ? R.string.donate_money_title : R.string.remove_ads_title), mContext.getString(isDonate ? R.string.donate_money_title : R.string.remove_ads_title), price, Constants.PAYMENT_TYPE_DONATION);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        Utils.sendPaymentRequest(mContext, "alipay", mContext.getString(isDonate ? R.string.donate_money_title : R.string.remove_ads_title), mContext.getString(isDonate ? R.string.donate_money_title : R.string.remove_ads_title), price, Constants.PAYMENT_TYPE_DONATION);
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        if (isDonate && MoKeeUtils.isSupportLanguage(false)) {
                            Utils.pointPaymentRequest(mContext);
                        } else {
                            Utils.restorePaymentRequest(mContext);
                        }
                        break;
                }
            }
        };

        Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(isDonate ? R.string.donate_dialog_title : R.string.remove_ads_title)
                .setMessage(R.string.donate_dialog_message)
                .setView(donateView)
                .setPositiveButton(R.string.donate_dialog_via_paypal, mDialogButton)
                .setNegativeButton(R.string.donate_dialog_via_alipay, mDialogButton);
        if (isDonate && MoKeeUtils.isSupportLanguage(false)) {
            builder.setNeutralButton(R.string.donate_dialog_via_point, mDialogButton);
        } else {
            if (Utils.getPaidTotal(mContext) == 0f) {
                builder.setNeutralButton(R.string.donate_dialog_via_restore, mDialogButton);
            }
        }
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(resultCode) {
            case Activity.RESULT_OK:
                SnackbarManager.show(Snackbar.with(this).text(R.string.donate_money_toast_success)
                        .duration(5000L).actionListener(new ActionClickListener(){
                            @Override
                            public void onActionClicked(Snackbar snackbar) {
                                donateOrRemoveAdsDialog(MoKeeCenter.this, true);
                            }
                        }).actionLabel(R.string.donate_money_again).colorResource(R.color.snackbar_background));
                RequestUtils.getRanking(getApplicationContext());
                getSharedPreferences(Constants.DOWNLOADER_PREF, 0).edit()
                        .putLong(Constants.KEY_DISCOUNT_TIME, System.currentTimeMillis())
                        .putLong(Constants.KEY_LEFT_TIME, 0).apply();
                break;
            case 200:
                SnackbarManager.show(Snackbar.with(this).text(R.string.donate_money_restored_success)
                        .duration(5000L).colorResource(R.color.snackbar_background));
                RequestUtils.getRanking(getApplicationContext());
                break;
            case 500:
                SnackbarManager.show(Snackbar.with(this).text(R.string.donate_money_restored_failed)
                        .duration(5000L).colorResource(R.color.snackbar_background));
                break;
            case 408:
                SnackbarManager.show(Snackbar.with(this).text(R.string.donate_money_restored_timeout)
                        .duration(5000L).colorResource(R.color.snackbar_background));
                break;
        }
        MoKeeUpdaterFragment.refreshOption();
    }

}
