import org.apache.tools.ant.taskdefs.Java
import org.gradle.internal.impldep.org.apache.commons.io.output.ByteArrayOutputStream
import org.gradle.internal.impldep.org.bouncycastle.util.Properties
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.com.intellij.util.io.DataInputOutputUtil
import java.util.Properties as JavaProperties

plugins {
    `kotlin-dsl`
}

allprojects {

    group = "com.github.ericytsang"
    version = "36.0.0"

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

task("install_tag_and_push")
{
    dependsOn.addAll(allprojects
        .iterator().asSequence()
        .flatMap {it.tasks.toList().asSequence()}
        .filter {it.name == "install"})

    actions.apply {} += Action<Task> {

        // make sure working branch is clean
        if (!isWorkingBranchClean()) throw Exception("working branch not clean")

        // add tag and push
        check(Runtime.getRuntime().exec("git tag -a \"$version\" -m \"v$version\"").waitFor() == 0)
        check(Runtime.getRuntime().exec("git push origin $version").waitFor() == 0)
    }
}

fun isWorkingBranchClean():Boolean
{
    val process = Runtime.getRuntime().exec("git status")
    val dataRead = ByteArray(1024)
    val len = process.inputStream.read(dataRead)
    val processOutput = String(dataRead,0,len)
    check(process.waitFor() == 0)
    return "nothing to commit, working tree clean" in processOutput
}
