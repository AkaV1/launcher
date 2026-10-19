package com.example.switchlauncher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class Palette(
    val bg: Color,
    val tile: Color,
    val text: Color,
    val subtext: Color,
    val bar: Color,
)

private val LightPalette = Palette(
    bg = Color(0xFFEBEBEB), tile = Color.White, text = Color(0xFF2D2D2D),
    subtext = Color(0xFF7A7A7A), bar = Color(0xFFDADADA),
)
private val DarkPalette = Palette(
    bg = Color(0xFF2D2D2D), tile = Color(0xFF3C3C3C), text = Color(0xFFF2F2F2),
    subtext = Color(0xFFA0A0A0), bar = Color(0xFF1F1F1F),
)
private val SwitchRed = Color(0xFFE60012)
private val SwitchBlue = Color(0xFF00C3E3)

private data class StatusInfo(val time: String, val battery: Int)

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("launcher", Context.MODE_PRIVATE) }
    val repo = remember { GameRepository(context) }

    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var showAll by remember { mutableStateOf(prefs.getBoolean("showAll", false)) }
    var favorites by remember {
        mutableStateOf(prefs.getStringSet("favorites", emptySet()).orEmpty().toSet())
    }
    var refreshKey by remember { mutableIntStateOf(0) }
    var apps by remember { mutableStateOf(emptyList<GameApp>()) }
    var selected by remember { mutableIntStateOf(0) }
    var pendingPackage by remember { mutableStateOf<String?>(null) }
    val palette = if (dark) DarkPalette else LightPalette

    // Un launcher no debe cerrarse con "atrás"
    BackHandler {}

    // Refrescar la lista al instalar/desinstalar/actualizar apps
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                refreshKey++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(showAll, refreshKey) { apps = repo.load(onlyGames = !showAll) }

    // Favoritos primero, luego orden alfabético
    val games = remember(apps, favorites) {
        apps.sortedWith(
            compareByDescending<GameApp> { it.packageName in favorites }
                .thenBy { it.label.lowercase() }
        )
    }
    val index = selected.coerceIn(0, maxOf(games.lastIndex, 0))
    val current = games.getOrNull(index)

    // Mantener la selección sobre el mismo juego tras marcar/desmarcar favorito
    LaunchedEffect(games) {
        pendingPackage?.let { pkg ->
            val i = games.indexOfFirst { it.packageName == pkg }
            if (i >= 0) selected = i
            pendingPackage = null
        }
    }

    // Reloj y batería
    val status by produceState(initialValue = StatusInfo("", -1)) {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            val bi = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            value = StatusInfo(
                time = fmt.format(Date()),
                battery = if (level >= 0 && scale > 0) level * 100 / scale else -1,
            )
            delay(15_000)
        }
    }

    // Iconos de la barra de estado legibles según el tema
    val activity = context as? Activity
    SideEffect {
        activity?.window?.let { w ->
            val controller = WindowCompat.getInsetsController(w, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    // Borde pulsante del mosaico seleccionado
    val pulse by rememberInfiniteTransition(label = "pulse").animateColor(
        initialValue = SwitchRed,
        targetValue = SwitchBlue,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseColor",
    )

    // ---------- Acciones ----------

    fun select(newIndex: Int) {
        if (games.isEmpty()) return
        val target = newIndex.coerceIn(0, games.lastIndex)
        if (target != index) {
            view.playSoundEffect(
                if (target > index) SoundEffectConstants.NAVIGATION_RIGHT else SoundEffectConstants.NAVIGATION_LEFT
            )
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            selected = target
        }
    }

    fun launch(app: GameApp) {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        try {
            context.startActivity(app.intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleFavorite(app: GameApp) {
        val updated = if (app.packageName in favorites) favorites - app.packageName else favorites + app.packageName
        prefs.edit().putStringSet("favorites", updated).apply()
        pendingPackage = app.packageName
        favorites = updated
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun toggleFilter() {
        showAll = !showAll
        prefs.edit().putBoolean("showAll", showAll).apply()
        selected = 0
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    fun toggleTheme() {
        dark = !dark
        prefs.edit().putBoolean("dark", dark).apply()
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    // ---------- Control de mando ----------

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val onKey: (KeyEvent) -> Boolean = { ev ->
        if (ev.type != KeyEventType.KeyDown) {
            false
        } else {
            when (ev.key) {
                Key.DirectionLeft -> { select(index - 1); true }
                Key.DirectionRight -> { select(index + 1); true }
                Key.DirectionUp, Key.DirectionDown -> true
                Key.ButtonL1 -> { select(index - 5); true }
                Key.ButtonR1 -> { select(index + 5); true }
                Key.ButtonA, Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                    current?.let { launch(it) }; true
                }
                Key.ButtonY -> { current?.let { toggleFavorite(it) }; true }
                Key.ButtonX -> { toggleFilter(); true }
                Key.ButtonStart, Key.ButtonSelect -> { toggleTheme(); true }
                Key.ButtonB -> true
                else -> false
            }
        }
    }

    // ---------- UI ----------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent(onKey)
            .focusable()
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(status, palette)

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (current == null) {
                    Text(
                        text = if (showAll) "No hay apps para mostrar"
                        else "No se encontraron juegos.\nToca \"Todas las apps\" para ver todo.",
                        color = palette.subtext,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(32.dp),
                    )
                } else {
                    val listState = rememberLazyListState()
                    val sidePadding = (LocalConfiguration.current.screenWidthDp / 2 - 75).coerceAtLeast(16).dp

                    // Centrar el mosaico seleccionado con animación
                    LaunchedEffect(index, games.size) { listState.animateScrollToItem(index) }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LazyRow(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = sidePadding),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(230.dp),
                        ) {
                            itemsIndexed(games, key = { _, g -> g.packageName }) { i, game ->
                                GameTile(
                                    game = game,
                                    selected = i == index,
                                    favorite = game.packageName in favorites,
                                    palette = palette,
                                    pulse = pulse,
                                    onTap = { if (i == index) launch(game) else select(i) },
                                    onLongPress = { toggleFavorite(game) },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = current.label,
                            color = palette.text,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Text(
                            text = when {
                                current.packageName in favorites -> "★ Favorito"
                                showAll -> "App"
                                else -> "Juego"
                            },
                            color = palette.subtext,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            BottomBar(
                showAll = showAll,
                palette = palette,
                onToggleFilter = { toggleFilter() },
                onToggleTheme = { toggleTheme() },
            )
        }
    }
}

@Composable
private fun TopBar(status: StatusInfo, palette: Palette) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(SwitchRed),
            contentAlignment = Alignment.Center,
        ) { Text("🎮", fontSize = 20.sp) }
        Spacer(Modifier.weight(1f))
        Text(status.time, color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        if (status.battery >= 0) {
            Spacer(Modifier.width(14.dp))
            Text("🔋 ${status.battery}%", color = palette.text, fontSize = 16.sp)
        }
    }
}

@Composable
private fun BottomBar(
    showAll: Boolean,
    palette: Palette,
    onToggleFilter: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(if (showAll) "Solo juegos" else "Todas las apps", palette, onToggleFilter)
            Pill("Tema", palette, onToggleTheme)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "A Abrir   ·   Y Favorito   ·   X Filtro   ·   + Tema   ·   L/R Saltar",
            color = palette.subtext,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Pill(label: String, palette: Palette, onClick: () -> Unit) {
    val click by rememberUpdatedState(onClick)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.bar)
            .pointerInput(Unit) { detectTapGestures(onTap = { click() }) }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GameTile(
    game: GameApp,
    selected: Boolean,
    favorite: Boolean,
    palette: Palette,
    pulse: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val tileSize by animateDpAsState(if (selected) 180.dp else 140.dp, label = "tileSize")
    val tap by rememberUpdatedState(onTap)
    val longPress by rememberUpdatedState(onLongPress)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .size(tileSize)
            .shadow(if (selected) 12.dp else 2.dp, shape)
            .clip(shape)
            .background(palette.tile)
            .then(if (selected) Modifier.border(4.dp, pulse, shape) else Modifier)
            .pointerInput(game.packageName) {
                detectTapGestures(onTap = { tap() }, onLongPress = { longPress() })
            }
    ) {
        Image(
            bitmap = game.icon,
            contentDescription = game.label,
            modifier = Modifier.fillMaxSize().padding(18.dp),
        )
        if (favorite) {
            Text(
                "★",
                color = Color(0xFFFFB400),
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
    }
}
