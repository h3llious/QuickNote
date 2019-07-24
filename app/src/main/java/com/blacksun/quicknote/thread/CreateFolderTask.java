package com.blacksun.quicknote.thread;

import com.blacksun.quicknote.utils.DriveServiceHelper;

import java.util.concurrent.Callable;

import static com.blacksun.quicknote.utils.UtilHelper.FOLDER_NAME;

public class CreateFolderTask implements Callable<String> {
    private DriveServiceHelper driveServiceHelper;

    public CreateFolderTask(DriveServiceHelper driveServiceHelper){
        this.driveServiceHelper = driveServiceHelper;
    }

    @Override
    public String call() throws Exception {
        return driveServiceHelper.createFolder(FOLDER_NAME, null);
    }
}
