# 🛠️ Flutter → Android Native 마이그레이션 가이드

## 1. 개요

- 본 문서는 기존 Flutter 기반 **임대주택 공고 앱(imdaesomun)**을 Android Native(Kotlin, Jetpack Compose)로 마이그레이션하는
  실무 가이드입니다.
- PRD(제품 요구사항 명세서)와 기존 Flutter 구조를 참고하여, Android에서의 구현 전략과 매핑을 안내합니다.
- **Firebase 백엔드는 그대로 유지**하고, 클라이언트만 Android Native로 변경합니다.

### 1.1 현재 앱 정보

- **앱명**: imdaesomun (임대소문)
- **Flutter 버전**: 3.29.2, Dart 3.7.2
- **주요 기능**: SH/GH 임대공고 조회, 저장, 푸시 알림
- **백엔드**: Firebase Functions + Firestore + FCM

---

## 2. 전체 구조 매핑

| 구성 요소   | Flutter (기존)              | Android Native (마이그레이션)          |
|---------|---------------------------|----------------------------------|
| 언어      | Dart                      | Kotlin                           |
| UI      | Flutter 위젯                | Jetpack Compose                  |
| 상태관리/DI | Riverpod (Provider 패턴)    | ViewModel + StateFlow + Hilt     |
| 라우팅     | go_router                 | Navigation Compose               |
| 네트워킹    | Dio + ApiKeyInterceptor   | Retrofit + OkHttp + Interceptor  |
| 직렬화     | freezed + json_annotation | Kotlinx Serialization            |
| 로컬 저장소  | shared_preferences        | DataStore Preferences            |
| 보안 저장소  | flutter_secure_storage    | EncryptedSharedPreferences       |
| 인증      | Firebase Auth             | Firebase Auth (Android SDK)      |
| DB      | Firestore                 | Firestore (Android SDK)          |
| 푸시      | FCM                       | FCM (Android SDK)                |
| 원격 설정   | Remote Config             | Remote Config (Android SDK)      |
| 폰트      | Pretendard (assets)       | Pretendard (assets/fonts)        |
| 이미지 로딩  | flutter_svg               | Coil + SVG support               |
| 웹뷰      | webview_flutter           | WebView Compose                  |
| 테스트     | flutter_test              | JUnit, Espresso, Compose Testing |

---

## 3. 아키텍처 및 DI/상태관리

- **Flutter**: MVVM + Riverpod (Provider 기반 DI/상태관리)
- **Android**: Clean Architecture + MVVM + Hilt(DI) + ViewModel(State) + StateFlow

### 3.1 상세 매핑

| Flutter 구성                     | Android 구성                        |
|--------------------------------|-----------------------------------|
| `ConsumerWidget` + `WidgetRef` | `@Composable` + `ViewModel`       |
| `AsyncNotifier<T>`             | `StateFlow<UiState<T>>`           |
| `StateProvider<T>`             | `MutableStateFlow<T>`             |
| `Provider<T>` (DI)             | `@Provides` (Hilt Module)         |
| `ref.watch()` / `ref.read()`   | `collectAsState()` / `viewModel.` |

### 3.2 디렉토리 매핑

- `lib/src/core/providers/` → `di/` (Hilt 모듈)
- `lib/src/core/services/` → `data/service/` 또는 `domain/usecase/`
- `lib/src/data/repositories/` → `data/repository/`
- `lib/src/data/sources/remote/` → `data/remote/`
- `lib/src/data/sources/local/` → `data/local/`
- `lib/src/data/models/` → `data/model/` (data class)
- `lib/src/ui/pages/` → `ui/screen/`
- `lib/src/ui/widgets/` → `ui/component/`
- `lib/src/ui/components/` → `ui/component/common/`

---

## 4. 기능별 마이그레이션 전략

<details>
<summary>### 4.1 임대공고 크롤링/저장/조회</summary>

- **백엔드(Firebase Functions, Firestore)는 그대로 사용**
- Android에서는 Firestore SDK로 데이터 조회/저장
- 공고 리스트/상세/저장 기능은 ViewModel + Repository 패턴으로 구현

#### 4.1.1 Flutter 코드 분석

**현재 구조:**

```dart
// lib/src/ui/pages/home/home_page_view_model.dart
class ShNotices extends AsyncNotifier<List<Notice>> {
  @override
  Future<List<Notice>> build() async {
    final notices = await ref.read(noticeRepositoryProvider).getShNotices();
    return notices;
  }
}

// lib/src/data/repositories/notice_repository_impl.dart
class NoticeRepositoryImpl implements NoticeRepository {
  @override
  Future<List<Notice>> getShNotices() async {
    final isLatest = await _noticeSource.isLatestShNotices();
    if (isLatest) {
      final local = await _localSource.getShNotices();
      if (local != null && local.isNotEmpty) return local;
    }
    final remote = await _noticeSource.getShNotices();
    await _localSource.saveShNotices(remote);
    return remote;
  }
}
```

**Android 매핑:**

