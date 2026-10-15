> 15 Ekim 2026 son kullanıcı kararı: [Arayüz sadeleştirmesi](docs/ARAYUZ_SADELESTIRME.md) bu belgedeki çelişen menü/ayar/yedek kararlarından önce gelir. Kart menüsünde mevcut Notlar / Altı çizilenler / Favori korunur; Adı düzenle ve Kaldır sona eklenir (toplam beş). Ayarlar dişliyle doğrudan tek ekranda açılır. Yedek/depolama ve bağımsız kitap yönetimi kaldırılmıştır.

> 15 Ekim 2026 güncel karar: [UX-MVP-1–6 yenilemesi](docs/UX_YENIDEN_TASARIM_PLANI.md) kaynak/derleme ve belgelenen emülatör kapsamıyla tamamlandı; fiziksel ve performans kabulü açık. [Güncel teslim](docs/acceptance/reports/UX-MVP-6.md). Bu belgedeki çelişen eski görünüm/menü/işaret stili kararlarını yeni plan değiştirir. Eski MVP aşamalarının güncel kanıtı AGENTS ve kabul raporlarıdır.

# Kitap — MVP yol haritası, hedefler ve kabul kriterleri

Tarih: 14 Ekim 2026. Referans cihaz: 2 GB RAM'li Huawei tablet; kullanıcı modeli MatePad SE AGS5-W09, HarmonyOS 3.0 olarak bildirdi. Güncel durum: eski MVP-0–6 ve UX-MVP-1–6 kaynak/release teslimleri raporlandı; fiziksel Huawei ve tam V1 kabulü açık. Bu belge başlangıç aşamalarının kapsamını korur; güncel UX kararları ve kanıtı üstteki bağlantılardadır.

Bağlayıcı ürün/teknik kurallar [AGENTS.md](AGENTS.md), detaylı davranışlar [UYGULAMA_PLANI.md](UYGULAMA_PLANI.md), sayısal eşikler [performance-budgets.json](docs/acceptance/performance-budgets.json) içindedir. Bu yol haritası mevcut V1 kapsamını teslim edilebilir parçalara böler; metin seçimi, notlar veya düşük donanım hedeflerini V1 dışına taşımaz.

## 1. Bölümleme kararı

**Bir teknik hazırlık kapısı (MVP-0) ve altı kullanıcıya değer veren MVP** vardır. MVP-0 tek başına kitap okuma ürünü sayılmaz. MVP-1 ilk kullanılabilir okuyucu, MVP-2 uzun süre rahat okunabilen sürüm, MVP-5 kitap üzerinde çalışma sürümü, MVP-6 tam V1 teslimidir.

| Aşama | Kullanıcıya/ürüne kazancı | Önkoşul | Çıkış |
|---|---|---|---|
| MVP-0 | Cihaz/motor uygunluğu ve maliyetinin bilinmesi | Mevcut plan | Kurulabilir teknik release APK + uyumluluk/ölçüm raporu |
| MVP-1 | PDF ekle, oku, kapat, kaldığın yerden devam et | MVP-0 | Temel çevrimdışı kitap okuyucu APK |
| MVP-2 | Parmakla akıcı ve rahat uzun süre oku | MVP-1 | Gesture/zoom/tema/kırpma ve ergonomi APK |
| MVP-3 | Kitap ve sayfalar arasında hızlı yön bul | MVP-2 | Sayfalar paneli, yer imleri, kitaplık ve çift sayfa APK |
| MVP-4 | Seçtiğin satırları işaretle, listeden geri dön | MVP-3 | Gerçek metin seçimi ve Altı çizilenler APK |
| MVP-5 | Kitap/sayfa/pasaj notları yaz ve kaybetmeden sürdür | MVP-4 | Notlar ve taslak kurtarma APK |
| MVP-6 | Kayıtlarını yedekle, güncelle, güvenilir V1 kullan | MVP-5 | Tam V1 imzalı APK + test/ölçüm/kurulum paketi |

Sıra: MVP-0 → MVP-1 → MVP-2 → MVP-3 → MVP-4 → MVP-5 → MVP-6. UI çizimleri, corpus hazırlığı ve test tasarımı sonraki aşamalar için erkenden yapılabilir; kullanıcıya tamamlanmış özellik diye sunulmaz. Veri modelinin gelecekteki annotation/note ilişkileri MVP-1'de düşünülür, ancak kullanılmayan bütün tabloları ve boş kontrolleri peşinen üretmek zorunlu değildir.

## 2. Ortak aşama sözleşmesi

### Giriş ve çıkış

Her MVP'nin önkoşulu öncekinin ilgili temel davranışlarının doğrulanmasıdır. Önceki özellikler regressions ile korunur. Çıkış için kapsamındaki bütün zorunlu kriterler, aktif performans hedefleri ve veri bütünlüğü kontrolleri geçer; kod yazmak veya yalnız ekran görüntüsü almak yeterli değildir.

