package com.blacksun.quicknote.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.util.Pair;

import com.blacksun.quicknote.models.DriveFileHolder;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_DB;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_FOLDER;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive googleServiceDrive;
    private Context context;

    public static final String DRIVE_TAG = "GDrive";
//    final String MIME_TYPE_DB = "application/x-sqlite-3";
//    final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";

    public DriveServiceHelper(Drive driveService, Context context) {
        this.googleServiceDrive = driveService;
        this.context = context;
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

//            if (mime.equals(MIME_TYPE_DB) || mime.equals(MIME_TYPE_FOLDER)) {
//                isUnique = true;
//            }

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
//                DriveFileHolder holder = new DriveFileHolder();
                driveFile.setId(file.getId());
                driveFile.setName(file.getName());
//                driveFiles.add(holder);

//                if (isUnique) {
                isFound = true;
                break;
//                }
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

//    //needed for Drive upload
//    public String getMIMEType(java.io.File child) {
//        String mimeType;
//        Uri uri = Uri.fromFile(child);
//        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
//            ContentResolver cr = context.getContentResolver();
//            mimeType = cr.getType(uri);
//        } else {
//            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
//            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
//                    fileExtension.toLowerCase());
//        }
//        return mimeType;
//    }


    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    public Task<String> createFile() {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList("root"))
                    .setMimeType("text/plain")
                    .setName("Untitled file");

            File googleFile = googleServiceDrive.files().create(metadata).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            return googleFile.getId();
        });
    }

    /**
     * Opens the file identified by {@code fileId} and returns a {@link Pair} of its name and
     * contents.
     */
    public Task<Pair<String, String>> readFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the metadata as a File object.
            File metadata = googleServiceDrive.files().get(fileId).execute();
            String name = metadata.getName();

            // Stream the file contents to a String.
            try (InputStream is = googleServiceDrive.files().get(fileId).executeMediaAsInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String contents = stringBuilder.toString();

                return Pair.create(name, contents);
            }
        });
    }

    /**
     * Updates the file identified by {@code fileId} with the given {@code name} and {@code
     * content}.
     */
    public Task<Void> saveFile(String fileId, String name, String content) {
        return Tasks.call(mExecutor, () -> {
            // Create a File containing any metadata changes.
            File metadata = new File().setName(name);

            // Convert content to an AbstractInputStreamContent instance.
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

            // Update the metadata and contents.
            googleServiceDrive.files().update(fileId, metadata, contentStream).execute();
            return null;
        });
    }

    /**
     * Returns a {@link FileList} containing all the visible files in the user's My Drive.
     *
     * <p>The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the <a href="https://play.google.com/apps/publish">Google
     * Developer's Console</a> and be submitted to Google for verification.</p>
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, () ->
                googleServiceDrive.files().list().setSpaces("drive").execute());
    }

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        return intent;
    }

    /**
     * Opens the file at the {@code uri} returned by a Storage Access Framework {@link Intent}
     * created by {@link #createFilePickerIntent()} using the given {@code contentResolver}.
     */
    public Task<Pair<String, String>> openFileUsingStorageAccessFramework(
            ContentResolver contentResolver, Uri uri) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the document's display name from its metadata.
            String name;
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(nameIndex);
                } else {
                    throw new IOException("Empty cursor returned for file.");
                }
            }

            // Read the document's contents as a String.
            String content;
            try (InputStream is = contentResolver.openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                content = stringBuilder.toString();
            }

            return Pair.create(name, content);
        });
    }
}