package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import mx.edu.utng.compasos_wearos.presentation.theme.*

/**
 * componente composable que muestra la pantalla principal de ajustes en wear os,
 * permitiendo navegar hacia configuraciones de alerta o información del dispositivo mediante fichas interactivas.
 *
 * @param onAjustesAlertaClick función lambda de devolución de llamada ejecutada al seleccionar la opción de ajustes de alerta.
 * @param onAcercaClick función lambda de devolución de llamada ejecutada al seleccionar la opción acerca del dispositivo.
 */
@Composable
fun PantallaAjustes(
    onAjustesAlertaClick: () -> Unit,
    onAcercaClick: () -> Unit
) {
    val config = LocalConfiguration.current
    val screenDp = config.screenWidthDp.dp
    val paddingH = screenDp * 0.10f
    val paddingV = screenDp * 0.12f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Negro)
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
                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.title2,
                    color = Blanco
                )
            }

            // ── Card: Ajustes de Alerta ──────────────────────────
            item {
                TarjetaAjuste(
                    icono = Icons.Filled.Notifications,
                    colorIcono = RojoEmergencia,
                    titulo = "Ajustes de Alerta",
                    onClick = onAjustesAlertaClick
                )
            }

            // ── Card: Acerca del dispositivo ─────────────────────
            item {
                TarjetaAjuste(
                    icono = Icons.Filled.Info,
                    colorIcono = AzulClaro,
                    titulo = "Acerca del dispositivo",
                    onClick = onAcercaClick
                )
            }
        }
    }
}

/**
 * componente composable privado que representa una tarjeta o ficha individual de ajuste
 * con un ícono descriptivo, color personalizado y texto de etiqueta.
 *
 * @param icono vector de imagen [ImageVector] que ilustra el ajuste.
 * @param colorIcono color de tinte asignado al ícono y su contenedor circular.
 * @param titulo texto descriptivo de la opción de ajuste.
 * @param onClick función lambda ejecutada al pulsar sobre la tarjeta.
 */
@Composable
private fun TarjetaAjuste(
    icono: ImageVector,
    colorIcono: androidx.compose.ui.graphics.Color,
    titulo: String,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ChipDefaults.chipColors(
            backgroundColor = GrisOscuro
        ),
        shape = RoundedCornerShape(16.dp),
        label = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.body1,
                color = Blanco
            )
        },
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colorIcono.copy(alpha = 0.20f))
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = titulo,
                    tint = colorIcono,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}