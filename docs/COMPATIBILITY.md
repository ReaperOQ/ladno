# Совместимость Gradle-сборки

| Компонент | Версия | Основание |
|---|---|---|
| JDK | 21 LTS | Общая версия для сборки и запуска сервера. [Требования Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) |
| Gradle Wrapper | 9.3.1 | Версия из матрицы AGP 9.1, дистрибутив проверяется по SHA-256. [AGP 9.1](https://developer.android.com/build/releases/agp-9-1-0-release-notes) |
| Kotlin и Compose Compiler | 2.4.21 | Общая версия плагинов. [Матрица Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html), [Compose Compiler](https://kotlinlang.org/docs/multiplatform/compose-compiler.html) |
| Android Gradle Plugin | 9.1.1 | Встроенный Kotlin в приложении, Android-KMP plugin в общих библиотеках. [Миграция на AGP 9](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html) |
| Android SDK | compile/target 37, min 24, Build Tools 36.0.0 | [Матрица AGP](https://developer.android.com/build/releases/agp-9-1-0-release-notes), [совместимость Compose](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html) |
| AndroidX Activity Compose | 1.12.4 | [Релизы Activity](https://developer.android.com/jetpack/androidx/releases/activity#1.12.4) |
| Compose Multiplatform | 1.12.2 | Android и `wasmJs`. [Релиз Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.2) |
| Compose Material 3 | 1.12.0-alpha03 | Версия из релиза Compose 1.12.2. Экспериментальный `MaterialExpressiveTheme` изолирован в `:shared:designsystem`. [Expressive API](https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-material-expressive-theme.html) |
| Spring Boot | 4.1.2 | Отдельный Kotlin/JVM модуль, серверные зависимости управляются BOM. [Требования Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) |
| Ktor Client | 3.6.1 | Для `wasmJs` опубликован движок `Js` на базе Fetch. [Движки Ktor Client](https://ktor.io/docs/client-engines.html) |
| cryptography-kotlin | 0.6.0 | Для `wasmJs` предусмотрен WebCrypto provider. Браузерный WebCrypto требует безопасного контекста (HTTPS или localhost). [Релиз библиотеки](https://github.com/whyoleg/cryptography-kotlin/releases/tag/0.6.0), [Web Crypto API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API) |

Версии плагинов и библиотек фиксирует [каталог](../gradle/libs.versions.toml). Отдельный `compileKotlinWasmJs` подтвердил компиляцию обращений к `HttpClient(Js)` и `CryptographyProvider.Default`.

T03 использует `cryptography-core` и `cryptography-random` 0.6.0, провайдеры `cryptography-provider-jdk` и `cryptography-provider-webcrypto` 0.6.0. Локальный wrapping использует Android Keystore (AES key generation через `KeyGenParameterSpec` с API 23) или WebCrypto с `extractable=false` и IndexedDB (версия схемы 1). Для Wasm interop и тестов используется kotlinx.coroutines 1.11.0, для проверок на устройстве — AndroidX Test Runner 1.7.0. [Провайдеры 0.6.0](https://github.com/whyoleg/cryptography-kotlin/tree/0.6.0/cryptography-providers) · [Android Keystore](https://developer.android.com/privacy-and-security/keystore) · [WebCrypto CryptoKey](https://www.w3.org/TR/webcrypto-2/#cryptokey-interface).

## Сборка и проверка

T04: `shared/domain` использует kotlinx.serialization JSON **1.11.0** с compiler plugin **2.4.21**, kotlinx.datetime **0.7.1** и существующий kotlinx.coroutines **1.11.0** для Flow-портов. Версии фиксированы в каталоге. Domain не зависит от Android APIs, Room, Compose или Spring. [Настройка serialization](https://kotlinlang.org/docs/serialization-get-started.html), [строки JsonPrimitive](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive/), [datetime 0.7.1 и kotlin.time.Instant](https://github.com/Kotlin/kotlinx-datetime/releases/tag/v0.7.1). JVM-компиляция с сериализаторами и компиляция общего кода/тестов в Wasm klib проверены Kotlin CLI 2.4.21.

Для первого запуска нужны доступ к сервисам Gradle, Google Maven, Maven Central, Node.js и npm. Gradle загружает инструменты для сборки Wasm автоматически. Для запуска веб-приложения нужен браузер с WasmGC.

### Windows PowerShell

Установите JDK 21 и Android SDK Command-line Tools через Android Studio SDK Manager. Укажите локальный путь к JDK.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$sdkmanager = "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat"
& $sdkmanager --licenses
& $sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0' 'platform-tools'

.\gradlew.bat :androidApp:assembleDebug :webApp:wasmJsBrowserDistribution :server:build
```

Запустите backend через `& "$env:JAVA_HOME\bin\java.exe" -jar server/build/libs/ladno-server.jar`. В другом окне выполните `curl.exe -fsS http://localhost:8080/health`.

### Ubuntu

Установите JDK 21 и распакуйте Android SDK Command-line Tools в `$HOME/Android/Sdk/cmdline-tools/latest`. Ниже указан типовой путь к JDK для amd64.

```bash
sudo apt install openjdk-21-jdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME="$HOME/Android/Sdk"
sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
"$sdkmanager" --licenses
"$sdkmanager" 'platforms;android-37.0' 'build-tools;36.0.0' 'platform-tools'

./gradlew :androidApp:assembleDebug :webApp:wasmJsBrowserDistribution :server:build
```

Запустите backend через `"$JAVA_HOME/bin/java" -jar server/build/libs/ladno-server.jar`. В другом терминале выполните `curl -fsS http://localhost:8080/health`.

### Артефакты

| Задача | Артефакт |
|---|---|
| `:androidApp:assembleDebug` | `androidApp/build/outputs/apk/debug/androidApp-debug.apk` |
| `:notificationNotifier:assembleDebug` | `notificationNotifier/build/outputs/apk/debug/notificationNotifier-debug.apk` |
| `:webApp:wasmJsBrowserDistribution` | `webApp/build/dist/wasmJs/productionExecutable/` |
| `:server:build` | `server/build/libs/ladno-server.jar` (задача `:server:bootJar`) |

На подключённом устройстве или эмуляторе установите APK через `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`. Раздайте production distribution по HTTP командой `python -m http.server 8000 --directory webApp/build/dist/wasmJs/productionExecutable` (на Ubuntu `python3`) и откройте `http://localhost:8000/`.

Ожидаемый ответ `GET /health` — HTTP 200 и JSON `{"status":"UP"}`. Запуск через Gradle доступен задачей `:server:bootRun`. Выполненные проверки перечислены в [статусе](STATUS.md).

### Android API для T02

| API | Доступность |
|---|---|
| `NotificationListenerService`, `onListenerConnected` и `getActiveNotifications()` | API 18, callback подключения с API 21. [Listener API](https://developer.android.com/reference/android/service/notification/NotificationListenerService) |
| `EXTRA_TEXT`, `EXTRA_BIG_TEXT`, `EXTRA_TEXT_LINES` и `FLAG_GROUP_SUMMARY` | Поля с API 19, флаг с API 20. [Notification API](https://developer.android.com/reference/android/app/Notification) |
| Экран выдачи доступа и проверка разрешения | `ACTION_NOTIFICATION_LISTENER_SETTINGS` с API 22, `isNotificationListenerAccessGranted` с API 27. На API 24–26 используется проверка пакета через `NotificationManagerCompat` для единственного listener в APK. В debug APK разрешилась транзитивная AndroidX Core 1.16.0. [Settings](https://developer.android.com/reference/android/provider/Settings#ACTION_NOTIFICATION_LISTENER_SETTINGS) · [NotificationManager](https://developer.android.com/reference/android/app/NotificationManager#isNotificationListenerAccessGranted(android.content.ComponentName)) · [AndroidX Core](https://developer.android.com/reference/androidx/core/app/NotificationManagerCompat#getEnabledListenerPackages(android.content.Context)) |
| Канал и разрешение на отправку из notifier | Канал с API 26, `POST_NOTIFICATIONS` с API 33. [Каналы](https://developer.android.com/reference/android/app/NotificationChannel) · [Разрешение отправки](https://developer.android.com/develop/ui/compose/notifications/notification-permission) |

Compose Multiplatform 1.12.2 и Material 3 1.12.0-alpha03 поддерживают акцентные стили `Typography.*Emphasized` и кнопки с разной формой в обычном и нажатом состояниях. Оба Android-экрана T02 используют эти API. Общий экран приглашения использует ту же проектную палитру, формы и акцентную типографику на Android и Wasm. [Typography](https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-typography/) · [ButtonDefaults](https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-button-defaults/)
