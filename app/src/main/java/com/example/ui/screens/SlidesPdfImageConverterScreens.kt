package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OfficeFile
import com.example.data.PresentationEngine
import com.example.data.SlideItem
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.util.DocumentExportEngine
import com.example.viewmodel.OfficeViewModel
import java.io.File
import java.io.FileOutputStream

// ============================================================================
// 9. AMAZING SLIDES: PRESENTATION APPLICATION
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmazingPresentationEditorScreen(
    viewModel: OfficeViewModel,
    activeFile: OfficeFile?
) {
    val context = LocalContext.current
    val initialFile = activeFile ?: OfficeFile(
        title = "Executive Investor Pitch Deck",
        fileType = "PRESENTATION",
        content = PresentationEngine.createDefaultPresentationJson("Executive Investor Pitch Deck")
    )

    var deckTitle by rememberSaveable(initialFile.id) { mutableStateOf(initialFile.title) }
    var deck by remember(initialFile.id) {
        mutableStateOf(PresentationEngine.parseDeck(initialFile.content))
    }
    var selectedSlideIdx by rememberSaveable(initialFile.id) { mutableIntStateOf(0) }
    var isFullScreenPresenting by rememberSaveable { mutableStateOf(false) }
    var aiTopicPrompt by rememberSaveable { mutableStateOf("") }

    val safeIndex = selectedSlideIdx.coerceIn(0, (deck.slides.size - 1).coerceAtLeast(0))
    val currentSlide = deck.slides.getOrElse(safeIndex) {
        SlideItem(
            id = "s1",
            title = deckTitle,
            subtitle = "Amazing Office AI Suite",
            bullets = listOf("Key Point 1", "Key Point 2"),
            speakerNotes = ""
        )
    }

    fun updateCurrentSlide(transform: (SlideItem) -> SlideItem) {
        val mutable = deck.slides.toMutableList()
        if (safeIndex in mutable.indices) {
            mutable[safeIndex] = transform(mutable[safeIndex])
            deck = deck.copy(slides = mutable)
        }
    }

    fun buildCurrentOfficeFile(): OfficeFile = initialFile.copy(
        title = deckTitle,
        fileType = "PRESENTATION",
        content = PresentationEngine.serializeDeck(deck)
    )

    // Full-Screen Present Mode
    if (isFullScreenPresenting) {
        BackHandler { isFullScreenPresenting = false }
        val (bgColors, accentColor) = slideThemePalette(deck.themeId)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(bgColors))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$deckTitle • Slide ${safeIndex + 1} of ${deck.slides.size}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Button(
                        onClick = { isFullScreenPresenting = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit")
                        Spacer(Modifier.width(4.dp))
                        Text("Exit Slideshow")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = currentSlide.title,
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (currentSlide.subtitle.isNotBlank()) {
                        Text(
                            text = currentSlide.subtitle,
                            color = accentColor,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    currentSlide.bullets.forEach { bullet ->
                        Text(
                            text = "• $bullet",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { selectedSlideIdx = (safeIndex - 1).coerceAtLeast(0) },
                        enabled = safeIndex > 0,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous")
                        Text("Previous")
                    }
                    Text(
                        text = currentSlide.speakerNotes.ifBlank { "Amazing Slides Live Presenter" },
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { selectedSlideIdx = (safeIndex + 1).coerceAtMost(deck.slides.lastIndex) },
                        enabled = safeIndex < deck.slides.lastIndex,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Text("Next")
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next")
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- TOP BAR ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = deckTitle,
                    onValueChange = { deckTitle = it },
                    label = { Text("Presentation Title") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slides_title_input")
                )
                Button(
                    onClick = {
                        viewModel.saveActiveFile(
                            title = deckTitle,
                            content = PresentationEngine.serializeDeck(deck),
                            pageSettings = initialFile.pageSettings
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier.testTag("slides_save_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }
                Button(
                    onClick = { isFullScreenPresenting = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    modifier = Modifier.testTag("slides_present_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Present", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Present")
                }
            }
        }

        // --- SLIDE THUMBNAIL STRIP & CONTROLS ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                deck.slides.forEachIndexed { idx, slide ->
                    val isSelected = idx == safeIndex
                    Card(
                        modifier = Modifier
                            .width(135.dp)
                            .height(80.dp)
                            .clickable { selectedSlideIdx = idx }
                            .then(
                                if (isSelected) Modifier.border(2.dp, GoldAccent, RoundedCornerShape(10.dp))
                                else Modifier
                            ),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Slide ${idx + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) GoldLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                AssistChip(
                    onClick = {
                        val newSlide = SlideItem(
                            id = "s_${System.currentTimeMillis()}",
                            title = "New Slide ${deck.slides.size + 1}",
                            subtitle = "Key Subtitle",
                            bullets = listOf("Point 1", "Point 2", "Point 3"),
                            speakerNotes = ""
                        )
                        deck = deck.copy(slides = deck.slides + newSlide)
                        selectedSlideIdx = deck.slides.lastIndex
                    },
                    label = { Text("+ Add Slide") }
                )

                AssistChip(
                    onClick = {
                        val copySlide = currentSlide.copy(
                            id = "s_${System.currentTimeMillis()}",
                            title = "${currentSlide.title} (Copy)"
                        )
                        deck = deck.copy(slides = deck.slides + copySlide)
                        selectedSlideIdx = deck.slides.lastIndex
                    },
                    label = { Text("Duplicate") }
                )

                if (deck.slides.size > 1) {
                    AssistChip(
                        onClick = {
                            val filtered = deck.slides.filterIndexed { i, _ -> i != safeIndex }
                            deck = deck.copy(slides = filtered)
                            selectedSlideIdx = (safeIndex - 1).coerceAtLeast(0)
                        },
                        label = { Text("Delete Slide") }
                    )
                }
            }
        }

        // --- LIVE SLIDE PREVIEW CANVAS ---
        item {
            val (bgColors, accentColor) = slideThemePalette(deck.themeId)
            Card(
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(bgColors))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = currentSlide.title,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2
                            )
                            if (currentSlide.subtitle.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = currentSlide.subtitle,
                                    color = accentColor,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            currentSlide.bullets.take(4).forEach { b ->
                                Text(
                                    text = "• $b",
                                    color = Color.White.copy(alpha = 0.95f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AMAZING OFFICE SLIDES",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${safeIndex + 1} / ${deck.slides.size}",
                                color = accentColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- THEME SELECTOR & EXPORT BAR ---
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "NAVY_GOLD" to "Navy & Gold",
                    "ROYAL_MODERN" to "Royal Blue",
                    "EMERALD_GROWTH" to "Emerald Pitch",
                    "ROYAL_SANCTUARY" to "Sanctuary Purple",
                    "MINIMAL_SLATE" to "Executive Slate"
                ).forEach { (id, label) ->
                    FilterChip(
                        selected = deck.themeId == id,
                        onClick = { deck = deck.copy(themeId = id) },
                        label = { Text(label) }
                    )
                }

                OutlinedButton(
                    onClick = {
                        val pdf = DocumentExportEngine.exportToPdfFile(context, buildCurrentOfficeFile())
                        DocumentExportEngine.shareExportedFile(context, pdf, "application/pdf")
                        viewModel.showMessage("Exported Slide Deck PDF: ${pdf.name}")
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export PDF / Deck")
                }

                OutlinedButton(
                    onClick = {
                        DocumentExportEngine.printOfficeDocument(context, buildCurrentOfficeFile())
                    }
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Print Slides")
                }
            }
        }

        // --- SLIDE CONTENT & SPEAKER NOTES EDITOR ---
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Edit Slide ${safeIndex + 1} Content",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = currentSlide.title,
                        onValueChange = { newTitle -> updateCurrentSlide { it.copy(title = newTitle) } },
                        label = { Text("Slide Headline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentSlide.subtitle,
                        onValueChange = { newSub -> updateCurrentSlide { it.copy(subtitle = newSub) } },
                        label = { Text("Slide Subtitle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentSlide.bullets.joinToString("\n"),
                        onValueChange = { text ->
                            val lines = text.lines()
                            updateCurrentSlide { it.copy(bullets = lines) }
                        },
                        label = { Text("Bullet Points (one per line)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentSlide.speakerNotes,
                        onValueChange = { notes -> updateCurrentSlide { it.copy(speakerNotes = notes) } },
                        label = { Text("Speaker Notes") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- AI PRESENTATION GENERATOR ---
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Gemini AI Slide Deck Generator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aiTopicPrompt,
                            onValueChange = { aiTopicPrompt = it },
                            placeholder = { Text("e.g., 2026 Fintech Product Launch Strategy") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                viewModel.generateAiPresentationSlides(
                                    topic = aiTopicPrompt,
                                    slideCount = 5,
                                    onDeckGenerated = { generatedDeck ->
                                        deck = generatedDeck
                                        selectedSlideIdx = 0
                                        viewModel.showMessage("Generated 5-slide AI presentation deck!")
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            Text("Generate Deck")
                        }
                    }
                }
            }
        }
    }
}

private fun slideThemePalette(themeId: String): Pair<List<Color>, Color> = when (themeId) {
    "ROYAL_MODERN" -> listOf(Color(0xFF1E3A8A), Color(0xFF2563EB)) to Color(0xFF93C5FD)
    "EMERALD_GROWTH" -> listOf(Color(0xFF064E3B), Color(0xFF059669)) to Color(0xFF6EE7B7)
    "ROYAL_SANCTUARY" -> listOf(Color(0xFF3B0764), Color(0xFF6D28D9)) to GoldLight
    "MINIMAL_SLATE" -> listOf(Color(0xFF0F172A), Color(0xFF334155)) to Color(0xFF38BDF8)
    else -> listOf(NavyDark, NavyPrimary, Color(0xFF1E3A8A)) to GoldLight
}

// ============================================================================
// 10, 11 & 12. AMAZING PDF STUDIO & WORD ⇄ PDF CONVERTER
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmazingPdfAndConverterScreen(
    viewModel: OfficeViewModel,
    activeFile: OfficeFile?,
    startInConverterTab: Boolean = false
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable(startInConverterTab) {
        mutableIntStateOf(if (startInConverterTab) 1 else 0)
    }

    var pdfTitle by rememberSaveable(activeFile?.id) {
        mutableStateOf(activeFile?.title ?: "Corporate Agreement & Report.pdf")
    }
    var pdfTextContent by rememberSaveable(activeFile?.id) {
        mutableStateOf(
            activeFile?.content?.ifBlank { null } ?: """
                # AMAZING PDF STUDIO — VERIFIED DOCUMENT
                **Document Reference:** AD-2026-PDF-001
                
                ## 1. Document Overview
                This PDF workspace allows you to open device PDFs, search text, add highlights, insert comments, apply watermarks, rotate pages, extract pages, and convert between Word (.docx) and PDF formats.
                
                ## 2. Key Clauses & Annotations
                • [HIGHLIGHT] All deliverables are subject to quality verification.
                • [COMMENT] Approved by Executive Committee for Q4 rollout.
            """.trimIndent()
        )
    }
    var watermarkText by rememberSaveable { mutableStateOf("CONFIDENTIAL • AMAZING DIGITALS") }
    var rotationDegrees by rememberSaveable { mutableFloatStateOf(0f) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderedPageCount by rememberSaveable { mutableIntStateOf(1) }
    var currentPdfPage by rememberSaveable { mutableIntStateOf(0) }
    var currentPdfUri by remember { mutableStateOf<Uri?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            currentPdfUri = uri
            val (bmp, count) = DocumentExportEngine.renderPdfPageFromUri(context, uri, 0)
            renderedBitmap = bmp
            renderedPageCount = count.coerceAtLeast(1)
            currentPdfPage = 0
            viewModel.showMessage("Loaded PDF ($renderedPageCount pages) into PDF Studio.")
        }
    }

    val wordToPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val extracted = DocumentExportEngine.importDocxOrTextFromUri(context, uri)
            if (extracted.isNotBlank()) {
                pdfTextContent = extracted
                viewModel.showMessage("Imported Word document ready for PDF conversion & preview.")
            } else {
                viewModel.showMessage("Loaded file for conversion.")
            }
        }
    }

    fun buildPdfOfficeFile(): OfficeFile = OfficeFile(
        id = activeFile?.id.orEmpty(),
        userId = viewModel.currentUserId,
        title = pdfTitle,
        fileType = "PDF",
        content = if (watermarkText.isNotBlank()) "[WATERMARK: $watermarkText]\n\n$pdfTextContent" else pdfTextContent,
        pageSettings = "A4|PORTRAIT|NORMAL|$watermarkText|Amazing PDF Studio|true"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NavyPrimary,
            contentColor = Color.White
        ) {
            listOf("PDF Studio Editor", "Word to PDF", "PDF to Word (OCR)").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = label,
                            color = if (selectedTab == idx) GoldLight else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // --- PDF STUDIO EDITOR ---
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = pdfTitle,
                                onValueChange = { pdfTitle = it },
                                label = { Text("PDF File Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(onClick = { pdfPickerLauncher.launch("application/pdf") }) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Open PDF")
                            }
                            Button(
                                onClick = {
                                    val out = DocumentExportEngine.exportToPdfFile(context, buildPdfOfficeFile())
                                    DocumentExportEngine.shareExportedFile(context, out, "application/pdf")
                                    viewModel.showMessage("Generated & Downloaded PDF: ${out.name}")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Download PDF")
                            }
                        }
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                                label = { Text("Rotate Page (${rotationDegrees.toInt()}°)") }
                            )
                            AssistChip(
                                onClick = { pdfTextContent += "\n• [HIGHLIGHT] Key highlighted excerpt added." },
                                label = { Text("+ Highlight") }
                            )
                            AssistChip(
                                onClick = { pdfTextContent += "\n• [ANNOTATION] Reviewer comment added." },
                                label = { Text("+ Comment") }
                            )
                            AssistChip(
                                onClick = {
                                    viewModel.setAiSheetOpen(true)
                                    viewModel.runAiOfficeCommand(
                                        taskPrompt = "Summarize the key points and action items in this PDF document.",
                                        documentContext = pdfTextContent,
                                        targetContext = "PDF"
                                    )
                                },
                                label = { Text("AI Summarize PDF") }
                            )
                            AssistChip(
                                onClick = {
                                    DocumentExportEngine.printOfficeDocument(context, buildPdfOfficeFile())
                                },
                                label = { Text("Print PDF") }
                            )
                        }
                    }

                    if (renderedBitmap != null) {
                        item {
                            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Native PDF Page Viewer (Page ${currentPdfPage + 1} of $renderedPageCount)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Image(
                                        bitmap = renderedBitmap!!.asImageBitmap(),
                                        contentDescription = "Rendered PDF Page",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(340.dp)
                                            .graphicsLayer(rotationZ = rotationDegrees)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val uri = currentPdfUri ?: return@OutlinedButton
                                                val prev = (currentPdfPage - 1).coerceAtLeast(0)
                                                val (bmp, _) = DocumentExportEngine.renderPdfPageFromUri(context, uri, prev)
                                                if (bmp != null) {
                                                    renderedBitmap = bmp
                                                    currentPdfPage = prev
                                                }
                                            },
                                            enabled = currentPdfPage > 0
                                        ) {
                                            Text("Previous Page")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val uri = currentPdfUri ?: return@OutlinedButton
                                                val next = (currentPdfPage + 1).coerceAtMost(renderedPageCount - 1)
                                                val (bmp, _) = DocumentExportEngine.renderPdfPageFromUri(context, uri, next)
                                                if (bmp != null) {
                                                    renderedBitmap = bmp
                                                    currentPdfPage = next
                                                }
                                            },
                                            enabled = currentPdfPage < renderedPageCount - 1
                                        ) {
                                            Text("Next Page")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Watermark / Header Stamp") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = pdfTextContent,
                            onValueChange = { pdfTextContent = it },
                            label = { Text("PDF Text, Annotations & Extracted Content") },
                            minLines = 10,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(rotationZ = rotationDegrees)
                        )
                    }
                }

                1 -> {
                    // --- 11. WORD TO PDF CONVERTER ---
                    item {
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = RoyalBlue)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Word (.DOCX) to PDF Converter",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Upload a supported DOCX document or paste structured Word content below to generate a paginated, print-ready PDF preserving headings, tables, headers, and footers.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = { wordToPdfLauncher.launch("*/*") }) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Upload DOCX File")
                                    }
                                    Button(
                                        onClick = {
                                            val pdf = DocumentExportEngine.exportToPdfFile(context, buildPdfOfficeFile())
                                            DocumentExportEngine.shareExportedFile(context, pdf, "application/pdf")
                                            viewModel.showMessage("Converted Word document to PDF: ${pdf.name}")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Convert & Download PDF")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = pdfTextContent,
                            onValueChange = { pdfTextContent = it },
                            label = { Text("Word Document Preview before PDF Generation") },
                            minLines = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                2 -> {
                    // --- 12. PDF TO WORD CONVERTER (WITH OCR NOTICE) ---
                    item {
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "PDF to Editable Word (.DOCX) Converter & OCR",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = GoldAccent.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Transparency Notice: Complex multi-column layouts, handwriting, intricate tables, and scanned images are reconstructed into editable paragraphs and may require manual formatting adjustments in Amazing Word.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = { pdfPickerLauncher.launch("application/pdf") }) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Select PDF")
                                    }
                                    Button(
                                        onClick = {
                                            val docxFile = DocumentExportEngine.exportToDocxFile(
                                                context,
                                                buildPdfOfficeFile().copy(
                                                    title = pdfTitle.removeSuffix(".pdf"),
                                                    fileType = "WORD"
                                                )
                                            )
                                            DocumentExportEngine.shareExportedFile(
                                                context,
                                                docxFile,
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                            )
                                            viewModel.showMessage("Converted to editable DOCX: ${docxFile.name}")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Download Editable DOCX")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.createNewOfficeFile(
                                                title = "${pdfTitle.removeSuffix(".pdf")} (Converted)",
                                                fileType = "WORD",
                                                initialContent = pdfTextContent,
                                                openAfterCreate = true
                                            )
                                        }
                                    ) {
                                        Text("Open in Amazing Word")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = pdfTextContent,
                            onValueChange = { pdfTextContent = it },
                            label = { Text("Extracted Editable Word Paragraphs") },
                            minLines = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 13. AMAZING IMAGE EDITOR
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmazingImageEditorScreen(viewModel: OfficeViewModel) {
    val context = LocalContext.current

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var brightness by rememberSaveable { mutableFloatStateOf(0f) } // -100..100
    var contrast by rememberSaveable { mutableFloatStateOf(1f) } // 0.5..1.8
    var saturation by rememberSaveable { mutableFloatStateOf(1f) } // 0..2
    var rotation by rememberSaveable { mutableFloatStateOf(0f) }
    var flipHorizontal by rememberSaveable { mutableStateOf(false) }
    var overlayText by rememberSaveable { mutableStateOf("Amazing Digitals") }
    var watermarkEnabled by rememberSaveable { mutableStateOf(true) }
    var selectedAspectPreset by rememberSaveable { mutableStateOf("16:9 Presentation") }
    var selectedFilter by rememberSaveable { mutableStateOf("NONE") }

    // Undo / Redo Stack
    var undoStack by remember { mutableStateOf(listOf<String>()) }
    var redoStack by remember { mutableStateOf(listOf<String>()) }

    fun serializeState(): String =
        "$brightness|$contrast|$saturation|$rotation|$flipHorizontal|$overlayText|$selectedFilter"

    fun applySerializedState(s: String) {
        val p = s.split("|")
        if (p.size >= 7) {
            brightness = p[0].toFloatOrNull() ?: 0f
            contrast = p[1].toFloatOrNull() ?: 1f
            saturation = p[2].toFloatOrNull() ?: 1f
            rotation = p[3].toFloatOrNull() ?: 0f
            flipHorizontal = p[4].toBooleanStrictOrNull() ?: false
            overlayText = p[5]
            selectedFilter = p[6]
        }
    }

    fun pushUndoSnapshot() {
        undoStack = (undoStack + serializeState()).takeLast(15)
        redoStack = emptyList()
    }

    // Zero-permission Android Photo Picker per Google Play Policy
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bmp
                viewModel.showMessage("Loaded image into Amazing Image Studio.")
            } catch (e: Exception) {
                viewModel.showMessage("Could not decode selected image: ${e.localizedMessage}")
            }
        }
    }

    val aspectRatioFloat = when (selectedAspectPreset) {
        "1:1 Social" -> 1f
        "35:45 Passport" -> 35f / 45f
        "3:4 Poster" -> 3f / 4f
        "4:5 Flyer" -> 4f / 5f
        else -> 16f / 9f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- TOP ACTION BAR ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Amazing Image Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            val last = undoStack.lastOrNull() ?: return@IconButton
                            redoStack = redoStack + serializeState()
                            undoStack = undoStack.dropLast(1)
                            applySerializedState(last)
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = {
                            val next = redoStack.lastOrNull() ?: return@IconButton
                            undoStack = undoStack + serializeState()
                            redoStack = redoStack.dropLast(1)
                            applySerializedState(next)
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Upload Photo")
                    }
                }
            }
        }

        // --- LIVE IMAGE CANVAS ---
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatioFloat)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Edited Image Canvas",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    rotationZ = rotation,
                                    scaleX = if (flipHorizontal) -1f else 1f
                                )
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_office_hero_1794979674208),
                            contentDescription = "Default Studio Canvas",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    rotationZ = rotation,
                                    scaleX = if (flipHorizontal) -1f else 1f
                                )
                        )
                    }

                    // Filter Tint Overlay
                    val filterOverlayColor = when (selectedFilter) {
                        "WARM GOLD" -> GoldAccent.copy(alpha = 0.22f)
                        "COOL CORPORATE" -> RoyalBlue.copy(alpha = 0.22f)
                        "MONOCHROME" -> Color.Black.copy(alpha = 0.38f)
                        else -> Color.Transparent
                    }
                    if (filterOverlayColor != Color.Transparent) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(filterOverlayColor)
                        )
                    }

                    // Text / Logo Overlay
                    if (overlayText.isNotBlank()) {
                        Surface(
                            color = NavyPrimary.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = overlayText,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (watermarkEnabled) {
                        Text(
                            text = "AMAZING DIGITALS STUDIO",
                            color = Color.White.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        )
                    }
                }
            }
        }

        // --- ASPECT RATIO & SIZE PRESETS ---
        item {
            Text("Crop & Size Presets:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "16:9 Presentation",
                    "1:1 Social",
                    "35:45 Passport",
                    "3:4 Poster",
                    "4:5 Flyer"
                ).forEach { preset ->
                    FilterChip(
                        selected = selectedAspectPreset == preset,
                        onClick = {
                            pushUndoSnapshot()
                            selectedAspectPreset = preset
                        },
                        label = { Text(preset) }
                    )
                }
            }
        }

        // --- TRANSFORM & FILTER CONTROLS ---
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            pushUndoSnapshot()
                            rotation = (rotation + 90f) % 360f
                        }) {
                            Icon(Icons.Default.RotateRight, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Rotate 90°")
                        }
                        OutlinedButton(onClick = {
                            pushUndoSnapshot()
                            flipHorizontal = !flipHorizontal
                        }) {
                            Icon(Icons.Default.Flip, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Flip")
                        }
                        FilterChip(
                            selected = watermarkEnabled,
                            onClick = { watermarkEnabled = !watermarkEnabled },
                            label = { Text("Watermark") }
                        )
                    }

                    Text("Filter Presets:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("NONE", "WARM GOLD", "COOL CORPORATE", "MONOCHROME").forEach { f ->
                            FilterChip(
                                selected = selectedFilter == f,
                                onClick = {
                                    pushUndoSnapshot()
                                    selectedFilter = f
                                },
                                label = { Text(f) }
                            )
                        }
                    }

                    Text("Brightness (${brightness.toInt()})", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = -100f..100f
                    )

                    Text("Contrast (${(contrast * 100).toInt()}%)", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = contrast,
                        onValueChange = { contrast = it },
                        valueRange = 0.5f..1.8f
                    )

                    Text("Saturation (${(saturation * 100).toInt()}%)", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..2f
                    )

                    OutlinedTextField(
                        value = overlayText,
                        onValueChange = { overlayText = it },
                        label = { Text("Text / Sticker / Watermark Overlay") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val outFile = exportEditedImageToCache(
                                    context = context,
                                    source = selectedBitmap,
                                    brightness = brightness,
                                    contrast = contrast,
                                    overlayText = overlayText,
                                    formatExt = "png"
                                )
                                DocumentExportEngine.shareExportedFile(context, outFile, "image/png")
                                viewModel.showMessage("Exported PNG Image (original upload preserved): ${outFile.name}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export PNG")
                        }
                        Button(
                            onClick = {
                                val outFile = exportEditedImageToCache(
                                    context = context,
                                    source = selectedBitmap,
                                    brightness = brightness,
                                    contrast = contrast,
                                    overlayText = overlayText,
                                    formatExt = "jpg"
                                )
                                DocumentExportEngine.shareExportedFile(context, outFile, "image/jpeg")
                                viewModel.showMessage("Exported JPG Image: ${outFile.name}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export JPG")
                        }
                    }
                }
            }
        }
    }
}

private fun exportEditedImageToCache(
    context: Context,
    source: Bitmap?,
    brightness: Float,
    contrast: Float,
    overlayText: String,
    formatExt: String
): File {
    val width = source?.width?.coerceAtLeast(400) ?: 800
    val height = source?.height?.coerceAtLeast(300) ?: 450
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(android.graphics.Color.rgb(11, 37, 69))

    val cm = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(cm)
    }
    if (source != null) {
        canvas.drawBitmap(source, 0f, 0f, paint)
    }

    if (overlayText.isNotBlank()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isFakeBoldText = true
        }
        canvas.drawText(overlayText, 32f, height - 36f, textPaint)
    }

    val outFile = File(context.cacheDir, "amazing_studio_export.$formatExt")
    FileOutputStream(outFile).use { fos ->
        val compressFormat = if (formatExt == "jpg") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        output.compress(compressFormat, 92, fos)
    }
    return outFile
}
