configureKotlin(project)

dependencies {
    api project(":lib.regulatedstream")
    api project(":lib.simplepipestream")
    api project(":lib.net")
    api project(":lib.onlycallonce")
    testImplementation project(":lib.concurrent")
}
