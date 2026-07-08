package mx.edu.utng.compasos_wearos.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun CompaSOSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = androidx.wear.compose.material.Colors(
            primary          = RojoEmergencia,   // azul profundo — botón SOS
            primaryVariant   = RojoOscuro,        // variante oscura
            secondary        = AzulClaro,         // cian eléctrico — íconos / logo
            secondaryVariant = AzulSeguro,        // azul medio — vinculación
            background       = Negro,             // azul marino muy oscuro
            surface          = GrisOscuro,        // cards / superficies
            error            = AmarilloAlerta,    // advertencia
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