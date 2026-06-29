package mx.edu.utng.compasos_wearos.ui.screens.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.edu.utng.compasos_wearos.presentation.theme.*
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// TODO: cambiar a false antes de publicar
private const val ES_DEBUG_DASHBOARD = true

@Composable
fun DashboardScreen(
    onIrAInicio:      () -> Unit,
    onIrAAjustes:     () -> Unit,
    onDispararSOS:    () -> Unit,
    onSwipeIzquierda: () -> Unit,
    onTestMovimiento: () -> Unit = {},
    tabActiva:        String = "inicio",
    vm:               VinculacionViewModel = viewModel()
) {
    var gestoProcesado by remember { mutableStateOf(false) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    val config by vm.configReloj.collectAsState()
    val tiempoPresionMs = (config?.tiempoPanicoMs ?: 3000).toLong()
    val totalSegundos = (tiempoPresionMs / 1000).toInt()

    val scope = rememberCoroutineScope()

    // ── Reloj en tiempo real ────────────────────────────────────
    var horaActual  by remember { mutableStateOf("") }
    var fechaActual by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val fmtHora  = SimpleDateFormat("HH:mm", Locale.getDefault())
        val fmtFecha = SimpleDateFormat("EEE, dd MMM", Locale("es"))
        while (true) {
            val ahora = Date()
            horaActual  = fmtHora.format(ahora)
            fechaActual = fmtFecha.format(ahora).uppercase()
            delay(1_000)
        }
    }

    // ── Estado de presión ───────────────────────────────────────
    var presionando     by remember { mutableStateOf(false) }
    var segundosCuenta  by remember { mutableIntStateOf(totalSegundos) }
    var progresoPresion by remember { mutableFloatStateOf(0f) }

    // ── Color botón: azul → amarillo → rojo ────────────────────
    val colorBoton: Color = when {
        !presionando           -> AzulClaro
        progresoPresion < 0.5f -> {
            val t = progresoPresion / 0.5f
            lerp(AzulClaro, AmarilloAlerta, t)
        }
        else -> {
            val t = (progresoPresion - 0.5f) / 0.5f
            lerp(AmarilloAlerta, Color(0xFFE53935), t)
        }
    }

    val alphaParpadeo by animateFloatAsState(
        targetValue   = if (presionando) 0.70f else 1f,
        animationSpec = if (presionando)
            infiniteRepeatable(
                animation  = tween(350, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        else tween(150),
        label = "parpadeo"
    )

    // ── Anillos reactivos ───────────────────────────────────────
    val escalaAnillos by animateFloatAsState(
        targetValue   = if (presionando) 1.09f else 1f,
        animationSpec = if (presionando)
            infiniteRepeatable(
                animation  = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        else tween(300),
        label = "escalaAnillos"
    )

    val colorAnilloExterior by animateColorAsState(
        targetValue   = if (presionando) colorBoton.copy(alpha = 0.30f)
        else GrisOscuro.copy(alpha = 0.70f),
        animationSpec = tween(300),
        label         = "anilloExt"
    )

    val colorAnilloInterior by animateColorAsState(
        targetValue   = if (presionando) colorBoton.copy(alpha = 0.20f)
        else AzulSeguro.copy(alpha = 0.18f),
        animationSpec = tween(300),
        label         = "anilloInt"
    )

    if (mostrarDialogo) {
        DialogoConfirmacionSOS(
            onConfirmar = { mostrarDialogo = false; onDispararSOS() },
            onCancelar  = { mostrarDialogo = false }
        )
    }

    Scaffold(timeText = {}) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Negro)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart  = { gestoProcesado = false },
                        onDrag       = { _, drag ->
                            if (!gestoProcesado) {
                                if (abs(drag.x) > abs(drag.y) && drag.x < -60f) {
                                    gestoProcesado = true
                                    onSwipeIzquierda()
                                }
                            }
                        },
                        onDragEnd    = { gestoProcesado = false },
                        onDragCancel = { gestoProcesado = false }
                    )
                }
        ) {
            val d = minOf(maxWidth, maxHeight)

            val tamHora    = (d.value * 0.13f).sp
            val tamFecha   = (d.value * 0.052f).sp
            val tamSOS     = (d.value * 0.125f).sp
            val tamCaption = (d.value * 0.056f).sp
            val tamBadge   = (d.value * 0.048f).sp
            val padTop     = d * 0.075f
            val padLateral = d * 0.13f
            val zonaSegura = d * 0.50f

            // ── 1. HORA + FECHA ──────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = padTop, start = padLateral)
            ) {
                Text(
                    text          = horaActual,
                    fontSize      = tamHora,
                    fontWeight    = FontWeight.Bold,
                    color         = Blanco,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text          = fechaActual,
                    fontSize      = tamFecha,
                    color         = BlancoOpaco,
                    letterSpacing = 0.3.sp
                )
            }

            // ── 2. BADGE "● Activo" ──────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = padTop + d * 0.015f, end = padLateral)
                    .background(
                        color = VerdeConectado.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(42)
                    )
                    .padding(
                        horizontal = (d.value * 0.022f).dp,
                        vertical   = (d.value * 0.008f).dp
                    )
            ) {
                Text(
                    text       = "● Activo",
                    fontSize   = tamBadge,
                    color      = VerdeConectado,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── 3. ANILLOS + BOTÓN SOS ───────────────────────────
            Box(
                modifier         = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(zonaSegura * escalaAnillos)
                        .background(colorAnilloExterior, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(zonaSegura * 0.78f * escalaAnillos)
                        .background(colorAnilloInterior, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(zonaSegura * 0.62f)
                        .clip(RoundedCornerShape(zonaSegura * 0.15f))
                        .background(colorBoton.copy(alpha = alphaParpadeo))
                        .pointerInput(tiempoPresionMs) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitFirstDown()

                                    presionando     = true
                                    segundosCuenta  = totalSegundos
                                    progresoPresion = 0f

                                    val progressJob = scope.launch {
                                        val pasos      = tiempoPresionMs / 50
                                        val incremento = 1f / pasos
                                        repeat(pasos.toInt()) {
                                            delay(50)
                                            progresoPresion = (progresoPresion + incremento)
                                                .coerceIn(0f, 1f)
                                        }
                                    }
                                    val cuentaJob = scope.launch {
                                        while (segundosCuenta > 0) {
                                            delay(1_000)
                                            segundosCuenta--
                                        }
                                    }

                                    var soltado = false
                                    val event = withTimeoutOrNull(tiempoPresionMs) {
                                        while (true) {
                                            val ev      = awaitPointerEvent()
                                            val levanto = ev.changes.any { !it.pressed }
                                            if (levanto) { soltado = true; break }
                                        }
                                    }

                                    progressJob.cancel()
                                    cuentaJob.cancel()
                                    presionando     = false
                                    progresoPresion = 0f
                                    segundosCuenta  = totalSegundos

                                    if (event == null && !soltado) {
                                        mostrarDialogo = true
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text          = "SOS",
                        fontSize      = tamSOS,
                        color         = Blanco,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            // ── 4. TEXTO AYUDA ───────────────────────────────────
            Text(
                text      = if (presionando)
                    "Suelta para cancelar... $segundosCuenta"
                else
                    "Mantén presionado $totalSegundos seg",
                fontSize  = tamCaption,
                color     = if (presionando) colorBoton else BlancoOpaco,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = d * 0.16f,
                        start  = d * 0.10f,
                        end    = d * 0.10f
                    )
            )

            // ── 5. BOTÓN TEST (solo debug) ───────────────────────
            if (ES_DEBUG_DASHBOARD && config?.modoDiscreto == true) {                Button(
                    onClick  = onTestMovimiento,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = d * 0.03f)
                        .size((d.value * 0.15f).dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AmarilloAlerta.copy(alpha = 0.25f)
                    )
                ) {
                    Text(
                        text     = "🧪",
                        fontSize = (d.value * 0.065f).sp
                    )
                }
            }
        }
    }
}

// ── Interpolación lineal de Color ────────────────────────────
private fun lerp(start: Color, end: Color, t: Float): Color {
    val s = t.coerceIn(0f, 1f)
    return Color(
        red   = start.red   + (end.red   - start.red)   * s,
        green = start.green + (end.green - start.green)  * s,
        blue  = start.blue  + (end.blue  - start.blue)  * s,
        alpha = start.alpha + (end.alpha - start.alpha)  * s
    )
}

// ── Diálogo de confirmación ───────────────────────────────────
@Composable
private fun DialogoConfirmacionSOS(
    onConfirmar: () -> Unit,
    onCancelar:  () -> Unit
) {
    val totalSeg = 5
    var segsRestantes by remember { mutableIntStateOf(totalSeg) }
    val progreso = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        progreso.animateTo(
            targetValue   = 0f,
            animationSpec = tween(totalSeg * 1000, easing = LinearEasing)
        )
        onConfirmar()
    }
    LaunchedEffect(Unit) {
        while (segsRestantes > 0) { delay(1_000); segsRestantes-- }
    }

    Dialog(onDismissRequest = onCancelar) {
        BoxWithConstraints(
            modifier         = Modifier
                .fillMaxSize()
                .background(Negro),
            contentAlignment = Alignment.Center
        ) {
            val d = minOf(maxWidth, maxHeight)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((d.value * 0.025f).dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d * 0.10f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size((d.value * 0.20f).dp)
                        .clip(CircleShape)
                        .background(AmarilloAlerta.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Warning,
                        contentDescription = "Advertencia",
                        tint               = AmarilloAlerta,
                        modifier           = Modifier.size((d.value * 0.12f).dp)
                    )
                }

                Text(
                    text       = "¿Enviar alerta?",
                    fontSize   = (d.value * 0.082f).sp,
                    color      = Blanco,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )

                Text(
                    text       = "Se notificará a tus\ncontactos de emergencia",
                    fontSize   = (d.value * 0.055f).sp,
                    color      = BlancoOpaco,
                    textAlign  = TextAlign.Center,
                    lineHeight = (d.value * 0.072f).sp
                )

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress       = progreso.value,
                        modifier       = Modifier.size((d.value * 0.19f).dp),
                        strokeWidth    = (d.value * 0.014f).dp,
                        indicatorColor = AmarilloAlerta,
                        trackColor     = GrisMedio
                    )
                    Text(
                        text       = "$segsRestantes",
                        fontSize   = (d.value * 0.090f).sp,
                        color      = Blanco,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy((d.value * 0.03f).dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick  = onCancelar,
                        modifier = Modifier
                            .weight(1f)
                            .height((d.value * 0.13f).dp),
                        colors   = ButtonDefaults.buttonColors(
                            backgroundColor = GrisMedio
                        )
                    ) {
                        Text("No", fontSize = (d.value * 0.062f).sp, color = Blanco)
                    }
                    Button(
                        onClick  = onConfirmar,
                        modifier = Modifier
                            .weight(1f)
                            .height((d.value * 0.13f).dp),
                        colors   = ButtonDefaults.buttonColors(
                            backgroundColor = RojoEmergencia
                        )
                    ) {
                        Text("Enviar", fontSize = (d.value * 0.062f).sp, color = Blanco)
                    }
                }
            }
        }
    }
}