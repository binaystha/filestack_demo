package com.filestack.android;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.filestack.CloudResponse;
import com.filestack.Config;
import com.filestack.Sources;
import com.filestack.StorageOptions;
import com.filestack.android.internal.BackButtonListener;

import com.filestack.android.internal.CloudAuthFragment;
import com.filestack.android.internal.CloudListFragment;

import com.filestack.android.internal.Constants;
import com.filestack.android.internal.SelectionFactory;
import com.filestack.android.internal.SelectionSaver;
import com.filestack.android.internal.SourceInfo;
import com.filestack.android.internal.UploadService;
import com.filestack.android.internal.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class FsActivity1 extends AppCompatActivity implements
        SingleObserver<CloudResponse>, CompletableObserver, SelectionSaver.Listener, RecyclerViewAdapter.ItemListener {

    private static final String TYPE_PHOTO = "photo";
    private static final String TYPE_VIDEO = "video";
    private static final String PREF_PATH = "path";
    private static final String PREF_NAME = "name";
    private static final String PREF_SESSION_TOKEN = "sessionToken";
    private static final String STATE_SELECTED_SOURCE = "selectedSource";
    private static final String STATE_SHOULD_CHECK_AUTH = "shouldCheckAuth";
    private static final String ARG_ALLOW_MULTIPLE_FILES = "multipleFiles";
    private static final int READ_REQUEST_CODE = RESULT_FIRST_USER;
    static final int REQUEST_MEDIA_CAPTURE = 2;
    private static final String TAG = FsActivity1.class.getSimpleName();

    private BackButtonListener backListener;

    private String selectedSource;
    private boolean shouldCheckAuth;

    RecyclerView recyclerView;
    ArrayList arrayList;
    private boolean allowMultipleFiles;
    RelativeLayout rel;
    Button filestack__photo, filestack__video, filestack__gallery;
    List<String> sources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        setContentView(R.layout.filestack__activity_filestack1);
        rel = (RelativeLayout) findViewById(R.id.filestack__rel);
        recyclerView = (RecyclerView) findViewById(R.id.filestack__recyclerView);
        arrayList = new ArrayList();
        rel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                finish();
                return false;
            }
        });

