# UX-MVP-5 — sıkıştırarak üç sütunlu sayfalar

15 Ekim 2026. Kaynak, imzalı paket ve gezinme senaryoları tamamlandı. Görsel incelemede bulunan önizleme taşması UX-MVP-6'da düzeltilir; bu artifact tek başına görsel kabul edilmiş değildir. Fiziksel Huawei kabulü açık.

Tek native pinch koordinatörü 1× altındaki sanal mesafeyi izler: ek%18 hazırlama, %10 üstüne açılınca iptal ve parmak bırakmada açma. Android ScaleGestureDetector başlangıç slop'u toplam mesafeye katılır. Sayfalar simgesi aynı tam pencere görünümünü açar. Üç sütunlu liste satırları sanallaştırılır; yalnız görünür satır küçük resimleri etkinleştirilir. Mevcut satıra konumlanır. Kaydırma/geri dönüş, sayfa veya zoom komutu üretmez. Hedef seçimi kapanıştan sonra yapılır; kalıcı konum native presentation sonucu yazılır.

Üst bölümde kapat/numarayla git/yer imleri; yardımcı bölümde önceki-sonraki ve zoom/pan düğmeleri bulunur. Kayıt listeleri burada yoktur. Ortak Ayarlar'daki yardımcı kontrol tercihi korunur; yeni alanı içermeyen eski v1/v2 tercih payload'ları okunmaya devam eder.

95 native host, 30 Jest, TypeScript ve ESLint geçti. Uzun listeyi tamamen render etmeme, yalnız kapanıştan sonra seçim, çift basma ve placeholder'dan seçim senaryoları eklendi. Emülatörde 1×'e yalnız dönüş açmadı, ek pinch üç sütun/current-row erişilebilirlik geometrisini açtı, kaydırıp kapatmak45'i korudu, numarayla60'a gidince kart `(60/80)` oldu (`UX-MVP-5-functional.json`).

**Görsel regresyon:** Erişilebilirlik geometrisi doğru olsa da küçük resim native View'inin `Canvas.drawColor` çağrısı kendi sınırını aşarak diğer hücreleri kapattı. İlk UI senaryosunun geçmesi bu görselin doğru olduğu anlamına gelmedi. Son ekran incelemesi hatayı yakaladı. UX-MVP-6 native thumbnail/PDF canvas'larını kendi View sınırlarına clip eder; final ekranlar yeniden doğrulanır. UX-MVP-5 görüntüsü final kanıtı olarak kullanılmaz.

Artifact (tarihsel, final önerisi değildir): `artifacts/ux-mvp-5/kitap-uxmvp5-universal.apk`, 0.7.5/code14, 42.207481 MiB. SHA-256: `93a12bc15e1eb5494977be64e901879f8f99530966569ad21f8e18ffb761eb9f`. Aynı imza, shrunk JNI/DEX, ABI ve statik16 KB kontrolleri geçti. Bin sayfalı gerçek PDF/görsel/erişilebilirlik matrisi finalde genişletilir.

Güncel birleşik sonuç ve bu aşamanın final sınırları [UX-MVP-6 raporundadır](UX-MVP-6.md). Önceki artifact tarihsel olarak korunur; güncel önerilen APK0.8.0/code15’tir.
