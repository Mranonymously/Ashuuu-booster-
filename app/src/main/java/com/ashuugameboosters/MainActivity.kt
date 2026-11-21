package com.ashuugameboosters

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private val bg = Color(0xFF100F0E)
private val panel = Color(0xFF211D1A)
private val panel2 = Color(0xFF30231D)
private val orange = Color(0xFFFF681F)
private val muted = Color(0xFFB6AAA3)
private val white = Color(0xFFF7F3F0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(12, 5, 8)
        window.navigationBarColor = android.graphics.Color.rgb(12, 5, 8)
        setContent { BoosterEntry(this) }
    }
}

@Composable
private fun BoosterEntry(context: Context) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(2600); showSplash = false }
    Box(Modifier.fillMaxSize()) {
        BoosterApp(context)
        if (showSplash) {
            val pulse = rememberInfiniteTransition(label = "logo-pulse")
            val scale by pulse.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "logo-scale")
            val glow by pulse.animateFloat(0.55f, 1f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "logo-glow")
            var progress by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) { while (progress < 100) { delay(22); progress = (progress + 1).coerceAtMost(100) } }
            Box(Modifier.fillMaxSize().background(Color(0xFF0C0508)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(150.dp).graphicsLayer { scaleX = scale; scaleY = scale; alpha = glow }.background(Color(0xFF260A0F), RoundedCornerShape(38.dp)), contentAlignment = Alignment.Center) {
                        Text("A", color = Color(0xFFFF252F), fontSize = 112.sp, fontWeight = FontWeight.Black)
                    }
                    Text("ASHUUUGAME", color = white, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("BOOSTERS", color = Color(0xFFFF303B), fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                    Text("PLAY SMOOTHER • GAME HARDER", color = muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(22.dp))
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.width(230.dp).height(6.dp), color = Color(0xFFFF303B), trackColor = panel2)
                    Text("INITIALIZING...  $progress%", color = muted, fontSize = 11.sp, letterSpacing = 2.sp)
                }
                Text("POWERED BY ASHUUUGAME", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp), color = Color(0xFF9A3540), fontSize = 10.sp, letterSpacing = 2.sp)
            }
        }
    }
}

