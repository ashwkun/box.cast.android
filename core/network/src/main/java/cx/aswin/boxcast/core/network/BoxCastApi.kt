package cx.aswin.boxcast.core.network

import cx.aswin.boxcast.core.network.model.EpisodesResponse
import cx.aswin.boxcast.core.network.model.EpisodesPaginatedResponse
import cx.aswin.boxcast.core.network.model.PodcastResponse
import cx.aswin.boxcast.core.network.model.SearchResponse
import cx.aswin.boxcast.core.network.model.TrendingResponse
import cx.aswin.boxcast.core.network.model.SyncRequest
import cx.aswin.boxcast.core.network.model.SyncResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * BoxCast API - Cloudflare Worker proxy for Podcast Index
 */
interface BoxCastApi {
    
    @GET("trending")
    suspend fun getTrending(
        @Header("X-App-Key") apiKey: String,
        @Query("country") country: String? = "us",
        @Query("limit") limit: Int? = 50,
        @Query("cat") category: String? = null // New: Genre Filter
    ): TrendingResponse

    @GET("trending")
    @retrofit2.http.Streaming
    suspend fun getTrendingStream(
        @Header("X-App-Key") apiKey: String,
        @Query("country") country: String? = "us",
        @Query("limit") limit: Int? = 50,
        @Query("cat") category: String? = null // New: Genre Filter
    ): okhttp3.ResponseBody

    @GET("search")
    suspend fun search(
        @Header("X-App-Key") apiKey: String,
        @Query("q") query: String
    ): SearchResponse

    @GET("episodes")
    suspend fun getEpisodes(
        @Header("X-App-Key") apiKey: String,
        @Query("id") feedId: String
    ): EpisodesResponse
    
    @GET("episodes")
    suspend fun getEpisodesPaginated(
        @Header("X-App-Key") apiKey: String,
        @Query("id") feedId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("sort") sort: String = "newest"
    ): EpisodesPaginatedResponse
    
    @GET("podcast")
    suspend fun getPodcast(
        @Header("X-App-Key") apiKey: String,
        @Query("id") feedId: String
    ): PodcastResponse
    
    @POST("sync")
    suspend fun syncSubscriptions(
        @Header("X-App-Key") apiKey: String,
        @Body request: SyncRequest
    ): SyncResponse
}
