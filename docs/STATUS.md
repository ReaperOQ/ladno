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

**Далее T02** — Android NotificationListenerService и проверка синтетического уведомления с точным allowlist пакетов.
