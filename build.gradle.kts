plugins {
    id("java")
    application
    id("com.gradleup.shadow") version "9.0.0"
}


group = "me.lupo"
version = "1.0.0"

application {
    mainClass.set("me.lupo.Main")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.6") // For arg parsing
    implementation("org.jetbrains:annotations:24.1.0") // For annotations


    // Get the current OS to only download one version of ffmpeg
    val os = System.getProperty("os.name").lowercase()
    val ffmpegPlatform = when {
        os.contains("win") -> "windows-x86_64"
        os.contains("linux") -> "linux-x86_64"
        os.contains("mac") -> "macosx-x86_64"
        else -> throw GradleException("Unsupported OS: $os")
    }

    // Für ffmpeg
    implementation("org.bytedeco:javacv:1.5.13")
    implementation("org.bytedeco:ffmpeg:7.1-1.5.13:$ffmpegPlatform")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
    archiveClassifier.set("") // ersetzt die normale jar
    manifest {
        attributes["Main-Class"] = "me.lupo.Main"
    }
}

tasks.test {
    useJUnitPlatform()
}
