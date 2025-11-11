package com.sena.monitoreo.utils.voice

import com.masoudss.lib.WaveformSeekBar
import kotlinx.coroutines.*
import kotlin.random.Random

class WaveformManager(
    private val waveformSeekBar: WaveformSeekBar
) {
    private var animationJob: Job? = null
    private var isAnimating = false

    fun setupInitialSamples() {
        val samples = IntArray(100) { Random.nextInt(10, 100) }
        waveformSeekBar.setSampleFrom(samples)
        waveformSeekBar.progress = 0f
    }

    fun startAnimation(textLength: Int, scope: CoroutineScope, onAnimationEnd: () -> Unit = {}) {
        stopAnimation()
        isAnimating = true

        animationJob = scope.launch {
            val estimatedDurationMs = (textLength * 80).toLong().coerceAtLeast(3000L)
            val steps = estimatedDurationMs / 50L
            val progressStep = waveformSeekBar.maxProgress / steps.toFloat()

            for (i in 0 until steps.toInt()) {
                if (!isAnimating) break
                waveformSeekBar.progress += progressStep
                val dynamicSamples = IntArray(100) { Random.nextInt(5, 95) }
                waveformSeekBar.setSampleFrom(dynamicSamples)
                delay(50L)
            }
            onAnimationEnd()
        }
    }

    fun startLongAnimation(totalDurationMs: Long, scope: CoroutineScope, onAnimationEnd: () -> Unit = {}) {
        stopAnimation()
        isAnimating = true

        animationJob = scope.launch {
            val steps = totalDurationMs / 50L
            val progressStep = waveformSeekBar.maxProgress / steps.toFloat()

            for (i in 0 until steps.toInt()) {
                if (!isAnimating) break
                waveformSeekBar.progress += progressStep
                val dynamicSamples = IntArray(100) { Random.nextInt(5, 95) }
                waveformSeekBar.setSampleFrom(dynamicSamples)
                delay(50L)
            }
            onAnimationEnd()
        }
    }

    fun startContinuousAnimation(scope: CoroutineScope, onAnimationEnd: () -> Unit = {}) {
        stopAnimation()
        isAnimating = true

        animationJob = scope.launch {
            var progress = 0f
            val maxProgress = waveformSeekBar.maxProgress

            while (isAnimating) {
                progress += 2f // Incremento más rápido para mejor visualización
                if (progress > maxProgress) {
                    progress = 0f // Reiniciar cuando llega al final
                }

                waveformSeekBar.progress = progress

                // Actualizar samples dinámicamente
                val dynamicSamples = IntArray(100) { Random.nextInt(10, 90) }
                waveformSeekBar.setSampleFrom(dynamicSamples)

                delay(40L) // 25 FPS para animación más suave
            }

            waveformSeekBar.progress = 0f // Reset al finalizar
            onAnimationEnd()
        }
    }

    fun startTimedAnimation(totalDurationMs: Long, scope: CoroutineScope, onAnimationEnd: () -> Unit = {}) {
        stopAnimation()
        isAnimating = true

        animationJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + totalDurationMs

            while (isAnimating && System.currentTimeMillis() < endTime) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / totalDurationMs.toFloat()) * waveformSeekBar.maxProgress

                waveformSeekBar.progress = progress.coerceAtMost(waveformSeekBar.maxProgress)

                // Actualizar samples dinámicamente
                val dynamicSamples = IntArray(100) { Random.nextInt(10, 90) }
                waveformSeekBar.setSampleFrom(dynamicSamples)

                delay(40L)
            }

            onAnimationEnd()
        }
    }

    fun stopAnimation() {
        isAnimating = false
        animationJob?.cancel()
        animationJob = null
        waveformSeekBar.progress = 0f
    }

    fun reset() {
        stopAnimation()
        setupInitialSamples()
    }

    fun isAnimating(): Boolean = isAnimating
}