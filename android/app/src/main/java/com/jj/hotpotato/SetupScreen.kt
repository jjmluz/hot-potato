package com.jj.hotpotato

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Foco estilo Material para TV: superfície invertida e ligeiro aumento
@Stable
class FocusLook(background: State<Color>, content: State<Color>, scale: State<Float>) {
    val background by background
    val content by content
    val scale by scale
}

@Composable
fun focusLook(focus: Boolean, idle: Color, idleContent: Color, zoom: Float): FocusLook {
    val background = animateColorAsState(if (focus) InverseSurface else idle, tween(200, easing = Ease))
    val content = animateColorAsState(if (focus) InverseOnSurface else idleContent, tween(200, easing = Ease))
    val scale = animateFloatAsState(if (focus) zoom else 1f, tween(250, easing = EaseSpring))
    return remember { FocusLook(background, content, scale) }
}

@Composable
fun SetupScreen(game: Game) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 112.dp, vertical = 64.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.width(820.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Brand()
            T(
                stringResource(R.string.intro),
                34.sp,
                420,
                Modifier.widthIn(max = 780.dp),
                color = OnSurfaceVariant,
                lineHeight = 49.sp,
            )
            Column(
                Modifier.background(SurfaceContainer, rounded(48)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Option(stringResource(R.string.players), stringResource(R.string.players_hint), game.numPlayers, game.setupFocus == 0)
                Option(stringResource(R.string.lives), stringResource(R.string.lives_hint), game.lives, game.setupFocus == 1)
                Toggle(stringResource(R.string.kids), stringResource(R.string.kids_hint), game.kids, game.setupFocus == 2)
            }
            Spacer(Modifier.weight(1f))
            KeyHints()
        }

        Column(
            Modifier
                .width(840.dp)
                .align(Alignment.CenterVertically)
                .background(SurfaceContainer, rounded(48))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until game.numPlayers) PlayerRow(game, i)
            }
            Fab(
                stringResource(R.string.start),
                focus = game.setupFocus == game.numPlayers + SEAT_FOCUS,
                primary = false,
                height = 104.dp,
                Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun Brand() {
    Row(
        Modifier.offset(x = (-20).dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Cookie(40000, Modifier.fillMaxSize())
            Potato(Modifier.size(148.dp))
        }
        Column {
            T(stringResource(R.string.brand_tagline), 30.sp, 600, Modifier.padding(bottom = 12.dp), color = Primary, spacing = .5.sp)
            T(stringResource(R.string.brand_title), 116.sp, 750, lineHeight = 107.sp, spacing = (-3).sp)
        }
    }
}

@Composable
private fun Option(title: String, subtitle: String, value: Int, focus: Boolean) {
    OptionRow(title, subtitle, focus) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            StepButton(IconLeft, focus)
            Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) { T("$value", 56.sp, 700) }
            StepButton(IconRight, focus)
        }
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, on: Boolean, focus: Boolean) {
    OptionRow(title, subtitle, focus) { Switch(on) }
}

@Composable
private fun OptionRow(title: String, subtitle: String, focus: Boolean, control: @Composable () -> Unit) {
    val look = focusLook(focus, Color.Transparent, OnSurface, 1.03f)
    CompositionLocalProvider(LocalContent provides look.content) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = look.scale
                    scaleY = look.scale
                }
                .background(look.background, rounded(32))
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                T(title, 40.sp, 600)
                T(subtitle, 26.sp, 450, color = if (focus) Color(0xFF6B5B51) else OnSurfaceVariant)
            }
            control()
        }
    }
}

