package fr.i360matt.tweakedrunnable;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class TweakedRunnable implements Closeable {

    private static final Set<TweakedRunnable> services = new HashSet<>();
    private static boolean hideError = true;

    public static void hideError (final boolean stat) {
        hideError = stat;
    }

    public static TweakedRunnable create (final CustomRunnable runnable) {
        return new TweakedRunnable(runnable);
    }

    public static void closeAll () {
        services.forEach(TweakedRunnable::close);
    }
    public static void forceCloseAll () {
        services.forEach(TweakedRunnable::close);
    }


    private final ScheduledThreadPoolExecutor executor;
    private final CustomRunnable runnable;

    public TweakedRunnable (CustomRunnable miaou) {
        this.runnable = miaou;

        this.executor = new ScheduledThreadPoolExecutor(1);
        services.add(this);
    }

    /**
     * Allows to define the desired number of threads.
     *
     * @param threads The number of simultaneous threads / executions.
     */
    public TweakedRunnable setThreads (final int threads) {
        if (hideError && this.isDisabled())
            return this;

        executor.setCorePoolSize(threads);
        return this;
    }

    /**
     * Run without blocking.
     */
    protected void runInSameThread () {
        try {
            if (hideError && this.isDisabled())
                return;
            this.runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Run without blocking.
     *
     * @param then Then.
     * @param catcher Catch.
     */
    protected void runInSameThread (@NotNull final Runnable then, @NotNull final Runnable catcher) {
        try {
            if (hideError && this.isDisabled())
                return;
            this.runnable.run();
            then.run();
        } catch (final Throwable throwable) {
            catcher.run();
        }
    }


    // _________________________________________________________________________________________________________________

    public boolean isDisabled () {
        return (this.executor == null) || (this.executor.isShutdown());
    }

    /**
     * It is semi-multi-threaded with a blocking function,
     * i.e. it is like an ordinary Runnable,
     * but we can stop all its executions with forceClose()
     */
    public TweakedRunnable run () {
        if (hideError && this.isDisabled())
            return this;
        final CompletableFuture<?> future = new CompletableFuture<>();
        executor.execute(() -> {
            this.runInSameThread();
            future.complete(null);
        });
        future.join();

        return this;
    }

    /**
     * Allows to bring the concept then/catch async of JavaScript.
     * The method is blocking.
     * The executions can be stopped with forceClose().
     *
     * @param then Then.
     * @param catcher Catch.
     */
    public TweakedRunnable run (@NotNull final Runnable then, @NotNull final Runnable catcher) {
        if (hideError && this.isDisabled())
            return this;
        final CompletableFuture<?> future = new CompletableFuture<>();
        this.executor.execute(() -> {
            try {
                this.runnable.run();
                future.complete(null);
                then.run();
            } catch (final Throwable throwable) {
                future.complete(null);
                catcher.run();
            }
        });
        future.join();
        return this;
    }

    /**
     * The method is async.
     * Executions can be stopped with forceClose().
     * @return The Future of task.
     */
    public Future<?> runAsync () {
        if (hideError && this.isDisabled())
            return null;
        return this.executor.submit((Runnable) this::runInSameThread);
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
    public Future<?> runAsync (final Runnable then, final Runnable catcher) {
        if (hideError && this.isDisabled())
            return null;
        return this.executor.submit(() -> {
            runInSameThread(then, catcher);
        });
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
    public ScheduledFuture<?> runRepeated (int ms) {
        if (hideError && this.isDisabled())
            return null;
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
    public ScheduledFuture<?> runRepeated (int wait, int ms) {
        if (hideError && this.isDisabled())
            return null;
        return this.executor.scheduleAtFixedRate(this::runInSameThread, wait, ms, TimeUnit.MILLISECONDS);
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
    public ScheduledFuture<?> runAfter (int ms) {
        if (hideError && this.isDisabled())
            return null;
        return this.executor.schedule((Runnable) this::runInSameThread, ms, TimeUnit.MILLISECONDS);
    }

    /**
     * Close new tasks as well as running tasks.
     */
    public void forceClose () {
        if (this.executor != null)
            this.executor.shutdownNow();
        services.remove(this);
    }

    /**
     * Close new tasks, while letting current tasks run.
     */
    @Override
    public void close () {
        if (this.executor != null)
            this.executor.shutdown();
        services.remove(this);
    }
}
