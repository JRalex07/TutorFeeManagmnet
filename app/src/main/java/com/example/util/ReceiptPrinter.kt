package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.Payment
import com.example.data.model.TuitionProfile

object ReceiptPrinter {
  fun printReceipt(context: Context, payment: Payment, profile: TuitionProfile) {
    val periods = if (payment.allocatedFeePeriods.isEmpty()) "General Arrears / Advance"
    else payment.allocatedFeePeriods.joinToString(", ") { DateUtils.formatShortPeriod(it) }

    val htmlDocument = """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>Receipt - ${payment.receiptNumber}</title>
        <style>
          body { font-family: 'Helvetica Neue', Arial, sans-serif; padding: 24px; color: #1e293b; max-width: 600px; margin: 0 auto; }
          .header { text-align: center; border-bottom: 2px solid #2563eb; padding-bottom: 16px; margin-bottom: 20px; }
          .title { font-size: 24px; font-weight: bold; color: #1e3a8a; margin: 0; }
          .subtitle { font-size: 14px; color: #64748b; margin-top: 4px; }
          .badge { display: inline-block; background-color: #d1fae5; color: #065f46; font-size: 12px; font-weight: bold; padding: 4px 12px; border-radius: 9999px; margin-top: 8px; }
          .grid { display: flex; justify-content: space-between; margin-bottom: 16px; font-size: 14px; }
          .box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin-bottom: 16px; }
          .box-title { font-size: 12px; font-weight: bold; text-transform: uppercase; color: #64748b; margin-bottom: 8px; }
          .row { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 14px; }
          .amount-box { background: #eff6ff; border: 2px dashed #2563eb; border-radius: 8px; padding: 16px; text-align: center; margin: 20px 0; }
          .amount-val { font-size: 28px; font-weight: bold; color: #1d4ed8; }
          .amount-words { font-size: 13px; color: #475569; font-style: italic; margin-top: 4px; }
          .footer { text-align: center; font-size: 12px; color: #94a3b8; margin-top: 32px; border-top: 1px solid #e2e8f0; padding-top: 16px; }
          .signature { margin-top: 40px; display: flex; justify-content: space-between; font-size: 13px; }
          .sig-line { border-top: 1px solid #475569; width: 180px; text-align: center; padding-top: 4px; }
        </style>
      </head>
      <body>
        <div class="header">
          <h1 class="title">${profile.tuitionName}</h1>
          <div class="subtitle">${profile.teacherName} • ${profile.phone}</div>
          <div class="subtitle">${profile.address}</div>
          <div class="badge">OFFICIAL PAYMENT RECEIPT</div>
        </div>

        <div class="grid">
          <div><strong>Receipt No:</strong> ${payment.receiptNumber}</div>
          <div><strong>Date:</strong> ${DateUtils.formatDisplayDate(payment.paymentDate)}</div>
        </div>

        <div class="box">
          <div class="box-title">Student Information</div>
          <div class="row"><span>Student Name:</span> <strong>${payment.studentName}</strong></div>
          <div class="row"><span>Class & Batch:</span> <span>${payment.studentClass}</span></div>
          <div class="row"><span>Student ID:</span> <span>${payment.studentId}</span></div>
        </div>

        <div class="box">
          <div class="box-title">Fee Details</div>
          <div class="row"><span>Fee Period(s):</span> <strong>$periods</strong></div>
          <div class="row"><span>Payment Method:</span> <span>${payment.paymentMethod.label}</span></div>
          ${if (payment.transactionReference.isNotBlank()) "<div class=\"row\"><span>Transaction / Ref ID:</span> <span>${payment.transactionReference}</span></div>" else ""}
          ${if (payment.notes.isNotBlank()) "<div class=\"row\"><span>Notes:</span> <span>${payment.notes}</span></div>" else ""}
        </div>

        <div class="amount-box">
          <div class="box-title">Total Amount Received</div>
          <div class="amount-val">${FormatUtils.formatCurrency(payment.amount, profile.currencySymbol)}</div>
          <div class="amount-words">${FormatUtils.numberToWords(payment.amount)}</div>
        </div>

        <div class="signature">
          <div></div>
          <div class="sig-line">Authorized Signatory<br><small>(${profile.teacherName})</small></div>
        </div>

        <div class="footer">
          <p>${profile.receiptFooterNote}</p>
          <p>Generated electronically by Tuition Fee Manager on ${DateUtils.formatReceiptDateTime(payment.createdAt)}</p>
        </div>
      </body>
      </html>
    """.trimIndent()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
      override fun onPageFinished(view: WebView?, url: String?) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        val printAdapter = webView.createPrintDocumentAdapter("Receipt_${payment.receiptNumber}")
        val printAttributes = PrintAttributes.Builder()
          .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
          .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
          .build()
        printManager?.print("Tuition_Receipt_${payment.receiptNumber}", printAdapter, printAttributes)
      }
    }
    webView.loadDataWithBaseURL(null, htmlDocument, "text/HTML", "UTF-8", null)
  }
}
