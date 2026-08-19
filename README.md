# 📱 CompaSOS - Sistema de Alerta y Sincronización Multi-dispositivo

## 📋 Índice

1. [Visión General](#visión-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Estructura de Topics MQTT](#estructura-de-topics-mqtt)
4. [Módulo Teléfono](#módulo-teléfono)
5. [Módulo TV](#módulo-tv)
6. [Módulo Wear OS](#módulo-wear-os)
7. [Guía de Ejecución](#guía-de-ejecución)
8. [Estructura del Proyecto](#estructura-del-proyecto)

---

## Visión General

CompaSOS es un sistema de seguridad y monitoreo multi-dispositivo que integra:

- **Teléfono Android**: Centro de control y sincronización
- **TV Android**: Pantalla de visualización de alertas y ubicaciones
- **Reloj Wear OS**: Dispositivo de alerta SOS portátil

El sistema utiliza **MQTT** como protocolo de comunicación, con el teléfono actuando como el **nodo central** que procesa y distribuye toda la información.

---

## Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────────┐
│                          BROKER MQTT                               │
│                           Mosquitto                               │
│                    (192.168.1.102:1883)                           │
└──────┬──────────────┬──────────────┬──────────────────────────────┘
       │              │              │
       ▼              ▼              ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│  TELÉFONO   │ │     TV      │ │   RELOJ     │
│  Android    │ │  Android TV │ │  Wear OS    │
│             │ │             │ │             │
│ • Centro    │ │ • Visualiza │ │ • Envía SOS │
│ • Configura │ │ • Mapa      │ │ • Detección │
│ • Sincroniza│ │ • Alertas   │ │   de caídas │
└─────────────┘ └─────────────┘ └─────────────┘
```

### Servicios en el Teléfono (Foreground Services)

| Servicio | Función | Topics MQTT |
|----------|---------|-------------|
| `AlertaMqttService` | Recibe SOS del reloj y alertas de familiares | `compasos/alerta/+/sos`, `compasos/familia/{userId}/alerta` |
| `TvSyncService` | Sincroniza ubicaciones y alertas con TVs | `compasos/tv/{tvId}/*` |

---

## Estructura de Topics MQTT

### 1. Topics del Reloj (NO modificados)
```kotlin
compasos/alerta/{dispositivoId}/sos     // SOS desde el reloj
compasos/alerta/{dispositivoId}/audio   // Audio desde el reloj
```

### 2. Topics de Familiares (NO modificados)
```kotlin
compasos/familia/{usuarioId}/alerta     // Alerta a familiar
compasos/familia/{usuarioId}/ubicacion  // Ubicación a familiar
```

### 3. Topics de Vinculación (NUEVOS)
```kotlin
compasos/vinculacion/tv/{codigo}/solicitud   // TV → Teléfono
compasos/vinculacion/tv/{codigo}/respuesta   // Teléfono → TV
```

### 4. Topics de TV (NUEVOS)
```kotlin
compasos/tv/{tvId}/sesion           // Snapshot completo (retained)
compasos/tv/{tvId}/ubicacion/{userId} // Ubicación de un familiar (retained)
compasos/tv/{tvId}/alerta           // Alerta de emergencia
compasos/tv/{tvId}/notificacion     // Notificación informativa
compasos/tv/{tvId}/telefono_estado  // Presencia del teléfono (retained)
compasos/tv/{tvId}/estado           // Presencia de la TV (retained)
```

---

## Módulo Teléfono

### 📄 `config/MqttConfig.kt`
**Propósito:** Define la configuración central de MQTT y los topics del sistema.

```kotlin
package com.utng.compasos_movil.config

/**
 * Configuración central de MQTT para el módulo de teléfono.
 * Define la URL del broker, tiempos de espera y todos los topics utilizados
 * en la comunicación con el reloj, familiares y TVs.
 */
object MqttConfig {

    /**
     * ⚠️ ESTA IP DEBE SER IDÉNTICA A LA DEL MÓDULO TV.
     * URL del broker MQTT (Mosquitto) en la red local.
     * Debe ser accesible desde todos los dispositivos (teléfono, TV, reloj).
     */
    const val BROKER_URL = "tcp://192.168.1.102:1883"

    /** Tiempo máximo de espera para establecer conexión (segundos) */
    const val TIMEOUT_CONEXION = 10

    /** Intervalo de keep-alive para mantener la conexión activa (segundos) */
    const val KEEP_ALIVE = 60

    /** Nivel de calidad de servicio MQTT (1 = al menos una vez) */
    const val QOS = 1

    /** Raíces de topics para cada tipo de dispositivo */
    const val TOPIC_VINCULACION = "compasos/vinculacion"  // Vinculación de dispositivos
    const val TOPIC_DISPOSITIVO = "compasos/dispositivo"  // Estado de dispositivos
    const val TOPIC_ALERTA      = "compasos/alerta"       // Alertas del reloj (NO tocar)
    const val TOPIC_FAMILIA     = "compasos/familia"      // Comunicación con familiares (NO tocar)
    const val TOPIC_TV          = "compasos/tv"           // Sincronización con TV

    // ── Topics hacia la TV ────────────────────────────────────────────────────
    // La TV se suscribe a "compasos/tv/{tvId}/#", así que agregar un subtopic
    // nuevo aquí no obliga a tocar nada del otro lado.

    /**
     * Topic para el snapshot completo de sesión.
     * Contiene: usuario + lista de familiares con sus ubicaciones.
     * @param tvId ID único de la TV destino
     * @return Topic completo para enviar la sesión
     */
    fun topicTvSesion(tvId: String) = "$TOPIC_TV/$tvId/sesion"

    /**
     * Topic para la ubicación de un familiar específico.
     * El usuarioId va EN EL TOPIC para que el broker retenga la última posición de CADA uno.
     * @param tvId ID de la TV destino
     * @param usuarioId ID del familiar cuya ubicación se envía
     * @return Topic completo con el ID del familiar incluido
     */
    fun topicTvUbicacion(tvId: String, usuarioId: String) =
        "$TOPIC_TV/$tvId/ubicacion/$usuarioId"

    /**
     * Topic para alertas de emergencia.
     * NO es retained porque una alerta es un evento puntual, no un estado.
     * @param tvId ID de la TV destino
     * @return Topic para enviar alertas
     */
    fun topicTvAlerta(tvId: String) = "$TOPIC_TV/$tvId/alerta"

    /**
     * Topic para notificaciones informativas.
     * NO es retained para evitar que se repliquen al reiniciar la TV.
     * @param tvId ID de la TV destino
     * @return Topic para enviar notificaciones
     */
    fun topicTvNotificacion(tvId: String) = "$TOPIC_TV/$tvId/notificacion"

    /**
     * Topic para el estado de presencia del teléfono.
     * ES retained y también se usa como Last Will del teléfono.
     * @param tvId ID de la TV destino
     * @return Topic para el estado del teléfono
     */
    fun topicTvEstadoTelefono(tvId: String) = "$TOPIC_TV/$tvId/telefono_estado"

    /** Intervalo entre latidos del teléfono a las TVs (20 segundos) */
    const val INTERVALO_LATIDO_MS = 20_000L
}
```

---

### 📄 `config/MqttManager.kt`
**Propósito:** Cliente MQTT reutilizable con manejo de reconexión automática, mensajes retained y Last Will.

```kotlin
package com.utng.compasos_movil.config

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap

/**
 * Administrador de conexión MQTT para el teléfono.
 * 
 * Características principales:
 * - Reconexión automática al perder la conexión
 * - Re-suscripción automática a todos los topics después de reconectar
 * - Soporte para mensajes retained (estados)
 * - Soporte para Last Will (presencia)
 * - Publicación y suscripción segura con manejo de errores
 */
class MqttManager {

    companion object { private const val TAG = "MqttManager" }

    // Cliente MQTT de Paho
    private var client: MqttClient? = null

    // Mapa de suscripciones activas: topic → callback
    private val suscripciones = ConcurrentHashMap<String, (String, String) -> Unit>()

    // Callback para notificar cuando se establece o restaura la conexión
    private var onConexion: ((reconectado: Boolean) -> Unit)? = null

    /** Indica si el cliente está actualmente conectado al broker */
    val estaConectado: Boolean
        get() = client?.isConnected == false

    /**
     * Registra un callback que se ejecutará cuando la conexión se establezca
     * o se reconecte automáticamente.
     * @param bloque Función a ejecutar, recibe un booleano indicando si fue reconexión
     */
    fun alConectar(bloque: (reconectado: Boolean) -> Unit) { onConexion = bloque }

    /**
     * Establece la conexión con el broker MQTT.
     * @param clientId Identificador único del cliente (se genera automáticamente)
     * @param lwtTopic Topic para el mensaje de Last Will (opcional)
     * @param lwtPayload Payload del mensaje de Last Will (opcional)
     * @throws MqttException Si falla la conexión
     */
    @JvmOverloads
    fun conectar(
        clientId: String = "compasos_movil_${System.currentTimeMillis()}",
        lwtTopic: String? = null,
        lwtPayload: String? = null
    ) {
        if (estaConectado) return
        try {
            val c = MqttClient(MqttConfig.BROKER_URL, clientId, MemoryPersistence())

            // Configurar callbacks para eventos de conexión
            c.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d(TAG, if (reconnect) "🔄 Reconectado" else "✅ Conectado a $serverURI")
                    // Re-aplicar suscripciones en un hilo separado para no bloquear Paho
                    if (reconnect) Thread { reaplicarSuscripciones() }.start()
                    onConexion?.invoke(reconnect)
                }
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "⚠️ Conexión perdida: ${cause?.message}")
                }
                override fun messageArrived(topic: String?, message: MqttMessage?) {}
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            // Configurar opciones de conexión
            c.connect(MqttConnectOptions().apply {
                isCleanSession       = true           // No persistir sesión en el broker
                connectionTimeout    = MqttConfig.TIMEOUT_CONEXION
                keepAliveInterval    = MqttConfig.KEEP_ALIVE
                isAutomaticReconnect = true           // Reintentar automáticamente
                if (lwtTopic != null && lwtPayload != null) {
                    // Configurar Last Will: mensaje que el broker publicará si el cliente cae
                    setWill(lwtTopic, lwtPayload.toByteArray(Charsets.UTF_8), 1, true)
                }
            })

            client = c
            Log.d(TAG, "Conectado a: ${MqttConfig.BROKER_URL} como $clientId")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al conectar: código=${e.reasonCode}, ${e.message}")
            throw e
        }
    }

    /**
     * Re-aplica todas las suscripciones después de una reconexión automática.
     * Este método se ejecuta en un hilo separado para no bloquear el callback de Paho.
     */
    private fun reaplicarSuscripciones() {
        val c = client ?: return
        suscripciones.forEach { (topic, cb) ->
            try {
                c.subscribe(topic, MqttConfig.QOS) { t, msg ->
                    cb(t, String(msg.payload, Charsets.UTF_8))
                }
                Log.d(TAG, "↻ Re-suscrito a: $topic")
            } catch (e: Exception) {
                Log.e(TAG, "Error re-suscribiendo a $topic: ${e.message}")
            }
        }
    }

    /** Desconecta el cliente MQTT y limpia las suscripciones */
    fun desconectar() {
        try {
            client?.takeIf { it.isConnected }?.disconnect()
            Log.d(TAG, "Desconectado del broker")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        } finally {
            suscripciones.clear()
            client = null
        }
    }

    /**
     * Publica un mensaje en un topic MQTT.
     * @param topic Topic donde publicar
     * @param payload Contenido del mensaje (JSON)
     * @param qos Nivel de calidad de servicio (por defecto 1)
     * @param retained Si es true, el broker guarda el mensaje para nuevos suscriptores
     * @throws IllegalStateException Si no hay conexión activa
     */
    @JvmOverloads
    fun publicar(
        topic: String,
        payload: String,
        qos: Int = MqttConfig.QOS,
        retained: Boolean = false
    ) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        c.publish(topic, MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
            this.qos = qos
            this.isRetained = retained
        })
        Log.d(TAG, "► [$topic]${if (retained) "(retained)" else ""}: $payload")
    }

    /**
     * Publica un mensaje de forma segura (atrapa excepciones).
     * @return true si la publicación fue exitosa, false si hubo error
     */
    @JvmOverloads
    fun publicarSeguro(
        topic: String, payload: String,
        qos: Int = MqttConfig.QOS, retained: Boolean = false
    ): Boolean = try {
        publicar(topic, payload, qos, retained); true
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo publicar en $topic: ${e.message}"); false
    }

    /**
     * Suscribe un callback a un topic MQTT.
     * @param topic Topic a suscribir (puede incluir wildcards)
     * @param onMensaje Callback que recibe (topic, payload) cuando llega un mensaje
     * @throws IllegalStateException Si no hay conexión activa
     */
    fun suscribir(topic: String, onMensaje: (topic: String, payload: String) -> Unit) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        suscripciones[topic] = onMensaje
        c.subscribe(topic, MqttConfig.QOS) { t, message ->
            val payload = String(message.payload, Charsets.UTF_8)
            Log.d(TAG, "◄ [$t]: $payload")
            onMensaje(t, payload)
        }
        Log.d(TAG, "Suscrito a: $topic")
    }

    /** Cancela la suscripción a un topic específico */
    fun desuscribir(topic: String) {
        try {
            suscripciones.remove(topic)
            client?.unsubscribe(topic)
            Log.d(TAG, "Desuscrito de: $topic")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desuscribir de $topic: ${e.message}")
        }
    }
}
```

---

### 📄 `config/TvSyncService.kt`
**Propósito:** Servicio en primer plano que sincroniza el teléfono con las TVs. Es el corazón de la comunicación teléfono-TV.

```kotlin
package com.utng.compasos_movil.config

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.data.LocationRepository
import com.utng.compasos_movil.data.entity.DispositivoEntity
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Servicio en primer plano que maneja la sincronización entre el teléfono y las TVs.
 * 
 * Características principales:
 * - Corre en paralelo con AlertaMqttService (clientId MQTT diferente)
 * - Envía un snapshot completo (latido) cada 20 segundos a todas las TVs vinculadas
 * - Publica ubicaciones y alertas al instante (sin esperar el latido)
 * - Usa mensajes retained para que las TVs reciban el estado al encenderse
 * - Configura Last Will para notificar cuando el teléfono se desconecta
 * 
 * API pública estática para que AlertaPhoneRepository pueda empujar datos a la TV:
 * - sincronizarAhora(): Fuerza el envío del snapshot
 * - publicarUbicacion(): Envía ubicación de un familiar en tiempo real
 * - publicarAlerta(): Envía una alerta de emergencia
 * - publicarNotificacion(): Envía una notificación informativa
 */
class TvSyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mqtt  = MqttManager()
    private val fmt   = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private lateinit var db: AppDatabase
    private lateinit var session: SessionManager
    private lateinit var locationRepo: LocationRepository

    @Volatile private var usuarioId: String? = null
    private var jobLatido: Job? = null

    companion object {
        private const val TAG        = "TvSyncSvc"
        private const val CANAL      = "compasos_tv_sync"
        private const val NOTIF_ID   = 9101
        private const val EXTRA_USER = "usuario_id"

        /** Ventana para considerar a alguien "en línea": 5 minutos sin reportar = offline */
        private const val VENTANA_EN_LINEA_MS = 5 * 60_000L

        @Volatile private var instancia: TvSyncService? = null

        /**
         * Inicia el servicio de sincronización con TVs.
         * Se llama desde MainActivity al iniciar sesión o conceder permisos.
         * @param context Contexto de la aplicación
         * @param userId ID del usuario autenticado (opcional)
         */
        fun iniciar(context: Context, userId: String? = null) {
            val intent = Intent(context, TvSyncService::class.java).apply {
                userId?.let { putExtra(EXTRA_USER, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        /** Detiene el servicio de sincronización */
        fun detener(context: Context) {
            context.stopService(Intent(context, TvSyncService::class.java))
        }

        // ── API pública ──────────────────────────────────────────────────────────

        /**
         * Fuerza el envío del snapshot completo a todas las TVs vinculadas.
         * Se usa después de vincular una TV nueva o editar la lista de familiares.
         */
        fun sincronizarAhora() {
            val svc = instancia ?: return
            svc.scope.launch { svc.sincronizarTvs() }
        }

        /**
         * Publica la ubicación en vivo de un familiar a todas las TVs.
         * @param usuarioId ID del familiar cuya ubicación se publica
         * @param latitud Coordenada de latitud
         * @param longitud Coordenada de longitud
         * @param fecha Fecha de la ubicación (opcional, se usa la actual por defecto)
         */
        fun publicarUbicacion(
            usuarioId: String,
            latitud: Double,
            longitud: Double,
            fecha: String? = null
        ) {
            val svc = instancia ?: run {
                Log.w(TAG, "publicarUbicacion ignorada: TvSyncService no corre"); return
            }
            svc.scope.launch { svc.enviarUbicacion(usuarioId, latitud, longitud, fecha) }
        }

        /**
         * Publica una alerta de emergencia a todas las TVs.
         * @param alertaId ID único de la alerta
         * @param tipoAlerta Tipo de alerta (SOS, Caída, etc.)
         * @param descripcion Descripción de la alerta
         * @param emisorId ID de la persona que activó la alerta
         * @param emisorNombre Nombre del emisor (opcional)
         * @param latitud Ubicación del emisor (opcional)
         * @param longitud Ubicación del emisor (opcional)
         */
        fun publicarAlerta(
            alertaId: String,
            tipoAlerta: String,
            descripcion: String?,
            emisorId: String,
            emisorNombre: String?,
            latitud: Double?,
            longitud: Double?
        ) {
            val svc = instancia ?: run {
                Log.w(TAG, "publicarAlerta ignorada: TvSyncService no corre"); return
            }
            svc.scope.launch {
                val nombre = emisorNombre?.takeIf { it.isNotBlank() }
                    ?: svc.nombreDe(emisorId)

                val payload = JSONObject().apply {
                    put("alertaId",     alertaId)
                    put("tipoAlerta",   tipoAlerta)
                    put("descripcion",  descripcion ?: "")
                    put("emisorId",     emisorId)
                    put("emisorNombre", nombre)
                    latitud?.let  { put("latitud",  it) }
                    longitud?.let { put("longitud", it) }
                    put("fecha", svc.fmt.format(Date()))
                }.toString()

                // Sin retained: una alerta es un evento puntual
                svc.paraCadaTv { tv ->
                    svc.mqtt.publicarSeguro(MqttConfig.topicTvAlerta(tv.id), payload)
                }
                // Refresca el snapshot para que la ubicación del emisor llegue al mapa
                svc.sincronizarTvs()
            }
        }

        /**
         * Publica una notificación informativa a todas las TVs.
         * @param notificacionId ID único de la notificación
         * @param alertaId ID de la alerta asociada (opcional)
         * @param titulo Título de la notificación
         * @param mensaje Contenido de la notificación
         * @param tipo Tipo de notificación (info, sos, etc.)
         */
        fun publicarNotificacion(
            notificacionId: String,
            alertaId: String?,
            titulo: String,
            mensaje: String,
            tipo: String = "info"
        ) {
            val svc = instancia ?: return
            svc.scope.launch {
                val payload = JSONObject().apply {
                    put("notificacionId", notificacionId)
                    put("alertaId",       alertaId ?: "")
                    put("titulo",         titulo)
                    put("mensaje",        mensaje)
                    put("tipo",           tipo)
                    put("fecha",          svc.fmt.format(Date()))
                }.toString()
                svc.paraCadaTv { tv ->
                    svc.mqtt.publicarSeguro(MqttConfig.topicTvNotificacion(tv.id), payload)
                }
            }
        }
    }

    // ── Ciclo de vida del servicio ─────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        db           = AppDatabase.getInstance(applicationContext)
        session      = SessionManager(applicationContext)
        locationRepo = LocationRepository(applicationContext)
        instancia    = this

        crearCanal()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, notif(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, notif())
        }

        usuarioId = session.obtenerUsuarioId()
        conectar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nuevo = intent?.getStringExtra(EXTRA_USER) ?: session.obtenerUsuarioId()
        if (nuevo != null && nuevo != usuarioId) {
            usuarioId = nuevo
            Log.d(TAG, "userId actualizado: $nuevo")
            scope.launch { sincronizarTvs() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Enviar estado offline a las TVs antes de morir
        try {
            runBlocking {
                withTimeoutOrNull(1500) {
                    paraCadaTv { tv ->
                        mqtt.publicarSeguro(
                            MqttConfig.topicTvEstadoTelefono(tv.id),
                            JSONObject().put("online", false).toString(),
                            retained = true
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        jobLatido?.cancel()
        scope.cancel()
        mqtt.desconectar()
        instancia = null
        super.onDestroy()
    }

    // ── Conexión MQTT ─────────────────────────────────────────────────────────

    /**
     * Establece la conexión MQTT y configura el Last Will para que el broker
     * notifique a las TVs cuando el teléfono se desconecte inesperadamente.
     */
    private fun conectar() {
        scope.launch {
            var intentos = 0
            while (isActive) {
                try {
                    mqtt.alConectar { reconectado ->
                        scope.launch {
                            anunciarPresencia()
                            sincronizarTvs()
                        }
                        if (reconectado) Log.d(TAG, "Reconectado — snapshot reenviado")
                    }

                    mqtt.conectar(clientId = "compasos_tvsync_${System.currentTimeMillis()}")

                    anunciarPresencia()
                    iniciarLatido()
                    Log.d(TAG, "✅ TvSyncService listo")
                    return@launch

                } catch (e: Exception) {
                    intentos++
                    Log.e(TAG, "Error conectando (intento $intentos): ${e.message}")
                    delay(minOf(5_000L * intentos, 30_000L))
                }
            }
        }
    }

    /**
     * Inicia el latido periódico que envía el snapshot completo a las TVs.
     * También limpia el historial de ubicaciones antiguo al arrancar.
     */
    private fun iniciarLatido() {
        jobLatido?.cancel()
        jobLatido = scope.launch {
            // Limpieza única al arrancar: elimina ubicaciones de hace más de 24 horas
            runCatching {
                db.historialUbicacionDao().limpiarViejas(
                    fmt.format(Date(System.currentTimeMillis() - 24 * 60 * 60_000L))
                )
            }
            while (isActive) {
                try {
                    registrarUbicacionPropia()
                    sincronizarTvs()
                } catch (e: Exception) {
                    Log.e(TAG, "Error en latido: ${e.message}")
                }
                delay(MqttConfig.INTERVALO_LATIDO_MS)
            }
        }
    }

    // ── Publicación de datos ───────────────────────────────────────────────────

    /**
     * Envía el snapshot completo (usuario + familiares) a todas las TVs.
     * El mensaje es retained para que las TVs reciban el estado al encenderse.
     */
    private suspend fun sincronizarTvs() {
        val uid = usuarioId ?: session.obtenerUsuarioId() ?: return
        if (!mqtt.estaConectado) return

        val tvs = db.dispositivoDao().obtenerTvsVinculados(uid)
        if (tvs.isEmpty()) return

        val payload = construirSesion(uid) ?: return
        tvs.forEach { tv ->
            mqtt.publicarSeguro(MqttConfig.topicTvSesion(tv.id), payload, retained = true)
        }
        Log.d(TAG, "↻ Snapshot enviado a ${tvs.size} TV(s)")
    }

    /**
     * Envía la ubicación en vivo de un familiar específico a todas las TVs.
     * Cada familiar tiene su propio topic retained para que el broker guarde la última de cada uno.
     */
    private suspend fun enviarUbicacion(
        idUsuario: String, lat: Double, lng: Double, fecha: String?
    ) {
        val payload = JSONObject().apply {
            put("usuarioId", idUsuario)
            put("latitud",   lat)
            put("longitud",  lng)
            put("fecha",     fecha ?: fmt.format(Date()))
            put("enLinea",   true)
        }.toString()

        paraCadaTv { tv ->
            // retained por familiar: el broker guarda la última posición de CADA uno
            mqtt.publicarSeguro(
                MqttConfig.topicTvUbicacion(tv.id, idUsuario), payload, retained = true
            )
        }
    }

    /**
     * Guarda la ubicación del propio teléfono en la base de datos y la publica a las TVs.
     * Se ejecuta en cada latido para mantener actualizada la ubicación del dueño.
     */
    private suspend fun registrarUbicacionPropia() {
        val uid = usuarioId ?: return
        val loc = locationRepo.obtenerUltimaUbicacion() ?: return
        val ahora = fmt.format(Date())

        runCatching {
            db.historialUbicacionDao().insertar(
                HistorialUbicacionEntity(
                    id        = UUID.randomUUID().toString(),
                    usuarioId = uid,
                    latitud   = loc.latitud,
                    longitud  = loc.longitud,
                    fecha     = ahora
                )
            )
        }
        enviarUbicacion(uid, loc.latitud, loc.longitud, ahora)
    }

    /**
     * Anuncia la presencia del teléfono (online) a todas las TVs.
     * Este mensaje es retained y también se usa como Last Will.
     */
    private suspend fun anunciarPresencia() {
        val online = JSONObject().apply {
            put("online", true)
            put("ts", System.currentTimeMillis())
        }.toString()
        paraCadaTv { tv ->
            mqtt.publicarSeguro(
                MqttConfig.topicTvEstadoTelefono(tv.id), online, retained = true
            )
        }
    }

    /** Ejecuta un bloque para cada TV vinculada al usuario actual */
    private suspend fun paraCadaTv(bloque: (DispositivoEntity) -> Unit) {
        val uid = usuarioId ?: session.obtenerUsuarioId() ?: return
        if (!mqtt.estaConectado) return
        db.dispositivoDao().obtenerTvsVinculados(uid).forEach(bloque)
    }

    // ── Construcción del snapshot de sesión ───────────────────────────────────

    /**
     * Construye el JSON del snapshot completo de sesión.
     * Incluye: datos del usuario + todos sus familiares con ubicación.
     * @param uid ID del usuario dueño de la cuenta
     * @return String JSON con la sesión completa, o null si el usuario no existe
     */
    private suspend fun construirSesion(uid: String): String? {
        return try {
            val usuario = db.usuarioDao().obtenerPorId(uid) ?: run {
                Log.w(TAG, "Usuario $uid todavía no está en Room"); return null
            }

            val familiares = JSONArray()

            // El dueño va primero: la TV lo quiere ver en el mapa también
            familiares.put(jsonFamiliar(uid, usuario.nombre, usuario.apellidoPaterno, "Yo"))

            // Agregar todos los familiares vinculados
            for (rel in db.familiaUsuarioDao().obtenerTodosFamiliares(uid)) {
                val u = db.usuarioDao().obtenerPorId(rel.usuarioId) ?: continue
                familiares.put(
                    jsonFamiliar(u.id, u.nombre, u.apellidoPaterno, rel.rol ?: "Miembro")
                )
            }

            JSONObject().apply {
                put("usuarioId",  usuario.id)
                put("nombre",     usuario.nombre)
                put("email",      usuario.correo)
                put("fecha",      fmt.format(Date()))
                put("familiares", familiares)
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Error construyendo snapshot: ${e.message}", e)
            null
        }
    }

    /**
     * Construye el objeto JSON de un familiar con su ubicación más reciente.
     * @param id ID del familiar
     * @param nombre Nombre del familiar
     * @param apellido Apellido del familiar
     * @param rol Rol en la familia (Yo, Miembro, etc.)
     * @return JSONObject con los datos del familiar
     */
    private suspend fun jsonFamiliar(
        id: String, nombre: String, apellido: String?, rol: String
    ): JSONObject {
        val ubi = db.historialUbicacionDao().obtenerUltimaDeUsuario(id)
        return JSONObject().apply {
            put("usuarioId", id)
            put("nombre",    nombre)
            put("apellido",  apellido ?: "")
            ubi?.latitud?.let  { put("latitud",  it) }
            ubi?.longitud?.let { put("longitud", it) }
            put("fecha",   ubi?.fecha ?: "")
            put("enLinea", esReciente(ubi?.fecha))
            put("rol",     rol)
        }
    }

    /** Obtiene el nombre completo de un usuario por su ID */
    private suspend fun nombreDe(id: String): String {
        val u = db.usuarioDao().obtenerPorId(id) ?: return "Familiar"
        return listOfNotNull(u.nombre, u.apellidoPaterno).joinToString(" ").trim()
            .ifBlank { "Familiar" }
    }

    /**
     * Determina si una fecha es reciente (dentro de la ventana de 5 minutos).
     * @param fecha String con la fecha en formato "yyyy-MM-dd HH:mm:ss"
     * @return true si la fecha es reciente, false en caso contrario
     */
    private fun esReciente(fecha: String?): Boolean {
        if (fecha.isNullOrBlank()) return false
        return try {
            val t = fmt.parse(fecha)?.time ?: return false
            System.currentTimeMillis() - t < VENTANA_EN_LINEA_MS
        } catch (_: Exception) { false }
    }

    // ── Notificación persistente ──────────────────────────────────────────────

    private fun notif() = NotificationCompat.Builder(this, CANAL)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("CompaSOS — TV")
        .setContentText("Enviando ubicaciones y alertas a la pantalla")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(
                        CANAL, "Sincronización con TV",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        }
    }
}
```

---

### 📄 `AlertaPhoneRepository/AlertaPhoneRepository.kt`
**Propósito:** Repositorio que procesa alertas del reloj y las reenvía a familiares y TVs.

```kotlin
package com.utng.compasos_movil.AlertaPhoneRepository

import android.content.Context
import android.util.Log
import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import com.utng.compasos_movil.config.TvSyncService
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.data.LocationRepository
import com.utng.compasos_movil.data.UbicacionActual
import com.utng.compasos_movil.data.dao.AlertaDao
import com.utng.compasos_movil.data.dao.AudioDao
import com.utng.compasos_movil.data.dao.DispositivoDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao
import com.utng.compasos_movil.data.dao.NotificacionDao
import com.utng.compasos_movil.data.dao.UbicacionDao
import com.utng.compasos_movil.data.entity.AlertaEntity
import com.utng.compasos_movil.data.entity.AudioEntity
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.entity.NotificacionEntity
import com.utng.compasos_movil.data.entity.UbicacionEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio que procesa todas las alertas del sistema (reloj, familiares, móvil).
 * 
 * Puntos clave:
 * - Procesa SOS del reloj (sin cambios de lógica)
 * - Procesa alertas de familiares (sin cambios de lógica)
 * - NUEVO: Reenvía todas las alertas a las TVs vinculadas usando TvSyncService
 * - NUEVO: Publica ubicaciones en vivo a las TVs durante el rastreo
 * 
 * El constructor NO cambió: AlertaMqttService lo sigue construyendo igual.
 */
class AlertaPhoneRepository(
    private val alertaDao:         AlertaDao,
    private val ubicacionDao:      UbicacionDao,
    private val audioDao:          AudioDao,
    private val notificacionDao:   NotificacionDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val dispositivoDao:    DispositivoDao,
    private val sessionManager:    SessionManager,
    private val context:           Context
) {
    private val mqtt         = MqttManager()
    private val locationRepo = LocationRepository(context)
    private val fmt          = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Historial de ubicaciones por usuario (NUEVO: para la TV)
    private val historialDao = AppDatabase.getInstance(context).historialUbicacionDao()

    // ── SOS (viene del reloj) — SIN CAMBIOS DE LÓGICA ────────────────────────

    /**
     * Procesa un mensaje SOS recibido del reloj.
     * 
     * Flujo:
     * 1. Guarda la alerta en Room
     * 2. Obtiene la ubicación actual del teléfono
     * 3. Notifica a los familiares (topic compasos/familia/{userId}/alerta)
     * 4. NUEVO: Notifica a las TVs vinculadas
     * 
     * @param payloadJson JSON con los datos del SOS
     * @return AlertaEntity guardada, o null si falló
     */
    suspend fun procesarSOS(payloadJson: String): AlertaEntity? = withContext(Dispatchers.IO) {
        try {
            val json      = JSONObject(payloadJson)
            val usuarioId = sessionManager.obtenerUsuarioId() ?: run {
                Log.e("AlertaPhoneRepo", "No hay sesión activa")
                return@withContext null
            }

            val alertaId = json.optString("alertaId").ifBlank { UUID.randomUUID().toString() }

            val alerta = AlertaEntity(
                id            = alertaId,
                usuarioId     = usuarioId,
                dispositivoId = json.optString("dispositivoId", "desconocido"),
                tipoAlerta    = json.optString("tipoAlerta",    "SOS"),
                descripcion   = json.optString("descripcion",   "Alerta de pánico"),
                estado        = "activa",
                fecha         = json.optString("fecha", fmt.format(Date()))
            )
            alertaDao.insertar(alerta)
            Log.d("AlertaPhoneRepo", "Alerta guardada en Room: $alertaId")

            val ubicacion = locationRepo.obtenerUltimaUbicacion()
            if (ubicacion != null) {
                ubicacionDao.insertar(
                    UbicacionEntity(
                        id        = UUID.randomUUID().toString(),
                        alertaId  = alertaId,
                        latitud   = ubicacion.latitud,
                        longitud  = ubicacion.longitud,
                        precision = null,
                        velocidad = null,
                        fecha     = fmt.format(Date())
                    )
                )
                // ← NUEVO: guardar en historial por usuario (lo que lee la TV)
                guardarEnHistorial(usuarioId, ubicacion.latitud, ubicacion.longitud)
            }

            // Notificar a familiares y TVs
            notificarFamiliares(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            notificarTvs(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            alerta
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error procesando SOS: ${e.message}")
            null
        }
    }

    // ── Notificación a familiares (topic compasos/familia) — SIN CAMBIOS ─────

    /**
     * Notifica a todos los familiares vinculados sobre una alerta.
     * Se publica en compasos/familia/{usuarioId}/alerta para cada familiar.
     */
    private suspend fun notificarFamiliares(
        alerta:    AlertaEntity,
        usuarioId: String,
        latitud:   Double?,
        longitud:  Double?
    ) {
        try {
            val familiares = familiaUsuarioDao.obtenerTodosFamiliares(usuarioId)
            if (familiares.isEmpty()) {
                Log.d("AlertaPhoneRepo", "Sin familiares registrados para notificar")
                return
            }
            Log.d("AlertaPhoneRepo", "Notificando a ${familiares.size} familiar(es)")

            if (!mqtt.estaConectado) mqtt.conectar()

            val nombreEmisor = sessionManager.obtenerUsuarioNombre() ?: ""

            val payloadBase = JSONObject().apply {
                put("alertaId",      alerta.id)
                put("dispositivoId", alerta.dispositivoId)
                put("tipoAlerta",    alerta.tipoAlerta)
                put("descripcion",   alerta.descripcion)
                put("estado",        alerta.estado)
                put("fecha",         alerta.fecha)
                put("emisorId",      usuarioId)
                put("emisorNombre",  nombreEmisor)
                latitud?.let  { put("latitud",  it) }
                longitud?.let { put("longitud", it) }
            }.toString()

            for (familiar in familiares) {
                notificacionDao.insertar(
                    NotificacionEntity(
                        id           = UUID.randomUUID().toString(),
                        alertaId     = alerta.id,
                        destinatario = familiar.usuarioId,
                        tipo         = "SOS",
                        estado       = "enviada",
                        fecha        = fmt.format(Date())
                    )
                )
                mqtt.publicar(
                    "${MqttConfig.TOPIC_FAMILIA}/${familiar.usuarioId}/alerta",
                    payloadBase
                )
                Log.d("AlertaPhoneRepo", "Alerta enviada a familiar: ${familiar.usuarioId}")
            }
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error notificando familiares: ${e.message}")
        }
    }

    // ── Notificación a TVs vinculadas (NUEVO) ─────────────────────────────────

    /**
     * NUEVO: Notifica a todas las TVs vinculadas sobre una alerta.
     * Usa la API pública de TvSyncService para:
     * - Publicar la alerta (evento puntual)
     * - Publicar una notificación informativa
     */
    private suspend fun notificarTvs(
        alerta:    AlertaEntity,
        usuarioId: String,
        latitud:   Double?,
        longitud:  Double?
    ) {
        try {
            val nombre = sessionManager.obtenerUsuarioNombre() ?: ""

            // Publicar alerta de emergencia a las TVs
            TvSyncService.publicarAlerta(
                alertaId     = alerta.id,
                tipoAlerta   = alerta.tipoAlerta ?: "SOS",
                descripcion  = alerta.descripcion ?: "Alerta de emergencia",
                emisorId     = usuarioId,
                emisorNombre = nombre,
                latitud      = latitud,
                longitud     = longitud
            )

            // Publicar notificación informativa
            TvSyncService.publicarNotificacion(
                notificacionId = UUID.randomUUID().toString(),
                alertaId       = alerta.id,
                titulo         = "⚠️ ${alerta.tipoAlerta ?: "SOS"}",
                mensaje        = "$nombre activó una alerta de emergencia",
                tipo           = "sos"
            )
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error notificando TVs: ${e.message}")
        }
    }

    // ── Rastreo continuo de ubicación — SIN CAMBIOS DE LÓGICA ────────────────

    /**
     * Inicia el rastreo en vivo de ubicación para una alerta activa.
     * Cada 3 actualizaciones (aprox. cada 30 segundos) publica la ubicación a:
     * - Familiares (topic compasos/familia/{userId}/ubicacion)
     * - NUEVO: TVs vinculadas (via TvSyncService.publicarUbicacion)
     */
    fun iniciarRastreoEnVivo(alertaId: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val usuarioId = sessionManager.obtenerUsuarioId()
            var contador = 0
            locationRepo.ubicacionEnVivo()
                .catch { e -> Log.e("AlertaPhoneRepo", "Error en rastreo: ${e.message}") }
                .collect { ubicacion ->
                    ubicacionDao.insertar(
                        UbicacionEntity(
                            id        = UUID.randomUUID().toString(),
                            alertaId  = alertaId,
                            latitud   = ubicacion.latitud,
                            longitud  = ubicacion.longitud,
                            precision = null,
                            velocidad = null,
                            fecha     = fmt.format(Date())
                        )
                    )
                    if (++contador % 3 == 0) {
                        usuarioId?.let {
                            guardarEnHistorial(it, ubicacion.latitud, ubicacion.longitud)
                        }
                        publicarUbicacionAFamiliares(alertaId, ubicacion)
                        publicarUbicacionATvs(ubicacion)
                    }
                }
        }
    }

    /** Publica la ubicación en vivo a los familiares */
    private suspend fun publicarUbicacionAFamiliares(
        alertaId: String,
        ubicacion: UbicacionActual
    ) {
        try {
            val usuarioId  = sessionManager.obtenerUsuarioId() ?: return
            val familiares = familiaUsuarioDao.obtenerTodosFamiliares(usuarioId)
            if (familiares.isEmpty()) return

            if (!mqtt.estaConectado) mqtt.conectar()

            val payload = JSONObject().apply {
                put("alertaId",  alertaId)
                put("tipo",      "ubicacion_viva")
                put("usuarioId", usuarioId)
                put("emisorId",  usuarioId)
                put("latitud",   ubicacion.latitud)
                put("longitud",  ubicacion.longitud)
                put("fecha",     fmt.format(Date()))
            }.toString()

            for (familiar in familiares) {
                mqtt.publicar(
                    "${MqttConfig.TOPIC_FAMILIA}/${familiar.usuarioId}/ubicacion",
                    payload
                )
            }
            Log.d("AlertaPhoneRepo", "Ubicación en vivo publicada a ${familiares.size} familiar(es)")
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error publicando ubicación a familiares: ${e.message}")
        }
    }

    /**
     * NUEVO: Publica la ubicación en vivo a todas las TVs vinculadas.
     * Esto hace que el mapa de la TV se actualice en tiempo real durante un SOS.
     */
    private suspend fun publicarUbicacionATvs(ubicacion: UbicacionActual) {
        try {
            val usuarioId = sessionManager.obtenerUsuarioId() ?: return
            TvSyncService.publicarUbicacion(usuarioId, ubicacion.latitud, ubicacion.longitud)
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error publicando ubicación a TVs: ${e.message}")
        }
    }

    // ── Audio del reloj — SIN CAMBIOS ────────────────────────────────────────

    suspend fun procesarAudio(payloadJson: String) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(payloadJson)
            audioDao.insertar(
                AudioEntity(
                    id       = json.optString("id", UUID.randomUUID().toString()),
                    alertaId = json.getString("alertaId"),
                    urlAudio = json.optString("audio"),
                    duracion = null,
                    fecha    = json.optString("fecha", fmt.format(Date()))
                )
            )
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error procesando audio: ${e.message}")
        }
    }

    // ── Alerta recibida como familiar ────────────────────────────────────────

    /**
     * Procesa una alerta recibida de un familiar (el usuario es el destinatario).
     * Reenvía la alerta a las TVs para que aparezca en la pantalla.
     */
    suspend fun procesarAlertaFamiliar(payloadJson: String): AlertaEntity? =
        withContext(Dispatchers.IO) {
            try {
                val json      = JSONObject(payloadJson)
                val usuarioId = sessionManager.obtenerUsuarioId() ?: return@withContext null
                val alertaId  = json.optString("alertaId").ifBlank {
                    UUID.randomUUID().toString()
                }

                alertaDao.obtenerPorId(alertaId)?.let { return@withContext it }

                val emisorId     = json.optString("emisorId")
                val emisorNombre = json.optString("emisorNombre")

                val alerta = AlertaEntity(
                    id            = alertaId,
                    usuarioId     = usuarioId,
                    dispositivoId = json.optString("dispositivoId"),
                    tipoAlerta    = json.optString("tipoAlerta", "SOS"),
                    descripcion   = json.optString("descripcion"),
                    estado        = json.optString("estado", "activa"),
                    fecha         = json.optString("fecha", fmt.format(Date()))
                )
                alertaDao.insertar(alerta)

                notificacionDao.insertar(
                    NotificacionEntity(
                        id           = UUID.randomUUID().toString(),
                        alertaId     = alertaId,
                        destinatario = usuarioId,
                        tipo         = "recibida",
                        estado       = "recibida",
                        fecha        = fmt.format(Date())
                    )
                )

                val lat = json.optDouble("latitud")
                val lng = json.optDouble("longitud")
                if (!lat.isNaN() && !lng.isNaN()) {
                    ubicacionDao.insertar(
                        UbicacionEntity(
                            id        = UUID.randomUUID().toString(),
                            alertaId  = alertaId,
                            latitud   = lat,
                            longitud  = lng,
                            precision = null,
                            velocidad = null,
                            fecha     = fmt.format(Date())
                        )
                    )
                    // ← NUEVO: guardar en historial por usuario para la TV
                    if (emisorId.isNotBlank()) guardarEnHistorial(emisorId, lat, lng)
                }

                // ← NUEVO: reenviar a las TVs
                TvSyncService.publicarAlerta(
                    alertaId     = alertaId,
                    tipoAlerta   = alerta.tipoAlerta ?: "SOS",
                    descripcion  = alerta.descripcion,
                    emisorId     = emisorId.ifBlank { usuarioId },
                    emisorNombre = emisorNombre.ifBlank { null },
                    latitud      = lat.takeIf { !it.isNaN() },
                    longitud     = lng.takeIf { !it.isNaN() }
                )
                TvSyncService.publicarNotificacion(
                    notificacionId = UUID.randomUUID().toString(),
                    alertaId       = alertaId,
                    titulo         = "⚠️ Un familiar necesita ayuda",
                    mensaje        = emisorNombre.ifBlank { "Un familiar" } +
                            " activó su alerta de emergencia",
                    tipo           = "sos"
                )

                alerta
            } catch (e: Exception) {
                Log.e("AlertaPhoneRepo", "Error procesando alerta de familiar: ${e.message}")
                null
            }
        }

    /**
     * Procesa una ubicación en vivo recibida de un familiar.
     * NUEVO: La reenvía a las TVs para actualizar el mapa en tiempo real.
     */
    suspend fun procesarUbicacionFamiliar(payloadJson: String) = withContext(Dispatchers.IO) {
        try {
            val json     = JSONObject(payloadJson)
            val alertaId = json.optString("alertaId")
            if (alertaId.isBlank()) return@withContext

            val lat = json.optDouble("latitud")
            val lng = json.optDouble("longitud")
            if (lat.isNaN() || lng.isNaN()) return@withContext

            val fecha = json.optString("fecha", fmt.format(Date()))

            ubicacionDao.insertar(
                UbicacionEntity(
                    id        = UUID.randomUUID().toString(),
                    alertaId  = alertaId,
                    latitud   = lat,
                    longitud  = lng,
                    precision = null,
                    velocidad = null,
                    fecha     = fecha
                )
            )

            // ← NUEVO: guardar por usuarioId y empujar a la TV
            val emisorId = json.optString("usuarioId")
                .ifBlank { json.optString("emisorId") }

            if (emisorId.isNotBlank()) {
                guardarEnHistorial(emisorId, lat, lng, fecha)
                TvSyncService.publicarUbicacion(emisorId, lat, lng, fecha)
            } else {
                Log.w("AlertaPhoneRepo",
                    "Ubicación de familiar sin usuarioId — no se puede mandar a la TV")
            }

            Log.d("AlertaPhoneRepo", "Ubicación de familiar guardada para alerta: $alertaId")
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error procesando ubicación de familiar: ${e.message}")
        }
    }

    // ── SOS desde el propio móvil — SIN CAMBIOS DE LÓGICA ────────────────────

    suspend fun crearSOSDesdeMovil(): AlertaEntity? = withContext(Dispatchers.IO) {
        try {
            val usuarioId = sessionManager.obtenerUsuarioId() ?: run {
                Log.e("AlertaPhoneRepo", "crearSOSDesdeMovil: sin sesión activa")
                return@withContext null
            }
            val alertaId      = UUID.randomUUID().toString()
            val dispositivoId = "movil_$usuarioId"

            val alerta = AlertaEntity(
                id            = alertaId,
                usuarioId     = usuarioId,
                dispositivoId = dispositivoId,
                tipoAlerta    = "SOS",
                descripcion   = "Alerta de pánico desde el teléfono",
                estado        = "activa",
                fecha         = fmt.format(Date())
            )
            alertaDao.insertar(alerta)

            val ubicacion = locationRepo.obtenerUltimaUbicacion()
            if (ubicacion != null) {
                ubicacionDao.insertar(
                    UbicacionEntity(
                        id        = UUID.randomUUID().toString(),
                        alertaId  = alertaId,
                        latitud   = ubicacion.latitud,
                        longitud  = ubicacion.longitud,
                        precision = null,
                        velocidad = null,
                        fecha     = fmt.format(Date())
                    )
                )
                guardarEnHistorial(usuarioId, ubicacion.latitud, ubicacion.longitud)
            }
            notificarFamiliares(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            notificarTvs(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            alerta
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error en crearSOSDesdeMovil: ${e.message}")
            null
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Guarda una ubicación en el historial por usuario.
     * Esta tabla es la que usa la TV para mostrar la última ubicación de cada persona.
     */
    private suspend fun guardarEnHistorial(
        usuarioId: String, lat: Double, lng: Double, fecha: String = fmt.format(Date())
    ) {
        runCatching {
            historialDao.insertar(
                HistorialUbicacionEntity(
                    id        = UUID.randomUUID().toString(),
                    usuarioId = usuarioId,
                    latitud   = lat,
                    longitud  = lng,
                    fecha     = fecha
                )
            )
        }.onFailure {
            Log.e("AlertaPhoneRepo", "No se pudo guardar en historial: ${it.message}")
        }
    }
}
```

---

### 📄 `TvVinculacionModule/TvVinculacionViewModel.kt`
**Propósito:** Maneja el proceso de vinculación de una TV nueva con el teléfono.

```kotlin
package com.utng.compasos_movil.TvVinculacionModule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import com.utng.compasos_movil.config.TvSyncService
import com.utng.compasos_movil.data.dao.DispositivoDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao
import com.utng.compasos_movil.data.dao.HistorialUbicacionDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.DispositivoEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel que maneja el proceso de vinculación de una TV nueva.
 * 
 * Flujo:
 * 1. El usuario entra a "Vincular TV" en el teléfono → se genera un código de 6 caracteres
 * 2. El usuario teclea ese código en la TV
 * 3. La TV publica una solicitud en compasos/vinculacion/tv/{codigo}/solicitud
 * 4. El teléfono recibe la solicitud y responde con los datos del usuario
 * 5. Se guarda la TV en Room y se envía el snapshot inicial
 * 6. TvSyncService toma el control de la sincronización continua
 */
class TvVinculacionViewModel(
    private val usuarioDao:        UsuarioDao,
    private val dispositivoDao:    DispositivoDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val historialDao:      HistorialUbicacionDao,
    private val sessionManager:    SessionManager
) : ViewModel() {

    companion object { private const val TAG = "TvVinculacionVM" }

    private val mqtt = MqttManager()
    private val fmt  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val _estado = MutableStateFlow<EstadoVinculacionTv>(EstadoVinculacionTv.Inactivo)
    val estado: StateFlow<EstadoVinculacionTv> = _estado.asStateFlow()

    private var codigoActivo: String? = null

    /**
     * Inicia el proceso de vinculación:
     * 1. Genera un código de 6 caracteres alfanuméricos
     * 2. Conecta MQTT
     * 3. Se suscribe al topic de solicitud para ese código
     * 4. Muestra el código en pantalla
     */
    fun iniciarVinculacion() {
        val codigo = generarCodigo()
        codigoActivo = codigo
        _estado.value = EstadoVinculacionTv.Generando(codigo)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!mqtt.estaConectado) {
                    mqtt.conectar(clientId = "compasos_vinc_${System.currentTimeMillis()}")
                }
                val topicSolicitud = "${MqttConfig.TOPIC_VINCULACION}/tv/$codigo/solicitud"
                mqtt.suscribir(topicSolicitud) { _, payload ->
                    viewModelScope.launch(Dispatchers.IO) {
                        procesarSolicitudTv(payload, codigo)
                    }
                }
                Log.d(TAG, "Esperando solicitud de la TV con código: $codigo")
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar vinculación TV: ${e.message}")
                _estado.value = EstadoVinculacionTv.Error("Sin conexión al servidor MQTT")
            }
        }
    }

    /**
     * Procesa la solicitud de vinculación recibida de la TV.
     * @param payload JSON con los datos de la TV (tvId, modelo, fabricante)
     * @param codigoEsperado Código que se esperaba para esta vinculación
     */
    private suspend fun procesarSolicitudTv(payload: String, codigoEsperado: String) {
        if (codigoActivo != codigoEsperado) return
        try {
            val json   = JSONObject(payload)
            val modelo = json.optString("modelo", "Android TV")
            val idTv   = json.optString("tvId",
                json.optString("dispositivoId", "tv_${UUID.randomUUID()}"))

            val userId = sessionManager.obtenerUsuarioId() ?: run {
                _estado.update { EstadoVinculacionTv.Error("Sin sesión activa") }
                return
            }
            val usuario = usuarioDao.obtenerPorId(userId)
            val nombre  = usuario?.nombre ?: sessionManager.obtenerUsuarioNombre() ?: ""
            val email   = usuario?.correo ?: sessionManager.obtenerUsuarioEmail()  ?: ""

            // 1. Responder a la TV con los datos de sesión
            mqtt.publicar(
                "${MqttConfig.TOPIC_VINCULACION}/tv/$codigoEsperado/respuesta",
                JSONObject().apply {
                    put("usuarioId", userId)
                    put("nombre",    nombre)
                    put("email",     email)
                    put("tvId",      idTv)
                    put("aceptada",  true)
                }.toString()
            )

            // 2. Guardar la TV en Room — TvSyncService la lee de aquí
            dispositivoDao.insertar(
                DispositivoEntity(
                    id               = idTv,
                    usuarioId        = userId,
                    tipo             = "tv",
                    modelo           = modelo,
                    fabricante       = json.optString("fabricante").ifBlank { null },
                    numeroSerie      = json.optString("numeroSerie").ifBlank { null },
                    tokenFcm         = null,
                    bateria          = null,
                    conectado        = true,
                    fechaVinculacion = fmt.format(Date())
                )
            )
            Log.d(TAG, "TV guardada en Room: $idTv")

            // 3. Sesión inicial CON ubicaciones reales de todos los familiares
            val familiaresArray = JSONArray()
            familiaresArray.put(jsonFamiliar(userId, nombre, usuario?.apellidoPaterno, "Yo"))

            for (rel in familiaUsuarioDao.obtenerTodosFamiliares(userId)) {
                val u = usuarioDao.obtenerPorId(rel.usuarioId) ?: continue
                familiaresArray.put(
                    jsonFamiliar(u.id, u.nombre, u.apellidoPaterno, rel.rol ?: "Miembro")
                )
            }
            Log.d(TAG, "Sesión con ${familiaresArray.length()} familiar(es)")

            // Publicar el snapshot inicial (retained)
            mqtt.publicar(
                MqttConfig.topicTvSesion(idTv),
                JSONObject().apply {
                    put("usuarioId",  userId)
                    put("nombre",     nombre)
                    put("email",      email)
                    put("fecha",      fmt.format(Date()))
                    put("familiares", familiaresArray)
                }.toString(),
                retained = true
            )

            // 4. Que el servicio empuje el snapshot completo ya mismo
            TvSyncService.sincronizarAhora()

            _estado.update { EstadoVinculacionTv.Exitosa(modelo) }
            Log.d(TAG, "✅ TV vinculada: $modelo")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando solicitud de TV: ${e.message}", e)
            _estado.update { EstadoVinculacionTv.Error("Error al procesar la solicitud de la TV") }
        }
    }

    /**
     * Construye el JSON de un familiar con su última ubicación conocida.
     * Se usa para el snapshot inicial al vincular la TV.
     */
    private suspend fun jsonFamiliar(
        id: String, nombre: String, apellido: String?, rol: String
    ): JSONObject {
        val ubi = historialDao.obtenerUltimaDeUsuario(id)
        return JSONObject().apply {
            put("usuarioId", id)
            put("nombre",    nombre)
            put("apellido",  apellido ?: "")
            ubi?.latitud?.let  { put("latitud",  it) }
            ubi?.longitud?.let { put("longitud", it) }
            put("fecha",   ubi?.fecha ?: "")
            put("enLinea", ubi != null)
            put("rol",     rol)
        }
    }

    /** Limpia el estado de vinculación y cancela la suscripción */
    fun reiniciar() {
        codigoActivo?.let { cod ->
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    mqtt.desuscribir("${MqttConfig.TOPIC_VINCULACION}/tv/$cod/solicitud")
                }
            }
        }
        codigoActivo  = null
        _estado.value = EstadoVinculacionTv.Inactivo
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) { mqtt.desconectar() }
    }

    /** Genera un código alfanumérico de 6 caracteres (sin caracteres confundibles) */
    private fun generarCodigo(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Excluye O,0,I,1
        return (1..6).map { chars.random() }.joinToString("")
    }
}

/** Estados posibles del proceso de vinculación de TV */
sealed class EstadoVinculacionTv {
    object Inactivo : EstadoVinculacionTv()
    data class Generando(val codigo: String) : EstadoVinculacionTv()
    data class Exitosa(val modeloTv: String) : EstadoVinculacionTv()
    data class Error(val mensaje: String) : EstadoVinculacionTv()
}

/** Factory para crear el ViewModel con sus dependencias */
class TvVinculacionViewModelFactory(
    private val usuarioDao:        UsuarioDao,
    private val dispositivoDao:    DispositivoDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val historialDao:      HistorialUbicacionDao,
    private val sessionManager:    SessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TvVinculacionViewModel(
            usuarioDao, dispositivoDao, familiaUsuarioDao, historialDao, sessionManager
        ) as T
}
```

---

## Módulo TV

### 📄 `config/MqttConfig.kt`
**Propósito:** Configuración MQTT para el módulo de TV.

```kotlin
package com.example.compasos_tv.config

/**
 * Configuración MQTT para el módulo de TV.
 * La IP del broker DEBE SER IDÉNTICA a la del teléfono.
 */
object MqttConfig {

    /** ⚠️ TIENE QUE SER LA MISMA IP QUE EN EL TELÉFONO. */
    const val BROKER_URL       = "tcp://192.168.1.102:1883"

    const val TIMEOUT_CONEXION = 10
    const val KEEP_ALIVE       = 60
    const val QOS              = 1

    const val TOPIC_TV          = "compasos/tv"
    const val TOPIC_VINCULACION = "compasos/vinculacion"

    /** UNA sola suscripción con wildcard en vez de tres sueltas */
    fun topicTodoDeEstaTv(tvId: String) = "$TOPIC_TV/$tvId/#"

    fun topicSolicitud(codigo: String) = "$TOPIC_VINCULACION/tv/$codigo/solicitud"
    fun topicRespuesta(codigo: String) = "$TOPIC_VINCULACION/tv/$codigo/respuesta"

    /** Presencia de la propia TV (es su Last Will) */
    fun topicEstadoTv(tvId: String) = "$TOPIC_TV/$tvId/estado"

    /** Si el teléfono no da señales en este tiempo, la UI lo marca desconectado */
    const val TIMEOUT_TELEFONO_MS = 90_000L
}
```

---

### 📄 `config/MqttManager.kt`
**Propósito:** Singleton MQTT para el módulo de TV.

```kotlin
package com.example.compasos_tv.config

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton MQTT para la aplicación de TV.
 * 
 * Refactorizado a singleton para que TvMqttService y VinculacionTvRepository
 * compartan la misma conexión y no se pierdan mensajes.
 */
class MqttManager private constructor() {

    companion object {
        private const val TAG = "TvMqtt"

        /** Instancia única para toda la app de TV */
        val instancia: MqttManager by lazy { MqttManager() }
    }

    private var client: MqttClient? = null
    private val suscripciones = ConcurrentHashMap<String, (String, String) -> Unit>()
    private var onConexion: ((reconectado: Boolean) -> Unit)? = null

    val estaConectado: Boolean
        get() = client?.isConnected == true

    fun alConectar(bloque: (reconectado: Boolean) -> Unit) { onConexion = bloque }

    @Synchronized
    @JvmOverloads
    fun conectar(
        clientId: String = "compasos_tv_${System.currentTimeMillis()}",
        lwtTopic: String? = null,
        lwtPayload: String? = null
    ) {
        if (estaConectado) return

        val c = MqttClient(MqttConfig.BROKER_URL, clientId, MemoryPersistence())

        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d(TAG, if (reconnect) "🔄 Reconectado" else "✅ Conectado a $serverURI")
                if (reconnect) Thread { reaplicarSuscripciones() }.start()
                onConexion?.invoke(reconnect)
            }
            override fun connectionLost(cause: Throwable?) {
                Log.w(TAG, "⚠️ Conexión perdida: ${cause?.message}")
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) {}
            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        c.connect(MqttConnectOptions().apply {
            isCleanSession       = true
            connectionTimeout    = MqttConfig.TIMEOUT_CONEXION
            keepAliveInterval    = MqttConfig.KEEP_ALIVE
            isAutomaticReconnect = true
            if (lwtTopic != null && lwtPayload != null) {
                setWill(lwtTopic, lwtPayload.toByteArray(Charsets.UTF_8), 1, true)
            }
        })

        client = c
        Log.d(TAG, "Conectado al broker como $clientId")
    }

    private fun reaplicarSuscripciones() {
        val c = client ?: return
        suscripciones.forEach { (topic, cb) ->
            try {
                c.subscribe(topic, MqttConfig.QOS) { t, msg ->
                    cb(t, String(msg.payload, Charsets.UTF_8))
                }
                Log.d(TAG, "↻ Re-suscrito a: $topic")
            } catch (e: Exception) {
                Log.e(TAG, "Error re-suscribiendo a $topic: ${e.message}")
            }
        }
    }

    @JvmOverloads
    fun publicar(
        topic: String,
        payload: String,
        qos: Int = MqttConfig.QOS,
        retained: Boolean = false
    ) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        c.publish(topic, MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
            this.qos = qos
            this.isRetained = retained
        })
        Log.d(TAG, "► [$topic]${if (retained) "(retained)" else ""}: $payload")
    }

    @JvmOverloads
    fun publicarSeguro(
        topic: String, payload: String,
        qos: Int = MqttConfig.QOS, retained: Boolean = false
    ): Boolean = try {
        publicar(topic, payload, qos, retained); true
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo publicar en $topic: ${e.message}"); false
    }

    fun suscribir(topic: String, onMensaje: (String, String) -> Unit) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        suscripciones[topic] = onMensaje
        c.subscribe(topic, MqttConfig.QOS) { t, msg ->
            val payload = String(msg.payload, Charsets.UTF_8)
            Log.d(TAG, "◄ [$t]: $payload")
            onMensaje(t, payload)
        }
        Log.d(TAG, "Suscrito a: $topic")
    }

    fun desuscribir(topic: String) {
        try {
            suscripciones.remove(topic)
            client?.unsubscribe(topic)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desuscribir de $topic: ${e.message}")
        }
    }

    fun desconectar() {
        try { client?.takeIf { it.isConnected }?.disconnect() } catch (_: Exception) {}
        suscripciones.clear()
        client = null
    }
}
```

---

### 📄 `services/TvMqttService.kt`
**Propósito:** Servicio en primer plano que mantiene la TV conectada al broker MQTT y procesa los mensajes entrantes.

```kotlin
package com.example.compasos_tv.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.compasos_tv.config.MqttConfig
import com.example.compasos_tv.config.MqttManager
import com.example.compasos_tv.data.entitys.AlertaTvEntity
import com.example.compasos_tv.data.entitys.AppDatabaseTv
import com.example.compasos_tv.data.entitys.FamiliarTvEntity
import com.example.compasos_tv.data.entitys.NotificacionTvEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Objeto global que expone el estado de conexión de la TV.
 * La UI observa estos StateFlows para mostrar indicadores de conexión.
 */
object EstadoTv {
    private val _conectadoBroker = MutableStateFlow(false)
    val conectadoBroker = _conectadoBroker.asStateFlow()

    private val _telefonoEnLinea = MutableStateFlow(false)
    val telefonoEnLinea = _telefonoEnLinea.asStateFlow()

    private val _ultimoMensaje = MutableStateFlow(0L)
    val ultimoMensaje = _ultimoMensaje.asStateFlow()

    private val _ultimoError = MutableStateFlow<String?>(null)
    val ultimoError = _ultimoError.asStateFlow()

    fun setBroker(v: Boolean)   { _conectadoBroker.value = v }
    fun setTelefono(v: Boolean) { _telefonoEnLinea.value = v }
    fun latido()                { _ultimoMensaje.value = System.currentTimeMillis() }
    fun setError(m: String?)    { _ultimoError.value = m }
}

/**
 * Servicio en primer plano de la TV que maneja toda la comunicación MQTT.
 * 
 * Características:
 * - Se suscribe a un único wildcard: compasos/tv/{tvId}/#
 * - Procesa todos los mensajes entrantes (sesión, ubicaciones, alertas, notificaciones)
 * - Guarda los datos en Room para que la UI los observe via Flow
 * - Publica el estado de la TV (online/offline) con Last Will
 * - Vigila la presencia del teléfono (timeout de 90 segundos)
 */
class TvMqttService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mqtt  = MqttManager.instancia
    private lateinit var db: AppDatabaseTv
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val TAG       = "TvMqttSvc"
        private const val CANAL     = "compasos_tv_svc"
        private const val CANAL_SOS = "compasos_tv_sos"
        private const val NOTIF_ID  = 8001

        fun iniciar(context: Context) {
            val intent = Intent(context, TvMqttService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        /** ID único de esta TV basado en Android ID */
        fun obtenerTvId(context: Context): String {
            val androidId = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID
            )
            return "tv_$androidId"
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabaseTv.getInstance(applicationContext)
        crearCanales()
        startForeground(NOTIF_ID, notif())
        conectarYSuscribir()
        vigilarPresenciaTelefono()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        mqtt.desconectar()
        EstadoTv.setBroker(false)
        super.onDestroy()
    }

    // ── Conexión MQTT ─────────────────────────────────────────────────────────

    /**
     * Conecta al broker y se suscribe al wildcard de esta TV.
     * Reintenta con backoff exponencial si falla la conexión.
     */
    private fun conectarYSuscribir() {
        scope.launch {
            val tvId = obtenerTvId(applicationContext)

            mqtt.alConectar { reconectado ->
                EstadoTv.setBroker(true)
                EstadoTv.setError(null)
                scope.launch {
                    mqtt.publicarSeguro(
                        MqttConfig.topicEstadoTv(tvId),
                        JSONObject().put("online", true)
                            .put("ts", System.currentTimeMillis()).toString(),
                        retained = true
                    )
                }
                if (reconectado) Log.d(TAG, "Reconectado — suscripciones restauradas")
            }

            var intentos = 0
            while (isActive) {
                try {
                    mqtt.conectar(
                        clientId   = "compasos_${tvId.take(24)}",
                        lwtTopic   = MqttConfig.topicEstadoTv(tvId),
                        lwtPayload = JSONObject().put("online", false).toString()
                    )

                    // UNA suscripción para todo lo de esta TV
                    mqtt.suscribir(MqttConfig.topicTodoDeEstaTv(tvId)) { topic, payload ->
                        EstadoTv.latido()
                        scope.launch { despachar(topic, payload) }
                    }

                    // Respuesta de vinculación, si ya hay un código guardado
                    db.configTvDao().obtener()?.codigoVinculacion
                        ?.takeIf { it.isNotBlank() }
                        ?.let { cod ->
                            mqtt.suscribir(MqttConfig.topicRespuesta(cod)) { _, payload ->
                                scope.launch { procesarRespuestaVinculacion(payload) }
                            }
                        }

                    EstadoTv.setBroker(true)
                    Log.d(TAG, "✅ Suscrito a topics de TV: $tvId")
                    return@launch

                } catch (e: Exception) {
                    intentos++
                    EstadoTv.setBroker(false)
                    EstadoTv.setError("No se pudo conectar al servidor (${e.message})")
                    Log.e(TAG, "Error MQTT (intento $intentos): ${e.message}")
                    delay(minOf(5_000L * intentos, 30_000L))   // backoff hasta 30 s
                }
            }
        }
    }

    /**
     * Despacha el mensaje según el subtopic (después del tvId).
     * 
     * Ejemplo: compasos/tv/tv_abcd/sesion → sub = "sesion"
     *          compasos/tv/tv_abcd/ubicacion/u123 → sub = "ubicacion"
     */
    private suspend fun despachar(topic: String, payload: String) {
        val sub = topic.split("/").getOrNull(3) ?: return

        when (sub) {
            "sesion"          -> procesarSesion(payload)
            "ubicacion"       -> procesarUbicacion(payload)
            "alerta"          -> procesarAlerta(payload)
            "notificacion"    -> procesarNotificacion(payload)
            "telefono_estado" -> procesarEstadoTelefono(payload)
            "estado"          -> { /* nuestro propio retained */ }
            else              -> Log.d(TAG, "Subtopic no manejado: $sub")
        }
    }

    // ── Procesadores de mensajes ─────────────────────────────────────────────

    /**
     * Procesa el snapshot completo de sesión.
     * Actualiza el perfil del usuario y la lista de familiares.
     * Usa guardarConservandoUbicacion() para no perder las ubicaciones existentes.
     */
    private suspend fun procesarSesion(payloadJson: String) {
        try {
            val json   = JSONObject(payloadJson)
            val nombre = json.optString("nombre")
            val email  = json.optString("email")

            val config = db.configTvDao().obtener()
            if (config != null && config.vinculado) {
                db.configTvDao().guardar(
                    config.copy(
                        nombreUsuario       = nombre,
                        emailUsuario        = email,
                        ultimaActualizacion = System.currentTimeMillis()
                    )
                )
            }

            val familiaresArray = json.optJSONArray("familiares") ?: return
            for (i in 0 until familiaresArray.length()) {
                val f  = familiaresArray.getJSONObject(i)
                val id = f.optString("usuarioId")
                if (id.isBlank()) continue

                // guardarConservandoUbicacion NO pisa la ubicación si no viene en el snapshot
                db.familiarTvDao().guardarConservandoUbicacion(
                    FamiliarTvEntity(
                        usuarioId            = id,
                        nombre               = f.optString("nombre").ifBlank { "Familiar" },
                        apellido             = f.optString("apellido").ifBlank { null },
                        latitud              = f.optDouble("latitud").takeIf { !it.isNaN() },
                        longitud             = f.optDouble("longitud").takeIf { !it.isNaN() },
                        ultimaUbicacionFecha = f.optString("fecha").ifBlank { null },
                        enLinea              = f.optBoolean("enLinea", false)
                    )
                )
            }
            EstadoTv.setTelefono(true)
            Log.d(TAG, "Sesión actualizada: $nombre | ${familiaresArray.length()} familiar(es)")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando sesión: ${e.message}", e)
        }
    }

    /**
     * Procesa una ubicación en vivo de un familiar específico.
     * Actualiza la ubicación y marca al familiar como "en línea".
     */
    private suspend fun procesarUbicacion(payloadJson: String) {
        try {
            val json = JSONObject(payloadJson)
            val id   = json.optString("usuarioId")
            val lat  = json.optDouble("latitud")
            val lng  = json.optDouble("longitud")
            if (id.isBlank() || lat.isNaN() || lng.isNaN()) return

            val fecha = json.optString("fecha", fmt.format(Date()))

            db.familiarTvDao().actualizarUbicacion(id = id, lat = lat, lng = lng, fecha = fecha)
            db.familiarTvDao().marcarEnLinea(id, true)

            EstadoTv.setTelefono(true)
            Log.d(TAG, "📍 Ubicación de $id: $lat, $lng")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando ubicación: ${e.message}", e)
        }
    }

    /**
     * Procesa una alerta de emergencia recibida del teléfono.
     * Guarda en Room y muestra notificación en la TV.
     */
    private suspend fun procesarAlerta(payloadJson: String) {
        try {
            val json         = JSONObject(payloadJson)
            val emisorNombre = json.optString("emisorNombre").ifBlank { "Un familiar" }
            val tipo         = json.optString("tipoAlerta").ifBlank { "SOS" }
            val emisorId     = json.optString("emisorId")
            val lat          = json.optDouble("latitud")
            val lng          = json.optDouble("longitud")
            val fecha        = json.optString("fecha", fmt.format(Date()))

            db.alertaTvDao().insertar(
                AlertaTvEntity(
                    id           = json.optString("alertaId", UUID.randomUUID().toString()),
                    tipo         = tipo,
                    descripcion  = json.optString("descripcion"),
                    emisorNombre = emisorNombre,
                    emisorId     = emisorId,
                    latitud      = lat.takeIf { !it.isNaN() },
                    longitud     = lng.takeIf { !it.isNaN() },
                    fecha        = fecha,
                    leida        = false
                )
            )

            // Si venía con ubicación, aprovéchala para el mapa del emisor
            if (emisorId.isNotBlank() && !lat.isNaN() && !lng.isNaN()) {
                db.familiarTvDao().actualizarUbicacion(emisorId, lat, lng, fecha)
            }

            mostrarNotifAlerta(tipo, emisorNombre)
            EstadoTv.setTelefono(true)
            Log.d(TAG, "🚨 Alerta recibida de $emisorNombre")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando alerta: ${e.message}", e)
        }
    }

    /**
     * Procesa una notificación informativa (no emergencia).
     */
    private suspend fun procesarNotificacion(payloadJson: String) {
        try {
            val json = JSONObject(payloadJson)
            db.notificacionTvDao().insertar(
                NotificacionTvEntity(
                    id       = json.optString("notificacionId", UUID.randomUUID().toString()),
                    alertaId = json.optString("alertaId").ifBlank { null },
                    titulo   = json.optString("titulo").ifBlank { "CompaSOS" },
                    mensaje  = json.optString("mensaje"),
                    tipo     = json.optString("tipo", "info"),
                    fecha    = json.optString("fecha", fmt.format(Date())),
                    leida    = false
                )
            )
            EstadoTv.setTelefono(true)
            Log.d(TAG, "🔔 Notificación recibida")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación: ${e.message}", e)
        }
    }

    /**
     * Procesa el estado de presencia del teléfono (incluye su Last Will).
     */
    private fun procesarEstadoTelefono(payloadJson: String) {
        try {
            val online = JSONObject(payloadJson).optBoolean("online", false)
            EstadoTv.setTelefono(online)
            Log.d(TAG, if (online) "📱 Teléfono en línea" else "📱 Teléfono desconectado")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando estado del teléfono: ${e.message}")
        }
    }

    /**
     * Procesa la respuesta de vinculación del teléfono.
     */
    private suspend fun procesarRespuestaVinculacion(payloadJson: String) {
        try {
            val json = JSONObject(payloadJson)
            db.configTvDao().confirmarVinculacion(
                usuarioId = json.optString("usuarioId"),
                nombre    = json.optString("nombre"),
                email     = json.optString("email"),
                ts        = System.currentTimeMillis()
            )
            Log.d(TAG, "✅ TV vinculada con usuario: ${json.optString("nombre")}")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando vinculación: ${e.message}")
        }
    }

    // ── Vigilancia de presencia ───────────────────────────────────────────────

    /**
     * Vigila si el teléfono ha enviado mensajes recientemente.
     * Si pasa 90 segundos sin mensajes, marca al teléfono como desconectado.
     */
    private fun vigilarPresenciaTelefono() {
        scope.launch {
            while (isActive) {
                delay(20_000)
                val ultimo = EstadoTv.ultimoMensaje.value
                if (ultimo > 0 &&
                    System.currentTimeMillis() - ultimo > MqttConfig.TIMEOUT_TELEFONO_MS
                ) {
                    EstadoTv.setTelefono(false)
                    val corte = fmt.format(
                        Date(System.currentTimeMillis() - MqttConfig.TIMEOUT_TELEFONO_MS)
                    )
                    runCatching { db.familiarTvDao().marcarInactivosAntesDe(corte) }
                }
            }
        }
    }

    // ── Notificaciones ────────────────────────────────────────────────────────

    private fun mostrarNotifAlerta(tipo: String, emisor: String) {
        val notif = NotificationCompat.Builder(this, CANAL_SOS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Alerta $tipo")
            .setContentText("$emisor necesita ayuda")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun notif() = NotificationCompat.Builder(this, CANAL)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("CompaSOS TV")
        .setContentText("Conectado — recibiendo datos del teléfono")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    private fun crearCanales() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CANAL, "Servicio TV", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CANAL_SOS, "Alertas de emergencia",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }
}
```

---

### 📄 `data/entitys/dao/FamiliarTvDao.kt`
**Propósito:** DAO para gestionar la tabla de familiares en la TV.

```kotlin
package com.example.compasos_tv.data.entitys

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamiliarTvDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(familiar: FamiliarTvEntity)

    /**
     * Actualiza la ubicación de un familiar específico.
     * También marca al familiar como "en línea" automáticamente.
     */
    @Query("""
        UPDATE familiares_tv
        SET latitud = :lat,
            longitud = :lng,
            ultimaUbicacionFecha = :fecha,
            enLinea = 1
        WHERE usuarioId = :id
    """)
    suspend fun actualizarUbicacion(id: String, lat: Double, lng: Double, fecha: String)

    /** Observa todos los familiares ordenados por estado en línea y nombre */
    @Query("SELECT * FROM familiares_tv ORDER BY enLinea DESC, nombre ASC")
    fun observarTodos(): Flow<List<FamiliarTvEntity>>

    @Query("UPDATE familiares_tv SET enLinea = 0")
    suspend fun marcarTodosDesconectados()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(familiar: FamiliarTvEntity): Long

    @Query("""
        UPDATE familiares_tv
        SET nombre = :nombre, apellido = :apellido
        WHERE usuarioId = :id
    """)
    suspend fun actualizarPerfil(id: String, nombre: String, apellido: String?)

    @Query("UPDATE familiares_tv SET enLinea = :enLinea WHERE usuarioId = :id")
    suspend fun marcarEnLinea(id: String, enLinea: Boolean)

    /** Los que no reportan desde hace rato dejan de estar "en línea" */
    @Query("UPDATE familiares_tv SET enLinea = 0 WHERE ultimaUbicacionFecha < :antesDe")
    suspend fun marcarInactivosAntesDe(antesDe: String)

    @Query("SELECT COUNT(*) FROM familiares_tv")
    suspend fun contar(): Int

    /**
     * Transacción que guarda un familiar conservando su ubicación.
     * 
     * IMPORTANTE: Este método separa la actualización del perfil de la actualización
     * de ubicación. Solo pisa la ubicación cuando el snapshot trae una de verdad.
     * 
     * Esto evita el bug de "los familiares pierden su ubicación cada pocos segundos"
     * que ocurría con insertar() usando REPLACE.
     */
    @Transaction
    suspend fun guardarConservandoUbicacion(f: FamiliarTvEntity) {
        val insertado = insertarSiNoExiste(f)
        if (insertado == -1L) {
            // El familiar ya existe: actualizar solo el perfil
            actualizarPerfil(f.usuarioId, f.nombre, f.apellido)
            // Actualizar ubicación solo si viene con datos reales
            if (f.latitud != null && f.longitud != null) {
                actualizarUbicacion(
                    id    = f.usuarioId,
                    lat   = f.latitud,
                    lng   = f.longitud,
                    fecha = f.ultimaUbicacionFecha ?: ""
                )
            }
            // Después de actualizarUbicacion (que fuerza enLinea=1), restaurar el estado
            marcarEnLinea(f.usuarioId, f.enLinea)
        }
    }
}
```

---

### 📄 `Screan/TvAlertasScreen.kt`
**Propósito:** Pantalla de bandeja unificada de alertas y notificaciones para la TV.

```kotlin
package com.example.compasos_tv.Screan

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.compasos_tv.data.entitys.AppDatabaseTv
import com.example.compasos_tv.services.EstadoTv
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── Colores del tema oscuro para TV ─────────────────────────────────────────

private val ACard      = Color(0xFF0F1629)
private val ACardNueva = Color(0xFF0F1E2E)
private val ATexto     = Color(0xFFE0E0E0)
private val ASecund    = Color(0xFF9E9E9E)
private val ARojo      = Color(0xFFE53935)
private val AAzul      = Color(0xFF1976D2)
private val AVerde     = Color(0xFF4CAF50)

/**
 * Item unificado para la bandeja: sirve tanto para alertas como para notificaciones.
 * La UI de la TV combina ambas fuentes en una sola lista ordenada por fecha.
 */
data class ItemBandeja(
    val id: String,
    val esAlerta: Boolean,           // true = alerta (emergencia), false = notificación
    val titulo: String,
    val subtitulo: String?,
    val detalle: String?,
    val coords: String?,             // "📍 21.15, -101.68" si tiene ubicación
    val fecha: String,
    val leida: Boolean
)

// ── ViewModel: combina alertas y notificaciones en una sola bandeja ────────

class TvAlertasViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabaseTv.getInstance(app)

    /**
     * Combina las alertas y notificaciones en una sola lista ordenada por fecha.
     * La UI observa Room con Flow, Room despierta automáticamente cuando TvMqttService escribe.
     * Flujo: MQTT → Room → Flow → Compose
     */
    val items: StateFlow<List<ItemBandeja>> = combine(
        db.alertaTvDao().observarTodas(),      // Alertas de emergencia
        db.notificacionTvDao().observarTodas() // Notificaciones informativas
    ) { alertas, notifs ->
        val lista = mutableListOf<ItemBandeja>()

        alertas.forEach { a ->
            lista += ItemBandeja(
                id        = a.id,
                esAlerta  = true,
                titulo    = a.tipo ?: "SOS",
                subtitulo = a.emisorNombre?.let { "De: $it" },
                detalle   = a.descripcion,
                coords    = if (a.latitud != null && a.longitud != null)
                    "📍 %.4f, %.4f".format(a.latitud, a.longitud) else null,
                fecha     = a.fecha,
                leida     = a.leida
            )
        }
        notifs.forEach { n ->
            lista += ItemBandeja(
                id        = n.id,
                esAlerta  = false,
                titulo    = n.titulo,
                subtitulo = null,
                detalle   = n.mensaje,
                coords    = null,
                fecha     = n.fecha,
                leida     = n.leida
            )
        }
        lista.sortedByDescending { it.fecha }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Estado de conexión: (broker, teléfono) para mostrar indicadores en la UI */
    val estadoConexion: StateFlow<Pair<Boolean, Boolean>> = combine(
        EstadoTv.conectadoBroker, EstadoTv.telefonoEnLinea
    ) { broker, telefono -> broker to telefono }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false to false)

    fun marcarLeida(item: ItemBandeja) {
        viewModelScope.launch {
            if (item.esAlerta) db.alertaTvDao().marcarLeida(item.id)
            else               db.notificacionTvDao().marcarLeida(item.id)
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            TvAlertasViewModel(app) as T
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAlertasScreen() {
    val context = LocalContext.current
    val vm: TvAlertasViewModel = viewModel(
        factory = TvAlertasViewModel.Factory(context.applicationContext as Application)
    )
    val items    by vm.items.collectAsState()
    val conexion by vm.estadoConexion.collectAsState()
    val (brokerOk, telefonoOk) = conexion
    val noLeidas = items.count { !it.leida }

    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Encabezado con contador y estado ──────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Notifications, null, tint = ARojo, modifier = Modifier.size(30.dp))
            Text("Alertas y avisos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ATexto)

            // Contador de no leídas (badge rojo)
            if (noLeidas > 0) {
                Box(
                    modifier = Modifier
                        .background(ARojo, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$noLeidas nuevo${if (noLeidas > 1) "s" else ""}",
                        fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Indicadores de conexión (puntos verdes/rojos)
            Punto(brokerOk); Spacer(Modifier.width(6.dp))
            Text("Servidor", fontSize = 12.sp, color = ASecund)
            Spacer(Modifier.width(16.dp))
            Punto(telefonoOk); Spacer(Modifier.width(6.dp))
            Text("Teléfono", fontSize = 12.sp, color = ASecund)
        }

        // ── Contenido: lista o mensaje vacío ──────────────────────────────────
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Notifications, null, tint = ASecund, modifier = Modifier.size(64.dp))
                    Text(
                        when {
                            !brokerOk   -> "Sin conexión al servidor"
                            !telefonoOk -> "Esperando al teléfono…"
                            else        -> "Conectado. Sin alertas registradas"
                        },
                        fontSize = 20.sp, color = ATexto, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Las alertas y avisos de tus familiares aparecerán aquí",
                        fontSize = 14.sp, color = ASecund
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items = items, key = { it.id }) { item ->
                    ItemBandejaCard(item = item, onClick = { vm.marcarLeida(item) })
                }
            }
        }
    }
}

@Composable
private fun Punto(ok: Boolean) {
    Box(
        Modifier.size(9.dp).clip(CircleShape)
            .background(if (ok) AVerde else Color(0xFF424242))
    )
}

/**
 * Card individual para un item de la bandeja.
 * Soporta navegación por D-pad (focus) y clic en Enter/Center.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ItemBandejaCard(item: ItemBandeja, onClick: () -> Unit) {
    var tieneFoco by remember { mutableStateOf(false) }
    val acento = if (item.esAlerta) ARojo else AAzul

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (!item.leida) ACardNueva else ACard)
            .border(
                1.5.dp,
                when {
                    tieneFoco   -> AAzul
                    !item.leida -> acento.copy(alpha = 0.5f)
                    else        -> Color.Transparent
                },
                RoundedCornerShape(14.dp)
            )
            .onFocusChanged { tieneFoco = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) { onClick(); true } else false
            }
            .focusable()
            .padding(20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icono circular con el tipo de item
        Box(
            modifier = Modifier.size(50.dp).background(acento.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (item.esAlerta) Icons.Default.Warning else Icons.Default.Info,
                null, tint = acento, modifier = Modifier.size(26.dp)
            )
        }

        // Contenido principal: título, subtítulo, detalle, coordenadas
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(item.titulo, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = acento)
                if (!item.leida) {
                    Box(
                        modifier = Modifier
                            .background(acento, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("NUEVO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            item.subtitulo?.let { Text(it, fontSize = 14.sp, color = ATexto) }
            item.detalle?.takeIf { it.isNotBlank() }
                ?.let { Text(it, fontSize = 13.sp, color = ASecund) }
            item.coords?.let { Text(it, fontSize = 12.sp, color = ASecund) }
        }

        // Fecha y hora alineadas a la derecha
        Column(horizontalAlignment = Alignment.End) {
            Text(item.fecha.take(10), fontSize = 12.sp, color = ASecund)
            Text(
                item.fecha.drop(11).take(5),
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ATexto
            )
        }
    }
}
```

---

## Módulo Wear OS

### 📄 `config/mqtt/MqttConfig.kt`
**Propósito:** Configuración MQTT para el módulo de reloj.

```kotlin
package mx.edu.utng.compasos_wearos.config.mqtt

/**
 * Configuración MQTT para el módulo de Wear OS.
 * 
 * El reloj utiliza un subconjunto de los topics del sistema:
 * - vinculacion: para emparejarse con el teléfono
 * - alerta: para enviar SOS y audio
 * 
 * No utiliza mensajes retained ni Last Will.
 */
object MqttConfig {
    /**
     * Emulador Android → 10.0.2.2 (mapea al localhost de la máquina)
     * Dispositivo físico → IP local de la máquina en la misma WiFi
     * 
     * Ejemplo: "tcp://192.168.1.100:1883"
     */
    const val BROKER_URL = "tcp://192.168.1.102:1883"

    const val TIMEOUT_CONEXION = 10   // segundos
    const val KEEP_ALIVE       = 60   // segundos
    const val QOS              = 1    // At least once

    // Topics
    const val TOPIC_VINCULACION = "compasos/vinculacion"
    const val TOPIC_DISPOSITIVO = "compasos/dispositivo"
    const val TOPIC_ALERTA      = "compasos/alerta"
}
```

---

### 📄 `config/mqtt/MqttManager.kt`
**Propósito:** Administrador MQTT para el reloj.

```kotlin
package mx.edu.utng.compasos_wearos.config.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * Administrador MQTT para el módulo de Wear OS.
 * 
 * Características:
 * - Versión simplificada: no usa retained ni Last Will
 * - No tiene reconexión automática (debe hacerse manualmente)
 * - Utiliza MemoryPersistence
 * - QoS 1 por defecto
 */
class MqttManager {

    companion object {
        private const val TAG = "MqttManager"
    }

    private var client: MqttClient? = null

    val estaConectado: Boolean
        get() = client?.isConnected == true

    /** Llamar siempre desde Dispatchers.IO */
    fun conectar() {
        if (estaConectado) return

        try {
            val clientId = "compasos_movil_${System.currentTimeMillis()}"

            client = MqttClient(
                MqttConfig.BROKER_URL,
                clientId,
                MemoryPersistence()
            )

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = MqttConfig.TIMEOUT_CONEXION
                keepAliveInterval = MqttConfig.KEEP_ALIVE
                isAutomaticReconnect = false
            }

            client!!.connect(options)

            Log.d(
                TAG,
                "Conectado a: ${MqttConfig.BROKER_URL}"
            )

        } catch (e: MqttException) {

            Log.e(
                TAG,
                "Error al conectar: código=${e.reasonCode}, ${e.message}"
            )

            throw e
        }
    }

    fun desconectar() {
        try {
            client
                ?.takeIf { it.isConnected }
                ?.disconnect()

            Log.d(TAG, "Desconectado del broker")

        } catch (e: MqttException) {

            Log.e(
                TAG,
                "Error al desconectar: ${e.message}"
            )

        } finally {
            client = null
        }
    }

    /** Llamar desde Dispatchers.IO */
    fun publicar(
        topic: String,
        payload: String,
        qos: Int = MqttConfig.QOS
    ) {
        val c = client
            ?: throw IllegalStateException("MQTT no conectado")

        val message = MqttMessage(
            payload.toByteArray(Charsets.UTF_8)
        ).apply {
            this.qos = qos
        }

        c.publish(topic, message)

        Log.d(
            TAG,
            "► [$topic]: $payload"
        )
    }

    /**
     * El callback se ejecuta en el hilo interno de Paho.
     * Desde el callback se recomienda utilizar viewModelScope.launch
     * para actualizar el estado.
     */
    fun suscribir(
        topic: String,
        onMensaje: (
            topic: String,
            payload: String
        ) -> Unit
    ) {
        val c = client
            ?: throw IllegalStateException("MQTT no conectado")

        c.subscribe(topic, MqttConfig.QOS) { t, message ->

            val payload = String(
                message.payload,
                Charsets.UTF_8
            )

            Log.d(
                TAG,
                "◄ [$t]: $payload"
            )

            onMensaje(t, payload)
        }

        Log.d(
            TAG,
            "Suscrito a: $topic"
        )
    }

    fun desuscribir(topic: String) {
        try {

            client?.unsubscribe(topic)

            Log.d(
                TAG,
                "Desuscrito de: $topic"
            )

        } catch (e: MqttException) {

            Log.e(
                TAG,
                "Error al desuscribir de $topic: ${e.message}"
            )
        }
    }
}
```

---

### 📄 `data/entity/ConfigReloj.kt`
**Propósito:** Entidad Room que almacena la configuración del reloj.

```kotlin
package mx.edu.utng.compasos_wearos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla de configuración del reloj.
 * 
 * Se utiliza una única fila con id = 1.
 * Almacena:
 * - Configuración de emergencia
 * - Configuración de detección de caídas
 * - Configuración del sensor cardíaco
 * - Modo discreto
 * - Identificador del dispositivo vinculado
 * - Código MQTT pendiente de confirmación
 */
