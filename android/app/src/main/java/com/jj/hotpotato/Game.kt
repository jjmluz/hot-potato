package com.jj.hotpotato

import android.content.SharedPreferences
import android.content.res.AssetManager
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

// Tons 80 de várias cores, para combinarem com o esquema Material You
val PLAYER_COLORS = listOf(0xFFFFB4A8, 0xFFA8C8FF, 0xFFA6D68A, 0xFFECC45F, 0xFFD3BBFF, 0xFF80D5CF).map(::Color)
private const val FUSE_MIN = 25000f   // a batata rebenta entre 25 e 55 segundos
private const val FUSE_MAX = 55000f
private const val GRACE_MS = 3000f   // tempo mínimo de quem recebe a batata
private const val FEEDBACK_MS = 1600f
private const val BOOM_MS = 1500f
private const val PULSE_MS = 70f
const val MAX_PLAYERS = 6
const val MAX_NAME = 14
const val SEAT_FOCUS = 3   // foco do primeiro lugar no ecrã inicial, depois das opções
// O avatar por omissão de cada lugar é o de índice igual em AVATARS

enum class Screen { SETUP, GAME, EXPLOSION, WINNER }
enum class Key { UP, LEFT, RIGHT, DOWN, OK, BACK }
val DIRS = listOf(Key.UP, Key.LEFT, Key.RIGHT, Key.DOWN)

class Seat(val name: String, val avatar: Int)

class Player(val name: String, val avatar: Int, val color: Color, lives: Int) {
    var lives by mutableIntStateOf(lives)
}

// Formato: categoria, pergunta, resposta certa, errada, errada, errada (separados por tabs).
class Question(val category: String, val text: String, val answers: List<String>)

class Current(val category: String, val text: String, val kids: Boolean, val slots: Map<Key, String>, val correct: Key)

// Resultado da resposta; o texto fica a cargo do ecrã. next é quem recebe a batata, se acertou.
class Feedback(val good: Boolean, val next: String?)

private class Pending(var left: Float, val fn: () -> Unit)

fun loadQuestions(assets: AssetManager, file: String): List<Question> =
    assets.open(file).bufferedReader().readLines().map { line ->
        val cols = line.split('\t')
        Question(cols[0], cols[1], cols.drop(2))
    }

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

