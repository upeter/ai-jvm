import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dev.example"
version = "1.0"
description = "AI try outs with Ktor"

object Versions {
    const val kotlin_version = "2.0.0"
    const val kotlinx_version = "1.7.3"
    const val ktor_version = "2.3.9"
    const val jackson_version = "2.14.2"
    const val arrow_version = "1.2.4"
    const val langchain4j = "0.36.2"
    const val pgvector = "0.1.3"
    const val postgresql = "42.6.0"
    const val logback_version = "1.4.14"
}

repositories {
    mavenCentral()
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("io.ktor.plugin") version "2.3.9"
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
    application
}

application {
    mainClass.set("dev.example.demo.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

dependencies {
    ksp("io.arrow-kt:arrow-optics-ksp-plugin:${Versions.arrow_version}")

    implementation("io.ktor:ktor-server-core:${Versions.ktor_version}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor_version}")
    implementation("io.ktor:ktor-server-content-negotiation:${Versions.ktor_version}")
    implementation("io.ktor:ktor-serialization-jackson:${Versions.ktor_version}")
    implementation("io.ktor:ktor-server-status-pages:${Versions.ktor_version}")
    implementation("io.ktor:ktor-server-cors:${Versions.ktor_version}")
    implementation("io.ktor:ktor-server-call-logging:${Versions.ktor_version}")

    implementation("org.jetbrains.kotlin:kotlin-script-runtime:${Versions.kotlin_version}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin_version}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.kotlinx_version}")

    implementation("com.fasterxml.jackson.core:jackson-databind:${Versions.jackson_version}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson_version}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:${Versions.jackson_version}")

    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.8.1")
    implementation("ch.qos.logback:logback-classic:${Versions.logback_version}")

    implementation("dev.langchain4j:langchain4j:${Versions.langchain4j}")
    implementation("dev.langchain4j:langchain4j-open-ai:${Versions.langchain4j}")
    implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:${Versions.langchain4j}")
    implementation("dev.langchain4j:langchain4j-embeddings-bge-small-zh-v15-q:${Versions.langchain4j}")
    implementation("dev.langchain4j:langchain4j-pgvector:${Versions.langchain4j}")
    implementation("dev.langchain4j:langchain4j-document-parser-apache-pdfbox:${Versions.langchain4j}")
    implementation("com.pgvector:pgvector:${Versions.pgvector}")
    implementation("org.postgresql:postgresql:${Versions.postgresql}")

    testImplementation("io.ktor:ktor-server-test-host:${Versions.ktor_version}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${Versions.kotlin_version}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
    options.javaModuleVersion = "21"
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}