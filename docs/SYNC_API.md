# Offline-first и API синхронизации

## Локальный источник данных

UI никогда не ждёт HTTP для открытия операций, счетов, карт, иконок, правил, бюджета или графиков. Persisted финансовые payload шифруются, проекции для UI/поиска находятся в памяти. Команда пользователя изменяет локальный aggregate и outbox в одной транзакции. Локальная revision отличается от serverRevision; pending сохраняется после перезапуска. Реальная загрузка backend нужна только для activation, sync, admin и восстановления с сервера.

Android: Room transaction. Web: IndexedDB readwrite transaction по нужным object stores; completion подтверждается событием завершения транзакции, а не первым request success. Нельзя выполнять произвольные suspend/network операции внутри открытой IndexedDB транзакции. Blob иконки сохраняется вместе с локальной ссылкой/заданием upload либо staged перед финальным commit с последующей уборкой orphan assets.

Общие ports: TransactionRepository, AccountRepository, CardRepository, BankProfileRepository, IconAssetRepository, BudgetRepository, SourceConfigRepository, OutboxRepository, LocalUnitOfWork, SyncTransport, SessionStore, Clock, ConnectivityMonitor. Репозитории возвращают Flow с локальными результатами, не HTTP response.

## Агрегаты

Transaction со splits и связями evidence — единица финансовой записи. Transfer — одна transaction с двумя счетами; двух независимых расходов не создаётся. Для связи refund с expense проверяются права/валюта. Account/Card/BankProfile/Budget/Rule имеют свои revision, финансовые references проверяются клиентом после decrypt, сервер проверяет owner и metadata. Зависимости в outbox упорядочиваются: сначала банк/счёт, затем карта, затем операция; проверка полноты ссылок выполняется на клиенте. Если связанные records пришли на разных страницах pull, клиент помечает временную dependency и не объявляет повреждение данных до завершения загрузки снимка. После полного pull отсутствующая ссылка попадает в review/quarantine, а не превращается в расход с выдуманным счётом.

Настройки источника делятся на syncable profile/rule и device binding/enabled: включение listener на одном телефоне не включает его на другом. Веб может менять общий parser/filter profile, но не выдаёт себе Android notification access.

Asset upload — независимая очередь. Финансовый sync не ждёт изображения: entity содержит iconAssetId и fallback. Ссылка iconAssetId находится внутри encrypted record, поэтому сервер её не анализирует. Общий UI использует fallback, пока blob не загружен. После upload служебный asset-ready change с opaque assetId доводится до остальных клиентов. Очистку неиспользуемых assets инициирует клиент явными tombstones после проверки ссылок; сервер не может сам проверить encrypted ссылки. Автоматическую server GC по неизвестным ссылкам не запускать.

## API

Все пути версионированы `/api/v1`. OpenAPI создаётся в реализации на основании этого контракта, покрывается golden fixtures и не подменяется ручными несовместимыми DTO. Big integers в JSON — строки.

| Метод и путь | Назначение |
|---|---|
| POST `/activation/redeem` | Одноразовый код → user/device/session; отдельный idempotent activation protocol |
| POST `/session/refresh` | Rotation сессии; web cookie/Android secure token |
| POST `/session/logout` | Отзыв текущего сетевого сеанса |
| GET `/session/me` | userId, role, deviceId, accountEpoch, актуальные capability |
| POST `/sync/push` | Массив до 100 команд; результат каждой команды |
| GET `/sync/pull?cursor=...&limit=500` | Изменения пользователя после cursor; hasMore и nextCursor |
| GET `/sync/snapshot` | Начальная/восстановительная consistent snapshot с snapshotCursor |
| PUT `/assets/{assetId}` | Зашифрованный нормализованный image blob до 300 КиБ с envelope overhead, owner isolation |
| GET `/assets/{assetId}` | Загрузка приватного asset; кешировать локально |
| POST `/admin/invitations` | NEW_USER или ADD_DEVICE; только ADMIN |
| GET `/admin/invitations` | Метаданные кодов, без секретов |
| POST `/admin/invitations/{id}/revoke` | Отозвать ещё не использованный код |
| GET `/admin/users` | Список пользователей без финансовых данных |
| GET `/admin/users/{id}/devices` | Устройства пользователя |
| POST `/admin/devices/{id}/revoke` | Отозвать конкретное устройство |
| POST `/admin/users/{id}/revoke-sessions` | Отозвать все сетевые сессии пользователя |
| GET `/admin/audit` | Журнал доступа без сырого кода |
| POST `/me/delete` | Запрос удаления аккаунта, данные/outbox не должны воскреснуть |

