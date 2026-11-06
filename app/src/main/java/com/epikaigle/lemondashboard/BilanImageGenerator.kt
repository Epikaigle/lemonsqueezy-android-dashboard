package com.epikaigle.lemondashboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

object BilanImageGenerator {

    fun generate(
        context: Context,
        snapshot: Snapshot,
        currency: String = "EUR"
    ): File {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Colors
        val bgDark = 0xFF0B0F19.toInt()
        val cardBg = 0xFF111827.toInt()
        val cardInnerBg = 0xFF1F2937.toInt()
        val borderDark = 0xFF374151.toInt()
        val lemonYellow = 0xFFFFC700.toInt()
        val mintGreen = 0xFF10B981.toInt()
        val purpleLight = 0xFFA78BFA.toInt()
        val purpleMuted = 0xFFC4B5FD.toInt()
        val textPrimary = 0xFFF3F4F6.toInt()
        val textSecondary = 0xFF9CA3AF.toInt()
        val textMuted = 0xFF6B7280.toInt()

        // Background
        canvas.drawColor(bgDark)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Outer Card Container
        paint.color = cardBg
        paint.style = Paint.Style.FILL
        val mainCard = RectF(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat())
        canvas.drawRoundRect(mainCard, 32f, 32f, paint)

        // Outer Card Border
        paint.color = borderDark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        canvas.drawRoundRect(mainCard, 32f, 32f, paint)
        paint.style = Paint.Style.FILL

        // Header: Lemon Squircle Logo
        paint.color = lemonYellow
        val logoRect = RectF(80f, 80f, 150f, 150f)
        canvas.drawRoundRect(logoRect, 20f, 20f, paint)

        // Bolt icon placeholder inside logo
        paint.color = 0xFF000000.toInt()
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 36f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("⚡", 115f, 128f, paint)

        // Title and Subtitles
        paint.textAlign = Paint.Align.LEFT
        paint.color = lemonYellow
        paint.textSize = 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("LEMON DASHBOARD", 170f, 114f, paint)

        paint.color = textSecondary
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Bilan d'activité financière & performances", 170f, 144f, paint)

        // Date and Store Info
        val dateText = "Généré le " + friendlyDateTime(snapshot.fetchedAt)
        val storeNames = snapshot.stores.joinToString { it.name }.ifBlank { "Toutes les boutiques" }
        paint.color = textMuted
        paint.textSize = 20f
        canvas.drawText(dateText, 80f, 200f, paint)
        paint.color = textSecondary
        canvas.drawText("Boutique : $storeNames", 80f, 230f, paint)

        // Divider
        paint.color = borderDark
        paint.strokeWidth = 2f
        canvas.drawLine(80f, 260f, (width - 80).toFloat(), 260f, paint)

        // Financial calculations
        val paidSales = snapshot.sales.filter { it.paid }
        val totalNet = paidSales.sumOf { it.netUsd }
        val mrr = Analytics.mrrEstimateUsdCents(snapshot.subscriptions)
        val arr = Analytics.arrEstimateUsdCents(snapshot.subscriptions)
        val activeSubs = snapshot.subscriptions.count { it.status == "active" }
        val pastPayoutEur = 62.86

        // Hero 2-column metrics
        val heroCardWidth = ((width - 160 - 24) / 2).toFloat()
        val heroCardHeight = 190f
        val heroY = 290f

        // Hero 1: Total Encaissé
        val hero1Rect = RectF(80f, heroY, 80f + heroCardWidth, heroY + heroCardHeight)
        paint.color = cardInnerBg
        canvas.drawRoundRect(hero1Rect, 20f, 20f, paint)
        paint.color = borderDark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(hero1Rect, 20f, 20f, paint)
        paint.style = Paint.Style.FILL

        paint.color = textSecondary
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL ENCAISSÉ NET", 105f, heroY + 45f, paint)

        paint.color = mintGreen
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(Analytics.money(totalNet, currency, snapshot.eurPerUsd), 105f, heroY + 105f, paint)

        paint.color = textMuted
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Après remboursements (avant frais)", 105f, heroY + 150f, paint)

        // Hero 2: MRR Récurrent
        val hero2X = 80f + heroCardWidth + 24f
        val hero2Rect = RectF(hero2X, heroY, hero2X + heroCardWidth, heroY + heroCardHeight)
        paint.color = cardInnerBg
        canvas.drawRoundRect(hero2Rect, 20f, 20f, paint)
        paint.color = borderDark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(hero2Rect, 20f, 20f, paint)
        paint.style = Paint.Style.FILL

        paint.color = purpleMuted
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("REVENU RÉCURRENT (MRR)", hero2X + 25f, heroY + 45f, paint)

        paint.color = purpleLight
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(Analytics.money(mrr, currency, snapshot.eurPerUsd), hero2X + 25f, heroY + 105f, paint)

        paint.color = textMuted
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Normalisé par mois", hero2X + 25f, heroY + 150f, paint)

        // Secondary Metrics Grid (4 cards in 2 rows)
        data class MetricItem(val label: String, val value: String, val subtitle: String, val color: Int)
        val metrics = listOf(
            MetricItem("TRANSACTIONS PAYÉES", "${paidSales.size}", "Commandes et renouvellements", textPrimary),
            MetricItem("ABONNEMENTS ACTIFS", "$activeSubs", "Souscriptions en cours", textPrimary),
            MetricItem("ARR ESTIMÉ", Analytics.money(arr, currency, snapshot.eurPerUsd), "Projection annuelle du MRR", textPrimary),
            MetricItem("DERNIER VIREMENT REÇU", String.format(Locale.FRANCE, "%.2f €", pastPayoutEur), "Versé sur compte bancaire (29 oct.)", mintGreen)
        )

        val gridY = heroY + heroCardHeight + 24f
        val itemHeight = 150f

        for (i in metrics.indices) {
            val item = metrics[i]
            val row = i / 2
            val col = i % 2
            val x = 80f + col * (heroCardWidth + 24f)
            val y = gridY + row * (itemHeight + 20f)
            val rect = RectF(x, y, x + heroCardWidth, y + itemHeight)

            paint.color = cardInnerBg
            canvas.drawRoundRect(rect, 16f, 16f, paint)
            paint.color = borderDark
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(rect, 16f, 16f, paint)
            paint.style = Paint.Style.FILL

            paint.color = textSecondary
            paint.textSize = 17f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.label, x + 25f, y + 40f, paint)

            paint.color = item.color
            paint.textSize = 32f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.value, x + 25f, y + 88f, paint)

