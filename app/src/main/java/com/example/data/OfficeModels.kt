package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val subscriptionTier: String = "FREE",
    val storageUsedBytes: Long = 0L,
    val storageQuotaBytes: Long = 52_428_800L, // 50 MB for FREE
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OfficeFile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val fileType: String = "WORD", // WORD, SPREADSHEET, PRESENTATION, PDF, IMAGE, FOLDER
    val folderId: String = "",
    val content: String = "",
    val pageSettings: String = "A4|PORTRAIT|NORMAL||Amazing Office Document|true",
    val sizeBytes: Long = 1024L,
    val trashed: Boolean = false,
    val sharedWithEmails: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val phoneNumber: String = "",
    val selectedPlan: String = "PRO", // BASIC, PRO, PREMIUM
    val amountTransferred: Long = 5000L,
    val transactionReference: String = "",
    val paymentDate: String = "",
    val receiptSummary: String = "",
    val status: String = "PENDING", // PENDING, UNDER_REVIEW, VERIFIED, REJECTED
    val verifiedByAdminId: String = "",
    val verificationNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PricingPlan(
    @DocumentId val id: String = "",
    val tier: String = "FREE",
    val name: String = "Free Starter",
    val priceNgn: Long = 0L,
    val storageQuotaMb: Long = 50L,
    val featuresSummary: String = "",
    val updatedByUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val username: String = "amazingdigitals",
    val passwordHash: String = "",
    val mustChangePassword: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorName: String = "amazingdigitals",
    val actionType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// --- SUBSCRIPTION DEFAULTS ---
object SubscriptionDefaults {
    val defaultPlans = listOf(
        PricingPlan(
            id = "FREE",
            tier = "FREE",
            name = "Free Plan",
            priceNgn = 0L,
            storageQuotaMb = 50L,
            featuresSummary = "Up to 5 documents • Basic Word & Spreadsheet editing • 50 MB Cloud Storage • Standard Print & PDF Export"
        ),
        PricingPlan(
            id = "BASIC",
            tier = "BASIC",
            name = "Basic Plan",
            priceNgn = 2000L,
            storageQuotaMb = 1024L,
            featuresSummary = "Up to 50 documents • Full Word & PDF Studio • Basic Spreadsheets • 1 GB Cloud Storage • Word-to-PDF Converter"
        ),
        PricingPlan(
            id = "PRO",
            tier = "PRO",
            name = "Pro Suite",
            priceNgn = 5000L,
            storageQuotaMb = 5120L,
            featuresSummary = "Unlimited Word, Sheets & Slides • Full PDF Studio & OCR • Amazing Image Editor • Gemini AI Assistant • 5 GB Cloud Storage"
        ),
        PricingPlan(
            id = "PREMIUM",
            tier = "PREMIUM",
            name = "Premium Enterprise",
            priceNgn = 10000L,
            storageQuotaMb = 25600L,
            featuresSummary = "All Pro features • Priority Gemini AI Processing • Advanced Formulas & Charts • Batch Conversions • 25 GB Cloud Storage"
        )
    )

    fun quotaBytesForTier(tier: String): Long = when (tier.uppercase(Locale.US)) {
        "BASIC" -> 1_073_741_824L // 1 GB
        "PRO" -> 5_368_709_120L // 5 GB
        "PREMIUM" -> 26_843_545_600L // 25 GB
        else -> 52_428_800L // 50 MB
    }
}

// --- OFFICE TEMPLATES ---
data class OfficeTemplate(
    val id: String,
    val title: String,
    val category: String,
    val fileType: String,
    val description: String,
    val initialContent: String,
    val initialPageSettings: String = "A4|PORTRAIT|NORMAL|Amazing Digitals|Page Footer|true"
)

object OfficeTemplatesCatalog {
    val templates = listOf(
        OfficeTemplate(
            id = "tpl_word_proposal",
            title = "Executive Business Proposal",
            category = "Business",
            fileType = "WORD",
            description = "Structured client proposal with executive summary, scope of work, deliverables, and pricing table.",
            initialContent = """
                # EXECUTIVE BUSINESS PROPOSAL
                **Prepared by:** Amazing Digitals Consulting
                **Date:** November 2026
                
                ## 1. Executive Summary
                This proposal outlines our strategic initiative to modernize digital operations, streamline cross-department workflows, and increase annual recurring revenue by 35% over the next four quarters.
                
                ## 2. Project Objectives & Scope
                • Deploy unified cloud document management and collaborative workspaces.
                • Automate financial reporting and KPI dashboards across regional branches.
                • Train key stakeholders on AI-assisted document authoring and data analysis.
                
                ## 3. Investment & Milestone Schedule
                | Phase | Deliverable | Timeline | Investment (NGN) |
                | Phase 1 | Discovery & Architecture | Weeks 1-2 | ₦450,000 |
                | Phase 2 | Core Suite Deployment | Weeks 3-6 | ₦1,200,000 |
                | Phase 3 | Staff Training & QA | Weeks 7-8 | ₦350,000 |
                
                ## 4. Authorization & Next Steps
                Approved by: ___________________________    Date: _______________
            """.trimIndent()
        ),
        OfficeTemplate(
            id = "tpl_word_cv",
            title = "Modern Professional CV / Resume",
            category = "Career",
            fileType = "WORD",
            description = "Clean, ATS-friendly curriculum vitae for executives, engineers, educators, and graduates.",
            initialContent = """
                # ADEBAYO OLUWASEUN
                Lagos, Nigeria • +234 800 000 0000 • adebayo.pro@example.com
                
                ## PROFESSIONAL SUMMARY
                Results-driven Senior Operations & Product Specialist with 7+ years of experience leading digital transformation, enterprise software rollouts, and cross-functional teams.
                
                ## CORE COMPETENCIES
                • Strategic Planning & Financial Modeling
                • Enterprise Project Management (Agile / Prince2)
                • Data Analytics, Executive Reporting & Stakeholder Relations
                
                ## PROFESSIONAL EXPERIENCE
                **Lead Operations Manager — PrimeTech Solutions (2023 – Present)**
                • Directed a 15-person product and operations unit delivering 99.4% SLA compliance.
                • Reduced quarterly reporting turnaround time from 5 days to 4 hours using automated spreadsheets.
                
                ## EDUCATION & CERTIFICATIONS
                **B.Sc. Computer Science & Economics** — University of Lagos
            """.trimIndent()
        ),
        OfficeTemplate(
            id = "tpl_word_letter",
            title = "Official Corporate Letter",
            category = "Correspondence",
            fileType = "WORD",
            description = "Formal business and organizational letterhead template with header and signature block.",
            initialContent = """
                **AMAZING DIGITALS ENTERPRISE**
                Victoria Island, Lagos, Nigeria
                November 18, 2026
                
                **The Managing Director,**
                Apex Global Ventures Ltd,
                Abuja, Nigeria.
                
                Dear Sir/Madam,
                
                ## RE: PARTNERSHIP & ENTERPRISE SOFTWARE DEPLOYMENT
                We are pleased to formally confirm our readiness to deploy the Amazing Office AI Suite across your regional offices starting next month.
                
                Please find enclosed our implementation schedule and service level agreement for your review.
                
                Yours faithfully,
                
                **Emmanuel Ajibade**
                Chief Executive Officer, Amazing Digitals
            """.trimIndent()
        ),
        OfficeTemplate(
            id = "tpl_word_academic",
            title = "Academic Research Paper",
            category = "Education",
            fileType = "WORD",
            description = "Structured university assignment and research manuscript with abstract, methodology, and references.",
            initialContent = """
                # IMPACT OF ARTIFICIAL INTELLIGENCE ON MODERN OFFICE PRODUCTIVITY
                **Department of Information Systems • Faculty of Science**
                
                ## Abstract
                This study evaluates the quantitative and qualitative improvements in document authoring, financial computation, and executive presentations when integrating generative AI models into cloud office suites.
                
                ## 1. Introduction
                Organizations worldwide rely on word processors, spreadsheets, and slide decks for daily knowledge work.
                
                ## 2. Methodology
                A controlled trial of 120 professionals measured task completion speed and accuracy across document drafting and formula synthesis.
                
                ## 3. References
                1. Ajibade, E. (2026). Cloud Productivity Architectures in Emerging Markets.
            """.trimIndent()
        ),
        OfficeTemplate(
            id = "tpl_sheet_budget",
            title = "Corporate Revenue & Profit Ledger",
            category = "Finance",
            fileType = "SPREADSHEET",
            description = "Multi-sheet financial model with automatic SUM, AVERAGE, MAX, IF, and profit margin formulas.",
            initialContent = SpreadsheetEngine.createDefaultBudgetJson()
        ),
        OfficeTemplate(
            id = "tpl_slides_pitch",
            title = "Executive Investor Pitch Deck",
            category = "Presentations",
            fileType = "PRESENTATION",
            description = "6-slide investor and sales presentation deck with Navy & Gold executive styling.",
            initialContent = PresentationEngine.createDefaultPresentationJson("Executive Investor Pitch Deck", "NAVY_GOLD")
        ),
        OfficeTemplate(
            id = "tpl_slides_church",
            title = "Sunday Service & Ministry Presentation",
            category = "Church & Ministry",
            fileType = "PRESENTATION",
            description = "Warm sanctuary presentation slides for announcements, scripture readings, and sermon outlines.",
            initialContent = PresentationEngine.createDefaultPresentationJson("Sunday Worship & Sermon Outline", "ROYAL_SANCTUARY")
        )
    )
}

// --- SPREADSHEET ENGINE WITH REAL FORMULA SUPPORT ---
data class SheetTab(
    val name: String,
    val cells: Map<String, String> = emptyMap(), // e.g., "A1" -> "Department", "D2" -> "=B2-C2"
    val chartType: String = "BAR", // BAR, COLUMN, LINE, PIE, DOUGHNUT, AREA
    val chartTitle: String = "Revenue vs Expenses Overview",
    val chartRange: String = "A2:C5"
)

object SpreadsheetEngine {
    val columns = listOf("A", "B", "C", "D", "E", "F", "G", "H")
    const val defaultRows = 18

    fun createDefaultBudgetJson(): String {
        val initialCells = mapOf(
            "A1" to "Division",
            "B1" to "Revenue (NGN)",
            "C1" to "Expenses (NGN)",
            "D1" to "Net Profit",
            "E1" to "Status",
            "A2" to "Cloud SaaS",
            "B2" to "1850000",
            "C2" to "620000",
            "D2" to "=B2-C2",
            "E2" to "=IF(D2>500000,\"Exceeds Target\",\"On Track\")",
            "A3" to "Enterprise Training",
            "B3" to "940000",
            "C3" to "310000",
            "D3" to "=B3-C3",
            "E3" to "=IF(D3>500000,\"Exceeds Target\",\"On Track\")",
            "A4" to "Document Studio",
            "B4" to "1420000",
            "C4" to "480000",
            "D4" to "=B4-C4",
            "E4" to "=IF(D4>500000,\"Exceeds Target\",\"On Track\")",
            "A5" to "AI Consulting",
            "B5" to "2150000",
            "C5" to "590000",
            "D5" to "=B5-C5",
            "E5" to "=IF(D5>500000,\"Exceeds Target\",\"On Track\")",
            "A6" to "TOTAL",
            "B6" to "=SUM(B2:B5)",
            "C6" to "=SUM(C2:C5)",
            "D6" to "=SUM(D2:D5)",
            "E6" to "=CONCAT(\"Avg: \",ROUND(AVERAGE(D2:D5),0))"
        )
        val sheet1 = SheetTab(
            name = "Q4 Financials",
            cells = initialCells,
            chartType = "COLUMN",
            chartTitle = "Division Revenue & Profit (NGN)",
            chartRange = "A2:D5"
        )
        val sheet2 = SheetTab(
            name = "Staff Payroll",
            cells = mapOf(
                "A1" to "Staff Name",
                "B1" to "Role",
                "C1" to "Base Salary",
                "D1" to "Bonus",
                "E1" to "Total Pay",
                "A2" to "Chinedu Okafor",
                "B2" to "Lead Architect",
                "C2" to "450000",
                "D2" to "75000",
                "E2" to "=C2+D2",
                "A3" to "Amina Bello",
                "B3" to "Product Designer",
                "C3" to "380000",
                "D3" to "50000",
                "E3" to "=C3+D3",
                "A4" to "Tunde Adeyemi",
                "B4" to "QA Engineer",
                "C4" to "320000",
                "D4" to "40000",
                "E4" to "=C4+D4",
                "A5" to "Payroll Total",
                "C5" to "=SUM(C2:C4)",
                "D5" to "=SUM(D2:D4)",
                "E5" to "=SUM(E2:E4)"
            ),
            chartType = "PIE",
            chartTitle = "Payroll Distribution",
            chartRange = "A2:E4"
        )
        return serializeSheets(listOf(sheet1, sheet2))
    }

    fun serializeSheets(sheets: List<SheetTab>): String {
        val arr = JSONArray()
        for (sheet in sheets) {
            val obj = JSONObject()
            obj.put("name", sheet.name)
            obj.put("chartType", sheet.chartType)
            obj.put("chartTitle", sheet.chartTitle)
            obj.put("chartRange", sheet.chartRange)
            val cellsObj = JSONObject()
            sheet.cells.forEach { (k, v) -> if (v.isNotEmpty()) cellsObj.put(k, v) }
            obj.put("cells", cellsObj)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseSheets(json: String): List<SheetTab> {
        if (json.isBlank()) return parseSheets(createDefaultBudgetJson())
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SheetTab>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val cellsObj = obj.optJSONObject("cells") ?: JSONObject()
                val map = mutableMapOf<String, String>()
                val keys = cellsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = cellsObj.optString(k, "")
                }
                list.add(
                    SheetTab(
                        name = obj.optString("name", "Sheet ${i + 1}"),
                        cells = map,
                        chartType = obj.optString("chartType", "COLUMN"),
                        chartTitle = obj.optString("chartTitle", "Financial Chart"),
                        chartRange = obj.optString("chartRange", "A2:D5")
                    )
                )
            }
            if (list.isEmpty()) parseSheets(createDefaultBudgetJson()) else list
        } catch (_: Exception) {
            // Fallback for CSV or plain text import
            val map = mutableMapOf<String, String>()
            json.lines().take(defaultRows).forEachIndexed { rIdx, line ->
                line.split(",", "\t").take(columns.size).forEachIndexed { cIdx, cellVal ->
                    val trimmed = cellVal.trim()
                    if (trimmed.isNotEmpty()) {
                        map["${columns[cIdx]}${rIdx + 1}"] = trimmed
                    }
                }
            }
            listOf(SheetTab(name = "Sheet 1", cells = map))
        }
    }

    fun evaluateCell(ref: String, cells: Map<String, String>, visited: Set<String> = emptySet()): String {
        if (ref in visited) return "#REF!"
        val raw = cells[ref]?.trim() ?: return ""
        if (!raw.startsWith("=")) return raw
        val expr = raw.removePrefix("=").trim()
        return try {
            evaluateFormulaExpression(expr, cells, visited + ref)
        } catch (_: Exception) {
            "#ERROR!"
        }
    }

    fun evaluateFormulaExpression(expr: String, cells: Map<String, String>, visited: Set<String> = emptySet()): String {
        val upper = expr.uppercase(Locale.US).trim()

        // TODAY() and NOW()
        if (upper == "TODAY()") {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        if (upper == "NOW()") {
            return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        }

        // Function call pattern: FUNC(args)
        val funcIndex = expr.indexOf('(')
        if (funcIndex > 0 && expr.endsWith(")")) {
            val funcName = expr.substring(0, funcIndex).trim().uppercase(Locale.US)
            val inside = expr.substring(funcIndex + 1, expr.length - 1)
            val args = splitTopLevelArgs(inside)

            when (funcName) {
                "SUM" -> {
                    val nums = expandNumericArgs(args, cells, visited)
                    return formatNum(nums.sum())
                }
                "AVERAGE", "AVG" -> {
                    val nums = expandNumericArgs(args, cells, visited)
                    return if (nums.isEmpty()) "#DIV/0!" else formatNum(nums.average())
                }
                "COUNT" -> {
                    val nums = expandNumericArgs(args, cells, visited)
                    return nums.size.toString()
                }
                "COUNTA" -> {
                    val vals = expandRawArgs(args, cells, visited)
                    return vals.count { it.isNotBlank() }.toString()
                }
                "MAX" -> {
                    val nums = expandNumericArgs(args, cells, visited)
                    return if (nums.isEmpty()) "0" else formatNum(nums.maxOrNull() ?: 0.0)
                }
                "MIN" -> {
                    val nums = expandNumericArgs(args, cells, visited)
                    return if (nums.isEmpty()) "0" else formatNum(nums.minOrNull() ?: 0.0)
                }
                "ROUND" -> {
                    if (args.isEmpty()) return "#VALUE!"
                    val v = resolveSingleNumeric(args[0], cells, visited) ?: return "#VALUE!"
                    val digits = if (args.size > 1) (resolveSingleNumeric(args[1], cells, visited)?.toInt() ?: 0) else 0
                    val factor = 10.0.pow(digits)
                    return formatNum((v * factor).roundToLong() / factor)
                }
                "CONCAT" -> {
                    return args.joinToString("") { resolveSingleText(it, cells, visited) }
                }
                "LEFT" -> {
                    if (args.isEmpty()) return "#VALUE!"
                    val s = resolveSingleText(args[0], cells, visited)
                    val len = if (args.size > 1) (resolveSingleNumeric(args[1], cells, visited)?.toInt() ?: 1) else 1
                    return s.take(len.coerceAtLeast(0))
                }
                "RIGHT" -> {
                    if (args.isEmpty()) return "#VALUE!"
                    val s = resolveSingleText(args[0], cells, visited)
                    val len = if (args.size > 1) (resolveSingleNumeric(args[1], cells, visited)?.toInt() ?: 1) else 1
                    return s.takeLast(len.coerceAtLeast(0))
                }
                "MID" -> {
                    if (args.size < 3) return "#VALUE!"
                    val s = resolveSingleText(args[0], cells, visited)
                    val start = (resolveSingleNumeric(args[1], cells, visited)?.toInt() ?: 1).coerceAtLeast(1) - 1
                    val len = (resolveSingleNumeric(args[2], cells, visited)?.toInt() ?: 1).coerceAtLeast(0)
                    return if (start >= s.length) "" else s.substring(start, (start + len).coerceAtMost(s.length))
                }
                "IF" -> {
                    if (args.size < 2) return "#VALUE!"
                    val cond = evaluateCondition(args[0], cells, visited)
                    val branch = if (cond) args[1] else args.getOrElse(2) { "\"\"" }
                    return resolveSingleText(branch, cells, visited)
                }
                "IFS" -> {
                    var i = 0
                    while (i + 1 < args.size) {
                        if (evaluateCondition(args[i], cells, visited)) {
                            return resolveSingleText(args[i + 1], cells, visited)
                        }
                        i += 2
                    }
                    return "#N/A"
                }
                "AND" -> {
                    return args.all { evaluateCondition(it, cells, visited) }.toString().uppercase(Locale.US)
                }
                "OR" -> {
                    return args.any { evaluateCondition(it, cells, visited) }.toString().uppercase(Locale.US)
                }
                "COUNTIF" -> {
                    if (args.size < 2) return "#VALUE!"
                    val vals = expandRawArgs(listOf(args[0]), cells, visited)
                    val criterion = resolveSingleText(args[1], cells, visited)
                    return vals.count { matchesCriterion(it, criterion) }.toString()
                }
                "SUMIF" -> {
                    if (args.size < 2) return "#VALUE!"
                    val rangeRefs = expandCellRefs(args[0])
                    val criterion = resolveSingleText(args[1], cells, visited)
                    val sumRefs = if (args.size >= 3) expandCellRefs(args[2]) else rangeRefs
                    var total = 0.0
                    for (idx in rangeRefs.indices) {
                        val checkVal = evaluateCell(rangeRefs[idx], cells, visited)
                        if (matchesCriterion(checkVal, criterion)) {
                            val targetRef = sumRefs.getOrNull(idx) ?: continue
                            val num = evaluateCell(targetRef, cells, visited).toDoubleOrNull() ?: 0.0
                            total += num
                        }
                    }
                    return formatNum(total)
                }
                "VLOOKUP", "XLOOKUP" -> {
                    if (args.size < 2) return "#VALUE!"
                    val lookupKey = resolveSingleText(args[0], cells, visited)
                    val refs = expandCellRefs(args[1])
                    val matchRef = refs.firstOrNull {
                        evaluateCell(it, cells, visited).equals(lookupKey, ignoreCase = true)
                    } ?: return "#N/A"
                    val rowNum = matchRef.drop(1)
                    val targetColIdx = if (args.size >= 3) {
                        ((resolveSingleNumeric(args[2], cells, visited)?.toInt() ?: 1) - 1).coerceIn(0, columns.lastIndex)
                    } else 1
                    val startColIdx = columns.indexOf(refs.firstOrNull()?.take(1) ?: "A").coerceAtLeast(0)
                    val col = columns.getOrElse(startColIdx + targetColIdx) { "B" }
                    return evaluateCell("$col$rowNum", cells, visited)
                }
            }
        }

        // Binary arithmetic (+, -, *, /)
        return evalArithmetic(expr, cells, visited)
    }

    private fun splitTopLevelArgs(s: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var inQuotes = false
        val current = StringBuilder()
        for (ch in s) {
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }
                !inQuotes && ch == '(' -> {
                    depth++
                    current.append(ch)
                }
                !inQuotes && ch == ')' -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    current.append(ch)
                }
                !inQuotes && depth == 0 && ch == ',' -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString().trim())
        return result
    }

    fun expandCellRefs(rangeOrRef: String): List<String> {
        val cleaned = rangeOrRef.trim().uppercase(Locale.US)
        if (cleaned.contains(":")) {
            val parts = cleaned.split(":")
            if (parts.size == 2) {
                val startCol = parts[0].takeWhile { it.isLetter() }
                val startRow = parts[0].dropWhile { it.isLetter() }.toIntOrNull() ?: return emptyList()
                val endCol = parts[1].takeWhile { it.isLetter() }
                val endRow = parts[1].dropWhile { it.isLetter() }.toIntOrNull() ?: return emptyList()
                val c1 = columns.indexOf(startCol).coerceAtLeast(0)
                val c2 = columns.indexOf(endCol).coerceAtLeast(c1)
                val refs = mutableListOf<String>()
                for (r in startRow..endRow) {
                    for (c in c1..c2) {
                        refs.add("${columns[c]}$r")
                    }
                }
                return refs
            }
        }
        return if (isCellRef(cleaned)) listOf(cleaned) else emptyList()
    }

    private fun isCellRef(token: String): Boolean {
        if (token.length < 2 || token.length > 3) return false
        val col = token.take(1).uppercase(Locale.US)
        val row = token.drop(1).toIntOrNull() ?: return false
        return col in columns && row in 1..99
    }

    private fun expandRawArgs(args: List<String>, cells: Map<String, String>, visited: Set<String>): List<String> {
        val list = mutableListOf<String>()
        for (arg in args) {
            val refs = expandCellRefs(arg)
            if (refs.isNotEmpty()) {
                refs.forEach { list.add(evaluateCell(it, cells, visited)) }
            } else {
                list.add(resolveSingleText(arg, cells, visited))
            }
        }
        return list
    }

    private fun expandNumericArgs(args: List<String>, cells: Map<String, String>, visited: Set<String>): List<Double> {
        return expandRawArgs(args, cells, visited).mapNotNull { it.replace(",", "").toDoubleOrNull() }
    }

    private fun resolveSingleNumeric(arg: String, cells: Map<String, String>, visited: Set<String>): Double? {
        val t = resolveSingleText(arg, cells, visited)
        return t.replace(",", "").toDoubleOrNull()
    }

    private fun resolveSingleText(arg: String, cells: Map<String, String>, visited: Set<String>): String {
        val trimmed = arg.trim()
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        if (isCellRef(trimmed.uppercase(Locale.US))) {
            return evaluateCell(trimmed.uppercase(Locale.US), cells, visited)
        }
        if (trimmed.contains("(") && trimmed.endsWith(")")) {
            return evaluateFormulaExpression(trimmed, cells, visited)
        }
        return evalArithmetic(trimmed, cells, visited)
    }

    private fun evaluateCondition(cond: String, cells: Map<String, String>, visited: Set<String>): Boolean {
        val ops = listOf(">=", "<=", "<>", "!=", "=", ">", "<")
        for (op in ops) {
            val idx = cond.indexOf(op)
            if (idx > 0) {
                val left = resolveSingleText(cond.substring(0, idx), cells, visited)
                val right = resolveSingleText(cond.substring(idx + op.length), cells, visited)
                val lNum = left.toDoubleOrNull()
                val rNum = right.toDoubleOrNull()
                return if (lNum != null && rNum != null) {
                    when (op) {
                        ">=" -> lNum >= rNum
                        "<=" -> lNum <= rNum
                        "<>", "!=" -> lNum != rNum
                        "=" -> lNum == rNum
                        ">" -> lNum > rNum
                        "<" -> lNum < rNum
                        else -> false
                    }
                } else {
                    when (op) {
                        "<>", "!=" -> !left.equals(right, ignoreCase = true)
                        "=" -> left.equals(right, ignoreCase = true)
                        else -> false
                    }
                }
            }
        }
        return cond.trim().equals("TRUE", ignoreCase = true)
    }

    private fun matchesCriterion(cellVal: String, criterion: String): Boolean {
        val cleanCrit = criterion.trim()
        val ops = listOf(">=", "<=", "<>", ">", "<", "=")
        for (op in ops) {
            if (cleanCrit.startsWith(op)) {
                val rhs = cleanCrit.removePrefix(op).trim().toDoubleOrNull()
                val lhs = cellVal.toDoubleOrNull()
                if (lhs != null && rhs != null) {
                    return when (op) {
                        ">=" -> lhs >= rhs
                        "<=" -> lhs <= rhs
                        "<>" -> lhs != rhs
                        ">" -> lhs > rhs
                        "<" -> lhs < rhs
                        "=" -> lhs == rhs
                        else -> false
                    }
                }
            }
        }
        return cellVal.equals(cleanCrit, ignoreCase = true)
    }

    private fun evalArithmetic(expr: String, cells: Map<String, String>, visited: Set<String>): String {
        val trimmed = expr.trim()
        // Replace cell refs with their evaluated numeric values
        val cellPattern = Regex("\\b([A-Ha-h][1-9][0-9]?)\\b")
        val substituted = cellPattern.replace(trimmed) { match ->
            val ref = match.value.uppercase(Locale.US)
            val v = evaluateCell(ref, cells, visited)
            v.ifBlank { "0" }
        }
        // Simple +, -, *, / evaluator
        val plusIdx = substituted.lastIndexOf('+')
        if (plusIdx > 0) {
            val l = evalArithmetic(substituted.substring(0, plusIdx), emptyMap(), visited).toDoubleOrNull()
            val r = evalArithmetic(substituted.substring(plusIdx + 1), emptyMap(), visited).toDoubleOrNull()
            if (l != null && r != null) return formatNum(l + r)
        }
        val minusIdx = substituted.lastIndexOf('-')
        if (minusIdx > 0) {
            val l = evalArithmetic(substituted.substring(0, minusIdx), emptyMap(), visited).toDoubleOrNull()
            val r = evalArithmetic(substituted.substring(minusIdx + 1), emptyMap(), visited).toDoubleOrNull()
            if (l != null && r != null) return formatNum(l - r)
        }
        val mulIdx = substituted.lastIndexOf('*')
        if (mulIdx > 0) {
            val l = evalArithmetic(substituted.substring(0, mulIdx), emptyMap(), visited).toDoubleOrNull()
            val r = evalArithmetic(substituted.substring(mulIdx + 1), emptyMap(), visited).toDoubleOrNull()
            if (l != null && r != null) return formatNum(l * r)
        }
        val divIdx = substituted.lastIndexOf('/')
        if (divIdx > 0) {
            val l = evalArithmetic(substituted.substring(0, divIdx), emptyMap(), visited).toDoubleOrNull()
            val r = evalArithmetic(substituted.substring(divIdx + 1), emptyMap(), visited).toDoubleOrNull()
            if (l != null && r != null) {
                return if (r == 0.0) "#DIV/0!" else formatNum(l / r)
            }
        }
        val asNum = substituted.toDoubleOrNull()
        return if (asNum != null) formatNum(asNum) else trimmed
    }

    private fun formatNum(d: Double): String {
        val longVal = d.toLong()
        return if (d == longVal.toDouble()) longVal.toString() else String.format(Locale.US, "%.2f", d)
    }
}

