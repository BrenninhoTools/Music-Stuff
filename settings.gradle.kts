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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Music-Stuff"
val modules = providers.gradleProperty("musicstuff.modules").orNull?.split(',')?.map { it.trim() }?.toSet()
fun wanted(name: String) = modules == null || name in modules
if (wanted("app")) include(":app")
if (wanted("desktop")) include(":desktop")
