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
 * 每个线程下载信息
 * 
 * @author wszfer
 */
public class ThreadDownLoadInfo {
    private int threadId;
    private long startPos;// 开始点
    private long endPos;// 结束点
    private long downSize;// 已下载数据
    private String url;

    public ThreadDownLoadInfo(int threadId, long startPos, long endPos, long downSize, String url) {
        this.threadId = threadId;
        this.startPos = startPos;
        this.endPos = endPos;
        this.downSize = downSize;
        this.url = url;
    }

    public ThreadDownLoadInfo() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public long getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public long getEndPos() {
        return endPos;
    }

    public void setEndPos(int endPos) {
        this.endPos = endPos;
    }

    public long getDownSize() {
        return downSize;
    }

    public void setDownSize(int downSize) {
        this.downSize = downSize;
    }

    @Override
    public String toString() {
        return "DownLoadInfo [threadId=" + threadId + ", startPos=" + startPos + ", endPos="
                + endPos + ", downSize=" + downSize + ", url=" + url + "]";
    }

}
