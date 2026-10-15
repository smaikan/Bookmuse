# Bookmuse — proje bağlamı ve geliştirme sözleşmesi

Son güncelleme: 15 Ekim 2026. Bu dosya `kitap/` ve altındaki bütün proje çalışmalarına uygulanır. Amaç, sonraki Codex oturumlarının kullanıcı kararlarını ve teknik sınırları yeniden keşfetmeden doğru geliştirme yapmasıdır.

## 1. Mevcut durum ve doğruluk kaynakları

- **Arayüz sadeleştirme kaynak/derleme:** Bookmuse 0.8.1/code16, aynı package/imza, SQLite7; 78 native + 37 Jest, TypeScript ve 0 lint hatası (4 mevcut inline-style uyarısı). İmzalı universal ARM64+x86_64 42,329075 MiB; `artifacts/ui-simplification/kitap-uisimplification-universal.apk`. İmza/JNI/DEX/statik16 KB geçti. Güncellemede ilk 15 kitap/7 işaret/6 not ve ilgili DB satırları aynen korundu; beşli menü, ad/favori/not listeleri, sabit açık ayarlar, gece okuyucu, üçlü sayfa/numara/yer imi ve sentetik kaldırma senaryoları emülatörde yürütüldü. Kaynak, son paket ve ek ekran/audit durumu [güncel raporda](docs/acceptance/reports/ARAYUZ-SADELESTIRME.md); Huawei/FPS-PSS-p95 kabulü açık. Son JS düzeltmesi arama odağındaki ilk menü dokunuşunu `keyboardShouldPersistTaps="handled"` ile korur.

- **15 Ekim son sadeleştirme kararı önce gelir:** Kart ⋮ menüsü tam beş işlem içerir: **Notlar → Altı çizilenler → Favoriye ekle/çıkar → Adı düzenle → Kaldır**. Kullanıcının son düzeltmesi, önceki “yalnız iki seçenek” talebini geçersiz kıldı; ilk üçü kesinlikle kaldırılmaz. İsim mevcut değerle düzenlenir, kaldırma onay ister ve dış orijinal PDF'ye dokunmaz. Bağımsız Kitapları Yönet ve Yedekleme/Depolama ekranları, yönlendirmeleri ve yalnız bunlara ait backup/export kodu kaldırılır. Kitap/import/konum/not/işaret verileri korunur. Ayarlar alt sekme değildir; kitaplık başlığındaki dişli doğrudan tek ekranda Okuma Görünümü ve Ekran seçeneklerini açar. Ayarlar zemini #F5F3ED, kontroller yeşil ve okuma temasından bağımsızdır; Krem seçeneği sunulmaz, diğer temaların native kimlikleri 0/2/3/4 korunur. Eski theme=1 okunabilir fakat yeni seçimde gösterilmez. Üçlü sayfalarda Yardımcı Kontroller ve buna özel durum/komutlar kaldırılır; numarayla git, yer imleri, pinch, geri ve not/işaret konumundan Okumaya dön korunur. SQLite 7 değişmez, sıfırlama/migration yoktur. [Güncel kapsam](docs/ARAYUZ_SADELESTIRME.md). Eski MVP/UX kabul raporları tarihsel kanıttır.


- **Uygulama adı Bookmuse:** Kullanıcı 15 Ekim 2026’da görünen adı Bookmuse olarak seçti. Kaynaktaki release etiketi `Bookmuse`, debug etiketi `Bookmuse Dev`; iOS template display name de aynı markadadır (iOS ürün desteği yoktur). Android package/namespace `com.kitap`, debug `com.kitap.dev`, RN kayıt adı `Kitap`, native modül adları, yedek formatı ve imza korunur. Workspace/teknik proje adları `kitap` kalabilir. Önceki APK’lar korunur ve eski `Kitap` etiketini taşır. Yeni 0.8.1/code16 imzalı universal APK Bookmuse adını taşır; package/imza süreklidir.
- **Bookmuse Android ikonu:** Kullanıcının 1254×1254 PNG’sinden 10 legacy/round mipmap ve 432 px adaptive foreground üretildi; v26 XML ve krem arka plan eklendi. `scripts/update-launcher-icons.ps1` deterministik yeniden üretir; ayrıntı `mobile/assets/branding/README.md`. AAPT2 kaynak compile/link ve boyut/alfa/güvenli alan kontrolü geçti. Kaynak PNG korunur. İkon kaynakları daha sonra 0.8.1/code16 tam APK içine derlendi. Launcher ikonunun fiziksel Huawei görünümü ayrıca doğrulanmalıdır.

