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

fun commitAllAndIncrementPropertiesFile(makeTagToo:Boolean)
{
    val properties = JavaProperties()
    val propsFile = File("gradle.properties")
    properties.load(propsFile.inputStream())
    val commitMessage = properties["commit_message"]
        ?.toString()
        ?.takeIf {!it.isBlank()}
        ?:throw RuntimeException("must add commit message")

    check(Runtime.getRuntime().exec("git add --all").waitFor() == 0)
    check(Runtime.getRuntime().exec("git commit -s -m \"v$version: $commitMessage\"").waitFor() == 0)
    check(Runtime.getRuntime().exec("git push").waitFor() == 0)
    if (makeTagToo)
    {
        check(Runtime.getRuntime().exec("git tag -a \"$version\" -m \"v$version: $commitMessage\"").waitFor() == 0)
        check(Runtime.getRuntime().exec("git push origin $version").waitFor() == 0)
    }

    val currVresionNumber = properties["artifact_version"].toString().toLong()
    val nextVersionNumber = currVresionNumber.plus(1)
    properties["artifact_version"] = nextVersionNumber.toString()
    properties["commit_message"] = ""
    properties.store(propsFile.outputStream(),null)
}

val installCommitAllAndPushTask = task("install_commit_and_push")
{
    dependsOn.addAll(allprojects
        .iterator().asSequence()
        .flatMap {it.tasks.toList().asSequence()}
        .filter {it.name == "install"})
    actions.apply {} += Action<Task> {
        commitAllAndIncrementPropertiesFile(false)
    }
}

task("install_commit_tag_and_push")
{
    dependsOn.addAll(installCommitAllAndPushTask.dependsOn)
    actions.apply {} += Action<Task> {
        commitAllAndIncrementPropertiesFile(true)
    }
}
