# HoloDori Installer

An open-source Android installer for the **Hololive Dreams** game. 
This installer allows you to bypass Google Play device restrictions and install the game easily on "unsupported" devices using elevated privileges.

## ✨ Features
- **Auto Download**: Fetches the latest game package directly from APKPure.
- **Smart Installation Method**: Automatically detects and uses the best installation method available:
  - **Shizuku** (Recommended, no root required)
  - **Root** (For rooted devices)
- **Local Install**: Supports installing your own downloaded `.apk` or `.xapk` files directly from your storage.
- **Auto-Update Detection**: Checks if you already have the game installed and helps you download the latest version.

## 📥 How to Download
If you are new to GitHub and aren't sure how to download the app, just follow these simple steps:
1. Go to the **[Releases](https://github.com/LoggingNewMemory/HoloDori-Installer/releases)** page on the right side of this repository.
2. Under the **Assets** section of the latest release, tap on the apk file to download it.
3. Once downloaded, open the APK file to install the HoloDori Installer on your device.

## ⚙️ How It Works
1. Connects to APKPure and automatically fetches the latest `.xapk` file.
2. Extracts and splits the APKs (if necessary).
3. Installs the app using package manager commands (`pm install -i com.android.vending -r`) via Shizuku or Root to simulate an official Play Store installation.

## ⚠️ Important Note for Root Users
If your device is rooted and **not Play Protect Certified** (Check in: *Play Store > Settings > About*), **DO NOT USE "LOGIN WITH GOOGLE"** in the game! 
Instead, use **Login with Code**. 
> *Using Google Login on an uncertified rooted device may cause your Google Mobile Services (GMS) to get flagged and stop working entirely, forcing you to wipe device data. You have been warned!*

## 📱 Tested Devices
Hololive Dreams is officially restricted on many devices. This list tracks "unsupported" devices where the game has been tested and proven playable using this installer.

| Device Codename | Device Model | Notes |
| :--- | :--- | :--- |
| `TECNO LH8n` | TECNO POVA 5 Pro 5G | Runs at ~40 FPS on Basic Settings. Playable. |
| `Nabu` | Xiaomi Pad 5 | Runs well at ~35 FPS on Normal Settings. |
| `Infinix X6882` | Infinix HOT 50 4G | Runs well at ~36 FPS on Normal Settings. |


### Contribute to the List!
If you successfully run the game on an unlisted device, feel free to contribute via pull request! You will need:
* **Device Codename**: (e.g., run `getprop ro.product.vendor.device` or search Google)
* **Device Model**: (e.g., run `getprop ro.product.vendor.model` or search Google)
* **Notes**: A brief note about performance and graphics settings.

## 🔒 Is it Safe?
If you trust the closed-source game (Hololive Dreams), you can certainly trust this installer! This project is **100% open-source**, meaning you can review the code yourself to see exactly how it downloads and installs the game without any hidden behaviors.

## ❤️ Support the Developer
If this tool helped you play the game, consider supporting the development!
- [SociaBuzz (Global)](https://sociabuzz.com/kanagawa_yamada/tribe)
- [QRIS (Local/ID)](https://t.me/KLAGen2/86)
- [PayPal](https://www.paypal.me/KanagawaYamada)
