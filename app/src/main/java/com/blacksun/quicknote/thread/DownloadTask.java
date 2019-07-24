package com.blacksun.quicknote.thread;

import com.blacksun.quicknote.utils.DriveServiceHelper;

import java.io.IOException;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<Boolean> {
    private DriveServiceHelper driveServiceHelper;
    private String outputFilePath;
    private String attachId;

    public DownloadTask(DriveServiceHelper driveServiceHelper) {
        this.driveServiceHelper = driveServiceHelper;
    }

    @Override
    public Boolean call() throws IOException {
        driveServiceHelper.download(outputFilePath, attachId);
        return true;
    }
}