@Entity(tableName = "config_reloj")
data class ConfigReloj(

    @PrimaryKey
    val id: Int = 1,

    /** Tiempo de presión para activar pánico (ms) */
    val tiempoPanicoMs: Int = 3000,

    /** Habilitar detección por triple tap */
    val tripleTabActivado: Boolean = true,

    /** Habilitar detección de caídas */
    val deteccionCaidasActiva: Boolean = true,

    /** Sensibilidad para detección de caídas */
    val sensibilidadCaida: Float = 2.5f,

    /** Umbral de BPM para alerta cardíaca */
    val umbralBpmAlerta: Int = 130,

    /** Intervalo de monitoreo cardíaco (segundos) */
    val intervaloMonitoreoSeg: Int = 10,

    /** Habilitar grabación de audio al activar emergencia */
    val audioAlActivarEmergencia: Boolean = true,

    /** Duración máxima del audio (segundos) */
    val duracionAudioSeg: Int = 30,

    /** Modo discreto: vibración en lugar de sonidos visibles */
    val modoDiscreto: Boolean = true,

    /** ID del dispositivo vinculado (teléfono) */
    val deviceIdVinculado: String = "",

    /** Nombre del dispositivo vinculado */
    val nombreDispositivoVinculado: String = "",

    /** Código MQTT pendiente de confirmar */
    val codigoVinculacion: String = "",

    /** Timestamp de la última actualización */
    val ultimaActualizacion: Long = System.currentTimeMillis()
)
```

---

### 📄 `data/repository/VinculacionRepository.kt`
**Propósito:** Repositorio que maneja el flujo de vinculación del reloj mediante MQTT.

```kotlin
package mx.edu.utng.compasos_wearos.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.edu.utng.compasos_wearos.config.mqtt.MqttConfig
import mx.edu.utng.compasos_wearos.config.mqtt.MqttManager
import mx.edu.utng.compasos_wearos.data.VinculacionEvent
import mx.edu.utng.compasos_wearos.data.VinculacionManager
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.data.dao.ConfigRelojDao
import mx.edu.utng.compasos_wearos.data.db.WearDatabase
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj
import org.json.JSONObject

