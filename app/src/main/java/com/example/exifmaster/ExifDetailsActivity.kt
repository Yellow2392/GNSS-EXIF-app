package com.example.exifmaster

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.example.exifmaster.R
import java.io.File

class ExifDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exif_details)

        val imageView = findViewById<ImageView>(R.id.imageView)
        val exifTextView = findViewById<TextView>(R.id.exifTextView)

        val photoPath = intent.getStringExtra("photoPath") ?: return
        imageView.setImageURI(android.net.Uri.fromFile(File(photoPath)))

        val exif = ExifInterface(photoPath)
        val exifData = """
            Latitude: ${exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)}
            Longitude: ${exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)}
            Altitude: ${exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)}
            Date: ${exif.getAttribute(ExifInterface.TAG_DATETIME)}
        """.trimIndent()

        exifTextView.text = exifData
    }
}
