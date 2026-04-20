# 🚀 DevPulse (데브펄스)

DevPulse는 전 세계 주요 기술 블로그와 RSS 피드를 실시간으로 수집하여 개발자들에게 최신 트렌드를 제공합니다.

---

## ✨ 주요 기능 (Key Features)

- 📰 **실시간 뉴스 통합 수집**: Android, Medium, Kotlin Blog 등 주요 기술 소스의 RSS 통합 피드 제공.
- 🌐 **온디바이스 AI 번역**: Google ML Kit을 사용하여 영어 제목을 한국어로 즉시 번역.
- 🔔 **관심 키워드 알림**: 등록된 키워드 매칭 시 백그라운드 푸시 알림 발송 및 기사 즉시 이동.
- ⚙️ **동적 콘텐츠 관리**: 사용자가 직접 RSS 소스 및 알림 키워드를 추가/삭제할 수 있는 커스텀 기능.
- ⏱️ **읽기 편의성 제공**: 단어 수 기반 예상 읽기 소요 시간 표시 및 Chrome Custom Tabs 브라우징.

---

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Dependency Injection**: Dagger Hilt
- **Architecture**: Clean Architecture (Multi-module: app, core, model)
- **Networking**: Retrofit 2, OkHttp
- **Database**: Room DB (v5)
- **Background Task**: WorkManager
- **ML Service**: Google ML Kit (Translate)
- **Image Loading**: Coil
- **In-App Browser**: Chrome Custom Tabs

---

## 📂 프로젝트 구조 (Project Structure)

```
DevPulse/
├── :app    # Presentation 레이어: UI, ViewModel, NewsWorker 및 알림 구현
├── :core   # Data 레이어: API 통신, Repository 패턴, Database(Room) 및 DI 설정
└── :model  # Domain 레이어: 프로젝트 전반에서 공유되는 공통 데이터 엔티티 및 모델
```

---

## 🚀 시작하기 (Getting Started)

1. 이 저장소를 클론(Clone)합니다.
2. 안드로이드 스튜디오 최신 버전에서 프로젝트를 엽니다.
3. **Gradle Sync**를 완료한 후 앱을 실행합니다.