package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PhoneAndroid
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
fun PantallaConfirmacionVinculacion(
    nombreDispositivo: String = "Pixel 8 Pro",
    nombreReloj: String = "Este reloj",
    onVincular: () -> Unit,
    onCancelar: () -> Unit
) {
    // Tamaño real de la pantalla para calcular padding circular
    val config = LocalConfiguration.current
    val screenDp = config.screenWidthDp.dp
    val paddingH = screenDp * 0.10f   // 10% a cada lado
    val paddingV = screenDp * 0.06f   // 6% arriba/abajo

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
            verticalArrangement = Arrangement.spacedBy(6.dp),
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

            // ── Dispositivos: teléfono ··· reloj ─────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Teléfono
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, AzulClaro, CircleShape)
                                .background(AzulSeguro.copy(alpha = 0.20f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = "Teléfono",
                                tint = AzulClaro,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = nombreDispositivo,
                            style = MaterialTheme.typography.caption1,
                            color = BlancoOpaco
                        )
                    }

                    // Línea punteada
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 2.dp)
                                    .background(
                                        color = AzulClaro.copy(alpha = 0.50f),
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }

                    // Reloj
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, AzulClaro, CircleShape)
                                .background(AzulSeguro.copy(alpha = 0.20f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bluetooth,
                                contentDescription = "Reloj",
                                tint = AzulClaro,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = nombreReloj,
                            style = MaterialTheme.typography.caption1,
                            color = BlancoOpaco
                        )
                    }
                }
            }

            // ── Pregunta ──────────────────────────────────────────
            item {
                Text(
                    text = "¿Vincular con\n$nombreDispositivo?",
                    style = MaterialTheme.typography.title2,
                    color = Blanco,
                    textAlign = TextAlign.Center
                )
            }

            // ── Descripción ───────────────────────────────────────
            item {
                Text(
                    text = "El teléfono podrá recibir\ntus alertas de emergencia",
                    style = MaterialTheme.typography.caption1,
                    color = BlancoOpaco,
                    textAlign = TextAlign.Center
                )
            }

            // ── Botones ───────────────────────────────────────────
            item {
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
                        onClick = onVincular,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AzulSeguro
                        )
                    ) {
                        Text(
                            text = "Vincular",
                            style = MaterialTheme.typography.button,
                            color = Blanco
                        )
                    }
                }
            }
        }
    }
}