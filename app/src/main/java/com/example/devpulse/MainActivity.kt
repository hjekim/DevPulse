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
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
                        Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    label = { Text("피드") },
                                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    label = { Text("북마크") },
                                    icon = { Icon(Icons.Default.Bookmark, null) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NewsListScreen(
                        title = if (selectedTab == 0) "DevPulse" else "Saved Stories",
                        newsItems = if (selectedTab == 0) newsItems else bookmarks,
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
            .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(toolbarColor).build())
            .setShowTitle(true)
            .build()
        intent.launchUrl(context, Uri.parse(url))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra("news_url")?.let { url ->
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
        keywords.map { it.word }.distinct()
    }

    if (showKeywordDialog) {
        KeywordSettingsDialog(keywords, isNotificationEnabled, onAddKeyword, onDeleteKeyword, onToggleNotification, { showKeywordDialog = false })
    }
    if (showRssDialog) {
        RssSourceSettingsDialog(rssSources, onAddRssSource, onDeleteRssSource, { showRssDialog = false })
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                PremiumHeader(title, showFilters, isNotificationEnabled, { showRssDialog = true }, { showKeywordDialog = true })
                if (showFilters && filterKeywords.isNotEmpty()) {
                    FilterSection(selectedKeyword, filterKeywords, onKeywordSelected)
                }
            }
            
            if (isRefreshing && newsItems.isEmpty()) {
                items(5) { ShimmerNewsCard() }
            } else if (newsItems.isEmpty()) {
                item { EmptyState() }
            } else {
                items(newsItems, key = { it.link }) { item ->
                    PremiumNewsCard(
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
}

@Composable
fun PremiumHeader(title: String, showActions: Boolean, isNotificationEnabled: Boolean, onRssClick: () -> Unit, onNotificationClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.displaySmall, 
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                if (showActions) {
                    Row {
                        IconButton(onClick = onRssClick) { 
                            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                        IconButton(onClick = onNotificationClick) {
                            Icon(
                                if (isNotificationEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications, 
                                null, 
                                tint = if (isNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Text(
                text = "기술 트렌드를 한눈에 확인하세요", 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun PremiumNewsCard(
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column {
            AsyncImage(
                model = item.imageUrl ?: "https://images.unsplash.com/photo-1607705703571-c5a8695f18f6?q=80&w=1000",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.source.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = " ${item.readingTimeMin}분", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = translatedTitle ?: item.title,
                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (translatedTitle == null) {
                        if (isTranslating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("번역 중...", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Surface(
                                onClick = onTranslateClick,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "번역하기",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            imageVector = if (item.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, 
                            contentDescription = null, 
                            tint = if (item.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerNewsCard() {
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = ""
    )
    val shimmerColors = listOf(Color.LightGray.copy(alpha = 0.6f), Color.LightGray.copy(alpha = 0.2f), Color.LightGray.copy(alpha = 0.6f))
    val brush = Brush.linearGradient(colors = shimmerColors, start = Offset.Zero, end = Offset(translateAnim, translateAnim))

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(brush))
            Column(modifier = Modifier.padding(16.dp)) {
                Box(modifier = Modifier.width(80.dp).height(12.dp).background(brush))
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(brush))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.width(200.dp).height(24.dp).background(brush))
            }
        }
    }
}

@Composable
fun FilterSection(selectedKeyword: String?, filterKeywords: List<String>, onKeywordSelected: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp), 
        contentPadding = PaddingValues(horizontal = 20.dp), 
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        item {
            FilterChip(
                selected = selectedKeyword == null, 
                onClick = { onKeywordSelected(null) },
                label = { Text("전체 피드") }, 
                shape = RoundedCornerShape(14.dp), 
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary, 
                    selectedLabelColor = Color.White
                )
            )
        }
        items(filterKeywords) { keyword ->
            FilterChip(
                selected = selectedKeyword == keyword, 
                onClick = { onKeywordSelected(keyword) }, 
                label = { Text(keyword) }, 
                shape = RoundedCornerShape(14.dp), 
                border = null
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 100.dp), 
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Feed, 
            contentDescription = null, 
            modifier = Modifier.size(80.dp), 
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("오늘의 소식이 없습니다", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun RssSourceSettingsDialog(currentSources: List<RssSource>, onAdd: (String, String) -> Unit, onDelete: (RssSource) -> Unit, onDismiss: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("소스 관리", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                OutlinedTextField(newName, { newName = it }, label = { Text("이름") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(newUrl, { newUrl = it }, label = { Text("RSS 주소") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Button({ if (newName.isNotBlank() && newUrl.isNotBlank()) { onAdd(newName, newUrl); newName = ""; newUrl = "" } }, modifier = Modifier.align(Alignment.End).padding(top = 12.dp)) { Text("추가") }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Text("등록된 소스", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(currentSources) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) { Text(it.name, fontWeight = FontWeight.Bold); Text(it.url, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        IconButton({ onDelete(it) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    }}
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("완료") } }
    )
}

@Composable
fun KeywordSettingsDialog(keywords: List<Keyword>, enabled: Boolean, onAdd: (String) -> Unit, onDelete: (Keyword) -> Unit, onToggle: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var word by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 키워드", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("알림 활성화"); Switch(enabled, onToggle)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    OutlinedTextField(word, { word = it }, placeholder = { Text("키워드") }, modifier = Modifier.weight(1f), enabled = enabled, shape = RoundedCornerShape(12.dp))
                    IconButton({ if (word.isNotBlank()) { onAdd(word); word = "" } }, enabled = enabled) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Text("내 키워드", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(keywords) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(it.word, modifier = Modifier.padding(8.dp)); IconButton({ onDelete(it) }, enabled = enabled) { Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.error) }
                    }}
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("완료") } }
    )
}
