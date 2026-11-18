package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminRecord
import com.example.data.AuditLog
import com.example.data.OfficeFile
import com.example.data.OfficeRepository
import com.example.data.OfficeTemplate
import com.example.data.OfficeTemplatesCatalog
import com.example.data.PaymentRequest
import com.example.data.PresentationDeck
import com.example.data.PresentationEngine
import com.example.data.PricingPlan
import com.example.data.SlideItem
import com.example.data.SpreadsheetEngine
import com.example.data.SubscriptionDefaults
import com.example.data.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "OfficeViewModel"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiAssistantState {
    data object Idle : AiAssistantState
    data object Loading : AiAssistantState
    data class Streaming(val partialText: String, val targetContext: String) : AiAssistantState
    data class Success(val generatedText: String, val targetContext: String) : AiAssistantState
    data class Error(val message: String) : AiAssistantState
}

enum class WorkspaceSection {
    HOME,
    WORD_DOCUMENTS,
    SPREADSHEETS,
    PRESENTATIONS,
    PDF_STUDIO,
    IMAGE_EDITOR,
    FILE_CONVERTER,
    AI_ASSISTANT,
    MY_FILES,
    SHARED_DOCUMENTS,
    TEMPLATES,
    RECENT_ACTIVITY,
    SUBSCRIPTION,
    SETTINGS,
    HELP_SUPPORT,
    SUPER_ADMIN
}

