package com.jkantrell.mc.underilla.spigot.preparing;

import com.jkantrell.mc.underilla.spigot.Underilla;

public class ServerSetup {
    private ServerSetup() {}

    public static void setupPaper() {
        int cores = Runtime.getRuntime().availableProcessors();
        Underilla.getInstance().getLogger().info("Available cores: " + cores);
        Underilla.getInstance().getLogger().info("config/paper-global.yml should be edit to: chunk-system.worker-threads: " + (cores - 1));
    }
}
