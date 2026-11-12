plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.kylecorry.trail_sense"
version = "1.0.0"

kotlin {
    android {
        namespace = "com.kylecorry.trail_sense.shared"
        compileSdk = 37
        androidResources { enable = true }
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Domain models and calculations the shared UI and navigation build on
            api(project(":core"))
            api(libs.sol)
            // Navigation 3 keys and the app's back stack
            api(libs.androidx.navigation3.runtime)
            // System back (gesture, button) as common navigation events
            implementation(libs.cmp.navigationevent.compose)
            // Screen view models and lifecycle effects
            implementation(libs.cmp.lifecycle.viewmodel.compose)
            implementation(libs.cmp.lifecycle.runtime.compose)
            api(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.atomicfu)
            implementation(libs.andromeda.markdown)

            // Compose Multiplatform artifacts
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.cmp.material3)
            implementation(compose.ui)
            // @Preview on common composables (Android Studio renders them from the Android target)
            implementation(libs.cmp.ui.tooling.preview)
            implementation(compose.materialIconsExtended)
            // Google Sans Flex (OFL), the family's M3 Expressive typeface
            implementation(compose.components.resources)

            // Portable expressive theming
            implementation("com.materialkolor:material-kolor-material3:6.0.0")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
        }

        androidMain.dependencies {
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
        }
    }
}

dependencies {
    add("androidMainApi", platform(libs.compose.bom))
}

/**
 * Copies the app strings, plurals and string arrays listed in shared-strings.txt, with all their translations,
 * into Compose resources so common code can read them. Array items that point at a string (@string/name) are
 * resolved in each language. The app's res folder stays the only place strings and translations are edited.
 *
 * Android string syntax is resolved here (quotes, \' \" \@ \? escapes, whitespace collapsing, inline markup)
 * and the plain text is written back with only the escapes Compose resources understand (\n, \t, \\).
 */
@CacheableTask
abstract class SyncSharedStringsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val names: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appStrings: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appArrays: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val staticResources: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun sync() {
        val wanted = names.get().asFile.readLines().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }.toSortedSet()
        val out = outputDir.get().asFile
        out.deleteRecursively()
        staticResources.get().asFile.copyRecursively(out)

        // values folder -> name -> text, and name -> quantity -> text for plurals
        val byFolder = sortedMapOf<String, MutableMap<String, String>>()
        val pluralsByFolder = sortedMapOf<String, MutableMap<String, Map<String, String>>>()
        // Every string by folder, to resolve array items that point at strings
        val allByFolder = mutableMapOf<String, MutableMap<String, String>>()
        appStrings.files.forEach { file ->
            val folder = file.parentFile.name
            val qualifiers = folder.removePrefix("values").removePrefix("-")
            if (qualifiers.isNotEmpty() && !LOCALE.matches(qualifiers)) return@forEach
            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i) as org.w3c.dom.Element
                val name = node.getAttribute("name")
                val text = resolveAndroidString(node.textContent)
                allByFolder.getOrPut(folder) { mutableMapOf() }[name] = text
                if (name in wanted) {
                    byFolder.getOrPut(folder) { sortedMapOf() }[name] = text
                }
            }
            val plurals = doc.getElementsByTagName("plurals")
            for (i in 0 until plurals.length) {
                val node = plurals.item(i) as org.w3c.dom.Element
                val name = node.getAttribute("name")
                if (name !in wanted) continue
                val items = node.getElementsByTagName("item")
                val quantities = sortedMapOf<String, String>()
                for (j in 0 until items.length) {
                    val item = items.item(j) as org.w3c.dom.Element
                    quantities[item.getAttribute("quantity")] = resolveAndroidString(item.textContent)
                }
                pluralsByFolder.getOrPut(folder) { sortedMapOf() }[name] = quantities
            }
        }
        // Arrays are defined once (untranslated); their raw items, by name
        val rawArrays = sortedMapOf<String, List<String>>()
        appArrays.files.filter { it.parentFile.name == "values" }.forEach { file ->
            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string-array")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i) as org.w3c.dom.Element
                val name = node.getAttribute("name")
                if (name !in wanted) continue
                val items = node.getElementsByTagName("item")
                rawArrays[name] = (0 until items.length).map { items.item(it).textContent.trim() }
            }
        }

        val missing = wanted - byFolder["values"].orEmpty().keys - pluralsByFolder["values"].orEmpty().keys -
                rawArrays.keys
        check(missing.isEmpty()) { "shared-strings.txt lists strings the app does not define: $missing" }

        val defaults = allByFolder["values"].orEmpty()
        fun arraysFor(folder: String): Map<String, List<String>> {
            val localized = allByFolder[folder].orEmpty()
            return rawArrays.mapValues { (name, items) ->
                items.map { item ->
                    if (item.startsWith("@string/")) {
                        val key = item.removePrefix("@string/")
                        localized[key] ?: defaults[key] ?: error("Array $name points at a missing string: $key")
                    } else {
                        resolveAndroidString(item)
                    }
                }
            }
        }

        (byFolder.keys + pluralsByFolder.keys + (if (rawArrays.isEmpty()) emptySet() else allByFolder.keys))
            .toSortedSet().forEach { folder ->
            val strings = byFolder[folder].orEmpty()
            val plurals = pluralsByFolder[folder].orEmpty()
            val arrays = arraysFor(folder)
            // Android still names Hebrew and Indonesian by their old codes; write both so either lookup finds them.
            val folders = when (folder) {
                "values-iw" -> listOf(folder, "values-he")
                "values-in" -> listOf(folder, "values-id")
                else -> listOf(folder)
            }
            val xml = buildString {
                appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                appendLine("<!-- Generated by :shared:syncSharedStrings from app/src/main/res. Do not edit. -->")
                appendLine("<resources>")
                strings.forEach { (name, text) -> appendLine("""    <string name="$name">${encode(text)}</string>""") }
                plurals.forEach { (name, quantities) ->
                    appendLine("""    <plurals name="$name">""")
                    quantities.forEach { (quantity, text) ->
                        appendLine("""        <item quantity="$quantity">${encode(text)}</item>""")
                    }
                    appendLine("    </plurals>")
                }
                arrays.forEach { (name, items) ->
                    appendLine("""    <string-array name="$name">""")
                    items.forEach { appendLine("""        <item>${encode(it)}</item>""") }
                    appendLine("    </string-array>")
                }
                appendLine("</resources>")
            }
            folders.forEach { File(out, "$it/strings.xml").apply { parentFile.mkdirs() }.writeText(xml) }
        }
    }

    private fun resolveAndroidString(raw: String): String {
        val text = StringBuilder()
        var quoted = false
        var hadQuotes = false
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when {
                c == '\\' && i + 1 < raw.length -> {
                    when (val next = raw[i + 1]) {
                        'n' -> text.append('\n')
                        't' -> text.append('\t')
                        'u' -> {
                            text.append(raw.substring(i + 2, i + 6).toInt(16).toChar())
                            i += 4
                        }
                        else -> text.append(next)
                    }
                    i += 2
                    continue
                }
                c == '"' -> {
                    quoted = !quoted
                    hadQuotes = true
                }
                // As aapt2: Java whitespace collapses (so a thin space does), no-break spaces (French "Actif : %s") stay.
                !quoted && Character.isWhitespace(c) -> if (text.isEmpty() || text.last() != ' ') text.append(' ')
                else -> text.append(c)
            }
            i++
        }
        // Spaces inside quotes are deliberate; elsewhere Android trims the ends.
        return if (hadQuotes) text.toString() else text.toString().trim(' ')
    }

    private fun encode(text: String): String = text
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private companion object {
        val LOCALE = Regex("[a-z]{2,3}(-r[A-Z]{2})?")
    }
}

