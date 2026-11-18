package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.SubscriptionDefaults
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

data class PendingRegistrationData(
    val fullName: String = "",
    val username: String = "",
    val phone: String = ""
)

// 1. Silent Auto-Sign-In on Startup (Strictly ONE option: GetGoogleIdOption)
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

// 2. Interactive Google Sign-In (Strictly ONE option: GetSignInWithGoogleOption)
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activity = context as? Activity
            if (activity == null) {
                onAuthError("Activity context required for Google Sign-In")
                return@launch
            }
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

// 3. Safe Sign-Out Flow inside NonCancellable
fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingAndAuthScreen(
    onAuthenticated: (PendingRegistrationData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var showAuthPanel by rememberSaveable { mutableStateOf(false) }
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    // Registration profile fields saved upon Google Sign-In completion
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var acceptTerms by rememberSaveable { mutableStateOf(true) }

    // Attempt silent auto-sign-in once on launch
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                onAuthenticated(PendingRegistrationData(fullName, username, phone))
            },
            onUnauthenticated = {},
            scope = scope
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // --- TOP BRAND BAR ---
        item {
            Surface(
                color = NavyPrimary,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(RoyalBlue, GoldAccent))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AMAZING OFFICE AI",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "by Amazing Digitals",
                                color = GoldLight,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isRegisterMode = false
                            showAuthPanel = !showAuthPanel
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("header_login_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = "Login",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (showAuthPanel) "Back to Home" else "Login / Sign Up")
                    }
                }
            }
        }

        // --- AUTHENTICATION CARD (LOGIN & SIGN-UP WITH GOOGLE CREDENTIAL MANAGER) ---
        item {
            AnimatedVisibility(visible = showAuthPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRegisterMode) "Create Your Amazing Office Account" else "Sign In to Amazing Office AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                                Text(if (isRegisterMode) "Have an account? Login" else "New user? Sign Up")
                            }
                        }

                        Text(
                            text = "Secure cloud authentication powered by Google Identity & Firebase Security.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_fullname_input")
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Username") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("signup_username_input")
                                )
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("Phone Number") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("signup_phone_input")
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { acceptTerms = !acceptTerms }
                            ) {
                                Checkbox(
                                    checked = acceptTerms,
                                    onCheckedChange = { acceptTerms = it }
                                )
                                Text(
                                    text = "I accept the Amazing Digitals Terms of Service & Privacy Policy.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        if (authError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = authError.orEmpty(),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isRegisterMode && !acceptTerms) {
                                    authError = "Please accept the Terms & Conditions to register."
                                    return@Button
                                }
                                authError = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthenticated(
                                            PendingRegistrationData(
                                                fullName = fullName,
                                                username = username,
                                                phone = phone
                                            )
                                        )
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        authError = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Authenticating with Google...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Google Sign In",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (isRegisterMode) "Sign Up & Continue with Google" else "Sign in with Google",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Super Administrator (amazingdigitals): Sign in with Google first, then open the Super Admin Portal from the navigation menu.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 1. HERO SECTION ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(NavyDark, NavyPrimary, Color(0xFF173A6A))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.border(1.dp, GoldLight, CircleShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Powered",
                                tint = GoldLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "CREATE • EDIT • PRESENT • CALCULATE • CONVERT • PRINT",
                                color = GoldLight,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "YOUR COMPLETE OFFICE.\nPOWERED BY AI.",
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Create professional documents, build presentations, manage spreadsheets, edit PDFs, enhance pictures and convert files, all in one powerful workspace.",
                        color = Color(0xFFE2E8F0),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                isRegisterMode = true
                                showAuthPanel = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("hero_get_started_button")
                        ) {
                            Text("Get Started", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }

                        Button(
                            onClick = {
                                isRegisterMode = false
                                showAuthPanel = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("hero_login_button")
                        ) {
                            Text("Login", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showAuthPanel = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Explore Features")
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Hero Banner Preview Image
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, GoldLight.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_office_hero_1794979674208),
                            contentDescription = "Amazing Office AI Suite Workspace Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    }
                }
            }
        }

        // --- 2. OFFICE APPLICATIONS & 3-9. FEATURE HIGHLIGHTS ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "INTEGRATED PRODUCTIVITY SUITE",
                    color = RoyalBlue,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Everything You Need in One Unified Workspace",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(16.dp))

                val suiteCards = listOf(
                    SuitePillar(
                        title = "Amazing Word",
                        badge = "Word Processor",
                        icon = Icons.Default.Description,
                        accent = Color(0xFF1D4ED8),
                        features = "Full HOME/INSERT/LAYOUT/REVIEW/VIEW ribbon, DOCX import & export, pagination, tables, headers/footers, find & replace, PDF export & native printing."
                    ),
                    SuitePillar(
                        title = "Amazing Sheets",
                        badge = "Spreadsheet Engine",
                        icon = Icons.Default.TableChart,
                        accent = Color(0xFF059669),
                        features = "Multi-sheet tabs, live formula evaluator (SUM, AVERAGE, IF, IFS, VLOOKUP, COUNTIF, SUMIF, ROUND, CONCAT), Bar/Line/Pie/Doughnut charts & CSV/PDF export."
                    ),
                    SuitePillar(
                        title = "Amazing Slides",
                        badge = "Presentation Studio",
                        icon = Icons.Default.Slideshow,
                        accent = Color(0xFFD97706),
                        features = "Slide thumbnail strip, executive & church templates, speaker notes, full-screen presentation mode, AI slide deck generator & PDF/Slide export."
                    ),
                    SuitePillar(
                        title = "Amazing PDF Studio",
                        badge = "PDF & OCR Engine",
                        icon = Icons.Default.PictureAsPdf,
                        accent = Color(0xFFDC2626),
                        features = "Native PDF viewing, text highlights, annotations, watermarks, page numbering, rotation, merge/split tools, Word-to-PDF and PDF-to-Word conversion."
                    ),
                    SuitePillar(
                        title = "Amazing Image Editor",
                        badge = "Creative Studio",
                        icon = Icons.Default.Image,
                        accent = Color(0xFF7C3AED),
                        features = "Crop presets (Passport, Social Media, Poster, Flyer), brightness/contrast/saturation sliders, filters, text & watermark overlays, undo/redo & PNG/JPG export."
                    ),
                    SuitePillar(
                        title = "Gemini AI Office Assistant",
                        badge = "Powered by Gemini",
                        icon = Icons.Default.AutoAwesome,
                        accent = Color(0xFFB45309),
                        features = "Draft proposals, rewrite paragraphs, fix grammar, generate spreadsheet formulas, create slide outlines, and summarize PDFs with review-before-apply safety."
                    ),
                    SuitePillar(
                        title = "Universal File Converter",
                        badge = "DOCX ⇄ PDF",
                        icon = Icons.Default.SwapHoriz,
                        accent = Color(0xFF0284C7),
                        features = "Dedicated Word-to-PDF and PDF-to-Word conversion studio with OCR structure extraction, layout preservation, preview, download, and printing."
                    )
                )

                suiteCards.forEach { item ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(item.accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.accent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = item.accent.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = item.badge,
                                            color = item.accent,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.features,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 10. PRICING PLANS SECTION ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "FLEXIBLE SUBSCRIPTION PLANS",
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Transparent Pricing for Individuals, Schools & Enterprises",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(14.dp))

                SubscriptionDefaults.defaultPlans.forEach { plan ->
                    val isPopular = plan.tier == "PRO"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .then(
                                if (isPopular) Modifier.border(2.dp, GoldAccent, RoundedCornerShape(16.dp))
                                else Modifier
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = plan.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${plan.storageQuotaMb} MB Cloud Storage",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (plan.priceNgn == 0L) "FREE" else "₦${String.format("%,d", plan.priceNgn)}/mo",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isPopular) GoldAccent else RoyalBlue
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            plan.featuresSummary.split("•").map { it.trim() }.filter { it.isNotBlank() }.forEach { feat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = feat,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    isRegisterMode = true
                                    showAuthPanel = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPopular) GoldAccent else RoyalBlue
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (plan.priceNgn == 0L) "Start Free" else "Subscribe to ${plan.name}")
                            }
                        }
                    }
                }
            }
        }

        // --- 11. FREQUENTLY ASKED QUESTIONS ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "FREQUENTLY ASKED QUESTIONS",
                    color = RoyalBlue,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                val faqs = listOf(
                    "Can I import and export real Word (.docx) and PDF files?" to
                        "Yes. Amazing Word imports and exports OpenXML (.docx) documents, exports multi-page PDFs, and prints directly through the system print service.",
                    "How does the Gemini AI Office Assistant protect my documents?" to
                        "All AI commands generate a preview first. The assistant never overwrites your document without your explicit review and confirmation.",
                    "How do I upgrade to Basic, Pro, or Premium?" to
                        "Transfer the subscription amount to our official OPay account (Emmanuel Ajibade — 9080301021) and submit your transaction reference and receipt in the Subscription tab. Once verified by the Super Administrator, your plan activates immediately."
                )

                faqs.forEach { (q, a) ->
                    var expanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { expanded = !expanded },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = q,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle answer"
                                )
                            }
                            AnimatedVisibility(visible = expanded) {
                                Text(
                                    text = a,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 12. FOOTER ---
        item {
            Surface(
                color = NavyDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AMAZING OFFICE AI SUITE",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Everything You Need to Create, Edit, Calculate, Present and Convert.",
                        color = GoldLight,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    )
                    Text(
                        text = "© 2026 Amazing Digitals. All Rights Reserved.",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

private data class SuitePillar(
    val title: String,
    val badge: String,
    val icon: ImageVector,
    val accent: Color,
    val features: String
)
