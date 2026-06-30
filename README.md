# 🚨 CompaSOS

## Información del Proyecto

**Nombre del proyecto:** CompaSOS

**Nombre del estudiante:** José Andrés Gutiérrez Vargas

**Grupo:** GIDS6093

---

# Objetivo

Desarrollar una aplicación para Wear OS que permita brindar asistencia rápida en situaciones de emergencia mediante el envío de alertas, monitoreo de signos vitales y comunicación con una aplicación móvil. El objetivo principal es mejorar la seguridad del usuario proporcionando herramientas que faciliten la solicitud de ayuda de forma inmediata.

---

# Descripción de las funcionalidades

La aplicación CompaSOS cuenta con las siguientes funcionalidades:

- Inicio de sesión de usuarios.
- Registro de nuevos usuarios.
- Vinculación entre el reloj Wear OS y la aplicación móvil.
- Botón de emergencia para enviar alertas.
- Monitoreo de la frecuencia cardíaca en tiempo real.
- Detección de caídas utilizando los sensores del dispositivo.
- Envío automático de alertas cuando se detecta una posible emergencia.
- Visualización del estado del usuario.
- Interfaz diseñada y optimizada para dispositivos Wear OS.
- Comunicación entre reloj y teléfono mediante Data Layer.
- Almacenamiento de información utilizando Firebase.

---

# Tecnologías utilizadas

## Lenguaje de programación

- Kotlin

## Frameworks y herramientas

- Android Studio
- Jetpack Compose
- Wear OS
- Firebase Authentication
- Firebase Firestore
- Firebase Realtime Database
- Google Play Services
- Data Layer API
- Coroutines
- Material Design

---

# Instrucciones para ejecutar el proyecto

## Requisitos

- Android Studio Hedgehog o superior.
- JDK 17.
- Dispositivo Wear OS o emulador Wear OS.
- Dispositivo Android para la aplicación móvil.
- Cuenta de Firebase configurada.
- Archivo `google-services.json` agregado al proyecto.

## Pasos

1. Clonar el repositorio.

```bash
git clone  -b dev https://github.com/gutierrezvargasandy1/CompaSOS_WeareOS.git
```

2. Abrir el proyecto en Android Studio.

3. Sincronizar las dependencias de Gradle.

4. Ejecutar posteriormente la aplicación Wear OS.

5. En otro dispositivo Android Vincula mediante el Divice Manage de Android Studios.

6. Probar las funciones de monitoreo y envío de alertas.

---

# Capturas de pantalla de la aplicación

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


# Estructura del proyecto

```
CompaSOS/
├── compasos_wearos/
├── data/
├── helper/
├── ui/
├── viewmodel/
├── services/
├── navigation/
├── presentation/
└── README.md
```

---

# Autor

**José Andrés Gutiérrez Vargas**

**Grupo:** GIDS6093

---

# Licencia

Proyecto desarrollado con fines académicos para la Universidad Tecnológica del Norte de Guanajuato.