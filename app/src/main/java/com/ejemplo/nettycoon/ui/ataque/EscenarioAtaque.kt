package com.ejemplo.nettycoon.ui.ataque

/**
 * Nivel de dificultad pedagógica de un [EscenarioAtaque], según qué tan OBVIA es la decisión
 * correcta:
 *
 * - [FACIL]: casos de libro (HTTPS legítimo → permitir; fuerza bruta → bloquear).
 * - [MEDIO]: requieren algo de criterio (servicios sensibles expuestos, protocolos obsoletos).
 * - [DIFICIL]: ambiguos o con matiz (lo que parece sospechoso puede ser legítimo, y al revés).
 * - [IMPOSIBLE]: examen final "sin rueditas". La UI apaga TODO el material educativo (servicio,
 *   situación, pista, lección y "¿Por qué?") y muestra solo el contexto en crudo (puerto + IP +
 *   país + ISP); el jugador decide a ciegas. El marcador (acierto/consecuencias) y el registro de
 *   `EventoAtaque` siguen igual que en los demás niveles. El "pelado" es solo de presentación:
 *   este metadato no toca dominio, Room ni el motor.
 *
 * Se usa para servir el catálogo por niveles con una curva de aprendizaje (ver el selector del
 * ViewModel). Es solo metadato de presentación: no toca dominio, Room ni el motor.
 */
