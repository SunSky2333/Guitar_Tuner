package com.example.guitartuner

import com.example.guitartuner.dsp.PitchPipeline
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 集成测试：滑动窗 → 静音门限 → YIN → 指数平滑 全链路（[PitchPipeline]）。
 */
class PitchPipelineTest {

    private val fs = 44100

    @Test
    fun `full pipeline yields stable E2 from sliding windows`() {
        val pipeline = PitchPipeline(sampleRate = fs)
        val f0 = 82.41f

        // 2 秒 E2 正弦（幅值 20000 ≈ −7.3 dBFS，远超静音门限）
        val total = fs * 2
        val samples = ShortArray(total) { i ->
            (20000.0 * sin(2.0 * PI * f0 * i / fs)).roundToInt().toShort()
        }

        val results = mutableListOf<Float>()
        pipeline.push(samples) { f -> if (f != null) results.add(f) }

        assertTrue(results.isNotEmpty(), "应有窗口通过门限并检出音高")
        val steady = results.last()
        assertTrue(abs(steady - f0) < 0.5f, "稳态 %.3f Hz 应接近 %.2f Hz".format(steady, f0))
    }

    @Test
    fun `silence yields only null results`() {
        val pipeline = PitchPipeline(sampleRate = fs)
        var nullCount = 0
        var nonNullCount = 0

        val silence = ShortArray(fs)  // 1 秒静音
        val emitted = pipeline.push(silence) { f -> if (f == null) nullCount++ else nonNullCount++ }

        assertTrue(emitted > 0, "应至少产生一个窗口")
        assertEquals(0, nonNullCount, "静音不应检出任何音高")
        assertEquals(emitted, nullCount, "所有窗口都应回调 null")
    }
}
