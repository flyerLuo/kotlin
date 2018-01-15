
description = "Kotlin Android Extensions IDEA"

apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin {
    setPlugins("android", "copyright", "coverage", "gradle", "Groovy", "IntelliLang",
               "java-decompiler", "java-i18n", "junit", "maven", "properties", "testng")
}

val androidSdk by configurations.creating

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:light-classes"))
    compile(project(":idea:idea-core"))
    compile(project(":idea"))
    compile(project(":idea:idea-gradle"))
    compile(project(":plugins:android-extensions-compiler"))
    compileOnly(project(":kotlin-android-extensions-runtime"))

    testCompile(project(":compiler:tests-common"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:kapt3-idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-android"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(commonDep("junit:junit"))
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":plugins:android-extensions-jps"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":plugins:lint"))
    androidSdk(project(":custom-dependencies:android-sdk", configuration = "androidSdk"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellijPlugin("android") { include("android.jar", "android-common.jar", "sdk-common.jar", "sdk-tools.jar") })
        compileOnly(intellijPlugin("Groovy") { include("Groovy.jar") })
        compileOnly(intellij { include("extensions.jar", "openapi.jar", "util.jar", "idea.jar")} )
        testCompile(intellijPlugin("android") { include("android.jar", "android-common.jar", "sdk-common.jar", "sdk-tools.jar") })
        testCompile(intellijPlugin("Groovy") { include("Groovy.jar") })
        testCompile(intellij { include("extensions.jar")} )
        testRuntime(intellij())
        testRuntime(intellijPlugins("junit", "IntelliLang", "testng", "copyright", "properties", "java-i18n",
                                    "gradle", "Groovy", "coverage", "java-decompiler", "maven", "android"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    dependsOn(":kotlin-android-extensions-runtime:dist")
    dependsOn(androidSdk)
    workingDir = rootDir
    doFirst {
        systemProperty("android.sdk", androidSdk.singleFile.canonicalPath)
    }
}

runtimeJar()

ideaPlugin()
