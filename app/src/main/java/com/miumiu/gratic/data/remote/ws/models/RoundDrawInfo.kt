package com.miumiu.gratic.data.remote.ws.models

import com.miumiu.data.models.BaseModel
import com.miumiu.gratic.util.Constants.TYPE_CUR_ROUND_DRAW_INFO

data class RoundDrawInfo(
    val data: List<String>
) : BaseModel(TYPE_CUR_ROUND_DRAW_INFO)
