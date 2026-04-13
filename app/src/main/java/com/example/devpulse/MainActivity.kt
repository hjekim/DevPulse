package com.example.devpulse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.devpulse.model.Keyword
import com.example.devpulse.model.NewsItem
import com.example.devpulse.model.RssSource
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
                val rssSources by viewModel.rssSources.collectAsState()
                val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
                val selectedKeyword by viewModel.selectedKeyword.collectAsState()
                val isRefreshing by viewModel.isRefreshing.collectAsState()
                val translatedTitles by viewModel.translatedTitles.collectAsState()
                val translatingUrls by viewModel.translatingUrls.collectAsState()
                
                val context = LocalContext.current
                val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

                LaunchedEffect(Unit) {
                    intent?.getStringExtra("news_url")?.let { url ->
                        openCustomTab(context, url, primaryColor)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        Surface(
                            tonalElevation = 8.dp,
                            shadowElevation = 16.dp
                        ) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ) {
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
                        rssSources = rssSources,
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
                        onAddRssSource = { name, url -> viewModel.addRssSource(name, url) },
                        onDeleteRssSource = { viewModel.removeRssSource(it) },
                        modifier = Modifier.padding(innerPadding),
                        onNewsClick = { item ->
                            if (item.link.isNotEmpty()) {
                                openCustomTab(context, item.link, primaryColor)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun openCustomTab(context: android.content.Context, url: String, toolbarColor: Int) {
        val intent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarColor)
                    .build()
            )
            .setShowTitle(true)
            .build()
        intent.launchUrl(context, Uri.parse(url))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val url = intent?.getStringExtra("news_url")
        if (url != null) {
            openCustomTab(this, url, 0xFF6200EE.toInt()) 
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
    rssSources: List<RssSource>,
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
    onAddRssSource: (String, String) -> Unit,
    onDeleteRssSource: (RssSource) -> Unit,
    modifier: Modifier = Modifier,
    onNewsClick: (NewsItem) -> Unit = {}
) {
    var showKeywordDialog by remember { mutableStateOf(false) }
    var showRssDialog by remember { mutableStateOf(false) }
    
    val filterKeywords = remember(keywords) {
        (listOf("Compose", "Kotlin", "KMP", "Studio", "Performance") + keywords.map { it.word }).distinct()
    }

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

    if (showRssDialog) {
        RssSourceSettingsDialog(
            currentSources = rssSources,
            onAdd = onAddRssSource,
            onDelete = onDeleteRssSource,
            onDismiss = { showRssDialog = false }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                HeaderSection(
                    title = title,
                    showActions = showFilters,
                    isNotificationEnabled = isNotificationEnabled,
                    onRssClick = { showRssDialog = true },
                    onNotificationClick = { showKeywordDialog = true }
                )
                
                if (showFilters) {
                    FilterSection(
                        selectedKeyword = selectedKeyword,
                        filterKeywords = filterKeywords,
                        onKeywordSelected = onKeywordSelected
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            if (newsItems.isEmpty() && !isRefreshing) {
                item {
                    EmptyState()
                }
            }

            items(newsItems, key = { it.link }) { item ->
                NewsCard(
                    item = item,
                    translatedTitle = translatedTitles[item.link],
                    isTranslating = translatingUrls.contains(item.link),
                    onClick = { onNewsClick(item) },
                    onBookmarkClick = { onBookmarkClick(item) },
                    onTranslateClick = { onTranslateClick(item) }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
    title: String,
    showActions: Boolean,
    isNotificationEnabled: Boolean,
    onRssClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (showActions) {
                Row {
                    IconButton(onClick = onRssClick) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "RSS Sources",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            imageVector = if (isNotificationEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications, 
                            contentDescription = "Keyword Alerts",
                            tint = if (isNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(
            text = "Stay updated with Android & Kotlin news",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun FilterSection(
    selectedKeyword: String?,
    filterKeywords: List<String>,
    onKeywordSelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        item {
            FilterChip(
                selected = selectedKeyword == null,
                onClick = { onKeywordSelected(null) },
                label = { Text("All Feed") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = null,
                shape = RoundedCornerShape(12.dp)
            )
        }
        items(filterKeywords) { keyword ->
            FilterChip(
                selected = selectedKeyword == keyword,
                onClick = { onKeywordSelected(keyword) },
                label = { Text(keyword) },
                shape = RoundedCornerShape(12.dp),
                border = null
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No updates found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Check your RSS sources or try refreshing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.source,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (item.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (item.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = translatedTitle ?: item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " ${item.readingTimeMin}m read",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ${item.pubDate.split(" ").take(3).joinToString(" ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (translatedTitle == null) {
                    if (isTranslating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp), 
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Translating", 
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Surface(
                            onClick = onTranslateClick,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Translate",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RssSourceSettingsDialog(
    currentSources: List<RssSource>,
    onAdd: (String, String) -> Unit,
    onDelete: (RssSource) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Manage Sources", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Source Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    label = { Text("RSS URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newUrl.isNotBlank()) {
                            onAdd(newName, newUrl)
                            newName = ""
                            newUrl = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add")
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Registered", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(currentSources) { source ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(source.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(source.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onDelete(source) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
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
        shape = RoundedCornerShape(28.dp),
        title = { Text("Alert Keywords", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Notifications", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Switch(checked = isNotificationEnabled, onCheckedChange = onToggleNotification)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add keyword...") },
                        singleLine = true,
                        enabled = isNotificationEnabled,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (newKeyword.isNotBlank()) { onAdd(newKeyword); newKeyword = "" } },
                        enabled = isNotificationEnabled,
                        modifier = Modifier.background(
                            if(isNotificationEnabled) MaterialTheme.colorScheme.primary else Color.LightGray, 
                            CircleShape
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("My Keywords", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(currentKeywords) { keyword ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(keyword.word, style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { onDelete(keyword) }, enabled = isNotificationEnabled) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
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
