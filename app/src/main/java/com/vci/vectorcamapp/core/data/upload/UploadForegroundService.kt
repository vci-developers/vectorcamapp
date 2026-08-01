package com.vci.vectorcamapp.core.data.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.vci.vectorcamapp.MainActivity
import com.vci.vectorcamapp.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private enum class UploadState { WAITING, UPLOADING, IDLE }

/**
 * Sticky foreground service (START_STICKY) that anchors the app process during active uploads.
 *
 * WorkManager's SystemForegroundService only starts a foreground service once doWork() begins,
 * leaving a window where the process can be killed while work is still ENQUEUED. This service
 * fills that gap and also ensures Android restarts the process if the service is killed mid-upload.
 *
 * Lifecycle:
 *  - Started by WorkManagerRepository immediately before upload work is enqueued.
 *  - Observes WorkManager state and calls stopSelf() when no upload work remains active.
 *  - On system restart (START_STICKY), re-attaches to any in-flight WorkManager tasks.
 */
@AndroidEntryPoint
class UploadForegroundService : Service() {

    @Inject lateinit var workManager: WorkManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var observerJob: Job? = null
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        const val UPLOAD_WORK_TAG = "session_upload_work"

        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "upload_foreground_service_channel"
        private const val CHANNEL_NAME = "Upload Service"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, UploadForegroundService::class.java)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Preparing upload\u2026"), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        startObservingWork()
        return START_STICKY
    }

    /**
     * Observes WorkManager upload tags, updates the notification text to reflect the current
     * state, and stops the service once all upload work is settled.
     *
     * Uses a `seenActiveWork` flag so we do not stop prematurely if the initial observation
     * happens before WorkManager has registered the enqueued tasks, and to correctly handle
     * the START_STICKY restart path where some work may have already completed.
     */
    private fun startObservingWork() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            var seenActiveWork = false
            workManager.getWorkInfosByTagFlow(UPLOAD_WORK_TAG).collect { workInfos ->
                val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
                val isEnqueued = workInfos.any { it.state == WorkInfo.State.ENQUEUED }
                val hasActiveWork = isRunning || isEnqueued

                val state = when {
                    isRunning -> UploadState.UPLOADING
                    isEnqueued -> UploadState.WAITING
                    else -> UploadState.IDLE
                }

                if (hasActiveWork) {
                    seenActiveWork = true
                    updateNotification(state)
                } else if (seenActiveWork) {
                    stopSelf() // active → idle transition: all done
                } else if (workInfos.isNotEmpty()) {
                    stopSelf() // all work already completed/failed/cancelled
                }
                // workInfos.isEmpty() → work not yet enqueued, keep waiting
            }
        }
    }

    private fun updateNotification(state: UploadState) {
        val text = when (state) {
            UploadState.WAITING -> "Waiting for network connection\u2026"
            UploadState.UPLOADING -> "Upload is running in the background\u2026"
            UploadState.IDLE -> return
        }
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Uploading session data")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps data and image upload running in the background"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
