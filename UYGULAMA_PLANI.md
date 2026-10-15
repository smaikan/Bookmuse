> 15 Ekim 2026 son kullanıcı kararı: [Arayüz sadeleştirmesi](docs/ARAYUZ_SADELESTIRME.md) bu belgedeki çelişen menü/ayar/yedek kararlarından önce gelir. Kart menüsünde mevcut Notlar / Altı çizilenler / Favori korunur; Adı düzenle ve Kaldır sona eklenir (toplam beş). Ayarlar dişliyle doğrudan tek ekranda açılır. Yedek/depolama ve bağımsız kitap yönetimi kaldırılmıştır.

> 15 Ekim 2026 güncel karar: [UX-MVP-1–6 yenilemesi](docs/UX_YENIDEN_TASARIM_PLANI.md) kaynak/derleme ve belgelenen emülatör kapsamıyla tamamlandı; fiziksel ve performans kabulü açık. [Güncel teslim](docs/acceptance/reports/UX-MVP-6.md). Bu belgedeki çelişen eski görünüm/menü/işaret stili kararlarını yeni plan değiştirir. Eski MVP aşamalarının güncel kanıtı AGENTS ve kabul raporlarıdır.

# Tablet odaklı React Native PDF kitap okuyucu — geliştirme ve kabul planı

Tarih: 14 Ekim 2026. Referans: kullanıcının belirttiği 2 GB RAM'li Huawei tablet; kullanıcı modeli MatePad SE AGS5-W09, HarmonyOS 3.0 olarak bildirdi. Telefon ikincil hedef, tablet birincil hedef. Bu belge tasarım ve teknik planıdır. MVP-0–6 ve UX-MVP-1–6 kaynak/release teslimleri raporlandı; fiziksel cihaz ve tam V1 kabulü açık. Güncel kanıt `docs/acceptance/reports/UX-MVP-6.md` dosyasındadır. Buradaki kullanıcı senaryoları masa başı simülasyonudur. Performans değerleri ölçülecek hedeflerdir, elde edilmiş sonuç değildir.

Kapsam güncellemesi: kullanıcının yeni isteğiyle gerçek metin seçimi, altını çizme/vurgulama, kitap başına Altı çizilenler sekmesi ve kitap/genel-pasaj notları V1'e alınmıştır. "net oluşturmak" ifadesi bağlam gereği "not oluşturmak" olarak yorumlanmıştır. Bu nedenle eski Android için PdfRenderer tek başına yeterli motor kararı olmaktan çıkarılmış, PDFium tabanlı tek motor tercih edilmiştir.

## 1. Ürün kararı ve öncelikler

Amaç, yerel PDF kitaplarını uzun süre rahat okumak: sade kitaplık, dikkat dağıtmayan okuma ekranı, renk seçenekleri, yumuşak sayfa geçişi, kolay sayfa bulma, güvenilir konum kaydı, seçilen metnin altını çizme ve kitap başına not tutma.

Öncelik sırası: okuma konumunu ve kayıtları koruma; güvenilir PDF açma; düşük bellek; dokunmalara hızlı yanıt; okunabilirlik; görsel süslemeler. Renk değişikliği ve önizleme paneli temel kapsamdadır; performans bahanesiyle kaldırılmaz. Gerektiğinde ayrıntı kalitesi ve önceden hazırlanan sayfa sayısı uyarlanır.

Ana uygulama React Native + TypeScript olacak. PDF motoru, React Native'in yerel bileşeni olarak Android tarafında Kotlin ile yazılacak. Bu, uygulamanın Kotlin ile baştan yazılması değildir; ağır işin uygun katmanda yapılmasıdır. Tamamen JavaScript PDF motoru hedeflenmez.

İlk dağıtım doğrudan kurulan, imzalı Android APK. İnternet, kullanıcı hesabı, abonelik, reklam, analiz SDK'sı veya Google Play Services bağımlılığı yok. Huawei için ayrıca HMS SDK gerekmez. Telefon ve tablet arasında otomatik eşitleme ilk sürümde yok; yerel dışa/içe aktarma var.

Uyumluluk: seçilen güncel ve desteklenen React Native sürümünün minimum Android API seviyesi proje kurulumunda doğrulanır. API 24 / Android 7 altı cihazlar için destek varsayılmaz; ürün hedefi Android 8+ olsa da gerçek Huawei sürümü ile karar kesinleştirilir. CPU mimarisi de kontrol edilir; yalnız 32 bit çalışan eski tablet için seçilen RN/Hermes ve bağımlılıkların uyumlu APK üretebildiği ayrıca doğrulanır. Cihaz uyumsuzsa eski ve desteği bitmiş RN sürümüne sessizce dönülmez.

## 2. Teknoloji ve bağımlılık kararı

| Alan | Karar | Kontrol / gerekçe |
|---|---|---|
| Uygulama | React Native, TypeScript strict | Desteklenen kararlı sürüm ve kilitlenmiş bağımlılıklar |
| JavaScript motoru | RN ile gelen Hermes | Sürüm uyumu; JS işini sınırlama |
| Proje | React Native Community CLI, Android klasörü doğrudan sürüm kontrolünde | Özel PDF bileşeni ve Gradle üzerinde doğrudan kontrol |
| PDF yüzeyi | Fabric native component | Bitmapleri JS'ye taşımadan gösterme |
| Dosya / motor komutları | Turbo Native Module | URI, belge kimliği, küçük ayarlar ve olaylar |
| PDF işleme ve metin | PDFium tabanlı tek native motor, Kotlin/JNI bağlantısı | Eski Android'de de glyph konumu, metin seçimi ve görüntüleme; sürdürülebilir build/wrapper doğrulanacak |
| PDF worker | Tek öncelikli, sınırlı kuyruk | Aynı renderer üzerindeki işlerin çakışmasını önleme |
| Yerel kalıcı veri | SQLite; Kotlin repository ve küçük RN API'si | Tek veri kaynağı, transaction ve migration |
| Ekran geçişi | React Navigation native stack | Az ekran, yerel geçişler |
| Liste | Önce sanallaştırılmış FlatList | Sabit hücreler; ölçüm gerektirirse başka liste motoru |
| Arayüz hareketleri | Reanimated / Gesture Handler, sürüm uyumu doğrulanarak | RN panelleri için; PDF yüzeyindeki gesture sahibi native bileşen |
| Tasarım | RN StyleSheet ve küçük token seti | Büyük UI kit, çok font ve gereksiz bağımlılık yok |
| APK | Gradle release, R8 ve kaynak küçültme | İmzalı ve geliştirici sunucusundan bağımsız |

Önceki Expo önerisi bu ayrıntılı performans planında Community CLI yönünde güncellenmiştir. Expo ile de yapılabilir; doğrudan yönetilen native kaynak bu proje için daha uygun bakım tercihidir. Bu, Expo'nun mutlaka yavaş olduğu iddiası değildir.

react-native-pdf yalnız başlangıç karşılaştırma prototipinde değerlendirilebilir. Renk dönüşümü, tile önbelleği, gerçek metin seçimi ve bellek sınırları için gerekli kontrolü ölçerek sağlıyorsa yeniden değerlendirilir. Üretim APK'sında iki ayrı PDF motoru birlikte tutulmaz. PDFium render ve metin çıkarma aynı belge oturumunu kullanır; Android PdfRenderer + ayrı text engine şeklinde iki kez parse etme varsayılan değildir. PDFium'un metin API'si karakter kutusu, konumdan karakter bulma ve seçilen metni çıkarma sunar; tam seçim UX'i uygulamada yazılır. Sürdürülebilir Android build/wrapper; upstream sürüm, third-party lisans bildirimleri, güvenlik güncellemeleri, 32/64 bit ve 16 KB uyumu bakımından doğrulanır. Lisans kararı yalnız proje adından çıkarılmaz. Motorun yalnız PDF'yi açması yeterli kabul edilmez.

