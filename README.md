# GNSS-EXIFMaster App (Android)

## Arquitectura del Aplicativo: 

### Estructura del código (Kotlin + Jetpack Compose)

#### Módulo de Captura Fotográfica:
- Utiliza la API de cámara de Android (CameraX).

#### Módulo de Sensores IMU:
- Obtiene los datos de orientación y aceleración del IMU del celular.

#### Módulo de GNSS Externo:
- Lee y procesa datos NMEA vía USB o Bluetooth.

#### Módulo de Edición EXIF:
- Inserta los metadatos EXIF en las imágenes capturadas.

#### Módulo de Galería & Visualización:
- Permite ver y explorar las fotos junto a su información EXIF.

#### Módulo de Configuración Automática:
- Calibra sensores y ajusta parámetros según el celular.

## Descripción del APP: 
Un aplicativo que hace los siguiente: Tomar fotografías usando la cámara del celular y agregarles -a las imágenes capturadas y guardadas en formato JPG- unas geoetiquetas y angulos de inclinación del celular a los metadatos exif de cada imagen. Los datos de posicionamiento vienen de un dispositivo externo (un receptor GNSS que utiliza un módulo GNSS marca u-blox modelo ZED-F9P que entrega la información en formato NMEA usando un conector USB) y los datos del IMU (angulos de inclinación de la cámara de fotos) son entregados por el sistema operativo del celular. De igual forma, debe encontrarse la forma de obtener automaticamente los offsets de la posición de la cámara en relación al modelo del celular usado por la aplicación. 
Se quiere lograr lo mismo que hace la cámara de fotos de un drone de la marca DJI, modelo Mavic, con la información que captura en cada foto y la graba en los metadatos EXIF de cada foto. Existe un archivo de referencia. 

El hardware que se usará es el siguiente:
1. Receptor GNSS marca Mettatec, modelo x5 Mobile (Toda le información técnica está en el link: https://docs.mettatec.com/X5MPro/).
2. Un celular en Android.
3. Un cable de conexión USB para conectar el celular con el receptor GNSS.

Aplicación sencilla para asignar la posición de un receptor GNSS externo y el IMU del celular a los metadatos EXIF de cada imagen capturada.
