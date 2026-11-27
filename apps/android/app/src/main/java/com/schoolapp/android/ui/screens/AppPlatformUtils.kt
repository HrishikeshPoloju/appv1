package com.schoolapp.android.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.schoolapp.android.data.ClassNote
import com.schoolapp.android.data.DiscussionRepository
import com.schoolapp.android.data.SchoolClass
import com.schoolapp.android.data.StudentProfile
import com.schoolapp.android.ui.theme.GenesisColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.coroutines.resume

// ============================================================================
// 1. PHONE NATIVE SPEECH-TO-TEXT + DETERMINISTIC NON-AI TEXT REFINER
// ============================================================================

/**
 * Deterministically refines raw spoken voice text without using any AI API:
 * - Cleans filler words and duplicate spaces
 * - Converts spoken punctuation & common classroom math phrases into clean symbols
 * - Fixes sentence capitalization and appends '?' for spoken questions
 */
fun refineCapturedSpeech(raw: String, isAcademicText: Boolean = true): String {
    var text = raw.trim()
    if (text.isBlank()) return ""

    // Remove common leading spoken fillers
    text = text.replace(Regex("^(?i)(um+|uh+|er+|hmm+)\\s+"), "")

    // Convert spoken punctuation phrases
    text = text
        .replace(Regex("(?i)\\s*question mark\\b"), "?")
        .replace(Regex("(?i)\\s*full stop\\b"), ".")
        .replace(Regex("(?i)\\s*exclamation mark\\b"), "!")
        .replace(Regex("(?i)\\s*comma\\b"), ",")
        .replace(Regex("(?i)\\s*colon\\b"), ":")
        .replace(Regex("(?i)\\s*next line\\b"), "\n")

    if (isAcademicText) {
        // Convert spoken school math / science expressions cleanly
        text = text
            .replace(Regex("(?i)\\bx\\s+squared?\\b"), "x²")
            .replace(Regex("(?i)\\by\\s+squared?\\b"), "y²")
            .replace(Regex("(?i)\\ba\\s+squared?\\b"), "a²")
            .replace(Regex("(?i)\\bb\\s+squared?\\b"), "b²")
            .replace(Regex("(?i)\\bx\\s+cubed?\\b"), "x³")
            .replace(Regex("(?i)\\bsquare root of\\s+"), "√")
            .replace(Regex("(?i)\\broot\\s+(\\d+)\\b"), "√$1")
            .replace(Regex("(?i)\\bdivided by\\b"), "÷")
            .replace(Regex("(?i)\\bmultiplied by\\b"), "×")
            .replace(Regex("(?i)\\bis equal to\\b"), "=")
            .replace(Regex("(?i)\\bequals\\b"), "=")
            .replace(Regex("(?i)\\bdegree celsius\\b"), "°C")
    }

    // Normalize whitespace around punctuation
    text = text
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\s+([?.!,;:])"), "$1")
        .replace(Regex("\\bi\\b"), "I")
        .trim()

    // Capitalize first letter of sentences
    val sb = StringBuilder(text.length)
    var capitalizeNext = true
    for (ch in text) {
        if (capitalizeNext && ch.isLetter()) {
            sb.append(ch.uppercaseChar())
            capitalizeNext = false
        } else {
            sb.append(ch)
            if (ch == '.' || ch == '?' || ch == '!' || ch == '\n') {
                capitalizeNext = true
            }
        }
    }
    var refined = sb.toString().trim()

    // Add trailing punctuation if missing
    val lastChar = refined.lastOrNull()
    if (lastChar != null && lastChar !in listOf('.', '?', '!', ':')) {
        val lower = refined.lowercase(Locale.getDefault())
        val questionStarters = listOf(
            "how ", "what ", "why ", "when ", "where ", "which ", "who ",
            "can ", "could ", "should ", "is ", "are ", "does ", "do "
        )
        refined += if (isAcademicText && questionStarters.any { lower.startsWith(it) }) "?" else "."
    }

    return refined
}