Release APK her aşamada güncellenebilir uygulama kimliği ve imza sürekliliğini korur. User-facing aşamalarda önceki kurulumu kaldırmadan update testi yapılır. Debug build geliştirme için kullanılabilir, kabul performansı için kullanılamaz. MVP-0 test sürümü bile sonraki test kurulumlarını koruyacak signing/package stratejisini kaydeder.

### Durumlar

- **Planlandı:** kapsam/kriter var; uygulama kanıtı yok.
- **Geliştiriliyor:** çalışmaya başlandı, kriterler henüz tamamlanmadı.
- **Derleme doğrulandı:** artifact üretildi; cihaz kabulü anlamına gelmez.
- **Cihaz doğrulaması bekliyor:** gerekli fiziksel cihaz/ölçüm kanıtı yok.
- **Düzeltme gerekiyor:** test veya aktif hedef başarısız.
- **Kabul edildi:** ilgili tüm kriterler ve kanıtlar karşılandı.

Gerçek Huawei yoksa bağımsız kod/build/test işi sürer; aşama yalnız emülatör verisiyle kabul edilmiş sayılmaz. Bu bir sürekli kullanıcı onay döngüsü değildir: mevcut kapsam içinde sıradaki iş yürütülür, eksik kanıt açık raporlanır.

### Kriter nasıl kanıtlanır?

Her kriterde test girdisi, yapılan işlem, beklenen sonuç ve kanıt yeri bulunur. Otomasyon uygun olduğunda kullanılır; cihaz davranışı gerçekten yürütülür. Ekran kaydı etkileşim kanıtıdır, total native/GPU bellek kanıtı değildir. Geçersiz/parolalı gibi destek sınırları açıkça raporlanır. Bilinen crash, veri kaybı, yanlış alıntı veya sistem geri çakışmasıyla ilgili aşama geçmez.

Raporlarda kişisel PDF/not/alıntı yok; izinli veya sentetik corpus kullanılır. Ham metrik/trace ve artifact SHA256 kayıtlıdır. Test fixture'ları ölçülmüş uygulama performansı gibi sunulmaz.

## 3. MVP-0 — cihaz ve PDF motoru doğrulama kapısı

**Hedef:** React Native + tek PDFium motorunun gerçek düşük donanımlı tablette kurulabildiğini, aynı belge üzerinde hem görüntü hem gerçek metin verisi üretebildiğini ve ilk maliyetinin yönetilebilir olduğunu kanıtlamak.

**Önkoşul:** AGENTS ve mevcut ürün planı. Gerçek Huawei model/Android/ABI bilgisi henüz eksik; bağımsız proje ve motor denemesi yapılabilir, fiziksel kabul bekler.

**Kapsam:** minimal RN/TypeScript/Hermes app; sürüm uyum matrisi; Android build/signing/package stratejisi; Fabric/TurboModule iskeleti; tek PDFium/JNI oturumu/worker; küçük izinli PDF'yi aç/render/kapat; kelime hit-test ve metin çıkarma teknik denemesi; temel renk filtre denemesi; byte sayaçları ve ilk ölçümler. Kullanıcı kitaplığı/nihai gesture arayüzü bu kapıda yoktur.

**İş sırası:** gerçek cihaz envanteri → destekli sürüm/ABI seçimi → boş release baseline → render+text entegrasyonu → renk/koordinat/close denemesi → kurulum ve ilk bellek/APK karşılaştırması.

| Kriter | Deneme | Kabul sonucu |
|---|---|---|
| M0-A01 | Huawei model, Android, ABI, ekran ve RAM bilgisini al | Ölçülen envanter kayıtlı; uyumluluk tahmin edilmiyor |
| M0-A02 | Release APK'yı kur, geliştirme sunucusu kapalıyken uçak modunda aç | App açılır; Metro/GMS/HMS gerektirmez |
| M0-A03 | Temsilî Türkçe metin PDF'sini native render et | Sayfa okunabilir; renderer JS/WebView motoru değildir |
| M0-A04 | Aynı belge/sayfada seçilen kelimeyi ve karakter konumunu teknik olarak çıkar | Beklenen Türkçe alıntı ve PDF-space konum gelir; ikinci motor yok |
| M0-A05 | Orijinal/sepya/gece filtrelerini aynı sayfa üstünde dene | Okunabilir görüntü; her tema için bitmap kopyası yok; resim etkisi kayıtlı |
| M0-A06 | 20 belge aç/kapat ve sürmekteki işte close/viewport değişimi | Çift close/crash/eski frame yok; FD/page/text kaynakları birikmez |
| M0-A07 | RN/Hermes/PDFium/NDK/SDK sürümleri ve lisans bildirimlerini incele | Birlikte release derlenmiş matrix ve sürdürülebilir motor kararı kayıtlı |
| M0-A08 | Baseline ve render sırasında PSS/cache/APK maliyetini ölç | Aktif M0 hedefleri geçer; kontrol dışı maliyet gizlenmez |

**Kullanıcı gibi teknik demo:** APK'yı aç → örnek sayfayı gör → temayı değiştir → kelime extraction çıktısını doğrula → kapat/aç. Bu demo kitaplık veya bitmiş seçim UX'i diye sunulmaz.

