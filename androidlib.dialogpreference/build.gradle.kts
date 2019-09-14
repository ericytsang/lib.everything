configureAndroidLib(project)

dependencies {

    api project(":androidlib.core")
    
    // androidx preferences
    api deps.support.preference
}
