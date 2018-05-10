import org.apache.tools.ant.taskdefs.Java
import org.gradle.internal.impldep.org.bouncycastle.util.Properties
import org.gradle.jvm.tasks.Jar
import java.util.Properties as JavaProperties

plugins {
    `kotlin-dsl`
}

val projectVersion = "36.0.1"

subprojects {

    val kotlinVersion = "1.2.40"

    group = "com.github.ericytsang"
    version = projectVersion

    plugins.apply("maven")
    plugins.apply("kotlin")

    tasks.withType(JavaCompile::class.java) {
        sourceCompatibility = JavaVersion.VERSION_1_6.toString()
        targetCompatibility = JavaVersion.VERSION_1_6.toString()
    }

    dependencies {
        compile(kotlin("stdlib",kotlinVersion))
        compile(kotlin("reflect",kotlinVersion))
        if (name != "lib.testutils")
        {
            testCompile(project(":lib.testutils"))
        }
    }

    repositories {
        jcenter()
        mavenCentral()
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
    // install all modules
    dependsOn.addAll(allprojects
        .iterator().asSequence()
        .flatMap {it.tasks.toList().asSequence()}
        .filter {it.name == "install"})

    actions.apply {} += Action<Task> {

        // make sure working branch is clean
        executeCommand("git status","nothing to commit, working tree clean","working branch not clean")

        // make sure there is no conflicting release
        executeCommand("git fetch",0)
        executeCommand("git tag -l | grep -Fx $projectVersion",projectVersion,"a tag with the name \"$projectVersion\" already exists; please update version number")

        // add tag and push
        check(Runtime.getRuntime().exec("git tag -a \"$projectVersion\" -m \"v$projectVersion\"").waitFor() == 0)
        check(Runtime.getRuntime().exec("git push origin $projectVersion").waitFor() == 0)
    }
}

fun isThereAnExistingReleaseNamed(name:String):Boolean
{
    executeCommand("git fetch",0)
    executeCommand("git status","nothing to commit, working tree clean","working branch not clean")
}

fun executeCommand(
        command:String,
        expectedReturnValue:Int=0,
        failureMessage:String="execution of command: \"$command\" returned return: \"$expectedReturnValue\"")
{
    val returnValue = Runtime.getRuntime().exec(command).waitFor()
    if (returnValue != expectedReturnValue)
    {
        throw Exception(failureMessage,Exception("return value of command: $returnValue"))
    }
}

fun executeCommand(
        command:String,
        expectedOutput:String,
        failureMessage:String="output of command: \"$command\" did not contain: \"$expectedOutput\"")
{
    val process = Runtime.getRuntime().exec(command)
    val outputOfProcess = StringBuilder()
    val dataRead = ByteArray(1024)
    val len = process.inputStream.read(dataRead)
    outputOfProcess.append(String(dataRead,0,len))
    process.waitFor()
    if (expectedOutput !in outputOfProcess.toString())
    {
        throw Exception(failureMessage,Exception("output of command:\n$outputOfProcess"))
    }
}
