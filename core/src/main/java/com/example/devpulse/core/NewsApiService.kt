package com.example.devpulse.core

import com.example.devpulse.model.RssResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface NewsApiService {
    @GET
    suspend fun getNews(@Url url: String): RssResponse
}
