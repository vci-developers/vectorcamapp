package com.vci.vectorcamapp.core.data.upload.metadata

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.vci.vectorcamapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MetadataUploadNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "metadata_upload_channel"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.upload_notification_channel_metadata),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun buildInitialMetadataForegroundInfo(notificationId: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.upload_notification_registering))
            .setSmallIcon(R.drawable.ic_cloud_upload).setOngoing(true).build()

        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    fun showSpecimenProgressNotification(
        notificationId: Int,
        specimenId: String,
        currentSpecimenIndex: Int,
        totalSpecimens: Int,
        currentImageIndex: Int,
        totalImagesForSpecimen: Int
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.upload_notification_uploading_metadata, specimenId))
            .setContentText(
                context.getString(
                    R.string.upload_notification_specimen_progress,
                    currentSpecimenIndex + 1,
                    totalSpecimens
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(
                        R.string.upload_notification_specimen_image_progress,
                        currentSpecimenIndex + 1,
                        totalSpecimens,
                        currentImageIndex + 1,
                        totalImagesForSpecimen
                    )
                )
            ).setSmallIcon(R.drawable.ic_cloud_upload)
            .setProgress(totalSpecimens, currentSpecimenIndex + 1, false).setOngoing(true).build()

        notificationManager.notify(notificationId, notification)
    }

    fun showUploadRetryNotification(notificationId: Int, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.upload_notification_retrying))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_error)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showUploadErrorNotification(notificationId: Int, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.upload_notification_failed))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_error)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
