package com.filestack.android.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.filestack.Config;
import com.filestack.FileLink;
import com.filestack.android.FsActivity;
import com.filestack.android.FsActivity1;
import com.filestack.android.FsConstants;
import com.filestack.android.Selection;
import com.filestack.android.internal.Util;
import com.squareup.picasso.Picasso;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_FILESTACK = RESULT_FIRST_USER;
    private static final int REQUEST_SETTINGS = REQUEST_FILESTACK + 1;
    private static final String TAG = "MainActivity";
    ImageView img;
    VideoView video;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = (ImageView) findViewById(R.id.imageView);
        video = (VideoView) findViewById(R.id.video);
        if (savedInstanceState == null) {
            IntentFilter intentFilter = new IntentFilter(FsConstants.BROADCAST_UPLOAD);
            TextView logView = findViewById(R.id.log);
            UploadStatusReceiver1 receiver = new UploadStatusReceiver1(logView);
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
        }

//        IntentFilter filter = new IntentFilter(FsConstants.BROADCAST_UPLOAD);
//        registerReceiver(receiver, filter);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    public class UploadStatusReceiver1 extends BroadcastReceiver {
        private static final String TAG = "UploadStatusReceiver";

        private TextView logView;

        public UploadStatusReceiver1(TextView logView) {
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
            Picasso.with(context).load("https://cdn.filestackcontent.com/" + handle).into(img);

            video.setVideoURI(selection.getUri());
            video.start();

            video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //close the progress dialog when buffering is done

                }
            });
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Locale locale = Locale.getDefault();

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILESTACK && resultCode == RESULT_OK) {
            Log.i(TAG, "received filestack selections"+data.getExtras().get("uploadId"));
            String key = FsConstants.EXTRA_SELECTION_LIST;
            ArrayList<Selection> selections = data.getParcelableArrayListExtra(key);

            for (int i = 0; i < selections.size(); i++) {
                Selection selection = selections.get(i);
                String msg = String.format(locale, "selection %d: %s", i, selection.getName());
                Log.i(TAG, msg);
//                Bitmap bitmap2 = ThumbnailUtils.createVideoThumbnail(selections.get(i).getPath(),MediaStore.Images.Thumbnails.MINI_KIND);
//                img.setImageBitmap(bitmap2);
            }
        }
    }

    public void settings(View view) {
        launch1(view);
//        Intent intent = new Intent(this, SettingsActivity.class);
//        startActivityForResult(intent, REQUEST_SETTINGS);
    }

    public void launch(View view) {
        Intent intent = new Intent(this, FsActivity1.class);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> sources = sharedPref.getStringSet("upload_sources", null);
        intent.putExtra(FsConstants.EXTRA_SOURCES, new ArrayList<>(sources));

        boolean autoUpload = sharedPref.getBoolean("auto_upload", true);
        intent.putExtra(FsConstants.EXTRA_AUTO_UPLOAD, autoUpload);

        String mimeFilter = sharedPref.getString("mime_filter", null);
        String[] mimeTypes = mimeFilter.split(",");
        intent.putExtra(FsConstants.EXTRA_MIME_TYPES, mimeTypes);

//        String apiKey = sharedPref.getString("api_key", null);
        String policy = sharedPref.getString("policy", null);
        String signature = sharedPref.getString("signature", null);

//        if (apiKey == null) {
//            Toast.makeText(this, R.string.error_no_api_key, Toast.LENGTH_SHORT).show();
//            return;
//        }
        String apiKey = "ATlpVW2jhTceHdRpiJXKYz";
        Config config = new Config(apiKey, getString(R.string.return_url), policy, signature);
        intent.putExtra(FsConstants.EXTRA_CONFIG, config);

        boolean allowMultipleFiles = sharedPref.getBoolean("allow_multiple_files", true);
        intent.putExtra(FsConstants.EXTRA_ALLOW_MULTIPLE_FILES, allowMultipleFiles);

        startActivityForResult(intent, REQUEST_FILESTACK);
    }

    public void launch1(View view) {
        Intent intent = new Intent(this, FsActivity.class);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> sources = sharedPref.getStringSet("upload_sources", null);
        intent.putExtra(FsConstants.EXTRA_SOURCES, new ArrayList<>(sources));

        boolean autoUpload = sharedPref.getBoolean("auto_upload", true);
        intent.putExtra(FsConstants.EXTRA_AUTO_UPLOAD, autoUpload);

        String mimeFilter = sharedPref.getString("mime_filter", null);
        String[] mimeTypes = mimeFilter.split(",");
        intent.putExtra(FsConstants.EXTRA_MIME_TYPES, mimeTypes);

//        String apiKey = sharedPref.getString("api_key", null);
        String policy = sharedPref.getString("policy", null);
        String signature = sharedPref.getString("signature", null);

//        if (apiKey == null) {
//            Toast.makeText(this, R.string.error_no_api_key, Toast.LENGTH_SHORT).show();
//            return;
//        }
        String apiKey = "ATlpVW2jhTceHdRpiJXKYz";
        Config config = new Config(apiKey, getString(R.string.return_url), policy, signature);
        intent.putExtra(FsConstants.EXTRA_CONFIG, config);

        boolean allowMultipleFiles = sharedPref.getBoolean("allow_multiple_files", true);
        intent.putExtra(FsConstants.EXTRA_ALLOW_MULTIPLE_FILES, allowMultipleFiles);

        startActivityForResult(intent, REQUEST_FILESTACK);
    }
}
