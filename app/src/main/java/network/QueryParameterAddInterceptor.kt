package network

import okhttp3.Interceptor
import okhttp3.Response
import utils.WeatherHub
import utils.APP_ID
import utils.PreferenceManager

class QueryParameterAddInterceptor: Interceptor {

    val context = WeatherHub.context
    private val prefManager = PreferenceManager(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url().newBuilder()
            .addQueryParameter("appid", APP_ID)
            .addQueryParameter("units",prefManager.tempUnit)
            .build()

        val request = chain.request().newBuilder()
            .url(url)
            .build()

        return chain.proceed(request)
    }
}