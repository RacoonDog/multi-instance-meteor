package io.github.racoondog.multiinstance;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import org.slf4j.Logger;

public class MultiInstance extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
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
