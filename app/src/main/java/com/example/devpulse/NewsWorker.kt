package com.example.devpulse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.devpulse.core.NewsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class NewsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: NewsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val keywords = try {
            repository.getAllKeywords().first()
        } catch (e: Exception) {
            emptyList()
        }
        
        if (keywords.isEmpty()) return Result.success()

        val sources = mapOf(
            "Android Developers" to "https://android-developers.googleblog.com/feeds/posts/default?alt=rss",
            "Medium (Android)" to "https://medium.com/feed/androiddevelopers",
            "Kotlin Blog" to "https://blog.jetbrains.com/kotlin/feed/"
        )

        try {
            val news = repository.fetchNewsFromSources(sources)
            val matchedNews = news.filter { item ->
                keywords.any { keyword -> item.title.contains(keyword.word, ignoreCase = true) }
            }

            if (matchedNews.isNotEmpty()) {
                sendNotification(matchedNews.size, matchedNews.first().link)
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    private fun sendNotification(count: Int, url: String) {
        val channelId = "devpulse_news"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "News Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("news_url", url)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DevPulse New Articles")
            .setContentText("${count}개의 관심 키워드 소식이 올라왔습니다!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
