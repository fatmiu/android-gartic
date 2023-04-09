package com.miumiu.gratic.repository

import com.miumiu.gratic.data.remote.ws.Room
import com.miumiu.gratic.util.Resource

interface SetupRepository {

    suspend fun createRoom(room: Room): Resource<Unit>

    suspend fun getRooms(searchQuery: String): Resource<List<Room>>

    suspend fun joinRoom(username: String, roomName: String): Resource<Unit>
}