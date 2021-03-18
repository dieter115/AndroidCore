package be.appwise.core.networking.base

import android.os.Build
import android.provider.Settings
import android.util.Log
import be.appwise.core.core.CoreApp
import be.appwise.core.networking.NetworkConstants
import be.appwise.core.networking.Networking
import be.appwise.core.networking.NetworkingUtil
import be.appwise.core.networking.bagel.BagelInterceptor
import be.appwise.core.networking.interceptors.Authenticator
import be.appwise.core.networking.interceptors.HeaderInterceptor
import be.appwise.core.networking.model.AccessToken
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

abstract class BaseRestClient<T> {
    protected open val TAG = BaseRestClient::class.java.simpleName

    protected abstract val apiService: Class<T>

    protected abstract val protectedClient: Boolean

    protected abstract fun getBaseUrl(): String

    protected open val appName = Networking.getAppName()
    protected open val versionName = Networking.getVersionName()
    protected open val versionCode = Networking.getVersionCode()
    protected open val apiVersion = Networking.getApiVersion()
    protected open val packageName = Networking.getPackageName()
    open val clientId = Networking.getClientIdValue()
    open val clientSecret = Networking.getClientSecretValue()

    /**
     * Get the service attached to this retrofit object to do the network requests
     */
    val getService: T by lazy {
        Log.d(TAG, "getService()")
        getRetrofit.create(apiService)
    }

    /**
     * Get the OkHttpClient for this RestClient along with all the interceptors, headers, and more
     */
    val getHttpClient: OkHttpClient by lazy {
        Log.d(TAG, "getHttpClient")
        createHttpClient()
    }

    /**
     * Get the Retrofit object for this RestClient along with all Converters, BaseUrl, OkHttpClient, and more
     */
    val getRetrofit: Retrofit by lazy {
        Log.d(TAG, "getRetrofit")
        createRetrofit()
    }

    protected open fun createHttpClient(): OkHttpClient {
        Log.d(TAG, "createHttpClient")
        val builder = OkHttpClient.Builder()

        getInterceptors().forEach {
            builder.addInterceptor(it)
        }

        if (protectedClient) {
            builder.authenticator(Authenticator { onRefreshToken(it) })
        }
        //add it behind all the rest so we can send all the response/request data
        if (enableBagelInterceptor())
            builder.addInterceptor(getBagelInterceptor())

        return builder.build()
    }

    protected open fun onRefreshToken(refreshToken: String): AccessToken? {
        throw Exception("refreshToken should be overridden in order for this to work")
    }

    /**
     * Create a basis Retrofit object with a couple of ConverterFactories added to it
     * as well as the baseUrl and the okHttpClient.
     *
     * If a project specific object should be needed, this function can be overridden to suit your needs.
     */
    protected open fun createRetrofit(): Retrofit {
        Log.d(TAG, "createRetrofit")
        return Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(getGsonFactory())
            .client(getHttpClient)
            .build()
    }

    //<editor-fold desc="Interceptors">
    /**
     * Get the default HttpLoggingInterceptor that is being used in almost every project
     *
     * In case Bagel is enabled for this RestClient, the logging for requests and responses will be at a bare minimum.
     */
    protected fun getHttpLoggingInterceptor() = HttpLoggingInterceptor().apply {
        level =
            if (enableBagelInterceptor()) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.BODY
            }
    }

    /**
     * Get the default HeaderInterceptor that is being used in almost every project
     */
    protected fun getHeaderInterceptor() = HeaderInterceptor(
        appName,
        versionName,
        versionCode,
        apiVersion,
        packageName,
        protectedClient
    )

    protected fun getBagelInterceptor(): BagelInterceptor { val deviceName = Settings.Secure.getString(CoreApp.getContext().contentResolver, NetworkConstants.BAGEL_INTERCEPTOR_DEVICE_BLUETOOTH_NAME)
            ?: Settings.Global.getString(CoreApp.getContext().contentResolver, NetworkConstants.BAGEL_INTERCEPTOR_DEVICE_NAME)
        return BagelInterceptor(
            packageName,
            Settings.Secure.getString(CoreApp.getContext().contentResolver, Settings.Secure.ANDROID_ID),
            deviceName,
            Build.MANUFACTURER + "," + Build.MODEL + "; Android/" + Build.VERSION.SDK_INT
        )
    }

    /**
     * Get a default list of interceptors to be added to the restClient.
     *
     * In case a project specific order is needed this function can be
     * overridden and changed as needed (reordering, omitting or adding)
     */
    open fun getInterceptors(): List<Interceptor> {
        return listOf(
            getHttpLoggingInterceptor(), getHeaderInterceptor()
        )
    }

    /**
     * Allows you to enable the Bagle interceptor for an instance of the BaseRestClient
     *
     * It will be added after all other interceptors so headers and other request/response data
     * will be up to date when shown in Bagel
     *
     * Added it here so you can choose for each instance of a BaseRestclient
     * can be move to networking so you enable it for all or disable it for all
     * Maybe also limit it to DEBUG builds in future
     */

    open fun enableBagelInterceptor(): Boolean {
        return false
    }
    //</editor-fold>

    /**
     * Get the Gson Factory to handle all cases of type conversions from the responses
     */
    protected fun getGsonFactory(): GsonConverterFactory {
        return GsonConverterFactory.create(NetworkingUtil.getGson())
    }
}