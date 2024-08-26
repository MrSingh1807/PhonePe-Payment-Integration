pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven(url = "https://phonepe.mycloudrepo.io/public/repositories/phonepe-intentsdk-android")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://phonepe.mycloudrepo.io/public/repositories/phonepe-intentsdk-android")
    }
}

rootProject.name = "PhonePe Integration"
include(":app")
