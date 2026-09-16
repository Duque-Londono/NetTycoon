package com.ejemplo.nettycoon.ui.ataque

/**
 * Nivel de dificultad pedagógica de un [EscenarioAtaque], según qué tan OBVIA es la decisión
 * correcta:
 *
 * - [FACIL]: casos de libro (HTTPS legítimo → permitir; fuerza bruta → bloquear).
 * - [MEDIO]: requieren algo de criterio (servicios sensibles expuestos, protocolos obsoletos).
 * - [DIFICIL]: ambiguos o con matiz (lo que parece sospechoso puede ser legítimo, y al revés).
 *
 * Se usa para servir el catálogo por niveles con una curva de aprendizaje (ver el selector del
 * ViewModel). Es solo metadato de presentación: no toca dominio, Room ni el motor.
 */
enum class Dificultad { FACIL, MEDIO, DIFICIL }

/**
 * Escenario pedagógico de "Ataque en vivo".
 *
 * A diferencia de los ataques del [com.ejemplo.nettycoon.domain.firewall.GeneradorAtaques]
 * (aleatorios, para el bucle automatizado), estos son **fijos y curados**: cada uno enseña un
 * concepto de red/ciberseguridad en lenguaje humano y trae la "verdad" ([esMalicioso]) para
 * poder evaluar la decisión del jugador y darle una lección, acierte o falle.
 *
 * Es un modelo de PRESENTACIÓN de esta pantalla; no es una entidad de Room ni un DTO.
 */
data class EscenarioAtaque(
    /** IP del atacante/origen (fija, no consultada a la API geo-IP). */
    val ipAtacante: String,
    val pais: String,
    val isp: String,
    /** Puerto de destino del intento de conexión. */
    val puerto: Int,
    /** Nombre humano del servicio del puerto (ej. "HTTPS / navegación web segura"). */
    val servicioNombre: String,
    /** Verdad del escenario: `true` si el tráfico es realmente malicioso. */
    val esMalicioso: Boolean,
    /** Qué está pasando, en lenguaje sencillo. */
    val textoSituacion: String,
    /** Dato educativo que ayuda a decidir. */
    val textoPista: String,
    /** Lección a mostrar si el jugador acierta. */
    val leccionAcierto: String,
    /** Lección a mostrar si el jugador se equivoca. */
    val leccionError: String,
    /**
     * Nivel de dificultad pedagógica del escenario. Default [Dificultad.MEDIO] para no romper
     * llamadas/previews/tests que aún no lo especifican; los 30 del catálogo se clasifican
     * explícitamente.
     */
    val dificultad: Dificultad = Dificultad.MEDIO,
)

/**
 * Catálogo fijo de los 30 escenarios pedagógicos. El orden aquí es solo el de definición;
 * el ViewModel decide cuál mostrar mediante su selector.
 *
 * Balance curricular: ~40% tráfico legítimo (debe permitirse) y ~60% malicioso (debe bloquearse),
 * para enseñar a discriminar en vez de bloquear por reflejo. Cubre familias de amenaza (fuerza
 * bruta, bases de datos expuestas, protocolos inseguros, reconocimiento, malware/exfiltración),
 * tráfico legítimo común y varios casos ambiguos que enseñan criterio.
 *
 * Nota de posición: los índices 0 (HTTPS legítimo) y 1 (SSH malicioso) los usa la demo de
 * `AtaqueEnVivoScreen`; los 8 escenarios originales se conservan en su orden y los 22 nuevos
 * se añaden a continuación.
 */
object CatalogoAtaques {

