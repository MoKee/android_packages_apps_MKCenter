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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mokee.center.misc.Constants;

public class DBManager extends SQLiteOpenHelper {

    public DBManager(Context context) {
        super(context, "download.db", null, Constants.INTENT_FLAG_GET_UPDATE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS thread_info(_id integer PRIMARY KEY AUTOINCREMENT, thread_id integer, "
                + "start_pos long, end_pos long, down_size long,url text)");
        db.execSQL("CREATE TABLE IF NOT EXISTS download_info(_id integer PRIMARY KEY AUTOINCREMENT, down_id integer, "
                + "url text,local_file text,file_name text,file_size long,state integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
