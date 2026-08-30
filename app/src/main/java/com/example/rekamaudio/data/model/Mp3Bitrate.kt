package com.example.rekamaudio.data.model

enum class Mp3Bitrate(val bitsPerSecond: Int, val label: String) {
    BITRATE_128(128_000, "128 kbps"),
    BITRATE_192(192_000, "192 kbps"),
    BITRATE_320(320_000, "320 kbps")
}
