package com.miumiu.gratic.ui.create_room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miumiu.gratic.data.remote.ws.Room
import com.miumiu.gratic.repository.SetupRepository
import com.miumiu.gratic.util.Constants.MAX_ROOM_NAME_LENGTH
import com.miumiu.gratic.util.Constants.MIN_ROOM_NAME_LENGTH
import com.miumiu.gratic.util.DispatcherProvider
import com.miumiu.gratic.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val repository: SetupRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    sealed class Event {
        object InputEmptyError : Event()
        object InputTooShortError : Event()
        object InputTooLongError : Event()

        data class CreateRoomEvent(val room: Room) : Event()
        data class CreateRoomErrorEvent(val error: String) : Event()

        data class JoinRoomEvent(val roomName: String) : Event()
        data class JoinRoomErrorEvent(val error: String) : Event()
    }

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    fun createRoom(room: Room) {
        viewModelScope.launch(dispatchers.main) {
            val trimmedRoomName = room.name.trim()
            when {
                trimmedRoomName.isEmpty() -> {
                    _event.emit(Event.InputEmptyError)
                }

                trimmedRoomName.length < MIN_ROOM_NAME_LENGTH -> {
                    _event.emit(Event.InputTooShortError)
                }

                trimmedRoomName.length > MAX_ROOM_NAME_LENGTH -> {
                    _event.emit(Event.InputTooLongError)
                }

                else -> {
                    val result = repository.createRoom(room)
                    if (result is Resource.Success) {
                        _event.emit(Event.CreateRoomEvent(room))
                    } else {
                        _event.emit(
                            Event.CreateRoomErrorEvent(
                                result.message ?: return@launch
                            )
                        )
                    }
                }
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