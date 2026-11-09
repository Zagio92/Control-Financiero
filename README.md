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
| `android/` | App Android (Kotlin): muestra la web y se actualiza desde GitHub |
| `.github/workflows/android.yml` | Compila el APK y lo publica como Release en cada cambio |

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

## App Android y actualizaciones automáticas

La app Android (`android/`) muestra la misma web, que se copia sola dentro del APK al compilar. Solo hay una copia de la web: la de la raíz del repositorio.

Cada vez que se sube a `main` un cambio de la app (web o `android/`), GitHub Actions:

1. Compila el APK firmado, con versión `1.0.N` (N = número de compilación).
2. Lo publica como Release `vN` con el archivo `finanzapp.apk`.

La app del móvil consulta la última Release al abrirse (como mucho cada 6 horas) y, si hay una versión mayor, ofrece **Actualizar**: la descarga y abre el instalador de Android. También se puede comprobar a mano en *Más › Configuración › App Android*. Los datos no se tocan al actualizar.

### Firma

Android solo acepta una actualización firmada con la misma clave que la app instalada. La clave **no está en el repositorio**: está guardada en estos secretos del repositorio (*Settings › Secrets and variables › Actions*):

| Secreto | Contenido |
|---|---|
| `KEYSTORE_BASE64` | El archivo `finanzapp.jks` en base64 |
| `KEYSTORE_PASSWORD` | Contraseña del almacén |
| `KEY_ALIAS` | `finanzapp` |
| `KEY_PASSWORD` | Contraseña de la clave (la misma) |

Sin los secretos, la compilación se hace igualmente (para comprobar que funciona), pero no se publica la Release. Guarda una copia de `finanzapp.jks` y su contraseña: si se pierden, habrá que desinstalar la app y empezar con una clave nueva.