private enum class Page(val label: String) { Profile("Profile"), Lobby("Lobby"), Labs("Game-Labs"), Settings("Settings") }
private data class GameApp(val name: String, val packageName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoosterApp(context: Context) {
    var page by remember { mutableStateOf(Page.Lobby) }
    var drawerOpen by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf("Balanced") }
    var seconds by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var dnsTuner by remember { mutableStateOf(false) }
    var sessionMonitor by remember { mutableStateOf(false) }
    var visualBoost by remember { mutableStateOf(false) }
    var fpsOverlay by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("All") }
    var energy by remember { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("ashuuu_profile", Context.MODE_PRIVATE) }
    val userId = remember {
        prefs.getString("user_id", null) ?: (1..8).map { "0123456789abcdef".random() }.joinToString("").also {
            prefs.edit().putString("user_id", it).apply()
        }
    }
    var games by remember { mutableStateOf(emptyList<GameApp>()) }
    var ramUsed by remember { mutableIntStateOf(0) }
    var ramTotal by remember { mutableIntStateOf(0) }
    var storageUsed by remember { mutableIntStateOf(0) }
    var storageTotal by remember { mutableIntStateOf(0) }
    var cpuGHz by remember { mutableStateOf("N/A") }
    var cpuMaxGHz by remember { mutableStateOf("N/A") }
    var batteryTemp by remember { mutableStateOf("N/A") }
    var networkName by remember { mutableStateOf("Unknown") }

    fun refreshStats() {
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            ramTotal = (mi.totalMem / 1_073_741_824L).toInt()
            ramUsed = (((mi.totalMem - mi.availMem).toDouble() / mi.totalMem.coerceAtLeast(1L)) * 100).toInt()
        }
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val used = total - stat.availableBytes
            storageTotal = (total / 1_073_741_824L).toInt()
            storageUsed = if (total > 0) ((used.toDouble() / total) * 100).toInt() else 0
        }
        runCatching {
            val current = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").readText().trim().toLong() / 1_000_000.0
            cpuGHz = String.format(Locale.US, "%.2f", current)
        }
        runCatching {
            val max = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").readText().trim().toLong() / 1_000_000.0
            cpuMaxGHz = String.format(Locale.US, "%.2f", max)
        }
        runCatching {
            val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val t = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1)
            batteryTemp = if (t > 0) String.format(Locale.US, "%.1f°C", t / 10f) else "N/A"
        }
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            @Suppress("DEPRECATION")
            networkName = if (cm.activeNetworkInfo?.isConnected == true) cm.activeNetworkInfo?.typeName ?: "Connected" else "Offline"
        }
    }

    fun loadGames() {
        games = runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
                .map { GameApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .sortedBy { it.name.lowercase() }
        }.getOrDefault(emptyList())
    }

    LaunchedEffect(Unit) {
        refreshStats()
        loadGames()
        while (true) { delay(5000); refreshStats() }
    }
    LaunchedEffect(running) { while (running) { delay(1000); seconds++ } }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    LaunchedEffect(drawerOpen) { if (drawerOpen) drawerState.open() else drawerState.close() }

    MaterialTheme(colorScheme = darkColorScheme(primary = orange, secondary = orange, background = bg, surface = panel)) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = panel, modifier = Modifier.width(300.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Text("ASHUUUGAME", modifier = Modifier.padding(horizontal = 22.dp), color = orange, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text("BOOSTERS • CONTROL CENTER", modifier = Modifier.padding(start = 22.dp, bottom = 24.dp), color = muted, fontSize = 10.sp)
                    Page.values().forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.label, fontWeight = FontWeight.SemiBold) },
                            selected = page == item,
                            onClick = { page = item; drawerOpen = false },
                            icon = { Icon(when(item) {
                                Page.Profile -> Icons.Default.Person
                                Page.Lobby -> Icons.Default.SportsEsports
                                Page.Labs -> Icons.Default.Science
                                Page.Settings -> Icons.Default.Settings
                            }, null) },
                            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = panel2, selectedTextColor = orange, selectedIconColor = orange, unselectedTextColor = white, unselectedIconColor = muted),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Version 1.1 • Local mode", modifier = Modifier.padding(22.dp), color = muted, fontSize = 11.sp)
                }
            }
        ) {
            Scaffold(
                containerColor = bg,
                topBar = {
                    Row(Modifier.fillMaxWidth().background(bg).padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { drawerOpen = true }) { Icon(Icons.Default.Menu, "Open menu", tint = orange) }
                        Column(Modifier.weight(1f)) {
                            Text(page.label, color = white, fontSize = 27.sp, fontWeight = FontWeight.Black)
                            if (page == Page.Lobby) Text("By ASHUUUGAME", color = muted, fontSize = 12.sp)
                        }
                        Text("ID  ${userId.take(8)}", color = muted, fontSize = 12.sp, modifier = Modifier.background(panel, RoundedCornerShape(30.dp)).padding(horizontal = 13.dp, vertical = 10.dp))
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = panel, contentColor = muted) {
                        listOf(Page.Profile, Page.Lobby, Page.Labs).forEach { p ->
                            NavigationBarItem(
                                selected = page == p, onClick = { page = p },
                                icon = { Icon(when(p) { Page.Profile -> Icons.Default.Person; Page.Lobby -> Icons.Default.SportsEsports; else -> Icons.Default.Science }, null) },
                                label = { Text(p.label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = orange, selectedTextColor = orange, indicatorColor = panel2)
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (page) {
                        Page.Profile -> ProfileScreen(userId, ramUsed, ramTotal, storageUsed, storageTotal, cpuGHz, cpuMaxGHz, batteryTemp, networkName) { page = Page.Settings }
                        Page.Lobby -> LobbyScreen(games, energy, { energy = (energy + 50).coerceAtMost(560) }, { page = Page.Labs }, { launchGame(context, it.packageName) }, { loadGames() })
                        Page.Labs -> LabsScreen(selectedTab, { selectedTab = it }, dnsTuner, { dnsTuner = it }, sessionMonitor, { sessionMonitor = it }, visualBoost, { visualBoost = it }, fpsOverlay, { fpsOverlay = it }, profile, { profile = it }, seconds, running, { running = !running })
                        Page.Settings -> SettingsScreen(profile, dnsTuner, sessionMonitor, visualBoost, fpsOverlay, userId)
                    }
                }
            }
        }
    }
}

private fun launchGame(context: Context, packageName: String) {
    runCatching { context.packageManager.getLaunchIntentForPackage(packageName)?.let { context.startActivity(it) } }
}

@Composable
private fun ProfileScreen(userId: String, ramUsed: Int, ramTotal: Int, storageUsed: Int, storageTotal: Int, cpuGHz: String, cpuMaxGHz: String, batteryTemp: String, networkName: String, onSettings: () -> Unit) {
    val score = (100 - ramUsed / 3 - storageUsed / 5).coerceIn(0, 100)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("V1.1.0", color = muted, modifier = Modifier.weight(1f)); IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings", tint = orange) } } }
        item {
            Panel {
                Text("●  FREE", color = orange, fontWeight = FontWeight.Bold)
                Text(userId.take(8), color = white, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Local device profile • No account connected", color = muted)
                Text("Membership and purchases are not enabled in this build.", color = muted, fontSize = 11.sp)
            }
        }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, null, tint = orange); Spacer(Modifier.width(10.dp))
                    Text("Privilege Service", color = white, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Not Activated", color = muted, fontSize = 11.sp)
                }
                HorizontalDivider(color = bg)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("Environment Level", color = muted); Text("0 / 3", color = orange, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                    Column(Modifier.weight(1f)) { Text("Environment Score", color = muted); Text("$score / 100", color = orange, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                }
                LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth(), color = orange, trackColor = panel2)
            }
        }
        item { Text("Real-time Monitoring", color = white, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Panel(Modifier.weight(1f).height(210.dp)) {
                    Icon(Icons.Default.Speed, null, tint = orange); Text("CPU Speed", color = muted)
                    Spacer(Modifier.height(18.dp)); Text("$cpuGHz GHz", color = orange, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Max $cpuMaxGHz GHz", color = muted, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Panel {
                        Icon(Icons.Default.Memory, null, tint = orange); Text("RAM Usage", color = muted)
                        Text("$ramUsed / 100", color = orange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Total $ramTotal GB", color = muted, fontSize = 12.sp)
                        LinearProgressIndicator(progress = { ramUsed / 100f }, color = orange, trackColor = panel2)
                    }
                    Panel {
                        Icon(Icons.Default.Storage, null, tint = orange); Text("Storage", color = muted)
                        Text("$storageUsed / 100", color = orange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Total $storageTotal GB", color = muted, fontSize = 12.sp)
                        LinearProgressIndicator(progress = { storageUsed / 100f }, color = orange, trackColor = panel2)
                    }
                }
            }
        }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, null, tint = orange); Spacer(Modifier.width(8.dp))
                    Text("Network", color = white, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(networkName, color = orange)
                }
                Text("Battery temperature: $batteryTemp", color = muted)
                Text("Network latency is not measured in this offline-safe build.", color = muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LobbyScreen(games: List<GameApp>, energy: Int, onAddEnergy: () -> Unit, onOpenLabs: () -> Unit, onLaunch: (GameApp) -> Unit, onRefresh: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Panel {
                Text("GAME", color = muted, fontSize = 12.sp)
                Text("CORNER", color = white, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("ASHUUUGAME BOOSTERS", color = muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Energy", color = muted); Text("$energy / 560", color = white, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = onAddEnergy) { Icon(Icons.Default.Add, null); Text("Demo") }
                }
                Text("Demo energy counter only; no rewards or purchases are connected.", color = muted, fontSize = 10.sp)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Game Library  ${games.size}", color = white, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Text("Refresh") }
            }
        }
        if (games.isEmpty()) {
            item {
                Panel(Modifier.fillMaxWidth().height(230.dp)) {
                    Icon(Icons.Default.SportsEsports, null, tint = orange, modifier = Modifier.size(44.dp))
                    Text("No launchable apps detected", color = white, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Install a game, then tap Refresh. Android only shows apps that expose a launcher activity.", color = muted)
                    Button(onClick = onRefresh) { Text("Scan installed apps") }
                }
            }
        } else {
            items(games, key = { it.packageName }) { game ->
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsEsports, null, tint = orange); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(game.name, color = white, fontWeight = FontWeight.Bold); Text(game.packageName, color = muted, fontSize = 10.sp) }
                        Button(onClick = { onLaunch(game) }) { Icon(Icons.Default.PlayArrow, null); Text("Play") }
                    }
                }
            }
        }
        item { Button(onClick = onOpenLabs, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Science, null); Text("Open Game-Labs") } }
    }
}

