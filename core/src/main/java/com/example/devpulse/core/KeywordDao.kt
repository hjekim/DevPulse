package com.example.devpulse.core

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.devpulse.model.Keyword
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordDao {
    @Query("SELECT * FROM keywords")
    fun getAllKeywords(): Flow<List<Keyword>>

    @Query("SELECT * FROM keywords")
    suspend fun getKeywordsSync(): List<Keyword>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyword(keyword: Keyword)

    @Delete
    suspend fun deleteKeyword(keyword: Keyword)
}
