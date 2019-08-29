package com.blacksun.quicknote.thread;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
import static com.blacksun.quicknote.utils.UtilHelper.FOLDER_NAME;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_DB;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_FOLDER;

public class SyncDownTask implements Runnable {
    private DriveServiceHelper driveServiceHelper;
    private Context context;

    public SyncDownTask(DriveServiceHelper driveServiceHelper, Context context) {
        this.driveServiceHelper = driveServiceHelper;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            //delete old local files
            File[] files = DIRECTORY.listFiles();
            Log.d("Files", "Size: " + files.length);
            for (File child : files) {
                String name = child.getName();
                Log.d("Files",  " Deleting FileName:" + child.getName());
                if (!name.equals("instant-run")) {
//                                    allFilesPath.add(name);
                    child.delete();
                }

            }

            //database
            SearchTask searchDbTask = new SearchTask(driveServiceHelper, MIME_TYPE_DB, DATABASE_NAME, null);
            Future<ArrayList<DriveFileHolder>> resDbId = SyncManager.getSyncManager().callSyncArray(searchDbTask);

            //stop if no database found
            Future<Boolean> resDb;
            ArrayList<DriveFileHolder> database = resDbId.get();
            if (database.size() >= 1) {
                String databaseId = database.get(0).getId();

                DownloadTask downloadDbTask = new DownloadTask(driveServiceHelper, DATABASE_PATH, databaseId);
                resDb = SyncManager.getSyncManager().callSyncBool(downloadDbTask);

                Log.d(DRIVE_TAG, "Database downloaded " + databaseId);
            } else {
                Log.e(DRIVE_TAG, "Cannot download database");
                return;
            }

            SearchTask searchFolderTask = new SearchTask(driveServiceHelper, MIME_TYPE_FOLDER, FOLDER_NAME, null);
            Future<ArrayList<DriveFileHolder>> resFolder = SyncManager.getSyncManager().callSyncArray(searchFolderTask);
            ArrayList<DriveFileHolder> filesFolder = resFolder.get();
            String folderId;
            if (filesFolder.size() >= 1) {
                folderId = filesFolder.get(0).getId();
            } else {
                Log.e(DRIVE_TAG, "Cannot get \"files\" folder");
                return;
            }

            if (folderId != null) {
                SearchTask searchFilesTask = new SearchTask(driveServiceHelper, null, null, folderId);
                Future<ArrayList<DriveFileHolder>> resFiles = SyncManager.getSyncManager().callSyncArray(searchFilesTask);
                ArrayList<DriveFileHolder> attachments = resFiles.get();

                if (attachments.size() == 0){
                    Log.d(DRIVE_TAG, "No attaches");
                    return;
                }

                ArrayList<Future<Boolean>> results = new ArrayList<>();

                String filesDir = DIRECTORY.getAbsolutePath();
                for (DriveFileHolder attach : attachments) {
                    String attachId = attach.getId();

                    DownloadTask downloadAttachTask = new DownloadTask(driveServiceHelper, filesDir + "/" + attach.getName(), attachId);
                    Future<Boolean> resAttach = SyncManager.getSyncManager().callSyncBool(downloadAttachTask);
                    results.add(resAttach);

                }

                for (Future<Boolean> res : results) {
                    Log.d(DRIVE_TAG, "block thread");
                    res.get();
                }
            } else {
                Log.e(DRIVE_TAG, "No attachment");
            }

            //wait until finish
            resDb.get();

            SyncManager.getSyncManager().getMainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Finished downloading data from Drive", Toast.LENGTH_LONG).show();

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
}
