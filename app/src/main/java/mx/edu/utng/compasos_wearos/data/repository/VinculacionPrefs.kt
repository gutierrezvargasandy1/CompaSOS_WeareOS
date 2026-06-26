package mx.edu.utng.compasos_wearos.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "compasos_prefs")

object VinculacionPrefs {

    private val KEY_VINCULADO = booleanPreferencesKey("telefono_vinculado")

    fun estaVinculado(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_VINCULADO] ?: false
        }

    suspend fun marcarVinculado(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VINCULADO] = true
        }
    }
}