Android PdfRenderer.Page.selectContent API 35 / Android 15 ile eklenmiştir. Bu yüzden eski 2 GB Huawei referansında platform renderer'ını tek başına seçmek kullanıcı tarafından istenen gerçek metin seçimini karşılamaz. PDFium bağımlılığı APK ve native bellek tabanını artırabilir; önceki boyut/PSS hedefleri halen ölçüm hedefidir, doğrulanmış sonuç değildir. İlk prototip metin seçimiyle birlikte ölçülür, gerekirse sapma ve nedeni raporlanır.

Kesin RN/Reanimated/Gesture Handler/Gradle/SDK sürümleri, birlikte release APK derlendiği kanıtlandıktan sonra lockfile ve teknik karar kaydına yazılır. Rastgele en son sürümler bir araya getirilmez.

## 3. Bilgi mimarisi ve ekranlar

Üç ana ekran: Kitaplık, Okuyucu, Ayarlar. Her kitabın çalışma panelinde Sayfalar, Altı çizilenler ve Notlar sekmeleri vardır; yer imleri Sayfalar içinde ayrı filtre/sekme olarak bulunur. PDF ekleme, renk seçme ve not düzenleme küçük panel veya sistem ekranı olarak açılır. Kitaplık kartının menüsünden de o kitabın Altı çizilenler ve Notlar bölümüne doğrudan erişilir.

Kitaplıkta üstte son kitaba devam et alanı, altında liste/ızgara tercihi ve PDF ekle düğmesi. Hücre: ilk sayfadan küçük kapak, düzenlenebilir görünen ad, son sayfa, toplam sayfa. Başlık arama, son okunan/isim sıralaması. Yazar yalnız PDF metadata'sında gerçekten varsa gösterilir. Rastgele okuma yüzdesi, süre tahmini, sosyal özellik ve büyük dekoratif görseller yok.

İlk kullanımda tek eylem: "PDF ekle". Uzun tanıtım yok. İlk kitap açıldığında kısa ve kapatılabilir bir ipucu: "Ortaya dokun: araçlar. Kaydır: sayfa." Hareketle erişilen her işlevin düğmeyle karşılığı olacak.

Okuyucuda kitap merkezde; varsayılan görünüm tek sayfa, sayfaya sığdır. Üst/alt araçlar başlangıçta kısa süre görünür, sonra gizlenir; ekran okuyucu açıkken otomatik gizleme davranışı değiştirilir. Alt kenarda küçük sayfa bilgisi için sürekli göster / araçlarla göster seçeneği bulunur. Kontrol barları sayfanın üzerine büyük gölge bindirmez, aktifken kullanılabilir alanı düzgün hesaplar.

Araçlar: geri/kitaplık, kitap çalışma paneli (Sayfalar / Altı çizilenler / Notlar), yer imi, okuma görünümü. Uzun basınca küçük bağlamsal menü: Altını çiz, Vurgula, Not ekle, Kopyala. Daha az kullanılan ayarlar tek panelde. Saat ve pil göstergesi isteğe bağlı; sistemden olayla güncellenir, her saniye JS timer çalıştırılmaz.

## 4. Görsel rahatlık ve erişilebilirlik

Varsayılan okuma çevresi sıcak nötr; aşırı parlak arayüz yok. Örnek temalar: Orijinal, Krem, Sepya, Gece ve Özel. Özel temada sayfa tonu, yazı tonu, kontrast ve orijinal görünüme dönüş bulunur. Seçim mevcut sayfada küçük önizlemeyle görülür ve kitap başına saklanır; genel varsayılan ayrıca ayarlanır.

Sistem fontu, yeterli dokunma alanı (en az 48 dp hedefi), büyük yazı ayarına uyum, TalkBack etiketleri ve odağı, klavye ile temel gezinme. Sayfa bilgisi yalnız renkle anlatılmaz. Düşük kontrastlı sayaç, okunamayacak kadar silik yapılmaz. Tema seçici okunaksız renk çiftleri için düzeltme önerir; düşük kontrast kullanıcının bilinçli tercihi olarak erişilebilir kalabilir.

Android hareket azaltma / animasyon kapatma tercihine uyum. Sayfa sesleri, titreşim, parlak flaş, hareketli arka plan ve sürekli animasyon yok. Gece moduna girişte beyaz ara kare önlenir; native yükleme yüzeyi de tema rengindedir.

Parlaklık varsayılan olarak sistemden gelir. İsteğe bağlı yalnız uygulama penceresinin parlaklığı değişir; sistem parlaklık ayarı değiştirilmez. "Okurken ekran açık kalsın" isteğe bağlıdır ve okuyucudan çıkınca kaldırılır. Tam ekran modu, yön kilidi, sol el kullanımı ve düğmeyle sayfa geçişi seçenekleri bulunur.

PDF metninin TalkBack tarafından tamamen okunması ilk sürüm özelliği olarak vaat edilmez. Taranmış resim PDF'si erişilebilir metin taşımaz. Okuyucunun araçları erişilebilir olacaktır; PDF içeriğinin erişilebilirliği ayrı araştırma maddesidir.

## 5. Hareket sözleşmesi

| Durum / hareket | Davranış |
|---|---|
| Normal ölçekte sola/sağa tek parmak | Sonraki/önceki sayfa; okuma yönü ayarıyla tersine çevrilebilir |
| Küçük, eşik altı sürükleme | Aynı sayfaya yumuşak dönüş; konum değişmez |
| Tamamlanan sayfa geçişi | Yalnız geçiş yerleşince sayfa değişmiş kabul edilir ve kayıt yapılır |
| Ortaya tek dokunma | Araçları aç/kapat; çift dokunmayı ayırt etmek için kısa bekleme |
| Çift dokunma | Dokunulan noktayı koruyarak genişliğe sığdır / önceki ölçek |
| İki parmak | Büyütme/küçültme; gesture sırasında pahalı render yok |
| Büyütülmüş sayfada tek parmak | Sayfa içinde gezinme; aynı hareket sayfa çevirmez |
| Büyütülmüş sayfadan sonraki sayfa | Görünür araç veya önce sayfaya sığdır; yanlışlıkla atlama önlenir |
| Metin üzerinde uzun basma | Kelimeyi seç, seçim kulplarını ve işlem menüsünü aç |
| Seçim kulbunu sürükleme | Aralığı kelime/karakter sınırlarına göre genişlet/daralt; sayfa çevirme kapalı |
| Kaydedilmiş işarete dokunma | İşareti seç, ilişkili notu aç veya düzenle/sil menüsünü göster |
| İç kenar tutamacından içe kaydırma | Sayfa önizleme paneli |
| Android geri | Önce klavye/not editörü, seçim/bağlamsal menü ve açık panel; sonra okuma görünümü; ardından kitaplık. Not taslağı korunur |

Varsayılan geçiş kısa ve sakin, başlangıçta 180–240 ms aralığı; gerçek cihazda ayarlanır. Mesafe ve hız eşiği ekran genişliğine göre hesaplanır. Diyagonal hareketler dominant eksene göre kilitlenir. Yeni bir geçiş önceki animasyon tamamlanmadan başlarsa biriken animasyon kuyruğu oluşturulmaz; mevcut durum tutarlı şekilde yerleştirilir.

İlk ve son sayfada sınırlı esneme veya hareketsiz sınır; döngü yok. Sayfa kıvırma, 3D mesh, blur ve güçlü gölge ilk sürüme alınmaz. Gerekçesi: okuma için gerekli değil, düşük GPU'da maliyetli.

