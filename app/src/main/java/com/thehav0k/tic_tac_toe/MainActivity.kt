package com.thehav0k.tic_tac_toe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehav0k.tic_tac_toe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            TicTacToeTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppContent(
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = !darkTheme }
                    )
                }
            }
        }
    }
}

enum class Screen { Splash, Menu, PvPSetup, PvP, BotSetup, Bot, Tutorial, Result }
enum class PlayerType { Human, Bot }
enum class Difficulty { Easy, Medium, Hard }

data class Player(
    val name: String,
    val symbol: String,
    val color: Color,
    val type: PlayerType = PlayerType.Human
)

@Composable
fun AppContent(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    var screen by remember { mutableStateOf(Screen.Splash) }
    var player1Name by remember { mutableStateOf("") }
    var player2Name by remember { mutableStateOf("") }
    var player1Symbol by remember { mutableStateOf("⭕") }
    var player2Symbol by remember { mutableStateOf("❌") }
    var botDifficulty by remember { mutableStateOf(Difficulty.Easy) }
    var board by remember { mutableStateOf(Array(3) { Array(3) { "" } }) }
    var currentPlayer by remember { mutableStateOf(1) }
    var winner by remember { mutableStateOf<String?>(null) }
    var draw by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var isBotThinking by remember { mutableStateOf(false) }
    var lightTheme by remember { mutableStateOf(!darkTheme) }
    val emojiOptions = listOf("⭕", "❌", "⭐", "🍀", "🐱", "🐶", "🍕", "🎲", "🎮", "🌈", "🔥", "💎")
    val accent = if (lightTheme) Color(0xFF1976D2) else Color(0xFF90CAF9)

    fun resetBoard() {
        board = Array(3) { Array(3) { "" } }
        currentPlayer = 1
        winner = null
        draw = false
        showResult = false
        isBotThinking = false
    }

    fun checkWinner(): String? {
        val lines = listOf(
            // Rows
            listOf(board[0][0], board[0][1], board[0][2]),
            listOf(board[1][0], board[1][1], board[1][2]),
            listOf(board[2][0], board[2][1], board[2][2]),
            // Columns
            listOf(board[0][0], board[1][0], board[2][0]),
            listOf(board[0][1], board[1][1], board[2][1]),
            listOf(board[0][2], board[1][2], board[2][2]),
            // Diagonals
            listOf(board[0][0], board[1][1], board[2][2]),
            listOf(board[0][2], board[1][1], board[2][0])
        )
        for (line in lines) {
            if (line[0] != "" && line[0] == line[1] && line[1] == line[2]) {
                return line[0]
            }
        }
        return null
    }

    fun isDraw(): Boolean = board.all { row -> row.all { it != "" } } && checkWinner() == null

    fun makeMove(row: Int, col: Int, symbol: String): Boolean {
        if (board[row][col] == "") {
            board = board.mapIndexed { r, arr ->
                arr.mapIndexed { c, v -> if (r == row && c == col) symbol else v }.toTypedArray()
            }.toTypedArray()
            return true
        }
        return false
    }

    fun randomMove(): Pair<Int, Int>? {
        val empty = board.flatMapIndexed { r, row -> row.mapIndexedNotNull { c, v -> if (v == "") r to c else null } }
        return if (empty.isNotEmpty()) empty.random() else null
    }

    fun blockOrRandomMove(symbol: String): Pair<Int, Int>? {
        // Try to win
        for (r in 0..2) for (c in 0..2) {
            if (board[r][c] == "") {
                board[r][c] = symbol
                if (checkWinner() == symbol) {
                    board[r][c] = ""
                    return Pair(r, c)
                }
                board[r][c] = ""
            }
        }
        // Try to block
        val opp = if (symbol == player1Symbol) player2Symbol else player1Symbol
        for (r in 0..2) for (c in 0..2) {
            if (board[r][c] == "") {
                board[r][c] = opp
                if (checkWinner() == opp) {
                    board[r][c] = ""
                    return Pair(r, c)
                }
                board[r][c] = ""
            }
        }
        // Else random
        return randomMove()
    }

    fun minimaxMove(symbol: String): Pair<Int, Int>? {
        val opp = if (symbol == player1Symbol) player2Symbol else player1Symbol
        var bestScore = Int.MIN_VALUE
        var move: Pair<Int, Int>? = null
        fun minimax(isMax: Boolean): Int {
            val win = checkWinner()
            if (win == symbol) return 1
            if (win == opp) return -1
            if (isDraw()) return 0
            var best = if (isMax) Int.MIN_VALUE else Int.MAX_VALUE
            for (r in 0..2) for (c in 0..2) {
                if (board[r][c] == "") {
                    board[r][c] = if (isMax) symbol else opp
                    val score = minimax(!isMax)
                    board[r][c] = ""
                    best = if (isMax) max(best, score) else min(best, score)
                }
            }
            return best
        }
        for (r in 0..2) for (c in 0..2) {
            if (board[r][c] == "") {
                board[r][c] = symbol
                val score = minimax(false)
                board[r][c] = ""
                if (score > bestScore) {
                    bestScore = score
                    move = Pair(r, c)
                }
            }
        }
        return move
    }

    fun afterMove() {
        winner = checkWinner()
        draw = isDraw()
        if (winner != null || draw) {
            showResult = true
        } else {
            currentPlayer = 3 - currentPlayer
        }
    }

    suspend fun botMove(symbol: String, difficulty: Difficulty) {
        isBotThinking = true
        delay(500)
        if (showResult) {
            isBotThinking = false
            return
        }
        val move: Pair<Int, Int>? = when (difficulty) {
            Difficulty.Easy -> randomMove()
            Difficulty.Medium -> blockOrRandomMove(symbol)
            Difficulty.Hard -> minimaxMove(symbol)
        }
        move?.let { movePair: Pair<Int, Int> ->
            val (r, c) = movePair
            makeMove(r, c, symbol)
            afterMove()
        }
        isBotThinking = false
    }

    Crossfade(targetState = screen, animationSpec = tween(500)) { scr ->
        when (scr) {
            Screen.Splash -> SplashScreen { screen = Screen.Menu }
            Screen.Menu -> MainMenu(
                onPvP = { screen = Screen.PvPSetup },
                onBot = { screen = Screen.BotSetup },
                onTutorial = { screen = Screen.Tutorial },
                darkTheme = !lightTheme,
                onToggleTheme = {
                    lightTheme = !lightTheme
                    onToggleTheme()
                }
            )
            Screen.PvPSetup -> PvPSetupScreen(
                player1Name = player1Name,
                player2Name = player2Name,
                player1Symbol = player1Symbol,
                player2Symbol = player2Symbol,
                emojiOptions = emojiOptions,
                onNameChange = { i, v -> if (i == 1) player1Name = v else player2Name = v },
                onSymbolChange = { i, v -> if (i == 1) player1Symbol = v else player2Symbol = v },
                onStart = {
                    resetBoard()
                    screen = Screen.PvP
                },
                onBack = { screen = Screen.Menu }
            )
            Screen.PvP -> GameScreen(
                board = board,
                player1 = Player(
                    name = if (player1Name.isBlank()) "Player 1" else player1Name,
                    symbol = player1Symbol,
                    color = accent
                ),
                player2 = Player(
                    name = if (player2Name.isBlank()) "Player 2" else player2Name,
                    symbol = player2Symbol,
                    color = Color.Magenta
                ),
                currentPlayer = currentPlayer,
                onCellClick = { r, c ->
                    if (!showResult && board[r][c] == "") {
                        makeMove(r, c, if (currentPlayer == 1) player1Symbol else player2Symbol)
                        afterMove()
                    }
                },
                winner = winner,
                draw = draw,
                showResult = showResult,
                onRestart = {
                    resetBoard()
                },
                onMenu = {
                    resetBoard()
                    screen = Screen.Menu
                },
                onBack = {
                    resetBoard()
                    screen = Screen.PvPSetup
                },
                themeToggle = {
                    lightTheme = !lightTheme
                    onToggleTheme()
                },
                darkTheme = !lightTheme
            )
            Screen.BotSetup -> BotSetupScreen(
                playerName = player1Name,
                playerSymbol = player1Symbol,
                emojiOptions = emojiOptions,
                difficulty = botDifficulty,
                onNameChange = { player1Name = it },
                onSymbolChange = { player1Symbol = it },
                onDifficultyChange = { botDifficulty = it },
                onStart = {
                    player2Symbol = emojiOptions.first { it != player1Symbol }
                    resetBoard()
                    screen = Screen.Bot
                },
                onBack = { screen = Screen.Menu }
            )
            Screen.Bot -> GameScreen(
                board = board,
                player1 = Player(
                    name = if (player1Name.isBlank()) "You" else player1Name,
                    symbol = player1Symbol,
                    color = accent
                ),
                player2 = Player(
                    name = "Bot",
                    symbol = player2Symbol,
                    color = Color.Magenta,
                    type = PlayerType.Bot
                ),
                currentPlayer = currentPlayer,
                onCellClick = { r, c ->
                    if (!showResult && currentPlayer == 1 && board[r][c] == "") {
                        makeMove(r, c, player1Symbol)
                        afterMove()
                    }
                },
                winner = winner,
                draw = draw,
                showResult = showResult,
                onRestart = {
                    resetBoard()
                },
                onMenu = {
                    resetBoard()
                    screen = Screen.Menu
                },
                onBack = {
                    resetBoard()
                    screen = Screen.BotSetup
                },
                themeToggle = {
                    lightTheme = !lightTheme
                    onToggleTheme()
                },
                darkTheme = !lightTheme,
                isBot = true,
                isBotThinking = isBotThinking
            )
            Screen.Tutorial -> TutorialScreen(onBack = { screen = Screen.Menu })
            else -> {}
        }
    }

    // Bot move effect
    LaunchedEffect(screen, currentPlayer) {
        if (screen == Screen.Bot && currentPlayer == 2 && !showResult && !isBotThinking) {
            botMove(player2Symbol, botDifficulty)
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onFinish()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "Tic Tac Toe",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "by Md. Asif Khan",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(0.5f)
        )
    }
}

