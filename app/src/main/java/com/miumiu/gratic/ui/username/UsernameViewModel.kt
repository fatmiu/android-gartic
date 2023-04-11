package com.miumiu.gratic.ui.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miumiu.gratic.util.Constants.MAX_USERNAME_LENGTH
import com.miumiu.gratic.util.Constants.MIN_USERNAME_LENGTH
import com.miumiu.gratic.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsernameViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    sealed class Event {
        object InputEmptyError : Event()
        object InputTooShortError : Event()
        object InputTooLongError : Event()

        data class NavigateToSelectRoomEvent(val username: String) : Event()
    }

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    fun validateUsernameAndNavigateToSelectRoom(username: String) {
        viewModelScope.launch(dispatchers.main) {
            val trimmedUsername = username.trim()
            when {
                trimmedUsername.isEmpty() -> {
                    _event.emit(Event.InputEmptyError)
                }

                trimmedUsername.length < MIN_USERNAME_LENGTH -> {
                    _event.emit(Event.InputTooShortError)
                }

                trimmedUsername.length > MAX_USERNAME_LENGTH -> {
                    _event.emit(Event.InputTooLongError)
                }

                else -> _event.emit(Event.NavigateToSelectRoomEvent(username))
            }
        }
    }

}