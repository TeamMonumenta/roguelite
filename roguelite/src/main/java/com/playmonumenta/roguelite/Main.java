package com.playmonumenta.roguelite;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class Main extends JavaPlugin {

	private static @Nullable Main pluginInstance;

	@Override
	public void onEnable() {
		pluginInstance = this;

		new RL2Command(this);
	}

	public static Main getInstance() {
		Main instance = pluginInstance;
		if (instance == null) {
			throw new RuntimeException("You cannot get the instance before the plugin starts!");
		}
		return instance;
	}

}
