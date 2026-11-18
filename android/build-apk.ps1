param(
    [string]$Sdk = (Join-Path $env:TEMP 'mi-tabla-android-sdk')
)

$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$root = Split-Path $project -Parent
$output = Join-Path $project 'build'
$platform = Get-ChildItem (Join-Path $Sdk 'platform') -Filter android.jar -Recurse -File | Select-Object -First 1 -ExpandProperty FullName
$aapt2 = Get-ChildItem (Join-Path $Sdk 'build-tools') -Filter aapt2.exe -Recurse -File | Select-Object -First 1 -ExpandProperty FullName
$d8 = Get-ChildItem (Join-Path $Sdk 'build-tools') -Filter d8.bat -Recurse -File | Select-Object -First 1 -ExpandProperty FullName
$zipalign = Get-ChildItem (Join-Path $Sdk 'build-tools') -Filter zipalign.exe -Recurse -File | Select-Object -First 1 -ExpandProperty FullName
$apksigner = Get-ChildItem (Join-Path $Sdk 'build-tools') -Filter apksigner.bat -Recurse -File | Select-Object -First 1 -ExpandProperty FullName
$jdk = Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk*' | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
$jar = Join-Path $jdk 'bin\jar.exe'
$keytool = Join-Path $jdk 'bin\keytool.exe'

if (!$platform -or !$aapt2 -or !$d8 -or !$zipalign -or !$apksigner -or !(Test-Path $jar)) {
    throw "Falta el Android SDK en $Sdk (platform android.jar y build-tools para Windows)."
}

function Check-Exit([string]$stage) {
    if ($LASTEXITCODE -ne 0) { throw "Falló $stage (código $LASTEXITCODE)." }
}

New-Item -ItemType Directory -Force (Join-Path $output 'compiled'), (Join-Path $output 'classes'), (Join-Path $output 'dex') | Out-Null
Get-ChildItem (Join-Path $output 'compiled') -File | Remove-Item -Force
& $aapt2 compile --dir (Join-Path $project 'res') -o (Join-Path $output 'compiled')
Check-Exit 'compilar recursos'

$flats = @(Get-ChildItem (Join-Path $output 'compiled') -Filter '*.flat' | ForEach-Object FullName)
$resourceArgs = @()
foreach ($flat in $flats) { $resourceArgs += '-R', $flat }
& $aapt2 link -o (Join-Path $output 'unsigned.apk') --manifest (Join-Path $project 'AndroidManifest.xml') -I $platform --auto-add-overlay --min-sdk-version 23 --target-sdk-version 35 --version-code 1 --version-name 1.0 @resourceArgs
Check-Exit 'enlazar APK'

& javac -encoding UTF-8 -source 8 -target 8 -classpath $platform -d (Join-Path $output 'classes') (Join-Path $project 'src/com/mitabla/app/MainActivity.java')
Check-Exit 'compilar Java'

$classesJar = Join-Path $output 'classes.jar'
if (Test-Path $classesJar) { Remove-Item $classesJar }
& $jar cf $classesJar -C (Join-Path $output 'classes') .
Check-Exit 'empaquetar clases'
& $d8 --min-api 23 --lib $platform --output (Join-Path $output 'dex') $classesJar
Check-Exit 'convertir clases Android'

Copy-Item (Join-Path $output 'unsigned.apk') (Join-Path $output 'with-dex.apk') -Force
Push-Location (Join-Path $output 'dex')
try {
    & $jar uf (Join-Path $output 'with-dex.apk') classes.dex
    Check-Exit 'añadir clases al APK'
} finally {
    Pop-Location
}

$apk = Join-Path $root 'MiTabla.apk'
& $zipalign -f 4 (Join-Path $output 'with-dex.apk') $apk
Check-Exit 'alinear APK'

$keystore = Join-Path $env:USERPROFILE '.android/debug.keystore'
if (!(Test-Path $keystore)) {
    New-Item -ItemType Directory -Force (Split-Path $keystore -Parent) | Out-Null
    & $keytool -genkeypair -v -keystore $keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Mi Tabla, OU=Personal, O=Mi Tabla, L=Local, ST=Local, C=ES'
    Check-Exit 'crear clave de firma'
}
& $apksigner sign --ks $keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android $apk
Check-Exit 'firmar APK'
& $apksigner verify --verbose $apk
Check-Exit 'verificar APK'
Write-Host "APK listo: $apk"
