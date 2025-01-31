package com.blacksun.quicknote.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.activities.MainActivity;
import com.blacksun.quicknote.controllers.AttachManager;
import com.blacksun.quicknote.controllers.NoteManager;
import com.blacksun.quicknote.data.NoteContract;
import com.blacksun.quicknote.models.Attachment;
import com.blacksun.quicknote.models.Note;
import com.blacksun.quicknote.thread.CreateFolderTask;
import com.blacksun.quicknote.thread.DeleteFileTask;
import com.blacksun.quicknote.thread.DownloadTask;
import com.blacksun.quicknote.thread.SearchSingleTask;
import com.blacksun.quicknote.thread.SyncManager;
import com.blacksun.quicknote.thread.UploadTask;
import com.blacksun.quicknote.utils.DriveServiceHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.blacksun.quicknote.activities.MainActivity.DIRECTORY;
import static com.blacksun.quicknote.activities.MainActivity.REQUEST_RESTART;
import static com.blacksun.quicknote.utils.DatabaseHelper.DATABASE_NAME;
import static com.blacksun.quicknote.utils.DriveServiceHelper.CHANNEL_ID;
import static com.blacksun.quicknote.utils.DriveServiceHelper.DRIVE_TAG;
import static com.blacksun.quicknote.utils.UtilHelper.DATABASE_PATH;
import static com.blacksun.quicknote.utils.UtilHelper.FILE_DATABASE;
import static com.blacksun.quicknote.utils.UtilHelper.FOLDER_NAME;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_DB;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_FOLDER;
import static com.blacksun.quicknote.utils.UtilHelper.copy;
import static com.blacksun.quicknote.utils.UtilHelper.isInternetAvailable;

public class SyncService extends IntentService {

    DriveServiceHelper driveServiceHelper;
    Context context;

    public SyncService() {
        super(SyncService.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intentMain) {

        context = getApplicationContext();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this,
                Arrays.asList(MainActivity.DRIVE_SCOPES));
        credential.setSelectedAccount(account.getAccount());
        Drive googleServiceDrive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .setApplicationName(getResources().getString(R.string.app_name))
                .build();
        driveServiceHelper = new DriveServiceHelper(googleServiceDrive, this);

//        Runnable syncTask = new SyncTask(driveServiceHelper, getApplicationContext());
//        SyncManager.getSyncManager().runSync(syncTask);

        //check internet connection
        if (!isInternetAvailable()) {
            Log.d(DRIVE_TAG, "No internet connection");
            return;
        }
        Log.d(DRIVE_TAG, "Internet connected");

        //notification
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(MainActivity.class.getName());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("QuickNote")
                .setContentText(getString(R.string.service_syncing))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.service_syncing)))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        }

        int PROGRESS_MAX = 100;
        int PROGRESS_CURRENT = 0;
        builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        int notificationId = 10;
        startForeground(notificationId, builder.build());

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

            PROGRESS_CURRENT = 10;
            builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
            notificationManager.notify(notificationId, builder.build());

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
                    ArrayList<Note> localNotes = noteManager.getAllNotes(null);
                    Log.d(DRIVE_TAG, "Old database found! Size notes: " + localNotes.size());

                    //get all attaches
                    final AttachManager attachManager = AttachManager.newInstance(context);
                    ArrayList<Attachment> localAttaches = attachManager.getAllAttaches();
                    Log.d(DRIVE_TAG, "size attaches test: " + localAttaches.size());

                    //check db version
                    SQLiteDatabase sqlDb = SQLiteDatabase.openDatabase
                            (DATABASE_PATH, null, SQLiteDatabase.OPEN_READONLY);
                    int localVersion = sqlDb.getVersion();

                    // backup local db
                    File from = new File(DATABASE_PATH);
                    File to = new File(DATABASE_PATH + "_backup");
                    copy(Uri.fromFile(from), to, context);

                    //cloud db
                    DownloadTask downloadDbTask = new DownloadTask(driveServiceHelper, DATABASE_PATH, dbId);
                    resDb = SyncManager.getSyncManager().callSyncBool(downloadDbTask);
                    resDb.get();

                    PROGRESS_CURRENT = 20;
                    builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                    notificationManager.notify(notificationId, builder.build());

                    //check db version
                    sqlDb = SQLiteDatabase.openDatabase
                            (DATABASE_PATH, null, SQLiteDatabase.OPEN_READONLY);
                    int cloudVersion = sqlDb.getVersion();

