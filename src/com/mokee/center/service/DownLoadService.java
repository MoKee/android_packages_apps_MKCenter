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

package com.mokee.center.service;

import java.util.HashMap;
import java.util.Map;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.format.DateUtils;
import android.util.SparseArray;

import com.mokee.center.R;
import com.mokee.center.db.DownLoadDao;
import com.mokee.center.misc.Constants;
import com.mokee.center.misc.DownLoadInfo;
import com.mokee.center.receiver.DownloadReceiver;
import com.mokee.center.utils.DownLoader;

public class DownLoadService extends NonStopIntentService {
    private static final String TAG = "DownLoadService";

    public DownLoadService() {
        super(TAG);
    }

    public static final String ACTION_DOWNLOAD = "download";
    public static final String DOWNLOAD_TYPE = "download_type";
    public static final String DOWNLOAD_URL = "download_url";
    public static final String DOWNLOAD_FILE_PATH = "download_filepath";
    public static final String DOWNLOAD_ID = "download_id";
    public static final String DOWNLOAD_MD5 = "download_md5";

    public static final int START = 2;
    public static final int PAUSE = 3;
    public static final int DELETE = 4;
    public static final int CONTINUE = 5;
    public static final int ADD = 6;
    public static final int STOP = 7;

    private static Map<String, DownLoader> downloaders = new HashMap<String, DownLoader>();
    private static SparseArray<Builder> notifications = new SparseArray<Builder>();// 通知队列
    private static int notificationID = Constants.INTENT_FLAG_GET_UPDATE;
    private NotificationManager manager;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent action) {
        if (action != null && ACTION_DOWNLOAD.equals(action.getAction())) {
            int type = action.getIntExtra(DOWNLOAD_TYPE, 6);
            String url = action.getStringExtra(DOWNLOAD_URL);
            String filePath = action.getStringExtra(DOWNLOAD_FILE_PATH);
            long download_id = action.getLongExtra(DOWNLOAD_ID, System.currentTimeMillis());
            DownLoader downloader = null;
            switch (type) {
                case ADD:
                    notificationID = Constants.INTENT_FLAG_GET_UPDATE;
                    downloader = downloaders.get(url);
                    if (downloader == null) {
                        downloader = new DownLoader(url, filePath, handler,
                                System.currentTimeMillis(), this);
                        downloaders.put(url, downloader);
                        if (!DownLoadDao.getInstance().isHasInfos(url)) {
                            // init
                            DownLoadDao.getInstance().saveInfo(
                                    new DownLoadInfo(url, String.valueOf(download_id),
                                            filePath, filePath.substring(filePath.lastIndexOf("/") + 1,
                                                    filePath.length()), 0, DownLoader.STATUS_PENDING));
                        }
                    }
                    if (downloader.isDownLoading())
                        return;
                    DownLoadDao.getInstance().updataState(url, DownLoader.STATUS_DOWNLOADING);
                    DownLoadInfo loadInfo = downloader.getDownLoadInfo();
                    if (loadInfo != null) {
                        // 开始下载
                        downloader.start();
                        if (notifications.get(downloader.getNotificationID()) == null) {
                            addNotification(notificationID, R.string.mokee_updater_title);
                            downloader.setNotificationID(notificationID);
                        }
                    }
                    break;
                case PAUSE:
                    downloader = downloaders.get(url);
                    if (downloader != null) {
                        downloader.pause();
                        downloaders.remove(url);
                        if (downloaders.size() == 0) {
                            stopSelf();
                        }
                    }
                    // Clear the stored data from shared preferences
                    mPrefs = getSharedPreferences(Constants.DOWNLOADER_PREF, 0);
                    mPrefs.edit().remove(DownLoadService.DOWNLOAD_ID).remove(DownLoadService.DOWNLOAD_MD5)
                            .remove(DownLoadService.DOWNLOAD_URL).apply();
                    break;
            }
        }
    }

    /**
     * 添加通知
     */
    private void addNotification(int id, int title) {
        Builder builder = new Builder(this);
        builder.setContentTitle(getString(title));
        builder.setContentText(getString(R.string.download_running));
        builder.setColor(getResources().getColor(com.android.internal.R.color.system_notification_accent_color));
        builder.setSmallIcon(android.R.drawable.stat_sys_download);
        /* 设置点击消息时，显示的界面 */
        Intent nextIntent = new Intent(DownloadReceiver.ACTION_NOTIFICATION_CLICKED);
        PendingIntent pengdingIntent = PendingIntent.getBroadcast(this, 0, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pengdingIntent);
        builder.setProgress(100, 0, false);
        builder.setAutoCancel(true);
        builder.setTicker(getString(title));
        builder.setOngoing(true);
        notifications.put(id, builder);
        // Notification not = builder.build();
        // not.flags = Notification.FLAG_NO_CLEAR;
        manager.notify(id, builder.build());
    }

    /**
     * 定时更新通知进度
     */
    private void updateNotification(int id, int progress, long time) {
        if (notifications.get(id) == null) {
            return;
        }
        Builder notification = notifications.get(id);
        notification.setContentText(getString(R.string.download_remaining, DateUtils.formatDuration(time)));
        notification.setContentInfo(String.valueOf(progress) + "%");
        notification.setProgress(100, progress, false);
        manager.notify(id, notification.build());
    }

    public Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            DownLoader di;
            String url;
            Intent intent;
            DownLoadInfo dli;
            switch (msg.what) {
                case DownLoader.STATUS_DOWNLOADING:
                case DownLoader.STATUS_PENDING: // 更新通知
                    url = (String) msg.obj;
                    di = downloaders.get(url);
                    long time = 0;
                    if (di != null) {
                        try {
                            long allDownSize;
                            if (di.downloadedSize != 0) {// 除去已緩存
                                allDownSize = di.allDownSize - di.downloadedSize;
                            } else {
                                allDownSize = di.allDownSize;
                            }
                            long endtDown = System.currentTimeMillis() - di.getStartDown();// 时间
                            long surplusSize = di.getFileSize() - di.allDownSize;

                            if (surplusSize > 0 && allDownSize > 0) {
                                long speed = (allDownSize / endtDown);
                                if (speed > 0)
                                {
                                    time = (surplusSize / speed);
                                }
                            }
                            if (di.allDownSize > 0 && di.getFileSize() > 0) {
                                updateNotification(msg.arg2, Integer.valueOf(String.valueOf(di.allDownSize * 100
                                        / di.getFileSize())), time);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case DownLoader.STATUS_ERROR:
                case DownLoader.STATUS_DELETE:
                    di = (DownLoader) msg.obj;
                    url = di.fileUrl;
                    if (di != null) {
                        manager.cancel(di.getNotificationID());
                        notifications.remove(di.getNotificationID());
                        downloaders.remove(url);
                    }
                    DownLoadDao.getInstance().updataState(url, msg.what);
                    dli = DownLoadDao.getInstance().getDownLoadInfoByUrl(url);
                    if (dli != null) {
                        intent = new Intent();
                        intent.setAction(DownloadReceiver.ACTION_DOWNLOAD_COMPLETE);
                        intent.putExtra(DOWNLOAD_ID, Long.valueOf(dli.getDownID()));
	                    sendBroadcastAsUser(intent, UserHandle.CURRENT);
                    }
                    if (downloaders.size() == 0) {
                        stopSelf();
                    }
                    break;
                case DownLoader.STATUS_COMPLETE:
                    di = (DownLoader) msg.obj;
                    url = di.fileUrl;
                    if (notifications.get(di.getNotificationID()) != null) {
                        manager.cancel(di.getNotificationID());
                        notifications.remove(di.getNotificationID());
                        downloaders.remove(url);
                    }
                    DownLoadDao.getInstance().updataState(url, msg.what);
                    dli = DownLoadDao.getInstance().getDownLoadInfoByUrl(url);
                    intent = new Intent();
                    intent.setAction(DownloadReceiver.ACTION_DOWNLOAD_COMPLETE);
                    intent.putExtra(DOWNLOAD_ID, Long.valueOf(dli.getDownID()));
                    sendBroadcastAsUser(intent, UserHandle.CURRENT);
                    if (downloaders.size() == 0) {
                        stopSelf();
                    }
                    break;
            }
            return false;
        }
    });

    @Override
    public void onDestroy() {
        super.onDestroy();
        manager.cancel(notificationID);
        notifications.remove(notificationID);
    }

}