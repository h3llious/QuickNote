package com.blacksun.quicknote.thread;

import com.blacksun.quicknote.utils.DriveServiceHelper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class UploadTask implements Callable<Boolean> {
    private DriveServiceHelper driveServiceHelper;
    private File file;
    private String mimeType;
    private String parentId;

    public UploadTask(DriveServiceHelper driveServiceHelper, File file, String mimeType, String parentId) {
        this.driveServiceHelper = driveServiceHelper;
        this.file = file;
        this.mimeType = mimeType;
        this.parentId = parentId;
    }

    @Override
    public Boolean call() throws IOException {
        driveServiceHelper.upload(file, mimeType, parentId);
        return true;
    }
}
