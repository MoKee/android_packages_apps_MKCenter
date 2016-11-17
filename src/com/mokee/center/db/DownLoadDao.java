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

package com.mokee.center.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mokee.center.MKCenterApplication;
import com.mokee.center.misc.DownLoadInfo;

/**
 * 下载记录
 *
 * @author wszfer
 */
public class DownLoadDao {
    private static DownLoadDao downLoadDao = null;
    private Context context;

    private DownLoadDao(Context context) {
        this.context = context;
    }

    public static synchronized DownLoadDao getInstance() {
        if (downLoadDao == null) {
            downLoadDao = new DownLoadDao(MKCenterApplication.getContext());
        }
        return downLoadDao;
    }

    public SQLiteDatabase getConnection() {
        SQLiteDatabase sqliteDatabase = null;
        try {
            sqliteDatabase = new DBManager(context).getWritableDatabase();
        } catch (Exception e) {
        }
        return sqliteDatabase;
    }

    /**
     * 查看数据库中是否有数据
     */
    public synchronized boolean isHasInfos(String fileUrl) {
        SQLiteDatabase database = getConnection();
        int count = -1;
        Cursor cursor = null;
        try {
            String sql = "select count(*)  from download_info where url=?";
            cursor = database.rawQuery(sql, new String[] {
                    fileUrl
            });
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
            if (null != cursor) {
                cursor.close();
            }
        }
        return count > 0;
    }

    /**
     * 添加下载信息
     *
     * @param dli
     */
    public synchronized void saveInfo(DownLoadInfo dli) {
        SQLiteDatabase database = getConnection();
        try {
            String sql = "insert into download_info(url, down_id, local_file, file_name, file_size) values (?,?,?,?,?)";
            Object[] bindArgs = {
                    dli.getUrl(), dli.getDownID(), dli.getLocalFile(),
                    dli.getFileName(), dli.getFileSize()
            };
            database.execSQL(sql, bindArgs);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
        }
    }

    /**
     * 获取下载信息
     *
     * @param downID
     * @return
     */
    public synchronized DownLoadInfo getDownLoadInfo(String downID) {
        SQLiteDatabase database = getConnection();
        Cursor cursor = null;
        DownLoadInfo dli = null;
        try {
            String sql = "select url, local_file, file_name, file_size, state from download_info where down_id=?";
            cursor = database.rawQuery(sql, new String[] {
                    downID
            });
            while (cursor.moveToNext()) {
                dli = new DownLoadInfo(cursor.getString(0), downID,
                        cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                        cursor.getInt(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
            if (null != cursor) {
                cursor.close();
            }
        }
        return dli;
    }

    /**
     * getFileName
     * @param fileName
     * @return
     */
    public synchronized DownLoadInfo getDownLoadInfoByName(String fileName) {
        SQLiteDatabase database = getConnection();
        Cursor cursor = null;
        DownLoadInfo dli = null;
        try {
            String sql = "select url, down_id, local_file, file_name, file_size, state from download_info where file_name=?";
            cursor = database.rawQuery(sql, new String[] {
                    fileName
            });
            while (cursor.moveToNext()) {
                dli = new DownLoadInfo(cursor.getString(0), cursor.getString(0),
                        cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                        cursor.getInt(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
            if (null != cursor) {
                cursor.close();
            }
        }
        return dli;
    }

    /**
     * 获取下载信息
     *
     * @param fileUrl
     * @return
     */
    public synchronized DownLoadInfo getDownLoadInfoByUrl(String fileUrl) {
        SQLiteDatabase database = getConnection();
        Cursor cursor = null;
        DownLoadInfo dli = null;
        try {
            String sql = "select down_id, local_file, file_name, file_size, state from download_info where url=?";
            cursor = database.rawQuery(sql, new String[] {
                    fileUrl
            });
            while (cursor.moveToNext()) {
                dli = new DownLoadInfo(fileUrl, cursor.getString(0),
                        cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                        cursor.getInt(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
            if (null != cursor) {
                cursor.close();
            }
        }
        return dli;
    }

    /**
     * 更新状态
     *
     * @param fileUrl
     * @param state
     */
    public synchronized void updataState(String fileUrl, int state) {
        SQLiteDatabase database = getConnection();
        try {
            String sql = "update download_info set state=? where url=? ";
            Object[] bindArgs = {
                    state, fileUrl
            };
            database.execSQL(sql, bindArgs);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
        }
    }

    /**
     * 更新文件大小
     *
     * @param fileUrl
     * @param state
     */
    public synchronized void updataFileSize(String fileUrl, long fileSize) {
        SQLiteDatabase database = getConnection();
        try {
            String sql = "update download_info set file_size=? where url=? ";
            Object[] bindArgs = {
                    fileSize, fileUrl
            };
            database.execSQL(sql, bindArgs);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
        }
    }

    /**
     * 删除下载信息
     *
     * @param url
     */
    public synchronized void delete(String fileUrl) {
        SQLiteDatabase database = getConnection();
        try {
            database.delete("download_info", "url=?", new String[] {
                    fileUrl
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != database) {
                database.close();
            }
        }
    }
}
