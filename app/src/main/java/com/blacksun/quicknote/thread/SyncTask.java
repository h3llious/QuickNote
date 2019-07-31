package com.blacksun.quicknote.thread;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.activities.MainActivity;
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
import static com.blacksun.quicknote.utils.UtilHelper.FILE_DATABASE;
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
                //create new app folder in Drive
                CreateFolderTask createAppFolder = new CreateFolderTask(driveServiceHelper, context.getString(R.string.app_name), null);
                Future<String> resFolderId = SyncManager.getSyncManager().callSyncString(createAppFolder);
                appFolderId = resFolderId.get();
                Log.d(DRIVE_TAG, "Created new database");
                isCreatedAppFolder = false;
            } else {
                isCreatedAppFolder = true;
                Log.d(DRIVE_TAG, "Database already exist");
            }

            boolean isCreatedFilesFolder = true;

            //search 'files' folder
            SearchSingleTask searchFilesFolderTask = new SearchSingleTask(driveServiceHelper, MIME_TYPE_FOLDER, FOLDER_NAME, appFolderId);
            Future<String> resFilesFolderId = SyncManager.getSyncManager().callSyncString(searchFilesFolderTask);
            String filesFolderId = resFilesFolderId.get();

            if (filesFolderId == null) {
                //create new 'files' folder if not exist
                isCreatedFilesFolder = false;
                CreateFolderTask createFilesFolder = new CreateFolderTask(driveServiceHelper, FOLDER_NAME, appFolderId);
                Future<String> resFolderId = SyncManager.getSyncManager().callSyncString(createFilesFolder);
                filesFolderId = resFolderId.get();
                Log.d(DRIVE_TAG, "Created new 'files' folder");
            }


            //for deleting later
            String dbId = null;

            if (isCreatedAppFolder) {
                //search db
                //database
                SearchSingleTask searchDbTask = new SearchSingleTask(driveServiceHelper, MIME_TYPE_DB, DATABASE_NAME, appFolderId);
                Future<String> resDbId = SyncManager.getSyncManager().callSyncString(searchDbTask);
                dbId = resDbId.get();


                Future<Boolean> resDb;
                //download db for further usage
                if (dbId != null) {
                    //get all notes before overwriting existing db
                    final NoteManager noteManager = NoteManager.newInstance(context);
                    ArrayList<Note> localNotes = noteManager.getAllNotes();
                    Log.d(DRIVE_TAG, "Old database found! Size notes: " + localNotes.size());

                    //get all attaches
                    final AttachManager attachManager = AttachManager.newInstance(context);
                    ArrayList<Attachment> localAttaches = attachManager.getAllAttaches();
                    Log.d(DRIVE_TAG, "size attaches test: " + localAttaches.size());

                    //check db version
                    SQLiteDatabase sqlDb = SQLiteDatabase.openDatabase
                            (DATABASE_PATH, null, SQLiteDatabase.OPEN_READONLY);
                    int localVersion = sqlDb.getVersion();


                    //TODO backup local db


                    //cloud db
                    DownloadTask downloadDbTask = new DownloadTask(driveServiceHelper, DATABASE_PATH, dbId);
                    resDb = SyncManager.getSyncManager().callSyncBool(downloadDbTask);
                    resDb.get();


                    //check db version
                    sqlDb = SQLiteDatabase.openDatabase
                            (DATABASE_PATH, null, SQLiteDatabase.OPEN_READONLY);
                    int cloudVersion = sqlDb.getVersion();

                    boolean checkVersion;
                    if (cloudVersion < localVersion) {

                        DeleteFileTask deleteFileTask = new DeleteFileTask(driveServiceHelper, filesFolderId);
                        Future<Boolean> resDelFolder = SyncManager.getSyncManager().callSyncBool(deleteFileTask);
                        Log.d(DRIVE_TAG, "Folder files deleted " + filesFolderId);

                        isCreatedFilesFolder = false;
                        CreateFolderTask createFilesFolder = new CreateFolderTask(driveServiceHelper, FOLDER_NAME, appFolderId);
                        Future<String> resFolderId = SyncManager.getSyncManager().callSyncString(createFilesFolder);
                        filesFolderId = resFolderId.get();
                        Log.d(DRIVE_TAG, "Created new 'files' folder");

                        SyncManager.getSyncManager().getMainThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Old version of cloud database detected", Toast.LENGTH_LONG).show();
                            }
                        });


                    } else if (cloudVersion > localVersion) {
                        SyncManager.getSyncManager().getMainThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Old version of local database detected, please update to the latest version", Toast.LENGTH_LONG).show();
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
                    } else {


                        //check if there are some attachments missing
                        boolean isMissing = false;

                        //for each id in local db
                        for (int i = localNotes.size() - 1; i >= 0; i--) {
                            Note local = localNotes.get(i);
                            long noteLocalId = local.getId();


                            //add to fix overwriting new note
                            if (local.getSync() == 0) { //new note
                                //upload new attaches into Cloud
                                ArrayList<Future<Boolean>> results = new ArrayList<>();
                                for (int iAttach = localAttaches.size() - 1; iAttach >= 0; iAttach--) {

                                    Attachment attach = localAttaches.get(iAttach);
                                    if (attach.getNote_id() == noteLocalId) {
                                        File newFile = new File(attach.getPath());
                                        UploadTask uploadAttaches = new UploadTask(driveServiceHelper, newFile, getMIMEType(newFile), filesFolderId);
                                        Future<Boolean> res = SyncManager.getSyncManager().callSyncBool(uploadAttaches);
                                        results.add(res);
                                    }
                                }
                                for (Future<Boolean> res : results) {
                                    res.get();
                                }

                                //add new note into db
                                long newNoteId = noteManager.create(local);
                                Log.d(DRIVE_TAG, "new note from local created with id " + newNoteId);
                                localNotes.remove(i);

                                //...along with its attaches
                                for (int iNew = localAttaches.size() - 1; iNew >= 0; iNew--) {
                                    Attachment newAttach = localAttaches.get(iNew);
                                    if (noteLocalId == newAttach.getNote_id()) {
                                        newAttach.setNote_id(newNoteId);
                                        attachManager.create(newAttach);
                                        localAttaches.remove(iNew);
                                    }
                                }
                                continue;
                            }


                            Note cloudNote = noteManager.getNote(noteLocalId);


                            if (cloudNote != null) { //exist note satisfied conditions

                                //check deleted
                                int isDeleted = local.getDeleted();
                                if (isDeleted == NoteContract.NoteEntry.DELETED) {

                                    //delete attachments on Drive
                                    ArrayList<Future<Boolean>> delResults = new ArrayList<>();
                                    ArrayList<Attachment> cloudAttaches = AttachManager.newInstance(context).getAttach(noteLocalId, NoteContract.AttachEntry.ANY_TYPE);
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


                                        //delete attachment in db
                                        attachManager.delete(attach);
                                    }
                                    for (Future<Boolean> res : delResults) {
                                        res.get();
                                    }

                                    //delete in cloud db
                                    noteManager.delete(cloudNote);


                                    //delete local notes
                                    localNotes.remove(i);

                                    continue;

                                }


                                ArrayList<Attachment> cloudAttachesOfANote = attachManager.getAttach(cloudNote.getId(), NoteContract.AttachEntry.ANY_TYPE);

                                long localModTime = local.getDateModified();
                                long cloudModTime = cloudNote.getDateModified();

                                if (localModTime > cloudModTime) { //local is newer
                                    updateCloud(noteLocalId, localAttaches, filesFolderId);
                                } else if (localModTime < cloudModTime) {//local is older
                                    updateLocal(local, cloudNote, localAttaches, filesFolderId);
                                }

                                noteManager.delete(cloudNote); //reduce size of cloud db until only new notes left
                                for (Attachment attach : cloudAttachesOfANote) {
                                    attachManager.delete(attach);
                                }
                            } else { //check sync
                                int synced = local.getSync();

                                if (synced == 1) { //note deleted on cloud
                                    //delete note in localNotes
                                    localNotes.remove(i);

                                    //delete attaches in localAttaches
                                    for (int iAttach = localAttaches.size() - 1; iAttach >= 0; iAttach--) {
                                        if (localAttaches.get(iAttach).getNote_id() == noteLocalId) {
                                            localAttaches.remove(iAttach);
                                        }
                                    }
                                }

//                            else if (synced == 0) { //new note
//
//                                //upload new attaches into Cloud
//                                ArrayList<Future<Boolean>> results = new ArrayList<>();
//                                for (int iAttach = localAttaches.size() - 1; iAttach >= 0; iAttach--) {
//
//                                    Attachment attach = localAttaches.get(iAttach);
//                                    if (attach.getNote_id() == noteLocalId) {
//                                        File newFile = new File(attach.getPath());
//                                        UploadTask uploadAttaches = new UploadTask(driveServiceHelper, newFile, getMIMEType(newFile), filesFolderId);
//                                        Future<Boolean> res = SyncManager.getSyncManager().callSyncBool(uploadAttaches);
//                                        results.add(res);
//                                    }
//                                }
//                                for (Future<Boolean> res : results) {
//                                    res.get();
//                                }
//
//                                //add new note into db
//                                long newNoteId = noteManager.create(local);
//                                Log.d(DRIVE_TAG, "(obsolete)new note from local created with id "+newNoteId);
//                                localNotes.remove(i);
//
//                                //...along with its attaches
//                                for (int iNew = localAttaches.size() - 1; iNew >= 0; iNew--) {
//                                    Attachment newAttach = localAttaches.get(iNew);
//                                    if (noteLocalId == newAttach.getNote_id()) {
//                                        newAttach.setNote_id(newNoteId);
//                                        attachManager.create(newAttach);
//                                        localAttaches.remove(iNew);
//                                    }
//                                }
//
//                            }
                            }

                        }

                        //for the rest in cloud... TODO may need to check deleted note in local
                        //notes and attachments are still in db, no need to add again
                        ArrayList<Attachment> cloudAttaches = attachManager.getAllAttaches(); //download all new attachments
                        String filesDir = DIRECTORY.getAbsolutePath();

                        ArrayList<Future<Boolean>> addResults = new ArrayList<>();

                        for (Attachment attach : cloudAttaches) {

                            File addedFile = new File(attach.getPath());
                            String fileName = addedFile.getName();

                            SearchSingleTask searchFileTask = new SearchSingleTask(driveServiceHelper, getMIMEType(addedFile), fileName, filesFolderId);
                            Future<String> resFileId = SyncManager.getSyncManager().callSyncString(searchFileTask);
                            String fileId = resFileId.get();

                            if (fileId != null) {
                                DownloadTask downloadAttachTask = new DownloadTask(driveServiceHelper, filesDir + "/" + fileName, fileId);
                                Future<Boolean> resAttach = SyncManager.getSyncManager().callSyncBool(downloadAttachTask);
                                addResults.add(resAttach);
                                Log.d(DRIVE_TAG, "new file downloaded to local: " + fileName);
                            } else {
                                isMissing = true;
                            }


                        }

                        for (Future<Boolean> res : addResults) {
                            res.get();
                        }

                        //change notes into synced
                        ArrayList<Note> newCloudNotes = noteManager.getAllNotes();
                        for (Note note : newCloudNotes) {
                            note.setSync(NoteContract.NoteEntry.SYNCED);
                            noteManager.sync(note);
                        }


                        if (isMissing) {
                            SyncManager.getSyncManager().getMainThreadExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Attachment(s) missing!", Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        //the place temporary moved to back TODO backup database
                    }

                        //add info into database
                        for (int iNote = 0; iNote < localNotes.size(); iNote++) {

                            Note existedNote = localNotes.get(iNote);
                            if (existedNote.getDeleted() == NoteContract.NoteEntry.NOT_DELETED) {
                                noteManager.add(existedNote);
                                Log.d(DRIVE_TAG, "push note " + existedNote.getId());
                            }
                        }
                        for (int iAttach = 0; iAttach < localAttaches.size(); iAttach++) {
                            attachManager.add(localAttaches.get(iAttach));
                            Log.d(DRIVE_TAG, "push attach " + localAttaches.get(iAttach).getNote_id());
                        }
