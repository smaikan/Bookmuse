# MVP-1 dosya ve okuma konumu sözleşmesi

## Ekleme ve kimlik

Android ACTION_OPEN_DOCUMENT / application/pdf / CATEGORY_OPENABLE kullanılır.
Bu aşamada her seçilen kitap uygulamanın özel `files/books` klasörüne kopyalanır.
Geniş depolama izni ve uzun ömürlü dış URI izni gerekmez. Kullanıcıya ek disk alanı
kullanıldığı açıkça gösterilir. Kaynak taşınması veya sağlayıcı izni kaybı eldeki
kopyayı etkilemez. İleride doğrudan seekable URI erişimi eklenirse aynı documentId
ve hata/kurtarma sözleşmesi korunacaktır.

64 KiB native buffer ile streaming kopya + SHA256 → fd.sync → aynı klasörde rename
→ PDFium ile sayfa sayısı/ilk sayfa metadata doğrulama → SQLite transaction.
Dosya boyutu veya sayfa sayısına göre bütün sayfa bitmapleri/metinleri üretilmez.
PDF ve bitmap JS'ye taşınmaz. İptal/disk/provider/validation/DB hatası final ve `.part`
dosyalarını temizler. Process death sonrası ilk repository erişimi `.part` ve DB'de
referansı olmayan final kopyaları temizler. Kaynak dosya üzerinde yazma/silme yok.

ImportOperation iptali engine kuyruğunun arkasında beklemez; ayrı iptal worker'ı
stream'i kapatır. Başlamış senkron PDFium doğrulamasının anında kesileceği garanti
edilmez. DB commit iptal için son sınırdır: commit tamamlandıktan sonra iptal cevabı
false; başarıyla eklenen kitap iptal edilmiş gibi raporlanmaz.

Sağlayıcı metadata sorgusu ve asset açılışı CancellationSignal alır. Stream kopyası
iptalde kapatılır ve her chunk arasında token kontrol edilir. Mevcut hedef dosya
üzerine kopyalama başlamaz; hata temizliği mevcut dosyayı silemez.

Kalıcı documentId UUID, kaynak URI unique, sahip olunan filename UUID, SHA256,
byte boyutu, başlık, sayfa sayısı, tarih. Aynı URI ikinci kitap yaratmaz. Yeniden
bağlamada fingerprint+boyut+sayfa sayısı eşleşir; aynı kitap ID ve konum korunur.
Yeni baskı/farklı PDF reddedilir ve kullanıcı yeni kitap olarak ekleyebilir.
Her normal açılışta tam hash tekrar hesaplanmaz; özel kopyanın boyutu kontrol edilir.

## SQLite ve sahiplik

`kitap.db` schemaVersion 1: documents + reading_positions. Foreign key açık;
kitap kaldırınca yalnız okuma konumu cascade silinir. İleride annotation/note
silme sözleşmesi ayrı migration ile eklenecek. Bilinmeyen migration DB'yi sıfırlamaz.
Kitaplık küçük sayfalı sorgu (40), stabil ID ve son okunma sırası; FlatList sanallaştırır.
Kaldır uyarısı uygulama kopyası/konumun silineceğini, dış orijinalin kalacağını belirtir.

PDFium, SQLite, hash ve dosya I/O tek bounded seri worker'da. İçe aktarma sırasında
okuyucu açık olmaz. PDFium FD sahipliğini successful newDocument sonrası alır.
Yeni kitap önceki engine oturumunu kapatır. Eski view cleanup yalnız kendi sessionId'si
hâlâ aktifse kapatabilir; yeni kitabı yanlışlıkla kapatamaz.

## Sayfa ve lifecycle

İçeride 0, kullanıcıda 1 tabanlı fiziksel sayfa. PDFium'a ulaşmadan sayfa sınırı
doğrulanır. Numarayla git tam sayı, sınır ve JS safe integer kontrolü yapar.
Aktif session + page + viewport generation eski render sonucunu reddeder. Hedef
hazırlanırken önceki frame tutulur. Başarısız render sayaç/konumu değiştirmez.

Görünüm yeni bitmapi onDraw'da çizdikten sonra ready bildirir ve seri worker'a
konum yazısını hemen gönderir. Debounce bu aşamada yok; yalnız sayfa yerleşince
yazılır. UI counter ready sonrası, Kaydedildi yalnız SQLite commit sonucu.
Kill'de son başarılı commit kurtarılır. Background'da native view unmount olur,
görünen bitmap referansı bırakılır; foreground mevcut session/hedefle tekrar çizilir.
Okuyucu unmount closeSession gönderir. Native stack iki ekranı yönetir; PDF view
MVP-1'de pan/swipe recognizer kurmaz, sistem kenarını dışlama rect'iyle sahiplenmez.
Modern predictive back/cancel davranışı ve sağ/sol kenar Huawei/fiziksel modern
Android matrisiyle doğrulanmayı bekler.

## Kontroller ve kaynaklar

48 dp düğmeler, wrap yapan araçlar, büyük fontta kaydırılabilir toolbar ve numara
dialogu. PDF'nin tamamının TalkBack tarafından okunması iddia edilmez. Gece/sepya
teknik filtresi korunur; kitap başına kalıcı tema/zoom/swipe MVP-2 işidir.

[Android SAF](https://developer.android.com/training/data-storage/shared/documents-files),
[SQLiteOpenHelper](https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper),
[Native stack kurulum](https://reactnavigation.org/docs/getting-started/).

## MVP-1 araç matrisi ekleri

React Navigation native 7.5.1, native-stack 7.21.0, react-native-screens 4.30.0;
lockfile çözülmüş sürümleri tutar. screens'in root compileSdk karşılaştırması için
root API değeri sayısal 37'dir. Android plugin finalizeDsl hook'ları **RN rootproject
plugin uygulanmadan önce** kaydedilir; gerçek compileSdk 37 / compileSdkMinor 0
olur ve kurulu `android-37.0` paketi çözülür. node_modules dosyası yamalanmaz.
MainActivity, screens'in RNScreensFragmentFactory'sini onCreate öncesi bağlar.

Robolectric 4.17 yalnız testImplementation; APK bağımlılığı değildir. Repository
testleri Android API 28 simülasyonunda standart Application ile çalışır; RN/Hermes
ve Android PDFium native kütüphanelerini Windows JVM'e yükleme iddiası yoktur.
