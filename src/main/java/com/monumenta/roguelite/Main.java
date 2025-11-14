package com.monumenta.roguelite;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main PluginInstance;

    @Override
	public void onEnable() {
        PluginInstance = this;

		new RL2Command(this);
    }

	public static Main getInstance() {
        return PluginInstance;
    }

}
