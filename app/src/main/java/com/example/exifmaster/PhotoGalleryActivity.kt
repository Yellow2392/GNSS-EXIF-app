package com.example.exifmaster

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PhotoGalleryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var photoFiles: List<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listView = ListView(this)
        setContentView(listView)

        val photosDir = File(getExternalFilesDir(null), "Pictures")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }

        photoFiles = photosDir.listFiles()?.filter { it.extension == "jpg" } ?: emptyList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, photoFiles.map { it.name })
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ExifDetailsActivity::class.java)
            intent.putExtra("photoPath", photoFiles[position].absolutePath)
            startActivity(intent)
        }
    }
}
