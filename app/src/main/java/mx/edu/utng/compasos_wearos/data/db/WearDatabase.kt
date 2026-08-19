package mx.edu.utng.compasos_wearos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mx.edu.utng.compasos_wearos.data.dao.ConfigRelojDao
import mx.edu.utng.compasos_wearos.data.dao.EventoPendienteDao
import mx.edu.utng.compasos_wearos.data.dao.HistorialFrecuenciaDao
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj
import mx.edu.utng.compasos_wearos.data.entity.EventoPendiente
import mx.edu.utng.compasos_wearos.data.entity.HistorialFrecuencia

/**
 * base de datos principal de room para el dispositivo wear os compasos.
 * administra la persistencia local de las entidades del reloj y provee el acceso a sus respectivos daos.
 */
@Database(
    entities = [
        ConfigReloj::class,
        EventoPendiente::class,
        HistorialFrecuencia::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WearDatabase : RoomDatabase() {

    /**
     * provee la interfaz dao para gestionar las operaciones de configuración del reloj.
     *
     * @return instancia de [ConfigRelojDao].
     */
    abstract fun configRelojDao(): ConfigRelojDao

    /**
     * provee la interfaz dao para el manejo e historial de eventos pendientes de sincronización.
     *
     * @return instancia de [EventoPendienteDao].
     */
    abstract fun eventoPendienteDao(): EventoPendienteDao

    /**
     * provee la interfaz dao para el registro e historial de lecturas de frecuencia cardiaca.
     *
     * @return instancia de [HistorialFrecuenciaDao].
     */
    abstract fun historialFrecuenciaDao(): HistorialFrecuenciaDao

    /**
     * objeto de compañía que implementa el patrón singleton para garantizar una única instancia de la base de datos.
     */
    companion object {

        /** variable volátil que almacena la instancia única en memoria de la base de datos. */
        @Volatile
        private var INSTANCE: WearDatabase? = null

        /**
         * obtiene la instancia singleton de la base de datos [WearDatabase], construyéndola de manera sincrónicamente segura si no existe.
         *
         * @param context contexto de la aplicación necesario para la creación del almacenamiento persistente de room.
         * @return la instancia activa de [WearDatabase].
         */
        fun getInstance(context: Context): WearDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WearDatabase::class.java,
                    "compaSOS_wear.db"
                ).build().also { INSTANCE = it }
            }
    }
}