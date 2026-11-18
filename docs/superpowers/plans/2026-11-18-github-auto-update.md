# GitHub Auto-Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Both nubiaPhone and nubiaSMS check public GitHub Releases for newer APKs, show a MyOS “Yeni güncelleme mevcut” prompt (soft at lag 1, insistent at lag ≥ 2, never locking the app), and install via the system package installer.

**Architecture:** Pure Kotlin policy/parser + thin GitHub HTTP client + downloader/installer live under `update/` in each app (same logic, package rename). Compose dialog overlays `MainActivity` only; `InCallActivity` / SMS receivers stay untouched. Releases ship `update.json` + fixed APK asset name; CI builds signed release on `v*` tags.

**Tech Stack:** Kotlin, JUnit4, `HttpURLConnection` (no new HTTP lib), SharedPreferences, FileProvider, Jetpack Compose Material3, GitHub Releases API, GitHub Actions, JDK 17 release signing.

**Spec:** `docs/superpowers/specs/2026-11-18-github-auto-update-design.md`

## Global Constraints

- Spec copy: prompt title exactly `Yeni güncelleme mevcut`; primary button `Güncelle`; soft dismiss `Sonra`
- Soft when `lag == 1`; insistent when `lag >= 2`; never block dialer/SMS UI or `InCallActivity`
- Offline: do not lock; if last successful check had `lag >= 2`, show insistent again when online
- Public GitHub only (no embedded tokens)
- Release assets: APK name fixed per app + `update.json` with `versionCode` (Int) and `versionName` (String)
- minSdk 26, targetSdk/compileSdk 35, JDK 17
- Turkish UI strings; follow TASARIM.md / existing MyOS dialogs
- Keep release minify/R8; add keep rules if reflection-free code still needs them for update package

## Review Focus

- `lag == 2` after user dismisses overlay → next `onResume` must show insistent again without freezing the home UI underneath
- Network failure mid-download → user can retry; home/SMS remains usable
- GitHub returns a release whose APK asset is missing → treat as no usable update (no crash, no endless spinner)
- Installed `versionCode` equals latest → no dialog on start or Settings check
- `InCallActivity` open while MainActivity would show update → call UI must not be covered or finished by update logic

---

## File Structure

### nubiaPhone (`com.nubiaphone.dialer`)

```
app/src/main/java/com/nubiaphone/dialer/update/
  UpdateModels.kt          # UpdateInfo, UpdatePromptKind
  UpdatePolicy.kt          # lag → None | Soft | Insistent
  UpdateJsonParser.kt      # parse update.json body
  GithubReleaseClient.kt   # latest release → UpdateInfo?
  UpdateStore.kt           # last known UpdateInfo + soft-dismissed session flag
  UpdateDownloader.kt      # APK → cache file
  UpdateInstaller.kt       # FileProvider + install intent
  UpdateChecker.kt         # orchestrate check
app/src/main/java/com/nubiaphone/dialer/ui/update/
  UpdatePromptHost.kt      # Compose dialog + download state
app/src/test/java/com/nubiaphone/dialer/update/
  UpdatePolicyTest.kt
  UpdateJsonParserTest.kt
app/src/main/res/xml/file_paths.xml
.github/workflows/release.yml
keystore/ (gitignored) + README note
```

### nubiaSMS / aramalr (`com.nubiasms.messages`)

Same `update/` + `ui/update/` + tests + FileProvider + workflow, package renamed.

---

### Task 1: Update policy + JSON parser (nubiaPhone)

**Files:**
- Create: `app/src/main/java/com/nubiaphone/dialer/update/UpdateModels.kt`
- Create: `app/src/main/java/com/nubiaphone/dialer/update/UpdatePolicy.kt`
- Create: `app/src/main/java/com/nubiaphone/dialer/update/UpdateJsonParser.kt`
- Test: `app/src/test/java/com/nubiaphone/dialer/update/UpdatePolicyTest.kt`
- Test: `app/src/test/java/com/nubiaphone/dialer/update/UpdateJsonParserTest.kt`

