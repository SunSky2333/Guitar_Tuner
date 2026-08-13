package com.example.guitartuner.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.PI
import kotlin.math.sin

/**
 * 音频源抽象：解耦「采集」与「处理」，便于无真机时用假源联调 UI 与测试。
 *
 * 约定：[start] 的 onSamples 回调运行在非主线程，且传入的数组为全新实例
 * （回调内部可安全持有/异步使用，无需拷贝）。
 */
interface AudioSource {
    fun start(onSamples: (ShortArray) -> Unit)
    fun stop()
}

/**
 * 真机麦克风采集（AudioRecord，44.1 kHz / 单声道 / 16-bit PCM）。
 * 采集循环运行在独立守护线程，[stop] 时停止并释放。
 */
class AudioRecordSource(
    private val sampleRate: Int = 44100,
    private val chunkSize: Int = 2048,
) : AudioSource {

    private var recorder: AudioRecord? = null
    @Volatile private var running = false
    private var thread: Thread? = null

    override fun start(onSamples: (ShortArray) -> Unit) {
        if (running) return
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufSize = maxOf(minBuf * 2, chunkSize)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize,
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            error("AudioRecord 初始化失败（麦克风被占用或参数不支持）")
        }
        recorder = rec
        running = true
        rec.startRecording()
        thread = Thread {
            val buf = ShortArray(bufSize)
            while (running) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    onSamples(if (n == buf.size) buf.copyOf() else buf.copyOf(n))
                }
            }
        }.apply {
            name = "AudioRecordSource"
            isDaemon = true
            start()
        }
    }

    override fun stop() {
        running = false
        thread?.join(500)
        thread = null
        recorder?.let {
            runCatching { it.stop() }
            it.release()
        }
        recorder = null
    }
}

/**
 * 假音频源：按 [frequencyProvider] 连续生成正弦波，用于无真机/模拟器时联调 UI 与链路。
 * 仅用于开发演示，不参与正式采集路径。
 *
 * @param frequencyProvider 每块采样开始时读取的目标频率（Hz），可动态跟随弦位选择
 */
class FakeAudioSource(
    private val sampleRate: Int = 44100,
    private val chunkSize: Int = 2048,
    private val amplitude: Float = 0.5f,
    private val frequencyProvider: () -> Float = { 440f },
) : AudioSource {

    @Volatile private var running = false
    private var thread: Thread? = null

    override fun start(onSamples: (ShortArray) -> Unit) {
        if (running) return
        running = true
        thread = Thread {
            var phase = 0.0
            val buf = ShortArray(chunkSize)
            while (running) {
                val f = frequencyProvider()
                for (i in 0 until chunkSize) {
                    val v = amplitude * Short.MAX_VALUE * sin(2.0 * PI * f * phase / sampleRate)
                    buf[i] = v.toInt().toShort()
                    phase += 1.0
                }
                onSamples(buf.copyOf())
            }
        }.apply {
            name = "FakeAudioSource"
            isDaemon = true
            start()
        }
    }

    override fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }
}
