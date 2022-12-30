package io.github.racoondog.multiinstance.systems;

import io.github.racoondog.meteorsharedaddonutils.mixin.mixin.ISwarm;
import io.github.racoondog.multiinstance.utils.ArgsUtils;
import io.github.racoondog.multiinstance.utils.InstanceBuilder;
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
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.Swarm;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MultiInstanceScreen extends WindowScreen {
    private static final CharFilter NUMBER_FILTER = (text, c) -> '0' <= c && c <= '9';
    private static final CharFilter MEMORY_FILTER = (text, c) -> ('0' <= c && c <= '9') || c == 'k' || c == 'K' || c == 'm' || c == 'M' || c == 'g' || c == 'G';

    private final Account<?> account;
    private final InstanceBuilder builder;

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
        this.builder = new InstanceBuilder(account);
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
        jre = option(right, "JDK/JRE Location", builder.jre);
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
            builder.jre = jre.get();

            //Setup launch args
            builder.launchArgs.addAll(List.of(launchArgs.get().split(" ")));
            builder.modifyArg("--width", width.get());
            builder.modifyArg("--height", height.get());

            if (!swarmMode.get().equals(SwarmMode.Off)) {
                builder.modifyArg("--meteor:swarmMode", swarmMode.get().name().toLowerCase(Locale.ROOT));
                builder.modifyArg("--meteor:swarmIp", swarmIp.get());
                builder.modifyArg("--meteor:swarmPort", swarmPort.get());
            }

            if (deactivate.checked && !builder.hasArg("--meteor:deactivate")) builder.addArg("--meteor:deactivate");

            builder.jvmOpts = modifyJvmOpts(builder.jvmOpts);

            builder.start();

            launch.minWidth = 0;
            launch.set("Launch");
            locked = false;
        });
    }

    public String getWidth() {
        return ArgsUtils.getArgOrElse("--width", () -> String.valueOf(mc.getWindow().getWidth()));
    }

    public String getHeight() {
        return ArgsUtils.getArgOrElse("--height", () -> String.valueOf(mc.getWindow().getHeight()));
    }

    private boolean isHidden(String opt) {
        return opt.contains(" ");
    }

    private String parseJvmOpts() {
        StringBuilder sb = new StringBuilder();
        for (var str : builder.jvmOpts) if (!isHidden(str) && !str.startsWith("-Xms") && !str.startsWith("-Xmx")) sb.append(str).append(" ");
        return sb.toString().trim();
    }

    private String parseXms() {
        for (var token : builder.jvmOpts) {
            if (token.startsWith("-Xms")) return token.substring(4);
        }
        return "2048m";
    }

    private String parseXmx() {
        for (var token : builder.jvmOpts) {
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

    private List<String> modifyJvmOpts(List<String> baseOpts) {
        List<String> newOpts = new ArrayList<>();

        for (var entry : baseOpts) if (isHidden(entry)) newOpts.add(entry);
        newOpts.addAll(Arrays.asList(jvmOpts.get().split(" ")));
        newOpts.add("-Xms" + xms.get());
        newOpts.add("-Xmx" + xmx.get());
        return newOpts;
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
