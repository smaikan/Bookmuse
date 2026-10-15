> 15 Ekim 2026 son kullanıcı kararı: [Arayüz sadeleştirmesi](ARAYUZ_SADELESTIRME.md) bu belgedeki çelişen menü/ayar/yedek kararlarından önce gelir. Kart menüsünde mevcut Notlar / Altı çizilenler / Favori korunur; Adı düzenle ve Kaldır sona eklenir (toplam beş). Ayarlar dişliyle doğrudan tek ekranda açılır. Yedek/depolama ve bağımsız kitap yönetimi kaldırılmıştır.

# Kitap — UX-MVP-1–6 uygulama ve kabul planı

15 Ekim 2026. Kullanıcının onayladığı arayüz yenilemesidir; eski MVP-0–6 teslimleriyle karıştırılmaz. Kaynak/build, emülatör doğrulaması ve fiziksel Huawei kabulü ayrı durumlardır.

## Bağlayıcı kararlar

Yalnız kapak ızgarası; tek kitaplık scroll alanı; her kartta gerçek ilk sayfa kapağı, iki satır başlık, `(45/80)` ve sağ üst ⋮. Menü yalnız Notlar / Altı çizilenler / Favoriye ekle–çıkar. Sıralama ve Tümü/Favoriler filtresi farklı kontrollerdir. Pencere 320–599/600–839/840–1199/1200+ dp için 2/3/4/5 sütun; çok dar pencerede 1; dış boşluk 16, aralık 12 dp. Son satır genişlemez.

Okuyucu başlıksız ve tam ekran, normal ölçekte kırpmasız/oranı korunmuş tam sayfadır. Boşluk sayfa arka planıyla dolar. Dokunma katmanı PDF'yi küçültmez; üç işlem Sayfaya not ekle / Kitaba not ekle / Metin seç–Altını çiz ve ayrı küçük Sayfalar simgesidir. Görünüm/zoom/listeler bu menüde bulunmaz. Katman 160 ms opacity; blur ve otomatik kaybolma timer'ı yok. Kitaplık/Ayarlar gezinmesi okuyucuda görünmez.

Görünüm tüm kitaplarda ortaktır; başlangıç orijinal, kontrast 1, kırpmasız tam sayfa, tam ekran, tek sayfa. Eski kitap tercihleri korunur ama uygulanmaz. Ana Ayarlar: görünüm, düzen, ekran/erişilebilirlik, kitap yönetimi, yedek/depolama. Rename/relink/export/remove bu yönetimdedir.

Bütün yeni ve eski metin işaretleri #FFEB3B, yaklaşık %32 alfa sarı fosforlu highlight olur. Kimlik/alıntı/quads/tarih/not bağları korunur, değişen revision artar, eski stiller yerel kurtarma tablosunda tutulur. Region işaretleri dönüşmez. Her satır kendi quad'ıyla, filtre sonrası çizilir; üst üste sarılar koyulaşmaz. Orijinal PDF değiştirilmez. Seçim araçları Sarıyla işaretle/Kopyala/Pasaja not ekle/Seçimi kapat; metinsiz PDF'de region alternatifi.

Normal ölçekte tek parmak swipe, zoom'da pan, iki parmak 1–5× zoom; her stream tek sahibindir. %22 genişlik veya mevcut mesafe/hız eşiği, en çok 220 ms settle; mevcut drag konumundan devam. İptal konumu değiştirmez. İki dış OS kenarı korunur; ikinci parmak da korunan alandan başlayamaz. Çift sayfada ortak native koordinasyon.

Minimum ölçekten %18 ilave sıkıştırma overview'u hazırlar; %10 üstüne geri açılma iptal eder, parmak bırakma commit eder. Zoom'dan 1×'e sadece dönmek açmaz. Mevcut sayfaya konumlanan üç sütunlu sanal overview; sayfa numarası, kapat, numarayla git, yer imleri ve erişilebilir navigation. Liste scroll'u konumu değiştirmez; geri eski sayfa/zoom/odağı korur. Sayfa seçimi gerçek presentation sonrası kaydedilir. Yaklaşık 200 ms açılış; reduce-motion kısa fade.

