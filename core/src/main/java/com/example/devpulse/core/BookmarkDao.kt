package com.example.devpulse.core

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.devpulse.model.NewsItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks")
    fun getAllBookmarks(): Flow<List<NewsItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(item: NewsItem)

    @Delete
    suspend fun deleteBookmark(item: NewsItem)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE link = :link)")
    fun isBookmarked(link: String): Flow<Boolean>
}
