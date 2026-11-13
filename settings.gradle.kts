pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }

    plugins {
        id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
        kotlin("jvm") version "2.4.20"
        kotlin("plugin.compose") version "2.4.20"
    }
}
