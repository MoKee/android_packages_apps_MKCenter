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

import java.io.File;
import java.io.Serializable;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

public class ItemInfo implements Parcelable, Serializable {

    private static final long serialVersionUID = 5499890003569313403L;

    private String mMd5Sum;
    private String mFileName;
    private String mFileSize;
    private String mDownloadUrl;
    private String mChangelogUrl;

    // extras
    private String mDescription;
    private String mCheckflag;

    private ItemInfo() {
        // Use the builder
    }

    private ItemInfo(Parcel in) {
        readFromParcel(in);
    }

    public String getChangelogUrl() {
        return mChangelogUrl;
    }

    public String getMd5Sum() {
        return mMd5Sum;
    }

    public String getFileName() {
        return mFileName;
    }

    public String getFileSize() {
        return mFileSize;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getCheckflag() {
        return mCheckflag;
    }

    public File getChangeLogFile(Context context) {
        return new File(context.getCacheDir(), mFileName + ".html");
    }

    public static final Parcelable.Creator<ItemInfo> CREATOR = new Parcelable.Creator<ItemInfo>() {
        public ItemInfo createFromParcel(Parcel in) {
            return new ItemInfo(in);
        }

        public ItemInfo[] newArray(int size) {
            return new ItemInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMd5Sum);
        dest.writeString(mFileName);
        dest.writeString(mFileSize);
        dest.writeString(mDownloadUrl);
        dest.writeString(mChangelogUrl);
        dest.writeString(mDescription);
        dest.writeString(mCheckflag);
    }

    private void readFromParcel(Parcel in) {
        mMd5Sum = in.readString();
        mFileName = in.readString();
        mFileSize = in.readString();
        mDownloadUrl = in.readString();
        mChangelogUrl = in.readString();
        mDescription = in.readString();
        mCheckflag = in.readString();
    }

    public static class Builder {
        private String mChangelogUrl;
        private String mMd5Sum;
        private String mFileName;
        private String mFileSize;
        private String mDownloadUrl;

        // Extras
        private String mDescription;
        private String mCheckflag;

        public Builder setChangelog(String changelogUrl) {
            mChangelogUrl = changelogUrl;
            return this;
        }

        public Builder setMD5Sum(String md5Sum) {
            mMd5Sum = md5Sum;
            return this;
        }

        public Builder setFileName(String fileName) {
            mFileName = fileName;
            return this;
        }

        public Builder setFileSize(String fileSize) {
            mFileSize = fileSize;
            return this;
        }

        public Builder setDownloadUrl(String downloadUrl) {
            mDownloadUrl = downloadUrl;
            return this;
        }

        public Builder setDescription(String description) {
            mDescription = description;
            return this;
        }

        public Builder setCheckflag(String checkflag) {
            mCheckflag = checkflag;
            return this;
        }

        public ItemInfo build() {
            ItemInfo info = new ItemInfo();
            info.mChangelogUrl = mChangelogUrl;
            info.mMd5Sum = mMd5Sum;
            info.mFileName = mFileName;
            info.mFileSize = mFileSize;
            info.mDownloadUrl = mDownloadUrl;
            info.mDescription = mDescription;
            info.mCheckflag = mCheckflag;
            return info;
        }
    }
}
