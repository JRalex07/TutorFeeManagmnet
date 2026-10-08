package com.example.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayColors
import com.example.ui.components.ClayIconTile
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

private const val TAG = "SignInScreen"

@Composable
fun SignInScreen(
  onSignInSuccess: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  fun launchGoogleSignIn() {
    isLoading = true
    errorMessage = null

    coroutineScope.launch {
      try {
        val webClientId = context.getString(R.string.default_web_client_id)
        val credentialManager = CredentialManager.create(context)
        
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
          .build()

        val request = GetCredentialRequest.Builder()
          .addCredentialOption(signInOption)
          .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
          val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
          
          FirebaseAuth.getInstance().signInWithCredential(authCredential)
            .addOnSuccessListener {
              isLoading = false
              onSignInSuccess()
            }
            .addOnFailureListener { e ->
              isLoading = false
              Log.e(TAG, "Firebase Auth failed", e)
              errorMessage = "Authentication failed: ${e.localizedMessage ?: "Please try again."}"
            }
        } else {
          isLoading = false
          errorMessage = "Unexpected credential format returned."
        }
      } catch (e: GetCredentialCancellationException) {
        // User dismissed the account picker dialog
        Log.d(TAG, "Sign in was cancelled by user")
        isLoading = false
      } catch (e: Exception) {
        isLoading = false
        Log.e(TAG, "Sign in exception", e)
        errorMessage = e.localizedMessage ?: "Could not sign in with Google. Check connection."
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ClayColors.CanvasBg)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Inflated Clay Hero Card
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        backgroundColor = ClayColors.CardWhite,
        elevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Top Clay App Icon
          ClayIconTile(
            icon = Icons.Default.AccountBalanceWallet,
            tint = Color.White,
            backgroundColor = DeepTealPrimary,
            size = 68.dp,
            shape = RoundedCornerShape(22.dp)
          )

          Spacer(modifier = Modifier.height(18.dp))

          Text(
            text = "Tuition Fee Manager",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = TextInkPrimary,
            textAlign = TextAlign.Center
          )

          Text(
            text = "Independent Tutor Ledger & Cloud Sync",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondaryMuted,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(24.dp))

          // 3 Feature Pills
          FeatureHighlightRow(
            icon = Icons.Default.CloudSync,
            title = "Real-time Cloud Sync",
            desc = "Synced securely in Google Cloud Firestore"
          )
          Spacer(modifier = Modifier.height(12.dp))
          FeatureHighlightRow(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = "Instant Payment Receipts",
            desc = "Track cash, UPI, partials & print receipts"
          )
          Spacer(modifier = Modifier.height(12.dp))
          FeatureHighlightRow(
            icon = Icons.Default.Security,
            title = "Zero-Trust Privacy",
            desc = "Your student data is isolated to your Google account"
          )

          Spacer(modifier = Modifier.height(28.dp))

          if (errorMessage != null) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              color = ClayColors.RoseSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, ClayColors.RoseBorder)
            ) {
              Text(
                text = errorMessage ?: "",
                color = ClayColors.RoseAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
          }

          // Main Clay Sign In Button
          ClayButton(
            onClick = { launchGoogleSignIn() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            containerColor = DeepTealPrimary,
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp)
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Connecting to Google...",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            } else {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Sign in with Google",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Private & Local-First Database • Auto-Offline Supported",
        fontSize = 11.sp,
        color = TextSecondaryMuted,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  desc: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    ClayIconTile(
      icon = icon,
      tint = DeepTealPrimary,
      backgroundColor = ClayColors.TealSurface,
      size = 38.dp,
      shape = CircleShape
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = TextInkPrimary
      )
      Text(
        text = desc,
        fontSize = 11.sp,
        color = TextSecondaryMuted
      )
    }
  }
}
