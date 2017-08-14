package com.example.uberv.executorframeworkdemo.executors;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

public class SerialExecutor implements Executor {
    public static final String LOG_TAG = SerialExecutor.class.getSimpleName();

    private final Queue<Runnable> mTasks;
    private final Executor mExecutor;
    private Runnable mActiveTask;

    public SerialExecutor(Executor executor) {
        mExecutor = executor;
        mTasks = new ArrayDeque<>();
    }

    @Override
    public synchronized void execute(@NonNull final Runnable runnable) {
        Log.d(LOG_TAG, "SerialExecutor executing...");
        mTasks.add(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (mActiveTask == null) {
            scheduleNext();
        }
    }

    private void scheduleNext() {
        if ((mActiveTask = mTasks.poll()) != null) {
            mExecutor.execute(mActiveTask);
        }
    }
}
