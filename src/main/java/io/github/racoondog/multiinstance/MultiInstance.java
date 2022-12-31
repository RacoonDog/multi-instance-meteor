package io.github.racoondog.multiinstance;

import com.mojang.logging.LogUtils;
import io.github.racoondog.multiinstance.systems.QuickLaunchCommand;
import io.github.racoondog.multiinstance.utils.ArgsUtils;
import io.github.racoondog.multiinstance.utils.SwarmUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PostInit;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class MultiInstance extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final String[] LAUNCH_ARGS = FabricLoader.getInstance().getLaunchArguments(true);
    public static final List<String> JVM_OPTS = ManagementFactory.getRuntimeMXBean().getInputArguments();

    @Override
    public void onInitialize() {
        Commands.get().add(new QuickLaunchCommand());
    }

    @PostInit
    public static void postInit() {
        // Parse launch args
        if (ArgsUtils.hasArg("--meteor:deactivate")) {
            new ArrayList<>(Modules.get().getActive()).forEach(Module::toggle);
            Hud.get().active = false;
        }

        // After deactivate
        @Nullable String swarmMode = ArgsUtils.getArg("--meteor:swarmMode");
        if (swarmMode != null) {
            @Nullable String swarmIp = ArgsUtils.getArg("--meteor:swarmIp");
            int swarmPort = Integer.parseInt(ArgsUtils.getArg("--meteor:swarmPort"));

            SwarmUtils.configPort(swarmPort);

            if (swarmMode.equals("worker")) {
                SwarmUtils.configIp(swarmIp);
                SwarmUtils.beginWorker();
            } else if (swarmMode.equals("host")) SwarmUtils.beginHost();
        }
    }

    @Override
    public String getPackage() {
        return "io.github.racoondog.multiinstance";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("RacoonDog", "multi-instance-meteor");
    }
}
