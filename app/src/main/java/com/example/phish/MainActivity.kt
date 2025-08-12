package com.example.phish

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {
    private lateinit var tflite: Interpreter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val urlInput = findViewById<EditText>(R.id.urlInput)
        val checkButton = findViewById<Button>(R.id.checkButton)
        val resultText = findViewById<TextView>(R.id.resultText)

        tflite = Interpreter(loadModelFile())

        checkButton.setOnClickListener {
            val url = urlInput.text.toString()
            val features = extractFeatures(url)
            val prediction = predict(features)

            if (prediction > 0.5) {
                resultText.text = "🚨 May Be A Phishing URL!"
            } else {
                resultText.text = "✅ Safe URL"
            }
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = assets.openFd("phishing_detector.tflite")
        val inputStream = assetFileDescriptor.createInputStream()
        val modelBytes = inputStream.readBytes()
        val byteBuffer = ByteBuffer.allocateDirect(modelBytes.size)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(modelBytes)
        return byteBuffer
    }

    private fun extractFeatures(url: String): FloatArray {
        return floatArrayOf(
            url.length.toFloat(),
            if (url.contains("@")) 1.0f else 0.0f,
            url.count { it == '.' }.toFloat(),
            if (url.startsWith("https")) 1.0f else 0.0f,
            url.toSet().size.toFloat() / url.length
        )
    }

    private fun predict(input: FloatArray): Float {
        val inputBuffer = ByteBuffer.allocateDirect(input.size * 4).order(ByteOrder.nativeOrder())
        input.forEach { inputBuffer.putFloat(it) }

        val outputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        tflite.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        return outputBuffer.float
    }
}
