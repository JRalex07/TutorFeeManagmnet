package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.TuitionRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TuitionViewModel(application: Application) : AndroidViewModel(application) {

  val repository = TuitionRepository(application.applicationContext)

  val students = repository.students
  val feeRecords = repository.feeRecords
  val payments = repository.payments
  val refunds = repository.refunds
  val auditLogs = repository.auditLogs
  val tuitionProfile = repository.tuitionProfile
  val isLoading = repository.isLoading
  val isFirestoreConnected = repository.isFirestoreConnected

  private val _uiMessage = MutableSharedFlow<String>()
  val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

  // Selected student for Profile / Detail Screen
  private val _selectedStudentId = MutableStateFlow<String?>(null)
  val selectedStudentId: StateFlow<String?> = _selectedStudentId.asStateFlow()

  // Selected payment for Receipt View / Print
  private val _selectedReceiptPayment = MutableStateFlow<Payment?>(null)
  val selectedReceiptPayment: StateFlow<Payment?> = _selectedReceiptPayment.asStateFlow()

  // Search & Filters for Student List
  val studentSearchQuery = MutableStateFlow("")
  val studentFilterStatus = MutableStateFlow<StudentStatus?>(null)
  val studentFilterClass = MutableStateFlow<String?>(null)

  // Search & Filters for Pending Fees Screen
  val pendingSearchQuery = MutableStateFlow("")
  val pendingFilterPeriod = MutableStateFlow<String?>(null)
  val pendingFilterOnlyOverdue = MutableStateFlow(false)
  val pendingFilterOnlyPartial = MutableStateFlow(false)
  val pendingSortByOldest = MutableStateFlow(true)

  // Search & Filters for Payments Screen
  val paymentsSearchQuery = MutableStateFlow("")
  val paymentsDateFilter = MutableStateFlow("ALL") // "TODAY", "THIS_WEEK", "THIS_MONTH", "ALL"
  val paymentsMethodFilter = MutableStateFlow<PaymentMethod?>(null)

  // Real-time Dashboard Stats calculation
  val dashboardStats: StateFlow<DashboardStats> = combine(
    students,
    feeRecords,
    payments
  ) { stuList, feeList, payList ->
    val currentPeriod = DateUtils.currentPeriod()
    val activeStudents = stuList.filter { it.status == StudentStatus.ACTIVE }
    val inactiveStudents = stuList.filter { it.status != StudentStatus.ACTIVE }

    val currentMonthRecords = feeList.filter { it.feePeriod == currentPeriod }
    val expectedFeeThisMonth = currentMonthRecords.sumOf { it.netFee }
    val collectedThisMonth = currentMonthRecords.sumOf { it.paidAmount }
    val pendingThisMonth = currentMonthRecords.sumOf { it.remainingAmount }

    val allOverdueRecords = feeList.filter {
      it.status == FeeStatus.OVERDUE || (it.remainingAmount > 0 && DateUtils.isOverdue(it.dueDate))
    }
    val overdueAmount = allOverdueRecords.sumOf { it.remainingAmount }
    val advanceAmountTotal = stuList.sumOf { it.advanceBalance }

    val partialPaymentsCount = feeList.count { it.status == FeeStatus.PARTIALLY_PAID }

    // Today / Week / Month payments
    val validPayments = payList.filter { it.status == PaymentRecordStatus.COMPLETED }
    val paymentsToday = validPayments.filter { DateUtils.isToday(it.paymentDate) }
    val paymentsWeek = validPayments.filter { DateUtils.isThisWeek(it.paymentDate) }
    val paymentsMonth = validPayments.filter { DateUtils.isThisMonth(it.paymentDate) }

    // Student statuses for current month
    val paidStudentsCount = currentMonthRecords.count { it.status == FeeStatus.PAID }
    val partiallyPaidStudentsCount = currentMonthRecords.count { it.status == FeeStatus.PARTIALLY_PAID }
    val pendingStudentsCount = currentMonthRecords.count {
      it.remainingAmount > 0 && it.status != FeeStatus.PAID
    }
    val overdueStudentsCount = allOverdueRecords.map { it.studentId }.distinct().count()

    DashboardStats(
      totalStudents = stuList.size,
      activeStudents = activeStudents.size,
      inactiveStudents = inactiveStudents.size,
      newStudentsThisMonth = stuList.count { it.joiningDate.startsWith(currentPeriod) },
      expectedFeeThisMonth = expectedFeeThisMonth,
      collectedThisMonth = collectedThisMonth,
      pendingThisMonth = pendingThisMonth,
      overdueAmount = overdueAmount,
      advanceAmountTotal = advanceAmountTotal,
      partialPaymentsCount = partialPaymentsCount,
      paymentsTodayAmount = paymentsToday.sumOf { it.amount },
      paymentsTodayCount = paymentsToday.size,
      paymentsThisWeekAmount = paymentsWeek.sumOf { it.amount },
      paymentsThisMonthAmount = paymentsMonth.sumOf { it.amount },
      paidStudentsCount = paidStudentsCount,
      pendingStudentsCount = pendingStudentsCount,
      partiallyPaidStudentsCount = partiallyPaidStudentsCount,
      overdueStudentsCount = overdueStudentsCount
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    DashboardStats()
  )

  // Financial summary for selected student
  fun getStudentFinancialSummary(studentId: String): StudentFinancialSummary? {
    val student = students.value.find { it.id == studentId } ?: return null
    val currentPeriod = DateUtils.currentPeriod()
    val studentRecords = feeRecords.value.filter { it.studentId == studentId }
    val curRec = studentRecords.find { it.feePeriod == currentPeriod }

    val totalFeeDue = studentRecords.sumOf { it.netFee }
    val totalPaid = studentRecords.sumOf { it.paidAmount }
    val totalPending = studentRecords.sumOf { it.remainingAmount }
    val arrears = studentRecords.filter { it.feePeriod < currentPeriod }.sumOf { it.remainingAmount }

    return StudentFinancialSummary(
      student = student,
      totalFeeDueAllTime = totalFeeDue,
      totalPaidAllTime = totalPaid,
      totalPendingAllTime = totalPending,
      currentMonthDue = curRec?.netFee ?: student.monthlyFee,
      currentMonthPaid = curRec?.paidAmount ?: 0.0,
      currentMonthRemaining = curRec?.remainingAmount ?: student.monthlyFee,
      currentMonthStatus = curRec?.status ?: FeeStatus.DUE,
      advanceBalance = student.advanceBalance,
      totalArrears = arrears
    )
  }

  fun selectStudent(studentId: String?) {
    _selectedStudentId.value = studentId
  }

  fun selectReceiptPayment(payment: Payment?) {
    _selectedReceiptPayment.value = payment
  }

  fun recordStudentPayment(
    studentId: String,
    amount: Double,
    paymentMethod: PaymentMethod,
    paymentDate: String,
    transactionReference: String,
    notes: String,
    onSuccess: (Payment) -> Unit = {}
  ) {
    collectFee(
      studentId = studentId,
      amount = amount,
      paymentDate = paymentDate,
      paymentMethod = paymentMethod,
      transactionReference = transactionReference,
      targetPeriod = null,
      notes = notes,
      treatExcessAsAdvance = true,
      onSuccess = onSuccess
    )
  }

  fun saveStudent(student: Student, onComplete: () -> Unit) {
    viewModelScope.launch {
      val res = if (student.id.isBlank()) {
        repository.addStudent(student)
      } else {
        repository.updateStudent(student)
      }
      res.onSuccess {
        _uiMessage.emit("Student ${it.fullName} saved successfully!")
        onComplete()
      }.onFailure {
        _uiMessage.emit("Failed to save student: ${it.message}")
      }
    }
  }

  fun collectFee(
    studentId: String,
    amount: Double,
    paymentDate: String,
    paymentMethod: PaymentMethod,
    transactionReference: String,
    targetPeriod: String?,
    notes: String,
    treatExcessAsAdvance: Boolean,
    onSuccess: (Payment) -> Unit
  ) {
    viewModelScope.launch {
      val res = repository.collectFee(
        studentId = studentId,
        amount = amount,
        paymentDate = paymentDate,
        paymentMethod = paymentMethod,
        transactionReference = transactionReference,
        targetPeriod = targetPeriod,
        notes = notes,
        treatExcessAsAdvance = treatExcessAsAdvance
      )
      res.onSuccess { p ->
        _selectedReceiptPayment.value = p
        _uiMessage.emit("Payment of ₹${p.amount} recorded for ${p.studentName}")
        onSuccess(p)
      }.onFailure {
        _uiMessage.emit(it.message ?: "Failed to collect fee.")
      }
    }
  }

  fun cancelPayment(paymentId: String, reason: String) {
    viewModelScope.launch {
      val res = repository.cancelPayment(paymentId, reason)
      res.onSuccess {
        _uiMessage.emit("Payment cancelled and amounts restored successfully.")
      }.onFailure {
        _uiMessage.emit("Failed to cancel payment: ${it.message}")
      }
    }
  }

  fun issueRefund(studentId: String, amount: Double, method: PaymentMethod, reason: String) {
    viewModelScope.launch {
      val res = repository.issueRefund(studentId, amount, method, reason)
      res.onSuccess {
        _uiMessage.emit("Refund of ₹${it.amount} processed successfully.")
      }.onFailure {
        _uiMessage.emit("Failed to issue refund: ${it.message}")
      }
    }
  }

  fun waiveFee(feeRecordId: String, reason: String) {
    viewModelScope.launch {
      val res = repository.waiveFee(feeRecordId, reason)
      res.onSuccess {
        _uiMessage.emit("Fee waived successfully.")
      }.onFailure {
        _uiMessage.emit("Failed to waive fee: ${it.message}")
      }
    }
  }

  fun sendFeeReminder(
    studentId: String,
    feeRecordId: String?,
    channel: String,
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch {
      val res = repository.recordReminderSent(studentId, feeRecordId, channel)
      res.onSuccess {
        _uiMessage.emit("Reminder dispatched via $channel. Ledger updated!")
        onSuccess()
      }.onFailure {
        _uiMessage.emit("Failed to record reminder: ${it.message}")
      }
    }
  }

  fun markStudentLeaving(studentId: String, date: String, reason: String, settlementStatus: SettlementStatus) {
    viewModelScope.launch {
      val res = repository.markStudentLeaving(studentId, date, reason, settlementStatus)
      res.onSuccess {
        _uiMessage.emit("Student marked as left. Settlement status updated.")
      }.onFailure {
        _uiMessage.emit("Failed: ${it.message}")
      }
    }
  }

  fun deleteStudent(studentId: String, onComplete: () -> Unit) {
    viewModelScope.launch {
      val res = repository.deleteStudent(studentId)
      res.onSuccess {
        _uiMessage.emit("Student removed.")
        onComplete()
      }.onFailure {
        _uiMessage.emit("Failed to delete student: ${it.message}")
      }
    }
  }

  fun updateTuitionProfile(profile: TuitionProfile) {
    viewModelScope.launch {
      val res = repository.updateTuitionProfile(profile)
      res.onSuccess {
        _uiMessage.emit("Tuition settings updated.")
      }.onFailure {
        _uiMessage.emit("Failed to update settings: ${it.message}")
      }
    }
  }

  fun resetToSampleData() {
    viewModelScope.launch {
      val res = repository.resetToSampleData()
      res.onSuccess {
        _uiMessage.emit("Sample data loaded successfully.")
      }
    }
  }

  fun clearAllData() {
    viewModelScope.launch {
      val res = repository.clearAllData()
      res.onSuccess {
        _uiMessage.emit("Ledger cleared.")
      }
    }
  }
}