Экспорт/импорт выполняются локально и доступны без backend. CSV/JSON открытый экспорт помечается как содержащий личные данные. Защищённая backup-копия — отдельный файл с authenticated encryption через проверенную библиотеку; секрет никогда не отправляется серверу.

### Envelope команды push

Команда содержит `commandId`, `entityType`, `entityId`, `baseRevision`, `operation`, а вместо открытого финансового DTO — `encryptedPayload`: `formatVersion`, `keyVersion`, `nonceBase64`, `ciphertextBase64`. JSON schema и golden vectors создаются в задаче crypto contracts. Для каждого нового шифрования nonce новый; при retry сохраняется точный первоначальный ciphertext. В `docs/ENCRYPTION.md` заданы алгоритмы, AAD и правила ключей. Суммы, cardId, accountId и заметки находятся внутри ciphertext, а не в query params или открытых серверных columns.

Дополнительные endpoints vault: GET/PUT `/vault/recovery-envelope`, GET `/vault/key-metadata`. PUT recovery envelope использует optimistic revision и право текущего пользователя. Админ не получает расшифрованный VaultKey/RecoverySecret. По сети передаются encrypted records и служебные метаданные; все локальные долговременные пользовательские данные входят в sync inventory.

## Серверная транзакция и cursor

Для каждого user сервер сериализует записи change log через per-user sequence row/lock. При успешном изменении в одной DB transaction: validate owner and metadata → check command receipt → check baseRevision → update aggregate → increment user sequence → append change log → store receipt. Это предотвращает out-of-order commit, при котором глобальная SQL sequence позволила бы пропустить поздно закоммиченный элемент. Чтение snapshot использует согласованный снимок сущностей и его cursor.

Результат команды: APPLIED, ALREADY_APPLIED, CONFLICT, VALIDATION_ERROR, ACCOUNT_REVOKED. Один commandId с другим payloadHash → IDEMPOTENCY_KEY_REUSED; нельзя тихо вернуть старый успех. Повтор неизменного commandId даёт прежний receipt. Retention receipts и tombstones согласуется с допустимым сроком offline; начальная бета хранит их до явной compaction/migration без автоматического удаления через пару дней.

В батче отдельные агрегатные команды могут завершиться независимо. Если нужна атомарная связанная операция между агрегатами, вводится явная серверная составная команда, а не предположение «весь batch atomic». Между страницами pull клиент сначала атомарно применяет страницу и сохраняет cursor, затем просит следующую. Cursor чужого пользователя не работает. Истёкший cursor даёт SNAPSHOT_REQUIRED; resnapshot никогда не стирает локальную outbox.

## Merge и конфликты

Нормальный путь: offline local edit → push → receipt revision → pull. Входящий server record нельзя накладывать поверх pending local projection без проверки. Локально хранить acknowledged base + pending commands; pull обновляет base, затем переигрываются совместимые локальные команды. Для пересекающихся правок одного aggregate показывается SyncConflict: локальная/серверная версия, поля различий, «оставить локальную», «принять серверную», «объединить вручную».

Решение конфликта создаёт новый commandId на актуальной serverRevision. Старый command сохраняет статус resolved, а не повторяется бесконечно. Delete vs edit всегда конфликт; отмена удаления — явная операция. Две правки категории не должны превращаться в два расхода.

## Веб offline и перезапуск

App shell, шрифты кириллицы, иконки темы и wasm/js assets кешируются service worker после первого успешного запуска. Использовать content-hashed версии, атомарную активацию нового кеша и совместимость со schema IndexedDB. API/код активации не кешировать как публичный resource. Deep links на offline поддерживаются navigation fallback. Запрос `navigator.storage.persist()` и quota estimation — best effort, не обещание отсутствия очистки.

При нескольких вкладках выбрать одного sync leader с lease/BroadcastChannel или проверить эквивалентный механизм; commandId и optimistic revisions остаются финальной защитой. Убить вкладку во время транзакции, открыть заново, проверить сохранность данных/очереди. При quota failure изменение не показывать сохранённым. Для аппаратного выключения невозможно гарантировать больше, чем гарантирует storage API; приложение предлагает экспорт и показывает количество несинхронизированных изменений.

## Эксплуатация

Один Kotlin/JVM backend и PostgreSQL, HTTPS и static web под общим origin. Логи — requestId/reason/status/duration, без body финансов/кодов/token. Миграции Flyway вперёд с проверенным backup перед опасными изменениями. Восстановление сервера сохраняет accountEpoch, device sessions, receipts и cursor; откат change log требует epoch reset/resnapshot с сохранением клиентских очередей.
