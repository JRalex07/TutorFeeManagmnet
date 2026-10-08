package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
  private val periodFormat = SimpleDateFormat("yyyy-MM", Locale.US)
  private val displayPeriodFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
  private val shortPeriodFormat = SimpleDateFormat("MMM yyyy", Locale.US)
  private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
  private val receiptDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US)

  fun currentPeriod(): String = periodFormat.format(Date())

  fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

  fun currentDateString(): String = dateFormat.format(Date())

  fun formatDisplayPeriod(period: String): String {
    return try {
      val d = periodFormat.parse(period)
      if (d != null) displayPeriodFormat.format(d) else period
    } catch (_: Exception) {
      period
    }
  }

  fun formatShortPeriod(period: String): String {
    return try {
      val d = periodFormat.parse(period)
      if (d != null) shortPeriodFormat.format(d) else period
    } catch (_: Exception) {
      period
    }
  }

  fun formatDisplayDate(dateStr: String): String {
    return try {
      val d = dateFormat.parse(dateStr)
      if (d != null) displayDateFormat.format(d) else dateStr
    } catch (_: Exception) {
      dateStr
    }
  }

  fun formatReceiptDateTime(timestamp: Long): String {
    return receiptDateFormat.format(Date(timestamp))
  }

  fun isOverdue(dueDateStr: String): Boolean {
    return try {
      val dueDate = dateFormat.parse(dueDateStr) ?: return false
      val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.time
      dueDate.before(today)
    } catch (_: Exception) {
      false
    }
  }

  fun generateDueDate(period: String, preferredDay: Int): String {
    return try {
      val parts = period.split("-")
      val year = parts[0].toInt()
      val month = parts[1].toInt() - 1
      val cal = Calendar.getInstance()
      cal.set(Calendar.YEAR, year)
      cal.set(Calendar.MONTH, month)
      val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
      val safeDay = preferredDay.coerceIn(1, maxDay)
      cal.set(Calendar.DAY_OF_MONTH, safeDay)
      dateFormat.format(cal.time)
    } catch (_: Exception) {
      "$period-10"
    }
  }

  fun nextPeriod(period: String): String {
    return try {
      val parts = period.split("-")
      var year = parts[0].toInt()
      var month = parts[1].toInt() + 1
      if (month > 12) {
        month = 1
        year++
      }
      String.format(Locale.US, "%04d-%02d", year, month)
    } catch (_: Exception) {
      period
    }
  }

  fun previousPeriod(period: String): String {
    return try {
      val parts = period.split("-")
      var year = parts[0].toInt()
      var month = parts[1].toInt() - 1
      if (month < 1) {
        month = 12
        year--
      }
      String.format(Locale.US, "%04d-%02d", year, month)
    } catch (_: Exception) {
      period
    }
  }

  fun generatePeriodsList(count: Int = 12): List<String> {
    val result = mutableListOf<String>()
    var p = currentPeriod()
    // Go 3 months back
    repeat(3) {
      p = previousPeriod(p)
    }
    repeat(count) {
      result.add(p)
      p = nextPeriod(p)
    }
    return result
  }

  fun isToday(dateStr: String): Boolean {
    return dateStr == currentDateString()
  }

  fun isThisWeek(dateStr: String): Boolean {
    return try {
      val date = dateFormat.parse(dateStr) ?: return false
      val calToday = Calendar.getInstance()
      val calDate = Calendar.getInstance().apply { time = date }
      calToday.get(Calendar.WEEK_OF_YEAR) == calDate.get(Calendar.WEEK_OF_YEAR) &&
          calToday.get(Calendar.YEAR) == calDate.get(Calendar.YEAR)
    } catch (_: Exception) {
      false
    }
  }

  fun isThisMonth(dateStr: String): Boolean {
    return dateStr.startsWith(currentPeriod())
  }
}
