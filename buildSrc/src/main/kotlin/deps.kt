import org.gradle.api.artifacts.dsl.RepositoryHandler

import org.gradle.kotlin.dsl.*

/**
 * Shared file between builds so that they can all use the same dependencies and
 * maven repositories.
 **/
object deps
{
    private object versions
    {
        val activity = "1.0.0-rc01"
        val android_gradle_plugin = "3.5.0"
        val apache_commons = "2.5"
        val appcompat = "1.1.0-rc01"
        val arch_core = "2.0.1"
        val atsl_core = "1.2.0"
        val atsl_junit = "1.1.1"
        val atsl_rules = "1.2.0"
        val atsl_runner = "1.2.0"
        val atsl_uiautomator = "2.2.0"
        val benchmark = "1.0.0-alpha04"
        val constraint_layout = "2.0.0-alpha2"
        val core_ktx = "1.0.0"
        val crashlytics = "1.26.1" // todo: please check if this is the right version when adding dependencies to app modules
        val dagger = "2.16"
        val dexmaker = "2.2.0"
        val espresso = "3.2.0"
        val fragment = "1.2.0-alpha02"
        val glide = "4.8.0"
        val google_services = "4.2.0"
        val hamcrest = "1.3"
        val joda = "2.10.1"
        val junit = "4.12"
        val koin = "2.0.1"
        val kotlin = "1.3.41"
        val lifecycle = "2.2.0-alpha04"
        val mockito = "2.25.0"
        val mockito_all = "1.10.19"
        val mockito_android = "2.25.0"
        val mockwebserver = "3.8.1"
        val navigation = "2.2.0-alpha01"
        val okhttp_logging_interceptor = "3.9.0"
        val paging = "2.1.0-rc01"
        val retrofit = "2.3.0"
        val robolectric = "4.2"
        val robovm="2.3.7"
        val room = "2.1.0-alpha06"
        val rx_android = "2.0.1"
        val rxjava2 = "2.2.12"
        val support = "1.0.0"
        val timber = "4.5.1"
        val truth = "0.42"
        val work = "2.1.0"
    }

    // top-level dependencies
    val android_gradle_plugin = "com.android.tools.build:gradle:${versions.android_gradle_plugin}"
    val benchmark = "androidx.benchmark:benchmark-junit4:${versions.benchmark}"
    val benchmark_gradle = "androidx.benchmark:benchmark-gradle-plugin:${versions.benchmark}"
    val constraint_layout = "androidx.constraintlayout:constraintlayout:${versions.constraint_layout}"
    val dexmaker = "com.linkedin.dexmaker:dexmaker-mockito:${versions.dexmaker}"
    val hamcrest = "org.hamcrest:hamcrest-all:${versions.hamcrest}"
    val junit = "junit:junit:${versions.junit}"
    val mock_web_server = "com.squareup.okhttp3:mockwebserver:${versions.mockwebserver}"
    val okhttp_logging_interceptor = "com.squareup.okhttp3:logging-interceptor:${versions.okhttp_logging_interceptor}"
    val paging_ktx = "androidx.paging:paging-runtime-ktx:${versions.paging}"
    val robolectric = "org.robolectric:robolectric:${versions.robolectric}"
    val rxjava2 = "io.reactivex.rxjava2:rxjava:${versions.rxjava2}"
    val rx_android = "io.reactivex.rxjava2:rxandroid:${versions.rx_android}"
    val timber = "com.jakewharton.timber:timber:${versions.timber}"
    val truth = "com.google.truth:truth:${versions.truth}"

    // android sdk build versions
    object build_versions
    {
        val min_sdk = 14
        val target_sdk = 28
        val build_tools = "28.0.3"
    }

    //////////////////
    // dependencies //
    //////////////////

    object support
    {
        val annotations = "androidx.annotation:annotation:${versions.support}"
        val app_compat = "androidx.appcompat:appcompat:${versions.appcompat}"
        val recyclerview = "androidx.recyclerview:recyclerview:${versions.support}"
        val cardview = "androidx.cardview:cardview:${versions.support}"
        val design = "com.google.android.material:material:${versions.support}"
        val core_utils = "androidx.legacy:legacy-support-core-utils:${versions.support}"
        val core_ktx = "androidx.core:core-ktx:${versions.core_ktx}"
        val fragment_runtime = "androidx.fragment:fragment:${versions.fragment}"
        val fragment_runtime_ktx = "androidx.fragment:fragment-ktx:${versions.fragment}"
        val fragment_testing = "androidx.fragment:fragment-testing:${versions.fragment}"
        val preference = "androidx.preference:preference:${versions.support}"
    }

    object room
    {
        val runtime = "androidx.room:room-runtime:${versions.room}"
        val compiler = "androidx.room:room-compiler:${versions.room}"
        val rxjava2 = "androidx.room:room-rxjava2:${versions.room}"
        val testing = "androidx.room:room-testing:${versions.room}"
    }

    object lifecycle
    {
        val runtime = "androidx.lifecycle:lifecycle-runtime:${versions.lifecycle}"
        val java8 = "androidx.lifecycle:lifecycle-common-java8:${versions.lifecycle}"
        val compiler = "androidx.lifecycle:lifecycle-compiler:${versions.lifecycle}"
        val viewmodel_ktx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${versions.lifecycle}"
        val livedata_ktx = "androidx.lifecycle:lifecycle-livedata-ktx:${versions.lifecycle}"
    }

    object activity
    {
        val activity_ktx = "androidx.activity:activity-ktx:${versions.activity}"
    }

