package com.example.devpulse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.devpulse.core.NewsRepository
import com.example.devpulse.model.Keyword
import com.example.devpulse.model.NewsItem
import com.example.devpulse.model.RssSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _allNews = MutableStateFlow<List<NewsItem>>(emptyList())
    private val _selectedKeyword = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    
    private val _isNotificationEnabled = MutableStateFlow(repository.isNotificationEnabled())
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled
    
    private val _translatedTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val translatedTitles: StateFlow<Map<String, String>> = _translatedTitles

    private val _translatingUrls = MutableStateFlow<Set<String>>(emptySet())
    val translatingUrls: StateFlow<Set<String>> = _translatingUrls

    val bookmarks: StateFlow<List<NewsItem>> = repository.getBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val keywords: StateFlow<List<Keyword>> = repository.getAllKeywords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rssSources: StateFlow<List<RssSource>> = repository.getAllRssSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newsItems: StateFlow<List<NewsItem>> = combine(_allNews, _selectedKeyword, bookmarks) { all, keyword, bookmarkList ->
        val filtered = if (keyword == null) all else all.filter { it.title.contains(keyword, ignoreCase = true) }
        filtered.map { item ->
            item.copy(isBookmarked = bookmarkList.any { it.link == item.link })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedKeyword: StateFlow<String?> = _selectedKeyword
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            initializeDefaultDataIfNeeded()
            fetchNews()
            if (_isNotificationEnabled.value) {
                scheduleNewsCheck()
            }
        }
    }

    private suspend fun initializeDefaultDataIfNeeded() {
        if (!repository.isDefaultsInitialized()) {
            val defaultRss = listOf(
                RssSource("https://toss.tech/rss.xml", "Toss"),
                RssSource("https://android-developers.googleblog.com/feeds/posts/default?alt=rss", "Android Developers"),
                RssSource("https://medium.com/feed/androiddevelopers", "Medium (Android)"),
                RssSource("https://blog.jetbrains.com/kotlin/feed/", "Kotlin Blog")
            )
            defaultRss.forEach { repository.insertRssSource(it) }

            val defaultKeywords = listOf("Compose", "Kotlin", "KMP", "Studio", "Performance")
            defaultKeywords.forEach { repository.insertKeyword(Keyword(it)) }

            repository.setDefaultsInitialized(true)
        }
    }

    private fun scheduleNewsCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NewsWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "NewsCheckWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun cancelNewsCheck() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("NewsCheckWork")
    }

    fun toggleNotification(enabled: Boolean) {
        _isNotificationEnabled.value = enabled
        repository.setNotificationEnabled(enabled)
        if (enabled) scheduleNewsCheck() else cancelNewsCheck()
    }

    fun addKeyword(word: String) {
        viewModelScope.launch { repository.insertKeyword(Keyword(word)) }
    }

    fun removeKeyword(keyword: Keyword) {
        viewModelScope.launch { repository.deleteKeyword(keyword) }
    }

    fun addRssSource(name: String, url: String) {
        viewModelScope.launch { 
            repository.insertRssSource(RssSource(url, name))
            fetchNews()
        }
    }

    fun removeRssSource(source: RssSource) {
        viewModelScope.launch { 
            repository.deleteRssSource(source)
            fetchNews()
        }
    }

    fun setKeyword(keyword: String?) {
        _selectedKeyword.value = keyword
    }

    fun toggleBookmark(item: NewsItem) {
        viewModelScope.launch { repository.toggleBookmark(item) }
    }

    fun fetchNews() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val dbSources = repository.getAllRssSources().first()
                if (dbSources.isNotEmpty()) {
                    val sourcesMap = dbSources.associate { it.name to it.url }
                    val fetchedNews = repository.fetchNewsFromSources(sourcesMap)
                    _allNews.value = fetchedNews
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun translateTitle(item: NewsItem) {
        if (_translatedTitles.value.containsKey(item.link) || _translatingUrls.value.contains(item.link)) return
        viewModelScope.launch {
            _translatingUrls.value = _translatingUrls.value + item.link
            try {
                val translated = repository.translateText(item.title, "Korean")
                _translatedTitles.value = _translatedTitles.value + (item.link to translated)
            } finally {
                _translatingUrls.value = _translatingUrls.value - item.link
            }
        }
    }
}
