package io.github.racoondog.multiinstance;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.racoondog.meteorsharedaddonutils.features.arguments.AccountArgumentType;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.command.CommandSource;

import java.util.List;

@Environment(EnvType.CLIENT)
public class QuickLaunchCommand extends Command {
    public QuickLaunchCommand() {
        super("quick-launch", "Quickly launch another instance of Minecraft with the specified configurations.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        LiteralCommandNode<CommandSource> root = builder
            .executes(this::execute)
            .then(argument("account", AccountArgumentType.create()).executes(this::execute))
            .build();

        builder
            .then(literal("-join").redirect(root))
            .then(literal("-swarm").redirect(root))
            .then(literal("-deactivate").redirect(root));
    }

    private int execute(CommandContext<CommandSource> ctx) {
        List<String> nodes = ctx.getNodes().stream().map(node -> node.getNode().getName()).toList();

        boolean join = nodes.contains("-join");
        boolean swarm = nodes.contains("-swarm");
        boolean deactivate = nodes.contains("-deactivate");

        return 1;
    }
}
