package com.blacksun.quicknote.thread;

import com.blacksun.quicknote.utils.DriveServiceHelper;

import java.util.concurrent.Callable;

import static com.blacksun.quicknote.utils.UtilHelper.FOLDER_NAME;

public class CreateFolderTask implements Callable<String> {
    private DriveServiceHelper driveServiceHelper;
    private String name;

    public CreateFolderTask(DriveServiceHelper driveServiceHelper, String name){
        this.driveServiceHelper = driveServiceHelper;
        this.name = name;
    }

    @Override
    public String call() throws Exception {
        return driveServiceHelper.createFolder(name, null);
    }
}
