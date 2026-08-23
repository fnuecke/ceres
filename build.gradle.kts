plugins {
    java
    id("com.vanniktech.maven.publish") version "0.34.0"
}

fun getGitRef(): String = try {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { "unknown" }
} catch (ignored: Throwable) {
    "unknown"
}

val semver: String by project

version = "$semver+${getGitRef()}"
group = "li.cil.ceres"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    implementation("org.ow2.asm:asm-commons:9.10.1")
    implementation("org.ow2.asm:asm:9.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.8.2")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    archiveVersion = semver
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates(group.toString(), project.name, semver)

    pom {
        name = "Ceres"
        description = "A simple serialization library."
        url = "https://github.com/fnuecke/ceres"
        licenses {
            license {
                name = "MIT License"
                url = "https://github.com/fnuecke/ceres/blob/main/LICENSE"
            }
        }
        developers {
            developer {
                id = "fnuecke"
                name = "Florian Nücke"
            }
        }
        scm {
            connection = "scm:git:https://github.com/fnuecke/ceres.git"
            developerConnection = "scm:git:ssh://git@github.com/fnuecke/ceres.git"
            url = "https://github.com/fnuecke/ceres"
        }
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("li.cil.ceres.disableCodeGen", "false")
}

val testReflection = tasks.register<Test>("testReflection") {
    description = "reflection-based serializer tests"
    group = "verification"

    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    systemProperty("li.cil.ceres.disableCodeGen", "true")
}

tasks.check {
    dependsOn(testReflection)
}
