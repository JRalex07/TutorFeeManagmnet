package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeeStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecordStatus
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TuitionViewModel
import com.example.util.DateUtils
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  viewModel: TuitionViewModel
) {
  val feeRecords by viewModel.feeRecords.collectAsState()
  val payments by viewModel.payments.collectAsState()
  val students by viewModel.students.collectAsState()
  val profile by viewModel.tuitionProfile.collectAsState()

  var selectedTab by remember { mutableStateOf(0) } // 0: Monthly Report, 1: Method Breakdown, 2: Daily Collection
  var selectedPeriod by remember { mutableStateOf(DateUtils.currentPeriod()) }

  val availablePeriods = remember(feeRecords) {
    (feeRecords.map { it.feePeriod } + DateUtils.currentPeriod()).distinct().sortedDescending()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Financial Reports & Ledger", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(ClayColors.CanvasBg)
    ) {
      // Tab selector
      PrimaryTabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Monthly", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Payment Modes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("Daily Timeline", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        )
      }

      when (selectedTab) {
        0 -> MonthlyReportTab(
          selectedPeriod = selectedPeriod,
          availablePeriods = availablePeriods,
          onSelectPeriod = { selectedPeriod = it },
          feeRecords = feeRecords,
          students = students,
          currencySymbol = profile.currencySymbol
        )
        1 -> PaymentMethodReportTab(
          payments = payments,
          currencySymbol = profile.currencySymbol
        )
        2 -> DailyTimelineReportTab(
          payments = payments,
          currencySymbol = profile.currencySymbol
        )
      }
    }
  }
}

@Composable
private fun MonthlyReportTab(
  selectedPeriod: String,
  availablePeriods: List<String>,
  onSelectPeriod: (String) -> Unit,
  feeRecords: List<com.example.data.model.FeeRecord>,
  students: List<com.example.data.model.Student>,
  currencySymbol: String
) {
  val recordsForPeriod = remember(selectedPeriod, feeRecords) {
    feeRecords.filter { it.feePeriod == selectedPeriod }
  }

  val totalExpected = remember(recordsForPeriod) { recordsForPeriod.sumOf { it.netFee } }
  val totalCollected = remember(recordsForPeriod) { recordsForPeriod.sumOf { it.paidAmount } }
  val totalPending = remember(recordsForPeriod) { recordsForPeriod.sumOf { it.remainingAmount } }
  val overdueAmount = remember(recordsForPeriod) {
    recordsForPeriod.filter { it.status == FeeStatus.OVERDUE || DateUtils.isOverdue(it.dueDate) }.sumOf { it.remainingAmount }
  }

  val paidCount = recordsForPeriod.count { it.status == FeeStatus.PAID }
  val partialCount = recordsForPeriod.count { it.status == FeeStatus.PARTIALLY_PAID }
  val pendingCount = recordsForPeriod.count { it.remainingAmount > 0 && it.paidAmount == 0.0 }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Period selector chip row
    item {
      Column {
        Text("Select Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(availablePeriods) { p ->
            FilterChip(
              selected = selectedPeriod == p,
              onClick = { onSelectPeriod(p) },
              label = { Text(DateUtils.formatShortPeriod(p)) }
            )
          }
        }
      }
    }

    // High level metrics
    item {
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 5.dp
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "COLLECTION PERFORMANCE - ${DateUtils.formatDisplayPeriod(selectedPeriod).uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = DeepTealPrimary
          )

          Spacer(modifier = Modifier.height(12.dp))

          ReportDataRow("Total Expected Fees", FormatUtils.formatCurrency(totalExpected, currencySymbol))
          ReportDataRow("Total Collected", FormatUtils.formatCurrency(totalCollected, currencySymbol), textColor = StatusPaid, isBold = true)
          ReportDataRow("Total Pending", FormatUtils.formatCurrency(totalPending, currencySymbol), textColor = StatusDue)
          ReportDataRow("Overdue Amount", FormatUtils.formatCurrency(overdueAmount, currencySymbol), textColor = StatusOverdue)

          Spacer(modifier = Modifier.height(8.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(8.dp))

          ReportDataRow("Students Paid in Full", "$paidCount students")
          ReportDataRow("Students Partially Paid", "$partialCount students")
          ReportDataRow("Students with Pending Fees", "$pendingCount students")
        }
      }
    }

    // Student Breakdown
    item {
      Text(
        text = "Student Ledger for ${DateUtils.formatDisplayPeriod(selectedPeriod)}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
    }

    items(recordsForPeriod) { rec ->
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 3.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(rec.studentName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
              "Net: ${FormatUtils.formatCurrency(rec.netFee, currencySymbol)} • Paid: ${FormatUtils.formatCurrency(rec.paidAmount, currencySymbol)}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              "Due: ${FormatUtils.formatCurrency(rec.remainingAmount, currencySymbol)}",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = if (rec.remainingAmount > 0) StatusOverdue else StatusPaid
            )
            Text(rec.status.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}

@Composable
private fun PaymentMethodReportTab(
  payments: List<com.example.data.model.Payment>,
  currencySymbol: String
) {
  val validPayments = remember(payments) { payments.filter { it.status == PaymentRecordStatus.COMPLETED } }
  val total = remember(validPayments) { validPayments.sumOf { it.amount } }

  val methodGroups = remember(validPayments) {
    PaymentMethod.values().map { method ->
      val list = validPayments.filter { it.paymentMethod == method }
      val sum = list.sumOf { it.amount }
      val percentage = if (total > 0) (sum / total) * 100 else 0.0
      Triple(method, sum, percentage)
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "PAYMENT METHOD DISTRIBUTION",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Total All-Time Collections: ${FormatUtils.formatCurrency(total, currencySymbol)}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
      }
    }

    items(methodGroups) { (method, sum, percent) ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(method.label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
              "${FormatUtils.formatCurrency(sum, currencySymbol)} (${percent.toInt()}%)",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(
            progress = { (percent / 100f).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = BrandBluePrimary
          )
        }
      }
    }
  }
}

@Composable
private fun DailyTimelineReportTab(
  payments: List<com.example.data.model.Payment>,
  currencySymbol: String
) {
  val dateGroups = remember(payments) {
    payments
      .filter { it.status == PaymentRecordStatus.COMPLETED }
      .groupBy { it.paymentDate }
      .toList()
      .sortedByDescending { it.first }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (dateGroups.isEmpty()) {
      item {
        Text("No completed payments to analyze yet.", modifier = Modifier.padding(16.dp))
      }
    } else {
      items(dateGroups) { (date, paymentList) ->
        val dayTotal = paymentList.sumOf { it.amount }
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = DateUtils.formatDisplayDate(date),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = FormatUtils.formatCurrency(dayTotal, currencySymbol),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = StatusPaid
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            paymentList.forEach { p ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${p.studentName} (${p.paymentMethod.label})",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = FormatUtils.formatCurrency(p.amount, currencySymbol),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReportDataRow(
  label: String,
  value: String,
  textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
  isBold: Boolean = false
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = textColor)
  }
}