- **15 Ekim UX yenilemesi önceliklidir:** Kullanıcı [UX-MVP-1–6 planının](docs/UX_YENIDEN_TASARIM_PLANI.md) tamamının uygulanmasını istedi. İlk yenilemede kapak ızgarası, üç işlem kart menüsü/favoriler, ortak ana Ayarlar; son sadeleştirmede beşli menü ve doğrudan dişli ayarları önceliklidir. Kırpmasız tam ekran, PDF’yi küçültmeyen overlay, bütün eski/yeni metin işaretlerinde #FFEB3B sarı fosforlu görünüm ve over-pinch ile üç sütun overview. Eski per-book görünüm/crop/fit-width ve okuyucudaki kapsamlı panel kararları değişmiştir. Region, kayıtlar ve orijinal PDF korunur; fiziksel kabul ayrı izlenir.
- **UX-MVP-6 kaynak/derleme ve raporlanan emülatör işlev kontrolleri tamamlandı:** 0.8.0/code15, SQLite 7 / yedek v2; ARM64 23,069403 MiB / universal 42,208096 MiB, aynı imza, 95 native + 31 Jest, TypeScript/ESLint/JNI/DEX/statik16 KB geçti. UX5 native boya taşması clip ile düzeltildi. Final 10 Downloads PDF+1000 sayfa/tarama/farklı oran,2/3/4/5 sütun,%200 font, yeni sarı/pasaj/draft/restore/pinch/wipe ve eski veri korunması kontrol edildi. Tam TalkBack/iki OS kenarı/fiziksel Huawei/FPS-PSS-p95/16 KB runtime ve tam V1 kabulü açık; canonical performans INCOMPLETE. [Final rapor ve 50 kriter](docs/acceptance/reports/UX-MVP-6.md), [mimari](docs/architecture/UX_READER.md). Kullanıcı her MVP geçişinde aşama/test sonucunun açıkça bildirilmesini istedi.

- MVP-0/1 APK'ları korundu. MVP-2 imzalı release derlendi: native swipe/zoom/tile, iki OS kenar guard'ı, kalıcı tema/crop/ekran ayarları; 22,8550 MiB ARM64 APK ve 40 host testi geçti. Güncel kanıt [MVP-2 raporunda](docs/acceptance/reports/MVP-2.md); fiziksel Huawei kabulü açık. Kullanıcı MVP-2 bittikten sonra MVP-3 ve MVP-4'ün de yapılmasını açıkça istedi; sırayla bağımsız geliştirme sürer, cihaz kapısı geçmiş sayılmaz. Kullanıcı MVP-4 tesliminden sonra devam et diyerek MVP-5 geliştirmesini başlattı; MVP-6 kaynak/release ve belirtilen host/emülatör genel kontrolleri tamamlandı; fiziksel ve tam V1 kabul kapıları açık.
- MVP-3 kaynak/host/statik/release tamamlandı: 22,9028 MiB ARM64 APK ve 50 host testi; [MVP-3 raporu](docs/acceptance/reports/MVP-3.md). MVP-4 imzalı release derlendi: gerçek selection/underline/highlight, region, kitap başına list/filter/edit/delete/undo/jump-return; 22,9421 MiB ARM64 APK, 75 host testi ve shrunk JNI/DEX kontrolü geçti. [MVP-4 raporu](docs/acceptance/reports/MVP-4.md). Fiziksel kabul tüm aşamalarda açık. Motor/kalıcılık ayrıntısı [MVP4_ANNOTATIONS.md](docs/architecture/MVP4_ANNOTATIONS.md).
- MVP-5 imzalı release derlendi: kitap/sayfa/pasaj notu, Notlar, 650 ms draft/lease/revision ve explicit ilişkili sil/undo; 23,0027 MiB ARM64 APK ve 98 host testi + JNI/DEX/imza/16 KB geçti. [MVP-5 raporu](docs/acceptance/reports/MVP-5.md), [not sözleşmesi](docs/architecture/MVP5_NOTES.md). Fiziksel kabul açık; MVP-6 kaynak/release ve belgelenen emülatör kontrolleri tamamlandı; fiziksel kabul açık.
- MVP-6 teslim: 0.6.2/code 9, ARM64 23,035643 MiB ve universal ARM64+x86_64 42,143085 MiB; 116 host testi, imza/JNI/DEX/16 KB statik kontrolleri geçti. 10 Downloads PDF / 12 sayfa final imzalı APK ile API36 x86_64 emülatörde açıldı; 1000 not/restore çatışmaları, relink, VIEW/SEND, gerçek underline/pasaj notu, draft kurtarma ve 7→8→9 install-r kanıtları raporda. Emülatör RAM 3920 MiB / runtime 4 KB; Huawei/2 GB/16 KB runtime kanıtı değildir. Npm audit iki bağımsız build/test advisory ile temiz değil; release bundle içinde ilgili paketler yok. Canonical performans **INCOMPLETE**. [MVP-6 raporu](docs/acceptance/reports/MVP-6.md), [destek matrisi](docs/DESTEK_MATRISI.md). Sonraki iş fiziksel kabul/tam ağır corpus ve açık güvenlik risklerinin takibi; tam V1 kabulü açık.
- Konfigüre edilmiş sürüm matrisi ve yeniden derleme adımları [MVP0_TECHNOLOGY.md](docs/architecture/MVP0_TECHNOLOGY.md) dosyasındadır. Rapor olmadan derlenmiş APK veya Huawei performans sonucu varmış gibi davranma.
- Ürün planı: [UYGULAMA_PLANI.md](UYGULAMA_PLANI.md). Metin seçimi/notlar için bölüm 19'u da oku; ilk sürümde dışarıda bırakıldıkları eski karar geçerli değildir.
- Artımlı teslim ve aşama kabul planı: [MVP_PLANI.md](MVP_PLANI.md). MVP-0 teknik kapı, MVP-1–6 kullanıcı teslimleri; tam V1 ancak MVP-6 sonunda kabul edilir.
- Kullanıcının güncel açık talebi önce gelir. Bu dosya uygulama çalışma kurallarını, plan ayrıntılı senaryoları, `docs/acceptance/performance-budgets.json` otomatik ölçüm hedeflerini tanımlar.
- Belgeler çelişirse mevcut kullanıcı kararına göre tutarlı biçimde düzelt. Eski PdfRenderer-only veya Expo-only kararlarını geri getirme.
- "Planlandı", "uygulandı", "derlendi", "cihazda doğrulandı" ve "kabul edildi" farklı durumlardır. Kanıtı olmayan durum için tamamlandı deme.
- Bu proje, üst klasördeki SERANTIS `front-end/` projesi değildir. Oraya özel e-ticaret/API/domain kuralları bu bağımsız PDF okuyucuya taşınmaz.

