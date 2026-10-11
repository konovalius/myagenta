package com.example.myagent

import android.app.Application
import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.example.myagent.data.repository.MediaRepository
import com.example.myagent.data.service.MediaScannerService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class MyAgentApp : Application(), ImageLoaderFactory {
    
    @Inject
    lateinit var mediaScannerService: MediaScannerService

    @Inject
    lateinit var mediaRepository: MediaRepository
    
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            load(this@MyAgentApp, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = "MyAgent/1.0 (Android; ${BuildConfig.APPLICATION_ID})"

            // Постоянный кэш тайлов OSM: снижает нагрузку на прокси VPS.
            // Пути кладём в app-specific external storage — доступ без разрешений, живёт до удаления приложения.
            val baseDir = File(getExternalFilesDir(null) ?: filesDir, "osmdroid").apply { mkdirs() }
            val tileDir = File(baseDir, "tiles").apply { mkdirs() }
            osmdroidBasePath = baseDir
            osmdroidTileCache = tileDir

            // Размеры задаём ПОСЛЕ load(), иначе load() может урезать их по свободному месту.
            tileFileSystemCacheMaxBytes = 200L * 1024 * 1024   // 200 МБ
            tileFileSystemCacheTrimBytes = 180L * 1024 * 1024  // чистим до 180 МБ

            Log.d("OSM", "UserAgent = $userAgentValue; tileCache=$tileDir; max=200MB")
        }
        
        // Запускаем сканирование медиа через Coroutine
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Даем приложению немного времени для инициализации
                kotlinx.coroutines.delay(2000)
                
                Log.wtf("MyAgentApp", "Запуск сканирования медиа при старте приложения")
                
                // Сканируем существующие фото
                mediaScannerService.scanExistingPhotos()
                
                // Сканируем существующие видео
                mediaScannerService.scanExistingVideos()

                // Исправляем тип видео, записанных как 'photo'
                mediaRepository.fixVideoTypes()
                Log.wtf("MyAgentApp", "fixVideoTypes выполнен")
                
                Log.wtf("MyAgentApp", "Сканирование медиа завершено успешно")
            } catch (e: Exception) {
                Log.wtf("MyAgentApp", "Ошибка при сканировании медиа", e)
            }
        }
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
}