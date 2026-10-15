# MVP-4 — gerçek seçim, geometri ve kalıcı işaretler

14 Ekim 2026. Bir PDFium belge oturumu, tek worker; Fabric yüzey ve küçük
TurboModule komutları korunur. İşaretler orijinal PDF'ye gömülmez. Not/draft
MVP-5; yedek/restore MVP-6 kapsamındadır.

## Native geometri köprüsü

`PdfGeometry.glyphs` actor-owned `PdfPage` ve `PdfTextPage` use kapsamından
çağrılır. JNI pinned 2.0.3 AAR'ın **public JVM** `getPage$pdfiumandroid` ve
core `getPagePtr` accessor'larını kullanır; private alan reflection'ı yoktur.
Pointer JNI çağrısı dışında saklanmaz. R8 kuralları dört accessor/class ve
PdfGeometry'yi korur. JNI appmodules içinde derlenir; RN'nin standart OnLoad'u
aynı CMake hedefinde açıkça dahil edilir. `dlopen` mevcut `libpdfium.so` içindeki
sembolleri çözer; ikinci belge/motor oluşturulmaz.

FPDFText_GetUnicode, GetCharAngle (**float**), GetCharBox, HasUnicodeMapError,
IsGenerated ve FPDF_PageToDevice imzaları public header'a göre bağlandı;
semboller kullanılan ARM64 AAR'da LLVM nm ile kontrol edildi. Wrapper sürümü
değişince getter ABI + header + semboller + release shrink yeniden denetlenir.
Accessor veya sembol yoksa metin seçimi açık hata verir, bölge yolu kalır.
Kotlin metadata check kapatılmadı; compiler internal erişim suppression'ı yoktur.

