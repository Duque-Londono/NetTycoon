# NetTycoon

Juego móvil de estrategia y gestión de redes: construye, expande y defiende tu imperio de telecomunicaciones desde Android.

---

## Requisitos previos

Antes de compilar necesitas tener instalado:

- **Android Studio** compatible con **AGP 9** (Android Gradle Plugin 9.x).
- **JDK 25**.
- **Android SDK Platform 37**, instalable desde el **SDK Manager** de Android Studio
  (*Settings → Languages & Frameworks → Android SDK → SDK Platforms*).

> ⚠️ **Sin estos tres componentes el proyecto no compila.** Si el Sync o el build fallan,
> lo primero a verificar es que AGP 9, JDK 25 y el SDK Platform 37 estén correctamente
> instalados y seleccionados.

---

## Cómo empezar

1. **Clonar** el repositorio:
   ```bash
   git clone https://github.com/Duque-Londono/NetTycoon.git
   cd NetTycoon
   ```
2. **Abrir** la carpeta del proyecto en **Android Studio**.
3. Ejecutar **Sync Gradle** (Android Studio lo ofrece automáticamente; si no, *File → Sync Project with Gradle Files*).
4. **Correr** la app en un **emulador** o **dispositivo físico** con el botón *Run ▶*.

> **Nota sobre `google-services.json`:** este archivo **no está incluido en el repositorio**
> (está en `.gitignore`). Se obtiene por fuera y se añadirá cuando se integre **Firebase**
> en el siguiente módulo. La estructura base actual compila y corre sin él.

---

## Package

```
com.ejemplo.nettycoon
```

---

## Stack (resumen)

- **Lenguaje:** Kotlin 2.2.x
- **UI:** Jetpack Compose (Compose BOM) + Material 3
- **Navegación:** Navigation Compose
- **Arquitectura:** ViewModel + Lifecycle (Compose)
- **Persistencia local:** Room
- **Red:** Retrofit + OkHttp (Gson)
- **Build:** Gradle (Kotlin DSL) + AGP 9, catálogo de versiones (`libs.versions.toml`)
- **Asíncrono:** Kotlin Coroutines

---

## Estructura por capas

El código sigue una separación por capas dentro de `com.ejemplo.nettycoon`:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| **UI** | `ui/` | Pantallas Compose, componentes y tema (`theme/`, `login/`, `network/`, `firewall/`, `componentes/`) |
| **Navegación** | `navigation/` | `NavHost` y definición de rutas |
| **Dominio** | `domain/` | Modelos y lógica de negocio (`model/`, `firewall/`) |
| **Datos** | `data/` | `local/` (Room: entidades y DAOs), `remote/` (DTOs) y `repository/` |
| **Auth** | `auth/` | Autenticación (a integrar con Firebase) |

---

## Flujo de trabajo Git

- **Una rama por tarea** (feature/fix), nunca se trabaja directamente sobre `main`.
- Todo cambio entra vía **Pull Request con revisión**.
- **`main` siempre estable**: no se hace push directo a `main`.
