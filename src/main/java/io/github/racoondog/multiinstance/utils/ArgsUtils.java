package io.github.racoondog.multiinstance.utils;

import io.github.racoondog.multiinstance.MultiInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class ArgsUtils {
    public static int indexOf(Object[] data, Object o) {
        return indexOf(data, o, 0, data.length);
    }

    /** Based on {@link java.util.ArrayList#indexOf(Object)} */
    public static int indexOf(Object[] data, Object o, int start, int end) {
        if (o == null) {
            for (int i = start; i < end; i++) {
                if (data[i] == null) return i;
            }
        } else {
            for (int i = start; i < end; i++) {
                if (o.equals(data[i])) return i;
            }
        }
        return -1;
    }

    @Nullable
    public static String getArg(String argName) {
        return getArgOrElse(argName, () -> null);
    }

    public static String getArgOrElse(String argName, Supplier<String> supplier) {
        int argIdx = indexOf(MultiInstance.LAUNCH_ARGS, argName);
        return argIdx == -1 || argIdx >= MultiInstance.LAUNCH_ARGS.length ? supplier.get() : MultiInstance.LAUNCH_ARGS[argIdx + 1];
    }

    public static boolean hasArg(String argName) {
        int argIdx = indexOf(MultiInstance.LAUNCH_ARGS, argName);
        return argIdx != -1;
    }

    public static void modifyArg(List<String> list, String token, String replace) {
        int idx = list.indexOf(token);
        if (idx != -1) {
            list.set(idx + 1, replace);
        } else {
            list.add(token);
            list.add(replace);
        }
    }
}
