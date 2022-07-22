package com.uno.datalayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.uno.datalayer.databinding.ActivityMainBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream


class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(this) }

    private val clientDataViewModel by viewModels<ClientDataViewModel>()

    private var transcriptionNodeId: String? = null

    private var recorder: MediaRecorder? = null

    private val descriptors: Array<ParcelFileDescriptor> = ParcelFileDescriptor.createPipe()
    private val parcelRead = ParcelFileDescriptor(descriptors[0])
    private val parcelWrite = ParcelFileDescriptor(descriptors[1])

    val inputStream: InputStream = ParcelFileDescriptor.AutoCloseInputStream(parcelRead)

    lateinit var audioRecord: AudioRecord
    private var hearState: Boolean = false

    private val byteArrayOutputStream = ByteArrayOutputStream()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("Permission: ", "Granted")
            } else {
                Log.i("Permission: ", "Denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        binding.hear.setOnClickListener {
//            getNodeId()
//            startListening()
//        }

        binding.hear.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hearState = true
                getNodeId()
                startListening()
            } else {
                hearState = false
                stopListening()
            }
        }

    }

    private fun startListening() {
        requestPermission()
        lifecycleScope.launch {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@launch
            }
            Log.d(TAG, "Start Listening")
            record()
        }
    }

    private fun stopListening() {
        audioRecord.stop()
        Log.d(TAG, "Stopped Listening")
    }

    private fun getNodeId() {
        lifecycleScope.launch {
            try {
                val nodes = getCapabilitiesForReachableNodes()
                    .filterValues { "voice_transcription" in it}.keys
                updateNodeId(nodes)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "Querying nodes failed: $exception")
            }
        }
    }

    private fun updateNodeId(nodes: Set<Node>) {
        val message: String?

        if (nodes.isEmpty()) {
            message = getString(R.string.no_device)
        } else {
            message = getString(R.string.connected_nodes, nodes.joinToString(", ") { it.displayName })
            transcriptionNodeId = nodes.firstOrNull { it.isNearby }?.id ?: nodes.firstOrNull()?.id
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun requestTranscription(voiceData: ByteArray) {
        transcriptionNodeId?.also { nodeId ->
            lifecycleScope.launch {
                try {
                    messageClient.sendMessage(
                        nodeId,
                        VOICE_TRANSCRIPTION_MESSAGE_PATH,
                        voiceData
                    ).await()
                    Log.d(TAG, "Voice data sent")
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (exception: Exception) {
                    Log.d(TAG, "FAILED to send voice data")
                }
            }
        }
    }

    private fun checkNodes() {
        lifecycleScope.launch {
            try {
                val nodes = getCapabilitiesForReachableNodes()
                    .filterValues { "mobile" in it || "wear" in it }.keys
                displayNodes(nodes)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "Querying nodes failed: $exception")
            }
        }
    }

    private suspend fun getCapabilitiesForReachableNodes(): Map<Node, Set<String>> =
        capabilityClient.getAllCapabilities(CapabilityClient.FILTER_REACHABLE)
            .await()
            // Pair the list of all reachable nodes with their capabilities
            .flatMap { (capability, capabilityInfo) ->
                capabilityInfo.nodes.map { it to capability }
            }
            // Group the pairs by the nodes
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            // Transform the capability list for each node into a set
            .mapValues { it.value.toSet() }

    private fun displayNodes(nodes: Set<Node>) {
        val message = if (nodes.isEmpty()) {
            getString(R.string.no_device)
        } else {
            getString(R.string.connected_nodes, nodes.joinToString(", ") { it.displayName })
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(clientDataViewModel)
        messageClient.addListener(clientDataViewModel)
        capabilityClient.addListener(
            clientDataViewModel,
            Uri.parse("wear://"),
            CapabilityClient.FILTER_REACHABLE
        )
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(clientDataViewModel)
        messageClient.removeListener(clientDataViewModel)
        capabilityClient.removeListener(clientDataViewModel)
    }

    private fun requestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("Permission: ", "Already Granted")
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            ) -> {
                Log.i("Permission: ", "Required")
                    requestPermissionLauncher.launch(
                        Manifest.permission.RECORD_AUDIO
                    )
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun record(){
        audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(RECORDING_RATE)
                    .setChannelMask(CHANNEL_IN)
                    .setEncoding(FORMAT)
                    .build()
            )
            .setBufferSizeInBytes(intSize * 3)
            .build()

        audioRecord.startRecording()
        Log.d(TAG, "Recording started")

        bufferedFile()
    }

    private suspend fun bufferedFile(){
        try {
            withContext(Dispatchers.IO) {
                applicationContext.openFileOutput(OUTPUT_FILE_NAME, MODE_PRIVATE)
                    .buffered()
                    .use { bufferedOutputStream ->
                        val buffer = ByteArray(intSize)
                        while (hearState) {
                            val read = audioRecord.read(buffer, 0, buffer.size)
                            bufferedOutputStream.write(buffer, 0, read)
                            Log.d(TAG, "Recording buffering")

                            byteArrayOutputStream.write(buffer, 0, read)

                            requestTranscription(byteArrayOutputStream.toByteArray())
                        }
                    }
            }
        } finally {
            audioRecord.release()
            Log.d(TAG, "Recording released")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        const val VOICE_TRANSCRIPTION_MESSAGE_PATH = "/voice_transcription"
        private const val MODEL_FILE = "yamnet.tflite"

        private const val RECORDING_RATE = 8000 // can go up to 44K, if needed
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val OUTPUT_FILE_NAME = "audio.pcm"

        private val intSize = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT)
    }
}