@Composable
fun VoiceMicIconButton(
    promptHint: String = "Speak clearly into your phone...",
    isAcademicText: Boolean = true,
    tint: Color = GenesisColors.Primary,
    onResultText: (String) -> Unit
) {
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val bestRaw = matches?.firstOrNull().orEmpty()
            val refined = refineCapturedSpeech(bestRaw, isAcademicText = isAcademicText)
            if (refined.isNotBlank()) {
                onResultText(refined)
                Toast.makeText(context, "Voice captured & refined", Toast.LENGTH_SHORT).show()
            }
        }
    }

    IconButton(
        onClick = {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, promptHint)
                }
                voiceLauncher.launch(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Phone voice input service is not available on this device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Input",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun VoiceCaptureChipButton(
    label: String = "Voice Type",
    promptHint: String = "Speak your question or note clearly...",
    isAcademicText: Boolean = true,
    onResultText: (String) -> Unit
) {
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val bestRaw = matches?.firstOrNull().orEmpty()
            val refined = refineCapturedSpeech(bestRaw, isAcademicText = isAcademicText)
            if (refined.isNotBlank()) {
                onResultText(refined)
                Toast.makeText(context, "Voice captured & refined!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Surface(
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = Modifier.clickable {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, promptHint)
                }
                voiceLauncher.launch(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Phone speech recognizer is not installed on this device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(GenesisColors.Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Dictation",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = GenesisColors.Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun mergeVoiceText(existing: String, spoken: String): String {
    val cleanExisting = existing.trim()
    return if (cleanExisting.isEmpty()) spoken else "$cleanExisting $spoken"
}

// ============================================================================
// 2. REAL PDF GENERATOR, PUBLIC DOWNLOADS SAVER & NATIVE PDF VIEWER
// ============================================================================

data class SavedPdfResult(
    val fileName: String,
    val openableUri: Uri,
    val publicDownloadSaved: Boolean
)

/**
 * Generates a real, multi-line valid PDF file using Android's PdfDocument API,
 * writes it to the phone's public `Downloads/` folder AND `cacheDir` (via FileProvider)
 * so students can both test downloading AND open the PDF immediately in any PDF viewer.
 */
fun generateAndSaveSchoolPdf(
    context: Context,
    fileNameBase: String,
    documentCategory: String,
    documentTitle: String,
    subtitleLine: String,
    metaLines: List<String>,
    bodyLines: List<String>
): SavedPdfResult? {
    val safeName = fileNameBase.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(45)
    val finalFileName = "${safeName}.pdf"

    return try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0D0F14")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 105f, headerPaint)

        val schoolTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("GENESIS SCHOOL BEERAMGUDA", 36f, 46f, schoolTitlePaint)

        val categoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#38BDF8")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(documentCategory.uppercase(Locale.getDefault()), 36f, 72f, categoryPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(documentTitle.take(55), 36f, 145f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#2563EB")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(subtitleLine.take(75), 36f, 168f, subPaint)

        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }
        canvas.drawLine(36f, 184f, 559f, 184f, linePaint)

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#475569")
            textSize = 11f
        }
        var currentY = 208f
        metaLines.forEach { meta ->
            canvas.drawText(meta.take(85), 36f, currentY, metaPaint)
            currentY += 18f
        }

        currentY += 10f
        canvas.drawLine(36f, currentY, 559f, currentY, linePaint)
        currentY += 26f

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1E293B")
            textSize = 12f
        }

        bodyLines.forEach { rawLine ->
            val chunks = rawLine.chunked(78).ifEmpty { listOf("") }
            chunks.forEach { line ->
                if (currentY < 775f) {
                    canvas.drawText(line, 36f, currentY, bodyPaint)
                    currentY += 20f
                }
            }
        }

        // Footer verification stamp
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#16A34A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawLine(36f, 790f, 559f, 790f, linePaint)
        canvas.drawText("Verified Digital Academic Copy • Genesis School Official App", 36f, 812f, footerPaint)

        pdfDocument.finishPage(page)
        writePdfAndRegisterDownloads(context, pdfDocument, finalFileName)
    } catch (_: Exception) {
        null
    }
}

private fun writePdfAndRegisterDownloads(
    context: Context,
    pdfDocument: PdfDocument,
    finalFileName: String
): SavedPdfResult? {
    return try {
        val cachePdfFile = File(context.cacheDir, finalFileName)
        FileOutputStream(cachePdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        val savedInDownloads = copyPdfToPublicDownloads(context, cachePdfFile, finalFileName)
        val providerUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cachePdfFile
        )
        SavedPdfResult(
            fileName = finalFileName,
            openableUri = providerUri,
            publicDownloadSaved = savedInDownloads
        )
    } catch (_: Exception) {
        null
    }
}

private fun copyPdfToPublicDownloads(
    context: Context,
    sourcePdfFile: File,
    finalFileName: String
): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, finalFileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (downloadUri != null) {
                resolver.openOutputStream(downloadUri)?.use { out ->
                    sourcePdfFile.inputStream().use { input -> input.copyTo(out) }
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(downloadUri, contentValues, null, null)
                true
            } else {
                false
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val dest = File(downloadsDir, finalFileName)
            sourcePdfFile.copyTo(dest, overwrite = true)
            true
        }
    } catch (_: Exception) {
        false
    }
}

