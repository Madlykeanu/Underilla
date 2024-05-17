plugins {
    `java-library`
    id("io.github.goooler.shadow") version "8.1.7"
    id("io.papermc.paperweight.userdev") version "1.7.0" // paperweight // Check for new versions at https://plugins.gradle.org/plugin/io.papermc.paperweight.userdev
}

group = "com.Jkantrell.mc"
version = "1.4.3"

repositories {
    mavenLocal()
    mavenCentral()
    maven ("https://repo.papermc.io/repository/maven-public/")
    maven ("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven ("https://oss.sonatype.org/content/groups/public/")
    maven ("https://jitpack.io")
}


dependencies {
    // compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT") // without paperweight
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT") // paperweight
    implementation("com.jkantrell:Yamlizer:main-SNAPSHOT")
    implementation(project(":Underilla-Core"))
}

// tasks.build.dependsOn tasks.reobfJar // paperweight
// tasks.build.dependsOn tasks.shadowJar // without paperweight

tasks {
    shadowJar {
        minimize()
        val prefix = "${project.group}.lib"
        sequenceOf(
            "co.aikar",
            "org.bstats",
            "jakarta.annotation",
        ).forEach { pkg ->
            relocate(pkg, "$prefix.$pkg")
        }
    }

    assemble {
        // dependsOn(shadowJar) // Not needed, probably because reobfJar depends on shadowJar
        dependsOn(reobfJar)
    }

    compileJava {
        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }
}