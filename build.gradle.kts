import java.util.zip.ZipFile

// MC Warfare NTM Extras: content from HBM's Nuclear Tech Mod for 1.7.10 (build X5863) that NTM NEXT 1.0.3 does not have
// yet, as an add-on to NTM NEXT. Built the same way as NTM NEXT: Space. NTM NEXT is only referenced, never shipped.
plugins {
    java
    id("net.neoforged.moddev") version "2.0.147"
}

version = property("mod_version")!!
group = property("mod_group_id")!!
val ntmVersion = "${property("hbm_version")}+mc${property("minecraft_version")}"

base {
    archivesName = property("mod_id") as String
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
    maven("https://repo.warfactory.co/releases") {
        name = "Warfactory"
        content {
            includeGroup("dev.engine_room")
            includeModule("com.hbm", "ntm-next-neoforge")
        }
    }
    maven("https://repo.warfactory.co/snapshots") {
        content { includeGroup("mov.movblock.tenon") }
    }
}

val ntmJar: File = configurations.detachedConfiguration(dependencies.create("com.hbm:ntm-next-neoforge:$ntmVersion"))
    .apply { isTransitive = false }.singleFile
val ntmAccess: File = rootProject.file(".gradle-ntm").also { directory ->
    directory.mkdirs()
    ZipFile(ntmJar).use { jar ->
        for (entry in listOf("META-INF/accesstransformer.cfg", "interface-injection.json")) {
            val bytes = jar.getInputStream(jar.getEntry(entry)).readBytes()
            directory.resolve(entry.substringAfterLast('/')).writeBytes(bytes)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--add-modules=jdk.incubator.vector")
}

val pin = property("hbm_version") as String
val expansion = mapOf("version" to version, "hbm_range" to "[$pin,$pin.1)")

tasks.named<ProcessResources>("processResources") {
    inputs.properties(expansion)
    filesMatching("META-INF/neoforge.mods.toml") { expand(expansion) }
}

tasks.named<Jar>("jar") {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    includeEmptyDirs = false
}

dependencies {
    compileOnly("com.hbm:ntm-next-neoforge:$ntmVersion")
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.5")
    compileOnly("mov.movblock.tenon:tenon-strip-api:${property("tenon_version")}")


    compileOnly("dev.engine_room:crankshaft-neoforge:1.5.9+mc26.2")
}

neoForge {
    version = property("neo_version") as String
    accessTransformers.from(ntmAccess.resolve("accesstransformer.cfg"))
    interfaceInjectionData.from(ntmAccess.resolve("interface-injection.json"))
}
