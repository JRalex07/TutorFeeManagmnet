package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

  private const val TAG = "NotificationHelper"
  const val CHANNEL_ID = "fee_reminders_channel"
  private const val CHANNEL_NAME = "Fee Due & Overdue Reminders"
  private const val CHANNEL_DESC = "Notifications for student fee due alerts and sent reminders"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_DESC
        enableVibration(true)
      }
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      manager?.createNotificationChannel(channel)
    }
  }

  fun showReminderSentNotification(
    context: Context,
    studentName: String,
    amount: String,
    period: String,
    channel: String = "WhatsApp / SMS"
  ) {
    createNotificationChannel(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ActivityCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        Log.d(TAG, "Notification permission not granted, skipping system notification.")
        return
      }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      System.currentTimeMillis().toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Fee Reminder Dispatched")
      .setContentText("Reminder for $studentName ($amount for $period) sent via $channel.")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("Fee follow-up reminder for $studentName ($amount for $period) was successfully initiated via $channel. The student record has been updated.")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(studentName.hashCode(), notification)
    } catch (e: Exception) {
      Log.e(TAG, "Error posting notification", e)
    }
  }

  fun showOverdueAlertNotification(
    context: Context,
    studentName: String,
    amount: String,
    daysOverdue: Long
  ) {
    createNotificationChannel(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ActivityCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      System.currentTimeMillis().toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("⚠️ Overdue Tuition Alert: $studentName")
      .setContentText("$studentName is $daysOverdue days overdue for $amount.")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("$studentName has an outstanding tuition balance of $amount that is $daysOverdue days past the scheduled due date. Tap to open and collect or send a reminder.")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(studentName.hashCode() + 100, notification)
    } catch (e: Exception) {
      Log.e(TAG, "Error posting alert notification", e)
    }
  }
}
