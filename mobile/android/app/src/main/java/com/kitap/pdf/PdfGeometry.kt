package com.kitap.pdf

internal object PdfGeometry {
  // The existing appmodules library is loaded by RN. This bridge loads no second engine.
  external fun glyphs(page: io.legere.pdfiumandroid.PdfPage, text: io.legere.pdfiumandroid.PdfTextPage, count: Int): FloatArray?
}
