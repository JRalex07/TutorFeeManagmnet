package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeeStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Student
import com.example.ui.theme.*
import com.example.util.DateUtils
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
  student: Student,
  currencySymbol: String = "₹",
  onDismiss: () -> Unit,
  onConfirmPayment: (
    amount: Double,
    method: PaymentMethod,
    date: String,
    reference: String,
    notes: String
  ) -> Unit
) {
  var amountStr by remember { mutableStateOf(student.monthlyFeeAmount.toInt().toString()) }
  var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var paymentDate by remember { mutableStateOf(DateUtils.currentDateString()) }
  var transactionRef by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0

  val projectedStatus = when {
    enteredAmount >= student.monthlyFeeAmount -> FeeStatus.PAID
    enteredAmount > 0 -> FeeStatus.PARTIALLY_PAID
    else -> student.currentMonthStatus
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(14.dp),
    containerColor = CardSurfaceWhite,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Payments, contentDescription = null, tint = DeepTealPrimary)
        Text("Record Payment", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextInkPrimary)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Student Info Banner (opaque, soft border)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceMuted)
            .padding(12.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = student.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextInkPrimary
              )
              FeeStatusBadge(status = student.currentMonthStatus)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${student.studentId} • ${student.studentClass} • Contact: ${student.parentContact}",
              fontSize = 12.sp,
              color = TextSecondaryMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Monthly Fee: ${FormatUtils.formatCurrency(student.monthlyFeeAmount, currencySymbol)}",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = DeepTealPrimary
            )
          }
        }

        if (errorMessage != null) {
          Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
          )
        }

        // Amount input
        OutlinedTextField(
          value = amountStr,
          onValueChange = {
            amountStr = it
            errorMessage = null
          },
          label = { Text("Payment Amount (₹) *") },
          leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_amount_input"),
          shape = RoundedCornerShape(8.dp)
        )

        // Quick amount chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          AssistChip(
            onClick = { amountStr = student.monthlyFeeAmount.toInt().toString() },
            label = { Text("Full Fee (₹${student.monthlyFeeAmount.toInt()})", fontSize = 11.sp) }
          )
          AssistChip(
            onClick = { amountStr = (student.monthlyFeeAmount / 2).toInt().toString() },
            label = { Text("Half (₹${(student.monthlyFeeAmount / 2).toInt()})", fontSize = 11.sp) }
          )
        }

        // Status Preview Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
              if (projectedStatus == FeeStatus.PAID) StatusPaidContainer
              else StatusPartialContainer
            )
            .padding(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Updated Month Status:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = if (projectedStatus == FeeStatus.PAID) StatusOnPaidContainer else StatusOnPartialContainer
            )
            FeeStatusBadge(status = projectedStatus)
          }
        }

        // Payment Method
        Text("Payment Method", style = MaterialTheme.typography.labelSmall)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(PaymentMethod.CASH, PaymentMethod.UPI, PaymentMethod.BANK_TRANSFER).forEach { method ->
            FilterChip(
              selected = paymentMethod == method,
              onClick = { paymentMethod = method },
              label = { Text(method.label, fontSize = 11.sp) }
            )
          }
        }

        // Date input
        OutlinedTextField(
          value = paymentDate,
          onValueChange = { paymentDate = it },
          label = { Text("Payment Date") },
          leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp)
        )

        // Reference / Transaction ID
        OutlinedTextField(
          value = transactionRef,
          onValueChange = { transactionRef = it },
          label = { Text("Transaction Ref / UTR (Optional)") },
          placeholder = { Text("e.g. UPI-92810") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp)
        )

        // Notes
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountStr.toDoubleOrNull()
          if (amt == null || amt <= 0.0) {
            errorMessage = "Please enter a valid amount greater than 0."
            return@Button
          }
          onConfirmPayment(
            amt,
            paymentMethod,
            paymentDate.trim(),
            transactionRef.trim(),
            notes.trim()
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = DeepTealPrimary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("confirm_payment_button")
      ) {
        Text("Record Payment", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextInkPrimary),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderWarmGray),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Cancel")
      }
    }
  )
}
