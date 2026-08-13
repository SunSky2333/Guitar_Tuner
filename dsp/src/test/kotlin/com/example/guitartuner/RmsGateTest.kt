package com.example.guitartuner

import com.example.guitartuner.dsp.Rms
import com.example.guitartuner.dsp.RmsGate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RmsGateTest {

    private val sampleRate = 44100
    private val frameSize = 2048

    /** 生成 RMS 为指定 dBFS 的正弦帧（16-bit PCM） */
    private fun sineAtRmsDbfs(dbfs: Double, freq: Double = 440.0): ShortArray {
        val rmsTarget = 32768.0 * 10.0.pow(dbfs / 20.0)  // RMS 目标（原始域）
        val amp = rmsTarget * sqrt(2.0)                   // 正弦峰值
        return ShortArray(frameSize) { i ->
            (amp * sin(2.0 * PI * freq * i / sampleRate)).roundToInt().toShort()
        }
    }

    @Test
    fun `rms of silence is zero`() {
        assertEquals(0f, Rms.rms(ShortArray(frameSize)))
        assertEquals(-120f, Rms.rmsDbfs(ShortArray(frameSize)))
    }

    @Test
    fun `rmsDbfs of full-scale sine is about -3 dB`() {
        val full = ShortArray(frameSize) { i ->
            (32767.0 * sin(2.0 * PI * 440.0 * i / sampleRate)).roundToInt().toShort()
        }
        val db = Rms.rmsDbfs(full)
        assertTrue(abs(db + 3.01f) < 0.1f, "满量程正弦 dBFS=$db，期望 ≈ -3.01")
    }

    @Test
    fun `gate opens on loud and closes on silence with hysteresis`() {
        val gate = RmsGate(thresholdDbfs = -50f, hysteresisDb = 6f)
        val loud = sineAtRmsDbfs(-20.0)   // 远高于上阈值 -50
        val mid = sineAtRmsDbfs(-53.0)    // 迟滞区（-50 ~ -56 之间）
        val quiet = sineAtRmsDbfs(-80.0)  // 远低于下阈值 -56

        assertFalse(gate.process(quiet), "初始静音帧应判为静音")

        assertTrue(gate.process(loud), "强信号应打开门限")
        assertTrue(gate.process(mid), "迟滞区内（高于下阈值）应保持有声")
        assertFalse(gate.process(quiet), "低于下阈值应回到静音")
        assertFalse(gate.process(mid), "迟滞区内（低于上阈值）应保持静音")
        assertTrue(gate.process(loud), "再次强信号应重新打开")
    }

    @Test
    fun `reset restores silent state`() {
        val gate = RmsGate(thresholdDbfs = -50f, hysteresisDb = 6f)
        gate.process(sineAtRmsDbfs(-20.0))
        assertFalse(gate.silent)
        gate.reset()
        assertTrue(gate.silent)
    }
}
