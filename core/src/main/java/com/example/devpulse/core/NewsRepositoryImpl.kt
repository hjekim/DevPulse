package com.example.devpulse.core

import android.util.Log
import com.example.devpulse.model.NewsItem
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val apiService: NewsApiService,
    private val bookmarkDao: BookmarkDao
) : NewsRepository {

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.KOREAN)
        .build()
    
    private val translator = Translation.getClient(options)

    override fun getBookmarks(): Flow<List<NewsItem>> {
        return bookmarkDao.getAllBookmarks()
    }

    override suspend fun fetchNewsFromSources(sources: Map<String, String>): List<NewsItem> = coroutineScope {
        sources.map { (sourceName, url) ->
            async {
                try {
                    val response = apiService.getNews(url)
                    response.channel?.items?.map { rssItem ->
                        NewsItem(
                            title = rssItem.title ?: "No Title",
                            link = rssItem.link ?: "",
                            pubDate = rssItem.pubDate ?: "",
                            source = sourceName,
                            readingTimeMin = calculateReadingTime(rssItem.description)
                        )
                    } ?: emptyList()
                } catch (e: Exception) {
                    Log.e("NewsRepository", "Error fetching news from $sourceName", e)
                    emptyList()
                }
            }
        }.awaitAll().flatten().sortedByDescending { it.pubDate }
    }

    // 읽기 소요 시간 계산 로직
    private fun calculateReadingTime(description: String?): Int {
        if (description.isNullOrBlank()) return 1
        val words = description.split("\\s+".toRegex()).size
        val time = Math.ceil(words / 200.0).toInt()
        return if (time < 1) 1 else time
    }

    override suspend fun toggleBookmark(item: NewsItem) {
        if (item.isBookmarked) {
            bookmarkDao.deleteBookmark(item)
        } else {
            bookmarkDao.insertBookmark(item.copy(isBookmarked = true))
        }
    }

    override suspend fun translateText(text: String, targetLanguage: String): String {
        return try {
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
            val translatedText = translator.translate(text).await()
            Log.d("NewsRepository", "ML Kit Translated: $translatedText")
            translatedText
        } catch (e: Exception) {
            Log.e("NewsRepository", "ML Kit Translation failed", e)
            text 
        }
    }
}
