package com.vci.vectorcamapp.core.data.upload.metadata

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.vci.vectorcamapp.R
import kotlinx.coroutines.delay

class MetadataUploadWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams
) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        createNotificationChannel()

        setForeground(createForegroundInfo())

        try {
            for (i in 1..TOTAL_DATAPOINTS) {
                // Simulate metadata upload
                delay(1000)
                updateNotification(i)
            }
        } catch (e: Exception) {
            Log.d("UploadWorker", "Error during upload: ${e.message}")
            return Result.retry()
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Session Metadata Upload in Progress")
            .setContentText("Uploading datapoint 0 of $TOTAL_DATAPOINTS")
            .setProgress(TOTAL_DATAPOINTS, 0, false)
            .setSmallIcon(R.drawable.ic_upload)
            .setOngoing(true).build()

        return ForegroundInfo(
            NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun updateNotification(counter: Int) {
        val updatedNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Session Metadata Upload in Progress")
            .setContentText("Uploading datapoint $counter of $TOTAL_DATAPOINTS")
            .setProgress(TOTAL_DATAPOINTS, counter, false)
            .setSmallIcon(R.drawable.ic_upload)
            .setOngoing(true).build()

        notificationManager.notify(NOTIFICATION_ID, updatedNotification)
    }

    companion object {
        const val CHANNEL_ID = "metadata_upload_channel"
        const val CHANNEL_NAME = "Metadata Upload Channel"
        const val NOTIFICATION_ID = 1002
        const val TOTAL_DATAPOINTS = 20
    }
}