**Teslim:** minimal release APK/hash, cihaz/sürüm matrix'i, motor/lisans kararı ve baseline+render ölçümü. Başarısız native engine kararı, geri kalan büyük UI kurulmadan burada düzeltilir.

**Skill:** `kitap-rn-pdf-engine`; build/ölçüm için `kitap-android-release`.

## 4. MVP-1 — temel çevrimdışı okuyucu

**Hedef:** Kullanıcı kendi PDF'sini ekleyip baştan sona okuyabilsin ve sonraki açılışta kaldığı yere dönebilsin.

**Önkoşul:** MVP-0 motor/cihaz uygunluğu. Uyumluluk/bellek çözülmeden başka engine'e bağlı sahte demo ile ilerleme yok.

**Kapsam:** sistem seçicisi; yerel PDF URI/kalıcı izin, gerektiğinde atomik kopya; kitaplığa temel liste kaydı; fit tek sayfa; önceki/sonraki düğmeleri; sayaç ve numarayla git; SQLite konum kaydı; loading/error; kitaplıktan kaldırma; yeniden dosya bağlamanın temel konum koruma akışı. Sade UI ve temel erişilebilir etiketler ilk kullanıcı sürümünde vardır. Swipe/zoom/özel tema, tam kapak kitaplığı ve çalışma sekmeleri sonraki aşamalardadır.

**İş sırası:** belge kimliği/repository/migration temeli → güvenli import → native sayfa ve düğmeyle navigation → konum/lifecycle → liste/kaldırma/relink → gerçek dosya ve update testi.

| Kriter | Deneme | Kabul sonucu |
|---|---|---|
| M1-A01 | Sistem seçicisinden metin ve tarama PDF ekle | Seçilen belge açılır; geniş tüm-dosyalar izni yok |
| M1-A02 | Provider kopyası sırasında iptal/disk alanı hatası oluştur | Hazırmış gibi yarım kitap kaydı oluşmaz; geçici dosya temizlenir |
| M1-A03 | 1., orta ve son sayfaya düğme/numarayla git | Sayaç ve gerçek page eşleşir; ilk/son sınır ve geçersiz sayı güvenli |
| M1-A04 | Bir sayfada çık/background yap ve yeniden aç | Normal çıkışta aynı fiziksel sayfa; kill'de son commit geri gelir |
| M1-A05 | İki farklı kitabı sırayla oku, hızlı kitap değiştir | Konumlar karışmaz; eski render diğer kitapta görünmez |
| M1-A06 | Aynı URI'yi tekrar ekle; kaydı kaldır | Mükerrer kayıt kontrolü çalışır; dış orijinal PDF silinmez |
| M1-A07 | Dosyayı taşı/sil veya provider erişimini kes | Crash yok; kitap/konum korunur, yeniden seçme sunulur |
| M1-A08 | Boş/bozuk/desteklenmeyen parolalı PDF aç | Anlaşılır sonuç; sonsuz loading veya app freeze yok |
| M1-A09 | 100–500 sayfalı tipik yerel PDF ile aktif süre/PSS/cache hedeflerini ölç | M1 hedefleri geçer; tüm kitabın bitmapleri RAM'e alınmaz |
| M1-A10 | Önceki sürüm üzerine kur; TalkBack ve temel font büyütmeyi dene | Konum korunur; düğmeler erişilebilir, kritik işlem taşma nedeniyle kaybolmaz |

**Kullanıcı demo:** PDF ekle → sayfa 42'ye git → uygulamadan çık → yeniden aç → 42'den devam et → başka kitaba geç. İlk gerçek kullanılabilir okuyucu budur.

**Teslim:** kişisel PDF açabilen release APK, import/konum/relink senaryo kanıtları ve M1 metrik raporu.

**Skill:** `kitap-rn-pdf-engine`, `kitap-annotations-storage` (konum/SQLite), gerektiğinde `kitap-reader-gestures` ve `kitap-android-release`.

## 5. MVP-2 — akıcı ve göz yormayan okuma

**Hedef:** Kullanıcı kitabı parmakla rahat okuyabilsin; büyütme, renk ve yön ayarı düşük donanımda tepkiyi bozmasın.

**Önkoşul:** MVP-1 import/konum/native render doğru. Render bütçesi ve basic model üstüne gerçek okuma gesture'ı kurulur.

**Kapsam:** yatay page swipe; yarım sürüklemeyi iptal; hazır sayfa transform animasyonu; pinch/double-tap ve tile zoom; zoom-pan/page ayrımı; orijinal/krem/sepya/gece/özel renk/kontrast; kitap başına tema ve kırpma; açılıp kapanan araçlar/sayaç; yön kilidi, tam ekran, pencere parlaklığı/ekran açık; rotation/split-screen; hareket azaltma. Önizleme paneli yokken bile her iki sistem geri kenarı korunur.

**İş sırası:** insets+gesture durumları → page swipe ve commit → tile transform/zoom → filtre/crop → ayarlar/lifecycle → uzun oturum ölçümü.

