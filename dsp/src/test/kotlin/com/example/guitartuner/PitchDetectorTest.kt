package com.example.guitartuner

import com.example.guitartuner.dsp.PitchDetector
import com.example.guitartuner.music.NoteMapper
import com.example.guitartuner.music.Tunings
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * M1 核心 DSP 验收测试（文档 §11 / §12.1）：
 * 合成正弦波（44.1 kHz，2048 点帧）输入 YIN 检测器，
 * 覆盖 6 套调弦 26 个音，断言误差 ≤ 1 音分。
 */
class PitchDetectorTest {

    private val sampleRate = 44100
    private val frameSize = 2048

    /** 生成一帧 16-bit 正弦波 PCM */
    private fun sineFrame(freq: Double, amplitude: Double = 0.5): ShortArray {
        val frame = ShortArray(frameSize)
        for (i in 0 until frameSize) {
            val v = amplitude * Short.MAX_VALUE * sin(2.0 * PI * freq * i / sampleRate)
            frame[i] = v.toInt().toShort()
        }
        return frame
    }

    @Test
    fun `detects all 30 notes across 6 tunings within 1 cent`() {
        for (tuning in Tunings.ALL) {
            for (note in tuning.notes) {
                val f0 = note.frequency.toDouble()
                val detected = PitchDetector.detect(sineFrame(f0), sampleRate)
                assertNotNull(detected, "[${tuning.name}] ${note.name}: 未检出")
                val errCents = NoteMapper.cents(detected!!, note.frequency)
                assertTrue(
                    abs(errCents) <= 1.0,
                    "[${tuning.name}] ${note.name}: 标准 ${note.frequency} Hz, 检测 %.3f Hz, 误差 %.3f 音分"
                        .format(detected, errCents)
                )
            }
        }
    }

    @Test
    fun `detects frequency offset of 3 cents sharp`() {
        val fSharp = 440.0 * 2.0.pow(3.0 / 1200.0) // ≈ 440.764 Hz（A4 偏高 3 音分）
        val detected = PitchDetector.detect(sineFrame(fSharp), sampleRate)
        assertNotNull(detected)
        val errCents = NoteMapper.cents(detected!!, 440.0f)
        assertTrue(abs(errCents - 3.0) < 1.0, "期望 ~3 音分, 实测 %.3f".format(errCents))
    }

    @Test
    fun `silence frame returns null`() {
        val silence = ShortArray(frameSize) // 全 0
        assertEquals(null, PitchDetector.detect(silence, sampleRate))
    }

    @Test
    fun `note mapping for A4`() {
        val (name, octave) = NoteMapper.freqToNote(440.0f)
        assertEquals("A", name)
        assertEquals(4, octave)
    }

    @Test
    fun `note mapping for low E2`() {
        val (name, octave) = NoteMapper.freqToNote(82.41f)
        assertEquals("E", name)
        assertEquals(2, octave)
    }
}