val syncSharedStrings = tasks.register<SyncSharedStringsTask>("syncSharedStrings") {
    val appRes = rootProject.layout.projectDirectory.dir("app/src/main/res")
    names.set(layout.projectDirectory.file("shared-strings.txt"))
    appStrings.from(appRes.asFileTree.matching { include("values*/strings*.xml") })
    appArrays.from(appRes.asFileTree.matching { include("values/arrays*.xml") })
    staticResources.set(layout.projectDirectory.dir("src/commonMain/composeResources"))
    outputDir.set(layout.buildDirectory.dir("generated/sharedComposeResources/commonMain"))
}

compose.resources {
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = syncSharedStrings.flatMap { it.outputDir }
    )
}

/**
 * Lists the app's bundled text files (`app/src/main/res/raw`) as [RawText] constants in `RawTexts`, so common code
 * names them the way Android code names `R.raw` resources. The files themselves stay in the app, which reads them.
 */
@CacheableTask
abstract class GenerateRawTextsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val rawFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val names = rawFiles.files.map { it.nameWithoutExtension }.toSortedSet()
        val out = outputDir.get().asFile
        out.deleteRecursively()
        val file = File(out, "com/kylecorry/trail_sense/shared/text/RawTexts.kt")
        file.parentFile.mkdirs()
        file.writeText(buildString {
            appendLine("// Generated by :shared:generateRawTexts from app/src/main/res/raw. Do not edit.")
            appendLine("package com.kylecorry.trail_sense.shared.text")
            appendLine()
            appendLine("@Suppress(\"ObjectPropertyName\", \"unused\")")
            appendLine("object RawTexts {")
            names.forEach { appendLine("    val $it = RawText(\"$it\")") }
            appendLine("}")
        })
    }
}

val generateRawTexts = tasks.register<GenerateRawTextsTask>("generateRawTexts") {
    rawFiles.from(
        rootProject.layout.projectDirectory.dir("app/src/main/res/raw").asFileTree.matching { exclude("*.xml") }
    )
    outputDir.set(layout.buildDirectory.dir("generated/rawTexts/commonMain/kotlin"))
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(generateRawTexts.map { it.outputDir })
}