| Kriter | Deneme | Kabul sonucu |
|---|---|---|
| M2-A01 | İç sayfadan iki yöne sürükle; eşik altında bırak | Doğru sayfaya yumuşak geçiş veya aynı sayfaya dönüş; yanlış konum kaydı yok |
| M2-A02 | Sağ ve sol dış kenardan geri, yatay/dikey ve zoom'da | Sistem geri çalışır; page-swipe aynı stream'i sahiplenmez |
| M2-A03 | İki parmak/çift dokunma ile büyüt, tek parmakla pan | Metin netleşir; pan yanlış sayfa değişimine yol açmaz |
| M2-A04 | Yüksek zoom ve hızlı 10 geçiş | Dev tam-sayfa bitmap/sınırsız kuyruk yok; stale frame yok |
| M2-A05 | Gece/sepya/özel renk ve kontrast değiştir, reopen yap | Tercih kitap bazında korunur; beyaz flash yok; orijinale dönüş var |
| M2-A06 | Kenar kırp, döndür ve crop reset yap | Sayfa/odak korunur; PDF dosyası değişmez; reset doğru |
| M2-A07 | Araç/sayaç, pencere parlaklığı, ekran açık, yön kilidi | Kullanıcı tercihi çalışır; okuyucudan çıkınca sistem etkileri geri alınır |
| M2-A08 | Hareket azaltma, font büyütme ve TalkBack | Araçlar erişilebilir; gesture için düğme var; gizli focus/taşma yok |
| M2-A09 | 30–60 dk, 500 geçiş, 20 zoom ve 10 dönüş | Aktif latency/frame/PSS/cache/growth hedefleri geçer; OOM/ANR/crash yok |
| M2-A10 | Slow/heavy render sırasında dokun/iptal et | UI yanıtı devam eder; mevcut sayfa korunur, işlem durumu anlaşılır |

**Kullanıcı demo:** gece temalı kitap → birkaç sayfa sürükle → yarım sürüklemeyi bırak → büyüt ve pan yap → tableti çevir → dış kenardan geri. Bu sürüm uzun okuma için ilk uygun teslimdir.

**Teslim:** gesture/tema/zoom release APK, iki kenar regresyon kaydı, 30–60 dk oturum ve M2 metrik raporu.

**Skill:** `kitap-reader-gestures`, `kitap-rn-pdf-engine`, `kitap-android-release`.

## 6. MVP-3 — kitap ve sayfa gezinmesi

**Hedef:** Kitapta bir sayfayı veya yer imini hızlı bulmak ve kitaplığı rahat kullanmak; 1000 sayfa/çok kitapta kaynak tüketimi sınırlı kalmak.

**Önkoşul:** MVP-2 sistem geri, viewport transform ve temel cache doğruluğu.

**Kapsam:** Sayfalar paneli; sistem kenarının dışında iç tutamaç ve düğme; virtual thumbnail grid; numara/sürgü; yer imleri; kapakları lazy olan liste/ızgara kitaplık; başlık arama, sıralama, görünen ad düzenleme, son kitaba devam; uygun tablette isteğe bağlı çift sayfa/spread eşleşmesi; sol el tutamaç tercihi. Çalışma panelinin henüz uygulanmayan Altı çizilenler/Notlar sekmeleri çalışır gibi sunulmaz.

**İş sırası:** panel/liste virtualization → görünür thumbnail işleri → yer imleri → tam kitaplık → çift sayfa/bellek → insets ve büyük corpus regresyonu.

| Kriter | Deneme | Kabul sonucu |
|---|---|---|
| M3-A01 | İç tutamaç ve Sayfalar düğmesiyle paneli aç | Aynı panel açılır; tutamacın bütün hit alanı sistem kenarından dışarıda |
| M3-A02 | Sağ/sol dış kenardan geri; açık panelde geri | Panel yanlışlıkla açılmaz; açık panel önce kapanır, kitap konumu korunur |
| M3-A03 | 1000+ sayfa PDF'de sonlara hızlı kaydır | Sayılar erişilir; sadece görünür thumbnail'lar hazırlanır; RAM page count ile büyümez |
| M3-A04 | Thumbnail veya numara seç; slider sürükle ve bırak | Seçilen gerçek sayfa açılır; slider hareketinde her sayfa render edilmez |
| M3-A05 | Yer imi ekle/kaldır ve reopen yap | Kitap bazında doğru sayfaya döner; duplicate/sınır hatası yok |
| M3-A06 | Çok kitapla kapak/list/ızgara/isim arama ve sıralama | Bütün kapaklar açılışta decode edilmez; doğru sonuç/sıra/ad kaydı |
| M3-A07 | Uygun yatay tablette çift sayfa, kapak ve tek sayfaya dönüş | Spread/counter doğru; fiziksel aktif page korunur; bitmap hedefi geçer |
| M3-A08 | Sol el modu, window resize, büyük yazı ve TalkBack | İç tutamaç OS alanına girmez; panel ve odak erişilebilir |
| M3-A09 | Panel+okuma+çift sayfa ölçümleri ve uzun oturum | Panel p95 ≤250 ms dahil M3 hedefleri geçer; önceki M2 hedefleri bozulmaz |

