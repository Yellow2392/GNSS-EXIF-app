package com.example.exifmaster

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var captureButton: Button
    private lateinit var browseButton: Button
    private lateinit var previewView: PreviewView
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var rotationData = FloatArray(3)
    private var lastLocation: Location? = null

    private var imageCapture: ImageCapture? = null

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480))
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("EXIFMaster", "Camera initialization failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captureButton = findViewById(R.id.captureButton)
        browseButton = findViewById(R.id.browseButton)
        previewView = findViewById(R.id.previewView)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startCamera()

        registerSensors()
        requestLocationUpdates()
        requestPermissions()

        captureButton.setOnClickListener {
            if (lastLocation == null) {
                Toast.makeText(this, "Esperando señal GPS...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            capturePhoto()

            val overlay = View(this).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.WHITE)
                alpha = 0f
            }

            (window.decorView as ViewGroup).addView(overlay)

            overlay.animate()
                .alpha(1f)
                .setDuration(100)
                .withEndAction {
                    overlay.animate()
                        .alpha(0f)
                        .setDuration(100)
                        .withEndAction {
                            (window.decorView as ViewGroup).removeView(overlay)
                        }
                }

        }
        browseButton.setOnClickListener {
            val intent = Intent(this, PhotoGalleryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerSensors() {
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, this)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationValues)
            rotationData = orientationValues.map { Math.toDegrees(it.toDouble()).toFloat() }.toFloatArray()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onLocationChanged(location: Location) {
        lastLocation = location
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("EXIFMaster", "Photo captured: ${photoFile.absolutePath}")
                    addExifMetadata(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("EXIFMaster", "Photo capture failed", exception)
                }
            })
    }

    private fun saveImage(bitmap: Bitmap): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(storageDir, "IMG_$timestamp.jpg")

        FileOutputStream(photoFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return photoFile
    }

    private fun addExifMetadata(photoFile: File) {
        try {
            val exif = ExifInterface(photoFile.absolutePath)

            // Datos del GNSS en tiempo real
            /*
            lastLocation?.let {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToDMS(it.latitude))
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToDMS(it.longitude))
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, it.altitude.toString())
            }
            */
            lastLocation?.let { location ->
                Log.d("EXIFMaster", "Location Data - Lat: ${location.latitude}, Lon: ${location.longitude}, Alt: ${location.altitude}")

                val latDMS = convertToDMS(location.latitude)
                val lonDMS = convertToDMS(location.longitude)

                Log.d("EXIFMaster", "Converted GPS - Lat: $latDMS, Lon: $lonDMS")

                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latDMS)
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (location.latitude >= 0) "N" else "S")

                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lonDMS)
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (location.longitude >= 0) "E" else "W")

                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, convertAltitudeToRational(location.altitude))
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "0") // 0 = sobre el nivel del mar

                exif.saveAttributes()
            } ?: Log.e("EXIFMaster", "Error: lastLocation is null")

            // Datos de orientación del dispositivo
            exif.setAttribute("UserComment", "Pitch: ${rotationData[0]}, Yaw: ${rotationData[1]}, Roll: ${rotationData[2]}")

            // Datos de la cámara
            exif.setAttribute(ExifInterface.TAG_MAKE, "Android")
            exif.setAttribute(ExifInterface.TAG_MODEL, android.os.Build.MODEL)
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "8.8")
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "ASCII Pitch: ${rotationData[0]}, Yaw: ${rotationData[1]}, Roll: ${rotationData[2]}")

            // Guardar cambios
            exif.saveAttributes()
            Log.d("EXIFMaster", "GPS Latitud: ${exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)}")
            Log.d("EXIFMaster", "GPS Longitud: ${exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)}")
            Log.d("EXIFMaster", "GPS Altitud: ${exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)}")
            Log.d("EXIFMaster", "EXIF data written successfully")
        } catch (e: Exception) {
            Log.e("EXIFMaster", "Error writing EXIF data", e)
        }
    }

    /*
    private fun convertToDMS(coord: Double): String {
        val degrees = coord.toInt()
        val minutes = ((coord - degrees) * 60).toInt()
        val seconds = (((coord - degrees) * 60 - minutes) * 60 * 100).toInt()
        return "$degrees/1,$minutes/1,$seconds/100"
    }
    */

    private fun convertToDMS(coordinate: Double): String {
        val sb = StringBuilder(20)
        var latitude = coordinate
        latitude = abs(latitude)
        val degree = latitude.toInt()
        latitude *= 60.0
        latitude -= (degree * 60.0)
        val minute = latitude.toInt()
        latitude *= 60.0
        latitude -= (minute * 60.0)
        val second = (latitude * 1000.0).toInt()

        sb.setLength(0)
        sb.append(degree)
        sb.append("/1,")
        sb.append(minute)
        sb.append("/1,")
        sb.append(second)
        sb.append("/1000")
        return sb.toString()
    }

    private fun convertAltitudeToRational(altitude: Double): String {
        return "${(altitude * 1000).toInt()}/1000"
    }

}

private fun Preview.setSurfaceProvider(surfaceTextureListener: TextureView.SurfaceTextureListener?) {

}
