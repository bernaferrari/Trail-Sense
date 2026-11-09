package com.kylecorry.andromeda.files

import java.io.File
import java.net.JarURLConnection
import java.util.jar.JarFile
import okio.Source
import okio.source

private fun assetLoader(): ClassLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
actual fun openAsset(path: String): Source =
    (assetLoader().getResourceAsStream(assetPath(path)) ?: error("Asset not found: $path")).source()

actual fun listAssets(path: String): List<String> {
    val prefix = assetPath(path).let { if (it.isEmpty()) "" else "$it/" }
    val names = linkedSetOf<String>()
    fun add(name: String) {
        if (name.startsWith(prefix)) name.removePrefix(prefix).substringBefore('/').takeIf { it.isNotEmpty() }?.let(names::add)
    }
    val resources = assetLoader().getResources(prefix)
    while (resources.hasMoreElements()) {
        val url = resources.nextElement()
        when (url.protocol) {
            "file" -> File(url.toURI()).list()?.forEach(names::add)
            "jar" -> (url.openConnection() as JarURLConnection).apply { useCaches = false }.jarFile.use { jar ->
                jar.entries().asSequence().forEach { add(it.name) }
            }
        }
    }
    // Some JARs omit explicit directory entries, so they are absent from getResources(prefix).
    System.getProperty("java.class.path").orEmpty().split(File.pathSeparatorChar).forEach { entry ->
        val file = File(entry)
        if (file.isFile && file.extension.equals("jar", ignoreCase = true)) runCatching {
            JarFile(file).use { jar -> jar.entries().asSequence().forEach { add(it.name) } }
        }
    }
    return names.sorted()
}
