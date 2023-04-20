plugins {
  `java-library`
}

version = "0.1.0"

repositories {
  mavenCentral()
  maven("https://libraries.minecraft.net")
}

dependencies {
  api("com.mojang:brigadier:1.1.8")
  implementation("org.ow2.asm:asm:9.5")
}

tasks.jar {
  manifest {
    attributes(
      "Premain-Class" to "spy.Agent",
    )
  }
}