//        Button filestack__photo = (Button) findViewById(R.id.filestack__photo);
//        filestack__photo.setOnClickListener(this);
//
//        Button filestack__video = (Button) findViewById(R.id.filestack__video);
//        filestack__video.setOnClickListener(this);
//
//        Button filestack__gallery = (Button) findViewById(R.id.filestack__gallery);
//        filestack__gallery.setOnClickListener(this);

        sources = (List<String>) intent.getSerializableExtra(FsConstants.EXTRA_SOURCES);
        if (sources == null) {
            sources = Util.getDefaultSources();
        }
        int pos = 0;
        for (String source : sources) {
            int id = Util.getSourceIntId(source);
            SourceInfo info = Util.getSourceInfo(source);

            arrayList.add(new SourceInfo(String.valueOf(pos), info.getTextId(), info.getIconId(), getResources().getColor(R.color.filestack__accent)));
//            MenuItem item = menu.add(Menu.NONE, id, index++, info.getTextId());
//            item.setIcon(info.getIconId());
//            item.setCheckable(true);
            pos += 1;
        }

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, arrayList, this);
        recyclerView.setAdapter(adapter);


        /**
         AutoFitGridLayoutManager that auto fits the cells by the column width defined.
         **/

        /*AutoFitGridLayoutManager layoutManager = new AutoFitGridLayoutManager(this, 500);
        recyclerView.setLayoutManager(layoutManager);*/


        /**
         Simple GridLayoutManager that spans two columns
         **/
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);

        allowMultipleFiles = intent.getBooleanExtra(FsConstants.EXTRA_ALLOW_MULTIPLE_FILES, true);

        String[] mimeTypes = intent.getStringArrayExtra(FsConstants.EXTRA_MIME_TYPES);
        if (mimeTypes != null && sources.contains(Sources.CAMERA)) {
            if (!Util.mimeAllowed(mimeTypes, "image/jpeg") && !Util.mimeAllowed(mimeTypes, "video/mp4")) {
                sources.remove(Sources.CAMERA);
                Log.w(TAG, "Hiding camera since neither image/jpeg nor video/mp4 MIME type is allowed");
            }
        }

        if (savedInstanceState == null) {
            Config config = (Config) intent.getSerializableExtra(FsConstants.EXTRA_CONFIG);
            String sessionToken = preferences.getString(PREF_SESSION_TOKEN, null);
            Util.initializeClient(config, sessionToken);

            Util.getSelectionSaver().clear();

            selectedSource = sources.get(0);
//            nav.getMenu().performIdentifierAction(Util.getSourceIntId(selectedSource), 0);
//            if (drawer != null) {
//                drawer.openDrawer(Gravity.START);
//            }
        } else {
            selectedSource = savedInstanceState.getString(STATE_SELECTED_SOURCE);
            shouldCheckAuth = savedInstanceState.getBoolean(STATE_SHOULD_CHECK_AUTH);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

//        Util.getSelectionSaver().setItemChangeListener(this);

        if (shouldCheckAuth) {
            checkAuth();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        String sessionToken = Util.getClient().getSessionToken();
        preferences.edit().putString(PREF_SESSION_TOKEN, sessionToken).apply();
        Util.getSelectionSaver().setItemChangeListener(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(STATE_SELECTED_SOURCE, selectedSource);
        outState.putBoolean(STATE_SHOULD_CHECK_AUTH, shouldCheckAuth);
    }

    private void checkAuth() {
        SourceInfo info = Util.getSourceInfo(selectedSource);
        Util.getClient()
                .getCloudItemsAsync(info.getId(), "/")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);
    }


    @Override
    public void onEmptyChanged(boolean isEmpty) {
        invalidateOptionsMenu();
    }

    @Override
    public void onComplete() {
        checkAuth();
    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onSuccess(CloudResponse cloudResponse) {
        String authUrl = cloudResponse.getAuthUrl();

        // TODO Switching source views shouldn't depend on a network request

        if (authUrl != null) {
            shouldCheckAuth = true;
            CloudAuthFragment cloudAuthFragment = CloudAuthFragment.create(selectedSource, authUrl);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, cloudAuthFragment);
            transaction.commit();
        } else {
            shouldCheckAuth = false;
            CloudListFragment cloudListFragment = CloudListFragment.create(selectedSource, allowMultipleFiles);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.content, cloudListFragment);
            transaction.commit();
        }
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
    }