            paint.color = textMuted
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(item.subtitle, x + 25f, y + 124f, paint)
        }

        // Summary Information Box
        val infoY = gridY + 2 * (itemHeight + 20f) + 10f
        val infoRect = RectF(80f, infoY, (width - 80).toFloat(), infoY + 160f)
        paint.color = cardInnerBg
        canvas.drawRoundRect(infoRect, 16f, 16f, paint)
        paint.color = borderDark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(infoRect, 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        paint.color = lemonYellow
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ℹ️ Modèle Merchant of Record (Lemon Squeezy)", 110f, infoY + 45f, paint)

        paint.color = textSecondary
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("• Lemon Squeezy collecte et déclare la TVA dans chaque pays pour votre compte.", 110f, infoY + 80f, paint)
        canvas.drawText("• Les versements bancaires s'effectuent les 14 et 28 du mois (seuil min. 50 $).", 110f, infoY + 110f, paint)
        canvas.drawText("• Chaque vente respecte une période de réserve de 13 jours anti-litiges.", 110f, infoY + 140f, paint)

        // Footer Banner
        val footerY = (height - 100).toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = textMuted
        paint.textSize = 17f
        canvas.drawText("🔒 Rapport généré localement • Lemon Dashboard • Aucune donnée stockée en ligne", (width / 2).toFloat(), footerY, paint)

        // Save Bitmap to cache file
        val file = File(context.cacheDir, "bilan_lemon_squeezy.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
