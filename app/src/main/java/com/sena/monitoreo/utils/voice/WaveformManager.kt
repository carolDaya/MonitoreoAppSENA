package com.sena.monitoreo.utils.voice

import com.masoudss.lib.WaveformSeekBar
import kotlinx.coroutines.*
import kotlin.random.Random

class WaveformManager(
    private val waveformSeekBar: WaveformSeekBar
) {
    companion object {
        private const val TAG = "WaveformManager"
    }

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

        animationJob = scope.launch(Dispatchers.Main) { // Usamos Dispatchers.Main para manipulación de UI
            val estimatedDurationMs = (textLength * 80).toLong().coerceAtLeast(3000L)
            val steps = estimatedDurationMs / 50L
            val progressStep = waveformSeekBar.maxProgress / steps.toFloat()

            try {
                for (i in 0 until steps.toInt()) {
                    if (!isAnimating || isActive.not()) break // Verificar isActive
                    waveformSeekBar.progress += progressStep
                    val dynamicSamples = IntArray(100) { Random.nextInt(5, 95) }
                    waveformSeekBar.setSampleFrom(dynamicSamples)
                    delay(50L)
                }
            } catch (e: CancellationException) {
                // Se canceló la corutina
            } finally {
                // Solo llamar al callback si no fue cancelado explícitamente y aún estamos activos
                if (isActive) onAnimationEnd()
                stopAnimationCleanup()
            }
        }
    }

    fun startLongAnimation(totalDurationMs: Long, scope: CoroutineScope, onAnimationEnd: () -> Unit = {}) {
        stopAnimation()
        isAnimating = true

        animationJob = scope.launch(Dispatchers.Main) {
            val steps = totalDurationMs / 50L
            val progressStep = waveformSeekBar.maxProgress / steps.toFloat()

            try {
                for (i in 0 until steps.toInt()) {
                    if (!isAnimating || isActive.not()) break
                    waveformSeekBar.progress += progressStep
                    val dynamicSamples = IntArray(100) { Random.nextInt(5, 95) }
                    waveformSeekBar.setSampleFrom(dynamicSamples)
                    delay(50L)
                }
            } catch (e: CancellationException) {
                // Se canceló la corutina
            } finally {
                if (isActive) onAnimationEnd()
                stopAnimationCleanup()
            }
        }
    }

    fun startContinuousAnimation(scope: CoroutineScope, onAnimationEnd: () -> Unit = {}) {
        stopAnimation()
        isAnimating = true

        animationJob = scope.launch(Dispatchers.Main) {
            var progress = 0f
            val maxProgress = waveformSeekBar.maxProgress

            try {
                while (isAnimating && isActive) {
                    progress += 2f
                    if (progress > maxProgress) {
                        progress = 0f
                    }

                    waveformSeekBar.progress = progress

                    val dynamicSamples = IntArray(100) { Random.nextInt(10, 90) }
                    waveformSeekBar.setSampleFrom(dynamicSamples)

                    delay(40L)
                }
            } catch (e: CancellationException) {
                // Se canceló la corutina
            } finally {
                if (isActive) onAnimationEnd()
                stopAnimationCleanup()
            }
        }
    }

    fun startTimedAnimation(totalDurationMs: Long, scope: CoroutineScope, onAnimationEnd: () -> Unit = {}) {
        stopAnimation()
        isAnimating = true

        animationJob = scope.launch(Dispatchers.Main) {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + totalDurationMs

            try {
                while (isAnimating && System.currentTimeMillis() < endTime && isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (elapsed.toFloat() / totalDurationMs.toFloat()) * waveformSeekBar.maxProgress

                    waveformSeekBar.progress = progress.coerceAtMost(waveformSeekBar.maxProgress)

                    val dynamicSamples = IntArray(100) { Random.nextInt(10, 90) }
                    waveformSeekBar.setSampleFrom(dynamicSamples)

                    delay(40L)
                }
            } catch (e: CancellationException) {
            } finally {
                if (isActive) onAnimationEnd()
                stopAnimationCleanup()
            }
        }
    }

    private fun stopAnimationCleanup() {
        isAnimating = false
        animationJob = null
        waveformSeekBar.progress = 0f
    }

    fun stopAnimation() {
        isAnimating = false
        animationJob?.cancel()
    }
}