//    public void onClick(View v) {
//
//        Intent cameraIntent = null;
//        Util.getSelectionSaver().clear();
//        if (v.getId() == R.id.filestack__photo) {
//            cameraIntent = createCameraIntent(TYPE_PHOTO);
//            startActivityForResult(cameraIntent, REQUEST_MEDIA_CAPTURE);
//            Toast.makeText(getApplicationContext(), "Photo", Toast.LENGTH_SHORT).show();
//        } else if (v.getId() == R.id.filestack__video) {
//            cameraIntent = createCameraIntent(TYPE_VIDEO);
//            startActivityForResult(cameraIntent, REQUEST_MEDIA_CAPTURE);
//            Toast.makeText(getApplicationContext(), "Video", Toast.LENGTH_SHORT).show();
//        } else if (v.getId() == R.id.filestack__gallery) {
//            startFilePicker();
//            Toast.makeText(getApplicationContext(), "Gallery", Toast.LENGTH_SHORT).show();
//        } else {
//            checkAuth();
//        }
//
//
//    }

    private void startFilePicker() {
        final Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            boolean allowMultipleFiles = true;
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultipleFiles);
            intent.setType("*/*");

            Intent launchIntent = getIntent();
            String[] mimeTypes = launchIntent.getStringArrayExtra(FsConstants.EXTRA_MIME_TYPES);
            if (mimeTypes != null) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
            startActivityForResult(intent, READ_REQUEST_CODE);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Context context = this;
        SharedPreferences prefs = context.getSharedPreferences(getClass().getName(), MODE_PRIVATE);
        if (requestCode == REQUEST_MEDIA_CAPTURE && resultCode == RESULT_OK) {
            String path = prefs.getString(PREF_PATH, null);
            String name = prefs.getString(PREF_NAME, null);
            String mimeType = name.contains("jpg") ? "image/jpeg" : "video/mp4";
            Util.addMediaToGallery(this, path);
            Selection selection = new Selection(Sources.CAMERA, path, mimeType, name);
            Util.getSelectionSaver().toggleItem(selection);
            ArrayList<Selection> selections = new ArrayList<>();
            selections.add(selection);
            uploadSelections(selections);
        }

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "File Load Success");
            ClipData clipData = resultData.getClipData();
            ArrayList<Uri> uris = new ArrayList<>();

            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    uris.add(clipData.getItemAt(i).getUri());
                    Log.d(TAG, "File Uri: " + clipData.getItemAt(i).getUri());
                }
            } else {
                uris.add(resultData.getData());
                Log.d(TAG, "File Data: " + resultData.getData());
            }
            ArrayList<Selection> selections = new ArrayList<>();
            for (Uri uri : uris) {
                Selection selection = processUri(uri);
                Util.getSelectionSaver().toggleItem(selection);
                selections.add(selection);
            }

            uploadSelections(selections);
        }
    }

    private void uploadSelections(ArrayList<Selection> selections) {
        Log.d(TAG, "Upload started");
        Intent activityIntent = getIntent();
        boolean autoUpload = activityIntent.getBooleanExtra(FsConstants.EXTRA_AUTO_UPLOAD, true);
        if (autoUpload) {
            StorageOptions storeOpts = (StorageOptions) activityIntent
                    .getSerializableExtra(FsConstants.EXTRA_STORE_OPTS);
            Intent uploadIntent = new Intent(this, UploadService.class);
            uploadIntent.putExtra(FsConstants.EXTRA_STORE_OPTS, storeOpts);
            uploadIntent.putExtra(FsConstants.EXTRA_SELECTION_LIST, selections);
            ContextCompat.startForegroundService(this, uploadIntent);
            Log.d(TAG, "Store Options: " + storeOpts);
            for (int i = 0; i < selections.size(); i++) {
                Log.d(TAG, "Selections Uri: " + selections.get(i).getUri());
                Log.d(TAG, "Selections Mime: " + selections.get(i).getMimeType());
                Log.d(TAG, "Selections Size: " + selections.get(i).getSize());
                Log.d(TAG, "Selections Name: " + selections.get(i).getName());
                Log.d(TAG, "Selections Path: " + selections.get(i).getPath());
                Log.d(TAG, "Selections Provider: " + selections.get(i).getProvider());

            }
        }

        Intent data = new Intent();
        data.putExtra(FsConstants.EXTRA_SELECTION_LIST, selections);
        setResult(RESULT_OK, data);
        finish();
    }

    private Selection processUri(Uri uri) {
        ContentResolver resolver = getContentResolver();

        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

                // We can't upload files without knowing the size
                if (cursor.isNull(sizeIndex)) {
                    return null;
                }

                String name = cursor.getString(nameIndex);
                int size = cursor.getInt(sizeIndex);
                String mimeType = resolver.getType(uri);
                return SelectionFactory.from(uri, size, mimeType, name);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Intent createCameraIntent(String source) {
        Intent intent = null;
        File file = null;

        Context context = this;
        SharedPreferences prefs = context.getSharedPreferences(getClass().getName(), MODE_PRIVATE);

        try {
            switch (source) {
                case TYPE_PHOTO:
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    file = Util.createPictureFile(context);
                    break;
                case TYPE_VIDEO:
                    intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    file = Util.createMovieFile(context);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file != null) {
            String path = file.getAbsolutePath();
            String name = file.getName();
            prefs.edit().putString(PREF_PATH, path).putString(PREF_NAME, name).apply();
            Uri uri = Util.getUriForInternalMedia(this, file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        return intent;
    }


    @Override
    public void onItemClick(int pos) {
        Toast.makeText(getApplicationContext(), sources.get(pos) + " is clicked", Toast.LENGTH_SHORT).show();
        Util.getSelectionSaver().clear();
        Intent cameraIntent = null;
        if (sources.get(pos).equalsIgnoreCase("camera")) {
            cameraIntent = createCameraIntent(TYPE_PHOTO);
            startActivityForResult(cameraIntent, REQUEST_MEDIA_CAPTURE);
        } else if (sources.get(pos).equalsIgnoreCase("device")) {
            startFilePicker();
        } else {
            cameraIntent = createCameraIntent(TYPE_VIDEO);
            startActivityForResult(cameraIntent, REQUEST_MEDIA_CAPTURE);
        }
    }
}
