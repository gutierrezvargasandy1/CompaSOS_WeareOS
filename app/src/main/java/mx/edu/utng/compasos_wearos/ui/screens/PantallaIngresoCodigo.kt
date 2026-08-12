package mx.edu.utng.compasos_wearos.ui.screens

import android.app.RemoteInput
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.presentation.theme.*
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel

private const val LARGO_CODIGO = 6
private const val KEY_CODIGO = "key_codigo_vinculacion"

@Composable
fun PantallaIngresoCodigo(
    viewModel: VinculacionViewModel,
    onCancelar: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    var codigoIngresado by remember { mutableStateOf("") }

    // Si el estado pasa a CodigoIncorrecto, limpia el campo para reintentar
    LaunchedEffect(estado) {
        if (estado is VinculacionState.CodigoIncorrecto) {
            codigoIngresado = ""
        }
    }

    // ── Launcher del teclado nativo de Wear OS ──────────────────
    val remoteInputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val bundle: Bundle? = data?.let { RemoteInput.getResultsFromIntent(it) }
        val texto = bundle?.getCharSequence(KEY_CODIGO)?.toString()

        if (!texto.isNullOrBlank()) {
            val limpio = texto.trim().uppercase().take(LARGO_CODIGO)
            codigoIngresado = limpio
            if (limpio.isNotEmpty()) {
                viewModel.ingresarCodigo(limpio)
            }
        }
    }

    fun abrirTecladoNativo() {
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()

        val remoteInput: RemoteInput = RemoteInput.Builder(KEY_CODIGO)
            .setLabel("Código de vinculación")
            .build()

        val remoteInputs: List<RemoteInput> = listOf(remoteInput)

        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
        remoteInputLauncher.launch(intent)
    }

    // Abre el teclado automáticamente al entrar a la pantalla
    LaunchedEffect(Unit) {
        abrirTecladoNativo()
    }

    val config = LocalConfiguration.current
    val screenDp = config.screenWidthDp.dp
    val paddingH = screenDp * 0.10f
    val paddingV = screenDp * 0.06f

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

            // ── Instrucción ───────────────────────────────────────
            item {
                Text(
                    text = "Ingresa el código\nque aparece en tu teléfono",
                    style = MaterialTheme.typography.caption1,
                    color = BlancoOpaco,
                    textAlign = TextAlign.Center
                )
            }

            // ── Código capturado ────────────────────────────────
            item {
                val hayError = estado is VinculacionState.CodigoIncorrecto
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            width = 1.5.dp,
                            color = if (hayError) RojoOscuro else AzulClaro,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .background(AzulSeguro.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = codigoIngresado.ifEmpty { "· · · · · ·" },
                        style = MaterialTheme.typography.title3,
                        color = Blanco,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Mensaje de error ────────────────────────────────────
            if (estado is VinculacionState.CodigoIncorrecto) {
                item {
                    Text(
                        text = "Código incorrecto, inténtalo de nuevo",
                        style = MaterialTheme.typography.caption2,
                        color = RojoOscuro,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Botones ───────────────────────────────────────────
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { abrirTecladoNativo() },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AzulSeguro
                        )
                    ) {
                        Text(
                            text = "Escribir",
                            style = MaterialTheme.typography.button,
                            color = Blanco
                        )
                    }

                    Button(
                        onClick = onCancelar,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = GrisOscuro
                        )
                    ) {
                        Text(
                            text = "Cancelar",
                            style = MaterialTheme.typography.button,
                            color = Blanco
                        )
                    }
                }
            }
        }
    }
}