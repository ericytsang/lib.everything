import org.gradle.jvm.tasks.Jar

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
        compile(kotlin("reflect"))
        testCompile("junit:junit:4.12")
        testCompile(kotlin("test"))
        testCompile(kotlin("test-junit"))
        testCompile("org.mockito:mockito-all:2.0.2-beta")
        {
            exclude("org.hamcrest","mockito-all")
        }
        if ("testutils" !in name)
        {
            testCompile(project(":lib.testutils"))
        }
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
