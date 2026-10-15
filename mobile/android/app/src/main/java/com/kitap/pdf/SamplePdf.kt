package com.kitap.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

/** Original, non-personal technical corpus, generated on device; not a bundled book. */
object SamplePdf {
  const val EXPECTED = "İstanbul, ışık, şeker, yağmur ve öğle."
  fun create(context: Context): File {
    val destination = File(context.cacheDir, "kitap-mvp0-sample.pdf")
    val temporary = File(context.cacheDir, "kitap-mvp0-sample.tmp")
    val document = PdfDocument()
    try {
      val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
      val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 23f; typeface = Typeface.DEFAULT }
      page.canvas.drawColor(Color.WHITE)
      page.canvas.drawText("Sessiz bir sayfa, uzun bir yolculuk.", 42f, 110f, paint)
      paint.textSize = 20f
      page.canvas.drawText(EXPECTED, 42f, 155f, paint)
      page.canvas.drawText("Bu sayfa motor doğrulaması için üretildi.", 42f, 200f, paint)
      page.canvas.drawText("Kitapların burada, okuma sakin ve sade.", 42f, 245f, paint)
      document.finishPage(page)
      temporary.outputStream().use(document::writeTo)
    } finally { document.close() }
    check(temporary.renameTo(destination)) { "Örnek PDF hazırlanamadı" }
    return destination
  }
}
