package com.example.guitartuner.dsp

/**
 * 实时音高检测流水线（纯 Kotlin，平台无关）。
 *
 * 组合：滑动窗分帧 → 静音门限 → YIN 基频检测 → 结果平滑。
 * Android 层只需喂入 PCM 块，通过 [push] 的回调收到平滑后的基频（Hz）；
 * 静音或未检出时回调 null（同时重置平滑器，避免旧值残留到下一次拨弦）。
 */
class PitchPipeline(
    private val sampleRate: Int = 44100,
    frameSize: Int = 2048,
    hopSize: Int = 512,
    private val gate: RmsGate = RmsGate(),
    private val smoother: Smoother = ExponentialSmoother(),
) {
    private val window = SlidingWindow(frameSize, hopSize)

    fun reset() {
        window.reset()
        gate.reset()
        smoother.reset()
    }

    /**
     * 喂入一段 PCM，每个就绪窗口回调一次 [onResult]。
     *
     * @return 本段数据触发处理的窗口数
     */
    fun push(samples: ShortArray, onResult: (Float?) -> Unit): Int {
        return window.push(samples) { frame ->
            if (gate.process(frame)) {
                val f0 = PitchDetector.detect(frame, sampleRate)
                if (f0 != null) {
                    onResult(smoother.smooth(f0))
                } else {
                    smoother.reset()
                    onResult(null)
                }
            } else {
                smoother.reset()
                onResult(null)
            }
        }
    }
}
