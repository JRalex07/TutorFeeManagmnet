package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeeStatus
import com.example.data.model.Student
import com.example.data.model.StudentStatus
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TuitionViewModel
import com.example.util.FormatUtils
import com.example.util.ReminderUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
  viewModel: TuitionViewModel,
  onNavigateToDetail: (String) -> Unit,
  onNavigateToAddStudent: () -> Unit,
  onNavigateToCollectFee: (String) -> Unit
) {
  // Students reactively observed from Room database via ViewModel
  val students by viewModel.students.collectAsState()
  val profile by viewModel.tuitionProfile.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var showOverdueOnly by remember { mutableStateOf(false) }
  var selectedStatusFilter by remember { mutableStateOf<StudentStatus?>(null) }
  var selectedClassFilter by remember { mutableStateOf<String?>(null) }

  // State for Record Payment Dialog
  var studentForPaymentDialog by remember { mutableStateOf<Student?>(null) }
  var studentForReminderDialog by remember { mutableStateOf<Student?>(null) }

  val classes = remember(students) {
    students.map { it.studentClass }.distinct().filter { it.isNotBlank() }
  }

  val overdueStudentsCount = remember(students) {
    students.count { it.currentMonthStatus == FeeStatus.OVERDUE }
  }

  val filteredStudents = remember(students, searchQuery, showOverdueOnly, selectedStatusFilter, selectedClassFilter) {
    students.filter { student ->
      val matchesQuery = searchQuery.isBlank() ||
          student.name.contains(searchQuery, ignoreCase = true) ||
          student.studentId.contains(searchQuery, ignoreCase = true) ||
          student.phoneNumber.contains(searchQuery) ||
          student.parentContact.contains(searchQuery) ||
          student.studentClass.contains(searchQuery, ignoreCase = true) ||
          student.batch.contains(searchQuery, ignoreCase = true)

      val matchesOverdue = !showOverdueOnly || student.currentMonthStatus == FeeStatus.OVERDUE
      val matchesStatus = selectedStatusFilter == null || student.status == selectedStatusFilter
      val matchesClass = selectedClassFilter == null || student.studentClass == selectedClassFilter

      matchesQuery && matchesOverdue && matchesStatus && matchesClass
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Students Directory", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextInkPrimary)
            Text(
              "${filteredStudents.size} of ${students.size} enrolled students",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondaryMuted
            )
          }
        },
        actions = {
          IconButton(onClick = onNavigateToAddStudent) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Student", tint = DeepTealPrimary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurfaceWhite)
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = onNavigateToAddStudent,
        containerColor = DeepTealPrimary,
        contentColor = androidx.compose.ui.graphics.Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("add_student_fab")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Student")
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(ClayColors.CanvasBg)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by name, ID, contact, class...") },
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
          .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = OutlinedTextFieldDefaults.colors(
          unfocusedContainerColor = ClayColors.CardWhite,
          focusedContainerColor = ClayColors.CardWhite,
          unfocusedBorderColor = BorderWarmGray,
          focusedBorderColor = DeepTealPrimary
        )
      )

      // Filter Chips: Status & Overdue
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
      ) {
        // Highlighting overdue students filter chip
        item {
          FilterChip(
            selected = showOverdueOnly,
            onClick = { showOverdueOnly = !showOverdueOnly },
            leadingIcon = {
              Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = if (showOverdueOnly) ClayColors.RoseAccent else TextSecondaryMuted,
                modifier = Modifier.size(13.dp)
              )
            },
            label = {
              Text(
                "Overdue Only ($overdueStudentsCount)",
                fontSize = 12.sp,
                color = if (showOverdueOnly) ClayColors.RoseAccent else TextInkPrimary,
                fontWeight = if (showOverdueOnly) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ClayColors.RoseSurface,
              selectedLabelColor = ClayColors.RoseAccent
            )
          )
        }
        item {
          FilterChip(
            selected = selectedStatusFilter == null && !showOverdueOnly,
            onClick = {
              selectedStatusFilter = null
              showOverdueOnly = false
            },
            label = { Text("All Status", fontSize = 12.sp) }
          )
        }
        items(StudentStatus.values()) { status ->
          FilterChip(
            selected = selectedStatusFilter == status,
            onClick = {
              selectedStatusFilter = if (selectedStatusFilter == status) null else status
            },
            label = { Text(status.label, fontSize = 12.sp) }
          )
        }
      }

      // Filter Chips: Class
      if (classes.isNotEmpty()) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(bottom = 8.dp)
        ) {
          item {
            FilterChip(
              selected = selectedClassFilter == null,
              onClick = { selectedClassFilter = null },
              label = { Text("All Classes", fontSize = 12.sp) }
            )
          }
          items(classes) { cls ->
            FilterChip(
              selected = selectedClassFilter == cls,
              onClick = {
                selectedClassFilter = if (selectedClassFilter == cls) null else cls
              },
              label = { Text(cls, fontSize = 12.sp) }
            )
          }
        }
      }

      // Student Cards List fetched from Room Database
      if (filteredStudents.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.Default.PersonSearch,
              contentDescription = null,
              modifier = Modifier.size(52.dp),
              tint = TextSecondaryMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (searchQuery.isNotBlank() || selectedStatusFilter != null) "No matching students found." else "No students enrolled yet.",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = TextInkPrimary
            )
            Text(
              text = "Tap the 'Add Student' button to enroll a new student.",
              fontSize = 13.sp,
              color = TextSecondaryMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onNavigateToAddStudent,
              colors = ButtonDefaults.buttonColors(containerColor = DeepTealPrimary),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Student")
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredStudents, key = { it.id }) { student ->
            StudentItemCard(
              student = student,
              currencySymbol = profile.currencySymbol,
              onCardClick = { onNavigateToDetail(student.id) },
              onOpenRecordPaymentDialog = {
                studentForPaymentDialog = student
              },
              onOpenReminderDialog = {
                studentForReminderDialog = student
              }
            )
          }
        }
      }
    }
  }

  // Record Payment Dialog to update student's status for the current month
  if (studentForPaymentDialog != null) {
    RecordPaymentDialog(
      student = studentForPaymentDialog!!,
      currencySymbol = profile.currencySymbol,
      onDismiss = { studentForPaymentDialog = null },
      onConfirmPayment = { amount, method, date, reference, notes ->
        viewModel.recordStudentPayment(
          studentId = studentForPaymentDialog!!.id,
          amount = amount,
          paymentMethod = method,
          paymentDate = date,
          transactionReference = reference,
          notes = notes
        )
        studentForPaymentDialog = null
      }
    )
  }

  // 1-Tap Fee Reminder Dialog
  if (studentForReminderDialog != null) {
    SendReminderDialog(
      student = studentForReminderDialog!!,
      feeRecord = null,
      profile = profile,
      onDismiss = { studentForReminderDialog = null },
      onReminderSent = { channel ->
        viewModel.sendFeeReminder(studentForReminderDialog!!.id, null, channel)
      }
    )
  }
}

