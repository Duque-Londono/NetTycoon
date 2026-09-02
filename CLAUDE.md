# CLAUDE.md — NetTycoon

Guía operativa permanente para trabajar en este proyecto. Léela completa antes de cualquier tarea y respétala en todo momento.

**NetTycoon** es un juego móvil Android (Kotlin) de administración de infraestructura: el jugador defiende una red corporativa creando reglas de firewall (ALLOW/DENY por puerto e IP) contra ataques simulados que llegan enriquecidos con la IP real del atacante (consultada por API REST), con todo el progreso guardado offline. Proyecto académico, equipo de 3, plazo corto.

---

## ⛔ REGLAS DURAS (nunca las rompas)

1. **Package congelado:** `com.ejemplo.nettycoon`. Es el `namespace`/`applicationId`. NUNCA lo cambies ni crees paquetes fuera de esa raíz. Es el que está registrado en Firebase.
2. **Costo $0:** no introduzcas ninguna dependencia, servicio o plan de pago. En Firebase Auth **solo email/password**; **jamás** login por teléfono/SMS (es lo único que cobra).
3. **Red solo por HTTPS:** toda llamada de red debe ser HTTPS. No habilites tráfico en claro (cleartext) en el manifest ni en network-security-config. La API de geo-IP del juego es **ipwho.is** (HTTPS, sin API key). No uses ip-api.com (es solo HTTP).
4. **No toques la configuración de Gradle especial** (ver §Entorno). Esos flags son necesarios; no los borres ni los "simplifiques".
5. **Respeta el alcance congelado** (ver §Alcance). No construyas nada de la lista Out-of-Scope, ni siquiera "por si acaso".

---

## 🔄 PROTOCOLO DE TRABAJO (obligatorio en cada tarea)

1. **PLAN primero.** Ante cualquier tarea, presenta un plan detallado de lo que vas a hacer y **espera aprobación explícita ("aprobado") antes de ejecutar nada.** No escribas ni modifiques código hasta la aprobación.
2. **No corras el emulador.** No intentes crear ni arrancar AVDs ni lanzar la app en el emulador — es trabajo del equipo humano. Para verificar tu trabajo, compila (`./gradlew assembleDebug`) y reporta el resultado. No gastes tiempo ni tokens en el emulador.
3. **RESUMEN al final.** Al terminar, entrega: archivos creados/modificados, decisiones tomadas, versiones usadas, y cualquier cosa que hayas omitido o dejado pendiente.
4. **Cambios pequeños y por rama.** Trabaja siempre en una rama por tarea (`feat/...`), nunca directo a `main`. Commits pequeños, descriptivos, en español.
5. **Ante un bug, reproduce primero** (idealmente con una prueba) antes de arreglar.

---

## 🧱 ARQUITECTURA

- **Patrón: MVVM estricto, flujo unidireccional.**
  `UI (Composables) → ViewModel (expone UiState vía StateFlow) → Repository → [ Room | Retrofit | Firebase Auth ]`
- **La UI NUNCA habla directo con Room, Retrofit ni Firebase.** Todo pasa por ViewModel → Repository.
- **Estados de UI explícitos:** toda operación asíncrona (red, BD, auth) se modela con un `UiState` que contempla `Cargando / Éxito / Error`. La UI reacciona a ese estado.
- **Separación de dependencias externas** (cada una hace UNA cosa, ninguna depende de otra):
    - Firebase Auth → solo identidad/sesión.
    - API geo-IP (ipwho.is) → solo enriquecer ataques con datos de la IP.
    - Room → solo estado local offline.
- **Corrutinas** para todo lo asíncrono.

---

## 📁 ESTRUCTURA DE PAQUETES (por capa)

Todo bajo `com.ejemplo.nettycoon`:

```
NetTycoonApplication.kt      · clase Application (init de Firebase cuando exista)
MainActivity.kt              · hospeda el NavHost raíz dentro del tema de la app
data/
  local/entity/              · entidades Room
  local/dao/                 · un DAO por entidad
  local/                     · NetTycoonDatabase
  remote/dto/                · DTOs de respuestas JSON
  remote/                    · GeoIpApiService, provider de Retrofit
  repository/                · repositorios (fuente única de datos para los ViewModel)
domain/
  model/                     · modelos de dominio (no confundir con entidades ni DTOs)
  firewall/                  · MotorReglas (ALLOW/DENY), GeneradorAtaques
auth/                        · AuthRepository, AuthViewModel, AuthUiState (Firebase email/password)
ui/
  theme/                     · Color.kt, Type.kt, Shape.kt, Theme.kt (Material Design 3)
  login/                     · LoginScreen, RegistroScreen (+ su ViewModel)
  network/                   · pantalla de configuración de red
  firewall/                  · FirewallScreen (+ FirewallViewModel)
  componentes/               · Composables reutilizables
navigation/
  NetTycoonNavHost.kt        · NavHost; gate condicional (sesión → juego / sin sesión → login)
  Rutas.kt                   · rutas selladas
```

