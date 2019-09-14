buildscript {
    addRepos(repositories)
    dependencies {
        classpath(deps.android_gradle_plugin)
        classpath(deps.kotlin.plugin)
        classpath(deps.robovm.plugin)
        classpath(deps.crashlytics.google_services_plugin)
        classpath(deps.crashlytics.fabric_plugin)
    }
}

allprojects {
    addRepos(repositories)
}

tasks {
    register("clean", Delete::class) {
        delete(buildDir)
    }
}
