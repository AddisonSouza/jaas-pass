# jaas-pass

A **100% offline** personal password manager for Android, written in **Kotlin** using
**only the Android SDK + the standard library** (zero third-party dependencies). If the device
is stolen or the app's files are extracted, the contents must be **unreadable** without the
correct master password.

## Features

- **Master-password authentication** — never persisted, not even as a recoverable hash.
- **Encrypted vault** — entries (service / username / password) encrypted at rest.
- **Strong password generator** — built-in, configurable generator for new entries.
- **Session lock** — manual lock and auto-lock; clipboard is auto-cleared after copy.
- **Dark mode** — system-aware light/dark theming.

## Security

- Authentication by **master password**, never stored (not even as a recoverable hash).
- Entries encrypted with **AES-256-GCM** (AEAD / authenticated encryption).
- Key derivation via **PBKDF2-HMAC-SHA256** (≥ 600,000 iterations, versioned) using a
  **KEK/DEK** hierarchy (envelope encryption).
- Salt is rotated on every master-password change.
- Platform hardening: no `INTERNET` permission, `allowBackup=false`, `FLAG_SECURE`,
  clipboard auto-clear, and auto-lock.

### Threat model (summary)

**Guaranteed (with the app locked):** data at rest is unreadable; decryption is infeasible
without the master password; copying the files to another device grants no access; resistance
to offline brute-force scales with master-password strength and KDF cost.

**Not guaranteed:** a rooted device with the app **already unlocked** (key in memory);
keyloggers / malware with accessibility access; a weak master password.

> ⚠️ Forgetting the master password means **total, irreversible loss** — no cloud means no
> recovery.

Full threat model in
[`pscode/changes/archive/2026-06-21-local-password-manager/proposal.md`](pscode/changes/archive/2026-06-21-local-password-manager/proposal.md).

## Stack

- Kotlin · Android `compileSdk`/`targetSdk` 35, `minSdk` 26
- Gradle 8.9 (wrapper) · AGP 8.7.3 · JDK 21

## Build

```bash
./gradlew :app:assembleDebug      # outputs app/build/outputs/apk/debug/app-debug.apk
```

Install the resulting APK on a device by sideloading it.

## Development workflow (SDD)

Spec-driven development via [pscode](https://github.com/eipastel/pscode):
`proposal → specs → design → tasks → apply`. Completed change artifacts live under
`pscode/changes/archive/`.

## License

No license has been declared yet. All rights reserved by the author until one is added.
