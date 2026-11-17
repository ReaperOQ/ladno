package app.ladno.notifier

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ladno.designsystem.LadnoTheme

class NotifierActivity : ComponentActivity() {
    private val notifications get() = getSystemService(NotificationManager::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(
                NotificationChannel(CHANNEL, "T02 fixtures", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        setContent { LadnoTheme { Samples() } }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun Samples() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Ladno Notifier",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.displaySmallEmphasized,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Text(
                            text = "Если Android запросит разрешение на отправку, выдайте его и нажмите кнопку ещё раз.",
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    SampleButton("Отправить text") { post(1, fixture().setContentText("T02 text")) }
                    SampleButton("Отправить bigText") {
                        post(
                            2,
                            fixture().setContentText("T02 preview")
                                .setStyle(Notification.BigTextStyle().bigText("T02 expanded text")),
                        )
                    }
                    SampleButton("Отправить textLines") {
                        post(3, fixture().setStyle(Notification.InboxStyle().addLine("T02 line 1").addLine("T02 line 2")))
                    }
                    SampleButton("Создать уведомление для обновления") {
                        post(4, fixture().setContentText("T02 update before"))
                    }
                    SampleButton("Обновить то же уведомление") {
                        post(4, fixture().setContentText("T02 update after").setOnlyAlertOnce(true))
                    }
                    SampleButton("Отправить группу") {
                        post(5, fixture().setGroup(GROUP).setContentText("T02 group child"))
                        post(6, fixture().setGroup(GROUP).setGroupSummary(true).setContentText("T02 group summary"))
                    }
                    SampleButton("Отправить без текста") { post(7, fixture()) }
                    TextButton(
                        onClick = notifications::cancelAll,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Удалить тестовые уведомления") }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun SampleButton(label: String, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shapes = ButtonDefaults.shapes(
                shape = MaterialTheme.shapes.medium,
                pressedShape = MaterialTheme.shapes.small,
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) { Text(label) }
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
