package com.example.guitartuner

import com.example.guitartuner.dsp.ExponentialSmoother
import com.example.guitartuner.dsp.MedianSmoother
import kotlin.test.Test
import kotlin.test.assertEquals

class SmootherTest {

    @Test
    fun `median smoother drops a spike`() {
        val s = MedianSmoother(windowSize = 3)
        assertEquals(1f, s.smooth(1f))
        assertEquals(1f, s.smooth(1f))
        assertEquals(1f, s.smooth(1f))
        assertEquals(1f, s.smooth(100f))  // [1,1,100] 中位数为 1
        assertEquals(1f, s.smooth(1f))     // [1,100,1] → 1
    }

    @Test
    fun `median smoother even window averages middle two`() {
        val s = MedianSmoother(windowSize = 2)
        s.smooth(4f)
        assertEquals(5f, s.smooth(6f))  // (4 + 6) / 2
    }

    @Test
    fun `exponential alpha 1 passes through unchanged`() {
        val s = ExponentialSmoother(alpha = 1f)
        assertEquals(5f, s.smooth(5f))
        assertEquals(3f, s.smooth(3f))
    }

    @Test
    fun `exponential smoothing converges toward constant input`() {
        val s = ExponentialSmoother(alpha = 0.5f)
        assertEquals(10f, s.smooth(10f))  // 首值直接输出
        assertEquals(5f, s.smooth(0f))    // 0.5·0 + 0.5·10
        assertEquals(2.5f, s.smooth(0f))  // 0.5·0 + 0.5·5
    }

    @Test
    fun `reset clears state`() {
        val s = ExponentialSmoother(alpha = 0.5f)
        s.smooth(10f)
        s.reset()
        assertEquals(7f, s.smooth(7f))  // 重置后首值直接输出
    }
}
