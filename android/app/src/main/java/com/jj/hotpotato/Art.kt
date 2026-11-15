package com.jj.hotpotato

import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun icon(d: String) = ImageVector.Builder(
    defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)).build()

// Ícones (Material Symbols)
val IconArrow = icon("M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z")
val IconHeart = icon("M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z")
val IconCheck = icon("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z")
val IconClose = icon("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z")
val IconLeft = icon("M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z")
val IconRight = icon("M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z")
val IconEdit = icon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z")
val IconPlay = icon("M8 5v14l11-7z")
val IconFire = icon("M13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67zM11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.62-2.58.39 1.29.59 2.65.59 4.04 0 2.65-2.15 4.8-4.8 4.8z")

@Composable
fun Icon(vector: ImageVector, size: Dp, modifier: Modifier = Modifier, tint: Color = LocalContent.current) {
    Image(rememberVectorPainter(vector), null, modifier.size(size), colorFilter = ColorFilter.tint(tint))
}

private fun svgPath(d: String) = PathParser().parsePathString(d).toPath()

// Forma "cookie" do Material 3 Expressive: círculo com ondulações suaves, numa caixa de 200x200.
private val CookiePath: Path by lazy {
    Path().apply {
        for (i in 0 until 180) {
            val a = i / 180f * 2f * PI.toFloat()
            val r = 88f + 7f * cos(9f * a)
            val x = 100f + r * cos(a)
            val y = 100f + r * sin(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

// color é lida só ao desenhar, para a forma poder aquecer sem recompor nada.
@Composable
fun Cookie(spinMs: Int, modifier: Modifier = Modifier, color: () -> Color = { PrimaryContainer }) {
    val spin = rememberInfiniteTransition()
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(spinMs, easing = LinearEasing)))
    Canvas(modifier.graphicsLayer { rotationZ = angle }) {
        scale(size.width / 200f, pivot = Offset.Zero) { drawPath(CookiePath, color()) }
    }
}

private const val POTATO_W = 200
private const val POTATO_H = 170
private const val POTATO_PAD = 60    // margem para a sombra
private const val POTATO_SCALE = 2

// A batata, com sombra, é desenhada uma vez para um bitmap; depois só se move esse bitmap.
private val PotatoImage: ImageBitmap by lazy {
    val bitmap = Bitmap.createBitmap(
        (POTATO_W + 2 * POTATO_PAD) * POTATO_SCALE,
        (POTATO_H + 2 * POTATO_PAD) * POTATO_SCALE,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = NativeCanvas(bitmap)
    canvas.scale(POTATO_SCALE.toFloat(), POTATO_SCALE.toFloat())
    canvas.translate(POTATO_PAD.toFloat(), POTATO_PAD.toFloat())
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    fill.color = 0xFFC98E52.toInt()
    fill.setShadowLayer(24f, 0f, 18f, 0x59000000)
    canvas.drawPath(svgPath("M24 92C18 60 48 38 92 36c30-1 44-14 70-6 26 8 34 38 26 62-9 28-40 44-82 46-40 2-76-14-82-46z").asAndroidPath(), fill)
    fill.clearShadowLayer()

    fill.color = 0xFFA86F3A.toInt()
    canvas.drawPath(svgPath("M188 92c-9 28-40 44-82 46-40 2-76-14-82-46 12 22 44 34 82 32 38-2 68-12 82-32z").asAndroidPath(), fill)

    val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE7B57E.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 9f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawPath(svgPath("M50 64c14-12 34-17 56-17").asAndroidPath(), shine)

    fill.color = 0xFF8E5A2C.toInt()
    fun eye(cx: Float, cy: Float, rx: Float, ry: Float) = canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), fill)
    eye(66f, 90f, 6f, 4f)
    eye(128f, 58f, 5f, 3.5f)
    eye(156f, 96f, 6f, 4f)
    eye(104f, 112f, 4f, 3f)

    bitmap.asImageBitmap()
}

@Composable
fun Potato(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = min(size.width / POTATO_W, size.height / POTATO_H)
        val w = (POTATO_W + 2 * POTATO_PAD) * s
        val h = (POTATO_H + 2 * POTATO_PAD) * s
        drawImage(
            PotatoImage,
            dstOffset = IntOffset(((size.width - w) / 2).roundToInt(), ((size.height - h) / 2).roundToInt()),
            dstSize = IntSize(w.roundToInt(), h.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}

private val SteamPath: Path by lazy { svgPath("M20 45c-8-10 8-18 0-30M60 45c-8-10 8-18 0-30M100 45c-8-10 8-18 0-30") }

// opacity é lida só ao desenhar.
@Composable
fun Steam(modifier: Modifier = Modifier, opacity: () -> Float) {
    val rise = rememberInfiniteTransition()
    val y by rise.animateFloat(0f, -8f, infiniteRepeatable(tween(600, easing = Ease), RepeatMode.Reverse))
    Canvas(
        modifier.graphicsLayer {
            alpha = opacity()
            translationY = y * density
        }
    ) {
        scale(size.width / 120f, pivot = Offset.Zero) {
            drawPath(SteamPath, ErrorColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
        }
    }
}

// Avatar: sprite em pixel art sobre um fundo tonal com a cor do jogador
@Composable
fun AvatarBox(avatar: Int, color: Color, size: Dp, pad: Dp, radius: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(color, rounded(radius)).padding(pad)) {
        Image(
            AVATARS[avatar].sprite,
            null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
        )
    }
}
