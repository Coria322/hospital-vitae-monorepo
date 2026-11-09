package com.example.vitae_mobile.ui.seguridad

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.vitae_mobile.R
import com.example.vitae_mobile.ui.components.Encabezado

/**
 * Pantalla de sistema para un dispositivo bloqueado (RNF-006, PROV-SEC-011): un título, un mensaje genérico (nunca
 * qué señal se detectó ni qué versión se espera) y "Salir". No hay forma de continuar desde aquí.
 */
@Composable
fun PantallaBloqueo(
    @StringRes titulo: Int,
    @StringRes mensaje: Int,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Encabezado(stringResource(titulo))
            Text(text = stringResource(mensaje), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onSalir, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.bloqueo_salir))
            }
        }
    }
}
