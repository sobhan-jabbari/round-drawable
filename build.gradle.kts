import java.util.Date
import java.text.SimpleDateFormat
import com.google.common.base.CaseFormat

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}


task("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

val appVersionCode by extra(1004)
val appVersionName by extra("1.0.4")


task("backup", Zip::class) {
    dependsOn("clean")
    from(projectDir)
    val folders =
        listOf(".git/**", ".gradle/**", ".idea/**", "build/**", "app/release/**", "app/build/**", "app/src/androidTest/**", "app/src/test/**")
    excludes.addAll(folders)
    val date = Date()
    val sdf = SimpleDateFormat("yyyy.MM.dd_HH.mm")
    val time = sdf.format(date)
    val projectName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, rootProject.name)
    archiveFileName.set(projectName + "_" + time + "_v" + appVersionName + ".zip")
    destinationDirectory.set(file("../"))
}