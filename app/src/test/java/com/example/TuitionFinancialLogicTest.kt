package com.example

import com.example.data.model.FeeStatus
import com.example.util.DateUtils
import com.example.util.FormatUtils
import org.junit.Assert.*
import org.junit.Test

class TuitionFinancialLogicTest {

  @Test
  fun testFeeCalculationsAndStatus() {
    val baseFee = 1500.0
    val discount = 200.0
    val netFee = baseFee - discount
    assertEquals(1300.0, netFee, 0.001)

    // Partial payment
    val paid = 500.0
    val remaining = netFee - paid
    assertEquals(800.0, remaining, 0.001)

    val status = when {
      remaining == 0.0 -> FeeStatus.PAID
      paid > 0.0 -> FeeStatus.PARTIALLY_PAID
      else -> FeeStatus.DUE
    }
    assertEquals(FeeStatus.PARTIALLY_PAID, status)
  }

  @Test
  fun testAdvancePaymentAllocation() {
    val netFee = 1000.0
    val paymentAmount = 2500.0
    val allocatedCurrentMonth = netFee.coerceAtMost(paymentAmount)
    val remainingPayment = paymentAmount - allocatedCurrentMonth

    assertEquals(1000.0, allocatedCurrentMonth, 0.001)
    assertEquals(1500.0, remainingPayment, 0.001)
  }

  @Test
  fun testNumberToWords() {
    val words = FormatUtils.numberToWords(1500.0)
    assertTrue(words.contains("One Thousand Five Hundred Rupees Only"))
  }

  @Test
  fun testPeriodSequence() {
    val current = "2026-09"
    val next = DateUtils.nextPeriod(current)
    assertEquals("2026-10", next)
    val prev = DateUtils.previousPeriod(current)
    assertEquals("2026-08", prev)
  }

  @Test
  fun testStudentDataModelFields() {
    val student = com.example.data.model.Student(
      id = "stu_100",
      name = "Rahul Sharma",
      parentContact = "9876543210",
      monthlyFeeAmount = 2500.0,
      studentClass = "Class 10"
    )
    assertEquals("stu_100", student.id)
    assertEquals("Rahul Sharma", student.name)
    assertEquals("Rahul Sharma", student.fullName)
    assertEquals("9876543210", student.parentContact)
    assertEquals("9876543210", student.parentPhone)
    assertEquals(2500.0, student.monthlyFeeAmount, 0.001)
    assertEquals(2500.0, student.monthlyFee, 0.001)

    val payment = com.example.data.model.Payment(
      id = "pay_1",
      amount = 2500.0,
      studentId = student.id,
      paymentMethod = com.example.data.model.PaymentMethod.UPI
    )
    val updatedStudent = student.copy(
      currentMonthStatus = FeeStatus.PAID,
      paymentHistory = listOf(payment)
    )
    assertEquals(FeeStatus.PAID, updatedStudent.currentMonthStatus)
    assertEquals(1, updatedStudent.paymentHistory.size)
    assertEquals(2500.0, updatedStudent.paymentHistory[0].amount, 0.001)
  }
}
