package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.util.NotificationHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TuitionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
  object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
  object Students : Screen("students", "Students", Icons.Default.People)
  object CollectFee : Screen("collect_fee", "Collect Fee", Icons.Default.Payments)
  object Pending : Screen("pending", "Pending", Icons.Default.HourglassBottom)
  object Payments : Screen("payments", "Payments", Icons.AutoMirrored.Filled.ReceiptLong)
  object Reports : Screen("reports", "Reports", Icons.Default.BarChart)
  object Settings : Screen("settings", "Settings", Icons.Default.Settings)
  data class StudentDetail(val studentId: String) : Screen("student_detail", "Student Profile", Icons.Default.Person)
  data class AddEditStudent(val studentId: String?) : Screen("add_edit_student", "Student Form", Icons.Default.PersonAdd)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    NotificationHelper.createNotificationChannel(this)

    setContent {
      MyApplicationTheme {
        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { /* Notification permission result handled */ }

        LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.POST_NOTIFICATIONS
              ) != PackageManager.PERMISSION_GRANTED
            ) {
              permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
        }

        val auth = remember { FirebaseAuth.getInstance() }
        var currentUser by remember { mutableStateOf(auth.currentUser) }

        DisposableEffect(auth) {
          val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            currentUser = fbAuth.currentUser
          }
          auth.addAuthStateListener(listener)
          onDispose {
            auth.removeAuthStateListener(listener)
          }
        }

        if (currentUser == null) {
          SignInScreen(
            onSignInSuccess = {
              currentUser = auth.currentUser
            }
          )
        } else {
          TuitionApp(
            onSignOut = {
              auth.signOut()
            }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuitionApp(
  viewModel: TuitionViewModel = viewModel(),
  onSignOut: () -> Unit = {}
) {
  var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
  val screenStack = remember { mutableStateListOf<Screen>() }

  val snackbarHostState = remember { SnackbarHostState() }
  val profile by viewModel.tuitionProfile.collectAsState()
  val selectedReceipt by viewModel.selectedReceiptPayment.collectAsState()

  fun navigateTo(screen: Screen) {
    if (currentScreen != screen) {
      screenStack.add(currentScreen)
      currentScreen = screen
    }
  }

  fun navigateBack() {
    if (screenStack.isNotEmpty()) {
      currentScreen = screenStack.removeAt(screenStack.size - 1)
    } else {
      currentScreen = Screen.Dashboard
    }
  }

  // Handle hardware and gesture back navigation
  BackHandler(enabled = currentScreen != Screen.Dashboard) {
    navigateBack()
  }

  // Snackbar notifications listener
  LaunchedEffect(Unit) {
    viewModel.uiMessage.collectLatest { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  val mainTabs = listOf(
    Screen.Dashboard,
    Screen.Students,
    Screen.CollectFee,
    Screen.Pending,
    Screen.Payments
  )

  val isMainTab = currentScreen in mainTabs

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isTablet = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!isTablet && isMainTab) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
          ) {
            ClayCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(26.dp),
              backgroundColor = ClayColors.CardWhite,
              elevation = 6.dp
            ) {
              NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.height(64.dp)
              ) {
                mainTabs.forEach { tab ->
                  val isSelected = currentScreen == tab
                  NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                      if (currentScreen != tab) {
                        screenStack.clear()
                        currentScreen = tab
                      }
                    },
                    icon = {
                      Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(22.dp)
                      )
                    },
                    label = {
                      Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                      )
                    },
                    colors = NavigationBarItemDefaults.colors(
                      indicatorColor = ClayColors.TealSurface,
                      selectedIconColor = DeepTealPrimary,
                      selectedTextColor = DeepTealPrimary,
                      unselectedIconColor = TextSecondaryMuted,
                      unselectedTextColor = TextSecondaryMuted
                    )
                  )
                }
              }
            }
          }
        }
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        // Navigation Rail for Tablets / Wide Screens
        if (isTablet) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight()
          ) {
            Spacer(modifier = Modifier.height(16.dp))
            mainTabs.forEach { tab ->
              val isSelected = currentScreen == tab
              NavigationRailItem(
                selected = isSelected,
                onClick = {
                  if (currentScreen != tab) {
                    screenStack.clear()
                    currentScreen = tab
                  }
                },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title) }
              )
            }

            Spacer(modifier = Modifier.weight(1f))

            NavigationRailItem(
              selected = currentScreen == Screen.Reports,
              onClick = { navigateTo(Screen.Reports) },
              icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
              label = { Text("Reports") }
            )

            NavigationRailItem(
              selected = currentScreen == Screen.Settings,
              onClick = { navigateTo(Screen.Settings) },
              icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
              label = { Text("Settings") }
            )
          }
        }

        // Main Screen View
        Box(modifier = Modifier.weight(1f)) {
          when (val screen = currentScreen) {
            is Screen.Dashboard -> {
              DashboardScreen(
                viewModel = viewModel,
                onNavigateToStudents = { navigateTo(Screen.Students) },
                onNavigateToAddStudent = { navigateTo(Screen.AddEditStudent(null)) },
                onNavigateToCollectFee = { studentId ->
                  navigateTo(Screen.CollectFee)
                },
                onNavigateToPendingFees = { navigateTo(Screen.Pending) },
                onNavigateToPayments = { navigateTo(Screen.Payments) },
                onNavigateToReports = { navigateTo(Screen.Reports) },
                onNavigateToSettings = { navigateTo(Screen.Settings) }
              )
            }
            is Screen.Students -> {
              StudentListScreen(
                viewModel = viewModel,
                onNavigateToDetail = { studentId ->
                  navigateTo(Screen.StudentDetail(studentId))
                },
                onNavigateToAddStudent = {
                  navigateTo(Screen.AddEditStudent(null))
                },
                onNavigateToCollectFee = { studentId ->
                  navigateTo(Screen.CollectFee)
                }
              )
            }
            is Screen.CollectFee -> {
              CollectFeeScreen(
                viewModel = viewModel,
                preselectedStudentId = null,
                onNavigateBack = { navigateBack() },
                onPaymentSuccess = { payment ->
                  // Receipt dialog will open automatically via selectedReceiptPayment
                }
              )
            }
            is Screen.Pending -> {
              PendingFeesScreen(
                viewModel = viewModel,
                onNavigateToCollectFee = { studentId ->
                  navigateTo(Screen.CollectFee)
                },
                onNavigateToStudentDetail = { studentId ->
                  navigateTo(Screen.StudentDetail(studentId))
                }
              )
            }
            is Screen.Payments -> {
              PaymentHistoryScreen(
                viewModel = viewModel,
                onNavigateToStudentDetail = { studentId ->
                  navigateTo(Screen.StudentDetail(studentId))
                }
              )
            }
            is Screen.Reports -> {
              ReportsScreen(viewModel = viewModel)
            }
            is Screen.Settings -> {
              SettingsScreen(viewModel = viewModel, onSignOut = onSignOut)
            }
            is Screen.StudentDetail -> {
              StudentDetailScreen(
                viewModel = viewModel,
                studentId = screen.studentId,
                onNavigateBack = { navigateBack() },
                onNavigateToEdit = { sId ->
                  navigateTo(Screen.AddEditStudent(sId))
                },
                onNavigateToCollectFee = { sId ->
                  navigateTo(Screen.CollectFee)
                }
              )
            }
            is Screen.AddEditStudent -> {
              AddEditStudentScreen(
                viewModel = viewModel,
                studentId = screen.studentId,
                onNavigateBack = { navigateBack() }
              )
            }
          }
        }
      }
    }
  }

  // Payment Receipt Modal Dialog (Shown whenever a receipt is selected)
  if (selectedReceipt != null) {
    ReceiptDialog(
      payment = selectedReceipt!!,
      profile = profile,
      onDismiss = { viewModel.selectReceiptPayment(null) }
    )
  }
}