/**
 * Repositorio de vinculación para el reloj.
 * 
 * Flujo MQTT:
 * 1. El teléfono publica un código en compasos/vinculacion/{codigo}/solicitud
 * 2. El reloj recibe el código y lo guarda en Room
 * 3. El usuario introduce el código en la interfaz
 * 4. Se valida contra el código guardado
 * 5. Si coincide, se publica la respuesta en compasos/vinculacion/{codigo}/respuesta
 * 6. El teléfono confirma la vinculación
 */
class VinculacionRepository(
    private val context: Context
) {

    private val dao: ConfigRelojDao =
        WearDatabase
            .getInstance(context)
            .configRelojDao()

    private val mqtt =
        MqttManager()

    val vinculacionManager =
        VinculacionManager()

    private val repoScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    fun observarConfig(): Flow<ConfigReloj?> =
        dao.observarConfig()

    suspend fun guardarVinculacion(
        nodeId: String,
        nombreTelefono: String
    ) {

        val configActual =
            dao.obtenerConfig()
                ?: ConfigReloj()

        dao.guardarConfig(
            configActual.copy(
                deviceIdVinculado = nodeId,
                nombreDispositivoVinculado = nombreTelefono,
                codigoVinculacion = "",
                ultimaActualizacion = System.currentTimeMillis()
            )
        )
    }

    suspend fun borrarVinculacion() {

        val configActual =
            dao.obtenerConfig()
                ?: ConfigReloj()

        dao.guardarConfig(
            configActual.copy(
                deviceIdVinculado = "",
                nombreDispositivoVinculado = "",
                codigoVinculacion = "",
                ultimaActualizacion = System.currentTimeMillis()
            )
        )
    }

    /**
     * Detecta el teléfono mediante Wearable Node API (mecanismo anterior).
     * Se mantiene para compatibilidad con el flujo de vinculación por Bluetooth.
     */
    suspend fun detectarYGuardarTelefono(
        context: Context
    ) {

        vinculacionManager
            .detectarTelefonoConectado(context)

        val estadoActual =
            vinculacionManager.estado.value

        if (
            estadoActual is
                    VinculacionState.SolicitudRecibida &&
            estadoActual.codigoEsperado == null
        ) {

            guardarVinculacion(
                nodeId =
                    estadoActual.nombreTelefono,

                nombreTelefono =
                    estadoActual.nombreTelefono
            )
        }
    }

    // ══════════════════════════════════════════════════════════
    // FLUJO MQTT CON CÓDIGO (NUEVO)
    // ══════════════════════════════════════════════════════════

    /**
     * Escucha solicitudes de vinculación entrantes en el topic wildcard:
     * compasos/vinculacion/+/solicitud
     * 
     * Al recibir una, extrae el código y lo guarda en Room, luego notifica
     * al VinculacionManager para que la UI muestre la pantalla de ingreso de código.
     */
    suspend fun escucharSolicitudesVinculacion() {

        withContext(Dispatchers.IO) {

            try {

                if (!mqtt.estaConectado) {
                    mqtt.conectar()
                }

                mqtt.suscribir(
                    "${MqttConfig.TOPIC_VINCULACION}/+/solicitud"
                ) { _, payload ->

                    repoScope.launch {

                        try {

                            val json =
                                JSONObject(payload)

                            val codigo =
                                json.getString("code")

                            val usuarioId =
                                json.optString(
                                    "usuarioId",
                                    ""
                                )

                            guardarCodigoPendiente(
                                codigo
                            )

                            vinculacionManager
                                .procesarEvento(
                                    VinculacionEvent
                                        .SolicitudMqttRecibida(
                                            codigo,
                                            usuarioId
                                        )
                                )

                        } catch (e: Exception) {
                            // Payload inválido, se ignora
                        }
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "VinculacionRepo",
                    "No se pudo conectar al broker: ${e.message}"
                )

                vinculacionManager.error(
                    "Sin conexión al broker MQTT"
                )
            }
        }
    }

    private suspend fun guardarCodigoPendiente(
        codigo: String
    ) {

        if (dao.obtenerConfig() == null) {
            dao.guardarConfig(ConfigReloj())
        }

        dao.setCodigoVinculacion(codigo)
    }

    /**
     * Valida el código introducido por el usuario contra el código almacenado en Room.
     * @param codigoIngresado Código que el usuario escribió en la UI
     * @return true si el código coincide y la respuesta se publicó exitosamente
     */
    suspend fun confirmarCodigo(
        codigoIngresado: String
    ): Boolean {

        val codigoGuardado =
            dao.obtenerConfig()
                ?.codigoVinculacion
                .orEmpty()

        if (codigoGuardado.isBlank()) {
            return false
        }

        val coincide =
            codigoIngresado
                .trim()
                .equals(
                    codigoGuardado.trim(),
                    ignoreCase = true
                )

        if (!coincide) {

            vinculacionManager.procesarEvento(
                VinculacionEvent.CodigoIncorrecto
            )

            return false
        }

        return withContext(Dispatchers.IO) {

            try {

                if (!mqtt.estaConectado) {
                    mqtt.conectar()
                }

                val deviceId =
                    "wear_${ Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    )}"

                val respuesta =
                    JSONObject().apply {

                        put("deviceId", deviceId)
                        put("tipo", "reloj")
                        put("modelo", Build.MODEL)
                        put("fabricante", Build.MANUFACTURER)

                    }.toString()

                mqtt.publicar(
                    "${MqttConfig.TOPIC_VINCULACION}/$codigoGuardado/respuesta",
                    respuesta
                )

                guardarVinculacion(
                    nodeId = deviceId,
                    nombreTelefono = "Teléfono vinculado"
                )

                vinculacionManager
                    .vinculacionExitosa()

                true

            } catch (e: Exception) {

                Log.e(
                    "VinculacionRepo",
                    "No se pudo enviar la respuesta: ${e.message}"
                )

                vinculacionManager.error(
                    "Sin conexión, inténtalo de nuevo"
                )

                false
            }
        }
    }

    suspend fun cancelarVinculacionPendiente() {

        dao.limpiarCodigoVinculacion()

        vinculacionManager.procesarEvento(
            VinculacionEvent.Cancelar
        )
    }

    suspend fun setModoDiscreto(
        activo: Boolean
    ) {

        if (dao.obtenerConfig() == null) {
            dao.guardarConfig(ConfigReloj())
        }

        dao.setModoDiscreto(activo)
    }

    suspend fun setTiempoPanico(
        segundos: Int
    ) {

        if (dao.obtenerConfig() == null) {
            dao.guardarConfig(ConfigReloj())
        }

        dao.setTiempoPanico(segundos * 1000)
    }
}
```

---

### 📄 `data/repository/AlertaWearRepository.kt`
**Propósito:** Repositorio que maneja el envío de alertas SOS desde el reloj.

```kotlin
package mx.edu.utng.compasos_wearos.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.edu.utng.compasos_wearos.config.mqtt.MqttConfig
import mx.edu.utng.compasos_wearos.config.mqtt.MqttManager
import mx.edu.utng.compasos_wearos.data.db.WearDatabase
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio de alertas para el reloj.
 * 
 * El reloj actúa como control remoto de emergencia:
 * - Solo envía el trigger SOS (sin ubicación)
 * - El teléfono se encarga de obtener la ubicación y completar la alerta
 * - También puede enviar audio grabado desde el micrófono
 */
class AlertaWearRepository(
    private val context: Context
) {

    private val mqtt =
        MqttManager()

    private val dao =
        WearDatabase
            .getInstance(context)
            .configRelojDao()

    private val fmt =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        )

    /**
     * Envía un trigger SOS al teléfono.
     * 
     * Flujo:
     * 1. Genera un alertaId único
     * 2. Obtiene el deviceId del reloj
     * 3. Publica en compasos/alerta/{deviceId}/sos
     * 4. El teléfono recibe, obtiene ubicación y procesa la alerta completa
     * 
     * @return Pair(alertaId, dispositivoId) si se envió exitosamente, null en caso contrario
     */
    @SuppressLint("HardwareIds")
    suspend fun enviarSOS(): Pair<String, String>? =
        withContext(Dispatchers.IO) {

            try {

                if (!mqtt.estaConectado) {
                    mqtt.conectar()
                }

                val config =
                    dao.obtenerConfig()
                        ?: run {

                            Log.e(
                                "AlertaWearRepo",
                                "Sin config en Room"
                            )

                            return@withContext null
                        }

                val dispositivoId =
                    config.deviceIdVinculado.ifBlank {

                        "wear_${ Settings.Secure.getString(
                            context.contentResolver,
                            Settings.Secure.ANDROID_ID
                        )}"
                    }

                val alertaId =
                    UUID.randomUUID().toString()

                val payload =
                    JSONObject().apply {

                        put("alertaId", alertaId)
                        put("dispositivoId", dispositivoId)
                        put("tipoAlerta", "SOS")
                        put("descripcion", "Alerta de pánico desde el reloj")
                        put("estado", "activa")
                        put("fecha", fmt.format(Date()))

                    }.toString()

                mqtt.publicar(
                    "${MqttConfig.TOPIC_ALERTA}/$dispositivoId/sos",
                    payload
                )

                Log.d(
                    "AlertaWearRepo",
                    "Trigger SOS publicado: $alertaId"
                )

                Pair(alertaId, dispositivoId)

            } catch (e: Exception) {

                Log.e(
                    "AlertaWearRepo",
                    "Error al enviar SOS: ${e.message}"
                )

                null
            }
        }

    /**
     * Publica un chunk de audio grabado desde el micrófono del reloj.
     * @param alertaId ID de la alerta a la que pertenece el audio
     * @param dispositivoId ID del dispositivo que envía el audio
     * @param base64Chunk Audio codificado en base64
     */
    suspend fun publicarChunkAudio(
        alertaId: String,
        dispositivoId: String,
        base64Chunk: String
    ) = withContext(Dispatchers.IO) {

        try {

            if (!mqtt.estaConectado) {
                mqtt.conectar()
            }

            val payload =
                JSONObject().apply {

                    put("id", UUID.randomUUID().toString())
                    put("alertaId", alertaId)
                    put("audio", base64Chunk)
                    put("fecha", fmt.format(Date()))

                }.toString()

            mqtt.publicar(
                "${MqttConfig.TOPIC_ALERTA}/$dispositivoId/audio",
                payload
            )

        } catch (e: Exception) {

            Log.e(
                "AlertaWearRepo",
                "Error al publicar audio: ${e.message}"
            )
        }
    }
}
```

---

### 📄 `viewmodel/VinculacionViewModel.kt`
**Propósito:** ViewModel que coordina la vinculación, detección de movimientos y envío de SOS.

```kotlin
package mx.edu.utng.compasos_wearos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.edu.utng.compasos_wearos.data.VinculacionEvent
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj
import mx.edu.utng.compasos_wearos.data.repository.AlertaWearRepository
import mx.edu.utng.compasos_wearos.data.repository.VinculacionPrefs
import mx.edu.utng.compasos_wearos.data.repository.VinculacionRepository
import mx.edu.utng.compasos_wearos.services.MovimientoDetector

