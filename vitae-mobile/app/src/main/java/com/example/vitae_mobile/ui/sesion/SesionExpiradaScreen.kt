package com.example.vitae_mobile.ui.sesion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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

/** Se muestra tras 5 min de inactividad o un 401; los datos locales ya se borraron. */
@Composable
fun SesionExpiradaScreen(onIniciarSesion: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Encabezado(stringResource(R.string.pantalla_sesion_expirada))
            Text(text = stringResource(R.string.sesion_expirada_texto), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onIniciarSesion, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.pantalla_login))
            }
        }
    }
}
