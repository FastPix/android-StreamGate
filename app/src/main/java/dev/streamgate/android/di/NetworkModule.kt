package dev.streamgate.android.di

import android.util.Base64
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.streamgate.android.BuildConfig
import dev.streamgate.android.data.remote.FastPixApi
import dev.streamgate.android.utils.API_BASE_URL
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun providesFastPixApi(): FastPixApi {

        val jsonConfiguration = Json {
            ignoreUnknownKeys = true // Won't crash the app if backend adds new fields
            coerceInputValues = true
            encodeDefaults = true
        }

        val contentType = "application/json".toMediaType()

//        val loggingInterceptor = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            val credentials = "${BuildConfig.FASTPIX_TOKEN_ID}:${BuildConfig.FASTPIX_SECRET_KEY}"
            val auth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

            val requestWithHeaders = originalRequest.newBuilder()
                .header("Authorization", auth)
                .build()

            chain.proceed(requestWithHeaders)
        }

        val okHttpClient = OkHttpClient.Builder()
//            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(jsonConfiguration.asConverterFactory(contentType))
            .build()
            .create(FastPixApi::class.java)
    }

}