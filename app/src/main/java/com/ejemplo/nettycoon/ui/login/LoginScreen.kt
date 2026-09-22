package com.ejemplo.nettycoon.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.nettycoon.BuildConfig
import com.ejemplo.nettycoon.auth.AuthViewModel
import com.ejemplo.nettycoon.auth.EstadoOperacion
import com.ejemplo.nettycoon.ui.theme.Espaciado

/**
 * Pantalla de inicio de sesión (Material 3).
 *
 * MVVM: solo habla con [AuthViewModel]. La navegación se resuelve vía callbacks.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavegarARegistro: () -> Unit,
    onAuthExitoso: () -> Unit,
    onBypassDev: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Cuando la operación termina en éxito, deja que el gate de navegación actúe.
    androidx.compose.runtime.LaunchedEffect(estado.operacion) {
        if (estado.operacion is EstadoOperacion.Exito) onAuthExitoso()
    }

    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espaciado.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Marca: título destacado con el acento del tema.
        Text(
            text = "NetTycoon",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Inicia sesión para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Espaciado.xl))

        OutlinedTextField(
            value = estado.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = estado.emailInvalido,
            enabled = !estado.cargando,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Espaciado.md))

        OutlinedTextField(
            value = estado.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Contraseña") },
            singleLine = true,
            isError = estado.passwordInvalido,
            enabled = !estado.cargando,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation =
                if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Ocultar" else "Mostrar")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        val mensajeError = estado.mensajeError
        if (mensajeError != null) {
            Spacer(Modifier.height(Espaciado.sm))
            Text(
                text = mensajeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(Espaciado.lg))

        Button(
            onClick = viewModel::iniciarSesion,
            enabled = !estado.cargando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (estado.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Iniciar sesión")
            }
        }

        Spacer(Modifier.height(Espaciado.sm))
        TextButton(onClick = onNavegarARegistro, enabled = !estado.cargando) {
            Text("¿No tienes cuenta? Regístrate")
        }

        // Bypass de desarrollo: solo se compila y muestra en builds debug.
        if (BuildConfig.DEBUG) {
            TextButton(onClick = onBypassDev, enabled = !estado.cargando) {
                Text("Entrar sin cuenta (dev)")
            }
        }
    }
}
