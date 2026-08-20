"""生成舒缓的WAV背景音乐和轻柔提示音（第二版，更拟真的自然声）。

输出:
- raw/ambient_calm.wav      30 秒循环的舒缓氛围 pad（默认开场）
- raw/ambient_rain.wav      雨声（宽带雨幕底噪 + 密集带通水滴 pitter-patter）
- raw/ambient_birds.wav     鸟鸣（清晨草地底床 + 带颤音/滑音的鸟叫与啁啾）
- raw/ambient_ocean.wav     海浪（双周期浪涌 + 浪尖泡沫嘶声）
- raw/ambient_stream.wav    溪水（中高频潺潺水声 + 随机气泡 blop 下滑音）
- raw/ambient_campfire.wav  篝火（低频燃烧床 + 高频噼啪爆裂）
- raw/chime_phase.wav       阶段切换钟铃 (C5)
- raw/chime_breath_in.wav   吸气开始 (E5)
- raw/chime_breath_out.wav  呼气开始 (A4)

所有环境音都做 1.5s 首尾交叉淡化（crossfade_loop），形成真正无缝循环。
"""
import math
import os
import random
import struct
import wave

SAMPLE_RATE = 44100
OUT_DIR = r"H:\daydayup\workbuddy\2026-08-18-20-06-25\BreathTrainer\app\src\main\res\raw"
os.makedirs(OUT_DIR, exist_ok=True)


def write_wav(path: str, samples: list) -> None:
    """16-bit PCM mono WAV。"""
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


def soft_limit(samples: list, peak_target: float = 0.92) -> None:
    peak = max(abs(x) for x in samples) or 1.0
    if peak > peak_target:
        gain = peak_target / peak
        for i in range(len(samples)):
            samples[i] *= gain


def crossfade_loop(samples: list, xfade_seconds: float = 1.5) -> None:
    """让 WAV 真正"无缝"循环：把首尾 xfade_seconds 交叉混合。"""
    n = len(samples)
    xfade = int(xfade_seconds * SAMPLE_RATE)
    if xfade * 2 >= n:
        return
    for i in range(xfade):
        a = i / xfade
        head = samples[i]
        tail = samples[n - xfade + i]
        mixed = head * a + tail * (1 - a)
        samples[i] = mixed
        samples[n - xfade + i] = mixed


# ------------------------------------------------------------------
# 噪声原语
# ------------------------------------------------------------------
def white_noise(n: int, seed=None) -> list:
    if seed is not None:
        random.seed(seed)
    return [random.uniform(-1.0, 1.0) for _ in range(n)]


def pink_noise(n: int, seed=None) -> list:
    """Paul Kellet 粉红噪声近似。"""
    if seed is not None:
        random.seed(seed)
    b0 = b1 = b2 = b3 = b4 = b5 = b6 = 0.0
    out = [0.0] * n
    for i in range(n):
        w = random.uniform(-1.0, 1.0)
        b0 = 0.99886 * b0 + w * 0.0555179
        b1 = 0.99332 * b1 + w * 0.0750759
        b2 = 0.96900 * b2 + w * 0.1538520
        b3 = 0.86650 * b3 + w * 0.3104856
        b4 = 0.55000 * b4 + w * 0.5329522
        b5 = -0.7616 * b5 - w * 0.0168980
        out[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362) * 0.11
        b6 = w * 0.115926
    return out


def brown_noise(n: int, seed=None) -> list:
    if seed is not None:
        random.seed(seed)
    out = [0.0] * n
    last = 0.0
    for i in range(n):
        w = random.uniform(-1.0, 1.0)
        last = (last + 0.02 * w) / 1.02
        out[i] = last * 3.5
    return out


def one_pole_lp(x: list, cutoff: float = 0.1) -> list:
    y = [0.0] * len(x)
    prev = 0.0
    for i, s in enumerate(x):
        prev = prev + cutoff * (s - prev)
        y[i] = prev
    return y


def one_pole_hp(x: list, cutoff: float = 0.05) -> list:
    y = [0.0] * len(x)
    prev_in = 0.0
    prev_out = 0.0
    for i, s in enumerate(x):
        prev_out = (1 - cutoff) * (prev_out + s - prev_in)
        prev_in = s
        y[i] = prev_out
    return y