// Interruptor do Material 3: ligado, a pega cresce e encosta à direita com um visto.
@Composable
private fun Switch(on: Boolean) {
    val track by animateColorAsState(if (on) Primary else SurfaceContainerHighest, tween(200, easing = Ease))
    val handle by animateColorAsState(if (on) OnPrimary else OnSurfaceVariant, tween(200, easing = Ease))
    val shift by animateFloatAsState(if (on) 1f else 0f, tween(250, easing = EaseSpring))
    Box(
        Modifier
            .size(width = 120.dp, height = 68.dp)
            .background(track, CircleShape)
            .border(4.dp, if (on) Color.Transparent else OnSurfaceVariant, CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset { IntOffset((shift * 52.dp.toPx()).toInt(), 0) }
                .size(52.dp)
                .graphicsLayer {
                    val s = lerp(.62f, 1f, shift)
                    scaleX = s
                    scaleY = s
                }
                .background(handle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (on) Icon(IconCheck, 32.dp, tint = Primary)
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, focus: Boolean) {
    Box(
        Modifier.size(64.dp).background(if (focus) PrimaryContainer else SurfaceContainerHighest, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, 36.dp, tint = if (focus) OnPrimaryContainer else OnSurfaceVariant)
    }
}

@Composable
private fun KeyHints() {
    Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
        KeyHint("▲▼", stringResource(R.string.hint_choose))
        KeyHint("◀▶", stringResource(R.string.hint_change))
        KeyHint("OK", stringResource(R.string.hint_name))
    }
}

@Composable
private fun KeyHint(key: String, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.height(44.dp).border(2.dp, OutlineVariant, rounded(12)).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            T(key, 26.sp, 600, color = OnSurface)
        }
        T(label, 26.sp, 420, color = OnSurfaceVariant)
    }
}

@Composable
private fun PlayerRow(game: Game, i: Int) {
    val seat = game.roster[i]
    val editing = game.editing == i
    val focus = game.setupFocus == i + SEAT_FOCUS && game.editing == null
    val look = focusLook(focus, if (editing) SurfaceContainerHighest else Color.Transparent, OnSurface, 1.02f)
    CompositionLocalProvider(LocalContent provides look.content) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(86.dp)
                .graphicsLayer {
                    scaleX = look.scale
                    scaleY = look.scale
                }
                .background(look.background, rounded(28))
                .padding(start = 8.dp, end = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(IconLeft, 32.dp, Modifier.alpha(if (focus) 1f else 0f))
                AvatarBox(seat.avatar, PLAYER_COLORS[i], 68.dp, 6.dp, 22)
                Icon(IconRight, 32.dp, Modifier.alpha(if (focus) 1f else 0f))
            }
            if (editing) {
                NameField(game, i)
            } else {
                T(
                    seat.name.ifEmpty { game.defaultName(i) },
                    36.sp,
                    600,
                    Modifier.weight(1f).padding(horizontal = 18.dp),
                )
            }
            Icon(
                IconEdit,
                36.dp,
                Modifier.alpha(if (focus || editing) 1f else .6f),
                tint = when {
                    editing -> Primary
                    focus -> InverseOnSurface
                    else -> OnSurfaceVariant
                },
            )
        }
    }
}

// A editar o nome: campo preenchido do Material 3, ligado ao teclado do sistema.
@Composable
private fun RowScope.NameField(game: Game, i: Int) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    BasicTextField(
        game.nameField,
        Modifier
            .weight(1f)
            .height(64.dp)
            .focusRequester(focusRequester)
            .background(SurfaceContainerHigh, RoundedCornerShape(16.dp, 16.dp))
            .drawBehind {
                val line = 4.dp.toPx()
                drawRect(Primary, Offset(0f, size.height - line), Size(size.width, line))
            }
            .padding(horizontal = 18.dp),
        inputTransformation = InputTransformation.maxLength(MAX_NAME),
        textStyle = textStyle(36.sp, 600, OnSurface),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        onKeyboardAction = { game.stopEditing(true) },
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(Primary),
        decorator = { field ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (game.nameField.text.isEmpty()) T(game.defaultName(i), 36.sp, 600, color = OnSurface.copy(alpha = .45f))
                field()
            }
        },
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
}
