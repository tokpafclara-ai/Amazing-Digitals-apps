package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.OfficeFile
import com.example.data.OfficeTemplate
import com.example.data.OfficeTemplatesCatalog
import com.example.data.UserProfile
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.util.DocumentExportEngine
import com.example.viewmodel.OfficeViewModel
import com.example.viewmodel.WorkspaceSection
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainDashboardView(
    viewModel: OfficeViewModel,
    profile: UserProfile?,
    files: List<OfficeFile>
) {
    val activeFiles = remember(files) { files.filter { !it.trashed && it.fileType != "FOLDER" } }
    val wordCount = remember(activeFiles) { activeFiles.count { it.fileType == "WORD" } }
    val sheetCount = remember(activeFiles) { activeFiles.count { it.fileType == "SPREADSHEET" } }
    val slideCount = remember(activeFiles) { activeFiles.count { it.fileType == "PRESENTATION" } }
    val totalUsedBytes = remember(activeFiles) { activeFiles.sumOf { it.sizeBytes } }
    val quotaBytes = profile?.storageQuotaBytes ?: 52_428_800L
    val tier = profile?.subscriptionTier ?: "FREE"

    var showCreateDialog by remember { mutableStateOf(false) }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocType by remember { mutableStateOf("WORD") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Office File", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newDocTitle,
                        onValueChange = { newDocTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_doc_title_input")
                    )
                    Text("Select Application:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WORD", "SPREADSHEET", "PRESENTATION", "PDF", "IMAGE").forEach { type ->
                            FilterChip(
                                selected = newDocType == type,
                                onClick = { newDocType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateDialog = false
                        viewModel.createNewOfficeFile(
                            title = newDocTitle.ifBlank { "Untitled $newDocType" },
                            fileType = newDocType,
                            openAfterCreate = true
                        )
                        newDocTitle = ""
                    },
                    modifier = Modifier.testTag("confirm_create_doc_button")
                ) {
                    Text("Create & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- WELCOME HERO CARD ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(NavyPrimary, RoyalBlue)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = GoldAccent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$tier PLAN",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("dashboard_create_new_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create New", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Create New", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "WELCOME TO AMAZING OFFICE AI!",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Everything you need to work smarter, create faster and achieve more.",
                            color = Color(0xFFE2E8F0),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // --- 6 QUICK ACTION CARDS ---
        item {
            Text(
                text = "Quick Launch Applications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            val quickActions = listOf(
                QuickActionCardData("Create Document", "Amazing Word", Icons.Default.Description, Color(0xFF1D4ED8)) {
                    viewModel.createNewOfficeFile("Executive Document", "WORD")
                },
                QuickActionCardData("Create Spreadsheet", "Amazing Sheets", Icons.Default.TableChart, Color(0xFF059669)) {
                    viewModel.createNewOfficeFile("Financial Ledger", "SPREADSHEET")
                },
                QuickActionCardData("Create Presentation", "Amazing Slides", Icons.Default.Slideshow, Color(0xFFD97706)) {
                    viewModel.createNewOfficeFile("Executive Pitch Deck", "PRESENTATION")
                },
                QuickActionCardData("Edit PDF", "PDF Studio", Icons.Default.PictureAsPdf, Color(0xFFDC2626)) {
                    viewModel.navigateToSection(WorkspaceSection.PDF_STUDIO)
                },
                QuickActionCardData("Edit Image", "Image Studio", Icons.Default.Image, Color(0xFF7C3AED)) {
                    viewModel.navigateToSection(WorkspaceSection.IMAGE_EDITOR)
                },
                QuickActionCardData("Convert File", "Word ⇄ PDF", Icons.Default.SwapHoriz, Color(0xFF0284C7)) {
                    viewModel.navigateToSection(WorkspaceSection.FILE_CONVERTER)
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quickActions.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { action ->
                            ElevatedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { action.onClick() }
                                    .testTag("quick_action_${action.title.lowercase().replace(" ", "_")}"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(action.color.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = action.icon,
                                            contentDescription = action.title,
                                            tint = action.color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = action.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = action.subtitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- WORKSPACE METRICS & STORAGE QUOTA ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatMetricBox("Total Files", activeFiles.size.toString(), RoyalBlue)
                        StatMetricBox("Word Docs", wordCount.toString(), Color(0xFF1D4ED8))
                        StatMetricBox("Spreadsheets", sheetCount.toString(), Color(0xFF059669))
                        StatMetricBox("Slide Decks", slideCount.toString(), Color(0xFFD97706))
                    }

                    Spacer(Modifier.height(14.dp))

                    val usedKb = (totalUsedBytes / 1024.0)
                    val quotaMb = (quotaBytes / (1024.0 * 1024.0))
                    val ratio = (totalUsedBytes.toFloat() / quotaBytes.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = String.format(Locale.US, "Cloud Storage: %.1f KB used of %.0f MB", usedKb, quotaMb),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        TextButton(onClick = { viewModel.navigateToSection(WorkspaceSection.SUBSCRIPTION) }) {
                            Text("Upgrade Plan", color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = RoyalBlue
                    )
                }
            }
        }

        // --- RECENT DOCUMENTS, SPREADSHEETS & PRESENTATIONS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Workspace Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.navigateToSection(WorkspaceSection.MY_FILES) }) {
                    Text("View All Files")
                }
            }
        }

        if (activeFiles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Your Cloud Workspace is Ready",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap any Quick Launch button above or start from a professional template below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activeFiles.take(8), key = { it.id }) { file ->
                OfficeFileRowCard(
                    file = file,
                    onOpen = { viewModel.openFileInEditor(file) },
                    onDuplicate = { viewModel.duplicateFile(file) },
                    onTrash = { viewModel.toggleTrashFile(file, moveToTrash = true) }
                )
            }
        }

        // --- FEATURED TEMPLATES ---
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Starter Templates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.navigateToSection(WorkspaceSection.TEMPLATES) }) {
                    Text("All Templates")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OfficeTemplatesCatalog.templates.forEach { tpl ->
                    ElevatedCard(
                        modifier = Modifier
                            .width(230.dp)
                            .clickable { viewModel.createFromTemplate(tpl) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                color = RoyalBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${tpl.fileType} • ${tpl.category}",
                                    color = RoyalBlue,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = tpl.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = tpl.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class QuickActionCardData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun OfficeFileRowCard(
    file: OfficeFile,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onTrash: () -> Unit,
    onShare: ((String) -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onPermanentDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showShareDialog by remember { mutableStateOf(false) }
    var shareEmail by remember { mutableStateOf("") }

    if (showShareDialog && onShare != null) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share \"${file.title}\"", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter collaborator email to grant shared access:")
                    OutlinedTextField(
                        value = shareEmail,
                        onValueChange = { shareEmail = it },
                        label = { Text("Collaborator Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (file.sharedWithEmails.isNotBlank()) {
                        Text(
                            text = "Currently shared with: ${file.sharedWithEmails}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onShare(shareEmail)
                    shareEmail = ""
                    showShareDialog = false
                }) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val (icon, tint) = when (file.fileType) {
        "WORD" -> Icons.Default.Description to Color(0xFF1D4ED8)
        "SPREADSHEET" -> Icons.Default.TableChart to Color(0xFF059669)
        "PRESENTATION" -> Icons.Default.Slideshow to Color(0xFFD97706)
        "PDF" -> Icons.Default.PictureAsPdf to Color(0xFFDC2626)
        "IMAGE" -> Icons.Default.Image to Color(0xFF7C3AED)
        "FOLDER" -> Icons.Default.Folder to GoldAccent
        else -> Icons.Default.Description to RoyalBlue
    }

    val dateStr = remember(file.updatedAt, file.createdAt) {
        val ts = file.updatedAt ?: file.createdAt
        if (ts != null) {
            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(ts.toDate())
        } else "Just now"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = file.fileType, tint = tint)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.fileType} • ${(file.sizeBytes / 1024).coerceAtLeast(1)} KB • $dateStr",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (file.sharedWithEmails.isNotBlank()) {
                    Text(
                        text = "Shared with: ${file.sharedWithEmails}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SuccessGreen
                    )
                }
            }

            if (file.trashed) {
                if (onRestore != null) {
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore", tint = SuccessGreen)
                    }
                }
                if (onPermanentDelete != null) {
                    IconButton(onClick = onPermanentDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                IconButton(onClick = { DocumentExportEngine.printOfficeDocument(context, file) }) {
                    Icon(Icons.Default.Print, contentDescription = "Print")
                }
                if (onShare != null) {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                }
                IconButton(onClick = onTrash) {
                    Icon(Icons.Default.Delete, contentDescription = "Move to Trash")
                }
            }
        }
    }
}

// --- CLOUD FILE MANAGER SCREEN ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloudFileManagerScreen(
    viewModel: OfficeViewModel,
    files: List<OfficeFile>,
    searchQuery: String,
    initialFilter: String = "ALL"
) {
    var categoryFilter by rememberSaveable(initialFilter) { mutableStateOf(initialFilter) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create Cloud Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (folderName.isNotBlank()) {
                        viewModel.createNewOfficeFile(
                            title = folderName,
                            fileType = "FOLDER",
                            initialContent = "Cloud Folder",
                            openAfterCreate = false
                        )
                    }
                    folderName = ""
                    showNewFolderDialog = false
                }) {
                    Text("Create Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val filteredFiles = remember(files, categoryFilter, searchQuery) {
        files.filter { file ->
            val matchesQuery = searchQuery.isBlank() ||
                file.title.contains(searchQuery, ignoreCase = true) ||
                file.content.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (categoryFilter) {
                "TRASH" -> file.trashed
                "SHARED" -> !file.trashed && file.sharedWithEmails.isNotBlank()
                "ALL" -> !file.trashed
                else -> !file.trashed && file.fileType == categoryFilter
            }
            matchesQuery && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Personal Cloud File Manager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            OutlinedButton(onClick = { showNewFolderDialog = true }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Folder")
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            placeholder = { Text("Search documents, spreadsheets, slides, PDFs...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("file_manager_search_input")
        )

        Spacer(Modifier.height(10.dp))

        val categories = listOf(
            "ALL" to "All Files",
            "WORD" to "Documents",
            "SPREADSHEET" to "Spreadsheets",
            "PRESENTATION" to "Presentations",
            "PDF" to "PDFs",
            "IMAGE" to "Images",
            "FOLDER" to "Folders",
            "SHARED" to "Shared",
            "TRASH" to "Recently Deleted"
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { (key, label) ->
                FilterChip(
                    selected = categoryFilter == key,
                    onClick = { categoryFilter = key },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No files found in this category.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFiles, key = { it.id }) { file ->
                    OfficeFileRowCard(
                        file = file,
                        onOpen = {
                            if (file.fileType != "FOLDER") {
                                viewModel.openFileInEditor(file)
                            }
                        },
                        onDuplicate = { viewModel.duplicateFile(file) },
                        onTrash = { viewModel.toggleTrashFile(file, moveToTrash = true) },
                        onShare = { email -> viewModel.shareFileWithEmail(file, email) },
                        onRestore = { viewModel.toggleTrashFile(file, moveToTrash = false) },
                        onPermanentDelete = { viewModel.deleteFilePermanently(file) }
                    )
                }
            }
        }
    }
}

// --- TEMPLATES GALLERY SCREEN ---
@Composable
fun TemplatesGalleryScreen(viewModel: OfficeViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Professional Office Templates",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Launch ready-made templates for business, education, church ministry, finance, and careers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(OfficeTemplatesCatalog.templates, key = { it.id }) { tpl ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = RoyalBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${tpl.fileType} • ${tpl.category}",
                                color = RoyalBlue,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Button(onClick = { viewModel.createFromTemplate(tpl) }) {
                            Text("Use Template")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = tpl.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tpl.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- SETTINGS & HELP SCREEN ---
@Composable
fun SettingsAndSupportScreen(
    viewModel: OfficeViewModel,
    profile: UserProfile?,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSignOutClicked: () -> Unit
) {
    var fullName by rememberSaveable(profile?.fullName) { mutableStateOf(profile?.fullName.orEmpty()) }
    var username by rememberSaveable(profile?.username) { mutableStateOf(profile?.username.orEmpty()) }
    var phone by rememberSaveable(profile?.phone) { mutableStateOf(profile?.phone.orEmpty()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Workspace Settings & Support",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Account Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Workspace Theme", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = isDarkTheme, onCheckedChange = onToggleDarkTheme)
                    }
                    Button(
                        onClick = { viewModel.completeRegistrationProfile(fullName, username, phone) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Profile Changes")
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GoldAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Help, Support & Billing Guidance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "• Product Brand: AMAZING OFFICE AI SUITE by Amazing Digitals\n" +
                            "• Official OPay Payment Account: 9080301021 (Emmanuel Ajibade)\n" +
                            "• Document Formats: Supports DOCX, CSV, PDF export, OCR extraction & native Android system printing.\n" +
                            "• AI Assistant: Powered by Google Gemini via Firebase AI Logic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onSignOutClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_sign_out_button")
                    ) {
                        Text("Sign Out of Amazing Office AI")
                    }
                }
            }
        }
    }
}
