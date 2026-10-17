# Mi Control Financiero: subir a GitHub Pages

Contenido de esta carpeta (sube TODO, manteniendo la carpeta `icons`):

- `index.html`: la app completa
- `manifest.webmanifest`: datos para instalarla como app
- `sw.js`: permite usarla sin conexión
- `icons/`: iconos de la app

## Pasos

1. En github.com crea un repositorio nuevo (por ejemplo `finanzas`). Para usar GitHub Pages gratis el repositorio debe ser **público**. En él solo está el código de la app; tus datos nunca se suben, se quedan en tu móvil.
2. Pulsa *Add file › Upload files*, arrastra `index.html`, `manifest.webmanifest`, `sw.js` y la carpeta `icons`, y confirma con *Commit changes*.
3. Ve a *Settings › Pages*. En *Build and deployment* elige *Deploy from a branch*, rama `main`, carpeta `/ (root)`, y guarda.
4. Espera 1-2 minutos. Tu dirección será `https://TU-USUARIO.github.io/finanzas/`.
5. Ábrela en Chrome en el móvil, pulsa el menú ⋮ y elige *Instalar app* (o *Añadir a pantalla de inicio*).

## Actualizar la app

Sube el nuevo `index.html` al repositorio (*Add file › Upload files* y sustituye el anterior). La próxima vez que abras la app con conexión se actualiza sola. Tus datos no se tocan.

## Importante con los datos

- Se guardan en el navegador de cada dispositivo y asociados a la dirección web. No cambies el nombre del repositorio o del usuario una vez que empieces a usarla: la dirección cambiaría y la app abriría vacía.
- Si borras los datos de Chrome o desinstalas la app, se pierden. Haz copias desde *Más › Configuración › Copia de seguridad*.
- La app pide al navegador que trate los datos como persistentes. En *Configuración* puedes ver si lo concedió.