    object arch_core
    {
        val runtime = "androidx.arch.core:core-runtime:${versions.arch_core}"
        val testing = "androidx.arch.core:core-testing:${versions.arch_core}"
    }

    object retrofit
    {
        val runtime = "com.squareup.retrofit2:retrofit:${versions.retrofit}"
        val gson = "com.squareup.retrofit2:converter-gson:${versions.retrofit}"
        val mock = "com.squareup.retrofit2:retrofit-mock:${versions.retrofit}"
    }

    object dagger
    {
        val runtime = "com.google.dagger:dagger:${versions.dagger}"
        val android = "com.google.dagger:dagger-android:${versions.dagger}"
        val android_support = "com.google.dagger:dagger-android-support:${versions.dagger}"
        val compiler = "com.google.dagger:dagger-compiler:${versions.dagger}"
        val android_support_compiler = "com.google.dagger:dagger-android-processor:${versions.dagger}"
    }

    object espresso
    {
        val core = "androidx.test.espresso:espresso-core:${versions.espresso}"
        val contrib = "androidx.test.espresso:espresso-contrib:${versions.espresso}"
        val intents = "androidx.test.espresso:espresso-intents:${versions.espresso}"
    }

    object atsl
    {
        val core = "androidx.test:core:${versions.atsl_core}"
        val ext_junit = "androidx.test.ext:junit:${versions.atsl_junit}"
        val runner = "androidx.test:runner:${versions.atsl_runner}"
        val rules = "androidx.test:rules:${versions.atsl_rules}"
        val ui_automator = "androidx.test.uiautomator:uiautomator:${versions.atsl_uiautomator}"
    }

    object mockito
    {
        val core = "org.mockito:mockito-core:${versions.mockito}"
        val all = "org.mockito:mockito-all:${versions.mockito_all}"
        val android = "org.mockito:mockito-android:${versions.mockito_android}"
    }

    object kotlin
    {
        val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${versions.kotlin}"
        val reflect = "org.jetbrains.kotlin:kotlin-reflect:${versions.kotlin}"
        val test = "org.jetbrains.kotlin:kotlin-test-junit:${versions.kotlin}"
        val plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}"
        val allopen = "org.jetbrains.kotlin:kotlin-allopen:${versions.kotlin}"
    }

    object glide
    {
        val runtime = "com.github.bumptech.glide:glide:${versions.glide}"
        val compiler = "com.github.bumptech.glide:compiler:${versions.glide}"
    }

    object crashlytics
    {
        val google_services_plugin = "com.google.gms:google-services:${versions.google_services}"
        val fabric_plugin = "io.fabric.tools:gradle:${versions.crashlytics}"
    }

    object robovm
    {
        val rt = "com.mobidevelop.robovm:robovm-rt:${versions.robovm}"
        val cocoatouch = "com.mobidevelop.robovm:robovm-cocoatouch:${versions.robovm}"
        val plugin = "com.mobidevelop.robovm:robovm-gradle-plugin:${versions.robovm}"
    }

    object work
    {
        val runtime = "androidx.work:work-runtime:${versions.work}"
        val testing = "androidx.work:work-testing:${versions.work}"
        val runtime_ktx = "androidx.work:work-runtime-ktx:${versions.work}"
    }

    object navigation
    {
        val runtime = "androidx.navigation:navigation-runtime:${versions.navigation}"
        val runtime_ktx = "androidx.navigation:navigation-runtime-ktx:${versions.navigation}"
        val fragment = "androidx.navigation:navigation-fragment:${versions.navigation}"
        val fragment_ktx = "androidx.navigation:navigation-fragment-ktx:${versions.navigation}"
        val ui = "androidx.navigation:navigation-ui:${versions.navigation}"
        val ui_ktx = "androidx.navigation:navigation-ui-ktx:${versions.navigation}"
        val safe_args_plugin = "androidx.navigation:navigation-safe-args-gradle-plugin:${versions.navigation}"
    }

    object joda
    {
        val time = "joda-time:joda-time:${versions.joda}"
    }

    // koin x kotlin
    object koin
    {
        val core = /* implementation */ "org.koin:koin-core:${versions.koin}"   // Koin for Kotlin
        val ext = /* implementation */ "org.koin:koin-core-ext:${versions.koin}"   // Koin extended & experimental features
        val test = /* testImplementation */ "org.koin:koin-test:${versions.koin}"   // Koin for Unit tests
        val java = /* implementation */ "org.koin:koin-java:${versions.koin}"   // Koin for Java developers
    }

    // koin x android
    object koin_android
    {
        val core = /* implementation */ "org.koin:koin-android:${versions.koin}"   // Koin for Android
        val scope = /* implementation */ "org.koin:koin-android-scope:${versions.koin}"   // Koin Android Scope features
        val viewmodel = /* implementation */ "org.koin:koin-android-viewmodel:${versions.koin}"   // Koin Android ViewModel features
        val ext = /* implementation */ "org.koin:koin-android-ext:${versions.koin}"   // Koin Android Experimental features
    }

    // koin x androidx
    object koin_androidx
    {
        val scope = /* implementation */ "org.koin:koin-androidx-scope:${versions.koin}"   // Koin AndroidX Scope features
        val viewmodel = /* implementation */ "org.koin:koin-androidx-viewmodel:${versions.koin}"   // Koin AndroidX ViewModel features
        val ext = /* implementation */ "org.koin:koin-androidx-ext:${versions.koin}"   // Koin AndroidX Experimental features
    }

    // koin x ktor
    object koin_ktor
    {
        val ext = /* implementation */ "org.koin:koin-ktor:${versions.koin}"   // Koin for Ktor Kotlin
    }
}
