package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object ReminderUtils {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

  fun buildReminderMessage(
    studentName: String,
    parentContact: String,
    period: String,
    amount: String,
    dueDate: String,
    tuitionName: String,
    teacherName: String,
    upiId: String
  ): String {
    val cleanTuition = tuitionName.ifBlank { "Tuition Classes" }
    val displayPeriod = DateUtils.formatDisplayPeriod(period)
    val displayDueDate = if (dueDate.isNotBlank()) DateUtils.formatDisplayDate(dueDate) else "the due date"
    
    val paymentInstruction = if (upiId.isNotBlank()) {
      "You may conveniently pay via UPI to: $upiId or in cash."
    } else {
      "Kindly clear the fee at your earliest convenience."
    }

    return "Dear Parent,\n\n" +
        "This is a gentle reminder from $cleanTuition regarding $studentName's tuition fee for $displayPeriod.\n\n" +
        "• Student: $studentName\n" +
        "• Period: $displayPeriod\n" +
        "• Outstanding Fee: $amount\n" +
        "• Due Date: $displayDueDate\n\n" +
        "$paymentInstruction\n\n" +
        "If you have already completed the payment, please disregard this note.\n\n" +
        "Warm regards,\n" +
        "${teacherName.ifBlank { cleanTuition }}"
  }

  fun sendViaWhatsApp(context: Context, phoneNumber: String, message: String): Boolean {
    val cleanNumber = sanitizePhoneNumber(phoneNumber)
    return try {
      val encodedText = URLEncoder.encode(message, "UTF-8")
      val uri = if (cleanNumber.isNotBlank()) {
        Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedText")
      } else {
        Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
      }
      val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
      true
    } catch (e: Exception) {
      // Fallback to general intent
      try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, message)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(sendIntent, "Send Reminder via").apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        true
      } catch (ex: Exception) {
        Toast.makeText(context, "Could not open messaging app.", Toast.LENGTH_SHORT).show()
        false
      }
    }
  }

  fun sendViaSms(context: Context, phoneNumber: String, message: String): Boolean {
    val cleanNumber = sanitizePhoneNumber(phoneNumber)
    return try {
      val uri = Uri.parse("smsto:$cleanNumber")
      val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra("sms_body", message)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
      true
    } catch (e: Exception) {
      shareReminder(context, message)
    }
  }

  fun shareReminder(context: Context, message: String): Boolean {
    return try {
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(Intent.createChooser(intent, "Share Fee Reminder").apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      })
      true
    } catch (e: Exception) {
      Toast.makeText(context, "Unable to share message.", Toast.LENGTH_SHORT).show()
      false
    }
  }

  private fun sanitizePhoneNumber(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return if (digits.length == 10) {
      // Assume Indian country code 91 if 10 digits
      "91$digits"
    } else {
      digits
    }
  }

  fun calculateDaysOverdue(dueDateStr: String): Long {
    return try {
      val due = dateFormat.parse(dueDateStr) ?: return 0L
      val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.time
      val diff = now.time - due.time
      if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) else 0L
    } catch (_: Exception) {
      0L
    }
  }

  fun formatRelativeTime(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 0) return "Just now"

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
      minutes < 2 -> "Just now"
      minutes < 60 -> "${minutes}m ago"
      hours < 24 -> "${hours}h ago"
      days == 1L -> "Yesterday"
      days < 7 -> "${days}d ago"
      else -> {
        val sdf = SimpleDateFormat("dd MMM", Locale.US)
        sdf.format(Date(timestamp))
      }
    }
  }
}