**Interfaces:**
- Produces:
  - `data class UpdateInfo(val versionCode: Int, val versionName: String, val apkDownloadUrl: String)`
  - `enum class UpdatePromptKind { None, Soft, Insistent }`
  - `object UpdatePolicy { fun kind(installedVersionCode: Int, latestVersionCode: Int): UpdatePromptKind }`
  - `object UpdateJsonParser { fun parse(json: String): Pair<Int, String>? }` — returns `versionCode`/`versionName` or null if invalid

- [ ] **Step 1: Write failing tests**

```kotlin
// UpdatePolicyTest
assertEquals(UpdatePromptKind.None, UpdatePolicy.kind(5, 5))
assertEquals(UpdatePromptKind.None, UpdatePolicy.kind(5, 4))
assertEquals(UpdatePromptKind.Soft, UpdatePolicy.kind(4, 5))
assertEquals(UpdatePromptKind.Insistent, UpdatePolicy.kind(3, 5))
assertEquals(UpdatePromptKind.Insistent, UpdatePolicy.kind(2, 5))

// UpdateJsonParserTest
assertEquals(4 to "0.1.3", UpdateJsonParser.parse("""{"versionCode":4,"versionName":"0.1.3"}"""))
assertNull(UpdateJsonParser.parse("""{}"""))
assertNull(UpdateJsonParser.parse("not-json"))
```

- [ ] **Step 2: Run tests — expect FAIL (classes missing)**

Run: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home && ./gradlew :app:testReleaseUnitTest --tests 'com.nubiaphone.dialer.update.*'`  
Expected: FAIL compile / class not found

- [ ] **Step 3: Implement models, `UpdatePolicy.kind`, `UpdateJsonParser.parse`**

Parser: minimal org.json or hand-rolled — prefer `org.json.JSONObject` (Android SDK; for unit tests use `org.json:json` test dependency if needed). No OkHttp.

- [ ] **Step 4: Run tests — expect PASS**

- [ ] **Step 5: Commit** (only if repo has git; nubiaPhone may need `git init` first — do that here if missing)

```bash
git add app/src/main/java/com/nubiaphone/dialer/update/UpdateModels.kt \
  app/src/main/java/com/nubiaphone/dialer/update/UpdatePolicy.kt \
  app/src/main/java/com/nubiaphone/dialer/update/UpdateJsonParser.kt \
  app/src/test/java/com/nubiaphone/dialer/update/
git commit -m "feat(update): add version lag policy and update.json parser"
```

---

### Task 2: GitHub client, store, checker (nubiaPhone)

**Files:**
- Create: `app/src/main/java/com/nubiaphone/dialer/update/GithubReleaseClient.kt`
- Create: `app/src/main/java/com/nubiaphone/dialer/update/UpdateStore.kt`
- Create: `app/src/main/java/com/nubiaphone/dialer/update/UpdateChecker.kt`
- Modify: `app/build.gradle.kts` — `buildConfigField` for owner/repo/apk asset name
- Test: `app/src/test/java/com/nubiaphone/dialer/update/GithubReleaseClientTest.kt` (parse fixture JSON only; no live network)

**Interfaces:**
- Consumes: `UpdateInfo`, `UpdateJsonParser`, `UpdatePolicy`
- Produces:
  - `class GithubReleaseClient(private val owner: String, private val repo: String, private val apkAssetName: String, private val openUrl: (String) -> String = /* HttpURLConnection GET body */)`
    - `suspend fun fetchLatest(): UpdateInfo?` — GET `https://api.github.com/repos/{owner}/{repo}/releases/latest` with `Accept: application/vnd.github+json` and `User-Agent: nubiaPhone-updater`; find asset named `apkAssetName` and asset named `update.json`; parse update.json body via second GET or if content embedded skip; return null if anything missing
  - `class UpdateStore(prefs: SharedPreferences)`
    - `var lastInfo: UpdateInfo?` / `var softDismissedForVersionCode: Int` (0 = none)
  - `class UpdateChecker(client, store, installedVersionCode: Int)`
    - `suspend fun check(): UpdatePromptKind` — on success save `lastInfo`; return `UpdatePolicy.kind(...)`; on failure return `None` if no lastInfo, else kind from lastInfo (so offline insistent can reappear when UI uses lastInfo — see Step 3 note)
  - BuildConfig: `UPDATE_GITHUB_OWNER`, `UPDATE_GITHUB_REPO`, `UPDATE_APK_ASSET` defaulting to placeholders `"YOUR_GITHUB_USER"`, `"nubiaPhone"`, `"nubiaphone-release.apk"` readable from `local.properties` keys `update.github.owner` / `update.github.repo` when present

