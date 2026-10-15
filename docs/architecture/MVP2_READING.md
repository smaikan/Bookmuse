# MVP-2 okuyucu sözleşmesi

Native PDF View touch-down'da root WindowInsetsCompat systemGestures,
mandatorySystemGestures, systemBars ve cutout alanlarını birlikte kontrol eder.
Root ekran konumu çıkarılır; multiwindow/rotation için sabit piksel kenarı yok.
Insets bulunamazsa gesture başlamaz, sayfa/zoom düğmeleri kullanılabilir.
Sistemin sağ/sol kenarına exclusion rectangle eklenmez. Bir stream pinch/pan/page
rollerinden yalnız birini alır; zoom'da page swipe kapalıdır. ACTION_CANCEL veya
yetersiz sürükleme mevcut sayfaya döner ve konum yazmaz. Ters yönde release fling
önemsiz kısa drag'ı commit ettirmez. GestureDetector/ScaleGestureDetector/Canvas
ve ValueAnimator native'dir; sadece navigate, settled ready, zoom-end ve tools
olayları JS'ye gelir. Geçiş 220 ms; sistem/kullanıcı hareket azaltmada sade.

Görünen fit bitmap + isteğe bağlı tek yön komşu; visible iş → tile → prefetch
öncelikli, kapasitesi 16 olan tek worker. PDFium Config.pageRetentionCount=1;
motorun kendi native sayfa cache'i de açıkça sınırlı. Zoom başlayınca komşu bırakılır.
Parmak boyunca fit transform, sakinleşince yalnız viewport ile kesişen 512×512
tile'lar hazırlanır. Tile render büyük canvas ölçüsünü alsa da output yalnız küçük
tile bitmapidir. En fazla 16 görünür tile tutulur; 24 MiB rezerv görünür + üretim
ile birlikte geçerlidir. Bütçeye sığmayan netleştirme fit yüzeyini bozmaz. Tam kitap
veya 5× büyütülmüş tam sayfa bitmapine dönülmez; pixel buffer JS'ye gönderilmez.

PageViewport tam sayfanın normalize gösterim koordinatlarını fit/crop/zoom/pan
ile dönüştürür. Rotation center focus'u korur, pan crop sınırlarına clamp edilir.
PDF user-space/rotation dönüşümü PDFium'a aittir; sonraki annotation aynı zinciri
kullanacaktır. Bitmap bırakmak GPU/RenderThread referansının anında kapanması
değildir; total/peak PSS, frame ve growth gerçek cihazda ayrı ölçülür.

SQLite şema 2: book_preferences payload, documentId FK. Şema 1 migration eski
konumu/kimliği silmez. Önizlenen ayar yalnız commit ACK sonrası kalıcı sayılır;
eski save completion yeni ayarın durumunu ezmez. Tema/renk/kontrast aynı bitmapi
Canvas filtresiyle kullanır. Renk resimleri de etkiler ve Orijinal dönüşü vardır.
Kırpma normalize kenar oranıdır; orijinal PDF'ye yazılmaz.

ReaderWindow yalnız Activity penceresinde brightness/screenOn/fullscreen/orientation
uygular. İlk baseline saklanır; çıkış ve background'da geri alınır, resume'da aktif
kitap ayarı tekrar uygulanır. Global sistem parlaklığı veya orientation tercihi
değiştirilmez. Native SeekBar ve düğmeler gesture alternatifi; klavye/büyük font
ayar formunu kaydırabilir. Pencere/görsel davranış ve iki geri kenarının fiziksel
telefon/tablet doğrulaması açık kalır.
