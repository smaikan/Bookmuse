package com.kitap.pdf

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/** Streaming copy, SHA-256 and rename; bounded RAM independent of the book's size. */
object AtomicPdfCopy {
  data class Result(val file: File, val bytes: Long, val hash: String)
  fun copy(source: InputStream, target: File, operation: ImportOperation): Result {
    val temporary = File(target.parentFile, "${target.name}.part")
    require(!target.exists() && !temporary.exists()) { "Kopya hedefi zaten mevcut; mevcut dosya korundu" }
    var promoted = false
    try {
      operation.attach(source)
      val digest = MessageDigest.getInstance("SHA-256")
      var bytes = 0L
      source.use { input ->
        FileOutputStream(temporary).use { output ->
          val buffer = ByteArray(64 * 1024)
          while (true) {
            operation.check()
            val count = input.read(buffer)
            if (count == -1) break
            if (count == 0) continue
            output.write(buffer, 0, count)
            digest.update(buffer, 0, count)
            bytes += count
          }
          operation.check()
          require(bytes > 0) { "Seçilen dosya boş" }
          output.fd.sync()
        }
      }
      operation.attach(null)
      operation.check()
      check(!target.exists() && temporary.renameTo(target)) { "PDF kopyası tamamlanamadı" }
      promoted = true
      return Result(target, bytes, digest.digest().joinToString("") { "%02x".format(it) })
    } finally {
      temporary.delete()
      if (!promoted) target.delete()
    }
  }
}