//                    boolean checkVersion;
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

                            //notification
                            if (PROGRESS_CURRENT < 80)
                                PROGRESS_CURRENT += 5;
                            builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                            notificationManager.notify(notificationId, builder.build());

                            Note cloudNote = noteManager.getNote(noteLocalId);

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

                                    if (fileId != null) {
                                        DeleteFileTask deleteFileTask = new DeleteFileTask(driveServiceHelper, fileId);
                                        Future<Boolean> resDelFile = SyncManager.getSyncManager().callSyncBool(deleteFileTask);
                                        //TODO check if needed or not
                                        delResults.add(resDelFile);
                                    }

                                    //delete attachment in db
                                    attachManager.delete(attach);
                                }
                                for (Future<Boolean> res : delResults) {
                                    res.get();
                                }

                                //delete in cloud db
                                if (cloudNote != null)
                                    noteManager.delete(cloudNote);

                                //delete local notes
                                localNotes.remove(i);

                                //notification
                                if (PROGRESS_CURRENT < 80)
                                    PROGRESS_CURRENT += 1;
                                builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                                notificationManager.notify(notificationId, builder.build());

                                continue;

                            }

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
                                long newNoteId = noteManager.addWithNewId(local);
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

                                    //notification
                                    if (PROGRESS_CURRENT < 80)
                                        PROGRESS_CURRENT += 1;
                                    builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                                    notificationManager.notify(notificationId, builder.build());
                                }
                                continue;
                            }

                            if (cloudNote != null) { //exist note satisfied conditions

                                //notification
                                if (PROGRESS_CURRENT < 80)
                                    PROGRESS_CURRENT += 1;
                                builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                                notificationManager.notify(notificationId, builder.build());

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

                                            File delAttach = new File(localAttaches.get(iAttach).getPath());
                                            delAttach.delete();

                                            localAttaches.remove(iAttach);

                                            //notification
                                            if (PROGRESS_CURRENT < 80)
                                                PROGRESS_CURRENT += 1;
                                            builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                                            notificationManager.notify(notificationId, builder.build());
                                        }
                                    }
                                }
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

                                //notification
                                if (PROGRESS_CURRENT < 80)
                                    PROGRESS_CURRENT += 1;
                                builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                                notificationManager.notify(notificationId, builder.build());

                            } else {
                                isMissing = true;
                            }
                        }

                        for (Future<Boolean> res : addResults) {
                            res.get();
                        }

                        //change notes into synced
                        ArrayList<Note> newCloudNotes = noteManager.getAllNotes(null);
                        for (Note note : newCloudNotes) {
                            note.setSync(NoteContract.NoteEntry.SYNCED);
                            noteManager.sync(note);
                        }

                        if (isMissing) {
                            SyncManager.getSyncManager().getMainThreadExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, getString(R.string.service_missing_attaches), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        //the place temporary moved to back
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
                        attachManager.create(localAttaches.get(iAttach));
                        Log.d(DRIVE_TAG, "push attach " + localAttaches.get(iAttach).getNote_id());
                    }
