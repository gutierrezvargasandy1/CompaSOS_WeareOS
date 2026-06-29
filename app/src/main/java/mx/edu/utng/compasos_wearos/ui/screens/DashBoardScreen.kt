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
import kotlin.math.abs

@Composable
fun DashboardScreen(
    onIrAInicio: () -> Unit,
    onIrAAjustes: () -> Unit,
    onDispararSOS: () -> Unit,
    onSwipeIzquierda: () -> Unit,
    tabActiva: String = "inicio",
    vm: VinculacionViewModel = viewModel()
) {
    var gestoProcesado by remember { mutableStateOf(false) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    val config by vm.configReloj.collectAsState()
    val tiempoPresionMs = (config?.tiempoPanicoMs ?: 3000).toLong()

    val escalaBoton = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // ── NUEVAS VARIABLES para parpadeo y cuenta regresiva ──────
    var presionando by remember { mutableStateOf(false) }
    var segundosCuenta by remember { mutableIntStateOf((tiempoPresionMs / 1000).toInt()) }

    // Alpha del botón: parpadea entre 1f y 0.3f mientras se presiona
    val alphaBoton by animateFloatAsState(
        targetValue = if (presionando) 0.3f else 1f,
        animationSpec = if (presionando)
            infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        else tween(150),
        label = "parpadeo"
    )

    // ── ANIMACIONES DE ANILLOS ──────────────────────────────────
    // Escala: los anillos pulsan (crecen y encogen) mientras se presiona
    val escalaAnillos by animateFloatAsState(
        targetValue = if (presionando) 1.08f else 1f,
        animationSpec = if (presionando)
            infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        else tween(300),
        label = "escalaAnillos"
    )

    // Color anillo exterior: GrisOscuro → rojo oscuro al presionar
    val colorAnilloExterior by animateColorAsState(
        targetValue = if (presionando) RojoOscuro.copy(alpha = 0.60f) else GrisOscuro,
        animationSpec = tween(400),
        label = "colorExterior"
    )

    // Color anillo interior: azul → rojo al presionar
    val colorAnilloInterior by animateColorAsState(
        targetValue = if (presionando) RojoOscuro.copy(alpha = 0.45f) else AzulSeguro.copy(alpha = 0.20f),
        animationSpec = tween(400),
        label = "colorInterior"
    )
    // ───────────────────────────────────────────────────────────

    if (mostrarDialogo) {
        DialogoConfirmacionSOS(
            onConfirmar = {
                mostrarDialogo = false
                onDispararSOS()
            },
            onCancelar = {
                mostrarDialogo = false
            }
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
                        onDragStart = { gestoProcesado = false },
                        onDrag = { _, dragAmount ->
                            if (!gestoProcesado) {
                                val esHorizontal = abs(dragAmount.x) > abs(dragAmount.y)
                                if (esHorizontal && dragAmount.x < -60f) {
                                    gestoProcesado = true
                                    onSwipeIzquierda()
                                }
                            }
                        },
                        onDragEnd = { gestoProcesado = false },
                        onDragCancel = { gestoProcesado = false }
                    )
                }
        ) {
            val d = minOf(maxWidth, maxHeight)
            val zonaSegura = d * 0.60f

            // ── 1. BADGE "● Activo" ──────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = d * 0.12f, end = d * 0.18f)
                    .background(
                        color = VerdeConectado.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(42)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "● Activo",
                    style = MaterialTheme.typography.caption1,
                    color = VerdeConectado,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── 2. ANILLOS + BOTÓN SOS ───────────────────────────
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                // Anillo exterior — pulsa y cambia a rojo al presionar
                Box(
                    modifier = Modifier
                        .size(zonaSegura * escalaAnillos)
                        .background(color = colorAnilloExterior, shape = CircleShape)
                )
                // Anillo interior — pulsa y cambia a rojo al presionar
                Box(
                    modifier = Modifier
                        .size(zonaSegura * 0.78f * escalaAnillos)
                        .background(
                            color = colorAnilloInterior,
                            shape = CircleShape
                        )
                )
                // ── Botón SOS con parpadeo y cuenta regresiva ────
                Box(
                    modifier = Modifier
                        .size(zonaSegura * 0.62f)
                        .clip(RoundedCornerShape(zonaSegura * 0.14f))
                        .background(RojoOscuro.copy(alpha = alphaBoton))
                        .pointerInput(tiempoPresionMs) {
                            awaitPointerEventScope {
                                while (true) {
                                    // Espera toque inicial
                                    awaitFirstDown()

                                    // Activa parpadeo y reinicia cuenta
                                    presionando = true
                                    segundosCuenta = (tiempoPresionMs / 1000).toInt()

                                    // Animación bajada
                                    scope.launch {
                                        escalaBoton.animateTo(
                                            0.92f,
                                            animationSpec = tween(100)
                                        )
                                    }

                                    // Job independiente: reduce contador cada segundo
                                    val cuentaJob = scope.launch {
                                        while (segundosCuenta > 0) {
                                            delay(1_000)
                                            segundosCuenta--
                                        }
                                    }

                                    var soltado = false

                                    // Espera tiempoPresionMs o hasta que suelte
                                    val event = withTimeoutOrNull(tiempoPresionMs) {
                                        while (true) {
                                            val ev = awaitPointerEvent()
                                            val levanto = ev.changes.any { !it.pressed }
                                            if (levanto) {
                                                soltado = true
                                                break
                                            }
                                        }
                                    }

                                    // Cancela la cuenta regresiva si soltó antes
                                    cuentaJob.cancel()
                                    presionando = false
                                    segundosCuenta = (tiempoPresionMs / 1000).toInt()

                                    // null = timeout = dedo sostenido el tiempo completo
                                    if (event == null && !soltado) {
                                        mostrarDialogo = true
                                    }

                                    // Animación subida
                                    scope.launch {
                                        escalaBoton.animateTo(
                                            1f,
                                            animationSpec = spring()
                                        )
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SOS",
                        style = MaterialTheme.typography.title1,
                        color = Blanco,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            // ── 3. TEXTO DE AYUDA con cuenta regresiva ───────────
            Text(
                text = if (presionando) "Suelta para cancelar... $segundosCuenta"
                else "Mantén presionado ${tiempoPresionMs / 1000} seg",
                style = MaterialTheme.typography.caption1,
                color = if (presionando) AmarilloAlerta else BlancoOpaco,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = d * 0.10f)
            )
        }
    }
}

// ── Diálogo de confirmación con cuenta regresiva ─────────────
@Composable
private fun DialogoConfirmacionSOS(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val totalSegundos = 5
    var segundosRestantes by remember { mutableIntStateOf(totalSegundos) }

    // Progreso fluido: baja de 1f a 0f de forma continua sin saltos
    val progreso = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Anima el círculo de 1f → 0f en exactamente totalSegundos segundos
        progreso.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = totalSegundos * 1000,
                easing = LinearEasing
            )
        )
        onConfirmar()
    }

    // Actualiza solo el número de texto cada segundo
    LaunchedEffect(Unit) {
        while (segundosRestantes > 0) {
            delay(1_000)
            segundosRestantes--
        }
    }

    Dialog(onDismissRequest = onCancelar) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Negro),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {

                // Ícono de advertencia
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AmarilloAlerta.copy(alpha = 0.20f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Advertencia",
                        tint = AmarilloAlerta,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "¿Enviar alerta?",
                    style = MaterialTheme.typography.title2,
                    color = Blanco,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Se notificará a tus contactos de emergencia",
                    style = MaterialTheme.typography.caption1,
                    color = BlancoOpaco,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Temporizador circular
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = progreso.value,
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 3.dp,
                        indicatorColor = AmarilloAlerta,
                        trackColor = GrisMedio
                    )
                    Text(
                        text = "$segundosRestantes",
                        style = MaterialTheme.typography.title2,
                        color = Blanco,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botones
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCancelar,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = GrisMedio
                        )
                    ) {
                        Text(
                            text = "No",
                            style = MaterialTheme.typography.button,
                            color = Blanco
                        )
                    }

                    Button(
                        onClick = onConfirmar,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = RojoEmergencia
                        )
                    ) {
                        Text(
                            text = "Enviar",
                            style = MaterialTheme.typography.button,
                            color = Blanco
                        )
                    }
                }
            }
        }
    }
}