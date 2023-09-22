plugins {
  `java-library`
  id("maven-publish")
}

group = "dev.mcenv"
version = "0.5.0"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
  maven("https://libraries.minecraft.net")
}

dependencies {
  compileOnly("org.jetbrains:annotations:24.0.1")
  compileOnlyApi("com.mojang:brigadier:1.1.8")
  implementation("org.ow2.asm:asm:9.5")
}

tasks.jar {
  manifest {
    attributes(
      "Premain-Class" to "dev.mcenv.spy.Agent",
    )
  }
}

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/mcenv/spy")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
  publications {
    register<MavenPublication>("gpr") {
      from(components["java"])
    }
  }
}
