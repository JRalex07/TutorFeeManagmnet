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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeeStatus
import com.example.data.model.Student
import com.example.data.model.FeeRecord
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TuitionViewModel
import com.example.util.DateUtils
import com.example.util.FormatUtils
import com.example.util.ReminderUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingFeesScreen(
  viewModel: TuitionViewModel,
  onNavigateToCollectFee: (String) -> Unit,
  onNavigateToStudentDetail: (String) -> Unit
) {
  val feeRecords by viewModel.feeRecords.collectAsState()
  val students by viewModel.students.collectAsState()
  val profile by viewModel.tuitionProfile.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var filterOnlyOverdue by remember { mutableStateOf(false) }
  var filterOnlyPartial by remember { mutableStateOf(false) }
  var sortByOldest by remember { mutableStateOf(true) }

  var reminderTarget by remember { mutableStateOf<Pair<Student, FeeRecord?>?>(null) }

  // Extract all pending fee records (remainingAmount > 0 and not waived)
  val allPending = remember(feeRecords) {
    feeRecords.filter { it.remainingAmount > 0 && it.status != FeeStatus.WAIVED }
  }

  val totalPendingAmount = remember(allPending) { allPending.sumOf { it.remainingAmount } }
  val overduePendingAmount = remember(allPending) {
    allPending.filter { it.status == FeeStatus.OVERDUE || DateUtils.isOverdue(it.dueDate) }.sumOf { it.remainingAmount }
  }

  val filteredRecords = remember(allPending, searchQuery, filterOnlyOverdue, filterOnlyPartial, sortByOldest) {
    var list = allPending.filter { rec ->
      val matchesSearch = searchQuery.isBlank() ||
          rec.studentName.contains(searchQuery, ignoreCase = true) ||
          rec.feePeriod.contains(searchQuery)

      val matchesOverdue = !filterOnlyOverdue || (rec.status == FeeStatus.OVERDUE || DateUtils.isOverdue(rec.dueDate))
      val matchesPartial = !filterOnlyPartial || rec.status == FeeStatus.PARTIALLY_PAID

      matchesSearch && matchesOverdue && matchesPartial
    }

    list = if (sortByOldest) {
      list.sortedBy { it.feePeriod }
    } else {
      list.sortedByDescending { it.remainingAmount }
    }
    list
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Pending & Overdue Fees", fontWeight = FontWeight.Bold)
            Text(
              "${filteredRecords.size} outstanding entries",
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
      // Summary Card
      ClayCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp, 8.dp, 16.dp, 8.dp),
        backgroundColor = ClayColors.CardWhite,
        shape = RoundedCornerShape(22.dp),
        elevation = 5.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("Total Outstanding", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = FormatUtils.formatCurrency(totalPendingAmount, profile.currencySymbol),
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = StatusDue
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text("Critical Overdue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = FormatUtils.formatCurrency(overduePendingAmount, profile.currencySymbol),
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = StatusOverdue
            )
          }
        }
      }

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by student name or period...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedContainerColor = MaterialTheme.colorScheme.surface
        )
      )

      // Filter Chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 6.dp)
      ) {
        item {
          FilterChip(
            selected = filterOnlyOverdue,
            onClick = { filterOnlyOverdue = !filterOnlyOverdue },
            label = { Text("Only Overdue") },
            leadingIcon = if (filterOnlyOverdue) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
          )
        }
        item {
          FilterChip(
            selected = filterOnlyPartial,
            onClick = { filterOnlyPartial = !filterOnlyPartial },
            label = { Text("Partially Paid") },
            leadingIcon = if (filterOnlyPartial) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
          )
        }
        item {
          FilterChip(
            selected = !sortByOldest,
            onClick = { sortByOldest = !sortByOldest },
            label = { Text(if (sortByOldest) "Sort: Oldest First" else "Sort: Highest Due") }
          )
        }
      }

      // Pending List
      if (filteredRecords.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.Default.CheckCircleOutline,
              contentDescription = null,
              tint = StatusPaid,
              modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("All Caught Up!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
              "No pending tuition fees found matching your criteria.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 13.sp
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredRecords, key = { it.id }) { rec ->
            val isOverdue = rec.status == FeeStatus.OVERDUE || DateUtils.isOverdue(rec.dueDate)
            val studentObj = students.find { it.id == rec.studentId }
            val daysOverdue = if (isOverdue && rec.dueDate.isNotBlank()) ReminderUtils.calculateDaysOverdue(rec.dueDate) else 0L

            ClayCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(22.dp),
              backgroundColor = if (isOverdue) Color(0xFFFFF7F6) else ClayColors.CardWhite,
              borderWidth = if (isOverdue) 2.dp else 1.5.dp,
              highlightColor = if (isOverdue) ClayColors.RoseBorder else Color.White.copy(alpha = 0.85f),
              elevation = if (isOverdue) 6.dp else 5.dp
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = rec.studentName,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = TextInkPrimary
                    )
                    Text(
                      text = "${DateUtils.formatDisplayPeriod(rec.feePeriod)} • Due: ${DateUtils.formatDisplayDate(rec.dueDate)}",
                      fontSize = 12.sp,
                      color = if (isOverdue) ClayColors.RoseAccent else TextSecondaryMuted
                    )
                    if (studentObj != null) {
                      Text(
                        text = "${studentObj.studentClass} • Parent: ${studentObj.parentPhone.ifBlank { "N/A" }}",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                      )
                    }
                    if (rec.lastReminderSentAt != null || studentObj?.lastReminderSentAt != null) {
                      val reminderTime = rec.lastReminderSentAt ?: studentObj?.lastReminderSentAt
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "🔔 Reminder sent ${ReminderUtils.formatRelativeTime(reminderTime)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClayColors.TealAccent
                      )
                    }
                  }

                  ClayBadge(
                    text = if (isOverdue && daysOverdue > 0) "$daysOverdue days overdue" else if (isOverdue) "Overdue" else rec.status.label,
                    backgroundColor = if (isOverdue) ClayColors.RoseSurface else ClayColors.AmberSurface,
                    textColor = if (isOverdue) ClayColors.RoseAccent else ClayColors.AmberAccent,
                    borderColor = if (isOverdue) ClayColors.RoseBorder else ClayColors.AmberBorder,
                    icon = if (isOverdue) Icons.Default.Warning else null
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isOverdue) ClayColors.RoseBorder.copy(alpha = 0.4f) else BorderWarmGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(
                      text = "Remaining: ${FormatUtils.formatCurrency(rec.remainingAmount, profile.currencySymbol)}",
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 15.sp,
                      color = if (isOverdue) ClayColors.RoseAccent else ClayColors.AmberAccent
                    )
                    if (rec.paidAmount > 0) {
                      Text(
                        text = "Paid: ${FormatUtils.formatCurrency(rec.paidAmount, profile.currencySymbol)} of ${FormatUtils.formatCurrency(rec.netFee, profile.currencySymbol)}",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                      )
                    }
                  }

                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // One-Tap Send Reminder button
                    if (studentObj != null) {
                      ClayButton(
                        onClick = { reminderTarget = studentObj to rec },
                        shape = RoundedCornerShape(10.dp),
                        containerColor = if (isOverdue) ClayColors.RoseAccent else Color(0xFF6748A5),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                      ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }

                    ClaySecondaryButton(
                      onClick = { onNavigateToStudentDetail(rec.studentId) },
                      shape = RoundedCornerShape(10.dp),
                      backgroundColor = ClayColors.CardMuted,
                      contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                      Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextInkPrimary)
                    }

                    ClayButton(
                      onClick = { onNavigateToCollectFee(rec.studentId) },
                      shape = RoundedCornerShape(10.dp),
                      containerColor = DeepTealPrimary,
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(13.dp))
                      Spacer(modifier = Modifier.width(3.dp))
                      Text("Collect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // 1-Tap Fee Reminder Modal Dialog
  if (reminderTarget != null) {
    val (targetStudent, targetRec) = reminderTarget!!
    SendReminderDialog(
      student = targetStudent,
      feeRecord = targetRec,
      profile = profile,
      onDismiss = { reminderTarget = null },
      onReminderSent = { channel ->
        viewModel.sendFeeReminder(targetStudent.id, targetRec?.id, channel)
      }
    )
  }
}
