package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Payment
import com.example.data.model.TuitionProfile
import com.example.util.DateUtils
import com.example.util.FormatUtils
import com.example.util.ReceiptPrinter

@Composable
fun ReceiptDialog(
  payment: Payment,
  profile: TuitionProfile,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(16.dp)
        .clip(RoundedCornerShape(16.dp)),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(scrollState)
      ) {
        // Top action bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Fee Receipt",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Receipt Card Document
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = profile.tuitionName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )
          Text(
            text = "${profile.teacherName} • ${profile.phone}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = profile.address,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(8.dp))
          PaymentStatusBadge(status = payment.status)
          Spacer(modifier = Modifier.height(12.dp))

          HorizontalDivider()
          Spacer(modifier = Modifier.height(12.dp))

          // Meta info
          ReceiptRow(label = "Receipt No", value = payment.receiptNumber, isBold = true)
          ReceiptRow(label = "Date", value = DateUtils.formatDisplayDate(payment.paymentDate))
          ReceiptRow(label = "Student", value = payment.studentName, isBold = true)
          ReceiptRow(label = "Student ID", value = payment.studentId)
          ReceiptRow(label = "Class", value = payment.studentClass)

          Spacer(modifier = Modifier.height(8.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(8.dp))

          val periods = if (payment.allocatedFeePeriods.isEmpty()) "General / Advance"
          else payment.allocatedFeePeriods.joinToString(", ") { DateUtils.formatShortPeriod(it) }
          ReceiptRow(label = "Fee Period(s)", value = periods)
          ReceiptRow(label = "Payment Mode", value = payment.paymentMethod.label)
          if (payment.transactionReference.isNotBlank()) {
            ReceiptRow(label = "Reference / UTR", value = payment.transactionReference)
          }
          if (payment.notes.isNotBlank()) {
            ReceiptRow(label = "Notes", value = payment.notes)
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Amount Box
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(8.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "AMOUNT RECEIVED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = FormatUtils.formatCurrency(payment.amount, profile.currencySymbol),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = FormatUtils.numberToWords(payment.amount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = profile.receiptFooterNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 11.sp
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Print & Share
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = {
              ReceiptPrinter.printReceipt(context, payment, profile)
            },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Print / PDF")
          }

          Button(
            onClick = {
              FormatUtils.shareReceipt(context, payment, profile)
            },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share")
          }
        }
      }
    }
  }
}

@Composable
private fun ReceiptRow(label: String, value: String, isBold: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
