package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import com.example.data.model.FeeRecord
import com.example.data.model.FeeStatus
import com.example.data.model.Student
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TuitionViewModel
import com.example.util.DateUtils
import com.example.util.FormatUtils
import com.example.util.ReminderUtils

@Composable
fun DashboardScreen(
  viewModel: TuitionViewModel,
  onNavigateToStudents: () -> Unit,
  onNavigateToAddStudent: () -> Unit,
  onNavigateToCollectFee: (String?) -> Unit,
  onNavigateToPendingFees: () -> Unit,
  onNavigateToPayments: () -> Unit,
  onNavigateToReports: () -> Unit,
  onNavigateToSettings: () -> Unit
) {
  val stats by viewModel.dashboardStats.collectAsState()
  val profile by viewModel.tuitionProfile.collectAsState()
  val payments by viewModel.payments.collectAsState()
  val students by viewModel.students.collectAsState()
  val feeRecords by viewModel.feeRecords.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()

  val currentPeriod = DateUtils.currentPeriod()
  val displayPeriod = DateUtils.formatDisplayPeriod(currentPeriod)

  // Direct 1-tap record payment dialog
  var studentForQuickPayment by remember { mutableStateOf<Student?>(null) }
  var studentForReminder by remember { mutableStateOf<Pair<Student, FeeRecord?>?>(null) }

  // Overdue students list with their fee record for instant follow-up
  val overdueStudentsWithRecords = remember(students, feeRecords) {
    feeRecords
      .filter { it.remainingAmount > 0 && (it.status == FeeStatus.OVERDUE || DateUtils.isOverdue(it.dueDate)) }
      .mapNotNull { rec ->
        val s = students.find { it.id == rec.studentId }
        if (s != null) s to rec else null
      }
      .distinctBy { it.first.id }
  }

  val collectionRate = if (stats.expectedFeeThisMonth > 0) {
    ((stats.collectedThisMonth / stats.expectedFeeThisMonth) * 100).toInt().coerceIn(0, 100)
  } else 0

  // Actionable receivables: students with pending or overdue fee this month
  val pendingActionStudents = remember(students, feeRecords, currentPeriod) {
    students.filter { student ->
      val rec = feeRecords.find { it.studentId == student.id && it.feePeriod == currentPeriod }
      val isUnpaid = rec != null && rec.status in listOf(FeeStatus.DUE, FeeStatus.OVERDUE, FeeStatus.PARTIALLY_PAID)
      isUnpaid || student.currentMonthStatus in listOf(FeeStatus.DUE, FeeStatus.OVERDUE, FeeStatus.PARTIALLY_PAID)
    }.take(4)
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(ClayColors.CanvasBg),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Inflated Claymorphic Header
    item {
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 7.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = profile.tuitionName.ifBlank { "Tutor Cloud Ledger" },
                fontWeight = FontWeight.Black,
                color = TextInkPrimary,
                fontSize = 22.sp
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${profile.teacherName.ifBlank { "Private Tutor" }} • $displayPeriod",
                color = TextSecondaryMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
            }

            ClayIconTile(
              icon = Icons.Default.Settings,
              tint = DeepTealPrimary,
              backgroundColor = ClayColors.TealSurface,
              size = 42.dp,
              shape = RoundedCornerShape(14.dp)
            )
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Action Buttons: Claymorphic Collect Fee & Add Student
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            ClayButton(
              onClick = { onNavigateToCollectFee(null) },
              modifier = Modifier.weight(1.3f),
              containerColor = DeepTealPrimary,
              shape = RoundedCornerShape(18.dp)
            ) {
              Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Collect Fee", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            ClaySecondaryButton(
              onClick = onNavigateToAddStudent,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(18.dp),
              backgroundColor = ClayColors.CardMuted
            ) {
              Icon(Icons.Default.PersonAdd, contentDescription = null, tint = DeepTealPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Student", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepTealPrimary)
            }
          }
        }
      }
    }

    // 2. Overdue Attention Banner if needed
    if (stats.overdueAmount > 0) {
      item {
        ClayCard(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToPendingFees() },
          shape = RoundedCornerShape(20.dp),
          backgroundColor = ClayColors.RoseSurface,
          elevation = 4.dp
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              ClayIconTile(
                icon = Icons.Default.Warning,
                tint = Color.White,
                backgroundColor = ClayColors.RoseAccent,
                size = 36.dp,
                shape = RoundedCornerShape(10.dp)
              )
              Column {
                Text(
                  text = "Overdue Fees Follow-Up",
                  fontWeight = FontWeight.Bold,
                  color = ClayColors.RoseAccent,
                  fontSize = 14.sp
                )
                Text(
                  text = "${FormatUtils.formatCurrency(stats.overdueAmount, profile.currencySymbol)} across ${stats.overdueStudentsCount} student(s)",
                  fontSize = 12.sp,
                  color = TextSecondaryMuted
                )
              }
            }
            Text(
              text = "Review →",
              fontWeight = FontWeight.Bold,
              color = ClayColors.RoseAccent,
              fontSize = 13.sp
            )
          }
        }
      }
    }

    // 2b. Overdue Students Highlight & 1-Tap Reminders Section
    if (overdueStudentsWithRecords.isNotEmpty()) {
      item {
        ClayCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          backgroundColor = Color(0xFFFFF7F6),
          borderWidth = 1.8.dp,
          highlightColor = ClayColors.RoseBorder,
          elevation = 5.dp
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                ClayIconTile(
                  icon = Icons.Default.NotificationsActive,
                  tint = Color.White,
                  backgroundColor = ClayColors.RoseAccent,
                  size = 32.dp,
                  shape = RoundedCornerShape(10.dp)
                )
                Text(
                  text = "Overdue Fee Alerts",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextInkPrimary
                )
              }

              ClayBadge(
                text = "${overdueStudentsWithRecords.size} Actionable",
                backgroundColor = ClayColors.RoseSurface,
                textColor = ClayColors.RoseAccent,
                borderColor = ClayColors.RoseBorder
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            overdueStudentsWithRecords.take(3).forEach { (student, rec) ->
              val daysOverdue = if (rec.dueDate.isNotBlank()) ReminderUtils.calculateDaysOverdue(rec.dueDate) else 0L

              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, ClayColors.RoseBorder.copy(alpha = 0.5f))
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = student.name,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = TextInkPrimary
                    )
                    Text(
                      text = "${FormatUtils.formatCurrency(rec.remainingAmount, profile.currencySymbol)} • ${if (daysOverdue > 0) "$daysOverdue days overdue" else "Past due"}",
                      fontSize = 12.sp,
                      color = ClayColors.RoseAccent,
                      fontWeight = FontWeight.SemiBold
                    )
                    val reminderTime = rec.lastReminderSentAt ?: student.lastReminderSentAt
                    if (reminderTime != null) {
                      Text(
                        text = "🔔 Reminded ${ReminderUtils.formatRelativeTime(reminderTime)}",
                        fontSize = 10.sp,
                        color = ClayColors.TealAccent,
                        fontWeight = FontWeight.Medium
                      )
                    }
                  }

                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 1-Tap Remind
                    ClayButton(
                      onClick = { studentForReminder = student to rec },
                      shape = RoundedCornerShape(10.dp),
                      containerColor = ClayColors.RoseAccent,
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                      Spacer(modifier = Modifier.width(3.dp))
                      Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // 1-Tap Quick Payment
                    ClayButton(
                      onClick = { studentForQuickPayment = student },
                      shape = RoundedCornerShape(10.dp),
                      containerColor = DeepTealPrimary,
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text("Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }

            if (overdueStudentsWithRecords.size > 3) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "View all ${overdueStudentsWithRecords.size} overdue students →",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ClayColors.RoseAccent,
                modifier = Modifier
                  .align(Alignment.End)
                  .clickable { onNavigateToPendingFees() }
                  .padding(top = 4.dp)
              )
            }
          }
        }
      }
    }

    // 3. Empty State or Actionable Receivables
    if (students.isEmpty() && !isLoading) {
      item {
        ClayCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(26.dp),
          backgroundColor = ClayColors.CardWhite,
          elevation = 6.dp
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            ClayIconTile(
              icon = Icons.Default.School,
              tint = DeepTealPrimary,
              backgroundColor = ClayColors.TealSurface,
              size = 64.dp,
              shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Your Cloud Ledger is Ready!",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = TextInkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Connected live to Firebase Firestore. Add your first student to begin logging monthly fee collections, dues, and payment receipts.",
              fontSize = 13.sp,
              color = TextSecondaryMuted,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            ClayButton(
              onClick = onNavigateToAddStudent,
              containerColor = DeepTealPrimary
            ) {
              Icon(Icons.Default.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add First Student", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else if (pendingActionStudents.isNotEmpty()) {
      item {
        ClayCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          backgroundColor = ClayColors.CardWhite,
          elevation = 5.dp
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "ACTIONABLE DUES",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  color = DeepTealPrimary,
                  letterSpacing = 0.5.sp
                )
                Text(
                  text = "Uncollected Tuition ($displayPeriod)",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextInkPrimary
                )
              }
              Text(
                text = "View All →",
                fontSize = 12.sp,
                color = DeepTealPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToPendingFees() }
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            pendingActionStudents.forEachIndexed { index, student ->
              val rec = feeRecords.find { it.studentId == student.id && it.feePeriod == currentPeriod }
              val pendingAmount = rec?.remainingAmount ?: student.monthlyFeeAmount
              val status = rec?.status ?: student.currentMonthStatus

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = student.name,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = TextInkPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ClayBadge(
                      text = status.label,
                      backgroundColor = if (status == FeeStatus.OVERDUE) ClayColors.RoseSurface else ClayColors.AmberSurface,
                      textColor = if (status == FeeStatus.OVERDUE) ClayColors.RoseAccent else ClayColors.AmberAccent,
                      borderColor = if (status == FeeStatus.OVERDUE) ClayColors.RoseBorder else ClayColors.AmberBorder
                    )
                  }
                  Text(
                    text = "${student.studentClass} • Due: ${FormatUtils.formatCurrency(pendingAmount, profile.currencySymbol)}",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                  )
                }

                ClayButton(
                  onClick = { studentForQuickPayment = student },
                  containerColor = DeepTealPrimary,
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Collect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }

              if (index < pendingActionStudents.size - 1) {
                HorizontalDivider(color = BorderWarmGray.copy(alpha = 0.5f))
              }
            }
          }
        }
      }
    }

    // 4. Monthly Progress Clay Card
    item {
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 5.dp
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "COLLECTION PROGRESS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = TextSecondaryMuted,
                letterSpacing = 0.5.sp
              )
              Text(
                text = "${FormatUtils.formatCurrency(stats.collectedThisMonth, profile.currencySymbol)} of ${FormatUtils.formatCurrency(stats.expectedFeeThisMonth, profile.currencySymbol)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextInkPrimary
              )
            }
            ClayBadge(
              text = "$collectionRate% Realized",
              backgroundColor = if (collectionRate >= 75) ClayColors.GreenSurface else ClayColors.AmberSurface,
              textColor = if (collectionRate >= 75) ClayColors.GreenAccent else ClayColors.AmberAccent,
              borderColor = if (collectionRate >= 75) ClayColors.GreenBorder else ClayColors.AmberBorder
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          LinearProgressIndicator(
            progress = { (collectionRate / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = DeepTealPrimary,
            trackColor = ClayColors.CardMuted
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Uncollected: ${FormatUtils.formatCurrency(stats.pendingThisMonth, profile.currencySymbol)}",
              fontSize = 12.sp,
              color = ClayColors.AmberAccent,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${stats.paidStudentsCount} of ${stats.activeStudents} students paid",
              fontSize = 12.sp,
              color = TextSecondaryMuted
            )
          }
        }
      }
    }

    // 5. Four Tactile Clay KPI Stat Cards
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ClayStatCard(
          title = "Collected",
          value = FormatUtils.formatCurrency(stats.collectedThisMonth, profile.currencySymbol),
          subtitle = "${stats.paidStudentsCount} settled",
          icon = Icons.Default.CheckCircle,
          cardBg = ClayColors.GreenSurface,
          accentColor = ClayColors.GreenAccent,
          modifier = Modifier.weight(1f),
          onClick = onNavigateToPayments
        )

        ClayStatCard(
          title = "Pending",
          value = FormatUtils.formatCurrency(stats.pendingThisMonth, profile.currencySymbol),
          subtitle = "${stats.pendingStudentsCount} students",
          icon = Icons.Default.HourglassBottom,
          cardBg = ClayColors.AmberSurface,
          accentColor = ClayColors.AmberAccent,
          modifier = Modifier.weight(1f),
          onClick = onNavigateToPendingFees
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ClayStatCard(
          title = "Overdue",
          value = FormatUtils.formatCurrency(stats.overdueAmount, profile.currencySymbol),
          subtitle = "Needs follow-up",
          icon = Icons.Default.Warning,
          cardBg = ClayColors.RoseSurface,
          accentColor = ClayColors.RoseAccent,
          modifier = Modifier.weight(1f),
          onClick = onNavigateToPendingFees
        )

        ClayStatCard(
          title = "Students",
          value = "${stats.activeStudents}",
          subtitle = "${stats.totalStudents} enrolled",
          icon = Icons.Default.People,
          cardBg = ClayColors.BlueSurface,
          accentColor = ClayColors.BlueAccent,
          modifier = Modifier.weight(1f),
          onClick = onNavigateToStudents
        )
      }
    }

    // 6. Recent Payments Section
    if (payments.isNotEmpty()) {
      item {
        ClayCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          backgroundColor = ClayColors.CardWhite,
          elevation = 5.dp
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Recent Transactions",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextInkPrimary
              )
              Text(
                text = "History →",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeepTealPrimary,
                modifier = Modifier.clickable { onNavigateToPayments() }
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            payments.take(3).forEachIndexed { idx, pay ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { viewModel.selectReceiptPayment(pay) }
                  .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  ClayIconTile(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    tint = DeepTealPrimary,
                    backgroundColor = ClayColors.TealSurface,
                    size = 36.dp
                  )
                  Column {
                    Text(
                      text = pay.studentName,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextInkPrimary
                    )
                    Text(
                      text = "${pay.receiptNumber} • ${pay.paymentMethod.label}",
                      fontSize = 11.sp,
                      color = TextSecondaryMuted
                    )
                  }
                }
                Text(
                  text = "+${FormatUtils.formatCurrency(pay.amount, profile.currencySymbol)}",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = ClayColors.GreenAccent
                )
              }
              if (idx < 2 && idx < payments.size - 1) {
                HorizontalDivider(color = BorderWarmGray.copy(alpha = 0.5f))
              }
            }
          }
        }
      }
    }
  }

  // 1-Tap Quick Payment Dialog
  if (studentForQuickPayment != null) {
    RecordPaymentDialog(
      student = studentForQuickPayment!!,
      currencySymbol = profile.currencySymbol,
      onDismiss = { studentForQuickPayment = null },
      onConfirmPayment = { amount, method, date, ref, notes ->
        viewModel.collectFee(
          studentId = studentForQuickPayment!!.id,
          amount = amount,
          paymentDate = date,
          paymentMethod = method,
          transactionReference = ref,
          targetPeriod = null,
          notes = notes,
          treatExcessAsAdvance = true,
          onSuccess = { payment ->
            studentForQuickPayment = null
            viewModel.selectReceiptPayment(payment)
          }
        )
      }
    )
  }

  // 1-Tap Fee Reminder Modal Dialog
  if (studentForReminder != null) {
    val (targetStudent, targetRec) = studentForReminder!!
    SendReminderDialog(
      student = targetStudent,
      feeRecord = targetRec,
      profile = profile,
      onDismiss = { studentForReminder = null },
      onReminderSent = { channel ->
        viewModel.sendFeeReminder(targetStudent.id, targetRec?.id, channel)
      }
    )
  }
}
