package com.example.guitartuner.dsp

/**
 * 滑动窗分帧（文档 §9.3 实时性权衡的增强）。
 *
 * 文档 §7.2 的基线做法是「非重叠 2048 点帧」（每帧一次检测，更新周期 ≈ 帧长 ≈ 46 ms）。
 * 本类把采样流切成**重叠滑动窗**：维护最近 [frameSize] 个样本，每累计 [hopSize] 个
 * 新样本就发射一帧完整窗口。hop 越小更新越顺、有效延迟越低（hop 512 ≈ 11.6 ms @ 44.1 kHz）。
 *
 * 窗口未填满前不发射（不做零填充，避免污染 YIN）。
 */
class SlidingWindow(
    private val frameSize: Int = 2048,
    private val hopSize: Int = 512,
) {
    init {
        require(frameSize >= 1) { "frameSize 必须 >= 1" }
        require(hopSize in 1..frameSize) { "hopSize 必须在 [1, frameSize]" }
    }

    private val ring = ShortArray(frameSize)
    private var head = 0       // 环形缓冲下一写入位置（也是当前最旧样本）
    private var count = 0      // 已写入样本数（0..frameSize）
    private var untilEmit = 0  // 距下次发射还需的新样本数

    fun reset() {
        head = 0
        count = 0
        untilEmit = 0
    }

    /**
     * 喂入一段 PCM 样本；每凑满一个窗口就回调一次 [onFrame]（传入从旧到新排列的
     * frameSize 个最新样本）。返回触发发射的次数。
     */
    fun push(samples: ShortArray, onFrame: (ShortArray) -> Unit): Int {
        var emitted = 0
        for (s in samples) {
            ring[head] = s
            head = (head + 1) % frameSize
            if (count < frameSize) {
                count++
                if (count == frameSize) {
                    // 首次填满，立即发射
                    untilEmit = hopSize
                    onFrame(snapshot())
                    emitted++
                }
            } else {
                untilEmit--
                if (untilEmit <= 0) {
                    untilEmit = hopSize
                    onFrame(snapshot())
                    emitted++
                }
            }
        }
        return emitted
    }

    /** 复制最新 frameSize 个样本（从旧到新） */
    private fun snapshot(): ShortArray {
        val out = ShortArray(frameSize)
        var src = head
        for (i in 0 until frameSize) {
            out[i] = ring[src]
            src = (src + 1) % frameSize
        }
        return out
    }
}