**Kullanıcı demo:** kitaplığı isimle ara → kitaba devam et → panelde sayfa 900'ü seç → yer imi ekle → yatay çift sayfayı dene → geriyle paneli kapat.

**Teslim:** navigasyon APK, büyük liste/spread/OS-kenar testleri ve M3 performans raporu.

**Skill:** `kitap-reader-gestures`, `kitap-rn-pdf-engine`, `kitap-annotations-storage` (bookmark), `kitap-android-release`.

## 7. MVP-4 — metin seçme ve Altı çizilenler

**Hedef:** Kullanıcı seçtiği satırları kalıcı işaretleyebilsin, kitaba ait listede alıntı ve sayfayı görüp aynı yere dönebilsin.

**Önkoşul:** MVP-0'da native text altyapısı doğrulanmış; MVP-2 transform/zoom, MVP-3 panel ve kitap kimliği mevcut. Gerçek metin seçimi sona bırakılmış motor keşfi değildir.

**Kapsam:** uzun basma ve selection kulpları; çok satır tek sayfa seçim; altını çiz/vurgula/kopyala; renk/tür düzenleme; annotation+ranges transaction; overlay; kitap başına Altı çizilenler; sayfa/tarih sırası, renk/tür filtresi ve kayıtlı alıntı arama; doğru konuma jump ve Okumaya dön; silme/geri alma; taramalarda manuel bölge işareti. Not ekle işlemi MVP-5 tamamlanmadan etkin değil, eksik işlev çalışır gibi sunulmaz.

**İş sırası:** inverse transform/hit-test → selection gesture → durable anchor/model → overlay → liste/jump/undo → farklı metin corpus'u ve cache ölçümü.

| Kriter | Deneme | Kabul sonucu |
|---|---|---|
| M4-A01 | Türkçe kelime ve 3 satır seç | Önizlenen alıntı ve kulplar doğru; selection'da page-swipe başlamaz |
| M4-A02 | Altını çiz/vurgula, çift bas ve reopen yap | Quote+page+quad/type/color korunur; idempotent işlem duplicate yaratmaz |
| M4-A03 | İşaretli sayfada zoom/rotation/crop/tema | İşaret aynı metinde; filtre işaret rengini ters çevirmez |
| M4-A04 | Non-zero page box, 90/270° rotation, ligature/çok sütun/RTL | Desteklenen selection doğru; belirsiz/bozuk çıkarım doğru alıntı gibi sunulmaz |
| M4-A05 | Kitabın Altı çizilenler kartına dokun, sonra Okumaya dön | Doğru sayfa/satırlar görünür; önceki okuma noktası geri gelir |
| M4-A06 | 1000 işaretle liste/arama/sıra/filtre | Yalnız kitap kayıtları, paginated sonuç; screenshot cache veya tüm quad listesi JS'ye yüklenmez |
| M4-A07 | Renk/tür değiştir, sil ve geri al | Kalıcı model ve overlay tutarlı; silme/undo güvenilir |
| M4-A08 | Taramada uzun bas ve bölge işaretle | Olmayan metin seçiliyormuş gibi değil; bölge/page kaydı korunur |
| M4-A09 | DB yazı hatası ve stale engine sonucu üret | Kaydedildi yalan gösterilmez; seçim retry için durur; yanlış belgeye işaret yok |
| M4-A10 | Sağ/sol geri ve seçim kulplarını sınırda dene | OS kenarı korunur; geri seçim/menüyü kapatır, yanlış page geçişi yok |
| M4-A11 | Prepared selection, DB commit ve text/native bellek ölç | Seçim p95 ≤100 ms, commit p95 ≤300 ms; 4 MiB türetilmiş cache ve diğer M4 hedefleri geçer |

**Kullanıcı demo:** 3 satır seç → altını çiz → başka sayfaya git → Altı çizilenler'de alıntıyı bul → dokunarak satırlara dön → Okumaya dön → kapat/aç. Taramada aynı demo bölge işaretiyle yürütülür.

**Teslim:** annotation APK, anchor/Unicode/transaction/list testleri ve M4 ölçümleri. Bu sürüm altı çizme değerini verir; yazılı notlar henüz kabul edilmiş değildir.

**Skill:** `kitap-annotations-storage`, `kitap-rn-pdf-engine`, `kitap-reader-gestures`, `kitap-android-release`.

## 8. MVP-5 — kitap, sayfa ve pasaj notları

**Hedef:** Kullanıcı okurken düşüncelerini kitaba veya alıntıya bağlayabilsin; editör kapanması/OS kill sonrası son başarılı taslak geri gelsin.

**Önkoşul:** MVP-4 document/annotation kimlikleri ve SQLite kalıcılığı. Note taslağı ve annotation silme ilişkisi birlikte tasarlanır.

**Kapsam:** kitap geneli, sayfa ve pasaj notu; düz metin başlık/gövde; Notlar sekmesi; seçim/işaret üstünden not ekleme; draft/final ayrımı; debounce autosave/revision; kaydet durumları; klavye/focus/back; sıralama/arama/düzenleme/silme/geri alma; ilişkili işaret silinirken notu koruma veya açık silme kararı. Taramalarda genel/sayfa/bölge notu da çalışır.