```kotlin
// ui/screen/home/HomeViewModel.kt
class HomeViewModel @Inject constructor(
    private val noticeRepository: NoticeRepository
) : ViewModel() {
    private val _shNotices = MutableStateFlow<UiState<List<Notice>>>(UiState.Loading)
    val shNotices: StateFlow<UiState<List<Notice>>> = _shNotices.asStateFlow()

    fun loadShNotices() {
        viewModelScope.launch {
            try {
                val notices = noticeRepository.getShNotices()
                _shNotices.value = UiState.Success(notices)
            } catch (e: Exception) {
                _shNotices.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// data/repository/NoticeRepositoryImpl.kt
class NoticeRepositoryImpl @Inject constructor(
    private val remoteDataSource: NoticeRemoteDataSource,
    private val localDataSource: NoticeLocalDataSource
) : NoticeRepository {
    override suspend fun getShNotices(): List<Notice> {
        val isLatest = remoteDataSource.isLatestShNotices()
        if (isLatest) {
            val local = localDataSource.getShNotices()
            if (local.isNotEmpty()) return local
        }
        val remote = remoteDataSource.getShNotices()
        localDataSource.saveShNotices(remote)
        return remote
    }
}
```

#### 4.1.2 데이터 모델 매핑

**Flutter (Freezed):**

```dart
@freezed
abstract class Notice with _$Notice {
  const factory Notice({
    required String id,
    required String seq,
    required int no,
    required String title,
    required String department,
    required int regDate,
    required int hits,
    required String corporation,
    required List<File> files,
    required List<String> contents,
    required String link,
  }) = _Notice;
  
  factory Notice.fromJson(Map<String, dynamic> json) => _$NoticeFromJson(json);
}
```

**Android (Kotlinx Serialization):**

```kotlin
@Serializable
data class Notice(
    val id: String,
    val seq: String,
    val no: Int,
    val title: String,
    val department: String,
    val regDate: Int,
    val hits: Int,
    val corporation: String,
    val files: List<File>,
    val contents: List<String>,
    val link: String
)

@Serializable
data class File(
    val fileName: String,
    val fileLink: String,
    val fileId: String? = null
)
```

</details>

<details>
<summary>### 4.2 인증/사용자 관리</summary>

- Firebase Auth(Android SDK) 연동
- 로그인/회원탈퇴/닉네임 등은 ViewModel에서 처리

#### 4.2.1 Flutter 코드 분석

**현재 구조:**

```dart
// lib/src/ui/widgets/login/login_dialog_view_model.dart
class LoginDialogViewModel extends AutoDisposeNotifier<LoginDialogState> {
  Future<void> signIn({required String email, required String password}) async {
    final userCredential = await ref.read(userRepositoryProvider).signIn(
      email: email, password: password
    );
  }
}

// lib/src/data/repositories/user_repository_impl.dart
class UserRepositoryImpl implements UserRepository {
  @override
  Future<UserCredential> signIn({required String email, required String password}) async {
    return await _userSource.signIn(email: email, password: password);
  }
}
```

**Android 매핑:**

```kotlin
// ui/component/login/LoginViewModel.kt
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _loginState = MutableStateFlow<UiState<FirebaseUser?>>(UiState.Idle)
    val loginState: StateFlow<UiState<FirebaseUser?>> = _loginState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            try {
                val result = userRepository.signIn(email, password)
                _loginState.value = UiState.Success(result.user)
            } catch (e: Exception) {
                _loginState.value = UiState.Error(e.message ?: "Login failed")
            }
        }
    }
}

// data/repository/UserRepositoryImpl.kt
class UserRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {
    override suspend fun signIn(email: String, password: String): AuthResult =
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
}
```

</details>

<details>
<summary>### 4.3 푸시 알림</summary>

- FCM(Android SDK) 연동
- 토큰 관리 및 Firestore 저장 로직 구현

#### 4.3.1 Flutter 코드 분석

**현재 구조:**

```dart
// lib/main.dart
void initFcmToken(WidgetRef ref) async {
  final fcmToken = await FirebaseMessaging.instance.getToken();
  if (fcmToken != null) {
    ref.read(fcmTokenStateProvider.notifier).state = fcmToken;
    await ref.read(userRepositoryProvider).registerFcmToken(token: fcmToken);
  }
}

// lib/src/data/sources/remote/user_source.dart
Future<void> registerFcmToken({required String token, String? userId, bool? allowed}) async {
  await _dio.post('/registerFcmToken', 
    data: {'token': token, 'userId': userId, 'allowed': allowed});
}
```

**Android 매핑:**

```kotlin
// service/FcmService.kt
class FcmService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Repository를 통해 토큰 등록
        val userRepository = EntryPointAccessors.fromApplication(
            applicationContext, UserRepositoryEntryPoint::class.java
        ).userRepository()

        GlobalScope.launch {
            userRepository.registerFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // 푸시 알림 처리
    }
}

// data/repository/UserRepositoryImpl.kt
override suspend fun registerFcmToken(
    token: String,
    userId: String? = null,
    allowed: Boolean? = null
) {
    remoteDataSource.registerFcmToken(token, userId, allowed)
}
```

</details>

<details>
<summary>### 4.4 원격 설정/보안 키 관리</summary>

- Remote Config(Android SDK)로 API Key 수신
- EncryptedSharedPreferences 등으로 안전하게 저장
- 키 갱신/401 대응 로직 구현

#### 4.4.1 Flutter 코드 분석

**현재 구조:**

```dart
// lib/src/core/services/dio_service.dart
class ApiKeyInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode == 401) {
      final remoteConfig = FirebaseRemoteConfig.instance;
      await remoteConfig.fetchAndActivate();
      final newApiKey = remoteConfig.getString('imdaesomun_api_key');
      await _storage.write(key: 'imdaesomun_api_key', value: newApiKey);
      
      // Retry request
      final retryRequest = err.requestOptions;
      retryRequest.headers['x-imdaesomun-api-key'] = newApiKey;
      final response = await Dio().fetch(retryRequest);
      return handler.resolve(response);
    }
  }
}
```