def bandpass(x: list, hi: float, lo: float) -> list:
    """先高通（保留高于 hi 的部分），再低通（保留低于 lo 的部分）。"""
    return one_pole_lp(one_pole_hp(x, hi), lo)


def stamp(samples: list, template: list, pos: int) -> None:
    n = len(samples)
    m = len(template)
    for j in range(m):
        idx = pos + j
        if 0 <= idx < n:
            samples[idx] += template[j]


# ------------------------------------------------------------------
# 1. 默认氛围 pad（30s 循环）
# ------------------------------------------------------------------
def generate_ambient(seconds: float = 30.0) -> list:
    n = int(seconds * SAMPLE_RATE)
    samples = [0.0] * n
    chord_freqs = [
        [130.81, 164.81, 196.00, 261.63, 329.63],
        [110.00, 130.81, 164.81, 220.00, 261.63],
        [87.31, 130.81, 174.61, 220.00, 261.63],
        [98.00, 146.83, 196.00, 246.94, 293.66],
    ]
    chord_steps = n // len(chord_freqs)
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
        noise_state = 0.96 * noise_state + 0.04 * (random.random() - 0.5)
        s += 0.04 * noise_state
        samples[i] = s * 0.6
    soft_limit(samples)
    crossfade_loop(samples, 1.5)
    return samples


# ------------------------------------------------------------------
# 2. 柔和钟铃提示音
# ------------------------------------------------------------------
def generate_gong(freq: float = 528.0, seconds: float = 1.4, decay: float = 4.0) -> list:
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
# 3. 雨声（30s）— 宽带雨幕底噪 + 密集带通水滴 pitter-patter
# ------------------------------------------------------------------
def generate_rain(seconds: float = 30.0) -> list:
    random.seed(11)
    n = int(seconds * SAMPLE_RATE)
    w = white_noise(n)
    body = one_pole_lp(w, cutoff=0.25)        # 低频雨幕
    hiss = one_pole_hp(w, cutoff=0.6)         # 高频雨丝
    samples = [0.32 * body[i] + 0.5 * hiss[i] for i in range(n)]

    # 密集小水滴：带通的衰减正弦，模拟 pitter-patter（约 70 个/秒）
    L = int(0.02 * SAMPLE_RATE)
    for _ in range(int(seconds * 70)):
        pos = random.randint(0, n - L)
        f = random.uniform(1800, 5000)
        g = random.uniform(0.05, 0.18)
        for j in range(L):
            env = math.exp(-j / (0.004 * SAMPLE_RATE))
            samples[pos + j] += g * env * math.sin(2 * math.pi * f * j / SAMPLE_RATE)

    # 稀疏大雨滴：更低频、更长衰减
    Lh = int(0.05 * SAMPLE_RATE)
    for _ in range(int(seconds * 5)):
        pos = random.randint(0, n - Lh)
        f = random.uniform(400, 900)
        g = random.uniform(0.12, 0.30)
        for j in range(Lh):
            env = math.exp(-j / (0.02 * SAMPLE_RATE))
            samples[pos + j] += g * env * math.sin(2 * math.pi * f * j / SAMPLE_RATE)

    soft_limit(samples, 0.9)
    crossfade_loop(samples, 1.2)
    return samples


# ------------------------------------------------------------------
# 4. 鸟鸣（45s）— 清晨草地底床 + 带颤音/滑音的鸟叫与啁啾
# ------------------------------------------------------------------
def bird_note(f0: float, f1: float, dur: float, harm: int = 3,
              vib: float = 6.0, vibd: float = 0.04, attack: float = 0.006) -> list:
    L = int(dur * SAMPLE_RATE)
    out = [0.0] * L
    for j in range(L):
        t = j / SAMPLE_RATE
        f = f0 + (f1 - f0) * (j / L)
        f += vibd * f * math.sin(2 * math.pi * vib * t)   # 颤音
        s = math.sin(2 * math.pi * f * t)
        for h in range(2, harm + 1):                      # 少量谐波，更明亮像鸟
            s += (0.5 / h) * math.sin(2 * math.pi * f * h * t)
        if t < attack:
            env = t / attack
        else:
            env = math.exp(-(t - attack) * 3.0)
        out[j] = s * env
    return out