**İş sırası:** note/draft schema + revision → editör ve autosave → IME/lifecycle → kitap/annotation bağları → liste/arama/silme → kill/update/DB hata testleri.

| Kriter | Deneme | Kabul sonucu |
|---|---|---|
| M5-A01 | Aynı kitapta genel, sayfa ve pasaj notu oluştur | Üç bağ doğru; pasajda alıntı not gövdesinden ayrı |
| M5-A02 | Notlar ve Altı çizilenler'den pasaj notunu aç | Aynı kayıt; iki kopya veya kitaplar arası karışma yok |
| M5-A03 | Yazarken background/OS kill ve cold reopen | Son başarılı draft geri gelir; henüz commit edilmemiş tuşa sahte garanti yok |
| M5-A04 | Revision 2 yazmasını revision 1'den önce tamamla | Eski completion yeni içeriği/Save durumunu ezmez |
| M5-A05 | Kaydet veya DB hata/retry | Kaydedildi yalnız commit sonrası; draft/final transaction doğru, içerik kaybolmaz |
| M5-A06 | Tablet klavyesi, geri, büyük font ve TalkBack | Editör ve Kaydet erişilebilir; önce IME kapanır; dönüşte page/zoom korunur |
| M5-A07 | 1000 kayıt, uzun not ve hızlı yazma/arama/sıralama | Paginated liste; her tuşta full DB write/full list render yok |
| M5-A08 | Notlu işareti sil; notu koru/ikisini sil ve undo | Sessiz cascade veri kaybı yok; seçim açık ve ilişki tutarlı |
| M5-A09 | İki kitap notlarını aç ve önceki APK üstüne update yap | Kayıtlar kitap bazında ayrılır, migration notes/marks/positions'ı korur |
| M5-A10 | Not yazma, selection ve önceki okuma benchmark'larını çalıştır | Aktif tüm M5 hedefleri geçer; draft flush UI'ı bloke etmez |

**Kullanıcı demo:** kitaba genel not → 42. sayfaya not → çizili pasaja yorum → editörde yazıp başka uygulamaya geç → geri dön ve taslağı gör → kaydet → Notlar'dan aç.

**Teslim:** tam çalışma alanı APK, draft/revision/silme/IME kanıtları ve M5 regresyon/performance raporu. Kişisel çalışma için değerli sürümdür, yedek/final destek matrisi MVP-6'da tamamlanır.

**Skill:** `kitap-annotations-storage`, `kitap-reader-gestures`, gerektiğinde `kitap-rn-pdf-engine`, `kitap-android-release`.

## 9. MVP-6 — yedekleme, kurtarma ve tam V1 teslimi

**Hedef:** Önceki tüm özellikleri bir arada güvenilir kullanmak; kitap çalışmasını taşımak, güncellemek ve hatalardan veri kaybetmeden toparlanmak.

**Önkoşul:** MVP-1–5 işlevleri ve aktif hedefleri. Performans ilk kez bu aşamada ele alınmaz; burada birleşik ürün ve geniş test matrisi doğrulanır.

**Kapsam:** sürümlü JSON backup/restore; UUID/revision dedupe/çatışma; kitap başına TXT/Markdown; notes/marks dahil tam relink; belge revizyonu uyarısı; copy/cache/library delete ayrımı; disk/provider/bozuk-file kurtarma; share/open akışının tamamlanması; cihaz/OS/ABI/16 KB matrisi; final R8/shrink; imzalı APK/hash; update ve kurulum/yedek talimatı. Parolalı dosya desteği seçilen engine/build kapsamına göre açık belgelenir, hiçbir desteklenmeyen dosya sonsuz loading'de kalmaz.

**İş sırası:** yedek şeması/import validation → export/relink/revision → tam hata matrix'i → release shrink/ABI → uzun oturum+full regression → upgrade ve teslim raporu.

