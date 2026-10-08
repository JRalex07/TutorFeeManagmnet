package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Payment
import com.example.data.model.PaymentMethod
import com.example.data.model.Student
import com.example.data.model.StudentStatus
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TuitionViewModel
import com.example.util.DateUtils
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectFeeScreen(
  viewModel: TuitionViewModel,
  preselectedStudentId: String?,
  onNavigateBack: () -> Unit,
  onPaymentSuccess: (Payment) -> Unit
) {
  val students by viewModel.students.collectAsState()
  val feeRecords by viewModel.feeRecords.collectAsState()
  val profile by viewModel.tuitionProfile.collectAsState()

  var selectedStudent by remember {
    mutableStateOf(students.find { it.id == preselectedStudentId } ?: students.firstOrNull { it.status == StudentStatus.ACTIVE })
  }

  var isStudentDropdownExpanded by remember { mutableStateOf(false) }
  var studentSearchInPicker by remember { mutableStateOf("") }

  // Target period selection: null = Auto (Oldest pending first), or specific period string
  var selectedTargetPeriod by remember { mutableStateOf<String?>(null) }
  var isPeriodDropdownExpanded by remember { mutableStateOf(false) }

  var amountStr by remember { mutableStateOf("") }
  var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var transactionRef by remember { mutableStateOf("") }
  var paymentDate by remember { mutableStateOf(DateUtils.currentDateString()) }
  var notes by remember { mutableStateOf("") }
  var treatExcessAsAdvance by remember { mutableStateOf(true) }

  var isSubmitting by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val scrollState = rememberScrollState()

  // Outstanding records for selected student
  val studentPendingRecords = remember(selectedStudent, feeRecords) {
    if (selectedStudent == null) emptyList()
    else feeRecords.filter { it.studentId == selectedStudent!!.id && it.remainingAmount > 0 }.sortedBy { it.feePeriod }
  }

  val totalPendingForStudent = remember(studentPendingRecords) {
    studentPendingRecords.sumOf { it.remainingAmount }
  }

  // Pre-fill amount when student or target period changes
  LaunchedEffect(selectedStudent, selectedTargetPeriod) {
    if (selectedStudent != null) {
      if (selectedTargetPeriod != null) {
        val rec = studentPendingRecords.find { it.feePeriod == selectedTargetPeriod }
        val dueAmt = rec?.remainingAmount ?: selectedStudent!!.monthlyFee
        amountStr = dueAmt.toInt().toString()
      } else {
        val totalDue = if (totalPendingForStudent > 0) totalPendingForStudent else selectedStudent!!.monthlyFee
        amountStr = totalDue.toInt().toString()
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Collect Tuition Fee", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        .verticalScroll(scrollState)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      if (validationError != null) {
        ClayCard(
          shape = RoundedCornerShape(16.dp),
          backgroundColor = ClayColors.RoseSurface,
          elevation = 2.dp
        ) {
          Text(
            text = validationError ?: "",
            color = ClayColors.RoseAccent,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Step 1: Select Student Card
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 5.dp
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("1. Select Student", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepTealPrimary)

          ExposedDropdownMenuBox(
            expanded = isStudentDropdownExpanded,
            onExpandedChange = { isStudentDropdownExpanded = it }
          ) {
            OutlinedTextField(
              value = selectedStudent?.let { "${it.fullName} (${it.studentId} - ${it.studentClass})" } ?: "Select student...",
              onValueChange = {},
              readOnly = true,
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStudentDropdownExpanded) },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
              shape = RoundedCornerShape(10.dp)
            )

            ExposedDropdownMenu(
              expanded = isStudentDropdownExpanded,
              onDismissRequest = { isStudentDropdownExpanded = false }
            ) {
              students.forEach { st ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(st.fullName, fontWeight = FontWeight.SemiBold)
                      Text("${st.studentId} • ${st.studentClass} • Monthly ₹${st.monthlyFee.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  },
                  onClick = {
                    selectedStudent = st
                    isStudentDropdownExpanded = false
                  }
                )
              }
            }
          }

          // Student Financial Health Banner
          if (selectedStudent != null) {
            val st = selectedStudent!!
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Total Pending / Arrears", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = FormatUtils.formatCurrency(totalPendingForStudent, profile.currencySymbol),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (totalPendingForStudent > 0) StatusOverdue else StatusPaid
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("Advance Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = FormatUtils.formatCurrency(st.advanceBalance, profile.currencySymbol),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StatusAdvance
                  )
                }
              }
            }
          }
        }
      }

      // Step 2: Fee Period & Payment Allocation
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 5.dp
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("2. Fee Period Allocation", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepTealPrimary)

          ExposedDropdownMenuBox(
            expanded = isPeriodDropdownExpanded,
            onExpandedChange = { isPeriodDropdownExpanded = it }
          ) {
            val periodText = if (selectedTargetPeriod == null) {
              "Auto: Settle Oldest Pending Fees First"
            } else {
              DateUtils.formatDisplayPeriod(selectedTargetPeriod!!)
            }

            OutlinedTextField(
              value = periodText,
              onValueChange = {},
              readOnly = true,
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPeriodDropdownExpanded) },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
              shape = RoundedCornerShape(10.dp)
            )

            ExposedDropdownMenu(
              expanded = isPeriodDropdownExpanded,
              onDismissRequest = { isPeriodDropdownExpanded = false }
            ) {
              DropdownMenuItem(
                text = { Text("Auto: Settle Oldest Pending Fees First (Recommended)", fontWeight = FontWeight.SemiBold) },
                onClick = {
                  selectedTargetPeriod = null
                  isPeriodDropdownExpanded = false
                }
              )
              studentPendingRecords.forEach { rec ->
                DropdownMenuItem(
                  text = {
                    Text("${DateUtils.formatDisplayPeriod(rec.feePeriod)} - Due: ${FormatUtils.formatCurrency(rec.remainingAmount, profile.currencySymbol)}")
                  },
                  onClick = {
                    selectedTargetPeriod = rec.feePeriod
                    isPeriodDropdownExpanded = false
                  }
                )
              }
              // Upcoming / future months option
              val nextP = DateUtils.nextPeriod(DateUtils.currentPeriod())
              DropdownMenuItem(
                text = { Text("Advance: ${DateUtils.formatDisplayPeriod(nextP)}") },
                onClick = {
                  selectedTargetPeriod = nextP
                  isPeriodDropdownExpanded = false
                }
              )
            }
          }
        }
      }

      // Step 3: Amount & Payment Details
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 5.dp
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("3. Payment Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepTealPrimary)

          OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it },
            label = { Text("Amount Received (₹) *") },
            leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
          )

          // Quick Amount Chips
          val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val chips = listOf(500.0, 1000.0, 1500.0, 2000.0)
            chips.forEach { chipAmt ->
              AssistChip(
                onClick = { amountStr = chipAmt.toInt().toString() },
                label = { Text("₹${chipAmt.toInt()}", fontSize = 11.sp) }
              )
            }
            if (totalPendingForStudent > 0) {
              AssistChip(
                onClick = { amountStr = totalPendingForStudent.toInt().toString() },
                label = { Text("Full Due", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
              )
            }
          }

          // Partial or Advance Explanation banner
          if (enteredAmount > 0 && selectedStudent != null) {
            if (enteredAmount < totalPendingForStudent) {
              val rem = totalPendingForStudent - enteredAmount
              Text(
                text = "⚡ Partial Payment: Student will have ${FormatUtils.formatCurrency(rem, profile.currencySymbol)} remaining pending.",
                fontSize = 12.sp,
                color = DeepTealPrimary,
                fontWeight = FontWeight.Medium
              )
            } else if (enteredAmount > totalPendingForStudent && totalPendingForStudent > 0) {
              val excess = enteredAmount - totalPendingForStudent
              Text(
                text = "⚡ Advance Payment: Excess ${FormatUtils.formatCurrency(excess, profile.currencySymbol)} will be credited to Advance Balance.",
                fontSize = 12.sp,
                color = StatusAdvance,
                fontWeight = FontWeight.Medium
              )
            }
          }

          // Payment Method Selector
          Text("Payment Method *", style = MaterialTheme.typography.labelSmall)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            PaymentMethod.values().take(4).forEach { method ->
              FilterChip(
                selected = paymentMethod == method,
                onClick = { paymentMethod = method },
                label = { Text(method.label, fontSize = 12.sp) }
              )
            }
          }

          OutlinedTextField(
            value = transactionRef,
            onValueChange = { transactionRef = it },
            label = { Text("Transaction Reference / UPI Ref (Optional)") },
            placeholder = { Text("e.g. UPI/39102910 or Cheque #129") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = paymentDate,
              onValueChange = { paymentDate = it },
              label = { Text("Payment Date") },
              leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f)
            )
          }

          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes / Remarks (Optional)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // Submit Button with Duplicate Protection
      ClayButton(
        onClick = {
          if (selectedStudent == null) {
            validationError = "Please select a student."
            return@ClayButton
          }
          val amt = amountStr.toDoubleOrNull()
          if (amt == null || amt <= 0) {
            validationError = "Please enter a valid amount greater than zero."
            return@ClayButton
          }

          isSubmitting = true
          validationError = null

          viewModel.collectFee(
            studentId = selectedStudent!!.id,
            amount = amt,
            paymentDate = paymentDate.trim(),
            paymentMethod = paymentMethod,
            transactionReference = transactionRef.trim(),
            targetPeriod = selectedTargetPeriod,
            notes = notes.trim(),
            treatExcessAsAdvance = treatExcessAsAdvance
          ) { payment ->
            isSubmitting = false
            onPaymentSuccess(payment)
          }
        },
        enabled = !isSubmitting && selectedStudent != null,
        modifier = Modifier.fillMaxWidth(),
        containerColor = DeepTealPrimary,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(vertical = 15.dp)
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(22.dp))
        } else {
          Icon(Icons.Default.Receipt, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Record Payment & Generate Receipt", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }
    }
  }
}
