package be.appwise.core.networking.base

import android.util.Log
import be.appwise.core.networking.HeaderInterceptor
import be.appwise.core.networking.Networking
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.realm.RealmObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

abstract class BaseRestClient {
    companion object {
        const val TAG = "BaseRestClient"
    }

    protected abstract val protectedClient: Boolean

    protected abstract fun getBaseUrl(): String

    val getHttpClient: OkHttpClient by lazy {
        Log.d(TAG, "getHttpClient")
        createHttpClient()
    }

    protected val getRetrofit: Retrofit.Builder by lazy {
        Log.d(TAG, "getRetrofit")
        createRetrofit()
    }

    protected open fun createHttpClient(): OkHttpClient {
        Log.d(TAG, "createHttpClient")
        return OkHttpClient.Builder()
            .addInterceptor(getHttpLogging())
            .addInterceptor(
                HeaderInterceptor(
                    Networking.getAppName(),
                    Networking.versionName(),
                    Networking.versionCode(),
                    Networking.apiVersion(),
                    Networking.getApplicationId(),
                    protectedClient
                )
            ).build()
    }

    protected open fun createRetrofit(): Retrofit.Builder {
        Log.d(TAG, "createRetrofit")
        return Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(getFactory())
            .client(getHttpClient)
    }

    protected fun getFactory(): GsonConverterFactory {
        return GsonConverterFactory.create(getGson())
    }

    protected fun getHttpLogging(): Interceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private fun getGson(): Gson {
        val builder = GsonBuilder()
        builder.setExclusionStrategies(object : ExclusionStrategy {
            override fun shouldSkipField(f: FieldAttributes): Boolean {
                return f.declaringClass == RealmObject::class.java
            }

            override fun shouldSkipClass(clazz: Class<*>): Boolean {
                return false
            }
        })
        return builder.create()
    }
}