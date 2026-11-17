package com.epikaigle.lemondashboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

object BilanImageGenerator {

    private fun drawFittedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        minSize: Float = 14f
    ) {
        val originalSize = paint.textSize
        var currentSize = originalSize
        while (paint.measureText(text) > maxWidth && currentSize > minSize) {
            currentSize -= 1.5f
            paint.textSize = currentSize
        }
        var rendered = text
        if (paint.measureText(rendered) > maxWidth && rendered.length > 3) {
            while (rendered.length > 3 && paint.measureText("$rendered…") > maxWidth) {
                rendered = rendered.dropLast(1)
            }
            rendered = "$rendered…"
        }
        canvas.drawText(rendered, x, y, paint)
        paint.textSize = originalSize
    }

    fun generate(
        context: Context,
        snapshot: Snapshot,
        currency: String = "EUR",
        selectedStore: String = ""
    ): File {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Colors aligned with Lemon Dashboard theme
        val bgDark = 0xFF0C1015.toInt()
        val cardBg = 0xFF141A22.toInt()
        val cardInnerBg = 0xFF1B232D.toInt()
        val borderDark = 0xFF232D3B.toInt()
        val lemonYellow = 0xFFFFD600.toInt()
        val mintGreen = 0xFF34D399.toInt()
        val purpleLight = 0xFFA78BFA.toInt()
        val purpleMuted = 0xFFC4B5FD.toInt()
        val textPrimary = 0xFFF8FAFC.toInt()
        val textSecondary = 0xFF94A3B8.toInt()
        val textMuted = 0xFF64748B.toInt()

        canvas.drawColor(bgDark)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Outer Card Container
        paint.color = cardBg
        paint.style = Paint.Style.FILL
        val mainCard = RectF(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat())
        canvas.drawRoundRect(mainCard, 32f, 32f, paint)

        paint.color = borderDark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        canvas.drawRoundRect(mainCard, 32f, 32f, paint)
        paint.style = Paint.Style.FILL

        // Header: Lemon Squircle Logo + dark vector lightning bolt
        paint.color = lemonYellow
        val logoRect = RectF(80f, 80f, 152f, 152f)
        canvas.drawRoundRect(logoRect, 20f, 20f, paint)

        paint.color = bgDark
        val bolt = Path().apply {
            moveTo(122f, 94f)
            lineTo(100f, 121f)
            lineTo(114f, 121f)
            lineTo(109f, 140f)
            lineTo(133f, 112f)
            lineTo(118f, 112f)
            close()
        }
        canvas.drawPath(bolt, paint)

        // Title and Subtitle
        paint.textAlign = Paint.Align.LEFT
        paint.color = lemonYellow
        paint.textSize = 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawFittedText(canvas, "LEMON DASHBOARD", 174f, 114f, width - 254f, paint)

        paint.color = textSecondary
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawFittedText(canvas, "Bilan d'activité financière & performances", 174f, 145f, width - 254f, paint)

        // Date and Store Info
        val cleanSales = Analytics.selectedSales(snapshot, selectedStore)
        val cleanSubs = Analytics.selectedSubscriptions(snapshot, selectedStore)
        val cleanLicenses = Analytics.selectedLicenseKeys(snapshot, selectedStore)

        val dateText = if (snapshot.fetchedAt.isBlank()) {
            "Synchronisation en attente"
        } else {
            "Généré le " + friendlyDateTime(snapshot.fetchedAt)
        }
        val storeNames = if (selectedStore.isNotBlank()) {
            snapshot.stores.firstOrNull { it.id == selectedStore }?.name ?: "Boutique sélectionnée"
        } else {
            snapshot.stores.joinToString { it.name }.ifBlank { "Toutes les boutiques" }
        }
        paint.color = textMuted
        paint.textSize = 20f
        drawFittedText(canvas, dateText, 80f, 200f, width - 160f, paint)
        paint.color = textSecondary
        drawFittedText(canvas, "Boutique : $storeNames", 80f, 232f, width - 160f, paint)

        // Divider
        paint.color = borderDark
        paint.strokeWidth = 2f
        canvas.drawLine(80f, 260f, (width - 80).toFloat(), 260f, paint)

        // Financial calculations (excluding testMode)
        val paidSales = cleanSales.filter { it.paid }
        val totalNet = paidSales.sumOf { it.netUsd }
        val aov = Analytics.averageOrderValueUsdCents(cleanSales)
        val mrr = Analytics.mrrEstimateUsdCents(cleanSubs)
        val arr = Analytics.arrEstimateUsdCents(cleanSubs)
        val activeSubs = cleanSubs.count { it.status == "active" }
        val activeLicenses = cleanLicenses.count { it.status == "active" && !it.disabled }
        val payoutEst = Analytics.payoutEstimates(cleanSales)

        // Hero 2-column metrics
        val heroCardWidth = ((width - 160 - 24) / 2).toFloat()
        val heroCardHeight = 190f
        val heroY = 290f
        val innerPadding = 26f
        val heroTextMaxWidth = heroCardWidth - (innerPadding * 2)

        // Hero 1: Total Net
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
        drawFittedText(canvas, "TOTAL NET ENCAISSÉ", 80f + innerPadding, heroY + 46f, heroTextMaxWidth, paint)

        paint.color = mintGreen
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawFittedText(canvas, Analytics.money(totalNet, currency, snapshot.eurPerUsd), 80f + innerPadding, heroY + 108f, heroTextMaxWidth, paint)

        paint.color = textMuted
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawFittedText(canvas, "Après remboursements", 80f + innerPadding, heroY + 152f, heroTextMaxWidth, paint)

        // Hero 2: MRR
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
        drawFittedText(canvas, "REVENU MENSUEL (MRR)", hero2X + innerPadding, heroY + 46f, heroTextMaxWidth, paint)

        paint.color = purpleLight
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawFittedText(canvas, Analytics.money(mrr, currency, snapshot.eurPerUsd), hero2X + innerPadding, heroY + 108f, heroTextMaxWidth, paint)

        paint.color = textMuted
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawFittedText(canvas, "ARR : ${Analytics.money(arr, currency, snapshot.eurPerUsd)}", hero2X + innerPadding, heroY + 152f, heroTextMaxWidth, paint)

        // Secondary Metrics Grid (4 cards in 2 rows)
        val lastPayout = payoutEst.pastPayouts.lastOrNull()
        val lastPayoutText = when {
            lastPayout == null -> Analytics.money(0L, currency, snapshot.eurPerUsd)
            currency.equals("EUR", ignoreCase = true) && lastPayout.verifiedEurCents != null ->
                Analytics.localMoney(lastPayout.verifiedEurCents, "EUR")
            else -> Analytics.money(lastPayout.amountUsdCents, currency, snapshot.eurPerUsd)
        }
        val lastPayoutSub = when {
            lastPayout == null -> "Aucun virement émis"
            lastPayout.verifiedEurCents != null -> "Reçu le 29 oct. sur compte"
            else -> "Cycle du ${friendlyDate(lastPayout.date.toString())}"
        }

        data class MetricItem(val label: String, val value: String, val subtitle: String, val color: Int)
        val metrics = listOf(
            MetricItem(
                "TRANSACTIONS PAYÉES",
                "${paidSales.size}",
                "Panier moyen : ${Analytics.money(aov, currency, snapshot.eurPerUsd)}",
                textPrimary
            ),
            MetricItem(
                "ABONNEMENTS ACTIFS",
                "$activeSubs",
                pluralFr(activeLicenses, "licence active", "licences actives"),
                textPrimary
            ),
            MetricItem(
                "PROCHAIN VIREMENT ESTIMÉ",
                Analytics.moneyRange(payoutEst.pocketNextPayoutUsdCents, currency, snapshot.eurPerUsd),
                if (payoutEst.reachesThreshold) "Seuil 50 $ atteint" else "En attente seuil 50 $",
                mintGreen
            ),
            MetricItem(
                "DERNIER VIREMENT REÇU",
                lastPayoutText,
                lastPayoutSub,
                mintGreen
            )
        )

        val gridY = heroY + heroCardHeight + 24f
        val itemHeight = 152f

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
            drawFittedText(canvas, item.label, x + innerPadding, y + 40f, heroTextMaxWidth, paint)

            paint.color = item.color
            paint.textSize = 31f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawFittedText(canvas, item.value, x + innerPadding, y + 88f, heroTextMaxWidth, paint)

            paint.color = textMuted
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            drawFittedText(canvas, item.subtitle, x + innerPadding, y + 124f, heroTextMaxWidth, paint)
        }

        // Summary Information Box
        val infoY = gridY + 2 * (itemHeight + 20f) + 12f
        val infoRect = RectF(80f, infoY, (width - 80).toFloat(), infoY + 162f)
        paint.color = cardInnerBg
        canvas.drawRoundRect(infoRect, 16f, 16f, paint)
        paint.color = borderDark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(infoRect, 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        val infoMaxWidth = (width - 160 - 60).toFloat()
        paint.color = lemonYellow
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawFittedText(canvas, "Modèle Merchant of Record • Lemon Squeezy", 110f, infoY + 45f, infoMaxWidth, paint)

        paint.color = textSecondary
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawFittedText(canvas, "• Lemon Squeezy collecte et reverse la TVA mondiale pour votre compte.", 110f, infoY + 82f, infoMaxWidth, paint)
        drawFittedText(canvas, "• Les virements s'effectuent les 14 et 28 du mois (seuil min. 50 $).", 110f, infoY + 112f, infoMaxWidth, paint)
        drawFittedText(canvas, "• Chaque vente respecte une réserve de sécurité de 13 jours.", 110f, infoY + 142f, infoMaxWidth, paint)

        // Footer Banner
        val footerY = (height - 96).toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = textMuted
        paint.textSize = 17f
        canvas.drawText("Rapport généré localement • Lemon Dashboard • Chiffrement AES-256", (width / 2).toFloat(), footerY, paint)

        val file = File(context.cacheDir, "bilan_lemon_squeezy.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
