package io.github.racoondog.multiinstance.utils;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ArgsUtils {
    public static final String[] args = FabricLoader.getInstance().getLaunchArguments(true);

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
        int argIdx = indexOf(args, argName);
        return argIdx == -1 || argIdx >= args.length ? supplier.get() : args[argIdx + 1];
    }

    public static boolean hasArg(String argName) {
        int argIdx = indexOf(args, argName);
        return argIdx != -1;
    }
}
