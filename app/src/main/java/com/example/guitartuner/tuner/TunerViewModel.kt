package com.example.guitartuner.tuner

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import com.example.guitartuner.audio.AudioRecordSource
import com.example.guitartuner.audio.AudioSource
import com.example.guitartuner.audio.FakeAudioSource
import com.example.guitartuner.dsp.ExponentialSmoother
import com.example.guitartuner.dsp.PitchPipeline
import com.example.guitartuner.dsp.RmsGate
import com.example.guitartuner.music.Instrument
import com.example.guitartuner.music.NoteMapper
import com.example.guitartuner.music.Tuning
import com.example.guitartuner.music.Tunings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.pow

/** 调音器 UI 状态 */
data class TunerUiState(
    val instrument: Instrument = Instrument.GUITAR,
    val tuningIndex: Int = 0,
    val selectedString: Int = 0,
    val useMic: Boolean = false,
    val isListening: Boolean = false,
    val permissionDenied: Boolean = false,
    val note: String? = null,
    val cents: Float = 0f,
    val inTune: Boolean = false,
)

/**
 * 调音器状态机：把音频源 + [PitchPipeline] 的结果映射到音符/音分，暴露为 StateFlow。
 *
 * 采集线程由 [AudioSource] 自行管理（非主线程）；检测结果节流到 ~30 fps 抛回 UI。
 */
class TunerViewModel : ViewModel() {

    private val pipeline = PitchPipeline(
        sampleRate = SAMPLE_RATE,
        gate = RmsGate(),
        smoother = ExponentialSmoother(alpha = 0.35f),
    )
    private var source: AudioSource? = null
    private var lastUiUpdateMs = 0L

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    /** 开始监听（麦克风或演示源，取决于 useMic） */
    fun start() {
        stop()
        val useMic = _uiState.value.useMic
        val src: AudioSource = if (useMic) {
            AudioRecordSource(sampleRate = SAMPLE_RATE)
        } else {
            FakeAudioSource(sampleRate = SAMPLE_RATE, frequencyProvider = ::demoFrequency)
        }
        source = src
        pipeline.reset()
        lastUiUpdateMs = 0L
        _uiState.update { it.copy(isListening = true, permissionDenied = false) }
        try {
            src.start { pcm -> pipeline.push(pcm, ::onPitch) }
        } catch (e: Exception) {
            source = null
            _uiState.update { it.copy(isListening = false, permissionDenied = true) }
        }
    }

    fun stop() {
        source?.stop()
        source = null
        _uiState.update { it.copy(isListening = false, note = null, cents = 0f, inTune = false) }
    }

    /** 切换来源（麦克风/演示），切换时停止监听，需重新点开始（麦克风走权限流程） */
    fun setUseMic(useMic: Boolean) {
        if (useMic == _uiState.value.useMic) return
        stop()
        _uiState.update { it.copy(useMic = useMic, permissionDenied = false) }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(isListening = false, permissionDenied = true) }
    }

    fun selectInstrument(instrument: Instrument) {
        _uiState.update {
            it.copy(
                instrument = instrument, tuningIndex = 0, selectedString = 0,
                note = null, cents = 0f, inTune = false,
            )
        }
    }

    fun selectTuning(index: Int) {
        _uiState.update {
            it.copy(tuningIndex = index, selectedString = 0, note = null, cents = 0f, inTune = false)
        }
    }

    fun selectString(index: Int) {
        _uiState.update { it.copy(selectedString = index, note = null, cents = 0f, inTune = false) }
    }

    private fun onPitch(f0: Float?) {
        if (f0 == null) {
            lastUiUpdateMs = 0L
            _uiState.update { it.copy(note = null, cents = 0f, inTune = false) }
            return
        }
        // 节流到 ~30 fps（文档 §9.7），静音清空不节流
        val now = SystemClock.elapsedRealtime()
        if (now - lastUiUpdateMs < UI_INTERVAL_MS) return
        lastUiUpdateMs = now

        val target = currentTarget(_uiState.value)
        val cents = NoteMapper.cents(f0, target)
        val (name, octave) = NoteMapper.freqToNote(f0)
        _uiState.update {
            it.copy(note = "$name$octave", cents = cents, inTune = abs(cents) < IN_TUNE_CENTS)
        }
    }

    private fun currentTarget(state: TunerUiState): Float =
        tuningsFor(state.instrument)[state.tuningIndex].notes[state.selectedString].frequency

    /** 演示源：按当前弦位目标频率 + 少量偏高，让指针偏离绿区以展示红/绿反馈 */
    private fun demoFrequency(): Float {
        val target = currentTarget(_uiState.value)
        return (target * 2.0.pow(DEMO_OFFSET_CENTS / 1200.0)).toFloat()
    }

    override fun onCleared() {
        stop()
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val IN_TUNE_CENTS = 5f
        const val UI_INTERVAL_MS = 33L      // ~30 fps
        const val DEMO_OFFSET_CENTS = 8.0   // 演示模式固定 +8 音分

        fun tuningsFor(instrument: Instrument): List<Tuning> =
            Tunings.ALL.filter { it.instrument == instrument }
    }
}
