package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StudentStatus(val label: String) {
  ACTIVE("Active"),
  INACTIVE("Inactive"),
  COMPLETED("Completed"),
  LEFT("Left Tuition"),
  SUSPENDED("Suspended")
}

enum class DiscountType(val label: String) {
  NONE("None"),
  FIXED("Fixed (₹)"),
  PERCENTAGE("Percentage (%)")
}

enum class FeeCycle(val label: String) {
  MONTHLY("Monthly"),
  QUARTERLY("Quarterly"),
  ANNUAL("Annual")
}

enum class FeeStatus(val label: String) {
  NOT_DUE("Not Due"),
  DUE("Due"),
  PENDING("Pending"),
  PARTIALLY_PAID("Partially Paid"),
  PAID("Paid"),
  OVERDUE("Overdue"),
  WAIVED("Waived"),
  CANCELLED("Cancelled"),
  ADVANCE_COVERED("Advance Covered")
}

enum class PaymentMethod(val label: String) {
  CASH("Cash"),
  UPI("UPI"),
  BANK_TRANSFER("Bank Transfer"),
  CHEQUE("Cheque"),
  OTHER("Other")
}

enum class PaymentRecordStatus(val label: String) {
  COMPLETED("Completed"),
  REVERSED("Reversed"),
  CANCELLED("Cancelled")
}

enum class SettlementStatus(val label: String) {
  FULLY_SETTLED("Fully Settled"),
  AMOUNT_PENDING("Amount Pending"),
  ADVANCE_REFUND_PENDING("Advance Refund Pending"),
  ADVANCE_ADJUSTED("Advance Adjusted")
}

@Entity(tableName = "students")
data class Student(
  @PrimaryKey
  val id: String = "",
  val name: String = "",
  val parentContact: String = "",
  val monthlyFeeAmount: Double = 1500.0,
  val studentId: String = "", // e.g., "STU-2026-001"
  val fatherName: String = "",
  val motherName: String = "",
  val dob: String = "",
  val gender: String = "",
  val phoneNumber: String = "",
  val address: String = "",
  val joiningDate: String = "",
  val status: StudentStatus = StudentStatus.ACTIVE,
  val notes: String = "",
  val feeCycle: FeeCycle = FeeCycle.MONTHLY,
  val discount: Double = 0.0,
  val discountType: DiscountType = DiscountType.NONE,
  val feeStartDate: String = "",
  val preferredPaymentDay: Int = 10,
  val advanceBalance: Double = 0.0,
  val studentClass: String = "Class 10",
  val section: String = "",
  val schoolName: String = "",
  val subjects: List<String> = emptyList(),
  val batch: String = "Evening Batch",
  val tuitionTiming: String = "5:00 PM - 7:00 PM",
  val leavingDate: String? = null,
  val leavingReason: String? = null,
  val settlementStatus: SettlementStatus? = null,
  val currentMonthStatus: FeeStatus = FeeStatus.DUE,
  val paymentHistory: List<Payment> = emptyList(),
  val lastReminderSentAt: Long? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
) {
  val fullName: String get() = name
  val parentPhone: String get() = parentContact
  val monthlyFee: Double get() = monthlyFeeAmount
  val displaySubjects: String get() = if (subjects.isEmpty()) "General Tuition" else subjects.joinToString(", ")
}

data class FeeRecord(
  val id: String = "",
  val studentId: String = "",
  val studentName: String = "",
  val feePeriod: String = "", // e.g. "2026-09"
  val baseFee: Double = 0.0, // Historical snapshot; never modified when student current fee changes
  val discount: Double = 0.0,
  val discountReason: String? = null,
  val netFee: Double = 0.0, // baseFee - discount
  val paidAmount: Double = 0.0,
  val remainingAmount: Double = 0.0, // netFee - paidAmount
  val dueDate: String = "", // e.g. "2026-09-10"
  val status: FeeStatus = FeeStatus.DUE,
  val waiverReason: String? = null,
  val paymentIds: List<String> = emptyList(),
  val notes: String = "",
  val lastReminderSentAt: Long? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

data class Payment(
  val id: String = "",
  val receiptNumber: String = "",
  val studentId: String = "",
  val studentName: String = "",
  val studentClass: String = "",
  val amount: Double = 0.0,
  val paymentDate: String = "",
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val transactionReference: String = "",
  val allocatedFeePeriods: List<String> = emptyList(),
  val allocatedAmounts: Map<String, Double> = emptyMap(),
  val status: PaymentRecordStatus = PaymentRecordStatus.COMPLETED,
  val cancellationReason: String? = null,
  val cancelledAt: Long? = null,
  val notes: String = "",
  val balanceAfterPayment: Double = 0.0,
  val createdAt: Long = System.currentTimeMillis()
)

data class Refund(
  val id: String = "",
  val studentId: String = "",
  val studentName: String = "",
  val amount: Double = 0.0,
  val refundDate: String = "",
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val reason: String = "",
  val createdAt: Long = System.currentTimeMillis()
)

data class FeeChange(
  val id: String = "",
  val studentId: String = "",
  val oldFee: Double = 0.0,
  val newFee: Double = 0.0,
  val effectiveFrom: String = "", // e.g., "2026-10"
  val reason: String = "",
  val createdAt: Long = System.currentTimeMillis()
)

data class AuditLog(
  val id: String = "",
  val action: String = "",
  val description: String = "",
  val studentId: String? = null,
  val entityId: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

data class TuitionProfile(
  val tuitionName: String = "Sharma Tuition Academy",
  val teacherName: String = "Er. Rajesh Sharma",
  val phone: String = "+91 98765 43210",
  val address: String = "Royal Palms, Sector 62, Noida",
  val upiId: String = "sharmatuition@okhdfcbank",
  val defaultMonthlyFee: Double = 1500.0,
  val defaultDueDay: Int = 10,
  val currencySymbol: String = "₹",
  val receiptFooterNote: String = "Thank you for your commitment to learning. Fees are acknowledged with gratitude."
)

data class DashboardStats(
  val totalStudents: Int = 0,
  val activeStudents: Int = 0,
  val inactiveStudents: Int = 0,
  val newStudentsThisMonth: Int = 0,
  val expectedFeeThisMonth: Double = 0.0,
  val collectedThisMonth: Double = 0.0,
  val pendingThisMonth: Double = 0.0,
  val overdueAmount: Double = 0.0,
  val advanceAmountTotal: Double = 0.0,
  val partialPaymentsCount: Int = 0,
  val paymentsTodayAmount: Double = 0.0,
  val paymentsTodayCount: Int = 0,
  val paymentsThisWeekAmount: Double = 0.0,
  val paymentsThisMonthAmount: Double = 0.0,
  val paidStudentsCount: Int = 0,
  val pendingStudentsCount: Int = 0,
  val partiallyPaidStudentsCount: Int = 0,
  val overdueStudentsCount: Int = 0
)

data class StudentFinancialSummary(
  val student: Student,
  val totalFeeDueAllTime: Double = 0.0,
  val totalPaidAllTime: Double = 0.0,
  val totalPendingAllTime: Double = 0.0,
  val currentMonthDue: Double = 0.0,
  val currentMonthPaid: Double = 0.0,
  val currentMonthRemaining: Double = 0.0,
  val currentMonthStatus: FeeStatus = FeeStatus.DUE,
  val advanceBalance: Double = 0.0,
  val totalArrears: Double = 0.0
)
