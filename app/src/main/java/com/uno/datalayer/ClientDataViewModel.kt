package com.uno.datalayer

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ClientDataViewModel :
    ViewModel(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private val _events = mutableStateListOf<Event>()
    val events: List<Event> = _events

    private val _liveData = MutableLiveData<Event>()
    val liveData: LiveData<Event> = _liveData

    private val _liveList = MutableLiveData<List<Event>>()
    val liveList : LiveData<List<Event>> = _liveList

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        _events.addAll(
            dataEvents.map { dataEvent ->
                val title = when (dataEvent.type) {
                    DataEvent.TYPE_CHANGED -> R.string.data_item_changed
                    DataEvent.TYPE_DELETED -> R.string.data_item_deleted
                    else -> R.string.data_item_unknown
                }

                val e = Event(
                    title = title,
                    text = dataEvent.dataItem.toString()
                )

                addEvent(e)

                Event(
                    title = title,
                    text = dataEvent.dataItem.toString()
                )
            }
        )
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        _events.add(
            Event(
                title = R.string.message_from_watch,
                text = messageEvent.toString()
            )
        )
        val e = Event(
            title = R.string.message_from_watch,
            text = messageEvent.toString()
        )
        addEvent(e)
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        _events.add(
            Event(
                title = R.string.capability_changed,
                text = capabilityInfo.toString()
            )
        )
        val e = Event(
            title = R.string.capability_changed,
            text = capabilityInfo.toString()
        )
        addEvent(e)
    }

    fun addEvent(event: Event){
        _liveList.value = _liveList.value?.plus(event) ?: listOf(event)
    }
}

/**
 * A data holder describing a client event.
 */
data class Event(
    @StringRes var title: Int,
    val text: String
)