// --- PRESENTATION ENGINE ---
data class SlideItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val bullets: List<String>,
    val speakerNotes: String,
    val layout: String = "TITLE_AND_BULLETS", // TITLE_ONLY, TITLE_AND_BULLETS, TWO_COLUMN, METRIC_HIGHLIGHT
    val transition: String = "FADE", // FADE, SLIDE, ZOOM
    val chartLabel: String = ""
)

data class PresentationDeck(
    val themeId: String = "NAVY_GOLD", // NAVY_GOLD, ROYAL_MODERN, EMERALD_GROWTH, ROYAL_SANCTUARY, MINIMAL_SLATE
    val slides: List<SlideItem> = emptyList()
)

object PresentationEngine {
    fun createDefaultPresentationJson(deckTitle: String, themeId: String = "NAVY_GOLD"): String {
        val slides = listOf(
            SlideItem(
                id = "s1",
                title = deckTitle,
                subtitle = "Powered by Amazing Office AI Suite • Amazing Digitals",
                bullets = listOf(
                    "Strategic Vision & Executive Overview",
                    "Data-Driven Operations & Cloud Collaboration",
                    "Q4 Milestones & Growth Projections"
                ),
                speakerNotes = "Welcome everyone and introduce the core objective of today's presentation.",
                layout = "TITLE_AND_BULLETS",
                transition = "FADE"
            ),
            SlideItem(
                id = "s2",
                title = "Core Strategic Pillars",
                subtitle = "Building Resilient Digital Infrastructure",
                bullets = listOf(
                    "1. Unified Document & Spreadsheet Authoring",
                    "2. Real-Time Cloud Storage & Role-Based Security",
                    "3. Gemini AI Productivity Automation Across Teams"
                ),
                speakerNotes = "Emphasize how each pillar reduces operational friction and boosts output.",
                layout = "TITLE_AND_BULLETS",
                transition = "SLIDE"
            ),
            SlideItem(
                id = "s3",
                title = "Key Performance Indicators",
                subtitle = "Year-over-Year Operational Gains",
                bullets = listOf(
                    "Revenue Growth: +42% YoY across enterprise accounts",
                    "Document Turnaround: 3.4x faster with AI drafting",
                    "Client Retention: 96.8% net satisfaction rating"
                ),
                speakerNotes = "Highlight the 42% YoY growth metric and invite questions on the financial breakdown.",
                layout = "METRIC_HIGHLIGHT",
                transition = "ZOOM",
                chartLabel = "Q1: 68% | Q2: 79% | Q3: 88% | Q4: 97%"
            )
        )
        return serializeDeck(PresentationDeck(themeId = themeId, slides = slides))
    }