**Cada ViewModel vive junto a su pantalla** (dentro de la carpeta de la feature en `ui/`), no en un paquete `viewmodel` aparte.

---

## 🎨 CONVENCIONES

- **UI:** 100% Jetpack Compose. **Material Design 3** vía el tema base en `ui/theme/` (los 4 archivos: `Color`, `Type`, `Shape`, `Theme`), con esquema claro y oscuro. No uses Views/XML para UI.
- **Navegación:** Navigation Compose con `NavHost`/`NavController`, rutas selladas en `Rutas.kt`, y paso de argumentos con seguridad de tipos. El destino inicial es condicional según haya sesión o no.
- **Nombres:** clases y archivos en español cuando sea natural (p. ej. `MotorReglas`, `ReglaFirewall`, `ConfiguracionRedScreen`); sufijos estándar en inglés donde es convención (`...Screen`, `...ViewModel`, `...Repository`, `...Dao`, `...Dto`, `...Entity`).
- **Room:** compilador vía **KSP** (no kapt). Entidades en `data/local/entity`, DAOs en `data/local/dao`.
- **Red:** Retrofit 2 + converter-gson + OkHttp logging-interceptor. Maneja explícitamente error de red y ausencia de conexión.
- **Nada de secretos en el código** ni en el repo (ver §Git).

---

## 🌱 GIT

- Nunca commits directos a `main`. Rama por tarea: `feat/`, `fix/`, `chore/`.
- Commits pequeños, en español, descriptivos. Un PR por tarea, revisado por un compañero antes de fusionar.
- **`.gitignore` debe excluir:** `local.properties`, `.idea/`, `build/`, `.gradle/`.
- **Debe incluir en el repo:** `gradlew`, `gradlew.bat`, `gradle/wrapper/`.
- `google-services.json`: por defecto va en `.gitignore` (se comparte por fuera). Si el equipo decide commitearlo, respétalo — pero no lo subas por iniciativa propia.

---

## ⚙️ ENTORNO (no lo cambies sin que te lo pidan)

Configuración verificada y funcionando. **No actualices ni degrades estas versiones por tu cuenta.**

- AGP **9.3.2** (Kotlin **integrado** — sin plugin `kotlin-android`) · Kotlin **2.2.10** · JDK **25**
- Compose BOM **2026.02.01** · Material 3
- `compileSdk = 37` · `targetSdk = 36` · Navigation Compose 2.9.5 · Lifecycle ViewModel Compose 2.11.0
- Coroutines 1.10.2 · Room 2.7.2 (runtime+ktx, compiler por KSP) · KSP 2.2.10-2.0.2 · Retrofit 2.11.0 + converter-gson · OkHttp logging-interceptor 4.12.0

**Flags de Gradle que NO debes borrar** (necesarios para KSP con el Kotlin integrado de AGP 9):
- `gradle.properties`: `android.disallowKotlinSourceSets=false`
- `build.gradle.kts` raíz: plugin `ksp` declarado con `apply false`.

Si necesitas añadir una dependencia nueva, propónla en el PLAN (no la agregues sin aprobación) y usa el version catalog (`libs.versions.toml`).

---

## 🎯 ALCANCE

**Construir (Must-Have):**
- Auth con Firebase (email/password) como compuerta antes del juego.
- Persistencia local con Room (reglas, eventos, estado de partida, usuario).
- Motor de reglas de firewall (ALLOW/DENY por puerto e IP) con feedback inmediato.
- Consumo de API REST real (geo-IP, ipwho.is) vía Retrofit, con estados carga/éxito/error.
- UI en Compose + Material Design 3.
- Navigation Compose con gate condicional y paso de argumentos.

**NUNCA construir (Out-of-Scope):**
- Tabla de clasificación global o sincronización en la nube.
- Simulación completa de hardware (DHCP, NAT, SSID, latencia real) → solo una pantalla de configuración básica de router.
- Canvas custom / mapa de oficina ilustrado → UI estándar de Compose.
- Login por SMS/teléfono, MFA, login social, recuperación de contraseña → **solo email/password**.
- Cualquier backend propio.

---

## 🔒 DECISIONES TÉCNICAS BLOQUEADAS

- **Auth:** Firebase Authentication, plan gratuito, solo email/password.
- **API del juego:** ipwho.is (HTTPS, sin API key, gratis para uso no comercial).
- **Persistencia:** Room (SQLite).
- **Bucle central del juego:** `Login → configurar red → llega ataque → consulta IP real → jugador decide ALLOW/DENY → feedback inmediato → guardar en Room → repetir`.