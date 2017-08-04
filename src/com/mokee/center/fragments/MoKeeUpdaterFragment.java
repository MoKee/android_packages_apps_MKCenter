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

package com.mokee.center.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.icu.text.SimpleDateFormat;
import android.mokee.utils.MoKeeUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.design.widget.Snackbar;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.mokee.center.R;
import com.mokee.center.activities.MoKeeCenter;
import com.mokee.center.db.DownLoadDao;
import com.mokee.center.db.ThreadDownLoadDao;
import com.mokee.center.misc.Constants;
import com.mokee.center.misc.DownLoadInfo;
import com.mokee.center.misc.ItemInfo;
import com.mokee.center.misc.State;
import com.mokee.center.misc.ThreadDownLoadInfo;
import com.mokee.center.receiver.DownloadReceiver;
import com.mokee.center.service.DownLoadService;
import com.mokee.center.service.UpdateCheckService;
import com.mokee.center.utils.DownLoader;
import com.mokee.center.utils.UpdateFilter;
import com.mokee.center.utils.Utils;
import com.mokee.center.widget.AdmobPreference;
import com.mokee.center.widget.EmptyListPreference;
import com.mokee.center.widget.ItemPreference;
import com.mokee.os.Build;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MoKeeUpdaterFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener,
        ItemPreference.OnReadyListener,
        ItemPreference.OnActionListener {

    public static final String EXPERIMENTAL_SHOW = "experimental_show";

    private static final String KEY_MOKEE_VERSION = "mokee_version";
    private static final String KEY_MOKEE_UNIQUE_ID = "mokee_unique_id";
    private static final String KEY_MOKEE_DONATE_INFO = "mokee_donate_info";
    private static final String KEY_MOKEE_LAST_CHECK = "mokee_last_check";

    private static final String UPDATES_CATEGORY = "updates_category";

    private static final int TAPS_TO_BE_A_EXPERIMENTER = 7;

    private static String TAG = "MoKeeUpdaterFragment";

    private static String updateTypeString, MoKeeVersionType, MoKeeVersionTypeString;

    private static SharedPreferences mPrefs;
    private static SwitchPreference mUpdateOTA;
    private static SwitchPreference mVerifyROM;

    private MoKeeCenter moKeeCenter;

    private boolean mDownloading = false;
    private long mDownloadId;
    private String mFileName;
    private int mExpHitCountdown;

    private AdmobPreference mAdmobView;
    private PreferenceScreen mRootView;
    private ListPreference mUpdateCheck;
    private ListPreference mUpdateType;
    private PreferenceCategory mUpdatesList;
    private ItemPreference mDownloadingPreference;

    private File mUpdateFolder;
    private ProgressDialog mProgressDialog;
    private Handler mUpdateHandler = new Handler();

    private long leftTime;
    private Runnable timerRunnable;

    private InterstitialAd mEnterInterstitialAd;
    private InterstitialAd mStartDownloadInterstitialAd;
    private AdRequest adRequest;

    // 更新进度条
    private Runnable mUpdateProgress = new Runnable() {
        public void run() {
            if (!mDownloading || mDownloadingPreference == null || mDownloadId < 0) {
                return;
            }

            ProgressBar progressBar = mDownloadingPreference.getProgressBar();
            if (progressBar == null) {
                return;
            }
            DownLoadInfo dli = DownLoadDao.getInstance().getDownLoadInfo(String.valueOf(mDownloadId));
            int status;

            if (dli == null) {
                // DownloadReceiver has likely already removed the download
                // from the DB due to failure or MD5 mismatch
                status = DownLoader.STATUS_PENDING;
            } else {
                status = dli.getState();
            }
            switch (status) {
                case DownLoader.STATUS_PENDING:
                    progressBar.setIndeterminate(true);
                    break;
                case DownLoader.STATUS_DOWNLOADING:
                    List<ThreadDownLoadInfo> threadList = ThreadDownLoadDao.getInstance().getThreadInfoList(dli.getUrl());
                    int totalBytes = -1;
                    int downloadedBytes = 0;
                    for (ThreadDownLoadInfo info : threadList) {
                        downloadedBytes += info.getDownSize();
                        totalBytes += info.getEndPos() - info.getStartPos() + 1;
                    }

                    if (totalBytes < 0) {
                        progressBar.setIndeterminate(true);
                    } else {
                        progressBar.setIndeterminate(false);
                        progressBar.setMax(totalBytes);
                        progressBar.setProgress(downloadedBytes);
                    }
                    break;
                case DownLoader.STATUS_ERROR:
                case DownLoader.STATUS_PAUSED:
                    mDownloadingPreference.setStyle(ItemPreference.STYLE_NEW);
                    resetDownloadState();
                    break;
            }
            if (status != DownLoader.STATUS_ERROR) {
                mUpdateHandler.postDelayed(this, 1500);
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadReceiver.ACTION_DOWNLOAD_STARTED.equals(action)) {
                mDownloadId = intent.getLongExtra(DownLoadService.DOWNLOAD_ID, -1);
                mUpdateHandler.post(mUpdateProgress);
            } else if (UpdateCheckService.ACTION_CHECK_FINISHED.equals(action)) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                    int count = intent.getIntExtra(UpdateCheckService.EXTRA_NEW_UPDATE_COUNT, -1);
                    if (count == 0) {
                        moKeeCenter.makeSnackbar(R.string.no_updates_found).show();
                    } else if (count < 0) {
                        moKeeCenter.makeSnackbar(R.string.update_check_failed, Snackbar.LENGTH_LONG).show();
                    }
                }
                updateLayout();
            } else if (MoKeeCenter.BR_ONNewIntent.equals(action)) {
                updateLayout();
                checkForDownloadCompleted(intent);
            }
        }
    };

    @Override
    @SuppressWarnings("deprecation")
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.moKeeCenter = (MoKeeCenter) activity;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refreshOption();
    }

    public void refreshOTAOption() {
        Float currentPaid = Utils.getPaidTotal(moKeeCenter);
        if (currentPaid < Constants.DONATION_REQUEST) {
            mPrefs.edit().putBoolean(Constants.OTA_CHECK_PREF, false).apply();
            mUpdateOTA.setEnabled(false);
            if (currentPaid == 0f) {
                mUpdateOTA.setSummary(getString(R.string.pref_ota_check_donation_request_summary,
                        Float.valueOf(Constants.DONATION_REQUEST - currentPaid).intValue()));
            } else {
                mUpdateOTA.setSummary(getString(R.string.pref_ota_check_donation_request_pending_summary,
                        currentPaid.intValue(),
                        Float.valueOf(Constants.DONATION_REQUEST - currentPaid.intValue()).intValue()));
            }
        } else {
            if (!MoKeeVersionTypeString.equals(updateTypeString)) {
                mPrefs.edit().putBoolean(Constants.OTA_CHECK_PREF, false).apply();
            }
            mUpdateOTA.setEnabled(true);
            mUpdateOTA.setSummary(R.string.pref_ota_check_summary);
        }
    }

    public void refreshVerifyOption() {
        Float currentPaid = Utils.getPaidTotal(moKeeCenter);
        if (currentPaid < Constants.DONATION_TOTAL) {
            mPrefs.edit().putBoolean(Constants.VERIFY_ROM_PREF, false).apply();
            mVerifyROM.setEnabled(false);
            if (currentPaid == 0f) {
                mVerifyROM.setSummary(getString(R.string.pref_verify_rom_donation_request_summary,
                        Float.valueOf(Constants.DONATION_TOTAL - currentPaid).intValue()));
            } else {
                mVerifyROM.setSummary(getString(R.string.pref_verify_rom_donation_request_pending_summary,
                        currentPaid.intValue(),
                        Float.valueOf(Constants.DONATION_TOTAL - currentPaid.intValue()).intValue()));
            }
        } else {
            mVerifyROM.setEnabled(true);
            mVerifyROM.setSummary(R.string.pref_verify_rom_summary);
        }
    }

    public void refreshOption() {
        moKeeCenter.invalidateOptionsMenu();
        refreshOTAOption();
        refreshVerifyOption();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Initialize the Mobile Ads SDK.
        if (!Utils.checkLicensed(moKeeCenter)) {
            MobileAds.initialize(getContext(), getString(R.string.app_id));
            adRequest = new AdRequest.Builder().build();
            if (mAdmobView != null) {
                mAdmobView.setAdRequest(adRequest);
            }

            mEnterInterstitialAd = new InterstitialAd(getContext());
            mEnterInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
            mEnterInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int errorCode) {
                    super.onAdFailedToLoad(errorCode);
                    if (errorCode == AdRequest.ERROR_CODE_NO_FILL) {
                        mEnterInterstitialAd.loadAd(adRequest);
                    }
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mEnterInterstitialAd.show();
                }
            });
            mEnterInterstitialAd.loadAd(adRequest);

            mStartDownloadInterstitialAd = new InterstitialAd(getContext());
            mStartDownloadInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
            mStartDownloadInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    mStartDownloadInterstitialAd.loadAd(adRequest);
                    if (!Utils.checkMinLicensed(moKeeCenter)) {
                        moKeeCenter.makeSnackbar(R.string.download_limited_mode, Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onAdFailedToLoad(int errorCode) {
                    super.onAdFailedToLoad(errorCode);
                    if (errorCode == AdRequest.ERROR_CODE_NO_FILL) {
                        mStartDownloadInterstitialAd.loadAd(adRequest);
                    }
                }
            });
            mStartDownloadInterstitialAd.loadAd(adRequest);
        }
    }

    @Override
    public void onPause() {
        if (mAdmobView != null) {
            mAdmobView.onAdPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mAdmobView != null) {
            mAdmobView.onAdDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the layouts
        addPreferencesFromResource(R.xml.mokee_updater);

        // Load the stored preference data
        mPrefs = moKeeCenter.getSharedPreferences(Constants.DOWNLOADER_PREF, 0);

        mRootView = (PreferenceScreen) findPreference(Constants.ROOT_PREF);
        mAdmobView = (AdmobPreference) findPreference(Constants.ADMOB_PREF);

        mUpdatesList = (PreferenceCategory) findPreference(UPDATES_CATEGORY);
        mUpdateCheck = (ListPreference) findPreference(Constants.UPDATE_INTERVAL_PREF);
        mUpdateType = (ListPreference) findPreference(Constants.UPDATE_TYPE_PREF);
        mUpdateOTA = (SwitchPreference) findPreference(Constants.OTA_CHECK_PREF);
        mVerifyROM = (SwitchPreference) findPreference(Constants.VERIFY_ROM_PREF);

        // Restore normal type list
        MoKeeVersionType = Utils.getReleaseVersionType();
        boolean isExperimental = TextUtils.equals(MoKeeVersionType, "experimental");
        boolean isUnofficial = TextUtils.equals(MoKeeVersionType, "unofficial");
        boolean experimentalShow = mPrefs.getBoolean(EXPERIMENTAL_SHOW, isExperimental);
        int type = mPrefs.getInt(Constants.UPDATE_TYPE_PREF, Utils.getUpdateType(MoKeeVersionType));
        if (type == 2 && !experimentalShow) {
            mPrefs.edit().putBoolean(EXPERIMENTAL_SHOW, false).putInt(Constants.UPDATE_TYPE_PREF, 0).apply();
        }
        if (type == 3 && !isUnofficial) {
            mPrefs.edit().putInt(Constants.UPDATE_TYPE_PREF, 0).apply();
        }

        if (mUpdateCheck != null) {
            if (Utils.checkMinLicensed(moKeeCenter)) {
                int check = mPrefs.getInt(Constants.UPDATE_INTERVAL_PREF, Constants.UPDATE_FREQ_DAILY);
                mUpdateCheck.setValue(String.valueOf(check));
                mUpdateCheck.setSummary(mapCheckValue(check));
                mUpdateCheck.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mUpdateCheck);
            }
        }

        if (mUpdateType != null) {
            mUpdateType.setValue(String.valueOf(type));
            mUpdateType.setOnPreferenceChangeListener(this);
            if (!isUnofficial) {
                if (experimentalShow) {
                    setExperimentalTypeEntries();
                } else {
                    setNormalTypeEntries();
                }
            } else {
                if (experimentalShow) {
                    setAllTypeEntries();
                } else {
                    setUnofficialTypeEntries();
                }
            }
            setUpdateTypeSummary(type);
        }

        MoKeeVersionTypeString = Utils.getReleaseVersionTypeString(moKeeCenter, MoKeeVersionType);

        if (!MoKeeVersionType.equals("history")) {
            // 增量更新
            refreshOTAOption();
            mUpdateOTA.setChecked(mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false));
            mUpdateOTA.setOnPreferenceChangeListener(this);
            isOTA(mUpdateOTA.isChecked());
            // 安全更新
            refreshVerifyOption();
            mVerifyROM.setChecked(mPrefs.getBoolean(Constants.VERIFY_ROM_PREF, false));
            mVerifyROM.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference(mUpdateOTA);
            getPreferenceScreen().removePreference(mVerifyROM);
        }

        setSummaryFromProperty(KEY_MOKEE_VERSION, "ro.mk.version");
        Utils.setSummaryFromString(this, KEY_MOKEE_UNIQUE_ID, Build.getUniqueID(moKeeCenter));

        updateLastCheckPreference();

        setHasOptionsMenu(true);

        discountDialog(mPrefs);
    }

    public void discountDialog(final SharedPreferences mPrefs) {
        final Float paid = mPrefs.getFloat(Constants.KEY_DONATE_AMOUNT, 0);

        // 同步云端支付信息
        if (paid.intValue() != Utils.getPaidTotal(moKeeCenter).intValue()) {
            Utils.restorePaymentRequest(moKeeCenter);
        }

        if (Utils.Discounting(mPrefs)) {
            final Float unPaid = Constants.DONATION_TOTAL - Constants.DONATION_DISCOUNT - paid;
            final SimpleDateFormat df = new SimpleDateFormat("mm:ss");

            final LayoutInflater inflater = LayoutInflater.from(moKeeCenter);

            @SuppressLint("InflateParams")
            final LinearLayout donateView = (LinearLayout) inflater.inflate(R.layout.donate, null);

            final TextView mLeftTimeView = (TextView) donateView.findViewById(R.id.request);

            donateView.findViewById(R.id.price).setVisibility(View.GONE);

            final ProgressBar mProgressBar = (ProgressBar) donateView.findViewById(R.id.progress);
            mProgressBar.setMax(100);
            mProgressBar.setVisibility(View.VISIBLE);

            leftTime = mPrefs.getLong(Constants.KEY_LEFT_TIME, Constants.DISCOUNT_THINK_TIME);
            mProgressBar.setProgress(Float.valueOf((float) leftTime / Constants.DISCOUNT_THINK_TIME * 100).intValue());
            mLeftTimeView.setText(String.format(getString(R.string.discount_dialog_expires), df.format(leftTime)));

            final AlertDialog.Builder builder = new AlertDialog.Builder(moKeeCenter)
                    .setTitle(R.string.discount_dialog_title)
                    .setMessage(getString(R.string.discount_dialog_message, unPaid.intValue(), Constants.DONATION_DISCOUNT))
                    .setView(donateView);

            builder.setPositiveButton(R.string.donate_dialog_via_paypal, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    requestForDiscountPayment("paypal", unPaid,
                            getString(R.string.discount_dialog_title));
                    mUpdateHandler.removeCallbacks(timerRunnable);
                    mPrefs.edit().putLong(Constants.KEY_LEFT_TIME, leftTime).apply();
                }
            });

            builder.setNegativeButton(R.string.donate_dialog_via_alipay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    requestForDiscountPayment("alipay", unPaid,
                            getString(R.string.discount_dialog_title));
                    mUpdateHandler.removeCallbacks(timerRunnable);
                    mPrefs.edit().putLong(Constants.KEY_LEFT_TIME, leftTime).apply();
                }
            });

            builder.setNeutralButton(R.string.discount_dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mUpdateHandler.removeCallbacks(timerRunnable);
                    mPrefs.edit()
                            .putLong(Constants.KEY_DISCOUNT_TIME, System.currentTimeMillis())
                            .putLong(Constants.KEY_LEFT_TIME, Constants.DISCOUNT_THINK_TIME)
                            .apply();
                }
            });

            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mUpdateHandler.removeCallbacks(timerRunnable);
                    mPrefs.edit().putLong(Constants.KEY_LEFT_TIME, leftTime).apply();
                }
            });

            final AlertDialog discountDialog = builder.create();
            discountDialog.show();

            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    leftTime = leftTime - DateUtils.SECOND_IN_MILLIS;
                    mProgressBar.setProgress(Float.valueOf((float) leftTime / Constants.DISCOUNT_THINK_TIME * 100).intValue());
                    mLeftTimeView.setText(String.format(getString(R.string.discount_dialog_expires), df.format(leftTime)));
                    if (leftTime <= 0) {
                        discountDialog.dismiss();
                        mPrefs.edit().putLong(Constants.KEY_DISCOUNT_TIME, System.currentTimeMillis())
                                .putLong(Constants.KEY_LEFT_TIME, Constants.DISCOUNT_THINK_TIME).apply();
                    } else {
                        mPrefs.edit().putLong(Constants.KEY_LEFT_TIME, leftTime).apply();
                        mUpdateHandler.postDelayed(this, DateUtils.SECOND_IN_MILLIS);
                    }
                }
            };

            mUpdateHandler.postDelayed(timerRunnable, DateUtils.SECOND_IN_MILLIS);
        }
    }

    private void requestForDiscountPayment(String measure, float price, String title) {
        if (measure.equals("paypal")) {
            price = price / 6f;
        }

        Utils.sendPaymentRequest(moKeeCenter, measure, title, title,
                String.valueOf(price), Constants.PAYMENT_TYPE_DISCOUNT);
    }

    private void setDonatePreference() {
        Float paid = Utils.getPaidTotal(moKeeCenter);
        Float amount = mPrefs.getFloat(Constants.KEY_DONATE_AMOUNT, 0f);
        if (amount < paid) {
            amount = paid;
        }
        int rank = mPrefs.getInt(Constants.KEY_DONATE_RANK, 0);
        int percent = mPrefs.getInt(Constants.KEY_DONATE_PERCENT, 0);
        if (paid == 0f) {
            Utils.setSummaryFromString(this, KEY_MOKEE_DONATE_INFO,
                    getString(R.string.donate_money_info_null));
        } else if (percent != 0) {
            Utils.setSummaryFromString(this, KEY_MOKEE_DONATE_INFO,
                    getString(R.string.donate_money_info_with_rank,
                            amount.intValue(), String.valueOf(percent) + "%", rank));
        } else {
            Utils.setSummaryFromString(this, KEY_MOKEE_DONATE_INFO,
                    getString(R.string.donate_money_info,
                            amount.intValue(), "1%"));
        }
    }

    private void setUpdateTypeSummary(int type) {
        CharSequence[] entryValues = mUpdateType.getEntryValues();
        CharSequence[] entries = mUpdateType.getEntries();
        for (int i = 0; i < entryValues.length; i++) {
            if (Integer.valueOf(entryValues[i].toString()) == type) {
                mUpdateType.setSummary(entries[i]);
                updateTypeString = entries[i].toString();
            }
        }
        mUpdateType.setValue(String.valueOf(type));
    }

    private void removeAdmobPreference() {
        if (mAdmobView != null) {
            mAdmobView.onAdDestroy();
            mRootView.removePreference(mAdmobView);
            mAdmobView = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mExpHitCountdown = mPrefs.getBoolean(EXPERIMENTAL_SHOW,
                TextUtils.equals(Utils.getReleaseVersionType(), "experimental")) ? -1 : TAPS_TO_BE_A_EXPERIMENTER;
        // Remove Ad
        if (Utils.checkLicensed(moKeeCenter)) {
            removeAdmobPreference();
        } else {
            if (mAdmobView != null) {
                mAdmobView.onAdResume();
            }
        }
        setDonatePreference();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(KEY_MOKEE_VERSION)) {
            // Don't enable experimental option for secondary users.
            if (UserHandle.myUserId() != UserHandle.USER_OWNER)
                return true;

            if (mExpHitCountdown > 0) {
                mExpHitCountdown--;
                if (mExpHitCountdown == 0) {
                    mPrefs.edit().putBoolean(EXPERIMENTAL_SHOW, true).apply();
                    moKeeCenter.makeSnackbar(R.string.show_exp_on, Snackbar.LENGTH_LONG).show();
                    String MoKeeVersionType = Utils.getReleaseVersionType();
                    boolean isUnofficial = TextUtils.equals(MoKeeVersionType, "unofficial");
                    if (!isUnofficial) {
                        setExperimentalTypeEntries();
                    } else {
                        setAllTypeEntries();
                    }
                } else if (mExpHitCountdown > 0 && mExpHitCountdown < (TAPS_TO_BE_A_EXPERIMENTER - 2)) {
                    moKeeCenter.makeSnackbar(getResources()
                            .getQuantityString(R.plurals.show_exp_countdown, mExpHitCountdown, mExpHitCountdown))
                            .show();
                }
            } else if (mExpHitCountdown < 0) {
                moKeeCenter.makeSnackbar(R.string.show_exp_already, Snackbar.LENGTH_LONG).show();
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    public void updateLastCheckPreference() {
        long lastCheckTime = mPrefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0);
        if (lastCheckTime == 0) {
            Utils.setSummaryFromString(this, KEY_MOKEE_LAST_CHECK,
                    getString(R.string.mokee_last_check_never));
        } else {
            Date lastCheck = new Date(lastCheckTime);
            String date = DateFormat.getLongDateFormat(moKeeCenter).format(lastCheck);
            String time = DateFormat.getTimeFormat(moKeeCenter).format(lastCheck);
            Utils.setSummaryFromString(this, KEY_MOKEE_LAST_CHECK, date + " " + time);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.updater, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (Utils.checkLicensed(moKeeCenter)) {
            menu.findItem(R.id.menu_unlock_features).setVisible(false);
        } else {
            menu.findItem(R.id.menu_donate).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                checkForUpdates();
                return true;
            case R.id.menu_delete_all:
                confirmDeleteAll();
                return true;
            case R.id.menu_unlock_features:
                moKeeCenter.donateOrUnlockFeatureDialog(false);
                return true;
            case R.id.menu_restore:
                Utils.restorePaymentRequest(moKeeCenter);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onReady(ItemPreference pref) {
        pref.setOnReadyListener(null);
        mUpdateHandler.post(mUpdateProgress);
    }

    private void setSummaryFromProperty(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property, getString(R.string.mokee_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private void resetDownloadState() {
        mDownloadId = -1;
        mFileName = null;
        mDownloading = false;
        mDownloadingPreference = null;
    }

    private void updateLayout() {
        updateLastCheckPreference();
        // Read existing Updates
        LinkedList<String> existingFiles = new LinkedList<>();
        mUpdateFolder = Utils.makeUpdateFolder();
        File[] files = mUpdateFolder.listFiles(new UpdateFilter(".zip"));
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory() && files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    existingFiles.add(file.getName());
                }
            }
        }

        // Clear the notification if one exists
        Utils.cancelNotification(moKeeCenter);

        // Build list of updates
        final LinkedList<ItemInfo> availableUpdates = State.loadMKState(
                moKeeCenter, State.UPDATE_FILENAME);

        if (!mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false)) {
            Collections.sort(availableUpdates, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    /* sort by date descending */
                    int lhsDate = Integer.valueOf(Utils.subBuildDate(lhs.getFileName(), false));
                    int rhsDate = Integer.valueOf(Utils.subBuildDate(rhs.getFileName(), false));
                    if (lhsDate == rhsDate) {
                        return 0;
                    }
                    return lhsDate < rhsDate ? 1 : -1;
                }
            });
        }

        // Update the preference list
        refreshPreferences(availableUpdates);
    }

    private void refreshPreferences(LinkedList<ItemInfo> updates) {
        if (mUpdatesList == null) {
            return;
        }

        // Clear the list
        mUpdatesList.removeAll();

        // Convert the installed version name to the associated filename
        String installedZip = Build.VERSION + ".zip";
        boolean isNew = true; // 判断新旧版本

        // Add the updates
        for (ItemInfo ui : updates) {
            // Determine the preference style and create the preference
            boolean isDownloading = ui.getFileName().equals(mFileName);
            boolean isLocalFile = Utils.isLocaUpdateFile(ui.getFileName());
            int style = 3;

            if (!mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false)) {
                isNew = Utils.isNewVersion(ui.getFileName());
            } else {
                isNew = Integer.valueOf(Utils.subBuildDate(ui.getFileName(), true)) > Integer.valueOf(Utils.subBuildDate(Build.VERSION, true));
                if (!isNew) {
                    break;
                }
            }

            if (isDownloading) {
                // In progress download
                style = ItemPreference.STYLE_DOWNLOADING;
            } else if (ui.getFileName().equals(installedZip)) {
                // This is the currently installed version
                style = ItemPreference.STYLE_INSTALLED;
            } else if (!isLocalFile && isNew) {
                style = ItemPreference.STYLE_NEW;
            } else if (!isLocalFile && !isNew) {
                style = ItemPreference.STYLE_OLD;
            } else if (isLocalFile) {
                style = ItemPreference.STYLE_DOWNLOADED;
            }

            ItemPreference up = new ItemPreference(moKeeCenter, ui, style);
            up.setOnActionListener(this);
            up.setKey(ui.getFileName());

            // If we have an in progress download, link the preference
            if (isDownloading) {
                mDownloadingPreference = up;
                up.setOnReadyListener(this);
                mDownloading = true;
            }

            // Add to the list
            mUpdatesList.addPreference(up);
        }

        // If no updates are in the list, show the default message
        if (mUpdatesList.getPreferenceCount() == 0) {
            EmptyListPreference pref = new EmptyListPreference(moKeeCenter, mUpdateOTA.isChecked());
            mUpdatesList.addPreference(pref);
        }
    }

    private String mapCheckValue(Integer value) {
        Resources resources = getResources();
        String[] checkNames = resources.getStringArray(R.array.update_check_entries);
        String[] checkValues = resources.getStringArray(R.array.update_check_values);
        for (int i = 0; i < checkValues.length; i++) {
            if (Integer.decode(checkValues[i]).equals(value)) {
                return checkNames[i];
            }
        }
        return getString(R.string.unknown);
    }

    private void isOTA(boolean value) {
        if (value) {
            mUpdateType.setEnabled(false);
        } else {
            mUpdateType.setEnabled(true);
        }
    }

    /**
     * 检测更新
     */
    private void checkForUpdates() {
        if (mProgressDialog != null) {
            return;
        }

        State.saveMKState(moKeeCenter, new LinkedList<ItemInfo>(), State.UPDATE_FILENAME);// clear
        refreshPreferences(new LinkedList<ItemInfo>());// clear

        // If there is no internet connection, display a message and return.
        if (!MoKeeUtils.isOnline(moKeeCenter)) {
            moKeeCenter.makeSnackbar(R.string.data_connection_required).show();
            return;
        }

        mProgressDialog = new ProgressDialog(moKeeCenter);
        mProgressDialog.setTitle(R.string.mokee_updater_title);
        mProgressDialog.setMessage(getString(R.string.checking_for_updates));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Intent cancelIntent = new Intent(moKeeCenter, UpdateCheckService.class);
                cancelIntent.setAction(UpdateCheckService.ACTION_CANCEL_CHECK);
                moKeeCenter.startServiceAsUser(cancelIntent, UserHandle.CURRENT);
                mProgressDialog = null;
                if (mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false)) {
                    mPrefs.edit().putBoolean(Constants.OTA_CHECK_MANUAL_PREF, false).apply();
                }
            }
        });

        if (mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false)) {
            mPrefs.edit()
                    .putBoolean(Constants.OTA_CHECK_MANUAL_PREF,
                            mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false))
                    .apply();
        }

        Intent checkIntent = new Intent(moKeeCenter, UpdateCheckService.class);
        checkIntent.setAction(UpdateCheckService.ACTION_CHECK);
        moKeeCenter.startServiceAsUser(checkIntent, UserHandle.CURRENT);

        mProgressDialog.show();
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(moKeeCenter)
                .setTitle(R.string.confirm_delete_dialog_title)
                .setMessage(R.string.confirm_delete_updates_all_dialog_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // We are OK to delete, trigger it
                        onPauseDownload(mPrefs);
                        deleteOldUpdates();
                        updateLayout();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private boolean deleteOldUpdates() {
        boolean success;
        // mUpdateFolder: Foldername with fullpath of SDCARD
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
            Utils.deleteDir(mUpdateFolder);
            mUpdateFolder.mkdir();
            success = true;
            moKeeCenter.makeSnackbar(R.string.delete_updates_success_message).show();
        } else if (!mUpdateFolder.exists()) {
            success = false;
            moKeeCenter.makeSnackbar(R.string.delete_updates_nofolder_message).show();
        } else {
            success = false;
            moKeeCenter.makeSnackbar(R.string.delete_updates_failure_message).show();
        }
        return success;
    }

    private void updateUpdatesType(int type) {
        mPrefs.edit().putInt(Constants.UPDATE_TYPE_PREF, type).apply();
        setUpdateTypeSummary(type);
        refreshOTAOption();
        checkForUpdates();
    }

    private void checkForDownloadCompleted(Intent intent) {
        if (intent == null) {
            return;
        }
        long downloadId = intent.getLongExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_ID, -1);
        if (downloadId < 0) {
            return;
        }
        String fullPathName = intent
                .getStringExtra(UpdateCheckService.EXTRA_FINISHED_DOWNLOAD_PATH);
        if (fullPathName == null) {
            return;
        }
        String fileName = new File(fullPathName).getName();

        // Find the matching preference so we can retrieve the ItemInfo
        ItemPreference pref = (ItemPreference) mUpdatesList.findPreference(fileName);
        if (pref != null) {
            pref.setStyle(ItemPreference.STYLE_DOWNLOADED);// download over
            // Change
            onStartUpdate(pref);
        }
        resetDownloadState();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Determine if there are any in-progress downloads
        mDownloadId = mPrefs.getLong(DownLoadService.DOWNLOAD_ID, -1);
        if (mDownloadId >= 0) {
            DownLoadInfo dli = DownLoadDao.getInstance().getDownLoadInfo(String.valueOf(mDownloadId));
            if (dli != null) {
                int status = dli.getState();
                if (status == DownLoader.STATUS_PENDING
                        || status == DownLoader.STATUS_DOWNLOADING
                        || status == DownLoader.STATUS_PAUSED) {
                    String localFileName = dli.getLocalFile();
                    if (!TextUtils.isEmpty(localFileName)) {
                        mFileName = localFileName.substring(localFileName.lastIndexOf("/") + 1,
                                localFileName.lastIndexOf("."));
                    }
                }
            }
        }
        if (mDownloadId < 0 || mFileName == null) {
            resetDownloadState();
        }

        updateLayout();
        IntentFilter filter = new IntentFilter(UpdateCheckService.ACTION_CHECK_FINISHED);
        filter.addAction(DownloadReceiver.ACTION_DOWNLOAD_STARTED);
        filter.addAction(MoKeeCenter.BR_ONNewIntent);// 唤醒
        moKeeCenter.registerReceiver(mReceiver, filter);

        checkForDownloadCompleted(moKeeCenter.getIntent());
        moKeeCenter.setIntent(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        mUpdateHandler.removeCallbacks(mUpdateProgress);
        moKeeCenter.unregisterReceiver(mReceiver);
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
    }

    @Override
    public void onStartDownload(ItemPreference pref) {
        // If there is no internet connection, display a message and return.
        if (!MoKeeUtils.isOnline(moKeeCenter)) {
            moKeeCenter.makeSnackbar(R.string.data_connection_required).show();
            return;
        } else if (mDownloading) {
            moKeeCenter.makeSnackbar(R.string.download_already_running, Snackbar.LENGTH_LONG).show();
            return;
        }

        if (!Utils.checkLicensed(moKeeCenter) && mStartDownloadInterstitialAd != null
                && mStartDownloadInterstitialAd.isLoaded()) {
            mStartDownloadInterstitialAd.show();
        } else {
            if (!Utils.checkMinLicensed(moKeeCenter)) {
                moKeeCenter.makeSnackbar(R.string.download_limited_mode, Snackbar.LENGTH_LONG).show();
            }
        }

        // We have a match, get ready to trigger the download
        mDownloadingPreference = pref;

        ItemInfo ui = mDownloadingPreference.getItemInfo();
        if (ui == null) {
            return;
        }

        mDownloadingPreference.setStyle(ItemPreference.STYLE_DOWNLOADING);
        mFileName = ui.getFileName();
        mDownloading = true;

        // Start the download
        Intent intent = new Intent(moKeeCenter, DownloadReceiver.class);
        intent.setAction(DownloadReceiver.ACTION_DOWNLOAD_START);
        intent.putExtra(DownloadReceiver.EXTRA_UPDATE_INFO, (Parcelable) ui);
        moKeeCenter.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    @Override
    public void onStopDownload(final ItemPreference pref) {
        if (!mDownloading || mFileName == null || mDownloadId < 0) {
            if (Utils.isNewVersion(pref.getItemInfo().getFileName())) {
                pref.setStyle(ItemPreference.STYLE_NEW);
            } else {
                pref.setStyle(ItemPreference.STYLE_OLD);
            }
            resetDownloadState();
            return;
        }

        new AlertDialog.Builder(moKeeCenter)
                .setTitle(R.string.confirm_download_cancelation_dialog_title)
                .setMessage(R.string.confirm_download_cancelation_dialog_message)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Set the preference back to new style
                        if (!mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false)) {
                            if (Utils.isNewVersion(pref.getItemInfo().getFileName())) {
                                pref.setStyle(ItemPreference.STYLE_NEW);
                            } else {
                                pref.setStyle(ItemPreference.STYLE_OLD);
                            }
                        } else {
                            pref.setStyle(ItemPreference.STYLE_NEW);
                        }
                        onPauseDownload(mPrefs);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    public void onPauseDownload(SharedPreferences prefs) {
        // We are OK to stop download, trigger it
        if (mDownloading) {
            moKeeCenter.makeSnackbar(R.string.download_cancelled).show();
        }

        resetDownloadState();
        mUpdateHandler.removeCallbacks(mUpdateProgress);

        Intent intent = new Intent(moKeeCenter, DownLoadService.class);
        intent.setAction(DownLoadService.ACTION_DOWNLOAD);
        intent.putExtra(DownLoadService.DOWNLOAD_TYPE, DownLoadService.PAUSE);
        intent.putExtra(DownLoadService.DOWNLOAD_URL, mPrefs.getString(DownLoadService.DOWNLOAD_URL, ""));

        moKeeCenter.startServiceAsUser(intent, UserHandle.CURRENT);
    }

    @Override
    public void onStartUpdate(ItemPreference pref) {
        final ItemInfo itemInfo = pref.getItemInfo();

        // Get the message body right
        String dialogBody = getString(
                itemInfo.getFileName().startsWith("OTA")
                        ? R.string.apply_update_ota_dialog_text
                        : R.string.apply_update_dialog_text,
                itemInfo.getFileName());

        // Display the dialog
        new AlertDialog.Builder(moKeeCenter)
                .setTitle(R.string.apply_update_dialog_title)
                .setMessage(dialogBody)
                .setPositiveButton(R.string.dialog_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            dialog.dismiss();
                            Utils.verifySystemCompatible(moKeeCenter, itemInfo.getFileName());
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to reboot into recovery mode", e);
                            moKeeCenter.makeSnackbar(R.string.apply_unable_to_reboot_toast).show();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDeleteUpdate(ItemPreference pref) {
        final String fileName = pref.getKey();

        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
            File zipFileToDelete = new File(mUpdateFolder, fileName);

            if (zipFileToDelete.exists()) {
                zipFileToDelete.delete();
            } else {
                Log.d(TAG, "Update to delete not found");
                return;
            }

            String message = getString(R.string.delete_single_update_success_message, fileName);
            moKeeCenter.makeSnackbar(message).show();
        } else if (!mUpdateFolder.exists()) {
            moKeeCenter.makeSnackbar(R.string.delete_updates_nofolder_message).show();
        } else {
            moKeeCenter.makeSnackbar(R.string.delete_updates_failure_message).show();
        }

        // Update the list
        updateLayout();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mUpdateCheck) {
            int value = Integer.valueOf((String) newValue);
            mPrefs.edit().putInt(Constants.UPDATE_INTERVAL_PREF, value).apply();
            mUpdateCheck.setSummary(mapCheckValue(value));
            Utils.scheduleUpdateService(moKeeCenter, value * 1000);
            return true;
        } else if (preference == mUpdateType) {
            final int value = Integer.valueOf((String) newValue);
            if (value == Constants.UPDATE_TYPE_NIGHTLY
                    || value == Constants.UPDATE_TYPE_EXPERIMENTAL
                    || value == Constants.UPDATE_TYPE_UNOFFICIAL
                    || value == Constants.UPDATE_TYPE_ALL) {
                int messageId = 0;
                switch (value) {
                    case 1:
                        messageId = R.string.nightly_alert;
                        break;
                    case 2:
                        messageId = R.string.experimenter_alert;
                        break;
                    case 3:
                        messageId = R.string.unofficial_alert;
                        break;
                    case 4:
                        messageId = R.string.all_alert;
                        break;
                }
                new AlertDialog.Builder(moKeeCenter)
                        .setTitle(R.string.alert_title)
                        .setMessage(messageId)
                        .setPositiveButton(getString(R.string.dialog_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        updateUpdatesType(value);
                                    }
                                })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
                return false;
            } else {
                updateUpdatesType(value);
            }
            return true;
        } else if (preference == mUpdateOTA) {
            boolean enabled = (Boolean) newValue;
            mPrefs.edit().putBoolean(Constants.OTA_CHECK_PREF, enabled).apply();
            isOTA(enabled);
            if (enabled) {
                updateUpdatesType(Utils.getUpdateType(MoKeeVersionType));
            }
            checkForUpdates();
            return true;
        } else if (preference == mVerifyROM) {
            boolean enabled = (Boolean) newValue;
            mPrefs.edit().putBoolean(Constants.VERIFY_ROM_PREF, enabled).apply();
            checkForUpdates();
            return true;
        }
        return false;
    }

    private void setAllTypeEntries() {
        String[] entries = getResources().getStringArray(
                R.array.update_all_entries);
        String[] entryValues = getResources().getStringArray(
                R.array.update_all_values);
        mUpdateType.setEntries(entries);
        mUpdateType.setEntryValues(entryValues);
    }

    private void setNormalTypeEntries() {
        String[] entries = getResources().getStringArray(
                R.array.update_normal_entries);
        String[] entryValues = getResources().getStringArray(
                R.array.update_normal_values);
        mUpdateType.setEntries(entries);
        mUpdateType.setEntryValues(entryValues);
    }

    private void setExperimentalTypeEntries() {
        String[] entries = getResources().getStringArray(
                R.array.update_experimental_entries);
        String[] entryValues = getResources().getStringArray(
                R.array.update_experimental_values);
        mUpdateType.setEntries(entries);
        mUpdateType.setEntryValues(entryValues);
    }

    private void setUnofficialTypeEntries() {
        String[] entries = getResources().getStringArray(
                R.array.update_unofficial_entries);
        String[] entryValues = getResources().getStringArray(
                R.array.update_unofficial_values);
        mUpdateType.setEntries(entries);
        mUpdateType.setEntryValues(entryValues);
    }

}