**Android 매핑:**

```kotlin
// data/remote/ApiKeyInterceptor.kt
class ApiKeyInterceptor @Inject constructor(
    private val secureStorage: SecureStorage,
    private val remoteConfig: FirebaseRemoteConfig
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = secureStorage.getApiKey()

        val requestWithApiKey = originalRequest.newBuilder()
            .addHeader("x-imdaesomun-api-key", apiKey)
            .build()

        val response = chain.proceed(requestWithApiKey)

        if (response.code == 401) {
            response.close()

            // Refresh API key from Remote Config
            val newApiKey = refreshApiKey()
            secureStorage.saveApiKey(newApiKey)

            // Retry with new API key
            val retryRequest = originalRequest.newBuilder()
                .addHeader("x-imdaesomun-api-key", newApiKey)
                .build()

            return chain.proceed(retryRequest)
        }

        return response
    }

    private fun refreshApiKey(): String {
        remoteConfig.fetchAndActivate()
        return remoteConfig.getString("imdaesomun_api_key")
    }
}

// data/local/SecureStorage.kt
class SecureStorage @Inject constructor(
    private val encryptedPrefs: SharedPreferences
) {
    fun getApiKey(): String = encryptedPrefs.getString("imdaesomun_api_key", "") ?: ""
    fun saveApiKey(apiKey: String) =
        encryptedPrefs.edit().putString("imdaesomun_api_key", apiKey).apply()
}
```

</details>

---

## 5. 디렉토리 구조 예시 (Android)

### 5.1 전체 구조

```
app/
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/imdaesomun/android/
│   │   │   ├── ImdaesomunApplication.kt
│   │   │   ├── di/                           # Hilt 모듈
│   │   │   │   ├── DatabaseModule.kt         # Firestore, Remote Config 등
│   │   │   │   ├── NetworkModule.kt          # Retrofit, OkHttp, Interceptor
│   │   │   │   ├── RepositoryModule.kt       # Repository 의존성
│   │   │   │   └── StorageModule.kt          # DataStore, SecureStorage
│   │   │   ├── data/
│   │   │   │   ├── local/                    # 로컬 데이터 소스
│   │   │   │   │   ├── NoticeLocalDataSource.kt
│   │   │   │   │   ├── UserLocalDataSource.kt
│   │   │   │   │   ├── SecureStorage.kt
│   │   │   │   │   └── AppDatabase.kt        # Room (필요시)
│   │   │   │   ├── remote/                   # 원격 데이터 소스
│   │   │   │   │   ├── NoticeRemoteDataSource.kt
│   │   │   │   │   ├── UserRemoteDataSource.kt
│   │   │   │   │   ├── ApiKeyInterceptor.kt
│   │   │   │   │   └── api/
│   │   │   │   │       ├── NoticeApi.kt      # Retrofit 인터페이스
│   │   │   │   │       └── UserApi.kt
│   │   │   │   ├── repository/               # Repository 구현
│   │   │   │   │   ├── NoticeRepositoryImpl.kt
│   │   │   │   │   └── UserRepositoryImpl.kt
│   │   │   │   └── model/                    # 데이터 모델
│   │   │   │       ├── Notice.kt
│   │   │   │       ├── File.kt
│   │   │   │       ├── NoticePagination.kt
│   │   │   │       └── User.kt
│   │   │   ├── domain/                       # Domain Layer (Clean Architecture)
│   │   │   │   ├── model/                    # Domain 모델
│   │   │   │   ├── repository/               # Repository 인터페이스
│   │   │   │   │   ├── NoticeRepository.kt
│   │   │   │   │   └── UserRepository.kt
│   │   │   │   └── usecase/                  # Use Cases
│   │   │   │       ├── GetShNoticesUseCase.kt
│   │   │   │       ├── GetGhNoticesUseCase.kt
│   │   │   │       ├── SaveNoticeUseCase.kt
│   │   │   │       └── LoginUseCase.kt
│   │   │   ├── ui/
│   │   │   │   ├── screen/                   # 화면별 UI
│   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   │   └── component/
│   │   │   │   │   │       ├── NoticeCard.kt
│   │   │   │   │   │       ├── NoticeOrderControl.kt
│   │   │   │   │   │       └── RefreshButton.kt
│   │   │   │   │   ├── notice/
│   │   │   │   │   │   ├── NoticeDetailScreen.kt
│   │   │   │   │   │   ├── NoticeDetailViewModel.kt
│   │   │   │   │   │   └── component/
│   │   │   │   │   │       ├── NoticeContent.kt
│   │   │   │   │   │       ├── FileList.kt
│   │   │   │   │   │       └── SaveButton.kt
│   │   │   │   │   ├── saved/
│   │   │   │   │   │   ├── SavedScreen.kt
│   │   │   │   │   │   ├── SavedViewModel.kt
│   │   │   │   │   │   └── component/
│   │   │   │   │   │       ├── SavedNoticeList.kt
│   │   │   │   │   │       └── CorporationFilter.kt
│   │   │   │   │   ├── profile/
│   │   │   │   │   │   ├── ProfileScreen.kt
│   │   │   │   │   │   ├── ProfileViewModel.kt
│   │   │   │   │   │   └── component/
│   │   │   │   │   │       ├── UserInfo.kt
│   │   │   │   │   │       ├── PushSettings.kt
│   │   │   │   │   │       └── MenuItems.kt
│   │   │   │   │   └── webview/
│   │   │   │   │       └── WebViewScreen.kt
│   │   │   │   ├── component/                # 공통 컴포넌트
│   │   │   │   │   ├── common/
│   │   │   │   │   │   ├── AppButton.kt
│   │   │   │   │   │   ├── AppTextField.kt
│   │   │   │   │   │   ├── AppCard.kt
│   │   │   │   │   │   ├── LoadingIndicator.kt
│   │   │   │   │   │   ├── ErrorView.kt
│   │   │   │   │   │   └── EmptyView.kt
│   │   │   │   │   ├── dialog/
│   │   │   │   │   │   ├── LoginDialog.kt
│   │   │   │   │   │   ├── LogoutDialog.kt
│   │   │   │   │   │   └── DeleteAccountDialog.kt
│   │   │   │   │   └── navigation/
│   │   │   │   │       ├── BottomNavigationBar.kt
│   │   │   │   │       └── TopAppBar.kt
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── AppNavigation.kt
│   │   │   │   │   ├── Screen.kt
│   │   │   │   │   └── NavigationArgs.kt
│   │   │   │   └── theme/
│   │   │   │       ├── Theme.kt
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Typography.kt
│   │   │   │       └── Shapes.kt
│   │   │   ├── util/                         # 유틸리티
│   │   │   │   ├── extension/
│   │   │   │   │   ├── StringExt.kt
│   │   │   │   │   ├── DateExt.kt
│   │   │   │   │   └── ContextExt.kt
│   │   │   │   ├── Constants.kt
│   │   │   │   ├── UiState.kt
│   │   │   │   └── NetworkResult.kt
│   │   │   └── service/                      # 서비스
│   │   │       ├── FcmService.kt             # FCM 메시징 서비스
│   │   │       └── DownloadService.kt        # 파일 다운로드 서비스 (필요시)
│   │   └── res/
│   │       ├── values/
│   │       │   ├── colors.xml
│   │       │   ├── strings.xml
│   │       │   ├── themes.xml
│   │       │   └── dimens.xml
│   │       ├── drawable/                     # 아이콘 리소스
│   │       │   ├── ic_home.xml
│   │       │   ├── ic_bookmark.xml
│   │       │   ├── ic_profile.xml
│   │       │   └── ic_*.xml                  # Flutter assets/icons/*.svg 대응
│   │       ├── font/                         # Pretendard 폰트
│   │       │   ├── pretendard_bold.ttf
│   │       │   ├── pretendard_semibold.ttf
│   │       │   ├── pretendard_medium.ttf
│   │       │   └── pretendard_regular.ttf
│   │       └── mipmap/                       # 앱 아이콘
│   │           └── ic_launcher.png
│   └── test/
│       ├── java/com/imdaesomun/android/
│       │   ├── repository/
│       │   ├── usecase/
│       │   ├── viewmodel/
│       │   └── api/
│       └── androidTest/
│           └── java/com/imdaesomun/android/
│               ├── ui/
│               └── integration/
├── build.gradle.kts                          # 모듈 수준 빌드 스크립트
└── google-services.json                      # Firebase 설정 (기존과 동일)
```

