package com.example.data

import android.content.Context
import com.example.R
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale

class OfficeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- 1. USER PROFILE ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureOrCreateUserProfile(
        fullName: String,
        username: String,
        email: String,
        phone: String
    ): Result<UserProfile> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docPath = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val parsed = existing.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (parsed != null) return Result.success(parsed)
            }
            val safeName = fullName.trim().ifBlank { "Amazing Office User" }.take(120)
            val safeUsername = username.trim().ifBlank {
                email.substringBefore("@").ifBlank { "user_${uid.take(6)}" }
            }.take(60)
            val safeEmail = email.trim().ifBlank { "user@amazingdigitals.com" }.take(150)
            val safePhone = phone.trim().take(40)

            val payload = mapOf(
                "userId" to uid,
                "fullName" to safeName,
                "username" to safeUsername,
                "email" to safeEmail,
                "phone" to safePhone,
                "subscriptionTier" to "FREE",
                "storageUsedBytes" to 0L,
                "storageQuotaBytes" to SubscriptionDefaults.quotaBytesForTier("FREE"),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                UserProfile(
                    id = uid,
                    userId = uid,
                    fullName = safeName,
                    username = safeUsername,
                    email = safeEmail,
                    phone = safePhone,
                    subscriptionTier = "FREE",
                    storageUsedBytes = 0L,
                    storageQuotaBytes = SubscriptionDefaults.quotaBytesForTier("FREE")
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileDetails(
        fullName: String,
        username: String,
        phone: String,
        storageUsedBytes: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "fullName" to fullName.trim().ifBlank { "Office User" }.take(120),
                    "username" to username.trim().ifBlank { "office_user" }.take(60),
                    "phone" to phone.trim().take(40),
                    "storageUsedBytes" to storageUsedBytes.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- 2. OFFICE FILES (WORD, SHEETS, SLIDES, PDF, IMAGE, FOLDER) ---
    fun observeUserFiles(userId: String): Flow<List<OfficeFile>> = flow {
        val path = "users/$userId/files"
        emitAll(
            db.collection("users").document(userId).collection("files")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(OfficeFile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createOfficeFile(
        title: String,
        fileType: String,
        content: String,
        folderId: String = "",
        pageSettings: String = "A4|PORTRAIT|NORMAL|Amazing Digitals|Confidential|true"
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/files"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().ifBlank { "Untitled $fileType" }.take(200)
            val safeType = fileType.uppercase(Locale.US).let {
                if (it in listOf("WORD", "SPREADSHEET", "PRESENTATION", "PDF", "IMAGE", "FOLDER")) it else "WORD"
            }
            val safeContent = content.take(490_000)
            val safeSettings = pageSettings.take(4_900)
            val computedBytes = (safeContent.toByteArray().size.toLong() + 1024L).coerceIn(0L, 104_857_600L)

            val docRef = db.collection("users").document(uid).collection("files").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "fileType" to safeType,
                "folderId" to folderId.take(128),
                "content" to safeContent,
                "pageSettings" to safeSettings,
                "sizeBytes" to computedBytes,
                "trashed" to false,
                "sharedWithEmails" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOfficeFile(
        fileId: String,
        title: String,
        content: String,
        pageSettings: String,
        folderId: String = "",
        trashed: Boolean = false,
        sharedWithEmails: String = ""
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/files/$fileId"
        return try {
            val uid = requireUserId()
            require(fileId.isNotBlank()) { "File ID must not be blank" }
            val safeTitle = title.trim().ifBlank { "Untitled Document" }.take(200)
            val safeContent = content.take(490_000)
            val safeSettings = pageSettings.take(4_900)
            val computedBytes = (safeContent.toByteArray().size.toLong() + 1024L).coerceIn(0L, 104_857_600L)

            val docRef = db.collection("users").document(uid).collection("files").document(fileId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "title" to safeTitle,
                    "folderId" to folderId.take(128),
                    "content" to safeContent,
                    "pageSettings" to safeSettings,
                    "sizeBytes" to computedBytes,
                    "trashed" to trashed,
                    "sharedWithEmails" to sharedWithEmails.take(2000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteOfficeFilePermanently(fileId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/files/$fileId"
        return try {
            val uid = requireUserId()
            require(fileId.isNotBlank()) { "File ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("files").document(fileId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- 3. MANUAL OPAY PAYMENT REQUESTS ---
    fun observeMyPaymentRequests(userId: String): Flow<List<PaymentRequest>> = flow {
        val path = "payment_requests"
        emitAll(
            db.collection("payment_requests")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitManualPaymentRequest(
        customerName: String,
        customerEmail: String,
        phoneNumber: String,
        selectedPlan: String,
        amountTransferred: Long,
        transactionReference: String,
        paymentDate: String,
        receiptSummary: String
    ): Result<String> {
        var docPath = "payment_requests"
        return try {
            val uid = requireUserId()
            val safePlan = selectedPlan.uppercase(Locale.US).let {
                if (it in listOf("BASIC", "PRO", "PREMIUM")) it else "PRO"
            }
            val safeRef = transactionReference.trim()
            require(safeRef.length >= 3) { "Transaction reference must be at least 3 characters." }

            val docRef = db.collection("payment_requests").document()
            docPath = docRef.path
            // CRITICAL: Status is strictly set to PENDING and verifiedByAdminId is blank.
            val payload = mapOf(
                "userId" to uid,
                "customerName" to customerName.trim().ifBlank { "Amazing Office Customer" }.take(120),
                "customerEmail" to customerEmail.trim().ifBlank { "customer@example.com" }.take(150),
                "phoneNumber" to phoneNumber.trim().ifBlank { "N/A" }.take(40),
                "selectedPlan" to safePlan,
                "amountTransferred" to amountTransferred.coerceIn(500L, 100_000_000L),
                "transactionReference" to safeRef.take(120),
                "paymentDate" to paymentDate.trim().ifBlank { "Today" }.take(40),
                "receiptSummary" to receiptSummary.trim().ifBlank { "Manual OPay transfer receipt attached" }.take(2000),
                "status" to "PENDING",
                "verifiedByAdminId" to "",
                "verificationNotes" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- 4. PRICING PLANS ---
    fun observePricingPlans(): Flow<List<PricingPlan>> = flow {
        val uid = requireUserId()
        val path = "pricing_plans"
        emitAll(
            db.collection("pricing_plans")
                .whereGreaterThanOrEqualTo("priceNgn", 0L)
                .snapshots()
                .map { snapshot ->
                    val loaded = snapshot.toObjects(PricingPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (loaded.isEmpty()) SubscriptionDefaults.defaultPlans else loaded.sortedBy { it.priceNgn }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- 5. SUPER ADMINISTRATOR OPERATIONS ---
    fun hashPassword(rawPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(rawPassword.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun getOrProvisionAdminRecord(): Result<AdminRecord?> {
        var docPath = "admins/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("admins").document(uid)
            docPath = docRef.path
            val snap = docRef.get().await()
            if (snap.exists()) {
                Result.success(snap.toObject(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
            } else {
                Result.success(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun rotateSuperAdminPassword(newPassword: String): Result<AdminRecord> {
        var docPath = "admins/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            require(newPassword.length >= 8) { "New administrator password must be at least 8 characters." }
            require(newPassword != "Mercy2026") { "New password must be different from initial password Mercy2026." }

            val docRef = db.collection("admins").document(uid)
            docPath = docRef.path
            val hashed = hashPassword(newPassword)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "passwordHash" to hashed,
                        "mustChangePassword" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "adminUid" to uid,
                        "username" to "amazingdigitals",
                        "passwordHash" to hashed,
                        "mustChangePassword" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            recordAuditLog(
                actionType = "ADMIN_PASSWORD_ROTATED",
                targetId = uid,
                details = "Super administrator amazingdigitals completed mandatory password rotation."
            )
            Result.success(
                AdminRecord(
                    id = uid,
                    adminUid = uid,
                    username = "amazingdigitals",
                    passwordHash = hashed,
                    mustChangePassword = false
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeAllPaymentRequestsForAdmin(): Flow<List<PaymentRequest>> = flow {
        val uid = requireUserId()
        val path = "payment_requests"
        emitAll(
            db.collection("payment_requests")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAuditLogsForAdmin(): Flow<List<AuditLog>> = flow {
        val uid = requireUserId()
        val path = "audit_logs"
        emitAll(
            db.collection("audit_logs")
                .whereGreaterThanOrEqualTo("actorUid", "")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun adminVerifyOrRejectPayment(
        payment: PaymentRequest,
        newStatus: String, // VERIFIED, REJECTED, UNDER_REVIEW
        verificationNotes: String
    ): Result<Unit> {
        var docPath = "payment_requests/${payment.id}"
        return try {
            val adminUid = requireUserId()
            require(payment.id.isNotBlank()) { "Payment ID must not be blank" }
            val safeStatus = newStatus.uppercase(Locale.US)
            require(safeStatus in listOf("VERIFIED", "REJECTED", "UNDER_REVIEW")) { "Invalid status" }

            val paymentRef = db.collection("payment_requests").document(payment.id)
            docPath = paymentRef.path
            paymentRef.update(
                mapOf(
                    "status" to safeStatus,
                    "verifiedByAdminId" to adminUid,
                    "verificationNotes" to verificationNotes.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // If approved (VERIFIED), activate the customer's subscription and update their storage quota
            if (safeStatus == "VERIFIED" && payment.userId.isNotBlank()) {
                val quotaBytes = SubscriptionDefaults.quotaBytesForTier(payment.selectedPlan)
                val userRef = db.collection("users").document(payment.userId)
                docPath = userRef.path
                val userSnap = userRef.get().await()
                if (userSnap.exists()) {
                    userRef.update(
                        mapOf(
                            "subscriptionTier" to payment.selectedPlan,
                            "storageQuotaBytes" to quotaBytes,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            recordAuditLog(
                actionType = "PAYMENT_$safeStatus",
                targetId = payment.id,
                details = "Payment ${payment.transactionReference} (${payment.customerEmail}) for ${payment.selectedPlan} (NGN ${payment.amountTransferred}) marked $safeStatus. Notes: ${verificationNotes.take(200)}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminSavePricingPlan(
        tier: String,
        name: String,
        priceNgn: Long,
        storageQuotaMb: Long,
        featuresSummary: String
    ): Result<Unit> {
        var docPath = "pricing_plans/$tier"
        return try {
            val adminUid = requireUserId()
            val safeTier = tier.uppercase(Locale.US)
            val docRef = db.collection("pricing_plans").document(safeTier)
            docPath = docRef.path
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(60),
                        "priceNgn" to priceNgn.coerceIn(0L, 10_000_000L),
                        "storageQuotaMb" to storageQuotaMb.coerceIn(10L, 1_000_000L),
                        "featuresSummary" to featuresSummary.trim().take(1000),
                        "updatedByUid" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "tier" to safeTier,
                        "name" to name.trim().take(60),
                        "priceNgn" to priceNgn.coerceIn(0L, 10_000_000L),
                        "storageQuotaMb" to storageQuotaMb.coerceIn(10L, 1_000_000L),
                        "featuresSummary" to featuresSummary.trim().take(1000),
                        "updatedByUid" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            recordAuditLog(
                actionType = "PLAN_UPDATED",
                targetId = safeTier,
                details = "Updated pricing plan $safeTier ($name) to NGN $priceNgn / ${storageQuotaMb}MB"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordAuditLog(
        actionType: String,
        targetId: String,
        details: String
    ): Result<Unit> {
        var docPath = "audit_logs"
        return try {
            val adminUid = requireUserId()
            val docRef = db.collection("audit_logs").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "actorUid" to adminUid,
                    "actorName" to "amazingdigitals",
                    "actionType" to actionType.take(80),
                    "targetId" to targetId.ifBlank { "system" }.take(128),
                    "details" to details.ifBlank { "Admin action executed" }.take(1000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
