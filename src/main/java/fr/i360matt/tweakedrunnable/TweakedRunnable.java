package fr.i360matt.tweakedrunnable;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public interface TweakedRunnable extends Runnable, Closeable {

    Map<TweakedRunnable, ScheduledExecutorService> executors = new HashMap<>();



    /**
     * Allows to initialize a ScheduledExecutorService with the desired number of threads.
     *
     * @param threads The number of simultaneous threads / executions.
     */
    default void setThreads (int threads) {
        if (!this.executors.containsKey(this)) {
            this.executors.put(this, Executors.newScheduledThreadPool(threads));
        }
    }

    /**
     * The main override method.
     *
     * It is semi-multi-threaded with a blocking function,
     * i.e. it is like an ordinary Runnable,
     * but we can stop all its executions with forceClose()
     */
    @Override
    default void run () {
        final CompletableFuture<?> future = new CompletableFuture<>();
        __initExec().execute(() -> {
            __runSameThread();
            future.complete(null);
        });
        future.join();
    }

    /**
     * Allows to bring the concept then/catch async of JavaScript.
     * The method is blocking.
     * The executions can be stopped with forceClose().
     *
     * @param then Then.
     * @param catcher Catch.
     */
    default void run (final Runnable then, final Runnable catcher) {
        final CompletableFuture<?> future = new CompletableFuture<>();
        __initExec().execute(() -> {
            try {
                this.body();
                future.complete(null);
                if (then != null)
                    then.run();
            } catch (final Throwable throwable) {
                future.complete(null);
                if (catcher != null)
                    catcher.run();
            }
        });
        future.join();
    }

    /**
     *
     * Allows to bring the concept then/catch async of JavaScript.
     *
     * The method is async.
     * Executions can be stopped with forceClose().
     *
     * @param then Then.
     * @param catcher Catch.
     * @return The Future of task.
     */
    default Future<?> runAsync (final Runnable then, final Runnable catcher) {
        return __initExec().submit(() -> {
            __runSameThread(then, catcher);
        });
    }

    /**
     * The method is async.
     * Executions can be stopped with forceClose().
     * @return The Future of task.
     */
    default Future<?> runAsync () {
        return __initExec().submit((Runnable) this::__runSameThread);
    }

    /**
     * Run the task after x ms.
     *
     * The method is async,
     * Executions can be cancelled by close() and can be stopped with forceClose().
     *
     * @param ms Timeout before execution.
     * @return The ScheduledFuture of task.
     */
    default ScheduledFuture<?> runAfter (int ms) {
        return __initExec().schedule((Runnable) this::__runSameThread, ms, TimeUnit.MILLISECONDS);
    }

    /**
     * Run the task every x ms.
     *
     * The method is async,
     * Executions can be cancelled by close() and can be stopped with forceClose().
     *
     * @param ms The periode of execution.
     * @return The ScheduledFuture of task.
     */
    default ScheduledFuture<?> runRepeated (int ms) {
        return runRepeated(0, ms);
    }

    /**
     * Run the task every x ms, after x ms.
     *
     * The method is async,
     * Executions can be cancelled by close() and can be stopped with forceClose().
     *
     * @param wait The time before starting repeated-task.
     * @param ms The periode of execution.
     * @return The ScheudledFuture of task.
     */
    default ScheduledFuture<?> runRepeated (int wait, int ms) {
        return __initExec().scheduleAtFixedRate(() -> __runSameThread(null, null), wait, ms, TimeUnit.MILLISECONDS);
    }

    /**
     * Run legacy lambda.
     */
    default void __runSameThread () {
        try {
            body();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Run legacy lambda.
     * @param then Then.
     * @param catcher Catcher.
     */
    default void __runSameThread (final Runnable then, final Runnable catcher) {
        try {
            this.body();
            if (then != null)
                then.run();
        } catch (final Throwable throwable) {
            if (catcher != null)
                catcher.run();
        }
    }

    /**
     * Create or get ScheduledExecutor with 1 thread.
     * @return ScheduledExecutor.
     */
    default ScheduledExecutorService __initExec () {
        ScheduledExecutorService ex;
        if ((ex = executors.get(this)) == null) {
            ex = Executors.newScheduledThreadPool(1);
            executors.put(this, ex);
        }
        return ex;
    }

    /**
     * Close new tasks, while letting current tasks run.
     */
    @Override
    default void close () {
        ScheduledExecutorService ex;
        if ((ex = executors.get(this)) != null) {
            ex.shutdown();
        }
    }

    /**
     * Close new tasks as well as running tasks.
     */
    default void forceClose () {
        ScheduledExecutorService ex;
        if ((ex = executors.get(this)) != null) {
            ex.shutdownNow();
        }
    }

    /**
     * The content of the task, which traditionally should be in run().
     * @throws Exception Catch any exceptions.
     */
    void body () throws Exception;








    static TweakedRunnable create (TweakedRunnable tweakedRunnable) {
        return tweakedRunnable;
        // just a shortcut
    }

}