### 5.2 Flutter와 Android 디렉토리 매핑

| Flutter 경로                     | Android 경로                                | 설명            |
|--------------------------------|-------------------------------------------|---------------|
| `lib/src/core/providers/`      | `di/`                                     | DI 모듈         |
| `lib/src/core/services/`       | `data/remote/` 또는 `util/`                 | 서비스 로직        |
| `lib/src/core/constants/`      | `util/Constants.kt`                       | 상수 관리         |
| `lib/src/core/enums/`          | `util/` (enum class)                      | 열거형 클래스       |
| `lib/src/core/router/`         | `ui/navigation/`                          | 네비게이션         |
| `lib/src/core/theme/`          | `ui/theme/`                               | 테마/스타일        |
| `lib/src/core/utils/`          | `util/extension/`                         | 유틸리티 함수       |
| `lib/src/data/models/`         | `data/model/`                             | 데이터 모델        |
| `lib/src/data/providers/`      | `di/` (Provider → @Provides)              | 의존성 제공        |
| `lib/src/data/repositories/`   | `data/repository/` + `domain/repository/` | Repository 패턴 |
| `lib/src/data/sources/local/`  | `data/local/`                             | 로컬 데이터 소스     |
| `lib/src/data/sources/remote/` | `data/remote/`                            | 원격 데이터 소스     |
| `lib/src/ui/components/`       | `ui/component/common/`                    | 공통 UI 컴포넌트    |
| `lib/src/ui/pages/home/`       | `ui/screen/home/`                         | 홈 화면          |
| `lib/src/ui/pages/notice/`     | `ui/screen/notice/`                       | 공고 상세 화면      |
| `lib/src/ui/pages/saved/`      | `ui/screen/saved/`                        | 저장된 공고 화면     |
| `lib/src/ui/pages/profile/`    | `ui/screen/profile/`                      | 프로필 화면        |
| `lib/src/ui/pages/webview/`    | `ui/screen/webview/`                      | 웹뷰 화면         |
| `lib/src/ui/widgets/`          | `ui/component/`                           | 재사용 위젯        |
| `assets/fonts/`                | `res/font/`                               | 폰트 리소스        |
| `assets/icons/`                | `res/drawable/`                           | 아이콘 리소스       |

