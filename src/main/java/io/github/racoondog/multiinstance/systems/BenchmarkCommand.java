package io.github.racoondog.multiinstance.systems;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.racoondog.meteorsharedaddonutils.features.arguments.AccountArgumentType;
import io.github.racoondog.multiinstance.utils.InstanceBuilder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.command.CommandSource;
import org.apache.commons.lang3.time.StopWatch;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

//todo unhardcode runner username using process id
//todo catch NPE on process start
@Environment(EnvType.CLIENT)
public class BenchmarkCommand extends Command {
    private static int timeoutSeconds = 45;
    private String username;
    private Account<?> account;
    private StopWatch stopWatch = StopWatch.create();
    private int left;
    private boolean flag = false;
    private static final LongList timings = new LongArrayList();
    private long timeout;
    private int errors;

    public BenchmarkCommand() {
        super("ql-benchmark", "Automatic quick launch benchmarks.", "qlb");

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("account", AccountArgumentType.create()).then(argument("int", IntegerArgumentType.integer(0))
            .executes(ctx -> {
                timings.clear();
                errors = 0;
                flag = true;
                left = IntegerArgumentType.getInteger(ctx, "int");
                account = AccountArgumentType.get(ctx);
                username = account.getUsername();
                info("Starting benchmark...");
                runTest();
                return 1;
            })));

        builder.then(literal("stop").executes(ctx -> {
            info("Stopping benchmark...");
            left = 0;
            return 1;
        }));

        builder.then(literal("setTimeout").then(argument("int", IntegerArgumentType.integer(0)).executes(ctx -> {
            int old = timeoutSeconds;
            timeoutSeconds = IntegerArgumentType.getInteger(ctx, "int");
            info("Timeout seconds set to %s from %s.", timeoutSeconds, old);
            return 1;
        })));
    }

    private void runTest() {
        timeout = timeoutSeconds * 20L;
        stopWatch.reset();
        stopWatch.start();
        MeteorExecutor.execute(() -> {
            InstanceBuilder builder = new InstanceBuilder(account);
            String[] tokens = mc.getCurrentServerEntry().address.split(":");
            builder.modifyArg("--server", tokens[0])
                .modifyArg("--port", tokens.length > 1 ? tokens[1] : "25565")
                .start();
        });
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        String text = event.getMessage().getString();
        if (flag) {
            if (text.equals(username + " joined")) {
                stopWatch.stop();
                long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
                ChatUtils.sendPlayerMsg("Took %.2f seconds.".formatted(time / 1000.0f));
                timings.add(time);
                left--;
                if (left > 0) {
                    runTest();
                } else {
                    BigInteger bigint = BigInteger.valueOf(0);
                    for (var val : timings) {
                        bigint = bigint.add(BigInteger.valueOf(val));
                    }
                    long average = bigint.divide(BigInteger.valueOf(timings.size())).longValue();
                    float success = ((float) timings.size()) / (timings.size() + errors) * 100;
                    ChatUtils.sendPlayerMsg("Average %.2f seconds. Success rate %.2f%s.".formatted(average / 1000.0f, success, "%"));
                    flag = false;
                }
            }
        } else if (text.startsWith("McChiggenLord > Took ") || text.startsWith("McChiggenLord > Timed out.")) System.exit(0);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (flag) {
            if (timeout == 0 && left > 0) {
                stopWatch.stop();
                ChatUtils.sendPlayerMsg("Timed out.");
                errors++;
                runTest();
                timeout = timeoutSeconds * 20L;
            } else {
                timeout--;
            }
        }
    }
}
