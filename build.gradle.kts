plugins {
    id("net.neoforged.moddev") version "2.0.78"
    kotlin("jvm") version "2.2.20"
}

val modId = property("mod_id") as String
val modName = property("mod_name") as String
val modVersion = property("mod_version") as String
val modGroup = property("mod_group") as String
val modAuthors = property("mod_authors") as String
val modDescription = property("mod_description") as String
val modLicense = property("mod_license") as String
val minecraftVersion = property("minecraft_version") as String
val neoforgeVersion = property("neoforge_version") as String
val kffVersion = property("kff_version") as String

group = modGroup
version = modVersion

base {
    archivesName.set(modId)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "KotlinForForge"
    }
}

neoForge {
    version = neoforgeVersion

    runs {
        create("client") {
            client()
            systemProperty("forge.logging.console.level", "debug")
        }
        create("server") {
            server()
            systemProperty("forge.logging.console.level", "debug")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:$kffVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to modName,
            "Implementation-Version" to modVersion,
        )
    }
}

tasks.processResources {
    val properties = mapOf(
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription,
        "mod_license" to modLicense,
        "minecraft_version" to minecraftVersion,
        "neoforge_version" to neoforgeVersion,
        "kff_version" to kffVersion,
    )

    inputs.properties(properties)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(properties)
    }
}
