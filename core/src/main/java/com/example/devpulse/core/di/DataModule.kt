package com.example.devpulse.core.di

import android.content.Context
import android.content.SharedPreferences
import com.example.devpulse.core.AppDatabase
import com.example.devpulse.core.BookmarkDao
import com.example.devpulse.core.KeywordDao
import com.example.devpulse.core.RssSourceDao
import com.example.devpulse.core.NewsApiService
import com.example.devpulse.core.NewsRepository
import com.example.devpulse.core.NewsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    fun provideKeywordDao(database: AppDatabase): KeywordDao {
        return database.keywordDao()
    }

    @Provides
    fun provideRssSourceDao(database: AppDatabase): RssSourceDao {
        return database.rssSourceDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("devpulse_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideNewsApiService(): NewsApiService {
        @Suppress("DEPRECATION")
        return Retrofit.Builder()
            .baseUrl("https://android-developers.googleblog.com/")
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNewsRepository(
        apiService: NewsApiService,
        bookmarkDao: BookmarkDao,
        keywordDao: KeywordDao,
        rssSourceDao: RssSourceDao,
        sharedPreferences: SharedPreferences
    ): NewsRepository {
        return NewsRepositoryImpl(apiService, bookmarkDao, keywordDao, rssSourceDao, sharedPreferences)
    }
}
