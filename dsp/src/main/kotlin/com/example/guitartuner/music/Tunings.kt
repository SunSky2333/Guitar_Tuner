package com.example.guitartuner.music

/**
 * 调弦定义（文档 §6）：吉他标准 + 降调（drop-D / drop-C）、尤克里里标准 + 降调（降半音 / 降全音）。
 * 共 6 套调弦、26 个音。频率统一由 [NoteMapper.midiToFreq] 计算。
 */
data class TuningNote(
    /** 音名（含八度，如 "E2"） */
    val name: String,
    /** MIDI 编号，A4 = 69 */
    val midi: Int,
) {
    /** 频率 Hz = 440 × 2^((midi − 69) / 12) */
    val frequency: Float get() = NoteMapper.midiToFreq(midi)
}

data class Tuning(
    /** 调弦方案名，如 "吉他标准 EADGBE" */
    val name: String,
    /** 乐器：吉他 / 尤克里里 */
    val instrument: Instrument,
    /** 弦位定义（吉他 6 弦，尤克里里 4 弦） */
    val notes: List<TuningNote>,
)

enum class Instrument { GUITAR, UKULELE }

object Tunings {

    /** 吉他标准调弦 EADGBE（6 弦 → 1 弦） */
    val GUITAR_STANDARD = Tuning(
        name = "吉他标准 EADGBE",
        instrument = Instrument.GUITAR,
        notes = listOf(
            TuningNote("E2", 40),
            TuningNote("A2", 45),
            TuningNote("D3", 50),
            TuningNote("G3", 55),
            TuningNote("B3", 59),
            TuningNote("E4", 64),
        ),
    )

    /** 吉他 drop-D：仅 6 弦降全音 D2 */
    val GUITAR_DROP_D = Tuning(
        name = "吉他 drop-D",
        instrument = Instrument.GUITAR,
        notes = listOf(
            TuningNote("D2", 38),
            TuningNote("A2", 45),
            TuningNote("D3", 50),
            TuningNote("G3", 55),
            TuningNote("B3", 59),
            TuningNote("E4", 64),
        ),
    )

    /** 吉他 drop-C：6 弦降全音 + 半音 C2 */
    val GUITAR_DROP_C = Tuning(
        name = "吉他 drop-C",
        instrument = Instrument.GUITAR,
        notes = listOf(
            TuningNote("C2", 36),
            TuningNote("A2", 45),
            TuningNote("D3", 50),
            TuningNote("G3", 55),
            TuningNote("B3", 59),
            TuningNote("E4", 64),
        ),
    )

    /** 尤克里里标准调弦 GCEA（4 弦 re-entrant 高音 G） */
    val UKULELE_STANDARD = Tuning(
        name = "尤克里里标准 GCEA",
        instrument = Instrument.UKULELE,
        notes = listOf(
            TuningNote("G4", 67),
            TuningNote("C4", 60),
            TuningNote("E4", 64),
            TuningNote("A4", 69),
        ),
    )

    /** 尤克里里降半音（整体移调 −1 半音） */
    val UKULELE_HALF_DOWN = Tuning(
        name = "尤克里里降半音",
        instrument = Instrument.UKULELE,
        notes = listOf(
            TuningNote("F#4", 66),
            TuningNote("B3", 59),
            TuningNote("D#4", 63),
            TuningNote("G#4", 68),
        ),
    )

    /** 尤克里里降全音（整体移调 −2 半音） */
    val UKULELE_WHOLE_DOWN = Tuning(
        name = "尤克里里降全音",
        instrument = Instrument.UKULELE,
        notes = listOf(
            TuningNote("F4", 65),
            TuningNote("A#3", 58),
            TuningNote("D4", 62),
            TuningNote("G4", 67),
        ),
    )

    /** 全部 6 套调弦 */
    val ALL: List<Tuning> = listOf(
        GUITAR_STANDARD,
        GUITAR_DROP_D,
        GUITAR_DROP_C,
        UKULELE_STANDARD,
        UKULELE_HALF_DOWN,
        UKULELE_WHOLE_DOWN,
    )
}
