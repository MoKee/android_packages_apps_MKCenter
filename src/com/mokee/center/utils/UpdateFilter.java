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

package com.mokee.center.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filename Filter for getting only Files that matches the Given Extensions
 * Extensions can be split with | Example: .zip|.md5sum
 */
public class UpdateFilter implements FilenameFilter {
    private final String[] mExtension;

    public UpdateFilter(String extensions) {
        mExtension = extensions.split("\\|");
    }

    public boolean accept(File dir, String name) {
        for (String extension : mExtension) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
