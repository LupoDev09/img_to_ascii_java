plugins {
    id("java")
    application
}

group = "me.lupo"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("me.lupo.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.sf.jopt-simple:jopt-simple:5.0.4") // For arg parsing

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
