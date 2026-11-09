package com.example.vitae_mobile.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull

/** Utilidades para inspeccionar el árbol semántico (el mismo que ve TalkBack) de la pantalla visible. */

fun SemanticsNode.descendientes(): Sequence<SemanticsNode> = sequence {
    yield(this@descendientes)
    children.forEach { yieldAll(it.descendientes()) }
}

/** Lo que TalkBack leería: el texto y la descripción de contenido del nodo (ya fusionados con los de sus hijos). */
val SemanticsNode.textoHablable: String
    get() = listOfNotNull(
        config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
        config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
    ).joinToString(" ").trim()

/** Botón, interruptor, casilla o cualquier cosa que se pueda tocar. */
val SemanticsNode.esInteractivo: Boolean
    get() = config.contains(SemanticsActions.OnClick) ||
        config.contains(SemanticsProperties.ToggleableState) ||
        config.getOrNull(SemanticsProperties.Role) in setOf(Role.Button, Role.Switch, Role.Checkbox, Role.RadioButton, Role.Tab)

/** Campo de texto que se puede escribir. */
val SemanticsNode.esEditable: Boolean get() = config.contains(SemanticsActions.SetText)

val SemanticsNode.esImagen: Boolean get() = config.getOrNull(SemanticsProperties.Role) == Role.Image

val SemanticsNode.esEncabezado: Boolean get() = config.contains(SemanticsProperties.Heading)
