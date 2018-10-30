package com.filestack.android.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.util.Log;
import android.widget.TextView;

import com.filestack.FileLink;
import com.filestack.android.FsConstants;
import com.filestack.android.Selection;

import java.util.Locale;

public class UploadStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "UploadStatusReceiver";

    private TextView logView;

    public UploadStatusReceiver(TextView logView) {
        this.logView = logView;
        Log.d(TAG, "Upload Start");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Upload Complete");
        String status = intent.getStringExtra(FsConstants.EXTRA_STATUS);
        Selection selection = intent.getParcelableExtra(FsConstants.EXTRA_SELECTION);
        FileLink fileLink = (FileLink) intent.getSerializableExtra(FsConstants.EXTRA_FILE_LINK);
        String name = selection.getName();
        String handle = fileLink != null ? fileLink.getHandle() : "n/a";
        logView.append("Selection========================\n");
        logView.append("Name: " + selection.getName() + "\n");
        logView.append("Path: " + selection.getPath() + "\n");
        logView.append("Uri: " + selection.getUri() + "\n");
        logView.append("========================\n");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        logView.append("Response========================\n");
        logView.append("Status: " + status.toUpperCase() + "\n");
        logView.append("Link: " + "https://cdn.filestackcontent.com/" + handle + "\n");
        logView.append("========================\n");
    }
}
