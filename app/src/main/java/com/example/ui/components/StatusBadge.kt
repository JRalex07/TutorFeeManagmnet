package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeeStatus
import com.example.data.model.PaymentRecordStatus
import com.example.data.model.StudentStatus
import com.example.ui.theme.*

@Composable
fun FeeStatusBadge(status: FeeStatus, modifier: Modifier = Modifier) {
  val (bgColor, textColor, borderColor) = when (status) {
    FeeStatus.PAID -> Triple(StatusPaidContainer, StatusOnPaidContainer, StatusPaid.copy(alpha = 0.35f))
    FeeStatus.PARTIALLY_PAID -> Triple(StatusPartialContainer, StatusOnPartialContainer, StatusPartial.copy(alpha = 0.35f))
    FeeStatus.OVERDUE -> Triple(StatusOverdueContainer, StatusOnOverdueContainer, StatusOverdue.copy(alpha = 0.35f))
    FeeStatus.DUE -> Triple(StatusDueContainer, StatusOnDueContainer, StatusDue.copy(alpha = 0.35f))
    FeeStatus.ADVANCE_COVERED -> Triple(StatusAdvanceContainer, StatusOnAdvanceContainer, StatusAdvance.copy(alpha = 0.35f))
    FeeStatus.WAIVED -> Triple(StatusWaivedContainer, StatusOnWaivedContainer, StatusWaived.copy(alpha = 0.35f))
    FeeStatus.CANCELLED -> Triple(StatusOverdueContainer, StatusOnOverdueContainer, StatusOverdue.copy(alpha = 0.35f))
    FeeStatus.NOT_DUE, FeeStatus.PENDING -> Triple(StatusDueContainer, StatusOnDueContainer, StatusDue.copy(alpha = 0.35f))
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(bgColor)
      .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = status.label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
fun StudentStatusBadge(status: StudentStatus, modifier: Modifier = Modifier) {
  val (bgColor, textColor, borderColor) = when (status) {
    StudentStatus.ACTIVE -> Triple(StatusPaidContainer, StatusOnPaidContainer, StatusPaid.copy(alpha = 0.35f))
    StudentStatus.INACTIVE -> Triple(StatusWaivedContainer, StatusOnWaivedContainer, StatusWaived.copy(alpha = 0.35f))
    StudentStatus.LEFT -> Triple(StatusOverdueContainer, StatusOnOverdueContainer, StatusOverdue.copy(alpha = 0.35f))
    StudentStatus.COMPLETED -> Triple(StatusAdvanceContainer, StatusOnAdvanceContainer, StatusAdvance.copy(alpha = 0.35f))
    StudentStatus.SUSPENDED -> Triple(StatusDueContainer, StatusOnDueContainer, StatusDue.copy(alpha = 0.35f))
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(bgColor)
      .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = status.label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun PaymentStatusBadge(status: PaymentRecordStatus, modifier: Modifier = Modifier) {
  val (bgColor, textColor, borderColor) = when (status) {
    PaymentRecordStatus.COMPLETED -> Triple(StatusPaidContainer, StatusOnPaidContainer, StatusPaid.copy(alpha = 0.35f))
    PaymentRecordStatus.REVERSED -> Triple(StatusDueContainer, StatusOnDueContainer, StatusDue.copy(alpha = 0.35f))
    PaymentRecordStatus.CANCELLED -> Triple(StatusOverdueContainer, StatusOnOverdueContainer, StatusOverdue.copy(alpha = 0.35f))
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(bgColor)
      .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = status.label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}
