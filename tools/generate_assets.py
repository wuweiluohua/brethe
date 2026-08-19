"""生成舒缓的WAV背景音乐和轻柔提示音。

输出:
- raw/ambient_calm.wav      30 秒循环的舒缓环境音（默认开场）
- raw/ambient_rain.wav      雨声
- raw/ambient_birds.wav     鸟鸣
- raw/ambient_ocean.wav     海浪
- raw/ambient_stream.wav    溪水
- raw/ambient_campfire.wav  篝火
- raw/chime_phase.wav       阶段切换钟铃 (C5)
- raw/chime_breath_in.wav   吸气开始 (E5)
- raw/chime_breath_out.wav  呼气开始 (A4)
"""
import math
import os
import random
import struct
import wave

SAMPLE_RATE = 44100
OUT_DIR = r"H:\daydayup\workbuddy\2026-08-18-20-06-25\BreathTrainer\app\src\main\res\raw"
os.makedirs(OUT_DIR, exist_ok=True)


def write_wav(path: str, samples: list[float]) -> None:
    """16-bit PCM mono WAV."""
    pcm = bytearray()
    for s in samples:
        v = max(-1.0, min(1.0, s))
        pcm.extend(struct.pack("<h", int(v * 32767)))
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(bytes(pcm))
    print(f"Wrote {path} ({os.path.getsize(path) / 1024:.1f} KB)")


def envelope(i: int, n: int, attack: float = 0.05, release: float = 0.4) -> float:
    a = int(attack * n)
    r = int(release * n)
    if i < a:
        return i / a
    if i > n - r:
        return (n - i) / r
    return 1.0


def fade_loop(samples: list[float], fade_seconds: float = 0.5) -> None:
    """渐入渐出，避免 pop。"""
    fade = int(fade_seconds * SAMPLE_RATE)
    for i in range(fade):
        a = i / fade
        samples[i] *= a
        samples[-(i + 1)] *= a


def soft_limit(samples: list[float], peak_target: float = 0.92) -> None:
    peak = max(abs(x) for x in samples) or 1.0
    if peak > peak_target:
        gain = peak_target / peak
        for i in range(len(samples)):
            samples[i] *= gain


def crossfade_loop(samples: list[float], xfade_seconds: float = 1.0) -> None:
    """让一个 WAV 真正"无缝"循环：把首尾 xfade_seconds 交叉混合。"""
    n = len(samples)
    xfade = int(xfade_seconds * SAMPLE_RATE)
    if xfade * 2 >= n:
        return
    for i in range(xfade):
        a = i / xfade  # 0..1
        head = samples[i]
        tail = samples[n - xfade + i]
        mixed = head * a + tail * (1 - a)
        samples[i] = mixed
        samples[n - xfade + i] = mixed


