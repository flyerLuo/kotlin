/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


buildscript {
    val buildSrcKotlinVersion: String by extra(findProperty("buildSrc.kotlin.version")?.toString() ?: embeddedKotlinVersion)
    val buildSrcKotlinRepo: String? by extra(findProperty("buildSrc.kotlin.repo") as String?)
    extra["versions.shadow"] = "2.0.1"
    extra["versions.intellij-plugin"] = "0.3.0-SNAPSHOT"
    extra["versions.native-platform"] = "0.14"

    repositories {
        buildSrcKotlinRepo?.let {
            maven(url = it)
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$buildSrcKotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$buildSrcKotlinVersion")
    }
}

logger.info("buildSrcKotlinVersion: " + extra["buildSrcKotlinVersion"])
logger.info("buildSrc kotlin compiler version: " + org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
logger.info("buildSrc stdlib version: " + KotlinVersion.CURRENT)

apply {
    plugin("kotlin")
    plugin("kotlin-sam-with-receiver")
}

plugins {
    `kotlin-dsl`
}

fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}

rootProject.apply {
    from(rootProject.file("../versions.gradle.kts"))
}

val intellijUltimateEnabled = project.getBooleanProperty("intellijUltimateEnabled")
                              ?: project.hasProperty("teamcity")
                              || System.getenv("TEAMCITY_VERSION") != null
val intellijSeparateSdks = project.getBooleanProperty("intellijSeparateSdks") ?: false
extra["intellijUltimateEnabled"] = intellijUltimateEnabled
extra["intellijSeparateSdks"] = intellijSeparateSdks
extra["intellijRepo"] = "https://www.jetbrains.com/intellij-repository"
extra["intellijReleaseType"] = "releases" // or "snapshots"
extra["versions.androidDxSources"] = "5.0.0_r2"

extra["customDepsRepo"] = "$rootDir/repo"
extra["customDepsOrg"] = "kotlin.build.custom.deps"

repositories {
    extra["buildSrcKotlinRepo"]?.let {
        maven(url = it)
    }
    maven(url = "https://dl.bintray.com/kotlin/kotlin-dev") // for dex-method-list
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local") // for native-platform
    jcenter()
}

dependencies {
    compile("net.rubygrapefruit:native-platform:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-amd64:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-i386:${property("versions.native-platform")}")
    compile("com.jakewharton.dex:dex-method-list:2.0.0-alpha")
    // TODO: adding the dep to the plugin breaks the build unexpectedly, resolve and uncomment
//    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["bootstrap_kotlin_version"]}")
    compile("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    compile("org.ow2.asm:asm-all:6.0_BETA")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)

tasks["build"].dependsOn(":prepare-deps:android-dx:build", ":prepare-deps:intellij-sdk:build")
