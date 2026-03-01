package com.cuscrud

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        println("a")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val testButton = findViewById<Button>(R.id.testButton)

        testButton.setOnClickListener {
            // This message confirms your logic is running correctly on the device
            statusText.text = "Success! Debugging is active."
        }
    }
}