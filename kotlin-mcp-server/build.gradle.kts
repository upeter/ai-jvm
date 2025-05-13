plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.github.onslip.gradle-one-jar") version "1.1.0"
    application
}



application {
    mainClass.set("MCPServicesKt")
}




group = "org.example"
version = "0.1.0"


val mcpVersion = "0.5.0"
val slf4jVersion = "2.0.9"
val ktorVersion = "3.1.1"
dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:$mcpVersion")
    implementation("org.slf4j:slf4j-nop:$slf4jVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
//    implementation("net.pwall.json:json-kotlin-schema:0.56")    // build.gradle.kts
    //implementation("com.github.Ricky12Awesome:json-schema-serialization:0.6.6")
testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MCPServicesKt"
    }
    archiveBaseName = "mcp-kt-jar"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}



kotlin {
    jvmToolchain(21)
}
