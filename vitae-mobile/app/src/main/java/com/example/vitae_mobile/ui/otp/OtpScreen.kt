package com.example.vitae_mobile.ui.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.auth.LONGITUD_OTP

@Composable
fun OtpScreen(viewModel: OtpViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.pantalla_otp),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.otp_instruccion, LONGITUD_OTP), style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = estado.codigo,
                onValueChange = viewModel::cambiarCodigo,
                label = { Text(stringResource(R.string.otp_codigo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            if (estado.error) {
                Text(stringResource(R.string.otp_error), color = MaterialTheme.colorScheme.error)
            }
            if (estado.sinConexion) {
                Text(stringResource(R.string.login_error_red), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = viewModel::verificar,
                enabled = !estado.cargando && estado.codigo.length == LONGITUD_OTP,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(if (estado.cargando) R.string.login_cargando else R.string.otp_verificar))
            }
        }
    }
}
