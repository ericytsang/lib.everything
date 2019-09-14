configureKotlin(project)

dependencies {
    api project(":lib.abstractstream")
    api project(":lib.simplepipestream")
    testImplementation project(":lib.streamtest")
}