## Aşamalar ve kabul

| Aşama | Kapsam | Zorunlu kabul |
|---|---|---|
| UX-MVP-1 | Kapak kitaplığı, ⋮, favori, sorgu/tercihler, konum bariyeri | U1-01 responsive sütun; U1-02 uzun başlık/kapaksız/son satır; U1-03 tek scroll; U1-04 üç işlem menü; U1-05 kalıcı favori; U1-06 arama+filtre+sıra; U1-07 45→kapat→(45/80)→45; U1-08 iptal konumu korur; U1-09 veri/orijinal koruma |
| UX-MVP-2 | Tam ekran, overlay, ortak Ayarlar | U2-01 sakin açılış; U2-02 dört kenar/oran; U2-03 doğru boşluk; U2-04 overlay boyut değiştirmez; U2-05 yalnız izinli işlemler; U2-06 ortak ayar; U2-07 focus/insets; U2-08 rotation/split-screen |
| UX-MVP-3 | Sarı overlay, seçim/not, eski stiller | U3-01 PDF üzerinde sarı; U3-02 reopen; U3-03 eski kimlik/notlar korunur; U3-04 sütun/Unicode/rotation; U3-05 transform; U3-06 temada okunur; U3-07 sil/undo/jump; U3-08 idempotent retry; U3-09 tarama; U3-10 draft/not regresyonu |
| UX-MVP-4 | Swipe/pinch/pan native koordinasyon | U4-01 kesintisiz drag/settle; U4-02 kısa/diyagonal iptal; U4-03 ilk/son; U4-04 pan; U4-05 pointer; U4-06 hareketli overlay; U4-07 cancel/lifecycle/generation; U4-08 OS kenarları |
| UX-MVP-5 | Over-pinch, üç sütun overview | U5-01 1× dönüş açmaz; U5-02 eşikler/iptal; U5-03 üç sütun/current; U5-04 bounded thumbnail; U5-05 dönüş odağı; U5-06 presentation commit; U5-07 placeholder seçimi; U5-08 simge/erişilebilirlik/geri |
| UX-MVP-6 | Final corpus/update/release/profiling | U6-01 veri/anchor/konum; U6-02 install-r; U6-03 v1/v2 dedupe; U6-04 OOM/ANR/crash; U6-05 TalkBack/font/reduce-motion; U6-06 Metro'suz release; U6-07 dürüst fiziksel kabul |

Sıra 1→2→3→4→5→6. Her aşama artifact/hash ve kriter kanıtı verir. Önceki APK'lar saklanır. Önceki davranışın regresyonu düzeltilmeden sonraki aşama geçti sayılmaz. Cihaz yoksa bağımsız kaynak/build işi devam eder; fiziksel kabul açık kalır.

## Veri ve teknik sınırlar

SQLite 5→6 favori/kitaplık tercihleri; 6→7 sarı stil dönüşümü. Ortak reader defaults uygulama tercihleridir. Backup v2 favori ve ortak ayarları kapsar, v1 okunur; eski yedekte olmayan favori mevcut kitapta korunur. Normalize staging'de digest öncesi idempotent yapılır; eski per-book görünüm ortak ayarı değiştirmez. Migration hatasında reset yok. Son gerçekten sunulmuş sayfa normal close öncesi kalıcılaştırılır; force-kill son commit'i korur.

Tek PDFium worker/session; Fabric'te native gestures/çizim, JS'ye küçük olaylar. Her frame React update, screenshot/base64, tam kitap render/index yok. Bitmap 24 MiB, text 4 MiB, thumbnail 1–2 MiB alt bütçe. Touch p95≤100 ms; overview p95≤250 ms; hazır seçim≤100 ms; commit≤300 ms; delayed frames<%5/frozen0; PSS160/peak220 MiB; durulmuş büyüme≤%10; APK30/universal50 MiB.

