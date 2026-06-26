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

    abstract fun configRelojDao(): ConfigRelojDao
    abstract fun eventoPendienteDao(): EventoPendienteDao
    abstract fun historialFrecuenciaDao(): HistorialFrecuenciaDao

    companion object {

        @Volatile
        private var INSTANCE: WearDatabase? = null

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