@Composable
fun MainMenu(
    onPvP: () -> Unit,
    onBot: () -> Unit,
    onTutorial: () -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Tic Tac Toe",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onPvP, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Play vs Player", fontSize = 20.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBot, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Play vs Bot", fontSize = 20.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onTutorial, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Tutorial", fontSize = 20.sp)
            }
            Spacer(Modifier.height(32.dp))
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (darkTheme) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
                    contentDescription = "Toggle theme"
                )
            }
        }
    }
}

@Composable
fun PvPSetupScreen(
    player1Name: String,
    player2Name: String,
    player1Symbol: String,
    player2Symbol: String,
    emojiOptions: List<String>,
    onNameChange: (Int, String) -> Unit,
    onSymbolChange: (Int, String) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Player vs Player Setup", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = player1Name,
                onValueChange = { onNameChange(1, it) },
                label = { Text("Player 1 Name") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            EmojiSelector(
                selected = player1Symbol,
                options = emojiOptions.filter { it != player2Symbol },
                onSelect = { onSymbolChange(1, it) },
                label = "Player 1 Symbol"
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = player2Name,
                onValueChange = { onNameChange(2, it) },
                label = { Text("Player 2 Name") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            EmojiSelector(
                selected = player2Symbol,
                options = emojiOptions.filter { it != player1Symbol },
                onSelect = { onSymbolChange(2, it) },
                label = "Player 2 Symbol"
            )
            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                    Text("Back")
                }
                Spacer(Modifier.width(16.dp))
                Button(onClick = onStart) {
                    Text("Start Game")
                }
            }
        }
    }
}