//                    }
                }
            } else { //if not created app folder
                setUpNotesForNewAppFolder();
            }

            PROGRESS_CURRENT = 90;
            builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
            notificationManager.notify(notificationId, builder.build());

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
                uploadAllAttachments(builder, PROGRESS_MAX, PROGRESS_CURRENT, notificationManager, notificationId, filesFolderId);
            }

            SyncManager.getSyncManager().getMainThreadExecutor().execute(command);

            builder.setProgress(0, 0, false)
                    .setContentText(getString(R.string.service_syncing_finished))
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(getString(R.string.service_syncing_finished)))
                    .setOngoing(false)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            notificationManager.notify(notificationId, builder.build());

            Thread.sleep(1000);

        } catch (InterruptedException e) {
            Log.e(DRIVE_TAG, "error interrupted " + e);
        } catch (ExecutionException e) {
            Log.e(DRIVE_TAG, "error execution " + e);
            e.printStackTrace();
            Throwable throwable = e.getCause();

            SyncManager.getSyncManager().getMainThreadExecutor().execute(command);
            notificationManager.cancel(notificationId);
            if (throwable instanceof UserRecoverableAuthIOException) {
                Intent errorIntent = ((UserRecoverableAuthIOException) throwable).getIntent();
                errorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.getApplicationContext().startActivity(errorIntent);
                SyncManager.getSyncManager().getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, getString(R.string.service_no_permission), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                File from = new File(DATABASE_PATH + "_backup");
                File to = new File(DATABASE_PATH);
                copy(Uri.fromFile(from), to, context);
                Log.d(DRIVE_TAG, "Backup db");
            }
        }
        finally {
            stopForeground(false);
        }
    }

    private void uploadAllAttachments(NotificationCompat.Builder builder, int PROGRESS_MAX, int PROGRESS_CURRENT, NotificationManagerCompat notificationManager, int notificationId, String filesFolderId) throws ExecutionException, InterruptedException {
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

            //notification
            if (PROGRESS_CURRENT < 100)
                PROGRESS_CURRENT += 1;
            builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
            notificationManager.notify(notificationId, builder.build());
        }
    }

    private void setUpNotesForNewAppFolder() {
        //get all notes before overwriting existing db
        final NoteManager noteManager = NoteManager.newInstance(context);
        ArrayList<Note> localNotes = noteManager.getAllNotes(null);
        Log.d(DRIVE_TAG, "Old database found! Size notes: " + localNotes.size());

        //get all attaches
        final AttachManager attachManager = AttachManager.newInstance(context);
        ArrayList<Attachment> localAttaches = attachManager.getAllAttaches();
        Log.d(DRIVE_TAG, "size attaches test: " + localAttaches.size());

        for (int i = localNotes.size() - 1; i >= 0; i--) {
            Note local = localNotes.get(i);
            long noteLocalId = local.getId();

            //check deleted
            int isDeleted = local.getDeleted();
            if (isDeleted == NoteContract.NoteEntry.DELETED) {

                //delete local notes
                localNotes.remove(i);
                noteManager.delete(local);

                continue;
            }
            noteManager.sync(local);
        }
    }

//    private Runnable command = new Runnable() {
//        @Override
//        public void run() {
////            Toast.makeText(context, "Finished syncing data!", Toast.LENGTH_LONG).show();
//            //just reload the screen
//            Intent startActivity = new Intent();
//
//            startActivity.setClass(context, MainActivity.class);
//            startActivity.setAction(REQUEST_RESTART);
//            startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//            context.startActivity(startActivity);
//        }
//    };

    private Runnable command = new Runnable() {
        @Override
        public void run() {
//            Toast.makeText(context, "Finished syncing data!", Toast.LENGTH_LONG).show();
            //just reload the screen
            Intent intent = new Intent(REQUEST_RESTART);
            LocalBroadcastManager.getInstance(SyncService.this).sendBroadcast(intent);
        }
    };

    private void updateLocal(Note local, Note cloudNote, ArrayList<Attachment> localAttaches, String filesFolderId) throws ExecutionException, InterruptedException {
        local.setTitle(cloudNote.getTitle());
        local.setContent(cloudNote.getContent());
        local.setDateModified(cloudNote.getDateModified());

        ArrayList<Attachment> cloudAttaches = AttachManager.newInstance(context).getAttach(local.getId(), NoteContract.AttachEntry.ANY_TYPE);

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

            if (fileId != null) {
                DownloadTask downloadAttachTask = new DownloadTask(driveServiceHelper, filesDir + "/" + fileName, fileId);
                Future<Boolean> resAttach = SyncManager.getSyncManager().callSyncBool(downloadAttachTask);
                addResults.add(resAttach);

                Log.d(DRIVE_TAG, "new file updated to local: " + fileName);
            }
        }

        for (Future<Boolean> res : addResults) {
            res.get();
        }
    }

    //upload new change into Drive
    private void updateCloud(long noteLocalId, ArrayList<Attachment> localAttaches, String filesFolderId) throws InterruptedException, ExecutionException {
        //need for deleting files in Drive
        ArrayList<Attachment> cloudAttaches = AttachManager.newInstance(context).getAttach(noteLocalId, NoteContract.AttachEntry.ANY_TYPE);

        //waiting for all thread finished
        ArrayList<Future<Boolean>> results = new ArrayList<>();

        //get filtered local attachments
        for (int i = 0; i < localAttaches.size(); i++) {
            Attachment attach = localAttaches.get(i);
            if (noteLocalId == attach.getNote_id()) {

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

            if (fileId != null) {
                DeleteFileTask deleteFileTask = new DeleteFileTask(driveServiceHelper, fileId);
                Future<Boolean> resDelFile = SyncManager.getSyncManager().callSyncBool(deleteFileTask);
                //TODO check if needed or not
                delResults.add(resDelFile);
            }
        }

        for (Future<Boolean> res : results) {
            res.get();
        }

        for (Future<Boolean> res : delResults) {
            res.get();
        }
    }

    private String getMIMEType(File child) {
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