- VS Code canlı geliştirme: kök klasörde Ctrl+Shift+B, `.vscode/tasks.json` ve `scripts/dev-android.ps1`; Metro localhost:8087, emülatör adı kitap-functional. Debug uygulama `com.kitap.dev` / Bookmuse Dev; release `com.kitap` kayıtlarından ayrıdır. [Kullanım](docs/VS_CODE_GELISTIRME.md). Debug/emülatör performansı Huawei kabulü değildir.

## 2. Kullanıcı ve ürün amacı

Kullanıcı indirdiği PDF kitaplarını öncelikle tablette, ayrıca telefonda okumak istiyor. İlk performans referansı **2 GB RAM'li Huawei tablet**. Kullanıcı modelini **MatePad SE AGS5-W09**, işletim sistemini **HarmonyOS 3.0** olarak bildirdi. Android API, CPU ABI, gerçek RAM ve ekran bilgisi ADB ile henüz ölçülmedi; HarmonyOS sürümünden API tahmini yapılmaz. Kullanıcı beyanı `docs/acceptance/reference-device.json`, gerçek cihaz gözlemi `docs/acceptance/device-inventory.json` içindedir.

Ürün: huzurlu, göz yormayan, sade, akıcı, hafif ve çevrimdışı kitap okuyucu. Mobil web sitesi veya WebView uygulaması değildir. Android'e doğrudan kurulabilen imzalı APK gereklidir. Uygulamanın görünen adı **Bookmuse** olarak seçilmiştir; ikon kullanıcı tarafından sağlanan `mobile/assets/branding/bookmuse-icon.png` dosyasıdır; domain henüz belirlenmemiştir. Mevcut teknik uygulama kimliği `com.kitap`, debug kimliği `com.kitap.dev` olarak kullanılır; imza/güncelleme sürekliliği korunur.

Öncelik: veri güvenilirliği → PDF doğruluğu → düşük bellek → hızlı dokunma tepkisi → okunabilirlik → görsel süslemeler. Performans hedefi kullanıcı özelliklerini sessizce kaldırma gerekçesi değildir; gerektiğinde ön hazırlık, çözünürlük veya çizim ayrıntısı uyarlanır.

## 3. Onaylanmış teknoloji yönü

- Ana uygulama **React Native + TypeScript strict**, Community CLI; Android kaynakları doğrudan yönetilir.
- React Native ile uyumlu paketlenmiş **Hermes**, güncel desteklenen ve birlikte derlenmiş kararlı sürümler; lockfile tutulur.
- PDF görünümü **Fabric native component**, dosya/motor/kayıt komutları küçük **Turbo Native Module** arayüzleri.
- Tek **PDFium tabanlı yerel motor**: görüntüleme, karakter konumları, hit-test ve metin çıkarma aynı belge oturumundan. Kotlin/JNI bağlantısı gerekir. Arayüzü Kotlin/Compose'a dönüştürme.
- Android'in PdfRenderer metin seçimi API 35 ile gelir; eski Huawei için tek başına yeterli değildir. PDFium + PdfRenderer şeklinde iki motoru aynı belgeyi iki kere parse etmek için birlikte kullanma.
- MVP-0 için `io.legere:pdfiumandroid:2.0.3` kararlı wrapper'ı seçildi; source/POM üzerinden API ve Kotlin metadata gereksinimi incelendi. Gerçek cihaz kabulü henüz verilmedi. Upstream/lisans/güvenlik/ABI/16 KB kontrolü ve performans sonuçları sürüm kararında korunur.
- SQLite yerel kayıtların tek kaynağı; Kotlin repository üzerinden küçük API. React Navigation native stack; sanallaştırılmış listeler. Reanimated/Gesture Handler yalnız uygun RN arayüz hareketlerinde; PDF yüzeyindeki hareketin sahibi native bileşen.
- GMS/HMS, hesap, internet, reklam, analytics ve abonelik zorunlu bağımlılıkları yok. iOS, bulut eşitleme ve sunucu V1 kapsamı değildir.
- Android 8+ ürün hedefi ve minimum toolchain API seviyesi gerçek cihazla doğrulanır. Eski 32-bit tablet desteği seçilen toolchain'in gerçekten ürettiği ABI ile kanıtlanır. Kullanıcı cihazı desteklenmiyorsa bunu açıkça bildir; sessizce eski/desteği bitmiş RN'ye dönme.

## 4. V1 ürün kapsamı