---

## 6. 주요 패키지 및 의존성

### 6.1 build.gradle.kts (Module: app)

```kotlin
dependencies {
    // Jetpack Compose BOM
    implementation platform ("androidx.compose:compose-bom:2024.12.01")
    implementation "androidx.compose.ui:ui"
    implementation "androidx.compose.ui:ui-tooling-preview"
    implementation "androidx.compose.material3:material3"
    implementation "androidx.activity:activity-compose:1.9.3"
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"

    // Navigation Compose
    implementation "androidx.navigation:navigation-compose:2.8.5"

    // Hilt DI
    implementation "com.google.dagger:hilt-android:2.51.1"
    implementation "androidx.hilt:hilt-navigation-compose:1.2.0"
    kapt "com.google.dagger:hilt-compiler:2.51.1"

    // Firebase (기존 Flutter 프로젝트와 동일한 google-services.json 사용)
    implementation platform ("com.google.firebase:firebase-bom:33.7.0")
    implementation "com.google.firebase:firebase-auth-ktx"
    implementation "com.google.firebase:firebase-firestore-ktx"
    implementation "com.google.firebase:firebase-messaging-ktx"
    implementation "com.google.firebase:firebase-config-ktx"

    // Network
    implementation "com.squareup.retrofit2:retrofit:2.11.0"
    implementation "com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0"
    implementation "com.squareup.okhttp3:logging-interceptor:4.12.0"
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3"

    // Local Storage
    implementation "androidx.datastore:datastore-preferences:1.1.1"
    implementation "androidx.security:security-crypto:1.1.0-alpha06"

    // Image Loading
    implementation "io.coil-kt.coil3:coil-compose:3.0.4"
    implementation "io.coil-kt.coil3:coil-svg:3.0.4"

    // WebView
    implementation "androidx.webkit:webkit:1.12.1"
    implementation "com.google.accompanist:accompanist-webview:0.36.0"

    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0"

    // Testing
    testImplementation "junit:junit:4.13.2"
    testImplementation "org.mockito:mockito-core:5.14.2"
    testImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0"
    testImplementation "com.google.truth:truth:1.4.4"
    androidTestImplementation "androidx.test.ext:junit:1.2.1"
    androidTestImplementation "androidx.test.espresso:espresso-core:3.6.1"
    androidTestImplementation "androidx.compose.ui:ui-test-junit4"
    debugImplementation "androidx.compose.ui:ui-tooling"
    debugImplementation "androidx.compose.ui:ui-test-manifest"
}
```

### 6.2 주요 기능별 의존성 매핑

| Flutter 패키지                   | Android 의존성                          | 용도            |
|-------------------------------|--------------------------------------|---------------|
| `flutter_riverpod`            | `hilt-android` + `viewmodel-compose` | 상태관리 + DI     |
| `go_router`                   | `navigation-compose`                 | 네비게이션         |
| `dio`                         | `retrofit2` + `okhttp3`              | HTTP 클라이언트    |
| `freezed` + `json_annotation` | `kotlinx-serialization`              | 직렬화           |
| `shared_preferences`          | `datastore-preferences`              | 로컬 저장소        |
| `flutter_secure_storage`      | `security-crypto`                    | 보안 저장소        |
| `firebase_core`               | `firebase-bom`                       | Firebase 기본   |
| `firebase_auth`               | `firebase-auth-ktx`                  | Firebase 인증   |
| `firebase_messaging`          | `firebase-messaging-ktx`             | FCM 푸시        |
| `firebase_remote_config`      | `firebase-config-ktx`                | Remote Config |
| `flutter_svg`                 | `coil-svg`                           | SVG 이미지 로딩    |
| `webview_flutter`             | `accompanist-webview`                | 웹뷰            |
| `shimmer`                     | Custom Compose implementation        | 로딩 애니메이션      |
| `url_launcher`                | `Intent.ACTION_VIEW`                 | 외부 URL 열기     |
| `share_plus`                  | `Intent.ACTION_SEND`                 | 공유 기능         |
| `permission_handler`          | Native Android permissions           | 권한 관리         |
| `intl`                        | `java.text.*` 또는 `kotlinx-datetime`  | 날짜/시간 포맷      |

### 6.3 AndroidManifest.xml 주요 설정

```xml

<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 인터넷 권한 (API 호출) -->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- 푸시 알림 권한 (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <!-- 네트워크 상태 확인 -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application android:name=".ImdaesomunApplication" android:label="임대소문"
        android:icon="@mipmap/ic_launcher" android:theme="@style/Theme.Imdaesomun">

        <!-- Main Activity -->
        <activity android:name=".MainActivity" android:exported="true"
            android:theme="@style/Theme.Imdaesomun">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- FCM Service -->
        <service android:name=".service.FcmService" android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <!-- Firebase Messaging default settings -->
        <meta-data android:name="com.google.firebase.messaging.default_notification_icon"
            android:resource="@drawable/ic_notification" />
        <meta-data android:name="com.google.firebase.messaging.default_notification_color"
            android:resource="@color/primary" />
        <meta-data android:name="com.google.firebase.messaging.default_notification_channel_id"
            android:value="notice_channel" />
    </application>
</manifest>
```

---

## 7. 상세 구현 예시

<details>
<summary>## 7. 상세 구현 예시</summary>