| Kriter | Deneme | Kabul sonucu |
|---|---|---|
| M6-A01 | Konum/tercih/bookmark/işaret/geometri/not yedekle ve geri yükle | Alanlar/Unicode doğru; password/PDF içeriği otomatik dahil değil |
| M6-A02 | Aynı backup'ı iki kez import; yeni revision çatışması | Duplicate yok; çatışma sessizce ezilmez, önizleme/karar var |
| M6-A03 | Başka cihaz/URI veya taşınan dosyayı yeniden bağla | Aynı belge doğrulanır; alıntı/not erişilebilir; yeni baskıya eski geometri sessizce uygulanmaz |
| M6-A04 | Kitap özetini TXT/Markdown dışa aktar | Kitap/sayfa/alıntı/not ilişkisi okunabilir; özel metin istemeden gönderilmez |
| M6-A05 | Disk dolması, cache temizleme, SD kart çıkarma ve yarım import | Kalıcı kayıtlar bozulmaz; app uygun kurtarma sunar; orijinal PDF silinmez |
| M6-A06 | Android share/open ile PDF aç; tekrar ekleme | Doğru belge, güvenli erişim/kopya, duplicate kontrolü |
| M6-A07 | Shrunk release'i gerçek ABI'lerde kur, destekli yeni Android'de 16 KB uyumu | JNI/Codegen/native yükleme doğru; destek matrix'i gerçek testle |
| M6-A08 | Önceki uygulama ve eski DB şeması üstüne aynı imzalı update | Position/mark/note/draft korunur; uninstall/reinstall testi yerine geçmez |
| M6-A09 | Tam corpus + 30–60 dk + 500 swipe/20 zoom/10 dönüş + 1000 kayıt | Bütün aktif hedefler, data/OS-back/anchor regressions geçer; OOM/ANR/crash yok |
| M6-A10 | Debug/secrets/test-assets/idle-work ve accessibility son kontrolü | Üretimde gereksiz maliyet/GMS ve özel veri/log yok; kritik kontroller erişilebilir |
| M6-A11 | APK/hash/source/build/test/kurulum/yedek teslimini incele | Tekrarlanabilir ve Metro bağımsız imzalı V1; gerçek ölçümler ve bilinen sınırlar raporlanmış |

**Kullanıcı demo:** notlu/işaretli kitap → yedek oluştur → APK'yı update et → bütün kayıtları gör → backup tekrar import et → duplicate olmadığını doğrula → dosyayı taşı ve tekrar bağla.

**Teslim:** final imzalı cihaz APK'sı, gerekiyorsa universal, SHA256/version/ABI, kaynak+lockfile/build notu, full kabul raporu ve kurulum/güncelleme/yedek/kurtarma talimatı. Bu aşama geçmeden "tam V1 tamamlandı" denmez.

**Skill:** `kitap-android-release`, `kitap-annotations-storage`; engine/gesture regressions için ilgili skill.

## 10. Performans hedeflerinin MVP bazında devreye girmesi

Eşiklerin tek otomatik kaynağı `docs/acceptance/performance-budgets.json` dosyasıdır. Aşağıdaki anahtarlar bu dosyaya atıftır; yeni threshold listesi yaratılmaz. İlk aktif olduğu aşamadan sonra ölçüm/etkilenen regresyon ilgili bütün MVP'lerde sürer. İlgili kritik native/UI/storage değişimlerinde etkilenen benchmark tekrarlanır; finalde bütün set yürütülür.

| İlk aktif aşama | Yeni zorunlu metrik anahtarları |
|---|---|
| MVP-0 | `reading_pss_mib`, `peak_pss_mib`, `bitmap_cache_mib`, `derived_text_cache_mib`, `device_apk_mib`, `crashes`, `anrs`, `ooms` |
| MVP-1 | `cold_library_ms`, `warm_resume_ms`, `first_readable_page_ms`, `touch_feedback_ms` |
| MVP-2 | `delayed_frames_percent`, `frozen_frames`, `quiescent_pss_growth_percent` |
| MVP-3 | `page_panel_ms` |
| MVP-4 | `prepared_selection_ms`, `annotation_commit_ms` |
| MVP-5 | Yeni sayısal threshold yok; önceki bütün metrikler + draft/not davranış kriterleri |
| MVP-6 | Bütün zorunlu metrikler birlikte; universal APK varsa `universal_apk_mib` de zorunlu değerlendirilir |

MVP-0'da reading PSS adı teknik örnek sayfa üzerinde ölçülür; kullanıcı kitaplığı/uzun oturum kabulü değildir. Tek sayfa text prototype'ında türetilmiş cache sayacı da vardır. M1 ilk okunabilir yerel sayfa süresi provider download/import zamanını içermez; o akış kullanıcıya ayrıca görünür olur.

Hazır page geçişi 60 Hz referansta yakın 60 FPS ve <%5 delayed frames; heavy page hazırlanması ayrı. Selection latency metadata hazırken ve long-press bekleme eşiği hariç. Aynı isim altında bu süreler karıştırılmaz.

### Erken MVP değerlendirmesi

Mevcut `evaluate_metrics.py` bütün zorunlu budget key'lerini bekler. MVP-1'de olmayan panel/annotation ölçümü için sahte sıfır üretme. Aşama değerlendirmesinde aktif anahtarlar checklist ile kanıtlanır veya ana budget'tan yalnız aktif anahtarları seçen **geçici, üretilmiş stage budget görünümü** kullanılır. Eşik ve reference device aynen kalır; elle daha gevşek sayılar eklenmez. Generated view nihai canonical dosyanın yerine geçmez. MVP-6 raporu orijinal tam bütçeye karşı çalışır.

Sayısal başarısızlık FAIL, eksik ölçüm/physical device INCOMPLETE. Stage PASS tüm V1'in kabulü değildir. Bütçe aracı işlevsel test, gerçek cihaz/evidence kimliği ve kullanıcı deneyimini kendisi doğrulamaz.

## 11. V1 kapsamı → MVP izlenebilirliği

