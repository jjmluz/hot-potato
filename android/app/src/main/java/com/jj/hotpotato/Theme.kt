package com.jj.hotpotato

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

// Material You (Material 3), esquema escuro gerado a partir de uma cor-semente âmbar
val Primary = Color(0xFFFFB77C)
val OnPrimary = Color(0xFF4D2600)
val PrimaryContainer = Color(0xFF6E3900)
val OnPrimaryContainer = Color(0xFFFFDCC3)
val SecondaryContainer = Color(0xFF5B4132)
val OnSecondaryContainer = Color(0xFFFFDCC3)
val TertiaryContainer = Color(0xFF464A1F)
val OnTertiaryContainer = Color(0xFFE3E6AF)
val ErrorColor = Color(0xFFFFB4AB)
val ErrorContainer = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD6)
val SuccessContainer = Color(0xFF1F5129)
val OnSuccessContainer = Color(0xFFB9F1B3)
val Success = Color(0xFF9DD598)
val Surface = Color(0xFF18120E)
val SurfaceContainer = Color(0xFF261E19)
val SurfaceContainerHigh = Color(0xFF312823)
val SurfaceContainerHighest = Color(0xFF3C332D)
val OnSurface = Color(0xFFEFE0D8)
val OnSurfaceVariant = Color(0xFFD6C3B7)
val OutlineVariant = Color(0xFF52443B)
val InverseSurface = Color(0xFFEFE0D8)
val InverseOnSurface = Color(0xFF372F29)

val Ease = CubicBezierEasing(.2f, 0f, 0f, 1f)
val EaseSpring = CubicBezierEasing(.34f, 1.4f, .64f, 1f)

// Fonte incluída no projeto para funcionar sem internet (só o subconjunto latino)
val GoogleSansFlex = FontFamily(
    listOf(400, 420, 450, 500, 550, 600, 650, 700, 750).map {
        Font(R.font.google_sans_flex, FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it)))
    }
)

// Cor do texto e dos ícones, herdada do contentor
val LocalContent = compositionLocalOf { OnSurface }

fun rounded(radius: Int) = RoundedCornerShape(radius.dp)

fun textStyle(
    size: TextUnit,
    weight: Int,
    color: Color,
    lineHeight: TextUnit = TextUnit.Unspecified,
    spacing: TextUnit = TextUnit.Unspecified,
    align: TextAlign = TextAlign.Unspecified,
) = TextStyle(
    color = color,
    fontFamily = GoogleSansFlex,
    fontSize = size,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight,
    letterSpacing = spacing,
    textAlign = align,
)

@Composable
fun T(
    text: String,
    size: TextUnit,
    weight: Int = 420,
    modifier: Modifier = Modifier,
    color: Color = LocalContent.current,
    lineHeight: TextUnit = TextUnit.Unspecified,
    spacing: TextUnit = TextUnit.Unspecified,
    align: TextAlign = TextAlign.Unspecified,
    balance: Boolean = false,
    strike: Boolean = false,
) {
    val style = textStyle(size, weight, color, lineHeight, spacing, align).copy(
        lineBreak = if (balance) LineBreak.Heading else LineBreak.Unspecified,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
    )
    BasicText(text, modifier, style)
}
