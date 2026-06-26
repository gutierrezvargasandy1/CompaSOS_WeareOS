package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import mx.edu.utng.compasos_wearos.presentation.theme.*

@Composable
fun PantallaInicio(
    onEmpezarClick: () -> Unit
) {
    val config = LocalConfiguration.current
    val screenDp = config.screenWidthDp.dp
    val paddingH = screenDp * 0.12f
    val paddingV = screenDp * 0.08f

    val gradienteFondo = Brush.radialGradient(
        colors = listOf(GrisOscuro, Negro),
        radius = 600f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradienteFondo)
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                horizontal = paddingH,
                vertical = paddingV
            )
        ) {

            // ── Título ───────────────────────────────────────────
            item {
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
            }

            // ── Ícono escudo con anillo ──────────────────────────
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(AzulSeguro.copy(alpha = 0.30f), Negro)
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(color = AzulSeguro.copy(alpha = 0.15f))
                    )
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Escudo de seguridad",
                        tint = AzulClaro,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // ── Subtítulo ────────────────────────────────────────
            item {
                Text(
                    text = "Asistencia cuando\nmás lo necesitas",
                    style = MaterialTheme.typography.body2,
                    color = BlancoOpaco,
                    textAlign = TextAlign.Center
                )
            }

            // ── Botón Empezar ────────────────────────────────────
            item {
                Button(
                    onClick = onEmpezarClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AzulSeguro
                    )
                ) {
                    Text(
                        text = "Empezar",
                        style = MaterialTheme.typography.button,
                        color = Blanco
                    )
                }
            }
        }
    }
}