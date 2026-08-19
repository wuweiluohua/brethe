package com.breath.trainer.audio

import androidx.annotation.RawRes
import com.breath.trainer.R

/**
 * 一段循环播放的环境音。
 *
 * - [id] 持久化到 DataStore（默认 `calm`）。
 * - [nameRes] / [descriptionRes] 用于设置 UI 展示。
 * - [rawResId] 指向 res/raw 下的 WAV 文件。
 */
data class AmbientSound(
    val id: String,
    @RawRes val rawResId: Int,
    val nameRes: Int,
    val descriptionRes: Int,
)

/**
 * 内置的环境音选择列表。
 *
 * 每个 id 既是 DataStore 的 key，也作为默认回退；启动时找不到的资源会被忽略。
 */
object AmbientSounds {

    val CALM = AmbientSound(
        id = "calm",
        rawResId = R.raw.ambient_calm,
        nameRes = R.string.ambient_calm_name,
        descriptionRes = R.string.ambient_calm_desc,
    )

    val RAIN = AmbientSound(
        id = "rain",
        rawResId = R.raw.ambient_rain,
        nameRes = R.string.ambient_rain_name,
        descriptionRes = R.string.ambient_rain_desc,
    )

    val BIRDS = AmbientSound(
        id = "birds",
        rawResId = R.raw.ambient_birds,
        nameRes = R.string.ambient_birds_name,
        descriptionRes = R.string.ambient_birds_desc,
    )

    val OCEAN = AmbientSound(
        id = "ocean",
        rawResId = R.raw.ambient_ocean,
        nameRes = R.string.ambient_ocean_name,
        descriptionRes = R.string.ambient_ocean_desc,
    )

    val STREAM = AmbientSound(
        id = "stream",
        rawResId = R.raw.ambient_stream,
        nameRes = R.string.ambient_stream_name,
        descriptionRes = R.string.ambient_stream_desc,
    )

    val CAMPFIRE = AmbientSound(
        id = "campfire",
        rawResId = R.raw.ambient_campfire,
        nameRes = R.string.ambient_campfire_name,
        descriptionRes = R.string.ambient_campfire_desc,
    )

    val ALL: List<AmbientSound> = listOf(CALM, RAIN, BIRDS, OCEAN, STREAM, CAMPFIRE)

    fun findById(id: String?): AmbientSound =
        ALL.firstOrNull { it.id == id } ?: CALM
}
