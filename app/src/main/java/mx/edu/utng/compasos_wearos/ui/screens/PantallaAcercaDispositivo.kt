package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj
import mx.edu.utng.compasos_wearos.presentation.theme.*
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel

@Composable
fun PantallaAcercaDispositivo(
    vm: VinculacionViewModel = viewModel()
) {
    // Observa la config de Room en tiempo real
    val config by vm.configReloj.collectAsState()

    PantallaAcercaDispositivoContenido(
        config          = config ?: ConfigReloj(),
        modeloReloj     = android.os.Build.MODEL,
        versionSO       = android.os.Build.VERSION.RELEASE,
        versionApp      = "1.0.0"
    )
}

@Composable
private fun PantallaAcercaDispositivoContenido(
    config      : ConfigReloj,
    modeloReloj : String,
    versionSO   : String,
    versionApp  : String
) {
    val localConfig = LocalConfiguration.current
    val screenDp    = localConfig.screenWidthDp.dp
    val paddingH    = screenDp * 0.08f
    val paddingV    = screenDp * 0.10f

    val estaVinculado = config.deviceIdVinculado.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Negro)
    ) {
        ScalingLazyColumn(
            modifier              = Modifier.fillMaxSize(),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            contentPadding        = PaddingValues(
                horizontal = paddingH,
                vertical   = paddingV
            )
        ) {

            // ── Título ───────────────────────────────────────────
            item {
                Text(
                    text       = "Acerca del dispositivo",
                    style      = MaterialTheme.typography.title2,
                    color      = Blanco,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Card: Info del reloj ─────────────────────────────
            item {
                SeccionInfo(titulo = "Este reloj") {
                    FilaInfo(
                        icono     = Icons.Filled.Watch,
                        colorIcono = AzulClaro,
                        etiqueta  = "Modelo",
                        valor     = modeloReloj
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FilaInfo(
                        icono      = Icons.Filled.Watch,
                        colorIcono = AzulClaro,
                        etiqueta   = "Android",
                        valor      = "Wear OS $versionSO"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FilaInfo(
                        icono      = Icons.Filled.Watch,
                        colorIcono = AzulClaro,
                        etiqueta   = "App",
                        valor      = "v$versionApp"
                    )
                }
            }

            // ── Card: Vinculación ────────────────────────────────
            item {
                SeccionInfo(titulo = "Vinculación") {

                    // Badge de estado
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (estaVinculado)
                                        VerdeConectado.copy(alpha = 0.20f)
                                    else
                                        GrisDesactivado.copy(alpha = 0.20f)
                                )
                        ) {
                            Icon(
                                imageVector    = if (estaVinculado)
                                    Icons.Filled.Link else Icons.Filled.LinkOff,
                                contentDescription = "Estado vinculación",
                                tint           = if (estaVinculado) VerdeConectado else GrisDesactivado,
                                modifier       = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text       = if (estaVinculado) "Vinculado" else "Sin vincular",
                                style      = MaterialTheme.typography.body1,
                                color      = if (estaVinculado) VerdeConectado else GrisDesactivado,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (estaVinculado) {
                                Text(
                                    text  = config.nombreDispositivoVinculado,
                                    style = MaterialTheme.typography.caption1,
                                    color = BlancoOpaco
                                )
                            }
                        }
                    }

                    // Detalles cuando está vinculado
                    if (estaVinculado) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FilaInfo(
                            icono      = Icons.Filled.Smartphone,
                            colorIcono = AzulSeguro,
                            etiqueta   = "Teléfono",
                            valor      = config.nombreDispositivoVinculado
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FilaInfo(
                            icono      = Icons.Filled.Link,
                            colorIcono = AzulSeguro,
                            etiqueta   = "ID",
                            valor      = config.deviceIdVinculado.take(12) + "..."
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FilaValor(
                            etiqueta = "Última sync",
                            valor    = formatearFecha(config.ultimaActualizacion)
                        )
                    }
                }
            }

            // ── Card: Configuración activa ───────────────────────
            item {
                SeccionInfo(titulo = "Configuración") {
                    FilaValor(
                        etiqueta = "Tiempo pánico",
                        valor    = "${config.tiempoPanicoMs / 1000} seg"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FilaValor(
                        etiqueta = "Modo discreto",
                        valor    = if (config.modoDiscreto) "Activado" else "Desactivado"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FilaValor(
                        etiqueta = "Monitoreo",
                        valor    = "Cada ${config.intervaloMonitoreoSeg} seg"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FilaValor(
                        etiqueta = "Alerta BPM",
                        valor    = "+${config.umbralBpmAlerta} bpm"
                    )
                }
            }
        }
    }
}

// ── Formatea timestamp a fecha legible ───────────────────────
private fun formatearFecha(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

// ── Componente: sección con título y contenido ───────────────
@Composable
private fun SeccionInfo(
    titulo    : String,
    contenido : @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = GrisOscuro, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text       = titulo,
            style      = MaterialTheme.typography.title3,
            color      = BlancoOpaco,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        contenido()
    }
}

// ── Componente: fila con ícono, etiqueta y valor ─────────────
@Composable
private fun FilaInfo(
    icono      : androidx.compose.ui.graphics.vector.ImageVector,
    colorIcono : androidx.compose.ui.graphics.Color,
    etiqueta   : String,
    valor      : String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colorIcono.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector        = icono,
                contentDescription = etiqueta,
                tint               = colorIcono,
                modifier           = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text     = etiqueta,
            style    = MaterialTheme.typography.caption1,
            color    = BlancoOpaco,
            modifier = Modifier.weight(1f)
        )
        Text(
            text       = valor,
            style      = MaterialTheme.typography.caption1,
            color      = Blanco,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Componente: fila solo etiqueta y valor ───────────────────
@Composable
private fun FilaValor(
    etiqueta : String,
    valor    : String
) {
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text  = etiqueta,
            style = MaterialTheme.typography.caption1,
            color = BlancoOpaco
        )
        Text(
            text       = valor,
            style      = MaterialTheme.typography.caption1,
            color      = Blanco,
            fontWeight = FontWeight.Medium
        )
    }
}