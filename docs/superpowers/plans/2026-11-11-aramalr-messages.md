# aramalr Messages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a sideloadable Kotlin/Compose SMS app on NX741J that looks like MyOS Settings, uses system dynamic color, sends/receives SMS as default role, fills OTPs into other apps via AutofillService, and logs OTP/SMS events on-device for iteration.

**Architecture:** Single `app` module. Pure-Kotlin OTP detection + on-device event log sit under `otp/` and `evidence/`. Telephony/SMS role plumbing lives in `sms/`. Compose UI (`ui/`) reads conversation/thread models from `sms/` repositories. `AutofillService` reads the latest detected OTP from a small in-process store fed by the incoming-SMS path. Theme is dynamic Material 3 + MyOS shape/spacing overlay.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (dynamic color), Navigation Compose, Android SMS role / Telephony, AutofillService, JUnit4 unit tests, ADB sideload to NX741J (Android 16).

**Spec:** `docs/superpowers/specs/2026-11-11-aramalr-messages-design.md`

## Global Constraints

- Package: `com.aramalr.messages`; app label: `aramalr`
- minSdk 26, targetSdk/compileSdk 36 (Android 16 device)
- Turkish UI strings for user-facing copy in v1
- No RCS / stars / archive / group product features
- Colors from `dynamicDarkColorScheme` / `dynamicLightColorScheme` — never hardcode screenshot periwinkle as brand
- Dark background structure: true black canvas + elevated card surface (~`Color(0xFF1A1A1A)` class only as dark fallback when dynamic surfaces fight MyOS; prefer dynamic tonal layers mapped to card roles)
- Google Messages stays installed; do not uninstall it
- OTP evidence log: app-private storage only; no network upload
- Dual SIM: use system default SMS subscription; add chooser only if send fails without subscriptionId
- Distribution: `./gradlew :app:installDebug` / ADB — no Play

## Review Focus

- Multipart / concatenated SMS bodies where the OTP spans or arrives in part 2 — detector and log must see the full body the user sees in-thread
- OTP-like numbers that are not codes (amounts, phone fragments, tracking IDs) — detector must prefer keyword-anchored codes over bare digit runs
- Autofill fields without `AUTOFILL_HINT_SMS_OTP` — service should still attempt heuristic OTP field matching; if none, log `assist_path=none` so secondary assist can be added later
- App not default SMS role — incoming path and notifications must degrade safely (no crash; prompt to set default from in-app settings)
- Empty inbox / permission-denied read — conversation list shows empty MyOS state, not a crash or stock error dump

---

## File Structure

```
settings.gradle.kts
build.gradle.kts
gradle.properties
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/aramalr/messages/
  AramalrApp.kt
  MainActivity.kt
  ui/theme/Theme.kt
  ui/theme/Shape.kt
  ui/theme/Type.kt
  ui/components/MyOsCard.kt
  ui/components/MyOsSearchField.kt
  ui/conversations/ConversationListScreen.kt
  ui/conversations/ConversationListViewModel.kt
  ui/thread/ThreadScreen.kt
  ui/thread/ThreadViewModel.kt
  ui/compose/NewMessageScreen.kt
  ui/settings/AppSettingsScreen.kt
  ui/navigation/AramalrNav.kt
  sms/SmsModels.kt
  sms/ConversationRepository.kt
  sms/SmsSender.kt
  sms/IncomingSmsReceiver.kt
  sms/SmsRoleHelper.kt
  otp/OtpDetector.kt
  otp/OtpMatch.kt
  otp/LatestOtpStore.kt
  evidence/OtpEvidenceLog.kt
  evidence/OtpEvidenceEvent.kt
  autofill/AramalrOtpAutofillService.kt
  notify/SmsNotifier.kt
app/src/test/java/com/aramalr/messages/
  otp/OtpDetectorTest.kt
  evidence/OtpEvidenceLogTest.kt
```

---

### Task 1: Android project scaffold + OTP detector

