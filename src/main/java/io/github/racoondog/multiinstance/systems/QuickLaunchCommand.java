package io.github.racoondog.multiinstance.systems;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.racoondog.meteorsharedaddonutils.features.arguments.AccountArgumentType;
import io.github.racoondog.multiinstance.utils.AccountUtil;
import io.github.racoondog.multiinstance.utils.InstanceBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.command.CommandSource;

import java.net.InetSocketAddress;

@Environment(EnvType.CLIENT)
public class QuickLaunchCommand extends Command {
    public QuickLaunchCommand() {
        super("quick-launch", "Quickly launch another instance of Minecraft with the specified configurations.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> execute(ctx, false))
            .then(literal("-join").executes(ctx -> execute(ctx, true)));

        builder.then(argument("account", AccountArgumentType.create()).executes(ctx -> executeAccount(ctx, false))
            .then(literal("-join").executes(ctx -> executeAccount(ctx, true))));
    }

    private int execute(CommandContext<CommandSource> ctx, boolean join) {
        MeteorExecutor.execute(() -> {
            InstanceBuilder builder = new InstanceBuilder(AccountUtil.getSelectedAccount());

            if (join) {
                if (!builder.hasArg("--meteor:joinServer")) builder.addArg("--meteor:joinServer");
                builder.modifyArg("--meteor:serverIp", ((InetSocketAddress) mc.getNetworkHandler().getConnection().getAddress()).getAddress().getHostAddress());
            }

            builder.start();
        });

        info("Starting instance...");

        return 1;
    }

    private int executeAccount(CommandContext<CommandSource> ctx, boolean join) {
        MeteorExecutor.execute(() -> {
            InstanceBuilder builder = new InstanceBuilder(AccountArgumentType.get(ctx));

            if (join) {
                if (!builder.hasArg("--meteor:joinServer")) builder.addArg("--meteor:joinServer");
                builder.modifyArg("--meteor:serverIp", ((InetSocketAddress) mc.getNetworkHandler().getConnection().getAddress()).getAddress().getHostAddress());
            }

            builder.start();
        });

        info("Starting instance...");

        return 1;
    }
}
