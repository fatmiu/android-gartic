package com.miumiu.gratic.data.remote.ws.models

import com.miumiu.data.models.BaseModel
import com.miumiu.gratic.util.Constants.TYPE_CHOSEN_WORD

data class ChosenWord(
    val chosenWord: String,
    val roomName: String
) : BaseModel(TYPE_CHOSEN_WORD)
