package io.github.racoondog.multiinstance;

import com.mojang.logging.LogUtils;
import io.github.racoondog.launchargsapi.api.ArgsListener;
import io.github.racoondog.multiinstance.systems.BenchmarkCommand;
import io.github.racoondog.multiinstance.systems.QuickLaunchCommand;
import io.github.racoondog.multiinstance.utils.SwarmUtils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PostInit;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class MultiInstance extends MeteorAddon implements ArgsListener {
    public static final Logger LOG = LogUtils.getLogger();
    public static final String[] LAUNCH_ARGS = FabricLoader.getInstance().getLaunchArguments(true);
    public static final List<String> JVM_OPTS = ManagementFactory.getRuntimeMXBean().getInputArguments();

    @Override
    public void onInitialize() {
        Commands.get().add(new QuickLaunchCommand());
        Commands.get().add(new BenchmarkCommand());
    }

    private static OptionSpec<Void> deactivateSpec;
    private static OptionSpec<String> swarmModeSpec;
    private static OptionSpec<String> swarmIpSpec;
    private static OptionSpec<Integer> swarmPortSpec;

    private static boolean deactivate;
    private static String swarmMode;
    private static String swarmIp;
    private static int swarmPort;

    @Override
    public void createSpecs(OptionParser optionParser) {
        deactivateSpec = optionParser.accepts("meteor:deactivate");
        swarmModeSpec = optionParser.accepts("meteor:swarmMode").withRequiredArg();
        swarmIpSpec = optionParser.accepts("meteor:swarmIp").withRequiredArg();
        swarmPortSpec = optionParser.accepts("meteor:swarmPortSpec").withRequiredArg().ofType(Integer.class);
    }

    @Override
    public void parseArgs(OptionSet optionSet) {
        deactivate = optionSet.has(deactivateSpec);
        swarmMode = optionSet.valueOf(swarmModeSpec);
        swarmIp = optionSet.valueOf(swarmIpSpec);
        swarmPort = optionSet.valueOf(swarmPortSpec);
    }

    @PostInit
    public static void postInit() {
        //parse launch args
        if (deactivate) {
            new ArrayList<>(Modules.get().getActive()).forEach(Module::toggle);
            Hud.get().active = false;
        }

        //after deactivate
        if (swarmMode != null) {
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