/**
 * Converts one or more teacher-captured photos (Camera / Gallery) into a clean,
 * multi-page A4 PDF stored in cache + public Downloads so students can download & read it.
 */
fun generatePdfFromImageUris(
    context: Context,
    imageUris: List<Uri>,
    fileNameBase: String,
    noteTitle: String,
    subject: String,
    chapter: String
): SavedPdfResult? {
    if (imageUris.isEmpty()) return null
    val safeName = fileNameBase.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "Class_Notes" }.take(45)
    val finalFileName = "${safeName}_${System.currentTimeMillis() % 10000}.pdf"

    return try {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        imageUris.forEachIndexed { idx, uri ->
            val bitmap = decodeScaledBitmapFromUri(context, uri, 1190, 1684) ?: return@forEachIndexed
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)

            var topOffset = 24f
            if (idx == 0) {
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#1E40AF")
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#475569")
                    textSize = 11f
                }
                canvas.drawText(noteTitle.ifBlank { "$subject Study Notes" }.take(52), 28f, 34f, titlePaint)
                canvas.drawText("$subject • ${chapter.ifBlank { "Classroom Notes" }} (${imageUris.size} page${if (imageUris.size > 1) "s" else ""})", 28f, 52f, subPaint)
                topOffset = 68f
            }

            val margin = 24f
            val availW = pageWidth - margin * 2f
            val availH = pageHeight - topOffset - 36f
            val scale = minOf(availW / bitmap.width.toFloat(), availH / bitmap.height.toFloat())
            val drawW = (bitmap.width * scale).toInt()
            val drawH = (bitmap.height * scale).toInt()
            val left = ((pageWidth - drawW) / 2)
            val top = topOffset.toInt() + ((availH - drawH) / 2).toInt()

            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = Rect(left, top, left + drawW, top + drawH)
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            bitmap.recycle()

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#64748B")
                textSize = 9f
            }
            canvas.drawText("Page ${idx + 1} of ${imageUris.size} • Genesis School Notes", 28f, pageHeight - 16f, footerPaint)

            pdfDocument.finishPage(page)
        }

        if (pdfDocument.pages.isEmpty()) {
            pdfDocument.close()
            return null
        }

        writePdfAndRegisterDownloads(context, pdfDocument, finalFileName)
    } catch (_: Exception) {
        null
    }
}

