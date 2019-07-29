package com.blacksun.quicknote.thread;

import com.blacksun.quicknote.utils.DriveServiceHelper;

import java.util.concurrent.Callable;

public class SearchSingleTask implements Callable<String> {
    private DriveServiceHelper driveServiceHelper;
    private String mimeType;
    private String folderName;
    private String parentId;

    public SearchSingleTask(DriveServiceHelper driveServiceHelper, String mimeType, String folderName, String parentId){
        this.driveServiceHelper = driveServiceHelper;
        this.mimeType = mimeType;
        this.folderName = folderName;
        this.parentId = parentId;
    }

    @Override
    public String call() throws Exception {
        //folder files
        return driveServiceHelper.searchSingle(mimeType, folderName, parentId);
    }
}

