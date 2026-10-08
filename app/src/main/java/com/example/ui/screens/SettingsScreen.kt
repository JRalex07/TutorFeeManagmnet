package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TuitionProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TuitionViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: TuitionViewModel,
  onSignOut: () -> Unit = {}
) {
  val profile by viewModel.tuitionProfile.collectAsState()
  val isFirestoreConnected by viewModel.isFirestoreConnected.collectAsState()

  val auth = FirebaseAuth.getInstance()
  val currentUser = auth.currentUser

  var tuitionName by remember(profile) { mutableStateOf(profile.tuitionName) }
  var teacherName by remember(profile) { mutableStateOf(profile.teacherName) }
  var phone by remember(profile) { mutableStateOf(profile.phone) }
  var address by remember(profile) { mutableStateOf(profile.address) }
  var upiId by remember(profile) { mutableStateOf(profile.upiId) }
  var defaultMonthlyFeeStr by remember(profile) { mutableStateOf(profile.defaultMonthlyFee.toInt().toString()) }
  var defaultDueDayStr by remember(profile) { mutableStateOf(profile.defaultDueDay.toString()) }
  var currencySymbol by remember(profile) { mutableStateOf(profile.currencySymbol) }
  var receiptFooter by remember(profile) { mutableStateOf(profile.receiptFooterNote) }

  var showSignOutDialog by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Profile & Settings", fontWeight = FontWeight.Bold) },
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
      // 1. Google Account & Session Card
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              ClayIconTile(
                icon = Icons.Default.AccountCircle,
                tint = DeepTealPrimary,
                backgroundColor = ClayColors.TealSurface,
                size = 46.dp,
                shape = CircleShape
              )
              Column {
                Text(
                  text = currentUser?.displayName ?: "Signed-in Tutor",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = TextInkPrimary
                )
                Text(
                  text = currentUser?.email ?: "Google Account Connected",
                  fontSize = 12.sp,
                  color = TextSecondaryMuted
                )
              }
            }

            ClayBadge(
              text = "Connected",
              backgroundColor = ClayColors.GreenSurface,
              textColor = ClayColors.GreenAccent,
              borderColor = ClayColors.GreenBorder,
              icon = Icons.Default.Check
            )
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = BorderWarmGray.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(
                Icons.Default.CloudSync,
                contentDescription = null,
                tint = DeepTealPrimary,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Live Firestore Cloud Sync Active",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextInkPrimary
              )
            }

            ClaySecondaryButton(
              onClick = { showSignOutDialog = true },
              shape = RoundedCornerShape(12.dp),
              backgroundColor = ClayColors.RoseSurface,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = ClayColors.RoseAccent,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sign Out", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ClayColors.RoseAccent)
            }
          }
        }
      }

      // 2. Tuition Profile Card
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 5.dp
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            "Tuition Center & Branding",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = DeepTealPrimary
          )

          OutlinedTextField(
            value = tuitionName,
            onValueChange = { tuitionName = it },
            label = { Text("Academy / Tuition Name") },
            placeholder = { Text("e.g. Apex Mathematics Coaching") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = teacherName,
            onValueChange = { teacherName = it },
            label = { Text("Tutor / Teacher Name") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Contact Phone (shown on receipts)") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Center Address") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = upiId,
            onValueChange = { upiId = it },
            label = { Text("UPI ID for Payment Receipts") },
            placeholder = { Text("e.g. name@okhdfcbank") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // 3. Fee Defaults Card
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 5.dp
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            "Default Billing Rules",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = DeepTealPrimary
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = defaultMonthlyFeeStr,
              onValueChange = { defaultMonthlyFeeStr = it },
              label = { Text("Default Fee (₹)") },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
              value = defaultDueDayStr,
              onValueChange = { defaultDueDayStr = it },
              label = { Text("Monthly Due Day") },
              placeholder = { Text("1-31") },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f)
            )
          }

          OutlinedTextField(
            value = receiptFooter,
            onValueChange = { receiptFooter = it },
            label = { Text("Receipt Footer Note") },
            placeholder = { Text("Thank you for your commitment to learning.") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // Save Button
      ClayButton(
        onClick = {
          val updated = profile.copy(
            tuitionName = tuitionName.trim(),
            teacherName = teacherName.trim(),
            phone = phone.trim(),
            address = address.trim(),
            upiId = upiId.trim(),
            defaultMonthlyFee = defaultMonthlyFeeStr.toDoubleOrNull() ?: profile.defaultMonthlyFee,
            defaultDueDay = defaultDueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: profile.defaultDueDay,
            currencySymbol = currencySymbol.trim(),
            receiptFooterNote = receiptFooter.trim()
          )
          viewModel.updateTuitionProfile(updated)
        },
        modifier = Modifier.fillMaxWidth(),
        containerColor = DeepTealPrimary,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
      ) {
        Icon(Icons.Default.Save, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }
    }
  }

  if (showSignOutDialog) {
    AlertDialog(
      onDismissRequest = { showSignOutDialog = false },
      title = { Text("Sign Out of Google?", fontWeight = FontWeight.Bold) },
      text = { Text("You will be signed out of your tutor cloud account. Offline changes remain safely saved.") },
      confirmButton = {
        Button(
          onClick = {
            showSignOutDialog = false
            auth.signOut()
            onSignOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Sign Out")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showSignOutDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
