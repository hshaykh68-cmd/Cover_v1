package com.cover.app.core.theme

import androidx.compose.ui.graphics.Color

// ============================================
// STEALTH LUXURY COLOR SYSTEM
// ============================================

// Void Background - deeper than black with subtle undertones
val VoidBlack = Color(0xFF000000)
val VoidBlue = Color(0xFF050508)
val VoidPurple = Color(0xFF0A0A0F)

// Glassmorphism Surfaces - layered transparency
val Surface10 = Color(0x1AFFFFFF)  // 10% white
val Surface15 = Color(0x26FFFFFF)  // 15% white
val Surface20 = Color(0x33FFFFFF)  // 20% white
val Surface30 = Color(0x4DFFFFFF)  // 30% white
val Surface40 = Color(0x66FFFFFF)  // 40% white

// Neon Accents - purposeful glow points
val CyanGlow = Color(0xFF00D4FF)
val CyanGlowSoft = Color(0x6600D4FF)
val AmberAlert = Color(0xFFFFB800)
val AmberSoft = Color(0x66FFB800)
val CrimsonSecurity = Color(0xFFFF2D55)
val EmeraldSuccess = Color(0xFF30D158)
val GoldPremium = Color(0xFFFFD700)

// Gradient Colors
val GradientStart = Color(0xFF001a33)
val GradientEnd = Color(0xFF000510)
val GlowCyanStart = Color(0xFF00D4FF)
val GlowCyanEnd = Color(0xFF0080FF)

// ============================================
// LEGACY COMPATIBILITY (for gradual migration)
// ============================================

// Primary palette - mapped to new system
val Purple80 = CyanGlowSoft
val PurpleGrey80 = Surface30
val Pink80 = CrimsonSecurity.copy(alpha = 0.5f)

val Purple40 = CyanGlow
val PurpleGrey40 = Surface40
val Pink40 = CrimsonSecurity

// Calculator colors - neumorphic redesign
val CalculatorDisplay = VoidBlue
val CalculatorButtonNumber = Surface15
val CalculatorButtonOperation = CyanGlow
val CalculatorButtonFunction = Surface20
val CalculatorButtonPressed = Surface30

// Vault colors - glassmorphic system
val VaultBackground = VoidBlack
val VaultSurface = Surface10
val VaultAccent = CyanGlow
val VaultSuccess = EmeraldSuccess
val VaultWarning = AmberAlert
val VaultError = CrimsonSecurity

// Text colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0x99FFFFFF)  // 60% white
val TextTertiary = Color(0x66FFFFFF)   // 40% white
val TextDisabled = Color(0x33FFFFFF)   // 20% white
