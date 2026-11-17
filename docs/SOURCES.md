# Проверенные первичные источники

Дата исследования: 17.11.2026. Документация меняется; первая задача реализации повторно фиксирует совместимые версии. Архитектура, формулы бюджета, сроки, разделение сущностей — проектные предложения, а не цитаты из документаций.

| Тема | Что подтверждает источник | Источник |
|---|---|---|
| Android listener | Системный сервис, callbacks, lifecycle и активные уведомления | [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService) |
| Ограничения текста | Android 15 ограничивает unredacted OTP content для недоверенных listeners | [Behavior changes: all apps](https://developer.android.com/about/versions/15/behavior-changes-all) |
| iOS | Service extension включается в своё приложение и обрабатывает его remote notifications; не аналог глобального Android listener | [Apple: Modifying content in newly delivered notifications](https://developer.apple.com/documentation/usernotifications/modifying-content-in-newly-delivered-notifications) |
| Видимость пакетов | Точные queries и минимальная package visibility; не требуется по умолчанию QUERY_ALL_PACKAGES | [Declare package visibility needs](https://developer.android.com/training/package-visibility/declaring) |
| Фоновые задачи | WorkManager подчиняется ограничениям ОС; periodic минимум 15 минут | [Define work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) |
| Kotlin/Wasm | Возможности и ограничения платформ различаются | [Stability of supported platforms](https://kotlinlang.org/docs/multiplatform/supported-platforms.html) |
| Версии Compose | Отдельный цикл Compose, соответствие Jetpack и окружению | [Compatibility and versions](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html) |
| Material Expressive | Экспериментальная theme и отдельная версия Material3; версия CMP не определяет автоматически доступность API | [Compose 1.9 release notes](https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html) |
| Свежий ориентир CMP | На момент исследования документация содержит 1.12.2, шрифты веба и Hot Reload tooling | [Compose 1.12 release notes](https://kotlinlang.org/docs/multiplatform/whats-new-compose-112.html) |
| Room | Реальная поддержка платформ и различия APIs; не основание предполагать общий Wasm driver | [Set up Room for KMP](https://developer.android.com/kotlin/multiplatform/room) |
| Storage | Quota, eviction и best-effort persistence в браузере | [MDN: Storage quotas and eviction criteria](https://developer.mozilla.org/en-US/docs/Web/API/Storage_API/Storage_quotas_and_eviction_criteria) |
| M3 Expressive | Компоненты и подход Material 3 | [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) |
| Crypto library | Поддерживаемые AEAD/provider matrix | [cryptography-kotlin AEAD](https://whyoleg.github.io/cryptography-kotlin/primitives/operations/aead/) |
| KDF | HKDF для высокоэнтропийных ключей, отличие от password KDF | [cryptography-kotlin Key Derivation](https://whyoleg.github.io/cryptography-kotlin/primitives/operations/key-derivation/) |
| AES-GCM | Уникальность IV, 96-bit nonce, authenticated additionalData | [MDN AesGcmParams](https://developer.mozilla.org/en-US/docs/Web/API/AesGcmParams) |
| Android keys | Неэкспортируемые ключи, ограничения и авторизация использования | [Android Keystore](https://developer.android.com/privacy-and-security/keystore) |