| Özellik/hedef | İlk kullanıcı teslimi | Tam birleşik doğrulama |
|---|---|---|
| React Native/Hermes/Fabric/PDFium/ABI | MVP-0 teknik kapı | MVP-6 |
| PDF seç/import/local copy/konum/basic liste | MVP-1 | MVP-6 |
| Sayaç/numara/önceki-sonraki/yeniden açma | MVP-1 | MVP-6 |
| Renk/kontrast/zoom/tile/crop/yön/parlaklık/ekran açık | MVP-2 | MVP-6 |
| Sistem sağ/sol geri | MVP-1 temel ekran; MVP-2 gesture | Her yeni gesture durumu; MVP-6 |
| Virtual sayfalar paneli/bookmark/cover/grid/search/sort | MVP-3 | MVP-6 |
| Çift sayfa/sol el/window uyumu | MVP-3; temel rotation MVP-2 | MVP-6 |
| Metin seçim/underline/highlight/anchor/list/jump | MVP-4; motor kabiliyeti MVP-0 | MVP-6 |
| Scan region işareti | MVP-4 | MVP-6 |
| Kitap/sayfa/pasaj not/draft/revision/search/undo | MVP-5 | MVP-6 |
| Yedek/restore/dedupe/export | MVP-6 | MVP-6 |
| Relink/copy-delete/cache recovery | Temel MVP-1; işaret/not bağları MVP-6 | MVP-6 |
| Share/open dış giriş | MVP-6 | MVP-6 |
| Erişilebilirlik/üretim APK/update/privacy | Temel MVP-1, her MVP artımlı | MVP-6 |

OCR, EPUB, cloud sync, bütün PDF content search, freehand, TTS, 3D curl ve PDF içine annotation gömme hiçbir MVP'ye sessizce eklenmez. Bunlar V1 sonrası ayrı kullanıcı kapsam kararıdır.

## 12. MVP raporu ve çalışma takibi

Her MVP için geliştirme başladığında `docs/acceptance/reports/MVP-N.md` gibi bir gerçek rapor oluşturulur; henüz olmayan raporları yapılmış test gibi doldurma. Raporda:

1. MVP durumu, commit/build/version/hash ve test tarihi.
2. Fiziksel model/OS/ABI/RAM/Hz veya cihaz kanıtının neden eksik olduğu.
3. Her kriter ID'si için PASS / FAIL / INCOMPLETE; senaryo, beklenen/gerçek sonuç ve kanıt.
4. Aktif metric key'leri, birimler, sample count/median/p95/peak, threshold ve raw rapor/trace.
5. Önceki aşama regressions; migration/update sonucu.
6. Açık sorunlar, davranış sınırları ve sıradaki bağımlı iş.

Bir kriter kapsamı bu MVP'de gerçekten yoksa sonraki milestone'a ait olarak gösterilir; bu MVP'nin zorunlu kriteri N/A ile geçilmez. Tamamlanmış sayfa/işaret/draft korunması testinin yerine yalnız statik screenshot kabul edilmez.

| Aşama | Şimdiki durum | Kanıt |
|---|---|---|
| MVP-0 | Cihaz doğrulaması bekliyor | Release ARM64 APK, yedi host testi ve statik imza/hizalama geçti; fiziksel kabul MVP-0 raporunda açık |
| MVP-1 | Release/host/statik kontroller geçti; fiziksel kabul açık | 22,7786 MiB ARM64 APK, 26 host testi; [MVP-1 raporu](docs/acceptance/reports/MVP-1.md) |
| MVP-2 | Release/host/statik geçti; fiziksel kabul açık | 22,8550 MiB ARM64 APK, 40 host testi; [rapor](docs/acceptance/reports/MVP-2.md) |
| MVP-3 | Release/host/statik geçti; fiziksel kabul açık | 22,9028 MiB ARM64 APK, 50 host testi; [rapor](docs/acceptance/reports/MVP-3.md) |
| MVP-4 | Release/host/statik geçti; fiziksel kabul açık | 22,9421 MiB ARM64 APK, 75 host testi, shrink JNI/DEX doğrulaması; [rapor](docs/acceptance/reports/MVP-4.md) |
| MVP-5 | Release/host/statik geçti; fiziksel kabul açık | 23,0027 MiB ARM64 APK, 98 host testi, JNI/DEX/imza/16 KB; [rapor](docs/acceptance/reports/MVP-5.md) |
| MVP-6 | Kaynak/release/host/statik ve belirtilen Android emülatör turu tamamlandı; canonical performans INCOMPLETE, fiziksel/tam V1 kabulü açık | 0.6.2/code 9, 116 host testi, 10 PDF/12 sayfa; [rapor](docs/acceptance/reports/MVP-6.md) |

Sıra ve kapsam burada kesindir; tarih/saat tahmini gerçek cihaz ve motor prototipi görülmeden kesin taahhüt değildir. Yol haritası oluşturulduktan sonra kullanıcı "ilk adımdan başla" diyerek MVP-0 geliştirmesini başlatmıştır. Bir aşamanın kaynak kodunun yazılması fiziksel kabulü veya tam V1 tamamlanmasını göstermez.