1. Sistem dosya seçicisinden ve Android paylaşım/açma akışından PDF ekleme; kalıcı erişim veya gerektiğinde yerel kopya.
2. Tek sanallaştırılmış kapak ızgarası, küçük devam satırı, arama, ayrı sıralama/favori filtresi; kartta `(45/80)` ve beş işlemli ⋮ (Notlar, Altı çizilenler, Favori, Adı düzenle, Kaldır). Bağımsız kitap yönetimi yoktur.
3. Kırpmasız/oranı korunmuş tam sayfa; ana Ayarlar’dan tek/çift sayfa. Numarayla gitme ve yer imleri üç sütunlu Sayfalar görünümündedir.
4. Parmağı takip eden wipe, 1–5× pinch/double-tap zoom; 1× altında ek sıkıştırma ile üç sütunlu, sanallaştırılmış genel görünüm.
5. Orijinal, sepya, gece ve özel renk/kontrast bütün kitaplarda ortaktır; Krem seçeneği kaldırılmıştır. Okuma teması Ayarlar ekranını değiştirmez. Eski kitap/crop tercihleri korunur; yeni arayüzde crop/fit-width sunulmaz.
6. Kaldığın yer, yön kilidi, sol el tercihleri, uygulama penceresi parlaklığı, isteğe bağlı ekran açık ve tam ekran.
7. Gerçek metin seçimi ve fosforlu sarı `#FFEB3B` highlight; eski metin stilleri de sarıya döner. Kart ⋮ menüsünden **Altı çizilenler**; sayfa/satır/alıntı ve doğru konuma dönüş.
8. Kitap geneli, sayfa veya seçilmiş pasaj notları; **Notlar** sekmesi; taslak kurtarma, düzenleme, arama, silme/geri alma.
9. Taramalarda metin yoksa bölge işareti ve sayfa/genel notu. Kaydedilmiş işaret/not listeleri içinde arama.
10. Dosya erişim kaybı ve bozuk PDF kurtarma; kayıtları koruyarak yeniden dosya bağlama.
11. Yedek/geri yükleme, TXT/Markdown ve bağımsız depolama yönetimi son sadeleştirme talebiyle güncel arayüzden ve özel kod yolundan kaldırıldı; tarihsel belgeler önceki sürümü tarif eder.
12. Kurulabilir imzalı APK ve ölçüm/kabul raporu.

## 5. V1 dışında kalanlar ve sınırlar

OCR; bağımsız metin yeniden akışı/font/punto; bütün PDF içerik araması; sesli okuma; serbest kalem çizimi; EPUB; bulut eşitleme; mağaza; otomatik klasör tarama; 3D sayfa kıvrılması; tek gesture ile sayfalar arası seçim; orijinal PDF içine annotation gömme. Yeni kullanıcı talebi gelirse kapsam açıkça güncellenir.

PDF içindekiler/link desteği motor keşfine bağlıdır; uygulanmamış bir kontrolü çalışır gibi gösterme. Renk değişimi metin ve fotoğraf ayrımını kusursuz yapmaz. OCR katmanı olmayan taramada gerçek kelime seçimi varmış gibi davranma. Standart PDF satır numarası yoktur; alıntı, sayfa ve gerçek satır geometrisi kullan.

## 6. Ekran ve okuma ergonomisi

Ana ekranlar Kitaplık / Ayarlar ve başlıksız Okuyucu’dur. Okuyucuda alt gezinme yoktur. Notlar ve Altı çizilenler listeleri yalnız kitap kartının ⋮ menüsündedir; okuyucunun Sayfalar görünümünde bu listeler bulunmaz. Yer imleri Sayfalar içindedir.

Kitap, oranı korunmuş tam sayfa olarak tüm okuma alanını kullanır. Başlık/sayaç/tutamaç sürekli görünmez. Dokununca PDF’yi küçültmeyen transparan katmanda yalnız sayfa notu / kitap notu / metin seçimi ve ayrı Sayfalar simgesi açılır. Sayaç Sayfalar görünümündedir. İlk/son sayfada döngü yoktur; kapak tek/spread ortak Ayarlar’dadır.

Tablet/telefon ayrımını cihaz adıyla değil pencere genişliğiyle yap. Rotation ve split-screen fiziksel sayfayı ve oransal odağı korur. Çift sayfa kullanıcı tercihidir, 2 GB profilde tek sayfa varsayılan. Gece modunda yükleme yüzeyi de koyu; beyaz flash yok.

Sistem fontu, büyütülmüş yazıya uyum, en az 48 dp hedef dokunma alanı, TalkBack etiket/focus, hareket azaltma ve tüm gesture işlevlerine düğme alternatifi. PDF'nin tüm içeriğini TalkBack okuyor diye kanıtsız iddiada bulunma. Ses, titreşim, blur, güçlü gölge ve sürekli hareket varsayılan yok.

## 7. Sistem geri hareketi: bağlayıcı çakışma kuralı

**Android'in hem sağ hem sol en dış kenarından başlayan geri hareketi korunur.**

