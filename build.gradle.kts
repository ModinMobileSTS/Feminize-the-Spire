plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val gameClasspath = files(
    "D:/Desktop/SlayTheAmethystModded/app/src/main/assets/components/mods/ModTheSpire.jar",
    "D:/Desktop/SlayTheAmethystModded/app/src/main/assets/components/mods/BaseMod.jar",
    "D:/Desktop/SlayTheAmethystModded/tools/desktop-1.0.jar"
)

val modManifestFile = layout.projectDirectory.file("src/main/resources/ModTheSpire.json")

fun modManifestString(fieldName: String): String {
    val manifest = modManifestFile.asFile.readText(Charsets.UTF_8)
    return Regex("\"${Regex.escape(fieldName)}\"\\s*:\\s*\"([^\"]+)\"")
        .find(manifest)
        ?.groupValues
        ?.get(1)
        ?: throw GradleException("Missing ModTheSpire manifest field: $fieldName")
}

val modName = modManifestString("name")
val modVersion = modManifestString("version")

dependencies {
    compileOnly(gameClasspath)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    archiveFileName.set("$modName $modVersion.jar")
    duplicatesStrategy = DuplicatesStrategy.FAIL
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    includeEmptyDirs = false
    exclude("images/npcs/**")
    exclude("images/monsters/**")
}
