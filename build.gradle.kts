import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.spring") version "2.1.10"
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.10.5"
}

group = "fr.miage.syp"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.withType<BootBuildImage> {
    val dockerImageName = System.getenv("DOCKER_IMAGE")
    if (!dockerImageName.isNullOrEmpty()) {
        this.imageName.set(dockerImageName)
    }

    val envTags = System.getenv("DOCKER_TAGS")
    if (!envTags.isNullOrEmpty()) {
        val realName = imageName.get().let {
            val lastIndex = it.lastIndexOf(":")
            it.substring(0, lastIndex)
        }
        this.tags.addAll(
            envTags.split(",").map { "${realName}:$it" })
    }

    System.getenv("USERNAME")?.let { username ->
        docker.publishRegistry.url = "ghcr.io"
        docker.publishRegistry.username = username
        docker.publishRegistry.password = System.getenv("GITHUB_TOKEN") ?: "INVALID_PASSWORD"
    }
}