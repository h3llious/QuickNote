package com.blacksun.quicknote.thread;

import android.telecom.Call;

import com.blacksun.quicknote.models.DriveFileHolder;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SyncManager {
    private final ThreadPoolExecutor syncThreadPool;
    private final BlockingQueue<Runnable> syncWorkQueue;

    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 50;

    private static SyncManager syncManager = null;
    private static MainThreadExecutor handler;

    static {
        syncManager = new SyncManager();
        handler = new MainThreadExecutor();
    }

    public static final String DOWN_DATA = "down";
    public static final String UP_DATA = "up";

    private SyncManager(){
        syncWorkQueue = new LinkedBlockingQueue<Runnable>();

        syncThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
                KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, syncWorkQueue);
    }

    public static SyncManager getSyncManager(){
        return syncManager;
    }

    public void runSync(Runnable task){
        syncThreadPool.execute(task);
    }

    public Future<String> callSyncString(Callable<String> task){
        return syncThreadPool.submit(task);
    }

    public Future<Boolean> callSyncBool(Callable<Boolean> task){
        return syncThreadPool.submit(task);
    }

    public Future<ArrayList<DriveFileHolder>> callSyncArray(Callable<ArrayList<DriveFileHolder>> task){
        return syncThreadPool.submit(task);
    }

    //to runs task on main thread from background thread
    public MainThreadExecutor getMainThreadExecutor(){
        return handler;
    }
}
