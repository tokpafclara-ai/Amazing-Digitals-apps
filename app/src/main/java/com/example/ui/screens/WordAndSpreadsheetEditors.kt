package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OfficeFile
import com.example.data.OfficeTemplatesCatalog
import com.example.data.SheetTab
import com.example.data.SpreadsheetEngine
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.util.DocumentExportEngine
import com.example.viewmodel.OfficeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 7. AMAZING WORD: ADVANCED WORD PROCESSOR
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmazingWordEditorScreen(
    viewModel: OfficeViewModel,
    activeFile: OfficeFile?
) {
    val context = LocalContext.current
    val initialFile = activeFile ?: OfficeFile(
        title = "Executive Business Proposal",
        fileType = "WORD",
        content = OfficeTemplatesCatalog.templates.first().initialContent
    )

    var docTitle by rememberSaveable(initialFile.id) { mutableStateOf(initialFile.title) }
    var docContent by rememberSaveable(initialFile.id) { mutableStateOf(initialFile.content) }

    // Page settings: paperSize|orientation|margins|header|footer|showPageNumbers
    val initialSettingsParts = remember(initialFile.id) { initialFile.pageSettings.split("|") }
    var paperSize by rememberSaveable(initialFile.id) { mutableStateOf(initialSettingsParts.getOrNull(0) ?: "A4") }
    var orientation by rememberSaveable(initialFile.id) { mutableStateOf(initialSettingsParts.getOrNull(1) ?: "PORTRAIT") }
    var marginPreset by rememberSaveable(initialFile.id) { mutableStateOf(initialSettingsParts.getOrNull(2) ?: "NORMAL") }
    var headerText by rememberSaveable(initialFile.id) { mutableStateOf(initialSettingsParts.getOrNull(3) ?: "Amazing Office AI Suite") }
    var footerText by rememberSaveable(initialFile.id) { mutableStateOf(initialSettingsParts.getOrNull(4) ?: "Amazing Digitals • Confidential") }
    var showPageNumbers by rememberSaveable(initialFile.id) {
        mutableStateOf(initialSettingsParts.getOrNull(5)?.toBooleanStrictOrNull() ?: true)
    }

    // Ribbon Tabs: FILE, HOME, INSERT, LAYOUT, REVIEW, VIEW
    val ribbonTabs = listOf("HOME", "INSERT", "LAYOUT", "REVIEW", "VIEW", "FILE")
    var selectedRibbonTab by rememberSaveable { mutableIntStateOf(0) }

    // Typography & View state
    var fontFamilyName by rememberSaveable { mutableStateOf("Sans-Serif") }
    var fontSizeSp by rememberSaveable { mutableIntStateOf(16) }
    var isBold by rememberSaveable { mutableStateOf(false) }
    var isItalic by rememberSaveable { mutableStateOf(false) }
    var isUnderline by rememberSaveable { mutableStateOf(false) }
    var isStrikethrough by rememberSaveable { mutableStateOf(false) }
    var textAlignMode by rememberSaveable { mutableStateOf("LEFT") }
    var readingMode by rememberSaveable { mutableStateOf(false) }
    var zoomScale by rememberSaveable { mutableFloatStateOf(1.0f) }

    // Find & Replace state
    var findText by rememberSaveable { mutableStateOf("") }
    var replaceText by rememberSaveable { mutableStateOf("") }

    // DOCX / Text File Picker
    val docxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val imported = DocumentExportEngine.importDocxOrTextFromUri(context, uri)
            if (imported.isNotBlank()) {
                docContent = imported
                viewModel.showMessage("Imported document content successfully.")
            } else {
                viewModel.showMessage("Could not extract text from selected file.")
            }
        }
    }

    fun currentPageSettingsString(): String =
        "$paperSize|$orientation|$marginPreset|$headerText|$footerText|$showPageNumbers"

    fun buildCurrentOfficeFile(): OfficeFile = initialFile.copy(
        title = docTitle,
        fileType = "WORD",
        content = docContent,
        pageSettings = currentPageSettingsString()
    )

    val wordCount = remember(docContent) {
        docContent.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val charCount = docContent.length
    val pageCount = remember(docContent) { (docContent.lines().size / 32).coerceAtLeast(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- TOP DOCUMENT TITLE & SAVE / AI / PRINT BAR ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("word_title_input")
                )

                Button(
                    onClick = {
                        viewModel.saveActiveFile(
                            title = docTitle,
                            content = docContent,
                            pageSettings = currentPageSettingsString()
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier.testTag("word_save_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }

                Button(
                    onClick = { viewModel.setAiSheetOpen(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    modifier = Modifier.testTag("word_ai_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI")
                }
            }
        }

        // --- RIBBON TABS ---
        ScrollableTabRow(
            selectedTabIndex = selectedRibbonTab,
            containerColor = NavyPrimary,
            contentColor = Color.White,
            edgePadding = 8.dp
        ) {
            ribbonTabs.forEachIndexed { idx, tabTitle ->
                Tab(
                    selected = selectedRibbonTab == idx,
                    onClick = { selectedRibbonTab = idx },
                    text = {
                        Text(
                            text = tabTitle,
                            fontWeight = if (selectedRibbonTab == idx) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (selectedRibbonTab == idx) GoldAccent else Color.White
                        )
                    }
                )
            }
        }

        // --- ACTIVE RIBBON TOOLBAR CONTENT ---
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (ribbonTabs[selectedRibbonTab]) {
                "HOME" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Sans-Serif", "Serif", "Monospace").forEach { family ->
                            FilterChip(
                                selected = fontFamilyName == family,
                                onClick = { fontFamilyName = family },
                                label = { Text(family) }
                            )
                        }
                        AssistChip(
                            onClick = { fontSizeSp = (fontSizeSp - 2).coerceAtLeast(12) },
                            label = { Text("A-") }
                        )
                        Text("${fontSizeSp}pt", style = MaterialTheme.typography.labelLarge)
                        AssistChip(
                            onClick = { fontSizeSp = (fontSizeSp + 2).coerceAtMost(28) },
                            label = { Text("A+") }
                        )
                        IconButton(onClick = { isBold = !isBold }) {
                            Icon(
                                Icons.Default.FormatBold,
                                contentDescription = "Bold",
                                tint = if (isBold) RoyalBlue else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { isItalic = !isItalic }) {
                            Icon(
                                Icons.Default.FormatItalic,
                                contentDescription = "Italic",
                                tint = if (isItalic) RoyalBlue else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { isUnderline = !isUnderline }) {
                            Icon(
                                Icons.Default.FormatUnderlined,
                                contentDescription = "Underline",
                                tint = if (isUnderline) RoyalBlue else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { isStrikethrough = !isStrikethrough }) {
                            Icon(
                                Icons.Default.FormatStrikethrough,
                                contentDescription = "Strikethrough",
                                tint = if (isStrikethrough) RoyalBlue else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { textAlignMode = "LEFT" }) {
                            Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Align Left")
                        }
                        IconButton(onClick = { textAlignMode = "CENTER" }) {
                            Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center")
                        }
                        IconButton(onClick = { textAlignMode = "RIGHT" }) {
                            Icon(Icons.AutoMirrored.Filled.FormatAlignRight, contentDescription = "Align Right")
                        }
                        IconButton(onClick = { docContent += "\n• " }) {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bullets")
                        }
                        IconButton(onClick = { docContent += "\n1. " }) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbering")
                        }
                    }
                }
                "INSERT" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                docContent += "\n\n| Column 1 | Column 2 | Column 3 |\n| Item A | ₦25,000 | Approved |\n| Item B | ₦40,000 | Pending |\n"
                            },
                            label = { Text("+ Table") }
                        )
                        AssistChip(
                            onClick = { docContent += "\n\n## Section Heading\n" },
                            label = { Text("+ Heading") }
                        )
                        AssistChip(
                            onClick = {
                                val nowStr = SimpleDateFormat("MMMM d, yyyy HH:mm", Locale.US).format(Date())
                                docContent += " $nowStr "
                            },
                            label = { Text("+ Date/Time") }
                        )
                        AssistChip(
                            onClick = { docContent += "\n\n--- PAGE BREAK ---\n\n" },
                            label = { Text("+ Page Break") }
                        )
                        AssistChip(
                            onClick = { docContent += " [https://amazingdigitals.com] " },
                            label = { Text("+ Hyperlink") }
                        )
                        listOf("₦", "©", "®", "™", "✓", "★").forEach { sym ->
                            AssistChip(
                                onClick = { docContent += sym },
                                label = { Text(sym) }
                            )
                        }
                    }
                }
                "LAYOUT" -> {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Paper:", style = MaterialTheme.typography.labelLarge)
                            listOf("A4", "LETTER").forEach { p ->
                                FilterChip(selected = paperSize == p, onClick = { paperSize = p }, label = { Text(p) })
                            }
                            Text("Orientation:", style = MaterialTheme.typography.labelLarge)
                            listOf("PORTRAIT", "LANDSCAPE").forEach { o ->
                                FilterChip(selected = orientation == o, onClick = { orientation = o }, label = { Text(o) })
                            }
                            Text("Margins:", style = MaterialTheme.typography.labelLarge)
                            listOf("NORMAL", "NARROW", "WIDE").forEach { m ->
                                FilterChip(selected = marginPreset == m, onClick = { marginPreset = m }, label = { Text(m) })
                            }
                            FilterChip(
                                selected = showPageNumbers,
                                onClick = { showPageNumbers = !showPageNumbers },
                                label = { Text("Page Numbers") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = headerText,
                                onValueChange = { headerText = it },
                                label = { Text("Header") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = footerText,
                                onValueChange = { footerText = it },
                                label = { Text("Footer") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                "REVIEW" -> {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = findText,
                                onValueChange = { findText = it },
                                label = { Text("Find") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = replaceText,
                                onValueChange = { replaceText = it },
                                label = { Text("Replace With") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (findText.isNotEmpty()) {
                                        docContent = docContent.replace(findText, replaceText, ignoreCase = true)
                                        viewModel.showMessage("Replaced occurrences of \"$findText\".")
                                    }
                                }
                            ) {
                                Icon(Icons.Default.FindReplace, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Replace")
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {
                                    viewModel.setAiSheetOpen(true)
                                    viewModel.runAiOfficeCommand(
                                        taskPrompt = "Proofread and polish the grammar and tone of this document while keeping its structure.",
                                        documentContext = docContent,
                                        targetContext = "WORD"
                                    )
                                },
                                label = { Text("AI Grammar & Polish") }
                            )
                            AssistChip(
                                onClick = {
                                    viewModel.setAiSheetOpen(true)
                                    viewModel.runAiOfficeCommand(
                                        taskPrompt = "Write an executive summary of this document.",
                                        documentContext = docContent,
                                        targetContext = "WORD"
                                    )
                                },
                                label = { Text("AI Summarize") }
                            )
                        }
                    }
                }
                "VIEW" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !readingMode,
                            onClick = { readingMode = false },
                            label = { Text("Print Layout") }
                        )
                        FilterChip(
                            selected = readingMode,
                            onClick = { readingMode = true },
                            label = { Text("Reading Mode") }
                        )
                        listOf(0.85f to "85%", 1.0f to "100%", 1.15f to "115%", 1.3f to "130%").forEach { (scale, label) ->
                            FilterChip(
                                selected = zoomScale == scale,
                                onClick = { zoomScale = scale },
                                label = { Text("Zoom $label") }
                            )
                        }
                    }
                }
                "FILE" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = {
                            docTitle = "Untitled Document"
                            docContent = ""
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New")
                        }
                        OutlinedButton(onClick = { docxLauncher.launch("*/*") }) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import DOCX")
                        }
                        Button(
                            onClick = {
                                val file = DocumentExportEngine.exportToDocxFile(context, buildCurrentOfficeFile())
                                DocumentExportEngine.shareExportedFile(
                                    context,
                                    file,
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                )
                                viewModel.showMessage("Exported DOCX: ${file.name}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Download DOCX")
                        }
                        Button(
                            onClick = {
                                val pdf = DocumentExportEngine.exportToPdfFile(context, buildCurrentOfficeFile())
                                DocumentExportEngine.shareExportedFile(context, pdf, "application/pdf")
                                viewModel.showMessage("Exported PDF: ${pdf.name}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export PDF")
                        }
                        Button(
                            onClick = {
                                DocumentExportEngine.printOfficeDocument(context, buildCurrentOfficeFile())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Print")
                        }
                    }
                }
            }
        }

        // --- PAGINATED DOCUMENT CANVAS ---
        val resolvedFontFamily = when (fontFamilyName) {
            "Serif" -> FontFamily.Serif
            "Monospace" -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
        val resolvedTextAlign = when (textAlignMode) {
            "CENTER" -> TextAlign.Center
            "RIGHT" -> TextAlign.End
            else -> TextAlign.Start
        }
        val decorations = buildList {
            if (isUnderline) add(TextDecoration.Underline)
            if (isStrikethrough) add(TextDecoration.LineThrough)
        }
        val combinedDecoration = if (decorations.isEmpty()) TextDecoration.None else TextDecoration.combine(decorations)
        val pagePadding = when (marginPreset) {
            "NARROW" -> 12.dp
            "WIDE" -> 28.dp
            else -> 20.dp
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pagePadding)
                ) {
                    // Page Header Preview
                    if (headerText.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = headerText,
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "$paperSize • $orientation",
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFCBD5E1))
                    }

                    if (readingMode) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = docContent,
                                style = TextStyle(
                                    fontFamily = resolvedFontFamily,
                                    fontSize = (fontSizeSp * zoomScale).sp,
                                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                    textDecoration = combinedDecoration,
                                    textAlign = resolvedTextAlign,
                                    color = Color(0xFF0F172A),
                                    lineHeight = (fontSizeSp * zoomScale * 1.45f).sp
                                )
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = docContent,
                            onValueChange = { docContent = it },
                            readOnly = false,
                            textStyle = TextStyle(
                                fontFamily = resolvedFontFamily,
                                fontSize = (fontSizeSp * zoomScale).sp,
                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = combinedDecoration,
                                textAlign = resolvedTextAlign,
                                color = Color(0xFF0F172A),
                                lineHeight = (fontSizeSp * zoomScale * 1.4f).sp
                            ),
                            placeholder = { Text("Start typing your document, report, letter, or proposal...") },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("word_editor_content_input")
                        )
                    }

                    // Page Footer & Word Count Bar
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFCBD5E1))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = footerText,
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = buildString {
                                append("$wordCount words • $charCount chars")
                                if (showPageNumbers) append(" • Page 1 of $pageCount")
                            },
                            color = NavyPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 8. AMAZING SHEETS: SPREADSHEET APPLICATION WITH FORMULA ENGINE & CHARTS
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmazingSpreadsheetEditorScreen(
    viewModel: OfficeViewModel,
    activeFile: OfficeFile?
) {
    val context = LocalContext.current
    val initialFile = activeFile ?: OfficeFile(
        title = "Corporate Revenue & Profit Ledger",
        fileType = "SPREADSHEET",
        content = SpreadsheetEngine.createDefaultBudgetJson()
    )

    var sheetTitle by rememberSaveable(initialFile.id) { mutableStateOf(initialFile.title) }
    var sheets by remember(initialFile.id) {
        mutableStateOf(SpreadsheetEngine.parseSheets(initialFile.content))
    }
    var activeSheetIndex by rememberSaveable(initialFile.id) { mutableIntStateOf(0) }
    val currentSheet = sheets.getOrElse(activeSheetIndex) { SheetTab(name = "Sheet 1") }

    var selectedCellRef by rememberSaveable { mutableStateOf("D2") }
    var formulaInput by rememberSaveable { mutableStateOf(currentSheet.cells["D2"] ?: "=B2-C2") }
    var showChartPanel by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(selectedCellRef, activeSheetIndex) {
        formulaInput = sheets.getOrNull(activeSheetIndex)?.cells?.get(selectedCellRef).orEmpty()
    }

    fun updateCellInCurrentSheet(ref: String, rawValue: String) {
        val mutableSheets = sheets.toMutableList()
        val sheet = mutableSheets.getOrNull(activeSheetIndex) ?: return
        val newCells = sheet.cells.toMutableMap()
        if (rawValue.isBlank()) newCells.remove(ref) else newCells[ref] = rawValue
        mutableSheets[activeSheetIndex] = sheet.copy(cells = newCells)
        sheets = mutableSheets
    }

    fun updateChartInCurrentSheet(chartType: String, chartTitle: String) {
        val mutableSheets = sheets.toMutableList()
        val sheet = mutableSheets.getOrNull(activeSheetIndex) ?: return
        mutableSheets[activeSheetIndex] = sheet.copy(chartType = chartType, chartTitle = chartTitle)
        sheets = mutableSheets
    }

    fun buildCurrentOfficeFile(): OfficeFile = initialFile.copy(
        title = sheetTitle,
        fileType = "SPREADSHEET",
        content = SpreadsheetEngine.serializeSheets(sheets)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- TOP ACTION BAR ---
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sheetTitle,
                            onValueChange = { sheetTitle = it },
                            label = { Text("Spreadsheet Title") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sheet_title_input")
                        )
                        Button(
                            onClick = {
                                viewModel.saveActiveFile(
                                    title = sheetTitle,
                                    content = SpreadsheetEngine.serializeSheets(sheets),
                                    pageSettings = initialFile.pageSettings
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.testTag("sheet_save_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save")
                        }
                        IconButton(onClick = { showChartPanel = !showChartPanel }) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = "Toggle Chart",
                                tint = if (showChartPanel) RoyalBlue else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            val csv = DocumentExportEngine.exportSpreadsheetToCsvFile(context, buildCurrentOfficeFile())
                            DocumentExportEngine.shareExportedFile(context, csv, "text/csv")
                            viewModel.showMessage("Exported CSV: ${csv.name}")
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Export CSV")
                        }
                        IconButton(onClick = {
                            DocumentExportEngine.printOfficeDocument(context, buildCurrentOfficeFile())
                        }) {
                            Icon(Icons.Default.Print, contentDescription = "Print Spreadsheet")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // --- FORMULA BAR (fx) ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = NavyPrimary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedCellRef,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                        Icon(Icons.Default.Functions, contentDescription = "Formula", tint = RoyalBlue)
                        OutlinedTextField(
                            value = formulaInput,
                            onValueChange = {
                                formulaInput = it
                                updateCellInCurrentSheet(selectedCellRef, it)
                            },
                            placeholder = { Text("Enter value or formula (e.g. =SUM(B2:B5), =IF(D2>0,\"Profit\",\"Loss\"))") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sheet_formula_input")
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Quick Formula Snippets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val snippets = listOf(
                            "=SUM(B2:B5)",
                            "=AVERAGE(B2:B5)",
                            "=MAX(B2:B5)",
                            "=MIN(B2:B5)",
                            "=COUNT(B2:B5)",
                            "=IF(D2>500000,\"High\",\"Standard\")",
                            "=ROUND(D2,0)",
                            "=TODAY()"
                        )
                        snippets.forEach { formula ->
                            AssistChip(
                                onClick = {
                                    formulaInput = formula
                                    updateCellInCurrentSheet(selectedCellRef, formula)
                                },
                                label = { Text(formula) }
                            )
                        }
                    }
                }
            }
        }

        // --- INTERACTIVE CHART VISUALIZATION PANEL ---
        item {
            AnimatedVisibility(visible = showChartPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentSheet.chartTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("COLUMN", "BAR", "LINE", "PIE", "DOUGHNUT", "AREA").forEach { cType ->
                                    FilterChip(
                                        selected = currentSheet.chartType == cType,
                                        onClick = { updateChartInCurrentSheet(cType, currentSheet.chartTitle) },
                                        label = { Text(cType, style = MaterialTheme.typography.labelMedium) }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        val chartPoints = remember(currentSheet.cells) {
                            (2..5).mapNotNull { row ->
                                val label = SpreadsheetEngine.evaluateCell("A$row", currentSheet.cells)
                                val value = SpreadsheetEngine.evaluateCell("B$row", currentSheet.cells).toDoubleOrNull()
                                    ?: SpreadsheetEngine.evaluateCell("C$row", currentSheet.cells).toDoubleOrNull()
                                if (label.isNotBlank() && value != null) label to value.toFloat() else null
                            }
                        }

                        SpreadsheetChartCanvas(
                            chartType = currentSheet.chartType,
                            points = chartPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }
            }
        }

        // --- MULTI-SHEET TABS ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sheets.forEachIndexed { idx, sheet ->
                    FilterChip(
                        selected = activeSheetIndex == idx,
                        onClick = { activeSheetIndex = idx },
                        label = { Text(sheet.name, fontWeight = FontWeight.Bold) }
                    )
                }
                AssistChip(
                    onClick = {
                        val newSheet = SheetTab(name = "Sheet ${sheets.size + 1}")
                        sheets = sheets + newSheet
                        activeSheetIndex = sheets.lastIndex
                    },
                    label = { Text("+ Add Sheet") }
                )
            }
        }

        // --- SPREADSHEET GRID ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Header Row (Columns A..H)
                    Row(modifier = Modifier.background(NavyPrimary)) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(36.dp)
                                .border(0.5.dp, Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("#", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        SpreadsheetEngine.columns.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(36.dp)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(col, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Rows 1..12
                    for (r in 1..12) {
                        Row {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(r.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            SpreadsheetEngine.columns.forEach { col ->
                                val ref = "$col$r"
                                val isSelected = selectedCellRef == ref
                                val evaluatedValue = SpreadsheetEngine.evaluateCell(ref, currentSheet.cells)
                                val rawVal = currentSheet.cells[ref].orEmpty()
                                val isFormula = rawVal.startsWith("=")
                                val isError = evaluatedValue.startsWith("#")

                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(40.dp)
                                        .background(
                                            when {
                                                isSelected -> RoyalBlue.copy(alpha = 0.14f)
                                                r == 1 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                                else -> MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) RoyalBlue else MaterialTheme.colorScheme.outlineVariant
                                        )
                                        .clickable {
                                            selectedCellRef = ref
                                            formulaInput = currentSheet.cells[ref].orEmpty()
                                        }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = evaluatedValue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (r == 1 || isFormula) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isError -> MaterialTheme.colorScheme.error
                                            isFormula -> RoyalBlue
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1
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

@Composable
private fun SpreadsheetChartCanvas(
    chartType: String,
    points: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        Color(0xFF1D4ED8),
        Color(0xFF059669),
        Color(0xFFD97706),
        Color(0xFF7C3AED),
        Color(0xFFDC2626)
    )
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Enter numeric data in rows A2:B5 to render chart.")
        }
        return
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
        ) {
            val maxVal = (points.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
            when (chartType) {
                "PIE", "DOUGHNUT" -> {
                    val total = points.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
                    val diameter = size.minDimension * 0.85f
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    var startAngle = -90f
                    points.forEachIndexed { idx, (_, value) ->
                        val sweep = (value / total) * 360f
                        drawArc(
                            color = palette[idx % palette.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = chartType == "PIE",
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = if (chartType == "DOUGHNUT") Stroke(width = diameter * 0.25f) else androidx.compose.ui.graphics.drawscope.Fill
                        )
                        startAngle += sweep
                    }
                }
                "LINE", "AREA" -> {
                    val stepX = size.width / (points.size.coerceAtLeast(2) - 1).toFloat()
                    val path = Path()
                    val areaPath = Path()
                    points.forEachIndexed { idx, (_, value) ->
                        val x = idx * stepX
                        val y = size.height - (value / maxVal) * (size.height * 0.82f)
                        if (idx == 0) {
                            path.moveTo(x, y)
                            areaPath.moveTo(x, size.height)
                            areaPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            areaPath.lineTo(x, y)
                        }
                        drawCircle(color = RoyalBlue, radius = 6.dp.toPx(), center = Offset(x, y))
                    }
                    if (chartType == "AREA") {
                        areaPath.lineTo(size.width, size.height)
                        areaPath.close()
                        drawPath(areaPath, color = RoyalBlue.copy(alpha = 0.22f))
                    }
                    drawPath(path, color = RoyalBlue, style = Stroke(width = 3.dp.toPx()))
                }
                "BAR" -> {
                    val barHeight = (size.height / (points.size * 1.6f)).coerceAtLeast(12f)
                    points.forEachIndexed { idx, (_, value) ->
                        val w = (value / maxVal) * (size.width * 0.88f)
                        val y = idx * (barHeight * 1.45f) + 6f
                        drawRect(
                            color = palette[idx % palette.size],
                            topLeft = Offset(0f, y),
                            size = Size(w, barHeight)
                        )
                    }
                }
                else -> {
                    // COLUMN
                    val colWidth = (size.width / (points.size * 2f)).coerceAtLeast(18f)
                    val spacing = size.width / points.size
                    points.forEachIndexed { idx, (_, value) ->
                        val h = (value / maxVal) * (size.height * 0.85f)
                        val x = idx * spacing + (spacing - colWidth) / 2f
                        drawRect(
                            color = palette[idx % palette.size],
                            topLeft = Offset(x, size.height - h),
                            size = Size(colWidth, h)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            points.forEachIndexed { idx, (label, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(palette[idx % palette.size])
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$label (${String.format(Locale.US, "%,.0f", value)})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
