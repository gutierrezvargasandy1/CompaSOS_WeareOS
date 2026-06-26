package mx.edu.utng.compasos_wearos.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import mx.edu.utng.compasos_wearos.viewmodel.NavBarViewModel

private val FondoPantalla  = Color(0xFF0B1220)
private val VerdeConectado = Color(0xFF43D053)
private val AzulAnilloExt = Color(0xFF152035)
private val AzulAnilloInt = Color(0xFF1C3050)
private val GrisSOS        = Color(0xFF111D2E)
private val BlancoOpaco    = Color(0xFF8A9BB0)

@Composable
fun DashboardScreen(
    onIrAInicio: () -> Unit,
    onIrAAjustes: () -> Unit,
    onDispararSOS: () -> Unit,
    tabActiva: String = "inicio",
    navBarViewModel: NavBarViewModel = viewModel()
) {
    val mostrarNavBar = navBarViewModel.mostrarNavBar.value

    Scaffold(
        timeText = { TimeText() }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)                  // recorte circular real
                .background(FondoPantalla)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        when {
                            dragAmount < -20f -> navBarViewModel.mostrarNavBar()
                            dragAmount >  20f -> navBarViewModel.ocultarNavBar()
                        }
                    }
                }
        ) {
            // d = diámetro real del círculo (el menor entre ancho y alto)
            val d = minOf(maxWidth, maxHeight)

            // Zona segura interior del círculo (el cuadrado inscrito = d * 0.707)
            // Usamos 0.65 para tener margen cómodo
            val zonaSegura = d * 0.60f

            // ── 1. BADGE "● Activo" ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = d * 0.12f, end = d * 0.18f)
                    .background(
                        color = VerdeConectado.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(42)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "● Activo",
                    color = VerdeConectado,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (d.value * 0.045f).sp
                )
            }

            // ── 2. ANILLOS + BOTÓN SOS — centrado ────────────────────────
            // Offset hacia arriba para dejar espacio al texto de ayuda abajo
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = d * 0.0002f),
                contentAlignment = Alignment.Center
            ) {
                // Anillo exterior — cabe dentro de la zona segura
                Box(
                    modifier = Modifier
                        .size(zonaSegura)
                        .background(color = AzulAnilloExt, shape = CircleShape)
                )
                // Anillo interior
                Box(
                    modifier = Modifier
                        .size(zonaSegura * 0.78f)
                        .background(color = AzulAnilloInt, shape = CircleShape)
                )
                // Botón SOS — siempre más chico que el anillo interior
                Button(
                    onClick = onDispararSOS,
                    modifier = Modifier.size(zonaSegura * 0.62f),
                    shape = RoundedCornerShape(zonaSegura * 0.14f),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = GrisSOS
                    )
                ) {
                    Text(
                        text = "SOS",
                        fontWeight = FontWeight.Bold,
                        fontSize = (d.value * 0.12f).sp,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }
            }

            // ── 3. TEXTO DE AYUDA ─────────────────────────────────────────
            Text(
                text = "Mantén presionado 3 seg",
                color = BlancoOpaco,
                fontSize = (d.value * 0.046f).sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = d * 0.10f)
            )

            // ── 4. NAVBAR ─────────────────────────────────────────────────
            CompaSOSNavBar(
                visible = mostrarNavBar,
                onIrAInicio = onIrAInicio,
                onIrAAjustes = onIrAAjustes,
                tabActiva = tabActiva,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}