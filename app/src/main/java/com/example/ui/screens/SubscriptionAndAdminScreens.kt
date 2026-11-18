package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.OfficeFile
import com.example.data.PaymentRequest
import com.example.data.PricingPlan
import com.example.data.UserProfile
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.AiAssistantState
import com.example.viewmodel.OfficeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 17. SUBSCRIPTION & MANUAL OPAY BANK TRANSFER PAYMENT SCREEN
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionAndPaymentScreen(
    viewModel: OfficeViewModel,
    profile: UserProfile?,
    plans: List<PricingPlan>,
    myPayments: List<PaymentRequest>
) {
    val context = LocalContext.current
    val currentTier = profile?.subscriptionTier ?: "FREE"

    var selectedPlanTier by rememberSaveable { mutableStateOf("PRO") }
    val matchingPlan = plans.firstOrNull { it.tier == selectedPlanTier }
    var amountTransferred by rememberSaveable(selectedPlanTier) {
        mutableStateOf((matchingPlan?.priceNgn ?: 5000L).toString())
    }
    var customerName by rememberSaveable(profile?.fullName) {
        mutableStateOf(profile?.fullName?.ifBlank { viewModel.currentUserDisplayName }.orEmpty())
    }
    var customerEmail by rememberSaveable(profile?.email) {
        mutableStateOf(profile?.email?.ifBlank { viewModel.currentUserEmail }.orEmpty())
    }
    var phoneNumber by rememberSaveable(profile?.phone) {
        mutableStateOf(profile?.phone.orEmpty())
    }
    var transactionReference by rememberSaveable { mutableStateOf("") }
    var paymentDate by rememberSaveable {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var receiptSummary by rememberSaveable {
        mutableStateOf("OPay Transfer Receipt to 9080301021 (Emmanuel Ajibade)")
    }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            receiptSummary = "Attached Receipt Image (${uri.lastPathSegment ?: "screenshot"}) • Transferred to OPay 9080301021"
            viewModel.showMessage("Attached transfer receipt screenshot.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- ACTIVE SUBSCRIPTION HEADER ---
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(NavyDark, NavyPrimary, RoyalBlue)))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "CURRENT SUBSCRIPTION TIER: $currentTier",
                            color = GoldLight,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Upgrade Your Amazing Office AI Workspace",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Unlock full Word, Spreadsheet, Presentation, PDF OCR, Image Studio, and Gemini AI capabilities.",
                            color = Color(0xFFE2E8F0),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // --- PRICING PLANS ---
        item {
            Text(
                text = "1. Select Your Subscription Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                plans.forEach { plan ->
                    val isSelected = selectedPlanTier == plan.tier
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) Modifier.border(2.dp, GoldAccent, RoundedCornerShape(16.dp))
                                else Modifier
                            ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${plan.storageQuotaMb} MB Cloud Storage",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (plan.priceNgn == 0L) "FREE" else "₦${String.format(Locale.US, "%,d", plan.priceNgn)}/mo",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) GoldAccent else RoyalBlue
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = plan.featuresSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (plan.tier != "FREE") {
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        selectedPlanTier = plan.tier
                                        amountTransferred = plan.priceNgn.toString()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) GoldAccent else RoyalBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isSelected) "Selected: ${plan.name}" else "Choose ${plan.name}")
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- OFFICIAL MANUAL BANK TRANSFER DETAILS ---
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, GoldAccent, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "2. Official Manual Bank Transfer Instructions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Surface(
                        color = NavyPrimary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Account Name: Emmanuel Ajibade",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bank: OPay",
                                color = GoldLight,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Account Number: 9080301021",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("OPay Account Number", "9080301021"))
                                        viewModel.showMessage("Copied OPay account number: 9080301021")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                    modifier = Modifier.testTag("copy_opay_account_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy")
                                }
                            }
                            Text(
                                text = "Currency: Nigerian Naira (NGN) • Selected Plan: $selectedPlanTier (₦$amountTransferred)",
                                color = Color(0xFFCBD5E1),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Text(
                        text = "Security Policy: Submitting a receipt creates a PENDING verification request. Only the authenticated Super Administrator can verify bank credit in OPay and activate paid subscriptions.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- SUBMIT PAYMENT RECEIPT FORM ---
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RoyalBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "3. Submit Payment Reference & Receipt",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerEmail,
                        onValueChange = { customerEmail = it },
                        label = { Text("Registered Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = amountTransferred,
                            onValueChange = { amountTransferred = it },
                            label = { Text("Amount (NGN)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = transactionReference,
                            onValueChange = { transactionReference = it },
                            label = { Text("OPay Transaction Reference") },
                            placeholder = { Text("e.g. OPAY-20261117-9081") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("payment_reference_input")
                        )
                        OutlinedTextField(
                            value = paymentDate,
                            onValueChange = { paymentDate = it },
                            label = { Text("Payment Date") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = receiptSummary,
                        onValueChange = { receiptSummary = it },
                        label = { Text("Receipt Details / Transfer Note") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                receiptPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Attach Receipt Screenshot")
                        }

                        Button(
                            onClick = {
                                val parsedAmount = amountTransferred.replace(",", "").toLongOrNull() ?: 5000L
                                viewModel.submitManualBankTransferPayment(
                                    customerName = customerName,
                                    customerEmail = customerEmail,
                                    phoneNumber = phoneNumber,
                                    selectedPlan = selectedPlanTier,
                                    amountTransferred = parsedAmount,
                                    transactionReference = transactionReference,
                                    paymentDate = paymentDate,
                                    receiptSummary = receiptSummary,
                                    onSubmitted = {
                                        transactionReference = ""
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("submit_payment_request_button")
                        ) {
                            Text("Submit for Verification", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- MY PAYMENT VERIFICATION STATUS HISTORY ---
        item {
            Text(
                text = "Your Payment Verification Requests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (myPayments.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No payment requests submitted yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(myPayments, key = { it.id }) { req ->
                val badgeColor = when (req.status) {
                    "VERIFIED" -> SuccessGreen
                    "REJECTED" -> DangerRed
                    "UNDER_REVIEW" -> RoyalBlue
                    else -> WarningAmber
                }
                ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${req.selectedPlan} Plan • ₦${String.format(Locale.US, "%,d", req.amountTransferred)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = badgeColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = req.status,
                                    color = badgeColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ref: ${req.transactionReference} • Date: ${req.paymentDate}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (req.verificationNotes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Admin Verification Note: ${req.verificationNotes}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 18. SUPER ADMINISTRATOR DASHBOARD & PAYMENT VERIFICATION PANEL
// ============================================================================

@Composable
fun SuperAdminDashboardScreen(
    viewModel: OfficeViewModel,
    plans: List<PricingPlan>
) {
    val isAdminAuthenticated by viewModel.isAdminAuthenticated.collectAsState()
    val mustChangePassword by viewModel.adminMustChangePassword.collectAsState()
    val adminPayments by viewModel.adminPayments.collectAsState()
    val adminUsers by viewModel.adminUsers.collectAsState()
    val adminAuditLogs by viewModel.adminAuditLogs.collectAsState()

    var adminUsernameInput by rememberSaveable { mutableStateOf("amazingdigitals") }
    var adminPasswordInput by rememberSaveable { mutableStateOf("") }
    var newAdminPassword by rememberSaveable { mutableStateOf("") }
    var confirmAdminPassword by rememberSaveable { mutableStateOf("") }

    var selectedAdminTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingConfirmPayment by remember { mutableStateOf<Pair<PaymentRequest, String>?>(null) }
    var verificationNoteInput by remember { mutableStateOf("") }

    // 1. SUPER ADMIN LOGIN GATE
    if (!isAdminAuthenticated) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Super Administrator Portal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Amazing Digitals • Privileged Verification Console",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Only the authenticated Super Administrator (amazingdigitals) is authorized to verify customer OPay transfers and activate subscriptions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = adminUsernameInput,
                        onValueChange = { adminUsernameInput = it },
                        label = { Text("Administrator Username") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_username_input")
                    )

                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        label = { Text("Administrator Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input")
                    )

                    Button(
                        onClick = {
                            viewModel.authenticateSuperAdmin(adminUsernameInput, adminPasswordInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_login_submit_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Authenticate Super Administrator", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    // 2. MANDATORY PASSWORD ROTATION ON FIRST LOGIN
    if (mustChangePassword) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GppGood, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Mandatory First-Login Password Change",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "For security compliance, you must replace the initial password (Mercy2026) with a new administrator password before accessing payment verification controls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newAdminPassword,
                        onValueChange = { newAdminPassword = it },
                        label = { Text("New Password (min 8 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_new_password_input")
                    )
                    OutlinedTextField(
                        value = confirmAdminPassword,
                        onValueChange = { confirmAdminPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_confirm_password_input")
                    )
                    Button(
                        onClick = {
                            viewModel.completeMandatoryAdminPasswordChange(newAdminPassword, confirmAdminPassword)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_rotate_password_button")
                    ) {
                        Text("Save New Password & Unlock Console", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    // Explicit Confirmation Dialog before Approving or Rejecting a Payment
    if (pendingConfirmPayment != null) {
        val (paymentReq, targetStatus) = pendingConfirmPayment!!
        val isApproval = targetStatus == "VERIFIED"
        AlertDialog(
            onDismissRequest = { pendingConfirmPayment = null },
            title = {
                Text(
                    text = if (isApproval) "Confirm OPay Credit & Approve Payment" else "Reject Payment Request",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isApproval) {
                            "IMPORTANT: Have you independently confirmed that ₦${String.format(Locale.US, "%,d", paymentReq.amountTransferred)} (Ref: ${paymentReq.transactionReference}) was credited to OPay Account 9080301021 (Emmanuel Ajibade)?"
                        } else {
                            "Enter the rejection reason to notify ${paymentReq.customerName} (${paymentReq.customerEmail}):"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = verificationNoteInput,
                        onValueChange = { verificationNoteInput = it },
                        label = { Text(if (isApproval) "Verification Note" else "Rejection Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminVerifyOrRejectPayment(
                            payment = paymentReq,
                            newStatus = targetStatus,
                            verificationNotes = verificationNoteInput.ifBlank {
                                if (isApproval) "Verified credit in OPay 9080301021 by amazingdigitals"
                                else "Payment reference could not be verified in OPay account"
                            }
                        )
                        verificationNoteInput = ""
                        pendingConfirmPayment = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApproval) SuccessGreen else DangerRed
                    )
                ) {
                    Text(if (isApproval) "Confirm & Activate ${paymentReq.selectedPlan}" else "Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmPayment = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val pendingCount = adminPayments.count { it.status == "PENDING" || it.status == "UNDER_REVIEW" }
    val approvedCount = adminPayments.count { it.status == "VERIFIED" }
    val rejectedCount = adminPayments.count { it.status == "REJECTED" }
    val totalVerifiedRevenue = adminPayments.filter { it.status == "VERIFIED" }.sumOf { it.amountTransferred }
    val paidSubscribers = adminUsers.count { it.subscriptionTier != "FREE" }
    val freeSubscribers = adminUsers.count { it.subscriptionTier == "FREE" }

    Column(modifier = Modifier.fillMaxSize()) {
        // Admin Header
        Surface(color = NavyDark, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldLight)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SUPER ADMIN CONSOLE • amazingdigitals",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Verified Revenue: ₦${String.format(Locale.US, "%,d", totalVerifiedRevenue)} • Pending Queue: $pendingCount",
                            color = GoldLight,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.logoutSuperAdminSession() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lock")
                }
            }
        }

        val adminTabs = listOf(
            "Payment Verification ($pendingCount)",
            "Analytics & Users (${adminUsers.size})",
            "Pricing Plans",
            "Audit Logs (${adminAuditLogs.size})"
        )

        ScrollableTabRow(
            selectedTabIndex = selectedAdminTab,
            containerColor = NavyPrimary,
            contentColor = Color.White,
            edgePadding = 8.dp
        ) {
            adminTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedAdminTab == idx,
                    onClick = { selectedAdminTab = idx },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedAdminTab == idx) GoldLight else Color.White,
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
            when (selectedAdminTab) {
                0 -> {
                    // --- ADMIN PAYMENT VERIFICATION PANEL ---
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.14f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = GoldAccent)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Mandatory Rule: A screenshot is not proof of payment. Confirm actual credit in OPay 9080301021 (Emmanuel Ajibade) before approving.",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (adminPayments.isEmpty()) {
                        item {
                            Text(
                                text = "No customer payment verification requests in queue.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(adminPayments, key = { it.id }) { req ->
                            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${req.customerName} (${req.customerEmail})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            color = when (req.status) {
                                                "VERIFIED" -> SuccessGreen.copy(alpha = 0.15f)
                                                "REJECTED" -> DangerRed.copy(alpha = 0.15f)
                                                else -> WarningAmber.copy(alpha = 0.15f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = req.status,
                                                color = when (req.status) {
                                                    "VERIFIED" -> SuccessGreen
                                                    "REJECTED" -> DangerRed
                                                    else -> WarningAmber
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Plan: ${req.selectedPlan} • Amount: ₦${String.format(Locale.US, "%,d", req.amountTransferred)} • Phone: ${req.phoneNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Transaction Ref: ${req.transactionReference} • Submitted: ${req.paymentDate}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = RoyalBlue
                                    )
                                    Text(
                                        text = "Receipt Details: ${req.receiptSummary}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (req.status == "PENDING" || req.status == "UNDER_REVIEW") {
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    verificationNoteInput = "Confirmed NGN ${req.amountTransferred} credit in OPay 9080301021"
                                                    pendingConfirmPayment = req to "VERIFIED"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Approve")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.adminVerifyOrRejectPayment(
                                                        payment = req,
                                                        newStatus = "UNDER_REVIEW",
                                                        verificationNotes = "Under review by Super Administrator"
                                                    )
                                                }
                                            ) {
                                                Text("Review")
                                            }
                                            Button(
                                                onClick = {
                                                    verificationNoteInput = ""
                                                    pendingConfirmPayment = req to "REJECTED"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Reject")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // --- PLATFORM ANALYTICS & REGISTERED USERS ---
                    item {
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Executive Platform Statistics",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    AdminMetricItem("Total Users", adminUsers.size.toString())
                                    AdminMetricItem("Paid Users", paidSubscribers.toString())
                                    AdminMetricItem("Free Users", freeSubscribers.toString())
                                }
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    AdminMetricItem("Pending Pay", pendingCount.toString())
                                    AdminMetricItem("Verified Pay", approvedCount.toString())
                                    AdminMetricItem("Rejected Pay", rejectedCount.toString())
                                }
                            }
                        }
                    }

                    items(adminUsers, key = { it.id }) { u ->
                        ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(u.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${u.email} • @${u.username}", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "Storage: ${(u.storageUsedBytes / 1024)} KB / ${(u.storageQuotaBytes / (1024 * 1024))} MB",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = RoyalBlue.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = u.subscriptionTier,
                                        color = RoyalBlue,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // --- PRICING PLANS MANAGEMENT ---
                    items(plans, key = { it.tier }) { plan ->
                        var priceInput by rememberSaveable(plan.tier, plan.priceNgn) {
                            mutableStateOf(plan.priceNgn.toString())
                        }
                        var quotaInput by rememberSaveable(plan.tier, plan.storageQuotaMb) {
                            mutableStateOf(plan.storageQuotaMb.toString())
                        }
                        var featuresInput by rememberSaveable(plan.tier, plan.featuresSummary) {
                            mutableStateOf(plan.featuresSummary)
                        }

                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${plan.name} (${plan.tier})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = priceInput,
                                        onValueChange = { priceInput = it },
                                        label = { Text("Price (NGN/mo)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = quotaInput,
                                        onValueChange = { quotaInput = it },
                                        label = { Text("Storage Quota (MB)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = featuresInput,
                                    onValueChange = { featuresInput = it },
                                    label = { Text("Features Summary") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        viewModel.adminSavePricingPlan(
                                            tier = plan.tier,
                                            name = plan.name,
                                            priceNgn = priceInput.toLongOrNull() ?: plan.priceNgn,
                                            storageQuotaMb = quotaInput.toLongOrNull() ?: plan.storageQuotaMb,
                                            featuresSummary = featuresInput
                                        )
                                    }
                                ) {
                                    Text("Update ${plan.tier} Plan")
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // --- IMMUTABLE SECURITY AUDIT LOGS ---
                    if (adminAuditLogs.isEmpty()) {
                        item {
                            Text(
                                text = "No administrator audit logs recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(adminAuditLogs, key = { it.id }) { log ->
                            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = log.actionType,
                                            color = RoyalBlue,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Actor: ${log.actorName}",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(log.details, style = MaterialTheme.typography.bodyMedium)
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
private fun AdminMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = RoyalBlue
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// 14. FLOATING GEMINI AI OFFICE ASSISTANT PANEL
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiOfficeAssistantPanel(
    viewModel: OfficeViewModel,
    activeFile: OfficeFile?,
    onApplyGeneratedText: (String) -> Unit
) {
    val aiState by viewModel.aiState.collectAsState()
    var customPrompt by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Amazing Office Gemini AI Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Review all AI suggestions before applying to your document",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { viewModel.setAiSheetOpen(false) }) {
                Icon(Icons.Default.Close, contentDescription = "Close AI Assistant")
            }
        }

        // Quick AI Office Commands
        val quickPrompts = listOf(
            "Write a professional business proposal",
            "Summarize this document",
            "Make this paragraph more executive & formal",
            "Create a 6-slide presentation outline",
            "Generate a spreadsheet formula for profit margin",
            "Draft an official corporate letter"
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPrompts.forEach { preset ->
                AssistChip(
                    onClick = {
                        customPrompt = preset
                        viewModel.runAiOfficeCommand(
                            taskPrompt = preset,
                            documentContext = activeFile?.content.orEmpty(),
                            targetContext = activeFile?.fileType ?: "WORD"
                        )
                    },
                    label = { Text(preset) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                placeholder = { Text("Ask Gemini AI to draft, rewrite, calculate, or summarize...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input")
            )
            Button(
                onClick = {
                    viewModel.runAiOfficeCommand(
                        taskPrompt = customPrompt,
                        documentContext = activeFile?.content.orEmpty(),
                        targetContext = activeFile?.fileType ?: "WORD"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                modifier = Modifier.testTag("ai_send_button")
            ) {
                Text("Run AI")
            }
        }

        when (val state = aiState) {
            is AiAssistantState.Idle -> {
                Text(
                    text = "Select a quick command above or enter a custom instruction.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is AiAssistantState.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text("Gemini AI is generating your content...")
                }
            }
            is AiAssistantState.Streaming -> {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Streaming Preview:",
                            style = MaterialTheme.typography.labelLarge,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = state.partialText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            is AiAssistantState.Success -> {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "AI Generated Output (Review Before Applying):",
                            style = MaterialTheme.typography.labelLarge,
                            color = SuccessGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                        OutlinedTextField(
                            value = state.generatedText,
                            onValueChange = {},
                            readOnly = true,
                            maxLines = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onApplyGeneratedText(state.generatedText)
                                    viewModel.setAiSheetOpen(false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Insert / Apply to Workspace")
                            }
                            OutlinedButton(
                                onClick = { viewModel.resetAiState() }
                            ) {
                                Text("Discard")
                            }
                        }
                    }
                }
            }
            is AiAssistantState.Error -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
