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
        executeCommand("git status","nothing to commit, working tree clean",{"working branch not clean"})

        // make sure there is no conflicting release
        executeCommand("git fetch",0)
        executeCommand("git tag -l | grep -Fx $projectVersion",projectVersion,{"a tag with the name \"$projectVersion\" already exists; please update version number:\n$it"})

        // add tag and push
        executeCommand("git tag -a \"$projectVersion\" -m \"v$projectVersion\"",0)
        executeCommand("git push origin $projectVersion",0)
    }
}

fun executeCommand(
        command:String,
        expectedReturnValue:Int=0,
        failureMessage:(actual:Int)->String={"execution of command \"$command\" returned $it instead of $expectedReturnValue"})
{
    val returnValue = Runtime.getRuntime().exec(command).waitFor()
    if (returnValue != expectedReturnValue)
    {
        throw Exception(failureMessage(returnValue))
    }
}

fun executeCommand(
        command:String,
        outputShouldContain:String,
        failureMessage:(actualOutput:String)->String={"output of command \"$command\" did not contain \"$outputShouldContain\". raw output:\n$it"})
{
    val process = Runtime.getRuntime().exec(command)
    val outputOfProcess = StringBuilder()
    val dataRead = ByteArray(1024)
    val len = process.inputStream.read(dataRead)
    outputOfProcess.append(String(dataRead,0,len))
    process.waitFor()
    if (outputShouldContain !in outputOfProcess.toString())
    {
        throw Exception(failureMessage(outputOfProcess.toString()))
    }
}
