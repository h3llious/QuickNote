package com.blacksun.quicknote.thread;

import com.blacksun.quicknote.utils.DriveServiceHelper;

import java.util.concurrent.Callable;

import static com.blacksun.quicknote.utils.UtilHelper.FOLDER_NAME;

public class CreateFolderTask implements Callable<String> {
    private DriveServiceHelper driveServiceHelper;
    private String name;
    private String parentId;

    public CreateFolderTask(DriveServiceHelper driveServiceHelper, String name, String parentId){
        this.driveServiceHelper = driveServiceHelper;
        this.name = name;
        this.parentId = parentId;
    }

    @Override
    public String call() throws Exception {
        return driveServiceHelper.createFolder(name, parentId);
    }
}
