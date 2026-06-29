package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import mx.edu.utng.compasos_wearos.presentation.theme.*

@Composable
fun MovimientoInusualScreen(
    onEnviar:   () -> Unit,
    onCancelar: () -> Unit
) {
    val totalSeg = 10  // Auto-envía en 10 segundos
    var segsRestantes by remember { mutableIntStateOf(totalSeg) }
    val progreso = remember { Animatable(1f)
    }

    // Pulso del ícono
    val escalaIcono by rememberInfiniteTransition(label = "pulso")
        .animateFloat(
            initialValue   = 1f,
            targetValue    = 1.15f,
            animationSpec  = infiniteRepeatable(
                animation  = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "escalaIcono"
        )

    LaunchedEffect(Unit) {

        progreso.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = totalSeg * 1000,
                easing = LinearEasing
            )
        )

    }
    LaunchedEffect(Unit) {

        while (segsRestantes > 0) {

            delay(1000)

            segsRestantes--

        }

        onEnviar()

    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Negro),
        contentAlignment = Alignment.Center
    ) {
        val d = minOf(maxWidth, maxHeight)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((d.value * 0.020f).dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d * 0.08f)
        ) {

            // ── Ícono con borde rojo pulsante ─────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(escalaIcono)
                    .size((d.value * 0.26f).dp)
                    .clip(RoundedCornerShape((d.value * 0.07f).dp))
                    .background(GrisOscuro)
                // Borde rojo simulado con Box exterior
            ) {
                // Borde rojo
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape((d.value * 0.07f).dp))
                        .background(
                            color = AmarilloAlerta.copy(alpha = 0f)  // transparente
                        )
                )
                // Borde visible usando padding negativo trick
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size((d.value * 0.26f).dp)
                        .clip(RoundedCornerShape((d.value * 0.07f).dp))
                        .background(GrisOscuro)
                        .padding((d.value * 0.008f).dp)
                ) {
                    Text(
                        text     = "🏃",
                        fontSize = (d.value * 0.13f).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height((d.value * 0.005f).dp))

            // ── Título ────────────────────────────────────────
            Text(
                text       = "Movimiento inusual\ndetectado",
                fontSize   = (d.value * 0.076f).sp,
                color      = AmarilloAlerta,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                lineHeight = (d.value * 0.095f).sp
            )

            // ── Subtítulo ─────────────────────────────────────
            Text(
                text      = "¿Enviar alerta?",
                fontSize  = (d.value * 0.058f).sp,
                color     = BlancoOpaco,
                textAlign = TextAlign.Center
            )

            // ── Temporizador circular ─────────────────────────
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress       = progreso.value,
                    modifier       = Modifier.size((d.value * 0.18f).dp),
                    strokeWidth    = (d.value * 0.018f).dp,
                    indicatorColor = Color(0xFFE53935),   // rojo
                    trackColor     = GrisMedio
                )
                Text(
                    text       = "$segsRestantes",
                    fontSize   = (d.value * 0.085f).sp,
                    color      = Blanco,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height((d.value * 0.005f).dp))

            // ── Botones ───────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy((d.value * 0.03f).dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cancelar
                Button(
                    onClick  = onCancelar,
                    modifier = Modifier
                        .weight(1f)
                        .height((d.value * 0.14f).dp),
                    colors   = ButtonDefaults.buttonColors(
                        backgroundColor = GrisMedio
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text     = "✕",
                            fontSize = (d.value * 0.065f).sp,
                            color    = Blanco
                        )
                        Text(
                            text     = "Cancelar",
                            fontSize = (d.value * 0.050f).sp,
                            color    = BlancoOpaco
                        )
                    }
                }

                // Enviar
                Button(
                    onClick  = onEnviar,
                    modifier = Modifier
                        .weight(1f)
                        .height((d.value * 0.14f).dp),
                    colors   = ButtonDefaults.buttonColors(
                        backgroundColor = GrisOscuro
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text     = "✓",
                            fontSize = (d.value * 0.065f).sp,
                            color    = Blanco
                        )
                        Text(
                            text     = "Enviar",
                            fontSize = (d.value * 0.050f).sp,
                            color    = BlancoOpaco
                        )
                    }
                }
            }
        }
    }
}