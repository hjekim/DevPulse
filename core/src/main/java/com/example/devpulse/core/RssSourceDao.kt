package com.example.devpulse.core

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.devpulse.model.RssSource
import kotlinx.coroutines.flow.Flow

@Dao
interface RssSourceDao {
    @Query("SELECT * FROM rss_sources")
    fun getAllSources(): Flow<List<RssSource>>

    @Query("SELECT * FROM rss_sources")
    suspend fun getAllSourcesSync(): List<RssSource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: RssSource)

    @Delete
    suspend fun deleteSource(source: RssSource)
}
