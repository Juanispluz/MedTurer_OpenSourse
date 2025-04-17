package com.example.medturer

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.hide() // Ocultar la ActionBar si es necesario

        // Obtener una referencia al ImageView del botón de retroceso
        val backButton: ImageView = findViewById(R.id.backButton)

        // Establecer un OnClickListener para el botón de retroceso
        backButton.setOnClickListener {
            // Llamar a finish() para volver a la Activity anterior
            finish()
        }


    }
}