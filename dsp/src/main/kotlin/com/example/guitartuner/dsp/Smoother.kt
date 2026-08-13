package com.example.guitartuner.dsp

/**
 * 结果平滑（文档 §8 / §9.4）：对连续帧的检测结果做平滑，抑制指针抖动。
 *
 * 注意：当检测中断（静音或未检出音高）时应调用 [reset]，避免旧值残留影响
 * 下一次拨弦的首帧输出。
 */
interface Smoother {
    fun smooth(value: Float): Float
    fun reset()
}

/**
 * 中值滤波：对最近 [windowSize] 个值取中位数，抗尖峰。
 *
 * @param windowSize 窗口长度（≥1），建议 3~7
 */
class MedianSmoother(private val windowSize: Int = 5) : Smoother {
    init {
        require(windowSize >= 1) { "windowSize 必须 >= 1" }
    }

    private val buffer = ArrayDeque<Float>()

    override fun smooth(value: Float): Float {
        buffer.addLast(value)
        if (buffer.size > windowSize) buffer.removeFirst()
        return median()
    }

    private fun median(): Float {
        val sorted = buffer.sorted()
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2]
        else (sorted[n / 2 - 1] + sorted[n / 2]) / 2f
    }

    override fun reset() = buffer.clear()
}

/**
 * 指数平滑：y[n] = α·x[n] + (1−α)·y[n−1]。
 *
 * @param alpha 平滑系数（0..1），越小越平滑、滞后越大，建议 0.2~0.5
 */
class ExponentialSmoother(private val alpha: Float = 0.3f) : Smoother {
    init {
        require(alpha in 0f..1f) { "alpha 必须在 [0, 1]" }
    }

    private var hasPrev = false
    private var prev = 0f

    override fun smooth(value: Float): Float {
        prev = if (!hasPrev) {
            hasPrev = true
            value
        } else {
            alpha * value + (1f - alpha) * prev
        }
        return prev
    }

    override fun reset() {
        hasPrev = false
        prev = 0f
    }
}
