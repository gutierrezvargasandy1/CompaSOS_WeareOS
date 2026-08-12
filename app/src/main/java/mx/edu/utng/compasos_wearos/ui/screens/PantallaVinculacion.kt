package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.compose.runtime.collectAsState
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.presentation.theme.*
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel

@Composable
fun PantallaVinculacion(
    viewModel: VinculacionViewModel,
    onSolicitudConCodigo: (String) -> Unit,   // MQTT trajo un código → ir a teclado
    onVinculacionExitosa: () -> Unit
) {

    val estado by viewModel.estado.collectAsState()

    val pulso = rememberInfiniteTransition(label = "pulso")

    val escala by pulso.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )

    LaunchedEffect(estado) {

        when (val estadoActual = estado) {

            is VinculacionState.Esperando -> {
                // Esperando conexión con el broker / solicitud del teléfono
            }

            is VinculacionState.SolicitudRecibida -> {
                estadoActual.codigoEsperado?.let { codigo ->
                    onSolicitudConCodigo(codigo)
                }
            }

            is VinculacionState.CodigoIncorrecto -> {
                // No debería llegar aquí, se maneja en PantallaIngresoCodigo
            }

            is VinculacionState.Vinculando -> {
                // Indicador de progreso, ya se muestra abajo
            }

            is VinculacionState.Vinculado -> {
                onVinculacionExitosa()
            }

            is VinculacionState.Error -> {
                // Aquí luego mostraremos un mensaje
            }

        }

    }

    val gradienteFondo = Brush.radialGradient(
        colors = listOf(
            GrisOscuro,
            Negro
        ),
        radius = 400f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradienteFondo),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

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

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(escala)
                        .clip(CircleShape)
                        .background(
                            AzulSeguro.copy(alpha = 0.15f)
                        )
                )

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AzulSeguro.copy(alpha = 0.40f),
                                    Negro
                                )
                            )
                        )
                )

                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = AzulClaro,
                    modifier = Modifier.size(28.dp)
                )

            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Visible para tu teléfono",
                style = MaterialTheme.typography.title3,
                color = Blanco,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = when (estado) {

                    is VinculacionState.Esperando ->
                        "Esperando conexión..."

                    is VinculacionState.SolicitudRecibida ->
                        "Solicitud recibida"

                    is VinculacionState.CodigoIncorrecto ->
                        "Código incorrecto, inténtalo de nuevo"

                    is VinculacionState.Vinculando ->
                        "Vinculando..."

                    is VinculacionState.Vinculado ->
                        "Vinculado"

                    is VinculacionState.Error ->
                        "Error de conexión"

                },
                style = MaterialTheme.typography.caption1,
                color = if (estado is VinculacionState.CodigoIncorrecto) RojoOscuro else BlancoOpaco,
                textAlign = TextAlign.Center
            )

        }

    }

}