/**
 * ViewModel principal del reloj.
 * 
 * Coordina:
 * - Vinculación con el teléfono (MQTT + Bluetooth)
 * - Detección de movimientos bruscos (caídas)
 * - Envío de SOS manual o automático
 * - Modo discreto (vibración vs sonido)
 * - Estado de vinculación (DataStore + Room)
 */
class VinculacionViewModel(
    app: Application
) : AndroidViewModel(app) {

    private val repo =
        VinculacionRepository(app)

    private val movimientoDetector =
        MovimientoDetector(app)

    val estado: StateFlow<VinculacionState> =
        repo.vinculacionManager.estado

    private val alertaRepo =
        AlertaWearRepository(app)

    // Para trackear la alerta activa
    private var alertaActivaId: String? = null
    private var alertaDispositivoId: String? = null

    val estaVinculado: StateFlow<Boolean?> =
        VinculacionPrefs
            .estaVinculado(app)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    val configReloj: StateFlow<ConfigReloj?> =
        repo.observarConfig()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    // ── Overlay: movimiento brusco detectado ────────────────────────────────

    private val _mostrarAlertaMovimiento =
        MutableStateFlow(false)

    val mostrarAlertaMovimiento: StateFlow<Boolean> =
        _mostrarAlertaMovimiento

    // ── Overlay: alerta ya enviada ──────────────────────────────────────────

    private val _alertaEnviada =
        MutableStateFlow(false)

    val alertaEnviada: StateFlow<Boolean> =
        _alertaEnviada

    init {

        // ── NUEVO: empieza a escuchar solicitudes MQTT ──────────────────────

        viewModelScope.launch {

            repo.escucharSolicitudesVinculacion()
        }

        // ── Observa configuración para activar/desactivar detector ──────────

        viewModelScope.launch {

            configReloj.collect { config ->

                if (config == null) {
                    return@collect
                }

                if (config.modoDiscreto) {

                    movimientoDetector.iniciar(
                        umbral = config.sensibilidadCaida.takeIf { it >= 18f } ?: 22f
                    )

                } else {

                    movimientoDetector.detener()
                }
            }
        }

        // ── Escucha disparos del detector de movimiento ─────────────────────

        viewModelScope.launch {

            movimientoDetector
                .movimientoErratico
                .collect {

                    if (
                        !_mostrarAlertaMovimiento.value &&
                        !_alertaEnviada.value &&
                        estaVinculado.value == true
                    ) {

                        _mostrarAlertaMovimiento.value = true
                    }
                }
        }
    }

    // ── Movimiento inusual ────────────────────────────────────────────────────

    fun descartarAlertaMovimiento() {
        _mostrarAlertaMovimiento.value = false
    }

    fun confirmarAlertaMovimiento() {
        _mostrarAlertaMovimiento.value = false
        dispararSOS()
    }

    // ── SOS manual o por movimiento ──────────────────────────────────────────

    fun dispararSOS() {

        viewModelScope.launch {

            val resultado =
                alertaRepo.enviarSOS()

            if (resultado != null) {

                alertaActivaId =
                    resultado.first

                alertaDispositivoId =
                    resultado.second

                iniciarUbicacionPeriodica(
                    resultado.first,
                    resultado.second
                )
            }

            _alertaEnviada.value = true
        }
    }

    private fun iniciarUbicacionPeriodica(
        alertaId: String,
        dispositivoId: String
    ) {
        // Espacio reservado para enviar ubicación periódica si el reloj tuviera GPS
        viewModelScope.launch {
            repeat(10) {
                delay(30_000L)
                // Actualmente el reloj no envía ubicación; el teléfono la obtiene
            }
        }
    }

    fun ocultarAlertaEnviada() {
        _alertaEnviada.value = false
    }

    // ── Flujo anterior: Wearable Node API (Bluetooth) ──────────────────────

    fun iniciarEsperaBluetooth() {

        viewModelScope.launch {

            while (true) {

                repo.detectarYGuardarTelefono(
                    getApplication()
                )

                delay(3_000)
            }
        }
    }

    // ── Flujo anterior: aceptar sin código ──────────────────────────────────

    fun aceptarVinculacion() {

        viewModelScope.launch {

            val estadoActual =
                estado.value

            if (
                estadoActual is
                        VinculacionState.SolicitudRecibida &&
                estadoActual.codigoEsperado == null
            ) {

                repo.guardarVinculacion(
                    nodeId = estadoActual.nombreTelefono,
                    nombreTelefono = estadoActual.nombreTelefono
                )

                repo.vinculacionManager.vinculacionExitosa()

                VinculacionPrefs.marcarVinculado(
                    getApplication()
                )
            }
        }
    }

    // ── NUEVO: flujo MQTT con código ─────────────────────────────────────────

    fun ingresarCodigo(codigo: String) {

        viewModelScope.launch {

            val exito =
                repo.confirmarCodigo(codigo)

            if (exito) {

                VinculacionPrefs.marcarVinculado(
                    getApplication()
                )
            }

            // Si no coincide, el estado cambia automáticamente a CodigoIncorrecto
        }
    }

    fun cancelarVinculacionMqtt() {

        viewModelScope.launch {

            repo.cancelarVinculacionPendiente()
        }
    }

    fun cancelarVinculacion() {

        repo.vinculacionManager.procesarEvento(
            VinculacionEvent.Cancelar
        )
    }

    fun confirmarVinculacion() {

        viewModelScope.launch {

            VinculacionPrefs.marcarVinculado(
                getApplication()
            )
        }
    }

    fun setModoDiscreto(activo: Boolean) {

        viewModelScope.launch {

            repo.setModoDiscreto(activo)
        }
    }

    fun setTiempoPanico(segundos: Int) {

        viewModelScope.launch {

            repo.setTiempoPanico(segundos)
        }
    }

    override fun onCleared() {

        super.onCleared()

        movimientoDetector.detener()
    }

    fun simularMovimiento() {
        _mostrarAlertaMovimiento.value = true
    }

    fun ocultarOverlayMovimiento() {
        _mostrarAlertaMovimiento.value = false
    }
}
```

---

## Guía de Ejecución

### Requisitos

- Android Studio Hedgehog o superior, JDK 17
- Teléfono/emulador Android (móvil) y dispositivo/emulador Android TV
- Reloj Wear OS vinculado por Bluetooth al teléfono
- **Los tres dispositivos en la misma red**
- Broker MQTT (Mosquitto) accesible por IP desde los tres dispositivos

### 1. Levantar el broker MQTT (Mosquitto)

```bash
# instalar (Linux)
sudo apt install mosquitto mosquitto-clients

