package mx.edu.utng.compasos_wearos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.edu.utng.compasos_wearos.data.repository.VinculacionPrefs

class VinculacionViewModel(app: Application) : AndroidViewModel(app) {

    val estaVinculado: StateFlow<Boolean?> = VinculacionPrefs
        .estaVinculado(app)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null   // null = todavía cargando
        )

    fun confirmarVinculacion() {
        viewModelScope.launch {
            VinculacionPrefs.marcarVinculado(getApplication())
        }
    }
}