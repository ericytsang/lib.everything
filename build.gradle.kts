// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {

    repositories {
        jcenter()
        google()
        mavenCentral()
        mavenLocal()
    }

    ext {
        lib_everything_version = '36.0.2'
        kotlin_version = '1.2.61'
        room_version = '1.1.1'
        paging_version = "1.0.1"
        work_manager_version = '1.0.0-alpha05'
        android_support_version = '27.1.1'
        firebase_version = '12.0.1'
        anko_version = '0.10.4'
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:3.1.4'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

subprojects {

    apply plugin: "maven"
    apply plugin: "kotlin"

    tasks.withType(JavaCompile) {
        sourceCompatibility = JavaVersion.VERSION_1_6
        targetCompatibility = JavaVersion.VERSION_1_6
    }

    repositories {
        jcenter()
        google()
        mavenCentral()
        maven { url 'https://maven.fabric.io/public' }
        maven { url 'https://maven.google.com/' }
        mavenLocal()
    }

    dependencies {
        compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
        compile "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"

        testCompile 'junit:junit:4.12'
        testCompile "org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version"
    }

    if (name != "lib.testutils") {
        dependencies {
            testCompile project(":lib.testutils")
        }
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
