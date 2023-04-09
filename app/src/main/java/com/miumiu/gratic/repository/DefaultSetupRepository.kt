package com.miumiu.gratic.repository

import android.content.Context
import com.miumiu.gratic.data.remote.api.SetupApi
import com.miumiu.gratic.data.remote.ws.Room
import com.miumiu.gratic.util.Resource
import javax.inject.Inject

class DefaultSetupRepository @Inject constructor(
    private val setupApi: SetupApi,
    private val context: Context
) : BaseRepository(context), SetupRepository {

    override suspend fun createRoom(room: Room): Resource<Unit> {
        return unitResponseFlow { setupApi.createRoom(room) }
    }

    override suspend fun getRooms(searchQuery: String): Resource<List<Room>> {
        return responseFlow { setupApi.getRooms(searchQuery) }
    }

    override suspend fun joinRoom(username: String, roomName: String): Resource<Unit> {
        return unitResponseFlow { setupApi.joinRoom(username, roomName) }
    }
}