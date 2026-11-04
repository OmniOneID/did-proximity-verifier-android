# Verifier AOS 가이드

## 개요
이 문서는 OpenDID **근접(proximity) 검증** 안드로이드 앱에 대한 가이드입니다.
홀더 단말이 온라인 상태가 아니어도, 근접 전송으로 주고받은 크리덴셜 제시를
검증하는 Verifier(리더) 역할을 수행합니다.

OpenDID 에서 `did-ca-aos`(지갑)는 OID4VP 의 **홀더** 절반을 구현하고,
`did-verifier-server`는 **인터넷** 전송에서의 Verifier 절반을 담당합니다.
이 앱은 **근접** 전송에서의 Verifier 절반을 담당합니다.

> 현재는 프로젝트 골격 단계이며, 근접 프로토콜 구현이 진행 중입니다.

### 예정 기능
| 기능 | 내용 |
| ---- | ---- |
| Device engagement | QR 기반 홀더 단말 engagement |
| 근접 전송 | BLE 세션 수립 및 암호화된 데이터 수신 |
| 제시 검증 | vp_token / mdoc 검증, 크리덴셜 상태 확인 |
| 결과 표시 | 검증된 클레임을 운영자에게 표시 |

## S/W 사양
| 구분             | 내용                                                      |
| ---------------- | --------------------------------------------------------- |
| OS               | Android 16 (API Level 36)                                 |
| 언어             | Java 21                                                   |
| IDE              | Android Studio 2025.3 이상                                |
| 빌드 시스템      | Gradle 9.4.1 / Android Gradle Plugin 9.1.0                |
| 아키텍처         | MVVM (Activity + ViewModel + LiveData + Repository), Hilt |
| UI               | View + XML, ViewBinding / DataBinding (Material 3)        |
| 호환성           | compileSdk 36, targetSdk 36, minSdk 26                    |
| 테스트 환경      | 최소: Android 8.0 (Oreo, API Level 26)                    |
|                  | 권장: Android 16 (API Level 36)                           |

**근접 기능은 실기기가 필요합니다.** 에뮬레이터는 BLE advertise/scan 을 지원하지 않으므로,
종단 간 검증에는 지갑 1대 + 검증앱 1대, 총 실기기 2대가 필요합니다.

## 프로젝트 클론 및 체크아웃
```git
git clone https://github.com/OmniOneID/did-proximity-verifier-android.git
```

## 빌드 방법
1. Android Studio 를 설치합니다.
2. File > Open 으로 `source/did-proximity-verifier-android` 폴더를 엽니다.
3. 대상 디바이스를 선택합니다. 근접 기능 확인에는 실기기가 필요합니다.
4. Run 을 눌러 빌드하고 설치합니다.

커맨드라인:
```bash
cd source/did-proximity-verifier-android
./gradlew assembleDebug
```

CI 와 동일한 검사를 실행하려면:
```bash
./gradlew build
```

## 프로젝트 구조
```
source/did-proximity-verifier-android/
└── app/                 검증앱 (org.omnione.did.proximity.verifier)
    ├── libs/            근접 SDK 리더 AAR (proximity-reader + proximity-core)
    └── src/main/java/org/omnione/did/proximity/verifier/
        ├── data/
        │   ├── model/       도메인 모델
        │   └── repository/  도메인 리포지토리
        ├── ui/<feature>/    화면 1개당 패키지 1개 (Activity + ViewModel + UiState)
        └── util/            유틸리티
```

각 화면은 `ui/<feature>/` 아래에 Activity + 전용 ViewModel + UiState 로 구현합니다.

### 근접 SDK
리더 표면은 [근접 SDK](https://github.com/OmniOneID/did-proximity-sdk-android) 의 단일 AAR 파일로
`app/libs/` 에 둡니다. AAR 파일에는 POM 이 없으므로 `proximity-core` 의 의존성은
`app/build.gradle` 이 직접 선언합니다. SDK 를 갱신하려면 SDK 저장소에서 리더 AAR 을 빌드해
`app/libs/` 의 파일을 교체하고 `app/build.gradle` 의 파일명을 맞춥니다.

## 변경 이력

- [Verifier AOS](CHANGELOG.md)

## OpenDID 시연 영상 <br>
OpenDID 시스템의 시연 영상은 [Demo Repository](https://github.com/OmniOneID/did-demo-server) 에서 확인할 수 있습니다. <br>

## 기여

행동 강령과 Pull Request 제출 절차는 [CONTRIBUTING.md](CONTRIBUTING.md) 와
[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) 를 참고하십시오.

## 라이선스
[Apache 2.0](LICENSE)