# mosquitto.conf
listener 1883 0.0.0.0
allow_anonymous true
```

`0.0.0.0` es obligatorio: sin eso, Mosquitto solo escucha en `localhost`.

```bash
sudo systemctl restart mosquitto
```

Anota la IP de la máquina donde corre esto en la red local.

### 2. Configurar la misma IP del broker en todos los módulos

```kotlin
// teléfono: app/src/main/java/com/utng/compasos_movil/config/MqttConfig.kt
const val BROKER_URL = "tcp://TU_IP_AQUI:1883"

// tv: app/src/main/java/com/example/compasos_tv/config/MqttConfig.kt
const val BROKER_URL = "tcp://TU_IP_AQUI:1883"   // ← la MISMA IP

// reloj: app/src/main/java/mx/edu/utng/compasos_wearos/config/mqtt/MqttConfig.kt
const val BROKER_URL = "tcp://TU_IP_AQUI:1883"   // ← la MISMA IP
```

### 3. Configurar las claves de API

Ambos módulos leen sus claves desde `app/src/main/res/values/developer-config.xml`:

```xml
<resources>
    <string name="mapbox_access_token" translatable="false">TU_MAPBOX_TOKEN_AQUI</string>
    <string name="youtube_api_key" translatable="false">TU_YOUTUBE_API_KEY_AQUI</string>
