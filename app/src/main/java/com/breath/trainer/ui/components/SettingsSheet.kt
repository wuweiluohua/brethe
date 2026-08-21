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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.breath.trainer.audio.VoiceGender
import com.breath.trainer.breathing.pattern.BreathingPattern
import com.breath.trainer.breathing.pattern.BreathingPatterns
import com.breath.trainer.ui.TrainerUiSettings

/**
 * 设置底部抽屉：呼吸节奏、环境音、循环轮数、播报方式、女声提示、音阶提示、背景音乐、保持屏幕常亮。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    visible: Boolean,
    settings: TrainerUiSettings,
    patterns: List<BreathingPattern>,
    ambients: List<AmbientSound>,
    voiceGenders: List<VoiceGender>,
    onRoundsChange: (Int) -> Unit,
    onVoicePromptChange: (Boolean) -> Unit,
    onChimePromptChange: (Boolean) -> Unit,
    onMusicChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onPatternChange: (BreathingPattern) -> Unit,
    onAmbientChange: (AmbientSound) -> Unit,
    onVoiceGenderChange: (VoiceGender) -> Unit,
    themeMode: Int = 0,
    onThemeModeChange: (Int) -> Unit = {},
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
                .verticalScroll(rememberScrollState())
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
                    // 滑块上限由"当前节奏的 totalRounds"放宽为统一的 MAX_ROUNDS，
                    // 不再被 4/6/5 这些节奏内嵌值锁死。
                    valueRange = 1f..MAX_ROUNDS.toFloat(),
                    steps = (MAX_ROUNDS - 1).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(id = R.string.settings_cycles_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 声音性别（女声 / 男声）
            Text(
                text = stringResource(id = R.string.settings_voice_gender),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.settings_voice_gender_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 改用卡片列表风格，与"环境音"分组保持一致：每条带 RadioButton + 名称 + 描述。
            VoiceGenderCardList(
                genders = voiceGenders,
                selected = settings.voiceGender,
                onSelect = onVoiceGenderChange,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 外观（Material 3 自定义主题的深色模式）
            Text(
                text = stringResource(id = R.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.settings_appearance_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            ThemeModeSegmented(
                selected = themeMode,
                onSelect = onThemeModeChange,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 训练提示：女声提示 / 音阶提示 两个独立开关
            Text(
                text = stringResource(id = R.string.settings_prompts),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ToggleRow(
                title = stringResource(id = R.string.settings_voice_prompt),
                checked = settings.voicePromptEnabled,
                onCheckedChange = onVoicePromptChange,
                description = stringResource(id = R.string.settings_voice_prompt_desc),
            )
            ToggleRow(
                title = stringResource(id = R.string.settings_chime_prompt),
                checked = settings.chimePromptEnabled,
                onCheckedChange = onChimePromptChange,
                description = stringResource(id = R.string.settings_chime_prompt_desc),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ToggleRow(
                title = stringResource(id = R.string.settings_music),
                checked = settings.musicEnabled,
                onCheckedChange = onMusicChange,
                description = stringResource(id = R.string.settings_music_desc),
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
 * 循环轮数的最大取值。
 *
 * 不再跟随"当前节奏"的 totalRounds（4 / 6 / 5），让用户每天最多能练 12 轮。
 */
private const val MAX_ROUNDS: Int = 12

/**
 * 卡片列表风格的声音性别选择器。
 *
 * 与"环境音"分组使用完全一致的视觉：每条卡片带 RadioButton + 名称 + 描述，选中态用 primaryContainer。
 */
@Composable
private fun VoiceGenderCardList(
    genders: List<VoiceGender>,
    selected: VoiceGender,
    onSelect: (VoiceGender) -> Unit,
) {
    genders.forEach { gender ->
        val isSelected = gender == selected
        val nameRes = when (gender) {
            VoiceGender.FEMALE -> R.string.voice_gender_female_name
            VoiceGender.MALE -> R.string.voice_gender_male_name
        }
        val descRes = when (gender) {
            VoiceGender.FEMALE -> R.string.voice_gender_female_desc
            VoiceGender.MALE -> R.string.voice_gender_male_desc
        }
        androidx.compose.material3.Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onSelect(gender) },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.RadioButton(
                    selected = isSelected,
                    onClick = { onSelect(gender) },
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
}

/**
 * 三态外观切换：跟随系统 / 浅色 / 深色。
 * 选中态使用主色填充，与播报方式分段控件风格一致。
 */
@Composable
private fun ThemeModeSegmented(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val items = listOf(
        0 to R.string.theme_mode_system,
        1 to R.string.theme_mode_light,
        2 to R.string.theme_mode_dark,
    )
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { (mode, nameRes) ->
                val isSelected = mode == selected
                androidx.compose.material3.Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clickable { onSelect(mode) },
                ) {
                    Text(
                        text = stringResource(id = nameRes),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}
