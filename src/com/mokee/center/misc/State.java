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

package com.mokee.center.misc;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

public class State {

    private static final String TAG = "State";
    public static final String UPDATE_FILENAME = "mkupdater.state";
    public static final String EXTRAS_FILENAME = "mkextras.state";

    public static void saveMKState(Context context, LinkedList<ItemInfo> availableUpdates,
            String fileName) {
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            File f = new File(context.getCacheDir(), fileName);
            fos = new FileOutputStream(f);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(availableUpdates);
            oos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Exception on saving instance state", e);
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                // ignored, can't do anything anyway
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static LinkedList<ItemInfo> loadMKState(Context context, String fileName) {
        LinkedList<ItemInfo> availableUpdates = new LinkedList<ItemInfo>();
        ObjectInputStream ois = null;
        FileInputStream fis = null;
        try {
            File f = new File(context.getCacheDir(), fileName);
            fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);

            Object o = ois.readObject();
            if (o != null && o instanceof LinkedList<?>) {
                availableUpdates = (LinkedList<ItemInfo>) o;
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to load stored class", e);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Unexpected state file format", e);
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No state info stored");
        } catch (IOException e) {
            Log.e(TAG, "Exception on loading state", e);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                // ignored, can't do anything anyway
            }
        }
        return availableUpdates;
    }
}
