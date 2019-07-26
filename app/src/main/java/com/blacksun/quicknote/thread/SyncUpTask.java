package com.blacksun.quicknote.thread;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.blacksun.quicknote.activities.MainActivity;
import com.blacksun.quicknote.models.DriveFileHolder;
import com.blacksun.quicknote.utils.DriveServiceHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.blacksun.quicknote.activities.MainActivity.DIRECTORY;
import static com.blacksun.quicknote.utils.DatabaseHelper.DATABASE_NAME;
import static com.blacksun.quicknote.utils.DriveServiceHelper.DRIVE_TAG;
import static com.blacksun.quicknote.utils.UtilHelper.DATABASE_PATH;
import static com.blacksun.quicknote.utils.UtilHelper.FILE_DATABASE;
import static com.blacksun.quicknote.utils.UtilHelper.FOLDER_NAME;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_DB;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_FOLDER;

public class SyncUpTask implements Runnable {
    private DriveServiceHelper driveServiceHelper;
    private Context context;

    public SyncUpTask(DriveServiceHelper driveServiceHelper, Context context) {
        this.driveServiceHelper = driveServiceHelper;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            //delete old files
            //database
            SearchTask searchDbTask = new SearchTask(driveServiceHelper, MIME_TYPE_DB, DATABASE_NAME, null);
            Future<ArrayList<DriveFileHolder>> resDbId = SyncManager.getSyncManager().callSyncArray(searchDbTask);

            //stop if no database found
            Future<Boolean> resDelDb = null;
            ArrayList<DriveFileHolder> database = resDbId.get();
            if (database.size() >= 1) {
                String databaseId = database.get(0).getId();

                DeleteFileTask deleteFileTask = new DeleteFileTask(driveServiceHelper, databaseId);
                resDelDb = SyncManager.getSyncManager().callSyncBool(deleteFileTask);

                Log.d(DRIVE_TAG, "Database deleted " + databaseId);
            } else {
                Log.e(DRIVE_TAG, "Cannot find database");
            }

            SearchTask searchFolderTask = new SearchTask(driveServiceHelper, MIME_TYPE_FOLDER, FOLDER_NAME, null);
            Future<ArrayList<DriveFileHolder>> resDelFolderId = SyncManager.getSyncManager().callSyncArray(searchFolderTask);
            ArrayList<DriveFileHolder> filesFolder = resDelFolderId.get();
            String folderDelId;
            Future<Boolean> resDelFolder = null;
            if (filesFolder.size() >= 1) {
                folderDelId = filesFolder.get(0).getId();

                DeleteFileTask deleteFileTask = new DeleteFileTask(driveServiceHelper, folderDelId);
                resDelFolder = SyncManager.getSyncManager().callSyncBool(deleteFileTask);
                Log.d(DRIVE_TAG, "Folder files deleted " + folderDelId);
            } else {
                Log.e(DRIVE_TAG, "Cannot find \"files\" folder");
            }

            if (resDelDb != null)
                resDelDb.get();
            if (resDelFolder != null)
                resDelFolder.get();








            //new implementation body
            UploadTask uploadDb = new UploadTask(driveServiceHelper, FILE_DATABASE, MIME_TYPE_DB, null);
            Future<Boolean> resDb = SyncManager.getSyncManager().callSyncBool(uploadDb);

            //maybe no need wrapper for folderID if have another runnable
            CreateFolderTask createFilesFolder = new CreateFolderTask(driveServiceHelper);
            Future<String> resFolderId = SyncManager.getSyncManager().callSyncString(createFilesFolder);
            String folderId = resFolderId.get();

            File[] files = DIRECTORY.listFiles();
            Log.d("Files", "Size: " + files.length);

            ArrayList<Future<Boolean>> results = new ArrayList<>();

            for (File child : files) {
                String name = child.getName();
                if (!name.equals("instant-run")) {
//                                    allFilesPath.add(name);
                    UploadTask uploadAttaches = new UploadTask(driveServiceHelper, child, getMIMEType(child), folderId);
                    Future<Boolean> res = SyncManager.getSyncManager().callSyncBool(uploadAttaches);
                    results.add(res);
                }
                Log.d("Files", getMIMEType(child) + " FileName:" + child.getName());
            }

            for (Future<Boolean> res : results) {
                Log.d(DRIVE_TAG, "block thread");
                res.get();
            }

            SyncManager.getSyncManager().getMainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Finished uploading data into Drive", Toast.LENGTH_LONG).show();
                    //just reload the screen
                    Intent startActivity = new Intent();
                    startActivity.setClass(context, MainActivity.class);
                    startActivity.setAction(MainActivity.class.getName());
                    startActivity.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    context.startActivity(startActivity);
                }
            });

        } catch (InterruptedException e) {
            Log.e(DRIVE_TAG, "error interrupted " + e);
        } catch (ExecutionException e) {
            Log.e(DRIVE_TAG, "error execution " + e);
            Throwable throwable = e.getCause();
            if (throwable instanceof UserRecoverableAuthIOException) {
                Intent errorIntent = ((UserRecoverableAuthIOException) throwable).getIntent();
                errorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.getApplicationContext().startActivity(errorIntent);
                SyncManager.getSyncManager().getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Please grant permissions and try again", Toast.LENGTH_LONG).show();

                    }
                });
            }
        }


    }

    public String getMIMEType(File child) {
        String mimeType;
        Uri uri = Uri.fromFile(child);
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

}
