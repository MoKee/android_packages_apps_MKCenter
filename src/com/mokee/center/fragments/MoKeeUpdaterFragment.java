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

package com.mokee.center.fragments;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import com.mokee.center.MKCenterApplication;
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
import com.mokee.center.requests.RankingRequest;
import com.mokee.center.service.DownLoadService;
import com.mokee.center.service.UpdateCheckService;
import com.mokee.center.utils.DownLoader;
import com.mokee.center.utils.UpdateFilter;
import com.mokee.center.utils.Utils;
import com.mokee.center.widget.AdmobPreference;
import com.mokee.center.widget.AppofferPreference;
import com.mokee.center.widget.EmptyListPreferenceStyle;
import com.mokee.center.widget.ItemPreference;

import mokee.support.widget.snackbar.Snackbar;
import mokee.support.widget.snackbar.SnackbarManager;
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
import android.mokee.utils.MoKeeUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import cn.waps.AppConnect;

import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.mokee.os.Build;

public class MoKeeUpdaterFragment extends PreferenceFragment implements OnPreferenceChangeListener,
        ItemPreference.OnReadyListener, ItemPreference.OnActionListener {

    private static Activity mContext;
    private static String TAG = "MoKeeUpdaterFragment";

    private static final String KEY_MOKEE_VERSION = "mokee_version";
    private static final String KEY_MOKEE_UNIQUE_ID = "mokee_unique_id";
    private static final String KEY_MOKEE_DONATE_INFO = "mokee_donate_info";
    private static final String KEY_MOKEE_VERSION_TYPE = "mokee_version_type";
    private static final String KEY_MOKEE_LAST_CHECK = "mokee_last_check";

    private static final String KEY_DONATE_TOTAL = "donate_total";
    private static final String KEY_DONATE_RANK = "donate_rank";
    private static final String KEY_DONATE_AMOUNT = "donate_amount";

    private boolean mDownloading = false;
    private long mDownloadId;
    private String mFileName;
    private static String updateTypeString, MoKeeVersionType, MoKeeVersionTypeString;

    private boolean mStartUpdateVisible = false;

    private static final String UPDATES_CATEGORY = "updates_category";

    public static final String EXPERIMENTAL_SHOW = "experimental_show";
    private static final int TAPS_TO_BE_A_EXPERIMENTER = 7;
    private int mExpHitCountdown;

    private static final int MENU_REFRESH = 0;
    private static final int MENU_DELETE_ALL = 1;
    private static final int MENU_DONATE = 2;
    private static final int MENU_REMOVE_ADS = 3;

    private static SharedPreferences mPrefs;
    private AdmobPreference mAdmobView;
    private AppofferPreference mAppofferView;
    private PreferenceScreen mRootView;
    private static SwitchPreference mUpdateOTA;
    private ListPreference mUpdateCheck;
    private ListPreference mUpdateType;
    private PreferenceCategory mUpdatesList;
    private ItemPreference mDownloadingPreference;
    private File mUpdateFolder;
    private ProgressDialog mProgressDialog;
    private Handler mUpdateHandler = new Handler();

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
                        SnackbarManager.show(Snackbar.with(mContext).text(R.string.no_updates_found).colorResource(R.color.snackbar_background));
                    } else if (count < 0) {
                        SnackbarManager.show(Snackbar.with(mContext).text(R.string.update_check_failed)
                                .duration(Snackbar.SnackbarDuration.LENGTH_LONG).colorResource(R.color.snackbar_background));
                    }
                }
                updateLayout();
            } else if (MoKeeCenter.BR_ONNewIntent.equals(action)) {
                // 唤醒
                if (intent.getBooleanExtra(UpdateCheckService.EXTRA_UPDATE_LIST_UPDATED, false)) {
                    updateLayout();
                }
                checkForDownloadCompleted(intent);
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (mAdmobView != null) {
            mAdmobView.onAdPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdmobView != null) {
            mAdmobView.onAdDestory();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        // Load the layouts
        addPreferencesFromResource(R.xml.mokee_updater);

        // Load the stored preference data
        mPrefs = mContext.getSharedPreferences(Constants.DOWNLOADER_PREF, 0);

        mRootView = (PreferenceScreen) findPreference(Constants.ROOT_PREF);
        mAppofferView = (AppofferPreference) findPreference(Constants.APPOFFER_PREF);
        mAdmobView = (AdmobPreference) findPreference(Constants.ADMOB_PREF);
        if (!Utils.checkLicensed(mContext)) {
            if (!MoKeeUtils.isSupportLanguage(false)) {
                mAdmobView.onAdCreate();
            }
        }
        mUpdatesList = (PreferenceCategory) findPreference(UPDATES_CATEGORY);
        mUpdateCheck = (ListPreference) findPreference(Constants.UPDATE_INTERVAL_PREF);
        mUpdateType = (ListPreference) findPreference(Constants.UPDATE_TYPE_PREF);
        mUpdateOTA = (SwitchPreference) findPreference(Constants.OTA_CHECK_PREF);// OTA更新

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
            int check = mPrefs.getInt(Constants.UPDATE_INTERVAL_PREF, Constants.UPDATE_FREQ_DAILY);
            mUpdateCheck.setValue(String.valueOf(check));
            mUpdateCheck.setSummary(mapCheckValue(check));
            mUpdateCheck.setOnPreferenceChangeListener(this);
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

        MoKeeVersionTypeString = Utils.getReleaseVersionTypeString(mContext, MoKeeVersionType);
        Utils.setSummaryFromString(this, KEY_MOKEE_VERSION_TYPE, MoKeeVersionTypeString);

        if (!MoKeeVersionType.equals("history")) {
            refreshOTAOption();
            mUpdateOTA.setChecked(mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false));
            mUpdateOTA.setOnPreferenceChangeListener(this);
            isOTA(mUpdateOTA.isChecked());
        } else {
            getPreferenceScreen().removePreference(mUpdateOTA);
        }

        setSummaryFromProperty(KEY_MOKEE_VERSION, "ro.mk.version");
        Utils.setSummaryFromString(this, KEY_MOKEE_UNIQUE_ID, Build.getUniqueID(mContext));

        updateLastCheckPreference();

        setHasOptionsMenu(true);

        float paid = Utils.getPaidTotal(mContext);
        if (paid < Constants.DONATION_REQUEST && MoKeeUtils.isApkInstalledAndEnabled("de.robv.android.xposed.installer", mContext)) {
            SnackbarManager.show(Snackbar.with(mContext).text(R.string.installed_xposed_toast)
                    .duration(10000L).colorResource(R.color.snackbar_background));
        }
        if (paid > 0f) {
            getRanking();
        }
    }

    public static void getRanking() {
        if (MoKeeUtils.isOnline(mContext)) {
            RankingRequest rankingRequest = new RankingRequest(Request.Method.POST,
                    URI.create(mContext.getString(R.string.conf_get_ranking_server_url_def)).toASCIIString(), Utils.getUserAgentString(mContext), new Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                if (jsonObject.has("total")) {
                                    int total = Integer.valueOf(jsonObject.get("total").toString());
                                    int rank = Integer.valueOf(jsonObject.get("rank").toString());
                                    float amount = Float.valueOf(jsonObject.get("amount").toString());
                                    mPrefs.edit().putInt(KEY_DONATE_TOTAL, total)
                                        .putInt(KEY_DONATE_RANK, rank)
                                        .putFloat(KEY_DONATE_AMOUNT, amount).apply();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.e("Error: ", error.getMessage());
                    VolleyLog.e("Error type: " + error.toString());
                }
            });
            rankingRequest.setTag(TAG);
            ((MKCenterApplication) mContext.getApplicationContext()).getQueue().add(rankingRequest);
        }
    }

    private void setDonatePreference() {
        Float paid = Utils.getPaidTotal(mContext);
        Float amount = mPrefs.getFloat(KEY_DONATE_AMOUNT, 0f);
        if (amount < paid) {
            amount = paid;
        }
        int rank = mPrefs.getInt(KEY_DONATE_RANK, 0);
        int total = mPrefs.getInt(KEY_DONATE_TOTAL, 0);
        if (paid == 0f) {
            Utils.setSummaryFromString(this, KEY_MOKEE_DONATE_INFO, getString(R.string.donate_money_info_null));
        } else if (total != 0) {
            Utils.setSummaryFromString(this, KEY_MOKEE_DONATE_INFO, String.format(mContext.getString(R.string.donate_money_info_with_rank),
                    amount.intValue(), String.valueOf((int)(((double)(total - rank) / total) * 100)) + "%", rank));
        } else {
            Utils.setSummaryFromString(this, KEY_MOKEE_DONATE_INFO, String.format(mContext.getString(R.string.donate_money_info),
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
            mAdmobView.onAdDestory();
            mRootView.removePreference(mAdmobView);
            mAdmobView = null;
        }
    }

    private void removeAppofferPreference() {
        if (mAppofferView != null) {
            mRootView.removePreference(mAppofferView);
            mAppofferView = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mExpHitCountdown = mPrefs.getBoolean(EXPERIMENTAL_SHOW,
                TextUtils.equals(Utils.getReleaseVersionType(), "experimental")) ? -1 : TAPS_TO_BE_A_EXPERIMENTER;
        // Remove Ad
        if (Utils.checkLicensed(mContext)) {
            removeAdmobPreference();
            removeAppofferPreference();
        } else {
            if (MoKeeUtils.isSupportLanguage(false)) {
                removeAdmobPreference();
                if (mAppofferView != null) {
                    mAppofferView.onAdResume(getActivity());
                }
                if (AppConnect.getInstance(mContext).hasPopAd(mContext)) {
                    AppConnect.getInstance(mContext).setPopAdBack(true);
                    AppConnect.getInstance(mContext).showPopAd(mContext);
                }
            } else {
                removeAppofferPreference();
                if (mAdmobView != null) {
                    mAdmobView.onAdResume();
                }
            }
        }
        setDonatePreference();
    }

    public static void refreshOption() {
        mContext.invalidateOptionsMenu();
        refreshOTAOption();
    }

    public static void refreshOTAOption() {
        Float currentPaid = Utils.getPaidTotal(mContext);
        if (currentPaid < Constants.DONATION_REQUEST) {
            mPrefs.edit().putBoolean(Constants.OTA_CHECK_PREF, false).apply();
            mUpdateOTA.setEnabled(false);
            if (currentPaid == 0f) {
                mUpdateOTA.setSummary(String.format(mContext.getString(R.string.pref_ota_check_donation_request_summary),
                        Float.valueOf(Constants.DONATION_REQUEST - currentPaid).intValue()));
            } else {
                mUpdateOTA.setSummary(String.format(mContext.getString(R.string.pref_ota_check_donation_request_pending_summary),
                        currentPaid.intValue(), Float.valueOf(Constants.DONATION_REQUEST - currentPaid.intValue()).intValue()));
            }
        } else {
            if (!MoKeeVersionTypeString.equals(updateTypeString)) {
                mPrefs.edit().putBoolean(Constants.OTA_CHECK_PREF, false).apply();
            }
            mUpdateOTA.setEnabled(true);
            mUpdateOTA.setSummary(R.string.pref_ota_check_summary);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(KEY_MOKEE_VERSION_TYPE)) {
            // Don't enable experimental option for secondary users.
            if (UserHandle.myUserId() != UserHandle.USER_OWNER)
                return true;

            if (mExpHitCountdown > 0) {
                mExpHitCountdown--;
                if (mExpHitCountdown == 0) {
                    mPrefs.edit().putBoolean(EXPERIMENTAL_SHOW, true).apply();
                    SnackbarManager.show(Snackbar.with(mContext).text(R.string.show_exp_on)
                            .duration(Snackbar.SnackbarDuration.LENGTH_LONG).colorResource(R.color.snackbar_background));
                    String MoKeeVersionType = Utils.getReleaseVersionType();
                    boolean isUnofficial = TextUtils.equals(MoKeeVersionType, "unofficial");
                    if (!isUnofficial) {
                        setExperimentalTypeEntries();
                    } else {
                        setAllTypeEntries();
                    }
                } else if (mExpHitCountdown > 0
                        && mExpHitCountdown < (TAPS_TO_BE_A_EXPERIMENTER - 2)) {
                    SnackbarManager.show(Snackbar.with(mContext).text(getResources()
                            .getQuantityString(R.plurals.show_exp_countdown, mExpHitCountdown, mExpHitCountdown))
                            .colorResource(R.color.snackbar_background));
                }
            } else if (mExpHitCountdown < 0) {
                SnackbarManager.show(Snackbar.with(mContext).text(R.string.show_exp_already)
                        .duration(Snackbar.SnackbarDuration.LENGTH_LONG).colorResource(R.color.snackbar_background));
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void updateLastCheckPreference() {
        long lastCheckTime = mPrefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0);
        if (lastCheckTime == 0) {
            Utils.setSummaryFromString(this, KEY_MOKEE_LAST_CHECK,
                    getString(R.string.mokee_last_check_never));
        } else {
            Date lastCheck = new Date(lastCheckTime);
            String date = DateFormat.getLongDateFormat(mContext).format(lastCheck);
            String time = DateFormat.getTimeFormat(mContext).format(lastCheck);
            Utils.setSummaryFromString(this, KEY_MOKEE_LAST_CHECK, date + " " + time);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!Utils.checkLicensed(mContext)) {
            menu.add(0, MENU_REMOVE_ADS, 0, R.string.menu_remove_ads).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        menu.add(0, MENU_DONATE, 0, R.string.menu_donate).setShowAsActionFlags(
                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh)
                .setIcon(R.drawable.ic_menu_refresh)
                .setShowAsActionFlags(
                        MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all).setShowAsActionFlags(
                MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DONATE:
                MoKeeCenter.donateOrRemoveAdsButton(getActivity(), true);
                return true;
            case MENU_REFRESH:
                checkForUpdates();
                return true;
            case MENU_DELETE_ALL:
                confirmDeleteAll();
                return true;
            case MENU_REMOVE_ADS:
                MoKeeCenter.donateOrRemoveAdsButton(getActivity(), false);
                return true;
        }
        return true;
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

    private void resetDownloadState() {
        mDownloadId = -1;
        mFileName = null;
        mDownloading = false;
        mDownloadingPreference = null;
    }

    private void updateLayout() {
        updateLastCheckPreference();
        // Read existing Updates
        LinkedList<String> existingFiles = new LinkedList<String>();
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
        Utils.cancelNotification(mContext);

        // Build list of updates
        final LinkedList<ItemInfo> availableUpdates = State.loadMKState(mContext, State.UPDATE_FILENAME);

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
            boolean isLocalFile = Utils.isLocaUpdateFile(ui.getFileName(), true);
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

            ItemPreference up = new ItemPreference(mContext, ui, style);
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
            EmptyListPreferenceStyle pref = new EmptyListPreferenceStyle(mContext, null, R.style.EmptyListPreferenceStyle);
            pref.setSummary(mUpdateOTA.isChecked() ? R.string.no_available_ota_intro : R.string.no_available_updates_intro);
            pref.setEnabled(false);
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
        State.saveMKState(mContext, new LinkedList<ItemInfo>(), State.UPDATE_FILENAME);// clear
        refreshPreferences(new LinkedList<ItemInfo>());// clear
        // If there is no internet connection, display a message and return.
        if (!MoKeeUtils.isOnline(mContext)) {
            SnackbarManager.show(Snackbar.with(mContext).text(R.string.data_connection_required).colorResource(R.color.snackbar_background));
            return;
        }
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setTitle(R.string.mokee_updater_title);
        mProgressDialog.setMessage(getString(R.string.checking_for_updates));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Intent cancelIntent = new Intent(mContext, UpdateCheckService.class);
                cancelIntent.setAction(UpdateCheckService.ACTION_CANCEL_CHECK);
                mContext.startServiceAsUser(cancelIntent, UserHandle.CURRENT);
                mProgressDialog = null;
                if (mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false)) {
                    mPrefs.edit().putBoolean(Constants.OTA_CHECK_MANUAL_PREF, false).apply();
                }
            }
        });

        if (mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false)) {
            mPrefs.edit().putBoolean(Constants.OTA_CHECK_MANUAL_PREF, mPrefs.getBoolean(Constants.OTA_CHECK_PREF, false)).apply();
        }
        Intent checkIntent = new Intent(mContext, UpdateCheckService.class);
        checkIntent.setAction(UpdateCheckService.ACTION_CHECK);
        mContext.startServiceAsUser(checkIntent, UserHandle.CURRENT);
        mProgressDialog.show();
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(mContext).setTitle(R.string.confirm_delete_dialog_title)
                .setMessage(R.string.confirm_delete_updates_all_dialog_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // We are OK to delete, trigger it
                        onPauseDownload(mPrefs);
                        deleteOldUpdates();
                        updateLayout();
                    }
                }).setNegativeButton(R.string.dialog_cancel, null).show();
    }

    private boolean deleteOldUpdates() {
        boolean success;
        // mUpdateFolder: Foldername with fullpath of SDCARD
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
            Utils.deleteDir(mUpdateFolder);
            mUpdateFolder.mkdir();
            success = true;
            SnackbarManager.show(Snackbar.with(mContext).text(R.string.delete_updates_success_message).colorResource(R.color.snackbar_background));
        } else if (!mUpdateFolder.exists()) {
            success = false;
            SnackbarManager.show(Snackbar.with(mContext).text(R.string.delete_updates_nofolder_message).colorResource(R.color.snackbar_background));
        } else {
            success = false;
            SnackbarManager.show(Snackbar.with(mContext).text(R.string.delete_updates_failure_message).colorResource(R.color.snackbar_background));
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
        mContext.registerReceiver(mReceiver, filter);

        checkForDownloadCompleted(mContext.getIntent());
        mContext.setIntent(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        mUpdateHandler.removeCallbacks(mUpdateProgress);
        mContext.unregisterReceiver(mReceiver);
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
    }

    @Override
    public void onStartDownload(ItemPreference pref) {
        // If there is no internet connection, display a message and return.
        if (!MoKeeUtils.isOnline(mContext)) {
            SnackbarManager.show(Snackbar.with(mContext).text(R.string.data_connection_required).colorResource(R.color.snackbar_background));
            return;
        }
        if (mDownloading) {
            SnackbarManager.show(Snackbar.with(mContext).text(R.string.download_already_running)
                    .duration(Snackbar.SnackbarDuration.LENGTH_LONG).colorResource(R.color.snackbar_background));
            return;
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
        Intent intent = new Intent(mContext, DownloadReceiver.class);
        intent.setAction(DownloadReceiver.ACTION_DOWNLOAD_START);
        intent.putExtra(DownloadReceiver.EXTRA_UPDATE_INFO, (Parcelable) ui);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
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
        new AlertDialog.Builder(mContext)
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
                }).setNegativeButton(R.string.dialog_cancel, null).show();
    }

    public void onPauseDownload(SharedPreferences prefs) {
        // We are OK to stop download, trigger it
        if (mDownloading)
        SnackbarManager.show(Snackbar.with(mContext).text(R.string.download_cancelled).colorResource(R.color.snackbar_background));
        resetDownloadState();
        mUpdateHandler.removeCallbacks(mUpdateProgress);
        Intent intent = new Intent(mContext, DownLoadService.class);
        intent.setAction(DownLoadService.ACTION_DOWNLOAD);
        intent.putExtra(DownLoadService.DOWNLOAD_TYPE, DownLoadService.PAUSE);
        intent.putExtra(DownLoadService.DOWNLOAD_URL, mPrefs.getString(DownLoadService.DOWNLOAD_URL, ""));

        mContext.startServiceAsUser(intent, UserHandle.CURRENT);
    }

    @Override
    public void onStartUpdate(ItemPreference pref) {
        final ItemInfo itemInfo = pref.getItemInfo();

        // Prevent the dialog from being triggered more than once
        if (mStartUpdateVisible) {
            return;
        }
        mStartUpdateVisible = true;
        // Get the message body right
        String dialogBody = getString(itemInfo.getFileName().startsWith("OTA")? R.string.apply_update_ota_dialog_text : R.string.apply_update_dialog_text, itemInfo.getFileName());
        // Display the dialog
        new AlertDialog.Builder(mContext).setTitle(R.string.apply_update_dialog_title)
                .setMessage(dialogBody)
                .setPositiveButton(R.string.dialog_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Utils.triggerUpdate(mContext, itemInfo.getFileName(), true);
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to reboot into recovery mode", e);
                            SnackbarManager.show(Snackbar.with(mContext).text(R.string.apply_unable_to_reboot_toast).colorResource(R.color.snackbar_background));
                        }
                    }
                }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mStartUpdateVisible = false;
                    }
                }).setCancelable(false).show();
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
            SnackbarManager.show(Snackbar.with(mContext).text(message).colorResource(R.color.snackbar_background));
        } else if (!mUpdateFolder.exists()) {
            SnackbarManager.show(Snackbar.with(mContext).text(R.string.delete_updates_nofolder_message).colorResource(R.color.snackbar_background));
        } else {
            SnackbarManager.show(Snackbar.with(mContext).text(R.string.delete_updates_failure_message).colorResource(R.color.snackbar_background));
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
            Utils.scheduleUpdateService(mContext, value * 1000);
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
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.alert_title)
                        .setMessage(messageId)
                        .setPositiveButton(getString(R.string.dialog_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        updateUpdatesType(value);
                                    }
                                }).setNegativeButton(R.string.dialog_cancel, null).show();
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
        }
        return false;
    }

    private void setAllTypeEntries() {
        String[] entries = mContext.getResources().getStringArray(
                R.array.update_all_entries);
        String[] entryValues = mContext.getResources().getStringArray(
                R.array.update_all_values);
        mUpdateType.setEntries(entries);
        mUpdateType.setEntryValues(entryValues);
    }

    private void setNormalTypeEntries() {
        String[] entries = mContext.getResources().getStringArray(
                R.array.update_normal_entries);
        String[] entryValues = mContext.getResources().getStringArray(
                R.array.update_normal_values);
        mUpdateType.setEntries(entries);
        mUpdateType.setEntryValues(entryValues);
    }

    private void setExperimentalTypeEntries() {
        String[] entries = mContext.getResources().getStringArray(
                R.array.update_experimental_entries);
        String[] entryValues = mContext.getResources().getStringArray(
                R.array.update_experimental_values);
        mUpdateType.setEntries(entries);
        mUpdateType.setEntryValues(entryValues);
    }

    private void setUnofficialTypeEntries() {
        String[] entries = mContext.getResources().getStringArray(
                R.array.update_unofficial_entries);
        String[] entryValues = mContext.getResources().getStringArray(
                R.array.update_unofficial_values);
        mUpdateType.setEntries(entries);
        mUpdateType.setEntryValues(entryValues);
    }

}
