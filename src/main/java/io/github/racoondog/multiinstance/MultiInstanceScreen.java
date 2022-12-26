package io.github.racoondog.multiinstance;

import io.github.racoondog.multiinstance.mixin.IMicrosoftAccount;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.screens.AccountsScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.types.MicrosoftAccount;
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

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MultiInstanceScreen extends WindowScreen {
    private final Account<?> account;
    private final List<String> jvmOpts = ManagementFactory.getRuntimeMXBean().getInputArguments();
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
        WTextBox jre = option(right, "JDK/JRE Location", getJre());
        table.row();

        WHorizontalList memConfig = right.add(theme.horizontalList()).expandX().widget();
        memConfig.add(theme.label("Min RAM")).expandX();
        WTextBox xms = memConfig.add(theme.textBox(parseXms())).expandX().widget();
        memConfig.add(theme.label("Max RAM")).expandX();
        WTextBox xmx = memConfig.add(theme.textBox(parseXmx())).expandX().widget();

        WTextBox jvmOpts = option(right, "JVM Options", parseJvmOpts());

        table.add(theme.horizontalSeparator()).expandX();
        table.row();

        WHorizontalList bottom = table.add(theme.horizontalList()).expandX().widget();
        WButton launch = bottom.add(theme.button("Launch")).expandCellX().widget();

        WLabel info = bottom.add(theme.label("")).expandX().widget();

        launch.action = () -> {
            info.set("");
            launch.minWidth = launch.width;
            launch.set("...");
            locked = true;

            MeteorExecutor.execute(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder();
                    pb.inheritIO(); //Redirect stdout
                    pb.environment(); //Copy env vars

                    String[] launchArgs = getLaunchArguments();
                    List<String> newJvmOpts = modifyJvmOpts(xms.get(), xmx.get(), jvmOpts.get());

                    List<String> args = pb.command();
                    args.add(jre.get());
                    args.addAll(newJvmOpts);
                    args.add("-cp");
                    args.add(System.getProperty("java.class.path"));
                    args.add(FabricLoader.getInstance().isDevelopmentEnvironment() ? "net.fabricmc.devlaunchinjector.Main" : KnotClient.class.getName());
                    args.addAll(Arrays.asList(launchArgs));

                    MultiInstance.LOG.info("Starting new instance...");
                    MultiInstance.LOG.info("JRE/JDK: " + jre.get());
                    MultiInstance.LOG.info("JVM Options: " + String.join(" ", newJvmOpts));

                    Process p = pb.start();

                    MultiInstance.LOG.info("Instance started with PID {}.", p.pid());
                } catch (IOException e) {
                    info.set("Could not start instance...");
                    MultiInstance.LOG.info("Could not start instance...");
                    e.printStackTrace();
                }

                launch.minWidth = 0;
                launch.set("Launch");
                locked = false;
            });
        };
        WButton back = bottom.add(theme.button("Back")).right().expandCellX().widget();
        back.action = () -> mc.setScreen(new AccountsScreen(theme));
    }

    private boolean isHidden(String opt) {
        return opt.contains(" ");
    }

    private String parseJvmOpts() {
        StringBuilder sb = new StringBuilder();
        for (var str : jvmOpts) if (!isHidden(str) && !str.startsWith("-Xms") && !str.startsWith("-Xmx")) sb.append(str).append(" ");
        return sb.toString().trim();
    }

    private String parseXms() {
        for (var token : jvmOpts) {
            if (token.startsWith("-Xms")) return token.substring(4);
        }
        return "2048m";
    }

    private String parseXmx() {
        for (var token : jvmOpts) {
            if (token.startsWith("-Xmx")) return token.substring(4);
        }
        return "2048m";
    }

    private List<String> modifyJvmOpts(String xms, String xmx, String jvmOpts) {
        List<String> newOpts = new ArrayList<>();

        for (var entry : this.jvmOpts) if (isHidden(entry)) newOpts.add(entry);
        newOpts.addAll(Arrays.asList(jvmOpts.split(" ")));
        newOpts.add("-Xms" + xms);
        newOpts.add("-Xmx" + xmx);
        return newOpts;
    }

    private String[] getLaunchArguments() {
        List<String> args = new ArrayList<>(List.of(FabricLoader.getInstance().getLaunchArguments(false)));

        modifyArg(args, "--username", account.getUsername());
        if (account.getCache().uuid != null) modifyArg(args, "--uuid", account.getCache().uuid);
        if (account instanceof MicrosoftAccount msacc) modifyArg(args, "--accessToken", ((IMicrosoftAccount) msacc).invokeAuth());

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

    private WTextBox option(WContainer root, String label, String placeholderText) {
        WHorizontalList list = root.add(theme.horizontalList()).expandX().widget();
        list.add(theme.label(label)).right().expandX();
        return list.add(theme.textBox(placeholderText)).minWidth(512).right().expandX().widget();
    }
}
