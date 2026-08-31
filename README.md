# Rekam Audio – Internal Audio Recorder

![Android](https://img.shields.io/badge/Android-10%2B-green.svg) ![License](https://img.shields.io/badge/License-MIT-blue.svg) ![APK](https://img.shields.io/badge/APK-arm64-blueviolet.svg)

**Rekam Audio** is a modern, privacy-focused Android app that captures **internal system audio** in high fidelity.
**Rekam Audio** 是一款现代化、注重隐私的安卓应用，可以高保真地录制**系统内部声音**。
*(Aplikasi perekam audio internal Android yang jernih, tanpa root, dan privasi aman.)*

Unlike traditional recorders that use the microphone, Rekam Audio taps directly into the system's audio stream — crystal-clear recordings without background noise. Perfect for **Zoom meetings, webinars, YouTube videos, or podcasts**.

**Language / 语言 / Bahasa:** [English](#-key-features) · [简体中文](#-主要功能) · [Bahasa Indonesia](#-fitur-utama-bahasa-indonesia)

---

## ✨ Key Features

*   **📱 Pure Internal Audio**
    *   Records system sound directly (**Zoom, YouTube, Podcasts, Webinars**).
    *   No microphone interference or background noise.
    *   **Note:** Requires Android 10+ (API 29).

*   **🎛 Multi-Format Recording**
    *   **WAV** – lossless, **M4A** – compressed AAC, or **MP3** – universal compatibility.
    *   Selectable MP3 bitrate: **128 / 192 / 320 kbps**.
    *   Live transcoding progress shown on the floating button and notification.
    *   If MP3 encoding ever fails, the recording is automatically saved as WAV so nothing is lost.

*   **🎨 Material You Design**
    *   Dynamic colors that adapt to your wallpaper (Light/Dark mode).
    *   Themed app icons.

*   **🔘 Floating Overlay Controls**
    *   A compact, draggable circular button — move it anywhere on screen.
    *   Pulse animation while recording; progress ring while transcoding.
    *   Start/stop recordings from *any* app.

*   **▶️ Playback with Seek Bar**
    *   Play recordings with a draggable progress bar and time display.
    *   Jump to any position instantly.

*   **📂 Built-in Manager**
    *   Play, **rename**, **share**, or **delete** recordings.
    *   File size and recording date shown for every item.

*   **🌐 Multilingual**
    *   English, 简体中文, 繁體中文, Bahasa Indonesia.

*   **🔒 Privacy First**
    *   **100% Offline:** No internet permission required.
    *   Only requests what is needed (Audio, Notification, Overlay).

---

## ⭐ 主要功能

*   **📱 纯内录（手机内部声音）**
    *   直接录制系统音频（Zoom、YouTube、播客、网课）。
    *   不经过麦克风，无环境杂音。
    *   **要求** Android 10+（API 29），无需 root。

*   **🎛 多格式录音**
    *   **WAV** 无损、**M4A** 压缩、**MP3** 通用格式三选一。
    *   MP3 码率三档可选：**128 / 192 / 320 kbps**。
    *   悬浮按钮和通知栏实时显示 MP3 转码进度。
    *   转码失败时自动降级保存 WAV，录音绝不丢失。

*   **🎨 Material You 设计**
    *   动态取色，跟随壁纸自动变换主题色（浅色/深色模式）。
    *   适配系统主题图标。

*   **🔘 悬浮录音按钮**
    *   小巧的圆形按钮，**按住拖动即可改变位置**。
    *   录制中有脉冲光晕动画，转码时显示进度环。
    *   在任何应用中都能随时开始/停止录音。

*   **▶️ 播放进度条**
    *   播放时显示可拖动的进度条与时间。
    *   拖动即可跳转到任意位置。

*   **📂 录音管理**
    *   播放、**重命名**、**分享**、**删除**录音文件。
    *   列表显示每个文件的录制日期和大小。

*   **🌐 多语言**
    *   英语、简体中文、繁體中文、印尼语。

*   **🔒 隐私优先**
    *   **100% 离线**：不申请任何联网权限。
    *   只申请必要权限（录音、通知、悬浮窗）。

---

## 🇮🇩 Fitur Utama (Bahasa Indonesia)
**Mengapa memilih Rekam Audio?**
*   **Perekam Suara Internal Murni:** Merekam audio sistem langsung (Zoom, Google Meet, YouTube, Spotify) tanpa suara bising dari luar. Tidak perlu root! (Android 10+).
*   **Multi-Format:** WAV (lossless), M4A, atau MP3 dengan pilihan bitrate 128/192/320 kbps, lengkap dengan progress bar pemutar dan tombol melayang yang bisa digeser.
*   **Desain Material You:** Tampilan modern yang mengikuti warna wallpaper HP Anda.
*   **Tombol Melayang (Overlay):** Mulai/stop rekaman dari aplikasi apa saja tanpa ribet.
*   **Manajemen File Mudah:** Putar, ganti nama, dan bagikan rekaman langsung ke WhatsApp atau media sosial.
*   **Privasi Terjamin:** 100% Offline. Tidak butuh internet. Data Anda aman.

> **Kata Kunci:** Perekam rapat online, rekam kuliah online, internal audio recorder, rekam musik spotify, perekam suara zoom jernih.

---

## 🛠️ Technical Stack
*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM + Clean Architecture
*   **Audio Engine:** MediaProjection API + AudioRecord + FFmpegKit (MP3 encoding)
*   **Min SDK:** Android 10 (API 29) · **Target SDK:** Android 15 (API 35)

## 📥 Installation / 安装 / Cara Install
1.  Download the latest APK from the [Releases](../../releases) page.（从 [Releases](../../releases) 页面下载最新 APK。）
2.  Install on your Android device (Android 10 or newer required).
3.  Grant the necessary permissions (Notification for controls, Overlay for the floating button).

## ⚠️ Known Issues / 已知限制 / Masalah yang Diketahui

*   **Google Chrome & Web Browsers:** Chrome on Android generally blocks internal audio capture (except via Microphone). This is a system-level restriction by Google.
    *   **Recommendation:** Use other browsers like **Samsung Internet** or **Firefox** for better compatibility.
    *   **安卓版 Chrome** 通常会禁止内录浏览器自身的声音（这是 Google 的系统级限制），建议使用 **三星浏览器** 或 **Firefox**。

*   **Some DRM/Streaming Apps** (e.g. Spotify, Netflix) may output silence when internal capture is active.
    *   **部分 DRM 流媒体应用**（如 Spotify、Netflix）在内录时可能只输出静音，属系统版权保护机制。

## 🤝 Contributing
Contributions are welcome! Please open an issue or submit a pull request if you have suggestions or bug fixes.

## 📄 License
[MIT License](LICENSE)
