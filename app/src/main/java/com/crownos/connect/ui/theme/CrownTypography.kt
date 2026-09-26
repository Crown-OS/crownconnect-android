package com.crownos.connect.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.crownos.connect.R

@OptIn(ExperimentalTextApi::class)
private fun interAt(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    interAt(FontWeight.Normal),
    interAt(FontWeight.Medium),
    interAt(FontWeight.SemiBold),
    interAt(FontWeight.Bold),
)

private fun interStyle(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = 1.2.em,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

@Immutable
object CrownType {
    val body = interStyle(14)
    val small = interStyle(12)
    val pageTitle = interStyle(22, FontWeight.Bold)
    val sectionLabel = interStyle(12, FontWeight.SemiBold)
    val rowTitle = interStyle(14)
    val rowDescription = interStyle(12)
    val value = interStyle(16)
    val sidebarItem = interStyle(14, FontWeight.Medium)
    val sidebarBrand = interStyle(15, FontWeight.Medium)
    val sidebarSection = interStyle(12, FontWeight.Medium)
    val buttonMedium = interStyle(14)
    val buttonSmall = interStyle(12)
    val choiceTitle = interStyle(15)
    val choiceSubtitle = interStyle(13)
    val calloutTitle = interStyle(14)
    val calloutBody = interStyle(13)
    val menuLabel = interStyle(15)
    val menuShortcut = interStyle(14)
    val menuHeader = interStyle(12, FontWeight.Medium)
    val wizardTitle = interStyle(21)
    val wizardSubtitle = interStyle(14)
    val heroTitle = interStyle(26)
    val heroSubtitle = interStyle(15)
    val headline = interStyle(17, FontWeight.SemiBold)
    val paragraph = interStyle(13)
    val pairingCode = interStyle(34, FontWeight.Bold)
}