**Files:**
- Create: Gradle wrapper + root/`app` Gradle files, `AndroidManifest.xml`, `MainActivity.kt`, `AramalrApp.kt` (empty shell)
- Create: `app/src/main/java/com/aramalr/messages/otp/OtpMatch.kt`
- Create: `app/src/main/java/com/aramalr/messages/otp/OtpDetector.kt`
- Test: `app/src/test/java/com/aramalr/messages/otp/OtpDetectorTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces:
  - `data class OtpMatch(val code: String, val startIndex: Int, val endIndex: Int, val confidence: Float)`
  - `object OtpDetector { fun detect(body: String): OtpMatch? }`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun detectsKeywordAnchoredSixDigitCode() {
  val m = OtpDetector.detect("DogruBank kodunuz: 482193. Paylasmayin.")
  assertEquals("482193", m!!.code)
}

@Test fun prefersCodeNearOtpKeywordsOverBareAmount() {
  val m = OtpDetector.detect("Odeme 1.250,00 TL. Dogrulama kodu 904421 gecerlidir.")
  assertEquals("904421", m!!.code)
}

@Test fun returnsNullWhenNoCode() {
  assertNull(OtpDetector.detect("Yarin saat 15:00 randevun var."))
}
```

- [ ] **Step 2: Run tests — expect FAIL (OtpDetector missing)**

Run: `cd /Users/farukkaygisiz24/Çalışmalar/Projects/aramalr && ./gradlew :app:testDebugUnitTest --tests com.aramalr.messages.otp.OtpDetectorTest`  
Expected: FAIL compiling or “Unresolved reference: OtpDetector”

- [ ] **Step 3: Scaffold app module + implement `OtpDetector.detect`**

Create Compose Application project structure (package `com.aramalr.messages`, minSdk 26, compileSdk 36). Implement detector: keyword list (`kod`, `code`, `otp`, `dogrulama`, `doğrulama`, `sifre`, `şifre`, `verification`) + 4–8 digit token near keywords; fallback strongest 4–8 digit run only when keyword present in body; never treat dotted currency amounts as codes.

- [ ] **Step 4: Run tests — expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests com.aramalr.messages.otp.OtpDetectorTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle app
git commit -m "feat: scaffold aramalr app and OTP detector"
```

---

### Task 2: On-device OTP evidence log

**Files:**
- Create: `app/src/main/java/com/aramalr/messages/evidence/OtpEvidenceEvent.kt`
- Create: `app/src/main/java/com/aramalr/messages/evidence/OtpEvidenceLog.kt`
- Test: `app/src/test/java/com/aramalr/messages/evidence/OtpEvidenceLogTest.kt`

**Interfaces:**
- Consumes: `OtpMatch` from Task 1
- Produces:
  - `data class OtpEvidenceEvent(val timestampMillis: Long, val sender: String, val body: String, val detectedCode: String?, val confidence: Float?, val assistPath: String, val notes: String = "")`
  - `class OtpEvidenceLog(private val dir: File) { fun append(event: OtpEvidenceEvent); fun readAll(): List<OtpEvidenceEvent>; fun clear() }`
  - Persist as JSONL under `dir` (app files dir at runtime: `filesDir/evidence/otp.jsonl`)

- [ ] **Step 1: Write failing tests** using a temp directory — append one event with detected code, `readAll()` returns it; `clear()` empties; assistPath `"none"` preserved.

- [ ] **Step 2: Run — expect FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests com.aramalr.messages.evidence.OtpEvidenceLogTest`

- [ ] **Step 3: Implement `OtpEvidenceLog` JSONL append/read/clear**

