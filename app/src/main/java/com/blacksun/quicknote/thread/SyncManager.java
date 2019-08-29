package com.blacksun.quicknote.thread;

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
    public static final String SYNC_DATA = "sync";


    private SyncManager(){
        BlockingQueue<Runnable> syncWorkQueue = new LinkedBlockingQueue<Runnable>();

        syncThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
                KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, syncWorkQueue);
    }

    public static SyncManager getSyncManager(){
        return syncManager;
    }

    public void runSync(Runnable task){
        syncThreadPool.execute(task);
    }

    Future<String> callSyncString(Callable<String> task){
        return syncThreadPool.submit(task);
    }

    Future<Boolean> callSyncBool(Callable<Boolean> task){
        return syncThreadPool.submit(task);
    }

    Future<ArrayList<DriveFileHolder>> callSyncArray(Callable<ArrayList<DriveFileHolder>> task){
        return syncThreadPool.submit(task);
    }

    //to runs task on main thread from background thread
    MainThreadExecutor getMainThreadExecutor(){
        return handler;
    }
}
