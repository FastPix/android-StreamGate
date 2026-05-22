package dev.streamgate.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.streamgate.android.data.remote.FastPixApi
import dev.streamgate.android.data.repository.UploadRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideUploadRepository(
        fastPixApi: FastPixApi,
        @ApplicationContext appContext: Context
    ): UploadRepository {
        return UploadRepository(fastPixApi, appContext)
    }

}