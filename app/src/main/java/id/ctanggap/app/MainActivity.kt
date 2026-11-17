package id.ctanggap.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import id.ctanggap.app.data.AppSettings
import id.ctanggap.app.service.DrivingService
import id.ctanggap.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CTanggapTheme { AppRoot() } }
    }
}

private enum class Stage { SPLASH, PERMISSION, MAIN }

/** Layar yang dibuka dari dalam tab (di luar navigasi bawah). */
private sealed interface Sub {
    data object AddContact : Sub
    data class Detail(val incidentId: Long) : Sub
}

@Composable
private fun AppRoot(vm: AppViewModel = viewModel()) {
    val ctx = LocalContext.current
    val settings = remember { AppSettings(ctx) }
    var stage by remember { mutableStateOf(Stage.SPLASH) }
    when (stage) {
        // 1. Splash -> 2. Izin/onboarding (jika belum) -> aplikasi utama
        Stage.SPLASH -> SplashScreen {
            stage = if (settings.onboardingDone && Permissions.allOk(ctx)) Stage.MAIN else Stage.PERMISSION
        }
        Stage.PERMISSION -> PermissionScreen(onFinish = { settings.onboardingDone = true; stage = Stage.MAIN })
        Stage.MAIN -> MainShell(vm, openPermission = { stage = Stage.PERMISSION })
    }
}

@Composable
private fun MainShell(vm: AppViewModel, openPermission: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sub by remember { mutableStateOf<Sub?>(null) }
    BackHandler(enabled = sub != null) { sub = null }

    Scaffold(
        containerColor = AppBg,
        bottomBar = { if (sub == null) BottomBar(tab) { tab = it } }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (val s = sub) {
                Sub.AddContact -> AddContactScreen(vm) { sub = null }                       // 8. Tambah kontak
                is Sub.Detail -> IncidentDetailScreen(vm, s.incidentId) { sub = null }       // 12. Detail insiden
                null -> when (tab) {
                    0 -> HomeScreen(vm, openPermission)                                       // 3-4. Beranda aktif/nonaktif
                    1 -> ContactsScreen(vm) { sub = Sub.AddContact }                          // 7. Kontak
                    2 -> SettingsScreen()                                                      // 9-10. Pengaturan + dialog opt-in
                    else -> HistoryScreen(vm) { sub = Sub.Detail(it) }                        // 11. Riwayat
                }
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("Beranda", Icons.Filled.Home, 0), Triple("Kontak", Icons.Filled.Call, 1),
        Triple("Riwayat", Icons.Filled.History, 3), Triple("Atur", Icons.Filled.Settings, 2)
    )
    NavigationBar(containerColor = Red) {
        items.forEach { (label, icon, idx) ->
            NavigationBarItem(
                selected = selected == idx, onClick = { onSelect(idx) },
                icon = { Icon(icon, label) }, label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Red, selectedTextColor = Cream, indicatorColor = Cream,
                    unselectedIconColor = Color.White, unselectedTextColor = Color.White
                )
            )
        }
    }
}

fun sendToService(ctx: Context, action: String) =
    ContextCompat.startForegroundService(ctx, Intent(ctx, DrivingService::class.java).setAction(action))
