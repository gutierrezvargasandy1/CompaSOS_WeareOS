package mx.edu.utng.compasos_wearos.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun CompaSOSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = androidx.wear.compose.material.Colors(
            primary          = RojoEmergencia,
            primaryVariant   = RojoOscuro,
            secondary        = AzulClaro,
            secondaryVariant = AzulSeguro,
            background       = Negro,
            surface          = GrisOscuro,
            error            = AmarilloAlerta,
            onPrimary        = Blanco,
            onSecondary      = Blanco,
            onBackground     = Blanco,
            onSurface        = Blanco,
            onError          = Negro
        ),
        typography = CompaSSOSTypography,
        content    = content
    )
}