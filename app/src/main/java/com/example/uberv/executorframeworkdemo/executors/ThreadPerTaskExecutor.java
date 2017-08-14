package com.example.uberv.executorframeworkdemo.executors;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.Executor;

/**
 * Executor that executes tasks in a new thread for each task
 */
public class ThreadPerTaskExecutor implements Executor{
    public static final String LOG_TAG=ThreadPerTaskExecutor.class.getSimpleName();

    @Override
    public void execute(@NonNull Runnable runnable) {
        Log.d(LOG_TAG,"ThreadPerTaskExecutor executing...");
        new Thread(runnable).start();
    }
}
