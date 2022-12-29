package io.github.racoondog.multiinstance;

import io.github.racoondog.meteorsharedaddonutils.mixin.mixin.IMicrosoftAccount;
import io.github.racoondog.meteorsharedaddonutils.mixin.mixin.ISwarm;
import io.github.racoondog.multiinstance.utils.ArgsUtils;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.screens.AccountsScreen;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.types.MicrosoftAccount;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.Swarm;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.knot.KnotClient;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MultiInstanceScreen extends WindowScreen {
    private static final CharFilter NUMBER_FILTER = (text, c) -> '0' <= c && c <= '9';
    private static final CharFilter MEMORY_FILTER = (text, c) -> ('0' <= c && c <= '9') || c == 'k' || c == 'K' || c == 'm' || c == 'M' || c == 'g' || c == 'G';
    private static final List<String> JVM_OPTS = ManagementFactory.getRuntimeMXBean().getInputArguments();

    private final Account<?> account;

    private WTextBox jre;
    private WTextBox xms;
    private WTextBox xmx;
    private WTextBox jvmOpts;
    private WTextBox launchArgs;
    private WTextBox width;
    private WTextBox height;
    private WDropdown<SwarmMode> swarmMode;
    private WTextBox swarmIp;
    private WTextBox swarmPort;
    private WCheckbox deactivate;

    private WButton launch;
    private WLabel info;

    public MultiInstanceScreen(GuiTheme theme, Account<?> account) {
        super(theme, "Multi Instance");

        this.account = account;
    }

    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).expandX().widget();

        WVerticalList left = table.add(theme.verticalList()).expandX().widget();
        WHorizontalList loggedInAs = left.add(theme.horizontalList()).expandWidgetY().expandX().widget();
        loggedInAs.add(theme.label("Logged in as: "));
        loggedInAs.add(theme.label(account.getUsername()).color(Color.GRAY));
        left.add(theme.texture(48, 48, account.getCache().getHeadTexture().needsRotate() ? 90 : 0, account.getCache().getHeadTexture()));

        table.add(theme.verticalSeparator()).expandWidgetY();
        WVerticalList right = table.add(theme.verticalList()).expandX().widget();
        jre = option(right, "JDK/JRE Location", getJre());
        table.row();

        WHorizontalList memConfig = right.add(theme.horizontalList()).expandX().widget();
        memConfig.add(theme.label("Min RAM")).expandX();
        xms = memConfig.add(theme.textBox(parseXms(), MEMORY_FILTER)).expandX().widget();
        memConfig.add(theme.label("Max RAM")).expandX();
        xmx = memConfig.add(theme.textBox(parseXmx(), MEMORY_FILTER)).expandX().widget();

        jvmOpts = option(right, "JVM Options", parseJvmOpts());
        launchArgs = option(right, "Launch Arguments");

        WHorizontalList sizeConfig = right.add(theme.horizontalList()).expandX().widget();
        sizeConfig.add(theme.label("Window Width")).expandX();
        width = sizeConfig.add(theme.textBox(getWidth(), NUMBER_FILTER)).expandX().widget();
        sizeConfig.add(theme.label("Window Height")).expandX();
        height = sizeConfig.add(theme.textBox(getHeight(), NUMBER_FILTER)).expandX().widget();

        WHorizontalList swarmConfig = right.add(theme.horizontalList()).expandX().widget();
        swarmConfig.add(theme.label("Swarm")).expandX();
        swarmMode = swarmConfig.add(theme.dropdown(SwarmMode.Off)).expandWidgetX().widget();
        swarmConfig.add(theme.label("IP")).expandX();
        swarmIp = swarmConfig.add(theme.textBox(getSwarmIp())).expandX().widget();
        swarmConfig.add(theme.label("Port")).expandX();
        swarmPort = swarmConfig.add(theme.textBox(getSwarmPort())).expandX().widget();

        WHorizontalList meteorConfig = right.add(theme.horizontalList()).expandX().widget();
        meteorConfig.add(theme.label("Deactivate Meteor")).expandX();
        deactivate = meteorConfig.add(theme.checkbox(false)).expandCellX().widget();

        table.add(theme.horizontalSeparator()).expandX();
        table.row();

        WHorizontalList bottom = table.add(theme.horizontalList()).expandX().widget();
        launch = bottom.add(theme.button("Launch")).expandCellX().widget();
        launch.action = this::activate;
        info = bottom.add(theme.label("")).expandX().widget();
        WButton back = bottom.add(theme.button("Back")).right().expandCellX().widget();
        back.action = () -> mc.setScreen(new AccountsScreen(theme));
    }

    private void activate() {
        info.set("");
        launch.minWidth = launch.width;
        launch.set("...");
        locked = true;

        MeteorExecutor.execute(() -> {
            try {
                launch();
            } catch (IOException e) {
                info.set("Could not start instance...");
                MultiInstance.LOG.info("Could not start instance...");
                e.printStackTrace();
            }

            launch.minWidth = 0;
            launch.set("Launch");
            locked = false;
        });
    }

    private void launch() throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO(); //Redirect stdout
        pb.environment(); //Copy env vars

        String[] args = getLaunchArguments();
        List<String> newJvmOpts = modifyJvmOpts();

        List<String> argsList = pb.command();
        argsList.add(jre.get());
        argsList.addAll(newJvmOpts);
        argsList.add("-cp");
        argsList.add(System.getProperty("java.class.path"));
        argsList.add(FabricLoader.getInstance().isDevelopmentEnvironment() ? "net.fabricmc.devlaunchinjector.Main" : KnotClient.class.getName());
        argsList.addAll(Arrays.asList(args));
        argsList.addAll(List.of(launchArgs.get().split(" ")));

        MultiInstance.LOG.info("Starting new instance...");
        MultiInstance.LOG.info("JRE/JDK: " + jre.get());
        MultiInstance.LOG.info("JVM Options: " + String.join(" ", newJvmOpts));

        Process p = pb.start();

        MultiInstance.LOG.info("Instance started with PID {}.", p.pid());
    }

    private boolean isHidden(String opt) {
        return opt.contains(" ");
    }

    private String parseJvmOpts() {
        StringBuilder sb = new StringBuilder();
        for (var str : JVM_OPTS) if (!isHidden(str) && !str.startsWith("-Xms") && !str.startsWith("-Xmx")) sb.append(str).append(" ");
        return sb.toString().trim();
    }

    private String parseXms() {
        for (var token : JVM_OPTS) {
            if (token.startsWith("-Xms")) return token.substring(4);
        }
        return "2048m";
    }

    private String parseXmx() {
        for (var token : JVM_OPTS) {
            if (token.startsWith("-Xmx")) return token.substring(4);
        }
        return "2048m";
    }

    private String getSwarmIp() {
        return ((ISwarm) Modules.get().get(Swarm.class)).getIpAddress().get();
    }

    private String getSwarmPort() {
        return ((ISwarm) Modules.get().get(Swarm.class)).getServerPort().toString();
    }

    private String getWidth() {
        return ArgsUtils.getArgOrElse("--width", () -> String.valueOf(mc.getWindow().getWidth()));
    }

    private String getHeight() {
        return ArgsUtils.getArgOrElse("--height", () -> String.valueOf(mc.getWindow().getHeight()));
    }

    private List<String> modifyJvmOpts() {
        List<String> newOpts = new ArrayList<>();

        for (var entry : JVM_OPTS) if (isHidden(entry)) newOpts.add(entry);
        newOpts.addAll(Arrays.asList(jvmOpts.get().split(" ")));
        newOpts.add("-Xms" + xms.get());
        newOpts.add("-Xmx" + xmx.get());
        return newOpts;
    }

    private String[] getLaunchArguments() {
        List<String> args = new ArrayList<>(List.of(ArgsUtils.args));

        modifyArg(args, "--username", account.getUsername());
        if (account.getCache().uuid != null) modifyArg(args, "--uuid", account.getCache().uuid);
        if (account instanceof MicrosoftAccount msacc) modifyArg(args, "--accessToken", ((IMicrosoftAccount) msacc).invokeAuth());

        modifyArg(args, "--width", width.get());
        modifyArg(args, "--height", height.get());

        String swarmModeStr = swarmMode.get().name().toLowerCase(Locale.ROOT);
        if (!swarmModeStr.equals("off")) {
            modifyArg(args, "--meteor:swarmMode", swarmModeStr);
            modifyArg(args, "--meteor:swarmIp", swarmIp.get());
            modifyArg(args, "--meteor:swarmPort", swarmPort.get());
        }

        if (deactivate.checked) args.add("--meteor:deactivate");

        return args.toArray(new String[0]);
    }

    private void modifyArg(List<String> list, String token, String replace) {
        int idx = list.indexOf(token);
        if (idx != -1) {
            list.set(idx + 1, replace);
        } else {
            list.add(token);
            list.add(replace);
        }
    }

    private String getJre() {
        return Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
    }

    private WTextBox option(WContainer root, String label) {
        return option(root, label, "");
    }

    private WTextBox option(WContainer root, String label, String placeholderText) {
        WHorizontalList list = root.add(theme.horizontalList()).expandX().widget();
        list.add(theme.label(label)).right().expandX();
        return list.add(theme.textBox(placeholderText)).minWidth(512).right().expandX().widget();
    }

    public enum SwarmMode {
        Off,
        Host,
        Worker
    }
}
