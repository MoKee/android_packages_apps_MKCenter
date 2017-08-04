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
import android.os.Bundle;
import android.os.Handler;
import android.os.RecoverySystem;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MoKeeCenter extends AppCompatActivity {

    public static final String BR_ONNewIntent = "com.mokee.center.action.ON_NEW_INTENT";

    private static CoordinatorLayout mRoot;

    public void donateOrUnlockFeatureDialog(final boolean isDonate) {
        final LayoutInflater inflater = LayoutInflater.from(this);

        @SuppressLint("InflateParams")
        final ViewGroup donateView = (ViewGroup) inflater.inflate(R.layout.donate, null);

        final TextView mRequest = (TextView) donateView.findViewById(R.id.request);
        final SeekBar mSeekBar = (SeekBar) donateView.findViewById(R.id.price);

        TextView mMessage = (TextView) donateView.findViewById(R.id.message);
        mMessage.setText(R.string.donate_dialog_message);

        mSeekBar.setOnSeekBarChangeListener(new SimpleOnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBar.setProgress(progress / 10 * 10);
                mRequest.setText(getString(R.string.donate_money_currency,
                        progress / 10 * 10 + Constants.DONATION_REQUEST_MIN));
            }
        });

        if (isDonate) {
            mSeekBar.setMax(Constants.DONATION_MAX - Constants.DONATION_REQUEST_MIN);
            mRequest.setText(getString(R.string.donate_money_currency, Constants.DONATION_REQUEST_MIN));
        } else {
            Float paid = Utils.getPaidTotal(this);
            Float unPaid = paid > Constants.DONATION_REQUEST ? Constants.DONATION_TOTAL - paid : Constants.DONATION_REQUEST - paid;
            mSeekBar.setMax(Constants.DONATION_TOTAL - paid.intValue());
            mRequest.setText(getString(R.string.donate_money_currency, unPaid.intValue()));
        }

        String title = isDonate ? getString(R.string.donate_money_title)
                : getString(R.string.unlock_features_title);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title).setView(donateView);

        builder.setPositiveButton(R.string.donate_dialog_via_paypal, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                float price = isDonate
                        ? (float) (mSeekBar.getProgress() + Constants.DONATION_REQUEST_MIN)
                        : mSeekBar.getProgress();
                requestForPayment("paypal", price, title);
            }
        });

        builder.setNegativeButton(R.string.donate_dialog_via_alipay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                float price = isDonate
                        ? (float) (mSeekBar.getProgress() + Constants.DONATION_REQUEST_MIN)
                        : mSeekBar.getProgress();
                requestForPayment("alipay", price, title);
            }
        });
//        if (isDonate && MoKeeUtils.isSupportLanguage(false)) {
//            builder.setNeutralButton(R.string.donate_dialog_via_point, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    Utils.pointPaymentRequest(MoKeeCenter.this);
//                }
//            });
//        } else if (Utils.getPaidTotal(MoKeeCenter.this) == 0f) {
//            builder.setNeutralButton(R.string.donate_dialog_via_restore, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    Utils.restorePaymentRequest(MoKeeCenter.this);
//                }
//            });
//        }

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
        switch(resultCode) {
            case Activity.RESULT_OK:
                makeSnackbar(R.string.donate_money_toast_success)
                        .setAction(R.string.donate_money_again, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                donateOrUnlockFeatureDialog(true);
                            }
                        })
                        .show();

                RequestUtils.getRanking(this);
                getSharedPreferences(Constants.DOWNLOADER_PREF, 0).edit()
                        .putLong(Constants.KEY_DISCOUNT_TIME, System.currentTimeMillis())
                        .putLong(Constants.KEY_LEFT_TIME, 0).apply();
                break;
            case 200:
                makeSnackbar(R.string.donate_money_restored_success).show();
                RequestUtils.getRanking(this);
                break;
            case 500:
                makeSnackbar(R.string.donate_money_restored_failed).show();
                break;
            case 408:
                makeSnackbar(R.string.donate_money_restored_timeout).show();
                break;
            case 8000:
            case 8001:
                try {
                    Bundle bundle = data.getExtras();
                    String updatePackagePath = bundle.getString("update_package_path");
                    LayoutInflater inflater = LayoutInflater.from(this);
                    ViewGroup resultView = (ViewGroup) inflater.inflate(R.layout.result, null);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    if (resultCode == 8000) {
                        if (new File(Constants.CHECK_LOG_FILE).exists()) {
                            HashMap<String, String> hashMap = Utils.buildSystemCompatibleMessage(this);
                            if (hashMap.size() != 0 && hashMap.get("status").equals("7")) {
                                builder.setTitle(R.string.verify_system_compatible_failed)
                                        .setView(resultView)
                                        .setCancelable(false)
                                        .setPositiveButton(android.R.string.ok, null);
                                TextView textView = (TextView) resultView.findViewById(R.id.message);
                                textView.setText(hashMap.get("result"));
                                builder.show();
                            } else {
                                // Reboot into recovery and trigger the update
                                Utils.triggerUpdateByPath(this, updatePackagePath);
                            }
                        } else {
                            // Reboot into recovery and trigger the update
                            Utils.triggerUpdateByPath(this, updatePackagePath);
                        }
                    } else {
                        String value = SystemProperties.get(Constants.ROOT_ACCESS_PROPERTY, "0");
                        if (value.equals("0") || value.equals("2")) {
                            Utils.triggerUpdateByPath(MoKeeCenter.this, updatePackagePath);
                        } else {
                            builder.setTitle(R.string.verify_system_compatible_title)
                                    .setView(resultView)
                                    .setCancelable(false)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .setNegativeButton(R.string.verify_system_compatible_root_skip, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Reboot into recovery and trigger the update
                                            try {
                                                dialog.dismiss();
                                                Utils.triggerUpdateByPath(MoKeeCenter.this, updatePackagePath);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                            TextView textView = (TextView) resultView.findViewById(R.id.message);
                            textView.setText(R.string.verify_system_compatible_root_request);
                            builder.show();
                        }
                    }
                } catch (IOException e) {
                    makeSnackbar(R.string.apply_unable_to_reboot_toast).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
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
                donateOrUnlockFeatureDialog(true);
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

    public static CoordinatorLayout getRoot() {
        return mRoot;
    }
}
