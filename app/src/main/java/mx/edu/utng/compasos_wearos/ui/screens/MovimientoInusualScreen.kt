package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import mx.edu.utng.compasos_wearos.helper.FeedbackHelper
import mx.edu.utng.compasos_wearos.presentation.theme.*
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel

/**
 * componente composable que maneja la pantalla de notificación cuando se detecta un movimiento inusual en el dispositivo wear os.
 * muestra una cuenta regresiva con un temporizador circular, opciones para cancelar o enviar manualmente la alerta,
 * y emite retroalimentación háptica y de sonido durante la cuenta regresiva.
 *
 * @param onEnviar función lambda de devolución de llamada ejecutada al enviar la alerta (por temporizador o botón).
 * @param onCancelar función lambda de devolución de llamada ejecutada al cancelar la alerta.
 * @param vm instancia del [VinculacionViewModel] inyectada para obtener configuraciones del reloj.
 */
@Composable
fun MovimientoInusualScreen(
    onEnviar:   () -> Unit,
    onCancelar: () -> Unit,
    vm:         VinculacionViewModel = viewModel()
) {
    val totalSeg = 10  // Auto-envía en 10 segundos
    var segsRestantes by remember { mutableIntStateOf(totalSeg) }
    val progreso = remember { Animatable(1f) }

    val context = LocalContext.current
    val feedback = remember { FeedbackHelper(context) }

    val config by vm.configReloj.collectAsState()
    val modoDiscreto = config?.modoDiscreto ?: false

    DisposableEffect(Unit) {
        onDispose { feedback.liberar() }
    }

    // ── Aviso inicial al entrar a esta pantalla ────────────────
    LaunchedEffect(Unit) {
        feedback.avisarDeteccion(modoDiscreto)
    }

    LaunchedEffect(Unit) {
        progreso.animateTo(
            targetValue   = 0f,
            animationSpec = tween(
                durationMillis = totalSeg * 1000,
                easing         = LinearEasing
            )
        )
    }

    // ── Conteo + vibración/sonido al ritmo de los segundos ─────
    LaunchedEffect(Unit) {
        while (segsRestantes > 0) {
            delay(1000)
            segsRestantes--
            feedback.vibrarTick()
            feedback.reproducirTick(modoDiscreto)
        }
        onEnviar()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Negro)
    ) {
        val d = minOf(maxWidth, maxHeight)

        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((d.value * 0.025f).dp),
            contentPadding = PaddingValues(
                horizontal = d * 0.10f,
                vertical   = d * 0.08f
            )
        ) {

            // ── Ícono de advertencia ───────────────────────────
            item {
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
            }

            // ── Título ───────────────────────────────────────
            item {
                Text(
                    text       = "Movimiento inusual\ndetectado",
                    fontSize   = (d.value * 0.082f).sp,
                    color      = Blanco,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                    lineHeight = (d.value * 0.10f).sp
                )
            }

            // ── Subtítulo ──────────────────────────────────────
            item {
                Text(
                    text       = "¿Enviar alerta?",
                    fontSize   = (d.value * 0.055f).sp,
                    color      = BlancoOpaco,
                    textAlign  = TextAlign.Center,
                    lineHeight = (d.value * 0.072f).sp
                )
            }

            // ── Temporizador circular ───────────────────────────
            item {
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
            }

            // ── Botones ──────────────────────────────────────
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((d.value * 0.03f).dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick  = onCancelar,
                        modifier = Modifier
                            .weight(1f)
                            .height((d.value * 0.13f).dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = GrisMedio
                        )
                    ) {
                        Text("No", fontSize = (d.value * 0.062f).sp, color = Blanco)
                    }
                    Button(
                        onClick  = onEnviar,
                        modifier = Modifier
                            .weight(1f)
                            .height((d.value * 0.13f).dp),
                        colors = ButtonDefaults.buttonColors(
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