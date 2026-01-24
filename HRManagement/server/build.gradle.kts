plugins {
    kotlin("jvm")
    application
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // Mail + logging
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm:2.3.7")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Auth / JWT
    implementation("io.ktor:ktor-server-auth:2.3.7")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.7")
    implementation("com.auth0:java-jwt:4.4.0")

    // Logging
    implementation("io.ktor:ktor-server-call-logging:2.3.7")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.43.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.43.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.43.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.43.0")
    implementation("mysql:mysql-connector-java:8.0.33")

    // Password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.7")
}
configurations.all {
    resolutionStrategy {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
}

application {
    mainClass.set("com.example.hrmanagement.ApplicationKt")
}

kotlin {
    jvmToolchain(11)
}
