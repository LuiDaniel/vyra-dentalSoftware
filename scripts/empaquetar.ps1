<#
  Empaqueta Vyra con jpackage (docs/09-plan-implementacion.md, fase 8).

  Uso (desde la raíz del repositorio):   powershell -ExecutionPolicy Bypass -File scripts/empaquetar.ps1
  Con instalador (.exe):                 ... -File scripts/empaquetar.ps1 -Instalador

  Sin -Instalador genera una carpeta lista para copiar y ejecutar: target/instalador/Vyra/Vyra.exe.
  El instalador necesita WiX Toolset 3.x en el PATH (https://wixtoolset.org).
  La base de datos de la aplicación empaquetada queda en %APPDATA%\Vyra\vyra.db.
#>
param([switch]$Instalador)

$ErrorActionPreference = 'Stop'
$jar = 'vyra-dental-software-1.0-SNAPSHOT.jar'

mvn -q package -DskipTests
if ($LASTEXITCODE -ne 0) { throw 'mvn package falló' }

Remove-Item -Recurse -Force target/libs, target/instalador -ErrorAction SilentlyContinue
mvn -q dependency:copy-dependencies '-DoutputDirectory=target/libs' '-DincludeScope=runtime'
if ($LASTEXITCODE -ne 0) { throw 'no se pudieron copiar las dependencias' }
Copy-Item "target/$jar" target/libs/

$tipo = if ($Instalador) { 'exe' } else { 'app-image' }
jpackage --type $tipo --name Vyra --app-version 1.0 --vendor 'Vyra' `
    --description 'Gestión de consultorio dental' `
    --input target/libs --main-jar $jar --main-class com.vyra.dental.Launcher `
    --add-modules java.sql,java.desktop,java.prefs,java.naming,java.xml,java.logging,jdk.unsupported `
    --dest target/instalador `
    $(if ($Instalador) { '--win-menu', '--win-shortcut', '--win-dir-chooser' })
if ($LASTEXITCODE -ne 0) { throw 'jpackage falló' }

Write-Host "Listo: target/instalador"
