package mx.edu.utng.compasos_wearos.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** extensión de la propiedad datastore para el contexto de la aplicación, utilizada para gestionar las preferencias persistentes de vinculación. */
val Context.dataStore by preferencesDataStore(name = "compasos_prefs")

/**
 * objeto singleton para consultar y modificar el estado de vinculación del teléfono mediante datastore preferences.
 */
object VinculacionPrefs {

    /** clave de preferencia de tipo booleana que almacena la bandera del estado de vinculación. */
    private val KEY_VINCULADO = booleanPreferencesKey("telefono_vinculado")

    /**
     * observa en tiempo real el estado de vinculación con el teléfono móvil mediante un flujo [Flow].
     *
     * @param context contexto necesario para acceder al datastore de la aplicación.
     * @return un flujo [Flow] que emite true si el teléfono se encuentra vinculado, o false en caso contrario.
     */
    fun estaVinculado(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_VINCULADO] ?: false
        }

    /**
     * actualiza la preferencia guardada en el dispositivo marcando como completado el proceso de vinculación.
     *
     * @param context contexto necesario para realizar la edición persistente en datastore.
     */
    suspend fun marcarVinculado(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VINCULADO] = true
        }
    }
}