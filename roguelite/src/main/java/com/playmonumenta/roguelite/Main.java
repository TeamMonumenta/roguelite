package com.playmonumenta.roguelite;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class Main extends JavaPlugin {

	private static @Nullable Main INSTANCE;

	@Override
	public void onEnable() {
		INSTANCE = this;

		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(LayoutManager.getInstance(), this);

		RL2Command.getInstance();
	}

	@Override
	public void onDisable() {
		LayoutManager.getInstance().onDisable();
		RL2Command.getInstance().onDisable();
	}

	public static Main getInstance() {
		Main instance = INSTANCE;
		if (instance == null) {
			throw new RuntimeException("You cannot get the instance before the plugin starts!");
		}
		return instance;
	}

}
