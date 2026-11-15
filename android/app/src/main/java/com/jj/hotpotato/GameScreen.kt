package com.jj.hotpotato

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Posições lidas ao desenhar a explosão, para ela nascer na batata.
private class Anchors {
    var screen: LayoutCoordinates? = null
    var potato: LayoutCoordinates? = null

    fun potatoCenter(): Offset? {
        val from = screen ?: return null
        val to = potato ?: return null
        return from.localPositionOf(to, Offset(to.size.width / 2f, to.size.height / 2f))
    }
}

@Composable
fun GameScreen(game: Game) {
    val anchors = remember { Anchors() }
    Box(Modifier.fillMaxSize().onPlaced { anchors.screen = it }) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val (x, y) = quake(game.boom)
                    translationX = x * density
                    translationY = y * density
                }
                .padding(start = 112.dp, end = 112.dp, top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(game)
            QuestionCard(game)
            Board(game, anchors)
        }
        Snackbar(game, Modifier.align(Alignment.BottomCenter))
        BoomFlash(game, anchors)
    }
}

@Composable
private fun TopBar(game: Game) {
    val holder = game.players[game.holder]
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBox(holder.avatar, holder.color, 96.dp, 8.dp, 32)
            Column {
                T(stringResource(R.string.holder), 26.sp, 500, color = OnSurfaceVariant)
                T(holder.name, 52.sp, 700, spacing = (-.5).sp)
            }
        }
        val compact = game.players.size > 4
        FlowRow(
            Modifier.widthIn(max = if (compact) 1240.dp else 1150.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            game.players.forEachIndexed { i, p -> Chip(p, i == game.holder, game.lives, compact) }
        }
    }
}

