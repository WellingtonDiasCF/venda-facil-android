# Venda Fácil para Android

[Português](README.md) · [English](README.en.md) · **Español**

[![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://github.com/WellingtonDiasCF/venda-facil-android/releases/latest)
[![Versión 1.6.0](https://img.shields.io/badge/versión-1.6.0-155e54)](https://github.com/WellingtonDiasCF/venda-facil-android/releases/latest)
[![Licencia MIT](https://img.shields.io/badge/licencia-MIT-e59a36)](LICENSE)

Administra productos, ventas, inventario, clientes, ventas a crédito y gastos directamente desde el teléfono. No requiere una cuenta, un servidor ni una conexión permanente a Internet.

> La interfaz de la aplicación está disponible actualmente en portugués de Brasil. Esta página explica en español cómo instalarla y utilizarla.

## Descargar

[**Descargar Venda Fácil 1.6.0 — instalación nueva**](https://github.com/WellingtonDiasCF/venda-facil-android/releases/download/v1.6.0/VendaFacil-1.6.0.apk)

Si utilizabas una versión de prueba anterior, descarga el [APK de actualización](https://github.com/WellingtonDiasCF/venda-facil-android/releases/download/v1.6.0/VendaFacil-1.6.0-atualizacao.apk) para conservar la compatibilidad de la firma.

Requiere Android 7.0 o posterior.

## Instalación

1. Descarga el APK desde el enlace anterior.
2. Abre el archivo en el teléfono.
3. Si Android solicita permiso, permite la instalación de aplicaciones para el navegador o gestor de archivos utilizado.
4. Pulsa **Instalar** y después **Abrir**.

Descarga la aplicación únicamente desde la [página oficial de versiones](https://github.com/WellingtonDiasCF/venda-facil-android/releases). Actualmente no está disponible en Google Play.

## Funciones

- Productos con precio, costo, icono y control de inventario opcional.
- Ventas con descuentos, notas y distintos métodos de pago.
- Cuentas de clientes y ventas a crédito.
- Pagos totales o parciales con historial.
- Envío de saldos pendientes por WhatsApp.
- Gastos únicos y recurrentes.
- Informes de facturación, beneficio estimado, cobros y valores pendientes.
- Exportación de informes en CSV.
- Copia de seguridad y restauración completa.

## Primeros pasos

1. Abre **Produtos** y registra los artículos que vendes.
2. Usa **Vender** para preparar una venta y elegir el método de pago.
3. Registra al cliente antes de marcar una venta como fiado/crédito.
4. Abre **Clientes** para registrar los pagos recibidos.
5. Usa **Relatórios** para consultar un período y exportar archivos CSV.

## Privacidad

Los productos, clientes, ventas y gastos se guardan localmente en el dispositivo. La aplicación no envía esta información automáticamente a servidores externos.

El envío por WhatsApp y la exportación de archivos solo se realizan cuando eliges esas opciones.

## Copias de seguridad y actualizaciones

Crea una copia antes de cambiar de teléfono o instalar una actualización importante. En la pantalla inicial, usa **Enviar backup pelo WhatsApp** o guarda el archivo en un lugar seguro. Para recuperarlo, selecciona **Importar e restaurar backup**.

No desinstales la aplicación antes de actualizarla, ya que Android puede borrar los datos locales. Si usabas una versión de prueba, instala el APK de actualización indicado arriba.

## Preguntas frecuentes

**¿Funciona sin Internet?**

Sí. Internet solo es necesario para descargar el APK o utilizar servicios externos como WhatsApp.

**¿Necesito una cuenta o suscripción?**

No. La aplicación funciona localmente y no requiere registro.

**¿Puedo usar los mismos datos en varios teléfonos simultáneamente?**

Cada instalación conserva datos separados. Exporta y restaura una copia para trasladar la información a otro dispositivo.

**¿Dónde está el historial de versiones?**

Consulta el [registro de cambios](CHANGELOG.md).

<details>
<summary>Información para desarrolladores</summary>

### Requisitos

- Java 17
- Android SDK 35

### Compilación

```bash
./gradlew assembleDebug
```

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

</details>

## Licencia

Distribuido bajo la [licencia MIT](LICENSE).