@Composable
private fun StudentItemCard(
  student: Student,
  currencySymbol: String,
  onCardClick: () -> Unit,
  onOpenRecordPaymentDialog: () -> Unit,
  onOpenReminderDialog: () -> Unit
) {
  val isOverdue = student.currentMonthStatus == FeeStatus.OVERDUE

  ClayCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    backgroundColor = if (isOverdue) Color(0xFFFFF7F6) else ClayColors.CardWhite,
    borderWidth = if (isOverdue) 1.8.dp else 1.5.dp,
    highlightColor = if (isOverdue) ClayColors.RoseBorder else Color.White.copy(alpha = 0.85f),
    elevation = if (isOverdue) 6.dp else 5.dp,
    onClick = onCardClick
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = student.name,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = TextInkPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            StudentStatusBadge(status = student.status)
          }

          Text(
            text = "${student.studentId} • ${student.studentClass} • ${student.batch}",
            fontSize = 12.sp,
            color = TextSecondaryMuted
          )

          if (student.parentContact.isNotBlank()) {
            Text(
              text = "Parent: ${student.parentContact}",
              fontSize = 11.sp,
              color = TextSecondaryMuted
            )
          }

          if (student.lastReminderSentAt != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "🔔 Reminded ${ReminderUtils.formatRelativeTime(student.lastReminderSentAt)}",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = ClayColors.TealAccent
            )
          }
        }

        // Current Month Status Badge
        FeeStatusBadge(status = student.currentMonthStatus)
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
            text = "Monthly: ${FormatUtils.formatCurrency(student.monthlyFeeAmount, currencySymbol)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOverdue) ClayColors.RoseAccent else DeepTealPrimary
          )
          if (student.advanceBalance > 0) {
            Text(
              text = "Advance: ${FormatUtils.formatCurrency(student.advanceBalance, currencySymbol)}",
              fontSize = 11.sp,
              color = StatusAdvance,
              fontWeight = FontWeight.SemiBold
            )
          }
          if (student.paymentHistory.isNotEmpty()) {
            Text(
              text = "${student.paymentHistory.size} payments on record",
              fontSize = 11.sp,
              color = StatusPaid,
              fontWeight = FontWeight.Medium
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          // 1-Tap Remind button if student has overdue fees
          if (isOverdue) {
            ClayButton(
              onClick = onOpenReminderDialog,
              shape = RoundedCornerShape(12.dp),
              containerColor = ClayColors.RoseAccent,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          ClayButton(
            onClick = onOpenRecordPaymentDialog,
            shape = RoundedCornerShape(12.dp),
            containerColor = DeepTealPrimary,
            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          ClaySecondaryButton(
            onClick = onCardClick,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = ClayColors.CardMuted,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text("Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextInkPrimary)
          }
        }
      }
    }
  }
}
