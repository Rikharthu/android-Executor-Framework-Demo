package com.example.uberv.executorframeworkdemo.executors;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.Executor;

/**
 * An executor that runs the submitted task immediately in the caller's thread
 */
public class DirectExecutor implements Executor {
    public static final String LOG_TAG=DirectExecutor.class.getSimpleName();

    @Override
    public void execute(@NonNull Runnable runnable) {
        Log.d(LOG_TAG,"DirectExecutor executing...");
        // An executor can run the submitted task immediately in the caller's thread
        runnable.run();
    }
}
