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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import mx.edu.utng.compasos_wearos.R
import mx.edu.utng.compasos_wearos.presentation.theme.*

@Composable
fun PantallaInicio(
    onEmpezarClick: () -> Unit
) {
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

            // ── Título ──────────────────────────────────────────────
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

            // ── Ícono escudo con anillo ──────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AzulSeguro.copy(alpha = 0.30f), Negro)
                        )
                    )
            ) {
                // Anillo exterior
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(color = AzulSeguro.copy(alpha = 0.15f))
                )
                // Ícono escudo — reemplaza con tu drawable real
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Escudo de seguridad",
                    tint = AzulClaro,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Subtítulo ────────────────────────────────────────────
            Text(
                text = "Asistencia cuando\nmás lo necesitas",
                style = MaterialTheme.typography.body2,
                color = BlancoOpaco,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Botón Empezar ────────────────────────────────────────
            Button(
                onClick = onEmpezarClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
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