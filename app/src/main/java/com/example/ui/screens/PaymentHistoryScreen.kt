package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecordStatus
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TuitionViewModel
import com.example.util.DateUtils
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
  viewModel: TuitionViewModel,
  onNavigateToStudentDetail: (String) -> Unit
) {
  val payments by viewModel.payments.collectAsState()
  val profile by viewModel.tuitionProfile.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var dateFilter by remember { mutableStateOf("ALL") } // "TODAY", "THIS_WEEK", "THIS_MONTH", "ALL"
  var methodFilter by remember { mutableStateOf<PaymentMethod?>(null) }

  val filteredPayments = remember(payments, searchQuery, dateFilter, methodFilter) {
    payments.filter { p ->
      val matchesSearch = searchQuery.isBlank() ||
          p.studentName.contains(searchQuery, ignoreCase = true) ||
          p.receiptNumber.contains(searchQuery, ignoreCase = true) ||
          p.transactionReference.contains(searchQuery, ignoreCase = true)

      val matchesDate = when (dateFilter) {
        "TODAY" -> DateUtils.isToday(p.paymentDate)
        "THIS_WEEK" -> DateUtils.isThisWeek(p.paymentDate)
        "THIS_MONTH" -> DateUtils.isThisMonth(p.paymentDate)
        else -> true
      }

      val matchesMethod = methodFilter == null || p.paymentMethod == methodFilter

      matchesSearch && matchesDate && matchesMethod
    }
  }

  val totalCollectedInView = remember(filteredPayments) {
    filteredPayments.filter { it.status == PaymentRecordStatus.COMPLETED }.sumOf { it.amount }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Payment History", fontWeight = FontWeight.Bold)
            Text(
              "${filteredPayments.size} receipts • Total: ${FormatUtils.formatCurrency(totalCollectedInView, profile.currencySymbol)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
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
      // Search Box
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by receipt #, student name, UTR...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryMuted) },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryMuted)
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = OutlinedTextFieldDefaults.colors(
          unfocusedContainerColor = ClayColors.CardWhite,
          focusedContainerColor = ClayColors.CardWhite,
          unfocusedBorderColor = BorderWarmGray,
          focusedBorderColor = DeepTealPrimary
        )
      )

      // Date Range Filter Chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
      ) {
        item {
          FilterChip(
            selected = dateFilter == "ALL",
            onClick = { dateFilter = "ALL" },
            label = { Text("All Time") }
          )
        }
        item {
          FilterChip(
            selected = dateFilter == "TODAY",
            onClick = { dateFilter = "TODAY" },
            label = { Text("Today") }
          )
        }
        item {
          FilterChip(
            selected = dateFilter == "THIS_WEEK",
            onClick = { dateFilter = "THIS_WEEK" },
            label = { Text("This Week") }
          )
        }
        item {
          FilterChip(
            selected = dateFilter == "THIS_MONTH",
            onClick = { dateFilter = "THIS_MONTH" },
            label = { Text("This Month") }
          )
        }
      }

      // Method Filter Chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 6.dp)
      ) {
        item {
          FilterChip(
            selected = methodFilter == null,
            onClick = { methodFilter = null },
            label = { Text("All Modes") }
          )
        }
        items(PaymentMethod.values()) { m ->
          FilterChip(
            selected = methodFilter == m,
            onClick = { methodFilter = if (methodFilter == m) null else m },
            label = { Text(m.label) }
          )
        }
      }

      // List
      if (filteredPayments.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.AutoMirrored.Filled.ReceiptLong,
              contentDescription = null,
              modifier = Modifier.size(54.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No payments found.", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Recorded payment receipts will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredPayments, key = { it.id }) { payment ->
            ClayCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(22.dp),
              backgroundColor = ClayColors.CardWhite,
              elevation = 5.dp,
              onClick = { viewModel.selectReceiptPayment(payment) }
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(
                      text = payment.studentName,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = MaterialTheme.colorScheme.onSurface
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
                      fontSize = 17.sp,
                      color = StatusPaid
                    )
                    PaymentStatusBadge(status = payment.status)
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  val periods = if (payment.allocatedFeePeriods.isEmpty()) "General / Advance"
                  else payment.allocatedFeePeriods.joinToString(", ") { DateUtils.formatShortPeriod(it) }

                  Column {
                    Text(
                      text = "Period(s): $periods",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = "Date: ${DateUtils.formatDisplayDate(payment.paymentDate)}",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  Text(
                    text = "View Receipt →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandBluePrimary
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