def generate_birds(seconds: float = 45.0) -> list:
    random.seed(23)
    n = int(seconds * SAMPLE_RATE)
    w = white_noise(n)
    bed = one_pole_lp(w, cutoff=0.18)     # 低频草地/微风
    hiss = one_pole_hp(w, cutoff=0.7)     # 叶片高频
    samples = [0.16 * bed[i] for i in range(n)]

    # 偶发叶片沙沙
    for _ in range(int(seconds * 1.5)):
        pos = random.randint(0, n - int(0.4 * SAMPLE_RATE))
        L = int(0.4 * SAMPLE_RATE)
        g = random.uniform(0.02, 0.06)
        for j in range(L):
            samples[pos + j] += g * math.exp(-abs(j - L / 2) / (0.12 * SAMPLE_RATE)) * hiss[pos + j]

    # 鸟叫乐句
    t = 1.0
    while t < seconds - 1.0:
        kind = random.random()
        if kind < 0.30:                       # 上行滑音
            note = bird_note(random.uniform(2200, 2800), random.uniform(3000, 3600), 0.18, harm=3)
            stamp(samples, note, int(t * SAMPLE_RATE))
            t += 0.18 + random.uniform(0.05, 0.15)
        elif kind < 0.55:                     # 下行滑音
            note = bird_note(random.uniform(3000, 3600), random.uniform(2200, 2700), 0.22, harm=3)
            stamp(samples, note, int(t * SAMPLE_RATE))
            t += 0.22 + random.uniform(0.05, 0.15)
        elif kind < 0.78:                     # 两音
            a = bird_note(random.uniform(2600, 3000), random.uniform(2600, 3000), 0.10, harm=3)
            b = bird_note(random.uniform(3000, 3500), random.uniform(3200, 3700), 0.12, harm=3)
            stamp(samples, a, int(t * SAMPLE_RATE)); t += 0.14
            stamp(samples, b, int(t * SAMPLE_RATE)); t += 0.16
        else:                                 # 啁啾（快速重复）
            base = random.uniform(2600, 3800)
            for _ in range(random.randint(4, 9)):
                note = bird_note(base * (1 + random.uniform(-0.05, 0.05)),
                                 base * (1 + random.uniform(-0.05, 0.05)), 0.05, harm=3)
                stamp(samples, note, int(t * SAMPLE_RATE))
                t += 0.07
        t += random.uniform(1.2, 3.5)        # 乐句间隔
    soft_limit(samples, 0.85)
    crossfade_loop(samples, 0.6)
    return samples


# ------------------------------------------------------------------
# 5. 海浪（32s）— 双周期浪涌 + 浪尖泡沫嘶声
# ------------------------------------------------------------------
def generate_ocean(seconds: float = 32.0) -> list:
    random.seed(37)
    n = int(seconds * SAMPLE_RATE)
    w = white_noise(n)
    roar = brown_noise(n, seed=38)            # 低频浪体
    foam = one_pole_hp(w, cutoff=0.5)         # 浪尖泡沫
    samples = [0.0] * n
    for i in range(n):
        t = i / SAMPLE_RATE
        s1 = 0.5 + 0.5 * math.sin(2 * math.pi * (1 / 9.0) * t)
        s2 = 0.5 + 0.5 * math.sin(2 * math.pi * (1 / 13.0) * t + 1.7)
        env = (s1 * 0.6 + s2 * 0.4) ** 2.2
        samples[i] = 0.5 * roar[i] * env + 0.35 * foam[i] * (env * env)
    soft_limit(samples, 0.88)
    crossfade_loop(samples, 1.5)
    return samples