Sistem geri hareketinin gerçek ekran kenarına müdahale edilmez. Hem sağ hem sol dış kenar korunur. Native WindowInsetsCompat systemGestures/mandatorySystemGestures değerleriyle tutamacın bütün hit alanı sistem bölgesinin dışında tutulur; sabit piksel bandı veya parent recognizer hitSlop'u ile kenar tekrar sahiplenilmez. Korunan alanda başlayan dokunuş page/panel gesture'ına sonradan dönüştürülmez. Panel düğmesi her zaman araçlardan açılabilir. Sistem gesture insets, sağ/sol geri, dikey/yatay, zoom/seçim/panel/klavye, üç düğmeli navigasyon ve TalkBack birlikte test edilir. Predictive back iptalinde sayfa/kayıt değişmez. Bağlayıcı ayrıntı AGENTS.md bölüm 7'dedir.

## 6. Sayfa paneli, konum ve tablet düzeni

Panel telefonlarda uygun alt panel veya tam genişlikte kısa sayfa tarayıcı; tablette yan panel. Grid hücreleri cihaz genişliğine göre 2–4 sütun; mevcut sayfa görünür konuma kaydırılır. Sanal liste yalnız görünen hücreler ve küçük bir tamponu yaşatır.

"Tüm sayfaları görmek", tüm küçük resimleri aynı anda üretmek anlamına gelmez. Sayfa sayıları hemen listelenir; görseller görünür sırada tembel hazırlanır. Hızlı kaydırmada eski istekler düşürülür. PDF okumaya ayrılan render işi her zaman thumbnail işinden önceliklidir. Panele girerken PDF yeniden açılmaz.

Sayfa numarasına dokunarak 1–N arası sayıya gitme, slider sürüklerken yalnız aday sayıyı gösterme ve bırakınca hedefi açma. Yer imleri sekmesi bulunur. PDF indeksleri içeride 0 tabanlı, kullanıcıya 1 tabanlıdır. PDF'nin basılı sayfa numarası fiziksel dosya sayfasından farklı olabilir; ilk sürüm açıkça dosya sayfasını gösterir. İsteğe bağlı basılı sayfa başlangıcı eşlemesi sonraki küçük geliştirmedir.

Yatay tablette çift sayfa kullanıcı seçeneğidir; varsayılan tek sayfa düşük belleği korur. Çift sayfada kapak tek, ardından 2–3 / 4–5 dizilimi varsayımı; sağdan sola okuma ve eşleşme başlangıcı değiştirilebilir. Konum kaydı sol sayfaya kör biçimde bağlanmaz; aktif fiziksel sayfa ve spread bilgisi saklanır. Sayfa göstergesi örneğin "42–43 / 318" olur. Genişlik ve bellek yetmiyorsa tek sayfaya dönüş açık bir kullanıcı bildirimiyle yapılır.

Ekran dönüşü ve bölünmüş ekran, tablet/telefon adına göre değil kullanılabilir pencere genişliğine göre ele alınır. Sayfa ve oransal okuma noktası korunur. Yeniden boyutlandırmada eski bitmap kısa süre ölçeklenir, yeni çözünürlük hazır olunca değiştirilir; aynı anda iki tam önbellek oluşturulmaz.

## 7. PDF renklerinin teknik sınırı ve çözümü

PDF sabit yerleşimlidir. İlk sürüm yazı fontunu, puntoyu veya satır aralığını EPUB gibi yeniden düzenlemez. Okunabilirlik için zoom, genişliğe sığdır ve kitap başına manuel kenar kırpma vardır. Kırpma görünümü değiştirir; PDF dosyasını düzenlemez ve sıfırlanabilir.

Renk dönüşümü görüntü üzerinde yapılır. Koyu/açık tonları seçilen yazı/sayfa renklerine eşleyen native ColorMatrix/Paint yaklaşımı başlangıçtır. Luminans eşlemesi renkli fotoğrafı da etkiler; yalnız yazıyı seçtiğimiz iddia edilmez. Orijinal mod fotoğrafları korur. Gelişmiş eşik/gamma filtreleri ancak GPU/CPU ölçümü ve eski Android uyumu uygun çıkarsa eklenir.

Tema değişince her sayfanın yeni renkli bitmap kopyası üretilmez. Aynı kaynak bitmap çizim sırasında filtrelenir. Özel renk sürgüsü her harekette React ağacını yeniden oluşturmaz; görünüm güncellenir, kalıcı kayıt kullanıcı bırakınca yapılır. Tema önizlemesi mevcut bitmapi yeniden kullanır.

Manuel kontrast ve orijinal dönüş taranmış gri veya sararmış sayfalarda önemlidir. Renkli görselleri otomatik tespit edip ayrı koruma, OCR, bağımsız metin yeniden akışı ve içerik düzenleme ilk sürümde yok. Kullanıcıya ayarlarda kısa ve anlaşılır "Bu görünüm resimlerin renklerini de değiştirebilir" açıklaması sunulur.

## 8. Dosya yaşam döngüsü ve veri güvenilirliği

PDF ekleme Android sistem dosya seçicisiyle; telefon indirmeleri, SD kart ve seçicideki sağlayıcılar kullanılabilir. Geniş "tüm dosyalar" izni istenmez. ACTION_OPEN_DOCUMENT ve kalıcı URI erişimi desteklendiğinde alınır. URI seekable değilse veya sağlayıcı kalıcı erişim veremiyorsa güvenli yerel kopya gerekir.

Varsayılan, erişimi kalıcı yerel PDF'yi kopyalamadan açmak. "Uygulamaya kopyala" seçeneği taşınan dosyalara karşı dayanıklılık sağlar, fakat disk kullanır. Uzak sağlayıcı seçildiyse "Cihaza hazırlanıyor" durumu, biliniyorsa byte ilerlemesi ve iptal; dosya hazır olmadan kitap hazır gibi gösterilmez.

Dosya tamamen JS ArrayBuffer/base64 içine alınmaz. Yerel worker üzerinden stream kopyalama, geçici dosya, tamamlandığında atomik isim değiştirme ve SQLite transaction. İptal, eksik alan ve çökme durumunda yarım dosya temizlenir. Bilinmeyen dosya boyutunda uydurma yüzde yok. Uzun içe aktarma okuyucuyu bloke etmez.

Dosya tanımlama: kalıcı belge kimliği + URI ve mümkünse boyut/değişiklik metadata'sı. Yinelenen ekleme öncelikle aynı URI üzerinden bulunur. Tam hash gerekiyorsa yalnız import sırasında arka planda ve iptal edilebilir biçimde; her açılışta büyük PDF hashlenmez. PDF'nin ilk sayfası kapak olarak tembel üretilir. Bütün sayfaların metadata ve thumbnail taraması import şartı değildir.

Bozuk PDF, boş belge, MIME/uzantı uyuşmazlığı, eksik font, aşırı büyük sayfa, şifreli belge, iptal ve erişim kaybı ayrı hata durumlarıdır. Seçilen PDFium build'inin parola ve ilgili PDF destek kapsamı corpus ile doğrulanır. Desteklenmeyen durumda açık uyarı; destekli belge için parola yalnız oturum belleğinde, loga ve kalıcı kayda yazılmaz. Şifreli/DRM dosya içeriğini aşma özelliği yok. Metin erişimi güvenilir değilse yanlış alıntı üretmek yerine bölge işareti/not seçeneği sunulur.

Asıl PDF taşınmış veya silinmişse kitaplık kaydı ve yer imleri korunur, "Dosyayı yeniden seç" sunulur. Yeni dosyanın aynı belge olduğu boyut/metadata ve gerektiğinde hash ile doğrulanır; farklı kitaba eski sayfa kör biçimde bağlanmaz.

"Kitaplıktan kaldır" dışarıdaki orijinal dosyayı silmez. Uygulamanın sahip olduğu kopyanın silinmesi ayrı ve açık işlem. Orijinal silme ilk sürümde yok. Önbellek temizleme kayıtları ve kitapları silmez.

## 9. Kalıcı veri modeli ve durum makinesi

