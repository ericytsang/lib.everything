import org.gradle.jvm.tasks.Jar

plugins {
    `kotlin-dsl`
}

subprojects {

    group = "com.github.ericytsang"
    version = "35.1.0-develop"

    repositories {
        jcenter()
    }

    plugins.apply("maven")
    plugins.apply("kotlin")

    plugins {
        maven
        kotlin("jvm")
    }

    tasks.withType(JavaCompile::class.java) {
        sourceCompatibility = JavaVersion.VERSION_1_6.toString()
        targetCompatibility = JavaVersion.VERSION_1_6.toString()
    }

    dependencies {
        compile(kotlin("stdlib"))
        compile(kotlin("reflect"))
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
