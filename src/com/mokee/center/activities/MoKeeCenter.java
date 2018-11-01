/*
 * Copyright (C) 2014-2018 The MoKee Open Source Project
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
import android.content.Intent;
import android.mokee.utils.MoKeeUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mokee.center.R;
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

    private DrawerLayout mRoot;
    private LocalBroadcastManager lbm;

    public void donateOrUnlockFeatureDialog(final boolean isDonate) {
        final LayoutInflater inflater = LayoutInflater.from(this);

        @SuppressLint("InflateParams") final ViewGroup donateView = (ViewGroup) inflater.inflate(R.layout.donate, null);

        final TextView mRequest = donateView.findViewById(R.id.request);
        final SeekBar mSeekBar = donateView.findViewById(R.id.price);
        final RadioGroup mVia = donateView.findViewById(R.id.via);

        TextView mMessage = donateView.findViewById(R.id.message);
        mMessage.setText(R.string.donate_dialog_message);

        Float paid = Utils.getPaidTotal(this);

        if (isDonate) {
            mSeekBar.setMax(Constants.DONATION_MAX - Constants.DONATION_REQUEST_MIN);
            mRequest.setText(getString(R.string.donate_money_currency, Constants.DONATION_REQUEST_MIN));
        } else {
            mSeekBar.setMax(Constants.DONATION_TOTAL);
            mSeekBar.setProgress(paid >= Constants.DONATION_REQUEST ? Constants.DONATION_TOTAL : Constants.DONATION_REQUEST);
            mRequest.setText(paid >= Constants.DONATION_REQUEST ? getString(R.string.unlock_features_verify_rom_title, Constants.DONATION_TOTAL - paid.intValue())
                    : getString(R.string.unlock_features_ota_title, Constants.DONATION_REQUEST - paid.intValue()));
        }

        mSeekBar.setOnSeekBarChangeListener(new SimpleOnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isDonate) {
                    seekBar.setProgress(progress / 10 * 10);
                    mRequest.setText(getString(R.string.donate_money_currency,
                            progress / 10 * 10 + Constants.DONATION_REQUEST_MIN));
                } else {
                    if (progress > Constants.DONATION_REQUEST || paid >= Constants.DONATION_REQUEST) {
                        seekBar.setProgress(Constants.DONATION_TOTAL);
                        if (paid >= Constants.DONATION_REQUEST) {
                            mRequest.setText(getString(R.string.unlock_features_verify_rom_title, mSeekBar.getProgress() - paid.intValue()));
                        } else {
                            mRequest.setText(getString(R.string.unlock_features_all_title, mSeekBar.getProgress() - paid.intValue()));
                        }
                    } else {
                        seekBar.setProgress(Constants.DONATION_REQUEST);
                        mRequest.setText(getString(R.string.unlock_features_ota_title, mSeekBar.getProgress() - paid.intValue()));
                    }
                }
            }
        });

        String title = isDonate ? getString(R.string.donate_money_title) : getString(R.string.unlock_features_title);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(donateView)
                .setPositiveButton(R.string.donate_dialog_next, (dialog, which) -> {
                    float price = isDonate ? (float) (mSeekBar.getProgress() + Constants.DONATION_REQUEST_MIN)
                            : mSeekBar.getProgress() - paid;
                    switch (mVia.getCheckedRadioButtonId()) {
                        case R.id.alipay:
                            requestForPayment("alipay", price, title);
                            break;
                        case R.id.wechat:
                            if (!MoKeeUtils.isApkInstalledAndEnabled("com.tencent.mm", this)) {
                                makeSnackbar(R.string.activity_not_found).show();
                            } else {
                                requestForPayment("wechat", price, title);
                            }
                            break;
                        case R.id.paypal:
                            requestForPayment("paypal", price, title);
                            break;
                    }
                })
                .setNeutralButton(R.string.donate_dialog_faq, (dialog, which) -> {
                    Uri uri = Uri.parse("https://bbs.mokeedev.com/t/topic/9049/1");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                })
                .show();
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

        mRoot = findViewById(R.id.root);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lbm = LocalBroadcastManager.getInstance(this);
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

        lbm.sendBroadcast(send);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                makeSnackbar(R.string.donate_money_toast_success)
                        .setAction(R.string.donate_money_again, (view) -> {
                            donateOrUnlockFeatureDialog(true);
                        }).show();
                RequestUtils.fetchDonationRanking(this);
                break;
            case 200:
                makeSnackbar(R.string.donate_money_restored_success).show();
                RequestUtils.fetchDonationRanking(this);
                break;
            case 500:
                makeSnackbar(R.string.donate_money_restored_failed).setAction(R.string.donate_money_restored_failed_solution, (view) -> {
                    Uri uri = Uri.parse("https://bbs.mokeedev.com/t/topic/577");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }).show();
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
                                TextView textView = resultView.findViewById(R.id.message);
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
                                    .setNegativeButton(R.string.verify_system_compatible_root_skip, ((dialog, which) -> {
                                        // Reboot into recovery and trigger the update
                                        dialog.dismiss();
                                        try {
                                            Utils.triggerUpdateByPath(MoKeeCenter.this, updatePackagePath);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }));
                            TextView textView = resultView.findViewById(R.id.message);
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
            if (fragment != null) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
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
            case android.R.id.home:
                mRoot.openDrawer(Gravity.START);
                return true;
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

}
