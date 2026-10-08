package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Unified Claymorphic Financial Theme Palette
// Tactile, matte, inflated pastel clay tones with deep trustworthy anchors
// =========================================================================

// Primary Brand Colors (Deep Jade Teal - grounded, confident, executive)
val DeepTealPrimary = Color(0xFF14736E)
val DeepTealDark = Color(0xFF0C4542)
val DeepTealLight = Color(0xFF229590)
val DeepTealContainer = Color(0xFFE4F3F1)
val DeepTealOnContainer = Color(0xFF083331)

// Backward compatibility alias for any existing references
val BrandBluePrimary = DeepTealPrimary
val BrandNavyDark = DeepTealDark
val BrandNavy = Color(0xFF132B2A)

// Secondary Brand Colors (Soft Sage / Moss Clay)
val SageSecondary = Color(0xFF4A8C86)
val SageContainer = Color(0xFFEAF4F3)
val SageOnContainer = Color(0xFF1A4742)

// Clay Surface & Background Colors
// Light Scheme (Warm soft clay canvas with crisp matte white elevated cards)
val ClayCanvasBackground = Color(0xFFF3F5F7)
val ClayCardWhite = Color(0xFFFFFFFF)
val ClayCardMuted = Color(0xFFF8FAFB)
val ClaySurfaceMuted = Color(0xFFECEFF2)
val ClayBorderWarmGray = Color(0xFFE2E7EC)

// Retain legacy aliases
val WarmIvoryBackground = ClayCanvasBackground
val CardSurfaceWhite = ClayCardWhite
val SurfaceMuted = ClaySurfaceMuted
val BorderWarmGray = ClayBorderWarmGray

// Typography Ink Colors
val TextInkPrimary = Color(0xFF1B242D)
val TextSecondaryMuted = Color(0xFF647482)
val TextTertiarySubtle = Color(0xFF8E9EA9)

// Dark Theme Clay Alternates (Restrained, smooth charcoal & slate clay)
val DarkClayBackground = Color(0xFF12161A)
val DarkClaySurface = Color(0xFF1B2127)
val DarkClayCardMuted = Color(0xFF232B32)
val DarkClayBorder = Color(0xFF2F3A44)
val DarkTextPrimary = Color(0xFFF3F6F9)
val DarkTextSecondary = Color(0xFF909FAE)

// Retain legacy dark aliases
val DarkBackground = DarkClayBackground
val DarkSurface = DarkClaySurface
val DarkBorder = DarkClayBorder

// Clay 3D Specular Highlight & Soft Shadow Colors
val ClayHighlightWhite = Color(0xE6FFFFFF)
val ClayHighlightSubtle = Color(0x66FFFFFF)
val ClayShadowAmbientLight = Color(0x1A142624)
val ClayShadowSpotLight = Color(0x24142624)
val ClayShadowAmbientDark = Color(0x40000000)
val ClayShadowSpotDark = Color(0x66000000)

// Semantic Financial Clay Status Colors (Pill Badges, Cards, Highlights)
// 1. Forest Green (Paid / Completed)
val StatusPaid = Color(0xFF16825B)
val StatusPaidContainer = Color(0xFFE7F6EF)
val StatusPaidBorder = Color(0xFFC4EBDA)
val StatusOnPaidContainer = Color(0xFF0E543A)

// 2. Muted Amber / Ochre (Due / Pending)
val StatusDue = Color(0xFFB57008)
val StatusDueContainer = Color(0xFFFFF4E0)
val StatusDueBorder = Color(0xFFFFE3B3)
val StatusOnDueContainer = Color(0xFF7A4A04)

// 3. Terracotta Brick / Rose (Overdue / Attention)
val StatusOverdue = Color(0xFFC93B3B)
val StatusOverdueContainer = Color(0xFFFFECEB)
val StatusOverdueBorder = Color(0xFFFFCDC9)
val StatusOnOverdueContainer = Color(0xFF852323)

// 4. Muted Lilac / Grape (Advance)
val StatusAdvance = Color(0xFF6748A5)
val StatusAdvanceContainer = Color(0xFFF1EDF9)
val StatusAdvanceBorder = Color(0xFFDCD2F2)
val StatusOnAdvanceContainer = Color(0xFF432D70)

// 5. Deep Teal / Pine (Partial)
val StatusPartial = Color(0xFF14736E)
val StatusPartialContainer = Color(0xFFE4F3F1)
val StatusPartialBorder = Color(0xFFBBE5E1)
val StatusOnPartialContainer = Color(0xFF0B3A37)

// 6. Slate Gray (Waived)
val StatusWaived = Color(0xFF647482)
val StatusWaivedContainer = Color(0xFFF0F3F5)
val StatusWaivedBorder = Color(0xFFD5DDE3)
val StatusOnWaivedContainer = Color(0xFF38434D)

// 7. Clay Sky Blue (Info / Analytics)
val ClaySkyBlue = Color(0xFF1A65C7)
val ClaySkyBlueContainer = Color(0xFFE8F1FC)
val ClaySkyBlueBorder = Color(0xFFC8DCF8)
val ClayOnSkyBlueContainer = Color(0xFF0D3E80)
