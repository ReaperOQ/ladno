package app.ladno.android

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import app.ladno.designsystem.LadnoTheme

class NotificationAccessActivity : ComponentActivity() {
    private var granted by mutableStateOf(false)
    private var settingsUnavailable by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LadnoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "Доступ к уведомлениям",
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.displaySmallEmphasized,
                            )
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = if (granted) "Доступ разрешён" else "Доступ выключен",
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                        style = MaterialTheme.typography.titleLargeEmphasized,
                                    )
                                    Text("Проверка Android API. Читается только текст синтетических уведомлений Ladno Notifier.")
                                }
                            }
                            Button(
                                onClick = ::openListenerSettings,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                shapes = ButtonDefaults.shapes(
                                    shape = MaterialTheme.shapes.large,
                                    pressedShape = MaterialTheme.shapes.small,
                                ),
                            ) { Text("Открыть настройки Android") }
                            if (settingsUnavailable) {
                                Text(
                                    text = "Этот экран настроек Android недоступен.",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(
                ComponentName(this, SpikeNotificationListener::class.java),
            )
        } else {
            packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
        }
    }

    private fun openListenerSettings() {
        settingsUnavailable = false
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            settingsUnavailable = true
        } catch (_: SecurityException) {
            settingsUnavailable = true
        }
    }
}
