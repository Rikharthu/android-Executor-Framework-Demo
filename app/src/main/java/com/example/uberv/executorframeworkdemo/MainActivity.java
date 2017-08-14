package com.example.uberv.executorframeworkdemo;

import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.uberv.executorframeworkdemo.executors.CustomCallable;
import com.example.uberv.executorframeworkdemo.executors.DirectExecutor;
import com.example.uberv.executorframeworkdemo.executors.MyThreadPoolManager;
import com.example.uberv.executorframeworkdemo.executors.SerialExecutor;
import com.example.uberv.executorframeworkdemo.executors.ThreadPerTaskExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity
        implements MyThreadPoolManager.UiThreadCallback {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final int DEFAULT_THREAD_POOL_SIZE = 4;

    @BindView(R.id.btn_run_executor)
    Button mRunExecutorBtn;
    @BindView(R.id.tv_output)
    TextView mOutputTv;

    // Executor is just an interface that executes passed runnables
    Executor mDirectExecutor;
    Executor mThreadPerTaskExecutor;
    Executor mSerialExecutor;
    Executor mLambdaExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            log("Executing lambda executor");
        }
    };
    private MyThreadPoolManager mMyThreadPoolManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mOutputTv.setText(getString(R.string.general_info));
        mRunExecutorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRunExecutor();
            }
        });

        mMyThreadPoolManager = MyThreadPoolManager.getsInstance();
        mMyThreadPoolManager.setUiThreadCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDirectExecutor = new DirectExecutor();
        mThreadPerTaskExecutor = new ThreadPerTaskExecutor();
        mSerialExecutor = new SerialExecutor(mThreadPerTaskExecutor);
    }

    @OnClick(R.id.btn_send_tasks)
    void sendTasksToMyThreadPool() {
        clearLog();
        for (int i = 0; i < 16; i++) {
            CustomCallable callable = new CustomCallable();
            callable.setCustomThreadPoolManager(mMyThreadPoolManager);
            mMyThreadPoolManager.addCallable(callable);
        }
    }

    @OnClick(R.id.btn_run_executor_service)
    void onExecutorServiceDemo() {
        clearLog();

        // You can use 'Executors' to create built-in Thread pool executors
        // Creates a thread pool with a fixed number of threads in the pool
        ExecutorService fourCoreThreadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
        // creates a new thread when there is a task in the queue
        // When there is no tasks in the queue for 60 seconds, the idle threads will be terminated
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        // creates a thread pool with only one thread
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

        // Since ExecutorService implements Executor, you still can post runnables to it:
        log("Starting FixedThreadPool executor service...");
        log("Core count: " + DEFAULT_THREAD_POOL_SIZE);
        for (int i = 0; i < 12; i++) {
            fourCoreThreadPool.execute(createDelayedLogRunnable(1000, "From fixed four-core thread pool executor, i=" + (i + 1)));
        }

        // Future can be used to retrieve the result from the callable by calling Future.get()
        // Callable is an interface that is similar to Runnable, but can return result
        Future future = cachedThreadPool.submit(new Callable() {
            @Override
            public Object call() throws Exception {
//                callBlockingFunction();
                delay(3000);
                return "Hello, World!";
            }
        });
    }


    private void onRunExecutor() {
        clearLog();
        log("Starting 5 direct executors");
        // This will block UI, since they run on main thread
        for (int i = 0; i < 5; i++) {
            mDirectExecutor.execute(createDelayedLogRunnable(300, "From Direct Executor: " + i));
        }
        // Each runs on it's own separate thread
        for (int i = 0; i < 5; i++) {
            mThreadPerTaskExecutor.execute(createDelayedLogRunnable(300, "From Thread-Per-Task Executor: " + i));
        }
        // Executors that queues tasks and uses another executor
        for (int i = 0; i < 10; i++) {
            mSerialExecutor.execute(createDelayedLogRunnable(300, "From Serial Executor (Uses Thread-Per-Task Executor): " + i));
        }
    }

    private void clearLog() {
        mOutputTv.setText("");
    }

    private void log(String message) {
        mOutputTv.setText(mOutputTv.getText() + "\n" + message);
    }

    private ExecutorService createThreadPoolExecutor() {
        // Get available processor cores on the phone
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();

        ExecutorService threadPoolExecutor = new ThreadPoolExecutor(
                NUMBER_OF_CORES,        // number of threads to keep in the pool (=number of cores)
                NUMBER_OF_CORES * 2,    // maximum allowed threads in the pool
                KEEP_ALIVE_TIME,        // time to wait before killing idle exceeding threads
                KEEP_ALIVE_TIME_UNIT,
                taskQueue,              // the queue to use for holding tasks submitted by execute() method
                new BackgroundThreadFactory() // OPTIONAL. The factory to use when creating new threads
        );

        return threadPoolExecutor;
    }

    @Override
    public void publishToUiThread(Message message) {
        log(message.getData().getString(Util.MESSAGE_BODY));
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

    private Runnable createDelayedLogRunnable(final long delay, final String message) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log(message);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void delay(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
