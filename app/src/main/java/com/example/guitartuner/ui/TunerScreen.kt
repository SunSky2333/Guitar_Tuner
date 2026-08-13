package com.example.guitartuner.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guitartuner.music.Instrument
import com.example.guitartuner.music.Tuning
import com.example.guitartuner.tuner.TunerUiState
import com.example.guitartuner.tuner.TunerViewModel

/**
 * 调音器主界面：表盘 + 来源切换 + 乐器 → 调弦 → 弦位三级选择（文档 §8）。
 */
@Composable
fun TunerScreen(modifier: Modifier = Modifier) {
    val viewModel: TunerViewModel = viewModel { TunerViewModel() }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.start() else viewModel.onPermissionDenied()
    }

    TunerContent(
        state = state,
        onStart = {
            if (state.useMic) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.start()
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                viewModel.start()
            }
        },
        onStop = viewModel::stop,
        onUseMicChange = viewModel::setUseMic,
        onInstrumentSelect = viewModel::selectInstrument,
        onTuningSelect = viewModel::selectTuning,
        onStringSelect = viewModel::selectString,
        modifier = modifier,
    )
}

@Composable
private fun TunerContent(
    state: TunerUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onUseMicChange: (Boolean) -> Unit,
    onInstrumentSelect: (Instrument) -> Unit,
    onTuningSelect: (Int) -> Unit,
    onStringSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 来源切换 + 开始/停止
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("演示", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.useMic, onCheckedChange = onUseMicChange)
            Text("麦克风", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = if (state.isListening) onStop else onStart) {
                Text(if (state.isListening) "停止" else "开始")
            }
        }

        if (state.permissionDenied) {
            Text(
                text = "麦克风权限被拒绝，无法采集",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // 表盘
        TunerGauge(
            note = state.note,
            cents = state.cents,
            inTune = state.inTune,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        // 乐器
        SectionLabel("乐器")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Instrument.entries.forEach { inst ->
                FilterChip(
                    selected = state.instrument == inst,
                    onClick = { onInstrumentSelect(inst) },
                    label = { Text(if (inst == Instrument.GUITAR) "吉他" else "尤克里里") },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 调弦
        SectionLabel("调弦")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TunerViewModel.tuningsFor(state.instrument).forEachIndexed { i, tuning ->
                FilterChip(
                    selected = state.tuningIndex == i,
                    onClick = { onTuningSelect(i) },
                    label = { Text(shortTuningName(tuning)) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 弦位
        SectionLabel("弦位")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val tuning = TunerViewModel.tuningsFor(state.instrument)[state.tuningIndex]
            tuning.notes.forEachIndexed { i, note ->
                FilterChip(
                    selected = state.selectedString == i,
                    onClick = { onStringSelect(i) },
                    label = { Text(note.name) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

private fun shortTuningName(tuning: Tuning): String = when (tuning.name) {
    "吉他标准 EADGBE" -> "标准"
    "吉他 drop-D" -> "drop-D"
    "吉他 drop-C" -> "drop-C"
    "尤克里里标准 GCEA" -> "标准"
    "尤克里里降半音" -> "降半音"
    "尤克里里降全音" -> "降全音"
    else -> tuning.name
}
