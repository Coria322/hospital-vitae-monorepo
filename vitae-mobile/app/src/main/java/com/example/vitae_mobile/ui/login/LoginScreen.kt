package com.example.vitae_mobile.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.BuildConfig
import com.example.vitae_mobile.R
import com.example.vitae_mobile.ui.components.Encabezado

@Composable
fun LoginScreen(viewModel: LoginViewModel, onOtp: () -> Unit, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.requiereOtp) {
        if (estado.requiereOtp) {
            viewModel.otpAbierto()
            onOtp()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Encabezado(stringResource(R.string.pantalla_login))
            OutlinedTextField(
                value = estado.correo,
                onValueChange = viewModel::cambiarCorreo,
                label = { Text(stringResource(if (BuildConfig.BACKEND_REAL) R.string.login_usuario else R.string.login_correo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (BuildConfig.BACKEND_REAL) KeyboardType.Text else KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.contrasena,
                onValueChange = viewModel::cambiarContrasena,
                label = { Text(stringResource(R.string.login_contrasena)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            mensajeEnLinea(estado.error)?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = viewModel::iniciarSesion,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(if (estado.cargando) R.string.login_cargando else R.string.login_entrar))
            }
        }
    }

    when (estado.error) {
        ErrorLogin.CuentaBloqueada -> Alerta(R.string.alerta_bloqueada_titulo, R.string.alerta_bloqueada_texto, viewModel::cerrarAlerta)
        ErrorLogin.SinAcceso -> Alerta(R.string.alerta_sin_acceso_titulo, R.string.alerta_sin_acceso_texto, viewModel::cerrarAlerta)
        else -> Unit
    }
}

@Composable
private fun mensajeEnLinea(error: ErrorLogin?): String? = when (error) {
    ErrorLogin.Credenciales -> stringResource(R.string.login_error_credenciales)
    ErrorLogin.Red -> stringResource(R.string.login_error_red)
    ErrorLogin.Incompatible -> stringResource(R.string.login_error_incompatible)
    ErrorLogin.CuentaBloqueada, ErrorLogin.SinAcceso, null -> null
}

// UI_UX_SPEC: los 403 y 423 se muestran como diálogo.
@Composable
private fun Alerta(titulo: Int, texto: Int, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(titulo)) },
        text = { Text(stringResource(texto)) },
        confirmButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.alerta_entendido)) } },
    )
}