    val escenarios: List<EscenarioAtaque> = listOf(
        EscenarioAtaque(
            ipAtacante = "190.85.12.44",
            pais = "Colombia",
            isp = "Claro",
            puerto = 443,
            servicioNombre = "HTTPS / navegación web segura",
            esMalicioso = false,
            textoSituacion = "Alguien desde Colombia intenta conectarse a tu red por el puerto 443.",
            textoPista = "El puerto 443 es el que usan las páginas web seguras (HTTPS). Es tráfico normal y muy común.",
            leccionAcierto = "Correcto. Era una conexión web legítima. Dejar pasar el tráfico normal mantiene tu red funcionando.",
            leccionError = "Cuidado: era tráfico legítimo. Bloquearlo es un 'falso positivo': dejaste sin servicio a un usuario real.",
            dificultad = Dificultad.FACIL,
        ),
        EscenarioAtaque(
            ipAtacante = "45.146.165.37",
            pais = "Rusia",
            isp = "Desconocido",
            puerto = 22,
            servicioNombre = "SSH / acceso remoto al servidor",
            esMalicioso = true,
            textoSituacion = "Una conexión desde Rusia intenta entrar por el puerto 22 de madrugada.",
            textoPista = "El puerto 22 (SSH) da control remoto de tu servidor. Un intento inesperado desde fuera suele ser alguien buscando colarse.",
            leccionAcierto = "Bien hecho. Bloqueaste un intento de acceso remoto no autorizado: así se protege el control del servidor.",
            leccionError = "Peligro: permitiste acceso remoto a un desconocido. Si logra entrar por SSH, controla tu servidor entero.",
            dificultad = Dificultad.MEDIO,
        ),
        EscenarioAtaque(
            ipAtacante = "193.32.162.10",
            pais = "Países Bajos",
            isp = "Hosting VPS",
            puerto = 3389,
            servicioNombre = "RDP / escritorio remoto",
            esMalicioso = true,
            textoSituacion = "Miles de intentos de conexión llegan al puerto 3389 en pocos segundos.",
            textoPista = "El puerto 3389 (escritorio remoto) permite ver y usar un equipo a distancia. Miles de intentos = un ataque de 'fuerza bruta' probando contraseñas.",
            leccionAcierto = "Correcto. Un aluvión de intentos es fuerza bruta: bloquearlo evita que adivinen la contraseña por repetición.",
            leccionError = "Riesgo alto: dejaste pasar un ataque de fuerza bruta. Con suficientes intentos podrían adivinar la clave y tomar el equipo.",
            dificultad = Dificultad.FACIL,
        ),
        EscenarioAtaque(
            ipAtacante = "170.110.40.5",
            pais = "México",
            isp = "Totalplay",
            puerto = 587,
            servicioNombre = "SMTP / envío de correo",
            esMalicioso = false,
            textoSituacion = "Tu servidor de correo intenta enviar mensajes por el puerto 587.",
            textoPista = "El puerto 587 se usa para ENVIAR correos electrónicos de forma legítima. Es parte normal de la operación.",
            leccionAcierto = "Correcto. Es correo saliente normal. Bloquearlo dejaría a tu empresa sin poder enviar emails.",
            leccionError = "Falso positivo: bloqueaste el envío de correo legítimo. Tu empresa se quedaría sin mandar mensajes.",
            dificultad = Dificultad.FACIL,
        ),
        EscenarioAtaque(
            ipAtacante = "89.248.165.14",
            pais = "Rumania",
            isp = "Servidor anónimo",
            puerto = 3306,
            servicioNombre = "MySQL / base de datos",
            esMalicioso = true,
            textoSituacion = "Una conexión externa intenta llegar directo al puerto 3306 de tu base de datos.",
            textoPista = "El puerto 3306 (MySQL) da acceso a tu base de datos. Nunca debería estar abierto a internet: ahí viven los datos sensibles.",
            leccionAcierto = "Excelente. Nadie de fuera debería tocar tu base de datos directamente. Bloquear protege la información.",
            leccionError = "Grave: expusiste tu base de datos a internet. Un atacante podría robar o borrar todos los datos.",
            dificultad = Dificultad.MEDIO,
        ),
        EscenarioAtaque(
            ipAtacante = "8.8.8.8",
            pais = "Estados Unidos",
            isp = "Google",
            puerto = 53,
            servicioNombre = "DNS / resolución de nombres",
            esMalicioso = false,
            textoSituacion = "Tu red consulta el puerto 53 para traducir nombres de páginas web.",
            textoPista = "El puerto 53 (DNS) convierte 'google.com' en la dirección numérica real. Sin él, no cargarían las webs.",
            leccionAcierto = "Correcto. El DNS es esencial para navegar. Bloquearlo dejaría tu red sin poder abrir ninguna página.",
            leccionError = "Falso positivo: bloqueaste el DNS. Sin él, ningún dispositivo de tu red podría abrir sitios web.",
            dificultad = Dificultad.FACIL,
        ),
        EscenarioAtaque(
            ipAtacante = "212.192.241.90",
            pais = "Desconocido",
            isp = "Desconocido",
            puerto = 23,
            servicioNombre = "Telnet / acceso remoto antiguo",
            esMalicioso = true,
            textoSituacion = "Alguien intenta conectarse por el puerto 23 (Telnet).",
            textoPista = "El puerto 23 (Telnet) es una forma ANTIGUA e insegura de control remoto: envía las contraseñas sin cifrar. Casi siempre es un intento malicioso.",
            leccionAcierto = "Bien. Telnet es inseguro y obsoleto: bloquearlo es lo correcto casi siempre.",
            leccionError = "Riesgo: Telnet manda las claves sin cifrar. Permitirlo facilita que intercepten credenciales.",
            dificultad = Dificultad.MEDIO,
        ),
        EscenarioAtaque(
            ipAtacante = "77.83.4.12",
            pais = "Bulgaria",
            isp = "VPS barato",
            puerto = 21,
            servicioNombre = "FTP / transferencia de archivos",
            esMalicioso = true,
            textoSituacion = "Una conexión externa intenta acceder al puerto 21 (FTP) de tu red.",
            textoPista = "El puerto 21 (FTP) transfiere archivos, pero de forma insegura. Un acceso externo inesperado suele buscar subir o robar archivos.",
            leccionAcierto = "Correcto. Un FTP abierto a desconocidos es un riesgo: bloquear evita que suban o extraigan archivos.",
            leccionError = "Riesgo: permitiste FTP a un desconocido. Podrían subir archivos maliciosos o llevarse los tuyos.",
            dificultad = Dificultad.MEDIO,
        ),

        // --- Escenarios 9-30 (nuevos): más tráfico legítimo, más familias y casos ambiguos ---

        // 9 · HTTP 80 · legítimo · tráfico común
        EscenarioAtaque(
            ipAtacante = "181.30.77.9",
            pais = "Argentina",
            isp = "Telecom",
            puerto = 80,
            servicioNombre = "HTTP / navegación web",
            esMalicioso = false,
            textoSituacion = "Un visitante desde Argentina abre tu sitio web por el puerto 80.",
            textoPista = "El puerto 80 es la web normal (HTTP), sin cifrar pero muy común. Que no lleve candado no lo hace un ataque: es cómo cargan muchísimas páginas.",
            leccionAcierto = "Correcto. Es una visita web normal. No todo el tráfico de internet es peligroso: la mayoría es gente usando servicios.",
            leccionError = "Falso positivo: bloqueaste una visita web legítima. Tu página dejaría de cargar para usuarios reales.",
            dificultad = Dificultad.FACIL,
        ),

        // 10 · IMAP 993 · legítimo · correo entrante
        EscenarioAtaque(
            ipAtacante = "80.58.61.250",
            pais = "España",
            isp = "Movistar",
            puerto = 993,
            servicioNombre = "IMAP / recibir correo (cifrado)",
            esMalicioso = false,
            textoSituacion = "El correo de un empleado se conecta al puerto 993 para descargar sus mensajes.",
            textoPista = "El puerto 993 es IMAP con cifrado: así los programas de correo LEEN el buzón de forma segura. Es parte del día a día de la oficina.",
            leccionAcierto = "Correcto. Recibir correo cifrado es tráfico legítimo. Bloquearlo dejaría a la gente sin poder leer sus emails.",
            leccionError = "Falso positivo: cortaste la recepción de correo. Los empleados no verían sus mensajes entrantes.",
            dificultad = Dificultad.FACIL,
        ),

        // 11 · NTP 123 · legítimo · tráfico común
        EscenarioAtaque(
            ipAtacante = "129.6.15.28",
            pais = "Alemania",
            isp = "Servidor de hora público",
            puerto = 123,
            servicioNombre = "NTP / sincronización de la hora",
            esMalicioso = false,
            textoSituacion = "Tus equipos consultan el puerto 123 para poner en hora sus relojes.",
            textoPista = "El puerto 123 (NTP) sirve para que los dispositivos tengan la hora exacta. Suena menor, pero muchos sistemas de seguridad y certificados fallan si la hora está mal.",
            leccionAcierto = "Correcto. Sincronizar la hora es tráfico legítimo y necesario. Sin él, se rompen certificados y registros.",
            leccionError = "Falso positivo: bloqueaste la hora de red. Con relojes desfasados fallan conexiones seguras y los registros pierden sentido.",
            dificultad = Dificultad.MEDIO,
        ),

        // 12 · Updates 443 · legítimo · actualizaciones
        EscenarioAtaque(
            ipAtacante = "20.190.128.44",
            pais = "Estados Unidos",
            isp = "Microsoft",
            puerto = 443,
            servicioNombre = "Actualizaciones de software",
            esMalicioso = false,
            textoSituacion = "Tus equipos descargan actualizaciones del sistema desde servidores de Microsoft por el puerto 443.",
            textoPista = "Las actualizaciones llegan por HTTPS (443) desde el fabricante. Aunque sea 'tráfico entrando', mantener el software al día es lo que TAPA los agujeros de seguridad.",
            leccionAcierto = "Correcto. Dejar pasar las actualizaciones oficiales mantiene los equipos protegidos y sin fallos conocidos.",
            leccionError = "Error: bloqueaste las actualizaciones. Los equipos se quedan con fallos viejos que los atacantes ya saben aprovechar.",
            dificultad = Dificultad.FACIL,
        ),

        // 13 · POP3 995 · legítimo · correo
        EscenarioAtaque(
            ipAtacante = "200.83.164.30",
            pais = "Chile",
            isp = "VTR",
            puerto = 995,
            servicioNombre = "POP3 / recibir correo (cifrado)",
            esMalicioso = false,
            textoSituacion = "Un programa de correo se conecta al puerto 995 para bajar los mensajes de un buzón.",
            textoPista = "El puerto 995 es POP3 cifrado: otra forma legítima de recibir correo (descarga los mensajes al equipo). Distinta a IMAP, pero igual de normal.",
            leccionAcierto = "Correcto. Es recepción de correo legítima. Existen varios protocolos de email y todos son tráfico normal.",
            leccionError = "Falso positivo: bloqueaste el correo entrante por POP3. El usuario se quedaría sin poder descargar sus mensajes.",
            dificultad = Dificultad.MEDIO,
        ),

        // 14 · App interna 8080 · legítimo AMBIGUO
        EscenarioAtaque(
            ipAtacante = "192.168.1.20",
            pais = "Red local (oficina)",
            isp = "Interno",
            puerto = 8080,
            servicioNombre = "Aplicación web interna de la empresa",
            esMalicioso = false,
            textoSituacion = "Desde un equipo de la propia oficina se accede a una app interna por el puerto 8080.",
            textoPista = "El 8080 parece un puerto 'raro', pero es habitual para aplicaciones web internas. Fíjate en el origen: la IP 192.168.x.x es de tu red local, no de internet.",
            leccionAcierto = "Correcto. Un puerto poco común no es malo por sí solo: aquí es una herramienta interna usada desde tu propia red. Mirar el origen es clave.",
            leccionError = "Falso positivo: bloqueaste una app interna que usa tu equipo. El puerto inusual asustó, pero el origen local dejaba claro que era legítimo.",
            dificultad = Dificultad.DIFICIL,
        ),

        // 15 · VoIP SIP 5060 · legítimo AMBIGUO
        EscenarioAtaque(
            ipAtacante = "51.210.44.6",
            pais = "Francia",
            isp = "OVH",
            puerto = 5060,
            servicioNombre = "VoIP / telefonía de la oficina (SIP)",
            esMalicioso = false,
            textoSituacion = "El sistema de teléfonos de la empresa se conecta al puerto 5060 con su proveedor de llamadas.",
            textoPista = "El puerto 5060 (SIP) es el de la telefonía por internet. Es un servicio contratado por la empresa: número desconocido de puerto, pero función legítima y esperada.",
            leccionAcierto = "Correcto. Que un puerto sea poco familiar no lo hace un ataque: aquí es la telefonía de la oficina funcionando.",
            leccionError = "Falso positivo: bloqueaste la telefonía VoIP. La empresa se quedaría sin poder hacer o recibir llamadas.",
            dificultad = Dificultad.DIFICIL,
        ),

        // 16 · Videollamada 443 desde el extranjero · legítimo AMBIGUO
        EscenarioAtaque(
            ipAtacante = "126.208.12.77",
            pais = "Japón",
            isp = "NTT",
            puerto = 443,
            servicioNombre = "Videollamada de trabajo (HTTPS)",
            esMalicioso = false,
            textoSituacion = "Una empleada de viaje en Japón se une a la videollamada del equipo por el puerto 443.",
            textoPista = "El país 'extranjero' asusta, pero el servicio es una videollamada cifrada normal (443) y el usuario es de tu equipo. El origen geográfico por sí solo no define un ataque.",
            leccionAcierto = "Correcto. Un país lejano no equivale a peligro: aquí es una compañera trabajando en remoto. Juzga el servicio y el usuario, no solo el mapa.",
            leccionError = "Falso positivo: bloqueaste a tu propia compañera por estar de viaje. El país extranjero no era motivo suficiente para cortar.",
            dificultad = Dificultad.DIFICIL,
        ),

        // 17 · SSH del admin desde IP conocida · legítimo AMBIGUO
        EscenarioAtaque(
            ipAtacante = "190.85.12.44",
            pais = "Colombia",
            isp = "Oficina (IP conocida)",
            puerto = 22,
            servicioNombre = "SSH / acceso remoto del administrador",
            esMalicioso = false,
            textoSituacion = "El administrador entra por SSH (puerto 22) desde la IP fija y conocida de la oficina para mantener el servidor.",
            textoPista = "Antes viste que un SSH desde fuera es peligroso. Pero aquí el origen es la IP conocida del propio administrador: el mismo puerto puede ser legítimo según QUIÉN se conecta.",
            leccionAcierto = "Excelente. No basta con mirar el puerto: aquí el origen es de confianza y era el admin haciendo su trabajo. Discriminar es la habilidad clave.",
            leccionError = "Falso positivo: bloqueaste al administrador desde su IP conocida. Bloquear por reflejo 'todo el SSH' también rompe el trabajo legítimo.",
            dificultad = Dificultad.DIFICIL,
        ),

        // 18 · PostgreSQL 5432 · malicioso · BD expuesta
        EscenarioAtaque(
            ipAtacante = "178.62.14.201",
            pais = "Ucrania",
            isp = "Hosting",
            puerto = 5432,
            servicioNombre = "PostgreSQL / base de datos",
            esMalicioso = true,
            textoSituacion = "Una conexión externa intenta llegar directamente al puerto 5432 de tu base de datos.",
            textoPista = "El puerto 5432 (PostgreSQL) es otra base de datos, como MySQL. Ninguna base de datos debería aceptar conexiones directas desde internet: ahí vive la información.",
            leccionAcierto = "Correcto. Da igual la marca de base de datos: ninguna debe estar abierta al exterior. Bloquear protege los datos.",
            leccionError = "Grave: dejaste tu base de datos accesible desde internet. Un extraño podría leer o borrar toda la información.",
            dificultad = Dificultad.MEDIO,
        ),

        // 19 · MongoDB 27017 · malicioso · BD expuesta
        EscenarioAtaque(
            ipAtacante = "116.62.9.180",
            pais = "China",
            isp = "China Telecom",
            puerto = 27017,
            servicioNombre = "MongoDB / base de datos",
            esMalicioso = true,
            textoSituacion = "Alguien busca conectarse al puerto 27017 de tu base de datos MongoDB.",
            textoPista = "El puerto 27017 (MongoDB) es tristemente famoso: muchas quedaron abiertas SIN contraseña y les robaron todos los datos. Nunca debe verse desde internet.",
            leccionAcierto = "Correcto. Bases de datos expuestas y sin clave son causa de fugas masivas. Bloquear el acceso externo es la defensa básica.",
            leccionError = "Muy grave: expusiste una MongoDB al exterior. Es uno de los descuidos que más filtraciones de datos ha provocado.",
            dificultad = Dificultad.MEDIO,
        ),

        // 20 · Redis 6379 · malicioso · BD expuesta / sin auth
        EscenarioAtaque(
            ipAtacante = "103.21.58.44",
            pais = "India",
            isp = "Airtel",
            puerto = 6379,
            servicioNombre = "Redis / almacén de datos en memoria",
            esMalicioso = true,
            textoSituacion = "Una conexión externa intenta alcanzar el puerto 6379 (Redis) de tu servidor.",
            textoPista = "Redis (6379) por defecto NO pide contraseña. Si queda abierto a internet, cualquiera entra sin credenciales y puede leer o alterar los datos.",
            leccionAcierto = "Correcto. Servicios que vienen sin autenticación por defecto son un regalo para el atacante si se exponen. Bloquear es imprescindible.",
            leccionError = "Grave: dejaste Redis abierto. Como no exige contraseña, un extraño podría manipular tus datos de inmediato.",
            dificultad = Dificultad.MEDIO,
        ),

        // 21 · SQL Server 1433 · malicioso · BD expuesta
        EscenarioAtaque(
            ipAtacante = "191.242.30.66",
            pais = "Brasil",
            isp = "Vivo",
            puerto = 1433,
            servicioNombre = "SQL Server / base de datos",
            esMalicioso = true,
            textoSituacion = "Un intento externo llega al puerto 1433 de tu base de datos SQL Server.",
            textoPista = "El puerto 1433 (SQL Server) es la base de datos típica de muchas empresas. Igual que las demás, no debe atender peticiones desde fuera de la red.",
            leccionAcierto = "Correcto. Otra base de datos corporativa que solo debe usarse desde dentro. Bloquear el acceso externo protege el negocio.",
            leccionError = "Grave: expusiste la base de datos de la empresa. Es un objetivo directo para robar información de clientes.",
            dificultad = Dificultad.MEDIO,
        ),

        // 22 · SSH fuerza bruta · malicioso · fuerza bruta
        EscenarioAtaque(
            ipAtacante = "14.225.19.88",
            pais = "Vietnam",
            isp = "Servidor VPS",
            puerto = 22,
            servicioNombre = "SSH / acceso remoto al servidor",
            esMalicioso = true,
            textoSituacion = "Miles de intentos de inicio de sesión golpean el puerto 22 probando usuarios y contraseñas sin parar.",
            textoPista = "Antes viste UN intento de SSH sospechoso. Esto es distinto: MILES de intentos seguidos = fuerza bruta, un robot probando claves hasta acertar.",
            leccionAcierto = "Correcto. Un bombardeo de intentos de acceso es fuerza bruta. Bloquear el origen frena que adivinen la contraseña por repetición.",
            leccionError = "Riesgo alto: dejaste correr una fuerza bruta contra SSH. Con suficientes pruebas podrían dar con la clave y tomar el servidor.",
            dificultad = Dificultad.FACIL,
        ),

        // 23 · FTP fuerza bruta · malicioso · fuerza bruta
        EscenarioAtaque(
            ipAtacante = "197.210.55.12",
            pais = "Nigeria",
            isp = "Hosting",
            puerto = 21,
            servicioNombre = "FTP / transferencia de archivos",
            esMalicioso = true,
            textoSituacion = "El puerto 21 recibe intentos de login repetidos, uno tras otro, con contraseñas distintas.",
            textoPista = "La fuerza bruta no es solo cosa de SSH o RDP: también atacan protocolos viejos como FTP probando claves en cadena hasta entrar.",
            leccionAcierto = "Correcto. Reconociste fuerza bruta en otro servicio. El patrón de 'muchos intentos seguidos' delata el ataque, sea cual sea el puerto.",
            leccionError = "Riesgo: permitiste una fuerza bruta contra FTP. Si aciertan la clave, acceden a los archivos compartidos.",
            dificultad = Dificultad.FACIL,
        ),

        // 24 · SMB 445 · malicioso · protocolo inseguro
        EscenarioAtaque(
            ipAtacante = "45.9.148.33",
            pais = "Rusia",
            isp = "Desconocido",
            puerto = 445,
            servicioNombre = "SMB / compartir archivos en red",
            esMalicioso = true,
            textoSituacion = "Una conexión externa intenta entrar por el puerto 445 (compartir archivos de Windows).",
            textoPista = "El puerto 445 (SMB) sirve para compartir carpetas DENTRO de una red, nunca hacia internet. Por ahí se propagaron ransomware famosos como WannaCry.",
            leccionAcierto = "Correcto. SMB abierto a internet es una puerta clásica de gusanos y ransomware. Solo debe usarse en la red interna: bloquear es lo correcto.",
            leccionError = "Muy grave: dejaste SMB expuesto. Es exactamente la vía que usaron ataques masivos de ransomware para propagarse.",
            dificultad = Dificultad.MEDIO,
        ),

        // 25 · Escaneo de puertos · malicioso · reconocimiento
        EscenarioAtaque(
            ipAtacante = "185.220.101.7",
            pais = "Países Bajos",
            isp = "Servidor VPS",
            puerto = 1,
            servicioNombre = "Escaneo de puertos (muchos a la vez)",
            esMalicioso = true,
            textoSituacion = "Una misma IP toca decenas de puertos distintos de tu red en pocos segundos, uno tras otro.",
            textoPista = "Esto no busca un servicio concreto: prueba MUCHOS puertos para ver cuáles están abiertos. Es reconocimiento, el paso previo a un ataque para mapear tu red.",
            leccionAcierto = "Correcto. Un barrido de puertos es un explorador buscando por dónde entrar. Bloquear el origen corta el reconocimiento antes del ataque real.",
            leccionError = "Riesgo: dejaste que te escanearan a gusto. Ahora el atacante sabe qué puertos tienes abiertos y por dónde intentar colarse.",
            dificultad = Dificultad.DIFICIL,
        ),

        // 26 · SNMP 161 · malicioso · reconocimiento / inseguro
        EscenarioAtaque(
            ipAtacante = "88.247.12.9",
            pais = "Turquía",
            isp = "Desconocido",
            puerto = 161,
            servicioNombre = "SNMP / gestión de dispositivos",
            esMalicioso = true,
            textoSituacion = "Un extraño consulta el puerto 161 de tus equipos desde internet.",
            textoPista = "El puerto 161 (SNMP) sirve para administrar equipos, pero también revela mucha información interna (nombres, versiones, configuración). Abierto a internet, es una filtración de datos útiles para atacar.",
            leccionAcierto = "Correcto. SNMP expuesto entrega detalles internos a cualquiera. Bloquearlo evita que un extraño estudie tus equipos.",
            leccionError = "Riesgo: dejaste SNMP accesible. El atacante obtiene un mapa de tus dispositivos y sus debilidades sin esfuerzo.",
            dificultad = Dificultad.DIFICIL,
        ),

        // 27 · Puerto C2 4444 · malicioso · malware
        EscenarioAtaque(
            ipAtacante = "5.188.206.18",
            pais = "Irán",
            isp = "Desconocido",
            puerto = 4444,
            servicioNombre = "Puerto de control remoto (usado por malware)",
            esMalicioso = true,
            textoSituacion = "Un equipo intenta conectarse hacia afuera por el puerto 4444 a una dirección desconocida.",
            textoPista = "El 4444 no es un servicio normal de oficina: es un puerto muy usado por herramientas de ataque para que un intruso controle el equipo a distancia (C2).",
            leccionAcierto = "Correcto. Un puerto alto y raro hablando con un desconocido suele ser malware llamando a casa. Bloquear corta el control del atacante.",
            leccionError = "Grave: permitiste una conexión típica de malware. Si el equipo ya estaba infectado, acabas de dejar que lo controlen.",
            dificultad = Dificultad.MEDIO,
        ),

        // 28 · Backdoor 31337 · malicioso · malware
        EscenarioAtaque(
            ipAtacante = "121.78.44.201",
            pais = "Corea del Sur",
            isp = "Servidor VPS",
            puerto = 31337,
            servicioNombre = "Puerta trasera (backdoor)",
            esMalicioso = true,
            textoSituacion = "Llega una conexión al puerto 31337, un número que no corresponde a ningún servicio legítimo.",
            textoPista = "El 31337 es un puerto 'de firma' históricamente usado por puertas traseras. Ningún programa serio lo usa: verlo activo casi siempre significa que algo malicioso está escuchando.",
            leccionAcierto = "Correcto. Puertos con fama de backdoor son señal de alarma. Bloquear evita que alguien use esa puerta trasera para entrar.",
            leccionError = "Grave: dejaste abierta una puerta trasera conocida. Es una invitación directa para que un atacante tome el control.",
            dificultad = Dificultad.DIFICIL,
        ),

        // 29 · Exfiltración 50100 · malicioso · exfiltración
        EscenarioAtaque(
            ipAtacante = "193.107.216.40",
            pais = "Polonia",
            isp = "Desconocido",
            puerto = 50100,
            servicioNombre = "Salida masiva de datos a una IP desconocida",
            esMalicioso = true,
            textoSituacion = "Un equipo interno envía GRANDES cantidades de datos hacia afuera, a una dirección extraña, por un puerto alto poco común.",
            textoPista = "Aquí lo raro no es que entren, sino que SALE mucha información hacia un destino desconocido. Eso es exfiltración: alguien robando datos y sacándolos de tu red.",
            leccionAcierto = "Correcto. Un flujo grande de datos saliendo a un destino raro es robo de información. Bloquearlo frena la fuga.",
            leccionError = "Grave: dejaste que se llevaran datos fuera de la red. La exfiltración es el momento en que el robo se consuma.",
            dificultad = Dificultad.DIFICIL,
        ),

        // 30 · SMTP 25 envío masivo · malicioso AMBIGUO (spam)
        EscenarioAtaque(
            ipAtacante = "36.89.140.22",
            pais = "Indonesia",
            isp = "Desconocido",
            puerto = 25,
            servicioNombre = "SMTP / correo (envío masivo)",
            esMalicioso = true,
            textoSituacion = "Desde fuera de tu red, alguien usa el puerto 25 para lanzar miles de correos en poco tiempo.",
            textoPista = "El correo es legítimo (viste el puerto 587). Pero el 25 abierto y con envíos masivos desde un extraño suele ser un bot usando tu red para mandar spam. El servicio es normal; el patrón, no.",
            leccionAcierto = "Correcto. No basta con reconocer 'correo': un envío masivo desde fuera es abuso para spam. Fíjate en el comportamiento, no solo en el servicio.",
            leccionError = "Riesgo: permitiste un envío masivo sospechoso. Podrían estar usando tu red para spam y acabar bloqueada por medio internet.",
            dificultad = Dificultad.DIFICIL,
        ),
    )
}