## Test ve tamamlanma

Host: migration/transaction/favori/sorgu/backup, yellow normalize/retry, transform, gesture eşiği/cancel/cache. Gerçek emulator: kapak/filter/menu, tam ekran ve overlay ölçüsü, sarı selection/not/reopen, swipe/pinch/overview ve konum. Corpus tipik/uzun/tarama/farklı oran/rotation/ağır/hatalı PDF. Private PDF ve raw notlar repo'ya girmez.

Final kullanıcı akışı: import→favori→oku→overlay→zoom/pan→swipe→sarı mark→pasaj notu→overview jump→kapat→kart→reopen→backup/restore. Fiziksel release: 30–60 dk, 500 geçiş, 20 zoom, 10 rotation; emulator Huawei/2 GB/FPS/PSS kabulü değildir. İmza/ABI/R8/JNI/16 KB statik, install-r ve Metro'suz final APK raporlanır. Ölçülmeyen kriter geçmez.

## Güncel durum

UX-MVP-1–6 kaynak ve imzalı APK'lar üretildi. Final 0.8.0/code15'te native thumbnail/PDF clip regresyonu düzeltildi; raporlanan host/emülatör işlev kontrolleri tamamlandı. 95 native + 31 Jest, TypeScript/ESLint ve statik imza/JNI/DEX/16 KB kontrolleri geçti. Gerçek emülatörde10 Downloads PDF,1000 sayfa,taramalar/farklı oranlar,2/3/4/5 sütun,%200 font,sarı/pasaj/draft/restore,pinch/wipe ve veri korunması kanıtları vardır. Bütün50 kriteri topluca geçmiş sayma; kanıt ve açık sınırlar [final raporda](acceptance/reports/UX-MVP-6.md). Fiziksel Huawei, tam erişilebilirlik/OS geri matrisi ve sayısal performans kabulü açık, canonical sonuç INCOMPLETE. Önceki APK'lar tarihsel artifact olarak korunur. Kullanıcıya her aşama geçişi bildirilir.

## Kriterlerin tam davranış sözleşmesi

Bu tablolar beklenen davranıştır; uygulandığı veya testten geçtiği anlamına gelmez. Gerçek sonuç ve test kapsamı aşama raporundadır.

