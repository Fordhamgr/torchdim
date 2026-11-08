# TorchDim

iOS-style flashlight brightness slider for Android. Quick Settings tile with a long-press brightness popup, using the Android 13+ torch strength API. No root.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-13%2B-green.svg)](https://developer.android.com/about/versions/13)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg)](https://kotlinlang.org)
[![Release](https://img.shields.io/badge/Release-v0.1.0-orange.svg)](https://github.com/Fordhamgr/torchdim/releases)

---

## Features

- 🔆 **Quick Settings Tile (Tap to Cycle):**
  - **First tap (from OFF):** Instantly turns on the torch at **Max brightness (Level 6)**.
  - **Subsequent taps:** Rapidly and smoothly cycles brightness downwards:
    $$\text{Level 6 (Max)} \to 5 \to 4 \to 3 \to 2 \to 1 \text{ (Min)} \to \text{Off} \to \text{Level 6 (Max)}$$
  - Dynamic tile subtitle updates live (`Level 6 (Max)`, `Level 5`, ..., `Off`).
- 🎨 **Adaptive Beam Ray Icons:**
  - Dynamic vector drawables switch instantly without IPC bitmap overhead.
  - Level 1 through 6 beam rays fan out proportionally to strength.
  - Shows an unlit outline flashlight when off.
- 📱 **iOS-Style Floating Slider (Tile Long-Press):**
  - Long-press the Quick Settings tile from anywhere to open a dimmed translucent overlay with a vertical pill slider.
  - Direct touch manipulation with haptic feedback ticks on each level change.
  - Bottom 15% zone turns the torch off.
  - Tap outside the pill to dismiss.
- 🎛️ **Modern Companion App:**
  - Clean OLED dark landing screen with full-height interactive capsule slider.
  - Live status indicator ("Level 4 of 6 · Torch is on").
  - Quick Settings guide card and power toggle button.
  - Clickable repository source link.
- ⚡ **No Root Required:**
  - Uses the official Android 13 (`API 33`) Camera2 HAL API (`CameraManager.turnOnTorchWithStrengthLevel`).

---

## Tested Devices

| Device | Chipset | Max Strength Level | Android Version | Status |
|---|---|---|---|---|
| **Motorola Edge 50 Neo** | MediaTek Dimensity 7300 | 6 (Default: 3) | Android 14 / 15 / 16 | Verified & Working |

*(Does your device support it? If `CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL` > 1 on your Camera HAL, TorchDim will support variable brightness on your device!)*

---

## Installation

1. Go to the [Releases](https://github.com/Fordhamgr/torchdim/releases) page.
2. Download `torchdim-v0.1.0-debug.apk`.
3. Install it on your phone:
   ```bash
   adb install -r torchdim-v0.1.0-debug.apk
   ```
4. **Add the Quick Settings tile:**
   - Swipe down your notification shade twice to open full Quick Settings.
   - Tap the pencil (Edit) icon.
   - Find **TorchDim** in available tiles and drag it into your active panel.

---

## Development & Build

```bash
git clone https://github.com/Fordhamgr/torchdim.git
cd torchdim
./gradlew assembleDebug
```

---

## License

MIT License. See [LICENSE](LICENSE) for details.
