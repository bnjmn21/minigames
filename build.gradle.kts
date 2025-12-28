plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "2.3.1"
//    id("com.gradleup.shadow") version "9.3.0"
}

group = "bnjmn21"
version = "1.0"
description = "My minigame plugin"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")

    val scoreboardLibraryVersion = "2.4.4"
    implementation("net.megavex:scoreboard-library-api:$scoreboardLibraryVersion")
//    runtimeOnly("net.megavex:scoreboard-library-implementation:$scoreboardLibraryVersion")
//    runtimeOnly("net.megavex:scoreboard-library-modern:$scoreboardLibraryVersion")
}

tasks {
    compileJava {
        options.release = 21
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
//    shadowJar {
//        archiveClassifier = ""
//        dependencies {
//            include(dependency("net.megavex:scoreboard-library-api"))
//            include(dependency("net.megavex:scoreboard-library-implementation"))
//            include(dependency("net.megavex:scoreboard-library-modern"))
//        }
//    }
}
