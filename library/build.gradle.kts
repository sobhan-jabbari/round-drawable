plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}

val appVersionCode: Int by rootProject.extra
val appVersionName: String by rootProject.extra

group = "ir.afraapps"
version = appVersionName


android {
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    lint {
        abortOnError = false
    }
    namespace = "ir.afraapps.kotlin.component"

}

// test: ./gradlew clean -xtest -xlint assemble publishToMavenLocal
// test: ./gradlew clean -xtest -xlint assemble publishReleasePublicationToMavenLocal


publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "ir.afraapps"
            artifactId = "round-drawable"
            version = appVersionName

            pom {
                name.set(project.name)
                description.set("The basic tools for kotlin android")
                url.set("https://github.com/sobhan-jabbari/${project.name}")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("sobhan-jabbari")
                        name.set("Ali Jabbari")
                        email.set("sobhan.jabbari@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:github.com/sobhan-jabbari/${project.name}.git")
                    url.set("https://github.com/sobhan-jabbari/${project.name}")
                }
            }

            afterEvaluate {
                from(components["release"])
            }
        }
    }

    repositories {
        maven {
            name = "afraapps"
            url = uri("${project.layout.buildDirectory}/afraapps")
        }
    }

}


dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.appcompat:appcompat-resources:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.core:core-ktx:1.12.0")
}