- Native tarafta `WindowInsetsCompat` üzerinden güncel `systemGestures`, `mandatorySystemGestures`, sistem bar/IME ve cutout bilgileri alınır. Insets rotation/window/navigation değişiminde yeniden hesaplanır. px/dp dönüşümü tek ve tutarlı yapılır.
- Sayfa çevirme ve panel gesture'ları, touch-down sistemin korunan kenar bölgesindeyse başlayamaz. Sonraki hareketin içeri gelmesi bu kararı değiştirmez.
- Yeni okuyucuda tutamaç yoktur; Sayfalar simge veya iç alanda başlayan iki parmak sıkıştırmasıyla açılır. Bütün hit hedefleri sistem bölgesinin dışında kalır. HitSlop veya parent pan recognizer ile kenar sessizce tekrar sahiplenilmez. Tutamaç için sabit "her cihazda 20 px" sınırı kullanılmaz.
- `setSystemGestureExclusionRects` ile geniş ekran kenarlarını uygulamaya ayırma. Temel tasarım sistem geri alanını korur.
- İç tek parmak normalde sayfa çevirir, zoom’da pan yapar; iç iki parmak zoom/over-pinch yapar; dış kenar sistem gerisidir. Aynı touch stream’e iki recognizer sahip olmaz.
- Insets güvenilir değilse geniş kenar yakalama yerine düğmeyle panel açma kullanılabilir. Üç düğmeli navigasyonda da sistem bar alanı korunur.
- Kabul: sağ ve soldan geri, dikey/yatay, tek/çift sayfa, normal/zoom/seçim, açık panel ve not klavyesinde doğru davranır; yanlış sayfa geçişi veya panel açılması yok. Modern Android predictive back iptali de test edilir; iptal edilen geri konumu/kaydı değiştirmez.

## 8. Gesture ve geri durum sözleşmesi

Normal ölçekte yatay sürükleme sayfa değiştirir; zoom'da tek parmak pan. Çift dokunma odak noktasına zoom, iki parmak pinch. Metne 400–500 ms başlangıç eşiğiyle uzun basma kelime seçer; kulplar aralığı genişletir. Aktif seçimde page-swipe durur. Küçük sürükleme mevcut sayfaya döner ve kaydı değiştirmez.

Swipe settle mesafeye bağlı en çok220 ms; overview yaklaşık200 ms; UI/native transform üzerinde. Hareket azaltmada kısa veya animasyonsuz davranış. Hızlı girişler birikmiş animasyon kuyruğu oluşturmaz. Diyagonal hareketler dominant eksene göre değerlendirilir. Hazır olmayan hedefte mevcut sayfa korunur, yükleme/iptal ve kademeli netleşme sunulur.

Geri sırası mevcut bağlama göre: IME → not editörü/taslak korunması → seçim/işaret menüsü → açık panel → okuma görünümünün sadeleşmesi → kitaplık. Geri navigasyonu kullanıcıya sistem hareketiyle tutarlı kalır; draft korunur. İşaret listesine gitmeden önce dönüş noktası saklanır; Okumaya dön önceki konumu açar.

## 9. Renk, koordinat ve işaret çizimi

Sayfa bitmapi native çizimde filtrelenir; her tema için ayrı tam sayfa kopyası oluşturma. Açık/koyu uç renklerin eşlemesi ve kontrast; resimler de etkilenebilir açıklaması; kolay Orijinal modu.

Ekran piksellerini kalıcı işaret konumu olarak saklama. PDF user-space quad'ları, MediaBox/CropBox, rotasyon, belge revizyonu, fiziksel sayfa, alıntı ve bağlam saklanır. Aynı dönüşüm zinciri render, hit-test, crop, zoom/pan ve overlay için kullanılır; ters dönüşüm doğrulanır.

Kaydedilmiş overlay tema filtresinden sonra çizilir; çizgi rengi yanlışlıkla tersine çevrilmez. Çok satır/çok sütun ve eğik yazı düz büyük rectangle ile yanlış sütunu kaplamaz. Standart satır numarası yoktur: `Sayfa 42 · 3 satır` + alıntı + konuma git. Görsel satır sırası varsa hesaplanmış bilgi olarak etiketlenir.

## 10. PDF motoru ve kaynak sahipliği

- Tek aktif belge oturumu, tek seri worker, sınırlandırılmış ve öncelikli iş kuyruğu. PDFium thread safety varsayılmaz.
- Dosya I/O, render, text extraction, DB ve hash işi UI/JS thread'de yok. JNI native crash'in try/catch ile çözüldüğünü varsayma.
- Görünür sayfa/tile → aktif selection hit-test → hedef sayfa → muhtemel sonraki → önceki → görünür thumbnails. Seçim aktifken gereksiz prefetch durur.
- Eski işlere session/viewport generation id. Kuyruktaki işler iptal edilir; başlamış senkron çağrının hemen kesilebileceğini varsayma. Eski sonuç yeni ekrana bağlanmaz.
- Text-page/page/document ve FD sıralı ve tek sahiplikle kapanır. close/render/rotation/background yarışları test edilir.
- JS'ye bitmap, base64 PDF, pixel array veya her frame bütün glyph geometrisi gönderme. Küçük komutlar, metadata, seçilen alıntı ve seyrek olaylar geçer.
- Kaydedilen işaret sayısı, açık bitmap veya renderer sayısını artırmaz. Okuma sırasında bütün kitabı OCR/index/render etme.

## 11. Düşük donanım bütçeleri

2 GB profilde **24 MiB** PDF bitmap bütçesi: görünür sayfa, komşu, üretim rezervi, zoom tile ve thumbnail birlikte. Thumbnail RAM başlangıcı 1–2 MiB bu bütçe içindedir. Normal profil ilk hedef 48 MiB; gerçek heap, viewport ve ölçümlere göre uyarlanır.

