package com.example.sneezeblesser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sneezeblesser.ui.theme.SneezeBlesserTheme
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private var isListening = false
    private val sampleRate = 44100 // Standard sampling rate for audio recording
    private lateinit var audioRecord: AudioRecord
    private lateinit var audioBuffer: ShortArray


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SneezeBlesserTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        requestMicrophonePermission()
    }
    private fun requestMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is granted
                startListening()
            }
            else -> {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startListening()
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startListening() {
        isListening = true

        // Initialize AudioRecord
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioBuffer = ShortArray(bufferSize)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord.startRecording()

        // Start a background thread to process the audio
        thread(start = true) {
            while (isListening) {
                val read = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                if (read > 0) {
                    // Process the audioBuffer to detect a sneeze
                    val isSneezeDetected = detectSneeze(audioBuffer, read)
                    if (isSneezeDetected) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Bless you!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // Simple sneeze detection based on amplitude threshold
    private fun detectSneeze(audioData: ShortArray, readSize: Int): Boolean {
        val threshold = 4500 // This threshold may need tuning
        for (i in 0 until readSize) {
//            println(audioData[i].toInt())
            if (audioData[i].toInt() >= threshold) {
                println("Sneeze detected!")
                return true
            }
        }
        return false
    }


    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }

    private fun stopListening() {
        isListening = false
        audioRecord.stop()
        audioRecord.release()
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SneezeBlesserTheme {
        Greeting("Android")
    }
}

