package com.filestack.android.internal;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.StorageOptions;
import com.filestack.android.FsConstants;
import com.filestack.android.R;
import com.filestack.android.Selection;

import java.util.ArrayList;

import static android.app.Activity.RESULT_FIRST_USER;

/**
 * Handles opening system file browser and processing results for local file selection.
 *
 * @see <a href="https://developer.android.com/guide/topics/providers/document-provider">
 * https://developer.android.com/guide/topics/providers/document-provider</a>
 */
public class LocalFilesFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = LocalFilesFragment.class.getSimpleName();
    private static final String ARG_ALLOW_MULTIPLE_FILES = "multipleFiles";
    private static final int READ_REQUEST_CODE = RESULT_FIRST_USER;

    public static Fragment newInstance(boolean allowMultipleFiles) {
        Fragment fragment = new LocalFilesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_MULTIPLE_FILES, allowMultipleFiles);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.filestack__fragment_local_files, container, false);
        view.findViewById(R.id.select_gallery).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View view) {
        startFilePicker();
    }

    private void startFilePicker() {
        final Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            boolean allowMultipleFiles = getArguments().getBoolean(ARG_ALLOW_MULTIPLE_FILES, true);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultipleFiles);
            intent.setType("*/*");

            Intent launchIntent = getActivity().getIntent();
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
        Intent activityIntent = getActivity().getIntent();
        boolean autoUpload = activityIntent.getBooleanExtra(FsConstants.EXTRA_AUTO_UPLOAD, true);
        if (autoUpload) {
            StorageOptions storeOpts = (StorageOptions) activityIntent
                    .getSerializableExtra(FsConstants.EXTRA_STORE_OPTS);
            Intent uploadIntent = new Intent(getActivity(), UploadService.class);
            uploadIntent.putExtra(FsConstants.EXTRA_STORE_OPTS, storeOpts);
            uploadIntent.putExtra(FsConstants.EXTRA_SELECTION_LIST, selections);
            ContextCompat.startForegroundService(getActivity(), uploadIntent);
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
        getActivity().setResult(getActivity().RESULT_OK, data);
        getActivity().finish();
    }

    private Selection processUri(Uri uri) {
        ContentResolver resolver = getActivity().getContentResolver();

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
}
