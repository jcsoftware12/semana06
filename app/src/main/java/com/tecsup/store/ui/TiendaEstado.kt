package com.tecsup.store.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado compartido de la tienda. Vive en la raíz de la navegación para que
 * el DropdownMenu de cada producto y el badge del drawer lean la misma lista.
 */
class TiendaEstado {
    var favoritos by mutableStateOf(setOf<Int>())
        private set

    val cantidadFavoritos: Int
        get() = favoritos.size

    fun esFavorito(id: Int): Boolean = id in favoritos

    fun alternarFavorito(id: Int): Boolean {
        val agregado = id !in favoritos
        favoritos = if (agregado) favoritos + id else favoritos - id
        return agregado
    }
}