- [ ] **Step 1: Write failing test with a fixed GitHub API JSON fixture** that includes two assets (`update.json`, `nubiaphone-release.apk`) and assert `fetchLatest` with fake `openUrl` returns expected `UpdateInfo` (version from second openUrl returning `{"versionCode":4,"versionName":"0.1.3"}`)

- [ ] **Step 2: Run — expect FAIL**

- [ ] **Step 3: Implement client + store + checker**

`UpdateChecker.check` failure behavior: keep previous `lastInfo`; return `UpdatePolicy.kind(installed, lastInfo.versionCode)` if `lastInfo != null`, else `None`. Soft dismiss: UI (Task 3) sets `softDismissedForVersionCode = latest` when Soft + Sonra; checker/UI treats Soft as None for display if `softDismissedForVersionCode == latest.versionCode` until next process death or new latest.

- [ ] **Step 4: Tests PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(update): GitHub latest release client and checker"
```

---

### Task 3: Download, install, Compose prompt, MainActivity wire (nubiaPhone)

**Files:**
- Create: `update/UpdateDownloader.kt`, `update/UpdateInstaller.kt`, `ui/update/UpdatePromptHost.kt`
- Create: `app/src/main/res/xml/file_paths.xml` (`cache-path` name `apk` path `.`)
- Modify: `AndroidManifest.xml` — `INTERNET`, `REQUEST_INSTALL_PACKAGES`, `FileProvider` (`${applicationId}.fileprovider`)
- Modify: `MainActivity.kt` — host `UpdatePromptHost` over nav; **do not** touch `InCallActivity`
- Modify: `ui/settings/SettingsScreen.kt` — row “Güncellemeleri kontrol et” under about group
- Test: `UpdatePolicy` already covers lag; add `UpdateStore` unit test for soft-dismiss preference if pure; skip instrumented install test

**Interfaces:**
- Produces:
  - `class UpdateDownloader(cacheDir: File) { suspend fun download(url: String, onProgress: (Float?) -> Unit): File }`
  - `object UpdateInstaller { fun install(context: Context, apk: File) }` — `FileProvider` URI + `Intent.ACTION_VIEW` / `ACTION_INSTALL_PACKAGE` with `FLAG_GRANT_READ_URI_PERMISSION`
  - `@Composable fun UpdatePromptHost(checker: UpdateChecker, ...)` — shows dialog; Soft has Sonra; Insistent has only Güncelle (dismissible via back — app stays usable); never `DialogProperties(dismissOnBackPress = false)` for a lock; on Güncelle run download then install
  - Exact strings: title `Yeni güncelleme mevcut`, `Güncelle`, `Sonra`

- [ ] **Step 1: Add manifest permission/provider + file_paths; stub downloader/installer; write a JVM test that soft-dismiss store suppresses Soft but not Insistent**

```kotlin
// soft dismissed for v5 → Soft display suppressed; Insistent for lag>=2 still shown
```

- [ ] **Step 2: Run unit test — FAIL then implement store helper `fun displayKind(policyKind, latestCode, softDismissedCode): UpdatePromptKind`**

Put `displayKind` on `UpdatePolicy` or small `UpdateUiPolicy` to keep UI thin.

- [ ] **Step 3: Implement downloader/installer + `UpdatePromptHost`; wire in `MainActivity` with `LifecycleResumeEffect` / LaunchedEffect to `check()` on start and resume; Settings row triggers same check and forces show**

- [ ] **Step 4: Manual verify on device (USB or Wi-Fi scrcpy):** install app, fake by pointing BuildConfig to a test repo later; at minimum compile `assembleRelease` succeeds and app launches without dialog when no network / empty config

Run: `JAVA_HOME=... ./gradlew :app:assembleRelease`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(update): prompt UI, APK download and sideload install"
```

