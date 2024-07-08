/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/8.1.1/userguide/multi_project_builds.html
 */
// pluginManagement {
//     repositories {
//         maven {
//             name = "Fabric"
//             url = "https://maven.fabricmc.net/"
//         }
//         gradlePluginPortal()
//     }
// }


plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

rootProject.name = "Underilla"
include("Underilla-Core", "Underilla-Spigot") //, "Underilla-Fabric"