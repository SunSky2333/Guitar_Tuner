package com.example.guitartuner

import com.example.guitartuner.dsp.PitchDetector
import com.example.guitartuner.music.NoteMapper
import com.example.guitartuner.music.Tunings
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * M1 验收明细报告（对应文档 §12.1 的 Python 验证脚本的 Kotlin 移植）。
 * 运行：./gradlew :dsp:verifyReport
 *
 * 合成正弦波（44.1 kHz，2048 点帧，与实时 App 相同帧长）输入 YIN 检测器，
 * 输出 6 套调弦 26 个音的逐项误差。
 */
fun main() {
    val sampleRate = 44100
    val frameSize = 2048

    fun sineFrame(freq: Double): ShortArray {
        val frame = ShortArray(frameSize)
        for (i in 0 until frameSize) {
            val v = 0.5 * Short.MAX_VALUE * sin(2.0 * PI * freq * i / sampleRate)
            frame[i] = v.toInt().toShort()
        }
        return frame
    }

    println("M1 验收明细（合成正弦波 @ %d Hz，帧长 %d 点 ≈ %.1f ms）".format(sampleRate, frameSize, frameSize * 1000.0 / sampleRate))
    println("=".repeat(76))
    println("%-18s %-6s %12s %12s %10s".format("调弦方案", "音名", "标准(Hz)", "检测(Hz)", "误差(cents)"))
    println("-".repeat(76))

    var maxErr = 0.0
    var worst: Triple<String, String, Double>? = null
    var total = 0

    for (tuning in Tunings.ALL) {
        for (note in tuning.notes) {
            val f0 = note.frequency.toDouble()
            val detected = PitchDetector.detect(sineFrame(f0), sampleRate)
            val err = if (detected != null) {
                abs(NoteMapper.cents(detected, note.frequency).toDouble())
            } else Double.NaN
            total++
            if (!err.isNaN()) {
                if (err > maxErr) {
                    maxErr = err
                    worst = Triple(tuning.name, note.name, err)
                }
            }
            println(
                "%-18s %-6s %12.2f %12.3f %+10.2f".format(
                    tuning.name, note.name, f0,
                    detected ?: Double.NaN, if (err.isNaN()) Double.NaN else err
                )
            )
        }
    }

    println("-".repeat(76))
    println("合计 %d 个音；最大误差 %.2f 音分（%s / %s）".format(total, maxErr, worst?.first, worst?.second))
    println("验收标准：误差 ≤ 1 音分 → ${if (maxErr <= 1.0) "PASS" else "FAIL"}")
}