</resources>
```

Además, el token de descarga de Mapbox en `~/.gradle/gradle.properties`:

```properties
MAPBOX_DOWNLOADS_TOKEN=sk.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 4. Compilar y ejecutar

1. **Teléfono**: Abrir el proyecto, sincronizar Gradle, ejecutar.
2. **TV**: Abrir el proyecto, sincronizar, ejecutar en emulador Android TV.
3. **Reloj**: Abrir el proyecto, sincronizar, ejecutar en emulador Wear OS.

### 5. Flujo de vinculación

1. En el **teléfono**: Dispositivos → ícono de vincular TV → aparece un código.
2. En la **TV**: pantalla de vinculación → teclear ese código.
3. La TV reintenta la solicitud cada 3 s durante 60 s.
4. Al confirmar, la TV navega a la pantalla principal.

### 6. Probar con mosquitto-clients

```bash
# Ver todo el tráfico
mosquitto_sub -h TU_IP -t 'compasos/#' -v

# Simular una alerta
mosquitto_pub -h TU_IP -t 'compasos/tv/tv_xxxxx/alerta' -m '{
  "alertaId":"a1","tipoAlerta":"SOS","descripcion":"Prueba manual",
  "emisorId":"u2","emisorNombre":"Luis Pérez",
  "latitud":21.15,"longitud":-101.68,"fecha":"2026-08-16 12:01:00"}'
```

