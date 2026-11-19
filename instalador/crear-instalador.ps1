# Arma el instalador de Vyra: target\instalador\Vyra-Setup-<version>.exe
#
#   powershell -ExecutionPolicy Bypass -File instalador\crear-instalador.ps1
#
# Necesita JDK 21 y Maven (como el resto del proyecto) e Inno Setup 6:
#   winget install --id JRSoftware.InnoSetup -e --scope user
#
# 1. mvn -Pinstalador package  -> target\instalador\Vyra (Vyra.exe con Java incluido, via jpackage)
# 2. ISCC vyra.iss             -> target\instalador\Vyra-Setup-<version>.exe

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$version = '1.0.0'

$iscc = @(
    "$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe",
    "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
    "$env:ProgramFiles\Inno Setup 6\ISCC.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $iscc) {
    throw 'No se encontró Inno Setup 6. Instálalo con: winget install --id JRSoftware.InnoSetup -e --scope user'
}

Push-Location $raiz
try {
    Write-Host '1/2 Empaquetando la aplicación con jpackage (pruebas incluidas)...'
    mvn -q -Pinstalador package "-Djpackage.version=$version"
    if ($LASTEXITCODE -ne 0) { throw 'Falló mvn -Pinstalador package' }

    Write-Host '2/2 Creando el instalador con Inno Setup...'
    & $iscc /Qp "/DVersion=$version" (Join-Path $PSScriptRoot 'vyra.iss')
    if ($LASTEXITCODE -ne 0) { throw 'Falló Inno Setup' }

    $salida = Join-Path $raiz "target\instalador\Vyra-Setup-$version.exe"
    Write-Host "Listo: $salida ($([math]::Round((Get-Item $salida).Length / 1MB, 1)) MB)"
} finally {
    Pop-Location
}
