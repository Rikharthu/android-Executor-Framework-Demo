package com.example.uberv.executorframeworkdemo.executors;

import android.os.Message;
import android.util.Log;

import com.example.uberv.executorframeworkdemo.Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyThreadPoolManager {
    public static final String LOG_TAG = MyThreadPoolManager.class.getSimpleName();

    private static MyThreadPoolManager sInstance = null;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    private final ExecutorService mExecutorService;
    private final BlockingQueue<Runnable> mTaskQueue;
    private List<Future> mRunningTaskList;
    // Keep a weak reference to the Activity
    private WeakReference<UiThreadCallback> uiThreadCallbackWeakReference;

    // The class is used as a singleton
    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        sInstance = new MyThreadPoolManager();
    }

    // Made constructor private to avoid the class being initiated from outside
    private MyThreadPoolManager() {
        // initialize a queue for the thread pool. New tasks will be added to this queue
        mTaskQueue = new LinkedBlockingQueue<Runnable>();
        mRunningTaskList = new ArrayList<>();
        mExecutorService = new ThreadPoolExecutor(
                NUMBER_OF_CORES,
                NUMBER_OF_CORES * 2,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mTaskQueue,
                new BackgroundThreadFactory());
    }

    public static MyThreadPoolManager getsInstance() {
        return sInstance;
    }

    // Keep a weak reference to the UI thread, so we can send messages to the UI thread
    public void setUiThreadCallback(UiThreadCallback uiThreadCallback) {
        this.uiThreadCallbackWeakReference = new WeakReference<>(uiThreadCallback);
    }

    private static class BackgroundThreadFactory implements ThreadFactory {

        private static int sTag = 1;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("CustomThread" + sTag);
            thread.setPriority(Thread.NORM_PRIORITY + 1);

            // A exception handler is created to log the exception from threads
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    Log.e(LOG_TAG, thread.getName() + " encountered an error: " + ex.getMessage());
                }
            });
            return thread;
        }
    }

    /* Remove all tasks in the queue and stop all running threads
 * Notify UI thread about the cancellation
 */
    public void cancelAllTasks() {
        synchronized (this) {
            mTaskQueue.clear();
            for (Future task : mRunningTaskList) {
                if (!task.isDone()) {
                    task.cancel(true);
                }
            }
            mRunningTaskList.clear();
        }
        sendMessageToUiThread(Util.createMessage(Util.MESSAGE_ID, "All tasks in the thread pool are cancelled"));
    }

    // Add a callable to the queue, which will be executed by the next available thread in the pool

    public void addCallable(Callable callable) {
        Future future = mExecutorService.submit(callable);
        mRunningTaskList.add(future);
    }

    // Pass the message to the UI thread
    public void sendMessageToUiThread(Message message) {
        if (uiThreadCallbackWeakReference != null && uiThreadCallbackWeakReference.get() != null) {
            uiThreadCallbackWeakReference.get().publishToUiThread(message);
        }
    }

    public interface UiThreadCallback {
        void publishToUiThread(Message message);
    }
}