---
# Capturas de pantalla de la aplicación Movil

## Inicio de sesión

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/f8d96141-0763-43aa-aeed-d37c0fd17e42" />

---

## Registro de usuario

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/94db63ae-921d-4ec2-b580-6e07157aa831" />

---

## Alarma de emergencia

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/49470c80-2d4e-4aeb-a2c3-a7cddd41c73b" />

---

## Detalle de cuenta en aplicación

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/5c552515-56f1-49a7-8f17-659b7e9508dd" />

---

## Dispositivos vinculados

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/de4ff045-91c3-42bd-b103-04367171ead4" />


# Capturas de pantalla de la aplicación WeareOS


## Pantalla de inicio de sesión

![Login](imagenes/login.png)

---

## Conexion con dispositivo Android

![Conexion](imagenes/Conexion.png)

---

## Pantalla confirmacion de Conexion

![confirmacion](imagenes/confirmacion.png)

## Pantalla Principal

![Principal](imagenes/principal.png)

---

## Pantalla SOS

![SOS](imagenes/sos.png)

---



# Capturas de pantalla de la aplicación TV
## Pantalla de inicio Dashboard

![Dashboard](imagenes/Dashboard.png)

---

## Pantalla de Conexion

![Conexion](imagenes/conexionn.png)

---

## Pantalla de Alertas

![alertass](imagenes/alertass.png)

## Pantalla Famila

![Famila](imagenes/Famila.png)

---

## Pantalla configuracion

![configuracion](imagenes/configuracion.png)


## Pantalla videos

![configuracion](imagenes/videos.png)

---



## Estructura del Proyecto

### 📱 CompaSOS_Movil

```
CompaSOS_Movil/
├── app/src/main/java/com/utng/compasos_movil/
│   ├── config/
│   │   ├── AlertaMqttService.kt      # Recibe SOS del reloj
│   │   ├── MqttConfig.kt             # Configuración MQTT
│   │   ├── MqttManager.kt            # Cliente MQTT
│   │   └── TvSyncService.kt          # Sincronización con TV (NUEVO)
│   ├── AlertaPhoneRepository/
│   │   └── AlertaPhoneRepository.kt  # Procesa alertas y reenvía a TVs
│   ├── TvVinculacionModule/
│   │   └── TvVinculacionViewModel.kt # Vinculación de TV
│   ├── data/
│   │   ├── dao/
│   │   │   ├── DispositivoDao.kt     # TVs vinculadas
│   │   │   └── HistorialUbicacionDao.kt # Última ubicación por usuario
│   │   └── entity/
│   │       ├── DispositivoEntity.kt
│   │       └── HistorialUbicacionEntity.kt
│   ├── utils/
│   │   └── TvMqttPublisher.kt        # API pública para enviar a TVs
│   └── MainActivity.kt               # Inicia servicios
```

### 📺 CompaSOS_TV

```
CompaSOS_TV/
├── app/src/main/java/com/example/compasos_tv/
│   ├── config/
│   │   ├── MqttConfig.kt             # Configuración MQTT
│   │   └── MqttManager.kt            # Singleton MQTT
│   ├── services/
│   │   ├── TvMqttService.kt          # Servicio MQTT de TV
│   │   └── VinculacionTvRepository.kt # Vinculación con teléfono
│   ├── data/entitys/
│   │   ├── dao/
│   │   │   ├── FamiliarTvDao.kt      # Familiares en TV
│   │   │   ├── AlertaTvDao.kt        # Alertas en TV
│   │   │   └── NotificacionTvDao.kt  # Notificaciones en TV
│   │   ├── FamiliarTvEntity.kt
│   │   ├── AlertaTvEntity.kt
│   │   └── NotificacionTvEntity.kt   # (NUEVO)
│   ├── Screan/
│   │   ├── TvAlertasScreen.kt        # Bandeja de alertas (NUEVO)
│   │   ├── TvDashboardScreen.kt      # Pantalla principal
│   │   └── VinculacionScreen.kt      # Pantalla de vinculación
│   └── MainActivity.kt               # Inicia TvMqttService
```

### ⌚ CompaSOS_WearOS

```
CompaSOS_WearOS/
├── app/src/main/java/mx/edu/utng/compasos_wearos/
│   ├── config/mqtt/
│   │   ├── MqttConfig.kt             # Configuración MQTT
│   │   └── MqttManager.kt            # Cliente MQTT
│   ├── data/
│   │   ├── entity/
│   │   │   └── ConfigReloj.kt        # Configuración en Room
│   │   ├── repository/
│   │   │   ├── VinculacionRepository.kt # Vinculación MQTT
│   │   │   ├── VinculacionPrefs.kt   # Estado en DataStore
│   │   │   └── AlertaWearRepository.kt # Envío de SOS
│   │   ├── VinculacionState.kt       # Estados de vinculación
│   │   └── VinculacionEvent.kt       # Eventos de vinculación
│   ├── viewmodel/
│   │   └── VinculacionViewModel.kt   # ViewModel principal
│   └── services/
│       └── MovimientoDetector.kt     # Detección de caídas
```

---

## Autores

**José Andrés Gutiérrez Vargas**

**Ana María Barrientos Guerrero**

**Grupo:** GIDS6093

---

## Licencia

Proyecto desarrollado con fines académicos para la Universidad Tecnológica del Norte de Guanajuato.