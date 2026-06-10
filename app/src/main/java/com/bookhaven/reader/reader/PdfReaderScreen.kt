package com.bookhaven.reader.reader

import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfReaderScreen(
    filePath: String,
    initialLocator: String,
    onBack: () -> Unit,
    onProgress: (Float, String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val renderer = remember(filePath) {
        runCatching {
            val pfd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(pfd) to pfd
        }.getOrNull()
    }
    DisposableEffect(renderer) {
        onDispose { renderer?.let { runCatching { it.first.close(); it.second.close() } } }
    }

    if (renderer == null || renderer.first.pageCount == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("This PDF could not be opened.", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onBack) { Text("Back to Library") }
            }
        }
        return
    }

    val pdf = renderer.first
    val pageCount = pdf.pageCount
    val renderLock = remember { Any() }
    val startPage = remember { initialLocator.toIntOrNull()?.coerceIn(0, pageCount - 1) ?: 0 }
    val pagerState = rememberPagerState(initialPage = startPage) { pageCount }
    var chromeVisible by remember { mutableStateOf(false) }

    val targetWidthPx = remember { with(density) { 1000.dp.toPx().toInt() }.coerceAtMost(2000) }

    LaunchedEffect(pagerState.currentPage) {
        onProgress((pagerState.currentPage + 1f) / pageCount, pagerState.currentPage.toString())
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF2B2B2E))) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val bitmap by produceState<Bitmap?>(initialValue = null, page, targetWidthPx) {
                value = withContext(Dispatchers.IO) {
                    synchronized(renderLock) {
                        runCatching {
                            pdf.openPage(page).use { p ->
                                val scale = targetWidthPx.toFloat() / p.width
                                val w = targetWidthPx
                                val h = (p.height * scale).toInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(AColor.WHITE)
                                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bmp
                            }
                        }.getOrNull()
                    }
                }
            }

            var scale by remember(page) { mutableFloatStateOf(1f) }
            var offsetX by remember(page) { mutableFloatStateOf(0f) }
            var offsetY by remember(page) { mutableFloatStateOf(0f) }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(page) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) { offsetX += pan.x; offsetY += pan.y }
                            else { offsetX = 0f; offsetY = 0f }
                        }
                    }
                    .pointerInput(page) {
                        detectTapGestures(onTap = { chromeVisible = !chromeVisible })
                    },
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page ${page + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale, scaleY = scale,
                                translationX = offsetX, translationY = offsetY
                            )
                    )
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        if (chromeVisible) {
            ReaderTopBar(
                title = File(filePath).nameWithoutExtension,
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Text(
                    "Page ${pagerState.currentPage + 1} of $pageCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth(),
                )
            }
        }
    }
}
