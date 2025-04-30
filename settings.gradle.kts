pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        //maven { url = uri("https://jitpack.io") }
        //maven { url = file("../VehicleHealthNativeApp/node_modules/react-native/android").toURI() }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        //maven { url = uri("https://jitpack.io") }
        //maven { url = file("../VehicleHealthNativeApp/node_modules/react-native/android").toURI() }
    }
}

rootProject.name = "VehicleHealth"
include(":app")
//include(":reactnative")

// Link to the React Native project's directory
//project(":reactnative").projectDir = file("../../VehicleHealthNativeApp/android")
 