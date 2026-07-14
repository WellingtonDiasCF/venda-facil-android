# Venda Fácil for Android

[Português](README.md) · **English** · [Español](README.es.md)

[![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://github.com/WellingtonDiasCF/venda-facil-android/releases/latest)
[![Version 1.6.0](https://img.shields.io/badge/version-1.6.0-155e54)](https://github.com/WellingtonDiasCF/venda-facil-android/releases/latest)
[![MIT License](https://img.shields.io/badge/license-MIT-e59a36)](LICENSE)

Manage products, sales, inventory, customers, credit sales, and expenses directly on your phone. No account, server, or permanent internet connection is required.

> The app interface is currently available in Brazilian Portuguese. This page explains how to install and use it in English.

## Download

[**Download Venda Fácil 1.6.0 — new installation**](https://github.com/WellingtonDiasCF/venda-facil-android/releases/download/v1.6.0/VendaFacil-1.6.0.apk)

If you used a previous test build, download the [update APK](https://github.com/WellingtonDiasCF/venda-facil-android/releases/download/v1.6.0/VendaFacil-1.6.0-atualizacao.apk) to keep the signing key compatible.

Requires Android 7.0 or newer.

## Installation

1. Download the APK from the link above.
2. Open the file on your phone.
3. If Android asks for permission, allow app installation for the browser or file manager used for the download.
4. Tap **Install**, then **Open**.

Only download the app from the [official Releases page](https://github.com/WellingtonDiasCF/venda-facil-android/releases). It is not currently available on Google Play.

## Features

- Products with price, cost, icon, and optional inventory tracking.
- Sales with discounts, notes, and multiple payment methods.
- Customer accounts and credit sales.
- Full or partial payments with payment history.
- WhatsApp messages containing outstanding customer balances.
- One-time and recurring expenses.
- Revenue, estimated profit, received, and outstanding reports.
- CSV report export.
- Full data backup and restore.

## Getting started

1. Open **Produtos** and add the items you sell.
2. Use **Vender** to build a sale and select the payment method.
3. Add a customer before marking a sale as credit/fiado.
4. Open **Clientes** to record customer payments.
5. Use **Relatórios** to review a period and export CSV files.

## Privacy

Products, customers, sales, and expenses are stored locally on the device. The app does not automatically send this information to external servers.

WhatsApp sharing and file exports only happen when you explicitly select those options.

## Backup and updates

Create a backup before changing phones or installing an important update. From the home screen, use **Enviar backup pelo WhatsApp** or save the file somewhere safe. Use **Importar e restaurar backup** to recover it.

Do not uninstall the app before updating, as Android may remove its local data. Previous test-build users should use the update APK linked in the download section.

## FAQ

**Does it work offline?**

Yes. Internet access is only needed to download the APK or use external services such as WhatsApp.

**Do I need an account or subscription?**

No. The app works locally and does not require registration.

**Can I use the same data on multiple phones at once?**

Each installation keeps separate data. Export and restore a backup to move information to another device.

**Where can I find the version history?**

See the [changelog](CHANGELOG.md).

<details>
<summary>Developer information</summary>

### Requirements

- Java 17
- Android SDK 35

### Build

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

</details>

## License

Distributed under the [MIT License](LICENSE).
