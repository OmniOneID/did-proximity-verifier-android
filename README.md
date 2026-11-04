# Verifier AOS Guide

## Overview
This document is a guide for the OpenDID **proximity Verifier** Android application.
The app plays the Verifier (reader) role of a credential presentation exchanged over a
proximity transport, without requiring the holder device to be online.

In OpenDID, `did-ca-aos` (the wallet) implements the **holder** half of OID4VP, and
`did-verifier-server` implements the Verifier half for **internet** transport.
This app implements the Verifier half for **proximity** transport.

> This project is at the scaffolding stage. The proximity protocol implementation is in progress.

### Planned Features
| Feature | Details |
| ------- | ------- |
| Device engagement | QR based engagement with a holder device |
| Proximity transport | BLE session establishment and encrypted data retrieval |
| Presentation verification | vp_token / mdoc verification, credential status check |
| Result display | Verified claim presentation to the operator |

## S/W Specifications
| Category         | Details                                                 |
| ---------------- | ------------------------------------------------------- |
| OS               | Android 16 (API Level 36)                               |
| Language         | Java 21                                                 |
| IDE              | Android Studio 2025.3 or later                          |
| Build System     | Gradle 9.4.1 / Android Gradle Plugin 9.1.0              |
| Architecture     | MVVM (Activity + ViewModel + LiveData + Repository), Hilt |
| UI               | View + XML with ViewBinding / DataBinding (Material 3)  |
| Compatibility    | compileSdk 36, targetSdk 36, minSdk 26                  |
| Test Environment | Minimum Requirements: Android 8.0 (Oreo, API Level 26)  |
|                  | Recommended Requirements: Android 16 (API Level 36)     |

**Proximity features require physical devices.** Emulators do not support BLE
advertising or scanning, so end-to-end verification needs two real devices —
one running the wallet and one running this app.

## Project Clone and Checkout
```git
git clone https://github.com/OmniOneID/did-proximity-verifier-android.git
```

## Build Method
1. Install Android Studio.
2. Open the project folder `source/did-proximity-verifier-android` via File > Open.
3. Select a target device. A physical device is required for any proximity feature.
4. Click Run to build and install.

Command line equivalent:
```bash
cd source/did-proximity-verifier-android
./gradlew assembleDebug
```

To run the same checks as CI:
```bash
./gradlew build
```

## Project Structure
```
source/did-proximity-verifier-android/
└── app/                 Verifier application (org.omnione.did.proximity.verifier)
    ├── libs/            Proximity SDK reader AAR (proximity-reader + proximity-core)
    └── src/main/java/org/omnione/did/proximity/verifier/
        ├── data/
        │   ├── model/       Domain models
        │   └── repository/  Domain repositories
        ├── ui/<feature>/    One package per screen (Activity + ViewModel + UiState)
        └── util/            Utilities
```

Each screen is implemented as an Activity with its own ViewModel and UiState under
`ui/<feature>/`.

### The proximity SDK
The reader surface comes from the
[proximity SDK](https://github.com/OmniOneID/did-proximity-sdk-android) as a single AAR in
`app/libs/`. AAR files carry no POM, so `app/build.gradle` declares the dependencies of
`proximity-core` itself. To update the SDK, build the reader AAR in the SDK repository, replace the
file in `app/libs/` and update the file name in `app/build.gradle`.

## Change Log

ChangeLog can be found :
<br>
- [Verifier AOS](CHANGELOG.md)

## OpenDID Demonstration Videos <br>
To watch our demonstration videos of the OpenDID system in action, please visit our [Demo Repository](https://github.com/OmniOneID/did-demo-server). <br>

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for details on our code of conduct, and the process for submitting pull requests to us.

## License
[Apache 2.0](LICENSE)
