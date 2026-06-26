package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.material3.LinearProgressIndicator
import mx.edu.utng.compasos_wearos.presentation.theme.*

@Composable
fun PantallaVinculacion(
    onVinculacionExitosa: () -> Unit
) {
    // ── Animación de pulso ───────────────────────────────────────
    val pulso = rememberInfiniteTransition(label = "pulso")
    val escala by pulso.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )

    // ── Progreso simulado (reemplaza con tu lógica Bluetooth real) ──
    var progreso by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (progreso < 1f) {
            kotlinx.coroutines.delay(80)
            progreso += 0.01f
        }
        onVinculacionExitosa()   // cuando llega al 100% navega
    }

    // ── Fondo ────────────────────────────────────────────────────
    val gradienteFondo = Brush.radialGradient(
        colors = listOf(GrisOscuro, Negro),
        radius = 400f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradienteFondo),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // ── Título ───────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Compa",
                    style = MaterialTheme.typography.title1,
                    color = Blanco
                )
                Text(
                    text = "SOS",
                    style = MaterialTheme.typography.title1,
                    color = AzulClaro
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Ícono Bluetooth con pulso ────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp)
            ) {
                // Anillo exterior pulsante
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(escala)
                        .clip(CircleShape)
                        .background(AzulSeguro.copy(alpha = 0.15f))
                )
                // Anillo interior fijo
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AzulSeguro.copy(alpha = 0.40f),
                                    Negro
                                )
                            )
                        )
                )
                // Ícono
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = "Bluetooth",
                    tint = AzulClaro,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Texto principal ──────────────────────────────────
            Text(
                text = "Visible para tu teléfono",
                style = MaterialTheme.typography.title3,
                color = Blanco,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Texto secundario ─────────────────────────────────
            Text(
                text = "Esperando conexión...",
                style = MaterialTheme.typography.caption1,
                color = BlancoOpaco,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))


        }
    }
}