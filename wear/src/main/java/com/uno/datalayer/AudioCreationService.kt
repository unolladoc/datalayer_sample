package com.uno.datalayer

import android.app.Service
import android.content.Intent
import android.media.AudioRecord
import android.os.IBinder
import android.util.Log
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class AudioCreationService: Service() {

    override fun onBind(p0: Intent?): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    private lateinit var classifier: AudioClassifier
    private lateinit var audioRecord: AudioRecord

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        classifier = AudioClassifier.createFromFile(this, MODEL_FILE)
        Log.d(TAG, "classifier: $classifier")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "service onStartCommand")
        startListening()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // stop listening if this service is destroyed
        stopListening()
        Log.d(TAG, "service onDestroy")
    }

    private fun startListening() {
        Log.d(TAG, "start recording")
        // Start recording
        audioRecord = classifier.createAudioRecord()
        if (audioRecord.state != AudioRecord.RECORDSTATE_RECORDING) {
            try {
                audioRecord.stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
        audioRecord.startRecording()
    }

    private fun stopListening() {
        Log.d(TAG, "stop listening")
        classifier.close()
        try {
            audioRecord.stop()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val TAG: String = AudioCreationService::class.java.name
        private const val MODEL_FILE = "yamnet.tflite"
    }
}