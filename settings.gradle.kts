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
        maven { url = uri("https://jitpack.io") }
        // ⚠️ Añade JCenter solo si es necesario
        maven { url = uri("https://jcenter.bintray.com/") }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        // ⚠️ Añade JCenter solo para dependencias viejas como kprogresshud
        maven(url = "https://jcenter.bintray.com/")
    }
}

rootProject.name = "SENA Monitoreo"
include(":app")
include(":mylibrary")
