
apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin {
    setPlugins("android", "gradle")
}

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:frontend"))
    compile(project(":idea")) { isTransitive = false }
    compile(project(":idea:kotlin-gradle-tooling"))
    compile(project(":kotlin-annotation-processing"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("openapi.jar", "external-system-rt.jar", "util.jar") })
        compileOnly(intellijPlugin("gradle") { include("gradle-core-*.jar", "gradle-tooling-api-*.jar", "gradle.jar") })
        compileOnly(intellijPlugin("android") { include("android.jar", "android-common.jar", "sdklib.jar", "sdk-common.jar", "sdk-tools.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar()

ideaPlugin {
    from(jar)
}
