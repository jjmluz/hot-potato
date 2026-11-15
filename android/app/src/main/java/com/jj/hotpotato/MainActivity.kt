package com.jj.hotpotato

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    private val sound = Sound()
    private lateinit var game: Game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dir = getString(R.string.questions_dir)
        game = Game(
            // Perguntas na língua da app: português se o sistema estiver em português, senão inglês.
            questions = loadQuestions(assets, "$dir/questions.tsv"),
            kidsQuestions = loadQuestions(assets, "$dir/questions-kids.tsv"),
            prefs = getSharedPreferences("definicoes", Context.MODE_PRIVATE),
            sound = sound,
            numberedName = { getString(R.string.player_n, it + 1) },
        )
        setContent { App(game) }
    }

    // Todas as teclas do comando passam por aqui; o ecrã não usa o sistema de foco do Android.
    // Com o teclado do sistema aberto, é ele que recebe as teclas, incluindo o Voltar.
    // O lint marca por engano este método da Activity como interno da biblioteca.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val key = keyOf(event.keyCode) ?: return super.dispatchKeyEvent(event)
        val press = event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0
        when {
            key == Key.BACK -> {
                if (event.action == KeyEvent.ACTION_UP && !game.back()) finish()
            }
            game.editing == null -> if (press) game.handleKey(key)
            key == Key.LEFT || key == Key.RIGHT -> return super.dispatchKeyEvent(event)
            press -> game.onEditingKey(key)
        }
        return true
    }

    // O Voltar é uma tecla do comando, não um gesto, por isso é tratado aqui como as outras.
    @SuppressLint("GestureBackNavigation")
    private fun keyOf(code: Int) = when (code) {
        KeyEvent.KEYCODE_DPAD_UP -> Key.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> Key.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> Key.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> Key.RIGHT
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> Key.OK
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> Key.BACK
        else -> null
    }

    override fun onResume() {
        super.onResume()
        sound.start()
    }

    override fun onPause() {
        game.pause()
        sound.stop()
        super.onPause()
    }
}
