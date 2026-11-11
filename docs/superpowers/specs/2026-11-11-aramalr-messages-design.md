# aramalr Messages — Design

**Date:** 2026-11-11  
**Device context:** Nubia Z80 Ultra (NX741J), Global MyOS, Android 16  
**Status:** Approved 2026-11-11 (builder-check questions resolved via on-device logging plan)

## Intent and why

Build a first-party **Messages** app for personal daily use on a Nubia Z80 Ultra so the messaging surface matches the phone’s system UI (Settings, notification shade, control center) instead of stock Google Messages Material chrome.

The user is happy with Google Messages’ *capabilities* (especially spam-adjacent reliability and verification-code flow) but not the visual break from MyOS. The product goal is **own UI + system/theme integration**, with verification-code delivery into *other* apps as a first-class, improved feature — not a themed skin over Google Messages.

Project / package working name: **aramalr**.

## Goals

1. **MyOS-native look:** Match Global MyOS Settings language — pure black (dark) / system light surfaces, grouped rounded cards (~high radius), large left-aligned titles, generous spacing, no harsh list dividers, Noto Sans / system font.
2. **Theme-driven color:** Accents and tonal surfaces come from the **phone’s current theme / Material You dynamic color**, not hardcoded brand blues from one screenshot session.
3. **Dark and light:** Both modes follow system (or explicit user override later if cheap); structure stays MyOS-like in both.
4. **Core SMS:** Conversation list, thread view, compose, send/receive SMS/MMS as needed for basic texting. App can be set as the **default SMS app** (role).
5. **Cross-app verification codes (priority):** When a bank/login screen needs an SMS code, aramalr should detect the code and fill or offer fill into the other app **more reliably than Google Messages** on this device. In-thread “copy code” chip is supporting, not the main win.
6. **Keep Google Messages installed during development** for side-by-side comparison; do not require uninstalling it to build or test UI.

## Non-goals (v1)

- RCS
- Group chats as a product feature set beyond whatever minimal multiparty SMS already requires for correctness
- Starred messages, archive, categories, “spam inbox” product UI
- Replacing Phone / Contacts in v1 (future family of apps; same design language later)
- Theming or hooking into the Google Messages APK
- Root, Xposed, or system overlay theming
- iOS parity features, stickers marketplace, chat bots

## Anti-goals

- Shipping a UI that still “feels like Google Messages” on this phone
- Hardcoding screenshot accent hexes so the app fights the user’s theme
- Breaking banking/login flows or capturing SMS for anything other than user-visible OTP assist
- Claiming default-SMS + autofill “works” without real-device checks against apps the user actually uses

## Constraints

- **Platform:** Android only; target device Android 16 (API as required by role/autofill on that release).
- **Stack (chosen):** Native **Kotlin + Jetpack Compose**.
- **Default SMS:** Must request and hold the SMS role to be the real messaging surface.
- **OTP into other apps:** Prefer Android **Autofill Framework** (`AutofillService`) and related SMS/user-consent patterns; improve detection heuristics beyond naive regex where Google fails on this device. Exact detection/fill pipeline is left to the builder within privacy-safe, user-consented Android APIs — no accessibility-abuse as the primary design.
- **Design reference:** Partner-provided MyOS screenshots under the Cursor project assets (Settings cards, Wi‑Fi/SIM pages, notification shade, control center, personalization). Structure from those; color from live theme.
- **Device available over ADB** (`NX741J`) for visual and OTP verification.
- **Language:** Turkish UI copy is fine and expected for personal use; no localization requirement in v1.
- **Repo:** `/Users/farukkaygisiz24/Çalışmalar/Projects/aramalr` (greenfield).

## Approaches considered

| Approach | Summary | Tradeoff |
|----------|---------|----------|
| **A. Native Kotlin + Compose** | Own SMS app + AutofillService + dynamic color | Most control; right layer for cross-app OTP. **Chosen.** |
| **B. Flutter / RN** | Faster generic UI | Default SMS + AutofillService painful; weak for the OTP goal. |
| **C. Fork QKSMS / similar** | Inherit SMS plumbing | Design and autofill still custom; inherit foreign architecture. |

**Partner chose A.**

Secondary product approach (already decided earlier): **own UI apps**, not theming Google’s APK (update-fragile). Backend/system capabilities (theme, autofill, SMS APIs) remain; Google Messages stays as comparison reference, not a dependency.

## Product shape (v1)

### Screens

1. **Conversation list** — Large “Mesajlar” title, search field in MyOS capsule style, conversations in card/grouped list language matching Settings rows (avatar/initial, title, snippet, time; unread affordance). FAB or clear compose action consistent with system density.
2. **Thread** — Back + contact/number title; bubbles with MyOS radius; composer bar; detected OTP highlighted with copy affordance.
3. **Compose new** — Recipient + body; enough to send a first SMS.
4. **Minimal settings (in-app)** — Only what v1 needs (e.g. open system default-SMS settings, dual-SIM default hint if the platform exposes it). Not a second “Google Messages settings” clone.

