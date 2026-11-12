# Faza 19 — Neutral Age Screen (Google Play Families Policy)

**Date:** 2026-11-12
**Trigger:** Play Console rejection (enforced 2026-11-11), "Families Policy Requirements: Neutral Age Screen" — the app was removed from Google Play.

## The rejection

> Apps targeting both children and adults must not use APIs or SDKs that aren't approved for children unless they have a neutral age screen. [...] Don't preset the birthdate to the required age or say that users need to be a certain age to use parts of the app.
>
> - The age screen [...] should be neutral and is not pre-defined or populated to age 13 years old and above.
> - Ensure that your app's age screen implementation will filter inappropriate app contents for users 13 years old and below.

## Why the old screen failed

The onboarding "parent gate" (`ParentGateStep`, `isAdultBirthYear`, 18–100) was not a neutral age screen:

1. **Not neutral** — titled "This part is for parents" / "Your year of birth", i.e. it told the user which answer was expected.
2. **Retryable** — a wrong year showed "Please ask a grown-up", cleared the field and allowed another try; nothing was saved.
3. **No child path, no filtering** — a pass/fail gate; the result never changed which SDKs or features ran.
4. **SDKs before the screen** — Firebase Analytics and Crashlytics auto-initialise at process start; RevenueCat started in `MainActivity.onCreate`.

## Decisions (user, 2026-11-12)

| Question | Decision |
|---|---|
| What is off for a child (≤ 13)? | Firebase Analytics + Crashlytics, Kidzo AI (Gemini), sign-in + purchases (RevenueCat), push (FCM) |
| Where is the screen? | Language → **neutral age screen** → (adult: sign-in) → child profile → handoff. The old parent gate is removed. |
| Existing installs? | Asked **once** on the first launch after the update. |

## Design

### `agegate/AgeGateRepository`
The user-provided repository, kept API-compatible but backed by `SharedPreferences` (file `age_gate`) instead of DataStore: the answer must be read synchronously in `Application.onCreate`, before any SDK, and the project has no DataStore dependency.

- `save(year, month)` writes once; later calls return `false` and change nothing.
- `bracketFor`: age `<= 13` → `CHILD`, otherwise `TEEN_OR_ADULT` (birth day assumed the 1st).
- `AgeGateState.isRestricted` is `true` for `CHILD` **and** for an unanswered screen.

### `agegate/AgeGatePolicy`
Applied by `AgeGate.init` (first thing in `Application.onCreate`) and again right after the answer:

| SDK | Manifest default | Adult | Child / unanswered |
|---|---|---|---|
| Firebase Analytics | `firebase_analytics_collection_enabled=false` | enabled | disabled |
| Crashlytics | `firebase_crashlytics_collection_enabled=false` | enabled | disabled, unsent reports deleted (child) |
| FCM | `firebase_messaging_auto_init_enabled=false` | auto-init on, token registered | off, token deleted (child) |
| RevenueCat | — | configured (`MainActivity`, also mid-session) | never configured |

### Feature filtering
- `FeatureGate(restricted = { AgeGate.isRestricted })`: `isHidden(feature)` for all account-only features (AI companion, cloud sync, premium, parent dashboard). Hidden features open **nothing** — no sign-in prompt.
- `MainScreen`: no Kidzo agent (no Gemini), no Kidzo FAB, AI requests silently ignored.
- `KidZoneApp`: dashboard / premium / account routes are not navigated to when hidden.
- Web UI: `index.html?...&child=1` → `<html class="kz-child">` → `.ai-only` (AI Studio header, Kidzo, AI story, personal story) hidden. "Magic sleep" (local audio) stays.
- Onboarding: a child skips sign-in, names themself ("Your name"), and does not see "hand the phone to your child".

### The screen (`agegate/NeutralAgeScreen`)
- Month chips + 4-digit year; nothing preselected.
- Texts only ask for the date of birth — no parents, no age limit, no hint.
- The only validation is age-independent (not in the future, ≤ 120 years ago).
- Shown in onboarding (step 0) and as the `age_gate` start route for existing installs.

## Not changed
- Firebase Auth (anonymous guest session) and Firestore (`users/{uid}`, banners, challenges) still start for everyone — they are the app's own backend, not ad/analytics SDKs, and carry no personal data for a guest.
- `AccountGate`'s math lock (`parentVerified`) is unrelated to this policy.

## Tests
- `AgeGateRepositoryTest` — the 13/14 boundary, save-once, restart, input validation, screen validation.
- `AccountStateTest` — `FeatureGate` restricted mode.
- `OnboardingViewModelTest` — adult and child flows, no way back to the age screen.

## Before resubmitting to Play (manual)
1. Fresh install, answer as a child (e.g. 2018): no sign-in step; no Kidzo FAB, no AI Studio buttons; parent dashboard and premium do not open; no notification permission prompt.
2. Fresh install, answer as an adult: sign-in step, AI, premium and push work as before.
3. Upgrade an existing install: the age screen appears once, then never again (also after force-stop).
4. Debug build: `adb shell setprop log.tag.FA VERBOSE` — no Analytics events before the answer or for a child.
5. Play Console → App content → Target audience and content: keep the declared age groups consistent with this behaviour; update `kidzone-privacy.html` if it describes Analytics/Crashlytics/AI for all users.