Byte tabanlı LRU, aktif yüzey pinleme, tahsis öncesi yer açma. Mevcut + okuma yönündeki bir sayfa; önceki ancak bütçe varsa. Üç tam sayfa koşulsuz tutulmaz. GPU/native/JS maliyeti bitmap bütçesinden ayrı PSS ölçümüne dahildir. Güvenli sahiplik bitmeden recycle yok.

Fit çözünürlüğü gerçek gösterim alanına göre; düşük profilde 1–1,5 megapiksel başlangıç, okunabilirlikle doğrulanır. Zoom sırasında transform, sakinleşince yalnız görünür 256/512 px tile; dev 4×/8× tüm sayfa bitmapi yok. ARGB_8888 ilk yol; RGB_565'in otomatik uygun olduğu varsayılmaz.

Text/geometri türetilmiş cache hedefi 4 MiB; PDFium text-page native maliyeti ayrıca ölçülür. Yalnız aktif sayfaya talep üzerine çıkarım; sınırsız glyph/JSON buffer yok. Disk thumbnail cache başlangıç 64 MiB, kullanıcı temizleyebilir; tam sayfaları sürekli encode etme.

Background/trim: önce prefetch, komşu ve görünmeyen tile/thumbnail bırakılır. Dahili byte sınırı her zaman yürür; yalnız OS callback'ine güvenme. Gerekirse kalite veya çift sayfa uyarlanır; okumayı ve kayıtları koru. Okurken sürekli timer, animasyon, filesystem scan ve React root re-render yok.

## 12. Dosya, kitap kimliği ve kurtarma

Sistem seçicisi `ACTION_OPEN_DOCUMENT`; mümkünse persistable URI permission. Geniş MANAGE_EXTERNAL_STORAGE gerektirme. Seekable olmayan veya kalıcı olmayan kaynağı native stream ile gerektiğinde kopyala. JS'ye tüm PDF'yi yükleme.

MVP-1 ilk yolu bütün seçilen kitaplar için uygulama içi özel kopyadır; ek disk alanı UI'da açıklanır. Böylece dış kaynak taşınması/izin iptali okuma erişimini kesmez. [MVP1_STORAGE.md](docs/architecture/MVP1_STORAGE.md) atomik import, SHA256 relink, schemaVersion 1 ve onDraw sonrası konum commit sözleşmesini tanımlar. Persistable doğrudan URI yolu henüz uygulanmadı.

Kopyalama geçici dosya → tamamlanma → atomik final ad → DB transaction. İptal/disk dolması/crash yarım dosyayı hazır kitap olarak bırakmaz. Uzak sağlayıcıda gerçek hazırlık durumu; byte boyutu bilinmiyorsa uydurma yüzde yok.

Kimlik sadece dosya adı değildir: documentId, URI, boyut/değişiklik ve gerektiğinde fingerprint. Her açılışta büyük dosya hashleme yok. Dosya taşınırsa alıntı/not/yer imi korunur; yeniden seçilen PDF aynı belge olarak doğrulanır. Yeni baskıya eski geometri sessizce uygulanmaz.

Kitaplıktan kaldır dış orijinali silmez. Uygulama kopyasını silme açık ve ayrı; işaret/notların silinmesi kullanıcıya açık. Cache temizleme kalıcı kayıtları silmez. Bozuk/boş/kısmi/parolalı/büyük PDF hata durumları ayrıdır. Parola oturum belleğinde; log, backup ve repository'ye yazılmaz.

## 13. Metin seçimi ve kitap çalışma alanı

Seçim menüsü Sarıyla işaretle / Kopyala / Pasaja not ekle / Seçimi kapat. Yeni metin akışında ince alt çizgi veya renk seçimi yoktur; eski API stilleri native repository’de sarıya normalize edilir. Tek sayfada çok satır; sayfalar arası kesintisiz seçim V1 dışı. Çok sütun, Unicode, ligature ve sağdan sola metin için seçimin gerçek içeriği önizlenir; bozuk alıntı doğruymuş gibi kaydedilmez.

Altı çizilenler kartı: sayfa, seçili metin, satır bilgisi, tür/renk, varsa not ve tarih. SQL pagination, sayfa/tarih sırası, sarı metin/bölge filtresi ve kayıtlı alıntıda arama. Eski renk alanı veri/backup uyumluluğu için kalır; renk seçici sunulmaz. Screenshot kopyası varsayılan yok. Dokununca doğru sayfa ve işaret görünür; erişim kaybında alıntı/not yine okunur.

İşaret+ranges transaction, idempotent komut ile çift basma mükerrer kayıt oluşturmaz. Yeni metinde renk/tür düzenleme yoktur; bölge türü korunur. Silme/geri alma ve ilişkili notları açık seçimle koruma/silme vardır; not sessizce silinmez. Kaydedildi yalnız commit sonucu.

Taramada OCR metni yoksa bölge seçme ve kullanıcı metni/notu; gerçek extraction diye etiketleme. Notlar genel kitap, sayfa veya pasaj bağlantılı. Düz metin editörü; klavye Kaydet düğmesini kapatmaz. Autosave draft başlangıç debounce 500–800 ms; revision ile eski write yeni yazıyı ezmez. Explicit kaydet final note'u transaction ile günceller; çıkış/background draft flush. Zorla kapatmada son başarılı draft geri gelir; henüz yazılmamış son tuşa garanti verme.

