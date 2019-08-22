package com.blacksun.quicknote.thread;

import com.blacksun.quicknote.models.DriveFileHolder;
import com.blacksun.quicknote.utils.DriveServiceHelper;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static com.blacksun.quicknote.utils.DatabaseHelper.DATABASE_NAME;
import static com.blacksun.quicknote.utils.DriveServiceHelper.DRIVE_TAG;
import static com.blacksun.quicknote.utils.UtilHelper.DATABASE_PATH;
import static com.blacksun.quicknote.utils.UtilHelper.FOLDER_NAME;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_DB;
import static com.blacksun.quicknote.utils.UtilHelper.MIME_TYPE_FOLDER;

public class SearchTask implements Callable<ArrayList<DriveFileHolder>>{
    private DriveServiceHelper driveServiceHelper;
    private String mimeType;
    private String folderName;
    private String parentId;

    public SearchTask(DriveServiceHelper driveServiceHelper, String mimeType, String folderName, String parentId){
        this.driveServiceHelper = driveServiceHelper;
        this.mimeType = mimeType;
        this.folderName = folderName;
        this.parentId = parentId;
    }

    @Override
    public ArrayList<DriveFileHolder> call() throws Exception {
                    //folder files
            return driveServiceHelper.search(mimeType, folderName, parentId);
    }
}

//public class SearchTask implements Runnable {
//    @Override
//    public void run() {
//        try {
//            //database
//            ArrayList<DriveFileHolder> database = driveServiceHelper.search(MIME_TYPE_DB, DATABASE_NAME, null);
//            if (database.size() >= 1) {
//                String databaseId = database.get(0).getId();
//
//                driveServiceHelper.download(DATABASE_PATH, databaseId);
//                Log.d(DRIVE_TAG, "Database downloaded " + databaseId);
//            } else {
//                Log.e(DRIVE_TAG, "Cannot download database");
//            }
//
//
//            //folder files
//            ArrayList<DriveFileHolder> filesFolder = driveServiceHelper.search(MIME_TYPE_FOLDER, FOLDER_NAME, null);
//            String folderId = null;
//            if (filesFolder.size() >= 1) {
//                folderId = filesFolder.get(0).getId();
//            } else {
//                Log.e(DRIVE_TAG, "Cannot get \"files\" folder");
//            }
//
//            //attachments
//            if (folderId != null) {
//                ArrayList<DriveFileHolder> attachments = driveServiceHelper.search(null, null, folderId);
//                String filesDir = getFilesDir().getAbsolutePath();
//                for (DriveFileHolder attach : attachments) {
//                    String attachId = attach.getId();
//
//                    driveServiceHelper.download(filesDir+"/"+attach.getName(), attachId);
//
//                }
//            } else {
//                Log.e(DRIVE_TAG, "No attachment");
//            }
//
//        } catch (UserRecoverableAuthIOException e) {
//            Log.e(DRIVE_TAG, "Error " + e.getMessage());
//            e.printStackTrace();
//            startActivityForResult(e.getIntent(), 6);
//
//        } catch (IOException e) {
//            Log.e(DRIVE_TAG, "Error " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}