Tablolar: documents (id, uri/kopya yolu, ad, boyut, fingerprint, sayfa sayısı, eklenme ve son okunma); reading_positions (documentId, fiziksel sayfa, oransal zoom/odak, spread, updatedAt); book_preferences (tema, renkler, kontrast, kırpma, yön, fit mode); bookmarks (documentId, sayfa, isteğe bağlı ad); annotations (id, documentId, documentRevision, tür, renk, alıntı metni, oluşturulma/güncellenme); annotation_ranges (annotationId, fiziksel sayfa, engineVersion, karakter başlangıç/bitiş, PDF-space quad'lar, sayfa kutusu/rotasyonu, bağlam alıntısı); notes (id, documentId, isteğe bağlı annotationId/sayfa, başlık, düz metin gövde, zamanlar, revision); note_drafts (noteId/geçici id, documentId, gövde, revision); app_preferences (varsayılanlar ve cache sınırı). Belge/sayfa ve belge/tarih indeksleriyle sorgu sayfalama. URI, kişisel alıntılar ve notlar tanılama loglarına yazılmaz.

Okuyucu durumları: idle, opening, ready, rendering, importing, recoverableError, fatalError, closing. Panel ve gesture durumları ayrıca tanımlanır. Eski belge veya eski boyut isteğinin sonucu yeni kitaba bağlanmamalıdır. Her oturum ve viewport değişimine generation id eklenir. close, background ve rotation yarışları native kaynak sahipliğiyle test edilir.

Konum yalnız tamamlanmış geçişte native katmanda kayda alınır. Hızlı gezinme için kısa debounce (başlangıç 250–500 ms), en geç 1 saniyede flush; background, çıkış, kitap değişimi ve yer imi işleminde bekleyen kayıt hemen gönderilir. Android zorla öldürme veya güç kesilmesinde hiç gerçekleşmeyen son yazmanın korunacağı vaat edilmez; sınır açık, kayıp en son commit öncesi ile sınırlı olmalıdır.

SQLite migration'ları versioned ve transaction içinde; başarısız migration kullanıcının kitaplık verisini sıfırlamaz. App data yedeği ve metadata dışa/içe aktarma: sürümlü JSON, şema kontrolü ve işlem önizlemesi; yer imleri, işaretler, alıntılar, notlar ve geometri de dahil. UUID/revision ile tekrar import'ta mükerrer kayıtları önleme; çatışmaları sessizce ezmeme. Dışa aktarılan dosya kitapları otomatik içermez. Başka cihazda URI aynı değildir; PDF'yi yeniden seçme akışı vardır. Parolalar yedeğe dahil edilmez. Kitap başına okunabilir TXT/Markdown çıktısı: kitap adı, sayfa, alıntı ve ilişkili not. Orijinal PDF içine annotation yazma V1 kapsamı değildir.

## 10. PDF motoru ve iş sıralaması

Native motor tek aktif belge ve tek PDFium belge oturumu açar. Dosya tanıtıcısı, sayfa, text-page ve bitmap sahipliği açık kuralla yönetilir. PDFium/JNI işleri tek seri worker üzerinde; wrapper'ın thread safety davranışı varsayılmaz. Görünür sayfanın text-page'i yalnız gerektiğinde açılır; kapatırken text-page, page ve document sırası doğru uygulanır. UI thread veya JS thread'de render/file I/O/DB işi yok. Aynı belge için eşzamanlı motor açıp gizli native belleği çoğaltmak varsayılan değildir.

Öncelik sırası: görünür eksik sayfa/tile; aktif kullanıcının metin hit-test/seçim işi; gidilecek sayfa; muhtemel sonraki sayfa; önceki sayfa; görünür thumbnail; uzaktaki ön hazırlık. Seçim aktifken komşu ön hazırlığı askıya alınır. Ön hazırlık düşük öncelikli, kullanıcı hareket ederken askıya alınabilir. Bir iş kuyruğu sınırsız büyümez; hızlı atlamada en güncel hedef tutulur. Kulpların UI hareketi mevcut geometriyle yapılır; henüz çıkmayan aralık için arka plan isteği son hedefe birleştirilir.

Kuyruktaki eski işler iptal edilir. Başlamış senkron PDF motor çağrısı güvenle anında kesilebiliyormuş gibi davranılmaz: sonucu eskiyse atılır, aynı bitmap yeni ekrana verilmez. Uzun render UI'ı kilitlemez fakat sıradaki render'ı geciktirebilir. Aşırı ağır/bozuk PDF için hata ve düşük çözünürlük denemesi vardır. Native parser crash veya uzun takılma gerçek örneklerde görülürse ayrı renderer process mimarisi ayrıca değerlendirilir; try/catch'in native crash'i yakaladığı varsayılmaz.

RN'ye yalnız küçük veriler ve olaylar gönderilir: belge açıldı, sayfa sayısı, yerleşen sayfa, hata, import ilerlemesi (seyrek). Bitmap, frame, pixel array ve sürekli gesture koordinatları JS'ye aktarılmaz. PDF gesture ve çizimi native view'da; RN panellerinde Reanimated. Aynı harekete iki katman birden sahip olmaz.

## 11. Bellek bütçesi ve önbellek

PDF dosyasının disk boyutu RAM ihtiyacını tek başına belirlemez. Sıkıştırılmış resimler render sırasında açılır; vector karmaşıklığı süreyi etkiler. Bitmap RAM'i yaklaşık genişlik × yükseklik × 4 byte. Örnek: 1200 × 1600 ARGB_8888 bitmap yaklaşık 7,3 MiB; 2400 × 3200 yaklaşık 29,3 MiB. Tabletin tüm fiziksel RAM'i uygulamaya ait değildir.

2 GB referans için başlangıç PDF bitmap bütçesi 24 MiB: görünür sayfa, yakın sayfa, tile, thumbnail ve bitmap üretim rezervi toplamı. Normal cihaz profili başlangıç 48 MiB; bunlar heap/memory class, low-RAM işareti, viewport ve gerçek ölçümle düşürülebilir. Page count'a göre RAM büyümez. GPU kopyası, Hermes, SQLite, native PDF parser ve geçiş rezervi ayrıca ölçülür; 24 MiB toplam uygulama belleği değildir.

Varsayılan düşük bellek: mevcut sayfa + okuma yönündeki bir sayfa; önceki sayfa yalnız bütçe varsa. Üç tam çözünürlüklü sayfa koşulsuz tutulmaz. Bitmap cache byte tabanlı LRU, aktif çizilen yüzeyler pinlenir; pinli + üretilecek yeni bitmap bütçesi aşılacaksa önce komşular boşaltılır, sonra kalite azaltılır. Referansı veya GPU kullanımı devam eden bitmap recycle edilmez. Tahsis sonrası gerçek allocationByteCount kontrol edilir.

Çözünürlük ekrandaki gerçek sayfa alanına ve kalite seviyesine göre; düşük bellek normal görünüm başlangıcı 1–1,5 megapiksel civarı, okunabilirlik ölçümüyle ayarlanır. Sabit düşük DPI ile tüm cihazların metni bulanık bırakılmaz. Temel fit görüntüsü ve zoom tile'ları birlikte bütçelenir. İlk görüntüleme yolunda ARGB_8888 kullanılır; motor/pixel format/renk kalitesi doğrulanmadan "RGB_565 seçip render belleğini otomatik yarıya indiririz" varsayımı yok.

Zoom sırasında mevcut görüntü transform ile hareket eder. Gesture sakinleşince görünen bölge 256/512 px tile'lara bölünerek daha net üretilir. Tile boyutu benchmark ile seçilir. Bütün sayfa 4×/8× dev bitmap yapılmaz. Görünmeyen tile'lar kısa sürede boşaltılır. Zoom sınırı belge ve bütçeye göre; bozulmadan cihazın sınırını aşmayan netlik. Küçük çıktı bitmapi, PDF içindeki dev resmin parser belleğini bütünüyle sınırlandırmaz; ağır PDF testlerinin sebebi budur.

Thumbnail başlangıç boyutu yaklaşık 120–180 px genişlik, panelde gerçek hücreye göre ayarlanır. Düşük bellek RAM thumbnail bütçesi 1–2 MiB, toplam 24 MiB içinde. Disk thumbnail cache başlangıç 64 MiB, kullanıcı sınırı ve alan kontrolü. Tam sayfaları sürekli PNG/JPEG'ye kodlayıp diske yazma yok; ilk sürümde kalıcı cache kapak ve thumbnails için. Disk cache silinebilir, uygulama bundan toparlanabilir.

Bellek baskısı: önce bekleyen ön hazırlığı durdur; komşu ve görünmeyen thumbnail/tile'ları bırak; görünür sayfanın ölçeğini bütçe içinde tut; gerekirse tek sayfa/ekonomi modu. Ekran kilidi ve background'da ağır işlemler durur, yeniden üretilebilir sayfa bitmapleri bırakılır. Foreground'da düşük çözünürlüklü cache uygunsa hemen gösterilir, ardından netleşir. Trim callback'leri tek koruma değildir; dahili byte sınırı sürekli uygulanır.

## 12. CPU, GPU, pil ve APK boyutu

Sayfa sürükleme sırasında PDF tekrar render edilmez; hazır yüzeyler translate/scale ile çizilir. Hareket başında her frame bitmap tahsisi, database yazması veya React setState yok. Native animasyonun da UI thread'i kullandığı unutulmaz; bu thread'de pahalı işlem yapılamaz.

Kitaplık açılışında bütün kapakların decode edilmesi yok. Liste hücreleri küçük ve sabit; kararlı key, sınırlı render window, metadata sayfalama, memo/selector kullanımı yalnız anlamlı yerlerde. App-wide global state'e bitmap veya dev metadata listesi girmez. Büyük kitabın tüm sayfaları React component olarak mount edilmez. Build-time font subset veya sistem fontu; birkaç küçük ikon.

Okurken sürekli timer, background scan veya tüm kitabı sırayla prefetch yok. Önbellek ısınması okuma yönünde küçük. Batarya tasarrufu/ısınma ve OS bellek durumu uygun olduğunda kalite/ön hazırlık uyarlanır; cihaz RAM miktarı tek karar değişkeni değildir. Boşta çizim yalnız invalidation olduğunda; okuyucu her saniye yeniden render olmaz.

Release APK'da geliştirme araçları ve test PDF'leri yok. R8 ve resource shrinking ile shrink sonrası gerçek kurulum, RN Codegen/JNI ve keep kuralları test edilir. Cihaza uygun CPU APK'sı boyutu azaltabilir; gerekirse ayrı imzalı ABI APK'ları ve tek universal APK sağlanır. 32-bit desteği seçilen toolchain izin verdiği ölçüde. Native kütüphaneler için 16 KB bellek sayfası uyumu yeni Android cihazlarda test edilir.

## 13. Ölçülebilir kabul hedefleri

Hedefler 2 GB Huawei üzerinde release APK ile doğrulanacak ilk tasarım bütçeleridir. Birinci prototipte gerçek donanım ve PDF motorunun taban maliyeti görülünce belgeli biçimde revize edilebilir; kullanıcıya ölçülmüş değer gibi sunulmaz. Tipik test: yerel 100–500 sayfalı metin PDF'si ve ekran boyutunda render. Çok ağır resim/vector PDF ayrı kategoridir.

| Ölçüm | İlk hedef / kabul yaklaşımı |
|---|---|
| Soğuk kitaplık açılışı | p95 ≤ 2,5 saniye; 20 tekrar, sabit test şartları |
| Sıcak uygulamaya dönüş | p95 ≤ 1 saniye; gerekiyorsa sayfa netleşmesi ayrı ölçülür |
| Yerel tipik PDF ilk okunabilir görüntü | p95 ≤ 2 saniye; import/kopyalama hariç |
| Hazır sayfa geçişi | 60 Hz cihazda 60 FPS'e yakın; gecikmiş frame oranı hedef <%5, frozen frame yok |
| Dokunma geri bildirimi | p95 ≤ 100 ms; PDF hazır olma süresinden ayrı |
| Sayfa panelinin görünmesi | p95 ≤ 250 ms; thumbnails sonradan gelebilir |
| Metin seçimi | Aktif sayfanın metadata'sı hazırken kulp/menü yanıtı hedef p95 ≤100 ms; ilk metin hazırlığı ayrı ölçülür |
| İşaret kaydetme | Tipik kısa pasaj için DB commit hedef p95 ≤300 ms; Kaydedildi yalnız başarılı commit sonrası |
| Kitap not/işaret listesi | 1000 kayıtta yalnız ilk sayfalı sonuçlar; bütün alıntılar/notlar RAM'e yüklenmez |
| Okuma toplam PSS | İlk hedef tipik tek sayfada ≤160 MiB; tepe ≤220 MiB; GPU/native ayrıca raporlanır |
| Uzun oturumda bellek | 500 geçiş + 20 zoom + 10 dönüş sonunda sürekli büyüme yok; quiescent PSS bazına dönüş farkı hedef ≤%10 |
| Bitmap cache | Düşük profilde 24 MiB bütçe; aşım tahsis öncesi engellenir |
| APK | Cihaza uygun release için başlangıç hedef ≤30 MiB; universal için ≤50 MiB; gerçek RN tabanına göre ölçülür |
| Sağlamlık | Tanımlı test corpus ve uzun oturumda OOM / ANR / crash yok |
| Konum kaydı | Normal çıkış/background sonrası doğru sayfa; zorla öldürmede son transaction geri gelir |
| Boşta maliyet | Sürekli JS timer, render veya PDF taraması yok; sistem profiler ile doğrulama |

Normal olmayan ağır PDF'de 2 saniye garantisi yok. Arayüz yanıt vermeye devam eder, yükleme bildirimi ve iptal bulunur. Hazırlanmamış sayfada geçiş animasyonu hazır bir sayfa kadar hızlı başlatılmak zorunda değildir: eski sayfa tutulur, hedef düşük çözünürlükte hazır olduğunda geçiş yapılır. Donanım yetersizliğinde netlik tamamlanması beklenebilir; UI'ın donması kabul edilmez.

Ölçümler Metro/debug performansına göre yapılmaz. Android Memory Profiler, dumpsys meminfo, Perfetto, uygun Android sürümünde Macrobenchmark/FrameTiming, eski sürümde platform profiler/frame stats kullanılır. İlk koşu ile sıcak cache sonuçları ayrı, medyan/p95/tepe değerleri ayrı raporlanır. Yan uygulamalar, pil durumu, sıcaklık, ekran yenileme hızı ve corpus sürümü kayıtlıdır.

## 14. Kullanıcı gibi masa başı simülasyonu ve eksik analizi

| Kullanıcı senaryosu | Sorun olabilecek nokta | Planlanan çözüm / kabul |
|---|---|---|
| İlk kez açıyorum, PDF ekliyorum | Nereden başlayacağım belirsiz | Tek ana eylem; sistem dosya seçicisi; ilk kitap ipucu |
| 400 sayfalı kitaba dokunuyorum | Tüm kitabın hazırlanmasını beklemek | Önce kayıtlı sayfa, kapak ve uzak sayfalar sonra |
| Gece moduna geçiyorum | Beyaz flaş, fotoğraflar bozuluyor | Tema renkli yükleme; mevcut bitmap filtresi; orijinal düğmesi |
| Özel sarı sayfa / kahverengi yazı istiyorum | Yalnız siyah-beyaz ters çevirme yetersiz | İki uç renk eşlemesi + kontrast; resim etkisi açık |
| Sayfayı yarıya kadar çekip bırakıyorum | Yanlış sayfanın kaydedilmesi | Yerleşen sayfa authoritative; başarısız sürükleme kaydetmez |
| Hızla on sayfa geçiyorum | Render kuyruğu ve RAM büyüyor | Bounded queue, son hedef, küçük prefetch, eski sonuç atma |
| Metni büyütüp sağa kaydırıyorum | Sayfa yanlışlıkla değişiyor | Zoom modunda tek parmak pan; sayfa geçişi ayrı kontrol |
| Kenardan içeri sürüklüyorum | Android geri hareketi uygulamadan çıkarıyor | İçte tutamaç; gerçek sistem kenarı korunur; düğme alternatifi |
| Önizlemelerde 900. sayfaya gidiyorum | 900 thumbnail RAM'e yükleniyor | Sanallaştırma, yalnız görünür görseller, sayı anında |
| Tableti yatay çeviriyorum | Sayfa kayboluyor veya kitap yeniden açılıyor | Fiziksel sayfa/oransal konum korunur; eski görüntüden geçiş |
| Çift sayfa açıyorum | Metin küçük, RAM artıyor | Kullanıcı seçimi, bütçeli render; tek sayfaya kolay dönüş |
| Bildirim için başka uygulamaya geçiyorum | OS uygulamayı öldürüyor | Native konum flush; foreground yeniden açma |
| Uygulamayı zorla kapatıyorum | Son konum tümden siliniyor | Transaction kayıtları; en son commit geri gelir |
| SD kartı çıkarıyorum | Crash / kitaplık kaydını kaybetme | Erişim hatası, kayıtları koruma ve yeniden bağlama |
| PDF'yi başka klasöre taşıyorum | URI bozuluyor | Dosyayı yeniden seç; belge eşleştirme |
| Disk doluyken PDF ekliyorum | Yarım kitap hazır görünüyor | Geçici kopya, atomik commit, iptal/temizlik |
| Kapaklar görünmüyor | Cache silinmesini dosya kaybı sanmak | Kapaklar tembel yeniden üretilir; metadata korunur |
| Kitaplığı temizliyorum | İndirilen orijinal PDF siliniyor | Kaldırma ile dosya silme ayrılır |
| Parola isteyen PDF açıyorum | Sonsuz loading veya parolanın loglanması | Desteklenen motor/API'de oturum parolası, aksi halde açık hata |
| Yeni APK ile güncelliyorum | İmza veya migration yüzünden kayıt kaybı | Aynı package/signing key, transaction migration, upgrade testi |
| Üç satır seçip altını çiziyorum | Sadece resmin üstüne gelişigüzel çizgi; alıntı kayboluyor | Gerçek metin aralığı + satır quad'ları + alıntı snapshot'ı transaction ile |
| Altı çizilenler listesine dönüyorum | Hangi kitabın hangi sayfası belirsiz | Kitap kapsamlı liste, sayfa/satır bilgisi, metin ve tek dokunuşla konuma dönüş |
| Altı çizilmiş yere not ekliyorum | Notun kitap/pasaj bağlantısı kayboluyor | documentId ve isteğe bağlı annotationId; bağlantılı not kartı |
| Not yazarken uygulama kapanıyor | Yazdıklarım siliniyor | Yerel draft autosave, revision sırası ve yeniden açılışta kurtarma |
| Taranmış PDF'de metin seçiyorum | Olmayan metin çıkarımı varmış gibi davranış | OCR'sız gerçek metin yok açıklaması; sayfa/bölge işareti ve kendi notu |
| Renk/kırpma/dönüş değiştiriyorum | Çizgiler metinden kayıyor | PDF-space geometri + aynı viewport transform + filtre sonrası overlay |
| Tabletim Google servislerine sahip değil | Uygulama açılmıyor | GMS/HMS/hesap/analytics bağımlılığı yok |
| TalkBack veya büyük yazı kullanıyorum | Gizlenen araçlar ve taşan düğmeler | Erişilebilir araçlar, focus, sistem ölçeklemesi ve düğme alternatifi |

Simülasyonun ortaya çıkardığı temel eksikler: renkli PDF'lerde metin ve resim ayrımı; basılı sayfa/dosya sayfası farkı; eski Android'de parola/metin özellikleri; URI erişim kaybı; zoom/sayfa gesture çakışması; native render iptal edilebilirliği; GPU belleğinin bitmap bütçesine ek maliyeti; ABI ve OS uyumluluğu. Bu belge her biri için görünür davranış ve teknik kontrol tanımlar; hiçbiri yalnız "sonra optimize ederiz" maddesi olarak bırakılmaz.

## 15. Test matrisi ve sürüm kapsamı

Belge corpus: küçük ve 500/1000+ sayfalı metinli PDF; düşük/yüksek çözünürlüklü tarama; 100/300+ MB resimli PDF; karma vector; farklı sayfa boyutları/yönleri; geniş beyaz kenar; renkli görseller; Türkçe karakterler ve gömülü font; şifreli/bozuk/kısmi PDF; sıfır byte ve yanlış uzantı; SD kart/content provider; çok büyük tek sayfa. Gerçek kişisel dosyalar tanılama deposuna veya source control'e kopyalanmaz.

Cihaz matrisi: kullanıcının 2 GB Huawei tableti birincil; 2/3 GB telefon; 4+ GB tablet; seçilen en düşük desteklenen Android; güncel Android; 32 bit gerekiyorsa gerçek uyumlu cihaz; 16 KB sayfalı yeni Android testi. Emülatör doğruluk için yararlıdır, gerçek tablet performansının kanıtı değildir.

Fonksiyonel testler: sayfa indeks sınırları; spread eşleme; kuyruk önceliği ve byte eviction; generation ile eski sonucu reddetme; erişim kaybı; SQLite migration ve yarım import kurtarma; renk dönüşümü doğruluğu; konum kayıt yarışları; metin-hit-test ve quad transform; not draft revision; işaret+aralık transaction; alıntı Unicode/ligature dönüşümü; not/işaret yedek tekrar import. E2E: PDF ekle/oku/tema/zoom/panel/yer imi/metin seç/çiz/not yaz/yeniden aç/güncelle. UI testleri deterministik corpus ile. Performans testleri aynı release artifact üzerinden.

Uzun oturum: 30–60 dakika aktif okuma; 500 sayfa hareketi, tekrarlı zoom/tema/dönüş; 20 kitap aç/kapat; ekran kilidi, uygulama değişimi, OS process kill, cache silme ve düşük depolama. Test sırasında crash/ANR, FD/sayfa/bitmap sızıntısı ve kalıcı bellek büyümesi izlenir. İşlem öldürme ve elektrik kesintisi aynı olay gibi yorumlanmaz.

V1: kitaplık; sistemden PDF açma/paylaşım yoluyla açma; tek sayfa; kullanıcı seçmeli uygun çift sayfa; sakin geçiş; zoom; sayfa paneli; numarayla gitme; temalar/özel renk/kontrast; manuel kenar kırpma; kaldığın yer; yer imleri; gerçek metin seçme; altını çizme/vurgulama; kitap başına Altı çizilenler ve Notlar sekmeleri; kitap/genel-sayfa-pasaj notları; taramalarda manuel bölge işareti; yön/parlaklık/ekran açık seçenekleri; ekonomi modu; erişim/hata kurtarma; işaret/not dahil yedek ve kitap başına TXT/Markdown çıktısı; imzalı APK.

V1 dışı ama kayıtlı: OCR, bağımsız yazı akışı/punto, sesli okuma, bütün PDF içinde metin arama, serbest kalem çizimi, EPUB, bulut eşitleme, mağaza, otomatik klasör tarama, gelişmiş içindekiler çıkarma, bir gesture ile sayfalar arası uzun seçim, orijinal PDF içine annotation gömme, 3D sayfa kıvrılması. Metin seçme, altını çizme, vurgulama ve yazılı notlar yeni kullanıcı isteğiyle V1'e alınmıştır. Kitaplık başlık araması ve kaydedilmiş işaret/not içinde arama V1'de; tüm PDF içerik araması bununla karıştırılmaz. PDF linkleri ve yerleşik outline desteği engine/API keşfine bağlı ayrı özellik; V1'de dokunulabilir gibi gösterilmez.

## 16. Uygulama aşamaları ve tamamlanma kapıları

Detaylı sıra, kullanıcı senaryoları ve her aşamanın ID'li kabul kriterleri [MVP_PLANI.md](MVP_PLANI.md) dosyasındadır. Önceki beş genel iş alanı, bir teknik kapı ve altı artımlı kullanıcı teslimine ayrılmıştır:

1. **MVP-0:** cihaz/release/tek PDFium render+text doğrulaması ve baseline ölçüm.
2. **MVP-1:** kişisel PDF ekleme, temel okuma ve kaldığın yer.
3. **MVP-2:** yumuşak geçiş, tile zoom, renk/crop ve ergonomi.
4. **MVP-3:** sanal sayfa paneli, yer imleri, tam kitaplık ve çift sayfa.
5. **MVP-4:** gerçek metin seçme, işaretleme ve Altı çizilenler.
6. **MVP-5:** kitap/sayfa/pasaj notları ve taslak kurtarma.
7. **MVP-6:** backup/restore/export, tam recovery, birleşik test/update ve final imzalı APK.

Performans ve veri güvenilirliği bütün aşamalarda ilgili hedeflerle doğrulanır; son aşamaya ertelenmez. Motorun gerçek text kabiliyeti MVP-0'da doğrulanır, tamamlanmış seçim arayüzü MVP-4'te teslim edilir. V1 kapsamı değişmez; tam V1 kabulü MVP-6'nın geçmesidir.

İlerleme takvime göre değil kapıların geçmesine göre kabul edilir. Özel native motor ve zoom cache ciddi mühendisliktir; küçük bir hafta sonu uygulaması gibi planlanmaz. Cihaz ve corpus görülmeden kesin bitiş tarihi verilmez. En erken somut teslim kurulum ve motor prototipidir; yeni özellik ekleme bu performans doğrulamasını ertelemez.

## 17. Skill, çalışma disiplini ve teslim

Gerekli uzmanlık: React Native/TypeScript; Fabric/Codegen/TurboModules; Kotlin/Android dosya ve yaşam döngüsü; PDFium/JNI ve tile rendering; metin hit-test, PDF koordinatları, glyph/line grouping ve Unicode; byte sınırlı cache ve kaynak sahipliği; gesture/animasyon; tablet ergonomisi ve erişilebilirlik; SQLite transaction/migration/draft persistence; Android profiling ve imzalı APK.

Mevcut pdf:pdf skill'i geliştirme sırasında corpus yapısı ve görsel sonuçları incelemek için kullanılabilir; RN uygulaması veya canlı Android testi yerine geçmez. Kullanıcı isteğiyle skill-creator kullanılarak dört proje skill'i `~/.codex/skills/` altında oluşturuldu: kitap-rn-pdf-engine, kitap-reader-gestures, kitap-annotations-storage, kitap-android-release. Skill seçme kuralları ve görevleri AGENTS.md bölüm 18'dedir. Release skill'i gerçek ölçüm JSON'unu bütçelerle karşılaştıran standart Python aracı içerir; uygulama performansını kendisi ölçmez. Web Sites, ASP.NET ve canlı Excel skill'leri bu proje için gerekli değildir.

Proje AGENTS.md oluşturuldu; ürün kapsamı, tek PDFium motoru, JS/native sınırı, byte bütçeleri, iki dış kenarda sistem geri davranışı, annotation/not kalıcılığı, dosya kurtarma, kabul kriterleri ve teslim kuralları ayrıntılı tanımlıdır. Otomatik ölçüm eşikleri `docs/acceptance/performance-budgets.json` dosyasındadır. Bu bağlam/skill dosyalarının oluşturulması uygulamanın geliştirilmiş veya cihazda çalışmış olduğu anlamına gelmez.

Teslim: kaynak kod, dependency lockfile, tekrar üretilebilir build notları; imzalı cihaza uygun APK ve gerekirse universal APK; package id/version code; APK hash; test ve ölçüm raporu; cihaz listesi ve destek sınırları; kurulum, güncelleme, yedekleme ve kurtarma talimatı. İmza anahtarı güvenli ve kullanıcı kontrolünde saklanır; repository'ye eklenmez. Sonraki APK aynı imzayla güncellenir; kaldırıp yeniden kurma önerisi kayıt kaybını gizleyen çözüm olmaz.

## 18. Resmi dayanaklar

- [React Native performans](https://reactnative.dev/docs/performance): JS/UI ayrımı ve release üzerinde ölçüm.
- [Hermes](https://reactnative.dev/docs/hermes): RN ile uyumlu paketli JS motoru.
- [Fabric](https://reactnative.dev/architecture/fabric-renderer): yerel görünüm entegrasyonu.
- [Android PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer): renderer kaynak yaşam döngüsü ve sürüme bağlı özellikler.
- [PdfRenderer.Page](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page): bitmap rendering ve bölgesel üretim.
- [Android bitmap belleği](https://developer.android.com/topic/performance/memory/guide/bitmaps): boyut ve pixel biçimi maliyeti.
- [Sistem belge erişimi](https://developer.android.com/training/data-storage/shared/documents-files): kalıcı URI izinleri ve dosya taşınma sınırları.
- [Android performans incelemesi](https://developer.android.com/topic/performance/inspecting-overview): profiler, izler ve cihaz ölçümü.
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview): tekrarlanabilir açılış/etkileşim ölçümleri.
- [React Native Android imzalama](https://reactnative.dev/docs/signed-apk-android): release paketleme ve imza.
- [PDFium metin API'si](https://pdfium.googlesource.com/pdfium/+/refs/heads/main/public/fpdf_text.h): metin çıkarma, karakter konumu ve seçim için hit-test altyapısı.

## 19. Metin seçme, altını çizme ve kitap notları — ayrıntılı sözleşme

### 19.1 Kullanıcı akışı

Metin taşıyan PDF'de kelimeye yaklaşık 400–500 ms uzun basınca kelime seçilir. İki kulp sürüklenerek bir veya birden çok satır seçilir. Kulplar parmak altında kaybolmayacak kadar büyük dokunma alanına sahiptir. Menü seçimi kapatmaz veya kitabı kaydırmaz: Altını çiz, Vurgula, Not ekle, Kopyala. Varsayılan alt çizgi metni örtmeyen ince çizgi; vurgulama hafif saydam dolgu. Renk seçimi küçük ve kitap temasına uygun; gece modunda okunabilir çizgi rengi korunur.

Seçim bir sayfa içinde çok satırlı olabilir. V1'de sayfa dışına sürükleyip bir sonraki sayfaya kesintisiz seçim yok; farklı sayfalarda ayrı işaretler yapılabilir. Veri modeli bir annotation için çoklu page range destekler; gelecekte sayfalar arası seçim migration gerektirmeden genişletilebilir. Birden çok sütun ve döndürülmüş yazıda otomatik satır sırası kesin kabul edilmez; seçilen metin önizlenir, belirsiz geometri yanlış sütunu işaretlemek yerine sınırlı seçim sunar.

Seçim sırasında normal sayfa geçiş gesture'ı askıya alınır. Seçimden çıkma: boş alana dokunma veya geri. İki parmak zoom gerekirse seçimi sonlandırıp zoom'a geçer; işaretlenmiş kayıt etkilenmez. İşaret kaydı idempotent komutla; çift basma mükerrer işaret oluşturmaz. Aynı bölgede farklı bilinçli işaretler desteklenir, üst üste işaret menüsünde seçim listesi açılır.

Kaydedildi geri bildirimi yalnız transaction başarıyla tamamlanınca. Başarısız DB yazısında seçim ekranda tutulur; "Kaydedilemedi, tekrar dene" sunulur. Kaydedilen işaretin rengi/türü değiştirilebilir, ilişkili not düzenlenebilir. Silmede kısa geri al imkanı; ilişkili not varsa notu koruma veya ikisini silme açık seçeneği, sessiz not silme yok.

### 19.2 Altı çizilenler sekmesi

Her kitap için ayrı liste. Her kart: fiziksel sayfa, seçilen metin, çizgi/vurgu türü ve rengi, varsa not özeti ve tarih. Varsayılan sayfa sırası; tarihe göre sıralama, renk/tür filtresi ve kayıtlı metin/not içinde arama. Liste yalnız o kitabın kayıtlarını getirir; SQL pagination ve indekslerle 1000 kayıtta bile tüm alıntıları yüklemez. Varsayılan kartlarda sayfa screenshot'ı yok; bir işaretin her defasında bitmap kopyası tutulmaz.

Karta dokununca PDF'nin ilgili fiziksel sayfası açılır, işaretli satırlar viewport'a getirilir; kısa ve sakin belirginleştirme, hareket azaltma varsa statik seçili çerçeve. Okuduğun yerden işarete gitmeden önce geri dönüş noktası tutulur; "Okumaya dön" kullanıcının önceki konumunu geri açar. PDF erişimi kayıpsa alıntı ve not listesi halen kullanılabilir; "Dosyayı yeniden seç" gösterilir.

"Hangi satır" bilgisi kayıtlı satır quad'ları ve alıntı ile kesin konuma dönüş anlamına gelir. PDF'lerde standart satır numarası yoktur. Uygulama hesapladığı görsel satır numarasını yalnız güvenilir tek-sütun akışta "uygulamanın satır sırası" olarak ek bilgi sunabilir. Ana gösterim "Sayfa 42 · 3 satır" + alıntı ve konuma gitmedir; PDF'de olmayan resmi satır numarası uydurulmaz.

### 19.3 Notlar sekmesi

Üç bağlantı türü: kitap hakkında genel not; belirli sayfaya bağlı not; seçilmiş pasaja/işarete bağlı not. Düz metin başlık ve gövde; büyük rich text editörü/HTML/WebView bağımlılığı yok. Pasaj notunda alıntı ve sayfa sabit üst bilgi; not değiştirildiğinde alıntı ayrı kalır. Kitap notları kitabın Notlar sekmesinde, pasaj notları hem Notlar hem Altı çizilenler kartından açılır.

Kaydet işlemi açık; yazarken başlangıç 500–800 ms debounce ile yerel taslak. UI "Kaydediliyor / Kaydedildi / Kaydedilemedi" durumunu DB cevabından alır. Revision numarası geç gelen eski write'ın yeni içeriği ezmesini önler. Editörden çıkış, background ve klavye kapanmasında bekleyen draft flush; OS kill'de en son başarılı draft geri gelir. Henüz diske yazılmamış son tuşun güç kesilmesinde korunacağı vaat edilmez. Kaydet, taslağı final note'a transaction ile taşır; düzenlenmiş taslak ile kaydedilmiş sürüm ayrıdır.

Tablet klavyesi açıldığında editör metni ve Kaydet düğmesi görünür kalır; Android back önce klavyeyi kapatır. Editör kapandıktan sonra okuma sayfası/zoom korunur. Çok uzun notlarda giriş uzunluğu ve autosave sıklığı sınırlanır; büyük metni her tuşta SQLite'a yazmak yok. Notları sayfa/tarih sırasıyla listeme, başlık/gövde araması, düzenleme, silme/geri alma ve kitap başına dışa aktarma vardır.

### 19.4 Geometri ve PDF revizyonu

Ekran x/y'si kalıcı konum olarak saklanmaz. Kaydedilen konum PDF'nin user-space koordinatları ve satır/kelime quad'larıdır. Aynı sayfa kutusu (MediaBox/CropBox), sayfa rotasyonu, uygulama kırpması, fit, zoom ve pan dönüşümü hem sayfaya hem overlay'e uygulanır. Ölçek değişince alt çizginin kullanıcı için makul ekran kalınlığı korunur. Eğik/döndürülmüş satır yalnız yatay rect çizgisiyle yanlış işaretlenmez. Tema filtrelenmiş sayfanın üstüne annotation overlay çizilir; işaret rengi sayfa filtresinde yanlışlıkla tersine çevrilmez.

Karakter indeksleri tek başına kalıcı referans değildir. Engine sürümü/fingerprint, PDF-space geometri, seçilen metin ve kısa ön/arka bağlam birlikte kaydedilir. PDF değişmediyse geometri korunur; PDF başka baskıyla değişmişse kayıtlar otomatik yeni sayfalara yapıştırılmaz. "Belge değişmiş olabilir" uyarısı, eski alıntıyı koruma ve kullanıcıyla yeniden eşleme. Bozuk Unicode mapping/ligature, Türkçe İ/ı/ş/ğ, satır sonu tireleri ve sağdan sola metin corpus testidir. Snapshot metin kullanıcının seçtiği görünümle karşılaştırılır; boş/bozuk çıkarım güvenilir alıntı gibi kaydedilmez.

### 19.5 Taranmış kitaplar

Resimden oluşan ve OCR katmanı olmayan PDF'de gerçek kelime seçimi yoktur. Seçim denemesinde kısa açıklama ve "Bölgeyi işaretle / Sayfaya not ekle" seçenekleri. Manuel bölge işareti aynı PDF-space geometriyle saklanır; kullanıcı isterse alıntıyı kendisi yazabilir, kaynağı "kullanıcı metni" olarak tutulur. Alıntı yoksa kart sayfa/bölge bilgisi ve notla açılır; sanki metin çıkarılmış gibi gösterilmez.

OCR ilk sürümde yok: offline OCR ek model ve ağır işlem ister. Kullanıcı tüm taramalarda da gerçek metin seçimi talep ederse ayrı on-demand OCR kapsamı planlanır; bütün kitabı içe aktarırken tarama yapılmaz, APK/bellek etkisi ayrıca kabul edilir. Genel kitap ve sayfa notları PDF türünden bağımsız çalışır.

### 19.6 Ek performans bütçesi ve kabul

Metin extraction bütün kitap için yapılmaz. Görünür sayfa ilk seçimde veya sayfa sakinleşince düşük öncelikle hazırlanır; sonraki sayfaların text metadata'sı zorunlu prefetch değildir. Uygulama tarafı türetilmiş text/geometri cache başlangıç hedefi toplam 4 MiB; önceki sayfa koşulsuz tutulmaz. PDFium text-page'in iç native maliyeti bu kontrol edilen cache'den ayrıdır ve PSS/native profiler ile ölçülür. Aşırı karakter/geometri sayısı buffer sınırlarıyla ele alınır; overflow veya sınırsız JSON üretimi yok.

Aktif seçim hit-test ve overlay native'de; her kulp hareketinde JS'ye tüm glyph listesi gönderilmez. JS yalnız seçimin metin özeti, menü durumu ve kayıtlı işaret id'sini alır. Annotation overlay görünür sayfadaki işaretleri çizer; bütün kitabın işaretleri her frame dolaşılmaz. Uzun notlar ve listeler sayfalı okunur. Kitabın 1000 işareti APK veya sayfa bitmap sayısını artırmaz.

Kabul: işaret kaydedip uygulamayı yeniden açınca alıntı/geometri/not tam geri gelir; zoom, rotation, crop ve gece modunda işaret yerini korur; karta dokununca doğru sayfa ve satırlar görünür; not yazıp background/kill sonrası son başarılı draft kurtulur; 1000 kayıt listesi responsive kalır; 2 GB Huawei'de render ve seçim birlikte bellek hedefleriyle ölçülür; seçilebilir ve tarama PDF davranışları ayrı test edilir. Bu özellikler artık V1 tamamlanma şartıdır.
