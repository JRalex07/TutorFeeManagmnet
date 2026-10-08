package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TuitionViewModel
import com.example.util.DateUtils
import com.example.util.FormatUtils
import com.example.util.ReminderUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
  viewModel: TuitionViewModel,
  studentId: String,
  onNavigateBack: () -> Unit,
  onNavigateToEdit: (String) -> Unit,
  onNavigateToCollectFee: (String) -> Unit
) {
  val students by viewModel.students.collectAsState()
  val feeRecords by viewModel.feeRecords.collectAsState()
  val payments by viewModel.payments.collectAsState()
  val profile by viewModel.tuitionProfile.collectAsState()

  val student = students.find { it.id == studentId }
  val studentFeeRecords = feeRecords.filter { it.studentId == studentId }.sortedByDescending { it.feePeriod }
  val studentPayments = payments.filter { it.studentId == studentId }.sortedByDescending { it.createdAt }

  val summary = viewModel.getStudentFinancialSummary(studentId)

  // Dialog States
  var showCancelPaymentDialog by remember { mutableStateOf<Payment?>(null) }
  var cancelReason by remember { mutableStateOf("") }

  var showRefundDialog by remember { mutableStateOf(false) }
  var refundAmountStr by remember { mutableStateOf("") }
  var refundReason by remember { mutableStateOf("") }
  var refundMethod by remember { mutableStateOf(PaymentMethod.CASH) }

  var showLeavingDialog by remember { mutableStateOf(false) }
  var leavingReason by remember { mutableStateOf("") }
  var leavingSettlement by remember { mutableStateOf(SettlementStatus.FULLY_SETTLED) }

  var showWaiveDialog by remember { mutableStateOf<FeeRecord?>(null) }
  var waiveReason by remember { mutableStateOf("") }

  var showDeleteDialog by remember { mutableStateOf(false) }

  var showReminderRecord by remember { mutableStateOf<FeeRecord?>(null) }
  var showStudentReminderDialog by remember { mutableStateOf(false) }

  if (student == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Student not found")
    }
    return
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(student.fullName, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { onNavigateToEdit(student.id) }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
          }
          IconButton(onClick = { showDeleteDialog = true }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Student", tint = MaterialTheme.colorScheme.error)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { onNavigateToCollectFee(student.id) },
        containerColor = DeepTealPrimary,
        contentColor = Color.White,
        shape = RoundedCornerShape(20.dp)
      ) {
        Icon(Icons.Default.Payments, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Collect Fee", fontWeight = FontWeight.Bold)
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(ClayColors.CanvasBg),
      contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Header Information Card
      item {
        ClayCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          backgroundColor = ClayColors.CardWhite,
          elevation = 5.dp
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = student.fullName,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${student.studentId} • ${student.studentClass} • ${student.batch}",
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              StudentStatusBadge(status = student.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              InfoItem("Parent Phone", student.parentPhone.ifBlank { "N/A" })
              InfoItem("Student Phone", student.phoneNumber.ifBlank { "N/A" })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              InfoItem("Monthly Base Fee", FormatUtils.formatCurrency(student.monthlyFee, profile.currencySymbol))
              InfoItem("Due Day", "${student.preferredPaymentDay}th of month")
            }

            if (student.subjects.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              InfoItem("Subjects", student.displaySubjects)
            }

            if (student.address.isNotBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              InfoItem("Address", student.address)
            }
          }
        }
      }

      // 2. Financial Summary Card
      if (summary != null) {
        item {
          ClayCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = ClayColors.CardWhite,
            elevation = 5.dp
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "FINANCIAL SUMMARY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DeepTealPrimary
              )

              Spacer(modifier = Modifier.height(10.dp))

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FinancialMetricBox(
                  title = "Current Month Due",
                  amount = FormatUtils.formatCurrency(summary.currentMonthRemaining, profile.currencySymbol),
                  badge = { FeeStatusBadge(status = summary.currentMonthStatus) },
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FinancialMetricBox(
                  title = "Advance Balance",
                  amount = FormatUtils.formatCurrency(summary.advanceBalance, profile.currencySymbol),
                  textColor = StatusAdvance,
                  modifier = Modifier.weight(1f)
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FinancialMetricBox(
                  title = "Total Paid (All Time)",
                  amount = FormatUtils.formatCurrency(summary.totalPaidAllTime, profile.currencySymbol),
                  textColor = StatusPaid,
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FinancialMetricBox(
                  title = "Total Pending / Arrears",
                  amount = FormatUtils.formatCurrency(summary.totalPendingAllTime, profile.currencySymbol),
                  textColor = if (summary.totalPendingAllTime > 0) StatusOverdue else StatusPaid,
                  modifier = Modifier.weight(1f)
                )
              }

              // Special actions: Refund advance or Mark leaving
              Spacer(modifier = Modifier.height(14.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (student.advanceBalance > 0) {
                  OutlinedButton(
                    onClick = { showRefundDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refund Advance", fontSize = 12.sp)
                  }
                }

                if (student.status == StudentStatus.ACTIVE) {
                  OutlinedButton(
                    onClick = { showLeavingDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark as Left", fontSize = 12.sp)
                  }
                }
              }

              // Overdue follow up 1-tap button
              if (summary.totalPendingAllTime > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                ClayButton(
                  onClick = { showStudentReminderDialog = true },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                  containerColor = ClayColors.RoseAccent,
                  contentPadding = PaddingValues(vertical = 10.dp, horizontal = 14.dp)
                ) {
                  Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Send Overdue Reminder to Parent", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }
            }
          }
        }
      }

      // 3. Monthly Fee Breakdown Section
      item {
        Text(
          text = "Monthly Fee Ledger",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(top = 8.dp)
        )
      }

      if (studentFeeRecords.isEmpty()) {
        item {
          Text(
            text = "No monthly fee records generated yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
          )
        }
      } else {
        items(studentFeeRecords, key = { it.id }) { record ->
          ClayCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ClayColors.CardWhite,
            elevation = 4.dp
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = DateUtils.formatDisplayPeriod(record.feePeriod),
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
                FeeStatusBadge(status = record.status)
              }

              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Net: ${FormatUtils.formatCurrency(record.netFee, profile.currencySymbol)} (Paid: ${FormatUtils.formatCurrency(record.paidAmount, profile.currencySymbol)})",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Remaining: ${FormatUtils.formatCurrency(record.remainingAmount, profile.currencySymbol)}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (record.remainingAmount > 0) StatusOverdue else StatusPaid
                )
              }

              if (record.remainingAmount > 0 && record.status != FeeStatus.WAIVED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  TextButton(
                    onClick = { showReminderRecord = record },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(13.dp), tint = ClayColors.RoseAccent)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Remind", fontSize = 11.sp, color = ClayColors.RoseAccent, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  TextButton(
                    onClick = { showWaiveDialog = record },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text("Waive Fee", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Button(
                    onClick = { onNavigateToCollectFee(student.id) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTealPrimary),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Text("Pay Now", fontSize = 11.sp)
                  }
                }
              }
            }
          }
        }
      }

      // 4. Payment Timeline Section
      item {
        Text(
          text = "Payment History Timeline",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(top = 12.dp)
        )
      }

      if (studentPayments.isEmpty()) {
        item {
          Text(
            text = "No payments recorded for this student yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
          )
        }
      } else {
        items(studentPayments, key = { it.id }) { payment ->
          ClayCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ClayColors.CardWhite,
            elevation = 4.dp,
            onClick = { viewModel.selectReceiptPayment(payment) }
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = DateUtils.formatDisplayDate(payment.paymentDate),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                  Text(
                    text = "${payment.receiptNumber} • ${payment.paymentMethod.label}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = FormatUtils.formatCurrency(payment.amount, profile.currencySymbol),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StatusPaid
                  )
                  PaymentStatusBadge(status = payment.status)
                }
              }

              if (payment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Note: ${payment.notes}",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Tap to view receipt",
                  fontSize = 11.sp,
                  color = DeepTealPrimary,
                  fontWeight = FontWeight.SemiBold
                )

                if (payment.status == PaymentRecordStatus.COMPLETED) {
                  TextButton(
                    onClick = { showCancelPaymentDialog = payment },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("Reverse / Cancel", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // 1. Cancel / Reverse Payment Dialog
  if (showCancelPaymentDialog != null) {
    val p = showCancelPaymentDialog!!
    AlertDialog(
      onDismissRequest = { showCancelPaymentDialog = null },
      title = { Text("Cancel / Reverse Payment", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Are you sure you want to reverse Receipt ${p.receiptNumber} for ${FormatUtils.formatCurrency(p.amount, profile.currencySymbol)}? This will restore student fee balances.")
          OutlinedTextField(
            value = cancelReason,
            onValueChange = { cancelReason = it },
            label = { Text("Reason for Reversal *") },
            placeholder = { Text("e.g. Wrong amount entered / bounce") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (cancelReason.isNotBlank()) {
              viewModel.cancelPayment(p.id, cancelReason)
              showCancelPaymentDialog = null
              cancelReason = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Confirm Reversal")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showCancelPaymentDialog = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // 2. Refund Dialog
  if (showRefundDialog) {
    AlertDialog(
      onDismissRequest = { showRefundDialog = false },
      title = { Text("Issue Advance Refund", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Student has an advance balance of ${FormatUtils.formatCurrency(student.advanceBalance, profile.currencySymbol)}.")
          OutlinedTextField(
            value = refundAmountStr,
            onValueChange = { refundAmountStr = it },
            label = { Text("Refund Amount (₹) *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = refundReason,
            onValueChange = { refundReason = it },
            label = { Text("Reason for Refund") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val amt = refundAmountStr.toDoubleOrNull() ?: 0.0
            if (amt > 0 && amt <= student.advanceBalance) {
              viewModel.issueRefund(student.id, amt, refundMethod, refundReason)
              showRefundDialog = false
              refundAmountStr = ""
              refundReason = ""
            }
          }
        ) {
          Text("Process Refund")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showRefundDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // 3. Leaving Tuition Dialog
  if (showLeavingDialog) {
    AlertDialog(
      onDismissRequest = { showLeavingDialog = false },
      title = { Text("Mark Student as Left", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Record student withdrawal while preserving complete historical financial records.")
          OutlinedTextField(
            value = leavingReason,
            onValueChange = { leavingReason = it },
            label = { Text("Reason for leaving") },
            modifier = Modifier.fillMaxWidth()
          )
          Text("Final Settlement Status:", style = MaterialTheme.typography.labelSmall)
          SettlementStatus.values().forEach { st ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { leavingSettlement = st }
                .padding(vertical = 4.dp)
            ) {
              RadioButton(selected = leavingSettlement == st, onClick = { leavingSettlement = st })
              Spacer(modifier = Modifier.width(6.dp))
              Text(st.label, fontSize = 13.sp)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.markStudentLeaving(
              student.id,
              DateUtils.currentDateString(),
              leavingReason,
              leavingSettlement
            )
            showLeavingDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Confirm")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showLeavingDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // 4. Waive Fee Dialog
  if (showWaiveDialog != null) {
    val rec = showWaiveDialog!!
    AlertDialog(
      onDismissRequest = { showWaiveDialog = null },
      title = { Text("Waive Fee for ${DateUtils.formatDisplayPeriod(rec.feePeriod)}", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("This will waive the outstanding balance of ${FormatUtils.formatCurrency(rec.remainingAmount, profile.currencySymbol)} with an official reason.")
          OutlinedTextField(
            value = waiveReason,
            onValueChange = { waiveReason = it },
            label = { Text("Reason for Fee Waiver *") },
            placeholder = { Text("e.g. Sibling discount / scholarship / illness") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (waiveReason.isNotBlank()) {
              viewModel.waiveFee(rec.id, waiveReason)
              showWaiveDialog = null
              waiveReason = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Confirm Waiver")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showWaiveDialog = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // 5. Delete Student Dialog
  if (showDeleteDialog) {
    ConfirmDialog(
      title = "Delete Student Record?",
      message = "Are you sure you want to remove ${student.fullName}? If student simply stopped attending, consider 'Mark as Left' instead to preserve financial reports.",
      confirmButtonText = "Delete Student",
      isDestructive = true,
      onConfirm = {
        viewModel.deleteStudent(student.id) {
          onNavigateBack()
        }
      },
      onDismiss = { showDeleteDialog = false }
    )
  }

  // 6. Overdue Fee Reminder Modal Dialog
  if (showStudentReminderDialog || showReminderRecord != null) {
    val targetRec = showReminderRecord ?: studentFeeRecords.firstOrNull { it.remainingAmount > 0 }
    SendReminderDialog(
      student = student,
      feeRecord = targetRec,
      profile = profile,
      onDismiss = {
        showStudentReminderDialog = false
        showReminderRecord = null
      },
      onReminderSent = { channel ->
        viewModel.sendFeeReminder(student.id, targetRec?.id, channel)
      }
    )
  }
}

@Composable
private fun InfoItem(label: String, value: String) {
  Column {
    Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
private fun FinancialMetricBox(
  title: String,
  amount: String,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  badge: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .padding(10.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        badge?.invoke()
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = amount, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
    }
  }
}
