package mx.edu.utng.compasos_wearos.ui.screens.components

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
                    .padding(top = d * 0.12f, end = d * 0.10f)
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
                // Anillo exterior
                Box(
                    modifier = Modifier
                        .size(zonaSegura)
                        .background(color = GrisOscuro, shape = CircleShape)
                )
                // Anillo interior
                Box(
                    modifier = Modifier
                        .size(zonaSegura * 0.78f)
                        .background(
                            color = AzulSeguro.copy(alpha = 0.20f),
                            shape = CircleShape
                        )
                )
                // Botón SOS con long press personalizado
                Box(
                    modifier = Modifier
                        .size(zonaSegura * 0.62f)
                        .clip(RoundedCornerShape(zonaSegura * 0.14f))
                        .background(RojoOscuro)
                        .pointerInput(tiempoPresionMs) {
                            awaitPointerEventScope {
                                while (true) {
                                    // Espera toque inicial
                                    awaitFirstDown()

                                    // Animación bajada
                                    scope.launch {
                                        escalaBoton.animateTo(
                                            0.92f,
                                            animationSpec = tween(100)
                                        )
                                    }

                                    var soltado = false

                                    // Espera tiempoPresionMs
                                    val event = withTimeoutOrNull(tiempoPresionMs) {
                                        // Espera cualquier evento (levantar dedo)
                                        while (true) {
                                            val ev = awaitPointerEvent()
                                            val levanto = ev.changes.any { !it.pressed }
                                            if (levanto) {
                                                soltado = true
                                                break
                                            }
                                        }
                                    }

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

            // ── 3. TEXTO DE AYUDA ────────────────────────────────
            Text(
                text = "Mantén presionado ${tiempoPresionMs / 1000} seg",
                style = MaterialTheme.typography.caption1,
                color = BlancoOpaco,
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
    var segundosRestantes by remember { mutableIntStateOf(5) }
    val progreso by remember {
        derivedStateOf { segundosRestantes / 5f }
    }

    LaunchedEffect(Unit) {
        while (segundosRestantes > 0) {
            delay(1_000)
            segundosRestantes--
        }
        onConfirmar()
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
                        progress = progreso,
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