package com.example.devpulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devpulse.core.NewsRepository
import com.example.devpulse.model.NewsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    private val sources = mapOf(
        "Android Developers" to "https://android-developers.googleblog.com/feeds/posts/default?alt=rss",
        "Medium (Android)" to "https://medium.com/feed/androiddevelopers",
        "Kotlin Blog" to "https://blog.jetbrains.com/kotlin/feed/"
    )

    private val _allNews = MutableStateFlow<List<NewsItem>>(emptyList())
    private val _selectedKeyword = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    
    // 번역된 제목들을 저장하는 Map (URL을 키로 사용)
    private val _translatedTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val translatedTitles: StateFlow<Map<String, String>> = _translatedTitles

    val bookmarks: StateFlow<List<NewsItem>> = repository.getBookmarks()
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
        fetchNews()
    }

    fun setKeyword(keyword: String?) {
        _selectedKeyword.value = keyword
    }

    fun toggleBookmark(item: NewsItem) {
        viewModelScope.launch {
            repository.toggleBookmark(item)
        }
    }

    fun fetchNews() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val fetchedNews = repository.fetchNewsFromSources(sources)
                _allNews.value = fetchedNews
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun translateTitle(item: NewsItem) {
        if (_translatedTitles.value.containsKey(item.link)) return // 이미 번역된 경우 패스

        viewModelScope.launch {
            val translated = repository.translateText(item.title, "Korean")
            _translatedTitles.value = _translatedTitles.value + (item.link to translated)
        }
    }
}
