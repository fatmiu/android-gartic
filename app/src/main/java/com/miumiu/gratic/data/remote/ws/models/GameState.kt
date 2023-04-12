package com.miumiu.gratic.data.remote.ws.models

import com.miumiu.data.models.BaseModel
import com.miumiu.gratic.util.Constants.TYPE_GAME_STATE

data class GameState(
    val drawingPlayer: String,
    val word: String
) : BaseModel(TYPE_GAME_STATE)
