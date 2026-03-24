package com.Guayand0.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SchedulerCompat {

    private final JavaPlugin plugin;
    private final boolean folia;
    private final Object globalScheduler;
    private final Object regionScheduler;
    private final Object asyncScheduler;

    public SchedulerCompat(JavaPlugin plugin) {
        this.plugin = plugin;

        boolean foliaDetected = false;
        Object global = null;
        Object region = null;
        Object async = null;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            Object server = Bukkit.getServer();
            Method getGlobalScheduler = server.getClass().getMethod("getGlobalRegionScheduler");
            Method getRegionScheduler = server.getClass().getMethod("getRegionScheduler");
            Method getAsyncScheduler = server.getClass().getMethod("getAsyncScheduler");
            global = getGlobalScheduler.invoke(server);
            region = getRegionScheduler.invoke(server);
            async = getAsyncScheduler.invoke(server);
            foliaDetected = true;
        } catch (Exception ignored) {
            foliaDetected = false;
            global = null;
            region = null;
            async = null;
        }

        this.folia = foliaDetected;
        this.globalScheduler = global;
        this.regionScheduler = region;
        this.asyncScheduler = async;
    }

    public boolean isFolia() {
        return folia;
    }

    public Object runGlobal(Runnable runnable) {
        if (!folia) {
            return Bukkit.getScheduler().runTask(plugin, runnable);
        }
        try {
            invokeCompatible(globalScheduler, "execute", plugin, runnable);
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Folia global scheduler execute failed", e);
        }
    }

    public Object runGlobalLater(Runnable runnable, long delayTicks) {
        if (!folia) {
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
        try {
            return invokeCompatible(globalScheduler, "runDelayed", plugin, (Consumer<Object>) task -> runnable.run(), Math.max(0L, delayTicks));
        } catch (Exception e) {
            if (delayTicks <= 0L) {
                runGlobal(runnable);
                return null;
            }
            try {
                return invokeCompatible(
                        asyncScheduler,
                        "runDelayed",
                        plugin,
                        (Consumer<Object>) task -> runGlobal(runnable),
                        Math.max(1L, delayTicks * 50L),
                        TimeUnit.MILLISECONDS
                );
            } catch (Exception ex) {
                throw new IllegalStateException("Folia delayed scheduler failed", ex);
            }
        }
    }

    public Object runGlobalTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (!folia) {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        }
        try {
            return invokeCompatible(
                    globalScheduler,
                    "runAtFixedRate",
                    plugin,
                    (Consumer<Object>) task -> runnable.run(),
                    Math.max(0L, delayTicks),
                    Math.max(1L, periodTicks)
            );
        } catch (Exception e) {
            try {
                return invokeCompatible(
                        asyncScheduler,
                        "runAtFixedRate",
                        plugin,
                        (Consumer<Object>) task -> runGlobal(runnable),
                        Math.max(0L, delayTicks * 50L),
                        Math.max(1L, periodTicks * 50L),
                        TimeUnit.MILLISECONDS
                );
            } catch (Exception ignored) {
                final CompatTask compatTask = new CompatTask();
                final long safePeriod = Math.max(1L, periodTicks);
                final Runnable[] loop = new Runnable[1];
                loop[0] = () -> {
                    if (compatTask.cancelled) return;
                    runnable.run();
                    if (compatTask.cancelled) return;
                    compatTask.delegate = runGlobalLater(loop[0], safePeriod);
                };
                compatTask.delegate = runGlobalLater(loop[0], Math.max(0L, delayTicks));
                return compatTask;
            }
        }
    }

    public Object runAsync(Runnable runnable) {
        if (!folia) {
            return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
        try {
            return invokeCompatible(asyncScheduler, "runNow", plugin, (Consumer<Object>) task -> runnable.run());
        } catch (Exception e) {
            throw new IllegalStateException("Folia async scheduler runNow failed", e);
        }
    }

    public Object runAtPlayer(Player player, Runnable runnable) {
        if (player == null) return null;
        if (!folia) {
            return Bukkit.getScheduler().runTask(plugin, runnable);
        }
        try {
            Object entityScheduler = invokeCompatible(player, "getScheduler");
            return invokeCompatible(entityScheduler, "run", plugin, (Consumer<Object>) task -> runnable.run(), (Runnable) () -> {});
        } catch (Exception e) {
            try {
                return runAtPlayerRegion(player, runnable);
            } catch (Exception ex) {
                throw new IllegalStateException("Folia player scheduler failed", ex);
            }
        }
    }

    public Object runAtPlayerLater(Player player, Runnable runnable, long delayTicks) {
        if (player == null) return null;
        if (!folia) {
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
        try {
            Object entityScheduler = invokeCompatible(player, "getScheduler");
            return invokeCompatible(
                    entityScheduler,
                    "runDelayed",
                    plugin,
                    (Consumer<Object>) task -> runnable.run(),
                    (Runnable) () -> {},
                    Math.max(0L, delayTicks)
            );
        } catch (Exception e) {
            try {
                return runAtPlayerRegionLater(player, runnable, delayTicks);
            } catch (Exception ex) {
                throw new IllegalStateException("Folia player delayed scheduler failed", ex);
            }
        }
    }

    public <T> T callSync(Callable<T> task, long timeout, TimeUnit unit) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        runGlobal(() -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future.get(timeout, unit);
    }

    public void cancelTask(Object task) {
        if (task == null) return;

        if (task instanceof CompatTask) {
            CompatTask compat = (CompatTask) task;
            compat.cancelled = true;
            if (compat.delegate != null && compat.delegate != compat) {
                cancelTask(compat.delegate);
            }
            return;
        }

        if (task instanceof BukkitTask) {
            ((BukkitTask) task).cancel();
            return;
        }

        try {
            Method cancel = task.getClass().getMethod("cancel");
            cancel.invoke(task);
        } catch (Exception ignored) {
            // Ignore.
        }
    }

    private Object invokeCompatible(Object target, String methodName, Object... args) throws Exception {
        if (target == null) {
            throw new NoSuchMethodException("Target is null for method " + methodName);
        }

        Method method = findCompatibleMethod(target.getClass(), methodName, args);
        if (method == null) {
            throw new NoSuchMethodException("No compatible method '" + methodName + "' found on " + target.getClass().getName());
        }
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Method findCompatibleMethod(Class<?> type, String name, Object[] args) {
        Method found = findCompatibleIn(type.getMethods(), name, args);
        if (found != null) return found;
        return findCompatibleIn(type.getDeclaredMethods(), name, args);
    }

    private Method findCompatibleIn(Method[] methods, String name, Object[] args) {
        for (Method method : methods) {
            if (!method.getName().equals(name)) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length != args.length) continue;
            boolean compatible = true;
            for (int i = 0; i < params.length; i++) {
                if (!isAssignable(params[i], args[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return null;
    }

    private boolean isAssignable(Class<?> paramType, Object arg) {
        if (arg == null) {
            return !paramType.isPrimitive();
        }
        Class<?> argType = arg.getClass();
        if (paramType.isPrimitive()) {
            if (paramType == long.class) return argType == Long.class;
            if (paramType == int.class) return argType == Integer.class;
            if (paramType == boolean.class) return argType == Boolean.class;
            if (paramType == double.class) return argType == Double.class;
            if (paramType == float.class) return argType == Float.class;
            if (paramType == short.class) return argType == Short.class;
            if (paramType == byte.class) return argType == Byte.class;
            if (paramType == char.class) return argType == Character.class;
            return false;
        }
        return paramType.isAssignableFrom(argType);
    }

    private static class CompatTask {
        private volatile boolean cancelled = false;
        private volatile Object delegate;
    }

    private Object runAtPlayerRegion(Player player, Runnable runnable) throws Exception {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return runGlobal(runnable);
        }
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        return invokeCompatible(regionScheduler, "execute", plugin, world, chunkX, chunkZ, runnable);
    }

    private Object runAtPlayerRegionLater(Player player, Runnable runnable, long delayTicks) throws Exception {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return runGlobalLater(runnable, delayTicks);
        }
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        return invokeCompatible(
                regionScheduler,
                "runDelayed",
                plugin,
                world,
                chunkX,
                chunkZ,
                (Consumer<Object>) task -> runnable.run(),
                Math.max(0L, delayTicks)
        );
    }
}
