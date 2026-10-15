> Güncel kaynak ve APK: Bookmuse 0.8.1/code16, aynı package/imza, SQLite7; [arayüz sadeleştirme raporu](acceptance/reports/ARAYUZ-SADELESTIRME.md). Universal ARM64+x86_64, minAPI24/target36 değişmedi. Aşağıdaki eski UX-MVP-6 ölçümleri tarihsel kanıttır; 0.8.1 için fiziksel Huawei/2 GB performans kabulü ayrıca açıktır.

# Destek ve doğrulama matrisi

15 Ekim 2026. Ürün Android 8+ hedefler; toolchain minimumu API 24'tür.
Bir APK'nın ABI içermesi o cihazda çalışmasının doğrulandığı anlamına gelmez.

| Ortam | Paket | Kanıt | Durum |
|---|---|---|---|
| Huawei MatePad SE AGS5-W09, kullanıcı beyanı HarmonyOS 3.0 / 2 GB | Gerçek ABI ölçümünden sonra seçilecek | Fiziksel cihaz bağlı değil; API/ABI/RAM ölçülmedi | Kurulum, gesture, FPS/PSS ve uzun oturum kabulü açık |
| Android 16 / API 36 x86_64 özel emülatör, ölçülen 3920 MiB RAM | Universal ARM64+x86_64 | Gerçek imzalı release kurulumu, PDFium/JNI, SAF, kayıt ve güncelleme testleri | İşlevsel kanıt; Huawei veya 2 GB performans kanıtı değil |
| ARM64 API 24+ | ARM64 veya universal | Derleme, imza, R8/DEX/JNI, ELF ve ZIP 16 KB statik kontrolleri | Fiziksel çalışma doğrulanmadı |
| Gerçek 16 KB sayfa boyutlu Android | Uygun ARM64 APK | Native dosya hizalaması statik kontrol edildi | 16 KB runtime testi yapılmadı |
| armeabi-v7a / x86 32 bit | Teslim APK'larında yok | Üretilmiş ve test edilmiş çıktı yok | Bu teslimde destek iddiası yok |
| iOS / EPUB / OCR / bulut | Yok | V1 kapsamı dışında | Desteklenmiyor |

Dosyalar yerel özel kopyaya alınır. Sistem seçicisi ve content URI paylaşımı
kullanılır; geniş depolama izni yoktur. GMS/HMS, hesap, internet, Metro ve sunucu
gerekmez. Parola isteyen PDF bu sürümde desteklenmez. Metin katmanı olmayan
taramada gerçek kelime seçimi yerine bölge işareti/sayfa notu kullanılır.

Final testleri İndirilenler’deki 10 mevcut PDF’ye ek olarak sentetik 80 ve 1000
sayfalı kitapları, metinsiz taramayı, geniş/çok sütunlu/döndürülmüş sayfaları ve
bozuk PDF reddini kapsar. Bütün bu dosyalar gerçek dosya seçicisinden açılır.
Yüksek çözünürlük tarama, ağır vektör/RTL, SD/uzak sağlayıcı, gerçek disk dolması,
iki OS geri kenarının tam matrisi ve fiziksel TalkBack testleri açık kalır.
Bu sınırlara rağmen bütün senaryoların geçtiği iddia edilmez.

Güncel sürüm/hash ve kabul durumu [UX-MVP-6 raporunda](acceptance/reports/UX-MVP-6.md);
kurulum/yedekleme [kullanım belgesinde](KULLANIM_VE_KURTARMA.md).