@Composable
private fun LabsScreen(tab: String, setTab: (String) -> Unit, dns: Boolean, setDns: (Boolean) -> Unit, monitor: Boolean, setMonitor: (Boolean) -> Unit, visual: Boolean, setVisual: (Boolean) -> Unit, fps: Boolean, setFps: (Boolean) -> Unit, profile: String, setProfile: (String) -> Unit, seconds: Int, running: Boolean, toggleTimer: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("LV.0", color = muted, modifier = Modifier.weight(1f)); Text(if (dns || monitor || visual || fps) "Features enabled" else "Inactive", color = orange) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Visual", "Configs").forEach { t -> FilterChip(selected = tab == t, onClick = { setTab(t) }, label = { Text(t) }) }
            }
        }
        item { Text("Feature Settings", color = white, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        if (tab == "All" || tab == "Configs") {
            item { SwitchRow("DNS & Latency Tuner", "Local toggle only; does not change Android DNS", Icons.Default.Wifi, dns, setDns) }
            item { SwitchRow("Session Monitor", "Track gaming session duration", Icons.Default.MonitorHeart, monitor, setMonitor) }
        }
        if (tab == "All" || tab == "Visual") {
            item { SwitchRow("Visual Boost", "Visual preference preset; no system GPU changes", Icons.Default.Visibility, visual, setVisual) }
            item { SwitchRow("FPS Overlay", "Overlay needs special Android permission and is not active", Icons.Default.Speed, fps, setFps) }
        }
        if (tab == "All" || tab == "Configs") {
            item {
                Panel {
                    Text("Performance Config", color = white, fontWeight = FontWeight.Bold)
                    listOf("Balanced", "Performance", "Battery Saver").forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = profile == p, onClick = { setProfile(p) }, colors = RadioButtonDefaults.colors(selectedColor = orange))
                            Text(p, color = white)
                        }
                    }
                    Text("Android manages CPU/GPU clocks and thermal limits.", color = muted, fontSize = 11.sp)
                }
            }
            item {
                Panel {
                    Text("Session Monitor", color = white, fontWeight = FontWeight.Bold)
                    Text("%02d:%02d".format(seconds / 60, seconds % 60), color = orange, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = toggleTimer) { Text(if (running) "Pause" else "Start") }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = orange, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, color = white, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(subtitle, color = muted, fontSize = 11.sp) }
            Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = white, checkedTrackColor = orange))
        }
    }
}

@Composable
private fun SettingsScreen(profile: String, dns: Boolean, monitor: Boolean, visual: Boolean, fps: Boolean, id: String) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Panel {
                Text("App Settings", color = white, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Current profile: $profile", color = muted)
                Text("Local device ID: $id", color = muted)
            }
        }
        item {
            Panel {
                Text("Feature status", color = white, fontWeight = FontWeight.Bold)
                Text("DNS tuner: ${if (dns) "On (local toggle)" else "Off"}", color = muted)
                Text("Session monitor: ${if (monitor) "On" else "Off"}", color = muted)
                Text("Visual boost: ${if (visual) "On (preset)" else "Off"}", color = muted)
                Text("FPS overlay: ${if (fps) "Selected (overlay unavailable)" else "Off"}", color = muted)
            }
        }
        item {
            Panel {
                Text("About ASHUUUGAME BOOSTERS", color = white, fontWeight = FontWeight.Bold)
                Text("Version 1.1.0 • Native Android • Offline-first", color = muted)
                Text("No root required. No fake FPS, CPU boost, RAM cleaning or DNS changes are performed.", color = muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
