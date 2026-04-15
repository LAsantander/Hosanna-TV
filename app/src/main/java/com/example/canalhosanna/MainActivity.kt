package com.example.canalhosanna

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

/**
 * Actividad principal de la aplicación.
 * Se utiliza @UnstableApi porque algunas funciones de Media3 aún están en fase experimental.
 */
@UnstableApi
class MainActivity : ComponentActivity() {
    
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuración para mantener la pantalla del televisor siempre encendida mientras la app esté abierta
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            // Contenedor principal con fondo negro para evitar destellos blancos al iniciar
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // URL del streaming en vivo (formato HLS/M3U8)
                val videoUrl = "https://1206618505.rsc.cdn77.org/LS-ATL-59020-1/tracks-v1a1/mono.ts.m3u8"
                
                // Llamada al componente del reproductor de video
                SimplePlayer(url = videoUrl)
            }
        }
    }
}

/**
 * Componente que gestiona el reproductor de video ExoPlayer.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SimplePlayer(url: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Inicialización y configuración del reproductor ExoPlayer
    // Se usa 'remember' para que el reproductor no se reinicie al recomponer la UI
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Definir el elemento multimedia a partir de la URL
            setMediaItem(MediaItem.fromUri(url))
            // Preparar el reproductor (empezar a cargar el buffer)
            prepare()
            // Iniciar la reproducción automáticamente cuando esté listo
            playWhenReady = true
        }
    }

    // Gestión del ciclo de vida: Pausar el video si el usuario sale de la app 
    // y liberar los recursos (memoria/red) cuando la app se cierra.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // Pausar si la app pasa a segundo plano
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                // Reanudar si el usuario vuelve a la app
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> Unit
            }
        }
        // Añadir el observador al ciclo de vida
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            // Limpieza: quitar el observador y destruir el reproductor al cerrar el componente
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Interfaz de usuario: Capas apiladas (Video debajo, Logo encima)
    Box(modifier = Modifier.fillMaxSize()) {
        
        // Integración de la vista de Android clásica (PlayerView) dentro de Jetpack Compose
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    // Vincular el ExoPlayer a la vista
                    player = exoPlayer
                    // Ocultar los controles de reproducción (pausa, barra de progreso) para TV
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Indicador visual de "LIVE" (En Vivo) en la esquina superior izquierda
        StatusBadge(
            modifier = Modifier
                .align(Alignment.TopStart) // Alinear arriba a la izquierda
                .padding(32.dp) // Margen de seguridad para pantallas de TV
        )
    }
}

/**
 * Componente visual que muestra la etiqueta "LIVE" roja.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatusBadge(modifier: Modifier = Modifier) {
    // Fondo redondeado con un rojo más profesional (Material Design) y transparencia suave
    Surface(
        modifier = modifier,
        colors = SurfaceDefaults.colors(
            containerColor = Color(0xFFD32F2F).copy(alpha = 0.7f)
        ),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Punto blanco que simula el indicador de grabación/vivo
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            // Texto descriptivo
            Text(
                text = " LIVE TV",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
