package com.breath.trainer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.breath.trainer.R
import com.breath.trainer.audio.AmbientSound
import com.breath.trainer.audio.VoiceStyle
import com.breath.trainer.breathing.pattern.BreathingPattern
import com.breath.trainer.breathing.pattern.BreathingPatterns
import com.breath.trainer.ui.TrainerUiSettings

/**
 * 设置底部抽屉：呼吸节奏、环境音、循环轮数、播报方式、声音、背景音乐、触感反馈、保持屏幕常亮。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    visible: Boolean,
    settings: TrainerUiSettings,
    patterns: List<BreathingPattern>,
    ambients: List<AmbientSound>,
    voiceStyles: List<VoiceStyle>,
    onRoundsChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onMusicChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onPatternChange: (BreathingPattern) -> Unit,
    onAmbientChange: (AmbientSound) -> Unit,
    onVoiceStyleChange: (VoiceStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 呼吸节奏
            Text(
                text = stringResource(id = R.string.settings_pattern),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.settings_pattern_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            patterns.forEach { p ->
                val selected = p.id == settings.pattern.id
                val descRes = when (p.id) {
                    BreathingPatterns.FOUR_SEVEN_EIGHT.id -> R.string.pattern_478_desc
                    BreathingPatterns.FOUR_TWO_SIX.id -> R.string.pattern_426_desc
                    BreathingPatterns.BOX.id -> R.string.pattern_box_desc
                    else -> R.string.pattern_478_desc
                }
                val nameRes = when (p.id) {
                    BreathingPatterns.FOUR_SEVEN_EIGHT.id -> R.string.pattern_478_name
                    BreathingPatterns.FOUR_TWO_SIX.id -> R.string.pattern_426_name
                    BreathingPatterns.BOX.id -> R.string.pattern_box_name
                    else -> R.string.pattern_478_name
                }
                androidx.compose.material3.Surface(
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onPatternChange(p) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selected,
                            onClick = { onPatternChange(p) },
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(id = nameRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(id = descRes),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 环境音
            Text(
                text = stringResource(id = R.string.settings_ambient),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.settings_ambient_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            ambients.forEach { a ->
                val selected = a.id == settings.ambient.id
                androidx.compose.material3.Surface(
                    color = if (selected) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onAmbientChange(a) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selected,
                            onClick = { onAmbientChange(a) },
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(id = a.nameRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(id = a.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 循环轮数
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(id = R.string.settings_cycles), color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "${settings.totalRounds} 轮",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Slider(
                    value = settings.totalRounds.toFloat(),
                    onValueChange = { onRoundsChange(it.toInt()) },
                    valueRange = 1f..settings.pattern.totalRounds.toFloat().coerceAtLeast(1f),
                    steps = (settings.pattern.totalRounds - 1).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(id = R.string.settings_cycles_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 播报方式（单词 / 长句）
            Text(
                text = stringResource(id = R.string.settings_voice_style),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.settings_voice_style_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            VoiceStyleSegmented(
                styles = voiceStyles,
                selected = settings.voiceStyle,
                onSelect = onVoiceStyleChange,
            )

            Spacer(modifier = Modifier.height(20.dp))

            ToggleRow(
                title = stringResource(id = R.string.settings_sound),
                checked = settings.soundEnabled,
                onCheckedChange = onSoundChange,
                description = stringResource(id = R.string.settings_sound_desc),
            )
            ToggleRow(
                title = stringResource(id = R.string.settings_music),
                checked = settings.musicEnabled,
                onCheckedChange = onMusicChange,
                description = stringResource(id = R.string.settings_music_desc),
            )
            ToggleRow(
                title = stringResource(id = R.string.settings_haptics),
                checked = settings.hapticsEnabled,
                onCheckedChange = onHapticsChange,
                description = stringResource(id = R.string.settings_haptics_desc),
            )
            ToggleRow(
                title = stringResource(id = R.string.settings_keep_screen_on),
                checked = settings.keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
                description = stringResource(id = R.string.settings_keep_screen_on_desc),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.78f)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
}

/**
 * 紧凑的 SegmentedButton 风格二选一，用于播报方式切换。
 */
@Composable
private fun VoiceStyleSegmented(
    styles: List<VoiceStyle>,
    selected: VoiceStyle,
    onSelect: (VoiceStyle) -> Unit,
) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            styles.forEach { style ->
                val isSelected = style == selected
                val nameRes = when (style) {
                    VoiceStyle.SHORT -> R.string.voice_style_short_name
                    VoiceStyle.GENTLE_LONG -> R.string.voice_style_long_name
                }
                val descRes = when (style) {
                    VoiceStyle.SHORT -> R.string.voice_style_short_desc
                    VoiceStyle.GENTLE_LONG -> R.string.voice_style_long_desc
                }
                androidx.compose.material3.Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(style) },
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(id = nameRes),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = descRes),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
