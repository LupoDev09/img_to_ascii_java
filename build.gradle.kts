plugins {
    id("java")
    application
}

group = "me.lupo"
version = "1.0.0"

application {
    mainClass.set("me.lupo.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.sf.jopt-simple:jopt-simple:5.0.4") // For arg parsing
    implementation("org.jetbrains:annotations:24.1.0") // For annotations

    implementation("org.bytedeco:javacv-platform:1.5.13") // Für ffmpeg

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "me.lupo.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}
