import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import org.gradle.kotlin.dsl.support.zipTo
import java.util.regex.Pattern

apply {
    plugin("kotlin")
}

plugins {
    id("com.github.jk1.tcdeps") version "0.17"
}

repositories {
    teamcityServer {
        setUrl("http://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val kotlinVersion = rootProject.extra["kotlinVersion"] as String
val cidrPluginDir: File by rootProject.extra
val appcodePluginDir: File by rootProject.extra
val appcodeVersion = rootProject.extra["versions.appcode"] as String
val appcodeVersionRepo = rootProject.extra["versions.appcode.repo"] as String

val cidrPlugin by configurations.creating
val platformDepsZip by configurations.creating

val pluginXmlPath = "META-INF/plugin.xml"
val javaPsiXmlPath = "META-INF/JavaPsiPlugin.xml"
val javaPluginXmlPath = "META-INF/JavaPlugin.xml"
val platformDepsJarName = "kotlinNative-platformDeps.jar"
val resourcesJarName = "resources_en.jar"
val pluginXmlLocation = File(buildDir, "pluginXml")

// Do not rename, used in JPS importer
val projectsToShadow by extra(
    listOf(
        ":kotlin-ultimate:cidr-native",
        ":kotlin-ultimate:appcode-native"
    )
)

dependencies {
    cidrPlugin(project(":prepare:cidr-plugin"))
    platformDepsZip(tc("$appcodeVersionRepo:$appcodeVersion:OC-plugins/kotlinNative-platformDeps-$appcodeVersion.zip"))
}

fun renamePluginXml(plugin: Configuration, jarFile: File, toName: String) = tasks.creating {
    inputs.files(plugin)
    outputs.files(fileFrom(buildDir, name, "META-INF/$toName"))

    doFirst {
        val pluginXmlText = zipTree(jarFile)
            .matching { include(pluginXmlPath) }
            .singleFile
            .readText()
        outputs.files.singleFile.writeText(pluginXmlText)
    }
}

val kotlinPluginXml by renamePluginXml(cidrPlugin, cidrPlugin.singleFile, "KotlinPlugin.xml")

val resourcesJar by task<Zip> {
    archiveName = resourcesJarName
    val platformDepsJar = zipTree(platformDepsZip.singleFile).matching { include("**/$resourcesJarName") }.singleFile
    from(zipTree(platformDepsJar))
}

val preparePluginXml by task<Copy> {
    dependsOn(":kotlin-ultimate:appcode-native:assemble")

    val cidrPluginVersion = project.findProperty("cidrPluginVersion") as String? ?: "beta-1"
    val appcodePluginVersion = "$kotlinVersion-AppCode-$cidrPluginVersion-$appcodeVersion"

    inputs.property("appcodePluginVersion", appcodePluginVersion)

    from(project(":kotlin-ultimate:appcode-native").mainSourceSet.output.resourcesDir) { include(pluginXmlPath) }
    into(pluginXmlLocation)

    val sinceBuild =
        if (appcodeVersion.matches(Regex("\\d+\\.\\d+"))) appcodeVersion else appcodeVersion.substring(0, appcodeVersion.lastIndexOf('.'))
    val untilBuild = appcodeVersion.substring(0, appcodeVersion.lastIndexOf('.')) + ".*"

    filter {
        it
            .replace(
                "<!--idea_version_placeholder-->",
                "<idea-version since-build=\"$sinceBuild\" until-build=\"$untilBuild\"/>"
            )
            .replace(
                "<!--version_placeholder-->",
                "<version>$appcodePluginVersion</version>"
            )
    }
}

val jar = runtimeJar {
    archiveName = "kotlin-plugin.jar"
    dependsOn(cidrPlugin)
    dependsOn(preparePluginXml)
    from(kotlinPluginXml) { into("META-INF") }

    from {
        zipTree(cidrPlugin.singleFile).matching {
            exclude(pluginXmlPath)
        }
    }

    for (p in projectsToShadow) {
        dependsOn("$p:classes")
        from(getSourceSetsFrom(p)["main"].output) {
            exclude(pluginXmlPath)
        }
    }
    from(pluginXmlLocation) { include(pluginXmlPath) }
}

fun Zip.includePatched(fileToMarkers: Map<String, List<String>>) {
    val notDone = mutableSetOf<Pair<String, String>>()
    fileToMarkers.forEach { (path, markers) ->
        for (marker in markers) {
            notDone += path to marker
        }
    }

    eachFile {
        val markers = fileToMarkers[this.sourcePath] ?: return@eachFile
        this.filter {
            var data = it
            for (marker in markers) {
                val newData = data.replace(("^(.*" + Pattern.quote(marker) + ".*)$").toRegex(), "<!-- $1 -->")
                data = newData
                notDone -= path to marker
            }
            data
        }
    }
    doLast {
        check(notDone.size == 0) {
            "Filtering failed for: " +
                    notDone.joinToString(separator = "\n") { (file, marker) -> "file=$file, marker=`$marker`" }
        }
    }
}

val platformDepsJar by task<Zip> {
    archiveName = platformDepsJarName
    val platformDepsJar = zipTree(platformDepsZip.singleFile).matching { include("**/$platformDepsJarName") }.singleFile
    from(zipTree(platformDepsJar)) {
        exclude(pluginXmlPath)
        includePatched(
            mapOf(
                javaPsiXmlPath to listOf("implementation=\"org.jetbrains.uast.java.JavaUastLanguagePlugin\""),
                javaPluginXmlPath to listOf("implementation=\"com.intellij.spi.SPIFileTypeFactory\"")
            )
        )
    }
}

task<Copy>("appcodePlugin") {
    into(appcodePluginDir)
    from(cidrPluginDir) { exclude("lib/kotlin-plugin.jar") }
    from(jar) { into("lib") }
    from(platformDepsJar) { into("lib") }
    from(zipTree(platformDepsZip.singleFile).files) {
        exclude("**/$platformDepsJarName")
        exclude("**/$resourcesJarName")
        into("lib")
    }
    from(resourcesJar) { into("lib") }
    from(File(project(":kotlin-ultimate:appcode-native").projectDir, "templates")) { into("templates") }
}
