package dev.streamgate.android.data.remote

import dev.streamgate.android.data.remote.model.request.UploadRequest
import dev.streamgate.android.data.remote.model.response.ListMediaResponse
import dev.streamgate.android.data.remote.model.response.MediaResponse
import dev.streamgate.android.data.remote.model.response.UploadUrlResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FastPixApi {

    @POST("on-demand/upload")
    suspend fun initiateAndGetSessionUri(
        @Header("Authorization") authHeader: String,
        @Body request: UploadRequest
    ): Response<UploadUrlResponse>

    @GET("on-demand/{uploadId}")
    suspend fun getMediaInfo(
        @Header("Authorization") authHeader: String,
        @Path("uploadId") uploadId: String
    ): Response<MediaResponse>

    @GET("on-demand")
    suspend fun listMedia(
        @Header("Authorization") authHeader: String,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 1,
        @Query("orderBy") orderBy: String = "desc",  // "asc" or "desc"
    ): Response<ListMediaResponse>

}