- [ ] **Step 4: Run — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -am "feat: add on-device OTP evidence JSONL log"
```

---

### Task 3: MyOS theme + shell UI chrome

**Files:**
- Create: `ui/theme/Theme.kt`, `Shape.kt`, `Type.kt`
- Create: `ui/components/MyOsCard.kt`, `MyOsSearchField.kt`
- Modify: `MainActivity.kt` to host `AramalrTheme { }` with a placeholder “Mesajlar” title + search + one empty card (no SMS yet)

**Interfaces:**
- Consumes: system dark/light
- Produces:
  - `@Composable fun AramalrTheme(content: @Composable () -> Unit)`
  - `@Composable fun MyOsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`
  - `@Composable fun MyOsSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String)`
  - Shapes: card corner ~24.dp; search capsule
  - Dark canvas: `Color.Black` background behind cards

- [ ] **Step 1: Implement theme + components; show placeholder list chrome in `MainActivity`**

No unit test — visual gate on device.

- [ ] **Step 2: Install and screenshot-compare mentally to Settings**

Run: `./gradlew :app:installDebug` (device `NX741J` attached)  
Expected: app opens with large “Mesajlar”, capsule search “Mesajlarda arayın”, black canvas + rounded card — not stock Google Messages look

- [ ] **Step 3: Commit**

```bash
git commit -am "feat: add MyOS-like theme and list chrome"
```

---

### Task 4: Conversation + message models and repository (read path)

**Files:**
- Create: `sms/SmsModels.kt`, `sms/ConversationRepository.kt`
- Modify: manifest permissions `READ_SMS`, `RECEIVE_SMS`, `SEND_SMS`, `READ_CONTACTS` (display names; optional fail-soft)

**Interfaces:**
- Consumes: Telephony content provider
- Produces:
  - `data class Conversation(val threadId: Long, val address: String, val displayName: String?, val snippet: String, val dateMillis: Long, val unreadCount: Int)`
  - `data class Message(val id: Long, val threadId: Long, val address: String, val body: String, val dateMillis: Long, val isOutgoing: Boolean, val otp: OtpMatch?)`
  - `class ConversationRepository(context: Context) { fun observeConversations(): Flow<List<Conversation>>; fun observeMessages(threadId: Long): Flow<List<Message>>; fun getOrCreateThreadId(address: String): Long }`
  - Message mapping runs `OtpDetector.detect(body)` into `otp`

- [ ] **Step 1: Implement repository read APIs against `Telephony.Sms` / `Telephony.Threads`**

Builder chooses ContentObserver vs periodic refresh; must emit on inbox change.

- [ ] **Step 2: Manual verify with `adb shell content query` or temporary debug — empty or real threads load without crash when SMS permission granted**

- [ ] **Step 3: Commit**

```bash
git commit -am "feat: add SMS conversation and message repository"
```

---

### Task 5: Conversation list + thread + compose screens

**Files:**
- Create: `ui/navigation/AramalrNav.kt`
- Create: `ui/conversations/ConversationListScreen.kt`, `ConversationListViewModel.kt`
- Create: `ui/thread/ThreadScreen.kt`, `ThreadViewModel.kt`
- Create: `ui/compose/NewMessageScreen.kt`
- Modify: `MainActivity.kt` to host nav

**Interfaces:**
- Consumes: `ConversationRepository`, `OtpMatch`
- Produces: Nav routes `list`, `thread/{threadId}`, `new`
- Thread shows OTP copy chip when `message.otp != null` (clipboard secondary assist)
- List: search filters by address/name/snippet; MyOS cards/rows; Turkish copy

- [ ] **Step 1: Wire navigation + list/thread/new UI reading repository**

Send not required yet (Task 6).

- [ ] **Step 2: `installDebug` — browse threads if SMS permission granted; OTP chip appears on a synthetic/local test message if needed**

- [ ] **Step 3: Commit**

```bash
git commit -am "feat: add conversation list, thread, and compose screens"
```

---

### Task 6: Send SMS + dual-SIM default + role helper entry

**Files:**
- Create: `sms/SmsSender.kt`, `sms/SmsRoleHelper.kt`
- Create: `ui/settings/AppSettingsScreen.kt`
- Modify: thread/new screens to call send
- Modify: manifest — SMS role intent filters / `RESPOND_VIA_MESSAGE` as required for default SMS app on API 36 (builder verifies against current platform docs)

**Interfaces:**
- Consumes: `ConversationRepository.getOrCreateThreadId`
- Produces:
  - `class SmsSender(context: Context) { fun send(address: String, body: String, subscriptionId: Int? = null): Result<Unit> }` — uses `SmsManager` for default subscription when `subscriptionId == null`
  - `object SmsRoleHelper { fun isDefaultSms(context: Context): Boolean; fun requestDefaultSms(activity: Activity) }`
  - Settings screen: button “Varsayılan SMS uygulaması” → role request; short note that Google Messages can stay installed

- [ ] **Step 1: Implement sender + role helper + settings affordance; wire send from thread/new**

- [ ] **Step 2: On device — set aramalr as default SMS, send SMS to a second number or self-loop if available; verify appears in thread**

- [ ] **Step 3: Commit**

```bash
git commit -am "feat: send SMS and request default SMS role"
```

---

### Task 7: Incoming SMS path — receiver, notifier, evidence, latest OTP store

**Files:**
- Create: `sms/IncomingSmsReceiver.kt`
- Create: `otp/LatestOtpStore.kt`
- Create: `notify/SmsNotifier.kt`
- Modify: manifest receiver for `SMS_DELIVER` / `SMS_RECEIVED` as appropriate for default app
- Modify: wire `OtpEvidenceLog` + `OtpDetector` + `LatestOtpStore`

**Interfaces:**
- Consumes: `OtpDetector`, `OtpEvidenceLog`
- Produces:
  - `object LatestOtpStore { fun publish(match: OtpMatch, sender: String, receivedAtMillis: Long); fun latest(): Pair<OtpMatch, Long>?; fun clear() }` — in-memory + optional short TTL (e.g. 5 minutes)
  - Receiver: persist via Telephony (default app path), notify, `detect` → `LatestOtpStore.publish`, `OtpEvidenceLog.append` with `assistPath` `"pending"` then update when autofill uses it (Task 8 may set `"autofill"`)
  - Review Focus: assemble full body for multipart before detect/log

- [ ] **Step 1: Implement receiver + notifier + LatestOtpStore + evidence append on receive**

- [ ] **Step 2: With app as default — receive an SMS; notification shows; evidence file gains a line (`adb shell run-as com.aramalr.messages cat ...` or in-app debug later)**

- [ ] **Step 3: Commit**

```bash
git commit -am "feat: handle incoming SMS with notify, OTP store, and evidence log"
```

---

### Task 8: AutofillService for cross-app OTP

**Files:**
- Create: `autofill/AramalrOtpAutofillService.kt`
- Modify: manifest `service` with `android.permission.BIND_AUTOFILL_SERVICE` and autofill intent filter
- Modify: Settings screen — deep link / instructions to enable “aramalr” under system Autofill service
- Modify: evidence — when dataset is filled/selected, append/update `assistPath = "autofill"`

**Interfaces:**
- Consumes: `LatestOtpStore.latest()`
- Produces: `AramalrOtpAutofillService` extending `AutofillService` — on fill request, if latest OTP unexpired, offer dataset for fields with SMS OTP hints or heuristic password/otp-ish fields; if no field match, leave evidence `assistPath` as `"none"` (Review Focus)

- [ ] **Step 1: Implement AutofillService + settings enablement copy**

- [ ] **Step 2: On device — enable autofill service; trigger a login/OTP field (or a minimal test app if needed); confirm dataset appears when `LatestOtpStore` has a code**

- [ ] **Step 3: Commit**

```bash
git commit -am "feat: AutofillService for cross-app OTP fill"
```

---

### Task 9: Evidence browse + empty/permission polish + device acceptance

**Files:**
- Modify: `ui/settings/AppSettingsScreen.kt` — list recent evidence events (sender, detected code yes/no, assistPath), Clear button calling `OtpEvidenceLog.clear()`
- Modify: conversation list empty + permission-denied states (MyOS cards, Turkish copy)
- Modify: any remaining Review Focus gaps found on device

**Interfaces:**
- Consumes: `OtpEvidenceLog.readAll()` / `clear()`
- Produces: partner-visible on-device log UI for OTP iteration

- [ ] **Step 1: Settings evidence section + empty/permission UI**

- [ ] **Step 2: Acceptance on NX741J**

Checklist:
- List/thread match MyOS card language; dynamic accent follows theme
- Send/receive as default SMS
- OTP chip copy in thread
- Autofill enabled path works or evidence shows `assistPath=none` honestly
- Evidence log accumulates; clear works
- Google Messages still installed

- [ ] **Step 3: Commit**

```bash
git commit -am "feat: evidence UI and permission/empty polish"
```

---

## Self-review notes

- Spec goals 1–6 mapped to Tasks 3–9; OTP logging plan → Tasks 2, 7, 9; Autofill priority → Task 8; non-goals respected (no RCS/archive).
- Review Focus items pinned: multipart (Task 7), keyword vs amount (Task 1 tests), missing OTP hints (Task 8), non-default role (Task 6 settings + Task 7 degrade), empty/permission (Task 9).
