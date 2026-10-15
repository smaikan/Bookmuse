# MVP-6 yedek ve kurtarma sözleşmesi

14 Ekim 2026. Uygulama 0.6.2/code 9; veritabanı schemaVersion 5 korunur.
MVP-6 yeni kalıcı sütun gerektirmez. Eski 1–4 migration testleri ve MVP-5
not/draft davranışları aynı şema üzerinde sürer.

## Taşınabilir biçim

UTF-8 JSON: `format=kitap-backup`, `version=1`, `pageBase=0`,
`geometry=PDF-user-space-quads`, ardından sıralı `tables` alanı.
Tablolar: documents, reading_positions, book_preferences, bookmarks,
annotations, annotation_ranges, notes, note_drafts, app_preferences.
V1 alan/tablo sırası biçimin parçasıdır; bilinmeyen, eksik, yinelenen veya
yanlış tipli alanlar reddedilir. Gelecek biçim ayrı sürüm ve dönüşüm ister.

UUID, revision, tombstone, grup silme/geri alma bağları, PDF quads, kutular,
rotasyon, fingerprint, motor sürümü, alıntı/bağlam ve Unicode korunur.
Dosya yolu/URI, PDF bytes, parola, cache, arama türevleri ve editör lease'i
taşınmaz. Arama indeksleri yeniden hesaplanır; editörler yeni token alır.
Geri yüklenen PDF'siz kitap için rastgele yerel filename ve `backup:UUID`
kaynak tanımı native üretilir. Yedekten keyfi dosya yolu çalıştırılmaz.

## Bellek ve doğrulama

Yazma JsonWriter/SQL cursor ile satır satır, actor üzerinde snapshot transaction
içinde. Okuma JsonReader ile özel cache klasöründe disk SQLite staging'e.
JS'ye içerik veya tüm geometri gönderilmez; küçük sayımlar ve 20 kitaplık
önizleme sayfaları gider. Tek dosya en fazla 256 MiB gerçek UTF-8 byte;
bir milyon satır. Bu sınır hem yazmada hem okumada uygulanır. Boyutu aşan
dış dosya yarım kalabilir; uygulama kayıtları değişmez, açık hata ve retry vardır.
JsonReader tahsisinden önce escaped string uzunluğu 310.000 karakterle
sınırlanır; not gövdesi ayrıca 50.000 UTF-16 birimi, başlık 200, alıntı 10.000.
Bozuk UTF-8 reddedilir. UUID, fiziksel sayfa, revision, fingerprint, tip/renk,
quad/alan, satır sırası, belge/işaret/not/draft foreign-key ilişkileri doğrulanır.

Staging process ölümünden kalırsa sonraki backup kullanımı başında yalnız
owned staging dosyaları temizlenir. Ana DB veya kişisel PDF'ler bu temizlikte yok.

## Önizleme ve karar

Belge UUID'si ile eşleşir; aynı UUID farklı fingerprint/size/page count ise
ret. Başka cihazda aynı fingerprint/size/page count tek mevcut kitaba eşleşirse
yerel belge kimliğine bağlanır; yeni kitap kopyası oluşturulmaz. Belirsiz
birden fazla eşleşme ve başka kitabın kayıt UUID'sini sahiplenme reddedilir.

Portable alanların canonical SHA256'sı aynıysa yeniden import etkisizdir.
Revision ve içerik farklılıkları sessizce üstün sayılmaz: farklı kitaplar
önizlemede gösterilir. Kullanıcının açık kararı kitap snapshot'ı bütününe uygulanır:

- **Yereli koru:** mevcut farklı kitapların bütün çalışması korunur, yalnız
  yeni kitaplar eklenir. App tercihi de korunur.
- **Yedeği kullan:** farklı kitapların position/preferences/bookmark/mark/note/draft
  snapshot'ı yedekten atomik değiştirilir. Daha yeni yerel kayıtlar da
  değişebileceği UI'da yazılıdır; güncel yedek önerilir. Mevcut PDF path/URI korunur.

Satır bazında otomatik merge yapılmaz; bu tercih ilişkili not, geometri ve
grup silme bağlarını ayrı sürümlere parçalamaz. Yedekte bulunmayan yerel
kitaplar silinmez. Karar öncesi ana DB hash'i tekrar doğrulanır; önizlemeden
sonra değişiklik varsa yeniden önizleme gerekir. Apply tüm kitaplarda tek
transaction: herhangi bir hata bütün değişiklikleri geri alır.

## Dosya akışları

SAF `ACTION_CREATE_DOCUMENT` yedek/TXT/Markdown; `ACTION_OPEN_DOCUMENT` restore.
Provider'a yazma, orada atomik rename garantisi vermez. Disk/sağlayıcı hatasında
yarım dış dosyayı kaldırıp yeniden deneme uyarısı gösterilir. Uygulama DB'si korunur.
TXT/Markdown kitap, fiziksel sayfa, alıntı, final not ve ayrıca etiketli dirty draft
içerir; silinmiş kayıtlar özete girmez. Markdown özel karakterleri kaçırılır.
Dışa aktarma kullanıcı seçtiği dosyaya yazmadır; mesaj/e-posta göndermez.
Markdown için `text/markdown` MIME kullanılır; `text/plain` ile sağlayıcının
`.md.txt` eklemesi gerçek Android testinde bulundu ve düzeltildi.

Android VIEW/SEND `content://` + `application/pdf` girişleri alınır. Native
private stream copy/validation/cancel hattı kullanılır. Fingerprint aynıysa
duplicate yok; eksik yerel kopya yeni seçilen aynı belgeyle onarılır. Aynı
provider URI artık başka belgeyse eski kayıt sessizce değiştirilmez.
Okuyucu açıkken gelen yeni dosya olayı kitaplığa döner, mevcut editörün taslak
flush'ını başlatır ve durable import tamamlandıktan sonra istenen kitabı açar.
Beklenmeyen SEND parcel tipi reddedilir; dış girişte Uri cast hatası uygulamayı
çökertmez. Dosya seçicisi sürerken ikinci seçici başlatılmaz.

PDF kopyasını silme kayıtları tutar; kitaplıktan tamamen kaldırma ayrıca
uyarıyla kayıtları da siler. İkisi de dış orijinali silmez. Thumbnail temizliği
yalnız cache dosyaları/metadata; not/draft/PDF konumuna dokunmaz. Process ölümünden
kalan thumbnail `.part` dosyaları ilk thumbnail talebinde tek kez temizlenir.

## Kanıt sınırı

Robolectric SQLite ve React host testleri cihaz testi değildir. İndirilenler
PDF corpus'unun host kontrolü Windows PDFium ile, Android kontrolü ayrıca gerçek
APK ile raporlanır. Emülatör Huawei performansını veya fiziksel gesture kabulünü
kanıtlamaz. Tam V1 kabulü ancak bütün M6 ve gerçek cihaz kapılarıyla verilir.

## Gerçek APK incelemesinde bulunan regresyon

İlk Android release denemesinde PDF çizildiği halde Reader hazır duruma geçemedi.
Fabric DiagnosticEvent varsayılan coalescing aynı event adına sahip ready ve
saved olaylarını bir JS frame içinde birleştiriyordu; saved, ready bilgisini
ezebiliyordu. Semantic olaylar artık canCoalesce=false ile ayrı teslim edilir.
Zoom olayları frame başına gönderilmez; pinch sonu/pan sonu veya komutla seyrektir.
Gerçek APK tekrar açma/sayaç/selection testleri bu düzeltmenin kanıtıdır.
