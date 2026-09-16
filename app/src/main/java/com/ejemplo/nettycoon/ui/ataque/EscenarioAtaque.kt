package com.ejemplo.nettycoon.ui.ataque

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
)

/**
 * Catálogo fijo de los 8 escenarios pedagógicos. El orden aquí es solo el de definición;
 * el ViewModel decide cuál mostrar mediante su selector.
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
        ),
    )
}
