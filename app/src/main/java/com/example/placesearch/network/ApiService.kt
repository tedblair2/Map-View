package com.example.placesearch.network

import com.example.placesearch.model.SearchResult
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("/search")
    fun search(
        @Query("q") query:String,
        @Query("format") format:String="json",
        @Query("addressdetails") includeAddressDetails: Boolean = true,
        @Query("limit") limit: Int = 10
    ):Call<List<SearchResult>>

    companion object{
        operator fun invoke():ApiService{
            val BASE_URL="https://nominatim.openstreetmap.org/"

            val retrofit by lazy {
                Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)
            }
            return retrofit
        }
    }
}