@Composable
private fun QuestionCard(game: Game) {
    val current = game.current ?: return
    Column(
        Modifier
            .padding(top = 40.dp)
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .background(SurfaceContainer, rounded(40))
            .padding(horizontal = 56.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        T(
            current.category,
            24.sp,
            600,
            Modifier
                .background(if (current.kids) PrimaryContainer else TertiaryContainer, rounded(12))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            color = if (current.kids) OnPrimaryContainer else OnTertiaryContainer,
            spacing = .5.sp,
        )
        T(
            current.text,
            56.sp,
            600,
            Modifier.widthIn(max = 1500.dp),
            lineHeight = 67.sp,
            spacing = (-.5).sp,
            align = TextAlign.Center,
            balance = true,
        )
    }
}

@Composable
private fun Board(game: Game, anchors: Anchors) {
    Column(
        Modifier.padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Answer(game, Key.UP)
        Row(
            Modifier.height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Answer(game, Key.LEFT)
            HotPotato(game, anchors)
            Answer(game, Key.RIGHT)
        }
        Answer(game, Key.DOWN)
    }
}

@Composable
private fun Answer(game: Game, dir: Key) {
    val current = game.current ?: return
    val answered = game.answered
    val correct = answered != null && dir == current.correct
    val wrong = answered == dir && !correct
    val dim = answered != null && !correct && !wrong

    val spec = tween<Color>(250, easing = Ease)
    val background by animateColorAsState(
        when {
            correct -> SuccessContainer
            wrong -> ErrorContainer
            else -> SurfaceContainerHigh
        },
        spec,
    )
    val content by animateColorAsState(
        when {
            correct -> OnSuccessContainer
            wrong -> OnErrorContainer
            else -> OnSurface
        },
        spec,
    )
    val keyBackground by animateColorAsState(
        when {
            correct -> Success
            wrong -> ErrorColor
            else -> PrimaryContainer
        },
        spec,
    )
    val keyContent by animateColorAsState(
        when {
            correct -> Color(0xFF0A3912)
            wrong -> Color(0xFF690005)
            else -> OnPrimaryContainer
        },
        spec,
    )
    val opacity by animateFloatAsState(if (dim) .38f else 1f, tween(250, easing = Ease))
    val scale by animateFloatAsState(if (correct) 1.04f else 1f, tween(300, easing = EaseSpring))
    val radius by animateDpAsState(if (correct) 66.dp else 36.dp, tween(300, easing = Ease))

    val right = dir == Key.RIGHT
    val arrow = when (dir) {
        Key.RIGHT -> 90f
        Key.DOWN -> 180f
        Key.LEFT -> -90f
        else -> 0f
    }
    val key = @Composable {
        Box(
            Modifier.size(92.dp).background(keyBackground, rounded(28)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IconArrow, 48.dp, Modifier.rotate(arrow), tint = keyContent)
        }
    }
    val label = @Composable { modifier: Modifier ->
        // Encolhe a letra se a resposta não couber em duas linhas.
        BasicText(
            current.slots.getValue(dir),
            modifier,
            style = TextStyle(
                color = content,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight(550),
                textAlign = if (right) TextAlign.End else TextAlign.Start,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 38.sp, stepSize = 2.sp),
        )
    }
    Row(
        Modifier
            .width(620.dp)
            .height(124.dp)
            .graphicsLayer {
                alpha = opacity
                scaleX = scale
                scaleY = scale
            }
            .background(background, RoundedCornerShape(radius))
            .padding(start = if (right) 36.dp else 20.dp, end = if (right) 20.dp else 36.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (right) {
            label(Modifier.weight(1f))
            key()
        } else {
            key()
            label(Modifier.weight(1f))
        }
    }
}

// Batata: a forma de fundo aquece de âmbar para vermelho.
// Tudo o que anima aqui é lido em lambdas de desenho, por isso nada se recompõe a cada frame.
@Composable
private fun HotPotato(game: Game, anchors: Anchors) {
    Box(
        Modifier.requiredSize(300.dp, 220.dp).onPlaced { anchors.potato = it },
        contentAlignment = Alignment.Center,
    ) {
        Cookie(14000, Modifier.size(236.dp)) { lerpColor(PrimaryContainer, ErrorContainer, game.heat) }
        Potato(
            Modifier.size(200.dp, 170.dp).graphicsLayer {
                val heat = game.heat
                val (x, y, r) = shake(game.shakePhase, heat)
                val pulse = if (game.pulseLeft > 0f) 1.08f else 1f
                translationX = x * density
                translationY = y * density
                rotationZ = r
                scaleX = pulse
                scaleY = pulse
            }
        )
        Steam(Modifier.align(Alignment.TopCenter).offset(y = (-34).dp).size(120.dp, 50.dp)) {
            (game.heat * 1.4f - .3f).coerceIn(0f, 1f)
        }
    }
}

private val SHAKE_X = floatArrayOf(0f, -1f, 0f, 1f, 0f)
private val SHAKE_Y = floatArrayOf(0f, 4f, -5f, 4f, 0f)

private class Shake(val x: Float, val y: Float, val r: Float) {
    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = r
}

private fun shake(phase: Float, heat: Float): Shake {
    val pos = phase * 4f
    val i = pos.toInt().coerceAtMost(3)
    val t = pos - i
    val swing = lerp(SHAKE_X[i], SHAKE_X[i + 1], t)
    return Shake(
        x = swing * (heat * 10f + 1f),
        y = lerp(SHAKE_Y[i], SHAKE_Y[i + 1], t) * heat,
        r = swing * (heat * 9f + 1.5f),
    )
}

private val QUAKE_X = floatArrayOf(0f, -20f, 16f, -10f, 6f, 0f)
private val QUAKE_Y = floatArrayOf(0f, 10f, -12f, 6f, -3f, 0f)
private const val QUAKE_SHARE = 1f / 3f    // meio segundo dos 1,5 s da explosão

private fun quake(boom: Float): Pair<Float, Float> {
    if (boom !in 0f..<QUAKE_SHARE) return 0f to 0f
    val pos = boom / QUAKE_SHARE * 5f
    val i = pos.toInt().coerceAtMost(4)
    val t = pos - i
    return lerp(QUAKE_X[i], QUAKE_X[i + 1], t) to lerp(QUAKE_Y[i], QUAKE_Y[i + 1], t)
}

@Composable
private fun Snackbar(game: Game, modifier: Modifier) {
    val feedback = game.feedback ?: return
    val hidden by animateFloatAsState(if (game.feedbackShown) 0f else 1f, tween(400, easing = EaseSpring))
    val shape = rounded(28)
    CompositionLocalProvider(LocalContent provides InverseOnSurface) {
        Row(
            modifier
                .padding(bottom = 28.dp)
                .graphicsLayer { translationY = hidden * 160f * density }
                .shadow(12.dp, shape)
                .background(InverseSurface, shape)
                .padding(start = 28.dp, end = 40.dp, top = 24.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(if (feedback.good) SuccessContainer else ErrorContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (feedback.good) IconCheck else IconClose,
                    32.dp,
                    tint = if (feedback.good) OnSuccessContainer else OnErrorContainer,
                )
            }
            val text = if (feedback.good) {
                stringResource(R.string.correct, feedback.next.orEmpty())
            } else {
                stringResource(R.string.wrong)
            }
            T(text, 36.sp, 600)
        }
    }
}

// Explosão: círculo que cresce a partir da batata e depois se funde com o fundo.
@Composable
private fun BoomFlash(game: Game, anchors: Anchors) {
    Canvas(Modifier.fillMaxSize()) {
        val boom = game.boom
        if (boom < 0f) return@Canvas
        val center = anchors.potatoCenter() ?: return@Canvas
        val radius = 2400.dp.toPx() * Ease.transform((boom / .55f).coerceAtMost(1f))
        drawCircle(ErrorColor, radius, center)
        drawCircle(Surface, radius, center, alpha = ((boom - .45f) / .55f).coerceIn(0f, 1f))
    }
}
