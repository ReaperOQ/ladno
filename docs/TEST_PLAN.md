# План проверок

Формальные проценты покрытия не заменяют сценарии. Все тестовые операции/коды/банковские тексты синтетические. Сборки и команды фиксируются после bootstrap по реальному Gradle task tree, а не угадываются заранее.

## Критические проверки

| Область | Что проверять | Доказательство готовности |
|---|---|---|
| Money | Minor units, precision, split sum, overflow, JSON strings | Unit и property-based тесты на Kotlin targets |
| Карты/счета | Несколько карт на счёт, last4 collision, archive, смена привязки | Domain tests + локальный CRUD/restart |
| Иконки | Normalize/crop, EXIF stripping, invalid file, offline blob, fallback, upload retry | Unit/integration + реальные Android/web screens |
| Бюджеты | Day/week/month, reserve, обязательства, exact remainder, leap/DST/cross-month | Golden cases из BUDGET_RULES и property tests распределения |
| Классификация | Income/expense/refund/transfer/fee, card repayment | Domain fixtures; totals не меняются от выбора карты |
| Listener | Allowlist before text, access off, reconnect, summaries, fields | Android instrumentation + debug notifier на устройстве |
| Parser | Баланс в push не становится суммой расхода; unknown/ambiguous | Положительные/отрицательные fixtures с явной provenance |
| Дедупликация | Replay, update/key reuse, same amount purchases, manual edit | Engine tests + crash/reconnect integration |
| Локальная запись | Aggregate+outbox atomicity, failure, restart/migrations | Room integration и настоящие IndexedDB browser tests |
| Вход | Invite-only, once/expiry/revoke, lost response/race, role/owner | Backend integration с настоящим PostgreSQL/Testcontainers |
| Offline auth | Истёкший token не удаляет данные и не блокирует ввод | Android airplane mode и web offline reload |
| Crypto | Cross-target vectors, AAD/tag/key tampering, nonce reuse prevention, recovery | JVM/Android/Wasm tests + проверка второго устройства |
| Sync | Repeated command, body mismatch, cursor commit order, pending merge | Server tests + два клиента с network fault injection |
| Конфликты | Concurrent edits, delete/edit, resolution new command | End-to-end проверка сохранения обеих версий |
| PWA | Offline deep link/reload, fonts/assets, upgrade cache, storage failure | Реальные поддерживаемые браузеры |
| Приватность | Финансовый plaintext и ключи отсутствуют в server DB/logs/local persistent indices | Автопроверки fixtures + ручная инспекция тестовых БД |
| Admin | USER denied, код one-time view, offline actions disabled, отсутствие чужих финансов | RBAC integration + UI сценарий |
| Импорт/экспорт | Repeat import, errors preview, formula injection, namespace safety | CSV/JSON fixtures и backup round trip |
| Доступность | TalkBack, focus/keyboard, 200% text, reduce motion, chart alternatives | Реальные проверки с перечнем устройств и screenshots |
| Нагрузка | 10k/50k records, cold decrypt/index, query/chart latency, memory | Измерения с median/p95, environment и dataset |

## Один полный end-to-end сценарий

1. Оператор запускает backend/PostgreSQL и bootstrap admin без стандартных credentials.
2. Admin активируется, открывает отдельный admin screen, создаёт NEW_USER invitation.
3. Android вводит код online, создаёт vault, сохраняет recovery key. Пользователь создаёт банк/счёт и две карты одного счёта.
4. Listener принимает разрешённый synthetic notification, затем его повтор. Существует одна операция. Второй push с теми же merchant/amount, но другой покупкой не исчезает.
5. На Android выключается сеть. Пользователь меняет категорию, добавляет расход, иконку и budget cap. Приложение принудительно закрывается и запускается: всё сохранено, outbox на месте.
6. Месячный, недельный, дневной план и график показывают одинаковую логику; банковских балансов нет.
7. Сеть восстанавливается; ответ на первый push искусственно теряется после server commit. Retry не создаёт повторного расхода.
8. Admin создаёт ADD_DEVICE к этому user. Веб активируется, вводит recovery key и получает те же счета/карты/иконки/операции/план.
9. Веб offline deep link reload работает; на обоих устройствах независимо меняется одна операция. При sync виден конфликт, обе версии доступны, выбор пользователя создаёт одну согласованную запись.
10. Backend DB и логи не содержат суммы/merchant/recovery key в открытом виде. Assets зашифрованы.
11. На новом чистом браузере восстановление из серверной копии с recovery key даёт тот же набор данных. Отдельно проверяется локальный backup round trip.
12. USER не может вызвать admin API. Отзыв сессии блокирует следующий server request, но offline сохранённые данные не объявляются удалёнными.

## Целевые показатели, которые надо измерить

UI локальное сохранение: цель до 200 мс на обычной операции после инициализации проекции. Фильтр/график 10 тыс. записей: цель до 300 мс; 50 тыс. — до 1 с после индексации. Эти числа — продуктовые цели, не результаты проведённых измерений. Cold decrypt/index измеряется отдельно; для него показывается прогресс и не блокируется весь UI. Производительность мобильного браузера измерять отдельно от desktop.

Матрица устройств: актуальный Android на физическом устройстве и один поддерживаемый старший/минимальный API эмулятора после выбора minSdk; Chrome/Firefox desktop; Chromium Android. Safari/iOS веб проверяется отдельно до заявления поддержки. Отсутствие Mac/iPhone не выдавать за пройденную проверку. Поддержку WasmGC/доступности/IndexedDB/CryptoKey persistence фиксировать фактически.

## Отчёт

Для каждого requirement указывать PASS/FAIL/NOT_RUN, дату, commit, environment, команду/шаги, evidence и blocker. NOT_RUN не считается PASS. Не собирать единый процент из несопоставимых JVM/Android/browser coverage; раздельные результаты плюс один сводный функциональный отчёт.