## 14. Kalıcı model ve yedek

Veri grupları: documents; reading_positions; book_preferences; bookmarks; annotations; annotation_ranges; notes; note_drafts; app_preferences. Ayrıntılı alanlar plan bölüm 9/19'da. Fiziksel sayfa içeride 0, kullanıcıda 1 tabanlı. Basılı kitap numarası ayrı bilgi.

Annotation referansı yalnız character index değildir: engineVersion, belge fingerprint/revision, PDF quad, alıntı ve kısa bağlam. Note bağı documentId ve isteğe bağlı annotationId/page. Foreign key ve silme davranışları explicit; ilişkili notun silinmesi örtük cascade ile sürpriz yaratmaz.

Konum tamamlanmış geçişte native kayda; 250–500 ms debounce, en geç 1 saniye flush başlangıç hedefi. Background/çıkış/kitap değişimi hemen flush. Migration transaction ve versioned; başarısız migration otomatik sıfırlama yapmaz.

Aşağıdaki JSON yedek sözleşmesi tarihsel MVP-6/UX-MVP-6 sürümünü tarif eder; 0.8.1 sadeleştirmesinde yedek/restore/export UI ve özel uygulama kodu kaldırılmıştır. Eski sürümde sürümlü JSON yedeği konum, yer imi, işaret/geometri, not ve tercihleri kapsardı. UUID/revision ile tekrar import dedupe, çatışma önizleme. Başka cihaz URI'leri geçersiz olabilir; dosya yeniden bağlama. TXT/Markdown kitap özeti sayfa/alıntı/not içerir. Parola dahil değil; orijinal PDF otomatik gömülmez/değiştirilmez.

## 15. Kabul hedefleri ve kanıt standardı

İşlevler ve veri doğruluğu V1 için zorunlu. Sayısal hedefler prototipte doğrulanacak başlangıç bütçeleridir. Bütçeyi karşılayamayan sonuç "geçti" değildir; neden ve önerilen düzeltme raporlanır. Hedef değişikliği gerekçesiyle plan/budget dosyası birlikte güncellenir; yalnız raporu geçirmek için sayılar değiştirilmez.

| Ölçüm | Hedef |
|---|---|
| Cold kitaplık | p95 ≤2500 ms, 20 tekrar |
| Warm dönüş | p95 ≤1000 ms |
| Tipik yerel PDF ilk okunabilir sayfa | p95 ≤2000 ms, import hariç |
| Dokunma | p95 ≤100 ms |
| Panel görünmesi | p95 ≤250 ms, thumbnail tamamlanması ayrı |
| Metadata hazırken metin seçimi | p95 ≤100 ms; long-press süresi hariç |
| Kısa işaret DB commit | p95 ≤300 ms |
| Hazır sayfa geçişi | 60 Hz'de 60 FPS'e yakın; geciken kare oranı <%5; frozen frame yok |
| Tipik tek sayfa toplam PSS | ≤160 MiB hedef |
| Peak PSS | ≤220 MiB hedef |
| Uzun oturum sonrası durulmuş PSS artışı | ≤%10; sürekli büyüme yok |
| Bitmap cache / türetilmiş text cache | 24 / 4 MiB hedefleri |
| Cihaza uygun / universal APK | ≤30 / ≤50 MiB hedefleri |
| 1000 işaret/not | Paginated, responsive liste |
| Sağlamlık | Test corpus/oturumda OOM, ANR, crash yok |

Sürelerde nearest-rank p95 ve birim açık. Cold/warm, cached/uncached, tipik/ağır PDF sonuçları karıştırılmaz. İlk sayfa "okunabilir" zamanı ile tüm detayın netleşmesi ayrı. Mem PSS ve Java heap aynı şey değildir; native/GPU belleği gizlenmez. 160 MiB bir bitmap bütçesi değildir.

Fiziksel Huawei, gerçek release artifact ve test corpus olmadan performans kabulü verilemez. Debug/Metro, emulator veya uydurma JSON ölçümü kanıt değildir. Perfetto, dumpsys meminfo, Memory Profiler, destekliyorsa Macrobenchmark/FrameTiming kullan. Eski Android için alternatif frame stats ölçümü belgelenir. Benchmark artefaktı, sürüm/hash, model/OS/ABI, pil/sıcaklık/yenileme hızı, corpus ve ham örnekler raporda olur.

## 16. Kullanıcı ve regresyon testleri

Masa başı simülasyonu gerçek test değildir. Her dilimde uygun senaryo gerçekten yürütülür. Düşük cihazda 30–60 dakika; 500 geçiş, en az 20 zoom ve 10 rotation; kitap aç/kapat, background/kill, ekran kilidi ve güncelleme. İşaret kaydet/kapat/aç → doğru alıntı/geometri/not; zoom/rotation/crop/tema → yer kayması yok; draft kill → son commit geri; yedek tekrar import → mükerrer yok.

