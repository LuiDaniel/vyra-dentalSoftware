; Instalador de Vyra para Windows (Inno Setup 6).
; Empaqueta la carpeta que arma jpackage (target\instalador\Vyra: Java incluido + la app)
; en un solo Vyra-Setup-<versión>.exe. No usar a mano: lo llama crear-instalador.ps1.
;
; Se instala para el usuario actual (sin pedir administrador) en
; %LOCALAPPDATA%\Programs\Vyra. Los datos (vyra.db y vyra.properties) quedan aparte, en
; %LOCALAPPDATA%\Vyra, y desinstalar NO los borra.

#ifndef Version
  #define Version "1.0.0"
#endif

; Los accesos directos abren Vyra con runtime\bin\javaw.exe (firmado por Oracle) y no con
; Vyra.exe: el lanzador de jpackage no tiene firma digital y Smart App Control de Windows 11
; lo bloquea ("Una directiva de Control de aplicaciones bloqueó este archivo").
; jpackage.app-path hace que los datos vayan a %LOCALAPPDATA%\Vyra (ver ConexionBD).
#define Java "{app}\runtime\bin\javaw.exe"
; Las comillas van cuádruples: el preprocesador las deja dobles y Parameters las pide así
#define Argumentos "-Dfile.encoding=UTF-8 -Djpackage.app-version=" + Version + " """"-Djpackage.app-path={app}\Vyra.exe"""" -cp """"{app}\app\*"""" com.vyra.dental.Lanzador"

[Setup]
AppId={{6E2B7B7A-3C1D-4D0F-9B9A-56A1C0D7E1A5}
AppName=Vyra
AppVersion={#Version}
AppVerName=Vyra {#Version}
AppPublisher=Equipo Vyra (UPeU)
AppComments=Gestión de un consultorio dental: pacientes, agenda, historia clínica, odontograma, tratamientos y pagos.
DefaultDirName={autopf}\Vyra
DefaultGroupName=Vyra
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
OutputDir=..\target\instalador
OutputBaseFilename=Vyra-Setup-{#Version}
SetupIconFile=vyra.ico
UninstallDisplayIcon={app}\vyra.ico
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "es"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "escritorio"; Description: "Crear un acceso directo en el escritorio"; GroupDescription: "Accesos directos:"

[Files]
Source: "..\target\instalador\Vyra\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "vyra.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Vyra"; Filename: "{#Java}"; Parameters: "{#Argumentos}"; WorkingDir: "{app}"; IconFilename: "{app}\vyra.ico"
Name: "{group}\Desinstalar Vyra"; Filename: "{uninstallexe}"
Name: "{autodesktop}\Vyra"; Filename: "{#Java}"; Parameters: "{#Argumentos}"; WorkingDir: "{app}"; IconFilename: "{app}\vyra.ico"; Tasks: escritorio

[Run]
Filename: "{#Java}"; Parameters: "{#Argumentos}"; WorkingDir: "{app}"; Description: "Abrir Vyra ahora"; Flags: nowait postinstall skipifsilent