    fun serializeDeck(deck: PresentationDeck): String {
        val root = JSONObject()
        root.put("themeId", deck.themeId)
        val arr = JSONArray()
        for (slide in deck.slides) {
            val sObj = JSONObject()
            sObj.put("id", slide.id)
            sObj.put("title", slide.title)
            sObj.put("subtitle", slide.subtitle)
            sObj.put("speakerNotes", slide.speakerNotes)
            sObj.put("layout", slide.layout)
            sObj.put("transition", slide.transition)
            sObj.put("chartLabel", slide.chartLabel)
            val bArr = JSONArray()
            slide.bullets.forEach { bArr.put(it) }
            sObj.put("bullets", bArr)
            arr.put(sObj)
        }
        root.put("slides", arr)
        return root.toString()
    }

    fun parseDeck(json: String): PresentationDeck {
        if (json.isBlank()) return parseDeck(createDefaultPresentationJson("Executive Presentation"))
        return try {
            val root = JSONObject(json)
            val themeId = root.optString("themeId", "NAVY_GOLD")
            val arr = root.optJSONArray("slides") ?: JSONArray()
            val list = mutableListOf<SlideItem>()
            for (i in 0 until arr.length()) {
                val sObj = arr.getJSONObject(i)
                val bArr = sObj.optJSONArray("bullets") ?: JSONArray()
                val bullets = (0 until bArr.length()).map { bArr.optString(it, "") }.filter { it.isNotBlank() }
                list.add(
                    SlideItem(
                        id = sObj.optString("id", "s_${i + 1}"),
                        title = sObj.optString("title", "Slide ${i + 1}"),
                        subtitle = sObj.optString("subtitle", ""),
                        bullets = bullets,
                        speakerNotes = sObj.optString("speakerNotes", ""),
                        layout = sObj.optString("layout", "TITLE_AND_BULLETS"),
                        transition = sObj.optString("transition", "FADE"),
                        chartLabel = sObj.optString("chartLabel", "")
                    )
                )
            }
            if (list.isEmpty()) parseDeck(createDefaultPresentationJson("Executive Presentation"))
            else PresentationDeck(themeId = themeId, slides = list)
        } catch (_: Exception) {
            parseDeck(createDefaultPresentationJson("Executive Presentation"))
        }
    }
}
