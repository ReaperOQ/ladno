# Статус реализации

## T01 · Каркас приложения

Готово. Одна Gradle-сборка с wrapper объединяет Android, Kotlin/Wasm и Spring Boot. Android и веб запускают общий экран с Material 3 Expressive. Сервер предоставляет `GET /health`. Версии, пути артефактов и команды для обеих ОС приведены в [документе о совместимости](COMPATIBILITY.md).

| Проверка | Результат |
|---|---|
| `gradlew.bat :androidApp:assembleDebug` | Windows, JDK 21. APK собран, подпись и launcher Activity проверены инструментами SDK |
| `gradlew.bat :webApp:wasmJsBrowserDistribution` | Windows, JDK 21. Собраны `index.html`, JS-обвязка и Wasm. В Chrome проверены заголовок и текст приглашения через дерево доступности при 390 × 760 (светлая тема) и 1280 × 800 (тёмная тема) |
| `gradlew.bat :server:build` | Windows, JDK 21. JAR запущен, `GET /health` вернул HTTP 200 и `{"status":"UP"}` |
| `compileKotlinWasmJs` в отдельном проверочном проекте | Ktor и cryptography-kotlin разрешились из каталога, обращения к их API скомпилировались |
| `sh -n gradlew` | Синтаксис wrapper проверен в Ubuntu через WSL |

На Ubuntu полный набор задач не запускался. Android APK не запускался на устройстве или эмуляторе, список `adb devices` был пуст. Webpack предупреждает о размере JS и Wasm (около 10,5 МиБ суммарно до HTTP-сжатия).

## T02 · Android NotificationListenerService

Реализованы debug listener, экран системного доступа **Ладно T02** и отдельный debug APK **Ladno Notifier**. Listener сверяет точный пакет до чтения extras, пропускает group summary и проверяет активные карточки после подключения. Диагностика ограничена фиксированными кодами в Logcat. Прогоны на реальных банковских уведомлениях отсутствуют.

| Проверка | Результат |
|---|---|
| `gradlew.bat --offline :androidApp:assembleDebug :notificationNotifier:assembleDebug :androidApp:assembleRelease -x :androidApp:lintVitalRelease` | Оба debug APK и release APK собраны на Windows, JDK 21. Обычная offline-сборка release остановилась на зависимостях AndroidX Activity 1.8.2 для lint, которых нет в кэше |
| `aapt dump xmltree` и проверка строк DEX в обоих APK Ладно и APK notifier | Listener, экран доступа и тестовый allowlist присутствуют только в debug APK Ладно. Notifier использует собственный пакет. Разрешения `INTERNET` в обоих debug APK нет |
| `gradlew.bat --offline :notificationNotifier:tasks --all` | Для notifier зарегистрированы задачи упаковки только debug-варианта |
| Устройство или эмулятор | **Не выполнено.** `adb devices` вернул пустой список, AVD и готовых образов нет. Проверки `text`, `bigText`, `textLines`, обновления, group summary, чужого пакета, переподключения и отключённого доступа описаны в [ручном сценарии T02](NOTIFICATION_SPIKE.md) |

**Далее T03** — проверить шифрование Android/JVM ↔ Wasm. Ручной прогон T02 остаётся открытым.