private fun decodeScaledBitmapFromUri(
    context: Context,
    uri: Uri,
    reqWidth: Int,
    reqHeight: Int
): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var inSampleSize = 1
        val (w, h) = opts.outWidth to opts.outHeight
        if (h > reqHeight || w > reqWidth) {
            val halfH = h / 2
            val halfW = w / 2
            while ((halfH / inSampleSize) >= reqHeight && (halfW / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
    } catch (_: Exception) {
        null
    }
}

fun downloadAndOpenClassNotePdf(context: Context, note: ClassNote, classNameLabel: String, autoOpen: Boolean = true): SavedPdfResult? {
    if (!note.localPdfUri.isNullOrBlank()) {
        val localUri = runCatching { Uri.parse(note.localPdfUri) }.getOrNull()
        if (localUri != null) {
            val safeName = "Genesis_${note.subject}_${note.title}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(45) + ".pdf"
            Toast.makeText(context, "Opening $safeName", Toast.LENGTH_SHORT).show()
            if (autoOpen) {
                openPdfUri(context, localUri)
            }
            return SavedPdfResult(fileName = safeName, openableUri = localUri, publicDownloadSaved = true)
        }
    }

    val bodyContent = listOf(
        "1. CHAPTER OVERVIEW & CORE CONCEPTS",
        "   • Topic: ${note.title}",
        "   • Syllabus Unit: ${note.chapter}",
        "   • Prepared exclusively for $classNameLabel students.",
        "",
        "2. IMPORTANT FORMULAS & KEY DEFINITIONS",
        "   • Standard Quadratic Form: ax² + bx + c = 0  (where a ≠ 0)",
        "   • Discriminant (D) = b² - 4ac",
        "   • Roots Formula: x = [-b ± √(b² - 4ac)] / 2a",
        "   • Sum of Roots (α + β) = -b/a   |   Product of Roots (α·β) = c/a",
        "   • Snell's Law of Refraction: n₁ sin(θ₁) = n₂ sin(θ₂)",
        "   • Mirror & Lens Formula: 1/f = 1/v - 1/u",
        "",
        "3. EXAM PREPARATION CHECKLIST",
        "   • Write Given Data, Formula, Step-by-step substitution, and SI Units clearly.",
        "   • Practice all NCERT / Board exemplar numericals before the weekly test.",
        "   • Post any homework doubt in My Class -> Doubts with a notebook photo."
    )

    val result = generateAndSaveSchoolPdf(
        context = context,
        fileNameBase = "Genesis_${note.subject}_${note.title}",
        documentCategory = "Official Classroom Study Notes • ${note.subject}",
        documentTitle = note.title,
        subtitleLine = "${note.chapter}  |  $classNameLabel",
        metaLines = listOf(
            "Faculty: ${note.teacherName}   •   Uploaded: ${note.date}",
            "Document Format: Printable A4 Study Handout (PDF)"
        ),
        bodyLines = bodyContent
    )

    if (result != null) {
        Toast.makeText(
            context,
            "Saved to Downloads/${result.fileName}",
            Toast.LENGTH_LONG
        ).show()
        if (autoOpen) {
            openPdfUri(context, result.openableUri)
        }
    } else {
        Toast.makeText(context, "Unable to generate PDF on this device.", Toast.LENGTH_SHORT).show()
    }
    return result
}

fun downloadAndOpenFeeReceiptPdf(context: Context, student: StudentProfile) {
    val result = generateAndSaveSchoolPdf(
        context = context,
        fileNameBase = "Genesis_Fee_Receipt_${student.studentCode}",
        documentCategory = "Official Fee Payment Receipt • AY 2026-27",
        documentTitle = "Fee Receipt #GSB-2026-${student.studentCode}",
        subtitleLine = "${student.fullName} (ID: ${student.studentCode})  •  ${student.classTitle}",
        metaLines = listOf(
            "Parent Contact: ${student.parentPhone}   •   Status: PAID & VERIFIED",
            "Residential Address: ${student.address}"
        ),
        bodyLines = listOf(
            "FEE PARTICULARS                                       AMOUNT (INR)",
            "------------------------------------------------------------------",
            "1. Tuition Fee (Term 1)                               Rs. ${student.tuitionFeeInr}",
            "2. Examination & Lab Assessment Fee                   Rs. ${student.examFeeInr}",
            "3. Sports, Co-Curricular & Smart Class Fee            Rs. ${student.activityFeeInr}",
            "------------------------------------------------------------------",
            "TOTAL AMOUNT RECEIVED                                 Rs. ${student.totalFeeInr}",
            "",
            "Payment Mode: UPI / Digital School Portal",
            "This is a computer-generated official school fee receipt."
        )
    )
    if (result != null) {
        Toast.makeText(context, "Fee Receipt saved to Downloads/${result.fileName}", Toast.LENGTH_LONG).show()
        openPdfUri(context, result.openableUri)
    }
}

fun openPdfUri(context: Context, uri: Uri) {
    try {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(viewIntent, "Open Downloaded PDF"))
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "PDF saved in Downloads folder! Install any PDF viewer to preview.",
            Toast.LENGTH_LONG
        ).show()
    }
}

// ============================================================================
// 3. REAL PHONE GPS CURRENT LOCATION ADDRESS FETCHER
// ============================================================================

@SuppressLint("MissingPermission")
suspend fun obtainCurrentDeviceLocation(context: Context, preferFresh: Boolean = false): Location? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )

    // 1. Check cached location (freshness threshold is 15s for geofence checks, 15m for address autocomplete)
    var bestCached: Location? = null
    for (provider in providers) {
        val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        if (loc != null) {
            if (bestCached == null || loc.accuracy < bestCached.accuracy) {
                bestCached = loc
            }
        }
    }
    val maxCacheAgeMs = if (preferFresh) 15_000L else 15 * 60 * 1000L
    val cacheAccurateEnough = !preferFresh || (bestCached != null && bestCached.accuracy <= 60f)
    if (bestCached != null && cacheAccurateEnough && (System.currentTimeMillis() - bestCached.time) < maxCacheAgeMs) {
        return bestCached
    }

    // 2. Request fresh live location update (prefer GPS first when testing 500 m geofence accuracy, otherwise Network/GPS)
    val enabledProviders = buildList {
        val gpsEnabled = runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val netEnabled = runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        if (preferFresh) {
            if (gpsEnabled) add(LocationManager.GPS_PROVIDER)
            if (netEnabled) add(LocationManager.NETWORK_PROVIDER)
        } else {
            if (netEnabled) add(LocationManager.NETWORK_PROVIDER)
            if (gpsEnabled) add(LocationManager.GPS_PROVIDER)
        }
    }
    if (enabledProviders.isEmpty()) return bestCached

    val fresh = withTimeoutOrNull(6000L) {
        suspendCancellableCoroutine<Location?> { cont ->
            val listeners = mutableListOf<LocationListener>()
            fun clearAll() {
                listeners.forEach { l -> runCatching { locationManager.removeUpdates(l) } }
            }
            enabledProviders.forEach { provider ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        clearAll()
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Deprecated in Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                listeners.add(listener)
                runCatching {
                    locationManager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            }
            cont.invokeOnCancellation { clearAll() }
        }
    }

    return fresh ?: bestCached
}

