package io.github.racoondog.multiinstance.systems;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.racoondog.meteorsharedaddonutils.features.arguments.AccountArgumentType;
import io.github.racoondog.multiinstance.utils.AccountUtil;
import io.github.racoondog.multiinstance.utils.InstanceBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
public class QuickLaunchCommand extends Command {
    private String username = null;
    private StopWatch stopWatch = null;

    public QuickLaunchCommand() {
        super("quick-launch", "Quickly launch another instance of Minecraft with the specified configurations.", "ql");

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> execute(ctx, false))
            .then(literal("-join").executes(ctx -> execute(ctx, true)));

        builder.then(argument("account", AccountArgumentType.create()).executes(ctx -> executeAccount(ctx, false))
            .then(literal("-join").executes(ctx -> executeAccount(ctx, true))));

        builder.then(argument("account", AccountArgumentType.create())
            .then(literal("-join").then(literal("-timer")
                .executes(ctx -> {
                    Account<?> account = AccountArgumentType.get(ctx);

                    MeteorExecutor.execute(() -> {
                        InstanceBuilder builder1 = new InstanceBuilder(account);
                        configureJoin(builder1);
                        builder1.start();
                    });

                    username = account.getUsername();
                    stopWatch = StopWatch.createStarted();

                    return 1;
                }))));
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (stopWatch == null) return;
        if (event.getMessage().getString().equals(username + " joined")) {
            stopWatch.stop();

            info("Took %s seconds.", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    private int execute(CommandContext<CommandSource> ctx, boolean join) {
        MeteorExecutor.execute(() -> {
            InstanceBuilder builder = new InstanceBuilder(AccountUtil.getSelectedAccount());

            if (join) configureJoin(builder);

            builder.start();
        });

        info("Starting instance...");

        return 1;
    }

    private int executeAccount(CommandContext<CommandSource> ctx, boolean join) {
        MeteorExecutor.execute(() -> {
            InstanceBuilder builder = new InstanceBuilder(AccountArgumentType.get(ctx));

            if (join) configureJoin(builder);

            builder.start();
        });

        info("Starting instance...");

        return 1;
    }

    private void configureJoin(InstanceBuilder builder) {
        if (mc.isIntegratedServerRunning()) {
            warning("Cannot join singeplayer world.");
            return;
        }

        ServerInfo serverInfo = mc.getCurrentServerEntry();

        if (serverInfo == null) {
            warning("Could not obtain server information.");
            return;
        }

        String[] address = serverInfo.address.split(":");

        builder.modifyArg("--server", address[0])
            .modifyArg("--port", address.length > 1 ? address[1] : "25565");
    }
}
