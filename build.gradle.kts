import org.gradle.internal.impldep.org.bouncycastle.util.Properties
import org.gradle.jvm.tasks.Jar
import java.util.Properties as JavaProperties

plugins {
    `kotlin-dsl`
}

allprojects {

    group = "com.github.ericytsang"
    version = "35.2."+properties["artifact_version"]!!

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

val installCommitAllAndPushTask = task("install_add_all_commit_and_increment_version_number")
{
    dependsOn.addAll(tasks
        .filter {name != it.name}
        .filter {"install" in it.name})
    actions.apply {} += Action<Task> {
        Runtime.getRuntime().exec("git add --all").waitFor()
        Runtime.getRuntime().exec("git commit -s -m \"v$version\"").waitFor()
        Runtime.getRuntime().exec("git push origin master").waitFor()

        val properties = JavaProperties()
        val propsFile = File("gradle.properties")
        properties.load(propsFile.inputStream())
        val currVresionNumber = properties["artifact_version"].toString().toLong()
        val nextVersionNumber = currVresionNumber.plus(1)
        properties.setProperty("artifact_version",nextVersionNumber.toString())
        properties.store(propsFile.outputStream(),null)
    }
}

task("tag_and_push")
{
    dependsOn.add(installCommitAllAndPushTask)
    actions.apply {} += Action<Task> {
        Runtime.getRuntime().exec("git tag -a \"$version\" -m \"release v$version\"").waitFor()
        Runtime.getRuntime().exec("git push origin $version").waitFor()
    }
}
