# Mi Tabla (Android)

La aplicación Android nativa está en `android/`. El APK instalable se genera como `MiTabla.apk` en esta carpeta. Permite elegir filas y columnas, escribir en las celdas, conservar datos en el teléfono y copiar la tabla al portapapeles con formato HTML y texto para hojas de cálculo. No necesita conexión ni permisos.

## Instalar en Android

Pasa `MiTabla.apk` al teléfono y ábrelo. Si Android pregunta, permite instalar aplicaciones desde la app con la que lo abriste (por ejemplo, Archivos). Compatible con Android 6 o posterior.

## Volver a compilar el APK

En Windows, con Android SDK Platform 35 y Build Tools 35.0.1 disponibles, ejecuta:

```powershell
.\android\build-apk.ps1 -Sdk "RUTA_AL_SDK"
```

El script firma el APK con una clave local de depuración (`%USERPROFILE%\.android\debug.keystore`). Conserva esa clave para poder actualizar la app instalada en el futuro.

## Versión web anterior

La versión web original permanece en esta carpeta. Para abrirla, ejecuta:

```sh
npm start
```

Y visita **http://localhost:4173**.
