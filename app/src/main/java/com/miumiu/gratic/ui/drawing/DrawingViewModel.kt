package com.miumiu.gratic.ui.drawing

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.miumiu.gratic.R
import com.miumiu.gratic.data.remote.ws.DrawingApi
import com.miumiu.gratic.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingApi: DrawingApi,
    private val dispatcherProvider: DispatcherProvider,
    private val gson: Gson
) : ViewModel() {

    private val _selectedColorButtonId = MutableStateFlow(R.id.rbBlack)
    val selectedColorButtonId: StateFlow<Int> = _selectedColorButtonId

    fun checkRadioButton(id: Int) {
        _selectedColorButtonId.value = id
    }
}