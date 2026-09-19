# Fix IllegalStateException in AndroidComposeView

The application is crashing with `java.lang.IllegalStateException: The ACTION_HOVER_EXIT event was not cleared.` in `AndroidComposeView`. This is a known framework issue in older versions of Jetpack Compose (1.6.x) that occurs when hover events (e.g., from a mouse or stylus) are processed for components that are being removed from the UI hierarchy.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/User/.gemini/antigravity/scratch/aura-customization-app/android-app/gradle/libs.versions.toml)
- Update `composeBom` to `2026.08.00` (latest stable).
- Update `activityCompose` to `1.13.0`.
- Update `navigationCompose` to `2.10.0`.
- Update `lifecycleRuntimeKtx` to `2.11.0`.

### UI Components

#### [MODIFY] [ExploreScreen.kt](file:///C:/Users/User/.gemini/antigravity/scratch/aura-customization-app/android-app/app/src/main/java/com/aura/launcher/customization/explore/ExploreScreen.kt)
- Improve `ModalBottomSheet` dismissal in `AccessibilityDialog` and `PerfAuditDialog`. Instead of immediately removing the dialog from the composition (which can orphan hover events), we will use `sheetState.hide()` to animate the dismissal and ensure cleaner disposal.

## Verification Plan

### Automated Tests
- Run `./gradlew app:assembleDebug` to ensure dependency updates are compatible.

### Manual Verification
- Deploy the app and navigate to the "Explore Ground" screen.
- Open the "Performance Audit" or "Accessibility" bottom sheets.
- Interact with buttons inside the sheets and dismiss them (ideally while hovering if using an emulator/mouse).
- Verify that the `ACTION_HOVER_EXIT` crash no longer occurs.