@Composable
fun BotSetupScreen(
    playerName: String,
    playerSymbol: String,
    emojiOptions: List<String>,
    difficulty: Difficulty,
    onNameChange: (String) -> Unit,
    onSymbolChange: (String) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Play vs Bot Setup", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = playerName,
                onValueChange = onNameChange,
                label = { Text("Your Name") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            EmojiSelector(
                selected = playerSymbol,
                options = emojiOptions,
                onSelect = onSymbolChange,
                label = "Your Symbol"
            )
            Spacer(Modifier.height(16.dp))
            Text("Bot Difficulty", fontWeight = FontWeight.SemiBold)
            Row {
                Difficulty.entries.forEach {
                    val selected = it == difficulty
                    Button(
                        onClick = { onDifficultyChange(it) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray
                        ),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(it.name)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                    Text("Back")
                }
                Spacer(Modifier.width(16.dp))
                Button(onClick = onStart) {
                    Text("Start Game")
                }
            }
        }
    }
}

@Composable
fun EmojiSelector(selected: String, options: List<String>, onSelect: (String) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            options.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (selected == emoji) MaterialTheme.colorScheme.primary else Color.LightGray)
                        .clickable { onSelect(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    board: Array<Array<String>>,
    player1: Player,
    player2: Player,
    currentPlayer: Int,
    onCellClick: (Int, Int) -> Unit,
    winner: String?,
    draw: Boolean,
    showResult: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
    themeToggle: () -> Unit,
    darkTheme: Boolean,
    isBot: Boolean = false,
    isBotThinking: Boolean = false
) {
    val player = if (currentPlayer == 1) player1 else player2
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = themeToggle) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
                        contentDescription = "Toggle theme"
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isBot) "You vs Bot" else "${player1.name} vs ${player2.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (showResult) "" else if (isBot && currentPlayer == 2) "Bot is thinking..." else "${player.name}'s turn (${player.symbol})",
                fontSize = 18.sp,
                color = player.color
            )
            Spacer(Modifier.height(16.dp))
            Board(
                board = board,
                onCellClick = onCellClick,
                player1 = player1,
                player2 = player2,
                currentPlayer = currentPlayer,
                showResult = showResult
            )
            Spacer(Modifier.height(16.dp))
            AnimatedVisibility(showResult) {
                ResultPopup(
                    winner = winner,
                    draw = draw,
                    player1 = player1,
                    player2 = player2,
                    onRestart = onRestart,
                    onMenu = onMenu
                )
            }
        }
    }
}

