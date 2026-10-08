package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// Soft Clay Palette definition
object ClayColors {
  val CanvasBg = Color(0xFFF2F4F5)
  val CardWhite = Color(0xFFFFFFFF)
  val CardMuted = Color(0xFFF8F9FA)
  
  // Pastel Clay Tints for status & tiles
  val TealSurface = Color(0xFFE4F3F1)
  val TealBorder = Color(0xFFBBE5E1)
  val TealAccent = Color(0xFF14736E)
  
  val GreenSurface = Color(0xFFE8F6EE)
  val GreenBorder = Color(0xFFC7EBD4)
  val GreenAccent = Color(0xFF1B7A4B)
  
  val AmberSurface = Color(0xFFFFF4E0)
  val AmberBorder = Color(0xFFFFE3B3)
  val AmberAccent = Color(0xFFB57008)
  
  val RoseSurface = Color(0xFFFFECEB)
  val RoseBorder = Color(0xFFFFCDC9)
  val RoseAccent = Color(0xFFC93B3B)
  
  val PurpleSurface = Color(0xFFF1EDF9)
  val PurpleBorder = Color(0xFFDCD2F2)
  val PurpleAccent = Color(0xFF6748A5)

  val BlueSurface = Color(0xFFE8F1FC)
  val BlueBorder = Color(0xFFC8DCF8)
  val BlueAccent = Color(0xFF1A65C7)
}

/**
 * Puffy, tactile Claymorphic Card container with soft double-shadow simulation
 * and a top-left highlight border that creates an inflated 3D matte clay look.
 */
@Composable
fun ClayCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(22.dp),
  backgroundColor: Color = ClayColors.CardWhite,
  elevation: Dp = 6.dp,
  borderWidth: Dp = 1.5.dp,
  highlightColor: Color = Color.White.copy(alpha = 0.85f),
  shadowColor: Color = Color(0x1A0F2B28),
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(if (isPressed && onClick != null) 0.98f else 1f, label = "clay_press")

  Box(
    modifier = modifier
      .scale(scale)
      .shadow(
        elevation = if (isPressed && onClick != null) elevation / 2 else elevation,
        shape = shape,
        clip = false,
        ambientColor = shadowColor,
        spotColor = shadowColor
      )
      .clip(shape)
      .background(backgroundColor)
      .then(
        if (onClick != null) {
          Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
          )
        } else Modifier
      )
  ) {
    // Top-left soft specular highlight rim to produce 3D clay inflation
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          brush = Brush.linearGradient(
            colors = listOf(
              highlightColor,
              highlightColor.copy(alpha = 0.3f),
              Color.Transparent,
              Color(0x08000000)
            )
          ),
          shape = shape
        )
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(borderWidth),
      content = content
    )
  }
}

/**
 * Tactile, pressable Clay Button with soft drop shadow and inner gloss/highlight.
 */
@Composable
fun ClayButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = RoundedCornerShape(20.dp),
  containerColor: Color = DeepTealPrimary,
  contentColor: Color = Color.White,
  elevation: Dp = 6.dp,
  contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
  content: @Composable RowScope.() -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "button_press")

  Surface(
    onClick = onClick,
    modifier = modifier
      .scale(scale)
      .shadow(
        elevation = if (isPressed) 2.dp else elevation,
        shape = shape,
        spotColor = containerColor.copy(alpha = 0.45f),
        ambientColor = Color(0x14000000)
      ),
    enabled = enabled,
    shape = shape,
    color = if (enabled) containerColor else Color(0xFFD1D5DB),
    contentColor = contentColor,
    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
    interactionSource = interactionSource
  ) {
    Row(
      modifier = Modifier.padding(contentPadding),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      content = content
    )
  }
}

/**
 * Secondary / Outlined soft clay button
 */
@Composable
fun ClaySecondaryButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(18.dp),
  backgroundColor: Color = ClayColors.CardWhite,
  textColor: Color = TextInkPrimary,
  borderStrokeColor: Color = BorderWarmGray,
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
  content: @Composable RowScope.() -> Unit
) {
  ClayCard(
    modifier = modifier,
    shape = shape,
    backgroundColor = backgroundColor,
    elevation = 3.dp,
    onClick = onClick
  ) {
    Row(
      modifier = Modifier.padding(contentPadding),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      content = content
    )
  }
}

/**
 * Tactile pill badge for financial status (Paid, Due, Overdue, Partial, Advance)
 */
@Composable
fun ClayBadge(
  text: String,
  modifier: Modifier = Modifier,
  backgroundColor: Color = ClayColors.TealSurface,
  textColor: Color = ClayColors.TealAccent,
  borderColor: Color = ClayColors.TealBorder,
  icon: ImageVector? = null
) {
  Surface(
    modifier = modifier
      .shadow(2.dp, CircleShape, spotColor = Color(0x10000000))
      .clip(CircleShape),
    color = backgroundColor,
    shape = CircleShape,
    border = BorderStroke(1.dp, borderColor)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = textColor,
          modifier = Modifier.size(13.dp)
        )
      }
      Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
      )
    }
  }
}

/**
 * Inflated metric KPI card on Dashboard with icon tile and big bold value
 */
@Composable
fun ClayStatCard(
  title: String,
  value: String,
  subtitle: String? = null,
  icon: ImageVector,
  cardBg: Color,
  accentColor: Color,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  ClayCard(
    modifier = modifier,
    shape = RoundedCornerShape(22.dp),
    backgroundColor = cardBg,
    elevation = 5.dp,
    onClick = onClick
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
          text = title,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondaryMuted
        )
        ClayIconTile(
          icon = icon,
          tint = accentColor,
          backgroundColor = Color.White.copy(alpha = 0.85f),
          size = 32.dp
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = TextInkPrimary
      )
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = TextSecondaryMuted
        )
      }
    }
  }
}

/**
 * Inflated circular/rounded icon container
 */
@Composable
fun ClayIconTile(
  icon: ImageVector,
  modifier: Modifier = Modifier,
  tint: Color = DeepTealPrimary,
  backgroundColor: Color = Color.White,
  size: Dp = 40.dp,
  shape: Shape = CircleShape
) {
  Box(
    modifier = modifier
      .size(size)
      .shadow(3.dp, shape, spotColor = Color(0x18000000))
      .clip(shape)
      .background(backgroundColor),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(size * 0.55f)
    )
  }
}
