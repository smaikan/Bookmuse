# UX-MVP-3 — sarı fosforlu işaretler

15 Ekim 2026. Kaynak, host ve imzalı release paket kontrolleri geçti; gerçek emülatör seçim/not/yeniden açılış akışı sürüyor. Fiziksel Huawei kabulü açık.

SQLite 6→7 geçişi eski underline/renkli highlight kayıtlarını `highlight/#FFEB3B` biçimine dönüştürür. Değişen revision artırılır; kimlik/alıntı/quad/tarih/not bağlantıları korunur. Eski stil yerel kurtarma tablosundadır. Region kayıtları korunur. Migration transaction kapsamındadır; hata halinde veritabanı sıfırlanmaz. Yeni metin işaretleri ve pasaj notu işaretleri aynı sarı stile normalize edilir. Eski v1/v2 yedekler karşılaştırmadan önce normalize edilir.

İşaretler PDF'nin renk filtresinden sonra %32 alfa ile çizilir. Satır quad'ları tek winding path içine alınır; örtüşen şeritlere tekrar tekrar alfa uygulanmaz. Orijinal PDF dosyasına yazılmaz. Seçim araçları Sarıyla işaretle / Kopyala / Pasaja not ekle / Seçimi kapat olarak sadeleşti.

89 native host testi ve 27 Jest testi, TypeScript geçti. Özellikle 6→7 veri/not/quad/tarih koruma, tekrar normalizasyon, migration hatasıyla rollback ve stil değişse bile idempotent kayıt testleri eklendi. Shrunk DEX/JNI, aynı imza, ARM64/x86_64 ve ELF/ZIP 16 KB statik kontrolleri geçti.

Artifact: `artifacts/ux-mvp-3/kitap-uxmvp3-universal.apk`, 0.7.3/code12, 42.141556 MiB. SHA-256: `ab009accfdb20b03226da422798a4d7b47b951a0902efb1f710a64927321fa0b`.

U3-03/08/10 veri kuralları host testleriyle desteklenir. U3-01/02/04/05/06/07/09 için emülatör corpus/zoom/yön/tema senaryoları tamamlanmadan geçti denmez. Çoklu sütun/rotated metin geometrisi mevcut TextGeometry testleriyle desteklenir, görsel corpus kapsamı finalde genişletilir.

Emülatörde gerçek uzun basma → Sarıyla işaretle → pasaj seçimi → not kaydet → kitabı kapat/aç akışı geçti. Gece temasında yeniden açılmış sayfanın iki sarı şeridi görüntüde doğrulandı (617 sarı tonlu piksel; bu sayısal kontrol performans ölçümü değildir). Kayıtlar 4 işaret/4 nota çıktı. Önceki işaretlerin kimlikleri, alıntıları, fiziksel sayfaları ve tarihleri korundu; bütün metin stilleri kanonik sarı oldu. `UX-MVP-3-functional.json` gerçek sonuçları içerir. İlk yeniden açılış görüntüsü yükleme sırasında alındığından görsel kanıt sayılmadı; hazır görüntüyle tekrar doğrulandı.

Sonraki UX-MVP-4'te swipe boyunca current/neighbor/departing overlay sahipliği birlikte taşınır ve overlay belleği gerçek quad sayısına göre rezerve edilir. Yedekten gelen eski stillerin kurtarma kayıtlarını canlı DB'ye taşıma testi de bu dilimde genişletilir. UX-MVP-3 tek başına bütün UX planının kabulü değildir.

Güncel birleşik sonuç ve bu aşamanın final sınırları [UX-MVP-6 raporundadır](UX-MVP-6.md). Önceki artifact tarihsel olarak korunur; güncel önerilen APK0.8.0/code15’tir.
