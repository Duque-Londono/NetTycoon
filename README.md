# NetTycoon

**Juego móvil Android (Kotlin) para aprender ciberseguridad desde cero.** El jugador defiende
una red corporativa decidiendo qué tráfico permitir o bloquear frente a ataques simulados, con
todo el progreso guardado offline. Proyecto académico.

---

## Qué es y cómo se juega

NetTycoon invierte el enfoque de un tutorial seco: en vez de leer teoría, el jugador **toma
decisiones reales** y aprende con el resultado.

- Llega un **ataque** explicado en lenguaje humano (situación + una pista educativa).
- El jugador decide **Permitir** o **Bloquear** ese tráfico.
- Recibe **feedback inmediato**: si acertó o no, **una lección** que lo explica, y el efecto en
  su red (puntaje, salud, dinero, nivel).
- Cuando repite con acierto el mismo patrón (puerto + acción), el juego le **sugiere crear una
  regla** para automatizar esa decisión — el puente entre "decidir a mano" y "automatizar".

El cruce entre la verdad del ataque y la decisión del jugador da 4 resultados:

| Verdad del tráfico | Decisión | Resultado |
|--------------------|----------|-----------|
| Malicioso | Bloquear | **Bloqueo correcto** ✅ (lo más premiado) |
| Legítimo | Permitir | **Permiso correcto** ✅ |
| Malicioso | Permitir | **Brecha** ❌ (dejaste pasar un ataque) |
| Legítimo | Bloquear | **Falso positivo** ❌ (bloqueaste tráfico legítimo) |

Diseño de balance: **defenderse activamente es lo más recompensado**; equivocarse por exceso de
bloqueo (falso positivo) también penaliza, para enseñar que bloquear de más tiene costo.

### Bucle central

```
Login → configurar red → llega ataque → jugador decide ALLOW/DENY
      → feedback inmediato + lección → guardar en Room → repetir
```

---

## Pantallas

| Pantalla | Paquete | Qué hace |
|----------|---------|----------|
| **Login / Registro** | `ui/login/` | Puerta de entrada (Firebase Auth, solo email/password). |
| **Panel** | `ui/panel/` | Hub: muestra el estado de la partida y da acceso a las tres pantallas. |
| **Ataque en vivo** | `ui/ataque/` | El jugador decide Permitir/Bloquear cada ataque y aprende con la lección. Incluye la sugerencia pedagógica de automatizar con reglas. |
| **Mis reglas** | `ui/firewall/` | CRUD de reglas ALLOW/DENY (por puerto e IP) que **automatizan** decisiones. Incluye un catálogo de puertos comunes explicados. |
| **Configurar red** | `ui/network/` | El jugador describe su propia infraestructura (IP del router, puertos LAN/WAN), con explicaciones. |

---

## APIs y servicios externos

### API de geolocalización de IP — **ipwho.is**

Única API REST externa del juego. Enriquece los ataques con datos reales de la IP (país e ISP).

- **Endpoint:** `GET https://ipwho.is/{ip}` (`data/remote/GeoIpApiService.kt`).
- **Cliente:** Retrofit 2 + OkHttp + converter-gson (`data/remote/RetrofitProvider.kt`,
  `baseUrl = "https://ipwho.is/"`).
- **Por qué esta:** es **HTTPS**, gratuita y **sin API key** (requisitos del proyecto: costo $0
  y todo el tráfico por HTTPS). Se descartó ip-api.com por ser solo HTTP.
- **Datos que se usan:** `country` (país) e `isp` — ver `data/remote/dto/IpWhoIsResponseDto.kt`.
  Nota: ipwho.is responde HTTP 200 incluso ante error lógico (`success=false`); ese caso se
  trata como error en `GeoIpRepository`.
- **Tolerancia a fallos ("best-effort"):** si la API falla o no hay conexión, **el juego
  continúa igual** y país/ISP quedan vacíos; la consulta nunca interrumpe una ronda. Los estados
  Cargando/Éxito/Error se manejan explícitamente en `data/repository/GeoIpRepository.kt`.

### Firebase Authentication

Autenticación **email/password** como compuerta antes del juego (`auth/`). Plan gratuito; **no**
se usa login por teléfono/SMS ni social (es lo único que cobraría).

> La llamada real a ipwho.is se orquesta en `domain/firewall/ProcesarAtaqueUseCase.kt` (flujo de
> ataques aleatorios). La pantalla **"Ataque en vivo"** usa un catálogo fijo (ver abajo) con
> país/ISP ya definidos, así que **no** hace peticiones de red: es contenido curado con fines
> educativos.

---

## Catálogo de ataques (contenido educativo)

Los ataques de la pantalla **"Ataque en vivo" NO vienen de un servidor ni de un script externo**:
están **definidos en el código** como una lista fija y curada.

- **Archivo:** `ui/ataque/EscenarioAtaque.kt`
- `data class EscenarioAtaque` — forma de cada escenario: `ipAtacante`, `pais`, `isp`, `puerto`,
  `servicioNombre`, `esMalicioso` (la "verdad"), `textoSituacion`, `textoPista`, `leccionAcierto`,
  `leccionError`.
