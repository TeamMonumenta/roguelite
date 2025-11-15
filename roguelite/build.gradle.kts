import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

dependencies {
	compileOnly(libs.commandapi)
	compileOnly(libs.gson)
	compileOnly(libs.structures)
}

plugins {
	id("com.playmonumenta.gradle-config") version "3.5"
}

monumenta {
	id("Roguelite")
	name("Roguelite")
	paper(
		"com.playmonumenta.roguelite.Main", BukkitPluginDescription.PluginLoadOrder.POSTWORLD, "1.20",
		depends = listOf("CommandAPI", "MonumentaStructureManagement"),
		apiJarVersion = "1.20.4-R0.1-SNAPSHOT",
	)
}

allprojects {
	tasks.withType<Javadoc> {
		options {
			(this as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
		}
	}
}
