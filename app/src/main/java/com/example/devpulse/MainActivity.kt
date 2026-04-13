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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.devpulse.model.Keyword
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
                val keywords by viewModel.keywords.collectAsState()
                val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
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
                        keywords = keywords,
                        isNotificationEnabled = isNotificationEnabled,
                        showFilters = selectedTab == 0,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.fetchNews() },
                        onKeywordSelected = { viewModel.setKeyword(it) },
                        onBookmarkClick = { viewModel.toggleBookmark(it) },
                        onTranslateClick = { viewModel.translateTitle(it) },
                        onAddKeyword = { viewModel.addKeyword(it) },
                        onDeleteKeyword = { viewModel.removeKeyword(it) },
                        onToggleNotification = { viewModel.toggleNotification(it) },
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
    keywords: List<Keyword>,
    isNotificationEnabled: Boolean,
    showFilters: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onKeywordSelected: (String?) -> Unit,
    onBookmarkClick: (NewsItem) -> Unit,
    onTranslateClick: (NewsItem) -> Unit,
    onAddKeyword: (String) -> Unit,
    onDeleteKeyword: (Keyword) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onNewsClick: (NewsItem) -> Unit = {}
) {
    var showKeywordDialog by remember { mutableStateOf(false) }
    val filterKeywords = listOf("Compose", "Kotlin", "KMP", "Studio", "Performance")

    if (showKeywordDialog) {
        KeywordSettingsDialog(
            currentKeywords = keywords,
            isNotificationEnabled = isNotificationEnabled,
            onAdd = onAddKeyword,
            onDelete = onDeleteKeyword,
            onToggleNotification = onToggleNotification,
            onDismiss = { showKeywordDialog = false }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (showFilters) {
                        IconButton(onClick = { showKeywordDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Notifications, 
                                contentDescription = "Keyword Alerts",
                                tint = if (isNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                if (showFilters) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedKeyword == null,
                                onClick = { onKeywordSelected(null) },
                                label = { Text("All") }
                            )
                        }
                        items(filterKeywords) { keyword ->
                            FilterChip(
                                selected = selectedKeyword == keyword,
                                onClick = { onKeywordSelected(keyword) },
                                label = { Text(keyword) }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
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
fun KeywordSettingsDialog(
    currentKeywords: List<Keyword>,
    isNotificationEnabled: Boolean,
    onAdd: (String) -> Unit,
    onDelete: (Keyword) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var newKeyword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keyword Alerts Settings") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Notifications", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = onToggleNotification
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("알림을 받을 키워드를 등록하세요.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("New keyword") },
                        singleLine = true,
                        enabled = isNotificationEnabled
                    )
                    IconButton(
                        onClick = {
                            if (newKeyword.isNotBlank()) {
                                onAdd(newKeyword)
                                newKeyword = ""
                            }
                        },
                        enabled = isNotificationEnabled
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Registered Keywords:", style = MaterialTheme.typography.labelLarge)
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(currentKeywords) { keyword ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(keyword.word, color = if (isNotificationEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(
                                onClick = { onDelete(keyword) },
                                enabled = isNotificationEnabled
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if (isNotificationEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
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
                            ActionText("번역하기", onTranslateClick)
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
                        modifier = Modifier.padding(top = 4.dp)
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

@Composable
fun ActionText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    )
}
