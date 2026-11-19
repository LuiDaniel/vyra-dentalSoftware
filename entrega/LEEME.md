# Entrega de Vyra

| Archivo | Qué es |
|---|---|
| `Vyra-Setup-1.0.0.exe` | Instalador para Windows 10/11 (64 bits). No necesita Java ni permisos de administrador. Abre Vyra desde el acceso directo del menú Inicio o del escritorio. Primer ingreso: `admin` / `admin123`. |
| `Manual-de-usuario-Vyra.pdf` | Manual de usuario con capturas. |
| `Vyra-video-presentacion.mp4` | Video de presentación (1 min 35 s, 1920×1080). |

El código fuente es el resto de este repositorio. Para volver a armar el instalador: `instalador\crear-instalador.ps1`.

**Ojo con Smart App Control (Windows 11):** abre Vyra siempre desde el acceso directo que crea el instalador, no desde `Vyra.exe` dentro de la carpeta de instalación (ese lanzador no tiene firma digital y Windows lo bloquea).
