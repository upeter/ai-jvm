import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object Versions {
    const val kotlin_version = "2.1.21"
    const val kotlinx_version = "1.10.2"
    const val spring_version = "3.4.4"
    const val jackson_version = "2.14.2"
    const val arrow_version = "1.2.4"
    const val langchain4j = "1.0.0"
    const val langchain4j_glue = "1.0.1-beta6"
    const val pgvector = "0.1.3"
    const val postgresql = "42.6.0"
    const val dataframe = "0.13.1"

}


group = "dev.example"
version = "1.0"
description = "AI try outs"
buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
    }
}

repositories {
    mavenCentral()
}


plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.spring") version "1.6.21"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.5"
    id("com.google.devtools.ksp") version "2.1.21-2.0.1"
//    id("org.jmailen.kotlinter") version "4.3.0"
    java
}





dependencies {
    ksp("io.arrow-kt:arrow-optics-ksp-plugin:${Versions.arrow_version}")


    implementation("org.jsoup:jsoup:1.14.3")

    implementation(platform("io.arrow-kt:arrow-stack:${Versions.arrow_version}"))
    implementation("io.arrow-kt:arrow-core")
    implementation("io.arrow-kt:arrow-optics")
    implementation("io.arrow-kt:arrow-fx-coroutines")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow-jvm:1.4.0")
    testImplementation("io.kotest.extensions:kotest-property-arrow-jvm:1.4.0")
    testImplementation("io.kotest.extensions:kotest-property-arrow-optics-jvm:1.4.0")


    implementation("org.jetbrains.kotlin:kotlin-script-runtime:${Versions.kotlin_version}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin_version}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Versions.kotlinx_version}")
    implementation("org.springframework.boot:spring-boot-starter:${Versions.spring_version}") {
        exclude("ch.qos.logback", "logback-classic")
    }
    implementation("org.springframework.boot:spring-boot-starter-web:${Versions.spring_version}")
    implementation("org.springframework.boot:spring-boot-starter-webflux:${Versions.spring_version}")
    //AI
//    implementation(platform("io.springboot.ai:spring-ai-bom:${Versions.spring_ai_version}"))
//    implementation("io.springboot.ai:spring-ai-openai-spring-boot-starter:${Versions.spring_ai_version}")

    implementation("com.fasterxml.jackson.core:jackson-databind:${Versions.jackson_version}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson_version}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:${Versions.jackson_version}")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.2")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc:${Versions.spring_version}")
    implementation("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${Versions.spring_version}")

    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.8.1")
    //implementation("ch.qos.logback:logback-classic:0.9.28")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin_version}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin_version}")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:${Versions.kotlin_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinx_version}")
    implementation("org.springframework.boot:spring-boot-starter-webflux:${Versions.spring_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.kotlinx_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Versions.kotlinx_version}")

    implementation("com.github.haifengl:smile-core:3.1.1")
//    implementation("nz.ac.waikato.cms.weka:weka-stable:3.8.6")

    implementation("dev.langchain4j:langchain4j:${Versions.langchain4j}")
    implementation("dev.langchain4j:langchain4j-open-ai-spring-boot-starter:${Versions.langchain4j_glue}") {
        exclude("ch.qos.logback", "logback-classic")
    }
    implementation("dev.langchain4j:langchain4j-spring-boot-starter:${Versions.langchain4j_glue}") {
        exclude("ch.qos.logback", "logback-classic")
    }

    implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:${Versions.langchain4j_glue}")
    implementation("dev.langchain4j:langchain4j-embeddings-bge-small-zh-v15-q:${Versions.langchain4j_glue}")
    implementation("dev.langchain4j:langchain4j-pgvector:${Versions.langchain4j_glue}")
    implementation("dev.langchain4j:langchain4j-document-parser-apache-pdfbox:${Versions.langchain4j_glue}")
    implementation("me.kpavlov.langchain4j.kotlin:langchain4j-kotlin:0.1.12")
    implementation("com.pgvector:pgvector:${Versions.pgvector}")
    implementation("org.postgresql:postgresql:${Versions.postgresql}")
    implementation("dev.langchain4j:langchain4j-open-ai:1.0.1")
    implementation("dev.langchain4j:langchain4j-reactor:${Versions.langchain4j_glue}")



    implementation("org.jetbrains.kotlinx:dataframe-core:${Versions.dataframe}")
    // Optional formats support
    implementation("org.jetbrains.kotlinx:dataframe-excel:${Versions.dataframe}")
    implementation("org.jetbrains.kotlinx:dataframe-jdbc:${Versions.dataframe}")
    implementation("org.jetbrains.kotlinx:dataframe-arrow:${Versions.dataframe}")
    implementation("org.jetbrains.kotlinx:dataframe-openapi:${Versions.dataframe}")

    implementation("org.jetbrains.kotlinx:kotlin-deeplearning-api:0.5.2")

    testImplementation("io.kotest:kotest-assertions-core-jvm:5.9.1")
    testImplementation("io.kotest:kotest-property-jvm:5.9.1")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.9.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")

    testImplementation("com.ninja-squad:springmockk:3.1.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.spring_version}")

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
    reports {
        junitXml.isOutputPerTestCase = true
    }
}

tasks.create<Delete>("cleanup") {
    delete(rootProject.layout.buildDirectory)
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }

}
val compileKotlin: KotlinCompile by tasks

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

springBoot {
    mainClass = "dev.example.demo.LangChain4JDemoApplication"
}