class OfficeViewModel(
    private val repository: OfficeRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String
) : ViewModel() {

    // Lazy initialization of Gemini model via Firebase AI Logic
    private val geminiModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    // Current navigation section
    private val _activeSection = MutableStateFlow(WorkspaceSection.HOME)
    val activeSection: StateFlow<WorkspaceSection> = _activeSection.asStateFlow()

    // Currently open file for editing
    private val _activeFile = MutableStateFlow<OfficeFile?>(null)
    val activeFile: StateFlow<OfficeFile?> = _activeFile.asStateFlow()

    // Global search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Status banner message (toast/snackbar feedback)
    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    // Floating AI Assistant visibility and state
    private val _isAiSheetOpen = MutableStateFlow(false)
    val isAiSheetOpen: StateFlow<Boolean> = _isAiSheetOpen.asStateFlow()

    private val _aiState = MutableStateFlow<AiAssistantState>(AiAssistantState.Idle)
    val aiState: StateFlow<AiAssistantState> = _aiState.asStateFlow()

    // Super Admin Session State
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private val _adminMustChangePassword = MutableStateFlow(false)
    val adminMustChangePassword: StateFlow<Boolean> = _adminMustChangePassword.asStateFlow()

    private val _adminRecord = MutableStateFlow<AdminRecord?>(null)

    private val _adminPayments = MutableStateFlow<List<PaymentRequest>>(emptyList())
    val adminPayments: StateFlow<List<PaymentRequest>> = _adminPayments.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val adminUsers: StateFlow<List<UserProfile>> = _adminUsers.asStateFlow()

    private val _adminAuditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val adminAuditLogs: StateFlow<List<AuditLog>> = _adminAuditLogs.asStateFlow()

    private var adminObserverJobs = mutableListOf<Job>()

    // --- TWO-TIER KOTLIN FLOWS FOR USER DATA ---
    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load user profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filesState: StateFlow<UiState<List<OfficeFile>>> = repository.observeUserFiles(currentUserId)
        .map<List<OfficeFile>, UiState<List<OfficeFile>>> { files ->
            // Keep activeFile synced if updated
            val current = _activeFile.value
            if (current != null) {
                val updated = files.firstOrNull { it.id == current.id }
                if (updated != null) {
                    _activeFile.value = updated
                }
            }
            UiState.Success(files)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user files", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load workspace files"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val myPaymentsState: StateFlow<UiState<List<PaymentRequest>>> = repository.observeMyPaymentRequests(currentUserId)
        .map<List<PaymentRequest>, UiState<List<PaymentRequest>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing payment requests", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load payment history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val pricingPlansState: StateFlow<UiState<List<PricingPlan>>> = repository.observePricingPlans()
        .map<List<PricingPlan>, UiState<List<PricingPlan>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            // Fall back to default plans if collection is empty or restricted
            emit(UiState.Success(SubscriptionDefaults.defaultPlans))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(SubscriptionDefaults.defaultPlans)
        )

    init {
        // Ensure user profile exists in Firestore and seed starter documents if empty
        viewModelScope.launch {
            repository.ensureOrCreateUserProfile(
                fullName = currentUserDisplayName.ifBlank { "Amazing Office Member" },
                username = currentUserEmail.substringBefore("@").ifBlank { "member" },
                email = currentUserEmail.ifBlank { "member@amazingdigitals.com" },
                phone = ""
            )
        }
    }

    fun navigateToSection(section: WorkspaceSection) {
        _activeSection.value = section
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showMessage(msg: String) {
        _bannerMessage.value = msg
    }

    fun clearMessage() {
        _bannerMessage.value = null
    }

    fun setAiSheetOpen(open: Boolean) {
        _isAiSheetOpen.value = open
    }

    fun resetAiState() {
        _aiState.value = AiAssistantState.Idle
    }

    // --- PROFILE ONBOARDING / UPDATE ---
    fun completeRegistrationProfile(
        fullName: String,
        username: String,
        phone: String
    ) {
        viewModelScope.launch {
            val currentFiles = (filesState.value as? UiState.Success)?.data.orEmpty()
            val usedBytes = currentFiles.sumOf { it.sizeBytes }
            val result = repository.updateUserProfileDetails(
                fullName = fullName,
                username = username,
                phone = phone,
                storageUsedBytes = usedBytes
            )
            if (result.isSuccess) {
                showMessage("Profile saved successfully!")
            } else {
                showMessage(result.exceptionOrNull()?.localizedMessage ?: "Could not update profile.")
            }
        }
    }

    // --- DOCUMENT / FILE OPERATIONS ---
    fun createNewOfficeFile(
        title: String,
        fileType: String,
        initialContent: String = "",
        openAfterCreate: Boolean = true
    ) {
        viewModelScope.launch {
            val defaultContent = when {
                initialContent.isNotBlank() -> initialContent
                fileType == "WORD" -> OfficeTemplatesCatalog.templates.first().initialContent
                fileType == "SPREADSHEET" -> SpreadsheetEngine.createDefaultBudgetJson()
                fileType == "PRESENTATION" -> PresentationEngine.createDefaultPresentationJson(title.ifBlank { "Executive Presentation" })
                fileType == "PDF" -> """
                    # AMAZING PDF STUDIO DOCUMENT
                    **Document Title:** ${title.ifBlank { "Annotated Document" }}
                    
                    ## Summary & Highlights
                    • Verified corporate PDF document ready for annotation, watermarking, rotation, merging, and export.
                    • Add text blocks, comments, watermarks, or page numbers below and click Export PDF or Print.
                """.trimIndent()
                fileType == "IMAGE" -> "FILTER:NONE|BRIGHTNESS:0|CONTRAST:100|SATURATION:100|ROTATION:0|TEXT:Amazing Digitals|ASPECT:FREE"
                else -> ""
            }

            val res = repository.createOfficeFile(
                title = title.ifBlank { "Untitled $fileType" },
                fileType = fileType,
                content = defaultContent
            )
            res.onSuccess { newId ->
                showMessage("Created \"${title.ifBlank { "Untitled $fileType" }}\"")
                if (openAfterCreate) {
                    val createdFile = OfficeFile(
                        id = newId,
                        userId = currentUserId,
                        title = title.ifBlank { "Untitled $fileType" },
                        fileType = fileType,
                        content = defaultContent
                    )
                    openFileInEditor(createdFile)
                }
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to create file.")
            }
        }
    }

    fun createFromTemplate(template: OfficeTemplate) {
        createNewOfficeFile(
            title = template.title,
            fileType = template.fileType,
            initialContent = template.initialContent,
            openAfterCreate = true
        )
    }

    fun openFileInEditor(file: OfficeFile) {
        _activeFile.value = file
        _activeSection.value = when (file.fileType) {
            "WORD" -> WorkspaceSection.WORD_DOCUMENTS
            "SPREADSHEET" -> WorkspaceSection.SPREADSHEETS
            "PRESENTATION" -> WorkspaceSection.PRESENTATIONS
            "PDF" -> WorkspaceSection.PDF_STUDIO
            "IMAGE" -> WorkspaceSection.IMAGE_EDITOR
            else -> WorkspaceSection.MY_FILES
        }
    }

    fun saveActiveFile(
        title: String,
        content: String,
        pageSettings: String,
        folderId: String = "",
        sharedWithEmails: String = "",
        silentAutoSave: Boolean = false
    ) {
        val current = _activeFile.value
        if (current == null || current.id.isBlank()) {
            createNewOfficeFile(
                title = title,
                fileType = current?.fileType ?: "WORD",
                initialContent = content,
                openAfterCreate = true
            )
            return
        }
        viewModelScope.launch {
            val updatedFile = current.copy(
                title = title,
                content = content,
                pageSettings = pageSettings,
                folderId = folderId.ifBlank { current.folderId },
                sharedWithEmails = sharedWithEmails.ifBlank { current.sharedWithEmails }
            )
            _activeFile.value = updatedFile
            val res = repository.updateOfficeFile(
                fileId = current.id,
                title = title,
                content = content,
                pageSettings = pageSettings,
                folderId = updatedFile.folderId,
                trashed = current.trashed,
                sharedWithEmails = updatedFile.sharedWithEmails
            )
            if (res.isSuccess) {
                if (!silentAutoSave) showMessage("Saved \"$title\" to Cloud Workspace.")
                syncUsedStorage()
            } else {
                if (!silentAutoSave) {
                    showMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to save document.")
                }
            }
        }
    }

    fun duplicateFile(file: OfficeFile) {
        createNewOfficeFile(
            title = "${file.title} (Copy)",
            fileType = file.fileType,
            initialContent = file.content,
            openAfterCreate = false
        )
    }

    fun toggleTrashFile(file: OfficeFile, moveToTrash: Boolean) {
        viewModelScope.launch {
            val res = repository.updateOfficeFile(
                fileId = file.id,
                title = file.title,
                content = file.content,
                pageSettings = file.pageSettings,
                folderId = file.folderId,
                trashed = moveToTrash,
                sharedWithEmails = file.sharedWithEmails
            )
            if (res.isSuccess) {
                showMessage(if (moveToTrash) "Moved \"${file.title}\" to Recently Deleted." else "Restored \"${file.title}\".")
            }
        }
    }

    fun deleteFilePermanently(file: OfficeFile) {
        viewModelScope.launch {
            val res = repository.deleteOfficeFilePermanently(file.id)
            if (res.isSuccess) {
                if (_activeFile.value?.id == file.id) {
                    _activeFile.value = null
                }
                showMessage("Deleted \"${file.title}\" permanently.")
                syncUsedStorage()
            }
        }
    }

    fun shareFileWithEmail(file: OfficeFile, email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) return
        val existingEmails = file.sharedWithEmails.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val updatedEmails = (existingEmails + cleanEmail).distinct().joinToString(", ")
        viewModelScope.launch {
            val res = repository.updateOfficeFile(
                fileId = file.id,
                title = file.title,
                content = file.content,
                pageSettings = file.pageSettings,
                folderId = file.folderId,
                trashed = file.trashed,
                sharedWithEmails = updatedEmails
            )
            if (res.isSuccess) {
                showMessage("Shared \"${file.title}\" with $cleanEmail.")
            }
        }
    }

    private fun syncUsedStorage() {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val files = (filesState.value as? UiState.Success)?.data ?: return
        val totalBytes = files.filter { !it.trashed }.sumOf { it.sizeBytes }
        viewModelScope.launch {
            repository.updateUserProfileDetails(
                fullName = profile.fullName,
                username = profile.username,
                phone = profile.phone,
                storageUsedBytes = totalBytes
            )
        }
    }

    // --- MANUAL OPAY PAYMENT SUBMISSION ---
    fun submitManualBankTransferPayment(
        customerName: String,
        customerEmail: String,
        phoneNumber: String,
        selectedPlan: String,
        amountTransferred: Long,
        transactionReference: String,
        paymentDate: String,
        receiptSummary: String,
        onSubmitted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.submitManualPaymentRequest(
                customerName = customerName,
                customerEmail = customerEmail,
                phoneNumber = phoneNumber,
                selectedPlan = selectedPlan,
                amountTransferred = amountTransferred,
                transactionReference = transactionReference,
                paymentDate = paymentDate,
                receiptSummary = receiptSummary
            )
            res.onSuccess {
                showMessage("Payment verification request submitted (Status: PENDING). Awaiting Super Administrator confirmation.")
                onSubmitted()
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Could not submit payment request.")
            }
        }
    }

    // --- SUPER ADMINISTRATOR AUTHENTICATION & CONTROLS ---
    fun authenticateSuperAdmin(usernameInput: String, passwordInput: String) {
        val cleanUser = usernameInput.trim()
        if (cleanUser != "amazingdigitals") {
            showMessage("Access denied: Invalid administrator username.")
            return
        }
        viewModelScope.launch {
            val recordResult = repository.getOrProvisionAdminRecord()
            if (recordResult.isFailure) {
                showMessage("Administrator access denied by backend security rules for this Google account.")
                return@launch
            }
            val existingRecord = recordResult.getOrNull()
            if (existingRecord == null) {
                // First login: must match ADMIN_INITIAL_PASSWORD=Mercy2026 and force password change
                if (passwordInput == "Mercy2026") {
                    _isAdminAuthenticated.value = true
                    _adminMustChangePassword.value = true
                    showMessage("First login verified. Please set a new administrator password to continue.")
                } else {
                    showMessage("Invalid initial administrator password.")
                }
            } else {
                val inputHash = repository.hashPassword(passwordInput)
                if (inputHash == existingRecord.passwordHash || (existingRecord.mustChangePassword && passwordInput == "Mercy2026")) {
                    _adminRecord.value = existingRecord
                    _isAdminAuthenticated.value = true
                    _adminMustChangePassword.value = existingRecord.mustChangePassword
                    if (!existingRecord.mustChangePassword) {
                        startAdminStreams()
                        showMessage("Welcome, Super Administrator (amazingdigitals).")
                    }
                } else {
                    showMessage("Invalid administrator password.")
                }
            }
        }
    }

    fun completeMandatoryAdminPasswordChange(newPassword: String, confirmPassword: String) {
        if (newPassword.length < 8) {
            showMessage("New password must be at least 8 characters long.")
            return
        }
        if (newPassword != confirmPassword) {
            showMessage("Passwords do not match.")
            return
        }
        if (newPassword == "Mercy2026") {
            showMessage("Please choose a new password different from the initial password Mercy2026.")
            return
        }
        viewModelScope.launch {
            val res = repository.rotateSuperAdminPassword(newPassword)
            res.onSuccess { record ->
                _adminRecord.value = record
                _adminMustChangePassword.value = false
                startAdminStreams()
                showMessage("Super Administrator password updated and recorded in audit logs.")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to update administrator password (check admin permissions).")
            }
        }
    }

    private fun startAdminStreams() {
        adminObserverJobs.forEach { it.cancel() }
        adminObserverJobs.clear()

        adminObserverJobs += viewModelScope.launch {
            repository.observeAllPaymentRequestsForAdmin()
                .catch { e ->
                    if (e is CancellationException && e !is FirebaseFirestoreException) throw e
                    Log.w(TAG, "Admin payment stream error", e)
                }
                .collect { _adminPayments.value = it }
        }
        adminObserverJobs += viewModelScope.launch {
            repository.observeAllUsersForAdmin()
                .catch { e ->
                    if (e is CancellationException && e !is FirebaseFirestoreException) throw e
                    Log.w(TAG, "Admin users stream error", e)
                }
                .collect { _adminUsers.value = it }
        }
        adminObserverJobs += viewModelScope.launch {
            repository.observeAuditLogsForAdmin()
                .catch { e ->
                    if (e is CancellationException && e !is FirebaseFirestoreException) throw e
                    Log.w(TAG, "Admin audit log stream error", e)
                }
                .collect { _adminAuditLogs.value = it }
        }
    }

    fun adminVerifyOrRejectPayment(
        payment: PaymentRequest,
        newStatus: String,
        verificationNotes: String
    ) {
        if (!_isAdminAuthenticated.value || _adminMustChangePassword.value) {
            showMessage("Super Administrator verification required.")
            return
        }
        viewModelScope.launch {
            val res = repository.adminVerifyOrRejectPayment(
                payment = payment,
                newStatus = newStatus,
                verificationNotes = verificationNotes
            )
            res.onSuccess {
                showMessage("Payment ${payment.transactionReference} marked as $newStatus.")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to update payment status.")
            }
        }
    }

    fun adminSavePricingPlan(
        tier: String,
        name: String,
        priceNgn: Long,
        storageQuotaMb: Long,
        featuresSummary: String
    ) {
        if (!_isAdminAuthenticated.value || _adminMustChangePassword.value) {
            showMessage("Super Administrator authentication required.")
            return
        }
        viewModelScope.launch {
            val res = repository.adminSavePricingPlan(
                tier = tier,
                name = name,
                priceNgn = priceNgn,
                storageQuotaMb = storageQuotaMb,
                featuresSummary = featuresSummary
            )
            res.onSuccess {
                showMessage("Updated $name ($tier) pricing plan.")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to update pricing plan.")
            }
        }
    }

    fun logoutSuperAdminSession() {
        adminObserverJobs.forEach { it.cancel() }
        adminObserverJobs.clear()
        _isAdminAuthenticated.value = false
        _adminMustChangePassword.value = false
        _activeSection.value = WorkspaceSection.HOME
        showMessage("Exited Super Administrator Console.")
    }

    // --- GEMINI AI OFFICE ASSISTANT (FIREBASE AI LOGIC) ---
    fun runAiOfficeCommand(
        taskPrompt: String,
        documentContext: String = "",
        targetContext: String = "GENERAL"
    ) {
        if (taskPrompt.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiAssistantState.Loading
            val fullPrompt = buildString {
                appendLine("You are the Amazing Office AI Assistant built by Amazing Digitals.")
                appendLine("Provide clear, directly usable, professional office output without conversational filler.")
                if (documentContext.isNotBlank()) {
                    appendLine("\n--- CURRENT DOCUMENT / WORKSPACE CONTEXT ---")
                    appendLine(documentContext.take(12_000))
                    appendLine("--- END CONTEXT ---\n")
                }
                appendLine("USER REQUEST: $taskPrompt")
            }

            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    geminiModel.generateContentStream(fullPrompt).collect { chunk ->
                        val textChunk = chunk.text.orEmpty()
                        accumulated.append(textChunk)
                        _aiState.value = AiAssistantState.Streaming(
                            partialText = accumulated.toString(),
                            targetContext = targetContext
                        )
                    }
                }
                val finalOutput = accumulated.toString().trim()
                if (finalOutput.isNotBlank()) {
                    _aiState.value = AiAssistantState.Success(
                        generatedText = finalOutput,
                        targetContext = targetContext
                    )
                } else {
                    _aiState.value = AiAssistantState.Error("Gemini returned an empty response. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                _aiState.value = AiAssistantState.Error("AI request timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                // ALWAYS rethrow CancellationException to preserve coroutine lifecycle
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI Logic generation failed", e)
                _aiState.value = AiAssistantState.Error(
                    e.localizedMessage ?: "AI generation failed."
                )
            }
        }
    }

    /**
     * Generates a structured multi-slide presentation deck from a user prompt and previews it before applying.
     */
    fun generateAiPresentationSlides(topic: String, slideCount: Int = 5, onDeckGenerated: (PresentationDeck) -> Unit) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiAssistantState.Loading
            val prompt = """
                Create a professional $slideCount-slide presentation outline about: "$topic".
                Format each slide strictly using "---SLIDE---" as a separator, with lines:
                TITLE: <Slide Title>
                SUBTITLE: <Slide Subtitle>
                BULLET: <Key Point 1>
                BULLET: <Key Point 2>
                BULLET: <Key Point 3>
                NOTES: <Speaker notes>
            """.trimIndent()
            try {
                val response = withTimeout(60_000L) {
                    geminiModel.generateContent(prompt)
                }
                val raw = response.text.orEmpty().trim()
                if (raw.isNotBlank()) {
                    val blocks = raw.split("---SLIDE---").map { it.trim() }.filter { it.isNotBlank() }
                    val parsedSlides = blocks.mapIndexed { idx, block ->
                        val lines = block.lines().map { it.trim() }
                        val title = lines.firstOrNull { it.startsWith("TITLE:", ignoreCase = true) }
                            ?.substringAfter(":")?.trim() ?: "Slide ${idx + 1}"
                        val subtitle = lines.firstOrNull { it.startsWith("SUBTITLE:", ignoreCase = true) }
                            ?.substringAfter(":")?.trim() ?: ""
                        val bullets = lines.filter { it.startsWith("BULLET:", ignoreCase = true) }
                            .map { it.substringAfter(":").trim() }
                            .ifEmpty { listOf("Key insight on $topic") }
                        val notes = lines.firstOrNull { it.startsWith("NOTES:", ignoreCase = true) }
                            ?.substringAfter(":")?.trim() ?: ""
                        SlideItem(
                            id = "ai_s_${idx + 1}",
                            title = title,
                            subtitle = subtitle,
                            bullets = bullets,
                            speakerNotes = notes
                        )
                    }
                    if (parsedSlides.isNotEmpty()) {
                        onDeckGenerated(PresentationDeck(themeId = "NAVY_GOLD", slides = parsedSlides))
                    }
                    _aiState.value = AiAssistantState.Success(raw, "PRESENTATION")
                } else {
                    _aiState.value = AiAssistantState.Error("Model returned an empty presentation response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI slide generation timed out", e)
                _aiState.value = AiAssistantState.Error("Slide generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI slide generation failed", e)
                _aiState.value = AiAssistantState.Error(e.localizedMessage ?: "Failed to generate presentation.")
            }
        }
    }
}
