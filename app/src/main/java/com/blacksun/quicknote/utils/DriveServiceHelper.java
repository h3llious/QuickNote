package com.blacksun.quicknote.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.models.DriveFileHolder;
import com.google.android.gms.drive.DriveFolder;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_DB;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_FOLDER;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
public class DriveServiceHelper {
//    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive googleServiceDrive;
    private Context context;


    public static final String CHANNEL_ID = "101";

    public static final String DRIVE_TAG = "GDrive";
//    final String MIME_TYPE_DB = "application/x-sqlite-3";
//    final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";

    public DriveServiceHelper(Drive driveService, Context context) {
        this.googleServiceDrive = driveService;
        this.context = context;

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getResources().getString(R.string.channel), importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public String searchSingle(String mime, String name, String parentId) throws IOException {
        DriveFileHolder driveFile = new DriveFileHolder();

        String query;
//        boolean isUnique = false;
        if (parentId == null) {
            query = "mimeType='" + mime + "' and name = '" + name + "'";

        } else {

            if (mime != null && name != null)
                query = "mimeType='" + mime + "' and name = '" + name + "' and '" + parentId + "' in parents";
            else
                query = "'" + parentId + "' in parents";

        }

        String pageToken = null;
        do {
            FileList result = googleServiceDrive.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();

            //if unique file is found, return
            boolean isFound = false;

            for (com.google.api.services.drive.model.File file : result.getFiles()) {
                driveFile.setId(file.getId());
                driveFile.setName(file.getName());

                isFound = true;
                break;
            }

            if (isFound) {
                break;
            }

            pageToken = result.getNextPageToken();
            Log.d(DRIVE_TAG, "Searching...");
        } while (pageToken != null);
        Log.d(DRIVE_TAG, "Searching completed");
        String id = null;
        id = driveFile.getId();
        return id;
    }

    public ArrayList<DriveFileHolder> search(String mime, String name, String parentId) throws IOException {
        ArrayList<DriveFileHolder> driveFiles = new ArrayList<>();

        String query;
        boolean isUnique = false;
        if (parentId != null) {
            if (mime == null && name == null)
                query = "'" + parentId + "' in parents";
            else
                query = "mimeType='" + mime + "' and name = '" + name + "' and '" + parentId + "' in parents";
        } else {
            query = "mimeType='" + mime + "' and name = '" + name + "'";

            if (mime.equals(MIME_TYPE_DB) || mime.equals(MIME_TYPE_FOLDER)) {
                isUnique = true;
            }
        }

        String pageToken = null;
        do {
            FileList result = googleServiceDrive.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();

            //if unique file is found, return
            boolean isFound = false;

            for (com.google.api.services.drive.model.File file : result.getFiles()) {
                DriveFileHolder holder = new DriveFileHolder();
                holder.setId(file.getId());
                holder.setName(file.getName());
                driveFiles.add(holder);

                if (isUnique) {
                    isFound = true;
                    break;
                }
            }

            if (isFound) {
                break;
            }

            pageToken = result.getNextPageToken();
            Log.d(DRIVE_TAG, "Searching...");
        } while (pageToken != null);
        Log.d(DRIVE_TAG, "Searching completed");
        return driveFiles;
    }

    public void download(String outputFilePath, String driveId) throws IOException {
        java.io.File file = new java.io.File(outputFilePath);
        OutputStream output = new FileOutputStream(file);
        googleServiceDrive.files().get(driveId).executeMediaAndDownloadTo(output);
    }

    public void upload(java.io.File filePath, String mimeType, String parentId) throws IOException {
        File attachMetadata = new File();
        attachMetadata.setName(filePath.getName());

        if (parentId != null)
            attachMetadata.setParents(Collections.singletonList(parentId));


        Log.d(DRIVE_TAG, "path " + filePath.getAbsolutePath());

        FileContent attachContent = new FileContent(mimeType, filePath);
        File uploaded = googleServiceDrive.files().create(attachMetadata, attachContent)
                .setFields("id")
                .execute();

        if (uploaded == null) {
            throw new IOException("Null result when requesting file upload.");
        }
    }

    public String createFolder(String name, String parentId) throws IOException {
        com.google.api.services.drive.model.File folderMetadata = new com.google.api.services.drive.model.File();
        if (parentId != null)
            folderMetadata.setParents(Collections.singletonList(parentId));
        folderMetadata.setMimeType(DriveFolder.MIME_TYPE);
        folderMetadata.setName(name);

        com.google.api.services.drive.model.File newFilesFolder =
                googleServiceDrive.files().create(folderMetadata).execute();

        if (newFilesFolder == null) {
            throw new IOException("Null result when requesting folder creation.");
        }

        return newFilesFolder.getId();
    }

    public void deleteFile(String fileId) throws IOException {
        googleServiceDrive.files().delete(fileId).execute();
    }

}