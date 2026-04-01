package com.example.devpulse.core

import com.example.devpulse.model.NewsItem
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val apiService: NewsApiService,
    private val bookmarkDao: BookmarkDao
) : NewsRepository {

    // BuildConfig에서 API 키를 가져오기 위해 주입받거나 직접 참조 (여기서는 단순화를 위해 직접 참조 방식 구조 제안)
    // 실제로는 별도의 Config 클래스로 관리하는 것이 좋습니다.
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = com.example.devpulse.core.BuildConfig.GEMINI_API_KEY
        )
    }

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
                            source = sourceName
                        )
                    } ?: emptyList()
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        }.awaitAll().flatten().sortedByDescending { it.pubDate }
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
            val response = generativeModel.generateContent("Translate this technical news title to $targetLanguage: $text")
            response.text ?: text
        } catch (e: Exception) {
            e.printStackTrace()
            text
        }
    }
}