### 7.1 UiState 패턴 (Flutter AsyncValue → Android UiState)

```kotlin
// util/UiState.kt
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}

// Extension functions for easy handling
inline fun <T> UiState<T>.onSuccess(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) action(data)
    return this
}

inline fun <T> UiState<T>.onError(action: (String, Throwable?) -> Unit): UiState<T> {
    if (this is UiState.Error) action(message, throwable)
    return this
}

inline fun <T> UiState<T>.onLoading(action: () -> Unit): UiState<T> {
    if (this is UiState.Loading) action()
    return this
}
```

### 7.2 Flutter AsyncNotifier → Android ViewModel 패턴

**Flutter:**

```dart
class ShNotices extends AsyncNotifier<List<Notice>> {
  @override
  Future<List<Notice>> build() async {
    final notices = await ref.read(noticeRepositoryProvider).getShNotices();
    return notices;
  }

  Future<void> getNotices({bool throttle = true}) async {
    // 스로틀링 로직
    state = const AsyncValue.loading();
    final notices = await ref.read(noticeRepositoryProvider).getShNotices();
    state = AsyncValue.data(notices);
  }
}
```

**Android:**

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getShNoticesUseCase: GetShNoticesUseCase,
    private val getGhNoticesUseCase: GetGhNoticesUseCase
) : ViewModel() {

    private val _shNotices = MutableStateFlow<UiState<List<Notice>>>(UiState.Idle)
    val shNotices: StateFlow<UiState<List<Notice>>> = _shNotices.asStateFlow()

    private val _ghNotices = MutableStateFlow<UiState<List<Notice>>>(UiState.Idle)
    val ghNotices: StateFlow<UiState<List<Notice>>> = _ghNotices.asStateFlow()

    // 스로틀링을 위한 Job 관리
    private var loadNoticesJob: Job? = null

    init {
        loadNotices()
    }

    fun loadNotices(forceRefresh: Boolean = false) {
        // 이전 Job 취소 (스로틀링)
        loadNoticesJob?.cancel()

        loadNoticesJob = viewModelScope.launch {
            if (!forceRefresh) {
                delay(300) // 스로틀링 딜레이
            }

            // SH 공고 로드
            _shNotices.value = UiState.Loading
            try {
                val shNotices = getShNoticesUseCase()
                _shNotices.value = UiState.Success(shNotices)
            } catch (e: Exception) {
                _shNotices.value = UiState.Error(e.message ?: "Failed to load SH notices", e)
            }

            // GH 공고 로드
            _ghNotices.value = UiState.Loading
            try {
                val ghNotices = getGhNoticesUseCase()
                _ghNotices.value = UiState.Success(ghNotices)
            } catch (e: Exception) {
                _ghNotices.value = UiState.Error(e.message ?: "Failed to load GH notices", e)
            }
        }
    }

    fun forceRefresh() = loadNotices(forceRefresh = true)
}
```

### 7.3 Compose UI 예시 (Flutter Widget → Android Composable)

**Flutter:**

```dart
class HomePage extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final shNoticesAsync = ref.watch(shNoticesProvider);
    final ghNoticesAsync = ref.watch(ghNoticesProvider);
    
    return Scaffold(
      body: RefreshIndicator(
        onRefresh: () async {
          ref.read(homePageViewModelProvider.notifier).forceRefresh();
        },
        child: shNoticesAsync.when(
          data: (notices) => NoticeList(notices: notices),
          loading: () => ShimmerLoading(),
          error: (error, _) => ErrorWidget(error: error),
        ),
      ),
    );
  }
}
```

**Android:**

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNoticeClick: (String) -> Unit
) {
    val shNotices by viewModel.shNotices.collectAsState()
    val ghNotices by viewModel.ghNotices.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = shNotices is UiState.Loading || ghNotices is UiState.Loading,
        onRefresh = { viewModel.forceRefresh() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // SH 공고 섹션
            item {
                Text(
                    text = "서울주택공사",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            when (shNotices) {
                is UiState.Loading -> {
                    items(3) { ShimmerNoticeCard() }
                }
                is UiState.Success -> {
                    items(shNotices.data) { notice ->
                        NoticeCard(
                            notice = notice,
                            onClick = { onNoticeClick(notice.id) }
                        )
                    }
                }
                is UiState.Error -> {
                    item {
                        ErrorCard(
                            message = shNotices.message,
                            onRetry = { viewModel.loadNotices() }
                        )
                    }
                }
                else -> {}
            }

            // GH 공고 섹션
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "경기주택공사",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            when (ghNotices) {
                is UiState.Loading -> {
                    items(3) { ShimmerNoticeCard() }
                }
                is UiState.Success -> {
                    items(ghNotices.data) { notice ->
                        NoticeCard(
                            notice = notice,
                            onClick = { onNoticeClick(notice.id) }
                        )
                    }
                }
                is UiState.Error -> {
                    item {
                        ErrorCard(
                            message = ghNotices.message,
                            onRetry = { viewModel.loadNotices() }
                        )
                    }
                }
                else -> {}
            }
        }

        PullRefreshIndicator(
            refreshing = shNotices is UiState.Loading || ghNotices is UiState.Loading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun NoticeCard(
    notice: Notice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notice.department,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                        .format(Date(notice.regDate * 1000L)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "조회수 ${notice.hits}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 7.4 Navigation 구현 (go_router → Navigation Compose)

**Flutter:**

```dart
final appRouter = GoRouter(
  routes: [
    StatefulShellRoute.indexedStack(
      builder: (context, state, navigationShell) => 
          BottomNav(navigationShell: navigationShell),
      branches: [
        StatefulShellBranch(routes: [
          GoRoute(path: '/home', builder: (context, state) => const HomePage()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/saved', builder: (context, state) => const SavedPage()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/profile', builder: (context, state) => const ProfilePage()),
        ]),
      ],
    ),
    GoRoute(
      path: '/notice/:id',
      builder: (context, state) => NoticePage(id: state.pathParameters['id']!),
    ),
  ],
);
```

**Android:**

```kotlin
// ui/navigation/Screen.kt
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Saved : Screen("saved")
    object Profile : Screen("profile")
    object NoticeDetail : Screen("notice/{noticeId}") {
        fun createRoute(noticeId: String) = "notice/$noticeId"
    }
    object WebView : Screen("webview/{url}") {
        fun createRoute(url: String) = "webview/${Uri.encode(url)}"
    }
}