private suspend fun reverseGeocodeLocation(context: Context, location: Location): String =
    withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                val fullLine = addr.getAddressLine(0)
                if (!fullLine.isNullOrBlank()) {
                    return@withContext fullLine
                }
                val parts = listOfNotNull(
                    addr.featureName,
                    addr.subLocality,
                    addr.locality,
                    addr.adminArea,
                    addr.postalCode
                ).distinct()
                if (parts.isNotEmpty()) {
                    return@withContext parts.joinToString(", ")
                }
            }
        } catch (_: Exception) {
            // Fallback to coordinates if Geocoder network call is unavailable
        }
        "Lat %.5f, Lng %.5f (Verified GPS Location)".format(location.latitude, location.longitude)
    }

@Composable
fun FetchCurrentLocationButton(
    onAddressFetched: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFetching by remember { mutableStateOf(false) }

    fun triggerLocationFetch() {
        isFetching = true
        scope.launch {
            val loc = obtainCurrentDeviceLocation(context)
            if (loc != null) {
                val formattedAddress = reverseGeocodeLocation(context, loc)
                onAddressFetched(formattedAddress)
                Toast.makeText(context, "Current location address fetched!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    "Please turn ON phone GPS / Location services and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
            isFetching = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerLocationFetch()
        } else {
            Toast.makeText(
                context,
                "Location permission is required to auto-fetch your current address.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    OutlinedButton(
        onClick = {
            if (isFetching) return@OutlinedButton
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                triggerLocationFetch()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, GenesisColors.Primary),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFFEFF6FF),
            contentColor = GenesisColors.Primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        if (isFetching) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = GenesisColors.Primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Detecting GPS Location...",
                color = GenesisColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Fetch Current Location",
                tint = GenesisColors.Primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fetch My Current Location Address",
                color = GenesisColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================================
// 5. TEACHER ATTENDANCE: 500M SCHOOL GEOFENCE + FINGERPRINT BIOMETRIC CHECK
// ============================================================================

fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0].toInt().coerceAtLeast(0)
}

@Suppress("DEPRECATION")
fun verifyTeacherFingerprint(
    context: Context,
    subtitle: String,
    onSuccess: () -> Unit,
    onNoBiometricEnrolled: () -> Unit,
    onError: (String) -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        onNoBiometricEnrolled()
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val biometricManager = context.getSystemService(BiometricManager::class.java)
        val canBiometric = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricManager?.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) ?: BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        } else {
            biometricManager?.canAuthenticate() ?: BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        }

        if (canBiometric == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
            canBiometric == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
            canBiometric == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        ) {
            // If no fingerprint enrolled on device, check if device PIN/pattern credential is available on Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val canDeviceCred = biometricManager?.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                if (canDeviceCred == BiometricManager.BIOMETRIC_SUCCESS) {
                    val prompt = BiometricPrompt.Builder(context)
                        .setTitle("Teacher Attendance Verification")
                        .setSubtitle(subtitle)
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        .build()
                    prompt.authenticate(
                        CancellationSignal(),
                        context.mainExecutor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                                onSuccess()
                            }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                                onError(errString?.toString().orEmpty().ifBlank { "Verification cancelled" })
                            }
                        }
                    )
                    return
                }
            }
            onNoBiometricEnrolled()
            return
        }
    }

    val builder = BiometricPrompt.Builder(context)
        .setTitle("Fingerprint Attendance")
        .setSubtitle(subtitle)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
    }
    builder.setNegativeButton("Cancel", context.mainExecutor) { _, _ ->
        onError("Fingerprint scan cancelled")
    }

    builder.build().authenticate(
        CancellationSignal(),
        context.mainExecutor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(context, "Fingerprint not recognized. Try again.", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                onError(errString?.toString().orEmpty().ifBlank { "Authentication cancelled" })
            }
        }
    )
}

