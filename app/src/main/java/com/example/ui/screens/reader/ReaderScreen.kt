package com.example.ui.screens.reader

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ReaderBackground
import com.example.data.model.ReaderMode
import com.example.ui.theme.SepiaBackground
import com.example.ui.theme.SepiaText
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showSettingsBottomSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (val state = uiState) {
                    is ReaderUiState.Success -> when (state.readerBg) {
                        ReaderBackground.BLACK -> Color.Black
                        ReaderBackground.WHITE -> Color.White
                        ReaderBackground.SEPIA -> SepiaBackground
                    }
                    else -> Color.Black
                }
            )
    ) {
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ReaderUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.loadChapterPages() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is ReaderUiState.Success -> {
                val coroutineScope = rememberCoroutineScope()

                // Reader Body according to selected Mode
                if (state.readerMode == ReaderMode.VERTICAL_CONTINUOUS) {
                    val listState = rememberLazyListState()

                    // Update page progress as user scrolls
                    LaunchedEffect(listState.firstVisibleItemIndex) {
                        viewModel.onPageChanged(listState.firstVisibleItemIndex)
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showControls = !showControls
                            },
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(state.pages) { index, page ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = page.imageUrl,
                                    contentDescription = "Page ${page.pageNumber}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Page badge overlay
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "${page.pageNumber}/${state.pages.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // HORIZONTAL PAGED
                    val pagerState = rememberPagerState(
                        initialPage = state.currentPageIndex,
                        pageCount = { state.pages.size }
                    )

                    LaunchedEffect(pagerState.currentPage) {
                        viewModel.onPageChanged(pagerState.currentPage)
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showControls = !showControls
                            }
                    ) { pageIndex ->
                        val page = state.pages.getOrNull(pageIndex)
                        if (page != null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = page.imageUrl,
                                    contentDescription = "Page ${page.pageNumber}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Top Toolbar Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.testTag("reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = state.manga.displayTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = state.currentChapter.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = { showSettingsBottomSheet = true },
                                modifier = Modifier.testTag("reader_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Reader Settings",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Bottom Control Bar Overlay with Page Slider & Prev/Next Chapter
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Page Slider
                            if (state.pages.size > 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "${state.currentPageIndex + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Slider(
                                        value = state.currentPageIndex.toFloat(),
                                        onValueChange = {
                                            viewModel.onPageChanged(it.toInt())
                                        },
                                        valueRange = 0f..(state.pages.size - 1).toFloat(),
                                        steps = (state.pages.size - 2).coerceAtLeast(0),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("page_slider"),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        )
                                    )

                                    Text(
                                        text = "${state.pages.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Prev / Next Chapter Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.navigateChapter(next = false) },
                                    modifier = Modifier.testTag("prev_chapter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NavigateBefore,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Prev Ch", color = Color.White)
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Page ${state.currentPageIndex + 1} of ${state.pages.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.navigateChapter(next = true) },
                                    modifier = Modifier.testTag("next_chapter_button")
                                ) {
                                    Text("Next Ch", color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.NavigateNext,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Reader Settings Bottom Sheet Modal
                if (showSettingsBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showSettingsBottomSheet = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "Reader Preferences",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Mode Selection
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Reading Direction",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FilterChip(
                                        selected = state.readerMode == ReaderMode.VERTICAL_CONTINUOUS,
                                        onClick = { viewModel.setReaderMode(ReaderMode.VERTICAL_CONTINUOUS) },
                                        label = { Text("Webtoon (Vertical)") },
                                        leadingIcon = { Icon(imageVector = Icons.Default.SwapVert, contentDescription = null) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("mode_vertical_chip")
                                    )

                                    FilterChip(
                                        selected = state.readerMode == ReaderMode.HORIZONTAL_PAGED,
                                        onClick = { viewModel.setReaderMode(ReaderMode.HORIZONTAL_PAGED) },
                                        label = { Text("Paged (Horizontal)") },
                                        leadingIcon = { Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("mode_horizontal_chip")
                                    )
                                }
                            }

                            // Background Color Selection
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Background Color",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FilterChip(
                                        selected = state.readerBg == ReaderBackground.BLACK,
                                        onClick = { viewModel.setReaderBackground(ReaderBackground.BLACK) },
                                        label = { Text("Dark Black") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("bg_black_chip")
                                    )

                                    FilterChip(
                                        selected = state.readerBg == ReaderBackground.SEPIA,
                                        onClick = { viewModel.setReaderBackground(ReaderBackground.SEPIA) },
                                        label = { Text("Sepia Warm") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("bg_sepia_chip")
                                    )

                                    FilterChip(
                                        selected = state.readerBg == ReaderBackground.WHITE,
                                        onClick = { viewModel.setReaderBackground(ReaderBackground.WHITE) },
                                        label = { Text("White Light") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("bg_white_chip")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
