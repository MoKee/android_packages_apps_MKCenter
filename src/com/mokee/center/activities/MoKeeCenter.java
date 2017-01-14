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

package com.mokee.center.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.mokee.utils.MoKeeUtils;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mokee.center.R;
import com.mokee.center.adapters.TabsAdapter;
import com.mokee.center.misc.Constants;
import com.mokee.center.service.UpdateCheckService;
import com.mokee.center.utils.RequestUtils;
import com.mokee.center.utils.Utils;
import com.mokee.center.widget.SimpleOnSeekBarChangeListener;

import cn.waps.AppConnect;

public class MoKeeCenter extends AppCompatActivity {

    public static final String BR_ONNewIntent = "com.mokee.center.action.ON_NEW_INTENT";

    private CoordinatorLayout mRoot;

    public void donateOrRemoveAdsDialog(final boolean isDonate) {
        final LayoutInflater inflater = LayoutInflater.from(this);

        @SuppressLint("InflateParams")
        final ViewGroup donateView = (ViewGroup) inflater.inflate(R.layout.donate, null);

        final TextView mRequest = (TextView) donateView.findViewById(R.id.request);
        final SeekBar mSeekBar = (SeekBar) donateView.findViewById(R.id.price);

        mSeekBar.setMax(Constants.DONATION_MAX - Constants.DONATION_REQUEST_MIN);
        mSeekBar.setOnSeekBarChangeListener(new SimpleOnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBar.setProgress(progress / 10 * 10);
                mRequest.setText(getString(R.string.donate_money_currency,
                        progress / 10 * 10 + Constants.DONATION_REQUEST_MIN));
            }
        });

        final ProgressBar mProgressBar = (ProgressBar) donateView.findViewById(R.id.progress);
        final Float paid = Utils.getPaidTotal(this);
        final Float unPaid = Constants.DONATION_TOTAL - paid;

        if (isDonate) {
            mSeekBar.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mRequest.setText(getString(R.string.donate_money_currency, Constants.DONATION_REQUEST_MIN));
        } else {
            mSeekBar.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setMax(Constants.DONATION_TOTAL);
            mProgressBar.setProgress(paid.intValue());
            mRequest.setText(getString(R.string.remove_ads_request_price, paid.intValue(), unPaid.intValue()));
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(isDonate ? R.string.donate_dialog_title : R.string.remove_ads_title)
                .setMessage(R.string.donate_dialog_message)
                .setView(donateView);

        final String title = isDonate
                ? getString(R.string.donate_money_title)
                : getString(R.string.remove_ads_title);

        builder.setPositiveButton(R.string.donate_dialog_via_paypal, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                float price = isDonate
                        ? (float) (mSeekBar.getProgress() + Constants.DONATION_REQUEST_MIN)
                        : unPaid;
                requestForPayment("paypal", price, title);
            }
        });

        builder.setNegativeButton(R.string.donate_dialog_via_alipay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                float price = isDonate
                        ? (float) (mSeekBar.getProgress() + Constants.DONATION_REQUEST_MIN)
                        : unPaid;
                requestForPayment("alipay", price, title);
            }
        });

        if (isDonate && MoKeeUtils.isSupportLanguage(false)) {
            builder.setNeutralButton(R.string.donate_dialog_via_point, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Utils.pointPaymentRequest(MoKeeCenter.this);
                }
            });
        } else if (Utils.getPaidTotal(MoKeeCenter.this) == 0f) {
            builder.setNeutralButton(R.string.donate_dialog_via_restore, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Utils.restorePaymentRequest(MoKeeCenter.this);
                }
            });
        }

        builder.show();
    }

    private void requestForPayment(String measure, float price, String title) {
        if (measure.equals("paypal")) {
            price = price / 6f;
        }

        Utils.sendPaymentRequest(this, measure, title, title,
                String.valueOf(price), Constants.PAYMENT_TYPE_DONATION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mRoot = (CoordinatorLayout) findViewById(R.id.root);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new TabsAdapter(this, getSupportFragmentManager()));

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        AppConnect.getInstance("179a03b58d0dc099e7770f1f5e1f8887", "default", this);
        if (!Utils.checkLicensed(this) && MoKeeUtils.isSupportLanguage(false)) {
            AppConnect.getInstance(this).initPopAd(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        final Intent send = new Intent(BR_ONNewIntent);

        send.putExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_ID,
                intent.getLongExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_ID, -1));

        send.putExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_PATH,
                intent.getStringExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_PATH));

        sendBroadcastAsUser(send, UserHandle.CURRENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                makeSnackbar(R.string.donate_money_toast_success)
                        .setAction(R.string.donate_money_again, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                donateOrRemoveAdsDialog(true);
                            }
                        })
                        .show();

                RequestUtils.getRanking(getApplicationContext());
                getSharedPreferences(Constants.DOWNLOADER_PREF, 0).edit()
                        .putLong(Constants.KEY_DISCOUNT_TIME, System.currentTimeMillis())
                        .putLong(Constants.KEY_LEFT_TIME, 0).apply();
                break;
            case 200:
                makeSnackbar(R.string.donate_money_restored_success).show();
                RequestUtils.getRanking(getApplicationContext());
                break;
            case 500:
                makeSnackbar(R.string.donate_money_restored_failed).show();
                break;
            case 408:
                makeSnackbar(R.string.donate_money_restored_timeout).show();
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_donate:
                donateOrRemoveAdsDialog(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Snackbar makeSnackbar(CharSequence text) {
        return makeSnackbar(text, Snackbar.LENGTH_SHORT);
    }

    public Snackbar makeSnackbar(@StringRes int resId) {
        return makeSnackbar(resId, Snackbar.LENGTH_SHORT);
    }

    public Snackbar makeSnackbar(CharSequence text, int duration) {
        return Snackbar.make(mRoot, text, duration);
    }

    public Snackbar makeSnackbar(@StringRes int resId, int duration) {
        return Snackbar.make(mRoot, resId, duration);
    }

}