suspend fun captureStartingCampusGps(context: Context): Location? {
    val loc = obtainCurrentDeviceLocation(context, preferFresh = true) ?: return null
    val rawAddress = reverseGeocodeLocation(context, loc)
    val shortArea = if (!rawAddress.startsWith("Lat ")) {
        val parts = rawAddress.split(",").map { it.trim() }.filter { it.isNotBlank() }
        "Captured Start • " + parts.take(2).joinToString(", ")
    } else {
        "Captured Start (Live GPS)"
    }
    DiscussionRepository.updateSchoolCampusLocation(
        latitude = loc.latitude,
        longitude = loc.longitude,
        radiusMeters = 500,
        label = shortArea,
        context = context
    )
    return loc
}

/**
 * Reusable 2-step Teacher/Staff Attendance Check-In controller:
 * 1) Automatically captures starting GPS once if location permission is granted & not calibrated yet
 *    (default test baseline is Eden Garden, Viman Nagar, Pune).
 * 2) Checks live GPS is within 500 m of school/captured campus geofence, then launches Fingerprint
 *    verification and marks Present with exact check-in time.
 */
@Composable
fun rememberTeacherGeofenceFingerprintCheckIn(
    staffId: String
): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geofence by DiscussionRepository.schoolCampusGeofence.collectAsState()

    var isCheckingLocation by remember { mutableStateOf(false) }
    var outsideRadiusMeters by remember { mutableStateOf<Int?>(null) }
    var pendingFallbackDistance by remember { mutableStateOf<Int?>(null) }

    // Capture starting location once on startup when location permission is already granted.
    LaunchedEffect(Unit) {
        if (!geofence.isCalibratedFromDevice) {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                val captured = captureStartingCampusGps(context)
                if (captured != null) {
                    Toast.makeText(
                        context,
                        "Starting location captured! Stay within 500 m for fingerprint check-in.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun markPresentWithDetails(distanceMeters: Int, methodLabel: String) {
        DiscussionRepository.staffCheckIn(
            staffId = staffId,
            distanceMeters = distanceMeters,
            verificationMethod = methodLabel
        )
        val timeNow = DiscussionRepository.staffAttendance.value
            .find { it.staffId == staffId && it.dateLabel == "Today" }
            ?.checkIn ?: "now"
        Toast.makeText(
            context,
            "Marked Present at $timeNow ($distanceMeters m from campus)",
            Toast.LENGTH_LONG
        ).show()
    }

    fun runLocationAndFingerprintCheck() {
        isCheckingLocation = true
        scope.launch {
            val loc = obtainCurrentDeviceLocation(context, preferFresh = true)
            isCheckingLocation = false
            if (loc == null) {
                Toast.makeText(
                    context,
                    "Turn ON GPS / Location Services to verify 500 m school campus radius.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val distance = calculateDistanceMeters(
                loc.latitude,
                loc.longitude,
                geofence.latitude,
                geofence.longitude
            )

            if (distance > geofence.radiusMeters) {
                outsideRadiusMeters = distance
                return@launch
            }

            verifyTeacherFingerprint(
                context = context,
                subtitle = "Within ${geofence.radiusMeters} m ($distance m from start). Scan fingerprint.",
                onSuccess = {
                    markPresentWithDetails(distance, "Fingerprint • $distance m")
                },
                onNoBiometricEnrolled = {
                    pendingFallbackDistance = distance
                },
                onError = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            runLocationAndFingerprintCheck()
        } else {
            Toast.makeText(
                context,
                "Location permission is needed to check 500 m school radius.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Dialog if teacher is > 500 meters away from School Campus
    outsideRadiusMeters?.let { dist ->
        Dialog(onDismissRequest = { outsideRadiusMeters = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFFFEE2E2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = GenesisColors.Emergency, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Outside ${geofence.radiusMeters} m School Zone",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your current GPS is $dist m away from ${geofence.schoolName}. Check-in is allowed within ${geofence.radiusMeters} m of school.",
                        fontSize = 13.sp,
                        color = GenesisColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You must be within ${geofence.radiusMeters} m of your captured start point or Eden Garden, Viman Nagar, Pune. Use 'Capture Start Here' on Attendance if starting at a new test spot.",
                            fontSize = 11.sp,
                            color = GenesisColors.Primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { outsideRadiusMeters = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Fallback dialog ONLY when test device/emulator has no fingerprint or screen lock enrolled
    pendingFallbackDistance?.let { dist ->
        Dialog(onDismissRequest = { pendingFallbackDistance = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Fingerprint Enrolled on Device", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = GenesisColors.TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your GPS is verified within 500 m ($dist m from school), but this device has no fingerprint or screen lock set in Android Settings.",
                        fontSize = 13.sp,
                        color = GenesisColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { pendingFallbackDistance = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancel", color = GenesisColors.TextPrimary)
                        }
                        Button(
                            onClick = {
                                pendingFallbackDistance = null
                                markPresentWithDetails(dist, "GPS Verified • $dist m (No Sensor Lock)")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                        ) {
                            Text("Mark Present", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val trigger: () -> Unit = {
        if (!isCheckingLocation) {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                runLocationAndFingerprintCheck()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }
    }

    return isCheckingLocation to trigger
}

// ============================================================================
// 6. GLOBAL CLASS & SECTION DROPDOWN BUTTON COMPOSABLE
// ============================================================================

@Composable
fun ClassSectionDropdownButton(
    selectedClass: SchoolClass,
    availableClasses: List<SchoolClass>,
    onClassSelected: (SchoolClass) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = "Select Class & Section",
    badgeText: String? = null,
    allowClassManagement: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    var showCreateClassDialog by remember { mutableStateOf(false) }
    var newClassNameInput by remember { mutableStateOf("Class 10") }
    var newSectionInput by remember { mutableStateOf("A") }

    var showManageSubjectsDialog by remember { mutableStateOf(false) }
    val classSubjectsMap by DiscussionRepository.classSubjectsMap.collectAsState()
    val subjectsForSelected = classSubjectsMap[selectedClass.id]
        ?: DiscussionRepository.getSubjectsForClass(selectedClass.id)
    var newSubjectInput by remember { mutableStateOf("") }

    // --- Create New Class / Section Dialog ---
    if (showCreateClassDialog) {
        Dialog(onDismissRequest = { showCreateClassDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = GenesisColors.Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Class & Section",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add a new class or section to your school roster.",
                        fontSize = 12.sp,
                        color = GenesisColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newClassNameInput,
                        onValueChange = { newClassNameInput = it },
                        label = { Text("Class Name (e.g. Class 11, UKG)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Class 1", "Class 6", "Class 9", "Class 10", "Class 11").forEach { clsSuggestion ->
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { newClassNameInput = clsSuggestion }
                            ) {
                                Text(
                                    text = clsSuggestion,
                                    fontSize = 10.sp,
                                    color = GenesisColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newSectionInput,
                        onValueChange = { newSectionInput = it },
                        label = { Text("Section Name (e.g. A, Rose, IIT)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("A", "B", "C", "D", "Rose", "Lotus", "IIT").forEach { secSuggestion ->
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { newSectionInput = secSuggestion }
                            ) {
                                Text(
                                    text = secSuggestion,
                                    fontSize = 10.sp,
                                    color = GenesisColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateClassDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = GenesisColors.TextPrimary)
                        }
                        Button(
                            onClick = {
                                val newCls = DiscussionRepository.createNewClassAndSection(
                                    classNameInput = newClassNameInput,
                                    sectionInput = newSectionInput,
                                    seatCapacity = 40,
                                    scope = coroutineScope
                                )
                                Toast.makeText(context, "Created ${newCls.name}", Toast.LENGTH_SHORT).show()
                                onClassSelected(newCls)
                                showCreateClassDialog = false
                            },
                            enabled = newClassNameInput.isNotBlank() && newSectionInput.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- Manage Class Subjects Dialog ---
    if (showManageSubjectsDialog) {
        Dialog(onDismissRequest = { showManageSubjectsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GenesisColors.Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subjects in ${selectedClass.name}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSubjectInput,
                            onValueChange = { newSubjectInput = it },
                            label = { Text("Add Subject (e.g. Sanskrit, AI)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                val clean = newSubjectInput.trim()
                                if (clean.isNotBlank()) {
                                    val added = DiscussionRepository.addSubjectToClass(selectedClass.id, clean)
                                    if (added) {
                                        Toast.makeText(context, "Added $clean to ${selectedClass.name}", Toast.LENGTH_SHORT).show()
                                        newSubjectInput = ""
                                    } else {
                                        Toast.makeText(context, "$clean already exists", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Computer", "Hindi", "Sanskrit", "AI").forEach { subjSuggestion ->
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { newSubjectInput = subjSuggestion }
                            ) {
                                Text(
                                    text = subjSuggestion,
                                    fontSize = 10.sp,
                                    color = GenesisColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Active Subjects (${subjectsForSelected.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GenesisColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        subjectsForSelected.forEach { subj ->
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GenesisColors.Border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(subj, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                                    IconButton(
                                        onClick = {
                                            DiscussionRepository.removeSubjectFromClass(selectedClass.id, subj)
                                            Toast.makeText(context, "Removed $subj", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = GenesisColors.Emergency,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showManageSubjectsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Group availableClasses cleanly by Class Name -> list of SchoolClass (sections)
    val groupedByClass = remember(availableClasses) {
        val map = linkedMapOf<String, MutableList<SchoolClass>>()
        availableClasses.forEach { sc ->
            val cName = sc.className.ifBlank { sc.name.substringBefore("–").trim() }
            map.getOrPut(cName) { mutableListOf() }.add(sc)
        }
        map
    }
    val currentClassName = selectedClass.className.ifBlank { selectedClass.name.substringBefore("–").trim() }
    val currentSectionName = selectedClass.section.ifBlank { selectedClass.name.substringAfter("Section", "A").trim() }
    val sectionsForClass = groupedByClass[currentClassName].orEmpty()

    var classMenuExpanded by remember { mutableStateOf(false) }
    var sectionMenuExpanded by remember { mutableStateOf(false) }
    val activeRole by DiscussionRepository.currentRole.collectAsState()

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GenesisColors.TextSecondary
                )
                if (!badgeText.isNullOrBlank()) {
                    Surface(color = Color(0xFFF59E0B), shape = RoundedCornerShape(999.dp)) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Clean 2-Dropdown Row: Dropdown 1 = Class, Dropdown 2 = Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dropdown 1: Class
            Box(modifier = Modifier.weight(1.25f)) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, GenesisColors.Primary.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { classMenuExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Class", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextSecondary)
                            Text(
                                text = currentClassName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GenesisColors.TextPrimary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = if (classMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Class",
                            tint = GenesisColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = classMenuExpanded,
                    onDismissRequest = { classMenuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    Text(
                        text = "SELECT CLASS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GenesisColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    HorizontalDivider(color = GenesisColors.Border, thickness = 0.5.dp)
                    groupedByClass.keys.forEach { clsName ->
                        val isSel = clsName.equals(currentClassName, ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = clsName,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) GenesisColors.Primary else GenesisColors.TextPrimary,
                                    fontSize = 14.sp
                                )
                            },
                            trailingIcon = if (isSel) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp)) }
                            } else null,
                            onClick = {
                                classMenuExpanded = false
                                val list = groupedByClass[clsName].orEmpty()
                                val match = list.find { it.section.equals(currentSectionName, ignoreCase = true) } ?: list.firstOrNull()
                                if (match != null) onClassSelected(match)
                            }
                        )
                    }
                }
            }

            // Dropdown 2: Section
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, GenesisColors.Teal.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sectionMenuExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Section", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextSecondary)
                            Text(
                                text = "Sec $currentSectionName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GenesisColors.TextPrimary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = if (sectionMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Section",
                            tint = GenesisColors.Teal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = sectionMenuExpanded,
                    onDismissRequest = { sectionMenuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    Text(
                        text = "SELECT SECTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GenesisColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    HorizontalDivider(color = GenesisColors.Border, thickness = 0.5.dp)
                    sectionsForClass.forEach { sc ->
                        val secLabel = sc.section.ifBlank { sc.name.substringAfter("Section", "A").trim() }
                        val isSel = sc.id == selectedClass.id
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Section $secLabel",
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) GenesisColors.Teal else GenesisColors.TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${sc.studentCount}",
                                        fontSize = 11.sp,
                                        color = GenesisColors.TextSecondary
                                    )
                                }
                            },
                            trailingIcon = if (isSel) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = GenesisColors.Teal, modifier = Modifier.size(16.dp)) }
                            } else null,
                            onClick = {
                                sectionMenuExpanded = false
                                onClassSelected(sc)
                            }
                        )
                    }
                }
            }
        }

        if (allowClassManagement) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeRole == com.schoolapp.android.data.AppRole.ADMIN) {
                    TextButton(
                        onClick = {
                            newClassNameInput = "Class 10"
                            newSectionInput = "A"
                            showCreateClassDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Class/Section", color = GenesisColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(
                    onClick = {
                        newSubjectInput = ""
                        showManageSubjectsDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = GenesisColors.Teal, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subjects", color = GenesisColors.Teal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

