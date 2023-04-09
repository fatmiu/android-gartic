package com.miumiu.gratic.repository

import android.content.Context
import com.miumiu.gratic.R
import com.miumiu.gratic.data.remote.responses.BasicApiResponse
import com.miumiu.gratic.util.Resource
import com.miumiu.gratic.util.checkForInternetConnection
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

open class BaseRepository @Inject constructor(private val context: Context) {

    protected suspend fun <T> responseFlow(call: suspend () -> Response<T>): Resource<T> {
        if (!context.checkForInternetConnection()) {
            return Resource.Error(context.getString(R.string.error_internet_turned_off))
        }
        val response = try {
            call()
        } catch (e: HttpException) {
            return Resource.Error(context.getString(R.string.error_http))
        } catch (e: IOException) {
            return Resource.Error(context.getString(R.string.check_internet_connection))
        }

        return if (response.isSuccessful && response.body() != null) {
            Resource.Success(response.body()!!)
        }else {
            Resource.Error(context.getString(R.string.error_unknown))
        }
    }

    protected suspend fun unitResponseFlow(call: suspend () -> Response<BasicApiResponse>): Resource<Unit> {
        if (!context.checkForInternetConnection()) {
            return Resource.Error(context.getString(R.string.error_internet_turned_off))
        }
        val response = try {
            call()
        } catch (e: HttpException) {
            return Resource.Error(context.getString(R.string.error_http))
        } catch (e: IOException) {
            return Resource.Error(context.getString(R.string.check_internet_connection))
        }

        return if (response.isSuccessful && response.body()?.successful == true) {
            Resource.Success(Unit)
        } else if (response.body()?.successful == false) {
            Resource.Error(response.body()!!.message!!)
        } else {
            Resource.Error(context.getString(R.string.error_unknown))
        }
    }
}