//                    }
                }
            }


            //upload db into Drive
            UploadTask uploadDb = new UploadTask(driveServiceHelper, FILE_DATABASE, MIME_TYPE_DB, appFolderId);
            Future<Boolean> resDb = SyncManager.getSyncManager().callSyncBool(uploadDb);
            resDb.get();


            if (dbId != null) {
                //delete old db
                DeleteFileTask deleteFileTask = new DeleteFileTask(driveServiceHelper, dbId);
                SyncManager.getSyncManager().callSyncBool(deleteFileTask); //no waiting
                Log.d(DRIVE_TAG, "Old database deleted " + dbId);
            }

            //when first created or folder is missing
            if (!isCreatedAppFolder || !isCreatedFilesFolder) {
                File[] files = DIRECTORY.listFiles();
                Log.d("Files", "Size: " + files.length);

                ArrayList<Future<Boolean>> results = new ArrayList<>();

                for (File child : files) {
                    String name = child.getName();
                    if (!name.equals("instant-run")) {
//                                    allFilesPath.add(name);
                        UploadTask uploadAttaches = new UploadTask(driveServiceHelper, child, getMIMEType(child), filesFolderId);
                        Future<Boolean> res = SyncManager.getSyncManager().callSyncBool(uploadAttaches);
                        results.add(res);
                    }
                    Log.d("Files", getMIMEType(child) + " FileName:" + child.getName());
                }

                for (Future<Boolean> res : results) {
                    Log.d(DRIVE_TAG, "block thread");
                    res.get();
                }
            }

            SyncManager.getSyncManager().getMainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Finished syncing data!", Toast.LENGTH_LONG).show();
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

                //check if attachment has already existed in cloud db
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

            Log.d(DRIVE_TAG, "new file updated to local: " + fileName);
        }

        for (Future<Boolean> res : addResults) {
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
