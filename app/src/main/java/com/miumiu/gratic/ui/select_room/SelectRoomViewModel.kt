package com.miumiu.gratic.ui.select_room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miumiu.gratic.data.remote.ws.Room
import com.miumiu.gratic.repository.SetupRepository
import com.miumiu.gratic.util.DispatcherProvider
import com.miumiu.gratic.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectRoomViewModel @Inject constructor(
    private val repository: SetupRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    sealed class Event {

        data class GetRoomEvent(val rooms: List<Room>) : Event()
        data class GetRoomErrorEvent(val error: String) : Event()
        object GetRoomLoadingEvent : Event()
        object GetRoomEmptyEvent : Event()

        data class JoinRoomEvent(val roomName: String) : Event()
        data class JoinRoomErrorEvent(val error: String) : Event()
    }

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    private val _rooms = MutableStateFlow<Event>(Event.GetRoomEmptyEvent)
    val rooms: StateFlow<Event> = _rooms

    fun getRooms(searchQuery: String) {
        _rooms.value = Event.GetRoomLoadingEvent
        viewModelScope.launch(dispatchers.main) {
            val result = repository.getRooms(searchQuery)
            if (result is Resource.Success) {
                _rooms.value = Event.GetRoomEvent(result.data ?: return@launch)
            } else {
                _event.emit(Event.GetRoomErrorEvent(result.message ?: return@launch))
            }
        }
    }

    fun joinRoom(username: String, roomName: String) {
        viewModelScope.launch(dispatchers.main) {
            val result = repository.joinRoom(username, roomName)
            if (result is Resource.Success) {
                _event.emit(Event.JoinRoomEvent(roomName))
            } else {
                _event.emit(Event.JoinRoomErrorEvent(result.message ?: return@launch))
            }
        }
    }
}