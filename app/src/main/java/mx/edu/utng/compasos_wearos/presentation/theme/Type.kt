package mx.edu.utng.compasos_wearos.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Typography

val CompaSSOSTypography = Typography(
    // Título principal — nombre de pantalla
    title1 = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    ),
    // Subtítulo — estado del dispositivo
    title2 = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium
    ),
    // Título pequeño — etiquetas
    title3 = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
    ),
    // Cuerpo principal
    body1 = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    // Cuerpo secundario — descripciones
    body2 = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),
    // Botones
    button = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    ),
    // Texto muy pequeño — metadatos, timestamps
    caption1 = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal
    )
)