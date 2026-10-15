package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.CancellationException

class AtomicPdfCopyTest {
  private fun directory() = Files.createTempDirectory("kitap-import-test").toFile()

  @Test fun streamIsHashedAndPromotedWithoutTemporaryFile() {
    val dir = directory()
    try {
      val target = File(dir, "book.pdf")
      val result = AtomicPdfCopy.copy(ByteArrayInputStream("abc".toByteArray()), target, ImportOperation())
      assertEquals(3L, result.bytes)
      assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", result.hash)
      assertEquals("abc", target.readText())
      assertFalse(File(dir, "book.pdf.part").exists())
    } finally { dir.deleteRecursively() }
  }

  @Test fun providerFailureLeavesNeitherFinalNorTemporaryFile() {
    val dir = directory()
    try {
      val source = object : InputStream() { override fun read(): Int = throw IOException("Provider disappeared") }
      assertThrows(IOException::class.java) { AtomicPdfCopy.copy(source, File(dir, "book.pdf"), ImportOperation()) }
      assertEquals(0, dir.listFiles()!!.size)
    } finally { dir.deleteRecursively() }
  }

  @Test fun cancellationMidStreamCleansUpAndPreventsCommit() {
    val dir = directory()
    val operation = ImportOperation()
    try {
      val source = object : ByteArrayInputStream(ByteArray(100_000)) {
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
          val count = super.read(buffer, offset, length)
          operation.cancel()
          return count
        }
      }
      assertThrows(CancellationException::class.java) { AtomicPdfCopy.copy(source, File(dir, "book.pdf"), operation) }
      var written = false
      assertThrows(CancellationException::class.java) { operation.commit { written = true } }
      assertFalse(written)
      assertEquals(0, dir.listFiles()!!.size)
    } finally { dir.deleteRecursively() }
  }

  @Test fun emptyFileDoesNotBecomeReady() {
    val dir = directory()
    try {
      assertThrows(IllegalArgumentException::class.java) {
        AtomicPdfCopy.copy(ByteArrayInputStream(ByteArray(0)), File(dir, "book.pdf"), ImportOperation())
      }
      assertEquals(0, dir.listFiles()!!.size)
    } finally { dir.deleteRecursively() }
  }

  @Test fun successfulCommitCannotBeReportedAsCancelled() {
    val operation = ImportOperation()
    var written = false
    operation.commit { written = true }
    assertTrue(written)
    assertFalse(operation.cancel())
    operation.check()
  }

  @Test fun anExistingTargetCannotBeOverwrittenOrRemovedOnFailure() {
    val dir = directory()
    try {
      val target = File(dir, "book.pdf").apply { writeText("original") }
      assertThrows(IllegalArgumentException::class.java) {
        AtomicPdfCopy.copy(ByteArrayInputStream("replacement".toByteArray()), target, ImportOperation())
      }
      assertEquals("original", target.readText())
    } finally { dir.deleteRecursively() }
  }
}
