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

/**
 * 整体下载信息
 * 
 * @author wszfer
 */
public class DownLoadInfo {
    public long fileSize;// 文件大小
    private long complete;// 已下载长度
    private String url;// 下载器标识
    private String downID;
    private String localFile;
    private String fileName;
    private int state;

    public DownLoadInfo(long fileSize, long complete, String url) {
        this.fileSize = fileSize;
        this.complete = complete;
        this.url = url;
    }

    public DownLoadInfo(String url, String downID, String localFile, String fileName,
            long fileSize, int state) {
        super();
        this.url = url;
        this.downID = downID;
        this.localFile = localFile;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.state = state;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public long getComplete() {
        return complete;
    }

    public void setComplete(int complete) {
        this.complete = complete;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDownID() {
        return downID;
    }

    public void setDownID(String downID) {
        this.downID = downID;
    }

    public String getLocalFile() {
        return localFile;
    }

    public void setLocalFile(String localFile) {
        this.localFile = localFile;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "DownLoadInfo [fileSize=" + fileSize + ", complete=" + complete + ", url=" + url
                + ", downID=" + downID + ", localFile=" + localFile + ", fileName=" + fileName
                + ", state=" + state + "]";
    }

}
