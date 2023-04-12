package com.miumiu.gratic.data.remote.ws.models

import com.miumiu.data.models.BaseModel
import com.miumiu.gratic.util.Constants.TYPE_GAME_ERROR

data class GameError(
    val errorType: Int
) : BaseModel(TYPE_GAME_ERROR) {

    companion object {

        const val ERROR_ROOM_NOT_FOUND = 0
    }
}
