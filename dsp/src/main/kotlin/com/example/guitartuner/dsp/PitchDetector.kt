package com.example.guitartuner.dsp

/**
 * YIN 基频检测器。
 *
 * 实现文档 §4 所述四步：差分函数 → 累积均值归一化 → 阈值检测 → 抛物线插值。
 * 纯 Kotlin，无任何 Android 依赖，可在 JVM 直接运行与测试。
 *
 * 推荐参数（文档 §4.3）：fs = 44100 Hz，帧长 2048 点，fmin = 55 Hz，fmax = 1200 Hz，threshold = 0.15。
 */
object PitchDetector {

    /**
     * 从一帧 PCM 数据中估计基频。
     *
     * @param frame 一帧 16-bit PCM 样本（ShortArray），推荐 2048 点（~46 ms @ 44.1 kHz）
     * @param sampleRate 采样率，Hz
     * @param fmin 最低可检测频率，Hz（决定 τ_max，覆盖吉他 drop-C 的 C2 = 65.41 Hz）
     * @param fmax 最高可检测频率，Hz（限制搜索区间下界，避免误判）
     * @param threshold YIN 阈值（0.1~0.2，论文经验值 0.15）
     * @return 估计基频 f0（Hz）；未检测到（如静音帧）返回 null
     */
    fun detect(
        frame: ShortArray,
        sampleRate: Int,
        fmin: Float = 55f,
        fmax: Float = 1200f,
        threshold: Float = 0.15f,
    ): Float? {
        val n = frame.size
        val tauMax = (sampleRate / fmin).toInt()      // 55 Hz → τmax ≈ 801，覆盖降调 C2
        val cmnd = FloatArray(tauMax + 1)
        cmnd[0] = 1f
        var running = 0f

        // 第 1~2 步：差分函数 + 累积均值归一化
        for (tau in 1..tauMax) {
            var s = 0f
            for (j in 0 until n - tau) {
                val d = frame[j] - frame[j + tau]
                s += (d * d).toFloat()
            }
            running += s
            cmnd[tau] = if (running > 0f) s * tau / running else 1f
        }

        // 第 3 步：阈值检测 + 局部最小（用 while 循环，便于在命中后继续右移找局部最小）
        var tauEst = -1
        var tau = (sampleRate / fmax).toInt().coerceAtLeast(1)
        while (tau <= tauMax) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 <= tauMax && cmnd[tau + 1] < cmnd[tau]) tau++
                tauEst = tau
                break
            }
            tau++
        }
        if (tauEst < 0) return null

        // 第 4 步：抛物线插值（亚采样精度）
        var tauRef = tauEst.toFloat()
        if (tauEst in 1 until tauMax) {
            val s0 = cmnd[tauEst - 1]
            val s1 = cmnd[tauEst]
            val s2 = cmnd[tauEst + 1]
            val denom = 2f * (2f * s1 - s2 - s0)
            if (denom != 0f) tauRef = tauEst + (s2 - s0) / denom
        }
        return sampleRate / tauRef
    }
}
