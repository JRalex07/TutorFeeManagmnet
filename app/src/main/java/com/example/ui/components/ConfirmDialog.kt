package com.example.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ConfirmDialog(
  title: String,
  message: String,
  confirmButtonText: String = "Confirm",
  dismissButtonText: String = "Cancel",
  icon: ImageVector? = null,
  isDestructive: Boolean = false,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = if (icon != null) {
      { Icon(imageVector = icon, contentDescription = null) }
    } else null,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium
      )
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm()
          onDismiss()
        },
        colors = if (isDestructive) {
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          )
        } else ButtonDefaults.buttonColors()
      ) {
        Text(confirmButtonText)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text(dismissButtonText)
      }
    }
  )
}