# ------------------------------------------------------------------
# 1. 背景音乐：30秒循环的低频氛围 + 缓慢的pad和声（默认 calma）
# ------------------------------------------------------------------
def generate_ambient(seconds: float = 30.0, loop_seconds: float = 30.0) -> list[float]:
    n = int(seconds * SAMPLE_RATE)
    loop_n = int(loop_seconds * SAMPLE_RATE)
    samples = [0.0] * n
    chord_freqs = [
        [130.81, 164.81, 196.00, 261.63, 329.63],   # C maj
        [110.00, 130.81, 164.81, 220.00, 261.63],   # A min
        [87.31,  130.81, 174.61, 220.00, 261.63],   # F maj
        [98.00,  146.83, 196.00, 246.94, 293.66],   # G maj
    ]
    chord_steps = loop_n // len(chord_freqs)

    random.seed(42)
    noise_state = 0.0
    for i in range(n):
        ci = min(i // chord_steps, len(chord_freqs) - 1)
        chord = chord_freqs[ci]
        local_i = i % chord_steps
        crossfade = min(1.0, local_i / (SAMPLE_RATE * 0.5)) * min(
            1.0, (chord_steps - local_i) / (SAMPLE_RATE * 0.5)
        )
        s = 0.0
        for f in chord:
            detune = 1.0 + (random.random() - 0.5) * 0.0006
            s += math.sin(2 * math.pi * f * detune * i / SAMPLE_RATE)
        s = s / len(chord)
        lfo = 0.5 + 0.5 * math.sin(2 * math.pi * 0.07 * i / SAMPLE_RATE)
        s *= 0.18 * crossfade * (0.55 + 0.45 * lfo)
        if random.random() < 0.00015:
            s += 0.06 * math.sin(2 * math.pi * (880 + random.random() * 600) * i / SAMPLE_RATE)
        noise_state = 0.96 * noise_state + 0.04 * (random.random() - 0.5)
        s += 0.04 * noise_state
        samples[i] = s * 0.6

    soft_limit(samples)
    fade_loop(samples)
    return samples


# ------------------------------------------------------------------
# 2. 柔和钟铃提示音
# ------------------------------------------------------------------
def generate_gong(freq: float = 528.0, seconds: float = 1.4, decay: float = 4.0) -> list[float]:
    n = int(seconds * SAMPLE_RATE)
    samples = []
    for i in range(n):
        t = i / SAMPLE_RATE
        env = math.exp(-decay * t)
        s = (
            math.sin(2 * math.pi * freq * t) * 0.5
            + math.sin(2 * math.pi * freq * 2 * t) * 0.18
            + math.sin(2 * math.pi * freq * 3 * t) * 0.07
            + math.sin(2 * math.pi * freq * 4.01 * t) * 0.04
            + math.sin(2 * math.pi * freq * 5.97 * t) * 0.02
        )
        s *= env * 0.55
        if i < SAMPLE_RATE * 0.02:
            s *= i / (SAMPLE_RATE * 0.02)
        samples.append(s)
    return samples


# ------------------------------------------------------------------
# 白噪声工具
# ------------------------------------------------------------------
def white_noise(n: int) -> list[float]:
    return [random.uniform(-1.0, 1.0) for _ in range(n)]


def one_pole_lp(x: list[float], cutoff: float = 0.1) -> list[float]:
    """一阶低通；cutoff 越大通过越多高频。"""
    y = [0.0] * len(x)
    prev = 0.0
    for i, s in enumerate(x):
        prev = prev + cutoff * (s - prev)
        y[i] = prev
    return y


def one_pole_hp(x: list[float], cutoff: float = 0.05) -> list[float]:
    """一阶高通：prev_output = x - prev_in + cutoff*prev_in ；这里用近似。"""
    y = [0.0] * len(x)
    prev_in = 0.0
    prev_out = 0.0
    for i, s in enumerate(x):
        prev_out = (1 - cutoff) * (prev_out + s - prev_in)
        prev_in = s
        y[i] = prev_out
    return y


# ------------------------------------------------------------------
# 3. 雨声（30s）— 全频段粉噪 + 随机雨滴撞击
# ------------------------------------------------------------------
def generate_rain(seconds: float = 30.0) -> list[float]:
    random.seed(11)
    n = int(seconds * SAMPLE_RATE)
    base = white_noise(n)
    # 通过更柔和的低通得到粉噪感觉
    body = one_pole_lp(base, cutoff=0.45)
    # 高频细节——单独一路，再叠回来
    sparkle = one_pole_hp(base, cutoff=0.7)
    samples = [0.7 * body[i] + 0.18 * sparkle[i] for i in range(n)]

    # 稀疏雨滴 click：短促高通脉冲
    drops = max(40, int(seconds * 12))
    for _ in range(drops):
        start = random.randint(0, n - 800)
        amp = random.uniform(0.25, 0.6)
        for j in range(400):
            if start + j >= n:
                break
            env = math.exp(-(j / 40.0))
            samples[start + j] += amp * env * (random.uniform(-1, 1))

    soft_limit(samples, 0.9)
    fade_loop(samples)
    crossfade_loop(samples, 0.6)
    return samples


# ------------------------------------------------------------------
# 4. 鸟鸣（45s）— 平缓粉噪 + 不规则鸟叫（频率 1800-3200Hz 短 chirp）
# ------------------------------------------------------------------
def generate_birds(seconds: float = 45.0) -> list[float]:
    random.seed(23)
    n = int(seconds * SAMPLE_RATE)
    base = white_noise(n)
    samples = [0.0] * n
    # 低频"草地"环境音
    bed = one_pole_lp(base, cutoff=0.35)
    for i in range(n):
        samples[i] += 0.32 * bed[i]

    # 鸟鸣：对每个 chirp 是一组带轻颤的频率跳跃
    num_calls = int(seconds * 0.9)  # 平均每秒 0.9 次
    for _ in range(num_calls):
        start = random.randint(0, n - int(0.6 * SAMPLE_RATE))
        dur = random.randint(int(0.08 * SAMPLE_RATE), int(0.35 * SAMPLE_RATE))
        base_f = random.uniform(1800, 3200)
        # 频率包络在 chirp 中轻微上行/下行
        amp = random.uniform(0.35, 0.7)
        for j in range(dur):
            t = j / SAMPLE_RATE
            env = math.exp(-(t / (dur / SAMPLE_RATE)) * 2.0)
            wob = math.sin(2 * math.pi * (8 + random.random() * 5) * t)
            f = base_f * (1 + 0.08 * wob)
            sig = math.sin(2 * math.pi * f * t) * env * amp
            # 添加一个比基频低 5x 的鸟胸腔共振
            sig += 0.4 * math.sin(2 * math.pi * (f / 5) * t) * env * amp
            idx = start + j
            if idx < n:
                samples[idx] += sig

    soft_limit(samples, 0.85)
    fade_loop(samples)
    crossfade_loop(samples, 0.5)
    return samples


# ------------------------------------------------------------------
# 5. 海浪（32s）— 周期性浪涌，约每 8 秒一次
# ------------------------------------------------------------------
def generate_ocean(seconds: float = 32.0) -> list[float]:
    random.seed(37)
    n = int(seconds * SAMPLE_RATE)
    base = white_noise(n)
    # 重要：低/高通必须在循环外算一次，否则 O(n²)
    roar = one_pole_lp(base, cutoff=0.12)
    hiss = one_pole_hp(base, cutoff=0.6)
    samples = [0.0] * n
    wave_period = 8.0  # 8 秒一个浪
    for i in range(n):
        t = i / SAMPLE_RATE
        phase = (t % wave_period) / wave_period
        if phase < 0.30:
            env = (phase / 0.30) ** 1.5
        elif phase < 0.55:
            env = 1.0
        else:
            env = ((1.0 - phase) / 0.45) ** 1.8
        samples[i] = 0.55 * roar[i] * env + 0.18 * hiss[i] * (env ** 1.5)

    soft_limit(samples, 0.88)
    fade_loop(samples, fade_seconds=1.5)
    crossfade_loop(samples, 1.0)
    return samples


# ------------------------------------------------------------------
# 6. 溪水（25s）— 高频嘶嘶感 + 间歇水花 pluck
# ------------------------------------------------------------------
def generate_stream(seconds: float = 25.0) -> list[float]:
    random.seed(53)
    n = int(seconds * SAMPLE_RATE)
    base = white_noise(n)
    hiss = one_pole_hp(base, cutoff=0.85)  # 高频水流主体
    body = one_pole_lp(base, cutoff=0.5)
    samples = [0.45 * hiss[i] + 0.18 * body[i] for i in range(n)]

    # 偶尔"水滴溅起"短促 pluck：短正弦串 + 快速衰减
    plucks = int(seconds * 3.5)
    for _ in range(plucks):
        start = random.randint(0, n - 1500)
        f = random.uniform(800, 2200)
        dur = random.randint(120, 600)
        amp = random.uniform(0.18, 0.45)
        for j in range(dur):
            t = j / SAMPLE_RATE
            env = math.exp(-(j / 60.0))
            idx = start + j
            if idx < n:
                samples[idx] += amp * env * math.sin(2 * math.pi * f * t)

    soft_limit(samples, 0.85)
    fade_loop(samples)
    crossfade_loop(samples, 0.5)
    return samples


# ------------------------------------------------------------------
# 7. 篝火（35s）— 缓慢低频燃烧 + 随机噼啪短脉冲
# ------------------------------------------------------------------
def generate_campfire(seconds: float = 35.0) -> list[float]:
    random.seed(71)
    n = int(seconds * SAMPLE_RATE)
    base = white_noise(n)
    # 低沉燃烧床
    bed = one_pole_lp(base, cutoff=0.2)
    samples = [0.55 * bed[i] for i in range(n)]

    # 噼啪：短爆裂（白噪脉冲 + 快速指数衰减）
    crackles = int(seconds * 4.0)
    for _ in range(crackles):
        start = random.randint(0, n - 2000)
        dur = random.randint(60, 250)
        amp = random.uniform(0.25, 0.7)
        for j in range(dur):
            env = math.exp(-(j / 25.0))
            idx = start + j
            if idx < n:
                samples[idx] += amp * env * random.uniform(-1, 1) * 0.7

    soft_limit(samples, 0.88)
    fade_loop(samples)
    crossfade_loop(samples, 0.5)
    return samples


# ------------------------------------------------------------------
# 主入口
# ------------------------------------------------------------------
if __name__ == "__main__":
    print("=== 背景音乐 ===")
    ambient = generate_ambient(seconds=30.0, loop_seconds=30.0)
    write_wav(os.path.join(OUT_DIR, "ambient_calm.wav"), ambient)

    print("=== 白噪声环境音 ===")
    rain = generate_rain(seconds=30.0)
    write_wav(os.path.join(OUT_DIR, "ambient_rain.wav"), rain)

    birds = generate_birds(seconds=45.0)
    write_wav(os.path.join(OUT_DIR, "ambient_birds.wav"), birds)

    ocean = generate_ocean(seconds=32.0)
    write_wav(os.path.join(OUT_DIR, "ambient_ocean.wav"), ocean)

    stream = generate_stream(seconds=25.0)
    write_wav(os.path.join(OUT_DIR, "ambient_stream.wav"), stream)

    fire = generate_campfire(seconds=35.0)
    write_wav(os.path.join(OUT_DIR, "ambient_campfire.wav"), fire)

    print("=== 提示音 ===")
    gong = generate_gong(freq=523.25, seconds=1.2, decay=3.5)  # C5
    write_wav(os.path.join(OUT_DIR, "chime_phase.wav"), gong)
    inhale = generate_gong(freq=659.25, seconds=0.9, decay=5.0)  # E5
    write_wav(os.path.join(OUT_DIR, "chime_breath_in.wav"), inhale)
    exhale = generate_gong(freq=440.00, seconds=1.2, decay=3.2)  # A4
    write_wav(os.path.join(OUT_DIR, "chime_breath_out.wav"), exhale)

    print("All audio assets generated.")
