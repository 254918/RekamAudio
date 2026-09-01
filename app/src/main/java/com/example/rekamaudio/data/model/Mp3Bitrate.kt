package com.example.rekamaudio.data.model

enum class Mp3Bitrate(val bitsPerSecond: Int, val label: String, val streamingSupported: Boolean) {
    BITRATE_128(128_000, "128 kbps", streamingSupported = true),
    BITRATE_192(192_000, "192 kbps", streamingSupported = true),
    BITRATE_320(320_000, "320 kbps", streamingSupported = false)
}
