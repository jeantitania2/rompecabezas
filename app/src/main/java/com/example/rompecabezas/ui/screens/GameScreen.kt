package com.example.rompecabezas.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rompecabezas.data.model.PuzzleItem
import com.example.rompecabezas.data.model.PuzzleLevel
import com.example.rompecabezas.data.model.PuzzlePiece
import com.example.rompecabezas.data.repository.PuzzleRepository
import com.example.rompecabezas.ui.components.PieceTray
import com.example.rompecabezas.ui.components.PuzzleBoard
import com.example.rompecabezas.ui.components.VictoryDialog
import com.example.rompecabezas.ui.components.ZoomableBox
import com.example.rompecabezas.ui.components.rememberZoomState
import com.example.rompecabezas.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    puzzleId: String,
    initialLevelNumber: Int,
    repository: PuzzleRepository,
    onNavigateBack: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var puzzleItem by remember { mutableStateOf<PuzzleItem?>(null) }
    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentLevelNumber by remember { mutableIntStateOf(initialLevelNumber) }
    var maxUnlockedLevel by remember { mutableIntStateOf(1) }

    val placedPieces = remember { mutableStateListOf<PuzzlePiece>() }
    val unplacedPieces = remember { mutableStateListOf<PuzzlePiece>() }
    var selectedPiece by remember { mutableStateOf<PuzzlePiece?>(null) }
    var selectedBoardSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var flashWrongSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var flashSuccessSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var showPreviewThumbnail by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var isLoadingLevel by remember { mutableStateOf(true) }

    val zoomState = rememberZoomState()

    // Handle system back button
    BackHandler {
        onNavigateBack()
    }

    // Load Puzzle Item & Bitmap
    LaunchedEffect(puzzleId) {
        withContext(Dispatchers.IO) {
            val item = repository.getPuzzleById(puzzleId)
            puzzleItem = item
            if (item != null) {
                maxUnlockedLevel = item.maxUnlockedLevel
                val bmp = ImageUtils.loadBitmapFromFile(item.filePath)
                fullBitmap = bmp
            }
        }
    }

    // Function to initialize/slice a specific level
    fun startLevel(levelNum: Int) {
        val bmp = fullBitmap ?: return
        isLoadingLevel = true
        isVictory = false
        selectedPiece = null
        selectedBoardSlot = null
        flashWrongSlot = null
        flashSuccessSlot = null
        currentLevelNumber = levelNum

        coroutineScope.launch {
            val level = PuzzleLevel.getLevel(levelNum, bmp.width, bmp.height)
            val pieces = withContext(Dispatchers.Default) {
                ImageUtils.sliceBitmap(bmp, level.rows, level.cols)
            }
            placedPieces.clear()
            unplacedPieces.clear()
            unplacedPieces.addAll(pieces.shuffled())
            isLoadingLevel = false
            zoomState.reset()
        }
    }

    // Whenever fullBitmap is ready or level changes, initialize level
    LaunchedEffect(fullBitmap, currentLevelNumber) {
        if (fullBitmap != null) {
            startLevel(currentLevelNumber)
        }
    }

    // Check victory condition
    fun checkVictory() {
        if (unplacedPieces.isEmpty() && placedPieces.isNotEmpty() && !isVictory) {
            isVictory = true
            repository.recordLevelCompleted(puzzleId, currentLevelNumber)
            // Update max unlocked level
            val nextLevel = (currentLevelNumber + 1).coerceAtMost(PuzzleLevel.MAX_LEVEL)
            if (nextLevel > maxUnlockedLevel) {
                maxUnlockedLevel = nextLevel
            }
        }
    }

    // Handle slot click on board
    fun onSlotClicked(row: Int, col: Int) {
        val alreadyPlaced = placedPieces.any { it.row == row && it.col == col }
        if (alreadyPlaced) {
            Toast.makeText(context, "Esta casilla ya está armada", Toast.LENGTH_SHORT).show()
            return
        }

        val currentSelected = selectedPiece
        if (currentSelected == null) {
            // User taps a board slot to target it
            selectedBoardSlot = if (selectedBoardSlot == Pair(row, col)) null else Pair(row, col)
            if (selectedBoardSlot != null) {
                Toast.makeText(context, "Casilla seleccionada. Elige la pieza que crees que va aquí.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // User has a selected piece and placed it in (row, col) where they think it goes
        if (currentSelected.row == row && currentSelected.col == col) {
            // Correct placement!
            placedPieces.add(currentSelected.copy(isPlaced = true))
            unplacedPieces.removeAll { it.id == currentSelected.id }
            selectedPiece = null
            selectedBoardSlot = null
            flashSuccessSlot = Pair(row, col)
            coroutineScope.launch {
                delay(450)
                flashSuccessSlot = null
            }
            checkVictory()
        } else {
            // Incorrect position feedback
            flashWrongSlot = Pair(row, col)
            coroutineScope.launch {
                delay(450)
                flashWrongSlot = null
            }
            Toast.makeText(context, "No encaja en esta casilla. ¡Sigue probando!", Toast.LENGTH_SHORT).show()
        }
    }

    val currentLevel = fullBitmap?.let {
        PuzzleLevel.getLevel(currentLevelNumber, it.width, it.height)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nivel $currentLevelNumber: ${currentLevel?.pieceCount ?: (currentLevelNumber * 10)} Piezas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Colocadas: ${placedPieces.size} de ${currentLevel?.pieceCount ?: (currentLevelNumber * 10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Toggle thumbnail preview of full image (like looking at the puzzle box)
                    IconButton(onClick = { showPreviewThumbnail = !showPreviewThumbnail }) {
                        Badge(
                            containerColor = if (showPreviewThumbnail) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(text = "Ojo", fontSize = 11.sp, modifier = Modifier.padding(2.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoadingLevel || fullBitmap == null || currentLevel == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Dividiendo imagen en ${currentLevelNumber * 10} piezas...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Level selector bar (shows unlocked levels)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nivel:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        for (lvl in 1..maxUnlockedLevel.coerceAtMost(10)) {
                            val isSelected = lvl == currentLevelNumber
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (lvl != currentLevelNumber) {
                                        startLevel(lvl)
                                    }
                                },
                                label = {
                                    Text(
                                        text = "${lvl * 10}p",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    // Main Interactive Zoomable Puzzle Board Area
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF1E1E24)),
                        contentAlignment = Alignment.Center
                    ) {
                        val availableWidth = constraints.maxWidth.toFloat()
                        val availableHeight = constraints.maxHeight.toFloat()

                        val imageAspect = fullBitmap!!.width.toFloat() / fullBitmap!!.height.toFloat()
                        val containerAspect = availableWidth / availableHeight

                        val boardWidthPx: Float
                        val boardHeightPx: Float

                        if (containerAspect > imageAspect) {
                            boardHeightPx = availableHeight * 0.90f
                            boardWidthPx = boardHeightPx * imageAspect
                        } else {
                            boardWidthPx = availableWidth * 0.92f
                            boardHeightPx = boardWidthPx / imageAspect
                        }

                        val boardWidthDp = with(density) { boardWidthPx.toDp() }
                        val boardHeightDp = with(density) { boardHeightPx.toDp() }

                        ZoomableBox(
                            modifier = Modifier.fillMaxSize(),
                            zoomState = zoomState
                        ) {
                            PuzzleBoard(
                                level = currentLevel,
                                placedPieces = placedPieces,
                                selectedSlot = selectedBoardSlot,
                                flashWrongSlot = flashWrongSlot,
                                flashSuccessSlot = flashSuccessSlot,
                                boardWidthPx = boardWidthPx,
                                boardHeightPx = boardHeightPx,
                                onSlotClicked = { r, c ->
                                    onSlotClicked(r, c)
                                },
                                modifier = Modifier.size(boardWidthDp, boardHeightDp)
                            )
                        }

                        // Floating Zoom Controls (Responsive & Easy to Use)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = { zoomState.zoomIn() },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Acercar Zoom",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = "${(zoomState.scale * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                FilledTonalIconButton(
                                    onClick = { zoomState.zoomOut() },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Text(text = "—", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                FilledTonalIconButton(
                                    onClick = { zoomState.reset() },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Restablecer Zoom",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Floating Reference Thumbnail
                        if (showPreviewThumbnail) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .size(110.dp)
                                    .shadow(8.dp, RoundedCornerShape(12.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = fullBitmap!!.asImageBitmap(),
                                        contentDescription = "Referencia",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { showPreviewThumbnail = false },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cerrar",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Piece Tray
                    PieceTray(
                        pieces = unplacedPieces,
                        selectedPiece = selectedPiece,
                        totalPieces = currentLevel.pieceCount,
                        onPieceSelected = { piece ->
                            if (selectedPiece?.id == piece.id) {
                                selectedPiece = null
                            } else {
                                selectedPiece = piece
                                // If the user had already tapped a slot on the board, check match with that slot
                                val targetSlot = selectedBoardSlot
                                if (targetSlot != null) {
                                    if (piece.row == targetSlot.first && piece.col == targetSlot.second) {
                                        placedPieces.add(piece.copy(isPlaced = true))
                                        unplacedPieces.removeAll { it.id == piece.id }
                                        selectedPiece = null
                                        selectedBoardSlot = null
                                        flashSuccessSlot = targetSlot
                                        coroutineScope.launch {
                                            delay(450)
                                            flashSuccessSlot = null
                                        }
                                        checkVictory()
                                    } else {
                                        flashWrongSlot = targetSlot
                                        coroutineScope.launch {
                                            delay(450)
                                            flashWrongSlot = null
                                        }
                                        Toast.makeText(context, "No encaja en la casilla seleccionada.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onShuffle = {
                            val shuffled = unplacedPieces.shuffled()
                            unplacedPieces.clear()
                            unplacedPieces.addAll(shuffled)
                        }
                    )
                }
            }

            // Victory Celebratory Dialog
            if (isVictory && currentLevel != null) {
                val hasNext = currentLevelNumber < PuzzleLevel.MAX_LEVEL
                val nextPieces = (currentLevelNumber + 1) * 10

                VictoryDialog(
                    completedLevel = currentLevelNumber,
                    pieceCount = currentLevel.pieceCount,
                    hasNextLevel = hasNext,
                    nextPieceCount = nextPieces,
                    onNextLevel = {
                        startLevel(currentLevelNumber + 1)
                    },
                    onReplayLevel = {
                        startLevel(currentLevelNumber)
                    },
                    onGoToGallery = onNavigateToGallery,
                    onGoHome = onNavigateToHome,
                    onDismiss = { isVictory = false }
                )
            }
        }
    }
}
