plugins {
    id("com.android.application") version "9.4.1" apply false
    // AGP 9 compiles Kotlin itself ("built-in Kotlin"); declaring KGP here only pins the Kotlin
    // version it uses (2.4.20, already in ~/.gradle, so offline builds work).
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
}
