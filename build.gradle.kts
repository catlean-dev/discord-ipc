plugins {
    kotlin("jvm") version "1.9.23"
    `maven-publish`
    java
}

group = "catlean"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
    implementation(kotlin("stdlib-jdk8"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "21"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "catlean"
            artifactId = "discord-ipc"
            version = project.version.toString()
        }
    }
}