package com.ricedotwho.dtmap.utils

import com.mojang.blaze3d.platform.NativeImage
import com.odtheking.odin.OdinMod.mc
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import java.nio.file.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.nameWithoutExtension

/**
 * @author lyric
 */
object MapImageLoader {
    val imagesPath: Path = FabricLoader.getInstance().configDir.resolve("dtmap/images")
    private val loadedImages = ConcurrentHashMap<String, ImageData>()
    private val watchService: WatchService

    /**
     * class representing loaded image data
     */
    data class ImageData(
        val resourceLocation: Identifier,
        val texture: DynamicTexture
    )

    /**
     * init function
     * creates the images directory, I didn't do this in SettingsData, because this is completely separate to settings.
     * this also loads all existing images at startup, and begins the threaded WatchService allowing for async loading of user images.
     */
    init {
        if (!imagesPath.exists()) {
            Files.createDirectories(imagesPath)
        }

        if (!imagesPath.exists()) throw IllegalStateException("Images path does not exist: $imagesPath")
        val imageFiles = Files.list(imagesPath).filter { it.extension.lowercase() == "png"}.toList()

        imageFiles.forEach { path ->
            loadImage(path)
        }

        watchService = FileSystems.getDefault().newWatchService()
        imagesPath.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE
        )

        var watcher = Thread(::watchLoop, "DTM-ImageLoader")
        watcher.start()

        ClientLifecycleEvents.CLIENT_STOPPING.register {
            watcher.interrupt()
            watchService.close()
        }
    }

    /**
     * Load a single image file and register it with the TextureManager.
     */
    private fun loadImage(path: Path, addedExtension: String = UUID.randomUUID().toString().replace("-", "").slice(0..4)) {
        val fileName = path.nameWithoutExtension

        if (path.extension.lowercase() != "png") return;

        // TODO: implement jpeg logic.

        try {
            val inputStream = path.inputStream()
            val nativeImage = NativeImage.read(inputStream)
            inputStream.close()

            val resourceLocation = Identifier.fromNamespaceAndPath("dtmap", "map_bg_${fileName.lowercase().replace(" ", "_")}${addedExtension}f")
            val width = nativeImage.width
            val height = nativeImage.height
            mc.execute {
                val texture = DynamicTexture({ resourceLocation.toString() }, nativeImage)
                mc.textureManager.register(resourceLocation, texture)
                loadedImages[fileName] = ImageData(resourceLocation, texture)
                Chat.send("§a[ImageManager] Loaded: §f${fileName} §7(${width}x${height})")
            }
        } catch (e: Exception) {
            Chat.send("§c[ImageManager] Failed to load '§f${fileName}§c': ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Main watch loop that polls for file system events.
     */
    private fun watchLoop() {
        while (true) {
            val key: WatchKey? = try {
                watchService.poll(1, java.util.concurrent.TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
                break
            }

            if (key == null) continue

            for (event in key.pollEvents()) {
                val kind = event.kind()
                if (kind == StandardWatchEventKinds.OVERFLOW) continue

                @Suppress("UNCHECKED_CAST")
                val ev = event as WatchEvent<Path>
                val filename = ev.context()
                val filePath = imagesPath.resolve(filename)

                when (kind) {
                    StandardWatchEventKinds.ENTRY_CREATE -> {
                        Thread.sleep(200)

                        if (filePath.exists() && filePath.extension.lowercase() == "png") {
                            loadImage(filePath)
                        }
                    }
                    StandardWatchEventKinds.ENTRY_DELETE -> {
                        val fileNameWithoutExt = filename.nameWithoutExtension
                        if (loadedImages.containsKey(fileNameWithoutExt)) {
                            unloadImage(fileNameWithoutExt)
                        }
                    }
                    StandardWatchEventKinds.ENTRY_MODIFY -> {
                        Thread.sleep(200)

                        val oldResource = loadedImages[filePath.nameWithoutExtension]
                        if (filePath.exists()) loadImage(filePath)
                        oldResource?.let { mc.textureManager.release(it.resourceLocation) }
                    }
                }
            }

            key.reset()
        }
    }

    /**
     * Unload an image and clean up its resources.
     */
    private fun unloadImage(fileName: String) {
        val imageData = loadedImages.remove(fileName) ?: return

        // Schedule texture cleanup on the main thread
        mc.execute {
            try {
                mc.textureManager.release(imageData.resourceLocation)
            } catch (e: Exception) {
                Chat.send("§c[ImageManager] Error unloading image '§f${fileName}§c': ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Get the Identifier for a specific image by name.
     */
    fun getImageIdentifier(name: String): Identifier? =
        loadedImages[name]?.resourceLocation

    /**
     * Get an array of all loaded image names for the dropdown.
     */
    fun getImageNames(): Array<String> =
        arrayOf("No image") + loadedImages.keys.sorted().toTypedArray()

    /**
     * Check if an image exists in the loaded images.
     */
    fun imageExists(name: String): Boolean =
        loadedImages.containsKey(name)
}
