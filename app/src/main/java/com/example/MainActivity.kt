package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.OfficeFile
import com.example.data.OfficeRepository
import com.example.data.PaymentRequest
import com.example.data.PricingPlan
import com.example.data.SubscriptionDefaults
import com.example.data.UserProfile
import com.example.ui.screens.AiOfficeAssistantPanel
import com.example.ui.screens.AmazingImageEditorScreen
import com.example.ui.screens.AmazingPdfAndConverterScreen
import com.example.ui.screens.AmazingPresentationEditorScreen
import com.example.ui.screens.AmazingSpreadsheetEditorScreen
import com.example.ui.screens.AmazingWordEditorScreen
import com.example.ui.screens.CloudFileManagerScreen
import com.example.ui.screens.LandingAndAuthScreen
import com.example.ui.screens.MainDashboardView
import com.example.ui.screens.PendingRegistrationData
import com.example.ui.screens.SettingsAndSupportScreen
import com.example.ui.screens.SubscriptionAndPaymentScreen
import com.example.ui.screens.SuperAdminDashboardScreen
import com.example.ui.screens.TemplatesGalleryScreen
import com.example.ui.screens.signOut
import com.example.ui.theme.AmazingOfficeTheme
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoyalBlue
import com.example.util.configureAppCheck
import com.example.viewmodel.OfficeViewModel
import com.example.viewmodel.UiState
import com.example.viewmodel.WorkspaceSection
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge layout
        enableEdgeToEdge()

        // STEP 3: Render Compose UI
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
            AmazingOfficeTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = { isDarkTheme = it }
                    )
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var pendingRegData by remember { mutableStateOf<PendingRegistrationData?>(null) }

    val user = currentUser
    if (user == null) {
        LandingAndAuthScreen(
            onAuthenticated = { regData ->
                pendingRegData = regData
            }
        )
    } else {
        key(user.uid) {
            val viewModel: OfficeViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        OfficeViewModel(
                            repository = OfficeRepository(db),
                            currentUserId = user.uid,
                            currentUserEmail = user.email.orEmpty(),
                            currentUserDisplayName = user.displayName.orEmpty()
                        )
                    }
                }
            )

            LaunchedEffect(pendingRegData) {
                val reg = pendingRegData
                if (reg != null && (reg.fullName.isNotBlank() || reg.username.isNotBlank() || reg.phone.isNotBlank())) {
                    viewModel.completeRegistrationProfile(
                        fullName = reg.fullName.ifBlank { user.displayName.orEmpty() },
                        username = reg.username.ifBlank { user.email?.substringBefore("@").orEmpty() },
                        phone = reg.phone
                    )
                    pendingRegData = null
                }
            }

            AuthenticatedOfficeWorkspace(
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedOfficeWorkspace(
    viewModel: OfficeViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val activeSection by viewModel.activeSection.collectAsStateWithLifecycle()
    val activeFile by viewModel.activeFile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val isAiSheetOpen by viewModel.isAiSheetOpen.collectAsStateWithLifecycle()

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val filesState by viewModel.filesState.collectAsStateWithLifecycle()
    val myPaymentsState by viewModel.myPaymentsState.collectAsStateWithLifecycle()
    val pricingPlansState by viewModel.pricingPlansState.collectAsStateWithLifecycle()

    val profile: UserProfile? = (userProfileState as? UiState.Success)?.data
    val files: List<OfficeFile> = (filesState as? UiState.Success)?.data.orEmpty()
    val myPayments: List<PaymentRequest> = (myPaymentsState as? UiState.Success)?.data.orEmpty()
    val plans: List<PricingPlan> = (pricingPlansState as? UiState.Success)?.data ?: SubscriptionDefaults.defaultPlans

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // BackHandler on secondary screens returns to Home Dashboard
    BackHandler(enabled = activeSection != WorkspaceSection.HOME) {
        viewModel.navigateToSection(WorkspaceSection.HOME)
    }

    if (isAiSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setAiSheetOpen(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AiOfficeAssistantPanel(
                viewModel = viewModel,
                activeFile = activeFile,
                onApplyGeneratedText = { generatedText ->
                    val current = activeFile
                    if (current != null) {
                        val mergedContent = if (current.content.isBlank()) generatedText
                        else "${current.content}\n\n$generatedText"
                        viewModel.saveActiveFile(
                            title = current.title,
                            content = mergedContent,
                            pageSettings = current.pageSettings
                        )
                    } else {
                        viewModel.createNewOfficeFile(
                            title = "AI Generated Document",
                            fileType = "WORD",
                            initialContent = generatedText,
                            openAfterCreate = true
                        )
                    }
                }
            )
        }
    }

    val drawerItems = listOf(
        DrawerNavItem(WorkspaceSection.HOME, "Home Dashboard", Icons.Default.Home),
        DrawerNavItem(WorkspaceSection.WORD_DOCUMENTS, "Word Documents", Icons.Default.Description),
        DrawerNavItem(WorkspaceSection.SPREADSHEETS, "Spreadsheets", Icons.Default.TableChart),
        DrawerNavItem(WorkspaceSection.PRESENTATIONS, "Presentations", Icons.Default.Slideshow),
        DrawerNavItem(WorkspaceSection.PDF_STUDIO, "PDF Studio", Icons.Default.PictureAsPdf),
        DrawerNavItem(WorkspaceSection.IMAGE_EDITOR, "Image Editor", Icons.Default.Image),
        DrawerNavItem(WorkspaceSection.FILE_CONVERTER, "File Converter", Icons.Default.SwapHoriz),
        DrawerNavItem(WorkspaceSection.AI_ASSISTANT, "AI Assistant", Icons.Default.AutoAwesome),
        DrawerNavItem(WorkspaceSection.MY_FILES, "My Files", Icons.Default.Folder),
        DrawerNavItem(WorkspaceSection.SHARED_DOCUMENTS, "Shared Documents", Icons.Default.FolderShared),
        DrawerNavItem(WorkspaceSection.TEMPLATES, "Templates", Icons.Default.ViewQuilt),
        DrawerNavItem(WorkspaceSection.RECENT_ACTIVITY, "Recent Activity", Icons.Default.History),
        DrawerNavItem(WorkspaceSection.SUBSCRIPTION, "Subscription & OPay", Icons.Default.CardMembership),
        DrawerNavItem(WorkspaceSection.SETTINGS, "Settings", Icons.Default.Settings),
        DrawerNavItem(WorkspaceSection.HELP_SUPPORT, "Help & Support", Icons.AutoMirrored.Filled.HelpOutline),
        DrawerNavItem(WorkspaceSection.SUPER_ADMIN, "Super Admin Console", Icons.Default.AdminPanelSettings)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(NavyDark, NavyPrimary, RoyalBlue)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "AMAZING OFFICE AI SUITE",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = profile?.fullName?.ifBlank { viewModel.currentUserDisplayName } ?: "Workspace Member",
                                color = GoldLight,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${profile?.subscriptionTier ?: "FREE"} PLAN • ${viewModel.currentUserEmail}",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    drawerItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.label, fontWeight = FontWeight.Medium) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            selected = activeSection == item.section,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (item.section == WorkspaceSection.AI_ASSISTANT) {
                                    viewModel.setAiSheetOpen(true)
                                } else {
                                    viewModel.navigateToSection(item.section)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            signOut(credentialManager, scope)
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_sign_out_button")
                    )
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = sectionTitle(activeSection),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Amazing Digitals • ${profile?.subscriptionTier ?: "FREE"} Plan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldLight
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_drawer_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.navigateToSection(WorkspaceSection.SUBSCRIPTION) },
                            modifier = Modifier.testTag("top_subscription_button")
                        ) {
                            Icon(Icons.Default.CardMembership, contentDescription = "Subscription", tint = GoldLight)
                        }
                        IconButton(
                            onClick = { viewModel.navigateToSection(WorkspaceSection.SUPER_ADMIN) },
                            modifier = Modifier.testTag("top_admin_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Super Admin Console", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavyPrimary,
                        titleContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.setAiSheetOpen(true) },
                    containerColor = GoldAccent,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini AI Assistant") },
                    text = { Text("Ask AI", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("floating_ai_assistant_button")
                )
            },
            bottomBar = {
                NavigationBar {
                    val bottomItems = listOf(
                        DrawerNavItem(WorkspaceSection.HOME, "Home", Icons.Default.Home),
                        DrawerNavItem(WorkspaceSection.WORD_DOCUMENTS, "Word", Icons.Default.Description),
                        DrawerNavItem(WorkspaceSection.SPREADSHEETS, "Sheets", Icons.Default.TableChart),
                        DrawerNavItem(WorkspaceSection.PRESENTATIONS, "Slides", Icons.Default.Slideshow),
                        DrawerNavItem(WorkspaceSection.PDF_STUDIO, "PDF", Icons.Default.PictureAsPdf),
                        DrawerNavItem(WorkspaceSection.MY_FILES, "Files", Icons.Default.Folder)
                    )
                    bottomItems.forEach { nav ->
                        NavigationBarItem(
                            selected = activeSection == nav.section,
                            onClick = { viewModel.navigateToSection(nav.section) },
                            icon = { Icon(nav.icon, contentDescription = nav.label) },
                            label = { Text(nav.label, maxLines = 1) },
                            modifier = Modifier.testTag("bottom_nav_${nav.label.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeSection) {
                    WorkspaceSection.HOME -> MainDashboardView(
                        viewModel = viewModel,
                        profile = profile,
                        files = files
                    )
                    WorkspaceSection.WORD_DOCUMENTS -> AmazingWordEditorScreen(
                        viewModel = viewModel,
                        activeFile = activeFile?.takeIf { it.fileType == "WORD" }
                    )
                    WorkspaceSection.SPREADSHEETS -> AmazingSpreadsheetEditorScreen(
                        viewModel = viewModel,
                        activeFile = activeFile?.takeIf { it.fileType == "SPREADSHEET" }
                    )
                    WorkspaceSection.PRESENTATIONS -> AmazingPresentationEditorScreen(
                        viewModel = viewModel,
                        activeFile = activeFile?.takeIf { it.fileType == "PRESENTATION" }
                    )
                    WorkspaceSection.PDF_STUDIO -> AmazingPdfAndConverterScreen(
                        viewModel = viewModel,
                        activeFile = activeFile?.takeIf { it.fileType == "PDF" },
                        startInConverterTab = false
                    )
                    WorkspaceSection.FILE_CONVERTER -> AmazingPdfAndConverterScreen(
                        viewModel = viewModel,
                        activeFile = activeFile?.takeIf { it.fileType == "PDF" },
                        startInConverterTab = true
                    )
                    WorkspaceSection.IMAGE_EDITOR -> AmazingImageEditorScreen(
                        viewModel = viewModel
                    )
                    WorkspaceSection.MY_FILES, WorkspaceSection.RECENT_ACTIVITY -> CloudFileManagerScreen(
                        viewModel = viewModel,
                        files = files,
                        searchQuery = searchQuery,
                        initialFilter = "ALL"
                    )
                    WorkspaceSection.SHARED_DOCUMENTS -> CloudFileManagerScreen(
                        viewModel = viewModel,
                        files = files,
                        searchQuery = searchQuery,
                        initialFilter = "SHARED"
                    )
                    WorkspaceSection.TEMPLATES -> TemplatesGalleryScreen(
                        viewModel = viewModel
                    )
                    WorkspaceSection.SUBSCRIPTION -> SubscriptionAndPaymentScreen(
                        viewModel = viewModel,
                        profile = profile,
                        plans = plans,
                        myPayments = myPayments
                    )
                    WorkspaceSection.SETTINGS, WorkspaceSection.HELP_SUPPORT -> SettingsAndSupportScreen(
                        viewModel = viewModel,
                        profile = profile,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        onSignOutClicked = { signOut(credentialManager, scope) }
                    )
                    WorkspaceSection.SUPER_ADMIN -> SuperAdminDashboardScreen(
                        viewModel = viewModel,
                        plans = plans
                    )
                    WorkspaceSection.AI_ASSISTANT -> MainDashboardView(
                        viewModel = viewModel,
                        profile = profile,
                        files = files
                    )
                }
            }
        }
    }
}

private data class DrawerNavItem(
    val section: WorkspaceSection,
    val label: String,
    val icon: ImageVector
)

private fun sectionTitle(section: WorkspaceSection): String = when (section) {
    WorkspaceSection.HOME -> "Amazing Office AI Suite"
    WorkspaceSection.WORD_DOCUMENTS -> "Amazing Word Processor"
    WorkspaceSection.SPREADSHEETS -> "Amazing Sheets"
    WorkspaceSection.PRESENTATIONS -> "Amazing Slides"
    WorkspaceSection.PDF_STUDIO -> "Amazing PDF Studio"
    WorkspaceSection.IMAGE_EDITOR -> "Amazing Image Studio"
    WorkspaceSection.FILE_CONVERTER -> "Word ⇄ PDF Converter"
    WorkspaceSection.AI_ASSISTANT -> "Gemini AI Assistant"
    WorkspaceSection.MY_FILES -> "Cloud File Manager"
    WorkspaceSection.SHARED_DOCUMENTS -> "Shared Documents"
    WorkspaceSection.TEMPLATES -> "Templates Gallery"
    WorkspaceSection.RECENT_ACTIVITY -> "Recent Activity"
    WorkspaceSection.SUBSCRIPTION -> "Subscription & OPay"
    WorkspaceSection.SETTINGS -> "Workspace Settings"
    WorkspaceSection.HELP_SUPPORT -> "Help & Support"
    WorkspaceSection.SUPER_ADMIN -> "Super Admin Portal"
}
