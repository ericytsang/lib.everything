import org.gradle.jvm.tasks.Jar

//buildscript {
//    ext {
//        kotlin_version = '1.1.51'
//    }
//    repositories {
//        mavenCentral()
//    }
//    dependencies {
//        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
//    }
//}
//

plugins {
    maven
    `kotlin-dsl`
    kotlin("jvm")
}

subprojects {

    group = "com.github.ericytsang"
    version = "30.0.0"

    repositories {
        jcenter()
    }

    plugins.apply("maven")
    plugins.apply("kotlin")

    tasks.withType(JavaCompile::class.java) {
        sourceCompatibility = JavaVersion.VERSION_1_6.toString()
        targetCompatibility = JavaVersion.VERSION_1_6.toString()
    }

    dependencies {
        compile(kotlin("stdlib"))
        testCompile("junit:junit:4.11")
        testCompile(kotlin("test-junit"))
    }

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        jcenter()
    }

    val sourcesJarTask = task("sourcesJar",Jar::class) {
        dependsOn(tasks.find {it.name == "classes"}!!)
        classifier = "sources"
        from(java.sourceSets["main"].allSource)
    }

    artifacts {
        add("archives",sourcesJarTask)
    }
}
