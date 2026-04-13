package com.example.devpulse.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss_sources")
data class RssSource(
    @PrimaryKey val url: String,
    val name: String
)
