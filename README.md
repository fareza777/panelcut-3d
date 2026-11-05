# PanelCut 3D

A local-first Android studio for designing furniture in 3D, nesting panels on sheets, producing cut lists, and estimating costs.

## Features

- Parametric 3D furniture editor and 2D plan view
- Immersive fullscreen 3D viewer with a light or dark stage
- MaxRects nesting with kerf, trim, and grain settings
- Cut lists, edge banding, material and labor costing
- PDF and CSV exports
- Offline-capable core tools with local Room storage
- AdMob banner, interstitial, and opt-in rewarded ads
- One-time **Remove Ads** purchase through Google Play
- Light, dark, and system app themes with USD/IDR and other currency display options

## Build

Requirements: Android Studio or JDK 17 with Android SDK, plus an Android device or emulator (minSdk 26).

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:testDebugUnitTest
```

The signed release APK is written to `app/build/outputs/apk/release/app-release.apk` when `keystore.properties` is present. Signing files are intentionally git-ignored.

## AdMob setup

The app ships with Google's official AdMob **test** application and ad-unit IDs, so no real impressions are generated during development. To use production inventory, add these values to the git-ignored `local.properties`:

```properties
admob.app.id=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
admob.banner.id=ca-app-pub-XXXXXXXXXXXXXXXX/BBBBBBBBBB
admob.interstitial.id=ca-app-pub-XXXXXXXXXXXXXXXX/IIIIIIIIII
admob.rewarded.id=ca-app-pub-XXXXXXXXXXXXXXXX/RRRRRRRRRR
```

The app ID and unit IDs are injected into the manifest and `BuildConfig` at build time. Never commit `local.properties` or production ad credentials.

Before publishing with real ads, configure the consent message/privacy options in the AdMob console and complete the consent flow required for your distribution regions. The current build initializes Google Mobile Ads directly; test IDs are the default so development traffic is safe.

## Remove Ads (in-app purchase)

The app queries a one-time, non-consumable product named:

```text
remove_ads
```

Create that product in Play Console → Monetize → Products → In-app products with a price of **US$4.99**, activate it, and publish a build to an internal testing track. Add a license tester account and test the purchase, restore, and acknowledgement flow from that Play-installed build.

Purchases are acknowledged and the entitlement is stored in local app settings. It is restored on startup from Google Play. A Play-installed build is mandatory for billing; sideloaded APKs cannot complete real purchases.

## Privacy

Projects, materials, and settings are stored in a local Room database. Design data is not uploaded to a server. Core design, nesting, cut-list, and export tools work without a connection.

Ads are served by Google AdMob and purchases are processed by Google Play. Those services require internet access and may collect device, usage, and advertising data under Google's policies. The privacy policy inside the app describes this behavior.

## Project notes

- Package: `com.panelcut3d.app`
- Version: `1.0.0` (`versionCode` 1)
- Target SDK: 35, min SDK: 26
- 3D renderer: Three.js in an Android WebView, bundled locally
- Tests: `AppCurrencyTest` and `NestingTest` under `app/src/test`
- Keep `keystore.properties`, `keystore/`, `*.jks`, and `local.properties` out of version control.

The MaxRects nesting implementation and the application source are part of this repository. Third-party components retain their respective licenses.
