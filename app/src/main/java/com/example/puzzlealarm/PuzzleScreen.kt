package com.example.puzzlealarm

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/* ===============================
   CONFIG
   =============================== */

private const val GRID_SIZE = 5   // 5x5
private const val EMPTY = 0

/* ===============================
   PUZZLE SCREEN
   =============================== */

@Composable
fun PuzzleScreen(
    onPuzzleSolved: () -> Unit
) {
    var tiles by remember { mutableStateOf(generateSolvablePuzzle()) }

    // Stop alarm when solved
    LaunchedEffect(tiles) {
        if (isSolved(tiles)) {
            onPuzzleSolved()
        }
    }

    // Pulsing title
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200),
            RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0B0B), Color(0xFF1F1F1F))
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Solve to Stop Alarm",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = pulseAlpha),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ✅ Correct grid container
        Box(
            modifier = Modifier.size(360.dp),
            contentAlignment = Alignment.Center
        ) {
            Column {
                for (row in 0 until GRID_SIZE) {
                    Row {
                        for (col in 0 until GRID_SIZE) {
                            val index = row * GRID_SIZE + col
                            PuzzleTile(
                                value = tiles[index],
                                onClick = {
                                    tiles = moveTile(tiles, index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ===============================
   TILE
   =============================== */

@Composable
private fun PuzzleTile(
    value: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .padding(4.dp)
            .shadow(
                elevation = if (value == EMPTY) 0.dp else 6.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (value == EMPTY) Color.Transparent else Color(0xFF1E88E5),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = value != EMPTY) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (value != EMPTY) {
            Text(
                text = value.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/* ===============================
   LOGIC
   =============================== */

private fun moveTile(tiles: List<Int>, index: Int): List<Int> {
    val emptyIndex = tiles.indexOf(EMPTY)

    val row = index / GRID_SIZE
    val col = index % GRID_SIZE
    val emptyRow = emptyIndex / GRID_SIZE
    val emptyCol = emptyIndex % GRID_SIZE

    val isAdjacent =
        (row == emptyRow && abs(col - emptyCol) == 1) ||
                (col == emptyCol && abs(row - emptyRow) == 1)

    if (!isAdjacent) return tiles

    val newTiles = tiles.toMutableList()
    newTiles[emptyIndex] = newTiles[index]
    newTiles[index] = EMPTY

    return newTiles
}


private fun isSolved(tiles: List<Int>): Boolean {
    for (i in 0 until GRID_SIZE * GRID_SIZE - 1) {
        if (tiles[i] != i + 1) return false
    }
    return tiles.last() == EMPTY
}

/* ===============================
   SHUFFLE (SOLVABLE)
   =============================== */

private fun generateSolvablePuzzle(): List<Int> {
    val tiles = (0 until GRID_SIZE * GRID_SIZE).toMutableList()
    do {
        tiles.shuffle()
    } while (!isSolvable(tiles) || isSolved(tiles))
    return tiles
}

private fun isSolvable(tiles: List<Int>): Boolean {
    var inversions = 0
    for (i in tiles.indices) {
        for (j in i + 1 until tiles.size) {
            if (tiles[i] != EMPTY && tiles[j] != EMPTY && tiles[i] > tiles[j]) {
                inversions++
            }
        }
    }

    val emptyRowFromBottom =
        GRID_SIZE - (tiles.indexOf(EMPTY) / GRID_SIZE)

    return if (GRID_SIZE % 2 == 1) {
        inversions % 2 == 0
    } else {
        if (emptyRowFromBottom % 2 == 0) inversions % 2 == 1
        else inversions % 2 == 0
    }
}
