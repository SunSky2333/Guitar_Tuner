package com.example.guitartuner.dsp

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * RMS 与静音门限（文档 §7.2 / §9.2）。
 *
 * 无人弹奏时 YIN 会给出乱跳的假频率，因此先用 RMS 做静音检测：
 * RMS 高于门限才把帧送入检测器，否则直接判为静音。
 *
 * 迟滞（hysteresis）用于抑制门限附近的临界抖动：由静音进入有声需超过
 * 上阈值 [thresholdDbfs]，由有声回到静音需低于下阈值 [thresholdDbfs] − [hysteresisDb]。
 * 建议门限约 −50 dBFS，需真机标定。
 */
object Rms {

    /** 16-bit PCM 满量程 */
    const val FULL_SCALE = 32768f

    /** 静音帧的 dBFS 兜底值（log10(0) 为负无穷） */
    const val SILENCE_DBFS = -120f

    /** 帧的 RMS 幅值（原始样本域，范围 0..32768） */
    fun rms(frame: ShortArray): Float {
        if (frame.isEmpty()) return 0f
        var sum = 0.0
        for (s in frame) {
            val v = s.toFloat()
            sum += v * v
        }
        return sqrt(sum / frame.size).toFloat()
    }

    /** RMS 转 dBFS：20·log10(rms / 32768)。静音帧返回 [SILENCE_DBFS]。 */
    fun rmsDbfs(frame: ShortArray): Float {
        val r = rms(frame)
        if (r <= 0f) return SILENCE_DBFS
        return (20.0 * log10((r / FULL_SCALE).toDouble())).toFloat()
    }
}

/**
 * 有状态静音门限（带迟滞）。
 *
 * @param thresholdDbfs 上阈值（dBFS），高于此值判为有声，建议 −50
 * @param hysteresisDb  迟滞带宽（dB），下阈值 = thresholdDbfs − hysteresisDb，建议 6
 */
class RmsGate(
    private val thresholdDbfs: Float = -50f,
    private val hysteresisDb: Float = 6f,
) {
    /** 当前是否静音 */
    var silent: Boolean = true
        private set

    private val upper: Float get() = thresholdDbfs
    private val lower: Float get() = thresholdDbfs - hysteresisDb

    /**
     * 处理一帧 PCM，返回是否有有效声音（true = 非静音，应送入检测器）。
     */
    fun process(frame: ShortArray): Boolean {
        val db = Rms.rmsDbfs(frame)
        silent = when {
            silent && db >= upper -> false      // 静音 → 有声：需超过上阈值
            !silent && db < lower -> true       // 有声 → 静音：需低于下阈值
            else -> silent                       // 迟滞区内保持原状态
        }
        return !silent
    }

    fun reset() {
        silent = true
    }
}
