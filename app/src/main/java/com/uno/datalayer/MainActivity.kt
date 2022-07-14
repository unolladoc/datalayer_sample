package com.uno.datalayer

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant

class MainActivity : ComponentActivity() {

    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(this) }
    private val nodeClient by lazy { Wearable.getNodeClient(this) }

    private val handheldDataViewModel by viewModels<HandheldDataViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var count = 0

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var lastTriggerTime = Instant.now() - (countInterval - Duration.ofSeconds(1))
                while (isActive) {

                    delay(
                        Duration.between(Instant.now(), lastTriggerTime + countInterval).toMillis()
                    )
                    lastTriggerTime = Instant.now()
                    sendCount(count)

                    count++
                }
            }
        }

        val launch = findViewById<Button>(R.id.launch)
        launch.setOnClickListener {
            startWearableActivity()
        }
    }

    private suspend fun sendCount(count: Int) {
        try {
            val request = PutDataMapRequest.create(COUNT_PATH).apply {
                dataMap.putInt(COUNT_KEY, count)
            }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(request).await()

            Log.d(TAG, "DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d(TAG, "Saving DataItem failed: $exception")
        }
    }

    private fun startWearableActivity() {
        lifecycleScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()

                // Send a message to all nodes in parallel
                nodes.map { node ->
                    async {
                        messageClient.sendMessage(node.id, START_ACTIVITY_PATH, byteArrayOf())
                            .await()
                    }
                }.awaitAll()

                Log.d(TAG, "Starting activity requests sent successfully")
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "Starting activity failed: $exception")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(handheldDataViewModel)
        messageClient.addListener(handheldDataViewModel)
        capabilityClient.addListener(
            handheldDataViewModel,
            Uri.parse("wear://"),
            CapabilityClient.FILTER_REACHABLE
        )
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(handheldDataViewModel)
        messageClient.removeListener(handheldDataViewModel)
        capabilityClient.removeListener(handheldDataViewModel)
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val COUNT_PATH = "/count"
        private const val MESSAGE_PATH = "/message"
        private const val MESSAGE_KEY = "message"
        private const val TIME_KEY = "time"
        private const val COUNT_KEY = "count"

        private val countInterval = Duration.ofSeconds(5)
    }
}