| ID | Beklenen sonuç |
|---|---|
| U1-01 | Telefon/tablet ve dikey/yatay pencerelerde 320–599/600–839/840–1199/1200+ dp için 2/3/4/5 sütun; çok dar pencerede tek sütun. |
| U1-02 | Uzun başlık, eksik kapak ve son satır kitapları düzeni bozmaz; son tek kart satıra yayılmaz. |
| U1-03 | Kart üzerinden başlayan dikey hareket bütün kitaplığı kaydırır. |
| U1-04 | ⋮ yalnız Notlar / Altı çizilenler / Favoriye ekle–çıkar içerir; dışarı dokunma ve geri kapatır, kitabı açmaz. |
| U1-05 | Favori yeniden açılışta korunur; başarısız DB işlemi yanlış durum göstermez. |
| U1-06 | Arama, favori filtresi ve sıralama birlikte doğru sonuç verir; tercihler saklanır. |
| U1-07 | Sayfa 45'te normal kapat → kart `(45/80)` → yeniden açılış 45 olur. |
| U1-08 | İptal edilen geçiş ve genel görünümde gezinme okuma konumunu değiştirmez. |
| U1-09 | Kitap kaldırma dış orijinale dokunmaz; not ve işaret işlemleri erişilebilir kalır. |
| U2-01 | Kitap ilk açılışta başlık, sayaç, tutamaç ve alt gezinme olmadan tam ekran görünür. |
| U2-02 | Normal ölçekte dört sayfa kenarı görünür; içerik kırpılmaz/esnetilmez. |
| U2-03 | Farklı oranlı sayfalardaki boşluk yalnız etkin arka planla dolar; başka sayfa/içerik görünmez. |
| U2-04 | Dokunma katmanı PDF'nin ölçüsünü, ölçeğini ve konumunu değiştirmez. |
| U2-05 | Menüde tema, zoom, Notlar listesi veya Altı çizilenler listesi yoktur. |
| U2-06 | Ana Ayarlar'dan yapılan ortak değişiklik farklı kitaplarda uygulanır; fiziksel sayfa korunur. |
| U2-07 | Gizli kontroller dokunma/TalkBack odağı almaz; görünür kontroller sistem alanlarına taşmaz. |
| U2-08 | Rotation ve split-window aynı fiziksel sayfayı korur. |
| U3-01 | Yeni işaret, seçim kapandıktan sonra PDF üzerinde sarı şerit olarak kalır. |
| U3-02 | Kitap ve uygulama kapatılıp açılınca aynı metin aynı konumda işaretlidir. |
| U3-03 | Eski alt çizgi ve renkli metin vurguları sarıya döner; kayıt sayısı ve not bağlantısı değişmez. |
| U3-04 | Çok satır/sütun, Türkçe ve döndürülmüş metinde yanlış alan boyanmaz. |
| U3-05 | Zoom, pan ve yön değişimi işareti metinden ayırmaz. |
| U3-06 | Gece dahil bütün temalarda harfler okunur; sarı filtreyle tersine çevrilmez. |
| U3-07 | Silme/geri alma ve listeden işaret konumuna gitme doğru çalışır. |
| U3-08 | Çift basma, kayıt hatası ve tekrar deneme mükerrer kayıt oluşturmaz. |
| U3-09 | Metinsiz tarama gerçek kelime seçimi varmış gibi davranmaz; bölge/not alternatifi verir. |
| U3-10 | Sayfa, kitap ve pasaj notu ile taslak kurtarma mevcut kayıtları korur. |
| U4-01 | Sürükleme boyunca sayfa parmağı izler; bırakmada animasyon aynı pikselden devam eder. |
| U4-02 | Kısa/diyagonal hareket yanlış sayfaya geçmez ve konumu değiştirmez. |
| U4-03 | İlk/son sayfada döngü veya boş sayfa oluşmaz. |
| U4-04 | Zoom'daki tek parmak pan sayfa çevirmeye dönüşmez. |
| U4-05 | Pointer ekleme/kaldırma görüntüyü sıçratmaz; kalan tek parmak yanlış gezinme başlatmaz. |
| U4-06 | Kayıtlı işaretler swipe sırasında kendi sayfalarıyla hareket eder. |
| U4-07 | İptal, rotation, background ve hızlı girişler eski görüntüyü yeni oturuma bağlamaz. |
| U4-08 | Sağ ve sol dış sistem geri alanı korunur; iki parmak da iç alandan başlamalıdır. |
| U5-01 | Zoom'u yalnız 1× düzeyine getirmek genel görünümü açmaz. |
| U5-02 | Ek %18 sıkıştırma hazırlar; eşik altı veya %10 üstüne geri açılan hareket iptal olur; bırakma commit eder. |
| U5-03 | Genel görünüm tam üç sütundur, fiziksel sıradadır ve mevcut sayfanın satırında açılır. |
| U5-04 | Binlerce sayfalı kitapta tüm küçük resimler aynı anda üretilmez veya bellekte tutulmaz. |
| U5-05 | Genel görünümde kaydırıp geri dönmek eski sayfa, zoom ve odağı korur. |
| U5-06 | Seçilen hedef gerçekten gösterilince sayaç ve SQLite konumu güncellenir. |
| U5-07 | Küçük resim hazır değilken fiziksel sayfa numarasıyla seçim yapılabilir. |
| U5-08 | Sayfalar simgesi, TalkBack ve geri aynı görünüm/sonuca ulaşır. |
| U6-01 | Açık veri kaybı, yanlış sayfadan devam veya işaret konumu hatası kalmaz. |
| U6-02 | Önceki imzalı sürüm üzerine kaldırmadan güncelleme kitap/not/taslakları korur. |
| U6-03 | v1/v2 yedek tekrarları mükerrer oluşturmaz; sarı dönüşüm tutarlıdır. |
| U6-04 | Belirtilen test oturumunda uygulamaya ait OOM, ANR ve crash olmaz. |
| U6-05 | Her hareketin erişilebilir alternatifi vardır; gizli kontroller odak almaz. |
| U6-06 | Final imzalı release Metro kapalıyken açılır ve çalışır. |
| U6-07 | Ölçülmeyen fiziksel cihaz/performance kriteri geçmiş gösterilmez. |

