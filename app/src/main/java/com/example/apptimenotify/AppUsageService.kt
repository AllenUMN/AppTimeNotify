package com.example.apptimenotify

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.*

class AppUsageService : Service() {
    private val CHANNEL_ID = "AppUsageTrackingChannel"
    private val NOTIFICATION_ID = 1
    private val LIMIT_NOTIFICATION_ID = 2
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private var targetPackage: String? = null
    private var targetAppName: String? = null
    private var limitMillis: Long = 0
    private var isNotified = false
    private var lastResetDay = -1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateForegroundNotification(0)
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        serviceScope.launch {
            UsagePrefs.getAppLimit(this@AppUsageService).collect { limit ->
                if (limit != null) {
                    targetPackage = limit.packageName
                    targetAppName = limit.appName
                    limitMillis = (limit.hours * 3600 + limit.minutes * 60) * 1000L
                    isNotified = false
                    
                    while (isActive) {
                        checkUsage()
                        delay(5000) // Check every 5 seconds
                    }
                } else {
                    stopSelf()
                }
            }
        }
    }

    private fun checkUsage() {
        val packageName = targetPackage ?: return

        // Daily reset logic
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (lastResetDay != -1 && lastResetDay != today) {
            isNotified = false
            Log.d("AppUsageService", "New day detected. Resetting notification status.")
        }
        lastResetDay = today

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var totalTime: Long = 0
        var lastResumedTime: Long = 0

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.packageName == packageName) {
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        lastResumedTime = event.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                        if (lastResumedTime != 0L) {
                            totalTime += event.timeStamp - lastResumedTime
                            lastResumedTime = 0
                        }
                    }
                }
            }
        }

        // If currently in foreground
        if (lastResumedTime != 0L) {
            totalTime += endTime - lastResumedTime
        }

        Log.d("AppUsageService", "Total time for $packageName: ${totalTime / 1000}s / ${limitMillis / 1000}s")

        updateForegroundNotification(totalTime)

        if (totalTime >= limitMillis && !isNotified) {
            sendLimitNotification(packageName)
            isNotified = true
        } else if (totalTime < limitMillis) {
            isNotified = false // Reset if they are under limit (maybe new day or limit changed)
        }
    }

    private fun updateForegroundNotification(totalTime: Long) {
        val appName = targetAppName ?: "Unknown App"
        val usedSec = totalTime / 1000
        val usedH = usedSec / 3600
        val usedM = (usedSec % 3600) / 60
        val usedS = usedSec % 60

        val limitSec = limitMillis / 1000
        val limitH = limitSec / 3600
        val limitM = (limitSec % 3600) / 60

        val contentText = String.format(
            Locale.getDefault(),
            "Target: %s | Used: %02dh %02dm %02ds | Limit: %02dh %02dm",
            appName, usedH, usedM, usedS, limitH, limitM
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Usage Monitoring")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Don't buzz on every update
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun sendLimitNotification(packageName: String) {
        val appName = targetAppName ?: packageName
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Time's Up!")
            .setContentText("You have reached your daily limit for $appName.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(LIMIT_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "App Usage Tracking Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows app usage stats and limit alerts"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
