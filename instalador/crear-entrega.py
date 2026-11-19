"""Arma la carpeta de entrega del proyecto, fuera del repositorio:

    ..\\entrega\\Vyra-Setup-<versión>.exe      instalador (antes: instalador\\crear-instalador.ps1)
    ..\\entrega\\Manual-de-usuario-Vyra.pdf    docs/06-manual-usuario.md con sus capturas
    ..\\entrega\\Vyra-codigo-fuente.zip        lo que está en Git + archivos nuevos (sin target, vyra.db ni secretos)

Uso (desde la raíz del repo):  python instalador\\crear-entrega.py
Necesita: pip install markdown  y Google Chrome (para imprimir el PDF).
"""
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

import markdown

RAIZ = Path(__file__).resolve().parent.parent
ENTREGA = RAIZ.parent / "entrega"
VERSION = "1.0.0"
CHROME = next((p for p in [
    Path(os.environ.get("ProgramFiles", r"C:\Program Files")) / "Google/Chrome/Application/chrome.exe",
    Path(os.environ.get("ProgramFiles(x86)", r"C:\Program Files (x86)")) / "Google/Chrome/Application/chrome.exe",
    Path(os.environ.get("LOCALAPPDATA", "")) / "Google/Chrome/Application/chrome.exe",
] if p.exists()), None)

ESTILO = """
@page { size: A4; margin: 18mm 16mm; }
body { font-family: 'Segoe UI', Inter, Arial, sans-serif; color: #0F172A; font-size: 11pt; line-height: 1.5; }
h1 { color: #0F766E; font-size: 24pt; margin: 0 0 4pt; }
h2 { color: #0F766E; border-bottom: 2px solid #99F6E4; padding-bottom: 3pt; margin-top: 22pt; page-break-after: avoid; }
h3 { color: #115E59; page-break-after: avoid; }
img { max-width: 100%; border: 1px solid #E2E8F0; border-radius: 6px; margin: 6pt 0; page-break-inside: avoid; }
table { border-collapse: collapse; width: 100%; margin: 8pt 0; font-size: 10pt; page-break-inside: avoid; }
th, td { border: 1px solid #E2E8F0; padding: 5pt 7pt; text-align: left; vertical-align: top; }
th { background: #F0FDFA; }
code { background: #F1F5F9; padding: 1pt 4pt; border-radius: 3px; font-size: 9.5pt; }
pre { background: #F1F5F9; padding: 8pt; border-radius: 6px; }
.portada { text-align: center; padding-top: 70mm; page-break-after: always; }
.portada img { width: 34mm; border: none; }
.portada h1 { font-size: 34pt; }
.portada p { color: #475569; font-size: 13pt; }
"""


def manual_pdf(destino: Path):
    md = (RAIZ / "docs/06-manual-usuario.md").read_text(encoding="utf-8")
    md = md.replace("](./manual/", "](" + (RAIZ / "docs/manual").as_uri() + "/")
    cuerpo = markdown.markdown(md, extensions=["tables", "fenced_code"])
    icono = (RAIZ / "instalador/vyra.png").as_uri()
    html = f"""<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Manual de usuario de Vyra</title>
<style>{ESTILO}</style></head><body>
<div class="portada"><img src="{icono}"><h1>Vyra</h1><p>Manual de usuario · versión 1.1</p>
<p>Sistema de gestión de citas y expedientes para un consultorio dental</p></div>
{cuerpo}</body></html>"""
    temporal = ENTREGA / "_manual.html"
    temporal.write_text(html, encoding="utf-8")
    if CHROME is None:
        sys.exit("No se encontró Google Chrome para imprimir el PDF")
    subprocess.run([str(CHROME), "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                    f"--print-to-pdf={destino}", temporal.as_uri()], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    temporal.unlink()


def codigo_fuente(destino: Path):
    archivos = subprocess.run(["git", "ls-files", "-co", "--exclude-standard"], cwd=RAIZ, check=True,
                              capture_output=True, text=True).stdout.splitlines()
    with zipfile.ZipFile(destino, "w", zipfile.ZIP_DEFLATED) as z:
        for nombre in archivos:
            ruta = RAIZ / nombre
            if ruta.is_file():
                z.write(ruta, "vyra-dentalSoftware/" + nombre)
    return len(archivos)


def main():
    ENTREGA.mkdir(exist_ok=True)
    instalador = RAIZ / f"target/instalador/Vyra-Setup-{VERSION}.exe"
    if not instalador.exists():
        sys.exit(f"Falta {instalador}: primero corre instalador\\crear-instalador.ps1")
    shutil.copy2(instalador, ENTREGA / instalador.name)
    manual_pdf(ENTREGA / "Manual-de-usuario-Vyra.pdf")
    n = codigo_fuente(ENTREGA / "Vyra-codigo-fuente.zip")
    for archivo in sorted(ENTREGA.iterdir()):
        print(f"{archivo.name:32} {archivo.stat().st_size / 1_048_576:6.1f} MB")
    print(f"({n} archivos en el código fuente) -> {ENTREGA}")


if __name__ == "__main__":
    main()
