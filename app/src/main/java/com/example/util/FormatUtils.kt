package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Payment
import com.example.data.model.TuitionProfile
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
  private val currencyFormatter = DecimalFormat("#,##,##0.##")

  fun formatCurrency(amount: Double, symbol: String = "₹"): String {
    val formatted = currencyFormatter.format(amount)
    return "$symbol$formatted"
  }

  fun numberToWords(amount: Double): String {
    val rounded = amount.toLong()
    if (rounded == 0L) return "Zero Rupees Only"

    val units = arrayOf(
      "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
      "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
      "Seventeen", "Eighteen", "Nineteen"
    )
    val tens = arrayOf(
      "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun convertLessThanOneThousand(n: Int): String {
      var current = ""
      var num = n
      if (num % 100 < 20) {
        current = units[num % 100]
        num /= 100
      } else {
        current = units[num % 10]
        num /= 10
        current = tens[num % 10] + if (current.isNotEmpty()) " $current" else ""
        num /= 10
      }
      if (num == 0) return current
      return units[num] + " Hundred" + if (current.isNotEmpty()) " and $current" else ""
    }

    var num = rounded
    val crore = (num / 10000000).toInt()
    num %= 10000000
    val lakh = (num / 100000).toInt()
    num %= 100000
    val thousand = (num / 1000).toInt()
    num %= 1000
    val hundred = num.toInt()

    val result = StringBuilder()
    if (crore > 0) result.append(convertLessThanOneThousand(crore)).append(" Crore ")
    if (lakh > 0) result.append(convertLessThanOneThousand(lakh)).append(" Lakh ")
    if (thousand > 0) result.append(convertLessThanOneThousand(thousand)).append(" Thousand ")
    if (hundred > 0) result.append(convertLessThanOneThousand(hundred))

    return result.toString().trim() + " Rupees Only"
  }

  fun generateReceiptText(payment: Payment, profile: TuitionProfile): String {
    val periods = if (payment.allocatedFeePeriods.isEmpty()) "General Arrears / Advance"
    else payment.allocatedFeePeriods.joinToString(", ") { DateUtils.formatShortPeriod(it) }

    return """
      ========================================
             ${profile.tuitionName.uppercase()}
             TUITION FEE PAYMENT RECEIPT
      ========================================
      Receipt No : ${payment.receiptNumber}
      Date       : ${DateUtils.formatDisplayDate(payment.paymentDate)}
      Teacher    : ${profile.teacherName}
      Phone      : ${profile.phone}
      Address    : ${profile.address}
      ----------------------------------------
      STUDENT DETAILS:
      Student    : ${payment.studentName}
      Class      : ${payment.studentClass}
      Student ID : ${payment.studentId}
      ----------------------------------------
      FEE DETAILS:
      Period(s)  : $periods
      Amount Paid: ${formatCurrency(payment.amount, profile.currencySymbol)}
      Amount Words: ${numberToWords(payment.amount)}
      Payment Mode: ${payment.paymentMethod.label}
      Ref / Txn  : ${if (payment.transactionReference.isBlank()) "N/A" else payment.transactionReference}
      Status     : ${payment.status.label}
      ----------------------------------------
      ${if (payment.notes.isNotBlank()) "Notes: ${payment.notes}\n----------------------------------------" else ""}
      ${profile.receiptFooterNote}
      ========================================
    """.trimIndent()
  }

  fun shareReceipt(context: Context, payment: Payment, profile: TuitionProfile) {
    val shareText = generateReceiptText(payment, profile)
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, shareText)
      putExtra(Intent.EXTRA_SUBJECT, "Tuition Fee Receipt - ${payment.receiptNumber} - ${payment.studentName}")
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Receipt via")
    context.startActivity(shareIntent)
  }
}
