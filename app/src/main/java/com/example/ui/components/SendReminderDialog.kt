package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FeeRecord
import com.example.data.model.Student
import com.example.data.model.TuitionProfile
import com.example.ui.theme.*
import com.example.util.FormatUtils
import com.example.util.NotificationHelper
import com.example.util.ReminderUtils

@Composable
fun SendReminderDialog(
  student: Student,
  feeRecord: FeeRecord?,
  profile: TuitionProfile,
  onDismiss: () -> Unit,
  onReminderSent: (channel: String) -> Unit
) {
  val context = LocalContext.current
  val period = feeRecord?.feePeriod ?: student.currentMonthStatus.name
  val dueDate = feeRecord?.dueDate ?: ""
  val overdueAmount = feeRecord?.remainingAmount ?: student.monthlyFeeAmount
  val formattedAmount = FormatUtils.formatCurrency(overdueAmount, profile.currencySymbol)
  val daysOverdue = if (dueDate.isNotBlank()) ReminderUtils.calculateDaysOverdue(dueDate) else 0L

  var messageText by remember {
    mutableStateOf(
      ReminderUtils.buildReminderMessage(
        studentName = student.name,
        parentContact = student.parentContact,
        period = period,
        amount = formattedAmount,
        dueDate = dueDate,
        tuitionName = profile.tuitionName,
        teacherName = profile.teacherName,
        upiId = profile.upiId
      )
    )
  }

  var isEditingMessage by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 20.dp),
      contentAlignment = Alignment.Center
    ) {
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Dialog Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              ClayIconTile(
                icon = Icons.Default.NotificationsActive,
                tint = Color.White,
                backgroundColor = ClayColors.RoseAccent,
                size = 40.dp,
                shape = RoundedCornerShape(12.dp)
              )
              Column {
                Text(
                  text = "Overdue Fee Reminder",
                  fontWeight = FontWeight.Black,
                  fontSize = 17.sp,
                  color = TextInkPrimary
                )
                Text(
                  text = "1-Tap Notification & Parent Messaging",
                  fontSize = 11.sp,
                  color = TextSecondaryMuted
                )
              }
            }

            IconButton(onClick = onDismiss) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryMuted)
            }
          }

          // Student & Overdue Summary Card
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = ClayColors.RoseSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ClayColors.RoseBorder)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column {
                  Text(
                    text = student.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextInkPrimary
                  )
                  Text(
                    text = "${student.studentClass} • Parent: ${student.parentContact.ifBlank { "No Phone" }}",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                  )
                }

                ClayBadge(
                  text = if (daysOverdue > 0) "$daysOverdue days overdue" else "Payment Overdue",
                  backgroundColor = Color.White,
                  textColor = ClayColors.RoseAccent,
                  borderColor = ClayColors.RoseBorder,
                  icon = Icons.Default.Warning
                )
              }

              Spacer(modifier = Modifier.height(10.dp))
              HorizontalDivider(color = ClayColors.RoseBorder.copy(alpha = 0.5f))
              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = "Outstanding Balance",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                  )
                  Text(
                    text = formattedAmount,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = ClayColors.RoseAccent
                  )
                }

                if (student.lastReminderSentAt != null) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.7f)
                  ) {
                    Text(
                      text = "Last reminded: ${ReminderUtils.formatRelativeTime(student.lastReminderSentAt)}",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = TextSecondaryMuted,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
              }
            }
          }

          // Message Preview & Edit Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Reminder Message Content",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = TextInkPrimary
            )
            TextButton(
              onClick = { isEditingMessage = !isEditingMessage },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Icon(
                if (isEditingMessage) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                if (isEditingMessage) "Done Editing" else "Customize",
                fontSize = 12.sp,
                color = DeepTealPrimary,
                fontWeight = FontWeight.Bold
              )
            }
          }

          if (isEditingMessage) {
            OutlinedTextField(
              value = messageText,
              onValueChange = { messageText = it },
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
              shape = RoundedCornerShape(14.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ClayColors.CardWhite,
                unfocusedContainerColor = ClayColors.CardWhite
              )
            )
          } else {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              color = ClayColors.CardMuted,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderWarmGray.copy(alpha = 0.6f))
            ) {
              Text(
                text = messageText,
                fontSize = 12.sp,
                color = TextInkPrimary,
                lineHeight = 17.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          Text(
            text = "Tap a channel below to send instantly:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondaryMuted
          )

          // 1-Tap Action Buttons
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // WhatsApp Button
            ClayButton(
              onClick = {
                val ok = ReminderUtils.sendViaWhatsApp(context, student.parentContact, messageText)
                if (ok) {
                  NotificationHelper.showReminderSentNotification(
                    context = context,
                    studentName = student.name,
                    amount = formattedAmount,
                    period = period,
                    channel = "WhatsApp"
                  )
                  onReminderSent("WhatsApp")
                  onDismiss()
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("send_whatsapp_button"),
              shape = RoundedCornerShape(16.dp),
              containerColor = Color(0xFF25D366),
              contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Send via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // SMS Button
            ClayButton(
              onClick = {
                val ok = ReminderUtils.sendViaSms(context, student.parentContact, messageText)
                if (ok) {
                  NotificationHelper.showReminderSentNotification(
                    context = context,
                    studentName = student.name,
                    amount = formattedAmount,
                    period = period,
                    channel = "SMS"
                  )
                  onReminderSent("SMS")
                  onDismiss()
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("send_sms_button"),
              shape = RoundedCornerShape(16.dp),
              containerColor = DeepTealPrimary,
              contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
            ) {
              Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Send via SMS Text", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Share Intent Button
              ClaySecondaryButton(
                onClick = {
                  val ok = ReminderUtils.shareReminder(context, messageText)
                  if (ok) {
                    NotificationHelper.showReminderSentNotification(
                      context = context,
                      studentName = student.name,
                      amount = formattedAmount,
                      period = period,
                      channel = "Share"
                    )
                    onReminderSent("Share")
                    onDismiss()
                  }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share App...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              // System Notification Button
              ClaySecondaryButton(
                onClick = {
                  NotificationHelper.showOverdueAlertNotification(
                    context = context,
                    studentName = student.name,
                    amount = formattedAmount,
                    daysOverdue = daysOverdue
                  )
                  onReminderSent("Device Notification")
                  onDismiss()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = ClayColors.RoseSurface
              ) {
                Icon(
                  Icons.Default.Notifications,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = ClayColors.RoseAccent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "Device Alert",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = ClayColors.RoseAccent
                )
              }
            }
          }
        }
      }
    }
  }
}
