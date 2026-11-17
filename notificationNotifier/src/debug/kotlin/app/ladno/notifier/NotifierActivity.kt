package app.ladno.notifier

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class NotifierActivity : Activity() {
    private val notifications get() = getSystemService(NotificationManager::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(
                NotificationChannel(CHANNEL, "T02 fixtures", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addView(TextView(this@NotifierActivity).apply {
                    text = "Если Android запросит разрешение на отправку, выдайте его и нажмите кнопку ещё раз."
                })
            }
        }
        buttons.action("Отправить text") { post(1, fixture().setContentText("T02 text")) }
        buttons.action("Отправить bigText") {
            post(
                2,
                fixture().setContentText("T02 preview").setStyle(Notification.BigTextStyle().bigText("T02 expanded text")),
            )
        }
        buttons.action("Отправить textLines") {
            post(3, fixture().setStyle(Notification.InboxStyle().addLine("T02 line 1").addLine("T02 line 2")))
        }
        buttons.action("Создать уведомление для обновления") {
            post(4, fixture().setContentText("T02 update before"))
        }
        buttons.action("Обновить то же уведомление") {
            post(4, fixture().setContentText("T02 update after").setOnlyAlertOnce(true))
        }
        buttons.action("Отправить группу") {
            post(5, fixture().setGroup(GROUP).setContentText("T02 group child"))
            post(6, fixture().setGroup(GROUP).setGroupSummary(true).setContentText("T02 group summary"))
        }
        buttons.action("Отправить без текста") { post(7, fixture()) }
        buttons.action("Удалить тестовые уведомления") { notifications.cancelAll() }
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(buttons)
        })
    }

    private fun LinearLayout.action(label: String, run: () -> Unit) {
        addView(Button(this@NotifierActivity).apply {
            text = label
            setOnClickListener { run() }
        })
    }

    @Suppress("DEPRECATION")
    private fun fixture(): Notification.Builder {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL)
        } else {
            Notification.Builder(this)
        }
        return builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Ladno T02")
    }

    private fun post(id: Int, fixture: Notification.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }
        if (!notifications.areNotificationsEnabled() ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                notifications.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE)
        ) {
            Toast.makeText(this, "Разрешите уведомления Ladno Notifier в настройках", Toast.LENGTH_LONG).show()
            return
        }
        notifications.notify(id, fixture.build())
    }

    private companion object {
        const val CHANNEL = "t02-fixtures"
        const val GROUP = "t02-group"
    }
}
