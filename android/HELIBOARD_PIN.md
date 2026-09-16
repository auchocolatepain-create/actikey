# HeliBoard pin — retain toolchain initially

Do **not** re-tool the keyboard build. Actikey stays on HeliBoard's pinned
toolchain until the offline gate + CI are green.

## Pin

- Upstream: `https://github.com/Helium314/HeliBoard`
- Pinned commit: `v2.3.1` tag (record exact SHA in `android/gradle/libs.versions.toml`
  comment when syncing; example placeholder `SHA: heli-v2.3.1` — maintainer must
  replace with `git rev-parse v2.3.1` output on sync).
- Integration method: `android/` is an overlay, not a fork-copy:
  - HeliBoard added as a git submodule at `android/vendor/HeliBoard` **or**
    Gradle source dependency pinned to the tag (preferred: submodule, shallow).
  - Actikey adds only: `org.actikey.*` packages, `en-US` + `fa` resource filter,
    Action Bar, LM Studio client, settings screens.
  - No build-logic rewrite, no AGP/Kotlin version bump, no new annotation
    processors without a size/latency justification entry in `docs/performance.md`.

## Language trim (APK size)

In `android/app/build.gradle`:

```gradle
android {
  defaultConfig {
    resConfigs "en", "fa"   // exactly two typing languages
  }
}
```

Plus a CI check (`android-ci.yml` → `checkLanguageTrim`) that fails if any
`values-<lang>` other than `values`, `values-en*`, `values-fa*`, or density/
orientation qualifiers introduce string resources, or if bundled dictionaries
under `assets/dicts` contain anything but `en_us` + `fa`.

Swipe is an **optional user-imported** `.so`/`.zip` loaded via `System.load`
from app-private storage; its absence/failure must not affect tap typing
(guarded by `SwipeGuard`, tested in `SwipeIsolationTest`).

## Toolchain freeze checklist (CI enforces)

- [ ] `assembleActikeyRelease` succeeds on GitHub-hosted `ubuntu-latest` (no GPU).
- [ ] All `testActikey*UnitTest` + `run_mock_protocol_tests.py` pass without GPU.
- [ ] `resConfigs` check passes; APK size reported as CI artifact.
- [ ] Real-model tests are **not** part of this CI job (separate manual workflow).