Corpus: 100–500 sayfa tipik metin, 1000+ sayfa, yüksek çözünürlük tarama, büyük resimli dosya, vector-heavy sayfa, Türkçe/ligature/çok sütun/RTL/eğik metin, değişken sayfa kutusu ve rotation, parolalı/bozuk/kısmi/boş dosya, SD kart/provider, çok büyük sayfa. Kişisel PDF'ler source control'e konmaz.

Anlamlı otomasyon: transform round-trip, page/spread boundary, cache eviction/pinning, queue/generation, SQLite revision/transaction/migration, import kurtarma, belge revizyonu ve yedek dedupe. Yalnız implementation'ı tekrar eden test yazma. Fonksiyonel ve bellek doğru olmadan estetik geliştirmeyi bitmiş sürüm sayma.

## 17. Geliştirme aşamaları ve bağımlılıklar

Geliştirme sırası ve ID'li kabul kriterleri MVP_PLANI.md'dedir:

1. **MVP-0:** cihaz/ABI/release ve tek PDFium render+text teknik kapısı; kitap okuma ürünü sayılmaz.
2. **MVP-1:** PDF ekleme, temel okuyucu, sayaç/numara, SQLite konum ve dosya kurtarma.
3. **MVP-2:** akıcı gesture/tile zoom, tema/kontrast/crop, ergonomi ve iki geri kenarı.
4. **MVP-3:** sanal sayfa paneli, yer imleri, tam kitaplık, çift sayfa ve sol el.
5. **MVP-4:** gerçek selection, underline/highlight, Altı çizilenler, durable anchor/jump.
6. **MVP-5:** genel/sayfa/pasaj notları, Notlar, autosave draft/revision ve kurtarma.
7. **MVP-6:** yedek/restore/export, tam relink/recovery, birleşik test, shrink/update ve V1 APK teslimi.

Her MVP önceki davranışları ve kayıtları korur; performans aşamalı olarak baştan test edilir. Henüz olmayan özellik için sahte ölçüm/sıfır veya etkin boş kontrol üretme. Başlangıçta active metric subset kullanılabilir, finalde canonical tam budget değerlendirilir. Gerçek cihaz kanıtı eksikse kod/build işi ilerleyebilir fakat ilgili aşama fiziksel kabul edilmiş sayılmaz. Ayrıntılı kriter ID'leri ve hedef aktivasyonu MVP_PLANI.md'de tek yerde tutulur.

Özel native motor sıradan bir JS PDF wrapper görevi gibi küçümsenmez. PDFium kontrolü/lisans/bellek problemi prototipte çözülür. Kesin dependency/SDK ve bitiş tarihi bilinmeyen cihaz bilgisi varmış gibi uydurulmaz. Kullanıcının istediği rutin ve geri alınabilir geliştirmelerde tekrar tekrar onay isteme; gerçekten eksik cihaz/veri bilgisi varsa sorarken bağımsız işi sürdür.

## 18. Proje skill'leri ve seçme kuralları

Skill'ler `~/.codex/skills/` altında keşfedilebilir ve `kitap-` ad alanındadır. Ürün bağlamı bu dosyada; skill'ler işi yapma yöntemini taşır, ayrı bir ürün kapsamı icat etmez.

| Skill | Ne zaman |
|---|---|
| `kitap-rn-pdf-engine` | RN/Fabric/JNI PDF motoru, tile/cache, render ve text worker işleri |
| `kitap-reader-gestures` | Tablet okuma UX'i, geri kenarları, panel/zoom/selection gesture ve erişilebilirlik |
| `kitap-annotations-storage` | Altı çizilenler, PDF quad'ları, not/draft, SQLite/yedek ve kayıt kurtarma |
| `kitap-android-release` | Düşük cihaz profiling, ölçüm değerlendirme, ABI/shrink/imza/update ve APK teslim |

Yalnız değişikliğe uygun skill ve reference yüklenir. Mevcut `pdf:pdf` corpus incelemede yardımcı; live Android testinin yerine geçmez. `skill-creator` skill bakımında kullanılır. Web Sites/ASP.NET/Excel bu projede gerekli değildir. Skill otomatik seçimi başka projelerde yalnız uygun native PDF-reader kapsamına uygulanır.

## 19. APK, dosya güvenliği ve tamamlanma raporu

Release artifact Metro'dan bağımsız, gerçek seçilen ABI'lerde kurulabilir. R8/resource shrink sonrası Codegen/JNI/keep kuralları test edilir. Gerekirse cihaz ABI APK'sı ve universal APK; 16 KB native sayfa uyumu desteklenen yeni Android'de. Uygulama kimliği/sürüm ve aynı signing key ile update test edilir.

İmza anahtarı/parola, kişisel PDF, not/alıntı ve debug credential repository'ye veya loga girmez. Keystore kullanıcı kontrolünde güvenli saklanır. Orijinal kitapları topluca silme/değiştirme bu ürünün normal akışı değildir.

Her anlamlı teslimde: değişen davranış; çalıştırılan kontroller; kullanılan cihaz/build; ölçümler; henüz doğrulanmayan sınırlar. Nihai teslim: kaynak+lockfile, build notları, imzalı APK/hash, ölçüm/kabul raporu, destek matrisi, kurulum/güncelleme/yedek/kurtarma talimatı. Gerçek cihaz yoksa kod/derleme tamamlanabilir fakat fiziksel kabul açık kalır; raporda bunu dürüstçe belirt.