---

### Task 4: Port update stack to nubiaSMS (aramalr)

**Files:**
- Create same types under `com.nubiasms.messages.update` and `ui/update`
- Mirror tests under `app/src/test/java/com/nubiasms/messages/update/`
- Modify: `AndroidManifest.xml`, `MainActivity.kt`, `ui/settings/AppSettingsScreen.kt`, `app/build.gradle.kts`
- Defaults: repo `"nubiaSMS"` or `"aramalr"`, asset `"nubiasms-release.apk"`

**Interfaces:** Identical to Tasks 1–3; only package / BuildConfig / asset name change.

- [ ] **Step 1: Copy + rename packages; adjust BuildConfig defaults; duplicate unit tests**

- [ ] **Step 2: Run** `./gradlew :app:testReleaseUnitTest --tests 'com.nubiasms.messages.update.*'` with JDK 17 — PASS

- [ ] **Step 3: Wire MainActivity + Settings; assembleRelease**

- [ ] **Step 4: Commit in aramalr repo**

```bash
git commit -m "feat(update): GitHub release updates for Mesajlar"
```

---

### Task 5: Release signing + GitHub Actions + docs

**Files:**
- Modify both apps’ `app/build.gradle.kts` — release signing from `keystore.properties` / env (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); if missing, fall back to debug (local) but document that CI must set secrets
- Create: each repo `.github/workflows/release.yml` — on tag `v*`: JDK 17, assembleRelease, write `update.json` from `versionCode`/`versionName`, `gh release create` with APK + `update.json`
- Modify: both README.md — publish steps, first-install note (debug→release may need uninstall), friend install from Releases
- Create/update `.gitignore` — `*.jks`, `keystore.properties`
- Spec already in `docs/superpowers/specs/2026-11-18-github-auto-update-design.md` — mark status approved if not

**Interfaces:**
- Workflow uploads asset names matching BuildConfig `UPDATE_APK_ASSET`
- `update.json` content: `{"versionCode":N,"versionName":"..."}` matching `defaultConfig`

- [ ] **Step 1: Add signingConfig wiring + gitignore; generate local keystore only if partner asks (do not commit secrets)**

- [ ] **Step 2: Add `release.yml` to nubiaPhone and aramalr**

- [ ] **Step 3: README sections for update channel; bump note: every release must increment `versionCode`**

- [ ] **Step 4: Commit docs/workflows (no keystore binaries)**

```bash
git commit -m "chore(release): signed GitHub Releases workflow and update docs"
```

---

## Self-Review Notes (author)

- Spec coverage: soft/insistent/no-lock, GitHub Releases + update.json, settings check, InCall excluded, signing+Actions, both apps — mapped to Tasks 1–5.
- Offline lock anti-goal: Task 2 failure keeps lastInfo; Task 3 never locks UI.
- Review Focus items pinned: dismiss+resume (Task 3 host), download fail (Task 3), missing asset → null (Task 2 test), lag 0 no dialog (Task 1+3), InCall untouched (Task 3 files list).