# ------------------------------------------------------------------
# 6. 溪水（25s）— 中高频潺潺水声 + 随机气泡 blop 下滑音
# ------------------------------------------------------------------
def generate_stream(seconds: float = 25.0) -> list:
    random.seed(53)
    n = int(seconds * SAMPLE_RATE)
    w = white_noise(n)
    gurgle = bandpass(w, hi=0.35, lo=0.85)   # 中高频水流主体
    flow = brown_noise(n, seed=54)           # 低频水流
    samples = [0.0] * n
    wob = 0.5
    for i in range(n):
        if i % 200 == 0:
            wob = max(0.3, min(1.0, wob + random.uniform(-0.1, 0.1)))
        samples[i] = 0.45 * gurgle[i] * wob + 0.12 * flow[i]
    # 气泡：快速下滑的正弦 blop
    for _ in range(int(seconds * 6)):
        pos = random.randint(0, n - int(0.08 * SAMPLE_RATE))
        L = int(0.08 * SAMPLE_RATE)
        f0 = random.uniform(500, 900)
        f1 = random.uniform(200, 400)
        g = random.uniform(0.12, 0.30)
        for j in range(L):
            t = j / SAMPLE_RATE
            f = f0 + (f1 - f0) * (j / L)
            env = math.exp(-j / (0.02 * SAMPLE_RATE))
            samples[pos + j] += g * env * math.sin(2 * math.pi * f * t)
    soft_limit(samples, 0.85)
    crossfade_loop(samples, 0.6)
    return samples


# ------------------------------------------------------------------
# 7. 篝火（35s）— 低频燃烧床 + 高频噼啪爆裂
# ------------------------------------------------------------------
def generate_campfire(seconds: float = 35.0) -> list:
    random.seed(71)
    n = int(seconds * SAMPLE_RATE)
    w = white_noise(n)
    bed = brown_noise(n, seed=72)            # 温暖低频燃烧床
    hiss = one_pole_hp(w, cutoff=0.85)       # 火焰高频嘶声（也用于噼啪带通）
    samples = [0.22 * bed[i] + 0.04 * hiss[i] for i in range(n)]
    # 密集噼啪：锐利的带通衰减噪声爆裂
    Lc = int(0.05 * SAMPLE_RATE)
    for _ in range(int(seconds * 10)):
        pos = random.randint(0, n - Lc)
        g = random.uniform(0.15, 0.5)
        for j in range(Lc):
            env = math.exp(-j / (0.008 * SAMPLE_RATE))
            idx = pos + j
            if idx < n:
                samples[idx] += g * env * hiss[idx]
    # 偶发大爆裂
    Lp = int(0.12 * SAMPLE_RATE)
    for _ in range(int(seconds * 0.4)):
        pos = random.randint(0, n - Lp)
        g = random.uniform(0.4, 0.85)
        for j in range(Lp):
            env = math.exp(-j / (0.02 * SAMPLE_RATE))
            idx = pos + j
            if idx < n:
                samples[idx] += g * env * hiss[idx]
    soft_limit(samples, 0.9)
    crossfade_loop(samples, 0.6)
    return samples


# ------------------------------------------------------------------
# 主入口
# ------------------------------------------------------------------
if __name__ == "__main__":
    print("=== 背景音乐 ===")
    write_wav(os.path.join(OUT_DIR, "ambient_calm.wav"), generate_ambient(seconds=30.0))

    print("=== 自然白噪声环境音 ===")
    write_wav(os.path.join(OUT_DIR, "ambient_rain.wav"), generate_rain(seconds=30.0))
    write_wav(os.path.join(OUT_DIR, "ambient_birds.wav"), generate_birds(seconds=45.0))
    write_wav(os.path.join(OUT_DIR, "ambient_ocean.wav"), generate_ocean(seconds=32.0))
    write_wav(os.path.join(OUT_DIR, "ambient_stream.wav"), generate_stream(seconds=25.0))
    write_wav(os.path.join(OUT_DIR, "ambient_campfire.wav"), generate_campfire(seconds=35.0))

    print("=== 提示音 ===")
    write_wav(os.path.join(OUT_DIR, "chime_phase.wav"), generate_gong(freq=523.25, seconds=1.2, decay=3.5))
    write_wav(os.path.join(OUT_DIR, "chime_breath_in.wav"), generate_gong(freq=659.25, seconds=0.9, decay=5.0))
    write_wav(os.path.join(OUT_DIR, "chime_breath_out.wav"), generate_gong(freq=440.00, seconds=1.2, decay=3.2))

    print("All audio assets generated.")
