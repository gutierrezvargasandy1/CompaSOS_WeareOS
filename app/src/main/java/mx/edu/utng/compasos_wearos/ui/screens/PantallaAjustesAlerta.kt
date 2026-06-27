package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import mx.edu.utng.compasos_wearos.presentation.theme.*
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel

@Composable
fun PantallaAjustesAlerta(
    vm: VinculacionViewModel = viewModel()
) {
    // Lee config de Room en tiempo real
    val config by vm.configReloj.collectAsState()

    // Estado local inicializado desde Room
    var alarmaDiscreta by remember(config) {
        mutableStateOf(config?.modoDiscreto ?: false)
    }
    var tiempoPulsacion by remember(config) {
        mutableIntStateOf((config?.tiempoPanicoMs ?: 3000) / 1000)
    }

    val localConfig = LocalConfiguration.current
    val screenDp = localConfig.screenWidthDp.dp
    val paddingH = screenDp * 0.08f
    val paddingV = screenDp * 0.10f

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
                    text = "Ajustes de Alerta",
                    style = MaterialTheme.typography.title2,
                    color = Blanco,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Card: Alarma discreta ────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = GrisOscuro,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GrisMedio)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsOff,
                                contentDescription = "Alarma discreta",
                                tint = BlancoOpaco,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alarma discreta",
                                style = MaterialTheme.typography.body1,
                                color = Blanco,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Sin sonido ni vibración",
                                style = MaterialTheme.typography.caption1,
                                color = BlancoOpaco
                            )
                        }

                        Switch(
                            checked = alarmaDiscreta,
                            onCheckedChange = { nuevo ->
                                alarmaDiscreta = nuevo
                                vm.setModoDiscreto(nuevo)  // guarda en Room
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = Blanco,
                                checkedTrackColor   = AzulSeguro,
                                uncheckedThumbColor = Blanco,
                                uncheckedTrackColor = GrisMedio
                            )
                        )
                    }
                }
            }

            // ── Card: Tiempo de pulsación ────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = GrisOscuro,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RojoEmergencia.copy(alpha = 0.20f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = "Tiempo de pulsación",
                                    tint = RojoEmergencia,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Tiempo de pulsación",
                                    style = MaterialTheme.typography.body1,
                                    color = Blanco,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Para activar el SOS",
                                    style = MaterialTheme.typography.caption1,
                                    color = BlancoOpaco
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Botón −
                            Button(
                                onClick = {
                                    if (tiempoPulsacion > 1) {
                                        tiempoPulsacion--
                                        vm.setTiempoPanico(tiempoPulsacion) // guarda en Room
                                    }
                                },
                                modifier = Modifier.size(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = GrisMedio
                                )
                            ) {
                                Text(
                                    text = "−",
                                    style = MaterialTheme.typography.title2,
                                    color = Blanco
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$tiempoPulsacion",
                                    style = MaterialTheme.typography.title1,
                                    color = AzulClaro,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "segundos",
                                    style = MaterialTheme.typography.caption1,
                                    color = BlancoOpaco
                                )
                            }

                            // Botón +
                            Button(
                                onClick = {
                                    if (tiempoPulsacion < 10) {
                                        tiempoPulsacion++
                                        vm.setTiempoPanico(tiempoPulsacion) // guarda en Room
                                    }
                                },
                                modifier = Modifier.size(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = GrisMedio
                                )
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.title2,
                                    color = Blanco
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}