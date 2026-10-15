# UX-MVP-4 — native sayfa ve zoom koordinasyonu

15 Ekim 2026. Kaynak, imzalı derleme, 94 native host/27 Jest testi ve belirtilen emülatör senaryoları geçti. Fiziksel Huawei kabulü açık.

Tek/çift PDF pane ortak native GestureHost altında çalışır. İki parmak ve pointer bitişi host'a aittir; seçim tek parmak akışını korur. Başlangıç ve ikinci pointer için güncel sistem inset guard'ları kullanılır. Zoom sırasında pan eşlenir. Sayfa geçişi hazır komşu görüntü varsa sürüklenen pikselden devam eder; hazır değilse eski okunabilir sayfa hedef hazırlanırken korunur. Tamamlama mesafesine bağlı animasyon en fazla220 ms'dir. Hızlı girişlerde yeni animasyon kuyruğu biriktirilmez.

Current/neighbor/staged/departing bitmap ve işaret geometrileri aynı generation/sahiplik kurallarıyla taşınır. PDF quad'ları aynı viewport dönüşümünde çizilir. Overlay belleği quad sayısına göre rezerve edilir; seçim başladığında spekülatif işler bırakılır. Yedek restore'u eski stil kurtarma kayıtlarını da korur. Yeni v1 sarı normalize/dedupe/restore testi eklendi.

Emülatörde kısa sürükleme 45'te kaldı; ileri46 ve geri45 geçişleri SQLite konumundan doğrulandı. Gerçek iki parmak stream'i shell test sürücüsüyle enjekte edildi; zoom'da pan ve ikinci parmak kalktıktan sonra kalan parmak hareketi sayfa çevirmedi. Bu sürücü `scripts/android-test/KitapTouch.java` dosyasındadır; uygulamaya paketlenmez ve uygulamaya INJECT_EVENTS izni verilmez. Metro testlerden önce kapatıldı.

Artifact: `artifacts/ux-mvp-4/kitap-uxmvp4-universal.apk`, 0.7.4/code13, 42.205376 MiB. SHA-256: `ca66f2143b0dcdae0622f9a0e740707316854ad1992d3747be27e8a845003527`.

İmza/JNI/DEX, iki ABI ve ELF/ZIP 16 KB statik kontrolleri geçti. İlk derlemedeki Kotlin listener receiver hatası düzeltildi; başarısız build teslim sayılmadı. Raporlanan APK başarılı yeniden derlemedir.

U4-02/04/05 temel etkileşimleri emülatörde doğrulandı. U4-01'in piksel devamlılığı ve süre sınırı host testinde, U4-03 sınırları mevcut PageIndex/Spread/GesturePolicy testlerinde desteklenir. U4-06 native ownership/draw zinciri uygulandı; hareket sırasında görsel matrisi finalde genişletilecek. U4-07/08 için dönüş/background/predictive back ve iki dış kenarın tam matrisi açık. Emülatör sonuçları Huawei, kare zamanlaması veya p95 performans kabulü değildir. Makine kaydı `UX-MVP-4-functional.json`.

Güncel birleşik sonuç ve bu aşamanın final sınırları [UX-MVP-6 raporundadır](UX-MVP-6.md). Önceki artifact tarihsel olarak korunur; güncel önerilen APK0.8.0/code15’tir.
