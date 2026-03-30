package com.example.devpulse.core

import com.example.devpulse.model.NewsItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val apiService: NewsApiService,
    private val bookmarkDao: BookmarkDao
) : NewsRepository {

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
}
