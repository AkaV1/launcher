# Switch Launcher

Launcher para Android con estilo de consola (inspirado en la pantalla de inicio de Nintendo Switch).
Hecho con Kotlin + Jetpack Compose.

## Cómo compilarlo

1. Abre la carpeta `SwitchLauncher` en **Android Studio** (Koala o más nuevo).
2. Espera a que termine el *Gradle Sync* (descarga Gradle 8.9 y las dependencias automáticamente).
3. Conecta tu teléfono (depuración USB activada) o usa un emulador y pulsa **Run**.
   Para generar un APK: *Build > Build Bundle(s) / APK(s) > Build APK(s)*.

## Alternativa sin Android Studio: compilar el APK en GitHub

1. Crea una cuenta gratis en github.com y un repositorio nuevo (puede ser privado).
2. Sube **el contenido** de la carpeta `SwitchLauncher` a la raíz del repositorio
   (incluida la carpeta oculta `.github`).
3. Ve a la pestaña **Actions > Build APK > Run workflow**.
4. Al terminar (unos 5 minutos), abre la ejecución y descarga **SwitchLauncher-APK** (viene en un ZIP).
5. Pasa `app-debug.apk` al teléfono e instálalo (permite "instalar apps desconocidas" cuando te lo pida).

## Cómo usarlo como pantalla de inicio

Al presionar el botón Home de Android te preguntará qué launcher usar: elige **Switch Launcher**
y "Siempre". También puedes hacerlo en *Ajustes > Apps > Apps predeterminadas > App de inicio*.

## Funciones

- Fila de mosaicos con animación y borde pulsante rojo/azul en el juego seleccionado.
- Detecta solo los **juegos instalados** (botón "Todas las apps" para ver todo).
- Favoritos (mantén pulsado un mosaico): aparecen primero y se guardan.
- Se actualiza solo al instalar o desinstalar apps.
- Reloj y batería en la barra superior, tema claro/oscuro.
- Sonidos de navegación y vibración suave (usa los efectos de sonido del sistema).

## Controles del mando Bluetooth

| Botón | Acción |
|---|---|
| D-pad izquierda / derecha | Mover selección |
| L1 / R1 | Saltar 5 juegos |
| A (o Enter) | Abrir juego |
| Y | Marcar / quitar favorito |
| X | Alternar juegos / todas las apps |
| Start / Select | Cambiar tema |

Táctil: toca un mosaico para seleccionarlo, tócalo de nuevo para abrirlo, mantenlo pulsado para favorito.

## Notas

- Los sonidos usan los efectos del sistema; si no los oyes, activa "Sonidos al tocar" en los ajustes de Android.
- El stick analógico no está mapeado (solo D-pad y botones). Se puede añadir con `onGenericMotionEvent`.
- Para una imagen/fondo propio, cambia `ic_launcher.xml` y los colores en `HomeScreen.kt`.
