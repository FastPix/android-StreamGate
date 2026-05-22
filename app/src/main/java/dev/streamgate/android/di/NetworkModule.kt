package dev.streamgate.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.streamgate.android.data.remote.FastPixApi
import dev.streamgate.android.utils.API_BASE_URL
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(jsonConfiguration.asConverterFactory(contentType))
            .build()
            .create(FastPixApi::class.java)
    }

}