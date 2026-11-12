import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm") version "2.4.20"
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("maven-publish")
}

version = project.property("mod_version") as String + "+" + project.property("minecraft_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 25
val isMac = OperatingSystem.current().isMacOsX
val isArm64 = setOf("aarch64", "arm64").contains(System.getProperty("os.arch"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

loom {
    accessWidenerPath = file("src/main/resources/dtmap.accesswidener")

    runConfigs.named("client") {
        generateRunConfig.set(true)
        jvmArguments.addAll(
            "-Dmixin.debug.export=true",
            "-Ddevauth.enabled=true",
            "-Ddevauth.account=main",
            "-Dfabric.log.disableAnsi=false",
            "-XX:StackShadowPages=32",
            "-XX:+AllowEnhancedClassRedefinition",
        )
    }

    runConfigs.named("server") {
        generateRunConfig.set(false)
    }
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
    mavenLocal()

    maven ( url = "https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1" )
    maven ( url = "https://repo.hypixel.net/repository/Hypixel" )

    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    // mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    //mappings(loom.officialMojangMappings())
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    compileOnly("maven.modrinth:sodium:mc1.21.11-0.8.6-fabric") { isTransitive = false }

    implementation("com.google.code.gson:gson:2.10.1")
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")

    implementation("maven.modrinth:odin:${project.property("odin_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version")!!,
            "loader_version" to project.property("loader_version")!!,
            "kotlin_loader_version" to project.property("kotlin_loader_version")!!
        )
    }
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
            freeCompilerArgs.add("-Xlambdas=class") //Commodore
        }
    }

    compileJava {
        sourceCompatibility = "25"
        targetCompatibility = "25"
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }
}
