package com.guesswhere.app.data

import retrofit2.http.GET
import retrofit2.http.QueryMap

/** MediaWiki action API endpoint on Wikimedia Commons. */
interface CommonsApi {
    @GET("w/api.php")
    suspend fun query(@QueryMap params: Map<String, String>): MwResponse
}