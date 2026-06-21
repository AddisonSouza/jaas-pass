plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Núcleo criptográfico puro (JVM). CONSTRAINT (design §1): ZERO dependências de terceiros —
// nem de produção, nem de teste. Apenas JDK + Kotlin stdlib (via plugin).
dependencies {
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Tarefa 1.5 — roda o harness de self-tests do CryptoManager (sem JUnit).
//   ./gradlew :vault-crypto:cryptoSelfTest
tasks.register<JavaExec>("cryptoSelfTest") {
    group = "verification"
    description = "Executa os self-tests do CryptoManager (harness próprio, zero deps)."
    dependsOn("testClasses")
    mainClass.set("com.jaaspass.crypto.CryptoSelfTestKt")
    classpath = sourceSets["test"].runtimeClasspath
}
