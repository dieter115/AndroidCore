package be.appwise.core.networking.base

import android.util.Log

abstract class BaseRestClientWithService<T>: BaseRestClient() {
    protected abstract val apiService: Class<T>

    val getService: T by lazy {
        Log.d(TAG, "getService()")
        getRetrofit.baseUrl(getBaseUrl()).build().create(apiService)
    }
}