Kaynaklar: [PDFium text public API](https://pdfium.googlesource.com/pdfium/+/refs/heads/main/public/fpdf_text.h),
[PDFium page mapping](https://pdfium.googlesource.com/pdfium/+/refs/heads/main/public/fpdfview.h).
Main header yalnız ABI dokümantasyonudur; pinned binary export kontrolünün
yerine sürüm uyumu varsayımı değildir.

## Koordinat ve metin sınırları

Glyph başına 19 float: Unicode scalar, durum, açı, sekiz PDF user-space quad
koordinatı, sekiz normalized-display koordinatı. Intrinsic page rotation/non-zero
box dönüşümünü render ile aynı FPDF_PageToDevice rotation=0 yapar. Normalized
geometri fit/crop/zoom/pan için PageViewport üzerinden geçer; ekran pikseli
kalıcı anchor değildir. Ters region dönüşümü aynı PDFium page-map API'sindedir.

Quarter-turn glyph açıları desteklenir; arbitrary skew AABB'den doğru quad
üretilemeyeceği için reddedilir. Eksik Unicode map, U+FFFD/NUL veya boş çıkarım
doğru alıntı diye kaydedilmez. Unicode scalar/PDFium indeksleri UTF-16 offset
olarak kullanılmaz; alıntı gerçek textPageGetText range'iyle alınır. PDF'nin
kendi okuma sırası korunur; ligature/çok sütun/RTL için preview kullanıcıya
gösterilir, tüm PDF'lerde semantik sıra doğruluğu garanti edilmez.

Merge aynı baseline, açı ve yakın boşluklar için yapılır. Farklı satır veya
geniş sütun aralığı bölünür. Karar PDF biriminde, display envelope baseline
yönü korunarak verilir; y inversion testi vardır. `satır parçası` hesaplanmış
geometri bilgisidir; basılı kitap satır numarası değildir.

## Cache ve gesture sahipliği

Text çıkarımı yalnız aktif fiziksel sayfaya ilk uzun basma/düğmede yapılır;
tüm kitap veya her sayfa geçişinde indexing yoktur. 32.000 glyph üstünde açık
sınır + bölge alternatifi. Türetilmiş **4 MiB** ByteBudget metadata, view seçim
preview'i (128 KiB), engine seçim snapshot'ı (128 KiB), page overlay'i (pane
başına 512 KiB) kapsar. JNI staging 128 glyph'tir; ikinci tam native vector yok.
TextGeometry refcount worker borcu bitmeden reservation bırakmaz. Page değişimi,
detach, background unmount ve idle trim metadata'yı bırakır. PDFium'un kendi
text-page/native belleği bu cache sayacı değildir; PSS ayrıca ölçülmelidir.

Long press tanınırken metadata beklese bile page swipe durur. Seçim kulpları
aynı touch stream'in sahibidir; pinch seçimi kapatıp zoom'a geçer. Hem sistem
geri kenarı touch-down'da reddedilir. Kulp hit alanları 48 dp, kök insets/IME/bar
alanı dışına clamp edilir; leader line gerçek quad noktasını gösterir. Düğmeler
metin/region seçimi, kelime aralığı, region boyutu ve zoom pan alternatifi sunar.
Geri, hazır veya hazırlanmakta olan seçimi zoom/araç/kitaplıktan önce kapatır.
Hedef render başarısızken ekranda kalan eski sayfaya yeni text layer bağlanmaz.

## SQLite schema 4 ve commit

SQLiteOpenHelper v1→v4 ve v3→v4 transaction migration; hata reset değildir.
annotations: UUID, document FK, fingerprint, fiziksel page, quote/context,
char range, MediaBox/CropBox/rotation JSON, engine version, tür/renk,
provenance, geometri parça sayısı, tarihler, revision ve deleted tombstone.
annotation_ranges: annotation FK + ordinal + sekiz REAL PDF koordinatı.

Annotation+ranges tek transaction. UUID tekrarında belge/page/quote/style ve
tam anchor eşleşmelidir. Range insert hata testinde annotation da rollback olur.
UI `Kaydedildi` yalnız promise commit ACK sonrası; hata aynı seçim ve aynı
UUID/komutla tekrar edilir. Stale session/selection veya fingerprint reddedilir.
Edit/delete/undo expected revision ile yapılır; stale revision kayıt ezmez.
Tombstone quads'ı korur. MVP-5 notes geldiğinde delete ilişkileri ayrıca explicit
tasarlanmalıdır; şu anda örtük note cascade yoktur çünkü notes tablosu yoktur.

SQL listesi yalnız kitap kapsamındadır, 40'lık sayfalar replace edilir; JS'de
1000 tam alıntı veya quad set'i birikmez. Kart preview en fazla 400 karakter;
tam alıntı talep üzerine detay/kopya için alınır. Literal arama, page/date sırası,
tür ve renk filtreleri vardır. Quote listesi PDF dosyası olmadan erişilebilir;
konuma git başarısızlığı panelde görünür ve kayıtları kaldırmaz.

## Overlay ve dönüş

Visible page ranges worker'da normalized quad'lara çevrilir; Canvas tema filtresi
ve tile'lardan **sonra** underline/highlight/region çizer. Bitmap sayısı işaret
sayısıyla artmaz. Bir sayfa overlay'inde 8192 range üstü açık hata sınırıdır;
liste erişilebilir kalır. 1000 tipik kısa işaret SQL/host senaryosu test edildi;
çok yoğun tek sayfa ve gerçek cihaz gecikmesi henüz kabul edilmedi.

Liste jump öncesi page/normalized center/zoom UI return anchor'ı saklanır.
Mark ilk gerçek quad'ına gidilir; Okumaya dön eski anchor'ı geri yükler.
Resize sırasında stale focus callback son viewport için tekrar denenir.
Focus komutu bir kez uygulanır; normal ileri/geri gezinmede eski jump tekrar
oynatılmaz. Hedef kırpılan bölümdeyse açık açıklama ve `İşaret için kırpmayı
sıfırla` düğmesi sunulur; kırpma tercihi kullanıcının bu eylemiyle güncellenir.
Return anchor oturum içidir; kill sonrası son committed fiziksel sayfa gelir.
Saved işaretlerin hedeflenen tüm PDF corpus'unda görsel doğruluğu, OS kenarları,
TalkBack, R8 sonrası JNI çalışması ve Huawei performansı fiziksel test bekler.

`scripts/inspect-geometry-bridge.py` release DEX class-data tanımlarında beş
JVM metodunu exact signature/public/native bilgisiyle ve libappmodules ELF'inde
JNI export'u doğrular. Bu yalnız string araması değildir; metodun shrink sonrası
tanımlı olduğunu denetler. Yine de accessor çağrısı veya PDFium'un cihazda
çalışmasını kanıtlamaz. `verify-android.ps1 -Milestone mvp-4` bunu otomatik yürütür.
