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
    implementation("net.sf.jopt-simple:jopt-simple:5.0.4") // For arg parsing
    implementation("org.jetbrains:annotations:24.1.0") // For annotations

    // Für ffmpeg
    implementation("org.bytedeco:javacv:1.5.13")
    implementation("org.bytedeco:ffmpeg:6.1.1-1.5.10:windows-x86_64")

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
