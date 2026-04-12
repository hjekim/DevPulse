package com.example.devpulse.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class NewsItem(
    @PrimaryKey val link: String,
    val title: String,
    val pubDate: String,
    val source: String,
    val isBookmarked: Boolean = false,
    val readingTimeMin: Int = 1 // 읽기 소요 시간 (분 단위) 추가
)
