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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.media3.common.Player
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
        
        // Configuración para mantener la pantalla del televisor siempre encendida
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            // Contenedor principal con fondo negro
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                val videoUrl = "https://1206618505.rsc.cdn77.org/LS-ATL-59020-1/tracks-v1a1/mono.ts.m3u8"
                SimplePlayer(url = videoUrl)
            }
        }
    }
}

/**
 * Componente que gestiona el reproductor de video ExoPlayer con auto-reconexión.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SimplePlayer(url: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    // Inicialización del reproductor
    val exoPlayer = remember {
        val player = ExoPlayer.Builder(context).build()
        player.apply {
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(5000)
                        .build()
                )
                .build()
            
            setMediaItem(mediaItem)
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = (playbackState != Player.STATE_READY)
                    if (playbackState == Player.STATE_READY) {
                        isError = false 
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    isError = true
                    isLoading = false
                    // REINTENTO AUTOMÁTICO INFINITO: Cada 5 segundos
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (isError) { 
                            prepare()
                            play()
                        }
                    }, 5000)
                }
            })
            
            prepare()
            playWhenReady = true
        }
        player
    }

    // Gestión del ciclo de vida para evitar congelamiento al encender la TV
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.stop() 
                Lifecycle.Event.ON_RESUME -> {
                    isError = false
                    isLoading = true
                    val mediaItem = MediaItem.Builder()
                        .setUri(url)
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(5000)
                                .build()
                        )
                        .build()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Capa de mensajes de estado
        if (isError) {
            Text(
                text = "Señal no disponible. Reintentando...",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (isLoading) {
            Text(
                text = "Cargando señal en vivo...",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Indicador visual LIVE
        if (!isLoading && !isError) {
            StatusBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatusBadge(modifier: Modifier = Modifier) {
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
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White)
            )
            Text(
                text = " LIVE TV",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
