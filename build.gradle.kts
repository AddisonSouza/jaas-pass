// Build raiz: declara os plugins essenciais (Android + Kotlin), aplicados no módulo :app.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