enum class Dificultad { FACIL, MEDIO, DIFICIL, IMPOSIBLE }

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
    /**
     * Explicación AMPLIADA del concepto de ciberseguridad detrás del escenario, que se despliega
     * bajo demanda con el botón "¿Por qué?" tras decidir (manual o automatizado). Da el trasfondo
     * (qué es, por qué la decisión correcta lo es, qué pasaría en el mundo real, un consejo), sin
     * repetir la pista ni la lección corta. Default `""` para no romper llamadas/previews/tests que
     * no la especifican; los 30 del catálogo la traen escrita. Si está vacía, la UI no muestra el
     * botón "¿Por qué?".
     */
    val explicacionAmpliada: String = "",
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
            explicacionAmpliada = "Piensa en el puerto 443 como la puerta principal de una tienda con vitrina blindada: es por donde entra casi todo el tráfico web seguro, el del candado del navegador. Permitirlo es lo correcto porque cerrar el 443 sería tapiar la entrada de tu propio local: nadie podría usar tus páginas ni tus servicios en la nube. Si lo bloqueas 'por si acaso', el resultado real es que la gente ve errores de 'no se puede conectar' y cree que tu sitio está caído. En la vida real, los administradores casi nunca cierran el 443; lo que hacen es vigilar qué pasa por esa puerta, no clausurarla.",
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
            explicacionAmpliada = "El puerto 22 (SSH) es como la llave maestra del cuarto de máquinas de tu servidor: quien entra puede tocarlo todo. Bloquear un intento inesperado desde fuera es lo correcto porque nadie ajeno debería tener esa llave, y menos de madrugada desde un país sin relación con tu empresa. Si lo permites y del otro lado hay un atacante probando contraseñas, puede terminar controlando el servidor completo: borrar datos, instalar malware o usarlo para atacar a otros. En la vida real, los administradores solo abren SSH a direcciones IP conocidas (la de la oficina) y jamás lo dejan abierto a todo internet.",
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
            explicacionAmpliada = "El puerto 3389 (escritorio remoto) es como una pantalla y un teclado del equipo colgados en la calle: quien acierte la clave lo maneja como si estuviera sentado frente a él. Miles de intentos en segundos no es una persona escribiendo mal la contraseña: es un robot probando combinaciones sin parar, lo que se llama fuerza bruta. Bloquearlo corta ese goteo antes de que dé con la clave; si lo dejas pasar, con tiempo suficiente entra y toma el equipo, cifra los archivos o los roba. En la vida real, el escritorio remoto expuesto a internet es una de las puertas favoritas del ransomware, por eso se protege con VPN y con bloqueo automático tras varios fallos.",
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
            explicacionAmpliada = "El puerto 587 es la oficina de correos por la que tu servidor SACA los emails que la empresa envía: facturas, confirmaciones, respuestas a clientes. Permitirlo es correcto porque es tráfico propio y esperado, no alguien intentando entrar. Si lo bloqueas, los correos se quedan atascados en la bandeja de salida y nadie recibe nada: el cliente cree que lo ignoras y el negocio se resiente sin que nadie entienda por qué. En la vida real, conviene distinguir el correo que SALE de forma legítima (587) del que ENTRA sin invitación; cerrar el envío propio es dispararse en el pie.",
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
            explicacionAmpliada = "Una base de datos en el puerto 3306 (MySQL) es como la caja fuerte donde guardas los datos de tus clientes; nunca debería estar en la acera, a la vista de la calle. Bloquear una conexión externa directa es correcto porque la base de datos solo debe hablar con TUS aplicaciones dentro de la red, no con desconocidos de internet. Si la dejas expuesta, un atacante puede intentar entrar y llevarse (o borrar) todos los registros de golpe: nombres, correos, contraseñas. En la vida real, muchas grandes filtraciones empezaron exactamente así: una base de datos que quedó 'abierta al público' por descuido.",
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
            explicacionAmpliada = "El puerto 53 (DNS) es la agenda de contactos de internet: traduce el nombre que escribes ('google.com') al número real de la máquina, igual que buscas un nombre en el móvil para no marcar el número de memoria. Permitirlo es correcto porque sin esa traducción los dispositivos saben adónde quieren ir pero no cómo llegar. Si lo bloqueas, deja de cargar TODO a la vez —webs, apps, actualizaciones— y parece que se cayó internet entero, cuando en realidad solo falta la agenda. En la vida real el DNS se permite siempre, pero se vigila: a veces el malware lo usa para camuflar hacia dónde envía los datos robados.",
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
            explicacionAmpliada = "Telnet (puerto 23) es control remoto de hace décadas, con un defecto grave: envía todo —incluida tu contraseña— en texto plano, como gritar la clave por la ventana en vez de pasarla en un sobre cerrado. Bloquearlo es lo correcto porque cualquiera que espíe la red por el camino puede leer esas credenciales tal cual. Si lo permites, no hace falta ni que adivinen nada: basta con escuchar y ya tienen usuario y clave para entrar cuando quieran. En la vida real Telnet está retirado: para lo mismo se usa SSH, que va cifrado; ver Telnet activo suele ser señal de un equipo viejo mal configurado.",
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
            explicacionAmpliada = "El puerto 21 (FTP) es un almacén de archivos con la puerta abierta y sin cámaras: mueve documentos, pero de forma antigua y sin cifrar. Bloquear un acceso externo inesperado es correcto porque un desconocido con entrada al almacén puede tanto llevarse lo que hay como dejar dentro algo dañino disfrazado. Si lo permites, podrían subir un archivo con malware que luego alguien abre, o descargar información confidencial sin que te enteres. En la vida real, para compartir archivos de forma segura se usan versiones cifradas (SFTP/FTPS) o servicios en la nube; el FTP clásico abierto a internet se considera un riesgo que conviene retirar.",
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
            explicacionAmpliada = "El puerto 80 (HTTP) es la versión sin candado de la web: la misma puerta que el 443, pero sin cifrar. Que no lleve candado no lo convierte en un ataque; es como una tienda con la puerta de cristal normal en vez de blindada, y muchísimas páginas siguen cargando así. Permitir una visita normal es correcto; si la bloqueas, tu sitio deja de abrirse para gente real y pierdes visitantes o clientes. En la vida real, en lugar de bloquear el 80 lo habitual es REDIRIGIRLO al 443 para que la navegación pase a ir cifrada, no cortar el acceso.",
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
            explicacionAmpliada = "El puerto 993 (IMAP cifrado) es como el buzón desde el que tu app de correo LEE los mensajes que llegan, y va dentro de un sobre sellado para que nadie los espíe por el camino. Permitirlo es correcto porque es el día a día de la oficina: la gente abriendo su bandeja de entrada. Si lo bloqueas, los correos siguen llegando al servidor pero nadie puede verlos desde su móvil o su portátil, y parece que 'no llega nada' cuando en realidad solo cortaste la lectura. En la vida real se prefiere siempre la versión cifrada (993) frente a la antigua sin cifrar (143), para que las credenciales y los mensajes viajen protegidos.",
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
            explicacionAmpliada = "El puerto 123 (NTP) es el reloj compartido de la oficina: mantiene todos los equipos con la hora exacta. Suena a algo menor, pero muchas cosas serias dependen de ello, igual que una reunión se descuadra si cada quien tiene una hora distinta. Permitirlo es correcto porque los certificados de seguridad (el candado de las webs) y los registros de actividad se apoyan en una hora fiable. Si lo bloqueas, los relojes se desfasan y empiezan a fallar conexiones seguras 'sin motivo', además de que los registros dejan de cuadrar. En la vida real, una hora mal sincronizada complica hasta investigar un incidente, porque no se sabe en qué orden pasaron las cosas.",
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
            explicacionAmpliada = "Estas descargas por HTTPS (443) desde el fabricante son como el mecánico que viene a cambiar la cerradura defectuosa de tu casa: es tráfico que entra, sí, pero para tapar agujeros, no para abrirlos. Permitirlas es correcto porque cada actualización corrige fallos que ya se conocen públicamente. Si las bloqueas, los equipos se quedan con esas cerraduras viejas que los atacantes saben forzar de memoria, y tarde o temprano alguien lo intenta. En la vida real, tener el software al día es de las defensas más baratas y efectivas que existen; casi todos los ataques masivos aprovechan fallos que ya tenían parche disponible.",
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
            explicacionAmpliada = "El puerto 995 (POP3 cifrado) es otra forma legítima de recibir correo, con una diferencia frente a IMAP: en vez de leer los mensajes en el servidor, se los DESCARGA al equipo, como quien vacía el buzón y se lleva las cartas a casa. Permitirlo es correcto porque sigue siendo correo normal, solo que gestionado de otra manera. Si lo bloqueas, el usuario que usa este método se queda sin poder bajar sus mensajes y cree que su correo está roto. En la vida real conviene recordar que un mismo servicio (el email) tiene varios protocolos válidos; ver un puerto poco familiar no significa peligro, hay que fijarse en qué hace.",
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
            explicacionAmpliada = "El puerto 8080 suena 'raro', pero es de lo más común para aplicaciones web internas de una empresa, como una herramienta que solo usan los empleados desde dentro. La clave está en el origen: una IP que empieza por 192.168.x.x es de tu propia red local, no de internet, igual que una llamada desde otra extensión de la misma oficina. Permitirlo es correcto porque es tráfico de casa hacia una herramienta de casa; si lo bloqueas por 'puerto desconocido', dejas sin funcionar una app que tu gente necesita. En la vida real, un buen criterio no mira solo el número de puerto, sino QUIÉN se conecta: el mismo puerto puede ser inofensivo desde dentro y sospechoso desde fuera.",
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
            explicacionAmpliada = "El puerto 5060 (SIP) es la centralita de teléfonos moderna: en vez de llamar por la línea de cobre de toda la vida, la empresa habla con su proveedor de telefonía a través de internet. Es un puerto poco familiar, pero corresponde a un servicio contratado y esperado. Permitirlo es correcto porque es la telefonía de la oficina funcionando; si lo bloqueas, se quedan sin poder hacer ni recibir llamadas y el problema parece un misterio. En la vida real, la telefonía VoIP sí es un blanco de fraude (llamadas caras a escondidas), así que no se bloquea, sino que se limita a hablar solo con el proveedor de confianza y se vigila el gasto.",
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
            explicacionAmpliada = "Aquí lo que asusta es el país (Japón), pero el servicio es una simple videollamada cifrada (por el 443) y quien se conecta es alguien de tu propio equipo trabajando en remoto. Un origen geográfico lejano, por sí solo, no significa ataque: la gente viaja, y bloquear por el mapa es como no dejar entrar a tu compañera a la reunión porque hoy llamó desde otra ciudad. Permitirlo es correcto; si la bloqueas, cortas su trabajo sin razón real. En la vida real, el país es una señal más a tener en cuenta, no una sentencia: se combina con quién es el usuario y qué está haciendo antes de decidir.",
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
            explicacionAmpliada = "Antes viste que un SSH (puerto 22) desde fuera es peligroso; este caso enseña el matiz: el mismo puerto es legítimo cuando quien entra es el administrador desde la IP fija y conocida de la oficina, como el empleado que abre con SU llave por la puerta de siempre, no un extraño forzando la cerradura. Permitirlo es correcto porque el origen es de confianza y era trabajo real de mantenimiento. Si bloqueas 'todo el SSH' por reflejo, dejas al propio admin fuera y frenas el mantenimiento del servidor. En la vida real esto se resuelve con listas de IP permitidas (allowlist): no se cierra el servicio, se limita a quién puede usarlo.",
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
            explicacionAmpliada = "PostgreSQL en el puerto 5432 es otra caja fuerte de datos, distinta marca que MySQL pero misma idea: guarda información valiosa y no debería tener puerta a la calle. Bloquear la conexión externa directa es correcto porque la regla no cambia por el fabricante: ninguna base de datos debe aceptar visitas desde internet, solo desde tus propias aplicaciones internas. Si la expones, un extraño puede intentar leer o borrar todo lo que contiene. En la vida real, el error típico es pensar 'como es otra base de datos, quizá esta sí se puede abrir'; la respuesta es la misma para todas: cerradas al exterior.",
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
            explicacionAmpliada = "MongoDB en el puerto 27017 es una base de datos muy usada por aplicaciones modernas, y tiene una fama triste: durante años muchísimas quedaron abiertas a internet y, encima, SIN contraseña, como una caja fuerte en la acera y con la puerta entornada. Bloquear el acceso externo es correcto porque nadie de fuera tiene por qué tocarla. Si la expones, un atacante ni siquiera necesita romper nada: entra, copia todo y a veces hasta borra los datos y pide rescate. En la vida real han ocurrido filtraciones enormes exactamente por esto; por eso la primera regla es no exponerla y la segunda es exigir siempre autenticación.",
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
            explicacionAmpliada = "Redis (puerto 6379) es un almacén de datos ultrarrápido que las apps usan como memoria de apoyo, y trae un detalle peligroso de fábrica: por defecto NO pide contraseña, es como un cajón sin cerradura pensado para usarse solo dentro de casa. Bloquear el acceso externo es correcto porque, si queda expuesto, cualquiera desde internet entra sin credenciales y manipula o borra lo que hay. Si lo permites, el atacante no tiene ni que esforzarse: la puerta ya estaba abierta. En la vida real, los servicios que vienen sin autenticación por defecto son un regalo para quien busca víctimas fáciles; deben quedarse en la red interna y, aun así, protegerse con contraseña.",
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
            explicacionAmpliada = "SQL Server en el puerto 1433 es la base de datos típica de muchas empresas que trabajan con tecnología Microsoft: ahí suelen vivir clientes, ventas y facturación. Bloquear un intento externo es correcto porque, como toda base de datos, solo debe atender a tus aplicaciones internas, nunca a internet abierto. Si la expones, se convierte en un blanco directo: quien logre entrar accede al corazón del negocio de una sola vez. En la vida real, las bases de datos corporativas se colocan en la parte más protegida de la red, detrás de varias capas, precisamente porque un fallo ahí afecta a todo lo demás.",
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
            explicacionAmpliada = "Antes viste UN intento sospechoso de SSH; esto es distinto en cantidad: MILES de inicios de sesión seguidos, probando usuario y clave sin descanso, como un ladrón que prueba llave tras llave en la cerradura hasta que una entra. Ese patrón de repetición es la firma de la fuerza bruta. Bloquear el origen es correcto porque frena la avalancha antes de que acierte; si lo dejas correr, es cuestión de estadística y tiempo que dé con una contraseña débil. En la vida real esto se combate con claves largas, con bloqueo tras varios fallos y con acceso solo desde IP conocidas, para que probar a ciegas no sirva de nada.",
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
            explicacionAmpliada = "La fuerza bruta no es cosa solo de SSH o del escritorio remoto: aquí atacan el FTP (puerto 21) con intentos de login en cadena, uno tras otro, cambiando la contraseña cada vez. Lo que delata el ataque no es el puerto, sino el patrón: muchísimos intentos seguidos, algo que una persona normal jamás hace. Bloquearlo es correcto; si lo permites y aciertan la clave, entran al almacén de archivos y pueden robar o manipular su contenido. En la vida real, reconocer el patrón de 'demasiados intentos' es clave, porque los atacantes automatizan estos barridos contra cualquier servicio que pida usuario y contraseña.",
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
            explicacionAmpliada = "El puerto 445 (SMB) sirve para compartir carpetas e impresoras DENTRO de una red, como el archivador común de la oficina: útil de puertas para adentro, nunca colgado en la calle. Bloquear un acceso externo es correcto porque ese archivador no tiene ninguna razón para estar accesible desde internet. Si lo expones, te vuelves vulnerable a gusanos que saltan de equipo en equipo solos; por ahí se propagaron ataques de ransomware famosos como WannaCry, cifrando media empresa en minutos. En la vida real, SMB se mantiene estrictamente en la red interna y se aísla, porque su exposición ha causado algunos de los incidentes más costosos de la historia.",
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
            explicacionAmpliada = "Un escaneo de puertos es cuando una misma IP toca decenas de tus puertos en segundos, no para usar un servicio concreto, sino para ver cuáles están abiertos: es el ladrón que recorre la manzana probando cada puerta y ventana para anotar por dónde se podría entrar. Bloquear ese origen es correcto porque es reconocimiento, el paso PREVIO al ataque real. Si lo dejas pasar, el atacante se lleva un mapa de tus puntos débiles y vuelve directo a ellos. En la vida real, detectar y bloquear escaneos temprano da ventaja: cortas al atacante mientras todavía está mirando, antes de que elija por dónde golpear.",
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
            explicacionAmpliada = "El puerto 161 (SNMP) sirve para administrar y consultar equipos de red, pero de paso revela un montón de información interna: nombres de máquinas, versiones, cómo está montada la red. Es como dejar los planos del edificio y la lista de cerraduras pegados en la entrada. Bloquear el acceso externo es correcto porque un extraño no debería poder leer esos detalles. Si lo permites, el atacante estudia tu red cómodamente y descubre qué equipos son viejos o vulnerables sin apenas esfuerzo. En la vida real, SNMP se restringe a la red de gestión interna y se usa en su versión moderna y cifrada, porque las antiguas exponían datos con una simple 'contraseña' que muchos ni cambiaban.",
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
            explicacionAmpliada = "Ojo al detalle: aquí la conexión SALE de un equipo tuyo HACIA fuera, por el puerto 4444, a una dirección desconocida. El 4444 no es un servicio normal de oficina; es un puerto muy usado por herramientas de ataque para que un intruso maneje el equipo a distancia, lo que se llama 'llamar a casa' (command and control). Bloquearlo es correcto porque, si un equipo ya infectado logra esa conexión, el atacante toma el control desde lejos. En la vida real no basta con vigilar lo que ENTRA: vigilar lo que SALE hacia destinos raros es clave para pillar un equipo ya comprometido antes de que haga daño.",
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
            explicacionAmpliada = "El puerto 31337 no corresponde a ningún servicio serio; es un número 'de firma', históricamente asociado a puertas traseras (una entrada secreta que deja un intruso para volver cuando quiera). Verlo activo es como encontrar una puerta extra en la parte de atrás de tu casa que tú no instalaste. Bloquearlo es correcto porque ningún programa legítimo lo usa: su sola presencia es una señal de alarma. Si lo permites, le das paso franco a quien colocó esa puerta. En la vida real, aparte de bloquear, un puerto así obliga a investigar el equipo: probablemente ya haya algo malicioso instalado escuchando ahí.",
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
            explicacionAmpliada = "Aquí lo raro no es que ENTRE algo, sino que SALE mucho: un equipo interno envía una gran cantidad de datos hacia una dirección desconocida por un puerto alto poco común. Eso es exfiltración, el momento en que alguien se lleva la información fuera de tu red, como ver a un empleado cargar cajas de archivos en una furgoneta a medianoche. Bloquearlo es correcto porque frena el robo justo cuando se está consumando. Si lo permites, los datos ya están fuera y no hay vuelta atrás. En la vida real, vigilar las salidas inusuales de datos es de lo más valioso: muchas veces es la única señal de que hubo una brecha, ya en su fase final.",
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
            explicacionAmpliada = "El correo en sí es legítimo (ya viste el 587 para enviar), pero aquí el patrón no cuadra: el puerto 25 abierto y, desde fuera, alguien lanzando miles de correos en poco tiempo. Eso suele ser un bot aprovechando tu red para repartir spam, como un desconocido usando tu dirección de remitente para inundar buzones ajenos. Bloquearlo es correcto porque el servicio es normal, pero el comportamiento es abuso. Si lo permites, además del daño a otros, tu dominio y tus IP pueden acabar en listas negras y entonces tus correos legítimos dejan de llegar. En la vida real hay que mirar el comportamiento, no solo el servicio: 'es correo' no significa 'es bueno'.",
        ),
    )
}
