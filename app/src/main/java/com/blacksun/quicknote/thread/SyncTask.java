package com.blacksun.quicknote.thread;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.data.AttachManager;
import com.blacksun.quicknote.data.NoteContract;
import com.blacksun.quicknote.data.NoteManager;
import com.blacksun.quicknote.models.Attachment;
import com.blacksun.quicknote.models.DriveFileHolder;
import com.blacksun.quicknote.models.Note;
import com.blacksun.quicknote.utils.DriveServiceHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import java.io.File;
import java.io.IOException;
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

public class SyncTask implements Runnable {
    private DriveServiceHelper driveServiceHelper;
    private Context context;

    public SyncTask(DriveServiceHelper driveServiceHelper, Context context) {
        this.driveServiceHelper = driveServiceHelper;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            //search app folder
            SearchSingleTask searchAppFolderTask = new SearchSingleTask(driveServiceHelper, MIME_TYPE_FOLDER, context.getString(R.string.app_name), null);
            Future<String> resAppFolderId = SyncManager.getSyncManager().callSyncString(searchAppFolderTask);
            String appFolderId = resAppFolderId.get();

            boolean isCreatedAppFolder;

            if (appFolderId == null) {
                CreateFolderTask createAppFolder = new CreateFolderTask(driveServiceHelper, context.getString(R.string.app_name));
                Future<String> resFolderId = SyncManager.getSyncManager().callSyncString(createAppFolder);
                appFolderId = resFolderId.get();
                isCreatedAppFolder = false;
            } else {
                isCreatedAppFolder = true;
            }

            SearchSingleTask searchFilesFolderTask = new SearchSingleTask(driveServiceHelper, MIME_TYPE_FOLDER, FOLDER_NAME, appFolderId);
            Future<String> resFilesFolderId = SyncManager.getSyncManager().callSyncString(searchFilesFolderTask);
            String filesFolderId = resFilesFolderId.get();

            if (filesFolderId == null) {
                CreateFolderTask createFilesFolder = new CreateFolderTask(driveServiceHelper, FOLDER_NAME);
                Future<String> resFolderId = SyncManager.getSyncManager().callSyncString(createFilesFolder);
                filesFolderId = resFolderId.get();
            }

            if (isCreatedAppFolder) {
                //search db
                //database
                SearchSingleTask searchDbTask = new SearchSingleTask(driveServiceHelper, MIME_TYPE_DB, DATABASE_NAME, appFolderId);
                Future<String> resDbId = SyncManager.getSyncManager().callSyncString(searchDbTask);
                String dbId = resDbId.get();


                Future<Boolean> resDb;
                //download db for further usage
                if (dbId != null) {
                    //get all notes before overwriting existing db
                    ArrayList<Note> localNotes = NoteManager.newInstance(context).getAllNotes();
                    Log.d(DRIVE_TAG, "size test: " + localNotes.size());

                    //get all attaches
                    ArrayList<Attachment> localAttaches = AttachManager.newInstance(context).getAllAttaches();
                    Log.d(DRIVE_TAG, "size attaches test: " + localAttaches.size());

                    //cloud db
                    DownloadTask downloadDbTask = new DownloadTask(driveServiceHelper, DATABASE_PATH, dbId);
                    resDb = SyncManager.getSyncManager().callSyncBool(downloadDbTask);
                    resDb.get();

                    //for each id in local db
                    for (int i = 0; i < localNotes.size(); i++) {
                        Note local = localNotes.get(i);
                        long noteLocalId = local.getId();

                        Note cloudNote = NoteManager.newInstance(context).getNote(noteLocalId);


                        if (cloudNote != null) { //exist note satisfied conditions
                            long localModTime = local.getDateModified();
                            long cloudModTime = cloudNote.getDateModified();

                            if (localModTime > cloudModTime) { //local is newer
                                updateCloud(noteLocalId, localAttaches, filesFolderId);
                            } else if (localModTime < cloudModTime) {//local is older
                                updateLocal(local, cloudNote, localAttaches, filesFolderId);
                            }

                            NoteManager.newInstance(context).delete(cloudNote); //reduce size of cloud db until only new notes left
                        } else { //check sync
                            int synced = local.getSync();

                            if (synced == 1) { //note deleted on cloud
                                //delete note in localNotes

                                //delete attaches in localAttaches
                            } else if (synced == 0) { //new note
                                //upload new attaches into Cloud

                            }
                        }

                    }

                    //for the rest in cloud...


                    //add info into database

                }
            }


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

    private void updateLocal(Note local, Note cloudNote, ArrayList<Attachment> localAttaches, String filesFolderId) throws ExecutionException, InterruptedException {
        local.setTitle(cloudNote.getTitle());
        local.setContent(cloudNote.getContent());
        local.setDateModified(cloudNote.getDateModified());

        ArrayList<Attachment> cloudAttaches = AttachManager.newInstance(context).getAttach(local.getId(), NoteContract.AttachEntry.ANY_TYPE);

        //temp array of attaches of a given note
        //ArrayList<Attachment> updateAttaches = new ArrayList<>();

        for (int i = localAttaches.size() - 1; i >= 0; i--) {
            Attachment currentAttach = localAttaches.get(i);
            if (local.getId() == currentAttach.getNote_id()) {

                boolean isExist = false;

                for (int iCloud = 0; iCloud < cloudAttaches.size(); iCloud++) {
                    Attachment cloudAttach = cloudAttaches.get(iCloud);
                    if (currentAttach.getId() == cloudAttach.getId()) {
                        isExist = true;
                        cloudAttaches.remove(iCloud);
                        break;
                    }
                }

                if (!isExist) {
                    File deletedFile = new File(currentAttach.getPath());
                    boolean isDeleted = deletedFile.delete();
                    Log.d(DRIVE_TAG, "is deleted local file " + isDeleted);

                    localAttaches.remove(i);
                }
            }
        }

        String filesDir = DIRECTORY.getAbsolutePath();

        ArrayList<Future<Boolean>> addResults = new ArrayList<>();

        for (Attachment attach : cloudAttaches) {
            Attachment newAttach = new Attachment();
            newAttach.setNote_id(attach.getNote_id());
            newAttach.setPath(attach.getPath());
            newAttach.setType(attach.getType());

            localAttaches.add(newAttach);

            File addedFile = new File(attach.getPath());
            String fileName = addedFile.getName();

            SearchSingleTask searchFileTask = new SearchSingleTask(driveServiceHelper, getMIMEType(addedFile), fileName, filesFolderId);
            Future<String> resFileId = SyncManager.getSyncManager().callSyncString(searchFileTask);
            String fileId = resFileId.get();

            DownloadTask downloadAttachTask = new DownloadTask(driveServiceHelper, filesDir + "/" + fileName, fileId);
            Future<Boolean> resAttach = SyncManager.getSyncManager().callSyncBool(downloadAttachTask);
            addResults.add(resAttach);

            Log.d(DRIVE_TAG, "new file downloaded to local: "+fileName);
        }

        for (Future<Boolean> res: addResults) {
            res.get();
        }

    }


    //upload new change into Drive
    private void updateCloud(long noteLocalId, ArrayList<Attachment> localAttaches, String filesFolderId) throws InterruptedException, ExecutionException {
//        ArrayList<Attachment> updateAttaches = new ArrayList<>();

        //need for deleting files in Drive
        ArrayList<Attachment> cloudAttaches = AttachManager.newInstance(context).getAttach(noteLocalId, NoteContract.AttachEntry.ANY_TYPE);

        //waiting for all thread finished
        ArrayList<Future<Boolean>> results = new ArrayList<>();

        //get filtered local attachments
        for (int i = 0; i < localAttaches.size(); i++) {
            Attachment attach = localAttaches.get(i);
            if (noteLocalId == attach.getNote_id()) {
                //updateAttaches.add(attach);

                boolean isExist = false;

                for (int iCloud = 0; iCloud < cloudAttaches.size(); iCloud++) {
                    Attachment cloudAttach = cloudAttaches.get(iCloud);
                    if (attach.getId() == cloudAttach.getId()) {
                        isExist = true;
                        cloudAttaches.remove(iCloud);
                        break;
                    }
                }

                if (!isExist) {
                    File newFile = new File(attach.getPath());
                    UploadTask uploadAttaches = new UploadTask(driveServiceHelper, newFile, getMIMEType(newFile), filesFolderId);
                    Future<Boolean> res = SyncManager.getSyncManager().callSyncBool(uploadAttaches);
                    results.add(res);
                    Log.d(DRIVE_TAG, "file uploaded to Drive : " + newFile.getName());
                }
            }
        }

        ArrayList<Future<Boolean>> delResults = new ArrayList<>();

        for (Attachment attach : cloudAttaches) {
            File deletedFile = new File(attach.getPath());
            String fileName = deletedFile.getName();

            Log.d(DRIVE_TAG, "file deleted on Drive " + fileName);

            SearchSingleTask searchFileTask = new SearchSingleTask(driveServiceHelper, getMIMEType(deletedFile), fileName, filesFolderId);
            Future<String> resFileId = SyncManager.getSyncManager().callSyncString(searchFileTask);
            String fileId = resFileId.get();

            DeleteFileTask deleteFileTask = new DeleteFileTask(driveServiceHelper, fileId);
            Future<Boolean> resDelFile = SyncManager.getSyncManager().callSyncBool(deleteFileTask);
            //TODO check if needed or not
            delResults.add(resDelFile);
        }

        for (Future<Boolean> res : results) {
            res.get();
        }

        for (Future<Boolean> res : delResults) {
            res.get();
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
