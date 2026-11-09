# FinanzApp · Control Financiero

App personal de control financiero: movimientos, gastos fijos, presupuesto, ahorro y deudas.

Es una aplicación web instalable (PWA): funciona en el navegador del móvil, se puede añadir a la pantalla de inicio y se usa sin conexión. Los datos se guardan solo en el dispositivo; este repositorio contiene únicamente el código.

## Estructura

| Ruta | Qué es |
|---|---|
| `index.html` | La app completa (HTML, estilos, código y el logo incrustado) |
| `manifest.webmanifest` | Nombre, colores e iconos para instalarla como app |
| `sw.js` | Service worker: guarda una copia para usarla sin conexión |
| `icons/` | Iconos de la app (los usa el manifest y `sw.js`) |
| `recursos/logo/` | Logo original en PNG (material de diseño; la app no lo carga) |

Los archivos de la app tienen que quedarse en la raíz del repositorio para que GitHub Pages los publique.

## Publicar en GitHub Pages

1. El repositorio debe ser **público** para usar GitHub Pages gratis.
2. Ve a *Settings › Pages*. En *Build and deployment* elige *Deploy from a branch*, rama `main`, carpeta `/ (root)`, y guarda.
3. Espera 1-2 minutos. La dirección será `https://zagio92.github.io/Control-Financiero/`.
4. Ábrela en Chrome en el móvil, pulsa el menú ⋮ y elige *Instalar app* (o *Añadir a pantalla de inicio*).

## Actualizar la app

Sube el nuevo `index.html` (o haz commit del cambio en `main`). La próxima vez que abras la app con conexión se actualiza sola, y tus datos no se tocan.

Si cambias la lista de archivos que se guardan sin conexión, sube también el número de `CACHE` en `sw.js` (por ejemplo `finanzas-v2`) para que se borre la copia antigua.

## Importante con los datos

- Se guardan en el navegador de cada dispositivo, asociados a la dirección web. **No cambies el nombre del repositorio ni del usuario** una vez que empieces a usarla: la dirección cambiaría y la app se abriría vacía.
- Si borras los datos de Chrome o desinstalas la app, se pierden. Haz copias desde *Más › Configuración › Copia de seguridad*.
- La app pide al navegador que trate los datos como persistentes. En *Configuración* puedes ver si lo concedió.
