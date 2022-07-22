package com.uno.datalayer

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ClientDataViewModel :
    ViewModel(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

//    private val _events = mutableStateListOf<Event>()
//    val events: List<Event> = _events

    private val _liveData = MutableLiveData<Event>()
    val liveData: LiveData<Event> = _liveData

    private val _liveList = MutableLiveData<List<Event>>()
    val liveList : LiveData<List<Event>> = _liveList

    override fun onDataChanged(dataEvents: DataEventBuffer) {

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
        }

    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val e = Event(
            title = R.string.message_from_watch,
            text = messageEvent.toString()
        )
        addEvent(e)

        if (messageEvent.path == VOICE_TRANSCRIPTION_MESSAGE_PATH){
            val byteArray = messageEvent.data
            val shorts = ShortArray(byteArray.size / 2)
            ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

            Log.d("ClientDataModel", shorts.toString())
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        val e = Event(
            title = R.string.capability_changed,
            text = capabilityInfo.toString()
        )
        addEvent(e)
    }

    fun addEvent(event: Event){
        _liveList.value = _liveList.value?.plus(event) ?: listOf(event)
    }

    companion object {
        const val VOICE_TRANSCRIPTION_MESSAGE_PATH = "/voice_transcription"
    }
}

/**
 * A data holder describing a client event.
 */
data class Event(
    @StringRes var title: Int,
    val text: String
)
