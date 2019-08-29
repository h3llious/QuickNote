package com.blacksun.quicknote.thread;

import com.blacksun.quicknote.models.DriveFileHolder;
import com.blacksun.quicknote.utils.DriveServiceHelper;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class SearchTask implements Callable<ArrayList<DriveFileHolder>>{
    private DriveServiceHelper driveServiceHelper;
    private String mimeType;
    private String folderName;
    private String parentId;

    SearchTask(DriveServiceHelper driveServiceHelper, String mimeType, String folderName, String parentId){
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