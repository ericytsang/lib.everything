configureAndroidLib(project)

dependencies {

    api project(":androidlib.core")
    api project(":lib.datastore")
    api project(":lib.setofatleastone")
    api project(":lib.domainobjects")

    // joda time
    api deps.joda.time

    // work manager
    api deps.work.runtime_ktx

    // app compat
    api deps.support.core_utils
    api deps.support.app_compat
    api deps.support.recyclerview
}
