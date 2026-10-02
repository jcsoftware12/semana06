package com.tecsup.store.ui

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tecsup.store.data.Catalogo
import com.tecsup.store.model.Producto
import com.tecsup.store.ui.pantallas.DetalleScreen
import com.tecsup.store.ui.pantallas.FavoritosScreen
import com.tecsup.store.ui.pantallas.InicioScreen
import com.tecsup.store.ui.pantallas.PedidosScreen
import com.tecsup.store.ui.pantallas.PerfilScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavegacion() {
    val estado = remember { TiendaEstado() }
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val contexto = LocalContext.current
    var confirmarSalida by remember { mutableStateOf(false) }

    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route
    val destino = Destino.desdeRuta(ruta)
    val detalleId = entrada?.arguments?.getString("productoId")?.toIntOrNull()
    val productoDetalle = detalleId?.let { Catalogo.producto(it) }

    fun compartir(producto: Producto) {
        val texto = "Mira ${producto.nombre} en TECSUP Store por ${producto.precioTexto()}."
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        contexto.startActivity(Intent.createChooser(intent, "Compartir"))
    }

    fun alternarFavorito(producto: Producto) {
        val agregado = estado.alternarFavorito(producto.id)
        scope.launch {
            snackbar.showSnackbar(
                if (agregado) "${producto.nombre} se agregó a favoritos"
                else "${producto.nombre} se quitó de favoritos"
            )
        }
    }

    fun reportar(producto: Producto) {
        scope.launch { snackbar.showSnackbar("Reporte enviado: ${producto.nombre}") }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = destino != null,
        drawerContent = {
            AppDrawer(
                destinoActual = destino,
                cantidadFavoritos = estado.cantidadFavoritos,
                onDestino = { elegido ->
                    scope.launch { drawerState.close() }
                    nav.navigate(elegido.ruta) {
                        popUpTo(Destino.Inicio.ruta) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCerrarSesion = {
                    scope.launch { drawerState.close() }
                    confirmarSalida = true
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(productoDetalle?.nombre ?: destino?.titulo ?: "TECSUP Store")
                    },
                    navigationIcon = {
                        if (productoDetalle != null) {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Abrir menú")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Destino.Inicio.ruta,
                modifier = Modifier.padding(padding)
            ) {
                composable(Destino.Inicio.ruta) {
                    InicioScreen(
                        estado = estado,
                        onProducto = { nav.navigate("detalle/${it.id}") },
                        onFavorito = { alternarFavorito(it) },
                        onCompartir = { compartir(it) },
                        onReportar = { reportar(it) }
                    )
                }
                composable(Destino.Pedidos.ruta) { PedidosScreen() }
                composable(Destino.Favoritos.ruta) {
                    FavoritosScreen(
                        estado = estado,
                        onProducto = { nav.navigate("detalle/${it.id}") },
                        onFavorito = { alternarFavorito(it) },
                        onCompartir = { compartir(it) },
                        onReportar = { reportar(it) }
                    )
                }
                composable(Destino.Perfil.ruta) { PerfilScreen(estado) }
                composable(
                    route = "detalle/{productoId}",
                    arguments = listOf(navArgument("productoId") { type = NavType.StringType })
                ) {
                    val producto = productoDetalle
                    if (producto == null) {
                        Text("No se encontró el producto")
                    } else {
                        DetalleScreen(
                            producto = producto,
                            esFavorito = estado.esFavorito(producto.id),
                            onFavorito = { alternarFavorito(producto) },
                            onCompartir = { compartir(producto) }
                        )
                    }
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("Cerrar sesión") },
            text = { Text("En esta demo la sesión es local. El catálogo y los favoritos siguen en memoria hasta cerrar la app.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    scope.launch { snackbar.showSnackbar("Sesión cerrada") }
                }) { Text("Cerrar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            }
        )
    }
}
