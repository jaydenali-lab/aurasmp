plugins {
    java
}

group = "com.aurasmp"
version = "1.13.1"

// Paper API version. Targeting the 1.21.x line (api-version "1.21" in plugin.yml
// covers every 1.21.x server, so this jar runs on 1.21.11 too).
val paperApiVersion = "1.21.8-R0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.withType<JavaCompile> {
    // Source contains private-use glyph chars (ruin:icons font) — read them as UTF-8.
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("Ruin")
    archiveClassifier.set("")
}
