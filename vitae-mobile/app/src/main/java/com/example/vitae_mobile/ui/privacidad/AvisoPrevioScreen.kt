package com.example.vitae_mobile.ui.privacidad

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.privacidad.AvisoPrevio

/** Aviso simplificado con enlace al integral, antes de cualquier formulario. No recaba nada: solo informa. */
@Composable
fun AvisoPrevioScreen(onContinuar: () -> Unit, modifier: Modifier = Modifier) {
    val uri = LocalUriHandler.current
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.pantalla_aviso),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.aviso_previo_texto), style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { uri.openUri(AvisoPrevio.URL_INTEGRAL) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.aviso_enlace_integral))
            }
            Button(onClick = onContinuar, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.aviso_previo_continuar))
            }
        }
    }
}
