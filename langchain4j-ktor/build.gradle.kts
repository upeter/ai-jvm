import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dev.example"
version = "1.0"
description = "AI try outs with Ktor"

object Versions {
    const val kotlin_version = "2.1.20"
    const val kotlinx_version = "1.10.2"
    const val ktor_version = "3.1.2"
    const val jackson_version = "2.14.2"
    const val langchain4j = "0.36.2"
    const val logback_version = "1.4.14"
}

repositories {
    mavenCentral()
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("io.ktor.plugin") version "3.1.2"
    application
}

application {
    mainClass.set("dev.example.demo.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

dependencies {
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.kotlinx_version}")
    implementation("io.projectreactor:reactor-core:3.6.4")

    implementation("com.fasterxml.jackson.core:jackson-databind:${Versions.jackson_version}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson_version}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:${Versions.jackson_version}")

    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.8.1")
    implementation("ch.qos.logback:logback-classic:${Versions.logback_version}")

    implementation("dev.langchain4j:langchain4j:${Versions.langchain4j}")
    implementation("dev.langchain4j:langchain4j-open-ai:${Versions.langchain4j}")
    implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:${Versions.langchain4j}")
    // These dependencies are not needed for the basic functionality
    // implementation("dev.langchain4j:langchain4j-embeddings-bge-small-zh-v15-q:${Versions.langchain4j}")
    // implementation("dev.langchain4j:langchain4j-pgvector:${Versions.langchain4j}")
    // implementation("dev.langchain4j:langchain4j-document-parser-apache-pdfbox:${Versions.langchain4j}")
//    implementation("com.pgvector:pgvector:${Versions.pgvector}")
//    implementation("org.postgresql:postgresql:${Versions.postgresql}")

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
    // No special configuration needed
}
