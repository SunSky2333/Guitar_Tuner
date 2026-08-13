package com.example.guitartuner.music

/**
 * 音符映射：频率 → 音名 / 音分偏差（文档 §5）。
 */
object NoteMapper {

    /** 十二平均律音名，索引 = MIDI % 12 */
    val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * 频率 → (音名, 八度)。
     * MIDI 编号 = 69 + 12 × log2(f / 440)，A4(midi 69) = 440 Hz。
     */
    fun freqToNote(f0: Float): Pair<String, Int> {
        val midi = Math.round(69 + 12 * Math.log(f0 / 440.0) / Math.log(2.0)).toInt()
        return NOTE_NAMES[Math.floorMod(midi, 12)] to (midi / 12 - 1)
    }

    /**
     * 频率相对目标频率的音分偏差，正 = 偏高（#），负 = 偏低（b）。
     * cents = 1200 × log2(f0 / target)
     */
    fun cents(f0: Float, target: Float): Float {
        val ratio = f0.toDouble() / target.toDouble()
        return (1200.0 * Math.log(ratio) / Math.log(2.0)).toFloat()
    }

    /** 通用频率公式：f = 440 × 2^((midi − 69) / 12) */
    fun midiToFreq(midi: Int): Float =
        (440.0 * Math.pow(2.0, (midi - 69) / 12.0)).toFloat()
}