- `object CatalogoAtaques` — los **8 escenarios** pedagógicos (HTTPS, SSH, RDP, SMTP, MySQL, DNS,
  Telnet, FTP). Son fijos a propósito: cada uno enseña un concepto y trae la verdad para poder
  evaluar la decisión del jugador y darle una lección.

> Existe además un **generador de ataques aleatorios** (`domain/firewall/GeneradorAtaques.kt`)
> para el flujo automatizado. Son **dos fuentes de ataques distintas**: el catálogo fijo
> (pedagógico) y el generador aleatorio (motor). No deben confundirse.

El catálogo de **puertos comunes** de la pantalla de reglas (`ui/firewall/CatalogoPuertos.kt`) es
otra lista fija: asocia cada puerto con su nombre de servicio y una explicación (443 → "HTTPS /
webs seguras", 22 → "SSH / acceso remoto", etc.).

---

## Arquitectura

**MVVM estricto, flujo unidireccional:**

```
UI (Composables) → ViewModel (expone UiState vía StateFlow) → Repository → [ Room | Retrofit | Firebase Auth ]
```

- La **UI nunca** habla directo con Room, Retrofit ni Firebase: todo pasa por ViewModel →
  Repository.
- Toda operación asíncrona (red, BD, auth) se modela con un `UiState` que contempla
  **Cargando / Éxito / Error**.
- Cada dependencia externa hace **una sola cosa**: Firebase Auth (identidad), ipwho.is (enriquecer
  ataques), Room (estado local offline).
- **Corrutinas** para todo lo asíncrono.

### Motor de firewall (lógica pura, sin Android)

- `domain/firewall/MotorFirewall.kt` evalúa un ataque contra las reglas activas: coincide por
  **puerto exacto** + **IP** (igualdad exacta o `null` = comodín). Si varias reglas coinciden,
  **gana DENY**. Si ninguna coincide, aplica la **política por defecto DENY** (estándar seguro).
- `domain/firewall/ConsecuenciasPartida.kt` + `BalancePartida.kt` calculan el efecto en
  puntaje/salud/dinero/nivel (constantes ajustables sin tocar la lógica).

### Persistencia local (Room)

Tres entidades en `data/local/entity/`:

- `EstadoPartida` — métricas del juego + configuración de red (una partida por usuario).
- `ReglaFirewall` — reglas ALLOW/DENY del jugador.
- `EventoAtaque` — log de ataques (con país/ISP cuando la API los aporta).

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

### Verificar sin emulador

```bash
./gradlew testDebugUnitTest   # pruebas unitarias JVM (motor, validadores, ViewModels)
./gradlew assembleDebug       # compilar el APK de debug
```

> **Nota sobre `google-services.json`:** este archivo **no está incluido en el repositorio**
> (está en `.gitignore`) y se comparte por fuera del control de versiones. La estructura base
> compila y corre; para usar Firebase Auth se necesita ese archivo.

---

## Package

```
com.ejemplo.nettycoon
```

---

## Stack

- **Lenguaje:** Kotlin 2.2.x
- **UI:** Jetpack Compose (Compose BOM) + Material 3 (esquema claro y oscuro)
- **Navegación:** Navigation Compose (rutas selladas, argumentos con seguridad de tipos, gate
  condicional según sesión)
- **Arquitectura:** MVVM (ViewModel + Lifecycle Compose), StateFlow
- **Persistencia local:** Room (compilador por KSP)
- **Red:** Retrofit 2 + OkHttp (logging-interceptor) + converter-gson
- **Auth:** Firebase Authentication (email/password)
- **Asíncrono:** Kotlin Coroutines
- **Build:** Gradle (Kotlin DSL) + AGP 9, catálogo de versiones (`libs.versions.toml`)

---

## Estructura por capas

El código sigue una separación por capas dentro de `com.ejemplo.nettycoon`:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| **UI** | `ui/` | Pantallas Compose, componentes y tema (`theme/`, `login/`, `panel/`, `ataque/`, `firewall/`, `network/`, `componentes/`) |
| **Navegación** | `navigation/` | `NavHost` con gate condicional y rutas selladas (`Rutas.kt`) |
| **Dominio** | `domain/` | Modelos y lógica pura (`model/`, `firewall/`: motor, generador, consecuencias, caso de uso) |
| **Datos** | `data/` | `local/` (Room: entidades y DAOs), `remote/` (Retrofit + DTOs) y `repository/` |
| **Auth** | `auth/` | Autenticación con Firebase (email/password) |

---

## Flujo de trabajo Git

- **Una rama por tarea** (`feat/`, `fix/`, `docs/`), nunca se trabaja directamente sobre `main`.
- Todo cambio entra vía **Pull Request con revisión** de un compañero.
- **`main` siempre estable**: no se hace push directo a `main`.
- Commits pequeños y descriptivos, en español.
- `.gitignore` excluye `local.properties`, `.idea/`, `build/`, `.gradle/` y `google-services.json`.
