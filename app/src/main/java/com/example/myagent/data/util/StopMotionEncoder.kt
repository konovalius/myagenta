package com.example.myagent.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Утилита склейки списка JPEG-кадров в MP4 через MediaCodec (H.264) + MediaMuxer.
 * Не зависит от UI: принимает готовые файлы кадров и файл назначения.
 */
object StopMotionEncoder {

    private const val TAG = "StopMotionEncoder"
    private const val MIME_TYPE = "video/avc"
    private const val TIMEOUT_US = 10_000L

    /**
     * Кодирует список JPEG-файлов в MP4.
     * @param frameFiles список файлов-кадров в порядке воспроизведения (jpg)
     * @param outputFile файл, куда сохранить mp4
     * @param fps кадров в секунду итогового видео (например, 10)
     * @param width ширина кадра (например, 1080)
     * @param height высота кадра (например, 1920)
     * @return true если успешно
     */
    suspend fun encode(
        frameFiles: List<File>,
        outputFile: File,
        fps: Int = 10,
        width: Int = 1080,
        height: Int = 1920,
    ): Boolean = withContext(Dispatchers.IO) {
        if (frameFiles.isEmpty()) {
            Log.wtf(TAG, "encode: пустой список кадров")
            return@withContext false
        }

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1

        try {
            outputFile.parentFile?.mkdirs()

            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                setInteger(MediaFormat.KEY_BIT_RATE, (width * height * fps * 0.1).toInt())
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            val frameDurationUs = 1_000_000L / fps
            val total = frameFiles.size

            // Забирает все накопившиеся output-буферы и пишет их в muxer.
            fun drainEncoder(endOfStream: Boolean) {
                val timeout = if (endOfStream) TIMEOUT_US else 0L
                while (true) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)
                    if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (!endOfStream) break
                        continue
                    } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer!!.addTrack(encoder.outputFormat)
                            muxer!!.start()
                            muxerStarted = true
                        }
                        continue
                    } else if (outIndex < 0) {
                        continue
                    }

                    val encodedData = encoder.getOutputBuffer(outIndex)
                    if (encodedData != null) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) {
                                trackIndex = muxer!!.addTrack(encoder.outputFormat)
                                muxer!!.start()
                                muxerStarted = true
                            }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }

            for ((index, frameFile) in frameFiles.withIndex()) {
                val bitmap = decodeAndScale(frameFile, width, height)
                if (bitmap == null) {
                    Log.wtf(TAG, "frame $index/$total: не удалось декодировать ${frameFile.name}")
                    continue
                }

                try {
                    val inIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex < 0) {
                        Log.wtf(TAG, "frame $index/$total: input buffer недоступен")
                        drainEncoder(false)
                        continue
                    }

                    val image = encoder.getInputImage(inIndex)
                    if (image == null) {
                        Log.wtf(TAG, "frame $index/$total: getInputImage вернул null")
                        encoder.queueInputBuffer(inIndex, 0, 0, 0, 0)
                        drainEncoder(false)
                        continue
                    }

                    fillYuvImage(image, bitmap)
                    val ptsUs = index * frameDurationUs
                    val yuvSize = width * height * 3 / 2
                    encoder.queueInputBuffer(inIndex, 0, yuvSize, ptsUs, 0)
                    Log.wtf(TAG, "frame ${index + 1}/$total encoded")
                } finally {
                    bitmap.recycle()
                }

                drainEncoder(false)
            }

            // Сигнал о конце входного потока.
            val eosIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
            if (eosIndex >= 0) {
                encoder.queueInputBuffer(
                    eosIndex, 0, 0,
                    total * frameDurationUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
            drainEncoder(true)

            return@withContext muxerStarted
        } catch (e: Exception) {
            Log.wtf(TAG, "encode error: ${e.message}", e)
            return@withContext false
        } finally {
            try {
                if (muxerStarted) muxer?.stop()
            } catch (e: Exception) {
                Log.wtf(TAG, "muxer.stop error: ${e.message}")
            }
            try {
                muxer?.release()
            } catch (e: Exception) {
                Log.wtf(TAG, "muxer.release error: ${e.message}")
            }
            try {
                encoder?.stop()
            } catch (e: Exception) {
                Log.wtf(TAG, "encoder.stop error: ${e.message}")
            }
            try {
                encoder?.release()
            } catch (e: Exception) {
                Log.wtf(TAG, "encoder.release error: ${e.message}")
            }
        }
    }

    private fun decodeAndScale(file: File, width: Int, height: Int): Bitmap? {
        return try {
            val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            if (decoded.width == width && decoded.height == height) {
                decoded
            } else {
                val scaled = Bitmap.createScaledBitmap(decoded, width, height, true)
                if (scaled != decoded) decoded.recycle()
                scaled
            }
        } catch (e: Exception) {
            Log.wtf(TAG, "decode error ${file.name}: ${e.message}")
            null
        }
    }

    /** Заполняет YUV420-буфер кодека (через [Image]) пикселями из ARGB [bitmap]. */
    private fun fillYuvImage(image: Image, bitmap: Bitmap) {
        val w = image.width
        val h = image.height
        val argb = IntArray(w * h)
        bitmap.getPixels(argb, 0, w, 0, 0, w, h)

        // Y-плоскость.
        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        for (j in 0 until h) {
            val rowStart = j * yRowStride
            val srcRow = j * w
            for (i in 0 until w) {
                val c = argb[srcRow + i]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff
                val y = (((66 * r + 129 * g + 25 * b) + 128) shr 8) + 16
                yBuffer.put(rowStart + i * yPixelStride, y.coerceIn(0, 255).toByte())
            }
        }

        // U/V-плоскости (прореживание 2x2).
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride
        val chromaWidth = w / 2
        val chromaHeight = h / 2
        for (j in 0 until chromaHeight) {
            val uRow = j * uRowStride
            val vRow = j * vRowStride
            val srcRow = (j * 2) * w
            for (i in 0 until chromaWidth) {
                val c = argb[srcRow + i * 2]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff
                val u = (((-38 * r - 74 * g + 112 * b) + 128) shr 8) + 128
                val v = (((112 * r - 94 * g - 18 * b) + 128) shr 8) + 128
                uBuffer.put(uRow + i * uPixelStride, u.coerceIn(0, 255).toByte())
                vBuffer.put(vRow + i * vPixelStride, v.coerceIn(0, 255).toByte())
            }
        }
    }
}
