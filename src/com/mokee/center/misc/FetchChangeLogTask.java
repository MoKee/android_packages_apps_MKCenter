/*
 * Copyright (C) 2014 The MoKee OpenSource Project
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.RequestFuture;

import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.requests.ChangeLogRequest;
import com.mokee.center.utils.Utils;
import com.mokee.center.widget.NotifyingWebView;

public class FetchChangeLogTask extends AsyncTask<ItemInfo, Void, Void>
        implements DialogInterface.OnDismissListener {
    private static final String TAG = "FetchChangeLogTask";

    private Context mContext;
    private ItemInfo mInfo;
    private NotifyingWebView mChangeLogView;
    private AlertDialog mAlertDialog;

    public FetchChangeLogTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(ItemInfo... infos) {
        mInfo = infos[0];

        if (mInfo != null) {
            File changeLog = mInfo.getChangeLogFile(mContext);
            if (!changeLog.exists()) {
                fetchChangeLog(mInfo);
            }
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.change_log_dialog, null);
        final View progressContainer = view.findViewById(R.id.progress);
        mChangeLogView =
                (NotifyingWebView) view.findViewById(R.id.changelog);

        mChangeLogView.setOnInitialContentReadyListener(
                new NotifyingWebView.OnInitialContentReadyListener() {
                    @Override
                    public void onInitialContentReady(WebView webView) {
                        progressContainer.setVisibility(View.GONE);
                        mChangeLogView.setVisibility(View.VISIBLE);
                    }
                });

        mChangeLogView.getSettings().setTextZoom(80);
        mChangeLogView.getSettings().setDefaultTextEncodingName("UTF-8");
        mChangeLogView.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.white));

        // Prepare the dialog box
        mAlertDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.changelog_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_close, null)
                .create();
        mAlertDialog.setOnDismissListener(this);
        mAlertDialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        File changeLog = mInfo.getChangeLogFile(mContext);

        if (changeLog.length() == 0) {
            // Change log is empty
            Toast.makeText(mContext, R.string.no_changelog_alert, Toast.LENGTH_SHORT).show();
        } else {
            // Load the url
            mChangeLogView.loadUrl(Uri.fromFile(changeLog).toString());
        }
    }
    
    private void fetchChangeLog(final ItemInfo info) {
        Log.d(TAG, "Getting change log for " + info + ", url " + info.getChangelogUrl());

        final Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                    Toast.makeText(mContext, R.string.no_changelog_alert, Toast.LENGTH_SHORT).show();
                }
            }
        };
        // We need to make a blocking request here
        RequestFuture<String> future = RequestFuture.newFuture();
        ChangeLogRequest request = new ChangeLogRequest(Request.Method.GET, info.getChangelogUrl(),
                Utils.getUserAgentString(mContext), future, errorListener);
        request.setTag(TAG);

        ((MKCenterApplication) mContext.getApplicationContext()).getQueue().add(request);
        try {
            String response = future.get();
            parseChangeLogFromResponse(info, response);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void parseChangeLogFromResponse(ItemInfo info, String response) {
        boolean finished = false;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(info.getChangeLogFile(mContext)));
            ByteArrayInputStream bais = new ByteArrayInputStream(response.getBytes("UTF-8"));
            reader = new BufferedReader(new InputStreamReader(bais), 2 * 1024);
            String line;

            while ((line = reader.readLine()) != null) {
                // line = line.trim();
                if (line.isEmpty()) {
                    continue;
                } else if (line.startsWith("Project:")) {
                    writer.append("<u>");
                    writer.append(line);
                    writer.append("</u><br />");
                } else if (line.startsWith(" ")) {
                    writer.append("&#8226;&nbsp;");
                    writer.append(line);
                    writer.append("<br />");
                } else {
                    writer.append(line);
                    writer.append("<br />");
                }
            }
            finished = true;
        } catch (IOException e) {
            Log.e(TAG, "Downloading change log for " + info + " failed", e);
            // keeping finished at false will delete the partially written file below
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore, not much we can do anyway
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore, not much we can do anyway
                }
            }
        }

        if (!finished) {
            info.getChangeLogFile(mContext).delete();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        // Cancel all pending requests
        ((MKCenterApplication) mContext.getApplicationContext()).getQueue().cancelAll(TAG);
        // Clean up
        mChangeLogView.destroy();
        mChangeLogView = null;
        mAlertDialog = null;
    }
}
