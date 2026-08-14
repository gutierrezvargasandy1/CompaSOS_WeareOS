package mx.edu.utng.compasos_wearos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import mx.edu.utng.compasos_wearos.helper.FeedbackHelper

@Composable
fun AlertaEnviadaScreen() {
    // Estado de scroll optimizado para pantallas redondas de Wear OS
    val listState = rememberScalingLazyListState()

    val context = LocalContext.current
    val feedback = remember { FeedbackHelper(context) }
    DisposableEffect(Unit) {
        onDispose { feedback.liberar() }
    }

    // ── Vibración continua de 2 segundos al entrar ────────────
    LaunchedEffect(Unit) {
        feedback.vibrarContinua(2000)
    }

    // Colores extraídos exactamente de la imagen proporcionada
    val rojoAlertaTexto = Color(0xFFE57373)      // Rojo/Salmón del título "Alerta enviada"
    val fondoCardBloqueado = Color(0xFF1E1012)   // Fondo guinda/rojizo oscuro
    val bordeCardBloqueado = Color(0xFF3E1F22)   // Borde sutil guinda
    val fondoCardAudio = Color(0xFF0F1A14)       // Fondo verde muy oscuro
    val bordeCardAudio = Color(0xFF1B3224)       // Borde sutil verde
    val verdeAudioTexto = Color(0xFF81C784)      // Verde del texto de audio


    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background), // Tu color Negro (0xFF0A1628)
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 20.dp, start = 10.dp, end = 10.dp, bottom = 20.dp)
    ) {

        // 1. Icono de Check Superior
        item {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .border(3.dp, rojoAlertaTexto, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirmado",
                    tint = rojoAlertaTexto,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2. Título de Alerta
        item {
            Text(
                text = "Alerta enviada",
                color = rojoAlertaTexto,
                style = MaterialTheme.typography.title1.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 3. Burbujas de Familiares (MA, LM, SF)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {

            }
        }

        // 4. Card: Reloj Bloqueado
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(fondoCardBloqueado, RoundedCornerShape(16.dp))
                    .border(1.dp, bordeCardBloqueado, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = rojoAlertaTexto,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Reloj bloqueado.",
                        color = rojoAlertaTexto,
                        style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "No se puede cancelar.",
                        color = rojoAlertaTexto.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.body2.copy(fontSize = 11.sp)
                    )
                }
            }
        }

        // 5. Card: Enviando Audio
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(fondoCardAudio, RoundedCornerShape(16.dp))
                    .border(1.dp, bordeCardAudio, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Audio",
                    tint = verdeAudioTexto,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Enviando audio en",
                        color = verdeAudioTexto,
                        style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "tiempo real a tus familiares...",
                        color = verdeAudioTexto.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.body2.copy(fontSize = 11.sp)
                    )
                }
            }
        }
    }
}