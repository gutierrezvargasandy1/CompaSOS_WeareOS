package mx.edu.utng.compasos_wearos.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings

// Colores del tema (deben coincidir con tu Theme.kt)
private val AzulClaro = Color(0xFF4FC3F7)      // Ícono activo / label activo
private val GrisNavBar = Color(0xFF1A2A3A)     // Fondo de la barra
private val BlancoOpaco = Color(0xFFB0BEC5)    // Label inactivo

@Composable
fun CompaSOSNavBar(
    visible: Boolean,
    onIrAInicio: () -> Unit,
    onIrAAjustes: () -> Unit,
    // Indica cuál tab está activo para resaltar el punto indicador
    tabActiva: String = "inicio",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GrisNavBar)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── BOTÓN INICIO ──────────────────────────────────────────────
            NavBarItem(
                label = "Inicio",
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Inicio",
                        tint = if (tabActiva == "inicio") AzulClaro else BlancoOpaco,
                        modifier = Modifier.size(25.dp)
                    )
                },
                esActivo = tabActiva == "inicio",
                onClick = onIrAInicio
            )

            // ── BOTÓN AJUSTES ─────────────────────────────────────────────
            NavBarItem(
                label = "Ajustes",
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Ajustes",
                        tint = if (tabActiva == "ajustes") AzulClaro else BlancoOpaco,
                        modifier = Modifier.size(25.dp)
                    )
                },
                esActivo = tabActiva == "ajustes",
                onClick = onIrAAjustes
            )
        }
    }
}

/**
 * Ítem individual de la navbar:
 * ícono arriba → label abajo → punto indicador si está activo
 */
@Composable
private fun NavBarItem(
    label: String,
    icon: @Composable () -> Unit,
    esActivo: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Botón transparente — solo muestra el ícono sin fondo
        Button(
            onClick = onClick,
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent
            )
        ) {
            icon()
        }

        // Label del ícono
        Text(
            text = label,
            style = MaterialTheme.typography.caption2,
            fontSize = 9.sp,
            color = if (esActivo) AzulClaro else BlancoOpaco
        )

        // Punto indicador azul (solo visible en la tab activa)
        Spacer(modifier = Modifier.height(4.dp))
        if (esActivo) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(color = AzulClaro, shape = CircleShape)
            )
        } else {
            // Espacio equivalente para mantener el layout estable
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}