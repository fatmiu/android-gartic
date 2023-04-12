package com.miumiu.gratic.data.remote.ws.models

import com.miumiu.data.models.BaseModel
import com.miumiu.gratic.util.Constants.TYPE_PLAYERS_LIST

data class PlayersList(
    val players: List<PlayerData>
) : BaseModel(TYPE_PLAYERS_LIST)
