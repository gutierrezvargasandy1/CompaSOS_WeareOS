# 🚨 CompaSOS

## Información del Proyecto

**Nombre del proyecto:** CompaSOS

**Nombre de los estudiantes:** José Andrés Gutiérrez Vargas, Ana María Barrientos Guerrero

**Grupo:** GIDS6093

---

# Beneficiario y problemática atendida

El proyecto fue validado por **Saul Aguayo Arredondo**, Sub director de la **Escuela Secundaria Técnica No 26**.

En una situación de emergencia (una caída, una descompensación de salud, un accidente doméstico o un riesgo en la vía pública) lo que determina el desenlace es el tiempo que transcurre entre el incidente y el momento en que alguien se entera. Actualmente ese aviso depende de una cadena frágil de condiciones: que la persona traiga el teléfono consigo, esté consciente, pueda desbloquearlo, ubique al contacto correcto y logre explicar dónde se encuentra. Basta con que una sola de esas condiciones falle para que la ayuda se retrase o no llegue.

La problemática se agudiza en quienes pasan tiempo solos o dependen de un tercero: adultos mayores, personas con enfermedades crónicas o alguna discapacidad, y quienes se trasladan por zonas inseguras. Del otro lado, sus familiares y cuidadores no tienen una manera sencilla de saber si están bien sin llamar de forma constante, lo que impone una carga de vigilancia permanente que, aun así, no garantiza una reacción oportuna.

A lo anterior se suma una limitación tecnológica de fondo: las alternativas disponibles viven en un solo dispositivo, casi siempre el teléfono, que es justamente el que no siempre está encima ni al alcance de la mano. No aprovechan el reloj inteligente que la persona porta durante todo el día, ni la televisión, que en muchos hogares —sobre todo donde hay adultos mayores— es la pantalla de mayor uso y la más fácil de operar. El resultado es una cobertura fragmentada que deja al usuario desprotegido precisamente en los momentos y lugares donde más necesita apoyo.

Este módulo wearable atiende directamente esa brecha: lleva la capacidad de pedir ayuda al dispositivo que la persona porta de manera permanente, eliminando la dependencia de tener el teléfono a la mano.

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

# Autores

**José Andrés Gutiérrez Vargas**

**Ana María Barrientos Guerrero**

**Grupo:** GIDS6093

---

# Licencia

Proyecto desarrollado con fines académicos para la Universidad Tecnológica del Norte de Guanajuato.