@Composable
fun Board(
    board: Array<Array<String>>,
    onCellClick: (Int, Int) -> Unit,
    player1: Player,
    player2: Player,
    currentPlayer: Int,
    showResult: Boolean
) {
    val cellColors = listOf(Color.Black, Color.White)
    Column(
        modifier = Modifier
            .border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        for (r in 0..2) {
            Row {
                for (c in 0..2) {
                    val color = cellColors[(r + c) % 2]
                    val symbol = board[r][c]
                    val symbolColor = if (color == Color.Black) Color.White else Color.Black
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(color, RoundedCornerShape(8.dp))
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp))
                            .clickable(enabled = !showResult && symbol == "") { onCellClick(r, c) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (symbol.isNotEmpty()) {
                            Text(
                                text = symbol,
                                fontSize = 36.sp,
                                color = symbolColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (c < 2) Spacer(Modifier.width(8.dp))
                }
            }
            if (r < 2) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ResultPopup(
    winner: String?,
    draw: Boolean,
    player1: Player,
    player2: Player,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    val text = when {
        draw -> "It's a Draw!"
        winner == player1.symbol -> "${player1.name} Wins!"
        winner == player2.symbol -> "${player2.name} Wins!"
        else -> ""
    }
    Card(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = onRestart) { Text("Restart") }
                Spacer(Modifier.width(16.dp))
                Button(onClick = onMenu, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) { Text("Menu") }
            }
        }
    }
}

@Composable
fun TutorialScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("How to Play", fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Tic Tac Toe is a simple game for two players.\n\n" +
                "Players take turns marking a 3x3 grid. The first to get three in a row (horizontally, vertically, or diagonally) wins.\n\n" +
                "You can play against a friend or the bot. Choose your emoji, difficulty, and have fun!\n\n" +
                "Note: The bot in Hard mode can't be defeated!",
                fontSize = 18.sp
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onBack) { Text("Back to Menu") }
        }
    }
}

