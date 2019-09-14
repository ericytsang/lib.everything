import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.KotlinBuildScript
import org.gradle.kotlin.dsl.`java-library`
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.project
import java.io.ByteArrayOutputStream

fun addRepos(handler:RepositoryHandler)
{
    handler.google()
    handler.jcenter()
    handler.maven(url = "https://maven.fabric.io/public")
    handler.maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    handler.maven(url = "https://jitpack.io")
}

// for configuring android library projects
fun KotlinBuildScript.androidLibrary()
{
    plugins {
        id("com.android.library")
    }
    configureAndroidCommon()
}

// for configuring android library projects
fun KotlinBuildScript.androidApplication()
{
    plugins {
        id("com.android.application")
    }
    configureAndroidCommon()
}

private fun Project.getGitHash():String
{
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git","rev-parse","--short","HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

// for configuring android projects
fun KotlinBuildScript.configureAndroidCommon()
{
    plugins {
        kotlin("android")
        kotlin("android.extensions")
        kotlin("kapt")
    }

    /*
    android {
        compileSdkVersion build_versions . target_sdk
                buildToolsVersion build_versions . build_tools
                defaultConfig {
                    minSdkVersion build_versions . min_sdk
                            targetSdkVersion build_versions . target_sdk

                            testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"

                    vectorDrawables.useSupportLibrary true
                    multiDexEnabled true

                    buildConfigField "String","GIT_HASH","\"${getGitHash(p)}\""
                }
        dataBinding {
            enabled true
        }
        compileOptions {
            targetCompatibility JavaVersion . VERSION_1_8
                    sourceCompatibility JavaVersion . VERSION_1_8
        }
        kotlinOptions {
            jvmTarget = "1.8"
        }
        lintOptions {
            ignoreWarnings false
            abortOnError true
        }
        buildTypes {
            release {
                minifyEnabled false
                proguardFiles getDefaultProguardFile ('proguard-android.txt'),'proguard-rules.pro'
            }
        }
    }
    */

    dependencies {
        "androidTestImplementation"(deps.arch_core.testing)
        "androidTestImplementation"(deps.espresso.core)
        "androidTestImplementation"(deps.espresso.contrib)
        "androidTestImplementation"(deps.espresso.intents)
        "androidTestImplementation"(deps.atsl.ext_junit)
        "androidTestImplementation"(deps.atsl.ui_automator)
        "androidTestImplementation"(deps.work.testing)
        "androidTestImplementation"(deps.truth)
    }

    /*
    configurations.all {
    resolutionStrategy.eachDependency {
        details ->
        details
        if (details.requested.group == 'androidx.room'
                && !details.requested.name.contains('room-runtime')) {
            details.useVersion "2.1.0-alpha04"
        }
        if (details.requested.group == 'androidx.room'
                && !details.requested.name.contains('room-testing')) {
            details.useVersion "2.1.0-alpha04"
        }
        if (details.requested.group == 'androidx.room'
                && !details.requested.name.contains('room-rxjava2')) {
            details.useVersion "2.1.0-alpha04"
        }
        if (details.requested.group == 'androidx.room'
                && !details.requested.name.contains('room-common')) {
            details.useVersion "2.1.0-alpha04"
        }
        if (details.requested.group == 'androidx.room'
                && !details.requested.name.contains('room-migration')) {
            details.useVersion "2.1.0-alpha04"
        }
        if (details.requested.group == 'androidx.room'
                && !details.requested.name.contains('room-compiler')) {
            details.useVersion "2.1.0-alpha04"
        }
    }
    kotlinCommon(p)
    */
}

// for configuring plain kotlin projects
fun KotlinBuildScript.kotlinLibrary()
{
    plugins {
        `java-library`
        kotlin("jvm")
    }

    /*
    sourceCompatibility = "8"
    targetCompatibility = "8"
     */

    kotlinCommon()
}

// for configuring plain kotlin projects
fun KotlinBuildScript.kotlinCommon()
{
    dependencies {

        // kotlin
        "implementation"(deps.kotlin.stdlib)
        "implementation"(deps.kotlin.reflect)

        // test
        when
        {
            "lib.streamtest" in name -> "api"(project(":lib.testutils"))
            "lib.testutils" in name -> Unit // don't add project(":lib.testutils")
            else -> "testImplementation"(project(":lib.testutils"))
        }
    }
}
