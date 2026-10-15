# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:
-keep class com.kitap.pdf.PdfGeometry { *; }
# JNI accesses the pinned wrapper's public JVM accessors inside page/text use scopes.
-keep class io.legere.pdfiumandroid.PdfPage { public *** getPage$pdfiumandroid(); }
-keep class io.legere.pdfiumandroid.PdfTextPage { public *** getPage$pdfiumandroid(); }
-keep class io.legere.pdfiumandroid.core.unlocked.PdfPageU { public long getPagePtr(); }
-keep class io.legere.pdfiumandroid.core.unlocked.PdfTextPageU { public long getPagePtr(); }
