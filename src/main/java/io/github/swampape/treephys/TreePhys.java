package io.github.swampape.treephys;

import org.bukkit.plugin.java.JavaPlugin;

public final class TreePhys extends JavaPlugin {
    private BlockBreakListener listener = new BlockBreakListener();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}