// ui/navigation/AppNavigation.kt
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNoticeClick = { noticeId ->
                        navController.navigate(Screen.NoticeDetail.createRoute(noticeId))
                    }
                )
            }

            composable(Screen.Saved.route) {
                SavedScreen(
                    onNoticeClick = { noticeId ->
                        navController.navigate(Screen.NoticeDetail.createRoute(noticeId))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen()
            }

            composable(
                route = Screen.NoticeDetail.route,
                arguments = listOf(navArgument("noticeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val noticeId = backStackEntry.arguments?.getString("noticeId") ?: return@composable
                NoticeDetailScreen(
                    noticeId = noticeId,
                    onBackClick = { navController.popBackStack() },
                    onWebViewClick = { url ->
                        navController.navigate(Screen.WebView.createRoute(url))
                    }
                )
            }

            composable(
                route = Screen.WebView.route,
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: return@composable
                WebViewScreen(
                    url = Uri.decode(url),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
```

---

## 8. 실무 팁 및 고려사항

<details>
<summary>## 8. 실무 팁 및 고려사항</summary>

### 8.1 Flutter → Android 마이그레이션 시 주의사항

1. **상태 관리 패턴 변경**
    - Flutter의 `ref.watch()` → Android의 `collectAsState()`
    - `AsyncValue` → `UiState` 패턴으로 매핑
    - Provider 의존성 → Hilt 의존성 주입으로 변경

2. **생명주기 관리**
    - Flutter의 자동 메모리 관리 → Android의 명시적 생명주기 관리
    - `viewModelScope` 사용으로 메모리 누수 방지
    - Compose의 `remember`, `LaunchedEffect` 적절히 활용

3. **네트워킹 및 에러 처리**
    - Dio의 Interceptor → OkHttp의 Interceptor로 변경
    - Flutter의 `try-catch` → Kotlin의 `Result` 패턴 활용 권장
    - 네트워크 상태 체크 로직 구현

4. **UI 반응성**
    - Flutter의 Hot Reload → Compose Preview 활용
    - 상태 변경 시 recomposition 최적화
    - `derivedStateOf`, `remember` 적절한 사용

5. **Firebase 연동**
    - 동일한 `google-services.json` 파일 사용
    - Firebase SDK 초기화 코드 추가
    - Kotlin Coroutines 확장 함수 활용 (`-ktx` 라이브러리)

### 8.2 성능 최적화 팁

1. **Compose 최적화**
   ```kotlin
   // 불필요한 recomposition 방지
   @Stable
   data class NoticeUiState(...)
   
   // Key를 통한 효율적인 LazyList
   LazyColumn {
       items(notices, key = { it.id }) { notice ->
           NoticeCard(notice = notice)
       }
   }
   ```

2. **이미지 로딩 최적화**
   ```kotlin
   // Coil을 활용한 효율적인 이미지 로딩
   AsyncImage(
       model = ImageRequest.Builder(LocalContext.current)
           .data(imageUrl)
           .crossfade(true)
           .memoryCachePolicy(CachePolicy.ENABLED)
           .build(),
       contentDescription = null
   )
   ```

3. **데이터베이스 캐싱**
   ```kotlin
   // Room Database 활용 (필요시)
   @Entity(tableName = "notices")
   data class NoticeEntity(...)
   
   // 캐시 우선 로딩 전략
   override suspend fun getNotices(): Flow<List<Notice>> = flow {
       // 1. 로컬 캐시 먼저 방출
       val cached = localDataSource.getNotices()
       if (cached.isNotEmpty()) emit(cached)
       
       // 2. 원격 데이터 fetch 후 방출
       try {
           val remote = remoteDataSource.getNotices()
           localDataSource.saveNotices(remote)
           emit(remote)
       } catch (e: Exception) {
           if (cached.isEmpty()) throw e
       }
   }
   ```

### 8.3 테스트 전략

1. **Unit Testing**
   ```kotlin
   @ExperimentalCoroutinesTest
   class NoticeRepositoryTest {
       @Test
       fun `getShNotices returns cached data when available`() = runTest {
           // Given
           val cachedNotices = listOf(mockNotice)
           whenever(localDataSource.getShNotices()).thenReturn(cachedNotices)
           
           // When
           val result = repository.getShNotices()
           
           // Then
           assertThat(result).isEqualTo(cachedNotices)
       }
   }
   ```

2. **Compose UI Testing**
   ```kotlin
   @Test
   fun homeScreen_displayNotices_whenDataLoaded() {
       composeTestRule.setContent {
           HomeScreen(
               shNotices = UiState.Success(mockNotices),
               onNoticeClick = {}
           )
       }
       
       composeTestRule
           .onNodeWithText("서울주택공사")
           .assertIsDisplayed()
   }
   ```

</details>

---

## 9. 마이그레이션 체크리스트

### 9.1 개발 환경 설정

- [ ] Android Studio 설치 및 설정
- [ ] Kotlin, Compose 개발 환경 구성
- [ ] Firebase 프로젝트 연동 (`google-services.json`)
- [ ] 기존 Firebase Functions/Firestore 연결 확인

### 9.2 프로젝트 구조 설계

- [ ] Clean Architecture 디렉토리 구조 설계
- [ ] Hilt DI 모듈 설계 및 구현
- [ ] 데이터 모델 클래스 변환 (Freezed → Data class)
- [ ] Repository 패턴 구현
- [ ] UseCase 레이어 구현 (필요시)

### 9.3 핵심 기능 구현

- [ ] **임대공고 조회**: SH/GH 공고 리스트 화면
- [ ] **공고 상세**: 상세 내용, 첨부파일, 저장 기능
- [ ] **저장된 공고**: 사용자별 저장 공고 관리
- [ ] **사용자 관리**: 로그인/회원가입/프로필 관리
- [ ] **푸시 알림**: FCM 토큰 관리 및 알림 처리
- [ ] **설정**: 푸시 알림 설정, 정렬 기능

### 9.4 데이터 레이어

- [ ] NoticeRepository 구현 (로컬/원격 데이터 소스)
- [ ] UserRepository 구현 (인증, 푸시 토큰 관리)
- [ ] Retrofit API 인터페이스 정의
- [ ] DataStore/EncryptedSharedPreferences 구현
- [ ] API Key 자동 갱신 로직 (Interceptor)

### 9.5 UI 레이어

- [ ] Jetpack Compose 화면 구현
- [ ] ViewModel 상태 관리 구현
- [ ] Navigation Compose 라우팅 설정
- [ ] Material Design 3 테마 적용
- [ ] 반응형 UI 및 다양한 화면 크기 대응

### 9.6 보안 및 성능

- [ ] API Key 보안 저장 및 자동 갱신
- [ ] 네트워크 보안 (Certificate Pinning, 필요시)
- [ ] 이미지 로딩 최적화 (Coil)
- [ ] 메모리 누수 방지 (ViewModel scope)
- [ ] 오프라인 대응 (로컬 캐시)

### 9.7 테스트 및 배포

- [ ] Unit Test 작성 (Repository, ViewModel)
- [ ] UI Test 작성 (Compose Testing)
- [ ] Integration Test (Firebase 연동)
- [ ] 성능 테스트 및 최적화
- [ ] 앱 서명 및 Play Store 배포 준비

### 9.8 부가 기능

- [ ] 웹뷰 구현 (공고 원본 링크, 첨부파일)
- [ ] 공유 기능 (Intent.ACTION_SEND)
- [ ] 다크모드 지원
- [ ] 접근성 (Accessibility) 지원
- [ ] 앱 아이콘 및 스플래시 화면

---

## 10. 참고 자료 및 공식 문서

### 10.1 Android 개발 공식 문서

- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Navigation Compose**: https://developer.android.com/jetpack/compose/navigation
- **Hilt Dependency Injection
  **: https://developer.android.com/training/dependency-injection/hilt-android
- **ViewModel & StateFlow**: https://developer.android.com/topic/libraries/architecture/viewmodel
- **Material Design 3**: https://m3.material.io/

### 10.2 Firebase Android 문서

- **Firebase Android 설정**: https://firebase.google.com/docs/android/setup
- **Firebase Auth**: https://firebase.google.com/docs/auth/android/start
- **Cloud Firestore**: https://firebase.google.com/docs/firestore/quickstart#android
- **Firebase Messaging (FCM)**: https://firebase.google.com/docs/cloud-messaging/android/client
- **Remote Config**: https://firebase.google.com/docs/remote-config/use-config-android

### 10.3 주요 라이브러리 문서

- **Retrofit**: https://square.github.io/retrofit/
- **OkHttp**: https://square.github.io/okhttp/
- **Kotlinx Serialization**: https://github.com/Kotlin/kotlinx.serialization
- **DataStore**: https://developer.android.com/topic/libraries/architecture/datastore
- **Coil Image Loading**: https://coil-kt.github.io/coil/compose/

### 10.4 Flutter → Android 마이그레이션 참고

- **Android Clean Architecture**: https://github.com/android/architecture-samples
- **Compose Samples**: https://github.com/android/compose-samples
- **Firebase Android 샘플**: https://github.com/firebase/quickstart-android

### 10.5 코틀린 관련 자료

- **Kotlin Coroutines**: https://kotlinlang.org/docs/coroutines-overview.html
- **Kotlin Flow**: https://kotlinlang.org/docs/flow.html
- **Kotlin 스타일 가이드**: https://kotlinlang.org/docs/coding-conventions.html

---

> **📋 참고**: 본 마이그레이션 가이드는 실제 imdaesomun Flutter 프로젝트와 PRD를 기반으로 작성되었습니다. 실제 구현 시에는 프로젝트의 세부 요구사항과 팀의
> 개발 환경에 맞게 조정하여 사용하시기 바랍니다.