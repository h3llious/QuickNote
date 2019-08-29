package com.blacksun.quicknote.thread;

import com.blacksun.quicknote.utils.DriveServiceHelper;

import java.util.concurrent.Callable;

public class DeleteFileTask implements Callable<Boolean> {
    private DriveServiceHelper driveServiceHelper;
    private String id;

    DeleteFileTask(DriveServiceHelper driveServiceHelper, String id) {
        this.driveServiceHelper = driveServiceHelper;
        this.id = id;
    }

    @Override
    public Boolean call() throws Exception {
        driveServiceHelper.deleteFile(id);
        return true;
    }
}
