#!/usr/bin/env python3
"""Prepare Soundimage WAV masters for the Java 8 music player.

The source masters are stereo IEEE-float WAV files, while Java's Clip support is
most reliable with compact PCM16. This tool downmixes, resamples, peak-normalizes,
and gently closes the loop seam without requiring an external audio dependency.
"""

import argparse
import struct
import wave


def read_wav(path):
    with open(path, "rb") as source:
        if source.read(4) != b"RIFF":
            raise ValueError("not a RIFF file: " + path)
        source.read(4)
        if source.read(4) != b"WAVE":
            raise ValueError("not a WAVE file: " + path)
        format_data = None
        audio_data = None
        while True:
            header = source.read(8)
            if len(header) < 8:
                break
            chunk_id, size = struct.unpack("<4sI", header)
            payload = source.read(size)
            if size & 1:
                source.read(1)
            if chunk_id == b"fmt ":
                format_data = payload
            elif chunk_id == b"data":
                audio_data = payload
        if format_data is None or audio_data is None:
            raise ValueError("missing WAV format or audio data: " + path)

    tag, channels, rate, _, _, bits = struct.unpack("<HHIIHH", format_data[:16])
    if tag == 3 and bits == 32:
        values = struct.unpack("<%df" % (len(audio_data) // 4), audio_data)
    elif tag == 1 and bits == 16:
        raw = struct.unpack("<%dh" % (len(audio_data) // 2), audio_data)
        values = [value / 32768.0 for value in raw]
    elif tag == 1 and bits == 24:
        values = []
        for index in range(0, len(audio_data), 3):
            value = int.from_bytes(audio_data[index:index + 3], "little", signed=False)
            if value & 0x800000:
                value -= 0x1000000
            values.append(value / 8388608.0)
    else:
        raise ValueError("unsupported WAV encoding tag=%d bits=%d" % (tag, bits))
    mono = []
    for index in range(0, len(values), channels):
        mono.append(sum(values[index:index + channels]) / channels)
    return rate, mono


def resample(samples, source_rate, target_rate):
    if source_rate == target_rate:
        return list(samples)
    output_count = max(1, int(round(len(samples) * target_rate / source_rate)))
    scale = source_rate / float(target_rate)
    output = []
    for output_index in range(output_count):
        position = min(len(samples) - 1, output_index * scale)
        left = int(position)
        right = min(len(samples) - 1, left + 1)
        fraction = position - left
        output.append(samples[left] * (1.0 - fraction) + samples[right] * fraction)
    return output


def normalize(samples, peak_target=0.70):
    peak = max((abs(value) for value in samples), default=0.0)
    if peak <= 0.000001:
        return samples
    gain = peak_target / peak
    return [max(-1.0, min(1.0, value * gain)) for value in samples]


def close_loop_seam(samples, rate, seconds=0.35):
    count = min(len(samples) // 4, max(1, int(rate * seconds)))
    first = samples[:count]
    for index in range(count):
        blend = (index + 1) / float(count)
        target = first[index]
        position = len(samples) - count + index
        samples[position] = samples[position] * (1.0 - blend) + target * blend
    return samples


def write_pcm16(path, rate, samples):
    frames = bytearray()
    for value in samples:
        integer = int(round(max(-1.0, min(1.0, value)) * 32767.0))
        frames.extend(struct.pack("<h", integer))
    with wave.open(path, "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(rate)
        output.writeframes(bytes(frames))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("destination")
    parser.add_argument("--rate", type=int, default=22050)
    parser.add_argument("--peak", type=float, default=0.70)
    parser.add_argument("--no-seam", action="store_true")
    args = parser.parse_args()
    source_rate, samples = read_wav(args.source)
    samples = resample(samples, source_rate, args.rate)
    samples = normalize(samples, args.peak)
    if not args.no_seam:
        samples = close_loop_seam(samples, args.rate)
    write_pcm16(args.destination, args.rate, samples)


if __name__ == "__main__":
    main()
