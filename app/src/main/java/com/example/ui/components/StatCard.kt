package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StatCard(
  title: String,
  value: String,
  subtitle: String? = null,
  icon: ImageVector? = null,
  containerColor: Color = CardSurfaceWhite,
  contentColor: Color = TextInkPrimary,
  iconBackgroundColor: Color = SurfaceMuted,
  iconTintColor: Color = TextSecondaryMuted,
  indicatorText: String? = null,
  indicatorColor: Color? = null,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = containerColor,
      contentColor = contentColor
    ),
    border = BorderStroke(1.dp, BorderWarmGray),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 2.dp),
    onClick = { onClick?.invoke() },
    enabled = onClick != null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title.uppercase(),
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondaryMuted,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.5.sp,
          fontSize = 10.sp
        )

        if (icon != null) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(iconBackgroundColor),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = iconTintColor,
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        fontSize = 20.sp
      )

      if (!subtitle.isNullOrBlank() || !indicatorText.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (!indicatorText.isNullOrBlank() && indicatorColor != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(indicatorColor.copy(alpha = 0.12f))
                .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
              Text(
                text = indicatorText,
                color = indicatorColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          if (!subtitle.isNullOrBlank()) {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondaryMuted,
              fontSize = 11.sp
            )
          }
        }
      }
    }
  }
}
