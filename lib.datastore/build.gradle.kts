configureKotlin(project)

dependencies {
    api project(":lib.awaitable")
    api deps.rxjava2
}