### Dual SIM

Device is dual-SIM. v1 should **respect system default SIM for SMS** and not invent a complex SIM picker UI unless send fails without one. Builder may add the smallest OS-consistent chooser if required for correctness on NX741J.

### OTP / Autofill behavior

- **Primary:** When user is in another app’s OTP field, aramalr (as SMS handler + AutofillService) should surface/fill the code with higher hit rate than Google Messages on real messages the partner receives (banks, Google, etc.).
- **Secondary:** In-thread one-tap copy when a code is detected.
- **Success metric (informal):** Side-by-side on the same device — codes that Google misses or fills late are caught or filled more often by aramalr; no worse for common senders.
- **Privacy:** Codes used only for assist/fill; no exfiltration; clear user enablement for Autofill service in system settings.

### OTP evidence (partner cannot enumerate failures upfront)

Partner cannot currently list which senders/apps Google Messages fails on, or which assist path succeeds when it works. **v1 therefore includes an on-device research/logging loop** so real personal SMS become the dataset:

1. **Local log only** — on this device (and optional manual export the partner triggers). No cloud sync, no third-party analytics. Data is the partner’s own messages.
2. **What to capture (redactable):** incoming message body (or hashed/redacted form in later hardening), sender, timestamp, whether an OTP-like token was detected, parser confidence, and whether autofill/copy assist was offered or accepted. Builder picks storage format (e.g. app-private DB/file).
3. **Compare when useful:** while Google Messages remains installed, partner can note “Google missed this / was late” against the same log entry; structured side-by-side automation is optional, not required for v1.
4. **Architecture implication:** ship **AutofillService as the primary cross-app path**, plus in-thread copy. Logging will show if target apps never expose OTP autofill fields; then builder adds a **secondary assist** (e.g. notification action / clipboard offer) guided by evidence — not guessed upfront.
5. **Parser iteration:** improve detection rules from logged real samples after daily use; initial heuristics can be generic.

This closes the builder-check gaps without blocking design approval.

## Visual system (from MyOS reference)

Extracted structural tokens (dark reference); light mode mirrors structure with system light surfaces:

- Background: true black in dark (`#000000` class), not lifted gray scrap
- Cards / groups: dark elevated surface (~`#1A1A1A` class in dark), large corner radius (~20–24dp class)
- Primary text: white / high emphasis; secondary: muted gray
- Accent: **dynamic** (screenshot periwinkle was theme sample only)
- Controls: pill chips, soft toggles, chevrons — match Settings density
- Font: system / Noto Sans as on device
- Motion: subtle, system-like; no playful Material Expressive divergence that breaks MyOS calm

Builder owns exact Compose theme mapping (`ColorScheme` from dynamic color + custom shapes/spacing to mimic Settings cards).

## Architecture (builder-owned detail)

Suggested shape (not binding micro-design):

- `app` module — Compose UI, navigation, role entry points
- SMS data via Telephony / role-appropriate observers and send APIs
- OTP detector module — pluggable parsers + tests against sample message bodies
- `AutofillService` implementation — dataset for SMS OTP fields
- Theme — `dynamicDarkColorScheme` / `dynamicLightColorScheme` + MyOS shape/spacing overlay

## What’s left to the builder

- Module/package naming, Gradle/AGP versions, minSdk (must support roles + autofill on this phone)
- Exact navigation library, architecture pattern (e.g. simple UI + repositories)
- MMS support depth (images) — only as needed so basic SMS daily use isn’t broken
- OTP parser strategy and AutofillService field-matching details
- Log schema, retention (builder default: keep on device, partner-clearable, no upload), and a minimal in-app or debug way to browse/export logs
- Notification style for incoming SMS (should not scream “stock Google”)
- CI, signing, Play distribution — out of scope unless partner asks; sideload via ADB is enough for v1
- Automated UI tests extent

## Calls made by the author (check while reading)

- **Stack:** Kotlin + Jetpack Compose (Approach A) — partner-confirmed.
- **v1 scope:** SMS list/thread/compose only; no RCS/stars/archive/group product features.
- **Color:** dynamic from system theme; MyOS structure from screenshots.
- **OTP priority:** cross-app fill over in-thread copy; AutofillService primary; secondary assists only if logs show autofill cannot reach real banking/login fields.
- **Evidence:** on-device SMS/OTP logging instead of requiring failure examples before build.
- **Dual SIM:** respect system SMS default; smallest chooser only if send requires it on NX741J.
- **Distribution:** ADB sideload for v1; no Play requirement.
- **Google Messages:** stays installed for comparison during development.

## Out of scope follow-ons (later projects)

- Phone (dialer) and Contacts apps in the same design language
- Richer messaging (RCS, archive, stars, spam UI)
- Deeper Nubia system APK resource extraction if dynamic color + screenshots prove insufficient

## Approval

Partner approval of this document is required before implementation planning or scaffolding.

**Doc path:** `docs/superpowers/specs/2026-11-11-aramalr-messages-design.md`
