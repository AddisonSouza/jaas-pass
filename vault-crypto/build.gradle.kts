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

// Change "generate-strong-password" — roda os self-tests do PasswordGenerator (mesmo harness,
// zero deps).  ./gradlew :vault-crypto:passwordGenSelfTest
tasks.register<JavaExec>("passwordGenSelfTest") {
    group = "verification"
    description = "Executa os self-tests do PasswordGenerator (harness próprio, zero deps)."
    dependsOn("testClasses")
    mainClass.set("com.jaaspass.crypto.PasswordGeneratorSelfTestKt")
    classpath = sourceSets["test"].runtimeClasspath
}

// Validação 6.2/6.3 — emite SQL (esquema do VaultRepository + blobs reais) para montar um .db
// inspecionável com o sqlite3 do platform-tools.
tasks.register<JavaExec>("vaultDbProbe") {
    group = "verification"
    description = "Gera SQL de um cofre real (blobs cifrados) para inspeção do .db."
    dependsOn("testClasses")
    mainClass.set("com.jaaspass.crypto.VaultDbProbeKt")
    classpath = sourceSets["test"].runtimeClasspath
}
