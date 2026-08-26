package com.anticheat.web;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Bukkit 主线程切换桥。所有从 Web 线程访问 Bukkit API 的逻辑必须经过本类。
 * <ul>
 *   <li>{@link #syncSupply}：同步读，等待结果最长 5 秒</li>
 *   <li>{@link #syncRun}：异步写，fire-and-forget 回到主线程</li>
 * </ul>
 */
public final class BukkitBridge {

    private static final long DEFAULT_TIMEOUT_MS = 5_000L;

    private BukkitBridge() {
    }

    /**
     * 在 Bukkit 主线程同步执行读操作并返回结果。
     * 若已在主线程则直接调用；否则通过 callSyncMethod 提交并阻塞等待 5s。
     *
     * @throws IllegalStateException 主线程调用超时或被中断
     */
    public static <T> T syncSupply(Plugin plugin, Supplier<T> supplier) {
        if (Bukkit.isPrimaryThread()) {
            return supplier.get();
        }
        Future<T> future = Bukkit.getScheduler().callSyncMethod(plugin, new CallableSupplier<>(supplier));
        try {
            return future.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("Bukkit 主线程调用超时（5s）");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Bukkit 主线程调用失败", cause);
        }
    }

    /**
     * 在 Bukkit 主线程异步执行写操作（fire-and-forget）。
     * 若已在主线程则直接执行；否则 runTask 调度到下一 tick。
     */
    public static void syncRun(Plugin plugin, Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    /** 包装 Supplier 为 Callable，简化与 callSyncMethod 的对接。 */
    private static final class CallableSupplier<T> implements Callable<T> {
        private final Supplier<T> supplier;

        CallableSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T call() {
            return supplier.get();
        }
    }
}
