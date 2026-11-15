# Genesis School SaaS Platform — Workspace Rules

## 1. Product, Engineering & Unit-Economics Principles (5 Non-Negotiable Pillars)
1. **Teacher-First Simplicity (Zero Extra Work)**: Every feature MUST save time or improve teaching—never create unnecessary or duplicate data-entry work (target <= 30–60 seconds for attendance, <= 2 minutes for multi-section homework via templates, single-entry marks with auto-grade & auto-status-card generation, and automated substitution matching).
2. **Reliability Over Feature Quantity**: Attendance, fee payments, notifications, PDF downloads, and student records must work consistently and deterministically (online and offline-resilient) with production-ready UX (no `(Demo)` labels or dead Toast-only placeholders).
3. **Automation by Default**: Whenever the system has enough information to perform a repetitive task safely (e.g., `WHEN -> CHECK CONDITION -> THEN -> RECORD` workflows, auto-generating parent achievement cards from exam marks/attendance, auto-assigning counsellors on QR referral leads), it must do so automatically.
4. **Student & Parent Daily Usefulness**: The app must provide genuine daily value (consolidated daily update, homework & doubt companion, voice-to-text academic dictation, celebrate-worthy achievement cards)—not merely display static administrative tables.
5. **Management ROI (₹1 Lakh School Value Standard)**: Every school owner/principal must be able to see measurable financial return—what the school saves, improves, and gains (organic admissions via Proud Parent WhatsApp Status + Referral QR, faster fee collection, financial leakage prevention, student retention, and printing/SMS/staff cost reduction)—while keeping per-student recurring cloud AI cost at $0.

## 2. Android Compose UI, Navigation & Identity Guardrails (`apps/android`)
- **Back Navigation Invariant**: Every sub-screen and modal route MUST register an `androidx.activity.compose.BackHandler` that returns to its parent screen, and non-Home bottom tabs (`My Class`, `Doubts`, `Forms`) MUST route Back to `Home` (`selectedItem = 0`) so pressing Back never exits the app unless already on the `Home` root.
- **Royal School Blue Header & Button Contrast**:
  - Maintain clean, high-contrast **Royal School Blue (`#1E40AF`)** status bar and top headers across tabs and sub-screens.
  - Never set a light/white `color` globally on `Typography.labelLarge` in `Theme.kt`. Always verify explicit, high-contrast foreground `color` on `Button`, `OutlinedButton`, and `TextButton` labels and icons.
- **One-Time Student Onboarding Lock, Section Roster Verification & Academic Year Rollover**: First-time student login captures Full Name, Roll Number, Class, Section, mandatory Parent Contact Number, optional Parent Email, and Residential Address (with GPS `Geocoder` option) behind a strict verification warning pop-up, then locks `is_profile_locked = true` and registers the student in that Class-Section's Student Roster. Teachers/Faculty can Verify, Block, Unblock, or Edit students per section, create new Classes/Sections dynamically, promote/shift students across classes at year-end, and selectively keep or delete section-wise Notes, Doubts, Homework, or Roster data.

## 3. Build & Verification Commands
- **Backend (`backend/`)**: Verify changes with `npm --prefix /Users/polojuhrishikesh/appv1/backend run build`.
- **Android (`apps/android/`)**: On macOS with Android Studio's bundled JBR, compile debug APKs via `/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java` invoking `gradle/wrapper/gradle-wrapper.jar` with `-Dorg.gradle.java.home="/Applications/Android Studio.app/Contents/jbr/Contents/Home" assembleDebug`.