### Ayrıntılı veri ve teslim koşulları

Eski stillerin kimlikleri, PDF koordinatları, alıntı/bağlamları, tarihleri ve not ilişkileri korunur. Değişen kaydın revision'ı bir kez artar; eski tür/renk/revision yerel kurtarma kaydında tutulur. Transaction hatası kısmi dönüşüm bırakmaz. Tekrar migration veya eski yedek importu aynı kaydı tekrar tekrar dönüştürmez. Bölge kayıtları dönüşüm dışıdır. Kısa işaret ancak DB commit sonucu kaydedilmiş gösterilir. İşaret+ranges atomik, komut kimliği idempotent; metin seçimi çok satırlı kulplarla yapılır, pasaj notunun metin işareti sarıdır. Her satır gerçek yönlü quad kullanır; sütunlar arası boşluk boyanmaz; örtüşen sarı alanlar koyulaşmaz. Alfa yalnız okunabilirlik gerekçesiyle değiştirilebilir.

Okuma görünümünde yeni ortak başlangıç orijinal renk/kontrast1/kırpmasız tam sayfa/tam ekran/tek sayfadır. Eski kitap başına ayarlar silinmez fakat yeni okuyucuda uygulanmaz. Yedekten gelen eski kitap görünümü ortak ayarı sessizce değiştirmez. Dokunma katmanı 160 ms opacity ile açılır, blur ve otomatik kapanma timer'ı kullanmaz; boş alana dokunma veya okuma hareketi başlangıcı kapatır. Menü yalnız üç ana işlem ve ayrı Sayfalar simgesidir. Not listeleri kitap kartındadır. Görünüm ayarları, kitap yönetimi, yedek ve depolama ana Ayarlar'dadır.

Geçiş eşiği %22 ekran genişliği veya mevcut anlamlı mesafe/hız kuralıdır; settle en çok yaklaşık220 ms. Hedef hazır değilse eski okunabilir sayfa korunur; sonsuz iş/animasyon kuyruğu oluşmaz. İki parmak başlangıcı kaydedilmemiş seçimi kapatır, kayıtlı işaretleri silmez. Aktif seçim tek parmak swipe'ını durdurur. Tek/çift sayfa aynı native koordinasyonu kullanır. Pinch'in 1× altındaki sanal mesafesi odak/gerçek viewport'u bozmaz. Sistem cancel/rotation/background yarım overview açılışını iptal eder. Genel görünümde mevcut sayfa yaklaşık200 ms küçülür ve üç sütun listesi yumuşak görünür; reduce-motion ölçek animasyonunu kaldırır. Genel görünümde kapat/numarayla git/yer imleri ve yardımcı önceki-sonraki kontroller bulunur. Küçük resimler görünür satırlar için üretilir; sayı placeholder'dan da seçilebilir.

Final teslim önceki APK ve raporları korur; aynı uygulama kimliği ve imzayla güncellenir. Source/lockfile, APK/SHA-256, test girdileri ve sonuçları, ABI/imza/R8/JNI/Codegen/16 KB statik raporları sunulur. v1/v2 restore/dedupe, relink, export ve draft kurtarma regresyonları yürütülür. %200 font, TalkBack, reduce-motion, telefon/tablet ve yönler kapsanır; ölçülmeyen sınır raporda açık kalır. Fiziksel Huawei'de release ile 30–60 dk/500 geçiş/20 zoom/10 rotation yapılmadan 2 GB performans kabulü verilmez.