// Lógica do jogo. O estado é observável pelo Compose, sem depender de nenhum ecrã.
class Game(
    private val questions: List<Question>,
    private val kidsQuestions: List<Question>,
    private val prefs: SharedPreferences,
    private val sound: Sound,
    private val numberedName: (Int) -> String,    // nome de um lugar sem nome ("Jogador 1"), vindo dos recursos
) {
    var screen by mutableStateOf(Screen.SETUP); private set
    var setupFocus by mutableIntStateOf(0); private set
    var numPlayers by mutableIntStateOf(3); private set
    var lives by mutableIntStateOf(3); private set
    var kids by mutableStateOf(false); private set    // perguntas infantis para todos os jogadores
    val roster = mutableStateListOf<Seat>()    // nome e avatar de cada lugar, configurados no ecrã inicial
    var editing by mutableStateOf<Int?>(null); private set    // lugar com o nome em edição
    val nameField = TextFieldState()
    var players by mutableStateOf<List<Player>>(emptyList()); private set
    var holder by mutableIntStateOf(0); private set
    var loser by mutableIntStateOf(-1); private set
    var current by mutableStateOf<Current?>(null); private set
    var answered by mutableStateOf<Key?>(null); private set
    var feedback by mutableStateOf<Feedback?>(null); private set
    var feedbackShown by mutableStateOf(false); private set
    var paused by mutableStateOf(false); private set
    var pauseFocus by mutableIntStateOf(0); private set

    // Animação da batata: só lida ao desenhar, para nunca recompor o ecrã a cada frame.
    var heat by mutableFloatStateOf(0f); private set
    var shakePhase by mutableFloatStateOf(0f); private set
    var pulseLeft by mutableFloatStateOf(0f); private set
    var boom by mutableFloatStateOf(-1f); private set    // progresso da explosão (0 a 1), -1 fora dela

    private var fuseTotal = 0f
    private var fuseElapsed = 0f
    private var tickIn = 0f
    private var adultDeck = ArrayDeque<Int>()   // perguntas por sair, uma pilha por tipo
    private var kidsDeck = ArrayDeque<Int>()
    private var locked = false
    private var pending: Pending? = null

    init {
        loadSettings()
        showSetup()
    }

    fun defaultName(i: Int) = numberedName(i)
    private fun displayName(i: Int) = roster[i].name.trim().ifEmpty { defaultName(i) }

    private fun alivePlayers() = players.filter { it.lives > 0 }

    private fun nextAlive(from: Int): Int {
        val n = players.size
        for (i in 1..n) {
            val idx = (from + i) % n
            if (players[idx].lives > 0) return idx
        }
        return from
    }

    // Executa fn após ms, mas só conta o tempo enquanto o jogo não está em pausa.
    private fun schedule(ms: Float, fn: () -> Unit) {
        pending = Pending(ms, fn)
    }

    // ---------- Ecrã inicial ----------

    // Nomes, avatares e opções ficam guardados na TV para a próxima vez.
    private fun loadSettings() {
        for (i in 0 until MAX_PLAYERS) {
            val name = prefs.getString("name$i", "").orEmpty().take(MAX_NAME)
            roster.add(Seat(name, prefs.getInt("avatar$i", i) % AVATARS.size))
        }
        numPlayers = prefs.getInt("numPlayers", numPlayers).coerceIn(2, MAX_PLAYERS)
        lives = prefs.getInt("lives", lives).coerceIn(1, 5)
        kids = prefs.getBoolean("kids", kids)
    }

    private fun saveSettings() {
        val edit = prefs.edit()
        roster.forEachIndexed { i, seat ->
            edit.putString("name$i", seat.name).putInt("avatar$i", seat.avatar)
        }
        edit.putInt("numPlayers", numPlayers).putInt("lives", lives).putBoolean("kids", kids).apply()
    }

    private fun showSetup() {
        screen = Screen.SETUP
        setupFocus = numPlayers + SEAT_FOCUS
    }

    // Ordem de navegação: opções (jogadores, vidas, crianças), um lugar por jogador, botão Começar.
    private fun onSetupKey(key: Key) {
        val count = numPlayers + SEAT_FOCUS + 1
        val seat = setupFocus - SEAT_FOCUS
        when (key) {
            Key.UP -> setupFocus = (setupFocus - 1 + count) % count
            Key.DOWN -> setupFocus = (setupFocus + 1) % count
            Key.LEFT, Key.RIGHT -> {
                val d = if (key == Key.LEFT) -1 else 1
                when {
                    setupFocus == 0 -> numPlayers = (numPlayers + d).coerceIn(2, MAX_PLAYERS)
                    setupFocus == 1 -> lives = (lives + d).coerceIn(1, 5)
                    setupFocus == 2 -> kids = !kids
                    seat < numPlayers -> {
                        val avatar = (roster[seat].avatar + d + AVATARS.size) % AVATARS.size
                        roster[seat] = Seat(roster[seat].name, avatar)
                    }
                    else -> return
                }
                saveSettings()
            }
            Key.OK -> {
                if (setupFocus == 2) {
                    kids = !kids
                    saveSettings()
                    sound.blip()
                } else if (seat in 0 until numPlayers) {
                    sound.blip()
                    startEditing(seat)
                } else {
                    startGame()
                }
                return
            }
            Key.BACK -> return
        }
        sound.blip()
    }

    private fun startEditing(seat: Int) {
        nameField.setTextAndSelectAll(roster[seat].name)
        editing = seat
    }

    fun stopEditing(commit: Boolean) {
        val seat = editing ?: return
        editing = null
        if (commit) {
            val name = nameField.text.toString().trim().take(MAX_NAME)
            roster[seat] = Seat(name, roster[seat].avatar)
        }
        saveSettings()
    }

    // Enquanto se escreve um nome, as teclas vão para o campo, exceto OK, Voltar e ▲▼.
    fun onEditingKey(key: Key) {
        stopEditing(true)
        if (key == Key.OK) sound.blip() else handleKey(key)
    }

    // ---------- Jogo ----------

    private fun startGame() {
        saveSettings()
        players = List(numPlayers) { i ->
            Player(displayName(i), roster[i].avatar, PLAYER_COLORS[i], lives)
        }
        holder = Random.nextInt(players.size)
        adultDeck = ArrayDeque()
        kidsDeck = ArrayDeque()
        startRound()
    }

    private fun startRound() {
        fuseTotal = lerp(FUSE_MIN, FUSE_MAX, Random.nextFloat())
        fuseElapsed = 0f
        tickIn = 0f
        pending = null
        heat = 0f
        screen = Screen.GAME
        nextQuestion()
    }

    // Com a opção das crianças ligada, todos recebem as perguntas infantis.
    private fun drawQuestion(kids: Boolean): Question {
        val pool = if (kids) kidsQuestions else questions
        val deck = if (kids) kidsDeck else adultDeck
        if (deck.isEmpty()) deck.addAll(pool.indices.shuffled())
        return pool[deck.removeLast()]
    }

    private fun nextQuestion() {
        val question = drawQuestion(kids)
        val order = (0..3).shuffled()
        val slots = DIRS.indices.associate { i -> DIRS[i] to question.answers[order[i]] }
        current = Current(question.category, question.text, kids, slots, DIRS[order.indexOf(0)])
        answered = null
        feedbackShown = false
        locked = false
    }

    private fun showFeedback(good: Boolean, next: String? = null) {
        feedback = Feedback(good, next)
        feedbackShown = true
    }

    private fun answer(dir: Key) {
        if (locked) return
        locked = true
        answered = dir
        if (dir == current?.correct) {
            sound.correct()
            // Quem recebe a batata tem sempre uns segundos, para não perder por quem respondeu à última hora.
            fuseTotal = maxOf(fuseTotal, fuseElapsed + GRACE_MS)
            val next = nextAlive(holder)
            showFeedback(good = true, next = players[next].name)
            schedule(FEEDBACK_MS) {
                holder = next
                nextQuestion()
            }
        } else {
            sound.wrong()
            showFeedback(good = false)
            schedule(FEEDBACK_MS + 400f, ::nextQuestion)
        }
    }

    private fun updateFuse(dt: Float) {
        fuseElapsed += dt
        // O calor sobe em função do tempo máximo, para não denunciar quando vai rebentar.
        heat = (fuseElapsed / FUSE_MAX).coerceIn(0f, 1f)

        tickIn -= dt
        if (tickIn <= 0f) {
            sound.tick(heat)
            tickIn = lerp(650f, 130f, heat)
            pulseLeft = PULSE_MS
        }

        if (fuseElapsed >= fuseTotal) explode()
    }

    private fun explode() {
        locked = true
        loser = holder
        players[loser].lives -= 1
        feedbackShown = false
        sound.boom()
        boom = 0f
        schedule(BOOM_MS) {
            boom = -1f
            screen = Screen.EXPLOSION
        }
    }

    private fun afterExplosion() {
        if (alivePlayers().size <= 1) {
            sound.win()
            screen = Screen.WINNER
            return
        }
        holder = nextAlive(loser)
        startRound()
    }

    val winner get() = alivePlayers().first()

    // Avança o jogo; só é chamado com o ecrã de jogo ativo e fora de pausa.
    fun frame(dt: Float) {
        shakePhase = (shakePhase + dt / lerp(800f, 150f, heat)) % 1f
        if (pulseLeft > 0f) pulseLeft -= dt
        val task = pending
        if (task != null) {
            task.left -= dt
            if (boom >= 0f) boom = 1f - (task.left / BOOM_MS).coerceAtLeast(0f)
            if (task.left <= 0f) {
                pending = null
                task.fn()
            }
        } else if (!locked) {
            updateFuse(dt)
        }
    }

    // ---------- Pausa ----------

    private fun showPause(value: Boolean) {
        paused = value
        pauseFocus = 0
    }

    private fun onPauseKey(key: Key) {
        when (key) {
            Key.UP, Key.DOWN -> {
                pauseFocus = 1 - pauseFocus
                sound.blip()
            }
            Key.BACK -> showPause(false)
            Key.OK -> {
                val quit = pauseFocus == 1
                showPause(false)
                if (quit) {
                    pending = null
                    boom = -1f
                    showSetup()
                }
            }
            else -> {}
        }
    }

    // ---------- Comando ----------

    fun handleKey(key: Key) {
        if (paused) {
            onPauseKey(key)
            return
        }
        when (screen) {
            Screen.SETUP -> onSetupKey(key)
            Screen.GAME -> if (key == Key.BACK) showPause(true) else if (key in DIRS) answer(key)
            Screen.EXPLOSION -> if (key == Key.OK) afterExplosion()
            Screen.WINNER -> if (key == Key.OK) showSetup()
        }
    }

    // Tecla Voltar. Devolve false para a app fechar.
    fun back(): Boolean {
        if (editing != null) {
            stopEditing(false)
            return true
        }
        if (screen == Screen.SETUP && !paused) return false
        if (screen == Screen.WINNER) showSetup() else handleKey(Key.BACK)
        return true
    }

    // Chamado quando a app sai de primeiro plano (ex.: botão Início).
    fun pause() {
        if (screen == Screen.GAME && !paused) showPause(true)
    }
}
