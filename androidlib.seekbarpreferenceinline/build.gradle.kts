configureAndroidLib(project)

dependencies {

    api project(":androidlib.core")
    api project(":androidlib.seekbar")

    api deps.support.preference
}
