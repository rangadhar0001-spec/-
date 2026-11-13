package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.LessonPlan
import com.example.domain.model.NepStage
import com.example.domain.model.PanchapadiConfig
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun exportAndShareLessonPlan(
        context: Context,
        plan: LessonPlan,
        config: PanchapadiConfig,
        lang: AppLanguage
    ): Result<File> {
        return runCatching {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // A4 points
            val pageHeight = 842
            val margin = 40f
            val contentWidth = (pageWidth - margin * 2).toInt()

            val oriyaTypeface = ResourcesCompat.getFont(context, R.font.noto_sans_oriya) ?: Typeface.DEFAULT

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#085C66")
                textSize = 18f
                typeface = Typeface.create(oriyaTypeface, Typeface.BOLD)
            }
            val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B45309")
                textSize = 13f
                typeface = Typeface.create(oriyaTypeface, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1A2226")
                textSize = 11f
                typeface = oriyaTypeface
            }
            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#526068")
                textSize = 10f
                typeface = oriyaTypeface
            }

            var pageNum = 1
            var page = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var canvas = page.canvas
            var y = margin

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > pageHeight - margin) {
                    pdfDoc.finishPage(page)
                    pageNum++
                    page = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = margin
                }
            }

            fun drawWrappedText(text: String, paint: TextPaint, extraSpaceAfter: Float = 8f) {
                if (text.isBlank()) return
                val layout = StaticLayout.Builder
                    .obtain(text, 0, text.length, paint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1.15f)
                    .build()

                ensureSpace(layout.height + extraSpaceAfter)
                canvas.save()
                canvas.translate(margin, y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height + extraSpaceAfter
            }

            // Header Banner
            val appHeader = if (lang == AppLanguage.ODIA) {
                "ଓଡ଼ିଆ ଶିକ୍ଷା ସାଥୀ — NEP 2020 ପଞ୍ଚପଦୀ ପାଠ୍ୟ ଯୋଜନା"
            } else {
                "Odia Sikshya Sathi — NEP 2020 Panchapadi Lesson Plan"
            }
            drawWrappedText(appHeader, metaPaint, 4f)
            drawWrappedText(plan.title, titlePaint, 6f)

            val stageInfo = NepStage.fromKey(plan.stage)
            val dateStr = DateUtils.formatDate(plan.planDay.toInt(), plan.planMonth.toInt(), plan.planYear.toInt(), lang)
            val metaLine = "${stageInfo.odiaName} | ${plan.classLevel} | ${plan.subject} | ${plan.medium}\n" +
                "ତାରିଖ (Date): $dateStr | ସମୟ (Duration): ${plan.durationMinutes} min | ଶିକ୍ଷକ: ${plan.authorName}"
            drawWrappedText(metaLine, metaPaint, 8f)

            if (plan.isAiGenerated && !plan.isVerified) {
                drawWrappedText(
                    "⚠ AI ଡ୍ରାଫ୍ଟ (AI-Generated Draft) — ଶ୍ରେଣୀରେ ବ୍ୟବହାର ପୂର୍ବରୁ ଶିକ୍ଷକଙ୍କ ସମୀକ୍ଷା ଆବଶ୍ୟକ",
                    headerPaint,
                    10f
                )
            }

            val sections = listOf(
                "ଶିକ୍ଷଣ ଉଦ୍ଦେଶ୍ୟ (Learning Objectives)" to plan.learningObjectives,
                "ଶିକ୍ଷଣ-ଶିକ୍ଷା ସାମଗ୍ରୀ / TLM" to plan.tlmMaterials,
                "ଶିକ୍ଷକଙ୍କ କାର୍ଯ୍ୟ (Teacher Activities)" to plan.teacherActivities,
                "ଛାତ୍ରଛାତ୍ରୀଙ୍କ କାର୍ଯ୍ୟ (Student Activities)" to plan.studentActivities,
                "${config.aditiLabel} (${plan.aditiMinutes} min)" to plan.aditiStep,
                "${config.bodhaLabel} (${plan.bodhaMinutes} min)" to plan.bodhaStep,
                "${config.abhyasaLabel} (${plan.abhyasaMinutes} min)" to plan.abhyasaStep,
                "${config.prayogaLabel} (${plan.prayogaMinutes} min)" to plan.prayogaStep,
                "${config.prasaraLabel} (${plan.prasaraMinutes} min)" to plan.prasaraStep,
                "ଦକ୍ଷତା-ଆଧାରିତ ପ୍ରଶ୍ନ (Competency-Based Questions)" to plan.competencyQuestions,
                "ମୂଲ୍ୟାଙ୍କନ ମାନଦଣ୍ଡ (Assessment Criteria)" to plan.assessmentCriteria,
                "ଗୃହକାର୍ଯ୍ୟ ଓ ଉପଚାରାତ୍ମକ/ସମୃଦ୍ଧି କାର୍ଯ୍ୟ (Homework & Remedial)" to plan.homeworkActivities,
                "ସମାବେଶୀ ଶିକ୍ଷଣ ପରାମର୍ଶ (Inclusive Teaching)" to plan.inclusiveSuggestions,
                "ସାରାଂଶ (Summary)" to plan.summary,
                "ଶିକ୍ଷକଙ୍କ ଚିନ୍ତନ (Teacher Reflection)" to plan.teacherReflection
            )

            sections.forEach { (heading, body) ->
                if (body.isNotBlank()) {
                    drawWrappedText(heading, headerPaint, 3f)
                    drawWrappedText(body, bodyPaint, 10f)
                }
            }

            pdfDoc.finishPage(page)

            val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val safeName = plan.topic.replace(Regex("[^a-zA-Z0-9_]"), "_").take(24).ifBlank { "lesson_plan" }
            val outFile = File(pdfDir, "panchapadi_${safeName}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { stream ->
                pdfDoc.writeTo(stream)
            }
            pdfDoc.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, plan.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share / Print Panchapadi PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            outFile
        }
    }
}
