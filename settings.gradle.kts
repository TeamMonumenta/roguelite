rootProject.name = "roguelite"
include(":roguelite")
project(":roguelite").projectDir = file("roguelite")

pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://maven.playmonumenta.com/releases")
	}
}
