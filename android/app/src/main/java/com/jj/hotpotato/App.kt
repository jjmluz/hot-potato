package com.jj.hotpotato

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

// Palco fixo 1920x1080, escalado para caber no ecrã: 1.dp é sempre um píxel do palco.
@Composable
fun App(game: Game) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Surface), contentAlignment = Alignment.Center) {
        val scale = min(constraints.maxWidth / 1920f, constraints.maxHeight / 1080f)
        CompositionLocalProvider(LocalDensity provides Density(scale, 1f)) {
            Box(Modifier.requiredSize(1920.dp, 1080.dp)) {
                when (game.screen) {
                    Screen.SETUP -> SetupScreen(game)
                    Screen.GAME -> GameScreen(game)
                    Screen.EXPLOSION -> ExplosionScreen(game)
                    Screen.WINNER -> WinnerScreen(game)
                }
                if (game.paused) PauseOverlay(game)
            }
        }
    }

    // Ciclo principal: só corre durante o jogo e fora de pausa.
    if (game.screen == Screen.GAME && !game.paused) {
        LaunchedEffect(Unit) {
            var last = withFrameMillis { it }
            while (true) {
                withFrameMillis { now ->
                    game.frame(min(100L, now - last).toFloat())
                    last = now
                }
            }
        }
    }
}

// ---------- Peças partilhadas ----------

@Composable
fun Hearts(p: Player, total: Int, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(total) { i ->
            Icon(IconHeart, size, tint = if (i < p.lives) ErrorColor else OutlineVariant)
        }
    }
}

@Composable
fun Chip(p: Player, holder: Boolean, total: Int, compact: Boolean = false) {
    val shape = rounded(if (compact) 16 else 20)
    val out = p.lives == 0
    var box = Modifier.height(if (compact) 52.dp else 64.dp).alpha(if (out) .38f else 1f)
    box = if (holder) box.background(SecondaryContainer, shape) else box.border(2.dp, OutlineVariant, shape)
    Row(
        box.padding(start = if (compact) 6.dp else 12.dp, end = if (compact) 14.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBox(p.avatar, p.color, if (compact) 40.dp else 44.dp, 4.dp, if (compact) 12 else 14)
        T(
            p.name,
            if (compact) 22.sp else 26.sp,
            550,
            color = if (holder) OnSecondaryContainer else OnSurfaceVariant,
            strike = out,
        )
        Hearts(p, total, if (compact) 18.dp else 22.dp)
    }
}

// Botão principal. Em foco cresce e fica mais redondo.
@Composable
fun Fab(
    text: String,
    focus: Boolean,
    primary: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val active = focus || primary
    val scale by animateFloatAsState(if (focus) 1.04f else 1f, tween(250, easing = EaseSpring))
    val radius by animateDpAsState(if (focus) 56.dp else 32.dp, tween(300, easing = Ease))
    val tint = if (active) OnPrimary else OnPrimaryContainer
    CompositionLocalProvider(LocalContent provides tint) {
        Row(
            modifier
                .fillMaxWidth()
                .height(height)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(if (active) Primary else PrimaryContainer, RoundedCornerShape(radius)),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(IconPlay, 48.dp)
            T(text, 40.sp, 650)
        }
    }
}

// ---------- Diálogos (explosão, vencedor, pausa) ----------

@Composable
fun Dialog(width: Dp = 1080.dp, content: @Composable ColumnScope.() -> Unit) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(450, easing = EaseSpring)) }
    Column(
        Modifier
            .width(width)
            .graphicsLayer {
                val v = pop.value
                scaleX = .9f + .1f * v
                scaleY = scaleX
                alpha = v.coerceIn(0f, 1f)
            }
            .background(SurfaceContainerHigh, rounded(56))
            .padding(start = 80.dp, end = 80.dp, top = 72.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun BodyText(text: String) {
    T(text, 34.sp, 420, color = OnSurfaceVariant, lineHeight = 49.sp, align = TextAlign.Center)
}

private const val NAME_MARK = "\uE000"

@Composable
fun ExplosionScreen(game: Game) {
    val loser = game.players[game.loser]
    Centered {
        Dialog {
            Box(Modifier.size(128.dp).background(ErrorContainer, rounded(40)), contentAlignment = Alignment.Center) {
                Icon(IconFire, 72.dp, tint = OnErrorContainer)
            }
            T(stringResource(R.string.exploded), 64.sp, 650, spacing = (-.5).sp)
            // O nome entra como marcador, para ficar a negrito onde a frase o puser.
            val message = if (loser.lives == 0) {
                stringResource(R.string.eliminated, NAME_MARK)
            } else {
                pluralStringResource(R.plurals.lost_life, loser.lives, NAME_MARK, loser.lives)
            }
            BasicText(
                buildAnnotatedString {
                    val (before, after) = message.split(NAME_MARK, limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                    append(before)
                    withStyle(SpanStyle(color = OnSurface, fontWeight = FontWeight(650))) { append(loser.name) }
                    append(after)
                },
                style = textStyle(34.sp, 420, OnSurfaceVariant, lineHeight = 49.sp, align = TextAlign.Center),
            )
            FlowRow(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                game.players.forEachIndexed { i, p -> Chip(p, i == game.loser, game.lives) }
            }
            Fab(stringResource(R.string.continue_game), focus = false, primary = true, height = 112.dp, Modifier.padding(top = 24.dp))
        }
    }
}

@Composable
fun WinnerScreen(game: Game) {
    val winner = game.winner
    Centered {
        Dialog {
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Cookie(40000, Modifier.fillMaxSize())
                AvatarBox(winner.avatar, winner.color, 132.dp, 10.dp, 40)
            }
            T(stringResource(R.string.winner, winner.name), 80.sp, 650, spacing = (-.5).sp, align = TextAlign.Center)
            BodyText(stringResource(R.string.survived))
            Fab(stringResource(R.string.play_again), focus = false, primary = true, height = 112.dp, Modifier.padding(top = 24.dp))
        }
    }
}

@Composable
fun PauseOverlay(game: Game) {
    Box(Modifier.fillMaxSize().background(Color(0xB80C0806)), contentAlignment = Alignment.Center) {
        Dialog(width = 760.dp) {
            T(stringResource(R.string.paused), 64.sp, 650, spacing = (-.5).sp)
            BodyText(stringResource(R.string.cooling))
            Column(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MenuItem(stringResource(R.string.continue_game), game.pauseFocus == 0)
                MenuItem(stringResource(R.string.back_to_start), game.pauseFocus == 1)
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, focus: Boolean) {
    val look = focusLook(focus, SurfaceContainerHighest, OnSurface, 1.03f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(104.dp)
            .graphicsLayer {
                scaleX = look.scale
                scaleY = look.scale
            }
            .background(look.background, rounded(28)),
        contentAlignment = Alignment.Center,
    ) {
        T(text, 38.sp, 600, color = look.content)
    }
}
