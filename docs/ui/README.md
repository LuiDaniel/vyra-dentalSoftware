# Diseño de referencia de la interfaz

Maquetas en HTML de las pantallas de Vyra. Son la referencia visual para programar el frontend en JavaFX; las especificaciones (comportamiento, validaciones, roles) están en [`../08-guia-frontend.md`](../08-guia-frontend.md).

**Cómo verlas:** abre `Inicio.html` con doble clic (o arrástralo al navegador). El menú lateral enlaza las demás pantallas. La fuente Inter se carga desde Google Fonts; sin internet se ve con la fuente del sistema.

| Archivo | Pantalla | Sección de la guía |
|---|---|---|
| [`Login.html`](./Login.html) | Inicio de sesión | 7.1 |
| [`Inicio.html`](./Inicio.html) | Inicio (dashboard) | 7.2 |
| [`Citas.html`](./Citas.html) | Agenda de citas, con el formulario "Nueva cita" y el error de cruce de horario | 7.3 |
| [`Pacientes.html`](./Pacientes.html) | Lista de pacientes y ficha | 7.4 |
| [`Odontograma.html`](./Odontograma.html) | Odontograma en notación FDI | 7.6 |
| [`Pagos.html`](./Pagos.html) | Pagos y registro de pago | 7.8 |
| [`Sistema.html`](./Sistema.html) | Colores, tipografía, componentes y su equivalencia en JavaFX | 2 a 5 |

Notas:
- Los nombres, montos y fechas son **datos de ejemplo**.
- Estas páginas son estáticas: muestran el estado inicial de cada pantalla. En el lienzo original (https://claude.ai/artifact/5kwuCb73dLZPVYQn4iB68W) el odontograma, la agenda y los formularios responden a clics.
- Historia clínica, Tratamientos, Reportes, Profesionales y Usuarios no tienen maqueta propia: siguen los mismos patrones, descritos en la guía (secciones 7.5, 7.7, 7.9, 7.10 y 7.11).
