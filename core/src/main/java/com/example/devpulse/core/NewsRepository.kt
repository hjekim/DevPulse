package com.example.devpulse.core

import com.example.devpulse.model.NewsItem
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getBookmarks(): Flow<List<NewsItem>>
    suspend fun fetchNewsFromSources(sources: Map<String, String>): List<NewsItem>
    suspend fun toggleBookmark(item: NewsItem)
    suspend fun translateText(text: String, targetLanguage: String): String
}
