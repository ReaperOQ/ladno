# T02 · Android NotificationListenerService

Экран **Ладно T02** и listener входят только в debug APK Ладно. Отдельный APK **Ladno Notifier** имеет пакет `app.ladno.notifier` и собирается только в debug-варианте. Debug listener проверяет точное равенство имени пакета до обращения к `notification` и `extras`, пропускает group summary по флагу и сравнивает три поля с короткими синтетическими образцами. В Logcat под тегом `LadnoT02` выводятся фиксированные коды без текста, package name, notification key и данных исключения. Сетевых запросов и записи в хранилище приложения в spike нет.

## Ручная проверка на устройстве

**Статус: не выполнена.** `adb devices` вернул пустой список, настроенных AVD и готовых системных образов не найдено.

1. На тестовом устройстве или AVD, предпочтительно с API 33+, установите оба APK. Выполните команды из корня репозитория. На Windows используйте `./gradlew.bat`.

   ```sh
   ./gradlew :androidApp:assembleDebug :notificationNotifier:assembleDebug
   adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
   adb install -r notificationNotifier/build/outputs/apk/debug/notificationNotifier-debug.apk
   adb logcat -v brief -s LadnoT02:I
   ```

2. Откройте **Ладно T02**. Если доступ был выдан раньше, отзовите его через **Открыть настройки Android**. После возврата статус должен стать **Доступ выключен**. В **Ladno Notifier** нажмите **Удалить тестовые уведомления**, затем **Отправить text**. На Android 13+ при первом запросе разрешите отправку уведомлений и нажмите кнопку снова. Убедитесь, что карточка появилась в шторке, а `posted text_match` в Logcat не появилось. После этого удалите карточку кнопкой notifier.
3. В **Ладно T02** откройте системные настройки, включите доступ для listener **Ладно T02** и вернитесь на экран. Ожидаются статус **Доступ разрешён** и запись `connected`. Дальше оставьте экран Ладно в фоне и выполняйте действия в notifier по одному.

   | Действие | Ожидаемый результат |
   |---|---|
   | **Отправить text** | `posted text_match` |
   | **Отправить bigText** | `posted bigText_match` при наличии отдельного краткого `text` |
   | **Отправить textLines** | `posted textLines_match`, обе строки в исходном порядке |
   | **Создать уведомление для обновления**, дождаться записи, затем **Обновить то же уведомление** | Сначала `posted update_before_match`, потом `posted update_after_match`. В шторке та же карточка с обновлённым текстом |
   | **Отправить группу** | `posted group_child_match` и `posted group_summary_skipped` в любом порядке |
   | **Отправить без текста** | `posted redacted_or_empty`. Этот образец проверяет отсутствие текста |

4. При включённом доступе выполните `adb shell 'cmd notification post t02-rejected "T02 text"'`. Когда карточка появится в шторке, ожидается `posted package_rejected` без нового `posted text_match`. У карточки тот же синтетический текст, а Android отправляет её от пакета вызывающей shell-команды. Смахните эту тестовую карточку. Если команда `post` недоступна в данной прошивке, отметьте проверку как невыполненную. [Исходник Android](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/services/core/java/com/android/server/notification/NotificationShellCmd.java)
5. Проверьте переподключение. Удалите все карточки notifier, выключите доступ в настройках и вернитесь на экран Ладно. Ожидается **Доступ выключен**. Во время отключения отправьте **text**, затем удалите его. Отправьте **bigText** и оставьте его в шторке. В этот период новых записей `posted text_match` и `posted bigText_match` быть не должно. Включите доступ снова. Ожидаются **Доступ разрешён**, `connected` и `active bigText_match`. Для удалённой карточки `active text_match` отсутствует. Повторите **Отправить text** и проверьте, что появился новый `posted text_match`.
6. Повторите экран разрешения с крупным системным шрифтом и в тёмной теме. Проверьте, что кнопка доступна при прокрутке и озвучивается TalkBack. В конце отзовите доступ и удалите синтетические карточки. Сохраните только версию Android (API), состояние каждой проверки и фиксированные коды. Снимки шторки и полный Logcat для отчёта не нужны.

## Ограничения

- После подключения Android предоставляет снимок активных видимых карточек. История уже снятых карточек недоступна. `active` и `posted` могут относиться к одной карточке. Диагностика фиксирует доставку, а дедупликация относится к будущему импорту. [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService#getActiveNotifications())
- Android может ограничивать listener в рабочих профилях и на low-RAM устройствах с Android 10 и ниже. Начиная с Android 15 система может скрывать чувствительный текст у недоверенных listener. Пустой синтетический образец проверяет ветку `redacted_or_empty`, системное скрытие на устройстве остаётся непроверенным. [Ограничения listener](https://developer.android.com/reference/android/service/notification/NotificationListenerService) · [Защита чувствительных уведомлений в Android 15](https://developer.android.com/about/versions/15/behavior-changes-all)
- Используются только синтетические образцы. Доставка и форматы реальных банков остаются непроверенными.
