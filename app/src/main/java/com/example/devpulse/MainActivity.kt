package com.example.devpulse

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.devpulse.model.NewsItem
import com.example.devpulse.ui.theme.DevPulseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevPulseTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                
                val newsItems by viewModel.newsItems.collectAsState()
                val bookmarks by viewModel.bookmarks.collectAsState()
                val selectedKeyword by viewModel.selectedKeyword.collectAsState()
                val isRefreshing by viewModel.isRefreshing.collectAsState()
                val translatedTitles by viewModel.translatedTitles.collectAsState()
                val translatingUrls by viewModel.translatingUrls.collectAsState()
                
                val context = LocalContext.current
                val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { Text("News") },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "News") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { Text("Bookmarks") },
                                icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks") }
                            )
                        }
                    }
                ) { innerPadding ->
                    val displayItems = if (selectedTab == 0) newsItems else bookmarks
                    
                    NewsListScreen(
                        title = if (selectedTab == 0) "DevPulse" else "My Bookmarks",
                        newsItems = displayItems,
                        translatedTitles = translatedTitles,
                        translatingUrls = translatingUrls,
                        selectedKeyword = if (selectedTab == 0) selectedKeyword else null,
                        showFilters = selectedTab == 0,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.fetchNews() },
                        onKeywordSelected = { viewModel.setKeyword(it) },
                        onBookmarkClick = { viewModel.toggleBookmark(it) },
                        onTranslateClick = { viewModel.translateTitle(it) },
                        modifier = Modifier.padding(innerPadding),
                        onNewsClick = { item ->
                            if (item.link.isNotEmpty()) {
                                val intent = CustomTabsIntent.Builder()
                                    .setDefaultColorSchemeParams(
                                        CustomTabColorSchemeParams.Builder()
                                            .setToolbarColor(primaryColor)
                                            .build()
                                    )
                                    .setShowTitle(true)
                                    .build()
                                intent.launchUrl(context, Uri.parse(item.link))
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(
    title: String,
    newsItems: List<NewsItem>,
    translatedTitles: Map<String, String>,
    translatingUrls: Set<String>,
    selectedKeyword: String?,
    showFilters: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onKeywordSelected: (String?) -> Unit,
    onBookmarkClick: (NewsItem) -> Unit,
    onTranslateClick: (NewsItem) -> Unit,
    modifier: Modifier = Modifier,
    onNewsClick: (NewsItem) -> Unit = {}
) {
    val keywords = listOf("Compose", "Kotlin", "KMP", "Studio", "Performance")

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (showFilters) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedKeyword == null,
                                onClick = { onKeywordSelected(null) },
                                label = { Text("All") }
                            )
                        }
                        items(keywords) { keyword ->
                            FilterChip(
                                selected = selectedKeyword == keyword,
                                onClick = { onKeywordSelected(keyword) },
                                label = { Text(keyword) }
                            )
                        }
                    }
                }
            }
            
            if (newsItems.isEmpty() && !isRefreshing) {
                item {
                    Text(
                        text = "No items found.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 32.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(newsItems) { item ->
                NewsCard(
                    item = item,
                    translatedTitle = translatedTitles[item.link],
                    isTranslating = translatingUrls.contains(item.link),
                    onClick = { onNewsClick(item) },
                    onBookmarkClick = { onBookmarkClick(item) },
                    onTranslateClick = { onTranslateClick(item) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun NewsCard(
    item: NewsItem,
    translatedTitle: String?,
    isTranslating: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onTranslateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = translatedTitle ?: item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (translatedTitle == null) {
                        if (isTranslating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                Text(" 번역 중...", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Text(
                                text = "번역하기",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clickable { onTranslateClick() }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.pubDate,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.readingTimeMin}분 소요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = if (item.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (item.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
